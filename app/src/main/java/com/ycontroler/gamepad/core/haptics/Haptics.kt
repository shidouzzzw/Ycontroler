package com.ycontroler.gamepad.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.staticCompositionLocalOf

/** Kısa dokunsal geri bildirimler. */
interface Haptics {
    /** Tuşa basma: çok kısa tık. */
    fun tick(strength: Float)

    /** Çubuk sınırı: hafif halka titreşimi. */
    fun ring(strength: Float)

    /** L3/R3 gibi çift dokunuşlar. */
    fun doubleTick(strength: Float)
}

object NoHaptics : Haptics {
    override fun tick(strength: Float) = Unit
    override fun ring(strength: Float) = Unit
    override fun doubleTick(strength: Float) = Unit
}

class AndroidHaptics(context: Context) : Haptics {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val supportsAmplitude: Boolean = vibrator?.hasAmplitudeControl() == true

    override fun tick(strength: Float) = oneShot(10L, strength)

    override fun ring(strength: Float) = oneShot(7L, strength * 0.75f)

    override fun doubleTick(strength: Float) {
        val v = vibrator ?: return
        if (strength <= 0.01f) return
        try {
            val amp = amplitude(strength)
            if (supportsAmplitude && amp > 0) {
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 9, 42, 13), intArrayOf(0, amp, 0, amp), -1))
            } else {
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 9, 42, 13), -1))
            }
        } catch (_: Exception) {
            // Titreşim desteklenmiyorsa sessizce geç.
        }
    }

    private fun oneShot(durationMs: Long, strength: Float) {
        val v = vibrator ?: return
        if (strength <= 0.01f) return
        try {
            val amp = amplitude(strength)
            v.vibrate(
                VibrationEffect.createOneShot(
                    durationMs,
                    if (supportsAmplitude) amp else VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } catch (_: Exception) {
            // Titreşim desteklenmiyorsa sessizce geç.
        }
    }

    private fun amplitude(strength: Float): Int =
        (strength.coerceIn(0f, 1f) * 255f).toInt().coerceIn(1, 255)
}

val LocalHaptics = staticCompositionLocalOf<Haptics> { NoHaptics }
