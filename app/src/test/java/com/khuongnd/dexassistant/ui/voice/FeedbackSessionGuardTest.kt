package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.*
import org.junit.Test

class FeedbackSessionGuardTest {
    @Test fun staleTtsCompletionCannotReleaseReplacementPrompt() {
        val guard = FeedbackSessionGuard()
        val old = guard.begin()
        val current = guard.begin()
        assertFalse(guard.consume(old))
        assertTrue(guard.isCurrent(current))
        assertTrue(guard.consume(current))
        assertFalse(guard.consume(current)) // timeout racing onDone
    }
    @Test fun cancellationAndReentrantPlaybackInvalidateOldCallbacks() {
        val guard = FeedbackSessionGuard()
        val old = guard.begin()
        guard.cancel()
        assertFalse(guard.consume(old))
        val completed = guard.begin()
        assertTrue(guard.consume(completed))
        val reentrant = guard.begin()
        assertFalse(guard.consume(completed))
        assertTrue(guard.consume(reentrant))
    }
}
