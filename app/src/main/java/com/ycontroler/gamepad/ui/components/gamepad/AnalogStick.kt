package com.ycontroler.gamepad.ui.components.gamepad

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.haptics.LocalHaptics
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.launch

/** Analog çubuk davranış ayarları. Sol ve sağ çubuk için ayrı ayrı kullanılabilir. */
@Serializable
@Immutable
data class StickConfig(
    val floating: Boolean = false,
    val deadZone: Float = 0.06f,
    val sensitivity: Float = 1f,
    val exponential: Boolean = true,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val snapToCenter: Boolean = true,
    val knobScale: Float = 1f,
    val l3DoubleTap: Boolean = true
)

/**
 * Ham konumu ölü bölge, tepki eğrisi, hassasiyet ve eksen ters çevirme kurallarından geçirir.
 * Çıktı -1..1 aralığındadır.
 */
fun applyStickCurve(raw: Offset, config: StickConfig): Offset {
    val magnitude = raw.getDistance()
    if (magnitude < 1e-4f) return Offset.Zero
    val deadZone = config.deadZone.coerceIn(0f, 0.9f)
    val effectiveDz = if (config.snapToCenter) deadZone else deadZone * 0.35f
    if (magnitude <= effectiveDz) return Offset.Zero

    val normalized = ((magnitude - effectiveDz) / (1f - effectiveDz).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
    val curved = if (config.exponential) normalized.pow(1.75f) else normalized
    val scaled = (curved * config.sensitivity).coerceIn(0f, 1f)
    val ux = raw.x / magnitude
    val uy = raw.y / magnitude
    var outX = ux * scaled
    var outY = uy * scaled
    if (config.invertX) outX = -outX
    if (config.invertY) outY = -outY
    return Offset(outX, outY)
}

/**
 * Oyuk tabanlı, yarı küresel kafalı 3.5D analog çubuk.
 * Kafa hareket ettikçe ışık ve gölge kayar; bırakınca yay animasyonuyla merkeze döner.
 */
@Composable
fun AnalogStick(
    modifier: Modifier = Modifier,
    config: StickConfig = StickConfig(),
    onValue: (Offset) -> Unit = {},
    onL3: (Boolean) -> Unit = {},
    faceColor: Color = YcTheme.colors.surfaceHigh,
    accent: Color = YcTheme.colors.accent,
    hapticStrength: Float = 0.55f,
    label: String? = null,
    enabled: Boolean = true
) {
    val haptics = LocalHaptics.current
    val wellColor = YcTheme.colors.joystickWell
    val scope = rememberCoroutineScope()
    val settleAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var settling by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(Offset.Zero) }
    var active by remember { mutableStateOf(false) }
    var l3 by remember { mutableStateOf(false) }
    var originFraction by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    var atEdge by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .semantics {
                role = Role.Button
                contentDescription = label ?: "Analog stick"
            }
            .pointerInput(config, hapticStrength, enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val minSide = min(w, h)
                    val socketRadius = minSide / 2f - 2.dp.toPx()
                    val knobRadius = socketRadius * 0.54f * config.knobScale
                    val maxTravel = (socketRadius - knobRadius * 0.80f).coerceAtLeast(socketRadius * 0.25f)
                    val socketCenter = Offset(w / 2f, h / 2f)

                    // Floating modda çubuk parmağın bastığı yerde belirir.
                    val origin = if (config.floating) {
                        val d = down.position - socketCenter
                        val dist = d.getDistance()
                        if (dist > maxTravel * 0.55f && dist > 0f) {
                            socketCenter + d * (maxTravel * 0.55f / dist)
                        } else {
                            down.position
                        }
                    } else {
                        socketCenter
                    }
                    originFraction = Offset(origin.x / w, origin.y / h)
                    active = true
                    down.consume()

                    val now = System.currentTimeMillis()
                    if (config.l3DoubleTap && now - lastTapAt < 320L) {
                        l3 = !l3
                        onL3(l3)
                        haptics.doubleTick(hapticStrength)
                    }
                    lastTapAt = now

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val delta = change.position - origin
                        val raw = Offset(
                            (delta.x / maxTravel).coerceIn(-1f, 1f),
                            (delta.y / maxTravel).coerceIn(-1f, 1f)
                        )
                        dragValue = raw
                        onValue(applyStickCurve(raw, config))

                        val magnitude = raw.getDistance()
                        if (magnitude > 0.97f && !atEdge) {
                            haptics.ring(hapticStrength)
                            atEdge = true
                        } else if (magnitude < 0.85f) {
                            atEdge = false
                        }
                        change.consume()
                    }

                    onValue(Offset.Zero)
                    scope.launch {
                        settleAnim.snapTo(dragValue)
                        settling = true
                        settleAnim.animateTo(
                            targetValue = Offset.Zero,
                            animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f)
                        )
                        dragValue = Offset.Zero
                        settling = false
                    }
                    active = false
                    originFraction = Offset(0.5f, 0.5f)
                }
            }
    ) {
        val minSide = min(size.width, size.height)
        val socketCenter = Offset(size.width / 2f, size.height / 2f)
        val socketRadius = minSide / 2f - 2.dp.toPx()
        val knobRadius = socketRadius * 0.54f * config.knobScale
        val maxTravel = (socketRadius - knobRadius * 0.80f).coerceAtLeast(socketRadius * 0.25f)
        val origin = if (config.floating && active) {
            Offset(originFraction.x * size.width, originFraction.y * size.height)
        } else {
            socketCenter
        }
        val knobOffset = if (settling) settleAnim.value else dragValue
        val knobPosition = origin + knobOffset * maxTravel
        val knobMagnitude = knobOffset.getDistance()

        // 1) Dışarıdan yumuşak gölge
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                center = socketCenter + Offset(0f, socketRadius * 0.16f),
                radius = socketRadius * 1.18f
            ),
            radius = socketRadius * 1.18f,
            center = socketCenter + Offset(0f, socketRadius * 0.16f)
        )

        // 2) Oyuk taban
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    lerp(wellColor, Color.White, 0.06f),
                    wellColor
                ),
                center = socketCenter,
                radius = socketRadius
            ),
            radius = socketRadius,
            center = socketCenter
        )

        val socketPath = Path().apply { addOval(androidx.compose.ui.geometry.Rect(socketCenter, socketRadius)) }
        clipPath(socketPath) {
            // üst iç gölge (oyuk hissi)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.80f), Color.Transparent),
                    startY = socketCenter.y - socketRadius,
                    endY = socketCenter.y - socketRadius * 0.05f
                ),
                topLeft = Offset(socketCenter.x - socketRadius, socketCenter.y - socketRadius),
                size = androidx.compose.ui.geometry.Size(socketRadius * 2, socketRadius * 0.95f)
            )
            // alt iç aydınlanma
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.05f)),
                    startY = socketCenter.y + socketRadius * 0.45f,
                    endY = socketCenter.y + socketRadius
                ),
                topLeft = Offset(socketCenter.x - socketRadius, socketCenter.y + socketRadius * 0.45f),
                size = androidx.compose.ui.geometry.Size(socketRadius * 2, socketRadius * 0.55f)
            )
        }

        // 3) Kenar halkaları
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.04f)),
                startY = socketCenter.y - socketRadius,
                endY = socketCenter.y + socketRadius
            ),
            radius = socketRadius - 1.dp.toPx(),
            center = socketCenter,
            style = Stroke(width = 1.6.dp.toPx())
        )
        if (active || knobMagnitude > 0.02f) {
            drawCircle(
                color = accent.copy(alpha = 0.22f + 0.30f * knobMagnitude),
                radius = socketRadius - 4.dp.toPx(),
                center = socketCenter,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 4) Floating modda başlangıç halkası
        if (config.floating && active) {
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = socketRadius * 0.30f,
                center = origin,
                style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
            )
        }

        // 5) Kafa gölgesi — ışık sol üstte varsayılır
        val lightShift = Offset(-knobOffset.x * knobRadius * 0.30f, -knobOffset.y * knobRadius * 0.30f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                center = knobPosition + Offset(knobRadius * 0.12f, knobRadius * 0.42f),
                radius = knobRadius * 1.30f
            ),
            radius = knobRadius * 1.30f,
            center = knobPosition + Offset(knobRadius * 0.12f, knobRadius * 0.42f)
        )

        // 6) Kafa yan yüzeyi (kalınlık)
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(
                    lerp(faceColor, Color.Black, 0.55f),
                    lerp(faceColor, Color.Black, 0.80f)
                ),
                startY = knobPosition.y,
                endY = knobPosition.y + knobRadius
            ),
            radius = knobRadius,
            center = knobPosition + Offset(0f, knobRadius * 0.13f)
        )

        // 7) Kafa üst yüzü: yarı küresel gradyan
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    lerp(faceColor, Color.White, 0.20f + 0.04f * knobMagnitude),
                    lerp(faceColor, Color.Black, 0.34f)
                ),
                center = knobPosition + lightShift - Offset(0f, knobRadius * 0.30f),
                radius = knobRadius * 1.45f
            ),
            radius = knobRadius,
            center = knobPosition
        )

        // 8) Speküler parlama
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.42f), Color.Transparent),
                center = knobPosition + lightShift - Offset(0f, knobRadius * 0.42f),
                radius = knobRadius * 0.72f
            ),
            radius = knobRadius * 0.72f,
            center = knobPosition + lightShift - Offset(0f, knobRadius * 0.42f)
        )

        // 9) İç gölge ve kenar
        val knobPath = Path().apply { addOval(androidx.compose.ui.geometry.Rect(knobPosition, knobRadius)) }
        clipPath(knobPath) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                    startY = knobPosition.y + knobRadius * 0.15f,
                    endY = knobPosition.y + knobRadius
                ),
                topLeft = Offset(knobPosition.x - knobRadius, knobPosition.y + knobRadius * 0.15f),
                size = androidx.compose.ui.geometry.Size(knobRadius * 2, knobRadius * 0.85f)
            )
        }
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.05f)),
                startY = knobPosition.y - knobRadius,
                endY = knobPosition.y + knobRadius
            ),
            radius = knobRadius - 1.dp.toPx(),
            center = knobPosition,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // 10) L3 basılı göstergesi
        if (l3) {
            drawCircle(
                color = accent.copy(alpha = 0.85f),
                radius = knobRadius * 0.42f,
                center = knobPosition,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = accent.copy(alpha = 0.16f),
                radius = knobRadius * 0.42f,
                center = knobPosition
            )
        }
    }
}

