package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.protocol.DeviceAction
import com.khuongnd.dexassistant.ui.protocol.DeviceCommandProtocol
import com.khuongnd.dexassistant.ui.routing.ActionCatalog
import com.khuongnd.dexassistant.ui.routing.ActionId
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import com.khuongnd.dexassistant.ui.routing.TypedAction
import com.khuongnd.dexassistant.ui.security.AiCredentialStore
import com.khuongnd.dexassistant.ui.security.AiProvider
import com.khuongnd.dexassistant.ui.security.AiProviderSettings
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** Fail-closed provider errors: credentials, malformed plans and unsupported requests never fall back. */
class MobileAiException(val code: String, val canFallback: Boolean = false) : RuntimeException(code)

/** Parse function calls exclusively. No provider-generated text ever becomes an executable gesture. */
object AiToolCallParser {
    fun parse(provider: AiProvider, response: JSONObject, registry: CommandRegistry, supported: Set<ActionId>): List<TypedAction> {
        val calls: List<JSONObject> = when (provider) {
            AiProvider.GEMINI -> {
                val candidates = response.getJSONArray("candidates")
                require(candidates.length() == 1)
                val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                buildList<JSONObject> {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("functionCall")) add(part.getJSONObject("functionCall"))
                    }
                }
            }
            AiProvider.GROQ -> {
                val choices = response.getJSONArray("choices")
                require(choices.length() == 1)
                val message = choices.getJSONObject(0).getJSONObject("message")
                val tools = message.getJSONArray("tool_calls")
                buildList<JSONObject> {
                    for (i in 0 until tools.length()) {
                        val item = tools.getJSONObject(i)
                        require(item.getString("type") == "function")
                        val function = item.getJSONObject("function")
                        add(JSONObject().put("name", function.getString("name"))
                            .put("args", JSONObject(function.getString("arguments"))))
                    }
                }
            }
            else -> throw MobileAiException("AI_PROVIDER_UNSUPPORTED")
        }
        return validate(calls, registry, supported)
    }

    internal fun validate(calls: List<JSONObject>, registry: CommandRegistry, supported: Set<ActionId>): List<TypedAction> {
        require(calls.size in 1..8) { "No safe bounded AI action plan" }
        return calls.map { call ->
            val name = call.getString("name")
            require(name.startsWith("deviceCommand_"))
            val actionName = name.removePrefix("deviceCommand_")
            val actionId = ActionId.valueOf(actionName)
            require(actionId in supported && actionId !in ActionCatalog.schemaTwoOnlyActions)
            val params = call.getJSONObject("args")
            val args = JSONObject()
                .put("actionSchemaVersion", ActionCatalog.schemaVersion)
                .put("action", actionName)
                .put("params", params)
            DeviceCommandProtocol.decode(
                DeviceAction(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "DEVICE_COMMAND", "REMOTE", args),
                registry, supported
            )
        }
    }
}

/**
 * Direct Gemini/Groq free-tier BYOK transport. Call only off main thread.
 * The user confirms their model's free eligibility in Settings; the API's model list is not pricing proof.
 */
