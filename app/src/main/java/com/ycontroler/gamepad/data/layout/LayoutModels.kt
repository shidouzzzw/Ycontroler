package com.ycontroler.gamepad.data.layout

import com.ycontroler.gamepad.ui.components.gamepad.PadButton
import kotlinx.serialization.Serializable

/** Düzen üzerindeki kontrol tipi. */
@Serializable
enum class ControlType {
    BUTTON, STICK_LEFT, STICK_RIGHT, DPAD, TRIGGER_LEFT, TRIGGER_RIGHT, FACE_CLUSTER
}

/**
 * Tek bir kontrolün yerleşimi. Konum ve boyut ekranın oranına göre saklanır,
 * böylece her ekran boyutunda aynı düzen çalışır.
 */
@Serializable
data class ControlSpec(
    val id: String,
    val type: ControlType,
    val x: Float,
    val y: Float,
    val size: Float,
    val aspect: Float = 1f,
    val opacity: Float = 1f,
    val buttonIndex: Int = PadButton.A,
    val label: String = "A"
)

/** Kaydedilmiş gamepad düzeni / profili. */
@Serializable
data class LayoutProfile(
    val id: String,
    val name: String,
    val controls: List<ControlSpec>,
    val builtIn: Boolean = false,
    val updatedAt: Long = 0L
)

/** Uygulamayla gelen hazır düzenler. */
object DefaultLayouts {

    private fun button(
        id: String,
        index: Int,
        label: String,
        x: Float,
        y: Float,
        size: Float,
        aspect: Float = 1f
    ) = ControlSpec(
        id = id,
        type = ControlType.BUTTON,
        x = x,
        y = y,
        size = size,
        aspect = aspect,
        buttonIndex = index,
        label = label
    )

    private fun stick(id: String, left: Boolean, x: Float, y: Float, size: Float) = ControlSpec(
        id = id,
        type = if (left) ControlType.STICK_LEFT else ControlType.STICK_RIGHT,
        x = x,
        y = y,
        size = size,
        label = if (left) "L3" else "R3"
    )

    private fun trigger(id: String, left: Boolean, x: Float, y: Float, size: Float) = ControlSpec(
        id = id,
        type = if (left) ControlType.TRIGGER_LEFT else ControlType.TRIGGER_RIGHT,
        x = x,
        y = y,
        size = size,
        aspect = 0.62f,
        label = if (left) "LT" else "RT"
    )

    private fun systemButtons(y: Float, size: Float) = listOf(
        button("back", PadButton.BACK, "SEL", 0.42f, y, size),
        button("home", PadButton.HOME, "HOME", 0.50f, y, size),
        button("start", PadButton.START, "STA", 0.58f, y, size)
    )

    val fps = LayoutProfile(
        id = "fps",
        name = "FPS",
        builtIn = true,
        controls = listOf(
            stick("fps-stick-l", true, 0.13f, 0.72f, 0.32f),
            stick("fps-stick-r", false, 0.87f, 0.72f, 0.32f),
            ControlSpec("fps-dpad", ControlType.DPAD, 0.14f, 0.37f, 0.28f),
            ControlSpec("fps-face", ControlType.FACE_CLUSTER, 0.86f, 0.37f, 0.34f),
            button("fps-lb", PadButton.LB, "LB", 0.13f, 0.115f, 0.10f, 2.1f),
            button("fps-rb", PadButton.RB, "RB", 0.87f, 0.115f, 0.10f, 2.1f),
            trigger("fps-lt", true, 0.30f, 0.13f, 0.20f),
            trigger("fps-rt", false, 0.70f, 0.13f, 0.20f)
        ) + systemButtons(0.945f, 0.095f)
    )

    val racing = LayoutProfile(
        id = "racing",
        name = "Racing",
        builtIn = true,
        controls = listOf(
            stick("racing-stick-l", true, 0.11f, 0.78f, 0.30f),
            stick("racing-stick-r", false, 0.89f, 0.78f, 0.30f),
            ControlSpec("racing-dpad", ControlType.DPAD, 0.13f, 0.40f, 0.24f),
            ControlSpec("racing-face", ControlType.FACE_CLUSTER, 0.87f, 0.40f, 0.30f),
            button("racing-lb", PadButton.LB, "LB", 0.12f, 0.10f, 0.09f, 2.2f),
            button("racing-rb", PadButton.RB, "RB", 0.88f, 0.10f, 0.09f, 2.2f),
            trigger("racing-lt", true, 0.32f, 0.17f, 0.28f),
            trigger("racing-rt", false, 0.68f, 0.17f, 0.28f)
        ) + systemButtons(0.945f, 0.09f)
    )

    val platform = LayoutProfile(
        id = "platform",
        name = "Platform",
        builtIn = true,
        controls = listOf(
            stick("platform-stick-l", true, 0.14f, 0.76f, 0.28f),
            stick("platform-stick-r", false, 0.86f, 0.76f, 0.28f),
            ControlSpec("platform-dpad", ControlType.DPAD, 0.16f, 0.40f, 0.32f),
            ControlSpec("platform-face", ControlType.FACE_CLUSTER, 0.84f, 0.40f, 0.36f),
            button("platform-lb", PadButton.LB, "LB", 0.14f, 0.11f, 0.10f, 2.1f),
            button("platform-rb", PadButton.RB, "RB", 0.86f, 0.11f, 0.10f, 2.1f),
            trigger("platform-lt", true, 0.31f, 0.14f, 0.18f),
            trigger("platform-rt", false, 0.69f, 0.14f, 0.18f)
        ) + systemButtons(0.94f, 0.095f)
    )

    val emulator = LayoutProfile(
        id = "emulator",
        name = "Emulator",
        builtIn = true,
        controls = listOf(
            stick("emulator-stick-l", true, 0.12f, 0.70f, 0.26f),
            stick("emulator-stick-r", false, 0.88f, 0.70f, 0.26f),
            ControlSpec("emulator-dpad", ControlType.DPAD, 0.14f, 0.40f, 0.30f),
            ControlSpec("emulator-face", ControlType.FACE_CLUSTER, 0.86f, 0.40f, 0.32f),
            button("emulator-lb", PadButton.LB, "LB", 0.14f, 0.12f, 0.09f, 2.0f),
            button("emulator-rb", PadButton.RB, "RB", 0.86f, 0.12f, 0.09f, 2.0f),
            trigger("emulator-lt", true, 0.32f, 0.16f, 0.17f),
            trigger("emulator-rt", false, 0.68f, 0.16f, 0.17f),
            button("emulator-sel", PadButton.BACK, "SEL", 0.44f, 0.93f, 0.10f, 1.6f),
            button("emulator-start", PadButton.START, "STA", 0.56f, 0.93f, 0.10f, 1.6f)
        )
    )

    val all: List<LayoutProfile> = listOf(fps, racing, platform, emulator)
}
