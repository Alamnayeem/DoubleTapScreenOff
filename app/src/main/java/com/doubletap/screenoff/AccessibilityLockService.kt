package com.doubletap.screenoff

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class AccessibilityLockService : AccessibilityService() {

    companion object {
        var instance: AccessibilityLockService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Toast.makeText(this, "Double Tap Lock Accessibility Service Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event parsing needed as we only trigger programmatic screen locks
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    /**
     * Shuts down or locks the device screen securely via native global actions.
     */
    fun lockScreenDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val success = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            if (!success) {
                Toast.makeText(this, "Failed to lock using Accessibility. Try Device Admin.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Android 9+ required for Accessibility lock.", Toast.LENGTH_SHORT).show()
        }
    }
}
