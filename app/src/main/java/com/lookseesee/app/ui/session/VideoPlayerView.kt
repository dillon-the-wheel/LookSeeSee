package com.lookseesee.app.ui.session

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.DpSize
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.VideoPlayerView(uri: Uri, scale: Float, offset: Offset, pause: Boolean = false) {
    val context = LocalContext.current
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    var isPlaying by remember(uri) { mutableStateOf(false) }
    var isBuffering by remember(uri) { mutableStateOf(true) }
    var positionMs by remember(uri) { mutableFloatStateOf(0f) }
    var durationMs by remember(uri) { mutableFloatStateOf(0f) }
    var isScrubbing by remember(uri) { mutableStateOf(false) }
    var controlsVisible by remember(uri) { mutableStateOf(true) }
    var revealToken by remember(uri) { mutableIntStateOf(0) }
    val scrubberDescription = stringResource(R.string.cd_scrubber)

    // Only the first tap (and every tap after) starts the auto-hide countdown - the
    // initial play invitation stays on screen until someone actually presses it.
    LaunchedEffect(revealToken) {
        if (revealToken > 0) {
            controlsVisible = true
            delay(1_000)
            controlsVisible = false
        }
    }

    // Swiping down to open the grid overlay keeps this page composed underneath it,
    // so without this the video would keep playing (and its audio keep going) while
    // hidden behind the grid.
    LaunchedEffect(pause) {
        if (pause) player.pause()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
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

    // The clickable zone always stays put (size fixed before clickable, so the tap
    // target matches the full circle rather than just the icon's own bounds), but the
    // visible circle - background and icon/spinner together - only renders while
    // controlsVisible (or while buffering, which always stays visible as a loading cue).
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .size(64.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (isPlaying) player.pause() else player.play()
                revealToken++
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isBuffering || controlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_button),
                        tint = Color.White,
                    )
                }
            }
        }
    }

    if (durationMs > 0f) {
        // A small dot marks the current position rather than a filled bar, so the
        // scrubber doesn't read as a thick progress indicator obstructing the video.
        // Active/inactive track share the same faint color for the same reason.
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
                .padding(horizontal = 16.dp, vertical = 40.dp)
                .semantics { contentDescription = scrubberDescription },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.4f),
                inactiveTrackColor = Color.White.copy(alpha = 0.4f),
            ),
            interactionSource = sliderInteractionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = sliderInteractionSource,
                    colors = SliderDefaults.colors(thumbColor = Color.White),
                    thumbSize = DpSize(10.dp, 10.dp),
                )
            },
        )
    }
}
