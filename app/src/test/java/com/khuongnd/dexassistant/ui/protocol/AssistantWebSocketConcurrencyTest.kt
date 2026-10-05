package com.khuongnd.dexassistant.ui.protocol

import okhttp3.Request
import okhttp3.WebSocket
import okio.ByteString
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executor

class AssistantWebSocketConcurrencyTest {
    private class Socket : WebSocket {
        override fun request() = Request.Builder().url("https://localhost").build()
        override fun queueSize() = 0L
        override fun send(text: String) = true
        override fun send(bytes: ByteString) = true
        override fun close(code: Int, reason: String?) = true
        override fun cancel() = Unit
    }
    private class Events : AssistantWebSocket.Listener {
        val states = mutableListOf<String>()
        override fun onState(state: String) { states += state }
        override fun onActions(actions: List<DeviceAction>) = Unit
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

}
