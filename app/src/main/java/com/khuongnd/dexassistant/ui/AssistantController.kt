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
import com.khuongnd.dexassistant.ui.routing.CommandPrivacy
import com.khuongnd.dexassistant.ui.routing.LocalCommandParser
import com.khuongnd.dexassistant.ui.routing.CapabilityRegistry
import com.khuongnd.dexassistant.ui.execution.ActionRouter
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import com.khuongnd.dexassistant.ui.routing.CommandRouter
import com.khuongnd.dexassistant.ui.routing.Route
import com.khuongnd.dexassistant.ui.security.AiCredentialStore
import com.khuongnd.dexassistant.ui.security.AiProviderSettings
import com.khuongnd.dexassistant.ui.ai.MobileAiProxy
import com.khuongnd.dexassistant.ui.ai.MobileAiException
import com.khuongnd.dexassistant.ui.ai.DirectChatState
import com.khuongnd.dexassistant.ui.ai.DirectAiChat
import com.khuongnd.dexassistant.ui.routing.TypedAction
import com.khuongnd.dexassistant.ui.voice.VoiceAssistantService
import java.util.UUID
import com.khuongnd.dexassistant.ui.screen.DexAccessibilityService
import com.khuongnd.dexassistant.ui.screen.ScreenCommandParser
import com.khuongnd.dexassistant.ui.latency.RequestLatency

data class VoiceCommandResult(val requestId: String, val status: String, val message: String)

/** Activity facade; closing a screen only detaches its observer. */
class AssistantController(private val contextForVoice: Context, private val stateChanged: (UiState) -> Unit) {
    private val core = shared(contextForVoice)
    fun start() { core.addObserver(stateChanged); core.start() }
    fun stop() { core.removeObserver(stateChanged) }
    fun observeVoiceHealth() = core.observeVoiceHealth()
    fun stopObservingVoiceHealth() = Unit
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

class AssistantCoordinator internal constructor(
    private val context: Context,
    private val history: CommandHistoryStore = CommandHistoryStore(context),
    private val chatClient: okhttp3.OkHttpClient? = null,
    private val chatCredentials: AiCredentialStore = AiCredentialStore(context),
    private val chatIdleTimeoutMillis: Long = 15 * 60 * 1000L,
    private val latencySink: (String) -> Unit = { android.util.Log.i("DexLatency", it) },
) {
    private val observers = linkedSetOf<(UiState) -> Unit>()
    private var started = false
    private val commandGate = CommandSessionGate()
    private val pendingId: String? get() = commandGate.currentId
    private var pendingProgress: ((String, String) -> Unit)? = null
    private var pendingInteractionPrompt: ((ActionRouter.Prompt) -> Unit)? = null
    private var activeText = ""
    private var pendingCompletion: ((VoiceCommandResult) -> Unit)? = null
    private var pendingDispatched = false
    private var directDispatch = false
    private val aiSettings = AiProviderSettings(context)
    private var directProviderSnapshot: Map<String, *>? = null
    private var chatProviderSnapshot: Map<String, *>? = null
    private val providerListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        onMain {
            if (chatSession != null) endChat("STOP")
            if (pendingId != null && directDispatch) finish("CANCELLED", "Đã đổi cấu hình AI")
        }
    }
    private val commandSubscription by lazy { com.khuongnd.dexassistant.ui.ai.ChatGptSubscriptionProvider(context, aiSettings) }
    private val chatSubscription by lazy { com.khuongnd.dexassistant.ui.ai.ChatGptSubscriptionProvider(context, aiSettings) }
    private val aiProxyDelegate = lazy { MobileAiProxy(AiCredentialStore(context), aiSettings, subscription = commandSubscription) }
    private val aiProxy by aiProxyDelegate
    private val directExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { task ->
        Thread(task, "dex-direct-ai").apply { isDaemon = true }
    }
    private var directTask: java.util.concurrent.Future<*>? = null
    private var remoteDispatch = false
    private var latency: RequestLatency? = null
    private var latencyPath: String
        get() = latency?.path ?: "local"
        set(value) { latency?.path = value }
    private var speechEndAtNanos: Long? = null
    private fun markActionStart() {
        val now = SystemClock.elapsedRealtimeNanos()
        latency?.end("finalToActionStartLatency", now)
        val start = speechEndAtNanos ?: return
        speechEndAtNanos = null
        if (now >= start) latencyEvent("speechEndToActionStartLatency", (now - start) / 1_000)
    }
    private fun latencyEvent(name: String, micros: Long) {
        latency?.metric(name, micros)
    }
    private fun safeReason(value: String): String = if (value.matches(Regex("[A-Z][A-Z0-9_]{0,63}"))) value else "OTHER"
    private val executedActions = mutableSetOf<String>()
    @Volatile private var chatObserver: ((com.khuongnd.dexassistant.ui.protocol.ChatSession.Event) -> Unit)? = null
    fun observeChat(observer: ((com.khuongnd.dexassistant.ui.protocol.ChatSession.Event) -> Unit)?) { chatObserver = observer }
    private val directChatState = DirectChatState()
    private val directChat by lazy {
        chatClient?.let { DirectAiChat(chatCredentials, aiSettings, it, chatSubscription) }
            ?: DirectAiChat(chatCredentials, aiSettings, subscription = chatSubscription)
    }
    private val chatExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { task ->
        Thread(task, "dex-chat-ai").apply { isDaemon = true }
    }
    private var chatSession: UUID? = null
    private var chatTurn: UUID? = null
    private var chatTask: java.util.concurrent.Future<*>? = null
    private var chatEpoch = 0L
    private var chatLatency: RequestLatency? = null
    private var chatIdleObserver: (() -> Unit)? = null
    internal fun observeChatIdleExpiry(observer: (() -> Unit)?) { chatIdleObserver = observer }
    private val chatIdleExpiry: Runnable = Runnable {
        val active = chatSession != null
        endChat("IDLE")
        if (active && chatSession == null) chatIdleObserver?.invoke()
    }
    private fun rearmChatIdleExpiry() {
        main.removeCallbacks(chatIdleExpiry)
        main.postDelayed(chatIdleExpiry, chatIdleTimeoutMillis)
    }

