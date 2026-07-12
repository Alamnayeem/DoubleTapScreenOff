package com.doubletap.screenoff;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            
            SharedPreferences prefs = context.getSharedPreferences("doubletap_prefs", Context.MODE_PRIVATE);
            
            // Auto start Floating Overlay Widget if configured
            if (prefs.getBoolean("enable_floating_widget", false)) {
                Intent widgetIntent = new Intent(context, FloatingWidgetService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(widgetIntent);
                } else {
                    context.startService(widgetIntent);
                }
            }

            // Auto start Background Accelerometer Listener if configured
            if (prefs.getBoolean("enable_accelerometer", false)) {
                Intent sensorIntent = new Intent(context, DoubleTapSensorService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(sensorIntent);
                } else {
                    context.startService(sensorIntent);
                }
            }
        }
    }
}
