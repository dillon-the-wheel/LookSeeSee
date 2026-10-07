package com.lookseesee.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.lookseesee.app.data.model.Album
import com.lookseesee.app.data.model.MediaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads albums and media (images + videos) from the device's MediaStore.
 * Deliberately queries the Images and Video tables separately and merges the
 * results client-side, since that's the most reliable approach across OEM
 * MediaStore implementations (rather than relying on the shared Files table
 * exposing video-only columns consistently).
 */
class MediaRepository(private val context: Context) {

    private data class RawRow(
        val id: Long,
        val bucketId: String,
        val bucketName: String,
        val dateAdded: Long,
        val durationMs: Long,
    )

    suspend fun loadAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val videoIds = queryVideos(bucketIds = null)
        val photoIds = queryImages(bucketIds = null)
        val videoIdSet = videoIds.mapTo(HashSet()) { it.id }
        val rows = photoIds + videoIds

        rows.groupBy { it.bucketId }
            .map { (bucketId, groupRows) ->
                val newest = groupRows.maxBy { it.dateAdded }
                Album(
                    bucketId = bucketId,
                    displayName = groupRows.first().bucketName,
                    itemCount = groupRows.size,
                    thumbnailUri = contentUriFor(newest.id, isVideo = newest.id in videoIdSet),
                ) to newest.dateAdded
            }
            .sortedByDescending { (_, newestDateAdded) -> newestDateAdded }
            .map { (album, _) -> album }
    }

    suspend fun loadMedia(
        bucketIds: Set<String>,
        individualKeys: Set<String> = emptySet(),
        filter: MediaTypeFilter = MediaTypeFilter.BOTH,
    ): List<MediaEntry> = withContext(Dispatchers.IO) {
        val fromBuckets = if (bucketIds.isEmpty()) emptyList() else loadFromBuckets(bucketIds, filter)
        // Individually hand-picked items always show regardless of the type filter -
        // a parent who explicitly picked a video wants it included even in Photos-only mode.
        val fromPicks = if (individualKeys.isEmpty()) emptyList() else loadFromKeys(individualKeys)
        (fromBuckets + fromPicks)
            .distinctBy { it.id to (it is MediaEntry.Video) }
            .sortedByDescending { it.dateAdded }
    }

    private fun loadFromBuckets(bucketIds: Set<String>, filter: MediaTypeFilter): List<MediaEntry> {
        val photos = if (filter == MediaTypeFilter.VIDEOS) {
            emptyList()
        } else {
            queryImages(bucketIds).map { it.toPhoto() }
        }
        val videos = if (filter == MediaTypeFilter.PHOTOS) {
            emptyList()
        } else {
            queryVideos(bucketIds).map { it.toVideo() }
        }
        return photos + videos
    }

    private fun loadFromKeys(keys: Set<String>): List<MediaEntry> {
        val photoIds = mutableSetOf<Long>()
        val videoIds = mutableSetOf<Long>()
        for (key in keys) {
            val (id, isVideo) = MediaPickKey.decode(key) ?: continue
            if (isVideo) videoIds += id else photoIds += id
        }
        val photos = if (photoIds.isEmpty()) emptyList() else queryImagesByIds(photoIds).map { it.toPhoto() }
        val videos = if (videoIds.isEmpty()) emptyList() else queryVideosByIds(videoIds).map { it.toVideo() }
        return photos + videos
    }

    private fun RawRow.toPhoto() = MediaEntry.Photo(
        id = id,
        uri = contentUriFor(id, isVideo = false),
        bucketId = bucketId,
        dateAdded = dateAdded,
    )

    private fun RawRow.toVideo() = MediaEntry.Video(
        id = id,
        uri = contentUriFor(id, isVideo = true),
        bucketId = bucketId,
        dateAdded = dateAdded,
        durationMs = durationMs,
    )

    private fun contentUriFor(id: Long, isVideo: Boolean): Uri {
        val base = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return ContentUris.withAppendedId(base, id)
    }

    private fun bucketSelection(bucketIdColumn: String, bucketIds: Set<String>?): Pair<String?, Array<String>?> {
        if (bucketIds == null) return null to null
        return inSelection(bucketIdColumn, bucketIds)
    }

    private fun inSelection(column: String, values: Collection<String>): Pair<String?, Array<String>?> {
        val placeholders = values.joinToString(",") { "?" }
        return "$column IN ($placeholders)" to values.toTypedArray()
    }

    /**
     * Scopes the query to [bucketIds] via a SQL selection, rather than fetching every photo
     * or video on the entire device and filtering in Kotlin - with thousands of photos across
     * many albums, that unscoped fetch was the actual cause of the multi-second pause before
     * a session's gallery would even start, since the Setup screen's album list genuinely
     * needs every bucket (bucketIds == null there) but a session only ever needs the ones
     * the parent selected.
     */
    private fun queryImages(bucketIds: Set<String>?): List<RawRow> =
        queryImagesWithSelection(bucketSelection(MediaStore.Images.Media.BUCKET_ID, bucketIds))

    private fun queryImagesByIds(ids: Set<Long>): List<RawRow> =
        queryImagesWithSelection(inSelection(MediaStore.Images.Media._ID, ids.map { it.toString() }))

    private fun queryImagesWithSelection(selectionPair: Pair<String?, Array<String>?>): List<RawRow> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
        )
        val (selection, selectionArgs) = selectionPair
        val rows = mutableListOf<RawRow>()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                rows += RawRow(
                    id = cursor.getLong(idCol),
                    bucketId = cursor.getString(bucketIdCol) ?: continue,
                    bucketName = cursor.getString(bucketNameCol) ?: "Unknown",
                    dateAdded = cursor.getLong(dateCol),
                    durationMs = 0L,
                )
            }
        }
        return rows
    }

    private fun queryVideos(bucketIds: Set<String>?): List<RawRow> =
        queryVideosWithSelection(bucketSelection(MediaStore.Video.Media.BUCKET_ID, bucketIds))

    private fun queryVideosByIds(ids: Set<Long>): List<RawRow> =
        queryVideosWithSelection(inSelection(MediaStore.Video.Media._ID, ids.map { it.toString() }))

    private fun queryVideosWithSelection(selectionPair: Pair<String?, Array<String>?>): List<RawRow> {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
        )
        val (selection, selectionArgs) = selectionPair
        val rows = mutableListOf<RawRow>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (cursor.moveToNext()) {
                rows += RawRow(
                    id = cursor.getLong(idCol),
                    bucketId = cursor.getString(bucketIdCol) ?: continue,
                    bucketName = cursor.getString(bucketNameCol) ?: "Unknown",
                    dateAdded = cursor.getLong(dateCol),
                    durationMs = cursor.getLong(durationCol),
                )
            }
        }
        return rows
    }
}
