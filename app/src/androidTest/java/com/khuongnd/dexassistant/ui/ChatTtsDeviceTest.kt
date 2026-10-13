package com.khuongnd.dexassistant.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.voice.VoiceFeedback
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Opt-in public fixture speech with a separate offline TTS engine; no mic/provider/history. */
class ChatTtsDeviceTest {
    @Test fun offlineVietnameseChunksCompleteOnceAndCancellationAllowsSameIdReplacement() {
        assumeTrue("Explicit audible fixture opt-in required",
            InstrumentationRegistry.getArguments().getString("runChatTtsFixture") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val initialized = CountDownLatch(1)
        lateinit var feedback: VoiceFeedback
        instrumentation.runOnMainSync {
            feedback = VoiceFeedback(instrumentation.targetContext,
                capabilityChanged = { initialized.countDown() }, speechInputLimit = 16)
        }
        try {
            assertTrue(initialized.await(12, TimeUnit.SECONDS))
            assumeTrue("Offline Vietnamese voice required; never synthesize over network",
                feedback.voiceDescription == "TTS_VI_OFFLINE_READY")
            val completed = CountDownLatch(1)
            val starts = AtomicInteger()
            val completions = AtomicInteger()
            instrumentation.runOnMainSync {
                feedback.speakInteraction("Xin chào. Đây là kiểm tra phát tiếng Việt.", "same-public-id", {
                    assertTrue(it); completions.incrementAndGet(); completed.countDown()
                }, onStarted = { starts.incrementAndGet() })
            }
            assertTrue("All chunks must finish", completed.await(25, TimeUnit.SECONDS))
            assertEquals(1, starts.get())
            assertEquals(1, completions.get())

            val replaced = CountDownLatch(1)
            val cancelledCompletions = AtomicInteger()
            instrumentation.runOnMainSync {
                feedback.speakInteraction("Lượt cũ phải dừng, không mở lại microphone.", "same-public-id",
                    { cancelledCompletions.incrementAndGet() }, onStarted = {
                        feedback.cancel()
                        feedback.speakInteraction("Lượt mới đã xong.", "same-public-id", {
                            assertTrue(it); replaced.countDown()
                        })
                    })
            }
            assertTrue("Replacement must finish without old completion", replaced.await(25, TimeUnit.SECONDS))
            assertEquals(0, cancelledCompletions.get())
        } finally {
            instrumentation.runOnMainSync { feedback.close() }
        }
    }
}
