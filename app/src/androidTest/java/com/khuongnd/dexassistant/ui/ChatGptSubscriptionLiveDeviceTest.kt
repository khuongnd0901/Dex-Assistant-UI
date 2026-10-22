package com.khuongnd.dexassistant.ui

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.ai.ChatGptResponsesClient
import com.khuongnd.dexassistant.ui.ai.ResponsesStream
import com.khuongnd.dexassistant.ui.auth.ChatGptAuthManager
import com.khuongnd.dexassistant.ui.auth.ChatGptFailure
import com.khuongnd.dexassistant.ui.security.AiProvider
import com.khuongnd.dexassistant.ui.security.AiProviderSettings
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Explicit owner-authorized opt-in only. No tokens, prompts, text, response bodies or IDs are logged. */
class ChatGptSubscriptionLiveDeviceTest {
    @Test fun authorizedHarmlessInferenceWithStructuralDiagnostics() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue("Owner-authorized live probe required", InstrumentationRegistry.getArguments()
            .getString("subscriptionLiveProbe") == "true")
        val context = instrumentation.targetContext
        val auth = ChatGptAuthManager.get(context)
        check(auth.connected()) { "SUBSCRIPTION_NOT_CONNECTED" }
        val model = AiProviderSettings(context).model(AiProvider.CHATGPT_SUBSCRIPTION)
        check(model.isNotBlank()) { "SUBSCRIPTION_MODEL_NOT_SELECTED" }
        val capture = Buffer()
        var status = 0
        var media = "MISSING"
        val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS)
            .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                status = response.code
                val type = response.header("Content-Type")?.substringBefore(';')?.trim()
                media = if (type in setOf("text/event-stream", "application/json", "text/plain", "application/octet-stream")) type!! else "OTHER"
                val body = response.body ?: return@addInterceptor response
                val source = object : ForwardingSource(body.source()) {
                    override fun read(sink: Buffer, byteCount: Long): Long {
                        val count = super.read(sink, byteCount)
                        if (count > 0 && capture.size + count <= 2 * 1024 * 1024)
                            sink.copyTo(capture, sink.size - count, count)
                        return count
                    }
                }.buffer()
                response.newBuilder().body(object : ResponseBody() {
                    override fun contentType() = body.contentType()
                    override fun contentLength() = body.contentLength()
                    override fun source(): BufferedSource = source
                }).build()
            }.build()
        var reply: String? = null
        var failure = "NONE"
        val start = System.nanoTime()
        try {
            val response = ChatGptResponsesClient(auth, client).response(ChatGptResponsesClient.payload(model,
                JSONArray().put(JSONObject().put("role", "user").put("content", "Hãy chào tôi bằng một câu tiếng Việt.")),
                "Reply briefly in Vietnamese."))
            reply = ResponsesStream.chatText(response)
        } catch (e: Exception) {
            // Only local finite codes / exception class, never exception.message or causes.
            failure = if (e is ChatGptFailure) e.code else e.javaClass.simpleName
        } finally {
            val elapsed = (System.nanoTime() - start) / 1_000_000
            val shape = Bundle().apply {
                putInt("subscriptionHttpStatus", status)
                putString("subscriptionContentType", media)
                putLong("subscriptionInferenceMs", elapsed)
                putString("subscriptionFailure", failure)
                putBoolean("subscriptionReplyValidated", reply != null)
                putInt("subscriptionReplyCodePoints", reply?.codePointCount(0, reply!!.length) ?: 0)
            }
            val raw = capture.readUtf8()
            var deltas = 0; var doneItems = 0; var completed = 0; var created = 0; var dataRecords = 0
            var namedEvents = 0; var genericEvents = 0
            shape.putInt("capturedCharacters", raw.length)
            for (line in raw.lineSequence()) {
                if (line.startsWith("event:")) { namedEvents++; if (line.substring(6).trim() == "message") genericEvents++ }
                if (!line.startsWith("data:")) continue
                dataRecords++
                val event = try { JSONObject(line.substring(5).trim()) } catch (_: Exception) { continue }
                when (event.optString("type")) {
                    "response.created" -> created++
                    "response.output_text.delta" -> deltas++
                    "response.output_item.done" -> doneItems++
                    "response.completed" -> {
                        completed++
                        val response = event.optJSONObject("response")
                        shape.putBoolean("terminalHasResponse", response != null)
                        shape.putBoolean("terminalHasStatus", response?.has("status") == true)
                        shape.putBoolean("terminalStatusCompleted", response?.optString("status") == "completed")
                        shape.putBoolean("terminalHasOutput", response?.has("output") == true)
                        val output = response?.optJSONArray("output")
                        shape.putInt("terminalOutputItems", output?.length() ?: -1)
                        for (i in 0 until (output?.length() ?: 0)) {
                            val item = output!!.optJSONObject(i) ?: continue
                            if (item.optString("type") == "message") {
                                shape.putBoolean("messageHasStatus", item.has("status"))
                                shape.putBoolean("messageStatusCompleted", item.optString("status") == "completed")
                                shape.putBoolean("messageRoleAssistant", item.optString("role") == "assistant")
                                shape.putInt("messageContentParts", item.optJSONArray("content")?.length() ?: -1)
                            }
                        }
                    }
                }
            }
            shape.putInt("subscriptionCreatedEvents", created)
            shape.putInt("dataRecords", dataRecords)
            shape.putInt("namedEvents", namedEvents)
            shape.putInt("genericEvents", genericEvents)
            shape.putInt("subscriptionDeltaEvents", deltas)
            shape.putInt("subscriptionDoneItems", doneItems)
            shape.putInt("subscriptionCompletedEvents", completed)
            instrumentation.sendStatus(0, shape)
            capture.clear()
        }
        assertNotNull("Harmless subscription inference did not validate: $failure", reply)
    }
}
