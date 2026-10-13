package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.*
import org.junit.Test

class WakePcmBufferTest {
    @Test fun sameSizeFramesReuseMemoryAndReplaceEverySampleExactly() {
        val pcm = WakePcmBuffer()
        val frame = shortArrayOf(Short.MIN_VALUE, 0, Short.MAX_VALUE)
        val first = pcm.convert(frame, frame.size)
        assertArrayEquals(floatArrayOf(-1f, 0f, 32767 / 32768f), first, 0f)
        val second = pcm.convert(shortArrayOf(0, 3, -4), 3)
        assertSame(first, second)
        assertArrayEquals(floatArrayOf(0f, 3 / 32768f, -4 / 32768f), second, 0f)
    }
    @Test fun shortReadsCannotPassStaleTailToNativeAndCleanupErasesRetainedAudio() {
        val pcm = WakePcmBuffer()
        val frame = pcm.convert(shortArrayOf(1, 2, 3), 3)
        val short = pcm.convert(shortArrayOf(7, 8, 9), 1)
        assertEquals(1, short.size); assertNotSame(frame, short)
        pcm.clear()
        assertArrayEquals(floatArrayOf(0f), short, 0f)
        assertNotSame(short, pcm.convert(shortArrayOf(7), 1))
        listOf(0, 2, -1).forEach { count ->
            try { pcm.convert(shortArrayOf(1), count); fail("Invalid count") } catch (_: IllegalArgumentException) { }
        }
    }
}
