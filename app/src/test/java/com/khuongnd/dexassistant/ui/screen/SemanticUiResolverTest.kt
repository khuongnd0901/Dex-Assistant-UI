package com.khuongnd.dexassistant.ui.screen

import org.junit.Assert.*
import org.junit.Test

class SemanticUiResolverTest {
    private fun node(path: String, text: String = "", id: String = "", clickable: Boolean = false,
        kind: String = "android.widget.TextView") = UiNodeRecord(path,path.substringBeforeLast('/', "").takeIf { it.isNotEmpty() },
        ScreenRect(0,0,200,100),text,"",id,clickable,false,emptySet(),path,kind)
    private val cards = listOf(
        node("root/0",clickable=true,kind="android.view.ViewGroup"),
        node("root/0/title", "Nơi này có anh", "video_title"),
        node("root/0/artist", "Sơn Tùng M-TP", "artist"),
        node("root/1",clickable=true,kind="android.view.ViewGroup"),
        node("root/1/title", "Nơi này có anh", "video_title"),
        node("root/1/artist", "Bản cover", "artist"))

    @Test fun duplicateTitleNeedsContextOrChoice() {
        assertEquals(2,SemanticUiResolver.resolve(cards,SemanticUiResolver.Selector(text="Nơi này có anh")).size)
        assertEquals("root/0",SemanticUiResolver.resolve(cards,SemanticUiResolver.Selector(
            text="Nơi này có anh",contextText="Sơn Tùng M-TP")).single().node.path)
        assertEquals("root/1",SemanticUiResolver.resolve(cards,SemanticUiResolver.Selector(
            text="Nơi này có anh",ordinal=2)).single().node.path)
    }
    @Test fun conflictingResourceAndTextAndNeighborContextFailClosed() {
        assertTrue(SemanticUiResolver.resolve(cards,SemanticUiResolver.Selector(
            resourceId="artist",text="Nơi này có anh")).isEmpty())
        assertTrue(SemanticUiResolver.resolve(cards,SemanticUiResolver.Selector(
            text="Nơi này có anh",contextText="Unknown")).isEmpty())
        val missing = cards.filterNot { it.path == "root/0/artist" }
        assertEquals("root/1",SemanticUiResolver.resolve(missing,SemanticUiResolver.Selector(
            text="Nơi này có anh",contextText="Bản cover")).single().node.path)
        assertTrue(SemanticUiResolver.resolve(missing,SemanticUiResolver.Selector(
            resourceId="video_title",text="Nơi này có anh",contextText="Sơn Tùng M-TP")).isEmpty())
    }
    @Test fun clickableAncestorAndRoleMustMatch() {
        assertEquals("root/0",SemanticUiResolver.resolve(cards,SemanticUiResolver.Selector(
            resourceId="video_title",text="Nơi này có anh",contextText="Sơn Tùng M-TP",role="TEXT")).single().node.path)
        assertTrue(SemanticUiResolver.resolve(cards,SemanticUiResolver.Selector(
            text="Nơi này có anh",role="BUTTON")).isEmpty())
    }
}
