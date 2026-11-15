package com.ycontroler.gamepad.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ycontroler.gamepad.core.design.YcTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Yumuşak hareket eden neon ışımalardan oluşan arka plan.
 * Animasyon kapalıysa ışımalar sabit kalır (ekstra kare harcanmaz).
 */
@Composable
fun YcBackground(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val colors = YcTheme.colors
    val scale = YcTheme.animScale
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase = if (scale > 0f) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 6.2832f,
            animationSpec = infiniteRepeatable(
                tween(
                    durationMillis = (30000 / scale).toInt().coerceAtLeast(6000),
                    easing = LinearEasing
                )
            ),
            label = "phase"
        ).value
    } else {
        1.2f
    }

    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(colors.background)
            val w = size.width
            val h = size.height

            fun glow(cx: Float, cy: Float, radius: Float, color: Color, alpha: Float) {
                if (radius <= 0f) return
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                        center = Offset(cx, cy),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(cx, cy)
                )
            }

            glow(
                w * (0.16f + 0.07f * sin(phase)),
                h * 0.04f,
                w * 0.95f,
                colors.accent,
                0.20f * intensity
            )
            glow(
                w * (0.95f + 0.05f * cos(phase * 0.8f)),
                h * 0.34f,
                w * 0.85f,
                colors.accentSecondary,
                0.11f * intensity
            )
            glow(
                w * 0.5f,
                h * (1.02f + 0.02f * sin(phase * 1.3f)),
                w * 1.1f,
                colors.accent,
                0.09f * intensity
            )

            // Alt tarafta derinlik için vinyet
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, colors.background.copy(alpha = 0.65f)),
                    startY = h * 0.5f,
                    endY = h
                )
            )
        }
        content()
    }
}
