package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.auth.ChatGptAuthManager
import com.khuongnd.dexassistant.ui.auth.ChatGptFailure
import com.khuongnd.dexassistant.ui.auth.ChatGptHttp
import org.json.JSONObject

class ChatGptModelRepository(private val auth: ChatGptAuthManager) {
    data class Model(val slug: String, val displayName: String)
    fun load(): List<Model> {
        var session = auth.freshSession()
        val json = try { ChatGptHttp.getJson("https://api.openai.com/v1/models", session.accessToken) }
        catch (e: ChatGptFailure) {
            if (e.code != "SESSION_EXPIRED") throw e
            session = auth.freshSession(session.accessToken)
            ChatGptHttp.getJson("https://api.openai.com/v1/models", session.accessToken)
        }
        return parse(json)
    }
    companion object {
        fun parse(json: JSONObject): List<Model> {
            val models = json.getJSONArray("models")
            require(models.length() <= 1000)
            return buildList {
                val seen = mutableSetOf<String>()
                for (i in 0 until models.length()) {
                    val item = models.getJSONObject(i)
                    if (item.optString("visibility") != "list") continue
                    val slug = item.getString("slug")
                    require(slug.matches(Regex("[A-Za-z0-9._/-]{1,128}")) && seen.add(slug))
                    add(Model(slug, item.optString("display_name").ifBlank { slug }.take(200)))
                }
            }
        }
    }
}
