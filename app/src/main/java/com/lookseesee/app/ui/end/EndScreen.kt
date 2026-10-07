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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                .align(Alignment.Center)
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
        val sunCenter = Offset(size.width / 2f, horizonY)

        // Water, drawn before the reflection so the reflection sits on top of it.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(SunsetDusk.copy(alpha = 0.6f), SunsetDusk),
                startY = horizonY,
                endY = size.height,
            ),
            topLeft = Offset(0f, horizonY),
            size = Size(size.width, size.height - horizonY),
        )

        // Diffuse reflection: a soft, flattened radial glow rather than a crisp circle,
        // the way a real sun's reflection smears across rippling water.
        val reflectionCenter = Offset(size.width / 2f, horizonY + sunRadius * 1.1f)
        val reflectionGlow = Brush.radialGradient(
            colors = listOf(
                SunsetGold.copy(alpha = 0.45f),
                SunsetGold.copy(alpha = 0.18f),
                Color.Transparent,
            ),
            center = reflectionCenter,
            radius = sunRadius * 1.8f,
        )
        drawOval(
            brush = reflectionGlow,
            topLeft = Offset(reflectionCenter.x - sunRadius * 1.8f, reflectionCenter.y - sunRadius * 0.8f),
            size = Size(sunRadius * 3.6f, sunRadius * 1.6f),
        )

        // Sun: soft outer halo first, then the sharp core on top, then a lens-flare
        // cross and a few faint "ghost" circles trailing toward the canvas center.
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

        val flareColor = Color.White.copy(alpha = 0.5f)
        val streakThickness = 3.dp.toPx()
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, flareColor, Color.Transparent),
                startX = sunCenter.x - sunRadius * 3f,
                endX = sunCenter.x + sunRadius * 3f,
            ),
            topLeft = Offset(sunCenter.x - sunRadius * 3f, sunCenter.y - streakThickness / 2f),
            size = Size(sunRadius * 6f, streakThickness),
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, flareColor, Color.Transparent),
                startY = sunCenter.y - sunRadius * 2f,
                endY = sunCenter.y + sunRadius * 2f,
            ),
            topLeft = Offset(sunCenter.x - streakThickness / 2f, sunCenter.y - sunRadius * 2f),
            size = Size(streakThickness, sunRadius * 4f),
        )

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
    }
}
