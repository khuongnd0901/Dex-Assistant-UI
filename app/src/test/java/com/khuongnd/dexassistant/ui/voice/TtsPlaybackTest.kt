package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.*
import org.junit.Test

class TtsPlaybackTest {
    @Test fun codePointBoundedChatCanExceedTtsUtf16LimitWithoutLosingText() {
        val text = "😀".repeat(4000)
        val chunks = TtsPlayback.split(text, 3999)
        assertEquals(text, chunks.joinToString(""))
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 3999 && !it.last().isHighSurrogate() && !it.first().isLowSurrogate() })
    }

    @Test fun sentenceAndWhitespaceBoundariesPreserveVietnameseAndNewlines() {
        val text = "Xin chào. Đây là câu chuyện tiếng Việt!\nBạn nghe thấy không? "
        val chunks = TtsPlayback.split(text, 25)
        assertEquals(text, chunks.joinToString(""))
        assertTrue(chunks.all { it.length <= 25 })
        assertEquals(listOf(text), TtsPlayback.split(text, text.length))
    }

    @Test fun followUpCanOnlyBeginAfterEveryChunkCompletesOnce() {
        val sequence = TtsPlayback("abcdefghijkl", 4, "generation-1")
        val first = sequence.current!!
        assertTrue(sequence.start(first.id))
        assertFalse(sequence.start(first.id))
        assertFalse(sequence.complete("old-id"))
        assertTrue(sequence.complete(first.id))
        val second = sequence.current!!
        assertFalse(sequence.start(second.id)) // first audible start belongs to the whole reply
        assertFalse(sequence.complete(first.id))
        assertTrue(sequence.complete(second.id))
        val last = sequence.current!!
        assertTrue(sequence.complete(last.id))
        assertNull(sequence.current)
        assertFalse(sequence.complete(last.id))
    }

    @Test fun cancelAndReplacementRejectOldDoneAndStartEvenWithSamePublicId() {
        val old = TtsPlayback("abcdef", 3, "generation-1")
        val oldId = old.current!!.id
        old.cancel()
        val fresh = TtsPlayback("abcdef", 3, "generation-2")
        assertFalse(old.start(oldId))
        assertFalse(old.complete(oldId))
        assertFalse(fresh.complete(oldId))
        assertTrue(fresh.start(fresh.current!!.id))
    }
}
