package com.khuongnd.dexassistant.ui

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.actions.LocalActionExecutor
import com.khuongnd.dexassistant.ui.actions.LocalOutcome
import com.khuongnd.dexassistant.ui.actions.LocalResult
import com.khuongnd.dexassistant.ui.ai.*
import com.khuongnd.dexassistant.ui.auth.ChatGptAuthManager
import com.khuongnd.dexassistant.ui.auth.ChatGptFailure
import com.khuongnd.dexassistant.ui.device.DpcClient
import com.khuongnd.dexassistant.ui.execution.ActionRouter
import com.khuongnd.dexassistant.ui.routing.ActionId
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import com.khuongnd.dexassistant.ui.security.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Owner-opted live tests. Native effect is limited to one validated Chrome launch via ActionRouter. */
class ChatGptSubscriptionRoutingLiveDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun context(): Context {
        assumeTrue("Owner opt-in required", InstrumentationRegistry.getArguments().getString("subscriptionLiveProbe") == "true")
        return instrumentation.targetContext
    }
    private fun withFixture(block: (Context, AiProviderSettings, AiCredentialStore) -> Unit) {
        val owner = context()
        val auth = ChatGptAuthManager.get(owner)
        val connection = requireNotNull(auth.connectionKey())
        val original = AiProviderSettings(owner)
        check(original.subscriptionVerified(connection, false) && original.subscriptionVerified(connection, true))
        val ownerPrefs = owner.getSharedPreferences("ai_provider_settings_v1", 0).all
        val ownerKeys = owner.getSharedPreferences("ai_provider_keys_v1", 0).all
        val prefix = "t025-native-routing-${UUID.randomUUID()}-"
        val names = mutableSetOf<String>()
        val isolated = object : ContextWrapper(owner) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                names.add(prefix + name)
                return owner.getSharedPreferences(prefix + name, mode)
            }
        }
        try {
            val settings = AiProviderSettings(isolated)
            val model = original.model(AiProvider.CHATGPT_SUBSCRIPTION)
            settings.setModel(AiProvider.CHATGPT_SUBSCRIPTION, model)
            settings.setEnabled(AiProvider.CHATGPT_SUBSCRIPTION, true)
            settings.recordSubscriptionVerification(connection, model, false)
            settings.recordSubscriptionVerification(connection, model, true)
            settings.setDefaultProvider(AiProvider.CHATGPT_SUBSCRIPTION)
            block(isolated, settings, AiCredentialStore(isolated, prefix))
        } finally {
            names.forEach(owner::deleteSharedPreferences)
            assertTrue("Owner provider settings must remain unchanged", ownerPrefs == owner.getSharedPreferences("ai_provider_settings_v1", 0).all)
            assertTrue("Owner API keys must remain unchanged", ownerKeys == owner.getSharedPreferences("ai_provider_keys_v1", 0).all)
        }
    }

    @Test fun directChatAdapterKeepsDeviceRequestsConversational() = withFixture { context, settings, credentials ->
        val state = DirectChatState().apply { start() }
        val chat = DirectAiChat(credentials, settings, subscription = ChatGptSubscriptionProvider(context, settings))
        val report = Bundle()
        try {
            listOf("Chào tôi bằng một câu tiếng Việt.", "Hãy mở Chrome.").forEachIndexed { i, input ->
                val turn = state.begin(input)
                val start = System.nanoTime()
                val reply = chat.reply(turn)
                assertEquals(i + 1, state.complete(turn, reply))
                report.putLong("adapterChatTurn${i + 1}Ms", (System.nanoTime() - start) / 1_000_000)
                report.putInt("adapterChatTurn${i + 1}PriorPairs", turn.previous.size)
            }
            report.putBoolean("adapterChatDeviceActionsExecuted", false)
            report.putInt("adapterChatValidatedTurns", 2)
        } finally { chat.cancelPendingRequest(); state.clear(); instrumentation.sendStatus(0, report) }
    }

    @Test fun nativeCommandPlannerLaunchesOnlyValidatedChromeThroughActionRouter() = withFixture { context, settings, credentials ->
        val registry = CommandRegistry.load(context)
        check("chrome" in registry.appIds)
        val proxy = MobileAiProxy(credentials, settings, subscription = ChatGptSubscriptionProvider(context, settings))
        val input = "Xin hãy mở ứng dụng Chrome giúp tôi."
        assertNull("Deterministic local commands must not be sent to AI", com.khuongnd.dexassistant.ui.routing.LocalCommandEngine(registry).resolve(input))
        val start = System.nanoTime()
        val actions = proxy.plan(input, registry, setOf(ActionId.APP_LAUNCH))
        // Reject all effects except the single specifically reviewed configured-app launch.
        assertEquals(1, actions.size)
        assertEquals(ActionId.APP_LAUNCH, actions.single().action)
        assertEquals("chrome", actions.single().string("app"))
        val planningMs = (System.nanoTime() - start) / 1_000_000
        val complete = CountDownLatch(1)
        val result = AtomicReference<LocalResult?>()
        val dpc = DpcClient(context)
        var router: ActionRouter? = null
        try {
            instrumentation.runOnMainSync {
                router = ActionRouter(context, registry, LocalActionExecutor(context, dpc, registry::appPackage))
                router!!.execute(UUID.randomUUID().toString(), actions.single(), null, "t025-safe-chrome") {
                    result.set(it); complete.countDown()
                }
            }
            assertTrue("Scoped app launch deadline", complete.await(20, TimeUnit.SECONDS))
            val outcome = requireNotNull(result.get())
            instrumentation.sendStatus(0, Bundle().apply {
                putLong("nativeCommandPlanningMs", planningMs)
                putLong("nativeCommandEndToEndMs", (System.nanoTime() - start) / 1_000_000)
                putString("nativeCommandOutcome", outcome.outcome.name)
                putString("nativeCommandError", outcome.errorCode ?: "NONE")
                putInt("nativeCommandActionCount", 1)
                putBoolean("nativeCommandUsedActionRouter", true)
            })
            assertTrue("Truthful native dispatch required", outcome.outcome in setOf(LocalOutcome.DISPATCHED, LocalOutcome.VERIFIED))
        } finally {
            instrumentation.runOnMainSync { router?.cancel() }
            proxy.cancelPendingRequest(); dpc.unbind()
        }
    }

    @Test fun liveStreamCancellationDiscardsPartialReplyWithoutReplay() {
        val context = context()
        val auth = ChatGptAuthManager.get(context)
        val model = AiProviderSettings(context).model(AiProvider.CHATGPT_SUBSCRIPTION)
        val operation = SubscriptionOperation()
        var deltas = 0
        var cancelledAt = 0L
        val start = System.nanoTime()
        var code = "NONE"
        try {
            ChatGptResponsesClient(auth).response(ChatGptResponsesClient.payload(model,
                JSONArray().put(JSONObject().put("role", "user").put("content", "Viết một đoạn tiếng Việt khoảng 200 từ về cây xanh.")),
                "Reply in Vietnamese."), operation, onDelta = {
                    deltas++
                    if (cancelledAt == 0L) { cancelledAt = System.nanoTime(); operation.cancel() }
                })
            fail("Cancelled partial stream must not return a completed response")
        } catch (e: ChatGptFailure) { code = e.code }
        finally {
            instrumentation.sendStatus(0, Bundle().apply {
                putString("liveCancellationCode", code)
                putInt("liveCancellationDeltas", deltas)
                putLong("liveCancellationTotalMs", (System.nanoTime() - start) / 1_000_000)
                putLong("liveCancellationAfterDeltaMs", if (cancelledAt == 0L) -1 else (System.nanoTime() - cancelledAt) / 1_000_000)
                putBoolean("liveCancelledReplyCommitted", false)
            })
        }
        assertEquals("CANCELLED", code)
        assertTrue(deltas > 0)
    }
}
