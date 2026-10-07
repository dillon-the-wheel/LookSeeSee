package com.lookseesee.app.ui.end

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lookseesee.app.R
import com.lookseesee.app.ui.components.PinDialog
import com.lookseesee.app.ui.theme.SunsetDusk
import com.lookseesee.app.ui.theme.SunsetGold
import com.lookseesee.app.ui.theme.SunsetOrange
import com.lookseesee.app.ui.theme.SunsetPink
import kotlinx.coroutines.launch

private enum class PendingAction { PLAY_AGAIN, GO_HOME }

/**
 * The session's end: a sunset, a looping bell chime, and only two ways out, both
 * gated behind a long press + the parent's PIN so a toddler can't casually leave it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EndScreen(
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit,
    viewModel: EndViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }
    var pinError by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val player = MediaPlayer.create(context, R.raw.end_theme)
        player?.isLooping = true
        player?.start()
        onDispose { player?.release() }
    }

    fun handlePinSubmit(pin: String) {
        scope.launch {
            if (viewModel.verifyPin(pin)) {
                pinError = false
                when (pendingAction) {
                    PendingAction.PLAY_AGAIN -> onPlayAgain()
                    PendingAction.GO_HOME -> onGoHome()
                    null -> Unit
                }
                pendingAction = null
            } else {
                pinError = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SunsetBackground()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = { pendingAction = PendingAction.PLAY_AGAIN },
                ),
        ) {
            Icon(
                imageVector = Icons.Filled.Sailing,
                contentDescription = stringResource(R.string.cd_sailboat_icon),
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = stringResource(R.string.end_sailboat_hint),
                color = Color.White,
                fontSize = 11.sp,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = { pendingAction = PendingAction.GO_HOME },
                ),
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = stringResource(R.string.cd_house_icon),
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = stringResource(R.string.end_house_hint),
                color = Color.White,
                fontSize = 11.sp,
            )
        }

        Text(
            text = stringResource(R.string.end_all_done),
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                // Below the horizon (drawn at ~62% down), over the water rather than the sun.
                .align(BiasAlignment(horizontalBias = 0f, verticalBias = 0.6f))
                .background(Color.Black.copy(alpha = 0.25f))
                .padding(horizontal = 28.dp, vertical = 14.dp),
        )
    }

    if (pendingAction != null) {
        PinDialog(
            onDismiss = {
                pendingAction = null
                pinError = false
            },
            onSubmit = ::handlePinSubmit,
            isError = pinError,
        )
    }
}

@Composable
private fun SunsetBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val sky = Brush.verticalGradient(
            colors = listOf(SunsetGold, SunsetOrange, SunsetPink, SunsetDusk),
        )
        drawRect(brush = sky, size = size)

        val horizonY = size.height * 0.62f
        val sunRadius = size.width * 0.16f
        // Sun sits mostly above the horizon, dipping in by a small fraction of its
        // radius - the water drawn afterward covers that sliver, so the sun reads as
        // sitting on the horizon rather than floating above it or fully submerged.
        val sunCenter = Offset(size.width / 2f, horizonY - sunRadius * 0.85f)

        // Sun: soft outer halo first, then the sharp core on top, then a few faint
        // "ghost" circles trailing toward the canvas center.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SunsetGold.copy(alpha = 0.55f), Color.Transparent),
                center = sunCenter,
                radius = sunRadius * 2.2f,
            ),
            radius = sunRadius * 2.2f,
            center = sunCenter,
        )
        drawCircle(color = SunsetGold, radius = sunRadius, center = sunCenter)

        val canvasCenter = Offset(size.width / 2f, size.height / 2f)
        val flareDirection = canvasCenter - sunCenter
        for (i in 1..3) {
            val t = i * 0.35f
            val ghostCenter = sunCenter + flareDirection * t
            val ghostRadius = (sunRadius * (0.35f - i * 0.08f)).coerceAtLeast(2f)
            drawCircle(
                color = SunsetGold.copy(alpha = 0.25f / i),
                radius = ghostRadius,
                center = ghostCenter,
            )
        }

        drawAviators(sunCenter, sunRadius)

        // Water, drawn last so it sits in front of the sun and covers the small part
        // of it that dips below the horizon. The reflection is a horizontal gradient
        // across the water itself - bright where it's directly under the sun, fading
        // to plain ocean color at the left and right edges.
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(SunsetDusk, SunsetGold.copy(alpha = 0.85f), SunsetDusk),
                startX = 0f,
                endX = size.width,
            ),
            topLeft = Offset(0f, horizonY),
            size = Size(size.width, size.height - horizonY),
        )
    }
}

/** A pair of aviator sunglasses - two lenses, a bridge, and angled temple arms - to
 * personify the sun, drawn at roughly its eye-level so it reads as a little face. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAviators(sunCenter: Offset, sunRadius: Float) {
    val lensColor = Color(0xFF1A1A1A).copy(alpha = 0.88f)
    val glassesY = sunCenter.y - sunRadius * 0.05f
    val lensWidth = sunRadius * 0.62f
    val lensHeight = sunRadius * 0.46f
    val lensGap = sunRadius * 0.22f
    val leftLensCenter = Offset(sunCenter.x - lensGap / 2f - lensWidth / 2f, glassesY)
    val rightLensCenter = Offset(sunCenter.x + lensGap / 2f + lensWidth / 2f, glassesY)

    for (lensCenter in listOf(leftLensCenter, rightLensCenter)) {
        drawOval(
            color = lensColor,
            topLeft = Offset(lensCenter.x - lensWidth / 2f, lensCenter.y - lensHeight / 2f),
            size = Size(lensWidth, lensHeight),
        )
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(lensCenter.x - lensWidth * 0.2f, lensCenter.y - lensHeight * 0.2f),
            end = Offset(lensCenter.x + lensWidth * 0.05f, lensCenter.y - lensHeight * 0.3f),
            strokeWidth = lensHeight * 0.12f,
            cap = StrokeCap.Round,
        )
    }

    drawLine(
        color = lensColor,
        start = Offset(leftLensCenter.x + lensWidth / 2f, glassesY - lensHeight * 0.1f),
        end = Offset(rightLensCenter.x - lensWidth / 2f, glassesY - lensHeight * 0.1f),
        strokeWidth = lensHeight * 0.18f,
    )
    drawLine(
        color = lensColor,
        start = Offset(leftLensCenter.x - lensWidth / 2f, glassesY),
        end = Offset(leftLensCenter.x - lensWidth / 2f - sunRadius * 0.35f, glassesY - sunRadius * 0.12f),
        strokeWidth = lensHeight * 0.14f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = lensColor,
        start = Offset(rightLensCenter.x + lensWidth / 2f, glassesY),
        end = Offset(rightLensCenter.x + lensWidth / 2f + sunRadius * 0.35f, glassesY - sunRadius * 0.12f),
        strokeWidth = lensHeight * 0.14f,
        cap = StrokeCap.Round,
    )
}
