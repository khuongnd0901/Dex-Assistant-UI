package com.khuongnd.dexassistant.ui.voice

enum class VoiceState { STOPPED, LISTENING, WAKE_DETECTED, TRANSCRIBING, ROUTING, SENDING, EXECUTING, SPEAKING, PAUSED, ERROR }

enum class VoiceEvent { START, WAKE, STT_STARTED, FINAL_TEXT, BACKEND_SENT, PLAN_RECEIVED, COMMAND_FINISHED, SPEAK, FEEDBACK_FINISHED, INTERACTION_LISTEN, FAILURE, PAUSE, RESUME, STOP }

class VoiceStateMachine(initial: VoiceState = VoiceState.STOPPED) {
    var state: VoiceState = initial
        private set

    fun accept(event: VoiceEvent): VoiceState {
        state = when (event) {
            VoiceEvent.START -> if (state == VoiceState.STOPPED || state == VoiceState.ERROR) VoiceState.LISTENING else state
            VoiceEvent.WAKE -> requireState(VoiceState.LISTENING, VoiceState.WAKE_DETECTED)
            VoiceEvent.STT_STARTED -> requireState(VoiceState.WAKE_DETECTED, VoiceState.TRANSCRIBING)
            VoiceEvent.FINAL_TEXT -> requireState(VoiceState.TRANSCRIBING, VoiceState.ROUTING)
            VoiceEvent.BACKEND_SENT -> requireState(VoiceState.ROUTING, VoiceState.SENDING)
            VoiceEvent.PLAN_RECEIVED -> if (state in setOf(VoiceState.ROUTING, VoiceState.SENDING)) VoiceState.EXECUTING else invalid(event)
            VoiceEvent.COMMAND_FINISHED -> if (state in setOf(VoiceState.ROUTING, VoiceState.SENDING, VoiceState.EXECUTING)) VoiceState.LISTENING else invalid(event)
            VoiceEvent.SPEAK -> if (state in setOf(VoiceState.TRANSCRIBING, VoiceState.ROUTING, VoiceState.SENDING, VoiceState.EXECUTING, VoiceState.SPEAKING)) VoiceState.SPEAKING else invalid(event)
            VoiceEvent.INTERACTION_LISTEN -> requireState(VoiceState.SPEAKING, VoiceState.WAKE_DETECTED)
            VoiceEvent.FEEDBACK_FINISHED -> requireState(VoiceState.SPEAKING, VoiceState.LISTENING)
            VoiceEvent.FAILURE -> VoiceState.ERROR
            VoiceEvent.PAUSE -> if (state != VoiceState.STOPPED) VoiceState.PAUSED else invalid(event)
            VoiceEvent.RESUME -> requireState(VoiceState.PAUSED, VoiceState.LISTENING)
            VoiceEvent.STOP -> VoiceState.STOPPED
        }
        return state
    }

    private fun requireState(required: VoiceState, next: VoiceState): VoiceState =
        if (state == required) next else invalid(null)

    private fun invalid(event: VoiceEvent?): Nothing =
        throw IllegalStateException("Invalid voice transition: $state / ${event ?: "event"}")
}
