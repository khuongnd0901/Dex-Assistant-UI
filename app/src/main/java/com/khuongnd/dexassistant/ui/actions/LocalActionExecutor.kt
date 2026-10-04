package com.khuongnd.dexassistant.ui.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import com.khuongnd.dexassistant.ui.device.DpcClient
import com.khuongnd.dexassistant.ui.routing.LocalIntent
import com.khuongnd.dexassistant.ui.routing.RoutedCommand
import com.khuongnd.dexassistant.ui.routing.LocalCommandParser
import kotlin.math.roundToInt

enum class LocalOutcome { VERIFIED, DISPATCHED, FAILED, REJECTED }
data class LocalResult(val outcome: LocalOutcome, val errorCode: String? = null)

class LocalActionExecutor(private val context: Context, private val dpc: DpcClient) {
    fun execute(command: RoutedCommand, complete: (LocalResult) -> Unit) {
        when (command.intent) {
            LocalIntent.APP_LAUNCH -> complete(launchApp(command.arguments.getValue("alias")))
            LocalIntent.WIFI_SET -> dpc.toggleWifi(command.arguments.getValue("enabled").toBoolean()) {
                complete(LocalResult(if (it.status == "SUCCESS") LocalOutcome.VERIFIED else if (it.status == "REJECTED") LocalOutcome.REJECTED else LocalOutcome.FAILED, it.errorCode))
            }
            LocalIntent.BLUETOOTH_SET -> complete(LocalResult(LocalOutcome.FAILED, "CAPABILITY_UNAVAILABLE"))
            LocalIntent.VOLUME_SET -> complete(volume(command.arguments))
            LocalIntent.BRIGHTNESS_SET -> complete(brightness(command.arguments))
            LocalIntent.MEDIA_TRANSPORT -> complete(media(command.arguments.getValue("operation")))
            LocalIntent.YOUTUBE_SEARCH -> complete(youtube(command.arguments.getValue("query")))
            null -> complete(LocalResult(LocalOutcome.REJECTED, "UNSUPPORTED_COMMAND"))
        }
    }

    private fun launchApp(alias: String): LocalResult {
        val packageName = LocalCommandParser.DEFAULT_APP_ALIASES[alias] ?: return LocalResult(LocalOutcome.FAILED, "APP_NOT_FOUND")
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return LocalResult(LocalOutcome.FAILED, "APP_NOT_FOUND")
        return dispatch(intent)
    }

    private fun youtube(query: String): LocalResult {
        val available = try {
            context.packageManager.getApplicationInfo(YoutubeSearchIntents.PACKAGE_NAME, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) { false }
        // SEARCH resolves on YouTube but can leave its existing task on the previous screen.
        // Its results deep link carries the query; browser fallback is only for an absent app.
        return dispatch(YoutubeSearchIntents.build(query, inApp = available))
    }

    private fun dispatch(intent: Intent): LocalResult = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); LocalResult(LocalOutcome.DISPATCHED)
    }.getOrElse { LocalResult(LocalOutcome.FAILED, "APP_NOT_FOUND") }

    private fun volume(args: Map<String, String>): LocalResult = runCatching {
        val audio = context.getSystemService(AudioManager::class.java)
        val stream = AudioManager.STREAM_MUSIC
        val before = audio.getStreamVolume(stream)
        when (args.getValue("operation")) {
            "MUTE" -> audio.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0)
            "UNMUTE" -> audio.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
            "INCREASE", "DECREASE" -> repeat(args.getValue("steps").toInt()) {
                audio.adjustStreamVolume(stream, if (args.getValue("operation") == "INCREASE") AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, 0)
            }
            "SET" -> audio.setStreamVolume(stream, (audio.getStreamMaxVolume(stream) * args.getValue("percent").toInt() / 100.0).roundToInt(), 0)
        }
        val verified = when (args.getValue("operation")) {
            "MUTE" -> audio.isStreamMute(stream)
            "UNMUTE" -> !audio.isStreamMute(stream)
            "SET" -> kotlin.math.abs(audio.getStreamVolume(stream) - (audio.getStreamMaxVolume(stream) * args.getValue("percent").toInt() / 100.0).roundToInt()) <= 1
            "INCREASE" -> audio.getStreamVolume(stream) >= before
            "DECREASE" -> audio.getStreamVolume(stream) <= before
            else -> false
        }
        if (verified) LocalResult(LocalOutcome.VERIFIED) else LocalResult(LocalOutcome.FAILED, "DEVICE_STATE_MISMATCH")
    }.getOrElse { LocalResult(LocalOutcome.FAILED, "POLICY_DENIED") }

    private fun brightness(args: Map<String, String>): LocalResult {
        if (!Settings.System.canWrite(context)) return LocalResult(LocalOutcome.REJECTED, "USER_ACTION_REQUIRED")
        return runCatching {
            val resolver = context.contentResolver
            val operation = args.getValue("operation")
            if (operation == "AUTO_ON" || operation == "AUTO_OFF") {
                val target = if (operation == "AUTO_ON") Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, target)
                if (Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE) == target) LocalResult(LocalOutcome.VERIFIED)
                else LocalResult(LocalOutcome.FAILED, "DEVICE_STATE_MISMATCH")
            } else {
                val current = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128)
                val target = if (operation == "SET") (args.getValue("percent").toInt() * 255 / 100) else (current + if (operation == "INCREASE") 26 else -26).coerceIn(0, 255)
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, target)
                if (Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS) == target) LocalResult(LocalOutcome.VERIFIED)
                else LocalResult(LocalOutcome.FAILED, "DEVICE_STATE_MISMATCH")
            }
        }.getOrElse { LocalResult(LocalOutcome.FAILED, "POLICY_DENIED") }
    }

    private fun media(operation: String): LocalResult = runCatching {
        val code = when (operation) { "PLAY" -> KeyEvent.KEYCODE_MEDIA_PLAY; "PAUSE" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "NEXT" -> KeyEvent.KEYCODE_MEDIA_NEXT; "PREVIOUS" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS; else -> KeyEvent.KEYCODE_MEDIA_STOP }
        val audio = context.getSystemService(AudioManager::class.java)
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code)); audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        LocalResult(LocalOutcome.DISPATCHED)
    }.getOrElse { LocalResult(LocalOutcome.FAILED, "CAPABILITY_UNAVAILABLE") }
}
