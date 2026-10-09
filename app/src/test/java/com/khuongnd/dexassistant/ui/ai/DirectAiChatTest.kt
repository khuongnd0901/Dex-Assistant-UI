package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.security.AiProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class DirectAiChatTest {
    @Test fun boundedSessionTracksTurnsAndKeepsContextInMemoryOnly() {
        val chat = DirectChatState()
        val first = chat.begin("Xin chào", ZonedDateTime.parse("2026-10-09T09:00:00+07:00"))
        assertTrue(first.previous.isEmpty())
        assertEquals(1, chat.complete(first, "Chào bạn"))
        val second = chat.begin("Bạn nhớ câu trước không?")
        assertEquals(1, second.previous.size)
        assertEquals("Xin chào", second.previous.single().user)
        assertEquals(2, chat.complete(second, "Có"))
        chat.clear()
        assertTrue(chat.begin("Bắt đầu lại").previous.isEmpty())
    }

    @Test fun retainedTurnsStayBounded() {
        val chat = DirectChatState()
        repeat(35) {
            val turn = chat.begin("Câu $it")
            chat.complete(turn, "Đáp $it")
        }
        assertEquals(20, chat.begin("Tiếp").previous.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectCredentialLikeChatInputWithoutSendingProviderRequest() {
        DirectChatState().begin("api key abc123")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectOversizedChatInput() {
        DirectChatState().begin("a".repeat(2001))
    }

    @Test fun parseGeminiMultiturnTextAndGroqTextWithNoTools() {
        val gemini = JSONObject("""{"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"Xin "},{"text":"chào"}]}}]}""")
        val groq = JSONObject("""{"choices":[{"message":{"role":"assistant","content":"Xin chào"}}]}""")
        assertEquals("Xin chào", DirectAiChat.parse(AiProvider.GEMINI, gemini))
        assertEquals("Xin chào", DirectAiChat.parse(AiProvider.GROQ, groq))
    }

    @Test(expected = MobileAiException::class)
    fun rejectGeminiFunctionCallInChatReply() {
        val response = JSONObject("""{"candidates":[{"finishReason":"STOP","content":{"parts":[{"functionCall":{"name":"deviceCommand_WIFI_SET","args":{"enabled":true}}}]}}]}""")
        DirectAiChat.parse(AiProvider.GEMINI, response)
    }

    @Test(expected = MobileAiException::class)
    fun rejectGroqToolCallInChatReply() {
        val response = JSONObject("""{"choices":[{"message":{"tool_calls":[{"type":"function"}],"content":"ok"}}]}""")
        DirectAiChat.parse(AiProvider.GROQ, response)
    }

    @Test(expected = MobileAiException::class)
    fun rejectGeminiTruncation() {
        val response = JSONObject("""{"candidates":[{"finishReason":"MAX_TOKENS","content":{"parts":[{"text":"partial"}]}}]}""")
        DirectAiChat.parse(AiProvider.GEMINI, response)
    }
}
