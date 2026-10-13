package com.khuongnd.dexassistant.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.ai.*
import com.khuongnd.dexassistant.ui.security.*
import com.khuongnd.dexassistant.ui.history.CommandHistoryStore
import com.khuongnd.dexassistant.ui.protocol.ChatSession
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/** Offline transport uses uniquely named fixture preferences and Keystore aliases, never owner keys. */
class DirectAiFaultDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val fixtureScope = "offline-ai-fixture-${java.util.UUID.randomUUID()}-"
    private val fixturePreferences = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val context by lazy {
        object : android.content.ContextWrapper(instrumentation.targetContext) {
            override fun getApplicationContext(): android.content.Context = this
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                fixturePreferences.add(name)
                return instrumentation.targetContext.getSharedPreferences(fixtureScope + name, mode)
            }
        }
    }
    private fun configured(block: (AiCredentialStore, AiProviderSettings) -> Unit) {
        val secrets = AiCredentialStore(context, fixtureScope + "key-")
        val settings = AiProviderSettings(context)
        try {
            AiProvider.entries.forEach { settings.setEnabled(it, false) }
            settings.setDefaultProvider(AiProvider.GEMINI)
            settings.setModel(AiProvider.GEMINI, "offline-test-model")
            settings.setFreeTierConfirmed(AiProvider.GEMINI, true)
            settings.setEnabled(AiProvider.GEMINI, true)
            secrets.save(AiProvider.GEMINI, "offline-fixture-placeholder")
            block(secrets, settings)
        } finally {
            secrets.delete(AiProvider.GEMINI)
            settings.setEnabled(AiProvider.GEMINI, false)
            fixturePreferences.forEach {
                instrumentation.targetContext.getSharedPreferences(fixtureScope + it, 0).edit().clear().commit()
            }
        }
    }
    private fun response(request: Request, code: Int) = Response.Builder().request(request)
        .protocol(Protocol.HTTP_1_1).code(code).message("fixture")
        .body("""{"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"Xin chào"}]}}]}""".toResponseBody()).build()

    @Test fun geminiHttpFaultsHaveExactCodesAndDoNotReplay() = configured { secrets, settings ->
        listOf(429 to "AI_QUOTA_EXHAUSTED", 401 to "AI_AUTH_FAILED", 403 to "AI_AUTH_FAILED",
            408 to "CHAT_PROVIDER_UNAVAILABLE", 503 to "CHAT_PROVIDER_UNAVAILABLE").forEach { (status, code) ->
            val calls = AtomicInteger()
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                calls.incrementAndGet(); response(chain.request(), status)
            }.build()
            try {
                DirectAiChat(secrets, settings, client).reply(DirectChatState().begin("Xin chào"))
                fail("Expected HTTP $status failure")
            } catch (error: MobileAiException) {
                assertEquals(code, error.code)
                assertEquals(status !in listOf(401, 403), error.canFallback)
            }
            assertEquals("Gemini-only fault must not replay", 1, calls.get())
        }
    }
    @Test fun disconnectedTransportIsTimeoutWithoutReplay() = configured { secrets, settings ->
        val calls = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor {
            calls.incrementAndGet(); throw IOException("offline fixture")
        }.build()
        try {
            DirectAiChat(secrets, settings, client).reply(DirectChatState().begin("Xin chào"))
            fail("Expected disconnected transport")
        } catch (error: MobileAiException) {
            assertEquals("CHAT_PROVIDER_TIMEOUT", error.code)
            assertTrue(error.canFallback)
        }
        assertEquals(1, calls.get())
    }
    @Test fun endDuringConfirmedInFlightResponseSuppressesLateReplyAndRestartsCleanly() = configured { secrets, _ ->
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val returned = CountDownLatch(1)
        val calls = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            if (calls.incrementAndGet() == 1) {
                entered.countDown()
                // Return even after interruption to exercise stale-result rejection.
                while (release.count > 0) {
                    try { release.await(100, TimeUnit.MILLISECONDS) } catch (_: InterruptedException) { }
                }
            }
            response(chain.request(), 200).also { returned.countDown() }
        }.build()
        val events = LinkedBlockingQueue<ChatSession.Event>()
        val history = CommandHistoryStore(context, "offline_chat_history", "offline-chat-history")
        lateinit var core: AssistantCoordinator
        instrumentation.runOnMainSync {
            core = AssistantCoordinator(context, history, client, secrets)
            core.observeChat { events.offer(it) }; core.startChat()
        }
        try {
            assertTrue(events.poll(5, TimeUnit.SECONDS) is ChatSession.Event.Started)
            instrumentation.runOnMainSync { core.sendChat("Xin chào") }
            assertTrue("Request must be in flight", entered.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { core.endChat() }
            assertTrue(events.poll(5, TimeUnit.SECONDS) is ChatSession.Event.Ended)
            release.countDown()
            assertTrue(returned.await(5, TimeUnit.SECONDS))
            assertNull("No late reply/error", events.poll(1, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { core.startChat() }
            assertTrue(events.poll(5, TimeUnit.SECONDS) is ChatSession.Event.Started)
            instrumentation.runOnMainSync { core.sendChat("Phiên mới") }
            val reply = events.poll(5, TimeUnit.SECONDS)
            assertTrue(reply is ChatSession.Event.Reply)
            assertEquals(1, (reply as ChatSession.Event.Reply).turn)
            assertEquals(2, calls.get())
            history.flush(); assertTrue(history.read().isEmpty())
        } finally {
            release.countDown()
            instrumentation.runOnMainSync { core.endChat(); core.observeChat(null) }
            history.flush(); history.clear()
        }
    }

    @Test fun geminiCommandHttpFaultsHaveExactCodesAndDoNotReplay() = configured { secrets, settings ->
        val registry = com.khuongnd.dexassistant.ui.routing.CommandRegistry.load(context)
        val supported = setOf(com.khuongnd.dexassistant.ui.routing.ActionId.VOLUME_SET)
        listOf(429 to "AI_QUOTA_EXHAUSTED", 401 to "AI_AUTH_FAILED", 403 to "AI_AUTH_FAILED",
            408 to "AI_UNAVAILABLE", 503 to "AI_UNAVAILABLE").forEach { (status, code) ->
            val calls = AtomicInteger()
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                calls.incrementAndGet(); response(chain.request(), status)
            }.build()
            try {
                MobileAiProxy(secrets, settings, client).plan("Đặt âm lượng 30 phần trăm", registry, supported)
                fail("Expected Command HTTP $status failure")
            } catch (error: MobileAiException) {
                assertEquals(code, error.code)
                assertEquals(status !in listOf(401, 403), error.canFallback)
            }
            assertEquals("Gemini-only Command fault must not replay", 1, calls.get())
        }
    }

    @Test fun geminiCommandDisconnectedTransportIsTimeoutWithoutReplay() = configured { secrets, settings ->
        val calls = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor {
            calls.incrementAndGet(); throw IOException("offline fixture")
        }.build()
        try {
            MobileAiProxy(secrets, settings, client).plan("Đặt âm lượng 30 phần trăm",
                com.khuongnd.dexassistant.ui.routing.CommandRegistry.load(context),
                setOf(com.khuongnd.dexassistant.ui.routing.ActionId.VOLUME_SET))
            fail("Expected disconnected Command transport")
        } catch (error: MobileAiException) {
            assertEquals("AI_TIMEOUT", error.code)
            assertTrue(error.canFallback)
        }
        assertEquals(1, calls.get())
    }

    @Test fun endCancelsUnderlyingCallAndAllowsFreshSessionWithoutWaitingForTimeout() = configured { secrets, _ ->
        val entered = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        val calls = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            if (calls.incrementAndGet() == 1) {
                entered.countDown()
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
                while (!chain.call().isCanceled() && System.nanoTime() < deadline) {
                    try { Thread.sleep(10) } catch (_: InterruptedException) { }
                }
                if (chain.call().isCanceled()) cancelled.countDown()
                throw IOException("cancelled fixture")
            }
            response(chain.request(), 200)
        }.build()
        val events = LinkedBlockingQueue<ChatSession.Event>()
        val history = CommandHistoryStore(context, "cancel_chat_history", "cancel-chat-history")
        lateinit var core: AssistantCoordinator
        instrumentation.runOnMainSync {
            core = AssistantCoordinator(context, history, client, secrets)
            core.observeChat { events.offer(it) }; core.startChat()
        }
        try {
            assertTrue(events.poll(5, TimeUnit.SECONDS) is ChatSession.Event.Started)
            instrumentation.runOnMainSync { core.sendChat("Xin chào") }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { core.endChat(); core.startChat(); core.sendChat("Phiên mới") }
            assertTrue(events.poll(5, TimeUnit.SECONDS) is ChatSession.Event.Ended)
            assertTrue(events.poll(5, TimeUnit.SECONDS) is ChatSession.Event.Started)
            assertTrue("Underlying transport must be cancelled promptly", cancelled.await(1, TimeUnit.SECONDS))
            val reply = events.poll(2, TimeUnit.SECONDS)
            assertTrue("Fresh session cannot wait on the old HTTP timeout", reply is ChatSession.Event.Reply)
            assertEquals(1, (reply as ChatSession.Event.Reply).turn)
            assertEquals(2, calls.get())
            assertNull(events.poll(100, TimeUnit.MILLISECONDS))
        } finally {
            instrumentation.runOnMainSync { core.endChat(); core.observeChat(null) }
            history.flush(); history.clear()
        }
    }

    @Test fun explicitlyCancelledCallCannotFallbackToAnotherConfiguredProvider() = configured { secrets, settings ->
        settings.setModel(AiProvider.GROQ, "offline-secondary-model")
        settings.setFreeTierConfirmed(AiProvider.GROQ, true)
        settings.setEnabled(AiProvider.GROQ, true)
        secrets.save(AiProvider.GROQ, "offline-secondary-placeholder")
        val entered = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val calls = AtomicInteger()
        val error = java.util.concurrent.atomic.AtomicReference<MobileAiException?>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls.incrementAndGet(); entered.countDown()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (!chain.call().isCanceled() && System.nanoTime() < deadline) Thread.sleep(10)
            throw IOException("cancelled fixture")
        }.build()
        val chat = DirectAiChat(secrets, settings, client)
        val worker = Thread {
            try { chat.reply(DirectChatState().begin("Xin chào")) }
            catch (failure: MobileAiException) { error.set(failure) }
            finally { finished.countDown() }
        }
        try {
            worker.start()
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            chat.cancelPendingRequest()
            assertTrue(finished.await(2, TimeUnit.SECONDS))
            assertEquals("CHAT_PROVIDER_TIMEOUT", error.get()?.code)
            assertEquals(false, error.get()?.canFallback)
            assertEquals("Cancelled Gemini request must not contact secondary provider", 1, calls.get())
        } finally {
            chat.cancelPendingRequest(); worker.join(6000)
            secrets.delete(AiProvider.GROQ); settings.setEnabled(AiProvider.GROQ, false)
        }
    }

    @Test fun idleSessionEndsWithoutFirstTurnAndCanRestart() = configured { secrets, _ ->
        val client = OkHttpClient.Builder().addInterceptor { chain -> response(chain.request(), 200) }.build()
        val events = LinkedBlockingQueue<ChatSession.Event>()
        val history = CommandHistoryStore(context, "idle_chat_history", "idle-chat-history")
        val expirations = AtomicInteger()
        lateinit var core: AssistantCoordinator
        instrumentation.runOnMainSync {
            core = AssistantCoordinator(context, history, client, secrets, 750L)
            core.observeChatIdleExpiry {
                expirations.incrementAndGet()
                // Service teardown may call endChat again; it must not emit another Ended.
                core.endChat("STOP")
            }
            core.observeChat { events.offer(it) }; core.startChat()
        }
        try {
            assertTrue(events.poll(2, TimeUnit.SECONDS) is ChatSession.Event.Started)
            assertTrue("Session must expire before any user turn", events.poll(3, TimeUnit.SECONDS) is ChatSession.Event.Ended)
            assertNull(events.poll(100, TimeUnit.MILLISECONDS))
            assertEquals(1, expirations.get())
            instrumentation.runOnMainSync { core.startChat(); core.sendChat("Phiên mới") }
            assertTrue(events.poll(2, TimeUnit.SECONDS) is ChatSession.Event.Started)
            val reply = events.poll(2, TimeUnit.SECONDS)
            assertTrue(reply is ChatSession.Event.Reply)
            assertEquals(1, (reply as ChatSession.Event.Reply).turn)
            instrumentation.runOnMainSync { core.endChat() }
            assertTrue(events.poll(2, TimeUnit.SECONDS) is ChatSession.Event.Ended)
            assertEquals("Explicit end must not trigger idle teardown", 1, expirations.get())
        } finally {
            instrumentation.runOnMainSync { core.endChat(); core.observeChat(null); core.observeChatIdleExpiry(null) }
            history.flush(); history.clear()
        }
    }

    @Test fun fixtureCredentialsAndPreferencesLeaveOwnerStoresUnchanged() {
        val names = listOf("ai_provider_settings_v1", "ai_provider_keys_v1", "voice_policy", "voice_health")
        val before = names.associateWith { instrumentation.targetContext.getSharedPreferences(it, 0).all }
        val ownerAlias = "dex-assistant-ai-key-v1-gemini"
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val ownerKeyBefore = keyStore.containsAlias(ownerAlias)
        configured { secrets, settings ->
            assertEquals("offline-fixture-placeholder", secrets.load(AiProvider.GEMINI))
            assertTrue(settings.enabled(AiProvider.GEMINI))
            context.getSharedPreferences("voice_policy", 0).edit().putString("fixture_marker", "isolated").commit()
            assertTrue(keyStore.containsAlias(fixtureScope + "key-gemini"))
            names.forEach { assertTrue("Owner $it must not change", before.getValue(it) == instrumentation.targetContext.getSharedPreferences(it, 0).all) }
            assertEquals(ownerKeyBefore, keyStore.containsAlias(ownerAlias))
        }
        assertFalse(keyStore.containsAlias(fixtureScope + "key-gemini"))
        names.forEach { assertTrue("Owner $it must remain unchanged", before.getValue(it) == instrumentation.targetContext.getSharedPreferences(it, 0).all) }
    }
}
