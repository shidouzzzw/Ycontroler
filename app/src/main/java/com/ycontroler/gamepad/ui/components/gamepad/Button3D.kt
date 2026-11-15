package com.ycontroler.gamepad.ui.components.gamepad

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.haptics.LocalHaptics
import kotlinx.coroutines.launch

/** Tuş gövde biçimi. */
enum class CapShape { Circle, Rounded }

private fun lerpColor(from: Color, to: Color, amount: Float): Color =
    lerp(from, to, amount.coerceIn(0f, 1f))

/**
 * 3.5D tuş: altında yumuşak gölge, gövde kalınlığını gösteren koyu yan yüzey,
 * üstte gradyan + speküler parlama + iç gölge. Basınca fiziksel olarak iner.
 *
 * Dokunma işleme [detectTapGestures] ile yapılır; her tuş kendi işaretçisini takip
 * ettiği için 6+ parmakla eşzamanlı basış sorunsuz çalışır.
 */
@Composable
fun GamepadButton(
    label: String,
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: CapShape = CapShape.Circle,
    faceColor: Color = YcTheme.colors.surfaceHigh,
    accent: Color = YcTheme.colors.accent,
    glyph: FaceGlyph = FaceGlyph.Letter,
    glyphColor: Color = YcTheme.colors.text,
    depth: Dp = YcTheme.dims.buttonDepth,
    corner: Dp = 18.dp,
    enabled: Boolean = true,
    hapticStrength: Float = 0.55f,
    contentDescription: String? = null
) {
    val haptics = LocalHaptics.current
    val pressAnim = remember { Animatable(0f) }
    val measurer = rememberTextMeasurer()
    val glyphFont = YcTheme.type.title.fontFamily
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        pressAnim.animateTo(
            targetValue = if (pressed) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.42f,
                stiffness = 1500f,
                visibilityThreshold = 0.0005f
            )
        )
    }

    Canvas(
        modifier = modifier
            .semantics {
                role = Role.Button
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .pointerInput(enabled, hapticStrength) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onPress(true)
                        haptics.tick(hapticStrength)
                        tryAwaitRelease()
                        pressed = false
                        onPress(false)
                    }
                )
            }
    ) {
        val depthPx = depth.toPx()
        val press = pressAnim.value
        val capHeight = size.height - depthPx
        if (capHeight <= 0f) return@Canvas
        val faceTop = depthPx * (1f - press)
        val faceRect = Rect(Offset(0f, faceTop), Size(size.width, capHeight))
        val radius = if (shape == CapShape.Circle) {
            CornerRadius(capHeight / 2f)
        } else {
            CornerRadius(corner.toPx().coerceAtMost(capHeight / 2f))
        }
        val baseRect = Rect(Offset(0f, depthPx), Size(size.width, capHeight))

        drawCapShadow(faceRect = baseRect, press = press, corner = radius)
        drawCapSide(faceRect = faceRect, baseRect = baseRect, corner = radius, faceColor = faceColor, press = press)
        drawCapFace(faceRect = faceRect, corner = radius, faceColor = faceColor, accent = accent, press = press)

        val center = faceRect.center
        val facePath = capPath(faceRect, radius)
        when (glyph) {
            FaceGlyph.Letter -> {
                val fontSize = (capHeight / density * 0.40f).coerceIn(7f, 40f).sp
                val layout = measurer.measure(
                    text = label,
                    style = TextStyle(
                        fontFamily = glyphFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        letterSpacing = 0.4.sp
                    )
                )
                drawText(
                    textLayoutResult = layout,
                    color = glyphColor,
                    topLeft = Offset(
                        center.x - layout.size.width / 2f,
                        center.y - layout.size.height / 2f
                    )
                )
            }
            FaceGlyph.Cross -> drawSymbol(facePath, center, capHeight, glyphColor) { c, r, sw ->
                drawLine(glyphColor, Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r), sw, StrokeCap.Round)
                drawLine(glyphColor, Offset(c.x + r, c.y - r), Offset(c.x - r, c.y + r), sw, StrokeCap.Round)
            }
            FaceGlyph.Circle -> drawSymbol(facePath, center, capHeight, glyphColor) { c, r, _ ->
                drawCircle(glyphColor, radius = r, center = c, style = Stroke(width = capHeight * 0.055f))
            }
            FaceGlyph.Square -> drawSymbol(facePath, center, capHeight, glyphColor) { c, r, _ ->
                drawRoundRect(
                    color = glyphColor,
                    topLeft = Offset(c.x - r, c.y - r),
                    size = Size(r * 2, r * 2),
                    cornerRadius = CornerRadius(r * 0.28f),
                    style = Stroke(width = capHeight * 0.055f)
                )
            }
            FaceGlyph.Triangle -> drawSymbol(facePath, center, capHeight, glyphColor) { c, r, sw ->
                val p = Path().apply {
                    moveTo(c.x, c.y - r * 1.05f)
                    lineTo(c.x + r * 1.05f, c.y + r * 0.75f)
                    lineTo(c.x - r * 1.05f, c.y + r * 0.75f)
                    close()
                }
                drawPath(p, glyphColor, style = Stroke(width = sw, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }
    }
}

private fun DrawScope.drawSymbol(
    facePath: Path,
    center: Offset,
    capHeight: Float,
    color: Color,
    block: DrawScope.(Offset, Float, Float) -> Unit
) {
    clipPath(facePath) {
        block(center, capHeight * 0.16f, capHeight * 0.055f)
    }
}

/** Yumuşak, basınca küçülen gölge. */
private fun DrawScope.drawCapShadow(faceRect: Rect, press: Float, corner: CornerRadius) {
    val cx = faceRect.center.x
    val cy = faceRect.center.y
    val w = faceRect.width * (1f + 0.06f * (1f - press))
    val h = faceRect.height * 0.92f
    val alpha = 0.42f * (1f - 0.3f * press)
    for (i in 0..3) {
        val expand = i * faceRect.height * 0.10f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = alpha / (i + 1.6f)), Color.Transparent),
                center = Offset(cx, cy + h * 0.10f),
                radius = w * 0.62f + expand
            ),
            topLeft = Offset(cx - w / 2f - expand, cy - h / 2f + faceRect.height * 0.08f),
            size = Size(w + expand * 2f, h + expand * 2f)
        )
    }
    // yan yüzey tabanına çok yakın ince temas gölgesi
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.28f * (1f - 0.2f * press)),
        topLeft = Offset(cx - w / 2f, cy + h * 0.34f),
        size = Size(w, h * 0.18f),
        cornerRadius = corner
    )
}

