package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceInteractionLifecycleTest {
    @Test fun selectionAndConfirmationUseFreshRecognizerEpochsAndKeepParentExecution() {
        val state = VoiceStateMachine()
        val sessions = VoiceSessionGuard()
        state.accept(VoiceEvent.START); state.accept(VoiceEvent.WAKE); state.accept(VoiceEvent.STT_STARTED)
        val command = sessions.begin()
        assertTrue(sessions.consumeFinal(command)); assertFalse(sessions.consumeFinal(command))
        state.accept(VoiceEvent.FINAL_TEXT); state.accept(VoiceEvent.PLAN_RECEIVED); state.accept(VoiceEvent.SPEAK)
        state.accept(VoiceEvent.INTERACTION_LISTEN)
        val selection = sessions.begin()
        assertFalse(sessions.isCurrent(command)); assertFalse(sessions.consumeFinal(command))
        state.accept(VoiceEvent.STT_STARTED); assertTrue(sessions.consumeFinal(selection)); state.accept(VoiceEvent.FINAL_TEXT)
        state.accept(VoiceEvent.SPEAK); state.accept(VoiceEvent.INTERACTION_LISTEN)
        val confirmation = sessions.begin()
        assertFalse(sessions.consumeFinal(selection))
        state.accept(VoiceEvent.STT_STARTED); assertTrue(sessions.consumeFinal(confirmation)); state.accept(VoiceEvent.FINAL_TEXT)
        state.accept(VoiceEvent.SPEAK); state.accept(VoiceEvent.FEEDBACK_FINISHED)
        assertEquals(VoiceState.LISTENING,state.state)
    }
    @Test fun pauseStopAndTtsFailureNeverOpenAContinuation() {
        for (event in listOf(VoiceEvent.PAUSE, VoiceEvent.STOP, VoiceEvent.FAILURE)) {
            val state = VoiceStateMachine(VoiceState.SPEAKING)
            val sessions = VoiceSessionGuard(); val id = sessions.begin()
            sessions.cancel(); state.accept(event)
            assertFalse(sessions.isCurrent(id))
            assertTrue(runCatching { state.accept(VoiceEvent.INTERACTION_LISTEN) }.isFailure)
        }
    }
}
