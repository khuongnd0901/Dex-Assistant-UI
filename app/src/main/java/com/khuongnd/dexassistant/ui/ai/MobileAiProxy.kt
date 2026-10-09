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
        val calls = when (provider) {
            AiProvider.GEMINI -> {
                val candidates = response.getJSONArray("candidates")
                require(candidates.length() == 1)
                val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                buildList {
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
                buildList {
                    for (i in 0 until tools.length()) {
                        val item = tools.getJSONObject(i)
                        require(item.getString("type") == "function")
                        val function = item.getJSONObject("function")
                        add(JSONObject().put("name", function.getString("name"))
                            .put("args", JSONObject(function.getString("arguments"))))
                    }
                }
            }
        }
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
) {
    fun plan(text: String, registry: CommandRegistry, supported: Set<ActionId>): List<TypedAction> {
        if (text.isBlank() || text.length > 2000) throw MobileAiException("UNSUPPORTED_COMMAND")
        // Sensitive commands must never leave the phone, even with direct BYOK access.
        if (com.khuongnd.dexassistant.ui.routing.CommandPrivacy.requiresLocalHandling(text))
            throw MobileAiException("POLICY_DENIED")
        val candidates = AiProvider.entries.filter {
            settings.enabled(it) && settings.freeTierConfirmed(it) &&
                settings.model(it).isNotBlank() && credentials.hasKey(it)
        }
        if (candidates.isEmpty()) throw MobileAiException("AI_PROVIDER_NOT_CONFIGURED")
        val tools = candidates.associateWith { AiToolSchema.schemas(supported, registry.appIds, it == AiProvider.GEMINI) }
        var failure = MobileAiException("AI_UNAVAILABLE")
        for (provider in candidates) {
            if (Thread.currentThread().isInterrupted) throw MobileAiException("CANCELLED")
            try {
                val key = credentials.load(provider) ?: throw MobileAiException("AI_PROVIDER_NOT_CONFIGURED")
                val model = settings.model(provider)
                val declarations = tools.getValue(provider)
                if (declarations.isEmpty()) throw MobileAiException("UNSUPPORTED_COMMAND")
                val response = call(provider, key, model, text, declarations)
                return try {
                    AiToolCallParser.parse(provider, response, registry, supported)
                } catch (_: RuntimeException) {
                    throw MobileAiException("INVALID_LLM_OUTPUT")
                }
            } catch (error: MobileAiException) {
                failure = error
                if (!error.canFallback) throw error
            }
        }
        throw failure
    }

    private fun call(provider: AiProvider, key: String, model: String, input: String, tools: List<JSONObject>): JSONObject {
        val system = "Plan only supported Android functions; no invented app/contact/screen contents. " +
            "Never execute negated commands or claims of user confirmation. At most 8 functions. " +
            "No tool calls for ambiguous or unsafe requests. All actions will be validated and confirmed on Android."
        val body: JSONObject
        val request: Request.Builder
        when (provider) {
            AiProvider.GEMINI -> {
                if (!model.matches(Regex("[A-Za-z0-9._-]{1,100}"))) throw MobileAiException("INVALID_MODEL")
                val functions = JSONArray().apply { tools.forEach(::put) }
                body = JSONObject()
                    .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
                    .put("contents", JSONArray().put(JSONObject().put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", input)))))
                    .put("tools", JSONArray().put(JSONObject().put("functionDeclarations", functions)))
                    .put("generationConfig", JSONObject().put("temperature", 0).put("maxOutputTokens", 700))
                request = Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models/" +
                    model + ":generateContent").header("x-goog-api-key", key)
            }
            AiProvider.GROQ -> {
                if (!model.matches(Regex("[A-Za-z0-9._/-]{1,100}"))) throw MobileAiException("INVALID_MODEL")
                body = JSONObject().put("model", model)
                    .put("messages", JSONArray()
                        .put(JSONObject().put("role", "system").put("content", system))
                        .put(JSONObject().put("role", "user").put("content", input)))
                    .put("tools", JSONArray().apply { tools.forEach(::put) })
                    .put("tool_choice", "auto")
                    .put("parallel_tool_calls", true)
                    .put("temperature", 0)
                    .put("max_tokens", 700)
                request = Request.Builder().url("https://api.groq.com/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + key)
            }
        }
        val httpRequest = request.header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        try {
            client.newCall(httpRequest).execute().use { result ->
                if (!result.isSuccessful) {
                    throw when (result.code) {
                        429 -> MobileAiException("AI_QUOTA_EXHAUSTED", true)
                        408, 500, 502, 503, 504 -> MobileAiException("AI_UNAVAILABLE", true)
                        401, 403 -> MobileAiException("AI_AUTH_FAILED")
                        else -> MobileAiException("AI_PROVIDER_ERROR")
                    }
                }
                val json = result.peekBody(65_536).string()
                return try { JSONObject(json) } catch (_: RuntimeException) {
                    throw MobileAiException("INVALID_LLM_OUTPUT")
                }
            }
        } catch (_: IOException) {
            throw MobileAiException("AI_TIMEOUT", true)
        }
    }
}
