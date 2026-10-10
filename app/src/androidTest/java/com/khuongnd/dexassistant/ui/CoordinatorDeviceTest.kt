package com.khuongnd.dexassistant.ui

import android.media.AudioManager
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.history.CommandHistoryStore
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Headless routing evidence; does not assert wake/STT or physical Fold3 behavior. */
class CoordinatorDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @org.junit.Ignore("T-018: retired backend-required routing assumptions; direct AI acceptance is separate")
    @Test fun activityDetachLeavesLocalCoordinatorAliveAndBackendClausesStayAtomic() = Unit

    @Test fun immediateCancelPreventsQueuedLocalExecution() {
        val audio = context.getSystemService(AudioManager::class.java)
        val original = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val latch = CountDownLatch(1)
        var result: VoiceCommandResult? = null
        instrumentation.runOnMainSync {
            val core = AssistantController.shared(context)
            val id = core.dispatchVoice("đặt âm lượng 0 phần trăm") { result = it; latch.countDown() }
            core.cancelVoice(id)
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS))
        assertEquals("CANCELLED", result?.status)
        assertEquals(original, audio.getStreamVolume(AudioManager.STREAM_MUSIC))
    }

    private fun dispatch(core: AssistantCoordinator, text: String): VoiceCommandResult {
        val latch = CountDownLatch(1)
        var result: VoiceCommandResult? = null
        core.dispatchVoice(text) { result = it; latch.countDown() }
        assertTrue("Command completion timed out", latch.await(10, TimeUnit.SECONDS))
        return requireNotNull(result)
    }
}
