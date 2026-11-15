package com.ycontroler.gamepad.hid

/**
 * HID rapor tanımlayıcısı (report descriptor).
 *
 * Yapı:
 *  - 14 buton (1 bit her biri) + 2 bit dolgu  -> 2 bayt
 *  - 4 bit hat switch + 4 bit dolgu           -> 1 bayt
 *  - X, Y, Z, Rz eksenleri (8 bit, 0..255)     -> 4 bayt (sol ve sağ analog çubuklar)
 *  - Rx, Ry eksenleri (8 bit, 0..255)          -> 2 bayt (analog tetikler)
 *  Toplam rapor: 9 bayt
 *
 * Hat switch: 0 = kuzey, 1 = kuzeydoğu ... 7 = kuzeybatı; 15 (0x0F) = nötr.
 * Tetikler: 0 = bırakıldı, 255 = tam basılı.
 */
object GamepadDescriptor {

    const val REPORT_ID: Int = 0
    const val REPORT_SIZE_BYTES: Int = 9
    const val BUTTON_COUNT: Int = 14

    private val RAW = intArrayOf(
        // Kullanım sayfası: Generic Desktop, Kullanım: Game Pad
        0x05, 0x01,
        0x09, 0x05,
        0xA1, 0x01, // Collection (Application)

        // ---- Butonlar 1..14 ----
        0x05, 0x09, // Usage Page (Button)
        0x19, 0x01, // Usage Minimum (1)
        0x29, 0x0E, // Usage Maximum (14)
        0x15, 0x00, // Logical Minimum (0)
        0x25, 0x01, // Logical Maximum (1)
        0x75, 0x01, // Report Size (1)
        0x95, 0x0E, // Report Count (14)
        0x81, 0x02, // Input (Data, Var, Abs)
        0x75, 0x01, // Report Size (1)
        0x95, 0x02, // Report Count (2)
        0x81, 0x03, // Input (Const, Var, Abs) - dolgu

        // ---- Hat switch ----
        0x05, 0x01, // Usage Page (Generic Desktop)
        0x09, 0x39, // Usage (Hat switch)
        0x15, 0x00, // Logical Minimum (0)
        0x25, 0x07, // Logical Maximum (7)
        0x35, 0x00, // Physical Minimum (0)
        0x46, 0x3B, 0x01, // Physical Maximum (315)
        0x65, 0x14, // Unit (Eng Rot: Degree)
        0x75, 0x04, // Report Size (4)
        0x95, 0x01, // Report Count (1)
        0x81, 0x42, // Input (Data, Var, Abs, Null)
        0x65, 0x00, // Unit (None)
        0x75, 0x04, // Report Size (4)
        0x95, 0x01, // Report Count (1)
        0x81, 0x03, // Input (Const, Var, Abs) - dolgu

        // ---- Analog çubuklar: X, Y, Z, Rz ----
        0x05, 0x01, // Usage Page (Generic Desktop)
        0x09, 0x30, // X
        0x09, 0x31, // Y
        0x09, 0x32, // Z
        0x09, 0x35, // Rz
        0x15, 0x00, // Logical Minimum (0)
        0x26, 0xFF, 0x00, // Logical Maximum (255)
        0x75, 0x08, // Report Size (8)
        0x95, 0x04, // Report Count (4)
        0x81, 0x02, // Input (Data, Var, Abs)

        // ---- Analog tetikler: Rx, Ry ----
        0x09, 0x33, // Rx
        0x09, 0x34, // Ry
        0x15, 0x00,
        0x26, 0xFF, 0x00,
        0x75, 0x08,
        0x95, 0x02,
        0x81, 0x02,

        0xC0 // End Collection
    )

    val BYTES: ByteArray = ByteArray(RAW.size) { RAW[it].toByte() }
}
