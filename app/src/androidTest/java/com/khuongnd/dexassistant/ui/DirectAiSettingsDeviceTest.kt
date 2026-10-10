package com.khuongnd.dexassistant.ui

import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.state.ToggleableState
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.security.AiCredentialStore
import com.khuongnd.dexassistant.ui.security.AiProvider
import com.khuongnd.dexassistant.ui.security.AiProviderSettings
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Actual Settings renderer/semantics without coroutine TestScope: supports --no-restart. */
@OptIn(ExperimentalComposeUiApi::class)
class DirectAiSettingsDeviceTest {
    @Test fun configureGeminiThroughSettingsAndVerifyPersistence() {
        assumeTrue("Explicit configuration opt-in required", InstrumentationRegistry.getArguments()
            .getString("runDirectAiConfigure") == "true")
        val pacing = ProviderTestPacing.fromArguments()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val context = instrumentation.targetContext
        val input = File(context.filesDir, "t018-provider-input.json")
        fun roots(view: View): List<ViewRootForTest> = when (view) {
            is ViewRootForTest -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { roots(view.getChildAt(it)) }
            else -> emptyList()
        }
        fun nodes(): List<SemanticsNode> = WindowInspector.getGlobalWindowViews().flatMap(::roots)
            .flatMap { it.semanticsOwner.getAllSemanticsNodes(false) }
        fun waitNode(description: String, timeout: Long = 10_000, predicate: (SemanticsNode) -> Boolean): SemanticsNode {
            val deadline = android.os.SystemClock.elapsedRealtime() + timeout
            var found: SemanticsNode? = null
            while (found == null && android.os.SystemClock.elapsedRealtime() < deadline) {
                instrumentation.runOnMainSync { found = nodes().firstOrNull(predicate) }
                if (found == null) Thread.sleep(50)
            }
            return requireNotNull(found) { "Settings control unavailable: $description" }
        }
        fun tag(name: String) = waitNode(name) { it.config.getOrNull(SemanticsProperties.TestTag) == name }
        fun click(node: SemanticsNode) = instrumentation.runOnMainSync {
            assertTrue("Settings action must run", node.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke() == true)
        }
        fun taggedClick(name: String) { click(waitNode(name) {
            it.config.getOrNull(SemanticsProperties.TestTag) == name &&
                it.config.contains(SemanticsActions.OnClick) && !it.config.contains(SemanticsProperties.Disabled)
        }) }
        fun textValue(node: SemanticsNode): String = node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()
        fun clickableText(value: String, description: String, timeout: Long = 10_000) {
            val label = waitNode(description, timeout) { textValue(it) == value }
            var target: SemanticsNode? = label
            instrumentation.runOnMainSync {
                while (target != null && !target!!.config.contains(SemanticsActions.OnClick)) target = target!!.parent
            }
            click(requireNotNull(target) { "Settings label has no clickable parent: $description" })
        }
        fun waitState(description: String, timeout: Long = 10_000, predicate: () -> Boolean) {
            val deadline = android.os.SystemClock.elapsedRealtime() + timeout
            while (!predicate() && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(50)
            assertTrue(description, predicate())
        }
        fun openSettings() { taggedClick("advancedMenu"); taggedClick("settings") }
        try {
            require(input.isFile) { "Private provider input missing" }
            val payload = try { JSONObject(input.readText()) } catch (_: Exception) { throw AssertionError("Private provider input invalid") }
            val apiKey = payload.optString("key")
            val model = payload.optString("model")
            require(apiKey.isNotBlank() && model.isNotBlank()) { "Private provider input incomplete" }
            val provider = AiProvider.GEMINI
            val secrets = AiCredentialStore(context)
            val settings = AiProviderSettings(context)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                openSettings()
                fun savePrivateKey() {
                    val keyPrefs = context.getSharedPreferences("ai_provider_keys_v1", 0)
                    val priorCiphertext = keyPrefs.getString(provider.name + "_encrypted", null)
                    val keyField = tag("aiProviderApiKey")
                    // Never attach inserted secret text or a framework action exception to a failure.
                    var entered = false
                    instrumentation.runOnMainSync {
                        entered = runCatching { keyField.config.getOrNull(SemanticsActions.SetText)?.action?.invoke(AnnotatedString(apiKey)) == true }.getOrDefault(false)
                    }
                    assertTrue("Private API key must enter through Settings", entered)
                    taggedClick("saveAiKey")
                    waitState("Encrypted key save completes") {
                        keyPrefs.getString(provider.name + "_encrypted", null) != priorCiphertext && secrets.load(provider) == apiKey
                    }
                }
                savePrivateKey()
                savePrivateKey() // Exercise update through the same UI control before deletion.
                taggedClick("deleteAiKey")
                waitState("UI key deletion disables provider") { !secrets.hasKey(provider) && !settings.enabled(provider) }
                savePrivateKey()
                pacing.delayBeforeRequest()
                taggedClick("loadAiModels")
                clickableText(model, "discovered model", 25_000)
                clickableText("Lưu model", "save model")
                waitNode("model save resets free-tier confirmation") {
                    it.config.getOrNull(SemanticsProperties.TestTag) == "aiProviderStatus" &&
                        textValue(it) == "Đã lưu model ID; cần xác nhận Free Tier"
                }
                waitState("Provider model saved with free-tier reset") { settings.model(provider) == model && !settings.freeTierConfirmed(provider) }
                click(waitNode("free-tier checkbox recomposed OFF") {
                    it.config.getOrNull(SemanticsProperties.TestTag) == "confirmFreeTier" &&
                        it.config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.Off &&
                        !it.config.contains(SemanticsProperties.Disabled)
                })
                waitState("Free-tier confirmation saved") { settings.freeTierConfirmed(provider) }
                waitNode("free-tier checkbox recomposed ON") {
                    it.config.getOrNull(SemanticsProperties.TestTag) == "confirmFreeTier" &&
                        it.config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.On
                }
                if (!settings.enabled(provider)) {
                    click(waitNode("provider switch recomposed OFF") {
                        it.config.getOrNull(SemanticsProperties.TestTag) == "aiProviderEnabled" &&
                            it.config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.Off &&
                            !it.config.contains(SemanticsProperties.Disabled)
                    })
                    waitState("Provider enable saved") { settings.enabled(provider) }
                }
                waitNode("provider switch recomposed ON") {
                    it.config.getOrNull(SemanticsProperties.TestTag) == "aiProviderEnabled" &&
                        it.config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.On
                }
                if (settings.defaultProvider() != provider) taggedClick("setDefaultAiProvider")
                pacing.delayBeforeRequest()
                taggedClick("testAiKey")
                waitNode("authenticated key status", 25_000) {
                    it.config.getOrNull(SemanticsProperties.TestTag) == "aiProviderStatus" && textValue(it) == "Key hợp lệ; chưa xác nhận model Free Tier"
                }
                scenario.recreate()
                openSettings()
                waitNode("configured key persists") { textValue(it) == "API key: Đã cấu hình" }
                assertTrue("Encrypted key readback matches private input after recreation", secrets.load(provider) == apiKey)
                assertEquals("Provider model persists", model, settings.model(provider))
                assertTrue("Provider enable persists", settings.enabled(provider))
                assertTrue("Owner free-tier confirmation persists", settings.freeTierConfirmed(provider))
                assertEquals(provider, settings.defaultProvider())
                android.util.Log.i("DexDirectAcceptance", "settings=PASS encryptedKey=true persistence=true discovery=true probe=AUTHENTICATED")
            }
        } finally { assertTrue("Private provider input must be deleted", !input.exists() || input.delete()) }
    }
}
