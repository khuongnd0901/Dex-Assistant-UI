package com.khuongnd.dexassistant.ui.protocol

import com.khuongnd.dexassistant.ui.routing.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DeviceCommandProtocolTest {
    private val registry = CommandRegistry.bundledForTests()
    private fun action(arguments: JSONObject, tool: String = "DEVICE_COMMAND", mode: String = "REMOTE") =
        DeviceAction("plan","action",tool,mode,arguments)
    private fun arguments() = JSONObject("""{"actionSchemaVersion":1,"action":"CALL_CONTACT","params":{"contact":"Nguyễn Văn A"}}""")
    private fun decode(arguments: JSONObject) = DeviceCommandProtocol.decode(action(arguments),registry,ActionId.entries.toSet())
    private fun semantic(target: JSONObject) = JSONObject().put("actionSchemaVersion",2).put("action","CLICK_TARGET").put("params",JSONObject().put("target",target))
    private fun decodeSemantic(arguments: JSONObject) = DeviceCommandProtocol.decode(action(arguments),registry,ActionId.entries.toSet(),2)
    @Test fun closeWindowRequiresSchemaTwoConfiguredAppAndExplicitCapability() {
        val args = JSONObject("""{"actionSchemaVersion":2,"action":"APP_CLOSE_WINDOW","params":{"app":"youtube"}}""")
        assertEquals(ActionId.APP_CLOSE_WINDOW, decodeSemantic(args).action)
        assertEquals("youtube", decodeSemantic(args).string("app"))
        assertTrue(runCatching { decode(args) }.isFailure)
        assertTrue(runCatching { DeviceCommandProtocol.decode(action(args),registry,
            ActionId.entries.toSet() - ActionCatalog.schemaTwoOnlyActions,2) }.isFailure)
        assertTrue(runCatching { DeviceCommandProtocol.capabilities(registry,setOf(ActionId.APP_CLOSE_WINDOW)) }.isFailure)
        assertTrue(runCatching { decodeSemantic(JSONObject(args.toString()).put("params",JSONObject().put("app","unconfigured"))) }.isFailure)
        assertTrue(runCatching { ActionCatalog.validate("APP_CLOSE_WINDOW",mapOf("app" to "youtube")) }.isFailure)
        assertTrue(runCatching { ActionCatalog.validate("APP_CLOSE_WINDOW",mapOf("app" to "youtube","forceStop" to true),2) }.isFailure)
        assertEquals(Route.UNSUPPORTED, LocalCommandParser(registry).route("tắt youtube").route)
    }
    @Test fun strictPrimitiveTypesUnknownPolicyAndUnsupportedActionReject() {
        val valid = arguments()
        assertEquals("Nguyễn Văn A",decode(valid).string("contact"))
        listOf(
            arguments().put("confirmed",true),
            arguments().put("actionSchemaVersion","1"),
            arguments().put("actionSchemaVersion",1.0),
            arguments().put("actionSchemaVersion",2),
            arguments().put("params",JSONObject().put("contact","A").put("confirmed",true)),
            arguments().put("params",JSONObject().put("contact",JSONObject.NULL)),
            arguments().put("action","APP_LAUNCH").put("params",JSONObject().put("app","unconfigured")),
            arguments().put("action","VOLUME_SET").put("params",JSONObject().put("operation","SET").put("percent",40.0)),
            arguments().put("action","RUN_SCRIPT"),
        ).forEach { assertTrue(runCatching { decode(it) }.isFailure) }
        assertTrue(runCatching { DeviceCommandProtocol.decode(action(valid),registry,setOf(ActionId.WEB_SEARCH)) }.isFailure)
        assertTrue(runCatching { DeviceCommandProtocol.decode(action(valid,mode="DUMMY"),registry,ActionId.entries.toSet()) }.isFailure)
    }
    @Test fun legacyToolsAlsoResolveIntoValidatedTypedActions() {
        val app = DeviceCommandProtocol.decode(action(JSONObject().put("packageName","com.google.android.youtube").put("deepLink",JSONObject.NULL),"LAUNCH_APP"),registry,ActionId.entries.toSet())
        assertEquals(ActionId.APP_LAUNCH,app.action); assertEquals("youtube",app.string("app"))
        val wifi = DeviceCommandProtocol.decode(action(JSONObject().put("enabled",false),"TOGGLE_WIFI"),registry,ActionId.entries.toSet())
        assertFalse(wifi.boolean("enabled"))
        assertTrue(runCatching { DeviceCommandProtocol.decode(action(JSONObject().put("enabled","false"),"TOGGLE_WIFI"),registry,ActionId.entries.toSet()) }.isFailure)
        assertTrue(runCatching { DeviceCommandProtocol.decode(action(JSONObject().put("packageName","com.google.android.youtube").put("deepLink","javascript:alert(1)"),"LAUNCH_APP"),registry,ActionId.entries.toSet()) }.isFailure)
    }
    @Test fun descriptorContainsOnlyPublicActionAndConfiguredAppIds() {
        val descriptor = DeviceCommandProtocol.capabilities(registry,setOf(ActionId.APP_LAUNCH,ActionId.CALL_CONTACT))
        assertEquals(setOf("actionSchemaVersion","supportedActions","configuredAppIds"),descriptor.keys().asSequence().toSet())
        assertEquals(1,descriptor.getInt("actionSchemaVersion"))
        assertEquals(listOf("APP_LAUNCH","CALL_CONTACT"),(0 until descriptor.getJSONArray("supportedActions").length()).map { descriptor.getJSONArray("supportedActions").getString(it) })
        assertEquals(registry.appIds,(0 until descriptor.getJSONArray("configuredAppIds").length()).map { descriptor.getJSONArray("configuredAppIds").getString(it) }.toSet())
        assertTrue(runCatching { DeviceCommandProtocol.capabilities(registry,setOf(ActionId.CLICK_TARGET)) }.isFailure)
        val schemaTwo = DeviceCommandProtocol.capabilities(registry,setOf(ActionId.APP_LAUNCH,ActionId.CLICK_TARGET),2)
        assertEquals(2,schemaTwo.getInt("actionSchemaVersion"))
        assertTrue(schemaTwo.getJSONArray("supportedActions").toString().contains("CLICK_TARGET"))
    }
    @Test fun semanticTargetIsStrictAndNeverEnabledByCurrentCapabilities() {
        val target = JSONObject().put("appId","youtube").put("role","BUTTON").put("text","Phát")
            .put("contentDescription","Phát bài hát").put("resourceId","play_button").put("contextText","Nghệ sĩ")
            .put("ordinal",1)
        val decoded = decodeSemantic(semantic(target))
        assertEquals(ActionId.CLICK_TARGET,decoded.action)
        assertEquals("youtube",(decoded.params["target"] as Map<*, *>)["appId"])
        assertTrue(runCatching { decode(semantic(target)) }.isFailure)
        assertTrue(runCatching { DeviceCommandProtocol.decode(action(semantic(target)),registry,emptySet(),2) }.isFailure)
        assertTrue(runCatching { decodeSemantic(arguments()) }.isFailure)
        val absent = JSONObject().put("appId","youtube").put("ordinal",1)
        val invalid = listOf(
            absent, JSONObject(target.toString()).put("appId","missing_app"),
            JSONObject(target.toString()).put("appId",JSONObject.NULL),
            JSONObject(target.toString()).put("role","LINK"),
            JSONObject(target.toString()).put("text"," "),
            JSONObject(target.toString()).put("text","a".repeat(201)),
            JSONObject(target.toString()).put("text","bad\ntext"),
            JSONObject(target.toString()).put("ordinal",0),
            JSONObject(target.toString()).put("ordinal",51),
            JSONObject(target.toString()).put("ordinal",1.0),
            JSONObject(target.toString()).put("x",1),
        )
        invalid.forEachIndexed { index, value -> assertTrue("Invalid semantic target $index",runCatching { decodeSemantic(semantic(value)) }.isFailure) }
        assertTrue(runCatching { decodeSemantic(semantic(target).getJSONObject("params").put("extra",true).let { semantic(target).put("params",it) }) }.isFailure)
        assertTrue(runCatching { decodeSemantic(semantic(target).put("actionSchemaVersion",1)) }.isFailure)
        assertTrue(runCatching { decodeSemantic(semantic(target).put("actionSchemaVersion",2.0)) }.isFailure)
        assertTrue(runCatching { decodeSemantic(semantic(target).put("params",JSONObject().put("target","text"))) }.isFailure)
        assertTrue(runCatching { decodeSemantic(semantic(target).put("params",JSONObject().put("target",target).put("confirmed",true))) }.isFailure)
    }
    @Test fun canonicalBackendFixtureMatchesAndroidForEveryActionAndInvalidCase() {
        val fixture = javaClass.getResourceAsStream("/contracts/device-command-schema-1.json")!!.bufferedReader().use { JSONObject(it.readText()) }
        val valid = fixture.getJSONArray("valid")
        assertEquals(ActionId.entries.size - ActionCatalog.schemaTwoOnlyActions.size,valid.length())
        val decoded = (0 until valid.length()).map { decode(valid.getJSONObject(it)).action }.toSet()
        assertEquals(ActionId.entries.toSet() - ActionCatalog.schemaTwoOnlyActions,decoded)
        val invalid = fixture.getJSONArray("invalid")
        for (index in 0 until invalid.length()) assertTrue("Invalid shared fixture $index",runCatching { decode(invalid.getJSONObject(index)) }.isFailure)
    }
    @Test fun canonicalSchemaTwoFixtureMatchesAndroidDecoder() {
        val fixture = javaClass.getResourceAsStream("/contracts/device-command-schema-2.json")!!.bufferedReader().use { JSONObject(it.readText()) }
        val capabilities = fixture.getJSONObject("clientCapabilities")
        assertEquals(2,capabilities.getInt("actionSchemaVersion"))
        val supported = (0 until capabilities.getJSONArray("supportedActions").length()).map {
            ActionId.valueOf(capabilities.getJSONArray("supportedActions").getString(it))
        }.toSet()
        val valid = fixture.getJSONArray("valid")
        for (index in 0 until valid.length()) {
            val args = valid.getJSONObject(index)
            assertTrue("Valid schema-2 fixture $index",runCatching { DeviceCommandProtocol.decode(action(args),registry,supported,2) }.isSuccess)
            assertTrue("Schema-1 runtime must reject schema-2 fixture $index",runCatching { decode(args) }.isFailure)
        }
        val invalid = fixture.getJSONArray("invalid")
        for (index in 0 until invalid.length()) assertTrue("Invalid schema-2 fixture $index",
            runCatching { DeviceCommandProtocol.decode(action(invalid.getJSONObject(index)),registry,supported,2) }.isFailure)
    }
}
