package com.khuongnd.dexassistant.ui

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.history.CommandHistoryStore
import com.khuongnd.dexassistant.ui.voice.*
import org.junit.Assert.*
import org.junit.Test

/** No command dispatch, permission changes, audio export or transcript logging. */
class VoiceHandoffDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private var originalPolicy: Map<String, *> = emptyMap<String, Any>()
    @org.junit.Before fun backupPolicy() { originalPolicy = context.getSharedPreferences("voice_policy", 0).all }
    @org.junit.After fun restorePolicy() { restoreVoicePolicy(context, originalPolicy) }

    @org.junit.Before fun preserveOwnerEnabledServices() {
        instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    }
    private fun action(action: String) {
        instrumentation.runOnMainSync {
            if (action == VoiceAssistantService.ACTION_STOP) {
                context.getSharedPreferences("voice_policy", 0).edit().remove(VoiceAutoStart.KEY_SUPPRESSION).commit()
            }
            // Tests own a foreground Activity; avoid an FGS deadline for rapid control/STOP sequences.
            context.startService(Intent(context, VoiceAssistantService::class.java).setAction(action))
        }
        if (action == VoiceAssistantService.ACTION_STOP) {
            instrumentation.waitForIdleSync()
            val deadline = android.os.SystemClock.elapsedRealtime() + 5000
            val preferences = context.getSharedPreferences("voice_health", 0)
            while ((VoiceAssistantService.active != null || preferences.getString("state", "") != "STOPPED" ||
                context.getSharedPreferences("voice_policy", 0).getString(VoiceAutoStart.KEY_SUPPRESSION, "") != "USER_STOPPED") && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(20)
            assertNull("STOP must release service", VoiceAssistantService.active)
            assertEquals("STOPPED", preferences.getString("state", ""))
            assertEquals("USER_STOPPED", context.getSharedPreferences("voice_policy", 0).getString(VoiceAutoStart.KEY_SUPPRESSION, ""))
        }
    }
    private fun diagnostic(stopFirst: Boolean = true): Pair<VoiceAssistantService, RecognitionListener> {
        if (stopFirst) action(VoiceAssistantService.ACTION_STOP)
        action(VoiceAssistantService.ACTION_DIAGNOSTIC)
        val deadline = android.os.SystemClock.elapsedRealtime() + 5000
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            val service = VoiceAssistantService.active
            val id = service?.javaClass?.getDeclaredField("recognizerEpoch")?.apply { isAccessible = true }?.get(service) as? Long
            if (service != null && id != null) {
                val type = Class.forName("com.khuongnd.dexassistant.ui.voice.VoiceAssistantService\$CommandListener")
                val constructor = type.declaredConstructors.single().apply { isAccessible = true }
                return service to (constructor.newInstance(service, id) as RecognitionListener)
            }
            Thread.sleep(20)
        }
        error("Diagnostic recognizer did not start")
    }
    private fun awaitActualProviderReady(service: VoiceAssistantService) {
        val health = context.getSharedPreferences("voice_health", 0)
        val deadline = android.os.SystemClock.elapsedRealtime() + 8000
        while (!health.getString("detail", "").orEmpty().startsWith("Ready for Vietnamese command") && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(25)
        assertSame(service, VoiceAssistantService.active)
        assertTrue("Provider must acquire capture/focus before synthetic terminal injection", health.getString("detail", "").orEmpty().startsWith("Ready for Vietnamese command"))
    }
    @Test fun realProviderHandoffStaysReadyWithoutCompetingAppFocus() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val (service, _) = diagnostic()
            try {
                val preferences = context.getSharedPreferences("voice_health", 0)
                val deadline = android.os.SystemClock.elapsedRealtime() + 8000
                while (!preferences.getString("detail", "").orEmpty().startsWith("Ready for Vietnamese command") && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(25)
                assertTrue("Provider must reach ready", preferences.getString("detail", "").orEmpty().startsWith("Ready for Vietnamese command"))
                instrumentation.runOnMainSync {
                    context.getSharedPreferences("voice_policy",0).edit().putBoolean("autoStart",true).putBoolean("wakeWord",true).commit()
                    VoiceAutoStart.userResume(context)
                    VoiceAutoStart.evaluate(context)
                    assertSame("Recovery must not take over active STT",service,VoiceAssistantService.active)
                }
                Thread.sleep(2000)
                instrumentation.runOnMainSync {
                    assertTrue("Visual readiness must match provider readiness", VoiceLiveState.state.value.ready)
                    val feedback = service.javaClass.getDeclaredField("feedback").apply { isAccessible = true }.get(service) as com.khuongnd.dexassistant.ui.voice.VoiceFeedback
                    assertTrue("Media cue must dispatch and finish, not leak", feedback.readyCueDescription.startsWith("READY_CUE_MEDIA_FINISHED"))
                    assertNull(feedback.javaClass.getDeclaredField("cue").apply { isAccessible = true }.get(feedback))
                    assertFalse(service.javaClass.getDeclaredField("focusHeld").apply { isAccessible = true }.getBoolean(service))
                    assertEquals("TRANSCRIBING", context.getSharedPreferences("voice_health", 0).getString("state", ""))
                    assertTrue(context.getSharedPreferences("voice_health", 0).getString("detail", "").orEmpty().startsWith("Ready for Vietnamese command"))
                }
            } finally { action(VoiceAssistantService.ACTION_STOP) }
        }
    }
    @Test fun actualPartialFinalDiagnosticPathsNeverExecuteAndLateCallbackCannotUpdate() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val before = CommandHistoryStore(context).read()
            val (service, listener) = diagnostic()
            try {
                instrumentation.runOnMainSync {
                    listener.onReadyForSpeech(Bundle())
                    val feedback = service.javaClass.getDeclaredField("feedback").apply { isAccessible = true }.get(service) as com.khuongnd.dexassistant.ui.voice.VoiceFeedback
                    val cue = feedback.javaClass.getDeclaredField("cue").apply { isAccessible = true }.get(feedback)
                    assertNotNull("Readiness must allocate media cue", cue)
                    listener.onReadyForSpeech(Bundle())
                    assertSame("Duplicate readiness must not replay", cue, feedback.javaClass.getDeclaredField("cue").apply { isAccessible = true }.get(feedback))
                    listener.onPartialResults(Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("mật khẩu secret")) })
                    assertEquals("[REDACTED]", VoiceLiveState.state.value.transcript)
                    assertFalse(VoiceLiveState.state.value.final)
                    listener.onResults(Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("bấm ô 11")) })
                }
                assertEquals(before, CommandHistoryStore(context).read())
                assertEquals("bấm ô 11", VoiceLiveState.state.value.transcript)
                action(VoiceAssistantService.ACTION_STOP)
                instrumentation.runOnMainSync {
                    listener.onReadyForSpeech(Bundle())
                    assertFalse("Stale readiness cannot reactivate cue/UI", VoiceLiveState.state.value.ready)
                    val feedback = service.javaClass.getDeclaredField("feedback").apply { isAccessible = true }.get(service) as com.khuongnd.dexassistant.ui.voice.VoiceFeedback
                    assertNull(feedback.javaClass.getDeclaredField("cue").apply { isAccessible = true }.get(feedback))
                    listener.onPartialResults(Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("late")) })
                    listener.onResults(Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("mở youtube")) })
                }
                assertEquals("", VoiceLiveState.state.value.transcript)
                assertEquals(before, CommandHistoryStore(context).read())
            } finally { action(VoiceAssistantService.ACTION_STOP) }
        }
    }
    @Test fun missingTtsCompletionIsBoundedAndReturnsToNativeCapture() {
        ActivityScenario.launch(MainActivity::class.java).use {
            action(VoiceAssistantService.ACTION_START)
            val health = context.getSharedPreferences("voice_health", 0)
            val warmDeadline = android.os.SystemClock.elapsedRealtime() + 10_000
            while (!health.getString("detail", "").orEmpty().startsWith("Listening for Hey Hey offline") && android.os.SystemClock.elapsedRealtime() < warmDeadline) Thread.sleep(25)
            assertTrue("Warm actual native model before timing feedback", health.getString("detail", "").orEmpty().startsWith("Listening for Hey Hey offline"))
            val (service, _) = diagnostic(stopFirst = false)
            try {
                awaitActualProviderReady(service)
                var staleCompletion: (() -> Unit)? = null
                val started = android.os.SystemClock.elapsedRealtime()
                instrumentation.runOnMainSync {
                    val id = service.javaClass.getDeclaredField("recognizerEpoch").apply { isAccessible = true }.get(service) as Long
                    service.javaClass.getDeclaredField("diagnosticOnly").apply { isAccessible = true }.setBoolean(service, false)
                    val feedback = service.javaClass.getDeclaredField("feedback").apply { isAccessible = true }.get(service) as VoiceFeedback
                    feedback.javaClass.getDeclaredField("initialized").apply { isAccessible = true }.setBoolean(feedback, true)
                    feedback.javaClass.getDeclaredField("ready").apply { isAccessible = true }.setBoolean(feedback, true)
                    service.javaClass.getDeclaredMethod("finishWithFeedback", Long::class.javaPrimitiveType, String::class.java, String::class.java)
                        .apply { isAccessible = true }.invoke(service, id, "Khó quá, không làm được.", "AI_TIMEOUT")
                    // Suppress only completion delivery, leaving real TTS and watchdog lifecycle active.
                    val callback = feedback.javaClass.getDeclaredField("completion").apply { isAccessible = true }
                    @Suppress("UNCHECKED_CAST")
                    staleCompletion = callback.get(feedback) as? (() -> Unit)
                    assertNotNull(staleCompletion)
                    callback.set(feedback, null)
                }
                val deadline = started + 6_000
                while (!health.getString("detail", "").orEmpty().startsWith("Listening for Hey Hey offline") && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(25)
                assertTrue("Missing TTS terminal must rearm within 4s plus bounded warm capture", health.getString("detail", "").orEmpty().startsWith("Listening for Hey Hey offline"))
                while (!VoiceLiveState.state.value.microphone.startsWith("Micro") && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(25)
                assertTrue("Resumed wake must read samples, not merely open AudioRecord", VoiceLiveState.state.value.microphone.startsWith("Micro"))
                assertTrue(android.os.SystemClock.elapsedRealtime() - started >= 3_900)
                assertEquals("TTS_PLAYBACK_TIMEOUT", health.getString(VoiceAssistantService.KEY_TTS_STATUS, ""))
                instrumentation.runOnMainSync {
                    val epoch = service.javaClass.getDeclaredField("activeWakeEpoch").apply { isAccessible = true }
                    val resumed = epoch.get(service)
                    staleCompletion!!.invoke()
                    assertEquals("Late completion cannot replace resumed capture", resumed, epoch.get(service))
                    val recorder = service.javaClass.getDeclaredField("recorder").apply { isAccessible = true }.get(service) as android.media.AudioRecord
                    assertEquals(android.media.AudioRecord.RECORDSTATE_RECORDING, recorder.recordingState)
                    assertNotEquals(true, recorder.activeRecordingConfiguration?.isClientSilenced)
                }
            } finally {
                action(VoiceAssistantService.ACTION_STOP)
                val field = service.javaClass.getDeclaredField("wakeEngine").apply { isAccessible = true }
                val deadline = android.os.SystemClock.elapsedRealtime() + 5000
                while (field.get(service) != null && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(25)
                assertNull("Destroyed service must release cached native model on its worker", field.get(service))
            }
        }
    }
    @Test fun successiveLocalFinalsReturnToRealWakeCaptureWithoutBackendOrTap() {
        val records = context.getSharedPreferences("command_history_v1", 0)
        CommandHistoryStore(context).flush() // Snapshot only after prior writes have drained.
        val original = records.getString("records", null)
        ActivityScenario.launch(MainActivity::class.java).use {
            try {
                var firstService: VoiceAssistantService? = null
                var firstModel: Any? = null
                repeat(3) { cycle ->
                    // Controlled final injection exercises normal routing/feedback/wake, not wake accuracy.
                    val (service, listener) = diagnostic(stopFirst = cycle == 0)
                    if (firstService == null) firstService = service else assertSame("Cycles must reuse the running voice service", firstService, service)
                    awaitActualProviderReady(service)
                    instrumentation.runOnMainSync {
                        service.javaClass.getDeclaredField("diagnosticOnly").apply { isAccessible = true }.setBoolean(service, false)
                        VoiceLiveState.begin(false)
                        listener.onResults(Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("bấm ô 999")) })
                    }
                    val health = context.getSharedPreferences("voice_health", 0)
                    val deadline = android.os.SystemClock.elapsedRealtime() + 35_000
                    while ((health.getString("state", "") != "LISTENING" || !health.getString("detail", "").orEmpty().startsWith("Listening for Hey Hey offline")) && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(50)
                    assertEquals("LISTENING", health.getString("state", ""))
                    var capturing = false
                    instrumentation.runOnMainSync {
                        val recorder = service.javaClass.getDeclaredField("recorder").apply { isAccessible = true }.get(service) as? android.media.AudioRecord
                        capturing = recorder?.recordingState == android.media.AudioRecord.RECORDSTATE_RECORDING && recorder.activeRecordingConfiguration?.isClientSilenced != true
                    }
                    assertTrue("Real wake capture must resume unsilenced", capturing)
                    val model = service.javaClass.getDeclaredField("wakeEngine").apply { isAccessible = true }.get(service)
                    assertNotNull("Native wake model must remain available", model)
                    if (firstModel == null) firstModel = model else assertSame("Warm turns must reuse one model with a new stream", firstModel, model)
                    val measuredDeadline = android.os.SystemClock.elapsedRealtime() + 2500
                    while (!VoiceLiveState.state.value.microphone.startsWith("Micro") && android.os.SystemClock.elapsedRealtime() < measuredDeadline) Thread.sleep(25)
                    assertTrue("Wake must have read samples and published bounded microphone metadata", VoiceLiveState.state.value.microphone.startsWith("Micro"))
                    assertEquals("LOCAL", CommandHistoryStore(context).read().first().route)
                    assertEquals("INVALID_SCREEN_COMMAND", CommandHistoryStore(context).read().first().outcome)
                    instrumentation.runOnMainSync { listener.onError(SpeechRecognizer.ERROR_CLIENT); listener.onPartialResults(Bundle()) }
                    Thread.sleep(250)
                    assertEquals("LISTENING", health.getString("state", ""))
                }
            } finally {
                action(VoiceAssistantService.ACTION_STOP)
                CommandHistoryStore(context).flush() // Drain queued writes before restoring owner ciphertext/key.
                val editor = records.edit()
                if (original == null) editor.remove("records") else editor.putString("records", original)
                assertTrue(editor.commit())
                instrumentation.runOnMainSync {
                    val core = AssistantController.shared(context)
                    core.javaClass.getDeclaredMethod("refreshHistory").apply { isAccessible = true }.invoke(core)
                }
            }
        }
    }

}
