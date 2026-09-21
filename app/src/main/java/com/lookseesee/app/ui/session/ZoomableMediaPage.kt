package com.lookseesee.app.ui.session

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private const val MAX_SCALE = 4f
private val SWIPE_THRESHOLD_DP = 96.dp

/**
 * Hosts a single photo/video page with pinch-to-zoom. While unzoomed (scale == 1),
 * any drag is pure edge-of-image "overflow" from the first pixel, so it behaves as
 * a normal swipe. While zoomed in, drag first pans the image within its bounds;
 * only once the pan is clamped at an edge does further dragging in that direction
 * accumulate as swipe-through overflow. Swipe left/right changes page, swipe down
 * opens the grid - matching a gallery app's usual feel without fighting a built-in
 * pager's own drag handling.
 */
@Composable
fun ZoomableMediaPage(
    pageKey: Any,
    onRequestNext: () -> Unit,
    onRequestPrevious: () -> Unit,
    onRequestGrid: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(scale: Float, offset: Offset) -> Unit,
) {
    var scale by remember(pageKey) { mutableFloatStateOf(1f) }
    var offset by remember(pageKey) { mutableStateOf(Offset.Zero) }
    var boxSize by remember(pageKey) { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { SWIPE_THRESHOLD_DP.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .pointerInput(pageKey) {
                awaitEachGesture {
                    var localScale = scale
                    var localOffset = offset
                    var overflow = Offset.Zero

                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        if (zoomChange != 1f || panChange != Offset.Zero) {
                            val newScale = (localScale * zoomChange).coerceIn(1f, MAX_SCALE)
                            val maxOffsetX = (boxSize.width * (newScale - 1f) / 2f).coerceAtLeast(0f)
                            val maxOffsetY = (boxSize.height * (newScale - 1f) / 2f).coerceAtLeast(0f)

                            val rawX = localOffset.x + panChange.x
                            val rawY = localOffset.y + panChange.y
                            val clampedX = rawX.coerceIn(-maxOffsetX, maxOffsetX)
                            val clampedY = rawY.coerceIn(-maxOffsetY, maxOffsetY)

                            overflow += Offset(rawX - clampedX, rawY - clampedY)
                            localScale = newScale
                            localOffset = Offset(clampedX, clampedY)
                            scale = localScale
                            offset = localOffset
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })

                    val horizontalWins = abs(overflow.x) >= abs(overflow.y)
                    when {
                        overflow.y > swipeThresholdPx && !horizontalWins -> onRequestGrid()
                        overflow.x < -swipeThresholdPx && horizontalWins -> onRequestNext()
                        overflow.x > swipeThresholdPx && horizontalWins -> onRequestPrevious()
                    }

                    if (localScale <= 1f) {
                        scale = 1f
                        offset = Offset.Zero
                    }
                }
            },
    ) {
        content(scale, offset)
    }
}
