// Adapted from ChatGPT-Subscription-Android-POC (MIT); see third-party/siwc-poc-LICENSE.
package com.khuongnd.dexassistant.ui.auth

import android.content.Context
import android.net.Uri
import java.net.InetAddress
import java.net.ServerSocket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.json.JSONObject

/** One manager/refresh lock per application process. No remote service owns these credentials. */
internal interface SubscriptionSessionSource {
    fun freshSession(rejectedAccessToken: String? = null): ChatGptSession
    fun failure(e: Exception)
}

class ChatGptAuthManager internal constructor(private val store: SessionStorage,
    private val exchange: (Map<String, String>) -> JSONObject = { ChatGptHttp.postForm(TOKEN_URL, it) },
    private val revoke: (ChatGptSession) -> Unit = { current ->
        val endpoint = ChatGptHttp.getJson("https://auth.openai.com/.well-known/openid-configuration")
            .getString("revocation_endpoint")
        ChatGptHttp.postForm(endpoint, mapOf("token" to current.refreshToken,
            "token_type_hint" to "refresh_token", "client_id" to current.clientId), emptyAllowed = true)
    },
) : SubscriptionSessionSource {
    enum class State { NOT_CONNECTED, AUTHENTICATING, CONNECTED, TOKEN_REFRESHING,
        SESSION_EXPIRED, ACCESS_DENIED, QUOTA_LIMITED, ERROR }
    @Volatile var state = State.NOT_CONNECTED
        private set
    private val lock = Any()
    @Volatile private var generation = 0L
    fun sessionGeneration(): Long = generation
    /** Opaque non-secret binding for model acceptance; never a bearer credential or UI label. */
    fun connectionKey(): String? = store.load()?.let { session ->
        // The store's short atomic-read lock must not wait on a network refresh lock on the UI thread.
        MessageDigest.getInstance("SHA-256").digest(
            (session.clientId + "\n" + session.subject).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    @Volatile private var listener: ServerSocket? = null
    fun accountEmail(): String? = try { store.registration()?.optString("email") } catch (_: Exception) { null }
    fun connected(): Boolean = try { store.load()?.hasSubscriptionScope() == true } catch (_: Exception) { false }

    fun login(openBrowser: (Uri) -> Unit) {
        val epoch = synchronized(lock) {
            check(listener == null && state != State.AUTHENTICATING)
            state = State.AUTHENTICATING; ++generation
        }
        try {
            val old = store.load()
            val registration = store.registration()
            val previousClient = registration?.optString("client_id")?.takeIf { it.isNotBlank() }
            val stateValue = random(24); val nonce = random(24); val verifier = random(48)
            val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
            ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
                synchronized(lock) { require(generation == epoch); listener = server }
                server.soTimeout = 180_000
                val redirect = "http://127.0.0.1:${server.localPort}/auth/callback"
                val url = Uri.parse(AUTH_URL).buildUpon()
                    .appendQueryParameter("client_id", previousClient ?: "dynamic_agent_client")
                    .appendQueryParameter("redirect_uri", redirect).appendQueryParameter("response_type", "code")
                    .appendQueryParameter("scope", SCOPE).appendQueryParameter("resource", RESOURCE)
                    .appendQueryParameter("state", stateValue).appendQueryParameter("nonce", nonce)
                    .appendQueryParameter("code_challenge", challenge).appendQueryParameter("code_challenge_method", "S256")
                    .appendQueryParameter("ext_agent_host_id", store.hostId())
                if (previousClient == null) url.appendQueryParameter("agent_name_hint", "DeX Assistant")
                else if (old != null) url.appendQueryParameter("id_token_hint", old.idToken)
                openBrowser(url.build())
                val callback = awaitCallback(server, stateValue)
                val issued = AuthChecks.callback(callback.getQueryParameters("state"), stateValue,
                    callback.getQueryParameters("client_id"), previousClient,
                    callback.getQueryParameters("code"), callback.getQueryParameter("error"))
                synchronized(lock) { require(epoch == generation); store.issued(issued) }
                val tokens = exchange(mapOf("grant_type" to "authorization_code",
                    "client_id" to issued, "code" to callback.getQueryParameter("code")!!,
                    "code_verifier" to verifier, "redirect_uri" to redirect, "resource" to RESOURCE))
                val identity = ChatGptOidcVerifier.verify(tokens.getString("id_token"), issued, nonce)
                val previousSubject = registration?.optString("sub").orEmpty()
                require(previousSubject.isBlank() || previousSubject == identity.subject)
                val session = AuthChecks.session(tokens, issued, identity.subject, identity.email,
                    tokens.getString("id_token"), old)
                synchronized(lock) { require(epoch == generation); store.save(session); state = State.CONNECTED }
            }
        } catch (e: Exception) {
            synchronized(lock) { if (epoch == generation) failure(e) }
            throw if (e is ChatGptFailure) e else ChatGptFailure("AUTHENTICATION_FAILED")
        } finally { synchronized(lock) { if (epoch == generation) listener = null } }
    }

    /** A 401 from an older token must not rotate a token already refreshed by another caller. */
    override fun freshSession(rejectedAccessToken: String?): ChatGptSession = synchronized(lock) {
        if (state == State.AUTHENTICATING) throw ChatGptFailure("AUTHENTICATION_IN_PROGRESS")
        val current = store.load() ?: throw ChatGptFailure("NOT_CONNECTED")
        if (!current.hasSubscriptionScope()) throw ChatGptFailure("ACCESS_DENIED")
        if (current.expiresAt > System.currentTimeMillis() + 120_000 &&
            (rejectedAccessToken == null || rejectedAccessToken != current.accessToken)) {
            state = State.CONNECTED
            return@synchronized current
        }
        state = State.TOKEN_REFRESHING
        try {
            if (current.refreshToken.isBlank()) throw ChatGptFailure("SESSION_EXPIRED")
            val j = exchange(mapOf("grant_type" to "refresh_token",
                "client_id" to current.clientId, "refresh_token" to current.refreshToken, "resource" to RESOURCE))
            val fresh = AuthChecks.session(j, current.clientId, current.subject, current.email, current.idToken, current)
            store.save(fresh); state = State.CONNECTED; fresh
        } catch (e: Exception) {
            failure(e)
            throw if (e is ChatGptFailure) e else ChatGptFailure("SESSION_EXPIRED")
        }
    }

    fun cancelLogin() = synchronized(lock) {
        generation++; listener?.close(); listener = null
        state = if (connected()) State.CONNECTED else State.NOT_CONNECTED
    }

    /** Caller first cancels consumers. False means local logout succeeded, remote revocation unconfirmed. */
    fun logout(): Boolean = synchronized(lock) {
        cancelLogin()
        var revoked = false
        try {
            val current = store.load()
            if (current == null || current.refreshToken.isBlank()) revoked = true
            else {
                try { revoke(current) } catch (e: ChatGptFailure) {
                    if (e.code !in setOf("NETWORK_OR_INVALID_RESPONSE", "PROVIDER_UNAVAILABLE")) throw e
                    Thread.sleep(200) // One bounded backoff before clearing the renewable credential.
                    revoke(current)
                }
                revoked = true
            }
        } catch (_: Exception) { /* Local logout still clears credentials; UI reports unconfirmed revocation. */ }
        finally { store.clearSession(); state = State.NOT_CONNECTED }
        revoked
    }

    override fun failure(e: Exception) {
        state = when ((e as? ChatGptFailure)?.code) {
            "NOT_CONNECTED" -> State.NOT_CONNECTED
            "SESSION_EXPIRED" -> State.SESSION_EXPIRED
            "ACCESS_DENIED" -> State.ACCESS_DENIED
            "QUOTA_LIMITED" -> State.QUOTA_LIMITED
            else -> State.ERROR
        }
    }

    private fun awaitCallback(server: ServerSocket, expectedState: String): Uri {
        val deadline = System.nanoTime() + 180_000_000_000L
        while (System.nanoTime() < deadline) {
            server.soTimeout = ((deadline - System.nanoTime()) / 1_000_000).coerceIn(1, 180000).toInt()
            server.accept().use { socket ->
                socket.soTimeout = 3000
                // Bound the request line before allocation. Never log the line or query string.
                val line = StringBuilder(); val input = socket.getInputStream()
                while (true) {
                    val c = input.read(); if (c == -1 || c == 10) break
                    require(line.length < 16384); if (c != 13) line.append(c.toChar())
                }
                val parts = line.toString().split(' ')
                val uri = if (parts.size == 3 && parts[0] == "GET" && parts[1].startsWith("/auth/callback?"))
                    Uri.parse("http://127.0.0.1" + parts[1]) else null
                val valid = uri?.getQueryParameters("state") == listOf(expectedState)
                val body = if (valid) "Return to DeX Assistant to check sign-in." else "Invalid sign-in callback."
                val bytes = body.toByteArray(Charsets.UTF_8)
                val headers = "HTTP/1.1 ${if (valid) "200 OK" else "400 Bad Request"}\r\n" +
                    "Content-Type: text/plain; charset=utf-8\r\nContent-Length: ${bytes.size}\r\n" +
                    "Cache-Control: no-store\r\nConnection: close\r\n\r\n"
                socket.getOutputStream().apply { write(headers.toByteArray(Charsets.US_ASCII)); write(bytes); flush() }
                if (valid) return uri!!
            }
        }
        throw ChatGptFailure("AUTHENTICATION_TIMEOUT")
    }
    private fun random(size: Int) = Base64.getUrlEncoder().withoutPadding().encodeToString(
        ByteArray(size).also { SecureRandom().nextBytes(it) })
    companion object {
        const val RESOURCE = "https://api.openai.com/v1"
        const val SCOPE = "openid profile email offline_access resource.invoke chatgpt.tokens.use.direct"
        const val AUTH_URL = "https://auth.openai.com/api/accounts/authorize"
        const val TOKEN_URL = "https://auth.openai.com/api/accounts/oauth/token"
        @Volatile private var instance: ChatGptAuthManager? = null
        fun get(context: Context): ChatGptAuthManager = instance ?: synchronized(this) {
            instance ?: ChatGptAuthManager(ChatGptSessionStore(context.applicationContext)).also { instance = it }
        }
    }
}

/** Pure security checks, also exercised with synthetic callbacks/token responses. */
internal object AuthChecks {
    fun callback(states: List<String>, expected: String, clients: List<String>, old: String?,
        codes: List<String>, error: String?): String {
        require(states == listOf(expected) && expected.isNotBlank())
        if (error != null) throw ChatGptFailure("ACCESS_DENIED")
        require(codes.size == 1 && codes.single().isNotBlank() && clients.size <= 1)
        val issued = clients.singleOrNull() ?: old
        require(issued != null && issued.matches(Regex("oaiapp_[A-Za-z0-9_-]{1,200}")))
        require(old == null || issued == old)
        return issued
    }
    fun session(j: JSONObject, client: String, sub: String, email: String, idToken: String,
        old: ChatGptSession?, now: Long = System.currentTimeMillis()): ChatGptSession {
        val lifetime = j.getLong("expires_in")
        require(lifetime in 1..31_536_000 && j.getString("access_token").isNotBlank())
        require(j.optString("token_type", "Bearer").equals("Bearer", ignoreCase = true))
        val s = ChatGptSession(client, sub, email, idToken, j.getString("access_token"),
            j.optString("refresh_token").ifBlank { old?.refreshToken.orEmpty() },
            j.optString("scope").ifBlank { old?.scopes.orEmpty() }, now + lifetime * 1000)
        if (!s.hasSubscriptionScope()) throw ChatGptFailure("ACCESS_DENIED")
        return s
    }
}
