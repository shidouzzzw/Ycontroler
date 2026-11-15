package com.ycontroler.gamepad.data.settings

import androidx.compose.ui.graphics.Color
import com.ycontroler.gamepad.core.design.AccentTheme
import com.ycontroler.gamepad.core.design.AnimationLevel
import com.ycontroler.gamepad.core.i18n.Language
import com.ycontroler.gamepad.ui.components.gamepad.GamepadSkin
import com.ycontroler.gamepad.ui.components.gamepad.StickConfig
import kotlinx.serialization.Serializable

/** Tetik davranışı. */
@Serializable
enum class TriggerMode(val tr: String, val en: String) {
    ANALOG("Analog", "Analog"),
    DIGITAL("Dijital", "Digital");

    companion object {
        fun fromKey(key: String?): TriggerMode = entries.firstOrNull { it.name == key } ?: ANALOG
    }
}

/** Ekran yönü tercihi. */
@Serializable
enum class OrientationMode(val tr: String, val en: String) {
    LANDSCAPE("Yatay", "Landscape"),
    SENSOR("Cihaz sensörü", "Device sensor");

    companion object {
        fun fromKey(key: String?): OrientationMode = entries.firstOrNull { it.name == key } ?: LANDSCAPE
    }
}

/**
 * Uygulamanın tüm kalıcı ayarları. DataStore içinde tek bir JSON nesnesi olarak saklanır;
 * yeni alanlar varsayılan değerleriyle geriye dönük uyumlu kalır.
 */
@Serializable
data class YcSettings(
    // Genel
    val language: Language = Language.TR,
    val accent: AccentTheme = AccentTheme.NEON_BLUE,
    val customAccentArgb: Long? = null,
    val amoled: Boolean = false,
    val animationLevel: AnimationLevel = AnimationLevel.FULL,
    val onboardingDone: Boolean = false,

    // Kontrol
    val hapticsEnabled: Boolean = true,
    val hapticStrength: Float = 0.6f,
    val buttonScale: Float = 1f,
    val buttonOpacity: Float = 1f,
    val skin: GamepadSkin = GamepadSkin.XBOX,
    val soundEnabled: Boolean = true,

    // Analog
    val leftStick: StickConfig = StickConfig(),
    val rightStick: StickConfig = StickConfig(floating = true),

    // Tetikler
    val triggerMode: TriggerMode = TriggerMode.ANALOG,
    val triggerSensitivity: Float = 1f,

    // Sensör
    val gyroEnabled: Boolean = false,
    val gyroSensitivity: Float = 0.55f,

    // Bağlantı
    val autoReconnect: Boolean = true,
    val deviceName: String = "Ycontroler",
    val reportRateHz: Int = 250,

    // Ekran
    val keepScreenOn: Boolean = true,
    val brightness: Float? = null,
    val orientation: OrientationMode = OrientationMode.LANDSCAPE,
    val leftHanded: Boolean = false,
    val hudVisible: Boolean = true,

    // Düzen
    val activeLayoutId: String = "fps"
) {
    val customAccentColor: Color?
        get() = customAccentArgb?.let { Color(it) }
}
