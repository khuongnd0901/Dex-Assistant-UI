package com.khuongnd.dexassistant.dpc

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_LOCKED_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        if (!dpm.isDeviceOwnerApp(context.packageName)) return
        val pending = goAsync()
        Thread {
            try {
                PermissionProfileManager(context).apply()
                context.createDeviceProtectedStorageContext()
                    .getSharedPreferences(HEALTH_PREFERENCES, Context.MODE_PRIVATE)
                    .edit().putLong(KEY_LAST_POLICY_APPLY, System.currentTimeMillis()).apply()
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val HEALTH_PREFERENCES = "dpc_health"
        const val KEY_LAST_POLICY_APPLY = "lastPolicyApply"
    }
}
