package com.khuongnd.dexassistant.ui

import com.khuongnd.dexassistant.ui.routing.*
import java.io.File
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CommandEngineTest {
    private val json = listOf(File("src/main/assets/commands.json"),File("app/src/main/assets/commands.json")).first { it.isFile }.readText()
    private val registry = CommandRegistry.fromJson(json)
    private val engine = LocalCommandEngine(registry)

    @Test fun resolvesBundledCatalogWithTypedOriginalSlots() {
        val cases = listOf(
            Triple("mở YouTube",ActionId.APP_LAUNCH,mapOf("app" to "youtube")),
            Triple("chia màn hình Google Maps và YouTube",ActionId.APP_PAIR_LAUNCH,mapOf("firstApp" to "maps","secondApp" to "youtube")),
            Triple("mở nhạc Sơn Tùng",ActionId.YOUTUBE_PLAY_QUERY,mapOf("query" to "Sơn Tùng")),
            Triple("đưa tôi về nhà",ActionId.NAVIGATE,mapOf("destination" to "nhà","travelMode" to "DRIVING")),
            Triple("mở youtube tìm Sơn Tùng",ActionId.YOUTUBE_SEARCH,mapOf("query" to "Sơn Tùng")),
            Triple("tìm youtube bài Nơi này có anh",ActionId.YOUTUBE_SEARCH,mapOf("query" to "Nơi này có anh")),
            Triple("phát trên youtube A&B + C#",ActionId.YOUTUBE_PLAY_QUERY,mapOf("query" to "A&B + C#")),
            Triple("chỉ đường đến 12 Nguyễn Trãi",ActionId.NAVIGATE,mapOf("destination" to "12 Nguyễn Trãi","travelMode" to "DRIVING")),
            Triple("đi bộ đến Nhà hát Lớn",ActionId.NAVIGATE,mapOf("destination" to "Nhà hát Lớn","travelMode" to "WALKING")),
            Triple("tìm địa điểm gần đây quán cà phê",ActionId.SEARCH_PLACE,mapOf("query" to "gần đây quán cà phê")),
            Triple("gọi cho Nguyễn Văn A",ActionId.CALL_CONTACT,mapOf("contact" to "Nguyễn Văn A")),
            Triple("gọi số không chín một hai ba bốn",ActionId.CALL_NUMBER,mapOf("number" to "091234")),
            Triple("tìm trên google Việt Nam",ActionId.WEB_SEARCH,mapOf("query" to "Việt Nam")),
            Triple("bấm Tiếp tục",ActionId.CLICK_TEXT,mapOf("text" to "Tiếp tục")),
            Triple("cuộn xuống",ActionId.SCROLL,mapOf("direction" to "FORWARD")),
        )
        cases.forEach { (text,action,params) ->
            val result = engine.resolve(text) ?: error(text)
            assertEquals(text, action,result.typedAction.action); assertEquals(text,params,result.typedAction.params)
            assertEquals(1,result.configVersion)
        }
        assertEquals("Sơn Tùng!",engine.resolve("Hãy tìm nhạc Sơn Tùng!")!!.typedAction.string("query"))
        assertEquals("youtube",engine.resolve("mo you tube")!!.typedAction.string("app"))
        assertEquals(MatchQuality.EXACT,engine.resolve("mở YouTube")!!.matchQuality)
        assertEquals(MatchQuality.ACCENT_FOLDED,engine.resolve("mo youtube")!!.matchQuality)
    }
    @Test fun scopedFeedAndMediaDoNotConflict() {
        assertEquals(ActionId.FEED_NEXT,engine.resolve("video tiếp theo","tiktok")!!.typedAction.action)
        assertEquals(ActionId.MEDIA_TRANSPORT,engine.resolve("video tiếp theo","youtube")!!.typedAction.action)
        assertNull(engine.resolve("tua tới 30 giây"))
        assertEquals(120,engine.resolve("tua tới hai phút","youtube")!!.typedAction.int("seconds"))
        assertNull(engine.resolve("tua tới 61 phút","youtube"))
    }
    @Test fun explicitYoutubeAndMapsCommandsIgnoreWhichSplitWindowHasFocus() {
        for (focused in listOf("youtube","maps")) {
            assertEquals(ActionId.YOUTUBE_PLAY_QUERY,engine.resolve("mở nhạc Sơn Tùng",focused)!!.typedAction.action)
            assertEquals("Sơn Tùng",engine.resolve("mở nhạc Sơn Tùng",focused)!!.typedAction.string("query"))
            assertEquals(ActionId.NAVIGATE,engine.resolve("đưa tôi về nhà",focused)!!.typedAction.action)
        }
    }
    @Test fun nearbyEntertainmentSearchUsesChromeOnlyWhenChromeIsForeground() {
        for (focused in listOf("youtube", "maps")) {
            val mapsSearch = engine.resolve("tìm địa điểm vui chơi gần đây", focused)!!
            assertEquals(ActionId.SEARCH_PLACE, mapsSearch.typedAction.action)
            assertEquals("vui chơi gần đây", mapsSearch.typedAction.string("query"))
        }
        val chromeSearch = engine.resolve("tìm địa điểm vui chơi gần đây", "chrome")!!
        assertEquals(ActionId.WEB_SEARCH, chromeSearch.typedAction.action)
        assertEquals("web_search_in_chrome", chromeSearch.commandId)
        assertEquals("vui chơi gần đây", chromeSearch.typedAction.string("query"))
    }
    @Test fun onlyNamedTypedAppPairMayUseCompoundConnector() {
        assertEquals(ActionId.APP_PAIR_LAUNCH,engine.resolve("chia màn hình Google Maps và YouTube")!!.typedAction.action)
        assertNull(engine.resolve("mở nhạc Sơn Tùng và mở Chrome"))
        val root = JSONObject(json)
        val unsafe = JSONObject("""{"id":"unsafe_compound","action":"WEB_SEARCH","allowCompound":true,"patterns":["tìm {query}"],"slots":{"query":{"type":"TEXT","required":true}}}""")
        root.getJSONArray("commands").put(unsafe)
        assertTrue(runCatching { CommandRegistry.fromJson(root.toString()) }.isFailure)
    }
    @Test fun noPrefixDispatchNegationCompoundNumericOrAmbiguousRecipientGuess() {
        listOf("tôi đang xem youtube", "mở youtube rồi mở chrome", "đừng mở youtube", "mở youtube và tìm google mèo",
            "bấm số abc", "bấm số 2", "gọi số một hai đôi ba", "gọi số 12", "tìm video mật khẩu trên youtube",
            "chọn số hai", "xác nhận", "mở ứng dụng lạ").forEach { assertNull(it,engine.resolve(it)) }
    }
    @Test fun interactionsMatchEntireUtteranceAndHaveNoAction() {
        assertEquals(InteractionReply.Confirm,registry.interactions.match("Xác nhận"))
        assertEquals(InteractionReply.Select(2),registry.interactions.match("chọn số hai"))
        assertEquals(InteractionReply.Cancel,registry.interactions.match("hủy bỏ"))
        assertEquals(InteractionReply.Next,registry.interactions.match("đọc lựa chọn tiếp theo"))
        assertEquals(InteractionReply.Previous,registry.interactions.match("lựa chọn trước"))
        assertNull(registry.interactions.match("xác nhận rồi gọi"))
        assertNull(registry.interactions.match("chọn số 0"))
        assertNull(registry.interactions.match("tôi xác nhận"))
        assertNull(registry.interactions.match("hãy xác nhận"))
        assertNull(registry.interactions.match("xác nhận đi"))
        assertNull(registry.interactions.match("hãy đọc lựa chọn tiếp theo"))
        assertEquals(InteractionReply.Select(25),registry.interactions.match("chọn số hai mươi lăm"))
    }
    @Test fun invalidReloadPreservesLastGoodRegistry() {
        val holder = CommandRegistryHolder(registry)
        assertFalse(holder.reload("{}")); assertSame(registry,holder.current)
        assertTrue(holder.reload(JSONObject(json).put("configVersion",2).toString()))
        assertEquals(2,holder.current.configVersion)
    }
    @Test fun strictCatalogRejectsFieldsTypesVersionsAliasesAndConflicts() {
        fun rejects(change: (JSONObject)->Unit) {
            val root = JSONObject(json); change(root)
            assertTrue(runCatching { CommandRegistry.fromJson(root.toString()) }.isFailure)
        }
        rejects { it.put("script","anything") }
        rejects { it.put("schemaVersion",2) }
        rejects { it.put("schemaVersion","1") }
        rejects { it.put("minimumAppVersion",2) }
        rejects { it.getJSONArray("apps").getJSONObject(1).getJSONArray("aliases").put("you tube") }
        rejects { it.getJSONArray("apps").getJSONObject(1).getJSONArray("aliases").put("yóutube") }
        rejects { it.getJSONArray("commands").getJSONObject(0).put("action","RUN_SCRIPT") }
        rejects { it.getJSONArray("commands").getJSONObject(0).getJSONObject("slots").getJSONObject("app").put("type","EXPRESSION") }
        rejects { it.getJSONArray("commands").getJSONObject(0).getJSONArray("patterns").put("mở {missing}") }
        rejects { it.getJSONArray("commands").getJSONObject(0).getJSONArray("patterns").put("mở {app}") }
        rejects { it.getJSONArray("commands").getJSONObject(0).put("args","invalid") }
        rejects { it.getJSONArray("commands").getJSONObject(0).put("confirmed",true) }
        rejects { root -> root.getJSONArray("commands").let { commands -> (0 until commands.length()).map(commands::getJSONObject).first { it.getString("id") == "wifi_on" }.getJSONObject("args").put("enabled","true") } }
    }
    @Test fun equalSpecificityDifferentTargetsStayAmbiguous() {
        val root = JSONObject(json)
        root.getJSONArray("commands").put(JSONObject("""{"id":"ambiguous","action":"CALL_CONTACT","patterns":["tìm nhạc {contact}"],"slots":{"contact":{"type":"TEXT","required":true}},"scope":{"apps":["youtube"]}}"""))
        root.getJSONArray("commands").put(JSONObject("""{"id":"ambiguous_other","action":"WEB_SEARCH","patterns":["tìm nhạc {query}"],"slots":{"query":{"type":"TEXT","required":true}},"scope":{"apps":["youtube","chrome"]}}"""))
        assertNull(LocalCommandEngine(CommandRegistry.fromJson(root.toString())).resolve("tìm nhạc A","youtube"))
    }
    @Test fun remoteValidationRejectsUnknownParamsPrimitiveCoercionAndLimits() {
        listOf(
            "CALL_NUMBER" to mapOf("number" to "12"),
            "CALL_CONTACT" to mapOf("contact" to "A","confirmed" to true),
            "WIFI_SET" to mapOf("enabled" to "true"),
            "VOLUME_SET" to mapOf("operation" to "SET","percent" to 101),
            "VOLUME_SET" to mapOf("operation" to "MUTE","percent" to 50),
            "MEDIA_SEEK" to mapOf("operation" to "FORWARD","seconds" to 3601),
            "YOUTUBE_SELECT" to mapOf("index" to 1,"title" to "A"),
        ).forEach { (action,params) -> assertTrue(action,runCatching { ActionCatalog.validate(action,params) }.isFailure) }
    }
    @Test fun freeSlotsPreserveSymbolsAndMalformedNumbersNeverReachAi() {
        assertEquals("A & B?",engine.resolve("tìm youtube A & B?")!!.typedAction.string("query"))
        assertEquals("youtube",engine.resolve("mở youtube!")!!.typedAction.string("app"))
        val parser = LocalCommandParser(registry)
        assertEquals(Route.UNSUPPORTED,parser.route("gọi số một nghìn").route)
        assertEquals("AMBIGUOUS_PHONE_NUMBER",parser.route("gọi số một nghìn").arguments["reason"])
        assertEquals(Route.UNSUPPORTED,parser.route("xác nhận").route)
        assertEquals(Route.UNSUPPORTED,parser.route("chọn số hai").route)
        assertNull(engine.resolve("tua tới 71582789 phút", "youtube"))
    }
    @Test fun lazyProjectionOffsetsPreserveMixedUnicodeSlotsAndLiteralPunctuation() {
        assertEquals("Sơn Tùng 🎵 café?", engine.resolve("tìm youtube Sơn Tùng 🎵 café?")!!.typedAction.string("query"))
        assertEquals("Sơn Tùng", engine.resolve("tim youtube So\u031bn Tu\u0300ng")!!.typedAction.string("query"))
        assertEquals("youtube", engine.resolve("mở youtube!")!!.typedAction.string("app"))
        assertNull(engine.resolve("đây là một câu không thuộc catalog 🎵"))
    }

}
