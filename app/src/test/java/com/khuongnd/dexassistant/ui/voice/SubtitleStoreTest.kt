package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.*
import org.junit.Test

class SubtitleStoreTest {
    @Test fun phasesRevisionsAndDeadline() {
        val store = SubtitleStore(); store.begin(1)
        assertEquals(SubtitlePhase.PREPARING, store.state.value!!.phase)
        store.phase(1, SubtitlePhase.LISTENING)
        store.text(1, "mở bản đồ", false, 0)
        store.text(1, "", false, 0)
        assertEquals("mở bản đồ", store.state.value!!.text)
        store.phase(1, SubtitlePhase.RECOGNIZING)
        store.text(1, "mở bản đồ Việt Nam", false, 20)
        assertEquals(SubtitlePhase.RECOGNIZING, store.state.value!!.phase)
        store.text(1, "tìm Phà Cát Lái", true, 100)
        store.text(1, "late", false, 101)
        store.phase(1, SubtitlePhase.LISTENING)
        assertEquals("tìm Phà Cát Lái", store.state.value!!.text)
        store.expire(3099); assertNotNull(store.state.value)
        store.expire(3100); assertNull(store.state.value)
    }
    @Test fun staleEventsAndTimersCannotClearNewTurn() {
        val store = SubtitleStore(); store.begin(1); store.text(1, "old", true, 0)
        store.begin(2); store.text(1, "old partial", false, 1); store.clear(1); store.expire(4000)
        assertEquals(2L, store.state.value!!.turn)
        store.clear(); store.text(2, "late", true, 4000); assertNull(store.state.value)
    }
    @Test fun redactionAndLongVietnameseTail() {
        val store = SubtitleStore(); store.begin(1)
        store.text(1, "mật khẩu secret", false, 0); assertEquals("[REDACTED]", store.state.value!!.text)
        val phrase = "tìm đường đến Phà Cát Lái ".repeat(100)
        store.text(1, phrase, false, 0); assertEquals(phrase.takeLast(512), store.state.value!!.text)
        store.text(1, "", true, 0); assertNull(store.state.value)
    }
    @Test fun sessionIdentitySurvivesServiceGuardRecreation() {
        val first = VoiceSessionGuard().begin(); val second = VoiceSessionGuard().begin()
        assertTrue(second > first)
    }
}
