package com.khuongnd.dexassistant.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.khuongnd.dexassistant.ui.device.DpcClient
import com.khuongnd.dexassistant.ui.protocol.AssistantWebSocket
import com.khuongnd.dexassistant.ui.protocol.DeviceAction
import com.khuongnd.dexassistant.ui.security.CredentialStore
import com.khuongnd.dexassistant.ui.voice.VoiceAssistantService
import java.util.UUID

class AssistantController(
    private val context: Context,
    private val stateChanged: (UiState) -> Unit,
) : AssistantWebSocket.Listener {
    private val main = Handler(Looper.getMainLooper())
    private val credentials = CredentialStore(context)
    private val preferences = context.getSharedPreferences("assistant_config", Context.MODE_PRIVATE)
    private val dpc = DpcClient(context)
    private val webSocket = AssistantWebSocket(this)
    private var state = UiState(endpoint = preferences.getString("endpoint", "") ?: "")

    fun start() {
        dpc.bind()
        refreshHealth()
        publish()
    }

    fun stop() {
        webSocket.close()
        dpc.unbind()
    }

    fun provision(endpoint: String, token: String) {
        runCatching {
            credentials.save(token)
            preferences.edit().putString("endpoint", endpoint).apply()
            state = state.copy(endpoint = endpoint, status = "PROVISIONED", message = "Credential stored")
            publish()
            webSocket.connect(endpoint, token)
        }.onFailure { update("CONFIGURATION_ERROR", it.message ?: "Invalid configuration") }
    }

    fun connect() {
        val token = credentials.load()
        if (token == null || state.endpoint.isBlank()) {
            update("NOT_PROVISIONED", "Enter endpoint and token")
            return
        }
        runCatching { webSocket.connect(state.endpoint, token) }
            .onFailure { update("CONFIGURATION_ERROR", it.message ?: "Invalid endpoint") }
    }

    fun send(text: String) {
        if (text.isBlank()) return
        runCatching { webSocket.sendCommand(text) }
            .onSuccess { state = state.copy(lastRequestId = it.toString(), message = "Command sent"); publish() }
            .onFailure { update("SEND_FAILED", it.message ?: "Command not sent") }
    }

    fun retry() {
        if (!webSocket.retryLast()) update("NO_RETRY", "No command is available to retry")
    }

    fun refreshHealth() {
        val voice = context.getSharedPreferences("voice_health", Context.MODE_PRIVATE)
        state = state.copy(
            voiceState = voice.getString("state", "STOPPED") ?: "STOPPED",
            voiceDetail = voice.getString("detail", "Not started") ?: "Not started",
            assistantReadiness = voice.getString(VoiceAssistantService.KEY_ASSISTANT_READINESS, "NOT_BOUND") ?: "NOT_BOUND",
        )
        dpc.getCapabilityReport { result ->
            state = state.copy(capabilities = result.capabilities,
                deviceOwner = result.capabilities["deviceOwner"] == "GRANTED")
            publish()
        }
        publish()
    }

    fun applyPermissions() = dpc.applyPermissionProfile { result ->
        state = state.copy(capabilities = result.capabilities, message = "Permission profile checked")
        publish()
    }

    fun startVoice() {
        val result = runCatching { ContextCompat.startForegroundService(context,
            Intent(context, VoiceAssistantService::class.java).setAction(VoiceAssistantService.ACTION_START)) }
        update(if (result.isSuccess) "VOICE_START_REQUESTED" else "VOICE_START_FAILED",
            result.exceptionOrNull()?.javaClass?.simpleName ?: "App requested its voice service")
        refreshHealth()
    }

    fun stopVoice() {
        context.startService(Intent(context, VoiceAssistantService::class.java).setAction(VoiceAssistantService.ACTION_STOP))
        update("VOICE_STOP_REQUESTED", "App requested microphone stop")
        refreshHealth()
    }

    override fun onState(state: String) = onMain { update(state, state.lowercase().replace('_', ' ')) }

    override fun onActions(actions: List<DeviceAction>) = onMain {
        actions.forEach { action ->
            if (action.executionMode != "REMOTE") return@forEach
            when (action.tool) {
                "LAUNCH_APP" -> launchApp(action)
                "TOGGLE_WIFI" -> toggleWifi(action)
                else -> webSocket.sendActionResult(action.planId, action.actionId, "REJECTED", "POLICY_DENIED")
            }
        }
    }

    private fun launchApp(action: DeviceAction) {
        onMetric("action.start", action.actionId, SystemClock.elapsedRealtimeNanos())
        val packageName = action.arguments.optString("packageName")
        val deepLink = action.arguments.optString("deepLink").takeIf { it.isNotBlank() && it != "null" }
        val intent = if (deepLink != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).setPackage(packageName)
        } else {
            context.packageManager.getLaunchIntentForPackage(packageName)
        }
        if (intent == null) {
            webSocket.sendActionResult(action.planId, action.actionId, "FAILED", "APP_NOT_FOUND")
            return
        }
        val result = runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        webSocket.sendActionResult(action.planId, action.actionId,
            if (result.isSuccess) "SUCCESS" else "FAILED",
            if (result.isSuccess) null else "APP_NOT_FOUND")
        onMetric("action.result", action.actionId, SystemClock.elapsedRealtimeNanos())
    }

    private fun toggleWifi(action: DeviceAction) {
        onMetric("action.start", action.actionId, SystemClock.elapsedRealtimeNanos())
        dpc.toggleWifi(action.arguments.getBoolean("enabled")) { result ->
            webSocket.sendActionResult(action.planId, action.actionId, result.status, result.errorCode)
            onMetric("action.result", action.actionId, SystemClock.elapsedRealtimeNanos())
        }
    }

    override fun onError(message: String) = onMain { update("ERROR", message) }

    override fun onAuthenticationFailure() = onMain {
        credentials.clear()
        update("AUTHENTICATION_FAILED", "Credential rejected and removed; provision again")
    }

    override fun onMetric(event: String, correlationId: String, elapsedNanos: Long) {
        // Deliberately excludes command text and credentials.
        android.util.Log.i("DexMetric", "$event correlationId=$correlationId elapsedNanos=$elapsedNanos")
    }

    private fun update(status: String, message: String) {
        state = state.copy(status = status, message = message)
        publish()
    }

    private fun publish() = stateChanged(state)
    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }
}

data class UiState(
    val endpoint: String = "",
    val status: String = "DISCONNECTED",
    val message: String = "Not configured",
    val lastRequestId: String? = null,
    val voiceState: String = "STOPPED",
    val voiceDetail: String = "Not started",
    val assistantReadiness: String = "NOT_BOUND",
    val deviceOwner: Boolean = false,
    val capabilities: Map<String, String> = emptyMap(),
)
