package com.khuongnd.dexassistant.ui

import org.junit.Assert.*
import org.junit.Test

class CommandSessionGateTest {
    @Test fun cancelBeforePostedDispatchPreventsAdmission() {
        val gate = CommandSessionGate()
        gate.enqueue("queued")
        gate.cancelQueued("queued")
        assertNull(gate.beginQueued("queued"))
        assertNull(gate.currentId)
        gate.enqueue("new")
        assertEquals(true, gate.beginQueued("new"))
    }

    @Test fun oneCommandAtATimeAndCompletionOnlyOnce() {
        val gate = CommandSessionGate()
        assertTrue(gate.begin("first"))
        assertFalse(gate.begin("second"))
        assertEquals("first", gate.currentId)
        assertTrue(gate.finish("first"))
        assertFalse(gate.finish("first"))
        assertTrue(gate.begin("second"))
    }

    @Test fun lateCallbackAfterCancellationCannotCompleteNewCommand() {
        val gate = CommandSessionGate()
        gate.begin("cancelled")
        gate.finish("cancelled")
        gate.begin("new")
        assertFalse(gate.finish("cancelled"))
        assertEquals("new", gate.currentId)
        assertTrue(gate.finish("new"))
    }
}
