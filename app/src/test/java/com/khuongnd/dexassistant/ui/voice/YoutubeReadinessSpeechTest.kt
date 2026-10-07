package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.*
import org.junit.Test

class YoutubeReadinessSpeechTest {
    @Test fun previewReadinessIsDistinctFromIntentDeliveryAndPlayback() {
        assertEquals("Đã gửi yêu cầu.", VoiceResultSpeech.message("DISPATCHED"))
        for (code in listOf("YOUTUBE_INDEX_READY", "YOUTUBE_RESULTS_READY")) {
            val message = VoiceResultSpeech.message(code)
            assertTrue(message.contains("chuẩn bị số"))
            assertTrue(message.contains("xác nhận"))
            assertFalse(message.contains("đang phát", ignoreCase = true))
            assertNotEquals(VoiceResultSpeech.message("DISPATCHED"), message)
        }
        assertTrue(VoiceResultSpeech.message("YOUTUBE_SEARCH_UNVERIFIED").contains("Chưa xác minh"))
    }
}
