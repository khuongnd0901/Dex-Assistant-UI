package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.auth.*
import com.khuongnd.dexassistant.ui.routing.ActionId
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

class ChatGptSubscriptionTest {
    private fun completed(text: String = "Xin chào") = JSONObject().put("id", "r1").put("status", "completed")
        .put("output", JSONArray().put(JSONObject().put("type", "message").put("role", "assistant")
            .put("status", "completed").put("content", JSONArray().put(JSONObject().put("type", "output_text").put("text", text)))))
    private fun event(type: String, response: JSONObject? = null): String = "event: $type\ndata: " +
        JSONObject().put("type", type).apply { if (response != null) put("response", response) }.toString() + "\n\n"
    private fun stream() = event("response.created", JSONObject().put("id", "r1")) + event("response.completed", completed())
    private fun failure(code: String? = null, block: () -> Unit) {
        try { block(); fail("must reject") }
        catch (e: ChatGptFailure) { if (code != null) assertEquals(code, e.code) }
    }
    @Test fun vietnameseCompletionIsAuthoritative() {
        val deltas = StringBuilder()
        val delta = "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Xin\"}\n\n"
        val response = ResponsesStream.read(Buffer().writeUtf8(delta + stream()), onDelta = { deltas.append(it) })
        assertEquals("Xin", deltas.toString()); assertEquals("Xin chào", ResponsesStream.chatText(response))
    }
    @Test fun missingTerminalMalformedIncompleteAndQuotaFailClosed() {
        failure("INCOMPLETE_RESPONSE") { ResponsesStream.read(Buffer().writeUtf8("data: {\"type\":\"response.output_text.delta\",\"delta\":\"partial\"}\n\n")) }
        failure("INVALID_STREAM") { ResponsesStream.read(Buffer().writeUtf8("data: not-json\n\n")) }
        failure("INCOMPLETE_RESPONSE") { ResponsesStream.read(Buffer().writeUtf8(event("response.incomplete"))) }
        failure("QUOTA_LIMITED") { ResponsesStream.read(Buffer().writeUtf8(event("response.failed", JSONObject().put("error", JSONObject().put("code", "subscription_sharing_usage_limit_exceeded"))))) }
        failure("INVALID_STREAM") { ResponsesStream.read(Buffer().writeUtf8(event("response.completed", completed().put("status", "in_progress")))) }
        failure("INVALID_STREAM") { ResponsesStream.read(Buffer().writeUtf8(event("response.created", JSONObject().put("id", "other")) + event("response.completed", completed()))) }
    }
    @Test fun cancelledAndOversizedStreamsFail() {
        val op = SubscriptionOperation(); op.cancel()
        failure("CANCELLED") { ResponsesStream.read(Buffer().writeUtf8(stream()), op::check) }
        failure("STREAM_TOO_LARGE") { ResponsesStream.read(Buffer().writeUtf8("data: " + JSONObject().put("type", "response.output_text.delta").put("delta", "x".repeat(65537)) + "\n\n")) }
    }
    @Test fun chatNeverAcceptsToolCallsOrOverlongAnswers() {
        failure("CHAT_INVALID_OUTPUT") { ResponsesStream.chatText(completed("x".repeat(4001))) }
        failure("CHAT_INVALID_OUTPUT") { ResponsesStream.chatText(JSONObject().put("output", JSONArray().put(JSONObject().put("type", "function_call")))) }
    }
    @Test fun historyIsExplicitBoundedAndCommittedOnlyByCaller() {
        val state = DirectChatState(); state.start()
        repeat(25) { state.complete(state.begin("Câu $it"), "Trả lời $it") }
        val turn = state.begin("Tiếp tục", ZonedDateTime.parse("2026-10-22T22:00:00+07:00[Asia/Ho_Chi_Minh]"))
        val request = ChatGptSubscriptionProvider.chatPayload("account-model", turn)
        assertEquals(41, request.getJSONArray("input").length())
        assertEquals("Câu 5", request.getJSONArray("input").getJSONObject(0).getString("content"))
        assertFalse(request.getBoolean("store")); assertTrue(request.getBoolean("stream"))
        listOf("previous_response_id", "temperature", "max_output_tokens", "tools").forEach { assertFalse(request.has(it)) }
        assertEquals(20, state.begin("Lại").previous.size)
    }
    @Test fun modelsComeOnlyFromVisibleAccountCatalog() {
        val json = JSONObject("""{"models":[{"slug":"account-a","display_name":"A","visibility":"list"},{"slug":"hidden","visibility":"hide"},{"slug":"account-b","visibility":"list"}]}""")
        assertEquals(listOf("account-a", "account-b"), ChatGptModelRepository.parse(json).map { it.slug })
    }
    @Test fun namespaceSchemasAndTypedPlansAreValidated() {
        val supported = setOf(ActionId.VOLUME_SET)
        val tools = SubscriptionTools.schemas(supported, emptySet())
        assertEquals("namespace", tools.getJSONObject(0).getString("type"))
        val function = tools.getJSONObject(0).getJSONArray("tools").getJSONObject(0)
        assertEquals("function", function.getString("type")); assertFalse(function.has("function"))
        fun plan(name: String = "deviceCommand_VOLUME_SET", args: String = "{\"operation\":\"MUTE\"}") =
            JSONObject().put("status", "completed").put("output", JSONArray().put(JSONObject()
                .put("type", "function_call").put("namespace", "device").put("name", name)
                .put("status", "completed").put("call_id", "c1").put("arguments", args)))
        val registry = CommandRegistry.bundledForTests()
        assertEquals(ActionId.VOLUME_SET, SubscriptionTools.parse(plan(), registry, supported).single().action)
        fun reject(p: JSONObject) { try { SubscriptionTools.parse(p, registry, supported); fail() } catch (_: IllegalArgumentException) {} }
        reject(plan("deviceCommand_WIFI_SET", "{\"enabled\":true}"))
        reject(plan(args = "{\"operation\":\"MUTE\",\"requiresConfirmation\":false}"))
        reject(plan().apply { getJSONArray("output").getJSONObject(0).put("status", "in_progress") })
        reject(plan().apply { getJSONArray("output").put(getJSONArray("output").getJSONObject(0)) })
        reject(plan().apply { getJSONArray("output").getJSONObject(0).put("namespace", "other") })
        reject(JSONObject().put("status", "completed").put("output", JSONArray()))
        assertFalse(SubscriptionAcceptance.DEVICE_COMMANDS_ACCEPTED)
        assertFalse(SubscriptionAcceptance.DEFAULT_ACTIVATION_ACCEPTED)
    }

