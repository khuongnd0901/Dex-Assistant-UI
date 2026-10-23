package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.auth.SubscriptionSessionSource
import com.khuongnd.dexassistant.ui.auth.ChatGptFailure
import com.khuongnd.dexassistant.ui.auth.ChatGptHttp
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import org.json.JSONArray
import org.json.JSONObject

/** One operation per consumer, cancellation fences auth/transport races. Never reconnect a stream. */
class SubscriptionOperation {
    @Volatile var cancelled = false
        private set
    private val active = AtomicReference<Call?>()
    fun cancel() { cancelled = true; active.get()?.cancel() }
    fun check() { if (cancelled || Thread.currentThread().isInterrupted) throw ChatGptFailure("CANCELLED") }
    internal fun bind(call: Call) { check(); active.set(call); if (cancelled) call.cancel() }
    internal fun release(call: Call) { active.compareAndSet(call, null) }
}

/** Shared OAuth HTTP/SSE transport. No implicit retries, fallback, stored server state or API keys. */
class ChatGptResponsesClient internal constructor(private val auth: SubscriptionSessionSource,
    private val client: OkHttpClient = ChatGptHttp.client.newBuilder()
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()) {
    fun response(payload: JSONObject, operation: SubscriptionOperation = SubscriptionOperation(),
        onDelta: (String) -> Unit = {}, metric: (String, Long) -> Unit = { _, _ -> }): JSONObject {
        operation.check()
        var session = auth.freshSession()
        operation.check()
        // Only a pre-stream HTTP 401 can refresh/reissue once. Device execution happens later.
        for (attempt in 0..1) {
            val request = Request.Builder().url("https://api.openai.com/v1/responses")
                .header("Authorization", "Bearer ${session.accessToken}")
                .header("Accept", "text/event-stream")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
            val call = client.newCall(request); operation.bind(call)
            val timing = ProviderTiming(metric)
            try {
                call.execute().use { result ->
                    timing.headers()
                    operation.check()
                    if (result.code == 401 && attempt == 0) {
                        session = auth.freshSession(session.accessToken)
                        operation.check()
                    } else {
                        ChatGptHttp.checkStatus(result.code)
                        // Like the proven POC, validate SSE events rather than a proxy-specific MIME label.
                        // HTML/JSON errors still fail the bounded parser; only response.completed can succeed.
                        val body = result.body ?: throw ChatGptFailure("INVALID_STREAM")
                        return timing.body(body) { ResponsesStream.read(body.source(), operation::check, onDelta) }
                    }
                }
            } catch (e: ChatGptFailure) { auth.failure(e); throw e }
            catch (_: Exception) {
                operation.check()
                throw ChatGptFailure("NETWORK_OR_INVALID_RESPONSE")
            } finally { timing.headers(); operation.release(call) }
        }
        throw ChatGptFailure("SESSION_EXPIRED")
    }

    companion object {
        fun payload(model: String, input: JSONArray, instructions: String): JSONObject {
            require(model.isNotBlank() && model.length <= 128 && input.length() > 0)
            return JSONObject().put("model", model).put("input", input).put("instructions", instructions)
                .put("store", false).put("stream", true)
        }
    }
}

