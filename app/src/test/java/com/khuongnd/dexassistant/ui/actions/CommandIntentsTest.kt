package com.khuongnd.dexassistant.ui.actions

import java.net.URI
import java.net.URLDecoder
import org.junit.Assert.*
import org.junit.Test

class CommandIntentsTest {
    private val query = "Nhà A&B / C# + 100% ? = Hồ Chí Minh"
    private fun decoded(value: String) = URLDecoder.decode(value, "UTF-8")
    @Test fun googleSearchHasOneEncodedQuery() {
        val uri = URI(CommandIntents.webUrl(query))
        assertEquals("www.google.com", uri.host)
        assertNull(uri.fragment)
        assertEquals(1, uri.rawQuery.split('&').size)
        assertEquals(query, decoded(uri.rawQuery.substringAfter('=')))
    }
    @Test fun mapsFallbackPreservesDestinationAndWalkingMode() {
        val uri = URI(CommandIntents.navigationUrl(query, "WALKING", false))
        val params = uri.rawQuery.split('&').associate { it.substringBefore('=') to decoded(it.substringAfter('=')) }
        assertEquals(query, params["destination"])
        assertEquals("walking", params["travelmode"])
        assertNull(uri.fragment)
        assertEquals(3, params.size)
    }
    @Test fun mapsIntentUsesExplicitDrivingMode() {
        val uri = CommandIntents.navigationUrl(query, "DRIVING", true)
        assertTrue(uri.startsWith("google.navigation:q="))
        assertEquals(2, uri.substringAfter('?').split('&').size)
        assertEquals(query, decoded(uri.substringAfter("q=").substringBefore('&')))
        assertTrue(uri.endsWith("&mode=d"))
    }
    @Test fun searchPlaceCannotInjectNavigationParameters() {
        assertEquals(query, decoded(CommandIntents.placeUrl(query, true).substringAfter("?q=")))
        assertEquals(1, CommandIntents.placeUrl(query, true).substringAfter('?').split('&').size)
    }
    @Test(expected = IllegalArgumentException::class) fun unknownTravelModeRejected() {
        CommandIntents.navigationUrl("nhà", "SCRIPT", true)
    }
}
