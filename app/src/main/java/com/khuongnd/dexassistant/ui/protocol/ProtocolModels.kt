package com.khuongnd.dexassistant.ui.protocol

import org.json.JSONObject

data class DeviceAction(
    val planId: String,
    val actionId: String,
    val tool: String,
    val executionMode: String,
    val arguments: JSONObject,
)

object ProtocolParser {
    fun actions(message: String): List<DeviceAction> {
        val envelope = JSONObject(message)
        require(envelope.getInt("version") == 1) { "Unsupported protocol version" }
        if (envelope.getString("type") != "execution.plan") return emptyList()
        val payload = envelope.getJSONObject("payload")
        val planId = payload.getString("planId")
        val actions = payload.getJSONArray("actions")
        return buildList {
            for (index in 0 until actions.length()) {
                val action = actions.getJSONObject(index)
                add(
                    DeviceAction(
                        planId = planId,
                        actionId = action.getString("actionId"),
                        tool = action.getString("tool"),
                        executionMode = action.getString("executionMode"),
                        arguments = action.getJSONObject("arguments"),
                    ),
                )
            }
        }
    }

    fun type(message: String): String = JSONObject(message).getString("type")

    fun errorSummary(message: String): String {
        val payload = JSONObject(message).getJSONObject("payload")
        return "${payload.optString("code", "ERROR")}: ${payload.optString("message", "Unknown error")}" 
    }
}
