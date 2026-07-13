package com.doubletap.screenoff

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Device Administrator Activated", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Device Administrator Deactivated", Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, AdminReceiver::class.java)
        }

        /**
         * Locks the screen using legacy Device Policy Manager Admin authority.
         */
        fun lockWithAdmin(context: Context) {
            val policyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val componentName = getComponentName(context)
            if (policyManager != null && policyManager.isAdminActive(componentName)) {
                policyManager.lockNow()
            } else {
                Toast.makeText(context, "Please enable Accessibility Service or Device Admin inside app settings", Toast.LENGTH_LONG).show()
            }
        }
    }
}
