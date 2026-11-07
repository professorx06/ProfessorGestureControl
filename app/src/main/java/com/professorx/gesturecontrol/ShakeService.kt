package com.professorx.gesturecontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock

class ShakeService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var audioManager: AudioManager
    private var lastTrigger = 0L
    private var lastMagnitude = SensorManager.GRAVITY_EARTH
    private val prefs by lazy { getSharedPreferences("gesture_settings", Context.MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AudioManager::class.java)
        sensorManager = getSystemService(SensorManager::class.java)
        createChannel()
        val notification: Notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL)
                .setContentTitle("Professor Gesture Control")
                .setContentText("Listening for shake gestures")
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Professor Gesture Control")
                .setContentText("Listening for shake gestures")
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setOngoing(true)
                .build()
        }
        startForeground(NOTIFICATION_ID, notification)
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
        val delta = kotlin.math.abs(magnitude - lastMagnitude)
        lastMagnitude = magnitude
        val sensitivity = prefs.getInt("sensitivity", 35).coerceIn(5, 80)
        val threshold = 24f - (sensitivity * 0.22f)
        val now = SystemClock.elapsedRealtime()
        if (delta > threshold && now - lastTrigger > 900) {
            lastTrigger = now
            audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Gesture detection", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    companion object {
        private const val CHANNEL = "gesture_service"
        private const val NOTIFICATION_ID = 101
    }
}
