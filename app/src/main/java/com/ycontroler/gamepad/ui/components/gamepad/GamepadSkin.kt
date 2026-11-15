package com.ycontroler.gamepad.ui.components.gamepad

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/** Tuş sembolü teması. */
@Serializable
enum class GamepadSkin(val tr: String, val en: String) {
    XBOX("Xbox", "Xbox"),
    PLAYSTATION("PlayStation", "PlayStation"),
    NINTENDO("Nintendo", "Nintendo");

    companion object {
        fun fromKey(key: String?): GamepadSkin = entries.firstOrNull { it.name == key } ?: XBOX
    }
}

/** A/B/X/Y konumları (elmas dizilim). */
enum class FacePosition { TOP, RIGHT, BOTTOM, LEFT }

/** Yüzeyde çizilecek sembol tipi. */
enum class FaceGlyph { Letter, Cross, Circle, Square, Triangle }

@Immutable
data class FaceSpec(
    val label: String,
    val glyph: FaceGlyph,
    val color: Color
)

/**
 * Seçilen temaya göre harf, renk ve sembolleri döndürür.
 * Xbox: renkli harfler · PlayStation: geometrik semboller · Nintendo: harfler.
 */
fun faceSpec(skin: GamepadSkin, position: FacePosition): FaceSpec = when (skin) {
    GamepadSkin.XBOX -> when (position) {
        FacePosition.TOP -> FaceSpec("Y", FaceGlyph.Letter, Color(0xFFFFC93C))
        FacePosition.RIGHT -> FaceSpec("B", FaceGlyph.Letter, Color(0xFFFF5D6C))
        FacePosition.BOTTOM -> FaceSpec("A", FaceGlyph.Letter, Color(0xFF62D26F))
        FacePosition.LEFT -> FaceSpec("X", FaceGlyph.Letter, Color(0xFF4D9BFF))
    }
    GamepadSkin.PLAYSTATION -> when (position) {
        FacePosition.TOP -> FaceSpec("△", FaceGlyph.Triangle, Color(0xFF5BE3B0))
        FacePosition.RIGHT -> FaceSpec("○", FaceGlyph.Circle, Color(0xFFFF6B81))
        FacePosition.BOTTOM -> FaceSpec("×", FaceGlyph.Cross, Color(0xFF7EA6FF))
        FacePosition.LEFT -> FaceSpec("□", FaceGlyph.Square, Color(0xFFFF8ED1))
    }
    GamepadSkin.NINTENDO -> when (position) {
        FacePosition.TOP -> FaceSpec("X", FaceGlyph.Letter, Color(0xFF4D9BFF))
        FacePosition.RIGHT -> FaceSpec("A", FaceGlyph.Letter, Color(0xFF62D26F))
        FacePosition.BOTTOM -> FaceSpec("B", FaceGlyph.Letter, Color(0xFFFF5D6C))
        FacePosition.LEFT -> FaceSpec("Y", FaceGlyph.Letter, Color(0xFFFFC93C))
    }
}

/** Gamepad tuş indeksleri — HID raporundaki bit sırası ile aynıdır. */
object PadButton {
    const val A = 0
    const val B = 1
    const val X = 2
    const val Y = 3
    const val LB = 4
    const val RB = 5
    const val LT = 6
    const val RT = 7
    const val BACK = 8
    const val START = 9
    const val HOME = 10
    const val L3 = 11
    const val R3 = 12
    const val CAPTURE = 13
    const val COUNT = 14
}

/** Tetik eksenleri. */
object PadAxis {
    const val LX = 0
    const val LY = 1
    const val RX = 2
    const val RY = 3
    const val LT = 4
    const val RT = 5
    const val COUNT = 6
}
