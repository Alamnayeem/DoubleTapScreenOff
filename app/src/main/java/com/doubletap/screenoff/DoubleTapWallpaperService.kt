package com.doubletap.screenoff

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.wallpaper.WallpaperService
import android.view.GestureDetector
import android.view.MotionEvent

class DoubleTapWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return DoubleTapEngine()
    }

    inner class DoubleTapEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val paint = Paint().apply {
            color = Color.parseColor("#0a0a1a") // Elegant space color matching web styling
            style = Paint.Style.FILL
        }

        private var vibrator: Vibrator? = null

        private val gestureDetector = GestureDetector(applicationContext, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                triggerScreenLock()
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // Ignore single taps
                return super.onSingleTapConfirmed(e)
            }
        })

        override fun onCreate(surfaceHolder: android.view.SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        override fun onTouchEvent(event: MotionEvent) {
            gestureDetector.onTouchEvent(event)
            super.onTouchEvent(event)
        }

        // Support standard home screen command taps forwarded by some launchers
        override fun onCommand(action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean): Bundle? {
            if (action == WallpaperManager.COMMAND_TAP) {
                // Some older or custom launchers trigger tap commands directly.
                // We handle double-tap timings on COMMAND_TAP as a fallback.
                handleCommandTap()
            }
            return super.onCommand(action, x, y, z, extras, resultRequested)
        }

        private var lastTapTime: Long = 0
        private fun handleCommandTap() {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime < 350) {
                triggerScreenLock()
            }
            lastTapTime = currentTime
        }

        private fun triggerScreenLock() {
            // Trigger haptic vibration feedback
            vibrator?.let {
                if (it.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(75)
                    }
                }
            }

            // Lock screen using accessibility instance
            AccessibilityLockService.instance?.lockScreenDevice()
                ?: AdminReceiver.lockWithAdmin(applicationContext)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                drawWallpaper()
            }
        }

        override fun onSurfaceChanged(holder: android.view.SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawWallpaper()
        }

        private fun drawWallpaper() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.parseColor("#090d22")) // Dark Frosted elegant canvas background
                    
                    // Optional subtle dynamic geometric gradient line for premium visual identity
                    val paintLine = Paint().apply {
                        color = Color.parseColor("#1a1f4c")
                        strokeWidth = 4f
                    }
                    canvas.drawLine(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paintLine)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}
