package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.routing.ActionId
import org.json.JSONArray

/** At most two immutable strings for the current capability/app scope; no action or target is cached. */
internal class SerializedToolCatalog {
    data class Tools(val json: String, val count: Int)
    private var actions: Set<ActionId>? = null
    private var apps: Set<String>? = null
    private val providers = mutableMapOf<Boolean, Tools>()

    @Synchronized fun forScope(supported: Set<ActionId>, appIds: Set<String>, gemini: Boolean): Tools {
        if (actions != supported || apps != appIds) {
            actions = supported.toSet(); apps = appIds.toSet(); providers.clear()
        }
        return providers.getOrPut(gemini) {
            val schemas = AiToolSchema.schemas(actions!!, apps!!, gemini)
            Tools(JSONArray().apply { schemas.forEach(::put) }.toString(), schemas.size)
        }
    }
}
