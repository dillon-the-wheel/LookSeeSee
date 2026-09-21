package com.lookseesee.app.ui.session

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.lookseesee.app.R
import kotlinx.coroutines.delay

/**
 * A video page with only what a toddler needs: tap the center to play/pause, and a
 * scrubber at the bottom to seek. No trim, share, download, or speed controls -
 * ExoPlayer's default controller UI is disabled in favor of this minimal surface.
 */
@Composable
fun BoxScope.VideoPlayerView(uri: Uri, scale: Float, offset: Offset) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    var isPlaying by remember(uri) { mutableStateOf(false) }
    var positionMs by remember(uri) { mutableFloatStateOf(0f) }
    var durationMs by remember(uri) { mutableFloatStateOf(0f) }
    var isScrubbing by remember(uri) { mutableStateOf(false) }
    val scrubberDescription = stringResource(R.string.cd_scrubber)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            if (!isScrubbing) {
                positionMs = player.currentPosition.coerceAtLeast(0).toFloat()
                val total = player.duration
                if (total > 0) durationMs = total.toFloat()
            }
            delay(200)
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                useController = false
                this.player = player
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y,
            ),
    )

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {
                if (isPlaying) player.pause() else player.play()
            }
            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            .padding(20.dp),
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = stringResource(R.string.cd_play_button),
            tint = Color.White,
            modifier = Modifier.padding(4.dp),
        )
    }

    if (durationMs > 0f) {
        Slider(
            value = positionMs.coerceIn(0f, durationMs),
            onValueChange = {
                isScrubbing = true
                positionMs = it
            },
            onValueChangeFinished = {
                player.seekTo(positionMs.toLong())
                isScrubbing = false
            },
            valueRange = 0f..durationMs,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .semantics { contentDescription = scrubberDescription },
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}
