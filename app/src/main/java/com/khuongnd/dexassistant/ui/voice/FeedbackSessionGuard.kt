package com.khuongnd.dexassistant.ui.voice

/** A TTS completion or timeout may release only the playback that created it. */
internal class FeedbackSessionGuard {
    private var generation = 0L
    private var active = false
    fun begin(): Long { active = true; return ++generation }
    fun isCurrent(token: Long): Boolean = active && token == generation
    fun consume(token: Long): Boolean {
        if (!isCurrent(token)) return false
        active = false
        return true
    }
    fun cancel() { active = false; generation++ }
}
