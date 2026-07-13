package com.doubletap.screenoff

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.GestureDetector
import android.view.MotionEvent

class DoubleTapWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return DoubleTapEngine()
    }

    inner class DoubleTapEngine : Engine(), android.content.SharedPreferences.OnSharedPreferenceChangeListener {
        private val handler = Handler(Looper.getMainLooper())
        private val paint = Paint().apply {
            color = Color.parseColor("#0a0a1a")
            style = Paint.Style.FILL
        }
        private val prefs by lazy { applicationContext.getSharedPreferences("DoubleTapLockPrefs", Context.MODE_PRIVATE) }

        private val gestureDetector = GestureDetector(applicationContext, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                triggerScreenLock()
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return super.onSingleTapConfirmed(e)
            }
        })

        override fun onCreate(surfaceHolder: android.view.SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: android.content.SharedPreferences?, key: String?) {
            if (key == "pref_wallpaper_type" || key == "pref_wallpaper_color" || key == "pref_wallpaper_image_uri") {
                drawWallpaper()
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            gestureDetector.onTouchEvent(event)
            super.onTouchEvent(event)
        }

        override fun onCommand(action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean): Bundle? {
            if (action == WallpaperManager.COMMAND_TAP) {
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
            // Trigger haptic vibration + sound feedback
            LockFeedbackHelper.triggerFeedback(applicationContext)

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
                    val wallpaperType = prefs.getString("pref_wallpaper_type", "gradient") ?: "gradient"
                    val colorStr = prefs.getString("pref_wallpaper_color", "#090d22") ?: "#090d22"
                    val bgColor = try { Color.parseColor(colorStr) } catch(e: Exception) { Color.parseColor("#090d22") }

                    // Fill canvas with color background
                    canvas.drawColor(bgColor)
                    
                    if (wallpaperType == "gradient") {
                        // Drawing a beautiful dynamic structural line accent
                        val paintLine = Paint().apply {
                            color = Color.parseColor("#1a1f4c")
                            strokeWidth = 6f
                        }
                        canvas.drawLine(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paintLine)
                    } else if (wallpaperType == "image") {
                        val imageUriStr = prefs.getString("pref_wallpaper_image_uri", "") ?: ""
                        if (imageUriStr.isNotEmpty()) {
                            try {
                                val imageUri = Uri.parse(imageUriStr)
                                val inputStream = contentResolver.openInputStream(imageUri)
                                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                                inputStream?.close()
                                
                                if (bitmap != null) {
                                    val canvasWidth = canvas.width.toFloat()
                                    val canvasHeight = canvas.height.toFloat()
                                    val bitmapWidth = bitmap.width.toFloat()
                                    val bitmapHeight = bitmap.height.toFloat()

                                    // Scale center-crop implementation
                                    val scale = Math.max(canvasWidth / bitmapWidth, canvasHeight / bitmapHeight)
                                    val scaledWidth = bitmapWidth * scale
                                    val scaledHeight = bitmapHeight * scale
                                    val left = (canvasWidth - scaledWidth) / 2f
                                    val top = (canvasHeight - scaledHeight) / 2f

                                    val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                                    val dstRect = android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight)
                                    
                                    val paintBitmap = Paint().apply {
                                        isAntiAlias = true
                                        isFilterBitmap = true
                                    }
                                    canvas.drawBitmap(bitmap, srcRect, dstRect, paintBitmap)
                                    bitmap.recycle()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                // Fail-safe text message for custom wallpaper
                                val paintText = Paint().apply {
                                    color = Color.parseColor("#40FFFFFF")
                                    textSize = 45f
                                    textAlign = Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                                canvas.drawText("Double Tap to Lock Screen", canvas.width / 2f, canvas.height / 2f, paintText)
                            }
                        }
                    }
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}

