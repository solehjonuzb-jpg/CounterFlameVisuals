package com.counterflame.visuals

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import android.view.View

class MainActivity : Activity() {

    private lateinit var powerButton: TextView
    private var enabled = false
    private var waitingPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 5, 25)
        window.navigationBarColor = Color.rgb(12, 5, 25)
        createScreen()
    }

    private fun createScreen() {
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.rgb(12, 5, 25))

        val backgroundImage = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            val id = resources.getIdentifier(
                "app_background", "drawable", packageName
            )
            if (id != 0) setImageResource(id)
        }
        root.addView(backgroundImage,
            FrameLayout.LayoutParams(-1, -1))

        val shade = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xBB100020.toInt(),
                    0x99100025.toInt(),
                    0xEE08000F.toInt()
                )
            )
        }
        root.addView(shade,
            FrameLayout.LayoutParams(-1, -1))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 30, 28, 30)
        }

        val logo = TextView(this).apply {
            text = "CF"
            textSize = 76f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setShadowLayer(30f, 0f, 0f, 0xFF9D25FF.toInt())
        }

        val title = TextView(this).apply {
            text = "COUNTERFLAME"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }

        val subtitle = TextView(this).apply {
            text = "VISUALS"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(0xFFD58AFF.toInt())
        }

        powerButton = TextView(this).apply {
            text = "⏻"
            textSize = 48f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = powerBackground(false)
            elevation = 18f
            setOnClickListener { togglePower() }
        }

        val status = TextView(this).apply {
            tag = "status"
            text = "VISUALS: OFF"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }

        content.addView(logo)
        content.addView(title)
        content.addView(subtitle)

        val space = android.widget.Space(this)
        content.addView(space,
            LinearLayout.LayoutParams(1, 65))

        content.addView(powerButton,
            LinearLayout.LayoutParams(170, 170))

        val statusSpace = android.widget.Space(this)
        content.addView(statusSpace,
            LinearLayout.LayoutParams(1, 20))

        content.addView(status)

        root.addView(content,
            FrameLayout.LayoutParams(-1, -1))

        setContentView(root)
        updatePower(false)
    }

    private fun powerBackground(on: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (on) 0xFF8500FF.toInt()
                     else 0xFF1B1030.toInt())
            setStroke(4, if (on) 0xFFE4B0FF.toInt()
                         else 0xFF8B35D8.toInt())
        }
    }

    private fun togglePower() {
        if (enabled) {
            stopService(Intent(this, OverlayService::class.java))
            updatePower(false)
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            waitingPermission = true
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        startService()
    }

    private fun startService() {
        val intent = Intent(this, OverlayService::class.java)
        startForegroundService(intent)
        updatePower(true)
    }

    private fun updatePower(on: Boolean) {
        enabled = on
        powerButton.background = powerBackground(on)
        powerButton.setTextColor(Color.WHITE)

        val status = findViewById<TextView>(
            android.R.id.content
        )
        val label = findStatusView()
        label?.text = if (on) "VISUALS: ON" else "VISUALS: OFF"
    }

    private fun findStatusView(): TextView? {
        fun search(view: View): TextView? {
            if (view is TextView && view.tag == "status") return view
            if (view is android.view.ViewGroup) {
                for (i in 0 until view.childCount) {
                    val result = search(view.getChildAt(i))
                    if (result != null) return result
                }
            }
            return null
        }
        return search(window.decorView)
    }

    override fun onResume() {
        super.onResume()
        if (waitingPermission) {
            waitingPermission = false
            if (Settings.canDrawOverlays(this)) {
                startService()
            }
        }
    }
}
