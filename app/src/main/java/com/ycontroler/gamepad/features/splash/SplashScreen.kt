package com.ycontroler.gamepad.features.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ycontroler.gamepad.core.design.YcMotion
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.i18n.t
import com.ycontroler.gamepad.ui.components.GamepadMark
import com.ycontroler.gamepad.ui.components.YcBackground
import kotlinx.coroutines.delay

/**
 * Açılış ekranı: logo yay animasyonuyla büyür, ışıma nabız atar,
 * kelime işareti alttan süzülerek gelir ve ince bir yükleme çizgisi dolar.
 */
@Composable
fun SplashScreen(
    ready: Boolean = true,
    onFinished: () -> Unit
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    val scale = YcTheme.animScale
    var started by remember { mutableStateOf(false) }
    var minimumElapsed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        started = true
        delay(if (scale > 0f) 1750L else 600L)
        minimumElapsed = true
    }

    LaunchedEffect(minimumElapsed, ready) {
        if (minimumElapsed && ready) onFinished()
    }

    val logoScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.68f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 240f),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(YcMotion.dur(650, scale)),
        label = "logoAlpha"
    )
    val wordAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(YcMotion.dur(700, scale), delayMillis = YcMotion.dur(260, scale)),
        label = "wordAlpha"
    )
    val wordOffset by animateFloatAsState(
        targetValue = if (started) 0f else 22f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 180f),
        label = "wordOffset"
    )
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(YcMotion.dur(1300, scale), easing = FastOutSlowInEasing),
        label = "progress"
    )

    val pulse = rememberInfiniteTransition(label = "pulse")
    val glowPulse by pulse.animateFloat(
        initialValue = 0.86f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    YcBackground(intensity = 1.25f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = logoAlpha
                    val s = logoScale * glowPulse
                    scaleX = s
                    scaleY = s
                }
            ) {
                GamepadMark(
                    size = 170.dp,
                    glow = true,
                    glowStrength = glowPulse
                )
            }

            Spacer(Modifier.height(30.dp))

            Text(
                text = "YCONTROLER",
                style = TextStyle(
                    fontFamily = type.hero.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    letterSpacing = 7.sp,
                    brush = Brush.horizontalGradient(colors.accentGradient)
                ),
                modifier = Modifier
                    .alpha(wordAlpha)
                    .graphicsLayer { translationY = wordOffset }
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = t("PRO GAMEPAD · BLUETOOTH HID", "PRO GAMEPAD · BLUETOOTH HID"),
                style = type.label,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(wordAlpha * 0.9f)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 60.dp, vertical = 46.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .graphicsLayer { alpha = wordAlpha }
                    .padding(horizontal = 0.dp)
            ) {
                // arka iz
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .graphicsLayer { alpha = 0.12f }
                        .background(colors.text)
                )
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(Brush.horizontalGradient(colors.accentGradient))
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = t("Sürüm 1.0", "Version 1.0"),
                style = type.micro,
                color = colors.textTertiary,
                modifier = Modifier.alpha(wordAlpha)
            )
            Spacer(Modifier.size(2.dp))
        }
    }
}
