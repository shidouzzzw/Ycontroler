package com.ycontroler.gamepad.core.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.ycontroler.gamepad.core.i18n.Language
import com.ycontroler.gamepad.core.i18n.LocalLanguage
import kotlinx.serialization.Serializable

/** Kullanıcı ayarı: animasyon yoğunluğu. */
@Serializable
enum class AnimationLevel(val tr: String, val en: String) {
    FULL("Tam", "Full"),
    REDUCED("Azaltılmış", "Reduced"),
    OFF("Kapalı", "Off");

    val scale: Float
        get() = when (this) {
            FULL -> 1f
            REDUCED -> 0.55f
            OFF -> 0f
        }

    companion object {
        fun fromKey(key: String?): AnimationLevel = entries.firstOrNull { it.name == key } ?: FULL
    }
}

val LocalYcColors = staticCompositionLocalOf { ycColors(false, AccentTheme.NEON_BLUE.palette()) }
val LocalYcType = staticCompositionLocalOf { YcTypography() }
val LocalYcDims = staticCompositionLocalOf { YcDims() }
val LocalAccentTheme = staticCompositionLocalOf { AccentTheme.NEON_BLUE }
val LocalCustomAccent = staticCompositionLocalOf<Color?> { null }
val LocalAnimScale = staticCompositionLocalOf { 1f }

/** Tema değerlerine kısa erişim. */
object YcTheme {
    val colors: YcColors
        @Composable @ReadOnlyComposable get() = LocalYcColors.current
    val type: YcTypography
        @Composable @ReadOnlyComposable get() = LocalYcType.current
    val dims: YcDims
        @Composable @ReadOnlyComposable get() = LocalYcDims.current
    val accent: AccentTheme
        @Composable @ReadOnlyComposable get() = LocalAccentTheme.current
    val customAccent: Color?
        @Composable @ReadOnlyComposable get() = LocalCustomAccent.current
    val animScale: Float
        @Composable @ReadOnlyComposable get() = LocalAnimScale.current
}

@Composable
fun YcontrolerTheme(
    amoled: Boolean = false,
    accent: AccentTheme = AccentTheme.NEON_BLUE,
    customAccent: Color? = null,
    animationLevel: AnimationLevel = AnimationLevel.FULL,
    language: Language = Language.TR,
    content: @Composable () -> Unit
) {
    val palette = remember(accent, customAccent) {
        customAccent?.let { customPalette(it) } ?: accent.palette()
    }
    val colors = remember(amoled, palette) { ycColors(amoled, palette) }
    val type = remember { YcTypography() }
    val dims = remember { YcDims() }

    val scheme = remember(colors) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accent.copy(alpha = 0.18f),
            onPrimaryContainer = colors.text,
            secondary = colors.accentSecondary,
            onSecondary = colors.onAccent,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surfaceHigh,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            error = colors.danger,
            onError = Color.White
        )
    }

    CompositionLocalProvider(
        LocalYcColors provides colors,
        LocalYcType provides type,
        LocalYcDims provides dims,
        LocalAccentTheme provides accent,
        LocalCustomAccent provides customAccent,
        LocalAnimScale provides animationLevel.scale,
        LocalLanguage provides language,
        androidx.compose.material3.LocalContentColor provides colors.text,
        androidx.compose.material3.LocalTextStyle provides type.body
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = materialTypography(),
            content = content
        )
    }
}

/** Animasyon kapalıyken anlık geçiş, aksi hâlde yumuşak yay. */
@Composable
fun <T> motionSpring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMediumLow
) = if (LocalAnimScale.current <= 0f) {
    snap<T>()
} else {
    spring<T>(dampingRatio = dampingRatio, stiffness = stiffness)
}

/** Animasyon seviyesine göre tween. */
@Composable
fun <T> motionTween(base: Int, easing: androidx.compose.animation.core.Easing = FastOutSlowInEasing) =
    if (LocalAnimScale.current <= 0f) snap<T>() else tween<T>(durationMillis = YcMotion.dur(base, LocalAnimScale.current), easing = easing)

