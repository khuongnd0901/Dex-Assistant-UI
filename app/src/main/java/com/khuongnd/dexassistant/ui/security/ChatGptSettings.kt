package com.khuongnd.dexassistant.ui.security

import android.content.Context
import android.content.SharedPreferences
import com.khuongnd.dexassistant.ui.ai.SubscriptionAcceptance
import com.khuongnd.dexassistant.ui.auth.ChatGptAuthManager

/** Subscription preferences reuse existing keys without reading or deleting legacy API credentials. */
class ChatGptSettings(private val context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ai_provider_settings_v1", Context.MODE_PRIVATE)
    fun routingSnapshot(): Map<String, *> = prefs.all.filterKeys {
        it.startsWith("CHATGPT_SUBSCRIPTION_") || it.startsWith("subscription_") || it == "default_provider"
    }
    fun observeRouting(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }
    fun enabled(): Boolean = prefs.getBoolean("CHATGPT_SUBSCRIPTION_enabled", false)
    fun setEnabled(value: Boolean) { check(prefs.edit().putBoolean("CHATGPT_SUBSCRIPTION_enabled", value).commit()) }
    fun model(): String = prefs.getString("CHATGPT_SUBSCRIPTION_model", "").orEmpty()
    fun setModel(modelId: String) {
        val value = modelId.trim()
        require(value.length <= 128 && value.all { it.isLetterOrDigit() || it in "-_./" })
        check(prefs.edit().putString("CHATGPT_SUBSCRIPTION_model", value)
            .remove("subscription_chat_proof").remove("subscription_tools_proof").commit())
    }
    /** A legacy provider preference cannot activate subscription without a deliberate selection. */
    fun selected(): Boolean = prefs.getString("default_provider", null) == "CHATGPT_SUBSCRIPTION"
    fun select() {
        val auth = ChatGptAuthManager.get(context)
        val connection = auth.connectionKey()
        require(SubscriptionAcceptance.MANUAL_SELECTION_ACCEPTED && auth.connected() && enabled() && subscriptionVerified(connection, false) && subscriptionVerified(connection, true)) {
            "Subscription account/model acceptance pending"
        }
        check(prefs.edit().putString("default_provider", "CHATGPT_SUBSCRIPTION").commit())
    }
    fun subscriptionVerified(connection: String?, tools: Boolean): Boolean = SubscriptionProof.matches(
        prefs.getString(if (tools) "subscription_tools_proof" else "subscription_chat_proof", null), connection, model())
    internal fun recordSubscriptionVerification(connection: String, model: String, tools: Boolean) {
        require(model == model())
        check(prefs.edit().putString(if (tools) "subscription_tools_proof" else "subscription_chat_proof",
            SubscriptionProof.value(connection, model)).commit())
    }
    fun clearSubscriptionVerification() {
        check(prefs.edit().remove("subscription_chat_proof").remove("subscription_tools_proof").commit())
    }
}

/** Proofs are scoped to the validated account/client and exact selected model, never to all models. */
internal object SubscriptionProof {
    fun value(connection: String, model: String): String {
        require(connection.matches(Regex("[a-f0-9]{64}")) && model.matches(Regex("[A-Za-z0-9._/-]{1,128}")))
        return "$connection:$model"
    }
    fun matches(stored: String?, connection: String?, model: String): Boolean =
        connection != null && model.isNotBlank() && runCatching { stored == value(connection, model) }.getOrDefault(false)
}
