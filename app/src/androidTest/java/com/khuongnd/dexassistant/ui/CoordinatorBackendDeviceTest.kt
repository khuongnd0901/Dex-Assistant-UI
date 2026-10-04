package com.khuongnd.dexassistant.ui

import android.app.UiAutomation
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.history.CommandHistoryStore
import com.khuongnd.dexassistant.ui.protocol.AssistantWebSocket
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Opt-in real WS/routing/action/headless flow, not production credential provisioning. */
class CoordinatorBackendDeviceTest {
    @Test fun backendMultiActionCompletesAfterActivityObserverDetaches() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assertNotNull(instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES))
        val endpoint = InstrumentationRegistry.getArguments().getString("backendEndpoint")
        assumeTrue("A dedicated backend endpoint must be explicitly supplied", !endpoint.isNullOrBlank())
        val token = InstrumentationRegistry.getArguments().getString("backendToken")
        assumeTrue("A dedicated backend token must be explicitly supplied", !token.isNullOrBlank())
        val owner = instrumentation.targetContext
        val ownerConfig = owner.getSharedPreferences("assistant_config", 0).all
        val ownerCredential = owner.getSharedPreferences("device_credential", 0).all
        val ownerKeys = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val ownerCredentialKeyPresent = ownerKeys.containsAlias("dex-assistant-device-token-v1")
        val ownerHistoryKeyPresent = ownerKeys.containsAlias("dex-assistant-history-v1")
        val suffix = UUID.randomUUID().toString()
        val configPrefs = "backend_device_test_config_$suffix"
        val credentialPrefs = "backend_device_test_credential_$suffix"
        val historyPrefs = "backend_device_test_history_$suffix"
        val historyAlias = "dex-backend-device-test-history-$suffix"
        val isolatedContext = object : ContextWrapper(owner) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = super.getSharedPreferences(
                when (name) {
                    "assistant_config" -> configPrefs
                    "device_credential" -> credentialPrefs
                    else -> name
                }, mode,
            )
            override fun getApplicationContext(): Context = this
        }
        val history = CommandHistoryStore(owner, historyPrefs, historyAlias)
        val connected = CountDownLatch(1)
        val completed = CountDownLatch(1)
        val authenticationFailed = AtomicBoolean()
        val observerCalls = AtomicInteger()
        var result: VoiceCommandResult? = null
        var core: AssistantCoordinator? = null
        var socket: AssistantWebSocket? = null
        val observer: (UiState) -> Unit = {
            observerCalls.incrementAndGet()
            if (it.status == "CONNECTED") connected.countDown()
        }
        try {
            instrumentation.runOnMainSync {
                val isolatedCore = AssistantCoordinator(isolatedContext, history)
                core = isolatedCore
                isolatedCore.addObserver(observer)
                val actualSocket = isolatedCore.javaClass.getDeclaredField("webSocket").apply { isAccessible = true }
                    .get(isolatedCore) as AssistantWebSocket
                socket = actualSocket
                // CredentialStore's Keystore alias is fixed. RAM-only connect avoids save;
                // intercept auth failure so even a rejected token cannot delete the owner's key.
                actualSocket.javaClass.getDeclaredField("listener").apply { isAccessible = true }
                    .set(actualSocket, object : AssistantWebSocket.Listener by isolatedCore {
                        override fun onAuthenticationFailure() {
                            authenticationFailed.set(true)
                            actualSocket.disconnect()
                            connected.countDown(); completed.countDown()
                        }
                    })
                actualSocket.connect(requireNotNull(endpoint), requireNotNull(token))
            }
            assertTrue("Dedicated backend did not connect", connected.await(15, TimeUnit.SECONDS))
            assertFalse("Dedicated backend authentication failed", authenticationFailed.get())
            instrumentation.runOnMainSync { core!!.removeObserver(observer) }
            val detachedObserverCalls = observerCalls.get()
            instrumentation.runOnMainSync {
                core!!.dispatchVoice("mở YouTube rồi mở Chrome") {
                    result = it
                    android.util.Log.i("DexCoordinatorBackend", "terminal=${it.status} requestId=${it.requestId}")
                    completed.countDown()
                }
            }
            assertTrue("Backend command did not complete", completed.await(40, TimeUnit.SECONDS))
            assertFalse("Dedicated backend authentication failed", authenticationFailed.get())
            assertEquals("Actual terminal status", "DISPATCHED", result?.status)
            assertEquals("Detached observer must not receive subsequent state", detachedObserverCalls, observerCalls.get())
        } finally {
            instrumentation.runOnMainSync {
                core?.removeObserver(observer)
                core?.disconnect()
                socket?.close()
            }
            history.flush()
            history.clear() // Only the unique test preferences/key, never owner history.
            assertTrue(owner.getSharedPreferences(configPrefs, 0).edit().clear().commit())
            assertTrue(owner.getSharedPreferences(credentialPrefs, 0).edit().clear().commit())
            assertTrue("Owner configuration must remain unchanged", ownerConfig == owner.getSharedPreferences("assistant_config", 0).all)
            assertTrue("Owner credential preferences must remain unchanged", ownerCredential == owner.getSharedPreferences("device_credential", 0).all)
            assertEquals("Owner credential key must remain present", ownerCredentialKeyPresent, ownerKeys.containsAlias("dex-assistant-device-token-v1"))
            assertEquals("Owner history key must remain present", ownerHistoryKeyPresent, ownerKeys.containsAlias("dex-assistant-history-v1"))
        }
    }
}
