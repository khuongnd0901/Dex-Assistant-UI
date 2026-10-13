package com.khuongnd.dexassistant.ui.latency

import org.junit.Assert.*
import org.junit.Test

class RequestLatencyTest {
    @Test fun directOriginSurvivesSemanticFallbackAndTerminalConsumesOutstandingStages() {
        val lines = mutableListOf<String>()
        val trace = RequestLatency("fixture", lines::add)
        trace.stage("admitted")
        trace.start("firstStatusLatency", 1_000)
        trace.end("firstStatusLatency", 101_000)
        trace.resolveOrigin("direct"); trace.path = "direct"
        trace.path = "semantic"; trace.resolveOrigin("local")
        trace.metric("providerBodyLatency", 25)
        trace.path = "fallback"; trace.terminal("CANCELLED")
        trace.end("firstStatusLatency", 200_000); trace.metric("providerParseLatency", 30)
        trace.terminal("FAILED")
        assertEquals(4, lines.size)
        assertFalse(lines.first().contains("origin="))
        assertTrue(lines[2].contains("path=semantic origin=direct"))
        assertTrue(lines.last().contains("path=fallback origin=direct terminalReason=CANCELLED"))
    }
    @Test fun missingEndpointsUnknownOriginAndUnsafeReasonNeverFabricateMetrics() {
        val lines = mutableListOf<String>()
        val trace = RequestLatency("fixture", lines::add)
        trace.end("finalToActionStartLatency", 20_000)
        trace.metric("providerRequestLatency", -1)
        trace.terminal("private text")
        assertEquals(listOf("requestId=fixture path=local terminalReason=OTHER"), lines)
    }
}
