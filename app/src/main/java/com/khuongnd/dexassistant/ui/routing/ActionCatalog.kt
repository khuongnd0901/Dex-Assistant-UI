package com.khuongnd.dexassistant.ui.routing

/** Shared DEVICE_COMMAND actions. Execution policy is deliberately absent from config. */
enum class ActionId {
    APP_LAUNCH, WIFI_SET, BLUETOOTH_SET, VOLUME_SET, BRIGHTNESS_SET, MEDIA_TRANSPORT,
    YOUTUBE_SEARCH, YOUTUBE_PLAY_QUERY, YOUTUBE_SELECT, YOUTUBE_FULLSCREEN, MEDIA_SEEK,
    NAVIGATE, SEARCH_PLACE, CALL_CONTACT, CALL_NUMBER, WEB_SEARCH, CLICK_TEXT, SCROLL,
    FEED_NEXT, FEED_PREVIOUS, SYSTEM_ACTION, APP_PAIR_LAUNCH, CLICK_TARGET, APP_CLOSE_WINDOW,
}

data class TypedAction(val action: ActionId, val params: Map<String, Any>) {
    fun string(name: String): String = params[name] as? String ?: error("Missing text parameter: $name")
    fun int(name: String): Int = params[name] as? Int ?: error("Missing integer parameter: $name")
    fun boolean(name: String): Boolean = params[name] as? Boolean ?: error("Missing boolean parameter: $name")
}

object ActionCatalog {
    const val schemaVersion = 1
    const val semanticSchemaVersion = 2
    val schemaTwoOnlyActions = setOf(ActionId.CLICK_TARGET, ActionId.APP_CLOSE_WINDOW)
    private val appId = Regex("[a-z][a-z0-9_]{0,63}")
    private val phone = Regex("\\+?[0-9]{3,15}")
    fun validate(action: String, params: Map<String, Any>, version: Int = schemaVersion): TypedAction {
        require(version == schemaVersion || version == semanticSchemaVersion)
        val id = ActionId.valueOf(action)
        require(id !in schemaTwoOnlyActions || version == semanticSchemaVersion)
        fun keys(vararg allowed: String) { require(params.keys.all { it in allowed }) { "Unknown parameter for $id" } }
        fun text(key: String, max: Int = 500): String {
            val value = params[key] as? String ?: error("Expected text: $key")
            require(value.isNotBlank() && value.length <= max && value.none { it.isISOControl() }) { "Invalid text: $key" }
            return value
        }
        fun integer(key: String, range: IntRange) {
            require((params[key] as? Int)?.let { it in range } == true) { "Invalid integer: $key" }
        }
        fun bool(key: String) { require(params[key] is Boolean) { "Expected boolean: $key" } }
        fun option(key: String, vararg values: String): String = text(key).also { require(it in values) { "Invalid $key" } }
        when (id) {
            ActionId.APP_LAUNCH, ActionId.APP_CLOSE_WINDOW -> { keys("app"); require(appId.matches(text("app", 64))) }
            ActionId.APP_PAIR_LAUNCH -> {
                keys("firstApp", "secondApp")
                val first = text("firstApp", 64); val second = text("secondApp", 64)
                require(appId.matches(first) && appId.matches(second) && first != second)
            }
            ActionId.WIFI_SET, ActionId.BLUETOOTH_SET, ActionId.YOUTUBE_FULLSCREEN -> { keys("enabled"); bool("enabled") }
            ActionId.VOLUME_SET -> {
                val op = option("operation", "SET", "INCREASE", "DECREASE", "MUTE", "UNMUTE")
                when (op) {
                    "SET" -> { keys("operation", "percent"); integer("percent", 0..100) }
                    "INCREASE", "DECREASE" -> { keys("operation", "steps"); integer("steps", 1..10) }
                    else -> keys("operation")
                }
            }
            ActionId.BRIGHTNESS_SET -> {
                val op = option("operation", "SET", "INCREASE", "DECREASE", "AUTO_ON", "AUTO_OFF")
                if (op == "SET") { keys("operation", "percent"); integer("percent", 0..100) } else keys("operation")
            }
            ActionId.MEDIA_TRANSPORT -> { keys("operation"); option("operation", "PLAY", "PAUSE", "STOP", "NEXT", "PREVIOUS") }
            ActionId.YOUTUBE_SEARCH, ActionId.YOUTUBE_PLAY_QUERY, ActionId.SEARCH_PLACE, ActionId.WEB_SEARCH -> { keys("query"); text("query") }
            ActionId.YOUTUBE_SELECT -> {
                keys("index", "title"); require(params.size == 1)
                if (params.containsKey("index")) integer("index", 1..50) else text("title")
            }
            ActionId.MEDIA_SEEK -> { keys("operation", "seconds"); option("operation", "FORWARD", "BACKWARD"); integer("seconds", 1..3600) }
            ActionId.NAVIGATE -> { keys("destination", "travelMode"); text("destination"); option("travelMode", "DRIVING", "WALKING") }
            ActionId.CALL_CONTACT -> { keys("contact"); text("contact", 200) }
            ActionId.CALL_NUMBER -> { keys("number"); require(phone.matches(text("number", 16))) }
            ActionId.CLICK_TEXT -> { keys("text"); text("text", 200) }
            ActionId.SCROLL -> { keys("direction"); option("direction", "FORWARD", "BACKWARD") }
            ActionId.FEED_NEXT, ActionId.FEED_PREVIOUS -> keys()
            ActionId.SYSTEM_ACTION -> { keys("operation"); option("operation", "BACK", "HOME", "SETTINGS", "NOTIFICATIONS") }
            ActionId.CLICK_TARGET -> {
                keys("target")
                require(params.size == 1)
                val target = params["target"] as? Map<*, *> ?: error("Expected target object")
                require(target.keys.all { it is String && it in setOf("appId", "role", "text", "contentDescription", "resourceId", "contextText", "ordinal") })
                val targetApp = target["appId"] as? String ?: error("Expected appId")
                require(appId.matches(targetApp))
                if (target.containsKey("role")) require(target["role"] in setOf("BUTTON", "TEXT", "IMAGE", "LIST_ITEM"))
                val identifiers = listOf("text", "contentDescription", "resourceId", "contextText")
                require(identifiers.any { target.containsKey(it) })
                identifiers.filter(target::containsKey).forEach { key ->
                    val value = target[key] as? String ?: error("Expected text: $key")
                    require(value.isNotBlank() && value.length <= 200 && value.none { it.isISOControl() })
                }
                if (target.containsKey("ordinal")) require((target["ordinal"] as? Int)?.let { it in 1..50 } == true)
            }
        }
        val immutableParams = if (id == ActionId.CLICK_TARGET) mapOf("target" to java.util.Collections.unmodifiableMap(LinkedHashMap(params["target"] as Map<*, *>))) else params
        return TypedAction(id, java.util.Collections.unmodifiableMap(LinkedHashMap(immutableParams)))
    }
}
