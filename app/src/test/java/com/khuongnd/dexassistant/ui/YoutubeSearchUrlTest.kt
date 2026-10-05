package com.khuongnd.dexassistant.ui

import com.khuongnd.dexassistant.ui.actions.YoutubeSearchIntents
import com.khuongnd.dexassistant.ui.routing.LocalCommandParser
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeSearchUrlTest {
    @Test fun vietnameseQueryExtractsOriginalSlot() {
        val query = LocalCommandParser().route("Tìm nhạc Sơn Tùng").arguments.getValue("query")
        assertEquals("Sơn Tùng", query)
        assertResultsQuery(query)
    }

    @Test fun reservedCharactersStayInsideSingleQueryParameter() {
        assertResultsQuery("nhạc A&B + C# / 100% ? = Sơn Tùng")
    }

    private fun assertResultsQuery(query: String) {
        val uri = URI(YoutubeSearchIntents.url(query))
        assertEquals("https", uri.scheme)
        assertEquals("www.youtube.com", uri.host)
        assertEquals("/results", uri.path)
        assertNull(uri.fragment)
        val parameters = uri.rawQuery.split('&')
        assertEquals(1, parameters.size)
        assertEquals("search_query", parameters.single().substringBefore('='))
        assertEquals(query, URLDecoder.decode(parameters.single().substringAfter('='), StandardCharsets.UTF_8.name()))
    }
}
