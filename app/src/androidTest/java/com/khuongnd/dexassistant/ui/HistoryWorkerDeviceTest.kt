package com.khuongnd.dexassistant.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.history.CommandHistoryStore
import com.khuongnd.dexassistant.ui.screen.DexAccessibilityService
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import java.security.KeyStore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Dedicated test preferences/key: never clears or rotates the owner's history key. */
class HistoryWorkerDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val prefsName = "history_fifo_device_test"
    private val alias = "dex-history-fifo-device-test"
    private fun store() = CommandHistoryStore(context, prefsName, alias)
    private fun keys() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    @Before fun preserveOwnerAccessibility() {
        assertNotNull(instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES))
    }

    @Test fun twoInstancesOrderRecordClearKeyDeletionAndLaterRecord() {
        val first = store(); val second = store(); first.clear()
        try {
            val events = mutableListOf<String>()
            first.recordAsync("first", "LOCAL", null, "VERIFIED") { it.getOrThrow(); events.add("first") }
            second.clearAsync {
                assertTrue(it.getOrThrow().isEmpty())
                assertFalse(keys().containsAlias(alias))
                assertFalse(context.getSharedPreferences(prefsName, 0).contains("records"))
                events.add("clear")
            }
            first.recordAsync("second", "LOCAL", null, "VERIFIED") { it.getOrThrow(); events.add("second") }
            second.flush()
            assertEquals(listOf("first", "clear", "second"), events)
            assertEquals(listOf("second"), second.read().map { it.originalText })
            assertTrue(keys().containsAlias(alias))
        } finally { first.clear() }
    }

    @Test fun queuedPrivacyAndLegacyReadScrubCiphertext() {
        val store = store(); store.clear()
        try {
            store.recordAsync("mật khẩu nhiều từ bí mật", "LOCAL", null, "password several secret words") { it.getOrThrow() }
            assertEquals("[REDACTED]", store.read().first().originalText)
            val prefs = context.getSharedPreferences(prefsName, 0)
            val array = org.json.JSONArray(prefs.getString("records", "[]"))
            val stored = array.getJSONObject(0)
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keys().getKey(alias, null))
            val legacy = org.json.JSONObject().put("text", "mật khẩu nhiều từ bí mật").put("route", "LOCAL")
                .put("intent", org.json.JSONObject.NULL).put("outcome", "password several secret words")
            stored.put("iv", android.util.Base64.encodeToString(cipher.iv, android.util.Base64.NO_WRAP))
            stored.put("ciphertext", android.util.Base64.encodeToString(cipher.doFinal(legacy.toString().toByteArray()), android.util.Base64.NO_WRAP))
            assertTrue(prefs.edit().putString("records", array.toString()).commit())
            val migrated = store.read().first()
            assertEquals("[REDACTED]", migrated.originalText); assertEquals("[REDACTED]", migrated.outcome)
            val persisted = org.json.JSONArray(prefs.getString("records", "[]")).getJSONObject(0)
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keys().getKey(alias, null), javax.crypto.spec.GCMParameterSpec(128, android.util.Base64.decode(persisted.getString("iv"), android.util.Base64.NO_WRAP)))
            val clear = org.json.JSONObject(String(cipher.doFinal(android.util.Base64.decode(persisted.getString("ciphertext"), android.util.Base64.NO_WRAP))))
            assertEquals("[REDACTED]", clear.getString("text")); assertEquals("[REDACTED]", clear.getString("outcome"))
        } finally { store.clear() }
    }

    @Test fun corruptStorageFailsTruthfullyWithoutOverwriteAndClearRecovers() {
        val store = store(); store.clear()
        try {
            val prefs = context.getSharedPreferences(prefsName, 0)
            assertTrue(prefs.edit().putString("records", "not-json").commit())
            var failed = false
            store.recordAsync("ordinary", "LOCAL", null, "VERIFIED") { failed = it.isFailure }
            store.flush()
            assertTrue(failed); assertEquals("not-json", prefs.getString("records", null))
            assertTrue(runCatching { store.read() }.isFailure)
            assertEquals("not-json", prefs.getString("records", null))
            var readFailed = false
            store.readAsync { readFailed = it.isFailure }
            store.flush(); assertTrue(readFailed)
            assertEquals("not-json", prefs.getString("records", null))
            store.clear()
            store.record("recovered", "LOCAL", null, "VERIFIED")
            assertEquals("recovered", store.read().first().originalText)
        } finally { store.clear() }
    }

    @Test fun lateHistoryCannotReplaceNewerStatusOrResurrectAfterClear() {
        val store = store(); store.clear()
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        var observed = UiState()
        var core: AssistantCoordinator? = null
        try {
            store.record("old", "LOCAL", null, "VERIFIED")
            store.readAsync { it.getOrThrow(); entered.countDown(); check(release.await(10, TimeUnit.SECONDS)) }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                core = AssistantCoordinator(context, store)
                core!!.addObserver { observed = it }
                core!!.javaClass.getDeclaredMethod("refreshHistory").apply { isAccessible = true }.invoke(core)
                core!!.javaClass.getDeclaredMethod("update", String::class.java, String::class.java).apply { isAccessible = true }.invoke(core, "NEWER_STATUS", "newer result")
            }
            release.countDown(); store.flush(); instrumentation.waitForIdleSync()
            assertEquals("NEWER_STATUS", observed.status); assertEquals("newer result", observed.message)
            assertEquals(listOf("old"), observed.history.map { it.originalText })
            instrumentation.runOnMainSync {
                core!!.javaClass.getDeclaredMethod("refreshHistory").apply { isAccessible = true }.invoke(core)
                core!!.clearHistory()
                assertTrue(observed.historyClearing)
                assertTrue(observed.history.isEmpty())
            }
            store.flush(); instrumentation.waitForIdleSync()
            assertTrue(observed.history.isEmpty()); assertEquals("NEWER_STATUS", observed.status)
            assertFalse(observed.historyClearing)
            assertFalse(keys().containsAlias(alias))
        } finally { release.countDown(); store.flush(); store.clear() }
    }

    @Test fun terminalCompletesWhileHistoryIsBlockedAndStorageFailureStaysSeparate() {
        val store = store(); store.clear()
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        var observed = UiState()
        val terminal = CountDownLatch(1)
        var result: VoiceCommandResult? = null
        try {
            val prefs = context.getSharedPreferences(prefsName, 0)
            assertTrue(prefs.edit().putString("records", "not-json").commit())
            store.readAsync { assertTrue(it.isFailure); entered.countDown(); check(release.await(10, TimeUnit.SECONDS)) }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                val core = AssistantCoordinator(context, store)
                core.addObserver { observed = it }
                core.dispatchVoice("bấm ô 999") { result = it; terminal.countDown() }
            }
            assertTrue("Terminal must finish before blocked history is released", terminal.await(3, TimeUnit.SECONDS))
            val expectedStatus = if (DexAccessibilityService.active == null) "ACCESSIBILITY_PERMISSION_REQUIRED" else "INVALID_SCREEN_COMMAND"
            assertEquals(expectedStatus, result?.status)
            release.countDown(); store.flush(); instrumentation.waitForIdleSync()
            assertEquals(expectedStatus, observed.status)
            assertEquals("Không thể cập nhật lịch sử lệnh", observed.historyError)
            assertEquals("not-json", prefs.getString("records", null))
        } finally { release.countDown(); store.flush(); store.clear() }
    }
}
