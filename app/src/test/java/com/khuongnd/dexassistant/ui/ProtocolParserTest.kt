package com.khuongnd.dexassistant.ui

import com.khuongnd.dexassistant.ui.protocol.AssistantWebSocket
import com.khuongnd.dexassistant.ui.protocol.ProtocolParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolParserTest {
    @Test
    fun parsesRemoteToggleAction() {
        val actions = ProtocolParser.actions(
            """{"version":1,"type":"execution.plan","payload":{"planId":"p1","actions":[{"actionId":"a1","tool":"TOGGLE_WIFI","executionMode":"REMOTE","arguments":{"enabled":true}}]}}""",
        )

        assertEquals(1, actions.size)
        assertEquals("TOGGLE_WIFI", actions.single().tool)
        assertTrue(actions.single().arguments.getBoolean("enabled"))
    }

    @Test
    fun cleartextIsRestrictedToLoopback() {
        assertTrue(AssistantWebSocket.isLoopback("ws://127.0.0.1:8080/ws/v1/assistant"))
        assertTrue(AssistantWebSocket.isLoopback("ws://localhost:8080/ws/v1/assistant"))
        assertFalse(AssistantWebSocket.isLoopback("ws://192.168.1.10:8080/ws/v1/assistant"))
    }
}
