package com.khuongnd.dexassistant.ui.screen

/** RAM-only frozen numbering. Invalid/consumed previews remain a tombstone until explicit reset. */
class SemanticPreviewSession<T>(private val lifetimeMs: Long = 35_000) {
    sealed interface Selection<out T> {
        data object Missing : Selection<Nothing>
        data object Stale : Selection<Nothing>
        data class Ready<T>(val targets: List<T>) : Selection<T>
    }
    private var targets: List<T>? = null
    private var expiresAt = 0L
    var hasHistory = false
        private set
    fun publish(values: List<T>, now: Long) {
        require(values.isNotEmpty() && values.size <= 50)
        targets = values.toList(); expiresAt = now + lifetimeMs; hasHistory = true
    }
    fun current(now: Long, unchanged: (List<T>) -> Boolean): List<T>? {
        val frozen = targets ?: return null
        if (now >= expiresAt || !unchanged(frozen)) { invalidate(); return null }
        return frozen
    }
    fun consume(now: Long, unchanged: (List<T>) -> Boolean): Selection<T> {
        if (!hasHistory) return Selection.Missing
        val frozen = current(now, unchanged) ?: return Selection.Stale
        invalidate()
        return Selection.Ready(frozen)
    }
    fun invalidate() { targets = null }
    fun reset() { targets = null; expiresAt = 0L; hasHistory = false }
}
