package com.khuongnd.dexassistant.ui.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class DirectChatStateTest {
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

    @Test fun idleExpiryOccursExactlyAtFifteenMinutesAndClearResetsActivity() {
        val chat = DirectChatState()
        assertFalse(chat.expired(Long.MAX_VALUE))
        val turn = chat.begin("Xin chào")
        chat.complete(turn, "Chào bạn")
        val activity = chat.lastActivityMillis
        val timeout = 15 * 60 * 1000L
        assertFalse(chat.expired(activity + timeout - 1))
        assertTrue(chat.expired(activity + timeout))
        assertTrue(chat.expired(activity + timeout + 1))
        chat.clear()
        assertFalse(chat.expired(activity + timeout))
        assertTrue(chat.begin("Phiên mới").previous.isEmpty())
    }

    @Test fun idleExpiryStartsWhenSessionOpensBeforeAnyTurn() {
        val chat = DirectChatState()
        val openedAt = 10_000L
        val timeout = 15 * 60 * 1000L
        chat.start(openedAt)
        assertEquals(openedAt, chat.lastActivityMillis)
        assertFalse(chat.expired(openedAt + timeout - 1))
        assertTrue(chat.expired(openedAt + timeout))
        chat.clear()
        assertFalse(chat.expired(openedAt + timeout))
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

}
