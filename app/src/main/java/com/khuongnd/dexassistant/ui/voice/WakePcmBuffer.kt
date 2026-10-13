package com.khuongnd.dexassistant.ui.voice

/** Worker-confined exact-size PCM conversion. JNI synchronously consumes before the next reuse. */
internal class WakePcmBuffer {
    private var normalized = FloatArray(0)
    fun convert(samples: ShortArray, count: Int): FloatArray {
        require(count in 1..samples.size)
        // Native acceptWaveform consumes array.size, so a short read must never include a stale tail.
        if (normalized.size != count) { normalized.fill(0f); normalized = FloatArray(count) }
        for (i in 0 until count) normalized[i] = samples[i] / 32768f
        return normalized
    }
    fun clear() { normalized.fill(0f); normalized = FloatArray(0) }
}
