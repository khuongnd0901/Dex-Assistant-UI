package com.khuongnd.dexassistant.ui.latency

/** Per-request monotonic intervals. Missing endpoints never become fabricated durations. */
class CommandLatency(private val emit: (String, Long) -> Unit) {
    private val starts = mutableMapOf<String, Long>()

    fun start(name: String, atNanos: Long) { starts[name] = atNanos }

    fun end(name: String, atNanos: Long) {
        val start = starts[name] ?: return
        if (atNanos >= start) {
            starts.remove(name)
            emit(name, (atNanos - start) / 1_000)
        }
    }

    fun clear() { starts.clear() }
}
