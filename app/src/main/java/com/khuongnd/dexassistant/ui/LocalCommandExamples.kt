package com.khuongnd.dexassistant.ui

import android.content.Context
import com.khuongnd.dexassistant.ui.routing.ActionId
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import com.khuongnd.dexassistant.ui.routing.folded
import com.khuongnd.dexassistant.ui.execution.ActionRouter
import org.json.JSONObject

/** Display examples are derived from the same bundled catalog used by the router. */
internal data class LocalCommandExample(val id: String, val group: String, val phrases: List<String>, val note: String) {
    val text: String get() = phrases.first()
    val aliases: List<String> get() = phrases.drop(1)
}

internal object LocalCommandExamples {
    val groupOrder = listOf("Ứng dụng & cửa sổ", "Wi-Fi & kết nối", "Âm lượng & độ sáng",
        "Phát nhạc & video", "YouTube", "Bản đồ & tìm kiếm", "Gọi điện",
        "Điều khiển màn hình & hệ thống", "Video ngắn/TikTok")
    private val groupNames = mapOf(
        "APP_LAUNCH" to groupOrder[0], "APP_PAIR_LAUNCH" to groupOrder[0],
        "WIFI_SET" to groupOrder[1], "VOLUME_SET" to groupOrder[2], "BRIGHTNESS_SET" to groupOrder[2],
        "MEDIA_TRANSPORT" to groupOrder[3], "MEDIA_SEEK" to groupOrder[3],
        "YOUTUBE_SEARCH" to groupOrder[4], "YOUTUBE_PLAY_QUERY" to groupOrder[4],
        "YOUTUBE_SELECT" to groupOrder[4], "YOUTUBE_FULLSCREEN" to groupOrder[4],
        "NAVIGATE" to groupOrder[5], "SEARCH_PLACE" to groupOrder[5], "WEB_SEARCH" to groupOrder[5],
        "CALL_CONTACT" to groupOrder[6], "CALL_NUMBER" to groupOrder[6],
        "CLICK_TEXT" to groupOrder[7], "SCROLL" to groupOrder[7], "SYSTEM_ACTION" to groupOrder[7],
        "FEED_NEXT" to groupOrder[8], "FEED_PREVIOUS" to groupOrder[8],
    )
    private val values = mapOf(
        "app" to "YouTube", "firstApp" to "YouTube", "secondApp" to "Maps",
        "percent" to "50 phần trăm", "steps" to "2", "query" to "Sơn Tùng",
        "index" to "1", "title" to "Sơn Tùng", "seconds" to "30 giây",
        "destination" to "Hồ Gươm", "contact" to "mẹ", "number" to "0123456789", "text" to "Tìm kiếm",
    )
    private val placeholder = Regex("\\{([a-zA-Z][a-zA-Z0-9]*)\\}")
    private val notes = mapOf(
        "APP_LAUNCH" to "Cần ứng dụng đã cài và được cấu hình.",
        "APP_PAIR_LAUNCH" to "Cần hai ứng dụng đã cấu hình và cửa sổ DeX phù hợp.",
        "BRIGHTNESS_SET" to "Cần quyền thay đổi cài đặt hệ thống.",
        "MEDIA_TRANSPORT" to "Cần phiên phát nhạc/video phù hợp.",
        "YOUTUBE_SEARCH" to "Cần YouTube và cửa sổ DeX phù hợp.",
        "YOUTUBE_PLAY_QUERY" to "Cần YouTube và cửa sổ DeX phù hợp.",
        "YOUTUBE_SELECT" to "Cần danh sách video đang hiển thị; sẽ yêu cầu xác nhận.",
        "YOUTUBE_FULLSCREEN" to "Cần YouTube đang mở trong DeX.",
        "MEDIA_SEEK" to "Cần phiên phát và quyền truy cập thông báo.",
        "FEED_NEXT" to "Cần TikTok đang mở trong DeX và Trợ năng.",
        "FEED_PREVIOUS" to "Cần TikTok đang mở trong DeX và Trợ năng.",
        "NAVIGATE" to "Cần Google Maps.", "SEARCH_PLACE" to "Cần Google Maps hoặc Chrome phù hợp.",
        "CALL_CONTACT" to "Cần danh bạ và xác nhận; thiếu quyền gọi sẽ mở bàn phím gọi.",
        "CALL_NUMBER" to "Cần xác nhận; thiếu quyền gọi sẽ mở bàn phím gọi.",
        "CLICK_TEXT" to "Cần Trợ năng và cửa sổ DeX phù hợp.",
        "SCROLL" to "Cần Trợ năng và cửa sổ DeX phù hợp.",
        "SYSTEM_ACTION" to "Cần Trợ năng và ngữ cảnh DeX phù hợp.",
    )
    fun load(context: Context): List<LocalCommandExample> {
        CommandRegistry.load(context) // Reject a stale or malformed bundled catalog.
        val json = context.assets.open("commands.json").bufferedReader().use { it.readText() }
        val supported = ActionRouter.supportedActionIds().map(ActionId::name).toSet()
        return fromJson(json, supported)
    }
    fun fromJson(json: String, supported: Set<String>): List<LocalCommandExample> {
        val commands = JSONObject(json).getJSONArray("commands")
        return (0 until commands.length()).mapNotNull { index ->
            val item = commands.getJSONObject(index)
            val action = item.getString("action")
            if (action !in supported) return@mapNotNull null
            val group = requireNotNull(groupNames[action]) { "Missing display group for $action" }
            val patterns = item.getJSONArray("patterns")
            val phrases = (0 until patterns.length()).map { patternIndex ->
                val raw = patterns.getString(patternIndex)
                require(placeholder.findAll(raw).all { it.groupValues[1] in values }) { "Missing example for $raw" }
                placeholder.replace(raw) { values.getValue(it.groupValues[1]) }
            }
            val id = item.getString("id")
            LocalCommandExample(id, group, phrases,
                if (id == "web_search_in_chrome") "Cần Chrome đang mở trong DeX." else notes[action].orEmpty())
        }
    }
    fun search(examples: List<LocalCommandExample>, query: String): List<LocalCommandExample> {
        val term = query.trim()
        if (term.isEmpty()) return examples
        return examples.filter { item -> matches(item.group, term) || item.phrases.any { matches(it, term) } }
    }
    fun matches(value: String, query: String): Boolean = folded(value).contains(folded(query.trim()))
    val featuredIds = setOf("open_app", "wifi_on", "volume_percent", "media_pause", "youtube_search", "navigate", "scroll_forward")
}
