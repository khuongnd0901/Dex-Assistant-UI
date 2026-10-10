package com.khuongnd.dexassistant.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.history.CommandHistoryStore
import com.khuongnd.dexassistant.ui.protocol.ChatSession
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** Opt-in live text Chat only; does not certify microphone, voice routing or audible TTS. */
class DirectAiAcceptanceDeviceTest {
    /** Idempotent enable only: does not verify a Wi-Fi off/on cycle. */
    @Test fun localWifiEnableIsIdempotentOnAlreadyEnabledWifi() {
        assumeTrue("Explicit acceptance opt-in required", InstrumentationRegistry.getArguments()
            .getString("runDirectAiAcceptance") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val context = instrumentation.targetContext
        val wifi = context.getSystemService(android.net.wifi.WifiManager::class.java)
        assumeTrue("Wi-Fi must already be enabled; fixture never enables an initially disabled radio", wifi.isWifiEnabled)
        val suffix = UUID.randomUUID().toString()
        val history = CommandHistoryStore(context, "wifi_enable_acceptance_$suffix", "wifi-enable-acceptance-$suffix")
        val ownerReady = java.util.concurrent.atomic.AtomicBoolean()
        val observer: (UiState) -> Unit = { ownerReady.set(it.deviceOwner) }
        lateinit var core: AssistantCoordinator
        instrumentation.runOnMainSync { core = AssistantCoordinator(context, history); core.addObserver(observer); core.start() }
        val done = java.util.concurrent.CountDownLatch(1)
        var result: VoiceCommandResult? = null
        var requestId: String? = null
        try {
            val deadline = android.os.SystemClock.elapsedRealtime() + 5_000
            while (!ownerReady.get() && android.os.SystemClock.elapsedRealtime() < deadline) {
                instrumentation.runOnMainSync { core.refreshHealth() }
                Thread.sleep(100)
            }
            assertTrue("Existing Device Owner must bind before local command", ownerReady.get())
            assertTrue("Wi-Fi must still be enabled before idempotent command", wifi.isWifiEnabled)
            instrumentation.runOnMainSync { requestId = core.dispatchVoice("bật wifi") { result = it; done.countDown() } }
            assertTrue("Local Wi-Fi command must complete", done.await(15, TimeUnit.SECONDS))
            assertEquals("VERIFIED", result?.status)
            history.flush()
            assertEquals("LOCAL", history.read().first().route)
            assertTrue("Idempotent enable preserves Wi-Fi", wifi.isWifiEnabled)
            android.util.Log.i("DexDirectAcceptance", "wifiIdempotentEnable=VERIFIED route=LOCAL")
        } finally {
            instrumentation.runOnMainSync { requestId?.let { core.cancelVoice(it) }; core.removeObserver(observer); core.stopObservingVoiceHealth() }
            history.flush(); history.clear()
        }
    }

    @Test fun invalidCredentialFailsOnceAndCanRestart() {
        assumeTrue("Explicit live-provider opt-in required", InstrumentationRegistry.getArguments()
            .getString("runDirectAiAcceptance") == "true")
        val pacing = ProviderTestPacing.fromArguments()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val context = instrumentation.targetContext
        val settings = com.khuongnd.dexassistant.ui.security.AiProviderSettings(context)
        val provider = settings.defaultProvider()
        val secrets = com.khuongnd.dexassistant.ui.security.AiCredentialStore(context)
        val original = requireNotNull(secrets.load(provider)) { "Configured provider key required" }
        val suffix = UUID.randomUUID().toString()
        val history = CommandHistoryStore(context, "invalid_key_acceptance_$suffix", "invalid-key-acceptance-$suffix")
        val events = LinkedBlockingQueue<ChatSession.Event>()
        lateinit var core: AssistantCoordinator
        instrumentation.runOnMainSync { core = AssistantCoordinator(context, history); core.observeChat { events.offer(it) } }
        try {
            secrets.save(provider, "t018-intentionally-invalid-test-credential")
            instrumentation.runOnMainSync { core.startChat() }
            assertTrue(events.poll(5, TimeUnit.SECONDS) is ChatSession.Event.Started)
            pacing.delayBeforeRequest()
            instrumentation.runOnMainSync { core.sendChat("Xin chào, trả lời một câu ngắn.") }
            val event = events.poll(30, TimeUnit.SECONDS)
            assertTrue("Invalid credential must produce one Error", event is ChatSession.Event.Error)
            assertNull("Failure must not automatically replay", events.poll(2, TimeUnit.SECONDS))
            assertTrue("Chat error must not write action history", history.read().isEmpty())
            instrumentation.runOnMainSync { core.endChat() }
            assertTrue(events.poll(5, TimeUnit.SECONDS) is ChatSession.Event.Ended)
            secrets.save(provider, original)
            instrumentation.runOnMainSync { core.startChat() }
            assertTrue("Restored credential permits session restart", events.poll(5, TimeUnit.SECONDS) is ChatSession.Event.Started)
            android.util.Log.i("DexDirectAcceptance", "invalidCredential=ERROR events=single restart=STARTED")
        } finally {
            try { instrumentation.runOnMainSync { core.endChat(); core.observeChat(null) } }
            finally { secrets.save(provider, original); history.flush(); history.clear() }
        }
    }

    @Test fun configuredDirectCommandsSetVolumeAndRestoreOwnerVolume() {
        assumeTrue("Explicit live-provider opt-in required", InstrumentationRegistry.getArguments()
            .getString("runDirectAiAcceptance") == "true")
        val pacing = ProviderTestPacing.fromArguments()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val context = instrumentation.targetContext
        val audio = context.getSystemService(android.media.AudioManager::class.java)
        val original = audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val suffix = UUID.randomUUID().toString()
        val history = CommandHistoryStore(context, "direct_command_acceptance_$suffix", "direct-command-acceptance-$suffix")
        lateinit var core: AssistantCoordinator
        instrumentation.runOnMainSync { core = AssistantCoordinator(context, history) }
        fun dispatch(text: String, route: String) {
            if (route == "BACKEND_REQUIRED") pacing.delayBeforeRequest()
            val done = java.util.concurrent.CountDownLatch(1)
            var result: VoiceCommandResult? = null
            var requestId: String? = null
            instrumentation.runOnMainSync {
                requestId = core.dispatchVoice(text) { result = it; done.countDown() }
            }
            try {
                assertTrue("Command must complete", done.await(35, TimeUnit.SECONDS))
                assertEquals("VERIFIED", result?.status)
                history.flush()
                assertEquals(route, history.read().first().route)
                android.util.Log.i("DexDirectAcceptance", "commandRoute=$route terminal=VERIFIED")
            } finally { instrumentation.runOnMainSync { requestId?.let { core.cancelVoice(it) } } }
        }
        fun verifyVolume(percent: Int) {
            assertEquals((audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) * percent / 100.0).roundToInt(),
                audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC))
        }
        try {
            val providerSettings = com.khuongnd.dexassistant.ui.security.AiProviderSettings(context)
            val originalEnables = com.khuongnd.dexassistant.ui.security.AiProvider.entries.associateWith { providerSettings.enabled(it) }
            val changedProviders = originalEnables.filter { (provider, enabled) -> provider.supportsDirectCommands && enabled }.keys
            try {
                changedProviders.forEach { providerSettings.setEnabled(it, false) }
                assertTrue("Implemented cloud providers disabled for local command", originalEnables.keys.filter { it.supportsDirectCommands }.none { providerSettings.enabled(it) })
                dispatch("đặt âm lượng 40 phần trăm", "LOCAL")
                verifyVolume(40)
                android.util.Log.i("DexDirectAcceptance", "localCloudDisabled=true terminal=VERIFIED")
            } finally {
                changedProviders.forEach { providerSettings.setEnabled(it, originalEnables.getValue(it)) }
            }
            assertTrue("Original provider enable flags restored", originalEnables.all { (provider, enabled) -> providerSettings.enabled(provider) == enabled })
            // Deliberately outside local literal patterns, then compound input.
            dispatch("Xin hãy điều chỉnh mức âm lượng phương tiện thành 30 phần trăm", "BACKEND_REQUIRED")
            verifyVolume(30)
            dispatch("Đặt âm lượng phương tiện 20 phần trăm rồi đặt âm lượng phương tiện 60 phần trăm", "BACKEND_REQUIRED")
            verifyVolume(60)
        } finally {
            audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, original, 0)
            history.flush(); history.clear()
        }
    }

    @Test fun configuredProviderRetainsThreeTurnsAndEndSuppressesLateReply() {
        assumeTrue("Explicit live-provider opt-in required", InstrumentationRegistry.getArguments()
            .getString("runDirectAiAcceptance") == "true")
        val pacing = ProviderTestPacing.fromArguments()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val context = instrumentation.targetContext
        val policy = context.getSharedPreferences("voice_policy", 0).all
        val suffix = UUID.randomUUID().toString()
        val history = CommandHistoryStore(context, "direct_ai_acceptance_$suffix", "direct-ai-acceptance-$suffix")
        val events = LinkedBlockingQueue<ChatSession.Event>()
        lateinit var core: AssistantCoordinator
        var initialized = false
        fun next(): ChatSession.Event = requireNotNull(events.poll(30, TimeUnit.SECONDS)) { "Chat event timed out" }
        try {
            instrumentation.runOnMainSync {
                core = AssistantCoordinator(context, history)
                initialized = true
                core.observeChat { events.offer(it) }
                core.startChat()
            }
            assertTrue("Configured provider must start", next() is ChatSession.Event.Started)
            val marker = "MAYTIM${UUID.randomUUID().toString().replace("-", "").take(8)}"
            val prompts = listOf(
                "Hãy ghi nhớ mã $marker. Chỉ trả lời mã này.",
                "Mã tôi vừa nhờ bạn ghi nhớ là gì? Chỉ trả lời mã.",
                "Nhắc lại mã đã ghi nhớ một lần nữa. Chỉ trả lời mã.",
            )
            prompts.forEachIndexed { index, prompt ->
                pacing.delayBeforeRequest()
                instrumentation.runOnMainSync { core.sendChat(prompt) }
                val event = next()
                assertTrue("Turn ${index + 1} must produce Reply; event=${event.javaClass.simpleName}", event is ChatSession.Event.Reply)
                event as ChatSession.Event.Reply
                assertEquals(index + 1, event.turn)
                assertTrue("Reply must preserve context marker", event.text.contains(marker))
                assertTrue(event.text.codePointCount(0, event.text.length) <= 4000)
                android.util.Log.i("DexDirectAcceptance", "turn=${event.turn} replyChars=${event.text.length} context=true")
            }
            pacing.delayBeforeRequest()
            instrumentation.runOnMainSync {
                core.sendChat("Hãy giải thích ngắn gọn vì sao bầu trời có màu xanh.")
                core.endChat()
            }
            assertTrue("End must terminate session", next() is ChatSession.Event.Ended)
            assertNull("Ended session must not emit late reply/error", events.poll(25, TimeUnit.SECONDS))
            assertTrue("Chat must not write action history", history.read().isEmpty())
        } finally {
            if (initialized) instrumentation.runOnMainSync { core.endChat(); core.observeChat(null) }
            history.flush(); history.clear()
            assertEquals("Owner voice policy must remain unchanged", policy, context.getSharedPreferences("voice_policy", 0).all)
        }
    }
}
