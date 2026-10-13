package com.khuongnd.dexassistant.ui.voice

/** Main-thread playback cursor. Chunk text stays in RAM and callbacks identify one chunk. */
internal class TtsPlayback(text: String, limit: Int, private val token: String) {
    data class Chunk(val text: String, val id: String)
    private val chunks = split(text, limit)
    private var index = 0
    private var active = true
    private var started = false
    val current: Chunk? get() = if (active) Chunk(chunks[index], "$token-$index") else null

    fun start(id: String?): Boolean {
        if (id != current?.id || !active || started) return false
        started = true
        return true
    }

    /** Returns false for duplicate, cancelled or old callbacks. Null current means fully done. */
    fun complete(id: String?): Boolean {
        if (!active || id != current?.id) return false
        if (++index == chunks.size) active = false
        return true
    }

    fun cancel() { active = false }

    companion object {
        fun split(text: String, limit: Int): List<String> {
            require(limit >= 2 && text.isNotBlank())
            val chunks = mutableListOf<String>()
            var offset = 0
            while (offset < text.length) {
                var end = minOf(offset + limit, text.length)
                if (end < text.length) {
                    // Keep supplementary code points intact even at a hard boundary.
                    if (text[end - 1].isHighSurrogate() && text[end].isLowSurrogate()) end--
                    val halfway = offset + (end - offset) / 2
                    val sentence = (end - 1 downTo halfway).firstOrNull { text[it] in ".!?\n" }
                    val word = (end - 1 downTo halfway).firstOrNull { text[it].isWhitespace() }
                    end = sentence?.plus(1) ?: word?.plus(1) ?: end
                }
                chunks += text.substring(offset, end)
                offset = end
            }
            return chunks
        }
    }
}
