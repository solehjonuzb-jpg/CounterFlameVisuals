
package com.counterflame.visuals

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var powerButton: TextView
    private lateinit var statusText: TextView

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
        root.setBackgroundColor(Color.rgb(18, 8, 35))

        val backgroundImage = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP

            val imageId = resources.getIdentifier(
                "app_background",
                "drawable",
                packageName
            )

            if (imageId != 0) {
                setImageResource(imageId)
            }
        }

        root.addView(
            backgroundImage,
            FrameLayout.LayoutParams(-1, -1)
        )

        val shade = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    0xBB0C0519.toInt(),
                    0x990C0519.toInt(),
                    0xEE0C0519.toInt()
                )
            )
        }

        root.addView(
            shade,
            FrameLayout.LayoutParams(-1, -1)
        )

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(30), dp(28), dp(30))
        }

        val logo = TextView(this).apply {
            text = "CF"
            textSize = 76f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setShadowLayer(30f, 0f, 0f, 0xFFB82CFF.toInt())
        }

        val title = TextView(this).apply {
            text = "COUNTERFLAME"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }

        val subtitle = TextView(this).apply {
            text = "VISUALS COMPANION"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(0xFFFF85D5.toInt())
        }

        powerButton = TextView(this).apply {
            text = "START"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = powerBackground(false)
            elevation = dp(8).toFloat()

            setOnClickListener {
                togglePower()
            }
        }

        statusText = TextView(this).apply {
            text = "VISUALS: OFF"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }

        content.addView(
            logo,
            LinearLayout.LayoutParams(-1, dp(100))
        )

        content.addView(
            title,
            LinearLayout.LayoutParams(-1, dp(50))
        )

        content.addView(
            subtitle,
            LinearLayout.LayoutParams(-1, dp(40))
        )

        val space = View(this)

        content.addView(
            space,
            LinearLayout.LayoutParams(1, dp(65))
        )

        content.addView(
            powerButton,
            LinearLayout.LayoutParams(-1, dp(58))
        )

        val statusSpace = View(this)

        content.addView(
            statusSpace,
            LinearLayout.LayoutParams(1, dp(20))
        )

        content.addView(
            statusText,
            LinearLayout.LayoutParams(-1, dp(40))
        )

        root.addView(
            content,
            FrameLayout.LayoutParams(-1, -1)
        )

        setContentView(root)

        updatePower(false)
    }

    private fun powerBackground(on: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE

            orientation = GradientDrawable.Orientation.LEFT_RIGHT

            colors = if (on) {
                intArrayOf(
                    0xFFED3AAB.toInt(),
                    0xFF8A36FF.toInt()
                )
            } else {
                intArrayOf(
                    0xFF8A36FF.toInt(),
                    0xFFED3AAB.toInt()
                )
            }

            cornerRadius = dp(18).toFloat()
        }
    }

    private fun togglePower() {

        if (enabled) {
            stopService(
                Intent(this, OverlayService::class.java)
            )

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

            Toast.makeText(
                this,
                "Allow display over other apps",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        startOverlayService()
    }

    private fun startOverlayService() {

        try {
            val intent = Intent(
                this,
                OverlayService::class.java
            )

            startForegroundService(intent)

            updatePower(true)

        } catch (e: Exception) {

            updatePower(false)

            Toast.makeText(
                this,
                "Service error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updatePower(on: Boolean) {

        enabled = on

        powerButton.background = powerBackground(on)

        powerButton.text = if (on) {
            "STOP"
        } else {
            "START"
        }

        statusText.text = if (on) {
            "VISUALS: ON"
        } else {
            "VISUALS: OFF"
        }
    }

    override fun onResume() {
        super.onResume()

        if (waitingPermission) {

            waitingPermission = false

            if (Settings.canDrawOverlays(this)) {
                startOverlayService()
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

