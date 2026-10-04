package com.khuongnd.dexassistant.ui.routing

import java.text.Normalizer
import java.util.Locale

const val ROUTING_VERSION = "vi-local-v1"

enum class Route { LOCAL, BACKEND_REQUIRED, UNSUPPORTED }
enum class LocalIntent { APP_LAUNCH, WIFI_SET, BLUETOOTH_SET, VOLUME_SET, BRIGHTNESS_SET, MEDIA_TRANSPORT, YOUTUBE_SEARCH }

data class RoutedCommand(
    val route: Route,
    val normalizedText: String,
    val intent: LocalIntent? = null,
    val arguments: Map<String, String> = emptyMap(),
)

object VietnameseNormalizer {
    private val surroundingPunctuation = Regex("^[\\p{P}\\p{S}]+|[\\p{P}\\p{S}]+$")
    private val whitespace = Regex("\\s+")
    private val prefixes = listOf("vui lòng ", "hãy ", "giúp tôi ")
    private val suffixes = listOf(" giúp tôi", " đi")

    fun normalize(value: String): String {
        var result = Normalizer.normalize(value, Normalizer.Form.NFC)
            .lowercase(Locale.forLanguageTag("vi-VN"))
            .trim().replace(whitespace, " ").replace(surroundingPunctuation, "").trim()
        prefixes.firstOrNull(result::startsWith)?.let { result = result.removePrefix(it).trim() }
        suffixes.firstOrNull(result::endsWith)?.let { result = result.removeSuffix(it).trim() }
        return result
    }
}

class LocalCommandParser(private val appAliases: Set<String> = DEFAULT_APP_ALIASES.keys) {
    fun route(original: String): RoutedCommand {
        val text = VietnameseNormalizer.normalize(original)
        if (CommandPrivacy.requiresLocalHandling(original)) return RoutedCommand(Route.UNSUPPORTED, text)
        if (text.isBlank()) return backend(text)
        if (NEGATION.containsMatchIn(text) || MULTI_INTENT.containsMatchIn(text)) return backend(text)

        Regex("^(?:tắt|đóng) (.+)$").matchEntire(text)?.let { match ->
            if (match.groupValues[1] in DEFAULT_APP_ALIASES) return RoutedCommand(Route.UNSUPPORTED, text)
        }
        Regex("^(?:mở|khởi động) (.+)$").matchEntire(text)?.let { match ->
            val alias = match.groupValues[1]
            if (alias in appAliases) return local(text, LocalIntent.APP_LAUNCH, "alias" to alias)
        }
        Regex("^(bật|mở|tắt) (?:wi-fi|wifi)$").matchEntire(text)?.let {
            return local(text, LocalIntent.WIFI_SET, "enabled" to (it.groupValues[1] != "tắt").toString())
        }
        Regex("^(bật|mở|tắt) bluetooth$").matchEntire(text)?.let {
            return local(text, LocalIntent.BLUETOOTH_SET, "enabled" to (it.groupValues[1] != "tắt").toString())
        }
        if (text == "tắt tiếng" || text == "bật tiếng") {
            return local(text, LocalIntent.VOLUME_SET, "operation" to if (text == "tắt tiếng") "MUTE" else "UNMUTE")
        }
        Regex("^(tăng|giảm) âm lượng(?: ([1-9]|10) bước)?$").matchEntire(text)?.let {
            return local(text, LocalIntent.VOLUME_SET, "operation" to if (it.groupValues[1] == "tăng") "INCREASE" else "DECREASE",
                "steps" to (it.groupValues[2].ifBlank { "1" }))
        }
        Regex("^đặt âm lượng (100|[1-9]?[0-9]) (?:phần trăm|%)$").matchEntire(text)?.let {
            return local(text, LocalIntent.VOLUME_SET, "operation" to "SET", "percent" to it.groupValues[1])
        }
        Regex("^(tăng|giảm) độ sáng$").matchEntire(text)?.let {
            return local(text, LocalIntent.BRIGHTNESS_SET, "operation" to if (it.groupValues[1] == "tăng") "INCREASE" else "DECREASE")
        }
        Regex("^đặt độ sáng (100|[1-9]?[0-9]) (?:phần trăm|%)$").matchEntire(text)?.let {
            return local(text, LocalIntent.BRIGHTNESS_SET, "operation" to "SET", "percent" to it.groupValues[1])
        }
        Regex("^(bật|tắt) độ sáng tự động$").matchEntire(text)?.let {
            return local(text, LocalIntent.BRIGHTNESS_SET, "operation" to if (it.groupValues[1] == "bật") "AUTO_ON" else "AUTO_OFF")
        }
        MEDIA[text]?.let { return local(text, LocalIntent.MEDIA_TRANSPORT, "operation" to it) }
        Regex("^(?:nhạc .+|tìm (?:nhạc|video) .+)$").matchEntire(text)?.let {
            return local(text, LocalIntent.YOUTUBE_SEARCH, "query" to text)
        }
        if (Regex("^kết nối (?:wi-fi|wifi)(?: .+)?$").matches(text)) return backend(text)
        if (Regex("^(?:chia đôi màn hình|bấm .+|nhập .+)$").matches(text)) return RoutedCommand(Route.UNSUPPORTED, text)
        return backend(text)
    }

    private fun local(text: String, intent: LocalIntent, vararg args: Pair<String, String>) =
        RoutedCommand(Route.LOCAL, text, intent, mapOf(*args))
    private fun backend(text: String) = RoutedCommand(Route.BACKEND_REQUIRED, text)

    companion object {
        val DEFAULT_APP_ALIASES = mapOf("youtube" to "com.google.android.youtube", "chrome" to "com.android.chrome")
        private val NEGATION = Regex("^(?:đừng|không|chớ)\\b")
        private val MULTI_INTENT = Regex("\\s(?:và|rồi|sau đó)\\s")
        private val MEDIA = mapOf("phát nhạc" to "PLAY", "tiếp tục nhạc" to "PLAY", "tạm dừng nhạc" to "PAUSE",
            "bài tiếp theo" to "NEXT", "bài trước" to "PREVIOUS", "dừng nhạc" to "STOP")
    }
}
