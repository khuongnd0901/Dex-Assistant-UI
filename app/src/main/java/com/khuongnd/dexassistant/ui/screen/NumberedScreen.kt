package com.khuongnd.dexassistant.ui.screen

import java.text.Normalizer

/** Pure geometry and grammar; deliberately reserves malformed numeric commands locally. */
data class ScreenRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = right - left
    val height get() = bottom - top
    val x get() = (left + right) / 2f
    val y get() = (top + bottom) / 2f
    fun intersects(other: ScreenRect) = left < other.right && right > other.left && top < other.bottom && bottom > other.top
    fun inside(other: ScreenRect) = left >= other.left && top >= other.top && right <= other.right && bottom <= other.bottom
    fun cell(number: Int): ScreenRect? {
        if (number !in 1..24 || width < 6 || height < 4) return null
        val column = (number - 1) % 6; val row = (number - 1) / 6
        return ScreenRect(left + width * column / 6, top + height * row / 4,
            left + width * (column + 1) / 6, top + height * (row + 1) / 4)
    }
}
enum class ScreenVerb { CLICK_TARGET, CLICK_CELL, SUBDIVIDE, GRID, CANCEL, INVALID }
data class ScreenCommand(val verb: ScreenVerb, val number: Int? = null)
object ScreenCommandParser {
    fun parse(text: String): ScreenCommand? {
        val normalized = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace("đ", "d").replace(Regex("\\p{M}+"), "").trim().trimEnd('.', ',', '!', '?').replace(Regex("\\s+"), " ")
        // STT can attach a digit to the explicit numeric marker (ô2, số8).
        // Insert only the missing numeric separator, preserving whole-input matching.
        val clean = normalized.replace(Regex("\\b(o|so)(?=[0-9])"), "$1 ")
        if (clean in listOf("huy", "huy bo", "bo qua", "huy lenh")) return ScreenCommand(ScreenVerb.CANCEL)
        if (clean in listOf("hien luoi", "mo luoi")) return ScreenCommand(ScreenVerb.GRID)
        val match = Regex("^(bam(?: vao)? (?:so |o (?:so )?)?|chia o )(.+)$").matchEntire(clean)
        if (match != null) {
            val prefix = match.groupValues[1]; val number = number(match.groupValues[2])
            // Plain semantic clicks are handled by the typed command engine with confirmation.
            // Explicit numeric/grid syntax remains reserved, even when malformed.
            if (number == null && prefix in listOf("bam ", "bam vao ") &&
                !Regex("^(?:so|o|[0-9]|mot|hai|ba|bon|nam|sau|bay|tam|chin|muoi)(?: |$)").containsMatchIn(match.groupValues[2]) &&
                !Regex("(?:^| )(?:va|roi|sau do)(?: |$)").containsMatchIn(clean)) return null
            val verb = when {
                number == null -> ScreenVerb.INVALID
                prefix.startsWith("chia") -> ScreenVerb.SUBDIVIDE
                " o " in prefix -> ScreenVerb.CLICK_CELL
                else -> ScreenVerb.CLICK_TARGET
            }
            return ScreenCommand(if (verb in listOf(ScreenVerb.CLICK_CELL, ScreenVerb.SUBDIVIDE) && (number == null || number !in 1..24)) ScreenVerb.INVALID else verb, number)
        }
        if (Regex("(?:^| )(?:so|o) (?:\\d|mot|hai|ba|bon|nam|sau|bay|tam|chin|muoi)").containsMatchIn(clean) ||
            Regex("(?:^| )(huy|hien luoi|mo luoi|chia o)(?: |$)").containsMatchIn(clean) || Regex("(?:^| )bam(?: |$)").containsMatchIn(clean)) return ScreenCommand(ScreenVerb.INVALID)
        return null
    }
    fun number(text: String): Int? {
        text.toIntOrNull()?.let { return it.takeIf { n -> n > 0 } }
        val digits = mapOf("mot" to 1, "hai" to 2, "ba" to 3, "bon" to 4, "tu" to 4,
            "nam" to 5, "lam" to 5, "sau" to 6, "bay" to 7, "tam" to 8, "chin" to 9)
        digits[text]?.let { return it }
        if (text == "muoi") return 10
        val words = text.split(' ')
        if (words.size >= 2 && words[1] == "tram") {
            val hundreds = digits[words[0]] ?: return null
            if (words.size == 2) return hundreds * 100
            val rest = words.drop(2).dropWhile { it == "linh" || it == "le" }.joinToString(" ")
            return number(rest)?.takeIf { it < 100 }?.plus(hundreds * 100)
        }
        if (words.size == 2 && words[0] == "muoi") return digits[words[1]]?.plus(10)
        if (words.size in 2..3 && words[1] == "muoi") {
            val tens = digits[words[0]]?.takeIf { it >= 2 } ?: return null
            return if (words.size == 2) tens * 10 else digits[words[2]]?.plus(tens * 10)
        }
        return null
    }
}

