package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantReadinessTest {
    private val ready = AssistantReadinessInput(true, true, true, true, true, true)

    @Test fun `all readiness gates allow start only once`() {
        val coordinator = AssistantStartCoordinator()
        val result = AssistantReadinessEvaluator.evaluate(ready)
        assertEquals(AssistantReadiness.Ready, result)
        assertTrue(coordinator.shouldStart(result))
        assertFalse(coordinator.shouldStart(result))
    }

    @Test fun `locked user blocks start`() = assertBlocked(ready.copy(userUnlocked = false), "USER_LOCKED")
    @Test fun `missing role blocks start`() = assertBlocked(ready.copy(assistantRoleHeld = false), "ASSISTANT_ROLE_NOT_HELD")
    @Test fun `missing microphone permission blocks start`() = assertBlocked(ready.copy(recordAudioGranted = false), "RECORD_AUDIO_DENIED")
    @Test fun `missing foreground permission blocks start`() = assertBlocked(ready.copy(foregroundServiceGranted = false), "FOREGROUND_SERVICE_DENIED")
    @Test fun `disabled policy blocks start`() = assertBlocked(ready.copy(autoStartEnabled = false), "AUTO_START_DISABLED")
    @Test fun `disabled wake word blocks start`() = assertBlocked(ready.copy(wakeWordEnabled = false), "WAKE_WORD_DISABLED")

    private fun assertBlocked(input: AssistantReadinessInput, reason: String) {
        assertEquals(AssistantReadiness.Blocked(reason), AssistantReadinessEvaluator.evaluate(input))
        assertFalse(AssistantStartCoordinator().shouldStart(AssistantReadinessEvaluator.evaluate(input)))
    }
}
