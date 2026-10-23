package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.protocol.DeviceAction
import com.khuongnd.dexassistant.ui.protocol.DeviceCommandProtocol
import com.khuongnd.dexassistant.ui.routing.*
import org.json.JSONObject
import java.util.UUID

/** Sanitized failure, with no provider fallback or raw transport details. */
class MobileAiException(val code: String) : RuntimeException(code)

/** Android validates every completed function call before it can enter ActionRouter. */
internal object AiToolCallValidator {
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
