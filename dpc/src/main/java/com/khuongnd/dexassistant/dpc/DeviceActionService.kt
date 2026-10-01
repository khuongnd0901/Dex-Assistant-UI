package com.khuongnd.dexassistant.dpc

import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger

class DeviceActionService : Service() {
    private val handler = Handler(Looper.getMainLooper()) { request ->
        val reply = request.replyTo ?: return@Handler true
        when (request.what) {
            MSG_APPLY_PERMISSION_PROFILE -> {
                respondReport(reply, request.arg1, PermissionProfileManager(this).apply())
                return@Handler true
            }
            MSG_GET_CAPABILITY_REPORT -> {
                respondReport(reply, request.arg1, PermissionProfileManager(this).capabilityReport())
                return@Handler true
            }
            MSG_TOGGLE_WIFI -> Unit
            else -> return@Handler false
        }
        val desired = request.data.getBoolean(KEY_ENABLED)
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val ownerCheck = WifiDecision.preflight(dpm.isDeviceOwnerApp(packageName), true)
        if (ownerCheck.status != "PENDING") {
            respond(reply, request.arg1, ownerCheck.status, ownerCheck.errorCode)
            return@Handler true
        }

        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val accepted = try {
            wifi.setWifiEnabled(desired)
        } catch (_: SecurityException) {
            false
        }
        val policyCheck = WifiDecision.preflight(true, accepted)
        if (policyCheck.status != "PENDING") {
            respond(reply, request.arg1, policyCheck.status, policyCheck.errorCode)
            return@Handler true
        }
        observeWifiState(wifi, desired, reply, request.arg1, 0)
        true
    }

    private val messenger = Messenger(handler)

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    private fun observeWifiState(
        wifi: WifiManager,
        desired: Boolean,
        reply: Messenger,
        requestId: Int,
        attempt: Int,
    ) {
        if (wifi.isWifiEnabled == desired) {
            respond(reply, requestId, STATUS_SUCCESS, null)
        } else if (attempt >= MAX_ATTEMPTS) {
            respond(reply, requestId, STATUS_FAILED, ERROR_DEVICE_STATE_MISMATCH)
        } else {
            handler.postDelayed({ observeWifiState(wifi, desired, reply, requestId, attempt + 1) }, POLL_MS)
        }
    }

    private fun respond(reply: Messenger, requestId: Int, status: String, error: String?) {
        val response = Message.obtain(null, MSG_RESULT).apply {
            arg1 = requestId
            data = Bundle().apply {
                putString(KEY_STATUS, status)
                if (error != null) putString(KEY_ERROR_CODE, error)
            }
        }
        runCatching { reply.send(response) }
    }

    private fun respondReport(reply: Messenger, requestId: Int, report: Map<String, String>) {
        val response = Message.obtain(null, MSG_RESULT).apply {
            arg1 = requestId
            data = Bundle().apply {
                putString(KEY_STATUS, STATUS_SUCCESS)
                putStringArrayList(KEY_CAPABILITY_NAMES, ArrayList(report.keys))
                putStringArrayList(KEY_CAPABILITY_STATES, ArrayList(report.values))
            }
        }
        runCatching { reply.send(response) }
    }

    companion object {
        const val ACTION = "com.khuongnd.dexassistant.dpc.EXECUTE_DEVICE_ACTION"
        const val PACKAGE = "com.khuongnd.dexassistant.dpc"
        const val MSG_TOGGLE_WIFI = 1
        const val MSG_RESULT = 2
        const val MSG_APPLY_PERMISSION_PROFILE = 3
        const val MSG_GET_CAPABILITY_REPORT = 4
        const val KEY_ENABLED = "enabled"
        const val KEY_STATUS = "status"
        const val KEY_ERROR_CODE = "errorCode"
        const val KEY_CAPABILITY_NAMES = "capabilityNames"
        const val KEY_CAPABILITY_STATES = "capabilityStates"
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_REJECTED = "REJECTED"
        const val ERROR_DEVICE_STATE_MISMATCH = "DEVICE_STATE_MISMATCH"
        private const val POLL_MS = 250L
        private const val MAX_ATTEMPTS = 40
    }
}