/** Gövde kalınlığı: üst yüzün altından tabana uzanan koyu silindir. */
private fun DrawScope.drawCapSide(
    faceRect: Rect,
    baseRect: Rect,
    corner: CornerRadius,
    faceColor: Color,
    press: Float
) {
    val sideRect = Rect(
        left = faceRect.left,
        top = faceRect.center.y,
        right = faceRect.right,
        bottom = baseRect.bottom
    )
    val top = lerpColor(faceColor, Color.Black, 0.48f)
    val bottom = lerpColor(faceColor, Color.Black, 0.76f)
    drawPath(
        path = capPath(sideRect, corner),
        brush = Brush.verticalGradient(
            colors = listOf(top, bottom),
            startY = sideRect.top,
            endY = sideRect.bottom
        )
    )
    // yan yüzeyde ince vurgu çizgisi (basınca kaybolur)
    drawLine(
        color = Color.White.copy(alpha = 0.06f * (1f - press)),
        start = Offset(sideRect.left + sideRect.width * 0.12f, sideRect.top + 2f),
        end = Offset(sideRect.right - sideRect.width * 0.12f, sideRect.top + 2f),
        strokeWidth = 1f
    )
}

/** Üst yüz: dikey gradyan + speküler parlama + iç gölge. */
private fun DrawScope.drawCapFace(
    faceRect: Rect,
    corner: CornerRadius,
    faceColor: Color,
    accent: Color,
    press: Float
) {
    val facePath = capPath(faceRect, corner)
    val top = lerpColor(faceColor, Color.White, 0.13f + 0.10f * press)
    val bottom = lerpColor(faceColor, Color.Black, 0.30f - 0.06f * press)
    drawPath(
        path = facePath,
        brush = Brush.verticalGradient(
            colors = listOf(top, bottom),
            startY = faceRect.top,
            endY = faceRect.bottom
        )
    )

    clipPath(facePath) {
        // speküler parlama (üstte)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.34f * (1f - 0.35f * press)), Color.Transparent),
                center = Offset(faceRect.center.x, faceRect.top + faceRect.height * 0.10f),
                radius = faceRect.width * 0.62f
            ),
            radius = faceRect.width * 0.62f,
            center = Offset(faceRect.center.x, faceRect.top + faceRect.height * 0.10f)
        )
        // iç gölge (alt kenar)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.30f)),
                startY = faceRect.bottom - faceRect.height * 0.42f,
                endY = faceRect.bottom
            ),
            topLeft = Offset(faceRect.left, faceRect.bottom - faceRect.height * 0.42f),
            size = Size(faceRect.width, faceRect.height * 0.42f)
        )
        // ince iç halka
        drawPath(
            path = capPath(faceRect.deflate(faceRect.width * 0.045f), CornerRadius(corner.x)),
            color = Color.White.copy(alpha = 0.05f),
            style = Stroke(width = 1.2f)
        )
    }

    // dış kenar
    drawPath(
        path = facePath,
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.05f)),
            startY = faceRect.top,
            endY = faceRect.bottom
        ),
        style = Stroke(width = 1.4f)
    )
    // basınca hafif vurgu halkası
    if (press > 0.02f) {
        drawPath(
            path = facePath,
            color = accent.copy(alpha = 0.22f * press),
            style = Stroke(width = 2f)
        )
    }
}

