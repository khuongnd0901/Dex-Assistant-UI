package com.khuongnd.dexassistant.ui

import android.app.UiAutomation
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.voice.VoiceAssistantService
import com.khuongnd.dexassistant.ui.voice.VoiceState
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit coordinator operation only; recording collector never invokes this helper. */
class T017VoiceControlTest {
    @Test fun coordinatorPauseOrRestoreVoice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val operation = InstrumentationRegistry.getArguments().getString("t017VoiceControl")
        assumeTrue("Opt-in coordinator operation required", operation != null)
        require(operation == "pause" || operation == "resume")
        require(Build.MODEL.startsWith("SM-F926") && Build.VERSION.SDK_INT >= 35)
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val before = VoiceAssistantService.active?.currentVoiceState()
        require(before == VoiceState.LISTENING || before == VoiceState.PAUSED) {
            "Only already-running idle LISTENING/PAUSED is permitted; state=$before"
        }
        val expected = if (operation == "pause") VoiceState.PAUSED else VoiceState.LISTENING
        if (before != expected) instrumentation.runOnMainSync {
            val controller = AssistantController(instrumentation.targetContext) { }
            // No start/connect/provision/permissions; use the existing facade only.
            if (operation == "pause") controller.pauseVoice() else controller.resumeVoice()
        }
        val deadline = SystemClock.elapsedRealtime() + 5000
        while (VoiceAssistantService.active?.currentVoiceState() != expected && SystemClock.elapsedRealtime() < deadline) Thread.sleep(25)
        val after = VoiceAssistantService.active?.currentVoiceState()
        instrumentation.sendStatus(0, Bundle().apply {
            putString("t017CoordinatorOperation", operation)
            putString("voiceStateBefore", before.name)
            putString("voiceStateAfter", after?.name ?: "UNAVAILABLE")
        })
        require(after == expected) { "Coordinator voice guard failed; expected=$expected observed=$after" }
    }
}
