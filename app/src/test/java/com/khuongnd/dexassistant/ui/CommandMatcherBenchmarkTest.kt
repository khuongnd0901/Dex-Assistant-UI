package com.khuongnd.dexassistant.ui

import com.khuongnd.dexassistant.ui.routing.*
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Diagnostic host timing; the acceptance threshold is measured by ConfigCommandDeviceTest on Fold3. */
class CommandMatcherBenchmarkTest {
    @Test fun reportWarmP95Across1000SamePrefixPatterns() {
        val bundled = listOf(File("src/main/assets/commands.json"),File("app/src/main/assets/commands.json")).first { it.isFile }.readText()
        val config = JSONObject(bundled).put("commands",JSONArray().put(JSONObject()
            .put("id","benchmark").put("action","WEB_SEARCH")
            .put("patterns",JSONArray((0 until 1000).map { "mẫu $it {query}" }))
            .put("slots",JSONObject().put("query",JSONObject().put("type","TEXT").put("required",true)))))
        val engine = LocalCommandEngine(CommandRegistry.fromJson(config.toString()))
        val registry = CommandRegistry.fromJson(config.toString())
        assertEquals(1,registry.candidatesFor("mau 999 son tung").size)
        repeat(200) { assertNotNull(engine.resolve("mẫu 999 Sơn Tùng")) }
        val samples = LongArray(300) {
            val start = System.nanoTime()
            val resolved = engine.resolve("mẫu 999 Sơn Tùng")
            val duration = System.nanoTime()-start
            assertEquals("Sơn Tùng",resolved!!.typedAction.string("query"))
            duration
        }.sorted()
        println("T011 host patterns=1000 samples=300 p95Nanos=${samples[284]} deviceAcceptance=false")
    }
}
