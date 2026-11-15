package com.ycontroler.gamepad.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ycontroler.gamepad.core.design.YcTheme

/**
 * Cam yüzey: yarı saydam dikey gradyan + ince üst kenar parlaması.
 * Gerçek blur yerine katmanlı gradyan kullanılır; her cihazda akıcı kalır.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(YcTheme.dims.radiusL),
    topAlpha: Float = 0.09f,
    bottomAlpha: Float = 0.025f,
    borderAlpha: Float = 0.16f,
    glow: Color? = null,
    elevation: Dp = 0.dp
): Modifier {
    var result = this
    if (elevation > 0.dp) {
        result = result.shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = (glow ?: Color.Black).copy(alpha = 0.35f),
            spotColor = (glow ?: Color.Black).copy(alpha = 0.45f)
        )
    }
    return result
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = topAlpha),
                    Color.White.copy(alpha = bottomAlpha)
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = borderAlpha),
                    Color.White.copy(alpha = borderAlpha * 0.18f)
                )
            ),
            shape = shape
        )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(YcTheme.dims.radiusL),
    glow: Boolean = false,
    elevation: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(YcTheme.dims.l),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .glassSurface(
                shape = shape,
                glow = if (glow) YcTheme.colors.glow else null,
                elevation = elevation
            )
            .padding(contentPadding),
        content = content
    )
}

/** Ana eylem butonu: gradyan dolgu, neon gölge, basınca yay ile küçülme. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    shine: Boolean = true,
    minHeight: Dp = 58.dp
) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "ctaScale"
    )
    val animScale = YcTheme.animScale
    val shineTransition = rememberInfiniteTransition(label = "shine")
    val shineX = if (shine && animScale > 0f) {
        shineTransition.animateFloat(
            initialValue = -1f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                tween((4200 / animScale).toInt().coerceAtLeast(1500), easing = LinearEasing)
            ),
            label = "shineX"
        ).value
    } else {
        -1f
    }
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = minHeight)
            .shadow(
                elevation = if (enabled) 20.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = colors.glow.copy(alpha = 0.55f),
                spotColor = colors.glow.copy(alpha = 0.75f)
            )
            .clip(shape)
            .background(Brush.horizontalGradient(colors.accentGradient))
            .then(if (enabled) Modifier else Modifier.alpha(0.38f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Kayan parlama (shine) katmanı
        if (shine && animScale > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 0.5f }
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            start = Offset(shineX * 520f, 0f),
                            end = Offset(shineX * 520f + 180f, 220f)
                        )
                    )
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                CompositionLocalProvider(LocalContentColor provides colors.onAccent) { icon() }
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = type.title,
                color = colors.onAccent,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** İkincil buton: cam yüzey, ince çerçeve. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    minHeight: Dp = 50.dp
) {
    val colors = YcTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "secScale"
    )
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .heightIn(min = minHeight)
            .glassSurface(shape = shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .then(if (enabled) Modifier else Modifier.alpha(0.4f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides colors.text) { icon() }
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = YcTheme.type.bodyStrong, color = colors.text)
    }
}

/** Sadece ikon içeren yuvarlak cam buton. */
@Composable
fun IconButtonGlass(
    icon: YcIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = YcTheme.colors.text,
    contentDescription: String? = null
) {
    val shape = RoundedCornerShape(size / 2)
    Box(
        modifier = modifier
            .size(size)
            .glassSurface(shape = shape)
            .clickable(role = Role.Button, onClick = onClick)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        YcIcon(icon = icon, modifier = Modifier.size(size * 0.46f), tint = tint)
    }
}

/** Bölüm başlığı: üstte küçük etiket, altında başlık. */
@Composable
fun SectionHeader(label: String, title: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = label.uppercase(),
            style = YcTheme.type.label,
            color = YcTheme.colors.accent
        )
        Spacer(Modifier.size(6.dp))
        Text(text = title, style = YcTheme.type.headline, color = YcTheme.colors.text)
    }
}

/** Boş durum tasarımı. */
@Composable
fun EmptyState(
    icon: YcIcon,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(76.dp)
                .glassSurface(
                    shape = RoundedCornerShape(26.dp),
                    glow = YcTheme.colors.glow,
                    elevation = 18.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            YcIcon(icon, modifier = Modifier.size(30.dp), tint = YcTheme.colors.accent)
        }
        Spacer(Modifier.size(20.dp))
        Text(
            text = title,
            style = YcTheme.type.title,
            color = YcTheme.colors.text,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = message,
            style = YcTheme.type.body,
            color = YcTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.size(20.dp))
            action()
        }
    }
}
