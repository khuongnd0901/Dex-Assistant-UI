package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.*
import org.junit.Test

class MicrophoneCandidatesTest {
    @Test fun rejectedDefaultFallsBackToOnlyUsableBuiltIn() {
        assertEquals(1, nextMicrophoneCandidate(listOf(1, 2), setOf(2), 2))
    }
    @Test fun unpluggedActualFallsBackAndDoesNotSelectRemovedDevice() {
        assertEquals(1, nextMicrophoneCandidate(listOf(1, 3), emptySet(), 2))
    }
    @Test fun soleCurrentAndAllRejectedDoNotLoopRequests() {
        assertNull(nextMicrophoneCandidate(listOf(1, 2), setOf(2), 1))
        assertNull(nextMicrophoneCandidate(listOf(1, 2), setOf(1, 2), 2))
    }
    @Test fun cyclesStableOrderSkippingRejected() {
        assertEquals(3, nextMicrophoneCandidate(listOf(1, 2, 3), setOf(2), 1))
        assertEquals(1, nextMicrophoneCandidate(listOf(1, 2, 3), setOf(2), 3))
    }
}
