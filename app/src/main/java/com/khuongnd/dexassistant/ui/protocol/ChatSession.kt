package com.khuongnd.dexassistant.ui.protocol

import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID

/** Volatile Chat wire state. No Command result, action or history type enters this class. */
class ChatSession {
    sealed interface Event {
        data class Started(val sessionId: UUID, val idleTimeoutSeconds: Int) : Event
        data class Reply(val sessionId: UUID, val requestId: UUID, val text: String, val turn: Int) : Event
        data class Ended(val sessionId: UUID) : Event
        data class Error(val sessionId: UUID?, val requestId: UUID, val code: String, val message: String, val retryable: Boolean) : Event
    }

    data class Outbound(val type: String, val id: UUID, val payload: JSONObject)
    private var sessionId: UUID? = null
    private var startId: UUID? = null
    private var turnId: UUID? = null
    private var endId: UUID? = null
    private var endingSessionId: UUID? = null
    private var started = false
    private var lastTurn = 0

    fun start(): Outbound {
        check(sessionId == null) { "Chat session already active" }
        val session = UUID.randomUUID()
        val request = UUID.randomUUID()
        sessionId = session; startId = request; endId = null; endingSessionId = null; started = false; lastTurn = 0
        return Outbound("chat.session.start", request, JSONObject().put("sessionId", session.toString()).put("locale", "vi-VN"))
    }

    fun turn(text: String, now: ZonedDateTime = ZonedDateTime.now()): Outbound {
        val session = sessionId ?: error("No Chat session")
        check(started && endId == null && turnId == null) { "Chat is not ready for a turn" }
        require(text.isNotBlank() && text.codePointCount(0, text.length) <= 2_000) { "Invalid Chat input length" }
        val request = UUID.randomUUID()
        val payload = JSONObject().put("sessionId", session.toString()).put("text", text)
            .put("localTime", JSONObject().put("timestamp", now.toOffsetDateTime().toString()).put("timeZone", now.zone.id))
        turnId = request
        return Outbound("chat.turn.request", request, payload)
    }

    /** Invalidate all turn callbacks before the end frame is sent. */
    fun end(reason: String): Outbound? {
        require(reason == "USER" || reason == "STOP")
        val session = sessionId ?: return null
        val request = UUID.randomUUID()
        sessionId = null; startId = null; turnId = null; started = false; endId = request; endingSessionId = session
        return Outbound("chat.session.end", request, JSONObject().put("sessionId", session.toString()).put("reason", reason))
    }

    fun invalidate() {
        sessionId = null; startId = null; turnId = null; endId = null; endingSessionId = null; started = false; lastTurn = 0
    }

    fun accept(raw: String): Event? {
        require(raw.toByteArray(Charsets.UTF_8).size <= 16_384) { "Chat frame too large" }
        val envelope = JSONObject(raw)
        require(keys(envelope) == setOf("version", "type", "messageId", "correlationId", "sentAt", "payload"))
        require(envelope.get("version") is Number && envelope.getInt("version") == 1)
        val type = envelope.getString("type")
        require(type in setOf("chat.session.started", "chat.turn.response", "chat.session.ended", "chat.error"))
        UUID.fromString(envelope.getString("messageId"))
        OffsetDateTime.parse(envelope.getString("sentAt"))
        val correlation = UUID.fromString(envelope.getString("correlationId"))
        val payload = envelope.getJSONObject("payload")
        val incomingSession = if (payload.isNull("sessionId")) null else UUID.fromString(payload.getString("sessionId"))
        return when (type) {
            "chat.session.started" -> {
                require(keys(payload) == setOf("sessionId", "idleTimeoutSeconds"))
                val timeout = payload.getInt("idleTimeoutSeconds")
                require(payload.get("idleTimeoutSeconds") is Number && timeout == 900 && incomingSession != null)
                if (incomingSession != sessionId || correlation != startId || endId != null) null
                else { started = true; startId = null; Event.Started(incomingSession, timeout) }
            }
            "chat.turn.response" -> {
                require(keys(payload) == setOf("sessionId", "text", "turn"))
                val reply = payload.getString("text")
                val turn = payload.getInt("turn")
                require(payload.get("turn") is Number && reply.isNotBlank() && reply.codePointCount(0, reply.length) <= 4_000 && turn > 0 && incomingSession != null)
                if (incomingSession != sessionId || correlation != turnId || !started || endId != null || turn <= lastTurn) null
                else { turnId = null; lastTurn = turn; Event.Reply(incomingSession, correlation, reply, turn) }
            }
            "chat.session.ended" -> {
                require(keys(payload) == setOf("sessionId") && incomingSession != null)
                if (correlation != endId || incomingSession != endingSessionId) null
                else { endId = null; endingSessionId = null; Event.Ended(incomingSession) }
            }
            else -> {
                require(keys(payload) == setOf("sessionId", "code", "message", "retryable"))
                val code = payload.getString("code")
                val message = payload.getString("message")
                val retryable = payload.getBoolean("retryable")
                require(payload.get("retryable") is Boolean && code in ERROR_CODES && message.length <= 512)
                val matchingSession = incomingSession == sessionId || (incomingSession == null && correlation == startId)
                if (!matchingSession || correlation !in setOfNotNull(startId, turnId)) null
                else {
                    if (correlation == startId || code == "CHAT_INVALID_SESSION" || code == "CHAT_TURN_LIMIT") invalidate()
                    else turnId = null
                    Event.Error(incomingSession, correlation, code, message, retryable)
                }
            }
        }
    }

    private fun keys(json: JSONObject): Set<String> = json.keys().asSequence().toSet()

    companion object {
        private val ERROR_CODES = setOf("CHAT_INVALID_SESSION", "CHAT_SESSION_LIMIT", "CHAT_TURN_IN_PROGRESS",
            "CHAT_TURN_LIMIT", "CHAT_INPUT_TOO_LARGE", "CHAT_CONTEXT_LIMIT", "CHAT_OUTPUT_TOO_LARGE",
            "CHAT_SENSITIVE_INPUT_REJECTED", "CHAT_PROVIDER_UNAVAILABLE", "CHAT_PROVIDER_TIMEOUT",
            "CHAT_INVALID_OUTPUT", "CHAT_UNSUPPORTED_MESSAGE")
    }
}
