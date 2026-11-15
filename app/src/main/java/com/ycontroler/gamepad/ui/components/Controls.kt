package com.ycontroler.gamepad.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.design.palette
import kotlin.math.roundToInt

/** Özel anahtar: yay animasyonlu, vurgu renkli. */
@Composable
fun YcSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = YcTheme.colors
    val offset by animateDpAsState(
        targetValue = if (checked) 23.dp else 3.dp,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 800f),
        label = "switchOffset"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.accent else Color.White.copy(alpha = 0.10f),
        animationSpec = tween(200),
        label = "switchTrack"
    )
    val knobColor by animateColorAsState(
        targetValue = if (checked) colors.onAccent else Color(0xFFD6DCEA),
        animationSpec = tween(200),
        label = "switchKnob"
    )
    Box(
        modifier = modifier
            .size(width = 50.dp, height = 30.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(trackColor)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
            .clickable(role = Role.Switch, enabled = enabled) { onCheckedChange(!checked) }
            .then(if (checked) Modifier.background(colors.accent.copy(alpha = 0.85f)) else Modifier)
            .semantics { this.contentDescription = if (checked) "On" else "Off" },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(24.dp)
                .graphicsLayer {
                    shadowElevation = 4.dp.toPx()
                    shape = CircleShape
                    clip = true
                }
                .background(knobColor, CircleShape)
        )
    }
}

/** Özel kaydırıcı: gradyan dolgu, ışımalı başparmak. */
@Composable
fun YcSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true
) {
    val colors = YcTheme.colors
    val span = (range.endInclusive - range.start).coerceAtLeast(0.0001f)
    val fraction = ((value - range.start) / span).coerceIn(0f, 1f)

    fun emit(rawFraction: Float) {
        val clamped = rawFraction.coerceIn(0f, 1f)
        var v = range.start + clamped * span
        if (steps > 0) {
            val stepSize = span / steps
            v = (v / stepSize).roundToInt() * stepSize
        }
        onValueChange(v.coerceIn(range.start, range.endInclusive))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .then(if (enabled) Modifier else Modifier.graphicsLayer { alpha = 0.4f })
            .pointerInput(range, steps, enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    emit(offset.x / size.width.toFloat())
                }
            }
            .pointerInput(range, steps, enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset -> emit(offset.x / size.width.toFloat()) }
                ) { change, _ ->
                    emit(change.position.x / size.width.toFloat())
                    change.consume()
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val trackHeight = 7.dp.toPx()
            val radius = CornerRadius(trackHeight / 2f)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.07f),
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(size.width, trackHeight),
                cornerRadius = radius
            )
            if (fraction > 0.001f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(colors.accentGradient),
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = Size(size.width * fraction, trackHeight),
                    cornerRadius = radius
                )
            }
            val thumbX = size.width * fraction
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colors.accent.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(thumbX, centerY),
                    radius = 15.dp.toPx()
                ),
                radius = 15.dp.toPx(),
                center = Offset(thumbX, centerY)
            )
            drawCircle(color = Color(0xFFF7FAFF), radius = 7.5.dp.toPx(), center = Offset(thumbX, centerY))
            drawCircle(
                color = colors.accent.copy(alpha = 0.55f),
                radius = 10.5.dp.toPx(),
                center = Offset(thumbX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

@Composable
fun YcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = YcTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = YcTheme.type.body.copy(color = colors.text),
        cursorBrush = SolidColor(colors.accent),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .glassSurface(shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(10.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = YcTheme.type.body, color = colors.textTertiary)
                    }
                    innerTextField()
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
        }
    )
}

/** Ayar satırı: ikon, başlık, açıklama ve sağdaki kontrol. */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: YcIcon? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = YcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color.White.copy(alpha = 0.045f)),
                contentAlignment = Alignment.Center
            ) {
                YcIcon(icon, Modifier.size(17.dp), tint = colors.textSecondary)
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = YcTheme.type.bodyStrong, color = colors.text)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = YcTheme.type.bodySmall, color = colors.textTertiary)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/** Animasyonlu açılır ayar bölümü. */
@Composable
fun SettingsSection(
    title: String,
    icon: YcIcon,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    val colors = YcTheme.colors
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "sectionChevron"
    )
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                YcIcon(icon, Modifier.size(18.dp), tint = colors.accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = YcTheme.type.title, color = colors.text)
                if (subtitle != null && !expanded) {
                    Text(subtitle, style = YcTheme.type.micro, color = colors.textTertiary)
                }
            }
            YcIcon(
                icon = YcIcon.ChevronDown,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation),
                tint = colors.textTertiary
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(260)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(220)) + fadeOut(tween(140))
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                content()
            }
        }
    }
}

