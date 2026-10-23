package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.routing.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SubscriptionToolSchemaTest {
    private val supported = setOf(ActionId.VOLUME_SET, ActionId.NAVIGATE)
    @Test fun schemaContainsOnlyScopedNativeFunctions() {
        val tools = AiToolSchema.schemas(supported, emptySet())
        assertEquals(2, tools.size)
        assertTrue(tools.none { it.toString().contains("WIFI_SET") || it.has("function") })
        assertTrue(tools.all { it.getString("type") == "function" && it.has("parameters") })
        assertTrue(AiToolSchema.schemas(setOf(ActionId.CLICK_TARGET), emptySet()).isEmpty())
        assertTrue(AiToolSchema.schemas(setOf(ActionId.APP_LAUNCH), emptySet()).isEmpty())
    }
    private fun call(name: String = "VOLUME_SET", args: String = "{\"operation\":\"MUTE\"}") =
        JSONObject().put("name", "deviceCommand_" + name).put("args", JSONObject(args))
    @Test fun validatesBoundedTypedPlans() {
        val registry = CommandRegistry.bundledForTests()
        assertEquals("MUTE", AiToolCallValidator.validate(listOf(call()), registry, supported).single().params["operation"])
        for (calls in listOf(emptyList(), List(9) { call() }, listOf(call("WIFI_SET", "{\"enabled\":true}")),
            listOf(call(args = "{\"operation\":\"MUTE\",\"percent\":300}")))) {
            try { AiToolCallValidator.validate(calls, registry, supported); fail("Unsafe plan accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }
}
