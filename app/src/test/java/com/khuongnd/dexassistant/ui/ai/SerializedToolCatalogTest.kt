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
        val first = cache.forScope(actions, apps)
        assertSame(first, cache.forScope(actions, apps))
        actions.remove(ActionId.VOLUME_SET)
        val fewer = cache.forScope(actions, apps)
        assertNotSame(first, fewer)
        assertFalse(fewer.contains("deviceCommand_VOLUME_SET"))
        apps.add("maps")
        val changedApps = cache.forScope(actions, apps)
        assertNotSame(fewer, changedApps)
        assertTrue(changedApps.contains("maps"))
        assertFalse("Old immutable snapshot must not change", first.contains("maps"))
    }
    @Test(expected = IllegalArgumentException::class)
    fun emptyScopeCannotAdvertiseTools() {
        SerializedToolCatalog().forScope(setOf(ActionId.CLICK_TARGET), emptySet())
    }
}
