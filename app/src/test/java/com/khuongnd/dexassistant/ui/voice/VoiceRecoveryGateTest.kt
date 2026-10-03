package com.khuongnd.dexassistant.ui.voice
import org.junit.Assert.*
import org.junit.Test
class VoiceRecoveryGateTest {
    @Test fun permissionSuppressionAndInflightStatesNeverAdmit() {
        val gate = VoiceRecoveryGate()
        assertFalse(gate.admit(0,false,null))
        for (state in VoiceState.entries.filter { it != VoiceState.ERROR }) assertFalse(gate.admit(0,true,state))
        assertTrue(VoiceRecoveryGate().admit(0,true,VoiceState.PAUSED,true))
        assertFalse(VoiceRecoveryGate().admit(0,false,VoiceState.PAUSED,true))
        assertTrue(gate.admit(0,true,null)); assertFalse(gate.admit(0,true,null)); assertFalse(gate.admit(9999,true,VoiceState.ERROR))
    }
    @Test fun retriesAreBoundedAndUserResetAllowsFreshStart() {
        val gate = VoiceRecoveryGate()
        for (time in listOf(0L,10000L,25000L,55000L)) assertTrue(gate.admit(time,true,VoiceState.ERROR))
        assertFalse(gate.admit(354_999,true,VoiceState.ERROR))
        assertFalse(gate.admit(355_000,true,VoiceState.TRANSCRIBING))
        assertFalse(gate.admit(355_000,false,VoiceState.ERROR))
        assertTrue(gate.admit(355_000,true,VoiceState.ERROR))
        assertFalse(gate.admit(355_001,true,VoiceState.ERROR))
        gate.reset(); assertTrue(gate.admit(1_000_000,true,null))
    }
}
