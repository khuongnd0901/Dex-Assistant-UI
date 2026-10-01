package com.khuongnd.dexassistant.ui.voice

data class AssistantReadinessInput(
    val assistantRoleHeld: Boolean,
    val userUnlocked: Boolean,
    val recordAudioGranted: Boolean,
    val foregroundServiceGranted: Boolean,
    val autoStartEnabled: Boolean,
    val wakeWordEnabled: Boolean,
)

sealed interface AssistantReadiness {
    data object Ready : AssistantReadiness
    data class Blocked(val reason: String) : AssistantReadiness
}

object AssistantReadinessEvaluator {
    fun evaluate(input: AssistantReadinessInput): AssistantReadiness = when {
        !input.assistantRoleHeld -> AssistantReadiness.Blocked("ASSISTANT_ROLE_NOT_HELD")
        !input.userUnlocked -> AssistantReadiness.Blocked("USER_LOCKED")
        !input.recordAudioGranted -> AssistantReadiness.Blocked("RECORD_AUDIO_DENIED")
        !input.foregroundServiceGranted -> AssistantReadiness.Blocked("FOREGROUND_SERVICE_DENIED")
        !input.autoStartEnabled -> AssistantReadiness.Blocked("AUTO_START_DISABLED")
        !input.wakeWordEnabled -> AssistantReadiness.Blocked("WAKE_WORD_DISABLED")
        else -> AssistantReadiness.Ready
    }
}

class AssistantStartCoordinator {
    private var startRequested = false

    fun shouldStart(readiness: AssistantReadiness): Boolean {
        if (readiness !is AssistantReadiness.Ready || startRequested) return false
        startRequested = true
        return true
    }

    fun serviceStopped() { startRequested = false }
}
