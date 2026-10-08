package com.lookseesee.app.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads a MediaStore item's own cached thumbnail via ContentResolver.loadThumbnail -
 * the same thumbnail the system's gallery/picker UIs already generate and cache,
 * for both photos and videos - instead of decoding the full file ourselves on every
 * grid, which is what was actually making album/thumbnail menus slow to load.
 */
object ThumbnailLoader {
    private val cache = LruCache<Uri, Bitmap>(200)

    suspend fun load(context: Context, uri: Uri, widthPx: Int, heightPx: Int): Bitmap? {
        cache.get(uri)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.loadThumbnail(uri, Size(widthPx, heightPx), null)
            }.getOrNull()?.also { cache.put(uri, it) }
        }
    }
}
