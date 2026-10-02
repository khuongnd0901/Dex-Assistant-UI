package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceSessionGuardTest {
    @Test fun `late callbacks cannot revive a paused stopped or replaced session`() {
        val guard = VoiceSessionGuard()
        val old = guard.begin()
        guard.cancel()
        assertFalse(guard.isCurrent(old))
        assertFalse(guard.consumeFinal(old))
        val resumed = guard.begin()
        assertFalse(guard.isCurrent(old))
        assertTrue(guard.isCurrent(resumed))
    }
    @Test fun `duplicate final results dispatch once while partial does not consume final`() {
        val guard = VoiceSessionGuard()
        val id = guard.begin()
        assertTrue(guard.isCurrent(id)) // partial results do not dispatch
        assertTrue(guard.consumeFinal(id))
        assertFalse(guard.consumeFinal(id))
        assertTrue(guard.consumeFinal(guard.begin()))
    }
}
