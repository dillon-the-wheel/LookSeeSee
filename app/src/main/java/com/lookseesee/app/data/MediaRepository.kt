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
        val videoIds = queryVideos()
        val photoIds = queryImages()
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

    suspend fun loadMedia(bucketIds: Set<String>): List<MediaEntry> = withContext(Dispatchers.IO) {
        if (bucketIds.isEmpty()) return@withContext emptyList()

        val photos = queryImages()
            .filter { it.bucketId in bucketIds }
            .map { row ->
                MediaEntry.Photo(
                    id = row.id,
                    uri = contentUriFor(row.id, isVideo = false),
                    bucketId = row.bucketId,
                    dateAdded = row.dateAdded,
                )
            }
        val videos = queryVideos()
            .filter { it.bucketId in bucketIds }
            .map { row ->
                MediaEntry.Video(
                    id = row.id,
                    uri = contentUriFor(row.id, isVideo = true),
                    bucketId = row.bucketId,
                    dateAdded = row.dateAdded,
                    durationMs = row.durationMs,
                )
            }
        (photos + videos).sortedBy { it.dateAdded }
    }

    private fun contentUriFor(id: Long, isVideo: Boolean): Uri {
        val base = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return ContentUris.withAppendedId(base, id)
    }

    private fun queryImages(): List<RawRow> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
        )
        val rows = mutableListOf<RawRow>()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
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

    private fun queryVideos(): List<RawRow> {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
        )
        val rows = mutableListOf<RawRow>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
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
