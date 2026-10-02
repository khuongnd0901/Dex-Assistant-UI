package com.khuongnd.dexassistant.ui

import com.khuongnd.dexassistant.ui.history.HistoryEntry
import com.khuongnd.dexassistant.ui.history.HistoryRetention
import com.khuongnd.dexassistant.ui.routing.LocalCommandParser
import com.khuongnd.dexassistant.ui.routing.LocalIntent
import com.khuongnd.dexassistant.ui.routing.Route
import com.khuongnd.dexassistant.ui.routing.VietnameseNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRoutingTest {
    private val parser = LocalCommandParser()

    @Test fun normalizesNfcCaseWhitespacePunctuationAndPoliteness() {
        assertEquals("bật wifi", VietnameseNormalizer.normalize("  VUI LÒNG   bật WiFi!!! "))
        assertEquals("mở youtube", VietnameseNormalizer.normalize("Hãy mở YouTube đi"))
    }

    @Test fun routesCanonicalLocalExamples() {
        val cases = mapOf("mở youtube" to LocalIntent.APP_LAUNCH, "bật wifi" to LocalIntent.WIFI_SET,
            "tắt bluetooth" to LocalIntent.BLUETOOTH_SET, "giảm âm lượng 2 bước" to LocalIntent.VOLUME_SET,
            "đặt độ sáng 60 phần trăm" to LocalIntent.BRIGHTNESS_SET, "bài tiếp theo" to LocalIntent.MEDIA_TRANSPORT,
            "nhạc lái xe" to LocalIntent.YOUTUBE_SEARCH)
        cases.forEach { (text, intent) -> assertEquals(intent, parser.route(text).intent) }
    }

    @Test fun ambiguityNeverExecutesLocally() {
        listOf("đừng bật wifi", "bật wifi và mở youtube", "đặt âm lượng", "mở nhạc phù hợp tâm trạng")
            .forEach { assertEquals(Route.BACKEND_REQUIRED, parser.route(it).route) }
        assertEquals(Route.UNSUPPORTED, parser.route("chia đôi màn hình").route)
        assertEquals(Route.BACKEND_REQUIRED, parser.route("ket noi wifi nha").route)
    }

    @Test fun youtubeRetainsNormalizedPhrase() {
        assertEquals("nhạc lái xe", parser.route("Nhạc lái xe!").arguments["query"])
    }

    @Test fun retentionUsesEarlierAgeOrCountLimit() {
        val day = 24L * 60 * 60 * 1000; val now = 10 * day
        val entries = (0..105).map { HistoryEntry(it.toString(), now - it * 1_000, "text$it", "LOCAL", null, null) } +
            HistoryEntry("old", now - 8 * day, "old", "LOCAL", null, null)
        val retained = HistoryRetention.retain(entries, now)
        assertEquals(100, retained.size); assertTrue(retained.none { it.id == "old" })
    }
}
