package com.ycontroler.gamepad.ui.components.gamepad

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ycontroler.gamepad.core.design.YcTheme

/**
 * A/B/X/Y elmas dizilimi. Temaya göre renk ve semboller değişir.
 * Her tuş kendi dokunuşunu takip eder; 4 tuşa aynı anda basılabilir.
 */
@Composable
fun FaceCluster(
    skin: GamepadSkin,
    onButton: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 58.dp,
    hapticStrength: Float = 0.55f,
    enabled: Boolean = true
) {
    val colors = YcTheme.colors
    val boxSize = buttonSize * 2.5f

    fun capColor(position: FacePosition): Color {
        val spec = faceSpec(skin, position)
        return if (skin == GamepadSkin.PLAYSTATION) colors.surfaceHigh else spec.color
    }

    fun glyphColor(position: FacePosition): Color {
        val spec = faceSpec(skin, position)
        return if (skin == GamepadSkin.PLAYSTATION) spec.color else Color(0xFF080A12)
    }

    Box(modifier.size(boxSize)) {
        val specTop = faceSpec(skin, FacePosition.TOP)
        GamepadButton(
            label = specTop.label,
            onPress = { onButton(PadButton.Y, it) },
            modifier = Modifier
                .size(buttonSize)
                .align(Alignment.TopCenter),
            faceColor = capColor(FacePosition.TOP),
            glyph = if (skin == GamepadSkin.PLAYSTATION) FaceGlyph.Triangle else FaceGlyph.Letter,
            glyphColor = glyphColor(FacePosition.TOP),
            hapticStrength = hapticStrength,
            enabled = enabled,
            contentDescription = specTop.label
        )
        val specRight = faceSpec(skin, FacePosition.RIGHT)
        GamepadButton(
            label = specRight.label,
            onPress = { onButton(PadButton.B, it) },
            modifier = Modifier
                .size(buttonSize)
                .align(Alignment.CenterEnd),
            faceColor = capColor(FacePosition.RIGHT),
            glyph = if (skin == GamepadSkin.PLAYSTATION) FaceGlyph.Circle else FaceGlyph.Letter,
            glyphColor = glyphColor(FacePosition.RIGHT),
            hapticStrength = hapticStrength,
            enabled = enabled,
            contentDescription = specRight.label
        )
        val specBottom = faceSpec(skin, FacePosition.BOTTOM)
        GamepadButton(
            label = specBottom.label,
            onPress = { onButton(PadButton.A, it) },
            modifier = Modifier
                .size(buttonSize)
                .align(Alignment.BottomCenter),
            faceColor = capColor(FacePosition.BOTTOM),
            glyph = if (skin == GamepadSkin.PLAYSTATION) FaceGlyph.Cross else FaceGlyph.Letter,
            glyphColor = glyphColor(FacePosition.BOTTOM),
            hapticStrength = hapticStrength,
            enabled = enabled,
            contentDescription = specBottom.label
        )
        val specLeft = faceSpec(skin, FacePosition.LEFT)
        GamepadButton(
            label = specLeft.label,
            onPress = { onButton(PadButton.X, it) },
            modifier = Modifier
                .size(buttonSize)
                .align(Alignment.CenterStart),
            faceColor = capColor(FacePosition.LEFT),
            glyph = if (skin == GamepadSkin.PLAYSTATION) FaceGlyph.Square else FaceGlyph.Letter,
            glyphColor = glyphColor(FacePosition.LEFT),
            hapticStrength = hapticStrength,
            enabled = enabled,
            contentDescription = specLeft.label
        )
    }
}

/** Omuz tuşu (LB/RB) — yatay kapsül biçiminde 3.5D tuş. */
@Composable
fun ShoulderButton(
    label: String,
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    faceColor: Color = YcTheme.colors.surfaceHigh,
    hapticStrength: Float = 0.45f,
    width: Dp = 96.dp,
    height: Dp = 46.dp
) {
    GamepadButton(
        label = label,
        onPress = onPress,
        modifier = modifier.size(width = width, height = height),
        shape = CapShape.Rounded,
        faceColor = faceColor,
        glyph = FaceGlyph.Letter,
        glyphColor = YcTheme.colors.textSecondary,
        depth = 8.dp,
        corner = 14.dp,
        hapticStrength = hapticStrength,
        contentDescription = label
    )
}

/** Küçük yardımcı tuş (Start / Select / Home). */
@Composable
fun SmallButton(
    label: String,
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    faceColor: Color = YcTheme.colors.surfaceHigh,
    hapticStrength: Float = 0.4f,
    size: Dp = 40.dp
) {
    GamepadButton(
        label = label,
        onPress = onPress,
        modifier = modifier.size(size),
        faceColor = faceColor,
        glyph = FaceGlyph.Letter,
        glyphColor = YcTheme.colors.textSecondary,
        depth = 7.dp,
        hapticStrength = hapticStrength,
        contentDescription = label
    )
}
