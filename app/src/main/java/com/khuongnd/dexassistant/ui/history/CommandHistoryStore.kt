package com.khuongnd.dexassistant.ui.history

import android.content.Context
import com.khuongnd.dexassistant.ui.routing.CommandPrivacy
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class HistoryEntry(val id: String, val timestamp: Long, val originalText: String, val route: String, val intent: String?, val outcome: String?)

object HistoryRetention {
    const val DEFAULT_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
    const val DEFAULT_MAX_COUNT = 100
    fun retain(entries: List<HistoryEntry>, now: Long, maxAgeMs: Long = DEFAULT_MAX_AGE_MS, maxCount: Int = DEFAULT_MAX_COUNT) =
        entries.filter { it.timestamp >= now - maxAgeMs }.sortedWith(compareByDescending<HistoryEntry> { it.timestamp }.thenByDescending { it.id }).take(maxCount)
}

class CommandHistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized fun record(originalText: String, route: String, intent: String?, outcome: String?, timestamp: Long = System.currentTimeMillis()) {
        val safeText = CommandPrivacy.redact(originalText)
        val entry = HistoryEntry(UUID.randomUUID().toString(), timestamp, safeText, route, intent, sanitize(outcome))
        persist(prune(load() + entry, timestamp))
    }

    @Synchronized fun read(now: Long = System.currentTimeMillis()): List<HistoryEntry> = prune(load(), now).also(::persist)
    @Synchronized fun clear() { preferences.edit().clear().commit(); keyStore().deleteEntry(ALIAS) }

    private fun prune(entries: List<HistoryEntry>, now: Long) = HistoryRetention.retain(entries, now)
    private fun sanitize(value: String?) = value?.let(CommandPrivacy::redact)?.take(80)
    private fun persist(entries: List<HistoryEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val clear = JSONObject().put("text", entry.originalText).put("route", entry.route).put("intent", entry.intent).put("outcome", entry.outcome).toString()
            val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
            array.put(JSONObject().put("id", entry.id).put("timestamp", entry.timestamp)
                .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .put("ciphertext", Base64.encodeToString(cipher.doFinal(clear.toByteArray()), Base64.NO_WRAP)))
        }
        preferences.edit().putString(RECORDS, array.toString()).commit()
    }
    private fun load(): List<HistoryEntry> = runCatching {
        val array = JSONArray(preferences.getString(RECORDS, "[]")); buildList {
            for (i in 0 until array.length()) { val stored = array.getJSONObject(i); val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(stored.getString("iv"), Base64.NO_WRAP)))
                val clear = JSONObject(String(cipher.doFinal(Base64.decode(stored.getString("ciphertext"), Base64.NO_WRAP))))
                add(HistoryEntry(stored.getString("id"), stored.getLong("timestamp"), CommandPrivacy.redact(clear.getString("text")), clear.getString("route"), clear.optString("intent").takeIf { it.isNotBlank() && it != "null" }, sanitize(clear.optString("outcome").takeIf { it.isNotBlank() && it != "null" }))) }
        }
    }.getOrElse { emptyList() }
    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun key(): SecretKey = (keyStore().getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
        init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()); generateKey() }
    companion object { private const val PREFS="command_history_v1"; private const val RECORDS="records"; private const val ALIAS="dex-assistant-history-v1"; private const val TRANSFORMATION="AES/GCM/NoPadding"
    }
}