    private class Auth : SubscriptionSessionSource {
        var refreshes = 0
        override fun freshSession(rejectedAccessToken: String?): ChatGptSession {
            if (rejectedAccessToken != null) refreshes++
            return ChatGptSession("oaiapp_test", "sub", "", "id", "token$refreshes", "refresh", "resource.invoke chatgpt.tokens.use.direct", Long.MAX_VALUE)
        }
        override fun failure(e: Exception) {}
    }
    @Test fun transportRefreshesPreStream401OnceButNeverRetriesOtherFailures() {
        for (status in listOf(401, 403, 429, 500, 503)) {
            val auth = Auth(); var requests = 0
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                val code = if (requests++ == 0) status else 200
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("synthetic")
                    .header("Content-Type", "text/event-stream")
                    .body((if (code == 200) stream() else "private provider body").toResponseBody("text/event-stream".toMediaType())).build()
            }.build()
            val responses = ChatGptResponsesClient(auth, client)
            val request = ChatGptResponsesClient.payload("account-model", JSONArray().put(JSONObject().put("role", "user").put("content", "chào")), "Vietnamese")
            if (status == 401) {
                assertEquals("Xin chào", ResponsesStream.chatText(responses.response(request)))
                assertEquals(2, requests); assertEquals(1, auth.refreshes)
            } else {
                failure { responses.response(request) }; assertEquals(1, requests); assertEquals(0, auth.refreshes)
            }
        }
    }
    @Test fun completedSseDoesNotDependOnExactMimeButJsonBodyCannotSucceed() {
        for (media in listOf(null, "Text/Event-Stream", "application/octet-stream")) {
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .apply { if (media != null) header("Content-Type", media) }
                    .body(stream().toResponseBody()).build()
            }.build()
            assertEquals("Xin chào", ResponsesStream.chatText(ChatGptResponsesClient(Auth(), client).response(JSONObject())))
        }
        val jsonClient = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .header("Content-Type", "application/json").body("{}\n".toResponseBody()).build()
        }.build()
        failure("INCOMPLETE_RESPONSE") { ChatGptResponsesClient(Auth(), jsonClient).response(JSONObject()) }
    }
    @Test fun repeated401StopsAfterOneRefreshAndNetworkFailureIsNotReplayed() {
        for (networkError in listOf(false, true)) {
            val auth = Auth(); var requests = 0
            val client = OkHttpClient.Builder().retryOnConnectionFailure(false).addInterceptor { chain ->
                requests++
                if (networkError) throw java.io.IOException("synthetic offline")
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(401).message("Denied")
                    .body("hidden".toResponseBody()).build()
            }.build()
            failure(if (networkError) "NETWORK_OR_INVALID_RESPONSE" else "SESSION_EXPIRED") {
                ChatGptResponsesClient(auth, client).response(JSONObject())
            }
            assertEquals(if (networkError) 1 else 2, requests)
            assertEquals(if (networkError) 0 else 1, auth.refreshes)
        }
    }
    @Test fun interruptedStreamIsNotReissued() {
        val auth = Auth(); var requests = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .header("Content-Type", "text/event-stream").body(event("response.created", JSONObject().put("id", "r1")).toResponseBody()).build()
        }.build()
        failure("INCOMPLETE_RESPONSE") { ChatGptResponsesClient(auth, client).response(JSONObject()) }
        assertEquals(1, requests)
    }
}