/** Layer order must be front to back. Any overlapping foreground window excludes a target. */
object ScreenTargets {
    fun visible(candidates: List<ScreenRect>, foreground: List<ScreenRect>, screen: ScreenRect) =
        candidates.filter { it.width > 0 && it.height > 0 && it.inside(screen) && foreground.none(it::intersects) }
    fun merge(nodes: List<ScreenRect>, image: List<ScreenRect>): List<ScreenRect> {
        val unique = mutableListOf<ScreenRect>()
        nodes.distinct().sortedBy { it.width.toLong() * it.height }.forEach { candidate ->
            if (unique.none { it.inside(candidate) || candidate.inside(it) }) unique.add(candidate)
        }
        image.forEach { candidate -> if (unique.none { it.intersects(candidate) }) unique.add(candidate) }
        val rows = mutableListOf<MutableList<ScreenRect>>()
        var overlapBottom = Int.MIN_VALUE
        unique.sortedWith(compareBy<ScreenRect> { it.top }.thenBy { it.left }).forEach { target ->
            if (rows.isEmpty() || target.top >= overlapBottom) {
                rows.add(mutableListOf(target)); overlapBottom = target.bottom
            } else { rows.last().add(target); overlapBottom = minOf(overlapBottom, target.bottom) }
        }
        return rows.flatMap { it.sortedWith(compareBy<ScreenRect> { target -> target.left }.thenBy { target -> target.top }) }
    }
}

/** Exactly one gesture may consume a prepared mapping; invalidation never revives a stale ID. */
class ScreenSessionGate {
    private var currentId: String? = null
    @Synchronized fun activate(id: String) { currentId = id }
    @Synchronized fun isCurrent(id: String) = currentId == id
    @Synchronized fun invalidate() { currentId = null }
    @Synchronized fun consume(id: String): Boolean {
        if (!isCurrent(id)) return false
        currentId = null
        return true
    }
}

/** Only unique desktop identity/geometry matters; unrelated/default display updates do not expire it. */
object ScreenDisplays {
    fun matches(displayId: Int, bounds: ScreenRect, desktops: List<Pair<Int, ScreenRect>>): Boolean {
        val current = desktops.filter { it.first != 0 && it.second.width > 0 && it.second.height > 0 }.singleOrNull()
        return current?.first == displayId && current.second == bounds
    }
}

/** Stable content notification is harmless; missing/freshly changed topology or identity is not. */
object ScreenContentStability {
    fun <T : Any> invalidates(frozen: T, fresh: T?, windowStateChanged: Boolean = false): Boolean =
        windowStateChanged || fresh != frozen
}

/** Grid cells are display coordinates; numbered targets also freeze windows and node identity. */
data class ScreenWindow(val id: Int, val layer: Int, val type: Int, val bounds: ScreenRect,
    val focused: Boolean, val active: Boolean)
data class ScreenSnapshot(val displayId: Int, val bounds: ScreenRect, val targets: List<ScreenRect>,
    val windows: List<ScreenWindow>, val identities: List<String>) {
    fun mismatch(fresh: ScreenSnapshot?, grid: Boolean = false): String? = when {
        fresh == null -> "unavailable"
        displayId != fresh.displayId -> "display_id"
        bounds != fresh.bounds -> "display_bounds"
        !grid && windows != fresh.windows -> "window_topology"
        !grid && targets != fresh.targets -> "target_rects"
        !grid && identities != fresh.identities -> "target_identity"
        else -> null
    }
}
