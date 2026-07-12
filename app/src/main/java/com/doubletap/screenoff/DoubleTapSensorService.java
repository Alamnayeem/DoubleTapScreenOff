package com.doubletap.screenoff;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class DoubleTapSensorService extends Service implements SensorEventListener {

    private static boolean isServiceRunning = false;
    
    private SensorManager sensorManager;
    private Sensor accelerometer;
    
    // Tap detection thresholds
    private static final float TAP_THRESHOLD = 14.0f; // Rapid acceleration change
    private static final int TAP_TIME_LIMIT = 350; // Maximum time gap between double taps in ms
    
    private long lastTapTime = 0;
    private float lastX, lastY, lastZ;
    private boolean isFirstSample = true;
    private PowerManager.WakeLock wakeLock;

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
                "DoubleTap Sensor Core Enabled",
                "Listening for physical double-tap motion on phone body",
                NotificationHelper.SENSOR_CHANNEL_ID
        );
        startForeground(NotificationHelper.SENSOR_NOTIFICATION_ID, notification);

        // Keep CPU awake slightly for accelerometer polling if needed
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DoubleTapScreenOff::SensorWakeLock");
            wakeLock.acquire(10*60*1000L /*10 minutes max fallback, dynamically refreshed or persistent*/);
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if (isFirstSample) {
            lastX = x;
            lastY = y;
            lastZ = z;
            isFirstSample = false;
            return;
        }

        // High-pass filter acceleration delta calculation
        float deltaX = Math.abs(x - lastX);
        float deltaY = Math.abs(y - lastY);
        float deltaZ = Math.abs(z - lastZ);

        lastX = x;
        lastY = y;
        lastZ = z;

        float totalDelta = deltaX + deltaY + deltaZ;

        // Verify if motion exceeds threshold for body tap
        if (totalDelta > TAP_THRESHOLD) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTapTime < TAP_TIME_LIMIT && currentTime - lastTapTime > 80) {
                // Successful physical double-tap!
                triggerLockAction();
                lastTapTime = 0; // Reset
            } else {
                lastTapTime = currentTime;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    private void triggerLockAction() {
        // Play Feedback and send secure lock broadcast
        boolean soundEnabled = getSharedPreferences("doubletap_prefs", MODE_PRIVATE).getBoolean("enable_sound", true);
        boolean vibEnabled = getSharedPreferences("doubletap_prefs", MODE_PRIVATE).getBoolean("enable_vibration", true);

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

        // Lock screen broadcast
        Intent lockIntent = new Intent("com.doubletap.screenoff.ACTION_LOCK");
        sendBroadcast(lockIntent);
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }
}
