package com.khuongnd.dexassistant.ui.voice

/** Even if only one usable candidate remains, leave a rejected actual route. */
internal fun nextMicrophoneCandidate(candidates: List<Int>, rejected: Set<Int>, actual: Int?): Int? {
    val available = candidates.filterNot { it in rejected }
    if (available.isEmpty()) return null
    val current = available.indexOf(actual)
    if (current < 0) return available.first()
    return if (available.size == 1) null else available[(current + 1) % available.size]
}
