package com.ycontroler.gamepad.core.design

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.ycontroler.gamepad.R

/** Özel font: Sora (variable) — premium, geometrik ve teknik bir his verir. */
private val Sora = FontFamily(
    Font(R.font.sora_variable, FontWeight.Light),
    Font(R.font.sora_variable, FontWeight.Normal),
    Font(R.font.sora_variable, FontWeight.Medium),
    Font(R.font.sora_variable, FontWeight.SemiBold),
    Font(R.font.sora_variable, FontWeight.Bold),
    Font(R.font.sora_variable, FontWeight.ExtraBold)
)

private val cleanLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

/** Uygulamanın tipografi ölçeği. */
@Immutable
data class YcTypography(
    val hero: TextStyle = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.4).sp,
        lineHeightStyle = cleanLineHeight
    ),
    val display: TextStyle = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.8).sp,
        lineHeightStyle = cleanLineHeight
    ),
    val headline: TextStyle = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp,
        lineHeightStyle = cleanLineHeight
    ),
    val title: TextStyle = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp,
        lineHeightStyle = cleanLineHeight
    ),
    val body: TextStyle = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = cleanLineHeight
    ),
    val bodyStrong: TextStyle = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = cleanLineHeight
    ),
    val bodySmall: TextStyle = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        lineHeightStyle = cleanLineHeight
    ),
    val label: TextStyle = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.8.sp,
        lineHeightStyle = cleanLineHeight
    ),
    val micro: TextStyle = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.8.sp,
        lineHeightStyle = cleanLineHeight
    ),
    val number: TextStyle = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp,
        fontFeatureSettings = "tnum",
        lineHeightStyle = cleanLineHeight
    )
)

/** Material3 bileşenlerinin varsayılan görünmemesi için temel eşleme. */
fun materialTypography(): Typography = Typography(
    displayLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 48.sp),
    headlineLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.6.sp)
)
