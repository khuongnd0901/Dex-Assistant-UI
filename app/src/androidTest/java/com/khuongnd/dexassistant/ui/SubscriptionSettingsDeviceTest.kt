package com.khuongnd.dexassistant.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.security.ChatGptSettings
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** Offline preferences are isolated; rendering alone cannot launch login or touch saved credentials. */
class SubscriptionSettingsDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun settingsStartsAtSubscriptionAndOffersNoApiKeyControls() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val keys = context.getSharedPreferences("ai_provider_keys_v1", 0).all.toMap()
        val preferences = context.getSharedPreferences("ai_provider_settings_v1", 0).all.toMap()
        compose.setContent { MaterialTheme { ChatGptSubscriptionSettings() } }
        compose.onNodeWithTag("subscriptionStatus").assertExists()
        compose.onNodeWithTag("subscriptionSignIn").assertExists()
        compose.onNodeWithTag("aiProviderPicker").assertDoesNotExist()
        compose.onNodeWithTag("aiProviderApiKey").assertDoesNotExist()
        compose.onNodeWithTag("testAiKey").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(keys, context.getSharedPreferences("ai_provider_keys_v1", 0).all)
            assertEquals(preferences, context.getSharedPreferences("ai_provider_settings_v1", 0).all)
        }
    }
    @Test fun legacyPreferencesStayUntouchedAndModelChangeInvalidatesOnlySubscriptionProofs() {
        val owner = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "subscription-settings-${UUID.randomUUID()}-"
        val names = mutableSetOf<String>()
        val fixture = object : ContextWrapper(owner) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                names.add(prefix + name); return owner.getSharedPreferences(prefix + name, mode)
            }
        }
        val prefs = fixture.getSharedPreferences("ai_provider_settings_v1", 0)
        val keys = fixture.getSharedPreferences("ai_provider_keys_v1", 0)
        try {
            assertTrue(prefs.edit().putString("default_provider", "GEMINI").putString("GEMINI_model", "legacy")
                .putBoolean("GEMINI_enabled", true).putBoolean("GEMINI_free_confirmed", true).commit())
            assertTrue(keys.edit().putString("GEMINI_encrypted", "synthetic-ciphertext").putString("GEMINI_iv", "synthetic-iv").commit())
            val oldKeys = keys.all.toMap()
            val settings = ChatGptSettings(fixture)
            assertFalse(settings.selected()); assertFalse(settings.enabled())
            val before = settings.routingSnapshot()
            assertTrue(prefs.edit().putString("GEMINI_model", "legacy-edited").commit())
            assertEquals(before, settings.routingSnapshot())
            settings.setModel("model-a"); settings.setEnabled(true)
            settings.recordSubscriptionVerification("a".repeat(64), "model-a", false)
            settings.recordSubscriptionVerification("a".repeat(64), "model-a", true)
            assertTrue(settings.subscriptionVerified("a".repeat(64), false))
            assertFalse(settings.subscriptionVerified("b".repeat(64), false))
            settings.setModel("model-b")
            assertFalse(settings.subscriptionVerified("a".repeat(64), false))
            assertFalse(settings.subscriptionVerified("a".repeat(64), true))
            assertFalse(settings.selected())
            assertEquals("GEMINI", prefs.getString("default_provider", null))
            assertEquals("legacy-edited", prefs.getString("GEMINI_model", null))
            assertTrue(prefs.getBoolean("GEMINI_free_confirmed", false))
            assertEquals(oldKeys, keys.all)
            try { settings.setModel("https://bad?model"); fail() } catch (_: IllegalArgumentException) { }
            assertEquals("model-b", settings.model())
        } finally { names.forEach(owner::deleteSharedPreferences) }
    }
}
