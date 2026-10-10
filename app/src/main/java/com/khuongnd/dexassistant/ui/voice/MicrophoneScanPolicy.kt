package com.khuongnd.dexassistant.ui.voice

/** Sequential, bounded input exploration; activity is a heuristic, not speech recognition.
 * Never compares amplitudes from different microphones (gain differs between devices).
 */
internal class MicrophoneScanPolicy {
    private var routeSince = 0L
    private var activityUntil = 0L
    private var consecutive = 0
    private var noise = 0.001
    private var steadySince = 0L
    private var previousRms = 0.0
    private var previousCrossings = 0.0

    fun changed(now: Long) {
        routeSince = now
        activityUntil = now
        consecutive = 0
        noise = 0.001
        steadySince = now
        previousRms = 0.0
        previousCrossings = 0.0
    }

    fun observe(samples: ShortArray, count: Int, now: Long, silenced: Boolean): Boolean {
        require(count in 1..samples.size)
        if (silenced) { consecutive = 0; return now - routeSince >= DWELL_MS }
        var energy = 0.0
        var crossings = 0
        for (i in 0 until count) {
            val value = samples[i].toDouble() / 32768
            energy += value * value
            if (i > 0 && (samples[i] < 0) != (samples[i - 1] < 0)) crossings++
        }
        val rms = kotlin.math.sqrt(energy / count)
        val crossingRate = crossings.toDouble() / count
        val active = rms >= maxOf(0.008, noise * 3) && crossingRate in 0.01..0.4
        // Constant hum/tone can resemble voiced speech. Only cap stable activity;
        // variable speech keeps its hold even beyond five seconds.
        if (kotlin.math.abs(rms - previousRms) > maxOf(0.002, previousRms * 0.25) ||
            kotlin.math.abs(crossingRate - previousCrossings) > 0.03) steadySince = now
        previousRms = rms
        previousCrossings = crossingRate
        if (active && now - steadySince >= MAX_STEADY_MS) return true
        if (active) {
            consecutive++
            if (consecutive >= 3) activityUntil = now + HOLD_MS
        } else {
            consecutive = 0
            noise = noise * 0.98 + minOf(rms, 0.008) * 0.02
        }
        return now - routeSince >= DWELL_MS && now >= activityUntil && consecutive == 0
    }

    companion object {
        const val DWELL_MS = 600L
        const val HOLD_MS = 1800L
        const val MAX_STEADY_MS = 5000L
        const val SETTLE_MS = 200L
    }
}
