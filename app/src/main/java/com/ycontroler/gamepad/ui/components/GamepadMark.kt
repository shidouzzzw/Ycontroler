package com.ycontroler.gamepad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ycontroler.gamepad.core.design.YcTheme

/**
 * Marka işareti: dolgun, yumuşak köşeli bir gamepad silüeti.
 * Gölge, gradyan ve speküler parlaklık katmanlarıyla hafif 3.5D hissi verir.
 */
@Composable
fun GamepadMark(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    glow: Boolean = true,
    glowStrength: Float = 1f,
    tint: Color = YcTheme.colors.accent,
    tint2: Color = YcTheme.colors.accentSecondary
) {
    val colors = YcTheme.colors
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        if (glow) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = 0.32f * glowStrength), Color.Transparent),
                    center = center,
                    radius = w * 0.66f
                ),
                radius = w * 0.66f,
                center = center
            )
        }

        val body = Path().apply {
            moveTo(w * 0.50f, h * 0.27f)
            cubicTo(w * 0.66f, h * 0.27f, w * 0.76f, h * 0.32f, w * 0.80f, h * 0.44f)
            cubicTo(w * 0.865f, h * 0.58f, w * 0.905f, h * 0.745f, w * 0.835f, h * 0.785f)
            cubicTo(w * 0.78f, h * 0.815f, w * 0.72f, h * 0.745f, w * 0.675f, h * 0.665f)
            cubicTo(w * 0.635f, h * 0.59f, w * 0.575f, h * 0.565f, w * 0.50f, h * 0.565f)
            cubicTo(w * 0.425f, h * 0.565f, w * 0.365f, h * 0.59f, w * 0.325f, h * 0.665f)
            cubicTo(w * 0.28f, h * 0.745f, w * 0.22f, h * 0.815f, w * 0.165f, h * 0.785f)
            cubicTo(w * 0.095f, h * 0.745f, w * 0.135f, h * 0.58f, w * 0.20f, h * 0.44f)
            cubicTo(w * 0.24f, h * 0.32f, w * 0.34f, h * 0.27f, w * 0.50f, h * 0.27f)
            close()
        }

        // Alt gölge
        translate(top = h * 0.035f) {
            drawPath(body, Color.Black.copy(alpha = 0.5f))
        }

        // Gövde: dikey gradyan
        drawPath(
            path = body,
            brush = Brush.verticalGradient(
                colors = listOf(tint2, tint),
                startY = h * 0.27f,
                endY = h * 0.82f
            )
        )

        // Üst speküler parlama
        drawPath(
            path = body,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.42f), Color.Transparent),
                startY = h * 0.27f,
                endY = h * 0.48f
            )
        )

        // D-pad (sol) ve aksiyon tuşları (sağ)
        val glyph = Color(0xFF080B13).copy(alpha = 0.92f)
        val cx = w * 0.345f
        val cy = h * 0.44f
        val arm = w * 0.032f
        val len = w * 0.095f
        drawRoundRect(
            color = glyph,
            topLeft = Offset(cx - len, cy - arm),
            size = androidx.compose.ui.geometry.Size(len * 2, arm * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(arm)
        )
        drawRoundRect(
            color = glyph,
            topLeft = Offset(cx - arm, cy - len),
            size = androidx.compose.ui.geometry.Size(arm * 2, len * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(arm)
        )

        val r = w * 0.036f
        val rightX = w * 0.655f
        drawCircle(colors.onAccent.copy(alpha = 0.85f), radius = r, center = Offset(rightX, cy - w * 0.052f))
        drawCircle(colors.background.copy(alpha = 0.55f), radius = r, center = Offset(rightX + w * 0.062f, cy))
        drawCircle(colors.onAccent.copy(alpha = 0.85f), radius = r, center = Offset(rightX, cy + w * 0.052f))
        drawCircle(colors.background.copy(alpha = 0.55f), radius = r, center = Offset(rightX - w * 0.062f, cy))
    }
}
