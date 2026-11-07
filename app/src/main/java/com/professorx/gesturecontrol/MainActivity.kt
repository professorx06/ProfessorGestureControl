package com.professorx.gesturecontrol

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var sensitivityLabel: TextView
    private lateinit var lockStatus: TextView
    private val prefs by lazy { getSharedPreferences("gesture_settings", MODE_PRIVATE) }
    private val adminComponent by lazy { ComponentName(this, ScreenLockAdminReceiver::class.java) }
    private val devicePolicy by lazy { getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager }

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
            setPadding(dp(22), dp(28), dp(22), dp(28))
            setBackgroundColor(0xFFF4F6FA.toInt())
        }
        fun heading(text: String, size: Float, color: Int = 0xFF172033.toInt()) =
            TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(color)
            }

        root.addView(heading("Professor Gesture Control", 25f))
        root.addView(heading("Home gestures and quick volume access", 14f, 0xFF626B7A.toInt()).apply {
            setPadding(0, dp(6), 0, dp(20))
        })

        root.addView(heading("Double-tap to lock", 19f).apply { setPadding(0, dp(4), 0, dp(8)) })
        val gestureZone = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(0xFFE2EAF8.toInt())
        }
        gestureZone.addView(heading("DOUBLE TAP THIS AREA", 16f, 0xFF174A8B.toInt()))
        gestureZone.addView(heading("After setting this app as your Home app", 12f, 0xFF40516A.toInt()).apply {
            setPadding(0, dp(5), 0, 0)
        })
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onDoubleTap(e: MotionEvent): Boolean {
                lockScreen()
                return true
            }
        })
        gestureZone.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
        root.addView(gestureZone, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(116)
        ))

        lockStatus = heading("", 13f, 0xFF586174.toInt()).apply { setPadding(0, dp(8), 0, dp(4)) }
        root.addView(lockStatus)
        root.addView(actionButton("1. Allow screen-lock permission") { requestDeviceAdmin() })
        root.addView(actionButton("2. Set as default Home app") {
            try {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        })

        root.addView(heading("Your apps", 19f).apply { setPadding(0, dp(24), 0, dp(8)) })
        addLaunchableApps(root)

        root.addView(heading("Shake to open volume panel", 19f).apply { setPadding(0, dp(24), 0, dp(5)) })
        sensitivityLabel = heading("", 14f, 0xFF586174.toInt())
        root.addView(sensitivityLabel)
        val seek = SeekBar(this).apply {
            max = 80
            progress = prefs.getInt("sensitivity", 35).coerceIn(5, 80)
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

        root.addView(heading("Permission and device status", 19f).apply { setPadding(0, dp(24), 0, dp(8)) })
        status = heading("", 14f, 0xFF39465A.toInt()).apply {
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        root.addView(status)
        root.addView(heading(
            "Important: This app can lock the screen after a double-tap in the marked area when it is acting as your Home app. Waking a fully-off screen by double-tap is controlled by your phone firmware and cannot be guaranteed by a normal app.",
            12f, 0xFF626B7A.toInt()
        ).apply { setPadding(0, dp(18), 0, 0) })

        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        refreshStatus()
    }

    private fun addLaunchableApps(root: LinearLayout) {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(query, 0)
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        var row: LinearLayout? = null
        apps.forEachIndexed { index, info ->
            if (index % 3 == 0) {
                val newRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                row = newRow
                root.addView(newRow, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ))
            }
            val label = info.loadLabel(packageManager).toString()
            val icon = info.loadIcon(packageManager)
            icon.setBounds(0, 0, dp(32), dp(32))
            val appButton = Button(this).apply {
                text = label
                textSize = 11f
                isAllCaps = false
                maxLines = 2
                setCompoundDrawables(null, icon, null, null)
                compoundDrawablePadding = dp(5)
                setOnClickListener {
                    val launch = Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setClassName(info.activityInfo.packageName, info.activityInfo.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try { startActivity(launch) } catch (_: Exception) {
                        Toast.makeText(this@MainActivity, "Unable to open $label", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            row?.addView(appButton, LinearLayout.LayoutParams(0, dp(92), 1f).apply {
                setMargins(dp(3), dp(3), dp(3), dp(3))
            })
        }
        val remainder = apps.size % 3
        if (remainder != 0 && row != null) {
            repeat(3 - remainder) {
                row?.addView(View(this), LinearLayout.LayoutParams(0, dp(92), 1f))
            }
        }
        if (apps.isEmpty()) {
            root.addView(TextView(this).apply {
                text = "No launchable apps were found."
                textSize = 13f
            })
        }
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        gravity = Gravity.CENTER
        setOnClickListener { action() }
    }

    private fun requestDeviceAdmin() {
        if (devicePolicy.isAdminActive(adminComponent)) {
            Toast.makeText(this, "Screen-lock permission already enabled", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "This permission lets the double-tap gesture lock your phone screen.")
        }
        startActivity(intent)
    }

    private fun lockScreen() {
        if (!devicePolicy.isAdminActive(adminComponent)) {
            Toast.makeText(this, "First allow screen-lock permission", Toast.LENGTH_LONG).show()
            requestDeviceAdmin()
            return
        }
        try {
            devicePolicy.lockNow()
        } catch (_: SecurityException) {
            Toast.makeText(this, "Screen-lock permission is not active", Toast.LENGTH_LONG).show()
        }
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
        ) "Permission Granted" else "Not Granted"
        val sensorAvailable = getSystemService(android.hardware.SensorManager::class.java)
            ?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) != null
        val sensor = if (sensorAvailable) "Available" else "Not available"
        val admin = if (devicePolicy.isAdminActive(adminComponent)) "Permission Granted" else "Not Granted"
        lockStatus.text = "Screen-lock permission: $admin"
        status.text = "Notifications: $notification\nScreen lock (Device Admin): $admin\nAccelerometer: $sensor\nHome app: Choose Professor Gesture Control in Android Home settings."
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 20)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