private fun capPath(rect: Rect, corner: CornerRadius): Path = Path().apply {
    addRoundRect(RoundRect(rect, corner))
}

/**
 * Analog tetik: parmağın dikey sürükleme derinliğine göre 0..1 arası değer üretir.
 * Aynı 3.5D görsel dilini kullanır; dolum seviyesi tetiğin analog değerini gösterir.
 */
@Composable
fun AnalogTrigger(
    label: String,
    onValue: (Float) -> Unit,
    modifier: Modifier = Modifier,
    faceColor: Color = YcTheme.colors.surfaceHigh,
    accent: Color = YcTheme.colors.accent,
    hapticStrength: Float = 0.5f,
    corner: Dp = 16.dp,
    enabled: Boolean = true
) {
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()
    val settleAnim = remember { Animatable(0f) }
    var settling by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    val measurer = rememberTextMeasurer()
    val glyphFont = YcTheme.type.label.fontFamily
    val dimText = YcTheme.colors.textSecondary
    var wasFull by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier.pointerInput(hapticStrength, enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val startY = down.position.y
                val travel = size.height * 0.62f
                haptics.tick(hapticStrength * 0.8f)
                dragValue = 0.18f
                onValue(0.18f)
                down.consume()
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    val raw = 0.18f + ((change.position.y - startY) / travel).coerceIn(0f, 0.82f)
                    val v = raw.coerceIn(0f, 1f)
                    dragValue = v
                    onValue(v)
                    if (v >= 0.985f && !wasFull) {
                        haptics.ring(hapticStrength)
                        wasFull = true
                    } else if (v < 0.9f) {
                        wasFull = false
                    }
                    change.consume()
                }
                onValue(0f)
                scope.launch {
                    settleAnim.snapTo(dragValue)
                    settling = true
                    settleAnim.animateTo(0f, spring(stiffness = 900f, dampingRatio = 0.7f))
                    dragValue = 0f
                    settling = false
                }
            }
        }
    ) {
        val depthPx = size.height * 0.11f
        val value = if (settling) settleAnim.value else dragValue
        val capHeight = size.height - depthPx
        if (capHeight <= 0f) return@Canvas
        val faceTop = depthPx * (1f - value * 0.55f)
        val faceRect = Rect(Offset(0f, faceTop), Size(size.width, capHeight))
        val baseRect = Rect(Offset(0f, depthPx), Size(size.width, capHeight))
        val radius = CornerRadius(corner.toPx().coerceAtMost(capHeight / 4f))

        drawCapShadow(baseRect, value, radius)
        drawCapSide(faceRect, baseRect, radius, faceColor, value)
        drawCapFace(faceRect, radius, faceColor, accent, value)

        val facePath = capPath(faceRect, radius)
        // analog dolum
        val fillHeight = capHeight * value
        clipPath(facePath) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.85f),
                        lerpColor(accent, Color.Black, 0.35f).copy(alpha = 0.75f)
                    ),
                    startY = faceRect.bottom - fillHeight,
                    endY = faceRect.bottom
                ),
                topLeft = Offset(faceRect.left, faceRect.bottom - fillHeight),
                size = Size(faceRect.width, fillHeight)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                    startY = faceRect.bottom - fillHeight - capHeight * 0.18f,
                    endY = faceRect.bottom - fillHeight + capHeight * 0.02f
                ),
                topLeft = Offset(faceRect.left, faceRect.bottom - fillHeight - capHeight * 0.18f),
                size = Size(faceRect.width, capHeight * 0.20f)
            )
        }

        val fontSize = (capHeight / density * 0.22f).coerceIn(7f, 22f).sp
        val layout = measurer.measure(
            text = label,
            style = TextStyle(
                fontFamily = glyphFont,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 1.sp
            )
        )
        drawText(
            textLayoutResult = layout,
            color = if (value > 0.55f) Color.White.copy(alpha = 0.92f) else dimText,
            topLeft = Offset(
                faceRect.center.x - layout.size.width / 2f,
                faceRect.center.y - layout.size.height / 2f
            )
        )
    }
}
