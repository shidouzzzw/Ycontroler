package com.ycontroler.gamepad.hid

import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt

/** HID raporunun ham hâli. */
class GamepadReport {
    var buttons: Int = 0
    var hat: Int = -1
    var lx: Float = 0f
    var ly: Float = 0f
    var rx: Float = 0f
    var ry: Float = 0f
    var lt: Float = 0f
    var rt: Float = 0f

    fun reset() {
        buttons = 0
        hat = -1
        lx = 0f
        ly = 0f
        rx = 0f
        ry = 0f
        lt = 0f
        rt = 0f
    }

    /** 9 baytlık HID raporunu yazar. */
    fun writeTo(out: ByteArray) {
        require(out.size >= GamepadDescriptor.REPORT_SIZE_BYTES) { "Rapor tamponu çok küçük" }
        out[0] = (buttons and 0xFF).toByte()
        out[1] = ((buttons ushr 8) and 0xFF).toByte()
        val hatValue = if (hat in 0..7) hat else 0x0F
        out[2] = hatValue.toByte()
        out[3] = axisToByte(lx)
        out[4] = axisToByte(ly)
        out[5] = axisToByte(rx)
        out[6] = axisToByte(ry)
        out[7] = triggerToByte(lt)
        out[8] = triggerToByte(rt)
    }

    private fun axisToByte(value: Float): Byte =
        (((value.coerceIn(-1f, 1f) + 1f) * 127.5f).roundToInt().coerceIn(0, 255)).toByte()

    private fun triggerToByte(value: Float): Byte =
        ((value.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)).toByte()
}

/**
 * UI tarafından yazılan, HID gönderici thread'inin okuduğu paylaşılan giriş durumu.
 * Tüm erişimler kilitlidir; böylece dokunuş işleme ile rapor gönderimi çakışmaz.
 */
class GamepadInputState {

    private val lock = Any()
    private var buttons = 0
    private var hat = -1
    private var lx = 0f
    private var ly = 0f
    private var rx = 0f
    private var ry = 0f
    private var lt = 0f
    private var rt = 0f
    private var dirty = true

    fun setButton(index: Int, pressed: Boolean) {
        if (index !in 0 until GamepadDescriptor.BUTTON_COUNT) return
        synchronized(lock) {
            val mask = 1 shl index
            buttons = if (pressed) buttons or mask else buttons and mask.inv()
            dirty = true
        }
    }

    fun setHat(direction: Int) {
        synchronized(lock) {
            val normalized = if (direction in 0..7) direction else -1
            if (hat != normalized) {
                hat = normalized
                dirty = true
            }
        }
    }

    fun setStick(left: Boolean, value: Offset) {
        synchronized(lock) {
            if (left) {
                lx = value.x
                ly = value.y
            } else {
                rx = value.x
                ry = value.y
            }
            dirty = true
        }
    }

    fun setTrigger(left: Boolean, value: Float) {
        synchronized(lock) {
            if (left) lt = value else rt = value
            dirty = true
        }
    }

    /** Bağlantı koptuğunda takılı tuş kalmaması için her şeyi serbest bırakır. */
    fun releaseAll() {
        synchronized(lock) {
            buttons = 0
            hat = -1
            lx = 0f
            ly = 0f
            rx = 0f
            ry = 0f
            lt = 0f
            rt = 0f
            dirty = true
        }
    }

    /** Kirli bayrağını tüketir ve anlık görüntüyü döndürür. */
    fun consumeSnapshot(out: GamepadReport): Boolean = synchronized(lock) {
        if (!dirty) return@synchronized false
        out.buttons = buttons
        out.hat = hat
        out.lx = lx
        out.ly = ly
        out.rx = rx
        out.ry = ry
        out.lt = lt
        out.rt = rt
        dirty = false
        true
    }
}
