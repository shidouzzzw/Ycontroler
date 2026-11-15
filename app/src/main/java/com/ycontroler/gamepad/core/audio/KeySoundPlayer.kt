package com.ycontroler.gamepad.core.audio

import android.media.AudioManager
import android.media.ToneGenerator

/** İsteğe bağlı tuş sesi. Ayar kapalıysa hiç oluşturulmaz. */
class KeySoundPlayer {

    private val tone: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 32)
    }.getOrNull()

    fun tick() {
        runCatching { tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 12) }
    }

    fun release() {
        runCatching { tone?.release() }
    }
}
