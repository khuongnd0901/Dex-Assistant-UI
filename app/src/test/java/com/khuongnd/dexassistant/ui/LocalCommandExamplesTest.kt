package com.khuongnd.dexassistant.ui

import com.khuongnd.dexassistant.ui.routing.ActionId
import com.khuongnd.dexassistant.ui.execution.ActionRouter
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import com.khuongnd.dexassistant.ui.routing.LocalCommandParser
import com.khuongnd.dexassistant.ui.routing.Route
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import org.json.JSONObject

class LocalCommandExamplesTest {
    private val json = listOf(File("src/main/assets/commands.json"), File("app/src/main/assets/commands.json")).first { it.isFile }.readText()
    private val supported = ActionRouter.supportedActionIds().map(ActionId::name).toSet()

    @Test fun allDisplayedSamplesMatchSupportedCatalogCommands() {
        val examples = LocalCommandExamples.fromJson(json, supported)
        val registry = CommandRegistry.fromJson(json)
        val parser = LocalCommandParser(registry)
        val catalog = JSONObject(json).getJSONArray("commands")
        val supportedDefinitions = (0 until catalog.length()).map { catalog.getJSONObject(it) }.filter { it.getString("action") in supported }
        val expectedPatternCount = supportedDefinitions.sumOf { it.getJSONArray("patterns").length() }
        assertEquals(supportedDefinitions.size, examples.size)
        assertEquals(expectedPatternCount, examples.sumOf { it.phrases.size })
        assertEquals(LocalCommandExamples.groupOrder.toSet(), examples.map { it.group }.toSet())
        assertTrue(examples.map { it.id }.containsAll(LocalCommandExamples.featuredIds))
        assertFalse(examples.any { item -> item.phrases.any { '{' in it || '}' in it } || item.id.startsWith("bluetooth") })
        examples.forEach { example ->
            val app = when (example.id) {
                "web_search_in_chrome" -> "chrome"
                "feed_next", "feed_previous" -> "tiktok"
                else -> if (example.id.startsWith("youtube_") || example.id.startsWith("seek_")) "youtube" else null
            }
            example.phrases.forEach { phrase -> assertEquals("${example.id}: $phrase", Route.LOCAL, parser.route(phrase, app).route) }
        }
    }

    @Test fun searchFindsAliasesAndGroupNamesWithoutDroppingOtherPatterns() {
        val examples = LocalCommandExamples.fromJson(json, supported)
        assertTrue(LocalCommandExamples.search(examples, "cửa sổ").any { it.id == "open_app" })
        assertTrue(LocalCommandExamples.search(examples, "khởi động").any { it.id == "open_app" })
        assertTrue(LocalCommandExamples.search(examples, "am luong").any { it.id == "volume_percent" })
        assertTrue(LocalCommandExamples.search(examples, "ung dung").any { it.id == "open_app" })
        assertEquals(examples, LocalCommandExamples.search(examples, "  "))
    }
}
