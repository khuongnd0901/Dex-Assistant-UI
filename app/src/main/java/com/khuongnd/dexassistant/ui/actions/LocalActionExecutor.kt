package com.khuongnd.dexassistant.ui.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.ActivityOptions
import android.graphics.Rect
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import com.khuongnd.dexassistant.ui.device.DpcClient
import com.khuongnd.dexassistant.ui.routing.LocalIntent
import com.khuongnd.dexassistant.ui.routing.RoutedCommand
import android.Manifest
import android.net.Uri
import com.khuongnd.dexassistant.ui.routing.ActionCatalog
import com.khuongnd.dexassistant.ui.routing.ActionId
import com.khuongnd.dexassistant.ui.routing.TypedAction
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import com.khuongnd.dexassistant.ui.localdata.ContactCandidate
import com.khuongnd.dexassistant.ui.localdata.ContactIdentity
import com.khuongnd.dexassistant.ui.localdata.ContactResolver
import com.khuongnd.dexassistant.ui.localdata.LocalAliases
import com.khuongnd.dexassistant.ui.media.MediaSeek
import kotlin.math.roundToInt

enum class LocalOutcome { VERIFIED, DISPATCHED, FAILED, REJECTED }
data class LocalResult(val outcome: LocalOutcome, val errorCode: String? = null)

