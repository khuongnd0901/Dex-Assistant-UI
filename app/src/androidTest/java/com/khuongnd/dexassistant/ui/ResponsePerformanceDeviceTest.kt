package com.khuongnd.dexassistant.ui

import android.content.Context
import android.content.ContextWrapper
import android.os.Debug
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.ai.MobileAiProxy
import com.khuongnd.dexassistant.ui.ai.DirectAiChat
import com.khuongnd.dexassistant.ui.ai.DirectChatState
import com.khuongnd.dexassistant.ui.latency.RequestLatency
import com.khuongnd.dexassistant.ui.routing.ActionId
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import com.khuongnd.dexassistant.ui.security.*
import com.khuongnd.dexassistant.ui.voice.CommandRecognizer
import com.khuongnd.dexassistant.ui.voice.SherpaWakeEngine
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
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
        val scope = "t021-perf-${UUID.randomUUID()}-"
        val names = mutableSetOf<String>()
        val fixture = object : ContextWrapper(target) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                names += name
                return target.getSharedPreferences(scope + name, mode)
            }
        }
        val credentials = AiCredentialStore(fixture, scope + "key-")
        val settings = AiProviderSettings(fixture)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("fixture")
                .body("""{"candidates":[{"content":{"parts":[{"functionCall":{"name":"deviceCommand_VOLUME_SET","args":{"operation":"SET","percent":30}}}]}}]}""".toResponseBody()).build()
        }.build()
        try {
            AiProvider.entries.forEach { settings.setEnabled(it, false) }
            settings.setDefaultProvider(AiProvider.GEMINI)
            settings.setModel(AiProvider.GEMINI, "offline-benchmark-model")
            settings.setFreeTierConfirmed(AiProvider.GEMINI, true)
            settings.setEnabled(AiProvider.GEMINI, true)
            credentials.save(AiProvider.GEMINI, "offline-fixture-placeholder")
            val proxy = MobileAiProxy(credentials, settings, client)
            val registry = CommandRegistry.load(fixture)
            val actions = ActionId.entries.toSet()
            repeat(30) { proxy.plan("Đặt âm lượng 30 phần trăm", registry, actions) }
            repeat(3) { block ->
                val before = bytes()
                repeat(120) { trial("commandTransport", block, it) {
                    val trace = RequestLatency(UUID.randomUUID().toString()) { Log.i("DexLatency", it) }
                    trace.path = "direct"; trace.resolveOrigin("direct"); trace.stage("admitted")
                    assertEquals(ActionId.VOLUME_SET, proxy.plan("Đặt âm lượng 30 phần trăm", registry, actions, trace::metric).single().action)
                    trace.terminal("PLANNED")
                } }
                Log.i("DexT021", "kind=commandTransport block=$block samples=120 allocatedBytes=${bytes()?.let { after -> before?.let { after - it } } ?: -1}")
                repeat(120) { trial("recognizerDiscovery", block, it) { CommandRecognizer.component(target) } }
            }
        } finally {
            credentials.delete(AiProvider.GEMINI)
            names.forEach { target.getSharedPreferences(scope + it, 0).edit().clear().commit() }
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
