package com.khuongnd.dexassistant.ui.routing

import android.content.Context
import android.provider.Settings

data class Capability(val available: Boolean, val unavailableReason: String? = null)

class CapabilityRegistry(private val context: Context) {
    fun capability(intent: LocalIntent): Capability = when (intent) {
        LocalIntent.BLUETOOTH_SET -> Capability(false, "CAPABILITY_UNAVAILABLE") // No verified API 35 path in the DPC yet.
        LocalIntent.BRIGHTNESS_SET -> if (Settings.System.canWrite(context)) Capability(true) else Capability(false, "USER_ACTION_REQUIRED")
        else -> Capability(true)
    }
}

class CommandRouter(
    private val parser: LocalCommandParser,
    private val registry: CapabilityRegistry,
) {
    data class Decision(val command: RoutedCommand, val capability: Capability?)
    fun route(text: String, foregroundAppId: String? = null): Decision {
        val command = parser.route(text, foregroundAppId)
        return Decision(command, command.intent?.let(registry::capability))
    }
}
