package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.routing.ActionId
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import com.khuongnd.dexassistant.ui.security.AiProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileAiProxyTest {
    private val supported = setOf(ActionId.VOLUME_SET, ActionId.NAVIGATE)

    @Test
    fun providerSchemasContainOnlySupportedActions() {
        for (gemini in listOf(true, false)) {
            val tools = AiToolSchema.schemas(supported, emptySet(), gemini)
            assertEquals(2, tools.size)
            assertTrue(tools.none { it.toString().contains("WIFI_SET") })
            assertEquals(0, AiToolSchema.schemas(setOf(ActionId.CLICK_TARGET), emptySet(), gemini).size)
        }
    }

    @Test
    fun geminiToolCallBecomesValidatedTypedAction() {
        val response = JSONObject("""{"candidates":[{"content":{"parts":[{"functionCall":{"name":"deviceCommand_VOLUME_SET","args":{"operation":"MUTE"}}}]}}]}""")
        val result = AiToolCallParser.parse(AiProvider.GEMINI, response, CommandRegistry.bundledForTests(), supported)
        assertEquals(1, result.size)
        assertEquals(ActionId.VOLUME_SET, result.first().action)
        assertEquals("MUTE", result.first().params["operation"])
    }

    @Test
    fun groqToolCallBecomesValidatedTypedAction() {
        val response = JSONObject("""{"choices":[{"message":{"tool_calls":[{"type":"function","function":{"name":"deviceCommand_VOLUME_SET","arguments":"{\"operation\":\"MUTE\"}"}}]}}]}""")
        val result = AiToolCallParser.parse(AiProvider.GROQ, response, CommandRegistry.bundledForTests(), supported)
        assertEquals(ActionId.VOLUME_SET, result.single().action)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedFunctionNeverExecutes() {
        val response = JSONObject("""{"candidates":[{"content":{"parts":[{"functionCall":{"name":"deviceCommand_WIFI_SET","args":{"enabled":true}}}]}}]}""")
        AiToolCallParser.parse(AiProvider.GEMINI, response, CommandRegistry.bundledForTests(), supported)
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedParamsAreRejected() {
        val response = JSONObject("""{"candidates":[{"content":{"parts":[{"functionCall":{"name":"deviceCommand_VOLUME_SET","args":{"operation":"MUTE","percent":300}}}]}}]}""")
        AiToolCallParser.parse(AiProvider.GEMINI, response, CommandRegistry.bundledForTests(), supported)
    }

    @Test(expected = IllegalArgumentException::class)
    fun noFunctionCallsMustNotBecomeTextCommands() {
        val response = JSONObject("""{"candidates":[{"content":{"parts":[{"text":"Please tap the screen"}]}}]}""")
        AiToolCallParser.parse(AiProvider.GEMINI, response, CommandRegistry.bundledForTests(), supported)
    }
}
