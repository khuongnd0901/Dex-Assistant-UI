package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.routing.CommandPrivacy
import java.time.ZonedDateTime

/** In-memory, bounded conversational context. Never contains device action capabilities. */
class DirectChatState {
    data class Pair(val user: String, val assistant: String)
    data class Turn(val previous: List<Pair>, val input: String, val time: ZonedDateTime)
    private val pairs = ArrayDeque<Pair>()
    private var totalTurns = 0
    var lastActivityMillis: Long = 0
        private set

    /** The idle limit also applies before the first request. */
    fun start(now: Long = System.currentTimeMillis()) {
        clear()
        lastActivityMillis = now
    }

    fun begin(input: String, at: ZonedDateTime = ZonedDateTime.now()): Turn {
        require(input.isNotBlank() && input.codePointCount(0, input.length) <= 2000) { "CHAT_INPUT_TOO_LARGE" }
        require(!CommandPrivacy.requiresLocalHandling(input)) { "CHAT_SENSITIVE_INPUT_REJECTED" }
        require(totalTurns < 200) { "CHAT_TURN_LIMIT" }
        val history = pairs.toList().takeLast(20).toMutableList()
        // UTF-8 length is a conservative bound; also limit request count.
        while (history.isNotEmpty() && approximateSize(history, input) > 24000) history.removeAt(0)
        require(approximateSize(history, input) <= 24000) { "CHAT_CONTEXT_LIMIT" }
        lastActivityMillis = System.currentTimeMillis()
        return Turn(history, input.trim(), at)
    }

    fun complete(request: Turn, reply: String): Int {
        require(reply.isNotBlank() && reply.codePointCount(0, reply.length) <= 4000) { "CHAT_INVALID_OUTPUT" }
        pairs.addLast(Pair(request.input, reply))
        while (pairs.size > 20 || approximateSize(pairs.toList(), "") > 24000) pairs.removeFirst()
        totalTurns++
        lastActivityMillis = System.currentTimeMillis()
        return totalTurns
    }

    fun expired(now: Long = System.currentTimeMillis()): Boolean =
        lastActivityMillis > 0 && now - lastActivityMillis >= 15 * 60 * 1000

    fun clear() { pairs.clear(); totalTurns = 0; lastActivityMillis = 0 }

    private fun approximateSize(history: List<Pair>, input: String) =
        input.toByteArray(Charsets.UTF_8).size + history.sumOf {
            it.user.toByteArray(Charsets.UTF_8).size + it.assistant.toByteArray(Charsets.UTF_8).size
        }
}
