package com.lookseesee.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

class LookSeeSeeApp : Application(), ImageLoaderFactory {

    // Coil's video-frame decoder isn't registered by default - without this, any
    // AsyncImage pointed at a video content:// URI (album thumbnails, the video-only
    // grid) renders as a blank/black frame instead of the actual video thumbnail.
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
