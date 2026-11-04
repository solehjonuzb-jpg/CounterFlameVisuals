
package com.counterflame.visuals

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*
import android.provider.Settings
import android.view.WindowManager
import kotlin.math.max

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private var fpsView: TextView? = null

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var fpsParams: WindowManager.LayoutParams? = null

    private var panelVisible = false
    private var fpsEnabled = false
    private var pinkTheme = false
    private var opacity = 95

    private val prefs by lazy {
        getSharedPreferences("cf_visuals", MODE_PRIVATE)
    }

    private val updateHz = object : Runnable {
        override fun run() {
            if (fpsEnabled) {
                val hz = if (Build.VERSION.SDK_INT >= 30) {
                    display?.refreshRate ?: 60f
                } else {
                    @Suppress("DEPRECATION")
                    (wm.defaultDisplay.refreshRate)
                }

                fpsView?.text = "DISPLAY: ${hz.toInt()} Hz"

                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        pinkTheme = prefs.getBoolean("pink_theme", false)
        opacity = prefs.getInt("opacity", 95)

        startNotification()
        createBubble()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    private fun startNotification() {
        val channelId = "cf_visuals_channel"

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId,
                "CounterFlame Visuals",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("CounterFlame Visuals")
            .setContentText("Floating menu is running")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun glassBackground(): GradientDrawable {
        val color = if (pinkTheme) {
            0xEEED3AAB.toInt()
        } else {
            0xEE8A36FF.toInt()
        }

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            setColor(color)
            setStroke(dp(1), Color.WHITE)
        }
    }

    private fun panelBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(22).toFloat()
            setColor(0xEE170D2C.toInt())
            setStroke(
                dp(1),
                if (pinkTheme) 0xFFFF71CB.toInt()
                else 0xFFB56BFF.toInt()
            )
        }
    }

    private fun createBubble() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val button = TextView(this).apply {
            text = "CF"
            textSize = 17f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = glassBackground()
            elevation = dp(8).toFloat()
        }

        val params = WindowManager.LayoutParams(
            dp(58),
            dp(58),
            if (Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.END
        params.x = dp(12)
        params.y = dp(160)

        button.setOnClickListener {
            if (panelVisible) {
                hidePanel()
            } else {
                showPanel()
            }
        }

        button.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var moved = false

            override fun onTouch(
                view: View,
                event: MotionEvent
            ): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        moved = false
                        return false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (touchX - event.rawX).toInt()
                        val dy = (event.rawY - touchY).toInt()

                        if (kotlin.math.abs(dx) > dp(5) ||
                            kotlin.math.abs(dy) > dp(5)) {
                            moved = true
                        }

                        if (moved) {
                            params.x = max(0, startX + dx)
                            params.y = max(0, startY + dy)

                            try {
                                wm.updateViewLayout(button, params)
                            } catch (_: Exception) {}
                        }

                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (moved) return true
                    }
                }
                return false
            }
        })

        try {
            wm.addView(button, params)
            bubble = button
            bubbleParams = params
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun showPanel() {
        if (panelVisible) return

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = panelBackground()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "COUNTERFLAME"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        val close = TextView(this).apply {
            text = "  ✕  "
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setOnClickListener { hidePanel() }
        }

        header.addView(
            title,
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )

        header.addView(close)

        root.addView(header)

        val subtitle = TextView(this).apply {
            text = "VISUALS COMPANION"
            textSize = 11f
            setTextColor(0xFFFF8DDD.toInt())
            setPadding(0, 0, 0, dp(12))
        }

        root.addView(subtitle)

        val scroll = ScrollView(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(
            createSection("DISPLAY")
        )

        content.addView(
            createAction(
                "FPS Overlay",
                if (fpsEnabled) "ON" else "OFF"
            ) {
                toggleFps()
                refreshPanel()
            }
        )

        content.addView(
            createSection("APPEARANCE")
        )

        content.addView(
            createAction(
                "Change Theme",
                if (pinkTheme) "PINK" else "PURPLE"
            ) {
                pinkTheme = !pinkTheme
                prefs.edit()
                    .putBoolean("pink_theme", pinkTheme)
                    .apply()

                bubble?.background = glassBackground()
                refreshPanel()
            }
        )

        content.addView(
            createSection("SETTINGS")
        )

        val opacityLabel = TextView(this).apply {
            text = "Panel opacity: $opacity%"
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(dp(4), dp(10), dp(4), dp(5))
        }

        content.addView(opacityLabel)

        val seek = SeekBar(this).apply {
            max = 100
            progress = opacity

            setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        bar: SeekBar?,
                        value: Int,
                        fromUser: Boolean
                    ) {
                        opacity = max(30, value)
                        opacityLabel.text =
                            "Panel opacity: $opacity%"

                        panel?.alpha = opacity / 100f
                    }

                    override fun onStartTrackingTouch(
                        bar: SeekBar?
                    ) {}

                    override fun onStopTrackingTouch(
                        bar: SeekBar?
                    ) {
                        prefs.edit()
                            .putInt("opacity", opacity)
                            .apply()
                    }
                }
            )
        }

        content.addView(seek)

        content.addView(
            createSection("PROFILES")
        )

        content.addView(
            createAction("Save Current Profile", "SAVE") {
                prefs.edit()
                    .putBoolean("pink_theme", pinkTheme)
                    .putInt("opacity", opacity)
                    .apply()

                Toast.makeText(
                    this,
                    "Profile saved!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        content.addView(
            createAction("Reset Settings", "RESET") {
                pinkTheme = false
                opacity = 95

                prefs.edit()
                    .putBoolean("pink_theme", false)
                    .putInt("opacity", 95)
                    .apply()

                Toast.makeText(
                    this,
                    "Settings reset",
                    Toast.LENGTH_SHORT
                ).show()

                hidePanel()
                showPanel()
            }
        )

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        val params = WindowManager.LayoutParams(
            dp(320),
            dp(470),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER

        try {
            wm.addView(root, params)

            panel = root
            panelParams = params
            panelVisible = true

            root.alpha = opacity / 100f

        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Cannot display menu",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun createSection(name: String): TextView {
        return TextView(this).apply {
            text = name
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFFF82D6.toInt())
            setPadding(dp(4), dp(14), dp(4), dp(5))
        }
    }

    private fun createAction(
        name: String,
        state: String,
        action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = "$name                         $state"
            textSize = 14f
            gravity = Gravity.CENTER_VERTICAL
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(dp(12), 0, dp(12), 0)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(0x553D2B5B)
                setStroke(dp(1), 0x66FFFFFF)
            }

            layoutParams = LinearLayout.LayoutParams(
                -1,
                dp(46)
            ).apply {
                bottomMargin = dp(7)
            }

            setOnClickListener { action() }
        }
    }

    private fun toggleFps() {
        fpsEnabled = !fpsEnabled

        if (fpsEnabled) {
            createFpsView()
            handler.post(updateHz)
        } else {
            handler.removeCallbacks(updateHz)
            removeFpsView()
        }
    }

    private fun createFpsView() {
        if (fpsView != null) return

        val view = TextView(this).apply {
            text = "DISPLAY: -- Hz"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = glassBackground()
        }

        val params = WindowManager.LayoutParams(
            dp(175),
            dp(48),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = dp(80)

        try {
            wm.addView(view, params)
            fpsView = view
            fpsParams = params
        } catch (_: Exception) {
            fpsEnabled = false
        }
    }

    private fun removeFpsView() {
        try {
            fpsView?.let { wm.removeView(it) }
        } catch (_: Exception) {}

        fpsView = null
        fpsParams = null
    }

    private fun refreshPanel() {
        if (panelVisible) {
            hidePanel()
            showPanel()
        }
    }

    private fun hidePanel() {
        try {
            panel?.let { wm.removeView(it) }
        } catch (_: Exception) {}

        panel = null
        panelParams = null
        panelVisible = false
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateHz)

        hidePanel()
        removeFpsView()

        try {
            bubble?.let { wm.removeView(it) }
        } catch (_: Exception) {}

        bubble = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