class LocalActionExecutor(
    private val context: Context, private val dpc: DpcClient,
    private val appPackage: (String) -> String? = CommandRegistry.load(context)::appPackage,
) {
    private val aliases = LocalAliases(context)
    val contacts = ContactResolver(context, aliases)

    /** Both local and remote typed actions enter this boundary; sensitive calls require continuation. */
    fun execute(action: TypedAction, complete: (LocalResult) -> Unit) {
        val validated = runCatching { ActionCatalog.validate(action.action.name, action.params,
            if (action.action in ActionCatalog.schemaTwoOnlyActions) ActionCatalog.semanticSchemaVersion else ActionCatalog.schemaVersion) }.getOrNull()
        if (validated == null) { complete(LocalResult(LocalOutcome.REJECTED, "INVALID_ARGUMENTS")); return }
        val args = validated.params.mapValues { it.value.toString() }
        if (validated.action == ActionId.APP_CLOSE_WINDOW) { complete(LocalResult(LocalOutcome.REJECTED, "CAPABILITY_UNAVAILABLE")); return }
        when (validated.action) {
            ActionId.APP_LAUNCH -> complete(launchAppId(args.getValue("app")))
            ActionId.APP_PAIR_LAUNCH -> complete(LocalResult(LocalOutcome.REJECTED,"DEX_DISPLAY_REQUIRED"))
            ActionId.WIFI_SET -> dpc.toggleWifi(validated.params.getValue("enabled") as Boolean) {
                complete(LocalResult(if (it.status == "SUCCESS") LocalOutcome.VERIFIED else if (it.status == "REJECTED") LocalOutcome.REJECTED else LocalOutcome.FAILED, it.errorCode))
            }
            ActionId.BLUETOOTH_SET -> complete(LocalResult(LocalOutcome.REJECTED, "CAPABILITY_UNAVAILABLE"))
            ActionId.VOLUME_SET -> complete(volume(args))
            ActionId.BRIGHTNESS_SET -> complete(brightness(args))
            ActionId.MEDIA_TRANSPORT -> complete(media(args.getValue("operation")))
            ActionId.MEDIA_SEEK -> complete(MediaSeek(context).execute(args.getValue("operation"), args.getValue("seconds").toInt()))
            ActionId.YOUTUBE_SEARCH -> complete(youtube(args.getValue("query")))
            ActionId.SEARCH_PLACE -> complete(dispatch(CommandIntents.place(args.getValue("query"), packageAvailable(CommandIntents.MAPS_PACKAGE))))
            ActionId.NAVIGATE -> {
                val destination = aliases.place(args.getValue("destination"))
                if (destination == null) complete(LocalResult(LocalOutcome.REJECTED, "PLACE_ALIAS_NOT_CONFIGURED"))
                else {
                    val inApp = packageAvailable(CommandIntents.MAPS_PACKAGE)
                    val result = dispatch(CommandIntents.navigate(destination, args.getValue("travelMode"), inApp))
                    complete(if (result.outcome == LocalOutcome.DISPATCHED && !inApp) result.copy(errorCode = "DIRECTIONS_OPENED") else result)
                }
            }
            ActionId.WEB_SEARCH -> complete(dispatch(CommandIntents.web(args.getValue("query"))))
            ActionId.SYSTEM_ACTION -> complete(if (args["operation"] == "SETTINGS") dispatch(Intent(Settings.ACTION_SETTINGS))
                else LocalResult(LocalOutcome.REJECTED, "CAPABILITY_UNAVAILABLE"))
            ActionId.CALL_CONTACT, ActionId.CALL_NUMBER, ActionId.CLICK_TEXT -> complete(LocalResult(LocalOutcome.REJECTED, "CONFIRMATION_REQUIRED"))
            else -> complete(LocalResult(LocalOutcome.REJECTED, "CAPABILITY_UNAVAILABLE"))
        }
    }

    fun executeWebSearchInApp(query: String, packageName: String?): LocalResult =
        if (packageName == null) LocalResult(LocalOutcome.REJECTED, "APP_NOT_CONFIGURED")
        else dispatch(CommandIntents.web(query, packageName))

    fun executePlaceOnDesktop(query: String, displayId: Int): LocalResult = runCatching {
        val intent = CommandIntents.place(query, packageAvailable(CommandIntents.MAPS_PACKAGE))
        context.startActivity(intent, ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle())
        LocalResult(LocalOutcome.DISPATCHED)
    }.getOrElse { LocalResult(LocalOutcome.FAILED, "LAUNCH_FAILED") }

    /** Launch two configured app tasks on the chosen desktop; the router verifies their final windows. */
    fun launchAppPair(firstApp: String, secondApp: String, displayId: Int,
                      bounds: com.khuongnd.dexassistant.ui.screen.ScreenRect, isCurrent: () -> Boolean,
                      complete: (LocalResult) -> Unit) {
        if (firstApp == secondApp) { complete(LocalResult(LocalOutcome.REJECTED,"APP_PAIR_MUST_BE_DISTINCT")); return }
        val first = launchIntent(firstApp) ?: run { complete(LocalResult(LocalOutcome.REJECTED,"APP_NOT_CONFIGURED")); return }
        val second = launchIntent(secondApp) ?: run { complete(LocalResult(LocalOutcome.REJECTED,"APP_NOT_CONFIGURED")); return }
        if (bounds.width < 800 || bounds.height < 480) { complete(LocalResult(LocalOutcome.REJECTED,"DISPLAY_TOO_SMALL")); return }
        fun options(rect: Rect) = ActivityOptions.makeBasic().setLaunchDisplayId(displayId).setLaunchBounds(rect).toBundle()
        val middle = bounds.left + bounds.width / 2
        val leftBounds = Rect(bounds.left,bounds.top,middle,bounds.bottom)
        val rightBounds = Rect(middle,bounds.top,bounds.right,bounds.bottom)
        runCatching { context.startActivity(first.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),options(leftBounds)) }
            .onFailure { complete(LocalResult(LocalOutcome.FAILED,"APP_PAIR_FIRST_LAUNCH_FAILED")); return }
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isCurrent()) { complete(LocalResult(LocalOutcome.REJECTED,"CANCELLED")); return@postDelayed }
            runCatching {
                second.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                context.startActivity(second,options(rightBounds))
            }.onSuccess { complete(LocalResult(LocalOutcome.DISPATCHED)) }
                .onFailure { complete(LocalResult(LocalOutcome.FAILED,"APP_PAIR_SECOND_LAUNCH_FAILED")) }
        },350)
    }

    /** Called only after the router consumes a later spoken confirmation. Revalidate immediately. */
    fun executeConfirmedCall(candidate: ContactCandidate, complete: (LocalResult) -> Unit) {
        val unchanged = runCatching { contacts.revalidate(candidate) }.getOrDefault(false)
        complete(if (unchanged) call(candidate.number) else LocalResult(LocalOutcome.REJECTED, "RECIPIENT_CHANGED"))
    }

    fun executeConfirmedNumber(number: String, complete: (LocalResult) -> Unit) {
        complete(if (Regex("^\\+?[0-9]{3,15}$").matches(number)) call(number)
            else LocalResult(LocalOutcome.REJECTED, "INVALID_PHONE_NUMBER"))
    }

    private fun call(number: String): LocalResult {
        if (ContactIdentity.normalizedNumber(number) != number) return LocalResult(LocalOutcome.REJECTED, "INVALID_PHONE_NUMBER")
        val canCall = context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val result = dispatch(Intent(if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL,
            Uri.fromParts("tel", number, null)))
        return if (!canCall && result.outcome == LocalOutcome.DISPATCHED) result.copy(errorCode = "DIALER_OPENED") else result
    }

    private fun packageAvailable(packageName: String): Boolean = try {
        context.packageManager.getApplicationInfo(packageName, 0); true
    } catch (_: PackageManager.NameNotFoundException) { false }

    private fun launchAppId(id: String): LocalResult {
        val intent = launchIntent(id) ?: return LocalResult(LocalOutcome.FAILED, "APP_NOT_FOUND")
        if (id == "youtube") return launchYoutubeOnDesktop(intent)
        return dispatch(intent)
    }

    private fun launchIntent(id: String): Intent? = appPackage(id)?.let(context.packageManager::getLaunchIntentForPackage)

    fun execute(command: RoutedCommand, complete: (LocalResult) -> Unit) {
        command.resolved?.typedAction?.let { execute(it, complete); return }
        if (command.intent == null) { complete(LocalResult(LocalOutcome.REJECTED, "UNSUPPORTED_COMMAND")); return }
        // Compatibility for device-test/legacy callers. No phrase is reparsed by the executor.
        val params = command.arguments.toMutableMap()
        if (command.intent == LocalIntent.APP_LAUNCH && !params.containsKey("app")) {
            val alias = params.remove("alias")
            val id = alias?.let { CommandRegistry.default().appIdForAlias(it) }
            if (id == null) { complete(LocalResult(LocalOutcome.FAILED, "APP_NOT_FOUND")); return }
            params["app"] = id
        }
        val typed = runCatching {
            ActionCatalog.validate(command.intent.name, params.mapValues { (key, value) ->
                when (key) {
                    "enabled" -> value.toBooleanStrict()
                    "percent", "steps", "seconds", "index" -> value.toInt()
                    else -> value
                }
            })
        }.getOrNull()
        if (typed == null) complete(LocalResult(LocalOutcome.REJECTED, "INVALID_ARGUMENTS"))
        else execute(typed, complete)
    }

    private fun youtube(query: String): LocalResult {
        val available = try {
            context.packageManager.getApplicationInfo(YoutubeSearchIntents.PACKAGE_NAME, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) { false }
        // SEARCH resolves on YouTube but can leave its existing task on the previous screen.
        // Its results deep link carries the query; browser fallback is only for an absent app.
        if (!available) return LocalResult(LocalOutcome.REJECTED, "APP_NOT_FOUND")
        return launchYoutubeOnDesktop(YoutubeSearchIntents.build(query))
    }

    private fun launchYoutubeOnDesktop(intent: Intent): LocalResult {
        val display = context.getSystemService(android.hardware.display.DisplayManager::class.java)
            .getDisplays(com.khuongnd.dexassistant.ui.screen.DexAccessibilityService.SAMSUNG_DESKTOP_CATEGORY)
            .filter { it.isValid && it.displayId != android.view.Display.DEFAULT_DISPLAY }.singleOrNull()
            ?: return LocalResult(LocalOutcome.REJECTED, "DEX_DISPLAY_UNAVAILABLE")
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), ActivityOptions.makeBasic().setLaunchDisplayId(display.displayId).toBundle())
            LocalResult(LocalOutcome.DISPATCHED)
        }.getOrElse { LocalResult(LocalOutcome.FAILED, "LAUNCH_FAILED") }
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
