package com.khuongnd.dexassistant.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.voice.CommandRecognizer
import com.khuongnd.dexassistant.ui.voice.SherpaWakeEngine
import com.khuongnd.dexassistant.ui.voice.VoiceAssistantService
import com.khuongnd.dexassistant.ui.voice.VoiceFeedback
import org.junit.Assert.*
import org.junit.Test

/** Runs native KWS on the packaged model; silence is a negative test, not wake accuracy evidence. */
class VoiceRuntimeDeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private var originalPolicy: Map<String, *> = emptyMap<String, Any>()
    @org.junit.Before fun backupPolicy() { instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES); originalPolicy = context.getSharedPreferences("voice_policy", 0).all }
    @org.junit.After fun restorePolicy() { restoreVoicePolicy(context, originalPolicy) }


    @Test fun pinnedNativeModelLoadsDecodesSilenceAndReleases() {
        repeat(3) {
            SherpaWakeEngine(context.assets).use { engine ->
                repeat(100) { assertFalse(engine.accept(ShortArray(320), 320)) }
            }
        }
    }

    @Test fun commandRecognizerUsesVietnameseOnlineAllowedAndExcludesOurStub() {
        val intent = CommandRecognizer.intent()
        assertEquals("vi-VN", intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertFalse(intent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true))
        val component = CommandRecognizer.component(context)
        assertNotNull("AVD must provide an external recognizer for this check", component)
        assertNotEquals(context.packageName, component!!.packageName)
        instrumentation.sendStatus(0, Bundle().apply { putString("commandRecognizer", component.flattenToShortString()) })
    }

    @Test fun boundedRealVietnameseRecognizerSessionReportsDeviceOutcome() {
        val component = CommandRecognizer.component(context)
        assertNotNull(component)
        val ended = CountDownLatch(1)
        val ready = AtomicBoolean(false)
        val outcome = AtomicReference("CALLBACK_TIMEOUT")
        var recognizer: SpeechRecognizer? = null
        instrumentation.runOnMainSync {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context, component)
            recognizer!!.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { ready.set(true) }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onError(error: Int) { outcome.set(CommandRecognizer.errorCode(error)); ended.countDown() }
                override fun onResults(results: Bundle?) {
                    // No audio or recognized text is persisted or printed.
                    val count = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.size ?: 0
                    outcome.set("RESULTS_COUNT_$count")
                    ended.countDown()
                }
            })
            recognizer!!.startListening(CommandRecognizer.intent())
        }
        try { ended.await(30, TimeUnit.SECONDS) } finally {
            instrumentation.runOnMainSync { recognizer?.cancel(); recognizer?.destroy() }
        }
        instrumentation.sendStatus(0, Bundle().apply {
            putString("vietnameseRecognitionSession", "provider=${component!!.packageName};ready=${ready.get()};outcome=${outcome.get()};no_test_speech_supplied")
        })
        // This verifies bounded invocation/callback diagnosis, not Vietnamese recognition accuracy.
    }

    @Test fun boundedActualVietnameseFeedbackReportsSynthesisOutcome() {
        val ended = CountDownLatch(1)
        lateinit var feedback: VoiceFeedback
        instrumentation.runOnMainSync {
            feedback = VoiceFeedback(context)
            feedback.speak("Đã thực hiện.", "diagnostic-${java.util.UUID.randomUUID()}") { ended.countDown() }
        }
        try {
            if (!ended.await(25, TimeUnit.SECONDS)) {
                instrumentation.runOnMainSync { feedback.timeout() }
            }
            instrumentation.sendStatus(0, Bundle().apply {
                putString("vietnameseFeedback", "outcome=${feedback.voiceDescription};canned_text_only")
            })
        } finally {
            instrumentation.runOnMainSync { feedback.close() }
        }
        // An unavailable/network-failed voice remains a measured limitation, not synthesis success.
    }

    @Test fun serviceOwnsContinuousMicrophoneAndStopPreventsLateListening() {
        val packageName = context.packageName
        instrumentation.uiAutomation.executeShellCommand("pm grant $packageName ${Manifest.permission.RECORD_AUDIO}").close()
        val health = context.getSharedPreferences(VoiceAssistantService.HEALTH_PREFERENCES, Context.MODE_PRIVATE)
        // Foreground-start eligibility comes from the retained assistant role; no Activity dependency.
        instrumentation.runOnMainSync {
            context.startForegroundService(Intent(context, VoiceAssistantService::class.java)
                .setAction(VoiceAssistantService.ACTION_START))
        }
        try {
            waitFor(20_000) { health.getString(VoiceAssistantService.KEY_DETAIL, "")!!.startsWith("Listening for Hey Hey offline") }
            instrumentation.runOnMainSync {
                context.startService(Intent(context, VoiceAssistantService::class.java)
                    .setAction(VoiceAssistantService.ACTION_PAUSE))
            }
            waitFor(5_000) { health.getString(VoiceAssistantService.KEY_STATE, "") == "PAUSED" }
            Thread.sleep(500)
            assertEquals("PAUSED", health.getString(VoiceAssistantService.KEY_STATE, ""))
            instrumentation.runOnMainSync {
                context.startService(Intent(context, VoiceAssistantService::class.java)
                    .setAction(VoiceAssistantService.ACTION_RESUME))
            }
            waitFor(20_000) { health.getString(VoiceAssistantService.KEY_DETAIL, "")!!.startsWith("Listening for Hey Hey offline") }
        } finally {
            instrumentation.runOnMainSync {
                context.startService(Intent(context, VoiceAssistantService::class.java)
                    .setAction(VoiceAssistantService.ACTION_STOP))
            }
            waitFor(5_000) { health.getString(VoiceAssistantService.KEY_STATE, "") == "STOPPED" }
            Thread.sleep(500)
            assertEquals("STOPPED", health.getString(VoiceAssistantService.KEY_STATE, ""))
        }
    }

    private fun waitFor(timeout: Long, predicate: () -> Boolean) {
        val until = System.currentTimeMillis() + timeout
        while (!predicate() && System.currentTimeMillis() < until) Thread.sleep(50)
        assertTrue("Device condition did not become true", predicate())
    }
}
