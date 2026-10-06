package com.khuongnd.dexassistant.ui.protocol

import com.khuongnd.dexassistant.ui.voice.ChatVoiceRouting
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Opt-in host demo against the deterministic backend fixture, using the production WebSocket client. */
class ChatLiveDemoTest {
    private class Events : AssistantWebSocket.Listener {
        val states = LinkedBlockingQueue<String>()
        val chat = LinkedBlockingQueue<ChatSession.Event>()
        var commandActions = 0
        var commandErrors = 0
        var protocolErrors = 0
        override fun onState(state: String) { states.add(state) }
        override fun onActions(actions: List<DeviceAction>) { commandActions += actions.size }
        override fun onCommandError(requestId: String, code: String, message: String) { commandErrors++ }
        override fun onError(message: String) { commandErrors++ }
        override fun onAuthenticationFailure() { commandErrors++ }
        override fun onQuotaSnapshot(snapshot: ProtocolParser.QuotaSnapshot) = Unit
        override fun onQuotaUnsupported() = Unit
        override fun onDisconnected() = Unit
        override fun onMetric(event: String, correlationId: String, elapsedNanos: Long) = Unit
        override fun onChatEvent(event: ChatSession.Event) { chat.add(event) }
        override fun onChatProtocolError() { protocolErrors++ }
    }

    private fun Events.awaitState(expected: String) {
        repeat(5) { if (states.poll(5, TimeUnit.SECONDS) == expected) return }
        fail("No $expected state; commandErrors=$commandErrors")
    }

    private inline fun <reified T : ChatSession.Event> Events.awaitChat(): T {
        val event = chat.poll(15, TimeUnit.SECONDS)
        assertTrue("Expected ${T::class.simpleName}, got $event", event is T)
        return event as T
    }

    @Test fun threeTurnsThroughLiveChatWebSocketAndInjectedTtsCallback() {
        val endpoint = System.getenv("DEX_CHAT_DEMO_WS")
        val token = System.getenv("DEX_CHAT_DEMO_TOKEN")
        assumeTrue("Set DEX_CHAT_DEMO_WS and DEX_CHAT_DEMO_TOKEN to run live fixture demo",
            !endpoint.isNullOrBlank() && !token.isNullOrBlank())
        val events = Events()
        val client = AssistantWebSocket(events, callbackExecutor = java.util.concurrent.Executor { it.run() })
        val spoken = mutableListOf<String>()
        val injectedTts: (String, (Boolean) -> Unit) -> Unit = { text, complete ->
            spoken += text
            complete(true)
        }
        try {
            client.connect(endpoint!!, token!!)
            events.awaitState("CONNECTED")
            val session = client.startChat()
            assertEquals(session, events.awaitChat<ChatSession.Event.Started>().sessionId)

            val prompts = listOf(
                "Bây giờ là mấy giờ?",
                "Kể một câu chuyện phù hợp với thời gian hiện tại.",
                "Kể tiếp câu chuyện đó.",
            )
            val expectedReplies = listOf(
                "Bây giờ là 7 giờ sáng.",
                "Buổi sáng, bé An gặp một chú chim ở vườn.",
                "Chú chim dẫn bé An đến cây táo.",
            )
            prompts.forEachIndexed { index, prompt ->
                assertEquals(ChatVoiceRouting.Route.TURN, ChatVoiceRouting.route(prompt))
                val request = client.sendChatTurn(prompt)
                val reply = events.awaitChat<ChatSession.Event.Reply>()
                assertEquals(session, reply.sessionId)
                assertEquals(request, reply.requestId)
                assertEquals(index + 1, reply.turn)
                assertEquals(expectedReplies[index], reply.text)
                var playbackCompleted = false
                injectedTts(reply.text) { playbackCompleted = it }
                assertTrue("Injected TTS callback did not complete turn ${index + 1}", playbackCompleted)
                println("CHAT_DEMO turn=${reply.turn} session=$session request=$request reply=${reply.text} tts=INJECTED_CALLBACK_COMPLETE")
            }
            assertEquals(3, spoken.size)
            assertEquals(0, events.commandActions)
            assertEquals(0, events.commandErrors)
            assertEquals(0, events.protocolErrors)
            assertEquals(ChatVoiceRouting.Route.END, ChatVoiceRouting.route("Kết thúc trò chuyện"))
            client.endChat("USER")
            assertEquals(session, events.awaitChat<ChatSession.Event.Ended>().sessionId)
            assertThrows(IllegalStateException::class.java) { client.sendChatTurn("must not replay") }

            val cancelledSession = client.startChat()
            assertNotEquals(session, cancelledSession)
            assertEquals(cancelledSession, events.awaitChat<ChatSession.Event.Started>().sessionId)
            client.sendChatTurn("Lượt bị huỷ trước khi phát lại")
            client.disconnect()
            assertThrows(IllegalStateException::class.java) { client.sendChatTurn("must not replay") }
            // OkHttp close and server session cleanup are asynchronous.
            Thread.sleep(800)
            client.connect(endpoint, token)
            events.awaitState("CONNECTED")
            val reconnectedSession = client.startChat()
            assertNotEquals(cancelledSession, reconnectedSession)
            assertEquals(reconnectedSession, events.awaitChat<ChatSession.Event.Started>().sessionId)
            client.endChat("STOP")
            assertEquals(reconnectedSession, events.awaitChat<ChatSession.Event.Ended>().sessionId)
            assertEquals(0, events.commandActions)
            assertEquals(0, events.protocolErrors)
            println("CHAT_DEMO end=USER cancel=DISCONNECT reconnect=NEW_SESSION commandActions=0 transcriptStorage=NOT_INVOKED")
        } finally {
            client.close()
        }
    }
}
