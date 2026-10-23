package com.khuongnd.dexassistant.ui.auth

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import org.json.JSONObject

/** Never surface provider bodies, URLs, headers, or parser exceptions to UI/logs. */
class ChatGptFailure(val code: String) : RuntimeException(code)

internal object ChatGptHttp {
    val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()

    fun getJson(url: String, bearer: String? = null): JSONObject {
        val builder = Request.Builder().url(url)
        if (bearer != null) builder.header("Authorization", "Bearer $bearer")
        return json(builder.build())
    }

    fun postForm(url: String, fields: Map<String, String>, emptyAllowed: Boolean = false): JSONObject =
        json(Request.Builder().url(url).post(FormBody.Builder().apply {
            fields.forEach { (k, v) -> add(k, v) }
        }.build()).build(), emptyAllowed)

    private fun json(request: Request, emptyAllowed: Boolean = false): JSONObject {
        require(request.url.isHttps && request.url.host in setOf("auth.openai.com", "api.openai.com"))
        return try {
            client.newCall(request).execute().use { response ->
                if (response.code == 400 && request.url.encodedPath.endsWith("/oauth/token"))
                    throw ChatGptFailure("SESSION_EXPIRED")
                checkStatus(response.code)
                val source = response.body?.source() ?: throw ChatGptFailure("INVALID_RESPONSE")
                if (source.request(1024 * 1024L + 1)) throw ChatGptFailure("RESPONSE_TOO_LARGE")
                val body = source.readUtf8()
                if (emptyAllowed && body.isEmpty()) JSONObject() else JSONObject(body)
            }
        } catch (e: ChatGptFailure) { throw e }
        catch (_: Exception) { throw ChatGptFailure("NETWORK_OR_INVALID_RESPONSE") }
    }

    fun checkStatus(status: Int) {
        if (status in 200..299) return
        throw ChatGptFailure(when (status) {
            400 -> "INVALID_REQUEST"
            401 -> "SESSION_EXPIRED"
            404 -> "MODEL_UNAVAILABLE"
            403 -> "ACCESS_DENIED"
            429 -> "QUOTA_LIMITED"
            408, 500, 502, 503, 504 -> "PROVIDER_UNAVAILABLE"
            else -> "PROVIDER_ERROR"
        })
    }
}