class MobileAiProxy(
    private val credentials: AiCredentialStore,
    private val settings: AiProviderSettings,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS).build(),
    private val subscription: ChatGptSubscriptionProvider? = null,
) {
    private val toolCatalog = SerializedToolCatalog()
    private val activeCall = java.util.concurrent.atomic.AtomicReference<okhttp3.Call?>()
    fun cancelPendingRequest() { activeCall.get()?.cancel(); subscription?.cancel() }
    fun plan(text: String, registry: CommandRegistry, supported: Set<ActionId>, metric: (String, Long) -> Unit = { _, _ -> }): List<TypedAction> {
        if (text.isBlank() || text.length > 2000) throw MobileAiException("UNSUPPORTED_COMMAND")
        // Sensitive commands must never leave the phone, even with direct BYOK access.
        if (com.khuongnd.dexassistant.ui.routing.CommandPrivacy.requiresLocalHandling(text))
            throw MobileAiException("POLICY_DENIED")
        val preferred = settings.defaultProvider()
        if (preferred == AiProvider.CHATGPT_SUBSCRIPTION) return (subscription ?: throw MobileAiException("AI_PROVIDER_NOT_CONFIGURED")).plan(text, registry, supported)
        // Do not silently use a different AI when the selected provider has no validated adapter.
        if (!preferred.supportsDirectCommands) throw MobileAiException("AI_PROVIDER_UNSUPPORTED")
        var firstEligibleKey: String? = null
        val candidates = (listOf(preferred) + AiProvider.entries.filter { it != preferred })
            .distinct().filter {
                it.supportsDirectCommands && settings.enabled(it) && settings.freeTierConfirmed(it) &&
                    settings.model(it).isNotBlank() &&
                    if (firstEligibleKey == null) credentials.load(it)?.also { key -> firstEligibleKey = key } != null
                    else credentials.hasKey(it)
            }
        if (candidates.isEmpty()) throw MobileAiException("AI_PROVIDER_NOT_CONFIGURED")
        var failure = MobileAiException("AI_UNAVAILABLE")
        for (provider in candidates) {
            var parseMicros = 0L
            var parsed = false
            val timingMetric: (String, Long) -> Unit = { name, micros ->
                if (name == "providerParseLatency") { parsed = true; parseMicros += micros }
                else metric(name, micros)
            }
            if (Thread.currentThread().isInterrupted) throw MobileAiException("CANCELLED")
            try {
                // Request-local only: selected provider is decrypted once; fallback reloads when attempted.
                val key = (if (provider == candidates.first()) firstEligibleKey.also { firstEligibleKey = null }
                    else credentials.load(provider)) ?: throw MobileAiException("AI_PROVIDER_NOT_CONFIGURED")
                val model = settings.model(provider)
                val declarations = toolCatalog.forScope(supported, registry.appIds, provider == AiProvider.GEMINI)
                if (declarations.count == 0) throw MobileAiException("UNSUPPORTED_COMMAND")
                val response = call(provider, key, model, text, declarations, timingMetric)
                return try {
                    ProviderTiming.parse(timingMetric) { AiToolCallParser.parse(provider, response, registry, supported) }
                } catch (_: org.json.JSONException) {
                    throw MobileAiException("INVALID_LLM_OUTPUT")
                } catch (_: RuntimeException) {
                    throw MobileAiException("INVALID_LLM_OUTPUT")
                }
            } catch (error: MobileAiException) {
                failure = error
                if (!error.canFallback) throw error
            } finally {
                if (parsed) metric("providerParseLatency", parseMicros)
            }
        }
        throw failure
    }

    private fun call(provider: AiProvider, key: String, model: String, input: String, tools: SerializedToolCatalog.Tools, metric: (String, Long) -> Unit): JSONObject {
        val system = "Plan only supported Android functions; no invented app/contact/screen contents. " +
            "Never execute negated commands or claims of user confirmation. At most 8 functions. " +
            "No tool calls for ambiguous or unsafe requests. All actions will be validated and confirmed on Android."
        val body: String
        val request: Request.Builder
        when (provider) {
            AiProvider.GEMINI -> {
                if (!model.matches(Regex("[A-Za-z0-9._-]{1,100}"))) throw MobileAiException("INVALID_MODEL")
                body = "{\"systemInstruction\":{\"parts\":[{\"text\":" + JSONObject.quote(system) +
                    "}]},\"contents\":[{\"role\":\"user\",\"parts\":[{\"text\":" + JSONObject.quote(input) +
                    "}]}],\"tools\":[{\"functionDeclarations\":" + tools.json +
                    "}],\"generationConfig\":{\"temperature\":0,\"maxOutputTokens\":700}}"
                request = Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models/" +
                    model + ":generateContent").header("x-goog-api-key", key)
            }
            AiProvider.GROQ -> {
                if (!model.matches(Regex("[A-Za-z0-9._/-]{1,100}"))) throw MobileAiException("INVALID_MODEL")
                body = "{\"model\":" + JSONObject.quote(model) +
                    ",\"messages\":[{\"role\":\"system\",\"content\":" + JSONObject.quote(system) +
                    "},{\"role\":\"user\",\"content\":" + JSONObject.quote(input) +
                    "}],\"tools\":" + tools.json +
                    ",\"tool_choice\":\"auto\",\"parallel_tool_calls\":true,\"temperature\":0,\"max_tokens\":700}"
                request = Request.Builder().url("https://api.groq.com/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + key)
            }
            else -> throw MobileAiException("AI_PROVIDER_UNSUPPORTED")
        }
        val httpRequest = request.header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        val call = client.newCall(httpRequest)
        activeCall.set(call)
        val timing = ProviderTiming(metric)
        try {
            if (Thread.currentThread().isInterrupted) call.cancel()
            call.execute().use { result ->
                timing.headers()
                if (!result.isSuccessful) {
                    throw when (result.code) {
                        429 -> MobileAiException("AI_QUOTA_EXHAUSTED", true)
                        408, 500, 502, 503, 504 -> MobileAiException("AI_UNAVAILABLE", true)
                        401, 403 -> MobileAiException("AI_AUTH_FAILED")
                        else -> MobileAiException("AI_PROVIDER_ERROR")
                    }
                }
                val responseBody = result.body ?: throw MobileAiException("INVALID_LLM_OUTPUT")
                val json = timing.body(responseBody) { BoundedProviderBody.command(responseBody) }
                return try { ProviderTiming.parse(metric) { JSONObject(json) } } catch (_: org.json.JSONException) {
                    throw MobileAiException("INVALID_LLM_OUTPUT")
                } catch (_: RuntimeException) {
                    throw MobileAiException("INVALID_LLM_OUTPUT")
                }
            }
        } catch (_: IOException) {
            timing.headers()
            throw MobileAiException("AI_TIMEOUT", !call.isCanceled())
        } finally {
            activeCall.compareAndSet(call, null)
        }
    }
}