    /** Android owns the chat session; no WebSocket, persistence, or device action tools. */
    fun startChat() = onMain {
        if (chatSession != null) endChat("STOP")
        val session = UUID.randomUUID()
        val preferred = aiSettings.defaultProvider()
        val available = if (preferred == com.khuongnd.dexassistant.ui.security.AiProvider.CHATGPT_SUBSCRIPTION)
            chatSubscription.ready() else preferred.supportsDirectCommands && aiSettings.enabled(preferred) &&
            aiSettings.freeTierConfirmed(preferred) && aiSettings.model(preferred).isNotBlank() &&
            chatCredentials.hasKey(preferred)
        if (!available) {
            chatObserver?.invoke(com.khuongnd.dexassistant.ui.protocol.ChatSession.Event.Error(
                null, UUID.randomUUID(), "CHAT_PROVIDER_UNAVAILABLE",
                if (preferred == com.khuongnd.dexassistant.ui.security.AiProvider.CHATGPT_SUBSCRIPTION)
                    "Kiểm tra kết nối ChatGPT và hai phép thử cho model trong Settings"
                else "Cấu hình provider/model Free Tier trong Settings", false))
        } else {
            chatProviderSnapshot = aiSettings.routingSnapshot()
            chatSession = session; chatEpoch++
            directChatState.start()
            rearmChatIdleExpiry()
            chatObserver?.invoke(com.khuongnd.dexassistant.ui.protocol.ChatSession.Event.Started(session, 900))
        }
    }

