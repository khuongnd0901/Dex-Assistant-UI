package com.khuongnd.dexassistant.ui.protocol

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime
import java.util.UUID

class ChatSessionTest {
    private fun reply(type: String, correlation: UUID, payload: JSONObject, session: UUID? = null) =
        JSONObject().put("version", 1).put("type", type).put("messageId", UUID.randomUUID().toString())
            .put("correlationId", correlation.toString()).put("sentAt", "2026-10-06T00:00:01Z")
            .put("payload", payload.apply { if (session != null) put("sessionId", session.toString()) }).toString()

    @Test fun startTurnEndCorrelateAndRejectLateReply() {
        val chat = ChatSession()
        val start = chat.start()
        val session = UUID.fromString(start.payload.getString("sessionId"))
        assertThrows(IllegalStateException::class.java) { chat.turn("too early") }
        assertNull(chat.accept(reply("chat.session.started", UUID.randomUUID(), JSONObject().put("idleTimeoutSeconds", 900), session)))
        assertEquals(ChatSession.Event.Started(session, 900), chat.accept(reply("chat.session.started", start.id, JSONObject().put("idleTimeoutSeconds", 900), session)))
        val turn = chat.turn("Bây giờ là mấy giờ?", ZonedDateTime.parse("2026-10-06T07:00:02+07:00[Asia/Ho_Chi_Minh]"))
        assertEquals(session.toString(), turn.payload.getString("sessionId"))
        assertEquals("Asia/Ho_Chi_Minh", turn.payload.getJSONObject("localTime").getString("timeZone"))
        assertThrows(IllegalStateException::class.java) { chat.turn("overlap") }
        val answer = reply("chat.turn.response", turn.id, JSONObject().put("text", "7 giờ sáng").put("turn", 1), session)
        assertNull(chat.accept(reply("chat.turn.response", UUID.randomUUID(), JSONObject().put("text", "wrong").put("turn", 1), session)))
        assertEquals(ChatSession.Event.Reply(session, turn.id, "7 giờ sáng", 1), chat.accept(answer))
        assertNull(chat.accept(answer))
        val next = chat.turn("Kể chuyện")
        val end = chat.end("USER")!!
        assertNull(chat.accept(reply("chat.turn.response", next.id, JSONObject().put("text", "late").put("turn", 2), session)))
        val secondStart = chat.start()
        val secondSession = UUID.fromString(secondStart.payload.getString("sessionId"))
        assertNotEquals(session, secondSession)
        assertNull(chat.accept(reply("chat.session.ended", end.id, JSONObject(), session)))
        assertEquals(ChatSession.Event.Started(secondSession, 900), chat.accept(reply("chat.session.started", secondStart.id, JSONObject().put("idleTimeoutSeconds", 900), secondSession)))
    }

    @Test fun errorClearsPendingRequestAndInvalidationBlocksOldGeneration() {
        val chat = ChatSession()
        val start = chat.start(); val session = UUID.fromString(start.payload.getString("sessionId"))
        chat.accept(reply("chat.session.started", start.id, JSONObject().put("idleTimeoutSeconds", 900), session))
        val turn = chat.turn("hi")
        val error = reply("chat.error", turn.id, JSONObject().put("code", "CHAT_PROVIDER_TIMEOUT")
            .put("message", "Timed out").put("retryable", true), session)
        assertEquals(ChatSession.Event.Error(session, turn.id, "CHAT_PROVIDER_TIMEOUT", "Timed out", true), chat.accept(error))
        assertNull(chat.accept(error))
        val next = chat.turn("new id")
        assertNotEquals(turn.id, next.id)
        chat.invalidate()
        assertNull(chat.accept(reply("chat.turn.response", next.id, JSONObject().put("text", "stale").put("turn", 1), session)))
        assertThrows(IllegalStateException::class.java) { chat.turn("after disconnect") }
    }

    @Test fun rejectsMalformedChatAndUnicodeLimit() {
        val chat = ChatSession()
        val start = chat.start(); val session = UUID.fromString(start.payload.getString("sessionId"))
        assertThrows(IllegalArgumentException::class.java) { chat.accept(reply("chat.session.started", start.id, JSONObject().put("idleTimeoutSeconds", 900).put("actions", JSONObject()), session)) }
        assertThrows(Exception::class.java) { chat.accept("""{"version":1,"version":1,"type":"chat.session.started"}""") }
        chat.accept(reply("chat.session.started", start.id, JSONObject().put("idleTimeoutSeconds", 900), session))
        assertEquals(2_000, "😀".repeat(2_000).codePointCount(0, "😀".repeat(2_000).length))
        chat.turn("😀".repeat(2_000))
        assertThrows(IllegalArgumentException::class.java) { ChatSession().apply { val s = start(); val id = UUID.fromString(s.payload.getString("sessionId")); accept(reply("chat.session.started", s.id, JSONObject().put("idleTimeoutSeconds", 900), id)); turn("😀".repeat(2_001)) } }
    }

    @Test fun nullSessionErrorCorrelatesOnlyPendingStart() {
        val chat = ChatSession()
        val start = chat.start()
        val nullError = reply("chat.error", start.id, JSONObject().put("sessionId", JSONObject.NULL)
            .put("code", "CHAT_INVALID_SESSION").put("message", "Invalid session").put("retryable", false))
        assertEquals(ChatSession.Event.Error(null, start.id, "CHAT_INVALID_SESSION", "Invalid session", false), chat.accept(nullError))
        assertNull(chat.accept(nullError))
        val next = chat.start()
        assertNull(chat.accept(reply("chat.error", next.id, JSONObject().put("code", "CHAT_INVALID_SESSION")
            .put("message", "Wrong ID").put("retryable", false), UUID.randomUUID())))
    }

    @Test fun invalidSessionErrorEndsActiveSession() {
        val chat = ChatSession()
        val start = chat.start(); val session = UUID.fromString(start.payload.getString("sessionId"))
        chat.accept(reply("chat.session.started", start.id, JSONObject().put("idleTimeoutSeconds", 900), session))
        val turn = chat.turn("question")
        assertEquals("CHAT_INVALID_SESSION", (chat.accept(reply("chat.error", turn.id,
            JSONObject().put("code", "CHAT_INVALID_SESSION").put("message", "expired").put("retryable", false), session)) as ChatSession.Event.Error).code)
        assertThrows(IllegalStateException::class.java) { chat.turn("no replay") }
    }
}
