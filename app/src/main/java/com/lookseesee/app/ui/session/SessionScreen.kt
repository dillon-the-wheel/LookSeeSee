package com.lookseesee.app.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lookseesee.app.R
import com.lookseesee.app.data.model.MediaEntry

private val CORNER_SKIP_HOTSPOT_SIZE = 64.dp
private const val CORNER_TAP_REQUIRED_COUNT = 5
private const val CORNER_TAP_WINDOW_MS = 1500L

@Composable
fun SessionScreen(
    onEnterKiosk: () -> Unit,
    onSessionEnded: () -> Unit,
    viewModel: SessionViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        onEnterKiosk()
    }

    LaunchedEffect(Unit) {
        viewModel.sessionEnded.collect { onSessionEnded() }
    }

    // Swallow the back button entirely during a session - no path out except the
    // parent-only long-press actions on the end screen, or the corner tap-to-skip.
    BackHandler(enabled = true) {}

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (state.mediaList.isNotEmpty()) {
            AnimatedContent(
                targetState = state.currentIndex,
                transitionSpec = {
                    if (targetState >= initialState) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                val item = state.mediaList[index]
                ZoomableMediaPage(
                    pageKey = item.id,
                    onRequestNext = viewModel::goNext,
                    onRequestPrevious = viewModel::goPrevious,
                    onRequestGrid = viewModel::openGrid,
                ) { scale, offset ->
                    when (item) {
                        is MediaEntry.Photo -> AsyncImage(
                            model = item.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y,
                                ),
                        )
                        is MediaEntry.Video -> VideoPlayerView(item.uri, scale, offset)
                    }
                }
            }
        }

        if (state.showGrid) {
            GridOverlay(
                mediaList = state.mediaList,
                onSelect = viewModel::selectFromGrid,
            )
        }

        // Invisible bottom-right hotspot: 5 quick taps in a row skips straight to the end
        // screen, so a toddler's single stray tap while swiping can't trigger it by accident.
        var tapCount by remember { mutableIntStateOf(0) }
        var lastTapTime by remember { mutableLongStateOf(0L) }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(CORNER_SKIP_HOTSPOT_SIZE)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        val now = System.currentTimeMillis()
                        tapCount = if (now - lastTapTime > CORNER_TAP_WINDOW_MS) 1 else tapCount + 1
                        lastTapTime = now
                        if (tapCount >= CORNER_TAP_REQUIRED_COUNT) {
                            tapCount = 0
                            viewModel.skipToEnd()
                        }
                    },
                ),
        )
    }
}

@Composable
private fun GridOverlay(mediaList: List<MediaEntry>, onSelect: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(mediaList.size) { index ->
                AsyncImage(
                    model = mediaList[index].uri,
                    contentDescription = stringResource(R.string.cd_grid_thumbnail),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(2.dp)
                        .aspectRatio(1f)
                        .clickable { onSelect(index) },
                )
            }
        }
    }
}
