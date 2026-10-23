package com.khuongnd.dexassistant.ui

import android.os.Debug
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.latency.RequestLatency
import com.khuongnd.dexassistant.ui.routing.ActionId
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import com.khuongnd.dexassistant.ui.voice.CommandRecognizer
import com.khuongnd.dexassistant.ui.voice.SherpaWakeEngine
import okhttp3.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/** Synthetic offline AVD component workload. No speech, actual actions, live provider or owner prefs. */
class ResponsePerformanceDeviceTest {
    private val target = InstrumentationRegistry.getInstrumentation().targetContext
    private fun bytes() = Debug.getRuntimeStat("art.gc.bytes-allocated")?.toLongOrNull()
    private fun trial(kind: String, block: Int, trial: Int, work: () -> Unit) {
        val start = System.nanoTime(); work()
        Log.i("DexT021", "kind=$kind block=$block trial=$trial durationUs=${(System.nanoTime() - start) / 1000}")
    }
    @Test fun syntheticDiscoveryAndOfflineCommandTransport() {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            OfflineSubscriptionFixture.response(chain.request(), item = OfflineSubscriptionFixture.command)
        }.build()
        try {
            val proxy = OfflineSubscriptionFixture(client)
            val registry = CommandRegistry.load(target)
            val actions = ActionId.entries.toSet()
            repeat(30) { proxy.plan("Đặt âm lượng 30 phần trăm", registry, actions) }
            repeat(3) { block ->
                val before = bytes()
                repeat(120) { trial("subscriptionCommandTransport", block, it) {
                    val trace = RequestLatency(UUID.randomUUID().toString()) { Log.i("DexLatency", it) }
                    trace.path = "direct"; trace.resolveOrigin("direct"); trace.stage("admitted")
                    assertEquals(ActionId.VOLUME_SET, proxy.plan("Đặt âm lượng 30 phần trăm", registry, actions, trace::metric).single().action)
                    trace.terminal("PLANNED")
                } }
                Log.i("DexT021", "kind=subscriptionCommandTransport block=$block samples=120 allocatedBytes=${bytes()?.let { after -> before?.let { after - it } } ?: -1}")
                repeat(120) { trial("recognizerDiscovery", block, it) { CommandRecognizer.component(target) } }
            }
        } finally {
            client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll()
        }
    }
    @Test fun syntheticWarmNativeWakeFrames() {
        SherpaWakeEngine(target.assets).use { engine ->
            val samples = ShortArray(320) { ((it * 17) % 101 - 50).toShort() }
            repeat(100) { engine.accept(samples, samples.size) }
            repeat(3) { block ->
                engine.resetSession()
                val before = bytes()
                repeat(500) { trial("wakeFrame", block, it) { assertFalse(engine.accept(samples, samples.size)) } }
                Log.i("DexT021", "kind=wakeFrame block=$block samples=500 allocatedBytes=${bytes()?.let { after -> before?.let { after - it } } ?: -1}")
            }
        }
    }
}
