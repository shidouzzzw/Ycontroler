package com.ycontroler.gamepad.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

/**
 * Hazır vurgu temaları. Her tema bir ana renk, ikincil renk ve glow rengi taşır.
 * Kullanıcı ayrıca kendi vurgu rengini seçebilir (bkz. [CustomAccent]).
 */
@Serializable
enum class AccentTheme(val tr: String, val en: String) {
    NEON_BLUE("Neon Mavi", "Neon Blue"),
    MIDNIGHT_PURPLE("Mor Gece", "Midnight Purple"),
    INFERNO("Ateş", "Inferno"),
    EMERALD("Zümrüt", "Emerald"),
    RETRO_ORANGE("Retro Turuncu", "Retro Orange"),
    MONOCHROME("Monokrom", "Monochrome");

    companion object {
        fun fromKey(key: String?): AccentTheme = entries.firstOrNull { it.name == key } ?: NEON_BLUE
    }
}

@Immutable
data class AccentPalette(
    val primary: Color,
    val secondary: Color,
    val glow: Color,
    val onAccent: Color = Color(0xFF05060B),
    val gradient: List<Color> = listOf(primary, secondary)
)

fun AccentTheme.palette(): AccentPalette = when (this) {
    AccentTheme.NEON_BLUE -> AccentPalette(
        primary = Color(0xFF3D8BFF),
        secondary = Color(0xFF28E0FF),
        glow = Color(0xFF2B7BFF)
    )
    AccentTheme.MIDNIGHT_PURPLE -> AccentPalette(
        primary = Color(0xFF9B6BFF),
        secondary = Color(0xFFFF6BD6),
        glow = Color(0xFF8B5CFF)
    )
    AccentTheme.INFERNO -> AccentPalette(
        primary = Color(0xFFFF5A3C),
        secondary = Color(0xFFFFC046),
        glow = Color(0xFFFF4B2E)
    )
    AccentTheme.EMERALD -> AccentPalette(
        primary = Color(0xFF1FCE86),
        secondary = Color(0xFF9BFFD5),
        glow = Color(0xFF16C97D)
    )
    AccentTheme.RETRO_ORANGE -> AccentPalette(
        primary = Color(0xFFFF8A2B),
        secondary = Color(0xFFFFD166),
        glow = Color(0xFFFF7A18)
    )
    AccentTheme.MONOCHROME -> AccentPalette(
        primary = Color(0xFFE9EDF6),
        secondary = Color(0xFFA8B2C7),
        glow = Color(0xFFB9C4D8)
    )
}

/**
 * Kullanıcının seçtiği özel vurgu renginden palet üretir.
 * İkincil renk, ana rengin açılmış tonudur; glow ise doygunluğu artırılmış hâlidir.
 */
fun customPalette(primary: Color): AccentPalette {
    fun mix(c: Color, target: Color, amount: Float): Color = Color(
        red = c.red + (target.red - c.red) * amount,
        green = c.green + (target.green - c.green) * amount,
        blue = c.blue + (target.blue - c.blue) * amount,
        alpha = 1f
    )
    val secondary = mix(primary, Color.White, 0.55f)
    val glow = mix(primary, Color.White, 0.12f)
    val luminance = 0.299f * primary.red + 0.587f * primary.green + 0.114f * primary.blue
    val onAccent = if (luminance > 0.6f) Color(0xFF05060B) else Color.White
    return AccentPalette(primary, secondary, glow, onAccent)
}

/** Uygulamanın tüm renkleri. Tek yerden türetilir, ekranlarda sabit renk kullanılmaz. */
@Immutable
data class YcColors(
    val isAmoled: Boolean,
    val background: Color,
    val backgroundAlt: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val surfaceHigher: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val border: Color,
    val borderSoft: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentSecondary: Color,
    val glow: Color,
    val accentGradient: List<Color>,
    val onAccent: Color,
    val danger: Color,
    val success: Color,
    val warning: Color,
    val shadow: Color,
    val joystickWell: Color
)

fun ycColors(amoled: Boolean, palette: AccentPalette): YcColors {
    val black = Color(0xFF000000)
    val deep = if (amoled) black else Color(0xFF05060B)
    return YcColors(
        isAmoled = amoled,
        background = deep,
        backgroundAlt = if (amoled) Color(0xFF060606) else Color(0xFF0A0C15),
        surface = if (amoled) Color(0xFF0A0A0A) else Color(0xFF0E111C),
        surfaceHigh = if (amoled) Color(0xFF111111) else Color(0xFF141828),
        surfaceHigher = if (amoled) Color(0xFF181818) else Color(0xFF1B2033),
        glassTop = Color.White.copy(alpha = if (amoled) 0.075f else 0.09f),
        glassBottom = Color.White.copy(alpha = if (amoled) 0.018f else 0.028f),
        border = Color.White.copy(alpha = 0.14f),
        borderSoft = Color.White.copy(alpha = 0.07f),
        text = Color(0xFFF2F5FB),
        textSecondary = Color(0xFF9AA4BA),
        textTertiary = Color(0xFF606B82),
        accent = palette.primary,
        accentSecondary = palette.secondary,
        glow = palette.glow,
        accentGradient = palette.gradient,
        onAccent = palette.onAccent,
        danger = Color(0xFFFF5470),
        success = Color(0xFF35D89B),
        warning = Color(0xFFFFB020),
        shadow = Color.Black,
        joystickWell = Color(0xFF06070C)
    )
}

/** Tutarlı boşluk, köşe yarıçapı ve dokunma hedefi ölçüleri. */
@Immutable
data class YcDims(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val huge: Dp = 44.dp,
    val gutter: Dp = 20.dp,
    val radiusS: Dp = 12.dp,
    val radiusM: Dp = 18.dp,
    val radiusL: Dp = 24.dp,
    val radiusXl: Dp = 30.dp,
    val radiusPill: Dp = 100.dp,
    val buttonDepth: Dp = 10.dp,
    val minTouch: Dp = 52.dp,
    val hairline: Dp = 1.dp
)

/** Animasyon süreleri ve yay (spring) tanımları. Tek yerden yönetilir. */
object YcMotion {
    const val INSTANT = 0
    const val FAST = 150
    const val BASE = 260
    const val SLOW = 420
    const val HERO = 700

    /** Animasyon seviyesine göre süreyi ölçekler. 0 = animasyon kapalı. */
    fun dur(base: Int, scale: Float): Int = (base * scale).toInt().coerceAtLeast(0)
}