/** Vurgu rengi seçici: hazır temalar + serbest ton seçimi. */
@Composable
fun AccentPicker(
    selectedTheme: com.ycontroler.gamepad.core.design.AccentTheme,
    customArgb: Long?,
    onSelectTheme: (com.ycontroler.gamepad.core.design.AccentTheme) -> Unit,
    onCustomColor: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = YcTheme.colors
    val entries = com.ycontroler.gamepad.core.design.AccentTheme.entries
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            entries.forEach { theme ->
                val palette = theme.palette()
                val isSelected = customArgb == null && selectedTheme == theme
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.verticalGradient(listOf(palette.primary, palette.secondary))
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(13.dp)
                        )
                        .clickable(role = Role.RadioButton) { onSelectTheme(theme) }
                        .semantics {
                            contentDescription = theme.tr
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        YcIcon(YcIcon.Check, Modifier.size(16.dp), tint = palette.onAccent, strokeWidth = 2.2.dp)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "ÖZEL RENK",
            style = YcTheme.type.label,
            color = colors.textTertiary
        )
        Spacer(Modifier.height(6.dp))
        HueSlider(
            currentArgb = customArgb,
            onColor = onCustomColor
        )
    }
}

/** Ton çubuğu: 0-360 derece arasında vurgu rengi seçer. */
@Composable
fun HueSlider(
    currentArgb: Long?,
    onColor: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = YcTheme.colors
    val initialHue = remember(currentArgb) {
        if (currentArgb == null) {
            215f
        } else {
            val c = Color(currentArgb)
            val max = maxOf(c.red, c.green, c.blue)
            val min = minOf(c.red, c.green, c.blue)
            val delta = (max - min).coerceAtLeast(0.0001f)
            when (max) {
                c.red -> 60f * (((c.green - c.blue) / delta) % 6f)
                c.green -> 60f * (((c.blue - c.red) / delta) + 2f)
                else -> 60f * (((c.red - c.green) / delta) + 4f)
            }.let { if (it < 0f) it + 360f else it }
        }
    }
    var hue by remember(initialHue) { mutableFloatStateOf(initialHue) }

    fun emit(fraction: Float) {
        val newHue = (fraction.coerceIn(0f, 1f) * 360f)
        hue = newHue
        onColor(hueColor(newHue))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .pointerInput(Unit) {
                detectTapGestures { emit(it.x / size.width.toFloat()) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { emit(it.x / size.width.toFloat()) }
                ) { change, _ ->
                    emit(change.position.x / size.width.toFloat())
                    change.consume()
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val barHeight = 12.dp.toPx()
            val steps = 24
            val stepWidth = size.width / steps
            for (i in 0 until steps) {
                drawRect(
                    color = hueColor(i * 360f / steps),
                    topLeft = Offset(i * stepWidth, centerY - barHeight / 2f),
                    size = Size(stepWidth + 1f, barHeight)
                )
            }
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.20f),
                topLeft = Offset(0f, centerY - barHeight / 2f),
                size = Size(size.width, barHeight),
                cornerRadius = CornerRadius(barHeight / 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
            val x = size.width * (hue / 360f).coerceIn(0f, 1f)
            drawCircle(Color.White, radius = 9.dp.toPx(), center = Offset(x, centerY))
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = 9.dp.toPx(),
                center = Offset(x, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
            drawCircle(hueColor(hue), radius = 5.dp.toPx(), center = Offset(x, centerY))
        }
    }
}

fun hueColor(hue: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f / 60f
    val x = 1f - kotlin.math.abs(h % 2f - 1f)
    val (r, g, b) = when (h.toInt()) {
        0 -> Triple(1f, x, 0f)
        1 -> Triple(x, 1f, 0f)
        2 -> Triple(0f, 1f, x)
        3 -> Triple(0f, x, 1f)
        4 -> Triple(x, 0f, 1f)
        else -> Triple(1f, 0f, x)
    }
    return Color(
        red = r * 0.92f + 0.08f,
        green = g * 0.92f + 0.08f,
        blue = b * 0.92f + 0.08f
    )
}

/** Uygulamanın onay diyaloğu. */
@Composable
fun YcConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    val colors = YcTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(
                    shape = RoundedCornerShape(24.dp),
                    topAlpha = 0.12f,
                    bottomAlpha = 0.06f
                )
                .background(colors.background.copy(alpha = 0.92f))
                .padding(22.dp)
        ) {
            Text(title, style = YcTheme.type.headline, color = colors.text)
            Spacer(Modifier.height(8.dp))
            Text(message, style = YcTheme.type.bodySmall, color = colors.textSecondary)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    text = dismissText,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                Box(Modifier.weight(1f)) {
                    if (destructive) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.danger.copy(alpha = 0.16f))
                                .border(1.dp, colors.danger.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .clickable(onClick = onConfirm),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(confirmText, style = YcTheme.type.bodyStrong, color = colors.danger)
                        }
                    } else {
                        PrimaryButton(
                            text = confirmText,
                            onClick = onConfirm,
                            modifier = Modifier.fillMaxWidth(),
                            minHeight = 50.dp,
                            shine = false
                        )
                    }
                }
            }
        }
    }
}

/** Ayar satırı için küçük metin ipucu (sağda değer). */
@Composable
fun ValueText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = YcTheme.type.number,
        color = YcTheme.colors.textSecondary,
        modifier = modifier
    )
}

