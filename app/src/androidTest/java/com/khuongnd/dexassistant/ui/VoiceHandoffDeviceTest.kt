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
            val intent = Intent(context, VoiceAssistantService::class.java).setAction(action)
            if (action == VoiceAssistantService.ACTION_STOP) context.startService(intent)
            else androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
        if (action == VoiceAssistantService.ACTION_STOP) {
            instrumentation.waitForIdleSync()
            val deadline = android.os.SystemClock.elapsedRealtime() + 5000
            val preferences = context.getSharedPreferences("voice_health", 0)
            while ((VoiceAssistantService.active != null || preferences.getString("state", "") != "STOPPED") && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(20)
            assertNull("STOP must release service", VoiceAssistantService.active)
            assertEquals("STOPPED", preferences.getString("state", ""))
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
            val (_, listener) = diagnostic()
            try {
                instrumentation.runOnMainSync {
                    listener.onPartialResults(Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("mật khẩu secret")) })
                    assertEquals("[REDACTED]", VoiceLiveState.state.value.transcript)
                    assertFalse(VoiceLiveState.state.value.final)
                    listener.onResults(Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("bấm ô 11")) })
                }
                assertEquals(before, CommandHistoryStore(context).read())
                assertEquals("bấm ô 11", VoiceLiveState.state.value.transcript)
                action(VoiceAssistantService.ACTION_STOP)
                instrumentation.runOnMainSync {
                    listener.onPartialResults(Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("late")) })
                    listener.onResults(Bundle().apply { putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("mở youtube")) })
                }
                assertEquals("", VoiceLiveState.state.value.transcript)
                assertEquals(before, CommandHistoryStore(context).read())
            } finally { action(VoiceAssistantService.ACTION_STOP) }
        }
    }
    @Test fun successiveLocalFinalsReturnToRealWakeCaptureWithoutBackendOrTap() {
        val records = context.getSharedPreferences("command_history_v1", 0)
        val original = records.getString("records", null)
        ActivityScenario.launch(MainActivity::class.java).use {
            try {
                var firstService: VoiceAssistantService? = null
                repeat(3) { cycle ->
                    // Controlled final injection exercises normal routing/feedback/wake, not wake accuracy.
                    val (service, listener) = diagnostic(stopFirst = cycle == 0)
                    if (firstService == null) firstService = service else assertSame("Cycles must reuse the running voice service", firstService, service)
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
