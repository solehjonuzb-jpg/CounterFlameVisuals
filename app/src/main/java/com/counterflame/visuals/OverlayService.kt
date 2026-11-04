package com.counterflame.visuals

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var menu: View? = null

    private val prefs by lazy {
        getSharedPreferences("cf_visuals", MODE_PRIVATE)
    }

    private val purple = Color.rgb(171, 66, 255)
    private val pink = Color.rgb(255, 105, 210)
    private val white = Color.WHITE

    private val sections = linkedMapOf(
        "ESP" to listOf(
            "Player ESP", "Box ESP", "Name ESP", "Health ESP",
            "Distance ESP", "Skeleton ESP", "Head ESP", "Weapon ESP",
            "Item ESP", "Bomb ESP", "Grenade ESP", "Grenade Trail"
        ),
        "VISUALS" to listOf(
            "RGB Glow", "Neon Outline", "Glass Effect", "Custom Colors",
            "Crosshair Style", "Hit Effect", "Damage Indicator", "Tracer Effect"
        ),
        "RADAR" to listOf(
            "Radar Panel", "Map Display", "Player Markers",
            "Bomb Marker", "Grenade Marker", "Radar Zoom"
        ),
        "AIM" to listOf(
            "Custom Crosshair", "Crosshair Size", "Crosshair Color",
            "Recoil Display", "Spread Display", "Aim Training"
        ),
        "GRENADE" to listOf(
            "Grenade Information", "Trajectory Preview",
            "Landing Marker", "Smoke Timer", "Flash Indicator"
        ),
        "MISC" to listOf(
            "FPS Monitor", "Ping Monitor", "Network Status",
            "Performance Monitor", "UI Animation", "Quick Menu"
        )
    )

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()

        val notification = Notification.Builder(
            this,
            "cf_visuals"
        )
            .setContentTitle("CounterFlame Visuals")
            .setContentText("Floating interface is active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        showBubble()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "cf_visuals",
                "CounterFlame Visuals",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun glass(
        radius: Int = 18,
        active: Boolean = false
    ) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        if (active)
            intArrayOf(0xEE8526C9.toInt(), 0xEE351047.toInt())
        else
            intArrayOf(0xE92B1740.toInt(), 0xE9110D20.toInt())
    ).apply {
        cornerRadius = dp(radius).toFloat()
        setStroke(
            dp(1),
            if (active) pink else 0xAAAE67E8.toInt()
        )
    }

    private fun params(width: Int, height: Int, y: Int = 100) =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            this.y = dp(y)
        }

    private fun showBubble() {
        if (bubble != null) return

        val button = TextView(this).apply {
            text = "♛  CF  VISUALS   ☰"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(white)
            background = glass(40, true)
            setPadding(dp(17), dp(11), dp(17), dp(11))
            elevation = dp(12).toFloat()
        }

        val lp = params(dp(174), dp(49), 75)
        lp.gravity = Gravity.TOP or Gravity.END
        lp.x = dp(12)

        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false

        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = lp.x
                    startY = lp.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (kotlin.math.abs(dx) > dp(5) ||
                        kotlin.math.abs(dy) > dp(5)) moved = true
                    lp.x = (startX - dx).toInt()
                    lp.y = (startY + dy).toInt()
                    wm.updateViewLayout(button, lp)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) toggleMenu()
                    true
                }
                else -> true
            }
        }

        bubble = button
        wm.addView(button, lp)
    }

    private fun toggleMenu() {
        if (menu == null) showMenu() else hideMenu()
    }

    private fun hideMenu() {
        menu?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        menu = null
    }

    private fun showMenu() {
        if (menu != null) return

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = glass(24)
            elevation = dp(20).toFloat()
        }

        // HEADER
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = TextView(this).apply {
            text = "♛ CF"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(pink)
            setShadowLayer(dp(12).toFloat(), 0f, 0f, purple)
        }

        val heading = TextView(this).apply {
            text = "COUNTERFLAME\nVISUALS  •  v1.0"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            setPadding(dp(10), 0, 0, 0)
        }

        val close = TextView(this).apply {
            text = "×"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(pink)
            setOnClickListener { hideMenu() }
        }

        header.addView(logo)
        header.addView(heading, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(close, LinearLayout.LayoutParams(dp(40), dp(48)))
        panel.addView(header)

        // NAVIGATION
        val navigation = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }

        val navRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            isVerticalScrollBarEnabled = false
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val sectionViews = mutableMapOf<String, View>()

        fun sectionTitle(name: String): TextView {
            return TextView(this).apply {
                text = "✦  $name"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(pink)
                setPadding(dp(3), dp(13), dp(3), dp(8))
            }
        }

        fun addSection(name: String, features: List<String>) {
            val title = sectionTitle(name)
            sectionViews[name] = title
            content.addView(title)

            val grid = GridLayout(this).apply {
                columnCount = 2
            }

            features.forEach { feature ->
                val active = prefs.getBoolean(feature, false)

                val tile = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(9), dp(8), dp(7), dp(8))
                    background = glass(13, active)
                    isClickable = true
                    isFocusable = true
                }

                val label = TextView(this).apply {
                    text = feature
                    textSize = 10f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(white)
                }

                val switch = Switch(this).apply {
                    isChecked = active
                    scaleX = 0.72f
                    scaleY = 0.72f
                    buttonTintList = android.content.res.ColorStateList.valueOf(pink)
                    thumbTintList = android.content.res.ColorStateList.valueOf(pink)
                    trackTintList = android.content.res.ColorStateList.valueOf(purple)
                    setOnCheckedChangeListener { _, checked ->
                        prefs.edit().putBoolean(feature, checked).apply()
                        tile.background = glass(13, checked)
                    }
                }

                tile.addView(
                    label,
                    LinearLayout.LayoutParams(0, dp(42), 1f)
                )
                tile.addView(switch)

                tile.setOnClickListener {
                    switch.isChecked = !switch.isChecked
                }

                val cell = GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(58)
                    columnSpec = GridLayout.spec(
                        GridLayout.UNDEFINED, 1f
                    )
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                }

                grid.addView(tile, cell)
            }

            content.addView(grid)
        }

        sections.forEach { (name, features) ->
            val nav = TextView(this).apply {
                text = name
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(white)
                gravity = Gravity.CENTER
                setPadding(dp(11), dp(10), dp(11), dp(10))
                background = glass(12)
                setOnClickListener {
                    sectionViews[name]?.let {
                        scroll.smoothScrollTo(0, it.top)
                    }
                }
            }
            val navParams = LinearLayout.LayoutParams(-2, dp(38))
            navParams.setMargins(dp(3), dp(5), dp(3), dp(5))
            navRow.addView(nav, navParams)
        }

        navigation.addView(navRow)
        panel.addView(navigation)

        sections.forEach { (name, features) ->
            addSection(name, features)
        }

        // PROFILES
        content.addView(sectionTitle("PROFILES"))
        val profileRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        listOf("DEFAULT", "COMPETITIVE", "PRACTICE").forEach { profile ->
            val item = TextView(this).apply {
                text = profile
                textSize = 9f
                gravity = Gravity.CENTER
                setTextColor(white)
                background = glass(12)
                setPadding(dp(5), dp(13), dp(5), dp(13))
                setOnClickListener {
                    prefs.edit().putString("profile", profile).apply()
                    Toast.makeText(
                        this@OverlayService,
                        "$profile selected",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            val itemParams = LinearLayout.LayoutParams(0, dp(43), 1f)
            itemParams.setMargins(dp(3), 0, dp(3), 0)
            profileRow.addView(item, itemParams)
        }
        content.addView(profileRow)

        // THEME
        content.addView(sectionTitle("THEME"))
        val themeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        listOf(
            "PURPLE", "PINK", "CYAN", "RED", "AMOLED"
        ).forEach { theme ->
            val item = TextView(this).apply {
                text = theme
                textSize = 9f
                gravity = Gravity.CENTER
                setTextColor(white)
                background = glass(12, theme == "PURPLE")
                setPadding(dp(5), dp(12), dp(5), dp(12))
                setOnClickListener {
                    prefs.edit().putString("theme", theme).apply()
                    Toast.makeText(
                        this@OverlayService,
                        "$theme theme selected",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            val itemParams = LinearLayout.LayoutParams(0, dp(42), 1f)
            itemParams.setMargins(dp(2), 0, dp(2), 0)
            themeRow.addView(item, itemParams)
        }
        content.addView(themeRow)

        // SETTINGS
        content.addView(sectionTitle("SETTINGS"))

        listOf("UI Animation", "Haptic Feedback").forEach { setting ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = glass(12)
                setPadding(dp(10), dp(4), dp(10), dp(4))
            }

            val label = TextView(this).apply {
                text = setting
                textSize = 11f
                setTextColor(white)
            }

            val toggle = Switch(this).apply {
                isChecked = prefs.getBoolean(setting, true)
                setOnCheckedChangeListener { _, checked ->
                    prefs.edit().putBoolean(setting, checked).apply()
                }
            }

            row.addView(label, LinearLayout.LayoutParams(0, dp(42), 1f))
            row.addView(toggle)
            val rowParams = LinearLayout.LayoutParams(-1, dp(48))
            rowParams.setMargins(0, dp(3), 0, dp(3))
            content.addView(row, rowParams)
        }

        scroll.addView(content)
        panel.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        val footer = TextView(this).apply {
            text = "✦  COUNTERFLAME VISUALS  ✦"
            textSize = 9f
            gravity = Gravity.CENTER
            setTextColor(0xFFDDAAFF.toInt())
            setPadding(0, dp(10), 0, dp(3))
        }
        panel.addView(footer)

        val panelWidth = screenWidth - dp(20)
        val panelHeight = (screenHeight * 0.78f).toInt()

        menu = panel
        wm.addView(
            panel,
            params(panelWidth, panelHeight, dp(135))
        )
    }

    override fun onDestroy() {
        hideMenu()
        bubble?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
