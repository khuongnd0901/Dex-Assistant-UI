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

    @Test fun knownAppCloseIsImmediatelyUnsupportedWithoutBackend() {
        listOf("tắt YouTube", "đóng chrome", "Hãy đóng YouTube!").forEach {
            assertEquals(Route.UNSUPPORTED, parser.route(it).route)
            assertEquals(null, parser.route(it).intent)
        }
        listOf("đừng tắt youtube", "tắt youtube rồi mở chrome", "đóng ứng dụng lạ").forEach {
            assertEquals(Route.BACKEND_REQUIRED, parser.route(it).route)
        }
    }

    @Test fun ambiguityNeverExecutesLocally() {
        listOf("đừng bật wifi", "bật wifi và mở youtube", "đặt âm lượng", "mở nhạc phù hợp tâm trạng")
            .forEach { assertEquals(Route.BACKEND_REQUIRED, parser.route(it).route) }
        assertEquals(Route.UNSUPPORTED, parser.route("chia đôi màn hình").route)
        assertEquals(Route.UNSUPPORTED, parser.route("ket noi wifi nha").route)
    }

    @Test fun youtubeExtractsOriginalQuery() {
        assertEquals("lái xe!", parser.route("Nhạc lái xe!").arguments["query"])
    }

    @Test fun absoluteVolumeTargetsAcceptAdjacentPercentAndNaturalVerbs() {
        val cases = mapOf(
            "tăng âm lượng 100%" to "100", "Tăng âm lượng 100%!!!" to "100",
            "đặt âm lượng 50 %" to "50", "chỉnh âm lượng 0%" to "0",
            "điều chỉnh âm lượng về 45 phần trăm" to "45", "giảm âm lượng xuống 10%" to "10",
            "tăng âm lượng lên 80 phần trăm" to "80", "đặt âm lượng đến 75%" to "75",
        )
        cases.forEach { (text, target) ->
            val command = parser.route(text)
            assertEquals(text, Route.LOCAL, command.route)
            assertEquals(LocalIntent.VOLUME_SET, command.intent)
            assertEquals(mapOf("operation" to "SET", "percent" to target), command.arguments)
        }
        assertEquals("tăng âm lượng 100%", VietnameseNormalizer.normalize("“Tăng âm lượng 100%!”"))
    }

    @Test fun relativeVolumeStepsRemainDistinctFromAbsoluteTargets() {
        listOf("tăng" to "INCREASE", "giảm" to "DECREASE").forEach { (verb, operation) ->
            assertEquals(mapOf("operation" to operation, "steps" to "1"), parser.route("$verb âm lượng").arguments)
            assertEquals(mapOf("operation" to operation, "steps" to "10"), parser.route("$verb âm lượng 10 bước").arguments)
        }
        listOf("tăng âm lượng tối đa" to "100", "âm lượng tối thiểu" to "0").forEach { (text, target) ->
            assertEquals(mapOf("operation" to "SET", "percent" to target), parser.route(text).arguments)
        }
        assertEquals("MUTE", parser.route("tắt âm thanh").arguments["operation"])
        assertEquals("UNMUTE", parser.route("bỏ tắt tiếng").arguments["operation"])
    }

    @Test fun invalidAmbiguousAndCompoundVolumeCommandsDoNotExecuteLocally() {
        listOf(
            "tăng âm lượng 101%", "đặt âm lượng -1%", "đặt âm lượng +50%",
            "đặt âm lượng 50%%", "đặt âm lượng 50%+", "đặt âm lượng 1.5%",
            "đặt âm lượng 050%", "đặt âm lượng 50", "đặt âm lượng %",
            "tăng thêm 10%", "tăng âm lượng thêm 10%", "giảm âm lượng 11 bước",
            "đừng tăng âm lượng 100%", "không đặt âm lượng 50%",
            "tăng âm lượng 100% rồi mở youtube", "đặt âm lượng 50% và phát video",
            "giảm âm lượng 0 bước", "đặt độ sáng -1%", "chỉnh độ sáng 101%",
        ).forEach { text ->
            assertEquals(text, Route.BACKEND_REQUIRED, parser.route(text).route)
            assertEquals(text, null, parser.route(text).intent)
        }
        assertEquals(Route.UNSUPPORTED, parser.route("tìm video mật khẩu trên youtube").route)
        assertEquals(Route.UNSUPPORTED, parser.route("đặt âm lượng 50% token secret").route)
    }

    @Test fun appMediaBrightnessAndSearchSynonymsReuseExistingIntents() {
        listOf("bật youtube", "khởi động you tube", "mở google chrome").forEach {
            assertEquals(LocalIntent.APP_LAUNCH, parser.route(it).intent)
        }
        val media = mapOf("phát video" to "PLAY", "tiếp tục video" to "PLAY",
            "tạm dừng video" to "PAUSE", "tạm dừng" to "PAUSE", "dừng video" to "STOP",
            "video tiếp theo" to "NEXT", "quay lại bài trước" to "PREVIOUS", "video trước" to "PREVIOUS")
        media.forEach { (text, operation) ->
            assertEquals(LocalIntent.MEDIA_TRANSPORT, parser.route(text).intent)
            assertEquals(operation, parser.route(text).arguments["operation"])
        }
        assertEquals(mapOf("operation" to "SET", "percent" to "60"), parser.route("chỉnh độ sáng lên 60%").arguments)
        listOf("tìm kiếm nhạc sơn tùng", "tìm kiếm video mèo", "tìm sơn tùng trên youtube",
            "tìm kiếm hướng dẫn nấu ăn trên you tube").forEach { text ->
            assertEquals(LocalIntent.YOUTUBE_SEARCH, parser.route(text).intent)
            assertEquals(text.removePrefix("tìm kiếm nhạc ").removePrefix("tìm kiếm video ").removePrefix("tìm kiếm ").removePrefix("tìm ").removeSuffix(" trên youtube").removeSuffix(" trên you tube"), parser.route(text).arguments["query"])
        }
        listOf("tìm món ăn", "bật ứng dụng lạ", "đóng you tube", "phát video rồi mở chrome").forEach {
            assertTrue(parser.route(it).route != Route.LOCAL)
        }
    }

    @Test fun retentionUsesEarlierAgeOrCountLimit() {
        val day = 24L * 60 * 60 * 1000; val now = 10 * day
        val entries = (0..105).map { HistoryEntry(it.toString(), now - it * 1_000, "text$it", "LOCAL", null, null) } +
            HistoryEntry("old", now - 8 * day, "old", "LOCAL", null, null)
        val retained = HistoryRetention.retain(entries, now)
        assertEquals(100, retained.size); assertTrue(retained.none { it.id == "old" })
    }
}
