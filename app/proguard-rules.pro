# HID / Bluetooth
-keep class android.bluetooth.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.ycontroler.gamepad.** {
    *** Companion;
}
