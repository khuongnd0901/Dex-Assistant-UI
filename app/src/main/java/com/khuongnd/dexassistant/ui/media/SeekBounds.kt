package com.khuongnd.dexassistant.ui.media

object SeekBounds {
    fun target(position: Long, elapsedMs: Long, speed: Float, playing: Boolean,
        duration: Long?, seconds: Int, forward: Boolean): Long {
        require(position >= 0 && seconds in 1..3600 && speed.isFinite())
        val current = position + if (playing) (elapsedMs.coerceAtLeast(0) * speed).toLong() else 0L
        val result = (current + seconds * 1000L * if (forward) 1 else -1).coerceAtLeast(0)
        return if (duration != null && duration > 0) result.coerceAtMost(duration) else result
    }
}