    fun sendChat(text: String, voiceEpoch: Long? = null, finalAcceptedNanos: Long? = null) = onMain {
        val acceptedAt = finalAcceptedNanos ?: SystemClock.elapsedRealtimeNanos()
        val requestId = UUID.randomUUID()
        val trace = RequestLatency(requestId.toString(), latencySink).also {
            it.path = "chat"; it.resolveOrigin("chat"); it.stage("admitted", voiceEpoch)
            it.start("chatReplyLatency", acceptedAt)
        }
        val session = chatSession
        if (session == null || directChatState.expired() || chatProviderSnapshot != aiSettings.routingSnapshot()) {
            endChat("STOP")
            chatObserver?.invoke(com.khuongnd.dexassistant.ui.protocol.ChatSession.Event.Error(
                session, requestId, "CHAT_INVALID_SESSION", "Chat đã kết thúc", false))
            trace.terminal("CHAT_INVALID_SESSION")
            return@onMain
        }
        if (chatTurn != null) {
            chatObserver?.invoke(com.khuongnd.dexassistant.ui.protocol.ChatSession.Event.Error(
                session, requestId, "CHAT_TURN_IN_PROGRESS", "Đang trả lời", false))
            trace.terminal("CHAT_TURN_IN_PROGRESS")
            return@onMain
        }
        val turn = runCatching { directChatState.begin(text) }.getOrElse {
            val code = when (it.message) {
                "CHAT_SENSITIVE_INPUT_REJECTED" -> "CHAT_SENSITIVE_INPUT_REJECTED"
                "CHAT_TURN_LIMIT" -> "CHAT_TURN_LIMIT"
                "CHAT_CONTEXT_LIMIT" -> "CHAT_CONTEXT_LIMIT"
                else -> "CHAT_INPUT_TOO_LARGE"
            }
            chatObserver?.invoke(com.khuongnd.dexassistant.ui.protocol.ChatSession.Event.Error(
                session, requestId, code, code, false))
            trace.terminal(code)
            return@onMain
        }
        rearmChatIdleExpiry()
        chatTurn = requestId
        val epoch = chatEpoch
        trace.start("providerQueueLatency", SystemClock.elapsedRealtimeNanos())
        chatLatency = trace
        chatTask = chatExecutor.submit {
            trace.end("providerQueueLatency", SystemClock.elapsedRealtimeNanos())
            val result = runCatching { directChat.reply(turn, trace::metric) }
            onMain {
                if (chatSession != session || chatEpoch != epoch || chatTurn != requestId) return@onMain
                if (chatProviderSnapshot != aiSettings.routingSnapshot()) { endChat("STOP"); return@onMain }
                chatTurn = null; chatTask = null; chatLatency = null
                result.fold(onSuccess = { reply ->
                    val count = runCatching { directChatState.complete(turn, reply) }.getOrElse {
                        chatObserver?.invoke(com.khuongnd.dexassistant.ui.protocol.ChatSession.Event.Error(
                            session, requestId, "CHAT_INVALID_OUTPUT", "Phản hồi không hợp lệ", false))
                        trace.terminal("CHAT_INVALID_OUTPUT")
                        return@fold
                    }
                    rearmChatIdleExpiry()
                    trace.end("chatReplyLatency", SystemClock.elapsedRealtimeNanos())
                    trace.terminal("REPLIED")
                    chatObserver?.invoke(com.khuongnd.dexassistant.ui.protocol.ChatSession.Event.Reply(
                        session, requestId, reply, count))
                }, onFailure = { error ->
                    val code = when ((error as? MobileAiException)?.code) {
                        "AI_TIMEOUT", "CHAT_PROVIDER_TIMEOUT" -> "CHAT_PROVIDER_TIMEOUT"
                        "CHAT_INVALID_OUTPUT" -> "CHAT_INVALID_OUTPUT"
                        else -> "CHAT_PROVIDER_UNAVAILABLE"
                    }
                    chatObserver?.invoke(com.khuongnd.dexassistant.ui.protocol.ChatSession.Event.Error(
                        session, requestId, code, code, code == "CHAT_PROVIDER_TIMEOUT"))
                    trace.terminal(code)
                })
            }
        }
    }

