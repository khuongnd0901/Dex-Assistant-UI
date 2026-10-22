// Adapted from ChatGPT-Subscription-Android-POC (MIT); see third-party/siwc-poc-LICENSE.
package com.khuongnd.dexassistant.ui.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

/** Deliberately has no data-class toString exposing tokens. */
class ChatGptSession(val clientId: String, val subject: String, val email: String,
    val idToken: String, val accessToken: String, val refreshToken: String,
    val scopes: String, val expiresAt: Long) {
    fun hasSubscriptionScope() = scopes.split(' ').containsAll(listOf("resource.invoke", "chatgpt.tokens.use.direct"))
    internal fun json() = JSONObject().put("client_id", clientId).put("sub", subject).put("email", email)
        .put("id_token", idToken).put("access_token", accessToken).put("refresh_token", refreshToken)
        .put("scope", scopes).put("expires_at", expiresAt)
    companion object {
        internal fun from(j: JSONObject) = ChatGptSession(j.getString("client_id"), j.getString("sub"),
            j.optString("email"), j.getString("id_token"), j.getString("access_token"),
            j.optString("refresh_token"), j.getString("scope"), j.getLong("expires_at"))
    }
}

/** Atomic AES-GCM records in noBackupFilesDir; existing backup policies remain intact. */
internal interface SessionStorage {
    fun hostId(): String
    fun load(): ChatGptSession?
    fun registration(): JSONObject?
    fun issued(clientId: String)
    fun save(session: ChatGptSession)
    fun clearSession()
}

class ChatGptSessionStore internal constructor(context: Context,
    recordName: String = "chatgpt_subscription_v1",
    private val alias: String = "dex-assistant-chatgpt-subscription-v1",
) : SessionStorage {
    init { require(recordName.matches(Regex("[a-z0-9_]{1,64}"))) }
    private val file = AtomicFile(File(context.noBackupFilesDir, "$recordName.enc"))
    private fun read(): JSONObject {
        if (!file.baseFile.exists()) return JSONObject()
        try {
            val encrypted = file.readFully()
            require(encrypted.size in 29..262144)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, encrypted.copyOfRange(0, 12)))
            return JSONObject(String(cipher.doFinal(encrypted.copyOfRange(12, encrypted.size)), Charsets.UTF_8))
        } catch (_: Exception) { throw ChatGptFailure("SESSION_STORAGE_ERROR") }
    }
    private fun write(j: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.iv + cipher.doFinal(j.toString().toByteArray(Charsets.UTF_8))
        val output = file.startWrite()
        try { output.write(encrypted); file.finishWrite(output) }
        catch (_: Exception) { file.failWrite(output); throw ChatGptFailure("SESSION_STORAGE_ERROR") }
    }
    @Synchronized override fun hostId(): String {
        val j = read()
        if (!j.has("host_id")) write(j.put("host_id", "urn:uuid:${UUID.randomUUID()}"))
        return j.getString("host_id")
    }
    @Synchronized override fun load(): ChatGptSession? = read().optJSONObject("session")?.let(ChatGptSession::from)
    @Synchronized override fun registration(): JSONObject? = read().optJSONObject("registration")
    @Synchronized override fun issued(clientId: String) {
        val j = read(); val r = j.optJSONObject("registration") ?: JSONObject()
        write(j.put("registration", r.put("client_id", clientId)))
    }
    @Synchronized override fun save(session: ChatGptSession) {
        write(read().put("session", session.json()).put("registration", JSONObject()
            .put("client_id", session.clientId).put("sub", session.subject).put("email", session.email)))
    }
    @Synchronized override fun clearSession() { write(read().apply { remove("session") }) }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
            generateKey()
        }
    }
}
