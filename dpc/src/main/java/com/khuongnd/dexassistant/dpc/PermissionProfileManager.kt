package com.khuongnd.dexassistant.dpc

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo

class PermissionProfileManager(private val context: Context) {
    private val packageManager = context.packageManager
    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, DexDeviceAdminReceiver::class.java)

    fun apply(): Map<String, String> {
        val info = runCatching {
            packageManager.getPackageInfo(TARGET_PACKAGE, PackageManager.GET_PERMISSIONS)
        }.getOrElse { return mapOf("package" to PermissionPolicy.FAILED) }
        return info.requestedPermissions.orEmpty().associateWith(::grantAndClassify)
    }

    fun capabilityReport(): Map<String, String> = buildMap {
        put("deviceOwner", if (dpm.isDeviceOwnerApp(context.packageName)) PermissionPolicy.GRANTED else PermissionPolicy.FAILED)
        putAll(apply())
        SPECIAL_ACCESS.forEach { put(it, PermissionPolicy.USER_ACTION_REQUIRED) }
        put("android.permission.CAPTURE_VIDEO_OUTPUT", PermissionPolicy.UNSUPPORTED)
    }

    private fun grantAndClassify(permission: String): String {
        val dangerous = runCatching {
            @Suppress("DEPRECATION")
            val info = packageManager.getPermissionInfo(permission, 0)
            info.protection == PermissionInfo.PROTECTION_DANGEROUS
        }.getOrDefault(false)
        val granted = packageManager.checkPermission(permission, TARGET_PACKAGE) == PackageManager.PERMISSION_GRANTED
        if (!dangerous || granted) return PermissionPolicy.classify(true, dangerous, granted, null)
        if (!dpm.isDeviceOwnerApp(context.packageName)) return PermissionPolicy.USER_ACTION_REQUIRED
        val result = runCatching {
            dpm.setPermissionGrantState(admin, TARGET_PACKAGE, permission, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED)
        }.getOrElse { false }
        val verified = packageManager.checkPermission(permission, TARGET_PACKAGE) == PackageManager.PERMISSION_GRANTED
        return PermissionPolicy.classify(true, true, verified, result && verified)
    }

    companion object {
        const val TARGET_PACKAGE = "com.khuongnd.dexassistant.ui"
        val SPECIAL_ACCESS = listOf(
            "special.accessibility", "special.notification_listener", "special.overlay",
            "special.write_settings", "special.usage_access", "special.exact_alarm",
            "special.all_files", "special.install_unknown_apps", "special.battery_optimization",
            "special.media_projection",
        )
    }
}
