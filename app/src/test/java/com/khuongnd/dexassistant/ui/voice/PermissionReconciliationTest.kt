package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.*
import org.junit.Test

class PermissionReconciliationTest {
    @Test fun `startup waits for asynchronous owned profile result`() {
        val gate = PermissionReconciliation()
        assertTrue(gate.begin())
        assertFalse(gate.complete)
        assertFalse(gate.begin())
        assertTrue(gate.finish("SUCCESS", true))
        assertTrue(gate.complete)
        assertFalse(gate.begin())
    }

    @Test fun `missing owner and IPC failures never open gate and exhaust bounded retries`() {
        val gate = PermissionReconciliation()
        assertTrue(gate.begin())
        assertFalse(gate.finish("SUCCESS", false))
        assertTrue(gate.canRetry())
        assertTrue(gate.begin())
        assertFalse(gate.finish("FAILED", true))
        assertTrue(gate.begin())
        assertFalse(gate.finish("REJECTED", false))
        assertFalse(gate.canRetry())
        assertFalse(gate.begin())
        assertFalse(gate.complete)
    }
}
