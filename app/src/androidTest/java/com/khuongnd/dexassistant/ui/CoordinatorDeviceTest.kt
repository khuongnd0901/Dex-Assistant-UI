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

    @Test fun activityDetachLeavesLocalCoordinatorAliveAndBackendClausesStayAtomic() {
        val audio = context.getSystemService(AudioManager::class.java)
        val original = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        lateinit var core: AssistantCoordinator
        try {
            instrumentation.runOnMainSync {
                val facade = AssistantController(context) { }
                facade.start()
                facade.stop()
                core = AssistantController.shared(context)
                core.disconnect()
                core.clearHistory()
            }
            assertEquals("VERIFIED", dispatch(core, "đặt âm lượng 40 phần trăm").status)
            val localVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            assertEquals(kotlin.math.round(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * .4).toInt(), localVolume)
            assertEquals("SEND_FAILED", dispatch(core, "đặt âm lượng 80 phần trăm rồi mở YouTube tìm nhạc nhẹ").status)
            assertEquals(localVolume, audio.getStreamVolume(AudioManager.STREAM_MUSIC))
            assertEquals("BACKEND_REQUIRED", CommandHistoryStore(context).read().first().route)
            assertEquals("UNSUPPORTED", dispatch(core, "kết nối Wi-Fi abc mật khẩu nhiều từ bí mật").status)
            assertEquals("[REDACTED]", CommandHistoryStore(context).read().first().originalText)
        } finally {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, original, 0)
            instrumentation.runOnMainSync { AssistantController.shared(context).clearHistory() }
        }
    }

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
