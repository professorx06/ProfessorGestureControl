package com.professorx.gesturecontrol

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var sensitivityLabel: TextView
    private val prefs by lazy { getSharedPreferences("gesture_settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshStatus()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 28)
            setBackgroundColor(0xFFF5F7FA.toInt())
        }
        fun heading(text: String, size: Float) = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(0xFF172033.toInt())
        }
        root.addView(heading("Professor Gesture Control", 24f))
        root.addView(heading("Simple motion controls for your phone", 14f).apply {
            setPadding(0, 8, 0, 24)
            setTextColor(0xFF586174.toInt())
        })

        status = heading("", 14f)
        status.setPadding(16, 16, 16, 16)
        status.setBackgroundColor(0xFFFFFFFF.toInt())
        root.addView(status)

        root.addView(heading("Shake sensitivity", 18f).apply { setPadding(0, 28, 0, 4) })
        sensitivityLabel = heading("", 14f)
        root.addView(sensitivityLabel)
        val seek = SeekBar(this).apply {
            max = 80
            progress = prefs.getInt("sensitivity", 35)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    val v = value.coerceAtLeast(5)
                    sensitivityLabel.text = "Sensitivity: $v / 80"
                    if (fromUser) prefs.edit().putInt("sensitivity", v).apply()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        root.addView(seek)
        sensitivityLabel.text = "Sensitivity: ${seek.progress.coerceAtLeast(5)} / 80"

        root.addView(actionButton("Enable shake volume control") { startShakeService() })
        root.addView(actionButton("Stop shake control") {
            stopService(Intent(this, ShakeService::class.java))
            Toast.makeText(this, "Shake control stopped", Toast.LENGTH_SHORT).show()
            refreshStatus()
        })
        root.addView(actionButton("Open app permission settings") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
            })
        })
        root.addView(actionButton("Open Android double-tap-to-wake settings") {
            Toast.makeText(this, "If your phone supports it, look under Settings > Home screen & Lock screen or Gestures & motions.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_SETTINGS))
        })
        root.addView(heading(
            "Note: Double-tap-to-wake is controlled by phone firmware. This app cannot force the screen to wake while it is fully off. Home-screen double-tap-to-lock needs a launcher/accessibility integration and is not included in this starter build.",
            13f
        ).apply {
            setTextColor(0xFF626B7A.toInt())
            setPadding(0, 22, 0, 0)
        })
        setContentView(root)
        refreshStatus()
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        gravity = Gravity.CENTER
        setOnClickListener { action() }
    }

    private fun startShakeService() {
        val intent = Intent(this, ShakeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        Toast.makeText(this, "Shake volume control enabled", Toast.LENGTH_SHORT).show()
        refreshStatus()
    }

    private fun refreshStatus() {
        val notification = if (Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) "Granted / not required" else "Not granted"
        val sensor = if (getSystemService(android.hardware.SensorManager::class.java)
                ?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) != null) "Available" else "Not available"
        status.text = "Permission & device status\nNotifications: $notification\nAccelerometer: $sensor\nShake service: user-controlled"
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 20)
    }
}
