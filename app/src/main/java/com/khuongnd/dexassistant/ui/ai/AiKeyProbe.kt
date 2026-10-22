package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.security.AiProvider
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Authenticate provider credentials and list accessible model IDs without inference.
 * Model availability does NOT establish free-tier access, tool support or pricing.
 */
class AiKeyProbe(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build(),
) {
    fun test(provider: AiProvider, apiKey: String): ProbeResult =
        listModels(provider, apiKey).let { ProbeResult(it.status, it.httpStatus) }

    fun listModels(provider: AiProvider, apiKey: String): ModelProbeResult {
        val request = Request.Builder().url(modelsEndpoint(provider)).apply {
            if (provider == AiProvider.GEMINI) header("x-goog-api-key", apiKey)
            else header("Authorization", "Bearer " + apiKey)
        }.get().build()
        return try {
            client.newCall(request).execute().use { response ->
                val status = statusFor(response.code)
                if (status != ProbeStatus.AUTHENTICATED) return ModelProbeResult(status, response.code)
                val body = response.body?.string()?.take(1_000_000) ?: ""
                runCatching { parseModels(provider, JSONObject(body)) }
                    .fold(
                        onSuccess = { ModelProbeResult(status, response.code, it) },
                        onFailure = { ModelProbeResult(ProbeStatus.HTTP_ERROR, response.code) },
                    )
            }
        } catch (_: IOException) {
            ModelProbeResult(ProbeStatus.NETWORK_ERROR)
        }
    }

    companion object {
        const val GEMINI_MODELS_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val GROQ_MODELS_URL = "https://api.groq.com/openai/v1/models"
        const val OPENAI_MODELS_URL = "https://api.openai.com/v1/models"
        const val GROK_MODELS_URL = "https://api.x.ai/v1/models"
        const val DEEPSEEK_MODELS_URL = "https://api.deepseek.com/models"

        fun modelsEndpoint(provider: AiProvider): String = when (provider) {
            AiProvider.CHATGPT_SUBSCRIPTION -> throw IllegalArgumentException("Subscription uses OAuth")
            AiProvider.GEMINI -> GEMINI_MODELS_URL
            AiProvider.GROQ -> GROQ_MODELS_URL
            AiProvider.OPENAI -> OPENAI_MODELS_URL
            AiProvider.GROK -> GROK_MODELS_URL
            AiProvider.DEEPSEEK -> DEEPSEEK_MODELS_URL
        }

        fun statusFor(httpCode: Int): ProbeStatus = when (httpCode) {
            in 200..299 -> ProbeStatus.AUTHENTICATED
            401 -> ProbeStatus.INVALID_KEY
            403 -> ProbeStatus.ACCESS_DENIED
            429 -> ProbeStatus.QUOTA_LIMITED
            else -> ProbeStatus.HTTP_ERROR
        }

        fun parseModels(provider: AiProvider, json: JSONObject): List<String> {
            val array = if (provider == AiProvider.GEMINI) json.getJSONArray("models") else json.getJSONArray("data")
            return buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    if (provider == AiProvider.GEMINI) {
                        val methods = item.optJSONArray("supportedGenerationMethods")
                        if (methods != null && (0 until methods.length()).none {
                                methods.optString(it) == "generateContent"
                            }) continue
                    }
                    val id = if (provider == AiProvider.GEMINI) item.getString("name").removePrefix("models/")
                        else item.getString("id")
                    if (id.isNotBlank() && id.length <= 128) add(id)
                }
            }.distinct().sorted().take(100)
        }
    }
}

enum class ProbeStatus {
    AUTHENTICATED, INVALID_KEY, ACCESS_DENIED, QUOTA_LIMITED, NETWORK_ERROR, HTTP_ERROR
}
data class ProbeResult(val status: ProbeStatus, val httpStatus: Int? = null)
data class ModelProbeResult(
    val status: ProbeStatus,
    val httpStatus: Int? = null,
    val models: List<String> = emptyList(),
)
