package com.ycontroler.gamepad.ui.components.gamepad

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.haptics.LocalHaptics
import kotlin.math.atan2
import kotlin.math.min

/**
 * Hat-switch D-pad. 8 yönlü, 3.5D görünümlü; parmak basınca yön anında değişir.
 * Yön değerleri HID hat switch sırasıyla gönderilir: 0 = yukarı, 1 = sağ-yukarı, ... 7 = sol-yukarı, -1 = nötr.
 */
@Composable
fun DPad(
    onDirection: (Int) -> Unit,
    modifier: Modifier = Modifier,
    faceColor: Color = YcTheme.colors.surfaceHigh,
    accent: Color = YcTheme.colors.accent,
    hapticStrength: Float = 0.42f,
    enabled: Boolean = true
) {
    val haptics = LocalHaptics.current
    val pressAnim = remember { Animatable(0f) }
    var activeDir by remember { mutableIntStateOf(-1) }
    var touched by remember { mutableStateOf(false) }

    LaunchedEffect(touched) {
        pressAnim.animateTo(
            targetValue = if (touched) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.45f, stiffness = 1300f)
        )
    }

    Canvas(
        modifier = modifier
            .semantics {
                role = Role.Button
                contentDescription = "D-pad"
            }
            .pointerInput(hapticStrength, enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touched = true
                    var direction = resolveHat(down.position, size)
                    activeDir = direction
                    onDirection(direction)
                    if (direction >= 0) haptics.tick(hapticStrength)
                    down.consume()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val next = resolveHat(change.position, size)
                        if (next != direction) {
                            direction = next
                            activeDir = next
                            onDirection(next)
                            if (next >= 0) haptics.tick(hapticStrength * 0.8f)
                        }
                        change.consume()
                    }
                    touched = false
                    activeDir = -1
                    onDirection(-1)
                }
            }
    ) {
        val side = min(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)
        val depthPx = side * 0.085f
        val armWidth = side * 0.315f
        val armLength = side * 0.455f
        val corner = CornerRadius(armWidth * 0.20f)
        val press = pressAnim.value
        val faceTop = depthPx * (1f - press)

        drawCrossShadow(center, armWidth, armLength, depthPx, press)
        drawSide(center = center, armWidth = armWidth, armLength = armLength, depthPx = depthPx, faceColor = faceColor)

        translate(top = faceTop) {
            val face = crossPath(center, armWidth, armLength, corner)
            drawPath(
                path = face,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lerp(faceColor, Color.White, 0.14f + 0.08f * press),
                        lerp(faceColor, Color.Black, 0.34f)
                    ),
                    startY = center.y - armLength,
                    endY = center.y + armLength
                )
            )
            clipPath(face) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.30f * (1f - 0.4f * press)), Color.Transparent),
                        center = Offset(center.x, center.y - armLength * 0.75f),
                        radius = side * 0.7f
                    ),
                    radius = side * 0.7f,
                    center = Offset(center.x, center.y - armLength * 0.75f)
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.30f)),
                        startY = center.y + armLength * 0.35f,
                        endY = center.y + armLength
                    ),
                    topLeft = Offset(center.x - armLength, center.y + armLength * 0.35f),
                    size = Size(armLength * 2, armLength * 0.65f)
                )
            }
            drawPath(
                path = face,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.04f)),
                    startY = center.y - armLength,
                    endY = center.y + armLength
                ),
                style = Stroke(width = 1.4f)
            )

            // yön okları
            val arrowOffset = armLength * 0.62f
            val inactive = Color.White.copy(alpha = 0.34f)
            val upActive = activeDir == 0 || activeDir == 1 || activeDir == 7
            val rightActive = activeDir == 1 || activeDir == 2 || activeDir == 3
            val downActive = activeDir == 3 || activeDir == 4 || activeDir == 5
            val leftActive = activeDir == 5 || activeDir == 6 || activeDir == 7
            val arrowSize = armWidth * 0.24f
            chevron(Offset(center.x, center.y - arrowOffset), 0, arrowSize, if (upActive) accent else inactive)
            chevron(Offset(center.x + arrowOffset, center.y), 90, arrowSize, if (rightActive) accent else inactive)
            chevron(Offset(center.x, center.y + arrowOffset), 180, arrowSize, if (downActive) accent else inactive)
            chevron(Offset(center.x - arrowOffset, center.y), 270, arrowSize, if (leftActive) accent else inactive)

            // merkez nokta
            drawCircle(Color.White.copy(alpha = 0.10f), radius = armWidth * 0.075f, center = center)
        }
    }
}

