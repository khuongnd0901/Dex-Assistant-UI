package com.khuongnd.dexassistant.ui

import android.view.KeyEvent
import android.content.Intent
import android.speech.RecognizerIntent
import com.khuongnd.dexassistant.ui.voice.VoiceButtonActions
import com.khuongnd.dexassistant.ui.voice.VoiceActivationGate
import com.khuongnd.dexassistant.ui.voice.VoiceHardwareKeyGate
import com.khuongnd.dexassistant.ui.voice.VoiceState
import org.junit.Assert.*
import org.junit.Test

class VoiceHardwareActivationTest {
    @Test fun voiceButtonIntentActionsRejectUnknownActions() {
        assertTrue(VoiceButtonActions.supported(Intent.ACTION_VOICE_COMMAND))
        assertTrue(VoiceButtonActions.supported(Intent.ACTION_ASSIST))
        assertTrue(VoiceButtonActions.supported(RecognizerIntent.ACTION_WEB_SEARCH))
        listOf(null, "", Intent.ACTION_VIEW, Intent.ACTION_MAIN, Intent.ACTION_SEARCH,
            Intent.ACTION_WEB_SEARCH, RecognizerIntent.ACTION_RECOGNIZE_SPEECH,
            RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE).forEach {
            assertFalse(VoiceButtonActions.supported(it))
        }
    }
    @Test fun rejectedReadinessAndBusyTurnsNeverConsumeAdmission() {
        val gate = VoiceActivationGate()
        assertFalse(gate.admit(1000, false, VoiceState.LISTENING))
        VoiceState.entries.filterNot { VoiceActivationGate.idle(it) || it == VoiceState.TRANSCRIBING }.forEach {
            assertFalse("Reject $it", gate.admit(1000, true, it))
        }
        assertTrue("Active STT accepts explicit submit even inside start debounce",
            gate.admit(1050, true, VoiceState.TRANSCRIBING))
        assertTrue("Repeated submit reaches service; session guard consumes only once",
            gate.admit(1051, true, VoiceState.TRANSCRIBING))
        val sessions = com.khuongnd.dexassistant.ui.voice.VoiceSessionGuard()
        val epoch = sessions.begin()
        assertTrue(sessions.consumeFinal(epoch))
        assertFalse("A second submit/final callback cannot consume the turn again", sessions.consumeFinal(epoch))
        assertTrue(gate.admit(1000, true, VoiceState.LISTENING))
        assertFalse(gate.admit(1100, true, VoiceState.LISTENING))
        assertTrue(gate.admit(1600, true, VoiceState.LISTENING))
    }

    @Test fun admittedExternalPressConsumesRepeatAndMatchingReleaseOnce() {
        val gate = VoiceHardwareKeyGate()
        var calls = 0
        fun key(action: Int, repeat: Int = 0, cancelled: Boolean = false) = gate.onKey(
            4, true, KeyEvent.KEYCODE_VOICE_ASSIST, action, repeat, cancelled, 1000,
        ) { calls++; true }
        assertTrue(key(KeyEvent.ACTION_DOWN))
        assertTrue(key(KeyEvent.ACTION_DOWN, repeat = 1))
        assertTrue(key(KeyEvent.ACTION_UP, cancelled = true))
        assertFalse(key(KeyEvent.ACTION_UP))
        assertEquals(1, calls)
    }

    @Test fun unrelatedInternalBusyCancelledAndUnpairedKeysPassThrough() {
        val gate = VoiceHardwareKeyGate()
        var calls = 0
        for (code in listOf(KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_MUTE)) {
            assertFalse(gate.onKey(4, true, code, KeyEvent.ACTION_DOWN, 0, false, 1000) { calls++; true })
        }
        assertFalse(gate.onKey(0, false, KeyEvent.KEYCODE_ASSIST, KeyEvent.ACTION_DOWN, 0, false, 1000) { calls++; true })
        assertFalse(gate.onKey(4, true, KeyEvent.KEYCODE_ASSIST, KeyEvent.ACTION_DOWN, 0, true, 1000) { calls++; true })
        assertEquals(0, calls)
        assertFalse(gate.onKey(4, true, KeyEvent.KEYCODE_ASSIST, KeyEvent.ACTION_DOWN, 0, false, 1000) { false })
        assertFalse(gate.onKey(4, true, KeyEvent.KEYCODE_ASSIST, KeyEvent.ACTION_UP, 0, false, 1000) { calls++; true })
        assertEquals(0, calls)
    }

    @Test fun freshPressAfterLostReleaseCanActivateAgainWithoutConsumingOtherDevice() {
        val gate = VoiceHardwareKeyGate()
        var calls = 0
        assertTrue(gate.onKey(4, true, KeyEvent.KEYCODE_ASSIST, KeyEvent.ACTION_DOWN, 0, false, 1000) { calls++; true })
        assertFalse(gate.onKey(5, true, KeyEvent.KEYCODE_ASSIST, KeyEvent.ACTION_UP, 0, false, 1000) { true })
        assertTrue(gate.onKey(4, true, KeyEvent.KEYCODE_ASSIST, KeyEvent.ACTION_DOWN, 0, false, 2000) { calls++; true })
        assertFalse(gate.onKey(4, true, KeyEvent.KEYCODE_ASSIST, KeyEvent.ACTION_UP, 0, false, 1000) { true })
        assertTrue(gate.onKey(4, true, KeyEvent.KEYCODE_ASSIST, KeyEvent.ACTION_UP, 0, false, 2000) { true })
        assertEquals(2, calls)
    }
}
