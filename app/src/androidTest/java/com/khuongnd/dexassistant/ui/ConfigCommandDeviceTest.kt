package com.khuongnd.dexassistant.ui

import android.app.ActivityOptions
import android.app.UiAutomation
import android.content.ComponentName
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.actions.CommandIntents
import com.khuongnd.dexassistant.ui.actions.LocalActionExecutor
import com.khuongnd.dexassistant.ui.actions.LocalOutcome
import com.khuongnd.dexassistant.ui.actions.LocalResult
import com.khuongnd.dexassistant.ui.actions.YoutubeSearchIntents
import com.khuongnd.dexassistant.ui.device.DpcClient
import com.khuongnd.dexassistant.ui.execution.ActionRouter
import com.khuongnd.dexassistant.ui.protocol.DeviceAction
import com.khuongnd.dexassistant.ui.protocol.DeviceCommandProtocol
import com.khuongnd.dexassistant.ui.routing.ActionId
import com.khuongnd.dexassistant.ui.routing.ActionCatalog
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import com.khuongnd.dexassistant.ui.routing.CommandRegistryHolder
import com.khuongnd.dexassistant.ui.routing.LocalCommandEngine
import com.khuongnd.dexassistant.ui.screen.CommandRevalidationFixtureActivity
import com.khuongnd.dexassistant.ui.screen.DesktopUiAccess
import com.khuongnd.dexassistant.ui.screen.DexAccessibilityService
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Default tests compile assets and exercise rejected/cancelled actions: no dispatch, prefs, history or grants. */
@RunWith(AndroidJUnit4::class)
class ConfigCommandDeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    @Before fun preserveOwnerEnabledAccessibilityServices() {
        assertNotNull(instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES))
    }

    private fun boundOwnerService(): DexAccessibilityService {
        val context = instrumentation.targetContext
        val expected = ComponentName(context, DexAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').mapNotNull(ComponentName::unflattenFromString).contains(expected)
        assertTrue("Owner-enabled accessibility service required; test does not enable it", enabled)
        val timeoutArgument = InstrumentationRegistry.getArguments().getString("ownerAccessibilityBindTimeoutMs")
        val bindTimeoutMs = if (timeoutArgument == null) 10_000L else {
            val parsed = timeoutArgument.toLongOrNull()
            assertTrue("ownerAccessibilityBindTimeoutMs must be an integer within 10000..180000", parsed != null && parsed in 10_000L..180_000L)
            requireNotNull(parsed)
        }
        instrumentation.sendStatus(0, Bundle().apply {
            putString("accessibilityReadiness", "waiting_for_owner_accessibility_bind")
            putLong("ownerAccessibilityBindTimeoutMs", bindTimeoutMs)
        })
        await("Enabled DeX accessibility service did not bind within $bindTimeoutMs ms", bindTimeoutMs) {
            DexAccessibilityService.active != null
        }
        val service = requireNotNull(DexAccessibilityService.active)
        assertNotNull("Bound service instance lost its framework Accessibility connection", service.serviceInfo)
        return service
    }

    private fun bundledJson(): String = instrumentation.targetContext.assets.open("commands.json").bufferedReader().use { it.readText() }
    private fun registry() = CommandRegistry.fromJson(bundledJson(), BuildConfig.VERSION_CODE)

    @Test fun bundledCatalogCompilesAndKeepsOriginalQueryOnAndroid() {
        val registry = registry()
        val engine = LocalCommandEngine(registry)
        val query = "Sơn Tùng & A+B #100%"
        val resolved = requireNotNull(engine.resolve("mở youtube tìm $query"))
        assertEquals(ActionId.YOUTUBE_SEARCH, resolved.typedAction.action)
        assertEquals(query, resolved.typedAction.string("query"))
        val youtube = YoutubeSearchIntents.build(resolved.typedAction.string("query"))
        assertEquals(Intent.ACTION_VIEW, youtube.action)
        assertEquals("com.google.android.youtube", youtube.`package`)
        assertEquals(query, youtube.data?.getQueryParameter("search_query"))
        assertEquals(setOf("search_query"), youtube.data?.queryParameterNames)
        assertEquals(query, CommandIntents.web(query).data?.getQueryParameter("q"))
        val directions = CommandIntents.navigate(query, "WALKING", inApp = false)
        assertEquals(query, directions.data?.getQueryParameter("destination"))
        assertEquals("walking", directions.data?.getQueryParameter("travelmode"))
        val nearby = requireNotNull(engine.resolve("tìm địa điểm gần đây quán cà phê"))
        assertEquals("gần đây quán cà phê", nearby.typedAction.string("query"))
        val pair = requireNotNull(engine.resolve("chia màn hình Google Maps và YouTube"))
        assertEquals(ActionId.APP_PAIR_LAUNCH,pair.typedAction.action)
        assertEquals("maps",pair.typedAction.string("firstApp"))
        assertEquals("youtube",pair.typedAction.string("secondApp"))
        val music = requireNotNull(engine.resolve("mở nhạc Sơn Tùng","maps"))
        assertEquals(ActionId.YOUTUBE_PLAY_QUERY,music.typedAction.action)
        assertEquals("Sơn Tùng",music.typedAction.string("query"))
        assertEquals(ActionId.NAVIGATE,requireNotNull(engine.resolve("đưa tôi về nhà","youtube")).typedAction.action)
        assertNull(engine.resolve("bấm số abc"))
        assertNull(engine.resolve("mở youtube rồi gọi mẹ"))
    }

    @Test fun platformJsonRejectsUnsafeRemoteArgumentsAndKeepsLastGoodRegistry() {
        val registry = registry()
        fun decode(arguments: JSONObject, mode: String = "REMOTE", supported: Set<ActionId> = ActionId.entries.toSet()) =
            DeviceCommandProtocol.decode(DeviceAction("fixture-plan", "fixture-action", "DEVICE_COMMAND", mode, arguments), registry, supported)
        fun valid() = JSONObject("""{"actionSchemaVersion":1,"action":"WEB_SEARCH","params":{"query":"Sơn Tùng & A+B"}}""")
        assertEquals("Sơn Tùng & A+B", decode(valid()).string("query"))
        val invalid = listOf(
            valid().put("actionSchemaVersion", "1"), valid().put("actionSchemaVersion", 1.0),
            valid().put("confirmed", true), valid().put("params", JSONObject().put("query", JSONObject.NULL)),
            valid().put("params", JSONObject().put("query", "A").put("script", "arbitrary")),
            valid().put("action", "CALL_NUMBER").put("params", JSONObject().put("number", "09012;34")),
            valid().put("action", "VOLUME_SET").put("params", JSONObject().put("operation", "SET").put("percent", 101)),
            valid().put("action", "APP_LAUNCH").put("params", JSONObject().put("app", "unconfigured_app")),
            valid().put("action", "APP_PAIR_LAUNCH").put("params", JSONObject().put("firstApp", "maps").put("secondApp", "maps")),
            valid().put("action", "APP_PAIR_LAUNCH").put("params", JSONObject().put("firstApp", "maps").put("secondApp", "unconfigured_app")),
        )
        invalid.forEachIndexed { index, arguments ->
            assertTrue("Unsafe fixture $index must reject", runCatching { decode(arguments) }.isFailure)
        }
        assertTrue(runCatching { decode(valid(), mode = "LOCAL") }.isFailure)
        assertTrue(runCatching { decode(valid(), supported = setOf(ActionId.APP_LAUNCH)) }.isFailure)
        val holder = CommandRegistryHolder(registry)
        assertFalse(holder.reload(JSONObject(bundledJson()).put("schemaVersion", 99).toString()))
        assertFalse(holder.reload("{"))
        assertSame(registry, holder.current)
    }

    @Test fun warmMatcherReportsP95With1000PatternsOnDevice() {
        // One prefix deliberately exercises a crowded index bucket. Compilation is outside timing.
        val config = JSONObject(bundledJson()).put("commands", JSONArray().put(JSONObject()
            .put("id", "benchmark_query").put("action", "WEB_SEARCH")
            .put("patterns", JSONArray((0 until 1000).map { "mẫu $it {query}" }))
            .put("slots", JSONObject().put("query", JSONObject().put("type", "TEXT").put("required", true)))))
        val engine = LocalCommandEngine(CommandRegistry.fromJson(config.toString(), BuildConfig.VERSION_CODE))
        val phrase = "mẫu 999 Sơn Tùng"
        repeat(200) { assertNotNull(engine.resolve(phrase)) }
        val samples = LongArray(300) {
            val start = SystemClock.elapsedRealtimeNanos()
            val result = engine.resolve(phrase)
            val elapsed = SystemClock.elapsedRealtimeNanos() - start
            assertEquals("Sơn Tùng", result?.typedAction?.string("query"))
            elapsed
        }.sorted()
        val p95 = samples[284]
        instrumentation.sendStatus(0, Bundle().apply {
            putInt("patternCount", 1000); putInt("warmSamples", 300); putLong("matchP95Nanos", p95)
            putBoolean("fold3Target", Build.MODEL.startsWith("SM-F926"))
        })
        if (Build.MODEL.startsWith("SM-F926")) assertTrue("Fold3 warm p95 must be <= 5 ms; observed $p95 ns", p95 <= 5_000_000)
    }

    @Test fun callRequiresVoiceAndTtsErrorCancelsWithoutDispatch() {
        val context = instrumentation.targetContext
        val registry = registry()
        val call = ActionCatalog.validate("CALL_NUMBER", mapOf("number" to "0901234567"))
        instrumentation.runOnMainSync {
            val executor = LocalActionExecutor(context, DpcClient(context), registry::appPackage)
            var direct: LocalResult? = null
            executor.execute(call) { direct = it }
            assertEquals("CONFIRMATION_REQUIRED", direct?.errorCode)
            val router = ActionRouter(context, registry, executor)
            var terminal: LocalResult? = null
            router.execute("no-voice", call, null) { terminal = it }
            assertEquals("VOICE_INTERACTION_REQUIRED", terminal?.errorCode)
            terminal = null
            var prompt: ActionRouter.Prompt? = null
            router.execute("tts-error", call, { prompt = it }) { terminal = it }
            assertTrue(router.awaitingInteraction)
            assertNull(terminal)
            assertFalse(router.promptFinished(requireNotNull(prompt), false))
            assertEquals(LocalOutcome.REJECTED, terminal?.outcome)
            assertEquals("TTS_FAILED", terminal?.errorCode)
            assertFalse(router.continueInteraction("xác nhận"))
        }
    }

    @Test fun duplicateTtsAndCancelledParentCannotReviveCall() {
        val context = instrumentation.targetContext
        val registry = registry()
        val call = ActionCatalog.validate("CALL_NUMBER", mapOf("number" to "0901234567"))
        instrumentation.runOnMainSync {
            val router = ActionRouter(context, registry, LocalActionExecutor(context, DpcClient(context), registry::appPackage))
            val results = mutableListOf<LocalResult>()
            var prompt: ActionRouter.Prompt? = null
            router.execute("cancel-parent", call, { prompt = it }, "call-action") { results.add(it) }
            val original = requireNotNull(prompt)
            assertTrue(original.text.contains("0 9 0 1 2 3 4 5 6 7"))
            assertTrue(router.promptFinished(original, true))
            assertFalse(router.promptFinished(original, true))
            assertTrue(router.awaitingInteraction)
            assertTrue(results.isEmpty())
            assertTrue(router.continueInteraction("hủy"))
            assertEquals(listOf("CANCELLED"), results.map { it.errorCode })
            assertFalse(router.promptFinished(original, true))
            assertFalse(router.continueInteraction("xác nhận"))
            router.execute("new-parent", call, { prompt = it }) { results.add(it) }
            assertTrue(router.promptFinished(requireNotNull(prompt), true))
            assertFalse(router.continueInteraction("mở youtube"))
            assertFalse(router.awaitingInteraction)
            assertFalse(router.continueInteraction("xác nhận"))
            assertEquals(1, results.size)
        }
    }

    @Test fun systemCommandsRejectAbsentSamsungDesktopWithoutPhoneDispatch() {
        val context = instrumentation.targetContext
        val desktops = context.getSystemService(DisplayManager::class.java)
            .getDisplays(DexAccessibilityService.SAMSUNG_DESKTOP_CATEGORY).filter { it.isValid && it.displayId != 0 }
        assumeTrue("This regression requires an ordinary AOSP display", desktops.isEmpty())
        val registry = registry()
        instrumentation.runOnMainSync {
            val router = ActionRouter(context, registry, LocalActionExecutor(context, DpcClient(context), registry::appPackage))
            for (operation in listOf("BACK", "HOME", "SETTINGS", "NOTIFICATIONS")) {
                var result: LocalResult? = null
                router.execute("no-desktop", ActionCatalog.validate("SYSTEM_ACTION", mapOf("operation" to operation)), null) { result = it }
                assertEquals(LocalOutcome.REJECTED, result?.outcome)
                assertEquals("DEX_DISPLAY_UNAVAILABLE", result?.errorCode)
            }
            var pair: LocalResult? = null
            router.execute("no-desktop-pair",ActionCatalog.validate("APP_PAIR_LAUNCH",mapOf("firstApp" to "maps","secondApp" to "youtube")),null) { pair = it }
            assertEquals(LocalOutcome.REJECTED,pair?.outcome)
            assertEquals("DEX_DISPLAY_UNAVAILABLE",pair?.errorCode)
        }
    }

    /** Opt-in opens only this APK's disposable fixture and changes only its text; no gesture executes. */
    @Test fun changedSemanticTargetOnSelectedDesktopRejectsBeforeDispatch() {
        assumeTrue("Explicit isolated fixture opt-in required",
            InstrumentationRegistry.getArguments().getString("runConfigCommandFixture") == "true")
        val context = instrumentation.targetContext
        val service = boundOwnerService()
        val display = context.getSystemService(DisplayManager::class.java)
            .getDisplays(DexAccessibilityService.SAMSUNG_DESKTOP_CATEGORY).filter { it.isValid && it.displayId != 0 }.singleOrNull()
        assertNotNull("One selected DeX display is required", display)
        val displayId = requireNotNull(display).displayId
        try {
            instrumentation.runOnMainSync {
                context.startActivity(Intent(context, CommandRevalidationFixtureActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle())
            }
            val access = DesktopUiAccess(service)
            var target: DesktopUiAccess.Target? = null
            await("Fixture target must become visible on selected desktop") {
                instrumentation.runOnMainSync { target = access.targets(CommandRevalidationFixtureActivity.ORIGINAL).singleOrNull() }
                target != null
            }
            assertEquals(displayId, target!!.tree.displayId)
            assertEquals(context.packageName, target!!.tree.packageName)
            instrumentation.runOnMainSync { requireNotNull(CommandRevalidationFixtureActivity.active).changeLabel() }
            await("Changed fixture label must reach accessibility tree") {
                var visible = false
                instrumentation.runOnMainSync { visible = access.targets(CommandRevalidationFixtureActivity.CHANGED).size == 1 }
                visible
            }
            var status: String? = null
            instrumentation.runOnMainSync { access.click(requireNotNull(target)) { status = it } }
            assertEquals("SCREEN_CHANGED", status)
            assertEquals(0, requireNotNull(CommandRevalidationFixtureActivity.active).clicks)
        } finally {
            instrumentation.runOnMainSync { CommandRevalidationFixtureActivity.active?.finish() }
        }
    }
    /** Opt-in launches Maps and YouTube on DeX; reports only result and scoped window geometry. */
    @Test fun physicalMapsYoutubeAppPairReportsVerifiedLayout() {
        assumeTrue("Explicit physical app-pair opt-in required",
            InstrumentationRegistry.getArguments().getString("runPhysicalAppPair") == "true")
        assertTrue("Physical Fold3 required", Build.MODEL.startsWith("SM-F926"))
        val context = instrumentation.targetContext
        val service = boundOwnerService()
        val display = context.getSystemService(DisplayManager::class.java)
            .getDisplays(DexAccessibilityService.SAMSUNG_DESKTOP_CATEGORY)
            .filter { it.isValid && it.displayId != 0 }.singleOrNull()
        assertNotNull("One selected DeX display is required", display)
        val displayId = requireNotNull(display).displayId
        val registry = registry()
        val firstPackage = requireNotNull(registry.appPackage("maps"))
        val secondPackage = requireNotNull(registry.appPackage("youtube"))
        val complete = CountDownLatch(1)
        var terminal: LocalResult? = null
        instrumentation.runOnMainSync {
            val router = ActionRouter(context, registry, LocalActionExecutor(context, DpcClient(context), registry::appPackage))
            router.execute("physical-app-pair", ActionCatalog.validate("APP_PAIR_LAUNCH",
                mapOf("firstApp" to "maps", "secondApp" to "youtube")), null) {
                terminal = it
                complete.countDown()
            }
        }
        assertTrue("App-pair router must deliver a terminal result within 15 seconds", complete.await(15, TimeUnit.SECONDS))
        val result = requireNotNull(terminal)
        var geometry = JSONArray()
        instrumentation.runOnMainSync {
            geometry = JSONArray(DesktopUiAccess(service).applicationWindows(displayId)
                .filter { it.packageName == firstPackage || it.packageName == secondPackage }
                .map { window -> JSONObject().put("package", window.packageName)
                    .put("left", window.bounds.left).put("top", window.bounds.top)
                    .put("right", window.bounds.right).put("bottom", window.bounds.bottom) })
        }
        instrumentation.sendStatus(0, Bundle().apply {
            putString("appPairOutcome", result.outcome.name)
            putString("appPairErrorCode", result.errorCode)
            putInt("appPairDisplayId", displayId)
            putString("appPairScopedWindows", geometry.toString())
        })
        assertEquals("Physical router result: $result; scoped geometry: $geometry", LocalOutcome.VERIFIED, result.outcome)
        assertEquals("APP_PAIR_VERIFIED", result.errorCode)
    }

    /** Public query fixture through the same coordinator as natural voice; owner history is untouched. */
    @Test fun physicalMapsAddressVoiceRouteDispatchesOnSelectedDesktop() {
        assumeTrue("Explicit physical Maps query opt-in required",
            InstrumentationRegistry.getArguments().getString("runPhysicalMapsAddress") == "true")
        assertTrue("Physical Fold3 required", Build.MODEL.startsWith("SM-F926"))
        val context = instrumentation.targetContext
        val service = boundOwnerService()
        val access = DesktopUiAccess(service)
        val desktop = requireNotNull(access.desktop()) { "One selected DeX display is required" }
        val unique = java.util.UUID.randomUUID().toString()
        val history = com.khuongnd.dexassistant.ui.history.CommandHistoryStore(context,
            "maps_voice_fixture_$unique", "maps_voice_fixture_$unique")
        val done = CountDownLatch(1)
        var result: VoiceCommandResult? = null
        try {
            instrumentation.runOnMainSync {
                val coordinator = AssistantCoordinator(context, history)
                coordinator.dispatchVoice("Tìm địa chỉ Phà Cát Lái") { result = it; done.countDown() }
            }
            assertTrue(done.await(10, TimeUnit.SECONDS))
            assertEquals("DISPATCHED", result?.status)
            var publicQueryVisible = false
            await("Public Maps query must be visible on the selected DeX display") {
                instrumentation.runOnMainSync {
                    val tree = access.tree()
                    publicQueryVisible = tree?.displayId == desktop.displayId &&
                        tree.packageName == CommandIntents.MAPS_PACKAGE && tree.nodes.any {
                            com.khuongnd.dexassistant.ui.routing.folded(it.text).contains("pha cat lai") ||
                                com.khuongnd.dexassistant.ui.routing.folded(it.description).contains("pha cat lai")
                        }
                }
                publicQueryVisible
            }
            instrumentation.sendStatus(0, Bundle().apply {
                putString("mapsVoiceStatus", result?.status)
                putInt("mapsVoiceDisplayId", desktop.displayId)
                putBoolean("mapsPublicQueryVisible", publicQueryVisible)
            })
        } finally {
            history.flush(); history.clear()
            context.deleteSharedPreferences("maps_voice_fixture_$unique")
        }
    }

    private fun await(message: String, timeoutMs: Long = 10_000L, ready: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) { if (ready()) return; Thread.sleep(50) }
        fail(message)
    }
}
