package com.khuongnd.dexassistant.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.khuongnd.dexassistant.ui.ai.*
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Offline only. Uses only synthetic data and a reserved fixture Keystore record. No owner credentials/settings/grants/actions/voice/history are touched. */
@RunWith(AndroidJUnit4::class)
class ChatGptSubscriptionOfflineDeviceTest {
    @Test fun androidJsonAndSseRequireCompletedResponse() {
        val body = """{"type":"response.completed","response":{"id":"r","status":"completed","output":[{"type":"message","role":"assistant","status":"completed","content":[{"type":"output_text","text":"Xin chào"}]}]}}"""
        val response = ResponsesStream.read(Buffer().writeUtf8("data: $body\n\n"))
        assertEquals("Xin chào", ResponsesStream.chatText(response))
        try { ResponsesStream.read(Buffer().writeUtf8("data: {\"type\":\"response.created\",\"response\":{\"id\":\"r\"}}\n\n")); fail() }
        catch (e: com.khuongnd.dexassistant.ui.auth.ChatGptFailure) { assertEquals("INCOMPLETE_RESPONSE", e.code) }
    }
    @Test fun isolatedKeystoreRecordRestoresAndLogoutPreservesHostAndClient() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val record = "chatgpt_t025_offline_fixture"
        val alias = "dex-assistant-t025-offline-fixture-only"
        val file = android.util.AtomicFile(java.io.File(context.noBackupFilesDir, "$record.enc"))
        val keystore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        // This reserved fixture record/alias never contains owner credentials.
        try {
            file.delete(); if (keystore.containsAlias(alias)) keystore.deleteEntry(alias)
            val store = com.khuongnd.dexassistant.ui.auth.ChatGptSessionStore(context, record, alias)
            val host = store.hostId()
            val session = com.khuongnd.dexassistant.ui.auth.ChatGptSession("oaiapp_synthetic", "synthetic-sub", "", "fake-id-token",
                "fake-access-token", "fake-refresh-token", "resource.invoke chatgpt.tokens.use.direct", Long.MAX_VALUE)
            store.save(session)
            val restored = com.khuongnd.dexassistant.ui.auth.ChatGptSessionStore(context, record, alias)
            assertEquals(host, restored.hostId())
            assertEquals("fake-access-token", restored.load()!!.accessToken)
            assertFalse(String(file.readFully(), Charsets.ISO_8859_1).contains("fake-access-token"))
            assertNull(keystore.getKey(alias, null).encoded)
            restored.clearSession()
            assertNull(store.load()); assertEquals(host, store.hostId())
            assertEquals("oaiapp_synthetic", store.registration()!!.getString("client_id"))
        } finally { file.delete(); if (keystore.containsAlias(alias)) keystore.deleteEntry(alias) }
    }
    @Test fun subscriptionAutomaticDefaultAndUnprovenModelsRemainGated() {
        assertFalse(com.khuongnd.dexassistant.ui.security.SubscriptionProof.matches(null, "a".repeat(64), "model"))
        assertFalse(SubscriptionAcceptance.DEFAULT_ACTIVATION_ACCEPTED)
        val models = ChatGptModelRepository.parse(JSONObject("""{"models":[{"visibility":"list","slug":"account-model","display_name":"Account model"}]}"""))
        assertEquals("account-model", models.single().slug)
    }
}
