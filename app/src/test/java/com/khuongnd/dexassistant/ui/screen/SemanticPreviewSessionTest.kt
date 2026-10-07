package com.khuongnd.dexassistant.ui.screen

import org.junit.Assert.*
import org.junit.Test

class SemanticPreviewSessionTest {
    @Test fun frozenOrderingSurvivesReadsButConsumeIsOnce() {
        val session = SemanticPreviewSession<String>()
        assertEquals(SemanticPreviewSession.Selection.Missing, session.consume(0) { true })
        val values = mutableListOf("first", "second")
        session.publish(values, 100)
        values.reverse()
        assertEquals(listOf("first", "second"), session.current(34_000) { it == listOf("first", "second") })
        assertEquals(SemanticPreviewSession.Selection.Ready(listOf("first", "second")), session.consume(35_099) { true })
        assertEquals(SemanticPreviewSession.Selection.Stale, session.consume(35_099) { true })
    }
    @Test fun expiryAndIdentityChangeRejectWithoutRebuildingAndReadsNeverExtendExpiry() {
        val session = SemanticPreviewSession<String>()
        session.publish(listOf("A"), 10)
        repeat(100) { assertNotNull(session.current(35_009) { true }) }
        assertEquals(SemanticPreviewSession.Selection.Stale, session.consume(35_010) { true })
        session.publish(listOf("B"), 40_000)
        assertNull(session.current(40_001) { false })
        assertEquals(SemanticPreviewSession.Selection.Stale, session.consume(40_002) { true })
        session.reset()
        assertFalse(session.hasHistory)
        assertEquals(SemanticPreviewSession.Selection.Missing, session.consume(40_003) { true })
    }
}
