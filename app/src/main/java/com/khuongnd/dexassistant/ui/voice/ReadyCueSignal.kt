package com.khuongnd.dexassistant.ui.voice

/** A bounded rising two-note cue with click-free edges; contains no captured audio. */
object ReadyCueSignal {
    const val SAMPLE_RATE = 16000
    const val DURATION_MS = 360
    fun samples(): ShortArray = ShortArray(SAMPLE_RATE * DURATION_MS / 1000) { index ->
        val ms = index * 1000.0 / SAMPLE_RATE
        val local = if (ms < 160) ms else ms - 200
        if (local < 0 || local >= 160) 0 else {
            val envelope = minOf(1.0, local / 10.0, (160 - local) / 10.0)
            val frequency = if (ms < 160) 880.0 else 1320.0
            (18000 * envelope * kotlin.math.sin(2 * Math.PI * frequency * local / 1000)).toInt().toShort()
        }
    }
}
