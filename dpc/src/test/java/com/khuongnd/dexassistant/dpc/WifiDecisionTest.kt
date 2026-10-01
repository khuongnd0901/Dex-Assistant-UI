package com.khuongnd.dexassistant.dpc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WifiDecisionTest {
    @Test
    fun ownerAndPolicyFailuresAreDistinct() {
        assertEquals("DEVICE_OWNER_REQUIRED", WifiDecision.preflight(false, false).errorCode)
        assertEquals("POLICY_DENIED", WifiDecision.preflight(true, false).errorCode)
        assertNull(WifiDecision.preflight(true, true).errorCode)
    }
}
