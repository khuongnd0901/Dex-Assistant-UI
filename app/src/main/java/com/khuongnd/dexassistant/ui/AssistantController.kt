package com.khuongnd.dexassistant.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.khuongnd.dexassistant.ui.device.DpcClient
import com.khuongnd.dexassistant.ui.actions.LocalActionExecutor
import com.khuongnd.dexassistant.ui.history.CommandHistoryStore
import com.khuongnd.dexassistant.ui.protocol.AssistantWebSocket
import com.khuongnd.dexassistant.ui.protocol.DeviceAction
import com.khuongnd.dexassistant.ui.protocol.ProtocolParser
import com.khuongnd.dexassistant.ui.protocol.QuotaFreshness
import com.khuongnd.dexassistant.ui.routing.LocalCommandParser
import com.khuongnd.dexassistant.ui.routing.CapabilityRegistry
import com.khuongnd.dexassistant.ui.routing.CommandRouter
import com.khuongnd.dexassistant.ui.routing.Route
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
    private val voicePreferences = context.getSharedPreferences("voice_health", Context.MODE_PRIVATE)
    private val voiceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refreshVoiceHealth() }
    private val dpc = DpcClient(context)
    private val webSocket = AssistantWebSocket(this)
    private val parser = LocalCommandParser()
    private val router = CommandRouter(parser, CapabilityRegistry(context))
    private val localExecutor = LocalActionExecutor(context, dpc)
    private val history = CommandHistoryStore(context)
    private var state = UiState(endpoint = preferences.getString("endpoint", "") ?: "")

    fun start() {
        dpc.bind()
        refreshHealth()
        publish()
        refreshHistory()
    }

    fun observeVoiceHealth() {
        voicePreferences.registerOnSharedPreferenceChangeListener(voiceListener)
        refreshVoiceHealth()
    }

    fun stopObservingVoiceHealth() {
        voicePreferences.unregisterOnSharedPreferenceChangeListener(voiceListener)
    }

    fun stop() {
        stopObservingVoiceHealth()
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
        val decision = router.route(text)
        val routed = decision.command
        when (routed.route) {
            Route.LOCAL -> if (decision.capability?.available == false) {
                val error = decision.capability.unavailableReason ?: "POLICY_DENIED"
                history.record(text, routed.route.name, routed.intent?.name, error); update("FAILED", error); refreshHistory()
            } else localExecutor.execute(routed) { result -> onMain {
                history.record(text, routed.route.name, routed.intent?.name, result.errorCode ?: result.outcome.name)
                state = state.copy(status = result.outcome.name, message = result.errorCode ?: result.outcome.name); refreshHistory()
            } }
            Route.UNSUPPORTED -> { history.record(text, routed.route.name, null, "UNSUPPORTED_COMMAND"); update("UNSUPPORTED", "Lệnh chưa được hỗ trợ"); refreshHistory() }
            Route.BACKEND_REQUIRED -> runCatching { webSocket.sendCommand(text) }
                .onSuccess { history.record(text, routed.route.name, null, "SENT"); state = state.copy(lastRequestId = it.toString(), message = "Command sent"); refreshHistory() }
                .onFailure { history.record(text, routed.route.name, null, "AI_UNAVAILABLE"); update("SEND_FAILED", "Khó quá, không làm được"); refreshHistory() }
        }
    }

    fun retry() {
        if (!webSocket.retryLast()) update("NO_RETRY", "No command is available to retry")
    }

    private fun refreshVoiceHealth() {
        val voice = voicePreferences
        state = state.copy(
            voiceState = voice.getString("state", "STOPPED") ?: "STOPPED",
            voiceDetail = voice.getString("detail", "Not started") ?: "Not started",
            assistantReadiness = voice.getString(VoiceAssistantService.KEY_ASSISTANT_READINESS, "NOT_BOUND") ?: "NOT_BOUND",
        )
        publish()
    }

    fun refreshHealth() {
        refreshVoiceHealth()
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

    override fun onQuotaSnapshot(snapshot: ProtocolParser.QuotaSnapshot) = onMain {
        state = state.copy(quota = snapshot, quotaStale = QuotaFreshness.isStale(snapshot.staleAfter)); publish()
    }
    override fun onQuotaUnsupported() = onMain { state = state.copy(quota = null, quotaStale = true); publish() }
    override fun onDisconnected() = onMain { state = state.copy(quotaStale = true); publish() }

    fun clearHistory() { history.clear(); refreshHistory() }
    private fun refreshHistory() { state = state.copy(history = history.read()); publish() }

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
    val quota: ProtocolParser.QuotaSnapshot? = null,
    val quotaStale: Boolean = true,
    val history: List<com.khuongnd.dexassistant.ui.history.HistoryEntry> = emptyList(),
)
