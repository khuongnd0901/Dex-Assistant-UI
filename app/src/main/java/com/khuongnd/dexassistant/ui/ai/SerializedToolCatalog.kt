package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.routing.ActionId
import org.json.JSONArray

/** One immutable schema for the current capability/app scope; no action or target is cached. */
internal class SerializedToolCatalog {
    private var actions: Set<ActionId>? = null
    private var apps: Set<String>? = null
    private var json: String? = null
    @Synchronized fun forScope(supported: Set<ActionId>, appIds: Set<String>): String {
        if (actions != supported || apps != appIds) {
            actions = supported.toSet(); apps = appIds.toSet(); json = null
        }
        return json ?: SubscriptionTools.schemas(actions!!, apps!!).toString().also { json = it }
    }
}
