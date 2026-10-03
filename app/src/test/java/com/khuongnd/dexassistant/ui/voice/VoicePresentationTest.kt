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
