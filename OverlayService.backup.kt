package com.counterflame.visuals

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import android.graphics.drawable.GradientDrawable

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubble: View? = null
    private var menu: View? = null
    private val channelId = "cf_visuals"

    override fun onCreate() {
        super.onCreate()

        createNotification()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        showBubble()
    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId,
                "CounterFlame Visuals",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }.setContentTitle("CounterFlame Visuals")
            .setContentText("Floating menu active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()

        startForeground(1, notification)
    }

    private fun params(width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            if (Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 250
        }
    }

    private fun background(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            setStroke(2, Color.CYAN)
        }
    }

    private fun showBubble() {
        val button = TextView(this).apply {
            text = "CF"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = background(Color.rgb(20, 20, 35), 60f)
            elevation = 12f
        }

        val lp = params(58, 58)
        var startX = 0
        var startY = 0
        var downX = 0f
        var downY = 0f

        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x
                    startY = lp.y
                    downX = event.rawX
                    downY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (event.rawX - downX).toInt()
                    lp.y = startY + (event.rawY - downY).toInt()
                    windowManager.updateViewLayout(button, lp)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (kotlin.math.abs(event.rawX - downX) < 10 &&
                        kotlin.math.abs(event.rawY - downY) < 10) {
                        toggleMenu()
                    }
                    true
                }
                else -> true
            }
        }

        bubble = button
        windowManager.addView(button, lp)
    }

    private fun toggleMenu() {
        if (menu != null) {
            windowManager.removeView(menu)
            menu = null
        } else {
            showMenu()
        }
    }

    private fun showMenu() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 18, 22, 18)
            background = background(Color.rgb(13, 15, 28), 24f)
        }

        val title = TextView(this).apply {
            text = "COUNTERFLAME"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "VISUALS COMPANION"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        panel.addView(title)
        panel.addView(subtitle)

        val features = listOf(
            "RGB Interface",
            "Visuals Panel",
            "Dark Theme",
            "Floating Button"
        )

        features.forEach { name ->
            val item = TextView(this).apply {
                text = "  $name"
                textSize = 15f
                setTextColor(Color.WHITE)
                setPadding(12, 14, 12, 14)
                background = background(Color.rgb(30, 32, 48), 12f)
            }
            val space = LinearLayout.LayoutParams(-1, -2)
            space.topMargin = 10
            panel.addView(item, space)
        }

        val close = Button(this).apply {
            text = "CLOSE"
            setOnClickListener { toggleMenu() }
        }
        panel.addView(close)

        menu = panel
        windowManager.addView(panel, params(280, -2))
    }

    override fun onDestroy() {
        super.onDestroy()
        bubble?.let { windowManager.removeView(it) }
        menu?.let { windowManager.removeView(it) }
        bubble = null
        menu = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
