package com.khuongnd.dexassistant.dpc

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

class RecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val relevantPackage = intent.data?.schemeSpecificPart
        if (intent.action == Intent.ACTION_PACKAGE_REPLACED && relevantPackage != PermissionProfileManager.TARGET_PACKAGE) return
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        if (!dpm.isDeviceOwnerApp(context.packageName)) return
        val pending = goAsync()
        Thread {
            try {
                PermissionProfileManager(context).apply()
                val service = Intent(ACTION_START).setComponent(ComponentName(TARGET_PACKAGE, TARGET_SERVICE))
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service) else context.startService(service)
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_START = "com.khuongnd.dexassistant.ui.action.START_VOICE_SERVICE"
        const val TARGET_PACKAGE = "com.khuongnd.dexassistant.ui"
        const val TARGET_SERVICE = "com.khuongnd.dexassistant.ui.voice.VoiceAssistantService"
    }
}
