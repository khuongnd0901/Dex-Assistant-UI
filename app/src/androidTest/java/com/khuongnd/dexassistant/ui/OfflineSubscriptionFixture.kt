package com.khuongnd.dexassistant.ui

import com.khuongnd.dexassistant.ui.ai.*
import com.khuongnd.dexassistant.ui.auth.*
import com.khuongnd.dexassistant.ui.routing.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

internal class OfflineSubscriptionFixture(client: OkHttpClient) : AiSubscription {
    private val source = object : SubscriptionSessionSource {
        override fun freshSession(rejectedAccessToken: String?) = ChatGptSession("oaiapp_fixture", "fixture", "", "id",
            "synthetic-token", "synthetic-refresh", "resource.invoke chatgpt.tokens.use.direct", Long.MAX_VALUE)
        override fun failure(e: Exception) = Unit
    }
    private val responses = ChatGptResponsesClient(source, client)
    private val active = AtomicReference<SubscriptionOperation?>()
    override fun ready() = true
    override fun cancel() { active.get()?.cancel() }
    private fun response(payload: JSONObject, metric: (String, Long) -> Unit): JSONObject {
        val operation = SubscriptionOperation()
        check(active.compareAndSet(null, operation))
        try { return responses.response(payload, operation, metric = metric) }
        catch (e: ChatGptFailure) { throw MobileAiException(e.code) }
        finally { active.compareAndSet(operation, null) }
    }
    override fun reply(turn: DirectChatState.Turn, metric: (String, Long) -> Unit): String =
        ResponsesStream.chatText(response(ChatGptSubscriptionProvider.chatPayload("fixture-model", turn), metric))
    override fun plan(text: String, registry: CommandRegistry, supported: Set<ActionId>, metric: (String, Long) -> Unit): List<TypedAction> =
        SubscriptionTools.parse(response(ChatGptResponsesClient.payload("fixture-model",
            JSONArray().put(JSONObject().put("role", "user").put("content", text)), "Plan safely")
            .put("tools", SubscriptionTools.schemas(supported, registry.appIds)), metric), registry, supported)
    companion object {
        val chat = """{"type":"message","role":"assistant","status":"completed","content":[{"type":"output_text","text":"Xin chào"}]}"""
        val command = """{"type":"function_call","namespace":"device","name":"deviceCommand_VOLUME_SET","status":"completed","call_id":"c1","arguments":"{\"operation\":\"SET\",\"percent\":30}"}"""
        fun response(request: Request, code: Int = 200, item: String = chat): Response = Response.Builder()
            .request(request).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
            .body(("data: " + JSONObject().put("type", "response.completed").put("response", JSONObject()
                .put("id", "fixture").put("status", "completed").put("output", JSONArray().put(JSONObject(item)))) + "\n\n").toResponseBody()).build()
    }
}
