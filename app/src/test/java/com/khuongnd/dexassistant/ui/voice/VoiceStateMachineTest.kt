package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceStateMachineTest {
    @Test fun `happy path returns to listening`() {
        val machine = VoiceStateMachine()
        listOf(VoiceEvent.START, VoiceEvent.WAKE, VoiceEvent.STT_STARTED, VoiceEvent.FINAL_TEXT,
            VoiceEvent.PLAN_RECEIVED, VoiceEvent.COMMAND_FINISHED).forEach(machine::accept)
        assertEquals(VoiceState.LISTENING, machine.state)
    }

    @Test(expected = IllegalStateException::class)
    fun `cannot transcribe before wake`() { VoiceStateMachine(VoiceState.LISTENING).accept(VoiceEvent.STT_STARTED) }

    @Test fun `pause resume and stop are explicit`() {
        val machine = VoiceStateMachine(VoiceState.LISTENING)
        assertEquals(VoiceState.PAUSED, machine.accept(VoiceEvent.PAUSE))
        assertEquals(VoiceState.LISTENING, machine.accept(VoiceEvent.RESUME))
        assertEquals(VoiceState.STOPPED, machine.accept(VoiceEvent.STOP))
    }

    @Test fun `missing model pause can later resume`() {
        val machine = VoiceStateMachine()
        machine.accept(VoiceEvent.START)
        machine.accept(VoiceEvent.PAUSE)
        assertEquals(VoiceState.LISTENING, machine.accept(VoiceEvent.RESUME))
    }
}
