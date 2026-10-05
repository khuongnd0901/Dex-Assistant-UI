package com.khuongnd.dexassistant.ui.voice

/** Session epochs invalidate every late microphone, recognizer and TTS callback. */
class VoiceSessionGuard {
    companion object { private val nextEpoch = java.util.concurrent.atomic.AtomicLong() }
    private var epoch = 0L
    private var active = false
    private var finalConsumed = false

    @Synchronized fun begin(): Long {
        epoch = nextEpoch.incrementAndGet()
        active = true
        finalConsumed = false
        return epoch
    }
    @Synchronized fun isCurrent(id: Long): Boolean = active && epoch == id
    @Synchronized fun consumeFinal(id: Long): Boolean {
        if (!isCurrent(id) || finalConsumed) return false
        finalConsumed = true
        return true
    }
    @Synchronized fun cancel() { active = false; epoch = nextEpoch.incrementAndGet() }
}
