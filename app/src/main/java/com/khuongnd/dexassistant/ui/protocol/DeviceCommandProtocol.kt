package com.khuongnd.dexassistant.ui.protocol

import com.khuongnd.dexassistant.ui.routing.*
import org.json.JSONArray
import org.json.JSONObject

object DeviceCommandProtocol {
    fun capabilities(registry: CommandRegistry, supported: Set<ActionId>): JSONObject = JSONObject()
        .put("actionSchemaVersion",ActionCatalog.schemaVersion)
        .put("supportedActions",JSONArray(supported.map { it.name }.sorted()))
        .put("configuredAppIds",JSONArray(registry.appIds.sorted()))

    fun decode(action: DeviceAction, registry: CommandRegistry, supported: Set<ActionId>): TypedAction {
        require(action.executionMode == "REMOTE")
        val arguments = action.arguments
        val validated = when (action.tool) {
            "DEVICE_COMMAND" -> {
                require(arguments.keys().asSequence().toSet() == setOf("actionSchemaVersion","action","params"))
                require(arguments.get("actionSchemaVersion") is Int && arguments.getInt("actionSchemaVersion") == ActionCatalog.schemaVersion)
                val name = arguments.get("action").also { require(it is String) } as String
                val params = arguments.get("params").also { require(it is JSONObject) } as JSONObject
                ActionCatalog.validate(name,params.keys().asSequence().associateWith { key ->
                    params.get(key).also { require(it is String || it is Int || it is Boolean) }
                })
            }
            "TOGGLE_WIFI" -> {
                require(arguments.keys().asSequence().toSet() == setOf("enabled"))
                ActionCatalog.validate("WIFI_SET",mapOf("enabled" to arguments.get("enabled")))
            }
            "LAUNCH_APP" -> {
                require(arguments.keys().asSequence().all { it in setOf("packageName","deepLink") })
                val pkg = arguments.get("packageName").also { require(it is String) } as String
                // Legacy deep links have no typed action schema; retain only configured app launch.
                require(!arguments.has("deepLink") || arguments.isNull("deepLink") || arguments.get("deepLink") == "")
                val app = registry.appIdForPackage(pkg) ?: error("App is not configured")
                ActionCatalog.validate("APP_LAUNCH",mapOf("app" to app))
            }
            else -> error("Unsupported tool")
        }
        require(validated.action in supported)
        when (validated.action) {
            ActionId.APP_LAUNCH -> require(registry.appPackage(validated.string("app")) != null)
            ActionId.APP_PAIR_LAUNCH -> {
                val first = validated.string("firstApp"); val second = validated.string("secondApp")
                require(first in registry.appIds && second in registry.appIds && first != second)
            }
            else -> Unit
        }
        return validated
    }
}
