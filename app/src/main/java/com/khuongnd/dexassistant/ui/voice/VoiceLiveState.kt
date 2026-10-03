package com.khuongnd.dexassistant.ui.voice

import com.khuongnd.dexassistant.ui.routing.CommandPrivacy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveVoiceState(val transcript: String = "", val final: Boolean = false, val diagnostic: Boolean = false, val microphone: String = "Chưa đo micro", val ready: Boolean = false)
/** RAM only. Never written to preferences/history/logs or sent by a partial callback. */
object VoiceLiveState {
    private val mutable = MutableStateFlow(LiveVoiceState())
    val state = mutable.asStateFlow()
    fun begin(diagnostic: Boolean) { mutable.value = LiveVoiceState(diagnostic = diagnostic) }
    fun text(text: String, final: Boolean) { mutable.value = mutable.value.copy(transcript = safeTranscript(text).take(512), final = final) }
    fun ready(value: Boolean) { mutable.value = mutable.value.copy(ready = value) }
    fun microphone(detail: String) { mutable.value = mutable.value.copy(microphone = detail.take(180)) }
    fun safeTranscript(text: String): String {
        val folded = java.text.Normalizer.normalize(text.lowercase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace('đ', 'd')
        // Mask credential cues even while a partial result contains only the prefix of a keyword.
        val cue = Regex("\\b(?:mat|khau|ma|pass\\w*|bear\\w*|tok\\w*|secr\\w*|api)\\b")
        return if (cue.containsMatchIn(folded)) "[REDACTED]" else CommandPrivacy.redact(text)
    }
    fun clear() { mutable.value = LiveVoiceState() }
}
/** App owns focus only for its own feedback, never competing with an external STT service. */
object VoiceFocusPolicy {
    fun pauseForLoss(ownsFocus: Boolean, state: VoiceState, callbackGeneration: Long = 0, currentGeneration: Long = 0): Boolean = ownsFocus && callbackGeneration == currentGeneration && state == VoiceState.SPEAKING
}
