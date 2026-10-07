package com.khuongnd.dexassistant.ui

import com.khuongnd.dexassistant.ui.routing.ActionCatalog
import com.khuongnd.dexassistant.ui.routing.YoutubeCommandHandler
import com.khuongnd.dexassistant.ui.screen.AppUiTarget
import com.khuongnd.dexassistant.ui.screen.ScreenRect
import com.khuongnd.dexassistant.ui.screen.UiNodeRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeCommandHandlerTest {
    private fun card(index: Int, title: String, artist: String = "") = AppUiTarget(
        UiNodeRecord("card$index", null, ScreenRect(0, index * 100, 300, index * 100 + 80),
            "", "", "", true, false, emptySet()), title, artist)
    private fun index(value: Int) = ActionCatalog.validate("YOUTUBE_SELECT", mapOf("index" to value))
    private fun title(value: String) = ActionCatalog.validate("YOUTUBE_SELECT", mapOf("title" to value))
    @Test fun indexPreviewRequiresObservedQueryBeforeAcceptingCardsAndNeverFallsBackToGrid() {
        val handler = YoutubeCommandHandler()
        val cards = listOf(card(0, "Sơn Tùng", "Artist"))
        assertEquals(YoutubeCommandHandler.Observation.WAIT, handler.preview(cards, true, false, false))
        assertEquals(YoutubeCommandHandler.Observation.UNAVAILABLE, handler.preview(cards, true, false, true))
        assertEquals(YoutubeCommandHandler.Observation.CHOOSE, handler.preview(cards, true, true, false))
        assertEquals(YoutubeCommandHandler.Observation.CHOOSE, handler.preview(cards, false, false, false))
        assertEquals(YoutubeCommandHandler.Observation.WAIT, handler.preview(emptyList(), true, true, false))
        assertEquals(YoutubeCommandHandler.Observation.UNAVAILABLE, handler.preview(emptyList(), true, true, true))
        assertEquals(YoutubeCommandHandler.Observation.AMBIGUOUS,
            handler.preview(listOf(card(0, "Duplicate"), card(1, "Duplicate")), true, true, false))
    }

    @Test fun searchContextBelongsOnlyToItsParent() {
        val handler = YoutubeCommandHandler()
        handler.searched("request-1", "Nơi này có anh")
        assertEquals("Nơi này có anh", handler.queryForSelection("request-1"))
        assertEquals(null, handler.queryForSelection("request-2"))
        handler.clearForParent("request-2")
        assertEquals(null, handler.queryForSelection("request-1"))
    }

    @Test fun duplicateTitleNeedsDistinctArtistsEvenForIndexSelection() {
        val handler = YoutubeCommandHandler()
        val first = card(0, "Nơi này có anh", "Sơn Tùng M-TP")
        val missing = card(1, "Nơi này có anh")
        assertEquals(YoutubeCommandHandler.Selection.Ambiguous, handler.select(listOf(first, missing), index(1)))
        val cover = missing.copy(contextText = "Bản cover")
        assertEquals(YoutubeCommandHandler.Selection.Matches(listOf(0)), handler.select(listOf(first, cover), index(1)))
        assertEquals(YoutubeCommandHandler.Selection.Matches(listOf(0, 1)), handler.select(listOf(first, cover), title("Nơi này có anh")))
    }

    @Test fun unrelatedDuplicateDoesNotBlockUniqueTargetAndMissingFallsBack() {
        val handler = YoutubeCommandHandler()
        val cards = listOf(card(0, "Nơi này có anh"), card(1, "Bài khác"), card(2, "Bài khác"))
        assertEquals(YoutubeCommandHandler.Selection.Matches(listOf(0)), handler.select(cards, title("Nơi này có anh")))
        assertEquals(YoutubeCommandHandler.Selection.NotFound, handler.select(cards, index(4)))
    }

    @Test fun observationWaitsThenFallsBackOnlyWhileQueryIsVisible() {
        val handler = YoutubeCommandHandler()
        val empty = emptyList<AppUiTarget>()
        assertEquals(YoutubeCommandHandler.Observation.WAIT, handler.observe(empty, true, false))
        assertEquals(YoutubeCommandHandler.Observation.NUMBERED_FALLBACK, handler.observe(empty, true, true))
        assertEquals(YoutubeCommandHandler.Observation.UNAVAILABLE, handler.observe(empty, false, true))
        val duplicate = listOf(card(0, "Nơi này có anh"), card(1, "Nơi này có anh"))
        assertEquals(YoutubeCommandHandler.Observation.AMBIGUOUS, handler.observe(duplicate, true, false))
        assertEquals(YoutubeCommandHandler.Observation.CHOOSE, handler.observe(duplicate, true, false, index(1)))
    }
}
