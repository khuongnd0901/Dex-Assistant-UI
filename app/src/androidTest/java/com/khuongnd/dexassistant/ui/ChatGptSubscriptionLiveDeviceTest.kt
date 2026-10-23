package com.khuongnd.dexassistant.ui

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.ai.ChatGptResponsesClient
import com.khuongnd.dexassistant.ui.ai.ResponsesStream
import com.khuongnd.dexassistant.ui.auth.ChatGptAuthManager
import com.khuongnd.dexassistant.ui.auth.ChatGptFailure
import com.khuongnd.dexassistant.ui.security.ChatGptSettings
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
    private fun liveContext(): android.content.Context {
        assumeTrue("Owner-authorized live probe required", InstrumentationRegistry.getArguments()
            .getString("subscriptionLiveProbe") == "true")
        return InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test fun authorizedNamespacedFunctionPlanningNeverExecutes() {
        val context = liveContext()
        val auth = ChatGptAuthManager.get(context)
        val model = ChatGptSettings(context).model()
        val supported = setOf(com.khuongnd.dexassistant.ui.routing.ActionId.VOLUME_SET)
        val registry = com.khuongnd.dexassistant.ui.routing.CommandRegistry.load(context)
        val payload = ChatGptResponsesClient.payload(model,
            JSONArray().put(JSONObject().put("role", "user").put("content", "Lập kế hoạch tắt âm lượng media.")),
            "Call deviceCommand_VOLUME_SET with operation MUTE. This is a planning test only; no action executes.")
            .put("tools", com.khuongnd.dexassistant.ui.ai.SubscriptionTools.schemas(supported, registry.appIds))
        val start = System.nanoTime()
        var failure = "NONE"
        var count = 0
        val shape = Bundle()
        try {
            val response = ChatGptResponsesClient(auth).response(payload)
            shape.putBoolean("toolResponseCompleted", response.optString("status") == "completed")
            val items = response.getJSONArray("output")
            shape.putInt("toolOutputItems", items.length())
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                if (item.optString("type") == "function_call") {
                    shape.putBoolean("toolHasNamespace", item.has("namespace"))
                    shape.putBoolean("toolNamespaceDevice", item.optString("namespace") == "device")
                    shape.putBoolean("toolStatusCompleted", item.optString("status") == "completed")
                    shape.putBoolean("toolHasArguments", item.has("arguments"))
                }
            }
            val actions = com.khuongnd.dexassistant.ui.ai.SubscriptionTools.parse(response, registry, supported)
            check(actions.size == 1 && actions.single().params["operation"] == "MUTE")
            count = actions.size
            ChatGptSettings(context).recordSubscriptionVerification(requireNotNull(auth.connectionKey()), model, true)
        } catch (e: Exception) { failure = if (e is ChatGptFailure) e.code else e.javaClass.simpleName }
        finally {
            shape.putInt("toolValidatedActions", count)
            shape.putLong("toolPlanningMs", (System.nanoTime() - start) / 1_000_000)
            shape.putString("toolFailure", failure)
            shape.putBoolean("toolActionsExecuted", false)
            InstrumentationRegistry.getInstrumentation().sendStatus(0, shape)
        }
        org.junit.Assert.assertEquals("Structured tool planning: $failure", 1, count)
    }

    @Test fun authorizedRefreshModelsAndTwoTurnChatRestore() {
        val context = liveContext()
        val auth = ChatGptAuthManager.get(context)
        val previous = auth.freshSession()
        val refreshed = auth.freshSession(previous.accessToken)
        val models = com.khuongnd.dexassistant.ui.ai.ChatGptModelRepository(auth).load()
        val model = ChatGptSettings(context).model()
        check(models.any { it.slug == model })
        val report = Bundle().apply {
            putBoolean("liveRefreshAuthorized", refreshed.hasSubscriptionScope())
            putBoolean("liveAccessTokenChanged", refreshed.accessToken != previous.accessToken)
            putBoolean("liveRefreshTokenChanged", refreshed.refreshToken != previous.refreshToken)
            putInt("liveAccountModelCount", models.size)
        }
        val state = com.khuongnd.dexassistant.ui.ai.DirectChatState().apply { start() }
        try {
            listOf("Chào tôi bằng một câu tiếng Việt.", "Nhắc lại lời chào vừa rồi, ngắn gọn.").forEachIndexed { i, input ->
                val turn = state.begin(input)
                val start = System.nanoTime()
                val response = ChatGptResponsesClient(auth).response(
                    com.khuongnd.dexassistant.ui.ai.ChatGptSubscriptionProvider.chatPayload(model, turn))
                val reply = ResponsesStream.chatText(response)
                org.junit.Assert.assertEquals(i + 1, state.complete(turn, reply))
                report.putLong("liveChatTurn${i + 1}Ms", (System.nanoTime() - start) / 1_000_000)
                report.putInt("liveChatTurn${i + 1}PriorPairs", turn.previous.size)
                report.putInt("liveChatTurn${i + 1}ReplyCodePoints", reply.codePointCount(0, reply.length))
            }
            report.putBoolean("liveTwoTurnChatValidated", true)
        } finally { state.clear(); InstrumentationRegistry.getInstrumentation().sendStatus(0, report) }
    }

    @Test fun authorizedHarmlessInferenceWithStructuralDiagnostics() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue("Owner-authorized live probe required", InstrumentationRegistry.getArguments()
            .getString("subscriptionLiveProbe") == "true")
        val context = instrumentation.targetContext
        val auth = ChatGptAuthManager.get(context)
        check(auth.connected()) { "SUBSCRIPTION_NOT_CONNECTED" }
        val model = ChatGptSettings(context).model()
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
            ChatGptSettings(context).recordSubscriptionVerification(requireNotNull(auth.connectionKey()), model, false)
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
