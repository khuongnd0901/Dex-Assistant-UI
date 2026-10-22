package com.khuongnd.dexassistant.ui.auth

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ChatGptAuthTest {
    private val scope = "openid resource.invoke chatgpt.tokens.use.direct"
    private fun session(expiry: Long = Long.MAX_VALUE) = ChatGptSession("oaiapp_test", "sub", "", "id", "access", "refresh", scope, expiry)
    private class Store(var value: ChatGptSession?) : SessionStorage {
        var client = "oaiapp_test"
        override fun hostId() = "urn:uuid:synthetic"
        override fun load() = value
        override fun registration() = JSONObject().put("client_id", client)
        override fun issued(clientId: String) { client = clientId }
        override fun save(session: ChatGptSession) { value = session }
        override fun clearSession() { value = null }
    }
    private fun token() = JSONObject().put("access_token", "new-access").put("refresh_token", "rotated")
        .put("expires_in", 3600).put("scope", scope)
    private fun rejects(block: () -> Unit) { try { block(); fail("must reject") } catch (_: IllegalArgumentException) {} }

    @Test fun callbackBindsStateCodeAndIssuedClient() {
        assertEquals("oaiapp_test", AuthChecks.callback(listOf("state"), "state", listOf("oaiapp_test"), null, listOf("code"), null))
        rejects { AuthChecks.callback(listOf("wrong"), "state", listOf("oaiapp_test"), null, listOf("code"), null) }
        rejects { AuthChecks.callback(listOf("state", "state"), "state", emptyList(), "oaiapp_test", listOf("code"), null) }
        rejects { AuthChecks.callback(listOf("state"), "state", listOf("oaiapp_other"), "oaiapp_test", listOf("code"), null) }
        rejects { AuthChecks.callback(listOf("state"), "state", listOf("dynamic_agent_client"), null, listOf("code"), null) }
        rejects { AuthChecks.callback(listOf("state"), "state", emptyList(), null, listOf("code"), null) }
        rejects { AuthChecks.callback(listOf("state"), "state", emptyList(), "oaiapp_test", listOf("a", "b"), null) }
    }
    @Test fun deniedCallbackCannotExchange() {
        try { AuthChecks.callback(listOf("s"), "s", emptyList(), "oaiapp_test", emptyList(), "access_denied"); fail() }
        catch (e: ChatGptFailure) { assertEquals("ACCESS_DENIED", e.code) }
    }
    @Test fun scopesLifetimeRotationAndMissingRefresh() {
        val refreshed = AuthChecks.session(token(), "oaiapp_test", "sub", "", "id", session(), 100)
        assertEquals("rotated", refreshed.refreshToken); assertEquals(3600100L, refreshed.expiresAt)
        val noReplacement = token().apply { remove("refresh_token"); remove("scope") }
        assertEquals("refresh", AuthChecks.session(noReplacement, "oaiapp_test", "sub", "", "id", session()).refreshToken)
        try { AuthChecks.session(token().put("scope", "openid"), "oaiapp_test", "sub", "", "id", null); fail() }
        catch (e: ChatGptFailure) { assertEquals("ACCESS_DENIED", e.code) }
        rejects { AuthChecks.session(token().put("expires_in", -1), "oaiapp_test", "sub", "", "id", null) }
    }
    @Test fun refreshIsSerializedAndStale401DoesNotRotateAgain() {
        val store = Store(session(0)); val count = AtomicInteger()
        val auth = ChatGptAuthManager(store, exchange = { count.incrementAndGet(); token() })
        val pool = Executors.newFixedThreadPool(8); val start = CountDownLatch(1)
        try {
            val calls = (0..15).map { pool.submit<String> { start.await(); auth.freshSession("access").accessToken } }
            start.countDown()
            calls.forEach { assertEquals("new-access", it.get(3, TimeUnit.SECONDS)) }
            assertEquals(1, count.get()); assertEquals("rotated", store.value!!.refreshToken)
            assertEquals("new-access", ChatGptAuthManager(store).freshSession().accessToken)
        } finally { pool.shutdownNow() }
    }
    @Test fun logoutClearsTokensPreservesRegistrationAndCannotRefresh() {
        val store = Store(session()); var revoked = false
        val auth = ChatGptAuthManager(store, revoke = { revoked = true })
        assertTrue(auth.logout()); assertTrue(revoked); assertNull(store.value)
        assertEquals("oaiapp_test", store.client)
        try { auth.freshSession(); fail() } catch (e: ChatGptFailure) { assertEquals("NOT_CONNECTED", e.code) }
    }
    @Test fun failedRevocationStillClearsLocalSession() {
        val store = Store(session())
        val auth = ChatGptAuthManager(store, revoke = { throw ChatGptFailure("PROVIDER_UNAVAILABLE") })
        assertFalse(auth.logout()); assertNull(store.value)
    }
    @Test fun logoutRetriesTransientRevocationBeforeClearing() {
        val store = Store(session()); var attempts = 0
        val auth = ChatGptAuthManager(store, revoke = {
            attempts++; if (attempts == 1) throw ChatGptFailure("PROVIDER_UNAVAILABLE")
            assertNotNull(store.value)
        })
        assertTrue(auth.logout()); assertEquals(2, attempts); assertNull(store.value)
    }
    @Test fun logoutCannotBeResurrectedByInFlightRefresh() {
        val store = Store(session(0)); val entered = CountDownLatch(1); val finish = CountDownLatch(1)
        val auth = ChatGptAuthManager(store, exchange = { entered.countDown(); finish.await(3, TimeUnit.SECONDS); token() }, revoke = {})
        val pool = Executors.newFixedThreadPool(2)
        try {
            val refresh = pool.submit { auth.freshSession() }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            val logout = pool.submit<Boolean> { auth.logout() }
            finish.countDown(); refresh.get(3, TimeUnit.SECONDS); assertTrue(logout.get(3, TimeUnit.SECONDS))
            assertNull(store.value)
        } finally { finish.countDown(); pool.shutdownNow() }
    }
    @Test fun networkFailureDoesNotOverwriteRotatingCredentials() {
        val store = Store(session(0)); val original = store.value
        val auth = ChatGptAuthManager(store, exchange = { throw ChatGptFailure("PROVIDER_UNAVAILABLE") })
        try { auth.freshSession(); fail() } catch (_: ChatGptFailure) { assertSame(original, store.value) }
    }
}
