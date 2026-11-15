package com.ycontroler.gamepad.ui.components.gamepad

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.data.layout.ControlSpec
import com.ycontroler.gamepad.data.layout.ControlType
import com.ycontroler.gamepad.data.layout.LayoutProfile
import kotlin.math.min

/**
 * Bir düzeni çizer: kontrolcü ekranı ve düzen editörü aynı bileşeni kullanır.
 * Konumlar normalize edildiği için her ekran boyutunda tutarlı görünür.
 */
@Composable
fun GamepadLayout(
    layout: LayoutProfile,
    modifier: Modifier = Modifier,
    skin: GamepadSkin = GamepadSkin.XBOX,
    leftConfig: StickConfig = StickConfig(),
    rightConfig: StickConfig = StickConfig(),
    hapticStrength: Float = 0.5f,
    interactive: Boolean = true,
    opacity: Float = 1f,
    leftHanded: Boolean = false,
    selectedId: String? = null,
    showSelection: Boolean = false,
    onButton: (Int, Boolean) -> Unit = { _, _ -> },
    onHat: (Int) -> Unit = {},
    onStick: (Boolean, Offset) -> Unit = { _, _ -> },
    onTrigger: (Boolean, Float) -> Unit = { _, _ -> },
    onL3: (Boolean, Boolean) -> Unit = { _, _ -> }
) {
    val colors = YcTheme.colors
    BoxWithConstraints(modifier) {
        val unit = min(maxWidth.value, maxHeight.value)
        layout.controls.forEach { spec ->
            val width = (unit * spec.size * spec.aspect).dp
            val height = (unit * spec.size).dp
            val cx = if (leftHanded) 1f - spec.x else spec.x
            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * cx - width / 2,
                        y = maxHeight * spec.y - height / 2
                    )
                    .size(width, height)
                    .alpha(spec.opacity * opacity),
                contentAlignment = Alignment.Center
            ) {
                when (spec.type) {
                    ControlType.STICK_LEFT -> AnalogStick(
                        modifier = Modifier.fillMaxSize(),
                        config = leftConfig,
                        onValue = { onStick(true, it) },
                        onL3 = { onL3(true, it) },
                        faceColor = colors.surfaceHigh,
                        accent = colors.accent,
                        hapticStrength = hapticStrength,
                        label = "L3",
                        enabled = interactive
                    )
                    ControlType.STICK_RIGHT -> AnalogStick(
                        modifier = Modifier.fillMaxSize(),
                        config = rightConfig,
                        onValue = { onStick(false, it) },
                        onL3 = { onL3(false, it) },
                        faceColor = colors.surfaceHigh,
                        accent = colors.accent,
                        hapticStrength = hapticStrength,
                        label = "R3",
                        enabled = interactive
                    )
                    ControlType.DPAD -> DPad(
                        onDirection = onHat,
                        modifier = Modifier.fillMaxSize(),
                        faceColor = colors.surfaceHigh,
                        accent = colors.accent,
                        hapticStrength = hapticStrength * 0.85f,
                        enabled = interactive
                    )
                    ControlType.TRIGGER_LEFT -> AnalogTrigger(
                        label = "LT",
                        onValue = { onTrigger(true, it) },
                        modifier = Modifier.fillMaxSize(),
                        faceColor = colors.surfaceHigh,
                        accent = colors.accent,
                        hapticStrength = hapticStrength * 0.85f,
                        enabled = interactive
                    )
                    ControlType.TRIGGER_RIGHT -> AnalogTrigger(
                        label = "RT",
                        onValue = { onTrigger(false, it) },
                        modifier = Modifier.fillMaxSize(),
                        faceColor = colors.surfaceHigh,
                        accent = colors.accent,
                        hapticStrength = hapticStrength * 0.85f,
                        enabled = interactive
                    )
                    ControlType.FACE_CLUSTER -> FaceCluster(
                        skin = skin,
                        onButton = onButton,
                        modifier = Modifier.fillMaxSize(),
                        buttonSize = height / 2.5f,
                        hapticStrength = hapticStrength,
                        enabled = interactive
                    )
                    ControlType.BUTTON -> ControlButton(
                        spec = spec,
                        skin = skin,
                        interactive = interactive,
                        hapticStrength = hapticStrength,
                        onButton = onButton
                    )
                }

                if (showSelection && selectedId == spec.id) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .shadow(12.dp, RoundedCornerShape(14.dp), ambientColor = colors.accent, spotColor = colors.accent)
                            .border(
                                width = 1.5.dp,
                                color = colors.accent.copy(alpha = 0.95f),
                                shape = RoundedCornerShape(14.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlButton(
    spec: ControlSpec,
    skin: GamepadSkin,
    interactive: Boolean,
    hapticStrength: Float,
    onButton: (Int, Boolean) -> Unit
) {
    val colors = YcTheme.colors
    val position = facePositionFor(spec.buttonIndex)
    val isFace = position != null && spec.buttonIndex in PadButton.A..PadButton.Y
    val face = if (isFace) faceSpec(skin, position!!) else null
    val faceColor = when {
        face == null -> colors.surfaceHigh
        skin == GamepadSkin.PLAYSTATION -> colors.surfaceHigh
        else -> face.color
    }
    val glyphColor = when {
        face == null -> colors.textSecondary
        skin == GamepadSkin.PLAYSTATION -> face.color
        else -> Color(0xFF080A12)
    }
    val label = if (face != null && skin != GamepadSkin.PLAYSTATION) face.label else spec.label
    val glyph = if (face != null && skin == GamepadSkin.PLAYSTATION) face.glyph else FaceGlyph.Letter

    GamepadButton(
        label = label,
        onPress = { onButton(spec.buttonIndex, it) },
        modifier = Modifier.fillMaxSize(),
        shape = if (spec.aspect > 1.25f) CapShape.Rounded else CapShape.Circle,
        faceColor = faceColor,
        accent = colors.accent,
        glyph = glyph,
        glyphColor = glyphColor,
        depth = 8.dp,
        corner = 12.dp,
        enabled = interactive,
        hapticStrength = hapticStrength,
        contentDescription = spec.label
    )
}

/** A/B/X/Y indekslerini elmas konumuna çevirir. */
fun facePositionFor(buttonIndex: Int): FacePosition? = when (buttonIndex) {
    PadButton.A -> FacePosition.BOTTOM
    PadButton.B -> FacePosition.RIGHT
    PadButton.X -> FacePosition.LEFT
    PadButton.Y -> FacePosition.TOP
    else -> null
}
