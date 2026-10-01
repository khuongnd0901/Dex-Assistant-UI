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
) {
    private var socket: WebSocket? = null
    private var endpoint: String? = null
    private var token: String? = null
    private var reconnectAttempt = 0
    private var closedByUser = false
    private var lastRequest: PendingRequest? = null
    private val reconnectScheduler = Executors.newSingleThreadScheduledExecutor()

    fun connect(endpoint: String, token: String) {
        require(endpoint.startsWith("wss://") || isLoopback(endpoint)) {
            "Only WSS or loopback WS is allowed"
        }
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

    fun disconnect() {
        closedByUser = true
        socket?.close(1000, "User disconnected")
        socket = null
        listener.onState("DISCONNECTED")
    }

    fun close() {
        disconnect()
        reconnectScheduler.shutdownNow()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    fun sendCommand(text: String, requestId: UUID = UUID.randomUUID()): UUID {
        val request = PendingRequest(requestId, text)
        lastRequest = request
        send(request)
        return requestId
    }

    fun retryLast(): Boolean {
        val request = lastRequest ?: return false
        send(request)
        return true
    }

    fun sendActionResult(planId: String, actionId: String, status: String, errorCode: String?) {
        val payload = JSONObject()
            .put("planId", planId)
            .put("actionId", actionId)
            .put("status", status)
        if (errorCode != null) payload.put("errorCode", errorCode)
        sendEnvelope("action.result", UUID.randomUUID(), payload)
    }

    private fun send(request: PendingRequest) {
        val payload = JSONObject()
            .put("conversationId", "primary")
            .put("text", request.text)
            .put("locale", "vi-VN")
        if (!sendEnvelope("command.request", request.id, payload)) {
            listener.onState("COMMAND_NOT_SENT")
        } else {
            listener.onMetric("ws.send", request.id.toString(), SystemClock.elapsedRealtimeNanos())
        }
    }

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

    private fun webSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (socket !== webSocket) return
            reconnectAttempt = 0
            listener.onState("CONNECTED")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (socket !== webSocket) return
            runCatching {
                when (ProtocolParser.type(text)) {
                    "execution.plan" -> {
                        listener.onMetric("ws.plan_received", "plan", SystemClock.elapsedRealtimeNanos())
                        listener.onActions(ProtocolParser.actions(text))
                    }
                    "command.completed" -> listener.onState("COMPLETED")
                    "command.accepted" -> listener.onState("ACCEPTED")
                    "error" -> listener.onError(ProtocolParser.errorSummary(text))
                }
            }.onFailure { listener.onError("INVALID_MESSAGE: ${it.message}") }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (socket !== webSocket) return
            if (response?.code == 401) {
                closedByUser = true
                listener.onAuthenticationFailure()
                return
            }
            listener.onState("RECONNECTING")
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (socket !== webSocket) return
            if (!closedByUser) {
                listener.onState("RECONNECTING")
                scheduleReconnect()
            }
        }
    }

    private fun scheduleReconnect() {
        if (closedByUser) return
        val delay = minOf(30_000L, 1_000L shl minOf(reconnectAttempt++, 5))
        reconnectScheduler.schedule({
            if (closedByUser) return@schedule
            val savedEndpoint = endpoint ?: return@schedule
            val savedToken = token ?: return@schedule
            connect(savedEndpoint, savedToken)
        }, delay, TimeUnit.MILLISECONDS)
    }

    interface Listener {
        fun onState(state: String)
        fun onActions(actions: List<DeviceAction>)
        fun onError(message: String)
        fun onAuthenticationFailure()
        fun onMetric(event: String, correlationId: String, elapsedNanos: Long)
    }

    private data class PendingRequest(val id: UUID, val text: String)

    companion object {
        internal fun isLoopback(endpoint: String): Boolean =
            endpoint.startsWith("ws://127.0.0.1") || endpoint.startsWith("ws://localhost")
    }
}
