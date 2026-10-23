package com.khuongnd.dexassistant.ui

import android.content.Context
import android.media.AudioManager
import android.net.wifi.WifiManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.history.CommandHistoryStore
import com.khuongnd.dexassistant.ui.history.HistoryRetention
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Run on the retained API 35 test AVD with backend disconnected. */
class LocalFirstDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun localVolumeExecutesWithoutBackend() {
        val audio = context.getSystemService(AudioManager::class.java)
        val original = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        try {
            command("đặt âm lượng 40 phần trăm")
            compose.waitUntil(10_000) {
                CommandHistoryStore(context).read().firstOrNull()?.outcome == "VERIFIED"
            }
            assertEquals(kotlin.math.round(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * .4).toInt(),
                audio.getStreamVolume(AudioManager.STREAM_MUSIC))
            assertEquals("LOCAL", CommandHistoryStore(context).read().first().route)
        } finally { audio.setStreamVolume(AudioManager.STREAM_MUSIC, original, 0) }
    }

    @org.junit.Ignore("T-018: retired backend-required routing assumptions; direct AI acceptance is separate")
    @Test fun reasoningWithoutConnectionHasStableFailure() = Unit

    @Test fun deviceOwnerWifiExecutesLocallyAndRestoresState() {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        val original = wifi.isWifiEnabled
        try {
            command(if (original) "tắt wifi" else "bật wifi")
            compose.waitUntil(15_000) {
                val record = CommandHistoryStore(context).read().firstOrNull()
                record?.intent == "WIFI_SET" && record.outcome == "VERIFIED"
            }
            assertEquals(!original, wifi.isWifiEnabled)
        } finally {
            command(if (original) "bật wifi" else "tắt wifi")
            compose.waitUntil(15_000) { wifi.isWifiEnabled == original }
        }
    }

    @Test fun youtubePhraseDispatchesLocally() {
        command("nhạc lái xe")
        compose.waitUntil(10_000) {
            val record = CommandHistoryStore(context).read().firstOrNull()
            record?.intent == "YOUTUBE_SEARCH" && record.outcome == "DISPATCHED"
        }
        assertEquals("LOCAL", CommandHistoryStore(context).read().first().route)
    }

    @Test fun keystoreHistorySurvivesNewStoreAndExcludesPlaintext() {
        val store = CommandHistoryStore(context)
        val marker = "lệnh kiểm tra mã hóa AVD"
        val now = System.currentTimeMillis()
        store.record(marker, "LOCAL", "TEST", "VERIFIED", now)
        assertTrue(CommandHistoryStore(context).read().any { it.originalText == marker })
        val persisted = context.getSharedPreferences("command_history_v1", Context.MODE_PRIVATE)
            .getString("records", "")!!
        assertFalse(persisted.contains(marker))
        store.record("password=excluded", "UNSUPPORTED", null, "REJECTED", now + 1)
        assertEquals("[REDACTED]", store.read(now + 1).first().originalText)
        assertFalse(context.getSharedPreferences("command_history_v1", Context.MODE_PRIVATE)
            .getString("records", "")!!.contains("excluded"))
        store.clear()
        assertTrue(CommandHistoryStore(context).read().isEmpty())
    }

    @Test fun deviceHistoryPrunesEarlierAgeAndCount() {
        val store = CommandHistoryStore(context)
        val now = System.currentTimeMillis()
        store.clear()
        store.record("expired", "LOCAL", null, "VERIFIED", now - HistoryRetention.DEFAULT_MAX_AGE_MS - 1)
        assertTrue(store.read(now).isEmpty())
        repeat(101) { store.record("command $it", "LOCAL", null, "VERIFIED", now + it) }
        assertEquals(100, CommandHistoryStore(context).read(now + 101).size)
        assertFalse(store.read(now + 101).any { it.originalText == "command 0" })
        store.clear()
    }

    @Test fun privateCommandIsRejectedLocallyAndHistoryIsRedacted() {
        val store = CommandHistoryStore(context)
        store.clear()
        command("truy cập Wi-Fi abc, mật khẩu nhiều từ bí mật")
        compose.waitUntil(10_000) { store.read().firstOrNull()?.outcome == "LOCAL_ONLY_UNSUPPORTED" }
        assertEquals("UNSUPPORTED", store.read().first().route)
        assertEquals("[REDACTED]", store.read().first().originalText)
        store.clear()
    }

    @Test fun historicalSensitiveEntryIsRedactedOnReadAndPersist() {
        val store = CommandHistoryStore(context)
        store.clear()
        store.record("ordinary", "LOCAL", null, "VERIFIED")
        val prefs = context.getSharedPreferences("command_history_v1", Context.MODE_PRIVATE)
        val array = org.json.JSONArray(prefs.getString("records", "[]"))
        val entry = array.getJSONObject(0)
        val keys = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keys.getKey("dex-assistant-history-v1", null))
        val legacy = org.json.JSONObject().put("text", "mật khẩu một chuỗi nhiều từ bí mật")
            .put("route", "BACKEND_REQUIRED").put("intent", org.json.JSONObject.NULL).put("outcome", "password several secret words")
        entry.put("iv", android.util.Base64.encodeToString(cipher.iv, android.util.Base64.NO_WRAP))
        entry.put("ciphertext", android.util.Base64.encodeToString(cipher.doFinal(legacy.toString().toByteArray()), android.util.Base64.NO_WRAP))
        prefs.edit().putString("records", array.toString()).commit()
        val migrated = store.read().first()
        assertEquals("[REDACTED]", migrated.originalText)
        assertEquals("[REDACTED]", migrated.outcome)
        assertEquals("[REDACTED]", CommandHistoryStore(context).read().first().originalText)
        store.clear()
    }

    private fun command(text: String) {
        compose.onNodeWithTag("commandInput").performTextInput(text)
        compose.onNodeWithTag("runCommand").performClick()
    }
}
