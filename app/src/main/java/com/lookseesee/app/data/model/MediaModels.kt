package com.lookseesee.app.data.model

import android.net.Uri

/**
 * An on-device photo/video bucket (e.g. "Camera", "WhatsApp Images", a custom folder).
 */
data class Album(
    val bucketId: String,
    val displayName: String,
    val itemCount: Int,
    val thumbnailUri: Uri,
)

/**
 * A single photo or video within a selected album, ready to display in the session pager.
 */
sealed class MediaEntry {
    abstract val id: Long
    abstract val uri: Uri
    abstract val bucketId: String
    abstract val dateAdded: Long

    data class Photo(
        override val id: Long,
        override val uri: Uri,
        override val bucketId: String,
        override val dateAdded: Long,
    ) : MediaEntry()

    data class Video(
        override val id: Long,
        override val uri: Uri,
        override val bucketId: String,
        override val dateAdded: Long,
        val durationMs: Long,
    ) : MediaEntry()
}
