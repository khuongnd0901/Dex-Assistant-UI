package com.khuongnd.dexassistant.ui.latency

/** One immutable correlation identity. Origin is resolved once; executor path may change. */
class RequestLatency(val requestId: String, private val emit: (String) -> Unit) {
    @Volatile var path = "local"
    @Volatile var origin: String? = null
        private set
    private var closed = false
    private val intervals = CommandLatency { name, micros -> metric(name, micros) }
    @Synchronized fun resolveOrigin(value: String) { if (origin == null) origin = value }
    @Synchronized fun stage(name: String, epoch: Long? = null) {
        if (!closed) emit(identity() + " stage=$name" + (epoch?.let { " epoch=$it" } ?: ""))
    }
    @Synchronized fun metric(name: String, micros: Long) {
        if (!closed && micros >= 0) emit(identity() + " metric=$name durationUs=$micros")
    }
    @Synchronized fun start(name: String, atNanos: Long) { if (!closed) intervals.start(name, atNanos) }
    @Synchronized fun end(name: String, atNanos: Long) { if (!closed) intervals.end(name, atNanos) }
    @Synchronized fun terminal(reason: String) {
        if (closed) return
        val safe = if (reason.matches(Regex("[A-Z][A-Z0-9_]{0,63}"))) reason else "OTHER"
        emit(identity() + " terminalReason=$safe")
        closed = true
        intervals.clear()
    }
    private fun identity() = "requestId=$requestId path=$path" + (origin?.let { " origin=$it" } ?: "")
}
