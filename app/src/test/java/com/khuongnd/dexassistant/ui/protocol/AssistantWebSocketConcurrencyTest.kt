package com.khuongnd.dexassistant.ui.protocol

import okhttp3.Request
import okhttp3.WebSocket
import okio.ByteString
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executor

class AssistantWebSocketConcurrencyTest {
    private class Socket : WebSocket {
        val sent = mutableListOf<String>()
        override fun request() = Request.Builder().url("https://localhost").build()
        override fun queueSize() = 0L
        override fun send(text: String): Boolean { sent += text; return true }
        override fun send(bytes: ByteString) = true
        override fun close(code: Int, reason: String?) = true
        override fun cancel() = Unit
    }
    private class Events : AssistantWebSocket.Listener {
        val states = mutableListOf<String>()
        val chat = mutableListOf<ChatSession.Event>()
        var actions = 0
        override fun onState(state: String) { states += state }
        override fun onActions(actions: List<DeviceAction>) { this.actions += actions.size }
        override fun onChatEvent(event: ChatSession.Event) { chat += event }
        override fun onError(message: String) = Unit
        override fun onAuthenticationFailure() = Unit
        override fun onQuotaSnapshot(snapshot: ProtocolParser.QuotaSnapshot) = Unit
        override fun onQuotaUnsupported() = Unit
        override fun onDisconnected() = Unit
        override fun onMetric(event: String, correlationId: String, elapsedNanos: Long) = Unit
    }
    private fun install(client: AssistantWebSocket, socket: WebSocket) {
        AssistantWebSocket::class.java.getDeclaredField("socket").apply { isAccessible = true }.set(client, socket)
    }
    @Test fun queuedOldSocketFailureDoesNotDisconnectReplacement() {
        val queued = mutableListOf<Runnable>()
        val events = Events()
        val client = AssistantWebSocket(events, callbackExecutor = Executor { queued += it })
        try {
            val old = Socket()
            install(client, old)
            client.webSocketListener().onFailure(old, IllegalStateException("old connection failed"), null)
            assertTrue(events.states.isEmpty())
            install(client, Socket())
            queued.single().run()
            assertTrue(events.states.isEmpty())
        } finally { client.close() }
    }
    @Test fun disconnectDropsAlreadyQueuedNetworkMessage() {
        val queued = mutableListOf<Runnable>()
        val events = Events()
        val client = AssistantWebSocket(events, callbackExecutor = Executor { queued += it })
        try {
            val socket = Socket()
            install(client, socket)
            client.webSocketListener().onMessage(socket, """{"type":"command.accepted"}""")
            client.disconnect()
            queued.single().run()
            assertEquals(listOf("DISCONNECTED"), events.states)
        } finally { client.close() }
    }
    @Test fun plaintextExceptionRequiresExactLoopbackHost() {
        assertTrue(AssistantWebSocket.isLoopback("ws://localhost:8080/ws/v1/assistant"))
        assertTrue(AssistantWebSocket.isLoopback("ws://127.0.0.1:8080/ws/v1/assistant"))
        assertFalse(AssistantWebSocket.isLoopback("ws://localhost.evil.example/ws"))
        assertFalse(AssistantWebSocket.isLoopback("ws://127.0.0.10/ws"))
        assertFalse(AssistantWebSocket.isLoopback("ws://localhost@evil.example/ws"))
    }

    @Test fun chatFramesStayOutOfCommandCallbacksAndDisconnectInvalidatesTurn() {
        val events = Events()
        val client = AssistantWebSocket(events, callbackExecutor = Executor { it.run() })
        val socket = Socket()
        install(client, socket)
        try {
            val session = client.startChat()
            val startId = org.json.JSONObject(socket.sent.last()).getString("messageId")
            fun incoming(type: String, correlation: String, payload: org.json.JSONObject) = org.json.JSONObject()
                .put("version", 1).put("type", type).put("messageId", java.util.UUID.randomUUID().toString())
                .put("correlationId", correlation).put("sentAt", "2026-10-06T00:00:01Z")
                .put("payload", payload.put("sessionId", session.toString())).toString()
            client.webSocketListener().onMessage(socket, incoming("chat.session.started", startId, org.json.JSONObject().put("idleTimeoutSeconds", 900)))
            assertEquals(1, events.chat.size)
            val turnId = client.sendChatTurn("Kể chuyện")
            assertEquals("chat.turn.request", org.json.JSONObject(socket.sent.last()).getString("type"))
            client.webSocketListener().onMessage(socket, incoming("chat.turn.response", turnId.toString(), org.json.JSONObject().put("text", "Có một lần...").put("turn", 1)))
            assertEquals(2, events.chat.size)
            assertEquals(0, events.actions)
            assertTrue(events.states.isEmpty())
            val next = client.sendChatTurn("Tiếp đi")
            client.disconnect()
            client.webSocketListener().onMessage(socket, incoming("chat.turn.response", next.toString(), org.json.JSONObject().put("text", "late").put("turn", 2)))
            assertEquals(2, events.chat.size)
            assertThrows(IllegalStateException::class.java) { client.sendChatTurn("replay") }
        } finally { client.close() }
    }

}
