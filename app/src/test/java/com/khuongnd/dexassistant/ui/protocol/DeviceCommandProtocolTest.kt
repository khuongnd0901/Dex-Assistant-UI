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
    }
    @Test fun canonicalBackendFixtureMatchesAndroidForEveryActionAndInvalidCase() {
        val fixture = javaClass.getResourceAsStream("/contracts/device-command-schema-1.json")!!.bufferedReader().use { JSONObject(it.readText()) }
        val valid = fixture.getJSONArray("valid")
        assertEquals(ActionId.entries.size,valid.length())
        val decoded = (0 until valid.length()).map { decode(valid.getJSONObject(it)).action }.toSet()
        assertEquals(ActionId.entries.toSet(),decoded)
        val invalid = fixture.getJSONArray("invalid")
        for (index in 0 until invalid.length()) assertTrue("Invalid shared fixture $index",runCatching { decode(invalid.getJSONObject(index)) }.isFailure)
    }
}
