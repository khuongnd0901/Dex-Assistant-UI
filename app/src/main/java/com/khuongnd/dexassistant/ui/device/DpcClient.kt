package com.khuongnd.dexassistant.ui.device

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import java.util.concurrent.atomic.AtomicInteger

class DpcClient(private val context: Context) {
    private var remote: Messenger? = null
    private val callbacks = mutableMapOf<Int, (DeviceResult) -> Unit>()
    private val sequence = AtomicInteger()
    private val incoming = Messenger(Handler(Looper.getMainLooper()) { message ->
        if (message.what != MSG_RESULT) return@Handler false
        val callback = callbacks.remove(message.arg1) ?: return@Handler true
        val names = message.data.getStringArrayList(KEY_CAPABILITY_NAMES).orEmpty()
        val states = message.data.getStringArrayList(KEY_CAPABILITY_STATES).orEmpty()
        callback(DeviceResult(message.data.getString(KEY_STATUS) ?: STATUS_FAILED,
            message.data.getString(KEY_ERROR_CODE), names.zip(states).toMap()))
        true
    })

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            remote = Messenger(binder)
        }
        override fun onServiceDisconnected(name: ComponentName?) { remote = null }
    }

    fun bind(): Boolean = context.bindService(
        Intent(ACTION).setComponent(ComponentName(DPC_PACKAGE, SERVICE_CLASS)),
        connection,
        Context.BIND_AUTO_CREATE,
    )

    fun unbind() {
        runCatching { context.unbindService(connection) }
        remote = null
    }

    fun toggleWifi(enabled: Boolean, callback: (DeviceResult) -> Unit) {
        request(MSG_TOGGLE_WIFI, Bundle().apply { putBoolean(KEY_ENABLED, enabled) }, callback)
    }

    fun applyPermissionProfile(callback: (DeviceResult) -> Unit) = request(MSG_APPLY_PERMISSION_PROFILE, Bundle(), callback)
    fun getCapabilityReport(callback: (DeviceResult) -> Unit) = request(MSG_GET_CAPABILITY_REPORT, Bundle(), callback)
    fun startVoiceService(callback: (DeviceResult) -> Unit) = request(MSG_START_VOICE_SERVICE, Bundle(), callback)
    fun stopVoiceService(callback: (DeviceResult) -> Unit) = request(MSG_STOP_VOICE_SERVICE, Bundle(), callback)

    private fun request(what: Int, data: Bundle, callback: (DeviceResult) -> Unit) {
        val service = remote
        if (service == null) {
            callback(DeviceResult(STATUS_REJECTED, ERROR_DEVICE_OWNER_REQUIRED))
            return
        }
        val id = sequence.incrementAndGet()
        callbacks[id] = callback
        val request = Message.obtain(null, what).apply {
            arg1 = id
            replyTo = incoming
            this.data = data
        }
        runCatching { service.send(request) }.onFailure {
            callbacks.remove(id)?.invoke(DeviceResult(STATUS_FAILED, ERROR_ACTION_TIMEOUT))
        }
    }

    data class DeviceResult(val status: String, val errorCode: String?, val capabilities: Map<String, String> = emptyMap())

    companion object {
        private const val ACTION = "com.khuongnd.dexassistant.dpc.EXECUTE_DEVICE_ACTION"
        private const val DPC_PACKAGE = "com.khuongnd.dexassistant.dpc"
        private const val SERVICE_CLASS = "com.khuongnd.dexassistant.dpc.DeviceActionService"
        private const val MSG_TOGGLE_WIFI = 1
        private const val MSG_RESULT = 2
        private const val MSG_APPLY_PERMISSION_PROFILE = 3
        private const val MSG_GET_CAPABILITY_REPORT = 4
        private const val MSG_START_VOICE_SERVICE = 5
        private const val MSG_STOP_VOICE_SERVICE = 6
        private const val KEY_ENABLED = "enabled"
        private const val KEY_STATUS = "status"
        private const val KEY_ERROR_CODE = "errorCode"
        private const val KEY_CAPABILITY_NAMES = "capabilityNames"
        private const val KEY_CAPABILITY_STATES = "capabilityStates"
        private const val STATUS_FAILED = "FAILED"
        private const val STATUS_REJECTED = "REJECTED"
        private const val ERROR_DEVICE_OWNER_REQUIRED = "DEVICE_OWNER_REQUIRED"
        private const val ERROR_ACTION_TIMEOUT = "ACTION_TIMEOUT"
    }
}