/** Bounded incremental SSE; final response is authoritative, deltas are display-only. */
internal object ResponsesStream {
    fun read(source: BufferedSource, check: () -> Unit = {}, onDelta: (String) -> Unit = {}): JSONObject {
        var bytes = 0L; var events = 0; var deltaSize = 0
        val data = StringBuilder()
        var eventName = ""
        var responseId: String? = null
        val addedItems = mutableMapOf<Int, String>()
        val doneItems = sortedMapOf<Int, JSONObject>()
        val doneIds = mutableSetOf<String>()
        fun index(event: JSONObject): Int {
            val raw = event.opt("output_index")
            if (raw !is Int && raw !is Long) throw ChatGptFailure("INVALID_STREAM")
            val value = (raw as Number).toLong()
            if (value !in 0..127) throw ChatGptFailure("INVALID_STREAM")
            return value.toInt()
        }
        fun finalItem(item: JSONObject): JSONObject {
            // Optional item status is normalized only from a done event or a completed response.
            if (item.has("status") && !item.isNull("status") && item.getString("status") != "completed")
                throw ChatGptFailure("INVALID_STREAM")
            return JSONObject(item.toString()).apply { put("status", "completed") }
        }
        while (!source.exhausted()) {
            check()
            val line = source.readUtf8LineStrict(262144)
            bytes += line.toByteArray(Charsets.UTF_8).size + 1
            if (bytes > 2 * 1024 * 1024) throw ChatGptFailure("STREAM_TOO_LARGE")
            if (line.isEmpty()) {
                if (data.isEmpty()) { eventName = ""; continue }
                if (++events > 10000) throw ChatGptFailure("STREAM_TOO_LARGE")
                val event = try { JSONObject(data.toString()) } catch (_: Exception) { throw ChatGptFailure("INVALID_STREAM") }
                data.setLength(0)
                val type = event.getString("type")
                if (eventName.isNotEmpty() && eventName != type) throw ChatGptFailure("INVALID_STREAM")
                eventName = ""
                when (type) {
                    "response.created" -> {
                        if (responseId != null) throw ChatGptFailure("INVALID_STREAM")
                        responseId = event.getJSONObject("response").getString("id")
                    }
                    "response.output_item.added" -> {
                        val i = index(event)
                        val id = event.getJSONObject("item").getString("id")
                        if (id.isBlank() || id.length > 256 || addedItems.put(i, id) != null || doneItems.containsKey(i))
                            throw ChatGptFailure("INVALID_STREAM")
                    }
                    "response.output_item.done" -> {
                        val i = index(event)
                        val item = finalItem(event.getJSONObject("item"))
                        val id = item.getString("id")
                        if (id.isBlank() || id.length > 256 || !doneIds.add(id) || doneItems.containsKey(i) ||
                            (addedItems.containsKey(i) && addedItems[i] != id)) throw ChatGptFailure("INVALID_STREAM")
                        doneItems[i] = item
                    }
                    "response.output_text.delta" -> {
                        val delta = event.getString("delta"); deltaSize += delta.length
                        if (deltaSize > 65536) throw ChatGptFailure("STREAM_TOO_LARGE")
                        onDelta(delta)
                    }
                    "response.completed" -> {
                        check()
                        val response = event.getJSONObject("response")
                        if (response.getString("status") != "completed" ||
                            (response.has("error") && !response.isNull("error")) ||
                            (response.has("incomplete_details") && !response.isNull("incomplete_details")) ||
                            (responseId != null && response.getString("id") != responseId))
                            throw ChatGptFailure("INVALID_STREAM")
                        val snapshot = response.getJSONArray("output")
                        if (addedItems.keys.any { it !in doneItems }) throw ChatGptFailure("INCOMPLETE_RESPONSE")
                        val finalOutput = JSONArray()
                        if (doneItems.isNotEmpty()) {
                            if (responseId == null || doneItems.keys.toList() != (0 until doneItems.size).toList())
                                throw ChatGptFailure("INVALID_STREAM")
                            doneItems.values.forEach(finalOutput::put)
                            if (snapshot.length() > 0) {
                                val normalized = JSONArray()
                                for (i in 0 until snapshot.length()) normalized.put(finalItem(snapshot.getJSONObject(i)))
                                if (!sameJson(normalized, finalOutput)) throw ChatGptFailure("INVALID_STREAM")
                            }
                        } else {
                            for (i in 0 until snapshot.length()) finalOutput.put(finalItem(snapshot.getJSONObject(i)))
                        }
                        return JSONObject(response.toString()).put("output", finalOutput)
                    }
                    "response.failed", "response.incomplete", "error" -> {
                        val error = event.optJSONObject("response")?.optJSONObject("error") ?: event.optJSONObject("error")
                        val code = error?.optString("code") ?: event.optString("code")
                        throw ChatGptFailure(if (code in setOf("subscription_sharing_usage_limit_exceeded",
                                "subscription_sharing_usage_unavailable")) "QUOTA_LIMITED" else "INCOMPLETE_RESPONSE")
                    }
                }
            } else if (line.startsWith("data:")) {
                if (data.isNotEmpty()) data.append('\n')
                data.append(line.substring(5).removePrefix(" "))
                if (data.length > 262144) throw ChatGptFailure("STREAM_TOO_LARGE")
            } else if (line.startsWith("event:")) eventName = line.substring(6).trim()
        }
        throw ChatGptFailure("INCOMPLETE_RESPONSE")
    }

    private fun sameJson(first: Any?, second: Any?): Boolean = when {
        first is JSONObject && second is JSONObject -> {
            val keys = first.keys().asSequence().toSet()
            keys == second.keys().asSequence().toSet() && keys.all { sameJson(first.opt(it), second.opt(it)) }
        }
        first is JSONArray && second is JSONArray -> first.length() == second.length() &&
            (0 until first.length()).all { sameJson(first.opt(it), second.opt(it)) }
        else -> first == second
    }

    fun chatText(response: JSONObject): String {
        val output = response.getJSONArray("output")
        val text = StringBuilder()
        for (i in 0 until output.length()) {
            val item = output.getJSONObject(i)
            when (item.getString("type")) {
                "reasoning" -> Unit
                "message" -> {
                    require(item.getString("role") == "assistant" && item.getString("status") == "completed")
                    val content = item.getJSONArray("content")
                    for (j in 0 until content.length()) {
                        val part = content.getJSONObject(j)
                        require(part.getString("type") == "output_text")
                        text.append(part.getString("text"))
                    }
                }
                else -> throw ChatGptFailure("CHAT_INVALID_OUTPUT") // Tools/refusals never enter the action path.
            }
        }
        val value = text.toString().trim()
        if (value.isBlank() || value.codePointCount(0, value.length) > 4000 || '\u0000' in value)
            throw ChatGptFailure("CHAT_INVALID_OUTPUT")
        return value
    }
}
