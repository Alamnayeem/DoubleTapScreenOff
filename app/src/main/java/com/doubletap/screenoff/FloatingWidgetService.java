package com.doubletap.screenoff;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class FloatingWidgetService extends Service {

    private static boolean isServiceRunning = false;
    
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private long lastTouchTime = 0;

    public static boolean isRunning() {
        return isServiceRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;

        NotificationHelper.createNotificationChannels(this);
        Notification notification = NotificationHelper.buildForegroundNotification(
                this,
                "Floating Widget Enabled",
                "Movable Double-Tap-To-Lock bar overlay active",
                NotificationHelper.WIDGET_CHANNEL_ID
        );
        startForeground(NotificationHelper.WIDGET_NOTIFICATION_ID, notification);

        // Setup Floating View Window Layout params
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        // Place on the middle-right side of the screen as default touch target
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 0;
        params.y = 400;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Inflate small pill widget layout programmatically or build custom ImageView
        floatingView = new ImageView(this);
        ImageView imgView = (ImageView) floatingView;
        
        // Premium tactile visual appearance
        imgView.setImageResource(android.R.drawable.ic_menu_crop);
        imgView.setBackgroundColor(0xBB4F46E5); // 73% transparent primary indigo
        imgView.setPadding(16, 24, 16, 24);
        imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        // Handle gestures and movement
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_UP:
                        long clickTime = System.currentTimeMillis();
                        if (clickTime - lastTouchTime < 300) {
                            // double-tap confirmed!
                            triggerLockAction();
                        } else {
                            lastTouchTime = clickTime;
                        }
                        
                        // Prevent accidental jumpiness
                        int diffX = (int) (event.getRawX() - initialTouchX);
                        int diffY = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(diffX) < 10 && Math.abs(diffY) < 10) {
                            v.performClick();
                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX - (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingView, params);
    }

    private void triggerLockAction() {
        SharedPreferences prefs = getSharedPreferences("doubletap_prefs", MODE_PRIVATE);
        boolean soundEnabled = prefs.getBoolean("enable_sound", true);
        boolean vibEnabled = prefs.getBoolean("enable_vibration", true);

        if (vibEnabled) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(70);
            }
        }

        if (soundEnabled) {
            try {
                ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_SYSTEM, 70);
                toneGen.startTone(ToneGenerator.TONE_PROP_PROMPT, 80);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Fire system lock intent
        Intent lockIntent = new Intent("com.doubletap.screenoff.ACTION_LOCK");
        sendBroadcast(lockIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        if (floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
}
