package com.khuongnd.dexassistant.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.ai.*
import com.khuongnd.dexassistant.ui.history.CommandHistoryStore
import com.khuongnd.dexassistant.ui.protocol.ChatSession
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.UUID
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/** Offline synthetic subscription only; unique preferences/history, no owner credentials or actions. */
class SubscriptionLifecycleDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun fixture(timeout: Long = 900000, client: OkHttpClient,
        block: (AssistantCoordinator, LinkedBlockingQueue<ChatSession.Event>, CommandHistoryStore) -> Unit) {
        val owner = instrumentation.targetContext
        val prefix = "subscription-lifecycle-${UUID.randomUUID()}-"
        val names = mutableSetOf<String>()
        val context = object : ContextWrapper(owner) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                names.add(prefix + name); return owner.getSharedPreferences(prefix + name, mode)
            }
        }
        val history = CommandHistoryStore(context, prefix + "history", prefix + "history")
        val events = LinkedBlockingQueue<ChatSession.Event>()
        lateinit var core: AssistantCoordinator
        instrumentation.runOnMainSync {
            core = AssistantCoordinator(context, history, OfflineSubscriptionFixture(client), timeout)
            core.observeChat { events.offer(it) }; core.startChat()
        }
        try {
            assertTrue(events.poll(3, TimeUnit.SECONDS) is ChatSession.Event.Started)
            block(core, events, history)
        } finally {
            instrumentation.runOnMainSync { core.endChat(); core.observeChat(null); core.observeChatIdleExpiry(null) }
            history.flush(); history.clear(); names.forEach(owner::deleteSharedPreferences)
            client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll()
        }
    }
    @Test fun endCancelsHttpAndFreshSessionDoesNotWaitForOldTimeout() {
        val entered = CountDownLatch(1); val cancelled = CountDownLatch(1); val calls = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            if (calls.incrementAndGet() == 1) {
                entered.countDown()
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
                while (!chain.call().isCanceled() && System.nanoTime() < deadline) {
                    try { Thread.sleep(10) } catch (_: InterruptedException) { }
                }
                if (chain.call().isCanceled()) cancelled.countDown()
                throw IOException("fixture cancellation")
            }
            OfflineSubscriptionFixture.response(chain.request())
        }.build()
        fixture(client = client) { core, events, history ->
            instrumentation.runOnMainSync { core.sendChat("Xin chào") }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { core.endChat(); core.startChat(); core.sendChat("Phiên mới") }
            assertTrue(events.poll(2, TimeUnit.SECONDS) is ChatSession.Event.Ended)
            assertTrue(events.poll(2, TimeUnit.SECONDS) is ChatSession.Event.Started)
            assertTrue(cancelled.await(1, TimeUnit.SECONDS))
            val reply = events.poll(3, TimeUnit.SECONDS)
            assertTrue(reply is ChatSession.Event.Reply); assertEquals(1, (reply as ChatSession.Event.Reply).turn)
            assertEquals(2, calls.get()); assertNull(events.poll(100, TimeUnit.MILLISECONDS))
            history.flush(); assertTrue(history.read().isEmpty())
        }
    }
    @Test fun lateCompletedReplyAfterEndIsDiscarded() {
        val entered = CountDownLatch(1); val release = CountDownLatch(1); val returned = CountDownLatch(1)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            entered.countDown()
            while (release.count > 0) { try { release.await(100, TimeUnit.MILLISECONDS) } catch (_: InterruptedException) { } }
            OfflineSubscriptionFixture.response(chain.request()).also { returned.countDown() }
        }.build()
        try {
            fixture(client = client) { core, events, _ ->
                instrumentation.runOnMainSync { core.sendChat("Xin chào") }
                assertTrue(entered.await(3, TimeUnit.SECONDS))
                instrumentation.runOnMainSync { core.endChat() }
                assertTrue(events.poll(2, TimeUnit.SECONDS) is ChatSession.Event.Ended)
                release.countDown(); assertTrue(returned.await(2, TimeUnit.SECONDS))
                assertNull(events.poll(500, TimeUnit.MILLISECONDS))
            }
        } finally { release.countDown() }
    }
    @Test fun idleExpiryBeforeFirstTurnAndRestartRemainBounded() {
        val client = OkHttpClient.Builder().addInterceptor { OfflineSubscriptionFixture.response(it.request()) }.build()
        fixture(750, client) { core, events, _ ->
            assertTrue(events.poll(3, TimeUnit.SECONDS) is ChatSession.Event.Ended)
            assertNull(events.poll(100, TimeUnit.MILLISECONDS))
            instrumentation.runOnMainSync { core.startChat(); core.sendChat("Phiên mới") }
            assertTrue(events.poll(2, TimeUnit.SECONDS) is ChatSession.Event.Started)
            assertEquals(1, (events.poll(2, TimeUnit.SECONDS) as ChatSession.Event.Reply).turn)
        }
    }
    @Test fun sensitiveInputNeverReachesTransport() {
        val calls = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { calls.incrementAndGet(); OfflineSubscriptionFixture.response(it.request()) }.build()
        fixture(client = client) { core, events, _ ->
            instrumentation.runOnMainSync { core.sendChat("api key abc123") }
            assertTrue(events.poll(2, TimeUnit.SECONDS) is ChatSession.Event.Error)
            assertEquals(0, calls.get())
        }
    }
}
