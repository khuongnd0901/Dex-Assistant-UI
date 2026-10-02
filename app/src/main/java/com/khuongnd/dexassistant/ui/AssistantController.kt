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
import com.khuongnd.dexassistant.ui.routing.CommandPrivacy
import com.khuongnd.dexassistant.ui.routing.LocalCommandParser
import com.khuongnd.dexassistant.ui.routing.CapabilityRegistry
import com.khuongnd.dexassistant.ui.routing.CommandRouter
import com.khuongnd.dexassistant.ui.routing.Route
import com.khuongnd.dexassistant.ui.security.CredentialStore
import com.khuongnd.dexassistant.ui.voice.VoiceAssistantService
import java.util.UUID

data class VoiceCommandResult(val requestId: String, val status: String, val message: String)

/** Activity facade; closing a screen only detaches its observer. */
class AssistantController(context: Context, private val stateChanged: (UiState) -> Unit) {
    private val core = shared(context)
    fun start() { core.addObserver(stateChanged); core.start() }
    fun stop() { core.removeObserver(stateChanged) }
    fun observeVoiceHealth() = core.observeVoiceHealth()
    fun stopObservingVoiceHealth() = Unit
    fun provision(endpoint: String, token: String) = core.provision(endpoint, token)
    fun connect() = core.connect()
    fun send(text: String) = core.send(text)
    fun retry() = core.retry()
    fun refreshHealth() = core.refreshHealth()
    fun applyPermissions() = core.applyPermissions()
    fun startVoice() = core.startVoice()
    fun stopVoice() = core.stopVoice()
    fun clearHistory() = core.clearHistory()
    companion object {
        @Volatile private var instance: AssistantCoordinator? = null
        fun shared(context: Context): AssistantCoordinator = instance ?: synchronized(this) {
            instance ?: AssistantCoordinator(context.applicationContext).also { instance = it }
        }
    }
}

class AssistantCoordinator internal constructor(private val context: Context) : AssistantWebSocket.Listener {
    private val observers = linkedSetOf<(UiState) -> Unit>()
    private var started = false
    private val commandGate = CommandSessionGate()
    private val pendingId: String? get() = commandGate.currentId
    private var pendingProgress: ((String, String) -> Unit)? = null
    private var pendingCompletion: ((VoiceCommandResult) -> Unit)? = null
    private var pendingDispatched = false
    private var backendDispatch = false
    private var remoteDispatch = false
    private val executedActions = mutableSetOf<String>()
    private val receivedPlans = mutableSetOf<String>()
    private var connectionActive = false
    private val watchdog = Runnable { finish("ACTION_TIMEOUT", "Hết thời gian xử lý lệnh") }
    fun addObserver(observer: (UiState) -> Unit) { observers.add(observer); observer(state) }
    fun removeObserver(observer: (UiState) -> Unit) { observers.remove(observer) }
    fun dispatchVoice(text: String, progress: ((String, String) -> Unit)? = null, completion: (VoiceCommandResult) -> Unit): String {
        val id = UUID.randomUUID().toString()
        commandGate.enqueue(id)
        // Always enqueue so callers can retain the returned correlation ID before completion.
        main.post {
            val admitted = commandGate.beginQueued(id)
            if (admitted == null) completion(VoiceCommandResult(id, "CANCELLED", "Đã dừng"))
            else if (!admitted) completion(VoiceCommandResult(id, "COMMAND_IN_PROGRESS", "Đang xử lý lệnh trước"))
            else {
                pendingCompletion = completion; pendingProgress = progress; pendingDispatched = false
                progress?.invoke(id, "ROUTING")
                if (pendingId != id) return@post
                backendDispatch = false; remoteDispatch = false; executedActions.clear(); receivedPlans.clear()
                main.postDelayed(watchdog, 30_000)
                if (text.isBlank()) finish("NO_SPEECH", "Chưa nghe rõ lệnh") else send(text)
            }
        }
        return id
    }
    fun cancelVoice(requestId: String) {
        commandGate.cancelQueued(requestId)
        onMain { if (pendingId == requestId) finish("CANCELLED", "Đã dừng") }
    }
    private fun finish(status: String, message: String) {
        val id = pendingId ?: return
        val callback = pendingCompletion
        commandGate.finish(id); pendingCompletion = null; pendingProgress = null; main.removeCallbacks(watchdog)
        callback?.invoke(VoiceCommandResult(id, status, message))
    }
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
        if (started) return
        started = true
        dpc.bind()
        observeVoiceHealth()
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

