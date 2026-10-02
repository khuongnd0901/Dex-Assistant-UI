package com.khuongnd.dexassistant.ui.protocol

import org.json.JSONObject
import java.time.Instant

data class DeviceAction(
    val planId: String,
    val actionId: String,
    val tool: String,
    val executionMode: String,
    val arguments: JSONObject,
)

object QuotaFreshness {
    fun isStale(staleAfter: String, now: Instant = Instant.now()): Boolean = now >= Instant.parse(staleAfter)
}

object ProtocolParser {
    data class QuotaBudget(val unit: String, val used: Long?, val reserved: Long?, val limit: Long?, val remaining: Long?, val windowStart: String?, val resetAt: String?, val source: String, val observedAt: String?)
    data class QuotaProvider(val providerAlias: String, val modelAlias: String, val state: String, val budgets: List<QuotaBudget>)
    data class QuotaSnapshot(val snapshotAt: String, val staleAfter: String, val providers: List<QuotaProvider>)
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

    fun errorCode(message: String): String = JSONObject(message).getJSONObject("payload").optString("code", "ERROR")

    fun quotaSnapshot(message: String): QuotaSnapshot {
        val payload = JSONObject(message).getJSONObject("payload")
        require(payload.getInt("schemaVersion") == 1)
        val providersJson = payload.getJSONArray("providers")
        val providers = buildList { for (index in 0 until providersJson.length()) {
            val provider = providersJson.getJSONObject(index); val budgetsJson = provider.getJSONArray("budgets")
            val budgets = buildList { for (budgetIndex in 0 until budgetsJson.length()) { val budget = budgetsJson.getJSONObject(budgetIndex)
                fun nullableLong(name: String) = if (budget.isNull(name)) null else budget.getLong(name)
                fun nullableString(name: String) = if (budget.isNull(name)) null else budget.getString(name)
                add(QuotaBudget(budget.getString("unit"), nullableLong("used"), nullableLong("reserved"), nullableLong("limit"), nullableLong("remaining"), nullableString("windowStart"), nullableString("resetAt"), budget.getString("source"), nullableString("observedAt"))) }
            }
            add(QuotaProvider(provider.getString("providerAlias"), provider.getString("modelAlias"), provider.getString("state"), budgets))
        } }
        return QuotaSnapshot(payload.getString("snapshotAt"), payload.getString("staleAfter"), providers)
    }
}
