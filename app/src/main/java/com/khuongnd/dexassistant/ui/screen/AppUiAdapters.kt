package com.khuongnd.dexassistant.ui.screen

import java.text.Normalizer
import java.util.Locale

/** Local-only accessibility fixture model; no screen contents enter the protocol. */
data class UiNodeRecord(
    val path: String, val parentPath: String?, val bounds: ScreenRect,
    val text: String, val description: String, val viewId: String,
    val clickable: Boolean, val scrollable: Boolean, val actions: Set<Int>, val identity: String = "",
)
data class AppUiTarget(val node: UiNodeRecord, val label: String)
object AppUiAdapters {
    private val duration = Regex("(?:^| )\\d{1,2}:\\d{2}(?: |$)")
    private val adLabels = setOf("quảng cáo", "sponsored", "ad")
    private val whitespace = Regex("\\s+")
    fun label(value: String) = Normalizer.normalize(value, Normalizer.Form.NFC)
        .lowercase(Locale.forLanguageTag("vi-VN")).trim().replace(whitespace, " ")
    private fun ancestor(node: UiNodeRecord, nodes: Map<String, UiNodeRecord>): UiNodeRecord? {
        var current: UiNodeRecord? = node
        repeat(6) {
            if (current?.clickable == true) return current
            current = nodes[current?.parentPath]
        }
        return null
    }
    fun textTargets(nodes: List<UiNodeRecord>, query: String): List<AppUiTarget> {
        val normalizedQuery = label(query)
        val byPath = nodes.associateBy { it.path }
        return nodes.filter { label(it.text) == normalizedQuery || label(it.description) == normalizedQuery }
            .mapNotNull { node -> ancestor(node, byPath)?.let { AppUiTarget(it, query) } }.distinctBy { it.node.path }
    }

    /** A title alone is insufficient: require a card with thumbnail AND duration/video metadata. */
    fun youtubeResults(nodes: List<UiNodeRecord>): List<AppUiTarget> {
        val byPath = nodes.associateBy { it.path }
        return nodes.filter {
            it.text.isNotBlank() && it.viewId.substringAfterLast('/').let { id -> id == "title" || id == "video_title" }
        }.mapNotNull { title ->
            val card = ancestor(title, byPath) ?: return@mapNotNull null
            val children = nodes.filter { it.path.startsWith("${card.path}/") || it.path == card.path }
            val thumbnail = children.any { "thumbnail" in it.viewId || "video_thumbnail" in it.viewId }
            val video = children.any { "duration" in it.viewId || "video_length" in it.viewId ||
                duration.containsMatchIn(it.text + " " + it.description) }
            val ad = children.any { label(it.text) in adLabels }
            if (thumbnail && video && !ad) AppUiTarget(card, title.text) else null
        }.distinctBy { it.node.path }.sortedWith(compareBy({ it.node.bounds.top }, { it.node.bounds.left }))

    }

    fun fullscreen(nodes: List<UiNodeRecord>, enabled: Boolean): List<AppUiTarget> {
        val names = if (enabled) setOf("toàn màn hình", "full screen", "enter full screen")
            else setOf("thoát toàn màn hình", "exit full screen")
        return nodes.filter { it.clickable && (label(it.description) in names || label(it.text) in names) }
            .map { AppUiTarget(it, if (enabled) "toàn màn hình" else "thoát toàn màn hình") }
    }
    /** A gesture fallback must have one identifiable feed container, not a whole-window guess. */
    fun feed(nodes: List<UiNodeRecord>, packageName: String): UiNodeRecord? {
        if (packageName !in setOf("com.facebook.katana", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill")) return null
        val tiktok = packageName != "com.facebook.katana"
        return nodes.filter { node ->
            val id = node.viewId.substringAfterLast('/').lowercase(Locale.ROOT)
            val namedFeed = id in setOf("feed", "feed_list", "vertical_view_pager", "news_feed")
            val genericContainer = id in setOf("recycler_view", "viewpager", "view_pager")
            if (node.bounds.width <= 100 || node.bounds.height <= 200) return@filter false
            if (namedFeed) return@filter true
            if (!genericContainer) return@filter false
            nodes.any { child ->
                if (!child.path.startsWith("${node.path}/")) return@any false
                val childId = child.viewId.substringAfterLast('/').lowercase(Locale.ROOT)
                if (tiktok) childId in setOf("video_player", "feed_video", "player_view")
                else childId in setOf("feed_story", "post", "feed_post", "story")
            }
        }.singleOrNull()
    }
}
