package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.*
import org.junit.Test

class VoicePresentationTest {
    @Test fun focusHandoffIgnoresStaleLossButRetainsOwnedFeedbackInterruption() {
        assertFalse(VoiceFocusPolicy.pauseForLoss(false, VoiceState.TRANSCRIBING))
        assertFalse(VoiceFocusPolicy.pauseForLoss(false, VoiceState.SPEAKING))
        assertTrue(VoiceFocusPolicy.pauseForLoss(true, VoiceState.SPEAKING))
        assertFalse(VoiceFocusPolicy.pauseForLoss(true, VoiceState.SPEAKING, 1, 2))
        assertTrue(VoiceFocusPolicy.pauseForLoss(true, VoiceState.SPEAKING, 2, 2))
    }
    @Test fun readinessIsTransientAcrossTurnsAndCueIsFiniteWithSilentGap() {
        VoiceLiveState.begin(false)
        VoiceLiveState.ready(true)
        assertTrue(VoiceLiveState.state.value.ready)
        VoiceLiveState.begin(true)
        assertFalse(VoiceLiveState.state.value.ready)
        VoiceLiveState.ready(true)
        VoiceLiveState.ready(false)
        assertFalse(VoiceLiveState.state.value.ready)
        val pcm = ReadyCueSignal.samples()
        assertEquals(5760, pcm.size)
        assertTrue(pcm.take(2560).any { it > 10000 })
        assertTrue(pcm.slice(2560 until 3200).all { it == 0.toShort() })
        assertTrue(pcm.drop(3200).any { it < -10000 })
        assertEquals(0, pcm.first().toInt())
    }
    @Test fun partialAndFinalCredentialsAreMaskedAndOnlyRamStateChanges() {
        for (text in listOf("mật", "đặt mật khẩu abc", "bear", "pass", "secr", "mã", "token abc", "api key xyz")) assertEquals("[REDACTED]", VoiceLiveState.safeTranscript(text))
        VoiceLiveState.begin(true)
        VoiceLiveState.text("bấm ô mười một", false)
        assertTrue(VoiceLiveState.state.value.diagnostic); assertFalse(VoiceLiveState.state.value.final)
        VoiceLiveState.text("bấm ô 11", true)
        assertTrue(VoiceLiveState.state.value.final)
        VoiceLiveState.clear(); assertEquals("", VoiceLiveState.state.value.transcript)
    }
}
