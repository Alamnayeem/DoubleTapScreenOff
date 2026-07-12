package com.doubletap.screenoff;

import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Bundle;
import android.service.wallpaper.WallpaperService;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class DoubleTapWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new DoubleTapWallpaperEngine();
    }

    private class DoubleTapWallpaperEngine extends Engine {
        private final GestureDetector gestureDetector;

        DoubleTapWallpaperEngine() {
            gestureDetector = new GestureDetector(DoubleTapWallpaperService.this, 
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        triggerLock();
                        return true;
                    }
                });
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                drawWallpaper();
            }
        }

        private void drawWallpaper() {
            Canvas canvas = lockCanvas();
            if (canvas != null) {
                try {
                    java.io.File file = new java.io.File(getFilesDir(), "custom_wallpaper.png");
                    if (file.exists()) {
                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
                        if (bitmap != null) {
                            int canvasWidth = canvas.getWidth();
                            int canvasHeight = canvas.getHeight();
                            int bitmapWidth = bitmap.getWidth();
                            int bitmapHeight = bitmap.getHeight();

                            float scale;
                            float dx = 0;
                            float dy = 0;

                            if (bitmapWidth * canvasHeight > canvasWidth * bitmapHeight) {
                                scale = (float) canvasHeight / (float) bitmapHeight;
                                dx = (canvasWidth - bitmapWidth * scale) * 0.5f;
                            } else {
                                scale = (float) canvasWidth / (float) bitmapWidth;
                                dy = (canvasHeight - bitmapHeight * scale) * 0.5f;
                            }

                            android.graphics.Matrix matrix = new android.graphics.Matrix();
                            matrix.setScale(scale, scale);
                            matrix.postTranslate(dx, dy);

                            canvas.drawColor(android.graphics.Color.BLACK); // Clear background
                            canvas.drawBitmap(bitmap, matrix, null);
                            bitmap.recycle();
                            return;
                        }
                    }

                    // Draw a solid subtle background color matching the Slate theme background (0xFF0F172A)
                    canvas.drawColor(0xFF0F172A); 
                } catch (Exception e) {
                    canvas.drawColor(0xFF0F172A);
                } finally {
                    unlockAndPostCanvas(canvas);
                }
            }
        }

        private void triggerLock() {
            Intent lockIntent = new Intent("com.doubletap.screenoff.ACTION_LOCK");
            sendBroadcast(lockIntent);
        }
    }
}