    fun endChat(reason: String = "USER"): Unit = onMain {
        main.removeCallbacks(chatIdleExpiry)
        val session = chatSession ?: return@onMain
        chatEpoch++
        chatLatency?.terminal("CANCELLED"); chatLatency = null
        chatTask?.cancel(true); chatTask = null
        directChat.cancelPendingRequest()
        chatTurn = null; chatSession = null; directChatState.clear()
        chatObserver?.invoke(com.khuongnd.dexassistant.ui.protocol.ChatSession.Event.Ended(session))
    }
    private val watchdog = Runnable { finish("ACTION_TIMEOUT", "Hết thời gian xử lý lệnh") }
    private val commandDeadline = Runnable { finish("COMMAND_TIMEOUT", "Hết thời gian xử lý lệnh") }
    fun addObserver(observer: (UiState) -> Unit) { observers.add(observer); observer(state) }
    fun removeObserver(observer: (UiState) -> Unit) { observers.remove(observer) }
    fun dispatchVoice(text: String, progress: ((String, String) -> Unit)? = null, interaction: ((ActionRouter.Prompt) -> Unit)? = null, speechEndNanos: Long? = null, finalAcceptedNanos: Long? = null, completion: (VoiceCommandResult) -> Unit): String {
        val id = UUID.randomUUID().toString()
        val acceptedAt = finalAcceptedNanos ?: SystemClock.elapsedRealtimeNanos()
        val trace = RequestLatency(id, latencySink)
        trace.stage("admitted")
        trace.start("firstStatusLatency", acceptedAt)
        trace.start("finalToActionStartLatency", acceptedAt)
        commandGate.enqueue(id)
        // Always enqueue so callers can retain the returned correlation ID before completion.
        main.post {
            val admitted = commandGate.beginQueued(id)
            if (admitted == null) { trace.terminal("CANCELLED"); completion(VoiceCommandResult(id, "CANCELLED", "Đã dừng")) }
            else if (!admitted) { trace.terminal("COMMAND_IN_PROGRESS"); completion(VoiceCommandResult(id, "COMMAND_IN_PROGRESS", "Đang xử lý lệnh trước")) }
            else {
                pendingCompletion = completion; pendingProgress = progress; pendingInteractionPrompt = interaction; pendingDispatched = false; activeText = text
                directDispatch = false; remoteDispatch = false; executedActions.clear()
                latency = trace
                speechEndAtNanos = speechEndNanos
                progress?.invoke(id, "ROUTING")
                if (progress != null) trace.end("firstStatusLatency", SystemClock.elapsedRealtimeNanos())
                if (pendingId != id) return@post
                main.postDelayed(watchdog, 30_000)
                main.postDelayed(commandDeadline, 120_000)
                if (text.isBlank()) finish("NO_SPEECH", "Chưa nghe rõ lệnh") else send(text)
            }
        }
        return id
    }
    /** Account/provider edits stop only AI work; deterministic local execution remains independent. */
    fun aiConnectionChanged() = onMain {
        if (chatSession != null) endChat("STOP")
        if (pendingId != null && directDispatch) finish("CANCELLED", "Đã đổi kết nối AI")
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
        recordHistory(activeText, if (directDispatch) Route.BACKEND_REQUIRED.name else Route.LOCAL.name, null, "CANCELLED")
        pendingCompletion = null
        finish("CANCELLED", "Đã hủy lệnh trước")
        return false
    }
    private fun finish(status: String, message: String) {
        val id = pendingId ?: return
        actionRouter.cancel()
        directTask?.cancel(true)
        if (directDispatch && aiProxyDelegate.isInitialized()) aiProxy.cancelPendingRequest()
        directTask = null
        latency?.end("executionLatency", SystemClock.elapsedRealtimeNanos())
        latency?.terminal(status)
        latency = null
        speechEndAtNanos = null
        val callback = pendingCompletion
        commandGate.finish(id); pendingCompletion = null; pendingProgress = null; pendingInteractionPrompt = null; main.removeCallbacks(watchdog); main.removeCallbacks(commandDeadline)
        if (directDispatch) recordHistory(activeText, Route.BACKEND_REQUIRED.name, null, status)
        callback?.invoke(VoiceCommandResult(id, status, message))
    }
    private val main = Handler(Looper.getMainLooper())
    private val voicePreferences = context.getSharedPreferences("voice_health", Context.MODE_PRIVATE)
    private val voiceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refreshVoiceHealth() }
    private val dpc = DpcClient(context)
    private val commandRegistry = CommandRegistry.load(context)
    private val parser = LocalCommandParser(commandRegistry)
    private val router = CommandRouter(parser, CapabilityRegistry(context))
    private val localExecutor = LocalActionExecutor(context, dpc, commandRegistry::appPackage)
    private val actionRouter = ActionRouter(context, commandRegistry, localExecutor)
    private val historyPublication = com.khuongnd.dexassistant.ui.history.HistoryPublicationGate()
    private var state = UiState()

    fun start() {
        if (started) return
        started = true
        aiSettings.observeRouting(providerListener)
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

    fun send(text: String) {
        if (text.isBlank()) return
        if (pendingId != null) {
            if (pendingDispatched) { update("COMMAND_IN_PROGRESS", "Đang xử lý lệnh trước"); return }
            pendingDispatched = true
        } else { dispatchVoice(text) { }; return }
        latency?.start("routeLatency", SystemClock.elapsedRealtimeNanos())
        val screenCommand = router.screenCommand(text, actionRouter.foregroundAppId())
        if (screenCommand != null) {
            latency?.resolveOrigin("local")
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
            else service.execute(screenCommand, { if (pendingId == screenRequestId) markActionStart() }, complete)
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
        latency?.resolveOrigin(if (routed.route == Route.BACKEND_REQUIRED) "direct" else "local")
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
            Route.BACKEND_REQUIRED -> {
                directDispatch = true
                directProviderSnapshot = aiSettings.routingSnapshot()
                latencyPath = "direct"
                val requestId = activeRequestId ?: return
                pendingProgress?.invoke(requestId, "SENDING")
                state = state.copy(lastRequestId = requestId, message = "Direct AI planning"); publish()
                val trace = latency
                trace?.start("providerQueueLatency", SystemClock.elapsedRealtimeNanos())
                directTask = directExecutor.submit {
                    trace?.end("providerQueueLatency", SystemClock.elapsedRealtimeNanos())
                    val result = runCatching { aiProxy.plan(text, commandRegistry, actionRouter.supportedActions(), { name, micros -> trace?.metric(name, micros) }) }
                    onMain {
                        if (pendingId != requestId || !directDispatch) return@onMain
                        result.fold(
                            onSuccess = { actions ->
                                if (actions.isEmpty()) finish("UNSUPPORTED_COMMAND", "Lệnh chưa được hỗ trợ")
                                else {
                                    pendingProgress?.invoke(requestId, "EXECUTING")
                                    executeDirectActions(requestId, actions, 0)
                                }
                            },
                            onFailure = { throwable ->
                                val code = (throwable as? MobileAiException)?.code ?: "AI_UNAVAILABLE"
                                finish(code, "Khó quá, không làm được")
                            },
                        )
                    }
                }
            }
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

    /** Direct AI plans use exactly the existing guarded ActionRouter, without WebSocket receipts. */
    private fun executeDirectActions(requestId: String, actions: List<TypedAction>, index: Int) {
        if (pendingId != requestId || !directDispatch) return
        if (directProviderSnapshot != aiSettings.routingSnapshot()) {
            finish("CANCELLED", "Đã đổi cấu hình AI"); return
        }
        if (index == actions.size) {
            finish(if (remoteDispatch) "DISPATCHED" else "VERIFIED",
                if (remoteDispatch) "Đã gửi yêu cầu thực hiện" else "Đã thực hiện")
            return
        }
        val action = actions[index]
        val actionId = requestId + ":" + index
        if (!executedActions.add(actionId)) { finish("INVALID_LLM_OUTPUT", "Kế hoạch chưa hợp lệ"); return }
        if (action.action.name == "CLICK_TARGET") latencyPath = "semantic"
        latency?.start("executionLatency", SystemClock.elapsedRealtimeNanos())
        actionRouter.execute(requestId, action, pendingInteractionPrompt, actionId, metric = ::actionMetric) { result ->
            onMain {
                if (pendingId != requestId || !directDispatch) return@onMain
                latency?.end("executionLatency", SystemClock.elapsedRealtimeNanos())
                when (result.outcome.name) {
                    "VERIFIED", "DISPATCHED" -> {
                        if (result.outcome.name == "DISPATCHED") remoteDispatch = true
                        executeDirectActions(requestId, actions, index + 1)
                    }
                    else -> finish(result.errorCode ?: result.outcome.name, "Không thực hiện được lệnh")
                }
            }
        }
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

    private fun update(status: String, message: String) {
        state = state.copy(status = status, message = message)
        publish()
        if (status in setOf("UNSUPPORTED", "FAILED", "SEND_FAILED", "AUTHENTICATION_FAILED")) finish(status, message)
    }

    private fun publish() {
        observers.toList().forEach { it(state) }
        if (observers.isNotEmpty()) latency?.end("firstStatusLatency", SystemClock.elapsedRealtimeNanos())
    }
    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }
}

data class UiState(
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
    val history: List<com.khuongnd.dexassistant.ui.history.HistoryEntry> = emptyList(),
    val historyError: String? = null,
    val historyClearing: Boolean = false,
)
