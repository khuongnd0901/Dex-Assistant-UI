package com.khuongnd.dexassistant.ui

import android.app.UiAutomation
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioRecord
import android.os.SystemClock
import android.speech.RecognizerIntent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.actions.LocalActionExecutor
import com.khuongnd.dexassistant.ui.actions.LocalOutcome
import com.khuongnd.dexassistant.ui.actions.LocalResult
import com.khuongnd.dexassistant.ui.device.DpcClient
import com.khuongnd.dexassistant.ui.routing.LocalCommandParser
import com.khuongnd.dexassistant.ui.voice.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class VoiceHardwareDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun registeredVoiceCommandResolverIncludesDex() {
        for (action in listOf(Intent.ACTION_VOICE_COMMAND, Intent.ACTION_ASSIST, RecognizerIntent.ACTION_WEB_SEARCH)) {
            val candidates = context.packageManager.queryIntentActivities(Intent(action), PackageManager.MATCH_DEFAULT_ONLY)
            assertTrue("DeX voice button entry missing from supported action resolver", candidates.any {
                it.activityInfo.packageName == context.packageName && it.activityInfo.name == VoiceButtonActivity::class.java.name
            })
        }
    }

    /** Stay silent during this opt-in probe. Isolates execution with the existing diagnostic flag,
     * set on the same main-thread call before any recognizer callback can be delivered. */
    @Test fun directButtonReleasesWakeRecorderBeforeActualSttAndCancelsCleanly() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("runDirectButtonProbe") == "true")
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val service = VoiceAssistantService.active
        assertNotNull("An already-listening owner voice service is required", service)
        val active = requireNotNull(service)
        val policy = context.getSharedPreferences("voice_policy", 0)
        val history = context.getSharedPreferences("command_history_v1", 0)
        val beforePolicy = policy.all
        val beforeHistory = history.all
        var priorRecorder: AudioRecord? = null
        var accepted = false
        instrumentation.runOnMainSync {
            assertEquals(VoiceState.LISTENING, active.currentVoiceState())
            assertTrue("Existing assistant readiness/permission policy must permit activation", VoiceCommandActivation.allowed(context))
            priorRecorder = read(active, "recorder") as? AudioRecord
            assertNotNull("Actual wake capture must be present before handoff", priorRecorder)
            accepted = VoiceCommandActivation.request(context)
            if (accepted) {
                field(active, "diagnosticOnly").setBoolean(active, true)
                field(active, "diagnosticRestore").set(active, VoiceState.LISTENING)
            }
        }
        try {
            assertTrue("Direct button must be admitted without wake detection", accepted)
            await("Actual STT provider did not report readiness") {
                VoiceLiveState.state.value.ready && active.currentVoiceState() == VoiceState.TRANSCRIBING
            }
            instrumentation.runOnMainSync {
                assertNull("Wake microphone must be released before external STT", read(active, "recorder"))
                assertEquals(AudioRecord.STATE_UNINITIALIZED, priorRecorder!!.state)
                assertFalse("Busy STT must reject a second trigger", VoiceCommandActivation.request(context))
                assertNotNull(read(active, "recognizerEpoch"))
            }
        } finally {
            if (accepted) instrumentation.runOnMainSync {
                if (read(active, "diagnosticOnly") == true) {
                    val id = read(active, "recognizerEpoch") as? Long ?:
                        read(read(active, "sessions")!!, "epoch") as Long
                    active.javaClass.getDeclaredMethod("destroyRecognizer").apply { isAccessible = true }.invoke(active)
                    active.javaClass.getDeclaredMethod("finishDiagnostic", Long::class.javaPrimitiveType, String::class.java)
                        .apply { isAccessible = true }.invoke(active, id, "BUTTON_PROBE_CANCELLED")
                }
            }
            if (accepted) await("Cancel must restore genuine wake capture") {
                var resumed = false
                instrumentation.runOnMainSync {
                    val recorder = read(active, "recorder") as? AudioRecord
                    resumed = active.currentVoiceState() == VoiceState.LISTENING &&
                        recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING &&
                        recorder.activeRecordingConfiguration?.isClientSilenced != true &&
                        VoiceLiveState.state.value.microphone.startsWith("Micro")
                }
                resumed
            }
            assertTrue("Owner voice policy must remain unchanged", policy.all == beforePolicy)
            assertTrue("Isolated probe must not write owner history", history.all == beforeHistory)
        }
    }

    @Test fun numericPercentVolumeExecutesLocallyAndRestoresVolumeAndMute() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("runNumericVolumeProbe") == "true")
        val audio = context.getSystemService(AudioManager::class.java)
        assumeTrue("Stop media playback before the numeric volume probe", !audio.isMusicActive)
        val stream = AudioManager.STREAM_MUSIC
        val before = audio.getStreamVolume(stream)
        val beforeMute = audio.isStreamMute(stream)
        try {
            for (percent in listOf(40, 100)) {
                var result: LocalResult? = null
                instrumentation.runOnMainSync {
                    LocalActionExecutor(context, DpcClient(context)).execute(LocalCommandParser().route("tăng âm lượng $percent%")) {
                        result = it
                    }
                }
                assertEquals(LocalOutcome.VERIFIED, result?.outcome)
                assertEquals((audio.getStreamMaxVolume(stream) * percent / 100.0).roundToInt(), audio.getStreamVolume(stream))
            }
        } finally {
            audio.setStreamVolume(stream, before, 0)
            audio.adjustStreamVolume(stream, if (beforeMute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, 0)
            assertEquals(before, audio.getStreamVolume(stream))
            assertEquals(beforeMute, audio.isStreamMute(stream))
        }
    }

    private fun field(owner: Any, name: String) = owner.javaClass.getDeclaredField(name).apply { isAccessible = true }
    private fun read(owner: Any, name: String): Any? = field(owner, name).get(owner)
    private fun await(message: String, predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 12_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (predicate()) return
            Thread.sleep(50)
        }
        fail(message)
    }
}
