package com.khuongnd.dexassistant.ui.protocol

import android.os.SystemClock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors

class AssistantWebSocket(
    private val listener: Listener,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build(),
    private val callbackExecutor: java.util.concurrent.Executor = MainCallbackExecutor,
) {
    private var generation = 0L
    private var disposed = false
    private var reconnectFuture: java.util.concurrent.ScheduledFuture<*>? = null
    private var socket: WebSocket? = null
    private var endpoint: String? = null
    private var token: String? = null
    private var reconnectAttempt = 0
    private var closedByUser = false
    private var lastRequest: PendingRequest? = null
    private var quotaRequestId: UUID? = null
    private val reconnectScheduler = Executors.newSingleThreadScheduledExecutor()

    @Synchronized
    fun connect(endpoint: String, token: String) {
        require(endpoint.startsWith("wss://") || isLoopback(endpoint)) {
            "Only WSS or loopback WS is allowed"
        }
        check(!disposed) { "WebSocket is closed" }
        generation++
        reconnectFuture?.cancel(false); reconnectFuture = null
        this.endpoint = endpoint
        this.token = token
        closedByUser = false
        listener.onState("CONNECTING")
        val previous = socket
        socket = null
        previous?.cancel()
        socket = client.newWebSocket(
            Request.Builder().url(endpoint).header("Authorization", "Bearer $token").build(),
            webSocketListener(),
        )
    }

    @Synchronized
    fun disconnect() {
        generation++
        reconnectFuture?.cancel(false); reconnectFuture = null
        closedByUser = true
        socket?.close(1000, "User disconnected")
        socket = null
        listener.onState("DISCONNECTED")
    }

    @Synchronized
    fun close() {
        disconnect()
        disposed = true
        reconnectScheduler.shutdownNow()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    @Synchronized
    fun sendCommand(text: String, requestId: UUID = UUID.randomUUID(), capabilities: JSONObject? = null): UUID {
        val request = PendingRequest(requestId, text, capabilities?.toString())
        lastRequest = request
        check(send(request)) { "WebSocket is not connected" }
        return requestId
    }

    @Synchronized
    fun retryLast(): Boolean {
        val request = lastRequest ?: return false
        return send(request)
    }

    @Synchronized
    fun sendActionResult(planId: String, actionId: String, status: String, errorCode: String?) {
        val payload = JSONObject()
            .put("planId", planId)
            .put("actionId", actionId)
            .put("status", status)
        if (errorCode != null) payload.put("errorCode", errorCode)
        sendEnvelope("action.result", UUID.randomUUID(), payload)
    }

    @Synchronized
    private fun send(request: PendingRequest): Boolean {
        val payload = JSONObject()
            .put("conversationId", "primary")
            .put("text", request.text)
            .put("locale", "vi-VN")
        request.capabilities?.let { payload.put("clientCapabilities", JSONObject(it)) }
        if (!sendEnvelope("command.request", request.id, payload)) {
            listener.onState("COMMAND_NOT_SENT")
            return false
        } else {
            listener.onMetric("ws.send", request.id.toString(), SystemClock.elapsedRealtimeNanos())
        }
        return true
    }

    @Synchronized
    private fun sendEnvelope(type: String, messageId: UUID, payload: JSONObject): Boolean {
        val envelope = JSONObject()
            .put("version", 1)
            .put("type", type)
            .put("messageId", messageId.toString())
            .put("correlationId", JSONObject.NULL)
            .put("sentAt", Instant.now().toString())
            .put("payload", payload)
        return socket?.send(envelope.toString()) == true
    }

    /** Validate identity at delivery, so queued old-socket events cannot affect a newer command. */
    private fun dispatchCallback(webSocket: WebSocket, callback: () -> Unit) {
        callbackExecutor.execute {
            synchronized(this) {
                if (!disposed && socket === webSocket) callback()
            }
        }
    }

    internal fun webSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            dispatchCallback(webSocket) {
                reconnectAttempt = 0
                quotaRequestId = UUID.randomUUID().also { sendEnvelope("quota.snapshot.request", it, JSONObject()) }
                listener.onState("CONNECTED")
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            dispatchCallback(webSocket) {
                runCatching {
                    when (ProtocolParser.type(text)) {
                        "execution.plan" -> {
                            listener.onMetric("ws.plan_received", "plan", SystemClock.elapsedRealtimeNanos())
                            listener.onCorrelatedActions(JSONObject(text).optString("correlationId"), ProtocolParser.actions(text))
                        }
                        "command.completed" -> {
                            val envelope = JSONObject(text)
                            listener.onState("COMPLETED")
                            listener.onCommandCompleted(envelope.optString("correlationId"), envelope.getJSONObject("payload").optString("status"))
                        }
                        "command.accepted" -> listener.onState("ACCEPTED")
                        "quota.snapshot" -> {
                            val envelope = JSONObject(text)
                            if (envelope.optString("correlationId") == quotaRequestId?.toString()) listener.onQuotaSnapshot(ProtocolParser.quotaSnapshot(text))
                        }
                        "error" -> {
                            val code = ProtocolParser.errorCode(text)
                            val correlationId = JSONObject(text).optString("correlationId")
                            if (code == "UNSUPPORTED_MESSAGE" && correlationId == quotaRequestId?.toString()) listener.onQuotaUnsupported()
                            else {
                                val message = if (code in AI_ERRORS) "Khó quá, không làm được" else ProtocolParser.errorSummary(text)
                                listener.onError(message)
                                listener.onCommandError(correlationId, code, message)
                            }
                        }
                    }
                }.onFailure { listener.onError("INVALID_MESSAGE: ${it.message}") }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            dispatchCallback(webSocket) {
                if (response?.code == 401) {
                    closedByUser = true
                    listener.onAuthenticationFailure()
                    return@dispatchCallback
            }
            listener.onState("RECONNECTING")
            listener.onDisconnected()
            scheduleReconnect()
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            dispatchCallback(webSocket) {
                if (!closedByUser) {
                    scheduleReconnect()
                    listener.onState("RECONNECTING")
                    if (socket === webSocket) listener.onDisconnected()
            }
            }
        }
    }

    @Synchronized
    private fun scheduleReconnect() {
        if (closedByUser || disposed) return
        reconnectFuture?.cancel(false)
        val expectedGeneration = generation
        val delay = minOf(30_000L, 1_000L shl minOf(reconnectAttempt++, 5))
        reconnectFuture = reconnectScheduler.schedule({
            synchronized(this@AssistantWebSocket) {
                if (closedByUser || disposed || generation != expectedGeneration) return@synchronized
                val savedEndpoint = endpoint ?: return@synchronized
                val savedToken = token ?: return@synchronized
                connect(savedEndpoint, savedToken)
            }
        }, delay, TimeUnit.MILLISECONDS)
    }

    interface Listener {
        fun onState(state: String)
        fun onActions(actions: List<DeviceAction>)
        fun onCorrelatedActions(requestId: String, actions: List<DeviceAction>) = onActions(actions)
        fun onCommandCompleted(requestId: String, status: String) = Unit
        fun onCommandError(requestId: String, code: String, message: String) = Unit
        fun onError(message: String)
        fun onAuthenticationFailure()
        fun onQuotaSnapshot(snapshot: ProtocolParser.QuotaSnapshot)
        fun onQuotaUnsupported()
        fun onDisconnected()
        fun onMetric(event: String, correlationId: String, elapsedNanos: Long)
    }

    private data class PendingRequest(val id: UUID, val text: String, val capabilities: String?)

    companion object {
        private val AI_ERRORS = setOf("AI_QUOTA_EXHAUSTED", "AI_TIMEOUT", "AI_UNAVAILABLE")
        internal fun isLoopback(endpoint: String): Boolean = runCatching {
            val uri = java.net.URI(endpoint)
            uri.scheme == "ws" && uri.userInfo == null && uri.host?.lowercase(java.util.Locale.ROOT) in setOf("127.0.0.1", "localhost")
        }.getOrDefault(false)
    }
}

private object MainCallbackExecutor : java.util.concurrent.Executor {
    private val handler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }
    override fun execute(command: Runnable) { handler.post(command) }
}
