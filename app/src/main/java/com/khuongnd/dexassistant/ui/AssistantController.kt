package com.khuongnd.dexassistant.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import com.khuongnd.dexassistant.ui.execution.ActionRouter
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import com.khuongnd.dexassistant.ui.protocol.DeviceCommandProtocol
import com.khuongnd.dexassistant.ui.routing.CommandRouter
import com.khuongnd.dexassistant.ui.routing.Route
import com.khuongnd.dexassistant.ui.security.CredentialStore
import com.khuongnd.dexassistant.ui.voice.VoiceAssistantService
import java.util.UUID
import com.khuongnd.dexassistant.ui.screen.DexAccessibilityService
import com.khuongnd.dexassistant.ui.screen.ScreenCommandParser
import com.khuongnd.dexassistant.ui.latency.CommandLatency

data class VoiceCommandResult(val requestId: String, val status: String, val message: String)

/** Activity facade; closing a screen only detaches its observer. */
class AssistantController(private val contextForVoice: Context, private val stateChanged: (UiState) -> Unit) {
    private val core = shared(contextForVoice)
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
    fun autoStartVoice() = com.khuongnd.dexassistant.ui.voice.VoiceAutoStart.evaluate(contextForVoice)
    fun stopVoice() = core.stopVoice()
    fun resumeVoice() = core.voiceAction(VoiceAssistantService.ACTION_RESUME)
    fun pauseVoice() = core.voiceAction(VoiceAssistantService.ACTION_PAUSE)
    fun diagnosticListen() = core.voiceAction(VoiceAssistantService.ACTION_DIAGNOSTIC)
    fun clearHistory() = core.clearHistory()
    companion object {
        @Volatile private var instance: AssistantCoordinator? = null
        fun shared(context: Context): AssistantCoordinator = instance ?: synchronized(this) {
            instance ?: AssistantCoordinator(context.applicationContext).also { instance = it }
        }
    }
}

