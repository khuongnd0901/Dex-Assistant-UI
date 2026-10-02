package com.khuongnd.dexassistant.ui

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Opt-in real backend flow: -e backendEndpoint ws://127.0.0.1:8081/ws/v1/assistant. */
class CoordinatorBackendDeviceTest {
    @Test fun backendMultiActionCompletesAfterActivityObserverDetaches() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val endpoint = InstrumentationRegistry.getArguments().getString("backendEndpoint")
        assumeTrue("A dedicated backend endpoint must be explicitly supplied", !endpoint.isNullOrBlank())
        val token = InstrumentationRegistry.getArguments().getString("backendToken") ?: "avd-test-token"
        val connected = CountDownLatch(1)
        val completed = CountDownLatch(1)
        var result: VoiceCommandResult? = null
        var facade: AssistantController? = null
        val core = AssistantController.shared(instrumentation.targetContext)
        try {
            instrumentation.runOnMainSync {
                core.disconnect()
                facade = AssistantController(instrumentation.targetContext) {
                    if (it.status == "CONNECTED") connected.countDown()
                }
                facade?.start()
                facade?.provision(requireNotNull(endpoint), token)
            }
            assertTrue("Dedicated backend did not connect", connected.await(15, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { facade?.stop() }
            core.dispatchVoice("mở YouTube rồi mở Chrome") {
                result = it
                android.util.Log.i("DexCoordinatorBackend", "terminal=${it.status} requestId=${it.requestId}")
                completed.countDown()
            }
            assertTrue("Backend command did not complete", completed.await(40, TimeUnit.SECONDS))
            assertEquals("Actual terminal: ${result?.status}: ${result?.message}", "DISPATCHED", result?.status)
        } finally {
            instrumentation.runOnMainSync {
                facade?.stop()
                core.disconnect()
                core.clearHistory()
            }
        }
    }
}