/** Yön oku: 0 = yukarı, 90 = sağ, 180 = aşağı, 270 = sol. */
private fun DrawScope.chevron(center: Offset, direction: Int, size: Float, color: Color) {
    val path = Path().apply {
        when (direction) {
            0 -> {
                moveTo(center.x - size, center.y + size * 0.55f)
                lineTo(center.x, center.y - size * 0.55f)
                lineTo(center.x + size, center.y + size * 0.55f)
            }
            90 -> {
                moveTo(center.x - size * 0.55f, center.y - size)
                lineTo(center.x + size * 0.55f, center.y)
                lineTo(center.x - size * 0.55f, center.y + size)
            }
            180 -> {
                moveTo(center.x - size, center.y - size * 0.55f)
                lineTo(center.x, center.y + size * 0.55f)
                lineTo(center.x + size, center.y - size * 0.55f)
            }
            else -> {
                moveTo(center.x + size * 0.55f, center.y - size)
                lineTo(center.x - size * 0.55f, center.y)
                lineTo(center.x + size * 0.55f, center.y + size)
            }
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = size * 0.42f,
            cap = StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
    )
}

private fun crossPath(center: Offset, armWidth: Float, armLength: Float, corner: CornerRadius): Path {
    val vertical = Path().apply {
        addRoundRect(
            RoundRect(
                Rect(
                    center.x - armWidth / 2f,
                    center.y - armLength,
                    center.x + armWidth / 2f,
                    center.y + armLength
                ),
                corner
            )
        )
    }
    val horizontal = Path().apply {
        addRoundRect(
            RoundRect(
                Rect(
                    center.x - armLength,
                    center.y - armWidth / 2f,
                    center.x + armLength,
                    center.y + armWidth / 2f
                ),
                corner
            )
        )
    }
    return Path().apply { op(vertical, horizontal, PathOperation.Union) }
}

private fun DrawScope.drawCrossShadow(
    center: Offset,
    armWidth: Float,
    armLength: Float,
    depthPx: Float,
    press: Float
) {
    val alpha = 0.45f * (1f - 0.3f * press)
    for (i in 0..2) {
        val expand = i * armWidth * 0.16f
        drawRoundRect(
            brush = Brush.radialGradient(
                listOf(Color.Black.copy(alpha = alpha / (i + 1.5f)), Color.Transparent),
                center = Offset(center.x, center.y + armLength * 0.25f),
                radius = armLength * 1.1f + expand
            ),
            topLeft = Offset(center.x - armLength - expand, center.y - armLength + armLength * 0.12f),
            size = Size(armLength * 2 + expand * 2, armLength * 2 + expand * 2),
            cornerRadius = CornerRadius(armWidth / 2f)
        )
    }
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.25f),
        topLeft = Offset(center.x - armLength * 0.9f, center.y + armLength * 0.5f),
        size = Size(armLength * 1.8f, armLength * 0.45f),
        cornerRadius = CornerRadius(armWidth / 2f)
    )
}

/** Gövde kalınlığı: iki çubuğun alt kenarlarından tabana uzanan koyu yüzeyler. */
private fun DrawScope.drawSide(
    center: Offset,
    armWidth: Float,
    armLength: Float,
    depthPx: Float,
    faceColor: Color
) {
    val top = lerp(faceColor, Color.Black, 0.50f)
    val bottom = lerp(faceColor, Color.Black, 0.78f)
    val brush = Brush.verticalGradient(
        colors = listOf(top, bottom),
        startY = center.y,
        endY = center.y + armLength + depthPx
    )
    val vertical = Path().apply {
        addRoundRect(
            RoundRect(
                Rect(
                    center.x - armWidth / 2f,
                    center.y,
                    center.x + armWidth / 2f,
                    center.y + armLength + depthPx
                ),
                CornerRadius(armWidth * 0.20f)
            )
        )
    }
    val horizontal = Path().apply {
        addRoundRect(
            RoundRect(
                Rect(
                    center.x - armLength,
                    center.y - armWidth / 2f,
                    center.x + armLength,
                    center.y + armWidth / 2f + depthPx
                ),
                CornerRadius(armWidth * 0.20f)
            )
        )
    }
    drawPath(vertical, brush)
    drawPath(horizontal, brush)
}

/** Dokunma noktasından HID hat switch değeri üretir. -1 = nötr. */
private fun resolveHat(position: Offset, size: IntSize): Int {
    val center = Offset(size.width / 2f, size.height / 2f)
    val delta = position - center
    val minSide = min(size.width, size.height).toFloat()
    if (delta.getDistance() < minSide * 0.10f) return -1
    var degrees = Math.toDegrees(atan2(delta.y.toDouble(), delta.x.toDouble())).toFloat()
    if (degrees < 0f) degrees += 360f
    // 0 = doğu; HID'de 0 = kuzey
    val sector = ((degrees + 22.5f) / 45f).toInt() % 8
    return when (sector) {
        6 -> 0 // kuzey
        7 -> 1 // kuzey-doğu
        0 -> 2 // doğu
        1 -> 3 // güneydoğu
        2 -> 4 // güney
        3 -> 5 // güneybatı
        4 -> 6 // batı
        else -> 7 // kuzeybatı
    }
}