    fun provision(endpoint: String, token: String) {
        runCatching {
            credentials.save(token)
            preferences.edit().putString("endpoint", endpoint).apply()
            state = state.copy(endpoint = endpoint, status = "PROVISIONED", message = "Credential stored")
            publish()
            webSocket.connect(endpoint, token)
        }.onFailure { update("CONFIGURATION_ERROR", it.message ?: "Invalid configuration") }
    }

    fun disconnect() {
        webSocket.disconnect()
        if (backendDispatch) finish("DISCONNECTED", "Mất kết nối máy chủ")
    }

    fun connect() {
        if (connectionActive) return
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
        if (pendingId != null) {
            if (pendingDispatched) { update("COMMAND_IN_PROGRESS", "Đang xử lý lệnh trước"); return }
            pendingDispatched = true
        } else { dispatchVoice(text) { }; return }
        if (CommandPrivacy.requiresLocalHandling(text)) {
            history.record(text, Route.UNSUPPORTED.name, null, "LOCAL_ONLY_UNSUPPORTED")
            update("UNSUPPORTED", "Lệnh chứa thông tin đăng nhập hoặc kết nối Wi-Fi chỉ được xử lý trên máy; tính năng này chưa được hỗ trợ")
            refreshHistory()
            return
        }
        val activeRequestId = pendingId
        val decision = router.route(text)
        val routed = decision.command
        when (routed.route) {
            Route.LOCAL -> if (decision.capability?.available == false) {
                val error = decision.capability.unavailableReason ?: "POLICY_DENIED"
                history.record(text, routed.route.name, routed.intent?.name, error); update("FAILED", error); refreshHistory()
            } else {
                pendingId?.let { pendingProgress?.invoke(it, "EXECUTING") }
                if (pendingId != activeRequestId) return
                localExecutor.execute(routed) { result -> onMain {
                if (pendingId != activeRequestId) return@onMain
                history.record(text, routed.route.name, routed.intent?.name, result.errorCode ?: result.outcome.name)
                state = state.copy(status = result.outcome.name, message = result.errorCode ?: result.outcome.name); refreshHistory()
                finish(result.errorCode ?: result.outcome.name, when (result.outcome.name) {
                    "VERIFIED" -> "Đã thực hiện"
                    "DISPATCHED" -> "Đã gửi yêu cầu thực hiện"
                    else -> "Không thực hiện được lệnh"
                })
            } } }
            Route.UNSUPPORTED -> { history.record(text, routed.route.name, null, "UNSUPPORTED_COMMAND"); update("UNSUPPORTED", "Lệnh chưa được hỗ trợ"); refreshHistory() }
            Route.BACKEND_REQUIRED -> runCatching {
                backendDispatch = true
                pendingId?.let { pendingProgress?.invoke(it, "SENDING") }
                check(pendingId == activeRequestId) { "Command cancelled" }
                webSocket.sendCommand(text, UUID.fromString(pendingId))
            }
                .onSuccess { history.record(text, routed.route.name, null, "SENT"); state = state.copy(lastRequestId = it.toString(), message = "Command sent"); refreshHistory() }
                .onFailure { history.record(text, routed.route.name, null, "AI_UNAVAILABLE"); update("SEND_FAILED", "Khó quá, không làm được"); refreshHistory() }
        }
    }

