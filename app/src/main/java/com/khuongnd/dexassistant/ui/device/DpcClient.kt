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

class DpcClient(private val context: Context, private val connected: (() -> Unit)? = null) {
    private var remote: Messenger? = null
    private var bound = false
    private var wantsBinding = false
    private var connection: ServiceConnection? = null
    private var rebindAttempts = 0
    private val callbacks = mutableMapOf<Int, (DeviceResult) -> Unit>()
    private val sequence = AtomicInteger()
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val incoming = Messenger(Handler(Looper.getMainLooper()) { message ->
        if (message.what != MSG_RESULT) return@Handler false
        val callback = callbacks.remove(message.arg1) ?: return@Handler true
        val names = message.data.getStringArrayList(KEY_CAPABILITY_NAMES).orEmpty()
        val states = message.data.getStringArrayList(KEY_CAPABILITY_STATES).orEmpty()
        callback(DeviceResult(message.data.getString(KEY_STATUS) ?: STATUS_FAILED,
            message.data.getString(KEY_ERROR_CODE), names.zip(states).toMap()))
        true
    })

    fun bind(): Boolean {
        wantsBinding = true
        rebindAttempts = 0
        if (bound) {
            if (remote != null) connected?.invoke()
            return true
        }
        return bindOnce()
    }

    private fun bindOnce(): Boolean {
        if (!wantsBinding) return false
        val candidate = object : ServiceConnection {
            private fun current() = wantsBinding && connection === this

            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (!current()) return
                if (binder == null) { bindingLost(this); return }
                remote = Messenger(binder)
                connected?.invoke()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                if (current()) bindingLost(this)
            }

            override fun onBindingDied(name: ComponentName?) {
                if (current()) bindingLost(this)
            }

            override fun onNullBinding(name: ComponentName?) {
                if (current()) bindingLost(this)
            }
        }
        connection = candidate
        bound = runCatching {
            context.bindService(
                Intent(ACTION).setComponent(ComponentName(DPC_PACKAGE, SERVICE_CLASS)),
                candidate,
                Context.BIND_AUTO_CREATE,
            )
        }.getOrDefault(false)
        if (!bound) connection = null
        return bound
    }

    private fun bindingLost(lostConnection: ServiceConnection) {
        // Reset before completing requests: their callbacks may ask for another bind.
        remote = null
        connection = null
        val wasBound = bound
        bound = false
        if (wasBound) runCatching { context.unbindService(lostConnection) }
        val pending = callbacks.values.toList()
        callbacks.clear()
        pending.forEach { it(DeviceResult(STATUS_FAILED, ERROR_ACTION_TIMEOUT)) }
        scheduleRebind()
    }

    private fun scheduleRebind() {
        if (!wantsBinding || bound || rebindAttempts >= MAX_REBINDS) return
        rebindAttempts++
        timeoutHandler.postDelayed({
            if (wantsBinding && !bound && !bindOnce()) scheduleRebind()
        }, 1_000L)
    }

    fun unbind() {
        wantsBinding = false
        val previous = connection
        connection = null
        if (bound && previous != null) runCatching { context.unbindService(previous) }
        bound = false
        remote = null
        // Lifecycle cancellation drops callbacks; it must not trigger a retry in a dead owner.
        callbacks.clear()
        timeoutHandler.removeCallbacksAndMessages(null)
    }

    fun toggleWifi(enabled: Boolean, callback: (DeviceResult) -> Unit) {
        request(MSG_TOGGLE_WIFI, Bundle().apply { putBoolean(KEY_ENABLED, enabled) }, callback)
    }

    fun applyPermissionProfile(callback: (DeviceResult) -> Unit) = request(MSG_APPLY_PERMISSION_PROFILE, Bundle(), callback)
    fun getCapabilityReport(callback: (DeviceResult) -> Unit) = request(MSG_GET_CAPABILITY_REPORT, Bundle(), callback)

    private fun request(what: Int, data: Bundle, callback: (DeviceResult) -> Unit) {
        val service = remote
        if (service == null) {
            callback(DeviceResult(STATUS_FAILED, ERROR_ACTION_TIMEOUT))
            return
        }
        val id = sequence.incrementAndGet()
        callbacks[id] = callback
        timeoutHandler.postDelayed({
            callbacks.remove(id)?.invoke(DeviceResult(STATUS_FAILED, ERROR_ACTION_TIMEOUT))
        }, 10_000L)
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
        private const val MAX_REBINDS = 3
        private const val ACTION = "com.khuongnd.dexassistant.dpc.EXECUTE_DEVICE_ACTION"
        private const val DPC_PACKAGE = "com.khuongnd.dexassistant.dpc"
        private const val SERVICE_CLASS = "com.khuongnd.dexassistant.dpc.DeviceActionService"
        private const val MSG_TOGGLE_WIFI = 1
        private const val MSG_RESULT = 2
        private const val MSG_APPLY_PERMISSION_PROFILE = 3
        private const val MSG_GET_CAPABILITY_REPORT = 4
        private const val KEY_ENABLED = "enabled"
        private const val KEY_STATUS = "status"
        private const val KEY_ERROR_CODE = "errorCode"
        private const val KEY_CAPABILITY_NAMES = "capabilityNames"
        private const val KEY_CAPABILITY_STATES = "capabilityStates"
        private const val STATUS_FAILED = "FAILED"
        private const val ERROR_ACTION_TIMEOUT = "ACTION_TIMEOUT"
    }
}
