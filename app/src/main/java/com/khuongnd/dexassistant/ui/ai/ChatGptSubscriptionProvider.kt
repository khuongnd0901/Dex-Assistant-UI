package com.khuongnd.dexassistant.ui.ai

import android.content.Context
import com.khuongnd.dexassistant.ui.auth.ChatGptAuthManager
import com.khuongnd.dexassistant.ui.auth.ChatGptFailure
import com.khuongnd.dexassistant.ui.routing.ActionId
import com.khuongnd.dexassistant.ui.routing.CommandPrivacy
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import com.khuongnd.dexassistant.ui.routing.TypedAction
import com.khuongnd.dexassistant.ui.security.ChatGptSettings
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONArray
import org.json.JSONObject

/** Native account/model tools verified on Fold3; manual selection also requires scoped proofs. Automatic migration stays off. */
object SubscriptionAcceptance {
    const val DEFAULT_ACTIVATION_ACCEPTED = false
    const val MANUAL_SELECTION_ACCEPTED = true
    const val DEVICE_COMMANDS_ACCEPTED = true
}

/** Consumers share authentication/transport, but each owns its cancellable request. */
internal interface AiSubscription {
    fun ready(): Boolean
    fun cancel()
    fun reply(turn: DirectChatState.Turn, metric: (String, Long) -> Unit = { _, _ -> }): String
    fun plan(text: String, registry: CommandRegistry, supported: Set<ActionId>, metric: (String, Long) -> Unit = { _, _ -> }): List<TypedAction>
}

class ChatGptSubscriptionProvider(context: Context, private val settings: ChatGptSettings) : AiSubscription {
    private val auth = ChatGptAuthManager.get(context)
    private val responses = ChatGptResponsesClient(auth)
    private val toolCatalog = SerializedToolCatalog()
    private val active = AtomicReference<SubscriptionOperation?>()
    override fun cancel() { active.get()?.cancel() }
    override fun ready() = SubscriptionAcceptance.MANUAL_SELECTION_ACCEPTED && settings.selected() && auth.connected() &&
        settings.enabled() && settings.subscriptionVerified(auth.connectionKey(), false)

    private fun <T> request(block: (String, SubscriptionOperation) -> T): T {
        val op = SubscriptionOperation()
        if (!active.compareAndSet(null, op)) throw MobileAiException("AI_REQUEST_IN_PROGRESS")
        val model = settings.model()
        val generation = auth.sessionGeneration()
        try {
            if (!ready()) throw ChatGptFailure("NOT_CONNECTED")
            val value = block(model, op)
            op.check()
            if (auth.sessionGeneration() != generation || !settings.selected() ||
                settings.model() != model || !ready())
                throw ChatGptFailure("CANCELLED")
            return value
        } catch (e: ChatGptFailure) { throw MobileAiException(e.code) }
        catch (_: Exception) { throw MobileAiException("INVALID_LLM_OUTPUT") }
        finally { active.compareAndSet(op, null) }
    }

    override fun reply(turn: DirectChatState.Turn, metric: (String, Long) -> Unit): String = request { model, op ->
        require(!CommandPrivacy.requiresLocalHandling(turn.input))
        val payload = chatPayload(model, turn)
        val response = responses.response(payload, op, metric = metric)
        ProviderTiming.parse(metric) { ResponsesStream.chatText(response) }
    }

    override fun plan(text: String, registry: CommandRegistry, supported: Set<ActionId>, metric: (String, Long) -> Unit): List<TypedAction> {
        // No setting or model output can bypass the reviewed real function-calling gate.
        if (!SubscriptionAcceptance.DEVICE_COMMANDS_ACCEPTED || !settings.subscriptionVerified(auth.connectionKey(), true))
            throw MobileAiException("SUBSCRIPTION_COMMANDS_NOT_VERIFIED")
        return request { model, op ->
            require(text.isNotBlank() && text.length <= 2000 && !CommandPrivacy.requiresLocalHandling(text))
            val payload = ChatGptResponsesClient.payload(model,
                JSONArray().put(JSONObject().put("role", "user").put("content", text)),
                "Plan only supported device actions using the supplied functions. Reject ambiguous or unsafe requests. " +
                    "Never invent apps, recipients, targets, confirmation, credentials or success.")
                .put("tools", JSONArray(toolCatalog.forScope(supported, registry.appIds)))
            val response = responses.response(payload, op, metric = metric)
            ProviderTiming.parse(metric) { SubscriptionTools.parse(response, registry, supported) }
        }
    }

    companion object {
        fun chatPayload(model: String, turn: DirectChatState.Turn): JSONObject {
            val input = JSONArray()
            turn.previous.forEach {
                input.put(JSONObject().put("role", "user").put("content", it.user))
                input.put(JSONObject().put("role", "assistant").put("content", it.assistant))
            }
            input.put(JSONObject().put("role", "user").put("content", turn.input))
            return ChatGptResponsesClient.payload(model, input,
                "You are a brief Vietnamese-language conversation partner. Reply in the user's language, " +
                    "in at most 4000 Unicode characters and about 1024 tokens. Never claim to execute or control " +
                    "device actions, request credentials, or reveal sensitive information. " +
                    "Current local time: ${turn.time.toOffsetDateTime()}; time zone: ${turn.time.zone.id}.")
        }
    }
}

/** Documented namespace envelope; real-account compatibility remains a mandatory acceptance gate. */
internal object SubscriptionTools {
    fun schemas(supported: Set<ActionId>, apps: Set<String>): JSONArray {
        val tools = JSONArray()
        AiToolSchema.schemas(supported, apps).forEach {
            tools.put(it)
        }
        require(tools.length() > 0)
        return JSONArray().put(JSONObject().put("type", "namespace").put("name", "device")
            .put("description", "DeX Assistant validated Android actions, action schema 1.").put("tools", tools))
    }
    fun parse(response: JSONObject, registry: CommandRegistry, supported: Set<ActionId>): List<TypedAction> {
        require(response.getString("status") == "completed")
        val output = response.getJSONArray("output"); val calls = mutableListOf<JSONObject>()
        val ids = mutableSetOf<String>()
        for (i in 0 until output.length()) {
            val item = output.getJSONObject(i)
            if (item.getString("type") == "reasoning") continue
            require(item.getString("type") == "function_call" && item.getString("status") == "completed")
            require(item.getString("namespace") == "device")
            require(item.getString("call_id").isNotBlank() && ids.add(item.getString("call_id")))
            val args = item.getString("arguments")
            require(args.length <= 16384)
            calls.add(JSONObject().put("name", item.getString("name")).put("args", JSONObject(args)))
        }
        return AiToolCallValidator.validate(calls, registry, supported)
    }
}
