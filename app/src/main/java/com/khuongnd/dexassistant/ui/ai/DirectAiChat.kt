package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.routing.CommandPrivacy
import com.khuongnd.dexassistant.ui.security.AiCredentialStore
import com.khuongnd.dexassistant.ui.security.AiProvider
import com.khuongnd.dexassistant.ui.security.AiProviderSettings
import java.io.IOException
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

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

/** A text-only AI chat adapter. All provider requests are direct from Android, with NO tools. */
class DirectAiChat(
    private val credentials: AiCredentialStore,
    private val settings: AiProviderSettings,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS).readTimeout(16, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS).build(),
) {
    private val activeCall = java.util.concurrent.atomic.AtomicReference<okhttp3.Call?>()

    fun cancelPendingRequest() { activeCall.get()?.cancel() }

    private val system = "You are a brief Vietnamese-language conversation partner. " +
        "Never claim to control the device, execute an action, or request credentials. " +
        "Do not produce device instructions as if they were executed. Answer in the user's language."

    fun reply(turn: DirectChatState.Turn): String {
        if (Thread.currentThread().isInterrupted) throw MobileAiException("CHAT_PROVIDER_TIMEOUT")
        val preferred = settings.defaultProvider()
        // Do not silently switch away from an explicitly preferred unsupported provider.
        if (!preferred.supportsDirectCommands) throw MobileAiException("AI_PROVIDER_UNSUPPORTED")
        val providers = (listOf(preferred) + AiProvider.entries.filter { it != preferred })
            .distinct().filter { it.supportsDirectCommands && settings.enabled(it) &&
                settings.freeTierConfirmed(it) && settings.model(it).isNotBlank() && credentials.hasKey(it) }
        if (providers.isEmpty()) throw MobileAiException("AI_PROVIDER_NOT_CONFIGURED")
        var failure = MobileAiException("CHAT_PROVIDER_UNAVAILABLE")
        for (provider in providers) {
            if (Thread.currentThread().isInterrupted) throw MobileAiException("CHAT_PROVIDER_TIMEOUT")
            try {
                val key = credentials.load(provider) ?: throw MobileAiException("AI_PROVIDER_NOT_CONFIGURED")
                val result = call(provider, key, settings.model(provider), turn)
                return parse(provider, result)
            } catch (e: MobileAiException) {
                failure = e
                if (!e.canFallback) throw e
            }
        }
        throw failure
    }

    private fun call(provider: AiProvider, key: String, model: String, turn: DirectChatState.Turn): JSONObject {
        val input = turn.input
        val time = turn.time
        val systemText = system + " Current user local time: " + time.toOffsetDateTime() +
            "; time zone: " + time.zone.id + "."
        val req: Request.Builder
        val body: JSONObject
        when (provider) {
            AiProvider.GEMINI -> {
                require(model.matches(Regex("[A-Za-z0-9._-]{1,100}"))) { "INVALID_MODEL" }
                val contents = JSONArray()
                fun append(role: String, text: String) {
                    contents.put(JSONObject().put("role", role).put("parts",
                        JSONArray().put(JSONObject().put("text", text))))
                }
                turn.previous.forEach { append("user", it.user); append("model", it.assistant) }
                append("user", input)
                body = JSONObject()
                    .put("systemInstruction", JSONObject().put("parts",
                        JSONArray().put(JSONObject().put("text", systemText))))
                    .put("contents", contents)
                    .put("generationConfig", JSONObject().put("maxOutputTokens", 1024))
                req = Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models/" +
                    model + ":generateContent").header("x-goog-api-key", key)
            }
            AiProvider.GROQ -> {
                require(model.matches(Regex("[A-Za-z0-9._/-]{1,100}"))) { "INVALID_MODEL" }
                val messages = JSONArray().put(JSONObject().put("role", "system").put("content", systemText))
                turn.previous.forEach {
                    messages.put(JSONObject().put("role", "user").put("content", it.user))
                    messages.put(JSONObject().put("role", "assistant").put("content", it.assistant))
                }
                messages.put(JSONObject().put("role", "user").put("content", input))
                body = JSONObject().put("model", model).put("messages", messages)
                    .put("temperature", 0.4).put("max_tokens", 1024)
                req = Request.Builder().url("https://api.groq.com/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + key)
            }
            else -> throw MobileAiException("AI_PROVIDER_UNSUPPORTED")
        }
        val request = req.header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        val call = client.newCall(request)
        activeCall.set(call)
        try {
            if (Thread.currentThread().isInterrupted) call.cancel()
            call.execute().use { response ->
                if (!response.isSuccessful) throw when (response.code) {
                    429 -> MobileAiException("AI_QUOTA_EXHAUSTED", true)
                    408, 500, 502, 503, 504 -> MobileAiException("CHAT_PROVIDER_UNAVAILABLE", true)
                    401, 403 -> MobileAiException("AI_AUTH_FAILED")
                    else -> MobileAiException("CHAT_PROVIDER_UNAVAILABLE")
                }
                val text = response.body?.string() ?: throw MobileAiException("CHAT_INVALID_OUTPUT")
                if (text.length > 65536) throw MobileAiException("CHAT_INVALID_OUTPUT")
                return try { JSONObject(text) } catch (_: RuntimeException) {
                    throw MobileAiException("CHAT_INVALID_OUTPUT")
                }
            }
        } catch (_: IOException) {
            throw MobileAiException("CHAT_PROVIDER_TIMEOUT", !call.isCanceled())
        } finally {
            activeCall.compareAndSet(call, null)
        }
    }

    companion object {
        fun parse(provider: AiProvider, data: JSONObject): String {
            val text = try {
                when (provider) {
                    AiProvider.GEMINI -> {
                        val candidates = data.getJSONArray("candidates")
                        require(candidates.length() == 1)
                        val candidate = candidates.getJSONObject(0)
                        require(candidate.optString("finishReason") in setOf("STOP", ""))
                        val parts = candidate.getJSONObject("content").getJSONArray("parts")
                        buildString {
                            for (i in 0 until parts.length()) {
                                val part = parts.getJSONObject(i)
                                require(!part.has("functionCall"))
                                append(part.getString("text"))
                            }
                        }
                    }
                    AiProvider.GROQ -> {
                        val choices = data.getJSONArray("choices")
                        require(choices.length() == 1)
                        val message = choices.getJSONObject(0).getJSONObject("message")
                        require(!message.has("tool_calls") || message.isNull("tool_calls"))
                        message.getString("content")
                    }
                    else -> throw MobileAiException("AI_PROVIDER_UNSUPPORTED")
                }
            } catch (_: RuntimeException) {
                throw MobileAiException("CHAT_INVALID_OUTPUT")
            }
            if (text.isBlank() || text.codePointCount(0, text.length) > 4000 ||
                text.any { it == '\u0000' }) throw MobileAiException("CHAT_INVALID_OUTPUT")
            return text.trim()
        }
    }
}
