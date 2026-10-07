package com.khuongnd.dexassistant.ui.screen

import org.junit.Assert.*
import org.junit.Test

class AppUiAdaptersTest {
    private fun node(path: String, id: String = "", text: String = "", clickable: Boolean = false, description: String = "") =
        UiNodeRecord(path, path.substringBeforeLast('/', "").takeIf { it.isNotEmpty() }, ScreenRect(0, 0, 500, 600), text, description, id, clickable, false, emptySet())
    @Test fun videoCardsRequireTitleThumbnailAndDurationAndExcludeAds() {
        val nodes = listOf(node("r/channel", clickable=true), node("r/channel/title", "title", "Channel"),
            node("r/video", clickable=true), node("r/video/title", "video_title", "Nơi này có anh"),
            node("r/video/thumb", "thumbnail"), node("r/video/time", "duration", "4:20"),
            node("r/ad", clickable=true), node("r/ad/title", "title", "Advertisement"),
            node("r/ad/thumb", "thumbnail"), node("r/ad/time", "duration", "0:30"), node("r/ad/label", text="Quảng cáo"))
        assertEquals(listOf("Nơi này có anh"), AppUiAdapters.youtubeResults(nodes).map { it.label })
    }
    @Test fun semanticTargetsAreExactAndDeduplicateClickableParent() {
        val nodes = listOf(node("r/button", clickable=true), node("r/button/text", text="Tiếp tục"), node("r/button/label", description="Tiếp tục"))
        assertEquals(1, AppUiAdapters.textTargets(nodes, "tiếp tục").size)
        assertTrue(AppUiAdapters.textTargets(nodes, "tiếp").isEmpty())
    }
    @Test fun youtubeCardRetainsArtistWithinItsOwnCard() {
        val nodes = listOf(node("r/video",clickable=true),node("r/video/title","video_title","Nơi này có anh"),
            node("r/video/artist","artist","Sơn Tùng M-TP"),node("r/video/thumb","thumbnail"),
            node("r/video/time","duration","4:20"))
        assertEquals("Sơn Tùng M-TP",AppUiAdapters.youtubeResults(nodes).single().contextText)
    }
    @Test fun duplicateTitlesNeedDistinctArtistButUniqueTitleDoesNot() {
        val first = AppUiTarget(node("r/first",clickable=true),"Nơi này có anh","Sơn Tùng M-TP")
        val second = AppUiTarget(node("r/second",clickable=true),"Nơi này có anh","")
        val other = AppUiTarget(node("r/other",clickable=true),"Một bài khác","")
        assertFalse(AppUiAdapters.youtubeChoicesUnambiguous(listOf(first,second)))
        assertFalse(AppUiAdapters.youtubeChoicesUnambiguous(listOf(first,second.copy(contextText="Sơn Tùng M-TP"))))
        assertTrue(AppUiAdapters.youtubeChoicesUnambiguous(listOf(first,second.copy(contextText="Bản cover"))))
        assertTrue(AppUiAdapters.youtubeChoicesUnambiguous(listOf(first,other)))
    }
    @Test fun feedFallbackRejectsUnknownAndAmbiguousContainers() {
        assertNull(AppUiAdapters.feed(listOf(node("r", "viewpager")), "com.google.android.youtube"))
        assertNull(AppUiAdapters.feed(listOf(node("r", "viewpager")), "com.zhiliaoapp.musically"))
        assertNotNull(AppUiAdapters.feed(listOf(node("r", "viewpager"),node("r/video", "video_player")), "com.zhiliaoapp.musically"))
        assertNull(AppUiAdapters.feed(listOf(node("r", "feed"),node("r/x", "vertical_view_pager")), "com.zhiliaoapp.musically"))
    }
}
