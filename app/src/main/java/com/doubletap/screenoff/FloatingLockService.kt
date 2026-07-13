package com.doubletap.screenoff

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

class FloatingLockService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        setupFloatingView()
    }

    private fun setupFloatingView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Create programmatic floating bubble layout matching modern frosted-glass visual theme
        val context = this
        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        // Programmatic elegant circular container with premium glassmorphic border gradient
        val container = FrameLayout(context).apply {
            setPadding(12, 12, 12, 12)
        }

        val innerCircle = FrameLayout(context).apply {
            val gd = GradientDrawable().apply {
                setColor(Color.parseColor("#4f46e5")) // Indigo 600
                cornerRadius = 100f
                setStroke(2, Color.parseColor("#ffffff")) // White border accent
            }
            background = gd
            minimumWidth = 140
            minimumHeight = 140
        }

        val textIndicator = TextView(context).apply {
            text = "TAP\nLOCK"
            setTextColor(Color.WHITE)
            textSize = 10f
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
        }

        val textParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        innerCircle.addView(textIndicator, textParams)
        container.addView(innerCircle)
        floatingView = container

        // Dragging and Double Tap gestures implementation
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var lastTapTime: Long = 0
            private var isMoved = false

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isMoved = true
                        }

                        params.x = initialX + deltaX
                        params.y = initialY + deltaY
                        windowManager?.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoved) {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastTapTime < 300) {
                                triggerLock()
                            }
                            lastTapTime = currentTime
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager?.addView(floatingView, params)
    }

    private fun triggerLock() {
        LockFeedbackHelper.triggerFeedback(applicationContext)
        AccessibilityLockService.instance?.lockScreenDevice()
            ?: AdminReceiver.lockWithAdmin(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let {
            windowManager?.removeView(it)
        }
    }
}
