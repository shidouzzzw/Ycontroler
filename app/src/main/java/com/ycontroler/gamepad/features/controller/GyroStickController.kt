package com.ycontroler.gamepad.features.controller

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.geometry.Offset

/**
 * Jiroskopu sağ analog çubuk gibi kullanır.
 *
 * Açısal hız integrali alınır, sönümleme uygulanır: telefonu çevirdikçe çubuk o yöne kayar,
 * hareket durunca yumuşakça merkeze döner. Değerler ana iş parçacığına post edilir.
 */
class GyroStickController(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val handler = Handler(Looper.getMainLooper())

    private var callback: ((Offset) -> Unit)? = null
    private var sensitivity = 0.55f
    private var valueX = 0f
    private var valueY = 0f
    private var lastTimestamp = 0L

    fun start(sensitivity: Float, onValue: (Offset) -> Unit) {
        val manager = sensorManager ?: return
        val sensor = gyroscope ?: return
        this.sensitivity = sensitivity.coerceIn(0f, 1f)
        callback = onValue
        valueX = 0f
        valueY = 0f
        lastTimestamp = 0L
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        callback = null
        valueX = 0f
        valueY = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        val now = event.timestamp
        val dt = if (lastTimestamp == 0L) 0.01f else ((now - lastTimestamp) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastTimestamp = now

        // Yatay tutuşta dönme eksenleri: yaw -> X, pitch -> Y
        val factor = 0.55f + sensitivity * 2.4f
        valueX = (valueX + event.values[1] * dt * factor).coerceIn(-1f, 1f)
        valueY = (valueY + event.values[0] * dt * factor).coerceIn(-1f, 1f)

        // Hareket durduğunda merkeze dönüş
        val decay = (1f - dt * 2.4f).coerceIn(0f, 1f)
        valueX *= decay
        valueY *= decay
        if (kotlin.math.abs(valueX) < 0.004f) valueX = 0f
        if (kotlin.math.abs(valueY) < 0.004f) valueY = 0f

        val snapshot = Offset(valueX, valueY)
        handler.post { callback?.invoke(snapshot) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
