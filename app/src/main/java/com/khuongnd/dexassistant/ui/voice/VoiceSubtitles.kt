package com.khuongnd.dexassistant.ui.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SubtitlePhase { PREPARING, LISTENING, RECOGNIZING, FINAL }
data class SubtitleState(val turn: Long, val phase: SubtitlePhase, val text: String = "", val expiresAt: Long? = null)
/** Identity guarded, volatile presentation only. No partial enters command dispatch. */
class SubtitleStore {
    private val mutable = MutableStateFlow<SubtitleState?>(null)
    val state = mutable.asStateFlow()
    fun begin(turn: Long) { mutable.value = SubtitleState(turn, SubtitlePhase.PREPARING) }
    fun phase(turn: Long, phase: SubtitlePhase) { val old = mutable.value ?: return
        if (old.turn == turn && old.phase != SubtitlePhase.FINAL) mutable.value = old.copy(phase = phase) }
    fun text(turn: Long, text: String, final: Boolean, now: Long) {
        val old = mutable.value ?: return
        if (old.turn != turn || old.phase == SubtitlePhase.FINAL) return
        if (text.isBlank()) { if (final) clear(turn); return }
        mutable.value = old.copy(text = VoiceLiveState.safeTranscript(text).takeLast(512),
            phase = if (final) SubtitlePhase.FINAL else old.phase, expiresAt = if (final) now + 3000 else null)
    }
    fun expire(now: Long) { val old = mutable.value ?: return
        if (old.expiresAt?.let { now >= it } == true) clear(old.turn) }
    fun clear(turn: Long? = null) { if (turn == null || mutable.value?.turn == turn) mutable.value = null }
}
object VoiceSubtitles { val store = SubtitleStore() }
