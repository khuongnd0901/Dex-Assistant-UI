package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.routing.ActionId
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class SerializedToolCatalogTest {
    @Test fun cachesImmutableSchemasOnlyWhileCapabilityAndAppScopesMatch() {
        val cache = SerializedToolCatalog()
        val actions = mutableSetOf(ActionId.APP_LAUNCH, ActionId.VOLUME_SET)
        val apps = mutableSetOf("chrome")
        val first = cache.forScope(actions, apps, true)
        assertSame(first, cache.forScope(actions, apps, true))
        actions.remove(ActionId.VOLUME_SET)
        val fewer = cache.forScope(actions, apps, true)
        assertNotSame(first, fewer)
        assertFalse(fewer.json.contains("deviceCommand_VOLUME_SET"))
        apps.add("maps")
        val changedApps = cache.forScope(actions, apps, true)
        assertNotSame(fewer, changedApps)
        assertTrue(changedApps.json.contains("maps"))
        assertFalse("Old immutable snapshot must not change", first.json.contains("maps"))
    }
    @Test fun providerFormatsAndEmptyScopeRemainFailClosed() {
        val cache = SerializedToolCatalog()
        val actions = setOf(ActionId.VOLUME_SET, ActionId.CLICK_TARGET)
        val gemini = cache.forScope(actions, emptySet(), true)
        val groq = cache.forScope(actions, emptySet(), false)
        assertEquals(1, gemini.count); assertEquals(1, groq.count)
        assertTrue(JSONArray(gemini.json).getJSONObject(0).has("parameters"))
        assertTrue(JSONArray(groq.json).getJSONObject(0).has("function"))
        assertEquals(0, cache.forScope(setOf(ActionId.CLICK_TARGET), emptySet(), true).count)
    }
}
