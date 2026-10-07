package com.lookseesee.app.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

/**
 * Encodes a single hand-picked photo/video as a persistable string ("id:p" or "id:v")
 * so an individual pick can be told apart from an identically-numbered row in the
 * other MediaStore table, and so its content:// URI can be rebuilt without a query.
 */
object MediaPickKey {
    fun encode(id: Long, isVideo: Boolean): String = "$id:${if (isVideo) "v" else "p"}"

    fun decode(key: String): Pair<Long, Boolean>? {
        val parts = key.split(":")
        if (parts.size != 2) return null
        val id = parts[0].toLongOrNull() ?: return null
        return id to (parts[1] == "v")
    }

    fun uriFor(key: String): Uri? {
        val (id, isVideo) = decode(key) ?: return null
        val base = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        return ContentUris.withAppendedId(base, id)
    }
}
