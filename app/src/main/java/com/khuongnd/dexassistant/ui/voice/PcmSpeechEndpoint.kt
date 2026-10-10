package com.khuongnd.dexassistant.ui.voice

/** PCM-source EOF guard, not a speech recognizer. Service final/segment remains authoritative. */
internal class PcmSpeechEndpoint {
    private var activeFrames = 0
    private var heardActivity = false
    private var silentSamples = 0
    fun shouldEnd(samples: ShortArray, count: Int, millisecondsSinceReady: Long): Boolean {
        require(count in 1..samples.size)
        if (millisecondsSinceReady < 450) return false // Exclude the short ready cue.
        var energy = 0.0
        for (i in 0 until count) energy += samples[i].toDouble() * samples[i]
        val rms = kotlin.math.sqrt(energy / count) / 32768
        if (rms >= 0.008) {
            activeFrames++
            if (activeFrames >= 3) heardActivity = true
            silentSamples = 0
        } else { activeFrames = 0; if (heardActivity) silentSamples += count }
        return heardActivity && silentSamples >= 24_000 && millisecondsSinceReady >= 3000
    }
}
