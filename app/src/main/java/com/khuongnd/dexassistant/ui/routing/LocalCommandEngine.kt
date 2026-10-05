package com.khuongnd.dexassistant.ui.routing

/** Slot text is the original NFC text, never its accent-folded matching projection. */
enum class MatchQuality { EXACT, ACCENT_FOLDED }
data class ResolvedCommand(val typedAction: TypedAction, val commandId: String, val configVersion: Int, val matchQuality: MatchQuality)

class LocalCommandEngine(private val registry: CommandRegistry) {
    private val percent = Regex("(100|[1-9]?[0-9])(?: *%| phan tram)")
    private val duration = Regex("([0-9]+|mot|hai|ba|bon|nam|sau|bay|tam|chin|muoi) (giay|phut)")
    private val reservedScreenPrefix = Regex("^(?:so|o|luoi)\\b")
    private val digit = Regex("[0-9]")
    private val number = Regex("\\+?[0-9]{3,15}")
    private val openEndedYoutubeQuery = Regex("\\b(phu hop|goi y|tam trang|bat ky bai nao)\\b")
    fun resolve(original: String, foregroundAppId: String? = null): ResolvedCommand? {
        if (CommandPrivacy.requiresLocalHandling(original)) return null
        val preserved = VietnameseNormalizer.preserveSlots(original)
        val literalText = VietnameseNormalizer.prepare(original)
        // A free-text slot owns its punctuation; literal-only commands can tolerate STT punctuation.
        val text = preserved
        val projection = folded(text)
        if (text.isBlank() || isNegated(text)) return null
        val compoundRequest = compound.containsMatchIn(projection)
        // Offsets are only needed after a pattern matched slots. Literal and unmatched
        // commands avoid per-character Unicode normalization and list allocation.
        val offsets by lazy(LazyThreadSafetyMode.NONE) { projectionOffsets(text) }
        val literalProjection = if (literalText == text) projection else folded(literalText)
        val literalOffsets by lazy(LazyThreadSafetyMode.NONE) {
            if (literalText == text) offsets else projectionOffsets(literalText)
        }
        data class Candidate(val value: ResolvedCommand, val score: List<Int>)
        val indexed = registry.candidatesFor(projection)
        val candidates = (if (projection == literalProjection) indexed else
            (indexed + registry.candidatesFor(literalProjection)).distinct())
            .mapNotNull { pattern ->
                if (compoundRequest && !pattern.allowCompound) return@mapNotNull null
                if (pattern.scope.isNotEmpty() && foregroundAppId !in pattern.scope) return@mapNotNull null
                val capturesText = pattern.slots.values.any { it.type == SlotType.TEXT }
                val candidateText = if (capturesText) text else literalText
                val candidateProjection = if (capturesText) projection else literalProjection
                val match = pattern.regex.matchEntire(candidateProjection) ?: return@mapNotNull null
                val args = LinkedHashMap(pattern.args)
                pattern.slotNames.forEachIndexed { index, name ->
                    val group = match.groups[index + 1] ?: return@mapNotNull null
                    val candidateOffsets = if (capturesText) offsets else literalOffsets
                    val raw = candidateText.substring(candidateOffsets[group.range.first], candidateOffsets[group.range.last] + 1).trim()
                    val value: Any = when (pattern.slots.getValue(name).type) {
                        SlotType.TEXT -> raw.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        SlotType.APP_ALIAS -> registry.appIdForAlias(raw) ?: return@mapNotNull null
                        SlotType.INTEGER -> spokenInteger(folded(raw)) ?: return@mapNotNull null
                        SlotType.PERCENT -> percent.matchEntire(folded(raw))?.groupValues?.get(1)?.toInt() ?: return@mapNotNull null
                        SlotType.DURATION -> duration.matchEntire(folded(raw))?.let { d ->
                            spokenInteger(d.groupValues[1])?.toLong()?.times(if (d.groupValues[2] == "phut") 60 else 1)
                                ?.takeIf { it in 1..3600 }?.toInt()
                        } ?: return@mapNotNull null
                        SlotType.PHONE_NUMBER -> phoneNumber(raw) ?: return@mapNotNull null
                    }
                    if (pattern.action == "YOUTUBE_PLAY_QUERY" && openEndedYoutubeQuery.containsMatchIn(folded(value.toString()))) return@mapNotNull null
                    if (pattern.action == "CALL_CONTACT" && folded(value.toString()).startsWith("so ")) return@mapNotNull null
                    if (pattern.action == "CLICK_TEXT" && reservedScreenPrefix.containsMatchIn(folded(value.toString()))) return@mapNotNull null
                    args[name] = value
                }
                val action = try { ActionCatalog.validate(pattern.action,args) } catch (_: RuntimeException) { return@mapNotNull null }
                val quality = if (pattern.exactRegex.matches(candidateText)) MatchQuality.EXACT else MatchQuality.ACCENT_FOLDED
                Candidate(ResolvedCommand(action,pattern.commandId,registry.configVersion,quality),
                    listOf(if (quality == MatchQuality.EXACT) 1 else 0, if (pattern.scope.isNotEmpty()) 1 else 0, pattern.literalSpecificity, pattern.typedSpecificity, pattern.priority))
            }
        if (candidates.isEmpty()) return null
        val comparator = Comparator<Candidate> { left, right ->
            left.score.zip(right.score).firstOrNull { it.first != it.second }?.let { it.first.compareTo(it.second) } ?: 0
        }
        val best = candidates.maxWith(comparator)
        if (candidates.any { it.score.dropLast(1) == best.score.dropLast(1) && it.value.typedAction != best.value.typedAction }) return null
        return best.value
    }
    private fun phoneNumber(raw: String): String? {
        val text = folded(raw)
        if (number.matches(text)) return text
        val digits = text.split(' ').map { digitWords[it] ?: it.takeIf { value -> digit.matches(value) } ?: return null }.joinToString("")
        return digits.takeIf(number::matches)
    }
    companion object {
        // Never execute an initial clause or guess negation. Named composite flows have explicit config patterns.
        private val negation = Regex("^(?:đừng|không|chớ|dung|khong|cho)\\b",RegexOption.IGNORE_CASE)
        private val compound = Regex("\\s(?:va|roi|sau do)\\s")
        internal fun requiresBackend(text: String): Boolean = isNegated(text)
        private fun isNegated(text: String): Boolean = negation.containsMatchIn(text)
        private val digitWords = mapOf("khong" to "0", "mot" to "1", "hai" to "2", "ba" to "3", "bon" to "4", "nam" to "5", "sau" to "6", "bay" to "7", "tam" to "8", "chin" to "9")
        private fun projectionOffsets(text: String): IntArray {
            val offsets = ArrayList<Int>(text.length)
            text.forEachIndexed { index, char -> repeat(folded(char.toString()).length) { offsets.add(index) } }
            return offsets.toIntArray()
        }
    }
}
