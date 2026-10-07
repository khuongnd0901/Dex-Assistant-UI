package com.khuongnd.dexassistant.ui.screen

/** Resolves only local, bounded Accessibility snapshots. No selector can supply coordinates. */
object SemanticUiResolver {
    data class Selector(
        val resourceId: String? = null, val text: String? = null,
        val contentDescription: String? = null, val contextText: String? = null,
        val role: String? = null, val ordinal: Int? = null,
    )

    fun resolve(nodes: List<UiNodeRecord>, selector: Selector): List<AppUiTarget> {
        if (nodes.size > 4096) return emptyList()
        val byPath = nodes.associateBy { it.path }
        val candidates = nodes.mapNotNull { node ->
            if (selector.resourceId != null && node.viewId != selector.resourceId) return@mapNotNull null
            if (selector.text != null && AppUiAdapters.label(node.text) != AppUiAdapters.label(selector.text)) return@mapNotNull null
            if (selector.contentDescription != null && AppUiAdapters.label(node.description) != AppUiAdapters.label(selector.contentDescription)) return@mapNotNull null
            val clickable = sequence {
                var current: UiNodeRecord? = node
                repeat(7) { current?.let { yield(it) }; current = byPath[current?.parentPath] }
            }.firstOrNull { it.clickable } ?: return@mapNotNull null
            if (selector.role != null && !matchesRole(node, clickable, selector.role)) return@mapNotNull null
            // Context is confined to the clickable card; a neighboring result cannot supply metadata.
            val contextRoot = clickable
            if (selector.contextText != null && nodes.none {
                    (it.path == contextRoot.path || it.path.startsWith("${contextRoot.path}/")) &&
                        (AppUiAdapters.label(it.text) == AppUiAdapters.label(selector.contextText) ||
                            AppUiAdapters.label(it.description) == AppUiAdapters.label(selector.contextText))
                }) return@mapNotNull null
            val label = listOfNotNull(node.text, node.description, selector.contextText).firstOrNull { it.isNotBlank() }
                ?: clickable.text.ifBlank { clickable.description.ifBlank { clickable.viewId.substringAfterLast('/') } }
            AppUiTarget(clickable,label.take(200))
        }.distinctBy { it.node.path }
            .sortedWith(compareBy({ it.node.bounds.top }, { it.node.bounds.left }, { it.node.path }))
        return selector.ordinal?.let { listOfNotNull(candidates.getOrNull(it - 1)) } ?: candidates
    }

    private fun matchesRole(node: UiNodeRecord, clickable: UiNodeRecord, role: String): Boolean {
        val kind = node.className.substringAfterLast('.').lowercase()
        val clickKind = clickable.className.substringAfterLast('.').lowercase()
        return when (role) {
            "BUTTON" -> "button" in kind || "button" in clickKind
            "TEXT" -> "text" in kind || "text" in clickKind
            "IMAGE" -> "image" in kind || "image" in clickKind
            "LIST_ITEM" -> "recycler" in kind || "list" in kind || "recycler" in clickKind || "list" in clickKind
            else -> false
        }
    }
}
