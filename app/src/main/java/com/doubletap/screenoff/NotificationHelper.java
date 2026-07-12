package com.doubletap.screenoff;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    public static final String SENSOR_CHANNEL_ID = "com.doubletap.screenoff.sensor_channel";
    public static final String WIDGET_CHANNEL_ID = "com.doubletap.screenoff.widget_channel";
    
    public static final int SENSOR_NOTIFICATION_ID = 2026;
    public static final int WIDGET_NOTIFICATION_ID = 2027;

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel sensorChannel = new NotificationChannel(
                    SENSOR_CHANNEL_ID,
                    "Sensor Double Tap Detector",
                    NotificationManager.IMPORTANCE_LOW
            );
            sensorChannel.setDescription("Keeps the high-efficiency accelerometer background service running to process screen locks.");
            sensorChannel.setShowBadge(false);

            NotificationChannel widgetChannel = new NotificationChannel(
                    WIDGET_CHANNEL_ID,
                    "Floating Touch Bar Overlay",
                    NotificationManager.IMPORTANCE_LOW
            );
            widgetChannel.setDescription("Keeps the floating bubble/touch bar overlay active over other apps.");
            widgetChannel.setShowBadge(false);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(sensorChannel);
                manager.createNotificationChannel(widgetChannel);
            }
        }
    }

    public static Notification buildForegroundNotification(Context context, String title, String body, String channelId) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .build();
    }
}
