package com.ycontroler.gamepad.features.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ycontroler.gamepad.core.design.YcTheme
import com.ycontroler.gamepad.core.i18n.t
import com.ycontroler.gamepad.ui.components.ChoiceChip
import com.ycontroler.gamepad.ui.components.GlassCard
import com.ycontroler.gamepad.ui.components.SegmentedRow
import com.ycontroler.gamepad.ui.components.SectionHeader
import com.ycontroler.gamepad.ui.components.YcScreen
import com.ycontroler.gamepad.ui.components.gamepad.AnalogStick
import com.ycontroler.gamepad.ui.components.gamepad.AnalogTrigger
import com.ycontroler.gamepad.ui.components.gamepad.DPad
import com.ycontroler.gamepad.ui.components.gamepad.FaceCluster
import com.ycontroler.gamepad.ui.components.gamepad.GamepadSkin
import com.ycontroler.gamepad.ui.components.gamepad.PadButton
import com.ycontroler.gamepad.ui.components.gamepad.ShoulderButton
import com.ycontroler.gamepad.ui.components.gamepad.SmallButton
import com.ycontroler.gamepad.ui.components.gamepad.StickConfig
import java.util.Locale

/** Tasarım sistemi ve 3.5D kontrol bileşenlerinin canlı sergisi. */
@Composable
fun ComponentPreviewScreen(onBack: () -> Unit) {
    val colors = YcTheme.colors
    val type = YcTheme.type
    var skinIndex by remember { mutableIntStateOf(0) }
    val skin = GamepadSkin.entries[skinIndex]
    var floating by remember { mutableStateOf(false) }
    val stickConfig = remember(floating) {
        StickConfig(floating = floating, deadZone = 0.06f, sensitivity = 1f, exponential = true)
    }
    val pressed = remember { mutableStateMapOf<Int, Boolean>() }
    val leftStick = remember { mutableStateOf(Offset.Zero) }
    val rightStick = remember { mutableStateOf(Offset.Zero) }
    val leftValue = remember { mutableFloatStateOf(0f) }
    val rightValue = remember { mutableFloatStateOf(0f) }
    val hat = remember { mutableIntStateOf(-1) }
    var l3 by remember { mutableStateOf(false) }
    var floatingLeft by remember { mutableStateOf(false) }
    var floatingRight by remember { mutableStateOf(true) }

    YcScreen(
        title = t("Bileşen Galerisi", "Component Gallery"),
        subtitle = t("3.5D kontrol bileşenleri", "3.5D control components"),
        onBack = onBack
    ) {
        Spacer(Modifier.height(6.dp))

        SectionHeader(
            label = t("Tasarım Sistemi", "Design System"),
            title = t("Yüz Tuşları", "Face Buttons")
        )
        Spacer(Modifier.height(12.dp))
        SegmentedRow(
            options = GamepadSkin.entries.map { if (it == GamepadSkin.PLAYSTATION) "PlayStation" else it.tr },
            selectedIndex = skinIndex,
            onSelect = { skinIndex = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        GlassCard(modifier = Modifier.fillMaxWidth(), glow = true, elevation = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FaceCluster(
                    skin = skin,
                    onButton = { index, isPressed -> pressed[index] = isPressed },
                    buttonSize = 56.dp
                )
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = t("Basınca tuş fiziksel olarak iner.", "Buttons physically travel when pressed."),
                        style = type.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    PressedReadout(pressed = pressed)
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        SectionHeader(
            label = t("Omuz ve Tetik", "Shoulder & Trigger"),
            title = t("Analog Derinlik", "Analog Depth")
        )
        Spacer(Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShoulderButton(
                        label = "LB",
                        onPress = { pressed[PadButton.LB] = it },
                        width = 84.dp,
                        height = 44.dp
                    )
                    ShoulderButton(
                        label = "RB",
                        onPress = { pressed[PadButton.RB] = it },
                        width = 84.dp,
                        height = 44.dp
                    )
                }
                AnalogTrigger(
                    label = "LT",
                    onValue = { leftValue.floatValue = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(104.dp)
                )
                AnalogTrigger(
                    label = "RT",
                    onValue = { rightValue.floatValue = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(104.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ValuePill(t("Sol", "Left"), leftValue.floatValue)
                    ValuePill(t("Sağ", "Right"), rightValue.floatValue)
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        SectionHeader(
            label = t("Yön", "Direction"),
            title = t("Hat-Switch D-Pad", "Hat-Switch D-Pad")
        )
        Spacer(Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DPad(
                    onDirection = { hat.intValue = it },
                    modifier = Modifier.size(126.dp)
                )
                Spacer(Modifier.width(24.dp))
                Column {
                    Text(
                        text = t("8 yönlü, kaydırmalı geçiş", "8-way with slide support"),
                        style = type.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "HAT ${hat.intValue}",
                        style = type.number,
                        color = colors.accent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = hatName(hat.intValue),
                        style = type.micro,
                        color = colors.textTertiary
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        SectionHeader(
            label = t("Analog", "Analog"),
            title = t("3.5D Çubuklar", "3.5D Sticks")
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(
                text = t("Sabit", "Fixed"),
                selected = !floatingLeft,
                onClick = { floatingLeft = false }
            )
            ChoiceChip(
                text = "Floating L",
                selected = floatingLeft,
                onClick = { floatingLeft = true }
            )
            ChoiceChip(
                text = "Floating R",
                selected = floatingRight,
                onClick = { floatingRight = !floatingRight }
            )
        }
        Spacer(Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnalogStick(
                        modifier = Modifier.size(134.dp),
                        config = stickConfig.copy(floating = floatingLeft),
                        onValue = { leftStick.value = it },
                        onL3 = { l3 = it },
                        label = t("Sol çubuk", "Left stick")
                    )
                    Spacer(Modifier.height(10.dp))
                    StickReadout(t("Sol", "Left"), leftStick)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnalogStick(
                        modifier = Modifier.size(134.dp),
                        config = stickConfig.copy(floating = floatingRight),
                        onValue = { rightStick.value = it },
                        onL3 = { },
                        label = t("Sağ çubuk", "Right stick")
                    )
                    Spacer(Modifier.height(10.dp))
                    StickReadout(t("Sağ", "Right"), rightStick)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (l3) {
                    t("L3 basılı (çift dokunuş)", "L3 pressed (double tap)")
                } else {
                    t("L3 için çubuğa çift dokun", "Double tap the stick for L3")
                },
                style = type.micro,
                color = colors.textTertiary
            )
        }

        Spacer(Modifier.height(28.dp))

        SectionHeader(
            label = t("Sistem", "System"),
            title = t("Yardımcı Tuşlar", "System Buttons")
        )
        Spacer(Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SmallButton("SEL", { pressed[PadButton.BACK] = it })
                SmallButton("STA", { pressed[PadButton.START] = it })
                SmallButton("HOME", { pressed[PadButton.HOME] = it }, size = 46.dp)
                SmallButton("CAP", { pressed[PadButton.CAPTURE] = it })
                Box(
                    Modifier
                        .size(46.dp)
                        .background(colors.accent.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("L3", style = type.micro, color = colors.accent, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = t(
                "Bu ekran tasarım bileşenlerini canlı test etmek için var. Kontrolcü ekranı aynı bileşenleri kullanır.",
                "This screen exists to test the design components live. The controller screen uses the same components."
            ),
            style = type.bodySmall,
            color = colors.textTertiary
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun PressedReadout(pressed: Map<Int, Boolean>) {
    val colors = YcTheme.colors
    val active = pressed.filterValues { it }.keys.sorted()
    Column {
        Text(
            text = if (active.isEmpty()) t("Basılı tuş yok", "No buttons held") else active.joinToString(" · ") { buttonName(it) },
            style = YcTheme.type.number,
            color = if (active.isEmpty()) colors.textTertiary else colors.accent
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = t("Çoklu dokunuş destekli", "Multi-touch enabled"),
            style = YcTheme.type.micro,
            color = colors.textTertiary
        )
    }
}

@Composable
private fun StickReadout(title: String, state: State<Offset>) {
    val value = state.value
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title.uppercase(), style = YcTheme.type.label, color = YcTheme.colors.textTertiary)
        Spacer(Modifier.height(4.dp))
        Text(
            text = String.format(Locale.US, "%+.2f  %+.2f", value.x, value.y),
            style = YcTheme.type.number,
            color = if (value.getDistance() > 0.01f) YcTheme.colors.accent else YcTheme.colors.textSecondary
        )
    }
}

@Composable
private fun ValuePill(label: String, value: Float) {
    val colors = YcTheme.colors
    Column(Modifier.padding(top = 2.dp)) {
        Text(label.uppercase(), style = YcTheme.type.micro, color = colors.textTertiary)
        Text(
            text = String.format(Locale.US, "%.2f", value),
            style = YcTheme.type.number,
            color = if (value > 0.01f) colors.accent else colors.textSecondary
        )
    }
}

private fun buttonName(index: Int): String = when (index) {
    PadButton.A -> "A"
    PadButton.B -> "B"
    PadButton.X -> "X"
    PadButton.Y -> "Y"
    PadButton.LB -> "LB"
    PadButton.RB -> "RB"
    PadButton.LT -> "LT"
    PadButton.RT -> "RT"
    PadButton.BACK -> "SEL"
    PadButton.START -> "STA"
    PadButton.HOME -> "HOME"
    PadButton.L3 -> "L3"
    PadButton.R3 -> "R3"
    PadButton.CAPTURE -> "CAP"
    else -> "?"
}

private fun hatName(value: Int): String = when (value) {
    -1 -> "NÖTR"
    0 -> "YUKARI"
    1 -> "SAĞ-YUKARI"
    2 -> "SAĞ"
    3 -> "SAĞ-AŞAĞI"
    4 -> "AŞAĞI"
    5 -> "SOL-AŞAĞI"
    6 -> "SOL"
    7 -> "SOL-YUKARI"
    else -> "-"
}