    fun retry() {
        // Voice uses a fresh utterance after an ambiguous result, never automatic replay.
        update("NO_RETRY", "Hãy gọi lại và nói lệnh mới")
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

    override fun onState(state: String) = onMain {
        if (state in setOf("CONNECTING", "CONNECTED", "RECONNECTING")) connectionActive = true
        if (state == "DISCONNECTED") connectionActive = false
        update(state, state.lowercase().replace('_', ' '))
    }

    override fun onActions(actions: List<DeviceAction>) = Unit
    override fun onCorrelatedActions(requestId: String, actions: List<DeviceAction>) = onMain {
        if (pendingId != requestId || !backendDispatch) return@onMain
        val planId = actions.firstOrNull()?.planId ?: return@onMain
        if (receivedPlans.isNotEmpty() || !receivedPlans.add(planId)) return@onMain
        pendingProgress?.invoke(requestId, "EXECUTING")
        executeActions(requestId, actions, 0)
    }
    private fun executeActions(requestId: String, actions: List<DeviceAction>, index: Int) {
        if (pendingId != requestId || index >= actions.size) return
        val action = actions[index]
        if (action.executionMode != "REMOTE" || !executedActions.add(action.actionId)) {
            executeActions(requestId, actions, index + 1); return
        }
        when (action.tool) {
            "LAUNCH_APP" -> { remoteDispatch = true; launchApp(action) { success -> if (success) executeActions(requestId, actions, index + 1) } }
            "TOGGLE_WIFI" -> toggleWifi(action) { success -> if (success) executeActions(requestId, actions, index + 1) }
            else -> webSocket.sendActionResult(action.planId, action.actionId, "REJECTED", "POLICY_DENIED")
        }
    }

    private fun launchApp(action: DeviceAction, complete: (Boolean) -> Unit) {
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
            complete(false)
            return
        }
        val result = runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        webSocket.sendActionResult(action.planId, action.actionId,
            if (result.isSuccess) "SUCCESS" else "FAILED",
            if (result.isSuccess) null else "APP_NOT_FOUND")
        onMetric("action.result", action.actionId, SystemClock.elapsedRealtimeNanos())
        complete(result.isSuccess)
    }

    private fun toggleWifi(action: DeviceAction, complete: (Boolean) -> Unit) {
        onMetric("action.start", action.actionId, SystemClock.elapsedRealtimeNanos())
        val requestId = pendingId
        dpc.toggleWifi(action.arguments.getBoolean("enabled")) { result -> onMain {
            if (pendingId != requestId) return@onMain
            webSocket.sendActionResult(action.planId, action.actionId, result.status, result.errorCode)
            onMetric("action.result", action.actionId, SystemClock.elapsedRealtimeNanos())
            complete(result.status == "SUCCESS")
        } }
    }

    override fun onError(message: String) = onMain { update("ERROR", message) }
    override fun onCommandCompleted(requestId: String, status: String) = onMain {
        if (pendingId == requestId && backendDispatch) finish(
            if (status == "EXECUTED") if (remoteDispatch) "DISPATCHED" else "VERIFIED" else status,
            when (status) {
                "EXECUTED" -> if (remoteDispatch) "Đã gửi yêu cầu thực hiện" else "Đã thực hiện"
                "PLANNED" -> "Đã lập kế hoạch, chưa thực hiện trên máy"
                else -> "Không thực hiện được lệnh"
            })
    }
    override fun onCommandError(requestId: String, code: String, message: String) = onMain {
        if (pendingId == requestId && backendDispatch) finish(code, message)
    }

    override fun onAuthenticationFailure() = onMain {
        connectionActive = false
        credentials.clear()
        update("AUTHENTICATION_FAILED", "Credential rejected and removed; provision again")
    }

    override fun onQuotaSnapshot(snapshot: ProtocolParser.QuotaSnapshot) = onMain {
        state = state.copy(quota = snapshot, quotaStale = QuotaFreshness.isStale(snapshot.staleAfter)); publish()
    }
    override fun onQuotaUnsupported() = onMain { state = state.copy(quota = null, quotaStale = true); publish() }
    override fun onDisconnected() = onMain { state = state.copy(quotaStale = true); publish(); if (backendDispatch) finish("DISCONNECTED", "Mất kết nối máy chủ") }

    fun clearHistory() { history.clear(); refreshHistory() }
    private fun refreshHistory() { state = state.copy(history = history.read()); publish() }

    override fun onMetric(event: String, correlationId: String, elapsedNanos: Long) {
        // Deliberately excludes command text and credentials.
        android.util.Log.i("DexMetric", "$event correlationId=$correlationId elapsedNanos=$elapsedNanos")
    }

    private fun update(status: String, message: String) {
        state = state.copy(status = status, message = message)
        publish()
        if (status in setOf("UNSUPPORTED", "FAILED", "SEND_FAILED", "AUTHENTICATION_FAILED")) finish(status, message)
    }

    private fun publish() { observers.toList().forEach { it(state) } }
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
