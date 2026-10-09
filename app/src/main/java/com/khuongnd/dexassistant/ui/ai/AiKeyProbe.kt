package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.security.AiProvider
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Authentication-only probe. Lists models; it never creates an inference request,
 * selects a paid model, or invokes an Android device action.
 */
class AiKeyProbe(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build(),
) {
    fun test(provider: AiProvider, apiKey: String): ProbeResult {
        val request = when (provider) {
            AiProvider.GEMINI -> Request.Builder()
                .url(GEMINI_MODELS_URL)
                .header("x-goog-api-key", apiKey)
                .get().build()
            AiProvider.GROQ -> Request.Builder()
                .url(GROQ_MODELS_URL)
                .header("Authorization", "Bearer " + apiKey)
                .get().build()
        }
        return try {
            client.newCall(request).execute().use { response ->
                ProbeResult(statusFor(response.code), response.code)
            }
        } catch (_: IOException) {
            ProbeResult(ProbeStatus.NETWORK_ERROR)
        }
    }

    companion object {
        const val GEMINI_MODELS_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val GROQ_MODELS_URL = "https://api.groq.com/openai/v1/models"

        fun statusFor(httpCode: Int): ProbeStatus = when (httpCode) {
            in 200..299 -> ProbeStatus.AUTHENTICATED
            401 -> ProbeStatus.INVALID_KEY
            403 -> ProbeStatus.ACCESS_DENIED
            429 -> ProbeStatus.QUOTA_LIMITED
            else -> ProbeStatus.HTTP_ERROR
        }
    }
}

enum class ProbeStatus {
    AUTHENTICATED, INVALID_KEY, ACCESS_DENIED, QUOTA_LIMITED, NETWORK_ERROR, HTTP_ERROR
}
data class ProbeResult(val status: ProbeStatus, val httpStatus: Int? = null)
