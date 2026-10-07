package com.khuongnd.dexassistant.ui.latency

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandLatencyTest {
    @Test fun recordsOnlyPairedMonotonicIntervalsAndConsumesStartOnce() {
        val values = mutableListOf<Pair<String, Long>>()
        val latency = CommandLatency { name, micros -> values += name to micros }
        latency.end("sttLatency", 9_000_000)
        latency.start("sttLatency", 10_000_000)
        latency.end("sttLatency", 9_000_000)
        latency.end("sttLatency", 12_900_000)
        latency.end("sttLatency", 20_000_000)
        latency.start("wakeLatency", 20_000_000)
        latency.clear()
        latency.end("wakeLatency", 25_000_000)
        assertEquals(listOf("sttLatency" to 2900L), values)
    }
}
