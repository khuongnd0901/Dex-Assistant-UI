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

    @Test fun `backend and local routing both wait for feedback before listening`() {
        for (backend in listOf(false, true)) {
            val machine = VoiceStateMachine(VoiceState.TRANSCRIBING)
            assertEquals(VoiceState.ROUTING, machine.accept(VoiceEvent.FINAL_TEXT))
            if (backend) assertEquals(VoiceState.SENDING, machine.accept(VoiceEvent.BACKEND_SENT))
            assertEquals(VoiceState.EXECUTING, machine.accept(VoiceEvent.PLAN_RECEIVED))
            assertEquals(VoiceState.SPEAKING, machine.accept(VoiceEvent.SPEAK))
            assertEquals(VoiceState.LISTENING, machine.accept(VoiceEvent.FEEDBACK_FINISHED))
        }
    }

    @Test fun `recognition failure can give feedback without dispatching`() {
        val machine = VoiceStateMachine(VoiceState.TRANSCRIBING)
        assertEquals(VoiceState.SPEAKING, machine.accept(VoiceEvent.SPEAK))
        assertEquals(VoiceState.LISTENING, machine.accept(VoiceEvent.FEEDBACK_FINISHED))
    }

    @Test fun `pause and stop terminate recognition execution and feedback phases`() {
        for (phase in listOf(VoiceState.TRANSCRIBING, VoiceState.ROUTING, VoiceState.SENDING,
            VoiceState.EXECUTING, VoiceState.SPEAKING)) {
            val paused = VoiceStateMachine(phase)
            assertEquals(VoiceState.PAUSED, paused.accept(VoiceEvent.PAUSE))
            assertEquals(VoiceState.LISTENING, paused.accept(VoiceEvent.RESUME))
            val stopped = VoiceStateMachine(phase)
            assertEquals(VoiceState.STOPPED, stopped.accept(VoiceEvent.STOP))
        }
    }
}
