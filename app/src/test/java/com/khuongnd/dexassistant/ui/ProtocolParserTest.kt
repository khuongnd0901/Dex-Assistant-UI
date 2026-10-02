package com.khuongnd.dexassistant.ui

import com.khuongnd.dexassistant.ui.protocol.AssistantWebSocket
import com.khuongnd.dexassistant.ui.protocol.ProtocolParser
import com.khuongnd.dexassistant.ui.protocol.QuotaFreshness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

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

    @Test fun parsesCanonicalKnownQuotaFixture() {
        val snapshot = ProtocolParser.quotaSnapshot(fixture("quota-snapshot-known.json"))
        val provider = snapshot.providers.single(); val budget = provider.budgets.single()
        assertEquals("gemini-primary", provider.providerAlias); assertEquals("AVAILABLE", provider.state)
        assertEquals(4L, budget.used); assertEquals(1L, budget.reserved); assertEquals(100L, budget.limit)
        assertEquals(95L, budget.remaining); assertEquals("REQUEST", budget.unit); assertEquals("ESTIMATED", budget.source)
    }

    @Test fun canonicalUnknownQuotaDoesNotFabricateZero() {
        val budget = ProtocolParser.quotaSnapshot(fixture("quota-snapshot-unknown.json")).providers.single().budgets.single()
        assertEquals(null, budget.used); assertEquals(null, budget.reserved); assertEquals(null, budget.limit)
        assertEquals(null, budget.remaining); assertEquals(null, budget.resetAt); assertEquals(null, budget.observedAt)
        assertEquals(null, budget.windowStart)
        assertEquals("TOTAL_TOKEN", budget.unit); assertEquals("PROVIDER_REPORTED", budget.source)
    }

    @Test fun parsesCanonicalAiQuotaError() {
        assertEquals("AI_QUOTA_EXHAUSTED", ProtocolParser.errorCode(fixture("ai-quota-exhausted.json")))
    }

    @Test fun snapshotIsStaleAtBoundary() {
        assertTrue(QuotaFreshness.isStale("2026-10-02T00:05:01Z", Instant.parse("2026-10-02T00:05:01Z")))
        assertFalse(QuotaFreshness.isStale("2026-10-02T00:05:01Z", Instant.parse("2026-10-02T00:05:00Z")))
    }

    private fun fixture(name: String) = checkNotNull(javaClass.classLoader?.getResource(name)).readText()
}