class AssistantCoordinator internal constructor(private val context: Context, private val history: CommandHistoryStore = CommandHistoryStore(context)) : AssistantWebSocket.Listener {
    private val observers = linkedSetOf<(UiState) -> Unit>()
    private var started = false
    private val commandGate = CommandSessionGate()
    private val pendingId: String? get() = commandGate.currentId
    private var pendingProgress: ((String, String) -> Unit)? = null
    private var pendingInteractionPrompt: ((ActionRouter.Prompt) -> Unit)? = null
    private var activeText = ""
    private var pendingCompletion: ((VoiceCommandResult) -> Unit)? = null
    private var pendingDispatched = false
    private var backendDispatch = false
    private var remoteDispatch = false
    private var remoteResultCode: String? = null
    private var latency: CommandLatency? = null
    private var latencyPath = "local"
    private var speechEndAtNanos: Long? = null
    private fun markActionStart() {
        val start = speechEndAtNanos ?: return
        speechEndAtNanos = null
        val now = SystemClock.elapsedRealtimeNanos()
        if (now >= start) latencyEvent("speechEndToActionStartLatency", (now - start) / 1_000)
    }
    private fun latencyEvent(name: String, micros: Long) {
        pendingId?.let { android.util.Log.i("DexLatency", "requestId=$it path=$latencyPath metric=$name durationUs=$micros") }
    }
    private fun safeReason(value: String): String = if (value.matches(Regex("[A-Z][A-Z0-9_]{0,63}"))) value else "OTHER"
    private var pendingRemoteAction: DeviceAction? = null
    private val executedActions = mutableSetOf<String>()
    private val receivedPlans = mutableSetOf<String>()
    private var connectionActive = false
    @Volatile private var chatObserver: ((com.khuongnd.dexassistant.ui.protocol.ChatSession.Event) -> Unit)? = null
    fun observeChat(observer: ((com.khuongnd.dexassistant.ui.protocol.ChatSession.Event) -> Unit)?) { chatObserver = observer }
    fun startChat() = onMain { runCatching { webSocket.startChat() }.onFailure { chatObserver?.invoke(com.khuongnd.dexassistant.ui.protocol.ChatSession.Event.Error(null, UUID.randomUUID(), "CHAT_PROVIDER_UNAVAILABLE", "Chat connection unavailable", true)) } }
    fun sendChat(text: String) = onMain { runCatching { webSocket.sendChatTurn(text) }.onFailure { chatObserver?.invoke(com.khuongnd.dexassistant.ui.protocol.ChatSession.Event.Error(null, UUID.randomUUID(), "CHAT_INVALID_SESSION", "Chat session unavailable", false)) } }
    fun endChat(reason: String = "USER") = onMain { webSocket.endChat(reason) }
    private val watchdog = Runnable { finish("ACTION_TIMEOUT", "Hết thời gian xử lý lệnh") }
    private val commandDeadline = Runnable { finish("COMMAND_TIMEOUT", "Hết thời gian xử lý lệnh") }
    fun addObserver(observer: (UiState) -> Unit) { observers.add(observer); observer(state) }
    fun removeObserver(observer: (UiState) -> Unit) { observers.remove(observer) }
    fun dispatchVoice(text: String, progress: ((String, String) -> Unit)? = null, interaction: ((ActionRouter.Prompt) -> Unit)? = null, speechEndNanos: Long? = null, completion: (VoiceCommandResult) -> Unit): String {
        val id = UUID.randomUUID().toString()
        commandGate.enqueue(id)
        // Always enqueue so callers can retain the returned correlation ID before completion.
        main.post {
            val admitted = commandGate.beginQueued(id)
            if (admitted == null) completion(VoiceCommandResult(id, "CANCELLED", "Đã dừng"))
            else if (!admitted) completion(VoiceCommandResult(id, "COMMAND_IN_PROGRESS", "Đang xử lý lệnh trước"))
            else {
                pendingCompletion = completion; pendingProgress = progress; pendingInteractionPrompt = interaction; pendingDispatched = false; activeText = text
                backendDispatch = false; remoteDispatch = false; remoteResultCode = null; executedActions.clear(); receivedPlans.clear()
                latencyPath = "local"; latency = CommandLatency(::latencyEvent)
                speechEndAtNanos = speechEndNanos
                progress?.invoke(id, "ROUTING")
                if (pendingId != id) return@post
                main.postDelayed(watchdog, 30_000)
                main.postDelayed(commandDeadline, 120_000)
                if (text.isBlank()) finish("NO_SPEECH", "Chưa nghe rõ lệnh") else send(text)
            }
        }
        return id
    }
    fun cancelVoice(requestId: String) {
        commandGate.cancelQueued(requestId)
        onMain { if (pendingId == requestId) {
            DexAccessibilityService.active?.clear()
            finish("CANCELLED", "Đã dừng")
        } }
    }
    fun isAwaitingInteraction(requestId: String) = pendingId == requestId && actionRouter.awaitingInteraction
    fun interactionPromptFinished(prompt: ActionRouter.Prompt, success: Boolean): Boolean =
        pendingId == prompt.parentId && actionRouter.promptFinished(prompt, success)
    fun continueInteraction(requestId: String, text: String, speechEndNanos: Long? = null): Boolean {
        if (pendingId != requestId || !actionRouter.awaitingInteraction) return false
        speechEndAtNanos = speechEndNanos
        if (actionRouter.continueInteraction(text)) return true
        // A new final command replaces the interaction; the old completion must not speak over it.
        recordHistory(activeText, if (backendDispatch) Route.BACKEND_REQUIRED.name else Route.LOCAL.name, null, "CANCELLED")
        pendingCompletion = null
        finish("CANCELLED", "Đã hủy lệnh trước")
        return false
    }
    private fun finish(status: String, message: String) {
        val id = pendingId ?: return
        actionRouter.cancel()
        latency?.end("executionLatency", SystemClock.elapsedRealtimeNanos())
        android.util.Log.i("DexLatency", "requestId=$id path=$latencyPath terminalReason=${safeReason(status)}")
        latency?.clear(); latency = null
        speechEndAtNanos = null
        val callback = pendingCompletion
        pendingRemoteAction?.also { action ->
            pendingRemoteAction = null
            webSocket.sendActionResult(action.planId, action.actionId, "REJECTED", status)
        }
        commandGate.finish(id); pendingCompletion = null; pendingProgress = null; pendingInteractionPrompt = null; main.removeCallbacks(watchdog); main.removeCallbacks(commandDeadline)
        if (backendDispatch) recordHistory(activeText, Route.BACKEND_REQUIRED.name, null, status)
        callback?.invoke(VoiceCommandResult(id, status, message))
    }
    private val main = Handler(Looper.getMainLooper())
    private val credentials = CredentialStore(context)
    private val preferences = context.getSharedPreferences("assistant_config", Context.MODE_PRIVATE)
    private val voicePreferences = context.getSharedPreferences("voice_health", Context.MODE_PRIVATE)
    private val voiceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refreshVoiceHealth() }
    private val dpc = DpcClient(context)
    private val webSocket = AssistantWebSocket(this)
    private val commandRegistry = CommandRegistry.load(context)
    private val parser = LocalCommandParser(commandRegistry)
    private val router = CommandRouter(parser, CapabilityRegistry(context))
    private val localExecutor = LocalActionExecutor(context, dpc, commandRegistry::appPackage)
    private val actionRouter = ActionRouter(context, commandRegistry, localExecutor)
    private val historyPublication = com.khuongnd.dexassistant.ui.history.HistoryPublicationGate()
    private var state = UiState(endpoint = preferences.getString("endpoint", "") ?: "")

    fun start() {
        if (started) return
        started = true
        context.getSystemService(android.hardware.display.DisplayManager::class.java).registerDisplayListener(object : android.hardware.display.DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = refreshVoiceHealth()
            override fun onDisplayChanged(displayId: Int) = refreshVoiceHealth()
            override fun onDisplayRemoved(displayId: Int) = refreshVoiceHealth()
        }, main)
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
        latency?.start("routeLatency", SystemClock.elapsedRealtimeNanos())
        val screenCommand = router.screenCommand(text, actionRouter.foregroundAppId())
        if (screenCommand != null) {
            latency?.end("routeLatency", SystemClock.elapsedRealtimeNanos())
            latency?.start("executionLatency", SystemClock.elapsedRealtimeNanos())
            val service = DexAccessibilityService.active
            val screenRequestId = pendingId
            val complete: (String) -> Unit = { status -> onMain {
                if (pendingId == screenRequestId) {
                    recordHistory(text, Route.LOCAL.name, screenCommand.verb.name, status)
                    state = state.copy(status = status, message = status); publish()
                    finish(status, status)
                }
            } }
            if (service == null) complete("ACCESSIBILITY_PERMISSION_REQUIRED")
            else if (service.hasYoutubePreviewHistory && router.youtubeIndexSelection(text)) complete("SCREEN_CHANGED")
            else if (service.hasYoutubePreviewHistory && screenCommand.verb == com.khuongnd.dexassistant.ui.screen.ScreenVerb.CLICK_TARGET)
                service.execute(screenCommand, complete)
            else { markActionStart(); service.execute(screenCommand, complete) }
            return
        }
        if (CommandPrivacy.requiresLocalHandling(text)) {
            DexAccessibilityService.active?.clear()
            recordHistory(text, Route.UNSUPPORTED.name, null, "LOCAL_ONLY_UNSUPPORTED")
            update("UNSUPPORTED", "Lệnh chứa thông tin đăng nhập hoặc kết nối Wi-Fi chỉ được xử lý trên máy; tính năng này chưa được hỗ trợ")
            return
        }
        val activeRequestId = pendingId
        val decision = router.route(text, actionRouter.foregroundAppId())
        val routed = decision.command
        if (routed.resolved?.typedAction?.let { it.action == com.khuongnd.dexassistant.ui.routing.ActionId.YOUTUBE_SELECT && it.params.containsKey("index") } != true)
            DexAccessibilityService.active?.clear()
        latency?.end("routeLatency", SystemClock.elapsedRealtimeNanos())
        when (routed.route) {
            Route.LOCAL -> if (decision.capability?.available == false) {
                val error = decision.capability.unavailableReason ?: "POLICY_DENIED"
                recordHistory(text, routed.route.name, routed.intent?.name, error); update("FAILED", error)
            } else {
                pendingId?.let { pendingProgress?.invoke(it, "EXECUTING") }
                if (pendingId != activeRequestId) return
                val typed = routed.resolved?.typedAction
                if (typed == null) { finish("INVALID_ARGUMENTS", "Lệnh chưa hợp lệ"); return }
                if (typed.action.name == "CLICK_TARGET") latencyPath = "semantic"
                latency?.start("executionLatency", SystemClock.elapsedRealtimeNanos())
                main.removeCallbacks(watchdog)
                actionRouter.execute(activeRequestId!!, typed, pendingInteractionPrompt, routed.resolved.commandId,
                    localCommandId = routed.resolved.commandId, metric = ::actionMetric) { result -> onMain {
                if (pendingId != activeRequestId) return@onMain
                recordHistory(text, routed.route.name, routed.intent?.name, result.errorCode ?: result.outcome.name)
                state = state.copy(status = result.outcome.name, message = result.errorCode ?: result.outcome.name); publish()
                finish(result.errorCode ?: result.outcome.name, when (result.outcome.name) {
                    "VERIFIED" -> "Đã thực hiện"
                    "DISPATCHED" -> "Đã gửi yêu cầu thực hiện"
                    else -> "Không thực hiện được lệnh"
                })
            } } }
            Route.UNSUPPORTED -> {
                val reason = routed.arguments["reason"] ?: "UNSUPPORTED_COMMAND"
                recordHistory(text, routed.route.name, null, reason)
                state = state.copy(status = reason, message = "Lệnh chưa được hỗ trợ"); publish()
                finish(reason, "Lệnh chưa được hỗ trợ")
            }
            Route.BACKEND_REQUIRED -> runCatching {
                backendDispatch = true
                latencyPath = "fallback"
                pendingId?.let { pendingProgress?.invoke(it, "SENDING") }
                check(pendingId == activeRequestId) { "Command cancelled" }
                webSocket.sendCommand(text, UUID.fromString(pendingId), DeviceCommandProtocol.capabilities(commandRegistry, actionRouter.supportedActions()))
            }
                .onSuccess { recordHistory(text, routed.route.name, null, "SENT"); state = state.copy(lastRequestId = it.toString(), message = "Command sent"); publish() }
                .onFailure { recordHistory(text, routed.route.name, null, "AI_UNAVAILABLE"); update("SEND_FAILED", "Khó quá, không làm được") }
        }
    }

    fun retry() {
        // Voice uses a fresh utterance after an ambiguous result, never automatic replay.
        update("NO_RETRY", "Hãy gọi lại và nói lệnh mới")
    }

    private fun refreshVoiceHealth() {
        val voice = voicePreferences
        val desktop = context.getSystemService(android.hardware.display.DisplayManager::class.java)
            .getDisplays(DexAccessibilityService.SAMSUNG_DESKTOP_CATEGORY).any { it.isValid && it.displayId != android.view.Display.DEFAULT_DISPLAY }
        val enabled = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_accessibility_services")
            .orEmpty().split(':').mapNotNull(android.content.ComponentName::unflattenFromString)
            .contains(android.content.ComponentName(context, DexAccessibilityService::class.java))
        state = state.copy(
            dexStatus = if (desktop) "DeX: kết nối" else "DeX: chưa kết nối",
            accessibilityStatus = if (DexAccessibilityService.active != null) "Trợ năng: sẵn sàng" else if (enabled) "Trợ năng: đang kết nối" else "Trợ năng: chưa bật",
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

    fun voiceAction(action: String) {
        runCatching { ContextCompat.startForegroundService(context, Intent(context, VoiceAssistantService::class.java).setAction(action)) }
            .onFailure { update("VOICE_ACTION_FAILED", it.javaClass.simpleName) }
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
        latencyPath = "remote"
        val planId = actions.firstOrNull()?.planId ?: return@onMain
        if (receivedPlans.isNotEmpty() || !receivedPlans.add(planId)) return@onMain
        if (actions.size > 8 || actions.map { it.actionId }.distinct().size != actions.size || actions.any { it.planId != planId }) {
            finish("INVALID_MESSAGE", "Kế hoạch chưa hợp lệ"); return@onMain
        }
        pendingProgress?.invoke(requestId, "EXECUTING")
        executeActions(requestId, actions, 0)
    }
    private fun executeActions(requestId: String, actions: List<DeviceAction>, index: Int) {
        if (pendingId != requestId) return
        if (index >= actions.size) { main.postDelayed(watchdog, 30_000); return }
        val action = actions[index]
        if (!executedActions.add(action.actionId)) { finish("INVALID_MESSAGE", "Kế hoạch có thao tác trùng"); return }
        val typed = runCatching { DeviceCommandProtocol.decode(action, commandRegistry, actionRouter.supportedActions()) }.getOrNull()
        if (typed == null) {
            webSocket.sendActionResult(action.planId, action.actionId, "REJECTED", "POLICY_DENIED")
            main.postDelayed(watchdog, 30_000)
            return
        }
        main.removeCallbacks(watchdog)
        onMetric("action.start", action.actionId, SystemClock.elapsedRealtimeNanos())
        if (typed.action.name == "CLICK_TARGET") latencyPath = "semantic"
        latency?.start("executionLatency", SystemClock.elapsedRealtimeNanos())
        pendingRemoteAction = action
        actionRouter.execute(requestId, typed, pendingInteractionPrompt, action.actionId, metric = ::actionMetric) { result -> onMain {
            if (pendingId != requestId || pendingRemoteAction?.actionId != action.actionId) return@onMain
            pendingRemoteAction = null
            val success = result.outcome.name in setOf("DISPATCHED", "VERIFIED")
            if (result.outcome.name == "DISPATCHED") remoteDispatch = true
            remoteResultCode = result.errorCode
            webSocket.sendActionResult(action.planId, action.actionId,
                if (success) "SUCCESS" else if (result.outcome.name == "REJECTED") "REJECTED" else "FAILED", result.errorCode)
            onMetric("action.result", action.actionId, SystemClock.elapsedRealtimeNanos())
            latency?.end("executionLatency", SystemClock.elapsedRealtimeNanos())
            if (success) executeActions(requestId, actions, index + 1) else main.postDelayed(watchdog, 30_000)
        } }
    }

    override fun onError(message: String) = onMain { update("ERROR", message) }
    override fun onCommandCompleted(requestId: String, status: String) = onMain {
        if (pendingId == requestId && backendDispatch) finish(
            if (status == "EXECUTED") remoteResultCode ?: if (remoteDispatch) "DISPATCHED" else "VERIFIED" else remoteResultCode ?: status,
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
    override fun onDisconnected() = onMain {
        state = state.copy(quotaStale = true); publish()
        if (backendDispatch) finish("DISCONNECTED", "Mất kết nối máy chủ")
        chatObserver?.invoke(com.khuongnd.dexassistant.ui.protocol.ChatSession.Event.Error(null, UUID.randomUUID(), "CHAT_INVALID_SESSION", "Chat connection ended", true))
    }

    fun clearHistory() {
        val ticket = historyPublication.next()
        state = state.copy(history = emptyList(), historyError = null, historyClearing = true); publish()
        history.clearAsync { result -> publishHistory(ticket, result, clearing = true) }
    }
    private fun refreshHistory() {
        val ticket = historyPublication.next()
        history.readAsync { result -> publishHistory(ticket, result) }
    }
    private fun recordHistory(text: String, route: String, intent: String?, outcome: String?) {
        val ticket = historyPublication.next()
        history.recordAsync(text, route, intent, outcome) { result -> publishHistory(ticket, result) }
    }
    private fun publishHistory(ticket: Long, result: Result<List<com.khuongnd.dexassistant.ui.history.HistoryEntry>>, clearing: Boolean = false) = onMain {
        if (result.isFailure) android.util.Log.w("DexHistory", "stage=${if (clearing) "history_clear" else "history_update"} status=FAILED")
        historyPublication.publish(ticket) {
            result.fold(
                onSuccess = { state = state.copy(history = it, historyError = null, historyClearing = false) },
                onFailure = {
                    // Storage failure is independent of action success; never replace a newer command status.
                    state = state.copy(historyError = if (clearing) "Không thể xóa lịch sử lệnh" else "Không thể cập nhật lịch sử lệnh", historyClearing = false)
                },
            )
            publish()
        }
    }

    override fun onMetric(event: String, correlationId: String, elapsedNanos: Long) {
        if (correlationId != pendingId) return
        when (event) {
            "action_start" -> markActionStart()
            "ws.send" -> latency?.start("backendLatency", elapsedNanos)
            "ws.plan_received" -> latency?.end("backendLatency", elapsedNanos)
        }
    }
    private fun actionMetric(event: String, atNanos: Long) {
        when (event) {
            "path_semantic" -> latencyPath = "semantic"
            "path_fallback" -> latencyPath = "fallback"
            "action_start" -> markActionStart()
            "ui_resolution_start" -> latency?.start("uiResolutionLatency", atNanos)
            "ui_resolution_end" -> latency?.end("uiResolutionLatency", atNanos)
            "confirmation_start" -> {
                latency?.end("executionLatency", atNanos)
                latency?.start("confirmationWaitLatency", atNanos)
            }
            "confirmation_end" -> {
                latency?.end("confirmationWaitLatency", atNanos)
            }
            "execution_resume" -> {
                latency?.start("executionLatency", atNanos)
            }
        }
    }
    override fun onChatEvent(event: com.khuongnd.dexassistant.ui.protocol.ChatSession.Event) { onMain { chatObserver?.invoke(event) } }

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
    val dexStatus: String = "DeX: chưa kiểm tra",
    val accessibilityStatus: String = "Trợ năng: chưa kiểm tra",
    val voiceState: String = "STOPPED",
    val voiceDetail: String = "Not started",
    val assistantReadiness: String = "NOT_BOUND",
    val deviceOwner: Boolean = false,
    val capabilities: Map<String, String> = emptyMap(),
    val quota: ProtocolParser.QuotaSnapshot? = null,
    val quotaStale: Boolean = true,
    val history: List<com.khuongnd.dexassistant.ui.history.HistoryEntry> = emptyList(),
    val historyError: String? = null,
    val historyClearing: Boolean = false,
)
