package com.doubletap.screenoff;

import android.accessibilityservice.AccessibilityService;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class DoubleTapAccessibilityService extends AccessibilityService {

    public static final String ACTION_LOCK = "com.doubletap.screenoff.ACTION_LOCK";
    
    private BroadcastReceiver lockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_LOCK.equals(intent.getAction())) {
                boolean locked = false;
                
                // Attempt Accessibility Lock (Available in Android 9+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    locked = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
                }
                
                // Fallback to Device Admin Lock if accessibility locking fails or unsupported
                if (!locked) {
                    DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                    ComponentName adminName = new ComponentName(context, ScreenLockAdminReceiver.class);
                    if (dpm != null && dpm.isAdminActive(adminName)) {
                        dpm.lockNow();
                        locked = true;
                    }
                }
                
                if (locked) {
                    Toast.makeText(context, R.string.device_locked, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Lock failed. Please configure Accessibility Service or Device Admin.", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No interaction reading is required. This service is solely used for secure GLOBAL_ACTION_LOCK_SCREEN trigger.
    }

    @Override
    public void onInterrupt() {
        // Handle interruptions safely
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        IntentFilter filter = new IntentFilter(ACTION_LOCK);
        registerReceiver(lockReceiver, filter);
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(lockReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
