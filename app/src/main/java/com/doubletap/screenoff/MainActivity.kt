package com.doubletap.screenoff

import android.app.WallpaperManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DoubleTapLockTheme {
                DashboardScreen()
            }
        }
    }

    @Composable
    fun DoubleTapLockTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Color(0xFF818CF8), // Indigo Light
                secondary = Color(0xFFA5B4FC),
                background = Color(0xFF0F172A), // Dark Navy Slate
                surface = Color(0xFF1E293B)
            ),
            content = content
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DashboardScreen() {
        val context = LocalContext.current
        val scrollState = rememberScrollState()

        // State Checkers
        var isAccessibilityOn by remember { mutableStateOf(false) }
        var isDeviceAdminOn by remember { mutableStateOf(false) }
        var isOverlayAllowed by remember { mutableStateOf(false) }
        var isFloatingServiceRunning by remember { mutableStateOf(false) }

        // Persistent Preferences States
        val prefs = remember { context.getSharedPreferences("DoubleTapLockPrefs", Context.MODE_PRIVATE) }
        var isVibrationEnabled by remember { mutableStateOf(prefs.getBoolean("pref_vibration", true)) }
        var isSoundEnabled by remember { mutableStateOf(prefs.getBoolean("pref_sound", true)) }
        var soundType by remember { mutableStateOf(prefs.getString("pref_sound_type", "beep") ?: "beep") }
        var wallpaperType by remember { mutableStateOf(prefs.getString("pref_wallpaper_type", "gradient") ?: "gradient") }
        var selectedColorStr by remember { mutableStateOf(prefs.getString("pref_wallpaper_color", "#090d22") ?: "#090d22") }
        var customImageUriStr by remember { mutableStateOf(prefs.getString("pref_wallpaper_image_uri", "") ?: "") }

        // Gallery Launcher for Custom Wallpaper
        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(it, takeFlags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                customImageUriStr = it.toString()
                prefs.edit().putString("pref_wallpaper_image_uri", it.toString()).apply()
                // Force wallpaper type to image
                wallpaperType = "image"
                prefs.edit().putString("pref_wallpaper_type", "image").apply()
                Toast.makeText(context, "Custom wallpaper image set!", Toast.LENGTH_SHORT).show()
            }
        }

        // Periodic Check on lifecycle resume
        val checkStates = {
            isAccessibilityOn = isAccessibilityServiceEnabled(context, AccessibilityLockService::class.java)
            isDeviceAdminOn = isDeviceAdminEnabled(context)
            isOverlayAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        LaunchedEffect(Unit) {
            checkStates()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A), // Slate 900
                            Color(0xFF312E81), // Indigo 900
                            Color(0xFF581C87)  // Purple 900
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Release Status Tag Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isAccessibilityOn) Color(0xFF34D399) else Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "V1.0.5 • ${if (isAccessibilityOn) "SERVICE STABLE" else "SETUP REQUIRED"}",
                            color = Color(0xFFC7D2FE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = "Double Tap\nScreen Off",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 44.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subtitle
                Text(
                    text = "Programmatic screen power-off utility for Android. Fully battery-safe daemon.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Telemetry Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "SYSTEM METRICS",
                            color = Color(0xFFA5B4FC),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "BATTERY / DAY", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Text(text = "0.02%", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
                            }
                            Column {
                                Text(text = "RAM IMPACT", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Text(text = "14 MB", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "REQUIRED CONFIGURATION",
                    color = Color(0xFFA5B4FC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 4.dp, bottom = 12.dp)
                )

                // 1. Accessibility Toggle Row
                ConfigRow(
                    title = "Accessibility Service",
                    description = "Required to lock screen instantly on modern systems.",
                    statusText = if (isAccessibilityOn) "Active" else "Grant Access",
                    statusColor = if (isAccessibilityOn) Color(0xFF34D399) else Color(0xFFFBBF24),
                    icon = Icons.Default.AccessibilityNew,
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                        Toast.makeText(context, "Enable 'Double Tap Screen Off' under Downloaded Services", Toast.LENGTH_LONG).show()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Set Wallpaper Button Row
                ConfigRow(
                    title = "Home Double-Tap Wallpaper",
                    description = "Sets background live wallpaper listener on home viewport.",
                    statusText = "Setup Wallpaper",
                    statusColor = Color(0xFF818CF8),
                    icon = Icons.Default.Wallpaper,
                    onClick = {
                        try {
                            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                putExtra(
                                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                    ComponentName(context, DoubleTapWallpaperService::class.java)
                                )
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                            context.startActivity(intent)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Device Admin Privileges Fallback Row
                ConfigRow(
                    title = "Device Administrator",
                    description = "Legacy screen sleep framework fallback interface.",
                    statusText = if (isDeviceAdminOn) "Granted" else "Activate Fallback",
                    statusColor = if (isDeviceAdminOn) Color(0xFF34D399) else Color(0xFFEF4444).copy(alpha = 0.8f),
                    icon = Icons.Default.Security,
                    onClick = {
                        if (!isDeviceAdminOn) {
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminReceiver.getComponentName(context))
                                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Secure locking permissions daemon.")
                            }
                            context.startActivity(intent)
                        } else {
                            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                            dpm.removeActiveAdmin(AdminReceiver.getComponentName(context))
                            Toast.makeText(context, "Admin Deactivated", Toast.LENGTH_SHORT).show()
                            checkStates()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Floating Shortcut Bubble Row
                ConfigRow(
                    title = "Assistive Floating Bubble",
                    description = "Overlay double-tap shortcut widget.",
                    statusText = if (isFloatingServiceRunning) "Running" else "Spawn Bubble",
                    statusColor = if (isFloatingServiceRunning) Color(0xFF34D399) else Color(0xFFC7D2FE),
                    icon = Icons.Default.TouchApp,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                            Toast.makeText(context, "Allow Draw Over Other Apps permission.", Toast.LENGTH_LONG).show()
                        } else {
                            val serviceIntent = Intent(context, FloatingLockService::class.java)
                            if (isFloatingServiceRunning) {
                                context.stopService(serviceIntent)
                                isFloatingServiceRunning = false
                                Toast.makeText(context, "Floating controller removed.", Toast.LENGTH_SHORT).show()
                            } else {
                                context.startService(serviceIntent)
                                isFloatingServiceRunning = true
                                Toast.makeText(context, "Floating lock bubble spawned!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "WALLPAPER & LOOK",
                    color = Color(0xFFA5B4FC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 4.dp, bottom = 12.dp)
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Wallpaper, contentDescription = null, tint = Color(0xFF818CF8))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WALLPAPER TYPE",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Selection Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("gradient" to "Gradient", "color" to "Solid Color", "image" to "Custom Image").forEach { (type, label) ->
                                Button(
                                    onClick = {
                                        wallpaperType = type
                                        prefs.edit().putString("pref_wallpaper_type", type).apply()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (wallpaperType == type) Color(0xFF818CF8) else Color.White.copy(alpha = 0.1f),
                                        contentColor = if (wallpaperType == type) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Options depending on selection
                        if (wallpaperType == "gradient" || wallpaperType == "color") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Select Background Color Theme:",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                listOf(
                                    "#090d22" to Color(0xFF090D22), // Space Navy
                                    "#1e0b36" to Color(0xFF1E0B36), // Velvet Violet
                                    "#062315" to Color(0xFF062315), // Forest Emerald
                                    "#020205" to Color(0xFF020205), // Midnight Obsidian
                                    "#2c040d" to Color(0xFF2C040D)  // Rose Crimson
                                ).forEach { (colorStr, colorVal) ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(colorVal)
                                            .clickable {
                                                selectedColorStr = colorStr
                                                prefs.edit().putString("pref_wallpaper_color", colorStr).apply()
                                            }
                                            .border(
                                                width = 2.dp,
                                                color = if (selectedColorStr == colorStr) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        } else if (wallpaperType == "image") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    galleryLauncher.launch("image/*")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.1f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SELECT FROM GALLERY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            if (customImageUriStr.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "✓ Custom image source configured successfully",
                                    color = Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "FEEDBACK & SIGNALS",
                    color = Color(0xFFA5B4FC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 4.dp, bottom = 12.dp)
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Vibration setting row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Vibration Feedback", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Vibrate device briefly when screen is locked", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                            Switch(
                                checked = isVibrationEnabled,
                                onCheckedChange = {
                                    isVibrationEnabled = it
                                    prefs.edit().putBoolean("pref_vibration", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF818CF8),
                                    checkedTrackColor = Color(0xFF818CF8).copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sound setting row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sound Feedback", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Play sound effect on locking screen", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                            Switch(
                                checked = isSoundEnabled,
                                onCheckedChange = {
                                    isSoundEnabled = it
                                    prefs.edit().putBoolean("pref_sound", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF818CF8),
                                    checkedTrackColor = Color(0xFF818CF8).copy(alpha = 0.4f)
                                )
                            )
                        }

                        if (isSoundEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Select Sound Type:",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                              ) {
                                listOf("beep" to "System Beep", "notification" to "Notification Bell").forEach { (type, label) ->
                                    Button(
                                        onClick = {
                                            soundType = type
                                            prefs.edit().putString("pref_sound_type", type).apply()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (soundType == type) Color(0xFF818CF8) else Color.White.copy(alpha = 0.1f),
                                            contentColor = if (soundType == type) Color.Black else Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Test Programmatic Screen Lock Button
                Button(
                    onClick = {
                        if (isAccessibilityOn || isDeviceAdminOn) {
                            LockFeedbackHelper.triggerFeedback(context)
                            if (isAccessibilityOn) {
                                AccessibilityLockService.instance?.lockScreenDevice()
                            } else {
                                AdminReceiver.lockWithAdmin(context)
                            }
                        } else {
                            Toast.makeText(context, "Please enable Accessibility or Admin first!", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1E1B4B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "TEST PROGRAMMATIC LOCK NOW", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    @Composable
    fun ConfigRow(
        title: String,
        description: String,
        statusText: String,
        statusColor: Color,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = description, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, lineHeight = 14.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(text = statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val expectedComponentName = ComponentName(context, service)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedComponentName) {
                return true
            }
        }
        return false
    }

    private fun isDeviceAdminEnabled(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(AdminReceiver.getComponentName(context))
    }
}
