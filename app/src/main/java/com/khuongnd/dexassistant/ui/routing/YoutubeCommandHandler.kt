package com.khuongnd.dexassistant.ui.routing

import com.khuongnd.dexassistant.ui.screen.AppUiAdapters
import com.khuongnd.dexassistant.ui.screen.AppUiTarget

/** Deterministic search/select state shared by local and backend YouTube actions. */
class YoutubeCommandHandler {
    enum class Observation { WAIT, CHOOSE, AMBIGUOUS, NUMBERED_FALLBACK, UNAVAILABLE }
    sealed interface Selection {
        data class Matches(val indexes: List<Int>) : Selection
        data object Ambiguous : Selection
        data object NotFound : Selection
    }

    private data class Search(val parentId: String, val query: String)
    private var search: Search? = null

    fun clearForParent(parentId: String) {
        if (search?.parentId != parentId) search = null
    }

    fun searched(parentId: String, query: String) {
        search = Search(parentId, query)
    }

    fun queryForSelection(parentId: String): String? = search?.takeIf { it.parentId == parentId }?.query

    fun observe(targets: List<AppUiTarget>, queryVisible: Boolean, expired: Boolean,
                selection: TypedAction? = null): Observation {
        if (targets.isNotEmpty()) {
            if (selection == null && !AppUiAdapters.youtubeChoicesUnambiguous(targets)) return Observation.AMBIGUOUS
            return Observation.CHOOSE
        }
        if (!expired) return Observation.WAIT
        return if (queryVisible) Observation.NUMBERED_FALLBACK else Observation.UNAVAILABLE
    }

    fun select(targets: List<AppUiTarget>, selection: TypedAction): Selection {
        require(selection.action == ActionId.YOUTUBE_SELECT)
        val indexes = if (selection.params.containsKey("index")) {
            listOf(selection.int("index") - 1).filter { it in targets.indices }
        } else {
            val title = AppUiAdapters.label(selection.string("title"))
            targets.indices.filter { AppUiAdapters.label(targets[it].label) == title }
        }
        if (indexes.isEmpty()) return Selection.NotFound
        val relevant = if (selection.params.containsKey("index")) {
            val chosenTitles = indexes.map { AppUiAdapters.label(targets[it].label) }.toSet()
            targets.indices.filter { AppUiAdapters.label(targets[it].label) in chosenTitles }
        } else indexes
        // The adapter supplies card-local artist/context. Identical titles without
        // distinct metadata must never be resolved by an index alone.
        val choices = relevant.map { targets[it] }
        return if (AppUiAdapters.youtubeChoicesUnambiguous(choices)) Selection.Matches(indexes)
            else Selection.Ambiguous
    }
}
