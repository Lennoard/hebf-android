package com.androidvip.hebf.services.gb

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import com.androidvip.hebf.R
import com.androidvip.hebf.utils.Utils

class GameBoosterOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var rootLayout: LinearLayout? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val windowParams = createWindowParams()
        createRootLayout(windowParams)
    }

    override fun onDestroy() {
        runCatching {
            if (windowManager != null && rootLayout != null) {
                windowManager?.removeView(rootLayout)
            }
        }
        super.onDestroy()
    }

    private fun createWindowParams(): WindowManager.LayoutParams {
        val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                Utils.dpToPx(this, 56),
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            windowAnimations = R.style.WindowAnimation
        }
    }

    private fun createRootLayout(windowParams: WindowManager.LayoutParams) {
        rootLayout = LinearLayout(this).apply {
            layoutParams = WindowManager.LayoutParams()
        }

        val dp24 = Utils.dpToPx(this, 24)
        val dp4 = Utils.dpToPx(this, 4)

        rootLayout?.apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.overlay_background)
            setPadding(dp24, dp4, dp24, dp4)
        }

        populateRootLayout(windowParams)
    }

    private fun populateRootLayout(windowParams: WindowManager.LayoutParams) {
        val dp12 = Utils.dpToPx(this,  12)
        val dp40 = Utils.dpToPx(this, 40)

        val closeButton = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp40, dp40).apply {
                setMargins(dp12, dp12, dp12, dp40)
            }

            setBackgroundResource(R.drawable.close_ad_button)
            setPadding(8, 8, 8, 8)
            setImageResource(R.drawable.ic_close)
            setOnClickListener { stopSelf() }
        }

        val testText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(Color.WHITE)
        }

        rootLayout?.apply {
            addView(closeButton)
            addView(testText)
        }

        // Add layout raiz à janela
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Utils.canDrawOverlays(this)) {
                windowManager?.addView(rootLayout, windowParams)
            }
        } else {
            windowManager?.addView(rootLayout, windowParams)
        }
    }
}