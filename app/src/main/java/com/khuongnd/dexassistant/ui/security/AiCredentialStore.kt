package com.khuongnd.dexassistant.ui.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** User-entered AI provider credentials. Separate from the legacy backend bearer token. */
enum class AiProvider(val displayName: String, val supportsDirectCommands: Boolean) {
    CHATGPT_SUBSCRIPTION("ChatGPT Subscription", false),
    GEMINI("Gemini", true),
    GROQ("Groq", true),
    OPENAI("OpenAI API key", false),
    GROK("Grok (xAI)", false),
    DEEPSEEK("DeepSeek", false),
}

class AiCredentialStore(context: Context, private val aliasPrefix: String = "dex-assistant-ai-key-v1-") {
    private val prefs = context.applicationContext.getSharedPreferences("ai_provider_keys_v1", Context.MODE_PRIVATE)

    @Synchronized
    fun save(provider: AiProvider, apiKey: String) {
        require(provider != AiProvider.CHATGPT_SUBSCRIPTION) { "Subscription uses OAuth" }
        val normalized = apiKey.trim()
        require(normalized.isNotEmpty() && normalized.length <= 1024 && normalized.none(Char::isWhitespace)) {
            "Invalid API key format"
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key(provider))
        val encrypted = cipher.doFinal(normalized.toByteArray(Charsets.UTF_8))
        check(prefs.edit()
            .putString(provider.name + "_encrypted", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(provider.name + "_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .commit()) { "Cannot store AI key" }
    }

    /** Never use this value for UI display or logging. */
    @Synchronized
    fun load(provider: AiProvider): String? {
        val encrypted = prefs.getString(provider.name + "_encrypted", null) ?: return null
        val iv = prefs.getString(provider.name + "_iv", null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(provider),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    @Synchronized
    fun hasKey(provider: AiProvider) = load(provider) != null

    @Synchronized
    fun delete(provider: AiProvider) {
        check(prefs.edit().remove(provider.name + "_encrypted").remove(provider.name + "_iv").commit()) {
            "Cannot delete AI key"
        }
        val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keystore.containsAlias(alias(provider))) keystore.deleteEntry(alias(provider))
    }

    private fun key(provider: AiProvider): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias(provider), null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias(provider),
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
            generateKey()
        }
    }

    private fun alias(provider: AiProvider) = aliasPrefix + provider.name.lowercase()

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

/** Non-secret provider preferences and opt-in direct Command routing. */
class AiProviderSettings(private val context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ai_provider_settings_v1", Context.MODE_PRIVATE)

    fun routingSnapshot(): Map<String, *> = prefs.all.toMap()

    fun observeRouting(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun enabled(provider: AiProvider): Boolean = prefs.getBoolean(provider.name + "_enabled", false)

    fun setEnabled(provider: AiProvider, value: Boolean) {
        check(prefs.edit().putBoolean(provider.name + "_enabled", value).commit())
    }

    fun model(provider: AiProvider): String = prefs.getString(provider.name + "_model", "") ?: ""

    fun setModel(provider: AiProvider, modelId: String) {
        val normalized = modelId.trim()
        require(normalized.length <= 128 && normalized.all {
            it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' || it == '/'
        }) { "Invalid model ID" }
        // Confirm free-tier eligibility again whenever model changes.
        val edit = prefs.edit().putString(provider.name + "_model", normalized)
            .putBoolean(provider.name + "_free_confirmed", false)
        if (provider == AiProvider.CHATGPT_SUBSCRIPTION) edit.remove("subscription_chat_proof").remove("subscription_tools_proof")
        check(edit.commit())
    }

    fun freeTierConfirmed(provider: AiProvider): Boolean =
        prefs.getBoolean(provider.name + "_free_confirmed", false)

    fun setFreeTierConfirmed(provider: AiProvider, confirmed: Boolean) {
        check(prefs.edit().putBoolean(provider.name + "_free_confirmed", confirmed).commit())
    }

    /** Default provider is independent of each provider's default model and key. */
    fun defaultProvider(): AiProvider = AiProvider.entries.firstOrNull {
        it.name == prefs.getString("default_provider", AiProvider.GEMINI.name)
    } ?: AiProvider.GEMINI

    fun setDefaultProvider(provider: AiProvider) {
        if (provider == AiProvider.CHATGPT_SUBSCRIPTION) {
            val auth = com.khuongnd.dexassistant.ui.auth.ChatGptAuthManager.get(context)
            val connection = auth.connectionKey()
            require(auth.connected() && enabled(provider) &&
                com.khuongnd.dexassistant.ui.ai.SubscriptionAcceptance.MANUAL_SELECTION_ACCEPTED &&
                subscriptionVerified(connection, false) && subscriptionVerified(connection, true)) {
                "Subscription account/model acceptance pending"
            }
        }
        check(prefs.edit().putString("default_provider", provider.name).commit())
    }

    fun subscriptionVerified(connection: String?, tools: Boolean): Boolean = SubscriptionProof.matches(
        prefs.getString(if (tools) "subscription_tools_proof" else "subscription_chat_proof", null),
        connection, model(AiProvider.CHATGPT_SUBSCRIPTION))

    internal fun recordSubscriptionVerification(connection: String, model: String, tools: Boolean) {
        require(model == this.model(AiProvider.CHATGPT_SUBSCRIPTION))
        check(prefs.edit().putString(if (tools) "subscription_tools_proof" else "subscription_chat_proof",
            SubscriptionProof.value(connection, model)).commit())
    }

    fun clearSubscriptionVerification() {
        check(prefs.edit().remove("subscription_chat_proof").remove("subscription_tools_proof").commit())
    }

    /** Explicit user opt-in; defaults to the tested legacy WebSocket transport. */
    fun directMode(): Boolean = prefs.getBoolean("direct_command_enabled", false)

    fun setDirectMode(enabled: Boolean) {
        check(prefs.edit().putBoolean("direct_command_enabled", enabled).commit())
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
