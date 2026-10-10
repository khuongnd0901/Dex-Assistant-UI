package com.khuongnd.dexassistant.ui

import android.app.UiAutomation
import android.os.Bundle
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.voice.VoiceAssistantService
import com.khuongnd.dexassistant.ui.voice.VoiceLiveState
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in read-only RAM diagnostics. Never reads transcript, history, credentials or PCM. */
class VoiceRuntimeObservationDeviceTest {
    @Test fun observeVoiceRuntimeWithoutTranscriptOrActions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Explicit read-only voice runtime observation opt-in required",
            arguments.getString("observeVoiceRuntime") == "true")
        val seconds = arguments.getString("voiceObservationSeconds", "30")!!.toLongOrNull()
        assertTrue("voiceObservationSeconds must be 5..120", seconds != null && seconds in 5L..120L)
        assertNotNull(instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES))
        val deadline = SystemClock.uptimeMillis() + requireNotNull(seconds) * 1000
        while (SystemClock.uptimeMillis() < deadline) {
            val live = VoiceLiveState.state.value
            // Microphone description is RAM-only; emit categorical route and numeric RMS,
            // never the full description or live transcript.
            val route = when {
                live.microphone.startsWith("Micro điện thoại") -> "BUILTIN"
                live.microphone.startsWith("Micro USB") -> "USB"
                live.microphone.startsWith("Micro Bluetooth") -> "BLUETOOTH"
                live.microphone.startsWith("STT") -> "STT_SERVICE_MANAGED"
                else -> "UNVERIFIED"
            }
            val rms = Regex("RMS (-?[0-9]+(?:\\.[0-9]+)?)").find(live.microphone)?.groupValues?.get(1)
            instrumentation.sendStatus(0, Bundle().apply {
                putString("voiceRuntimeState", VoiceAssistantService.active?.currentVoiceState()?.name ?: "SERVICE_ABSENT")
                putString("microphoneRoute", route)
                putString("microphoneRms", rms ?: "UNAVAILABLE")
                putBoolean("recognizerReady", live.ready)
                putBoolean("diagnosticOnly", live.diagnostic)
                putString("captureAudioState", when {
                    live.microphone.contains("âm thanh bị hệ thống chặn") -> "SILENCED"
                    live.microphone.contains("đang nhận âm thanh") -> "RECEIVING"
                    else -> "UNVERIFIED"
                })
            })
            Thread.sleep(1000)
        }
    }
}
