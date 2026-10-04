package com.khuongnd.dexassistant.ui

import android.app.UiAutomation
import android.media.AudioRecord
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.voice.VoiceAssistantService
import com.khuongnd.dexassistant.ui.voice.VoiceLiveState
import com.khuongnd.dexassistant.ui.voice.VoiceStateMachine
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Opt-in live observation only. Run with `am instrument --no-restart` against an already
 * running target process. Never starts/pauses/stops a service, injects a callback, captures
 * audio, reads transcript/history/credentials, or changes preferences/permissions/routes.
 * Success means bounded observation completed, NOT that wake recognition works.
 */
@RunWith(AndroidJUnit4::class)
class VoiceCaptureObserverDeviceTest {
    @Test fun observeExistingCapture() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Explicit opt-in required", arguments.getString("observeExistingVoice") == "true")
        val durationSeconds = (arguments.getString("observeSeconds")?.toIntOrNull() ?: 20).coerceIn(1, 20)
        // Normal UiAutomation suppresses accessibility services and would perturb this diagnosis.
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val audioManager = instrumentation.targetContext.getSystemService(AudioManager::class.java)
        var sawService = false
        var completedSnapshots = 0
        var mainTimedOut = false
        val started = SystemClock.elapsedRealtime()
        val deadline = started + durationSeconds * 1_000L
        for (sample in 0 until durationSeconds) {
            val due = started + sample * 1_000L
            val remaining = due - SystemClock.elapsedRealtime()
            if (remaining > 0) Thread.sleep(remaining)
            if (SystemClock.elapsedRealtime() >= deadline) break
            audioManager.getDevices(AudioManager.GET_DEVICES_ALL).forEach { device ->
                emit(Bundle().apply {
                    putInt("deviceId", device.id)
                    putInt("deviceType", device.type)
                    putInt("isSource", if (device.isSource) 1 else 0)
                    putInt("isSink", if (device.isSink) 1 else 0)
                    putString("sampleRates", device.sampleRates.joinToString(","))
                    putString("channelCounts", device.channelCounts.joinToString(","))
                    putString("encodings", device.encodings.joinToString(","))
                })
            }
            val observed = CountDownLatch(1)
            var servicePresent = false
            var snapshotCompleted = false
            Handler(Looper.getMainLooper()).post {
                try {
                    val service = VoiceAssistantService.active
                    servicePresent = service != null
                    if (service == null) {
                        emit(Bundle().apply { putInt("servicePresent", 0) })
                    } else {
                        val now = SystemClock.elapsedRealtime()
                        val state = (read(service, "stateMachine") as VoiceStateMachine).state
                        val sessions = read(service, "sessions")!!
                        val recorder = read(service, "recorder") as? AudioRecord
                        val live = VoiceLiveState.state.value
                        // Only parse fixed numeric/boolean microphone metadata, never output its string.
                        val rms = Regex("RMS (-?\\d+(?:\\.\\d+)?)").find(live.microphone)
                            ?.groupValues?.get(1)?.toDoubleOrNull()
                        val data = Bundle().apply {
                            putInt("servicePresent", 1)
                            putString("stage", state.name)
                            putLong("atMs", now)
                            putLong("wakeEpoch", read(service, "activeWakeEpoch") as? Long ?: -1)
                            putLong("recognizerEpoch", read(service, "recognizerEpoch") as? Long ?: -1)
                            synchronized(sessions) {
                                putLong("sessionEpoch", read(sessions, "epoch") as Long)
                                putInt("sessionActive", if (read(sessions, "active") == true) 1 else 0)
                                putInt("finalConsumed", if (read(sessions, "finalConsumed") == true) 1 else 0)
                            }
                            val last = read(service, "lastMicUpdate") as Long
                            putLong("micUpdateAgeMs", if (last > 0) now - last else -1)
                            putInt("ready", if (live.ready) 1 else 0)
                            putInt("diagnostic", if (live.diagnostic) 1 else 0)
                            putInt("micStage", when {
                                live.microphone.startsWith("Micro") -> 1
                                live.microphone.startsWith("STT") -> 2
                                else -> 0
                            })
                            if (rms != null) putDouble("rms", rms)
                            putInt("recorderPresent", if (recorder != null) 1 else 0)
                            if (recorder != null) {
                                putInt("recorderState", recorder.state)
                                putInt("recordingState", recorder.recordingState)
                                putInt("routeType", recorder.routedDevice?.type ?: -1)
                                putInt("routeId", recorder.routedDevice?.id ?: -1)
                                putInt("preferredType", recorder.preferredDevice?.type ?: -1)
                                putInt("preferredId", recorder.preferredDevice?.id ?: -1)
                                putInt("configurationRouteType", recorder.activeRecordingConfiguration?.audioDevice?.type ?: -1)
                                putInt("configurationRouteId", recorder.activeRecordingConfiguration?.audioDevice?.id ?: -1)
                                putInt("clientSilenced", when (recorder.activeRecordingConfiguration?.isClientSilenced) {
                                    true -> 1
                                    false -> 0
                                    null -> -1
                                })
                            }
                        }
                        emit(data)
                        snapshotCompleted = true
                    }
                } catch (_: Exception) {
                    emit(Bundle().apply { putInt("snapshotReadError", 1) })
                } finally {
                    observed.countDown()
                }
            }
            val mainResponded = observed.await(
                (deadline - SystemClock.elapsedRealtime()).coerceIn(0, 2_000), TimeUnit.MILLISECONDS,
            )
            if (!mainResponded) {
                emit(Bundle().apply { putInt("mainSnapshotTimeout", 1) })
                mainTimedOut = true
                break // Do not accumulate snapshot work against a blocked main thread.
            }
            sawService = sawService || servicePresent
            if (snapshotCompleted) completedSnapshots += 1
            var matchingWorkers = 0
            Thread.getAllStackTraces().forEach { (thread, stack) ->
                val wakeFrames = stack.filter {
                    it.className == "android.media.AudioRecord" ||
                        it.className.startsWith("com.k2fsa.sherpa.onnx.") ||
                        it.className.startsWith("com.khuongnd.dexassistant.ui.voice.SherpaWakeEngine") ||
                        (it.className.startsWith("com.khuongnd.dexassistant.ui.voice.VoiceAssistantService") &&
                            it.methodName.contains("startWake"))
                }
                if (wakeFrames.isNotEmpty()) {
                    matchingWorkers += 1
                    emit(Bundle().apply {
                        putLong("workerThreadId", thread.id)
                        putString("stage", thread.state.name)
                        putString("methods", wakeFrames.take(8).joinToString(",") { it.methodName })
                    })
                }
            }
            emit(Bundle().apply { putInt("matchingWakeWorkers", matchingWorkers) })
        }
        emit(Bundle().apply { putInt("observedExistingService", if (sawService) 1 else 0) })
        assertTrue("No active service observed; this does not establish live wake health", sawService)
        assertTrue("No complete metadata snapshot observed", completedSnapshots > 0)
        assertTrue("Main-thread observation timed out; investigate emitted metadata", !mainTimedOut)
    }

    private fun read(owner: Any, name: String): Any? = owner.javaClass.getDeclaredField(name)
        .apply { isAccessible = true }.get(owner)

    private fun emit(metadata: Bundle) {
        // Known keys contain numeric metadata or fixed enum/method names only.
        val line = metadata.keySet().sorted().joinToString(" ") { "$it=${metadata.get(it)}" }
        Log.i("DexVoiceObserve", line)
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("voiceObservation", line)
        })
    }
}
