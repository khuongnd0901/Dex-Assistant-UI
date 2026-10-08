package com.khuongnd.dexassistant.ui

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Pure byte-format checks: no mic, fixture, routing or files. */
class SherpaCorpusPcmTest {
    @Test fun rejectsSilentShortOddAndOversizedTakes() {
        assertFalse(SherpaCorpusPcm.usable(ByteArray(16000)))
        assertFalse(SherpaCorpusPcm.usable(ByteArray(15998)))
        assertFalse(SherpaCorpusPcm.usable(ByteArray(16001)))
        assertFalse(SherpaCorpusPcm.usable(ByteArray(480002)))
    }
    @Test fun writesCorrectPcm16Mono16kWaveHeaderAndSamples() {
        val pcm = ByteBuffer.allocate(16000).order(ByteOrder.LITTLE_ENDIAN)
        repeat(8000) { pcm.putShort(if (it % 2 == 0) 1000 else -1000) }
        assertTrue(SherpaCorpusPcm.usable(pcm.array()))
        val wav = SherpaCorpusPcm.wave(pcm.array())
        val header = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals(16036, header.getInt(4))
        assertEquals(1, header.getShort(20).toInt())
        assertEquals(1, header.getShort(22).toInt())
        assertEquals(16000, header.getInt(24))
        assertEquals(16, header.getShort(34).toInt())
        assertEquals(16000, header.getInt(40))
        assertArrayEquals(pcm.array(), wav.copyOfRange(44, wav.size))
    }
}
