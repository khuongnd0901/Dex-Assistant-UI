package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.routing.ActionId
import com.khuongnd.dexassistant.ui.routing.ActionCatalog
import org.json.JSONArray
import org.json.JSONObject

/** Device capability scoped, subscription function schemas. Android revalidates every call. */
object AiToolSchema {
    fun schemas(supported: Set<ActionId>, appIds: Set<String>): List<JSONObject> =
        supported.sortedBy { it.name }.mapNotNull { id ->
            if (id in ActionCatalog.schemaTwoOnlyActions) return@mapNotNull null
            if (id in setOf(ActionId.APP_LAUNCH, ActionId.APP_PAIR_LAUNCH) &&
                appIds.size < if (id == ActionId.APP_PAIR_LAUNCH) 2 else 1) return@mapNotNull null

            val p = linkedMapOf<String, JSONObject>()
            val required = mutableListOf<String>()
            fun str(name: String, choices: List<String>? = null) {
                p[name] = JSONObject().put("type", "string").apply {
                    if (choices != null) put("enum", JSONArray(choices))
                }
                required.add(name)
            }
            fun integer(name: String, min: Int, max: Int, mandatory: Boolean = true) {
                p[name] = JSONObject().put("type", "integer").put("minimum", min).put("maximum", max)
                if (mandatory) required.add(name)
            }
            fun flag(name: String) {
                p[name] = JSONObject().put("type", "boolean")
                required.add(name)
            }
            val apps = appIds.sorted()
            when (id) {
                ActionId.APP_LAUNCH, ActionId.APP_CLOSE_WINDOW -> str("app", apps)
                ActionId.APP_PAIR_LAUNCH -> { str("firstApp", apps); str("secondApp", apps) }
                ActionId.WIFI_SET, ActionId.BLUETOOTH_SET, ActionId.YOUTUBE_FULLSCREEN -> flag("enabled")
                ActionId.VOLUME_SET -> { str("operation", listOf("SET", "INCREASE", "DECREASE", "MUTE", "UNMUTE")); integer("percent", 0, 100, false); integer("steps", 1, 10, false) }
                ActionId.BRIGHTNESS_SET -> { str("operation", listOf("SET", "INCREASE", "DECREASE", "AUTO_ON", "AUTO_OFF")); integer("percent", 0, 100, false) }
                ActionId.MEDIA_TRANSPORT -> str("operation", listOf("PLAY", "PAUSE", "STOP", "NEXT", "PREVIOUS"))
                ActionId.YOUTUBE_SEARCH, ActionId.YOUTUBE_PLAY_QUERY, ActionId.SEARCH_PLACE, ActionId.WEB_SEARCH -> str("query")
                ActionId.YOUTUBE_SELECT -> { integer("index", 1, 50, false); p["title"] = JSONObject().put("type", "string") }
                ActionId.MEDIA_SEEK -> { str("operation", listOf("FORWARD", "BACKWARD")); integer("seconds", 1, 3600) }
                ActionId.NAVIGATE -> { str("destination"); str("travelMode", listOf("DRIVING", "WALKING")) }
                ActionId.CALL_CONTACT -> str("contact")
                ActionId.CALL_NUMBER -> str("number")
                ActionId.CLICK_TEXT -> str("text")
                ActionId.SCROLL -> str("direction", listOf("FORWARD", "BACKWARD"))
                ActionId.SYSTEM_ACTION -> str("operation", listOf("BACK", "HOME", "SETTINGS", "NOTIFICATIONS"))
                ActionId.FEED_NEXT, ActionId.FEED_PREVIOUS -> Unit
                ActionId.CLICK_TARGET -> return@mapNotNull null
            }
            val params = JSONObject().put("type", "object").put("properties", JSONObject(p))
                .put("required", JSONArray(required))
            params.put("additionalProperties", false)
            val declaration = JSONObject().put("name", "deviceCommand_" + id.name)
                .put("description", "Request " + id.name + "; Android validates capability, consent, and execution.")
                .put("parameters", params)
            declaration.put("type", "function").put("strict", false)
        }

}
