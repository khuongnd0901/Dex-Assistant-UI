package com.khuongnd.dexassistant.ui.ai

import okhttp3.ResponseBody

/** Sanitized monotonic transport intervals; no provider URL, headers or payload are observed. */
internal class ProviderTiming(private val emit: (String, Long) -> Unit) {
    private val requestStarted = System.nanoTime()
    private var firstByteAt: Long? = null
    private var requestEnded = false
    fun headers() {
        if (requestEnded) return
        requestEnded = true
        elapsed("providerRequestLatency", requestStarted)
    }
    fun body(body: ResponseBody, read: () -> String): String {
        if (body.source().request(1)) {
            firstByteAt = System.nanoTime()
            elapsed("providerFirstByteLatency", requestStarted, firstByteAt!!)
        }
        val started = firstByteAt ?: System.nanoTime()
        return try { read() } finally { elapsed("providerBodyLatency", started) }
    }
    companion object {
        fun <T> parse(emit: (String, Long) -> Unit, parse: () -> T): T {
            val started = System.nanoTime()
            return try { parse() } finally {
                emit("providerParseLatency", (System.nanoTime() - started) / 1_000)
            }
        }
    }
    private fun elapsed(name: String, started: Long, ended: Long = System.nanoTime()) {
        if (ended >= started) emit(name, (ended - started) / 1_000)
    }
}
