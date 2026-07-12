package com.doubletap.screenoff;

import android.accessibilityservice.AccessibilityService;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

public class MainActivity extends AppCompatActivity {

    private TextView tvAccessibilityStatus;
    private TextView tvAdminStatus;
    private TextView tvWidgetStatus;
    private TextView tvSensorStatus;

    private MaterialSwitch switchFloatingWidget;
    private MaterialSwitch switchAccelerometer;
    private MaterialSwitch switchSound;
    private MaterialSwitch switchVibration;

    private Button btnToggleAccessibility;
    private Button btnToggleAdmin;
    private Button btnIgnoreBattery;
    private Button btnChooseWallpaper;
    private Button btnSetLiveWallpaper;
    private Button btnResetWallpaper;
    private MaterialCardView btnTestPad;
    private TextView tvAbout;
    private TextView tvPrivacy;

    private SharedPreferences prefs;
    private static final int PICK_IMAGE_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("doubletap_prefs", MODE_PRIVATE);

        // Bind Views
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus);
        tvAdminStatus = findViewById(R.id.tvAdminStatus);
        tvWidgetStatus = findViewById(R.id.tvWidgetStatus);
        tvSensorStatus = findViewById(R.id.tvSensorStatus);

        switchFloatingWidget = findViewById(R.id.switchFloatingWidget);
        switchAccelerometer = findViewById(R.id.switchAccelerometer);
        switchSound = findViewById(R.id.switchSound);
        switchVibration = findViewById(R.id.switchVibration);

        btnToggleAccessibility = findViewById(R.id.btnToggleAccessibility);
        btnToggleAdmin = findViewById(R.id.btnToggleAdmin);
        btnIgnoreBattery = findViewById(R.id.btnIgnoreBattery);
        btnChooseWallpaper = findViewById(R.id.btnChooseWallpaper);
        btnSetLiveWallpaper = findViewById(R.id.btnSetLiveWallpaper);
        btnResetWallpaper = findViewById(R.id.btnResetWallpaper);
        btnTestPad = findViewById(R.id.btnTestPad);
        tvAbout = findViewById(R.id.tvAbout);
        tvPrivacy = findViewById(R.id.tvPrivacy);

        // Load saved preferences
        switchFloatingWidget.setChecked(prefs.getBoolean("enable_floating_widget", false));
        switchAccelerometer.setChecked(prefs.getBoolean("enable_accelerometer", false));
        switchSound.setChecked(prefs.getBoolean("enable_sound", true));
        switchVibration.setChecked(prefs.getBoolean("enable_vibration", true));

        // Click listeners
        btnToggleAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(MainActivity.this, "Find 'Double Tap Screen Off' and enable it", Toast.LENGTH_LONG).show();
        });

        btnToggleAdmin.setOnClickListener(v -> {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminName = new ComponentName(MainActivity.this, ScreenLockAdminReceiver.class);
            if (dpm != null && !dpm.isAdminActive(adminName)) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.device_admin_description));
                startActivity(intent);
            } else if (dpm != null) {
                dpm.removeActiveAdmin(adminName);
                Toast.makeText(MainActivity.this, "Device Administrator Fallback deactivated", Toast.LENGTH_SHORT).show();
                updateStatuses();
            }
        });

        btnIgnoreBattery.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent();
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "App is already whitelisted from battery optimization", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Battery optimization not restricted on this SDK", Toast.LENGTH_SHORT).show();
            }
        });

        // Toggle handling
        switchFloatingWidget.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("enable_floating_widget", isChecked).apply();
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                    buttonView.setChecked(false);
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    Toast.makeText(MainActivity.this, "Please grant overlay permission", Toast.LENGTH_LONG).show();
                } else {
                    startFloatingWidgetService();
                }
            } else {
                stopFloatingWidgetService();
            }
            updateStatuses();
        });

        switchAccelerometer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("enable_accelerometer", isChecked).apply();
            if (isChecked) {
                startSensorService();
            } else {
                stopSensorService();
            }
            updateStatuses();
        });

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("enable_sound", isChecked).apply();
        });

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("enable_vibration", isChecked).apply();
        });

        // Test lock gesture on main pad
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                performLockRoutine(true);
                return true;
            }
        });

        btnTestPad.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        tvAbout.setOnClickListener(v -> showAboutDialog());
        tvPrivacy.setOnClickListener(v -> showPrivacyDialog());

        // Wallpaper Customization Click Listeners
        btnChooseWallpaper.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Wallpaper Photo"), PICK_IMAGE_REQUEST);
        });

        btnSetLiveWallpaper.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        new ComponentName(MainActivity.this, DoubleTapWallpaperService.class));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Your device launcher may require manual setting from Home Screen -> Wallpapers.", Toast.LENGTH_LONG).show();
            }
        });

        btnResetWallpaper.setOnClickListener(v -> {
            java.io.File file = new java.io.File(getFilesDir(), "custom_wallpaper.png");
            if (file.exists()) {
                file.delete();
            }
            Toast.makeText(MainActivity.this, "Wallpaper reset to default slate theme color!", Toast.LENGTH_SHORT).show();
        });

        // Check for overlay permission fallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && switchFloatingWidget.isChecked() && Settings.canDrawOverlays(this)) {
            startFloatingWidgetService();
        }

        // Ask the user to enable Accessibility Service on launch if not active
        if (!isAccessibilityServiceEnabled(this, DoubleTapAccessibilityService.class)) {
            showAccessibilityPromptDialog();
        }
    }

    private void showAccessibilityPromptDialog() {
        new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
                .setTitle("Enable Accessibility Service")
                .setMessage("Double Tap Screen Off requires the Accessibility Service permission to perform secure screen locking without physical buttons. This service is lightweight, fully local, offline, and safe. Please tap 'Enable' to open settings and select Double Tap Screen Off.")
                .setCancelable(false)
                .setPositiveButton("Enable", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                    Toast.makeText(this, "Find 'Double Tap Screen Off' under Downloaded Services and enable it", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Later", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri imageUri = data.getData();
            try {
                java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    java.io.File file = new java.io.File(getFilesDir(), "custom_wallpaper.png");
                    java.io.FileOutputStream outputStream = new java.io.FileOutputStream(file);
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    
                    inputStream.close();
                    outputStream.close();
                    
                    Toast.makeText(this, "Wallpaper successfully updated! Apply the Live Wallpaper to see changes.", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load wallpaper: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatuses();
    }

    private void updateStatuses() {
        // 1. Accessibility Service Enabled
        boolean accEnabled = isAccessibilityServiceEnabled(this, DoubleTapAccessibilityService.class);
        if (accEnabled) {
            tvAccessibilityStatus.setText(R.string.service_active_status);
            tvAccessibilityStatus.setTextColor(getResources().getColor(R.color.status_green));
        } else {
            tvAccessibilityStatus.setText(R.string.service_inactive_status);
            tvAccessibilityStatus.setTextColor(getResources().getColor(R.color.status_red));
        }

        // 2. Device Admin Active
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminName = new ComponentName(this, ScreenLockAdminReceiver.class);
        boolean adminActive = dpm != null && dpm.isAdminActive(adminName);
        if (adminActive) {
            tvAdminStatus.setText(R.string.service_active_status);
            tvAdminStatus.setTextColor(getResources().getColor(R.color.status_green));
            btnToggleAdmin.setText("Disable Device Admin Fallback");
        } else {
            tvAdminStatus.setText(R.string.service_inactive_status);
            tvAdminStatus.setTextColor(getResources().getColor(R.color.status_red));
            btnToggleAdmin.setText("Enable Device Admin Fallback");
        }

        // 3. Floating Widget service status
        boolean widgetRunning = FloatingWidgetService.isRunning();
        if (widgetRunning) {
            tvWidgetStatus.setText(R.string.service_active_status);
            tvWidgetStatus.setTextColor(getResources().getColor(R.color.status_green));
        } else {
            tvWidgetStatus.setText(R.string.service_inactive_status);
            tvWidgetStatus.setTextColor(getResources().getColor(R.color.status_red));
        }

        // 4. Sensor background service status
        boolean sensorRunning = DoubleTapSensorService.isRunning();
        if (sensorRunning) {
            tvSensorStatus.setText(R.string.service_active_status);
            tvSensorStatus.setTextColor(getResources().getColor(R.color.status_green));
        } else {
            tvSensorStatus.setText(R.string.service_inactive_status);
            tvSensorStatus.setTextColor(getResources().getColor(R.color.status_red));
        }
    }

    private void startFloatingWidgetService() {
        Intent serviceIntent = new Intent(this, FloatingWidgetService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopFloatingWidgetService() {
        stopService(new Intent(this, FloatingWidgetService.class));
    }

    private void startSensorService() {
        Intent serviceIntent = new Intent(this, DoubleTapSensorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopSensorService() {
        stopService(new Intent(this, DoubleTapSensorService.class));
    }

    private void performLockRoutine(boolean isInteractiveTest) {
        // Feedback vibration
        if (prefs.getBoolean("enable_vibration", true)) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(70);
            }
        }

        // Feedback sound
        if (prefs.getBoolean("enable_sound", true)) {
            try {
                ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_SYSTEM, 70);
                toneGen.startTone(ToneGenerator.TONE_PROP_PROMPT, 80);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (isInteractiveTest) {
            Toast.makeText(this, "Lock Triggered!", Toast.LENGTH_SHORT).show();
        }

        // Broadcast or execute lock action
        Intent lockIntent = new Intent("com.doubletap.screenoff.ACTION_LOCK");
        sendBroadcast(lockIntent);
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> service) {
        ComponentName expectedComponentName = new ComponentName(context, service);
        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null) return false;
        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);
        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
            if (enabledService != null && enabledService.equals(expectedComponentName)) return true;
        }
        return false;
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
                .setTitle("About Double Tap Screen Off")
                .setMessage("Double Tap Screen Off is a lightweight utility designed to help users quickly lock their mobile device without clicking physical buttons. By leveraging a high-performance background sensor service and secure Accessibility framework, it provides immediate reaction speeds with less than 0.1% battery consumption daily.\n\nDeveloped entirely in native Java.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showPrivacyDialog() {
        new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
                .setTitle("Privacy Policy")
                .setMessage("Your privacy is absolute. Double Tap Screen Off:\n\n• Does NOT request Internet permissions.\n• Does NOT track or collect user analytics or telemetry.\n• Operates entirely on-device offline.\n• Standard Android security permissions are requested strictly to perform the screen-locking action on double tap.")
                .setPositiveButton("OK", null)
                .show();
    }
}
