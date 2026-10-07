package com.khuongnd.dexassistant.ui.routing

import java.text.Normalizer
import java.util.Locale

const val ROUTING_VERSION = "vi-config-v1"
enum class Route { LOCAL, BACKEND_REQUIRED, UNSUPPORTED }
enum class LocalIntent {
    APP_LAUNCH, WIFI_SET, BLUETOOTH_SET, VOLUME_SET, BRIGHTNESS_SET, MEDIA_TRANSPORT, YOUTUBE_SEARCH,
    YOUTUBE_PLAY_QUERY, YOUTUBE_SELECT, YOUTUBE_FULLSCREEN, MEDIA_SEEK, NAVIGATE, SEARCH_PLACE,
    CALL_CONTACT, CALL_NUMBER, WEB_SEARCH, CLICK_TEXT, SCROLL, FEED_NEXT, FEED_PREVIOUS, SYSTEM_ACTION, APP_PAIR_LAUNCH,
}
data class RoutedCommand(
    val route: Route, val normalizedText: String, val intent: LocalIntent? = null,
    val arguments: Map<String,String> = emptyMap(), val resolved: ResolvedCommand? = null,
)
object VietnameseNormalizer {
    private val punctuation = Regex("^[.!?,;:…“”\"'«»]+|[.!?,;:…“”\"'«»]+$")
    private val whitespace = Regex("\\s+")
    private val prefix = Regex("^(?:vui lòng|hãy|giúp tôi) ",RegexOption.IGNORE_CASE)
    private val suffix = Regex(" (?:giúp tôi|đi)$",RegexOption.IGNORE_CASE)
    /** Case and accent-preserving normalization for extracted slots. */
    fun preserveSlots(value: String): String = Normalizer.normalize(value,Normalizer.Form.NFC).trim()
        .replace(whitespace," ").replace(prefix,"").trim()
    fun prepare(value: String): String = Normalizer.normalize(value,Normalizer.Form.NFC).trim()
        .replace(whitespace," ").replace(punctuation,"").trim().replace(prefix,"").replace(suffix,"").trim()
    fun normalize(value: String): String = prepare(value).lowercase(Locale.forLanguageTag("vi-VN"))
}
class LocalCommandParser(private val registry: CommandRegistry = CommandRegistry.default()) {
    private val engine = LocalCommandEngine(registry)
    private var allowedAppIds: Set<String>? = null
    constructor(appAliases: Set<String>) : this(CommandRegistry.default()) {
        allowedAppIds = appAliases.mapNotNull(registry::appIdForAlias).toSet()
    }
    /** Only a valid configured YouTube index action may precede the numeric screen guard. */
    fun youtubeIndexSelection(original: String): Boolean = engine.resolve(original, "youtube")?.typedAction?.let {
        it.action == ActionId.YOUTUBE_SELECT && it.params.containsKey("index")
    } == true
    fun screenCommand(original: String, foregroundAppId: String? = null): com.khuongnd.dexassistant.ui.screen.ScreenCommand? {
        val screen = com.khuongnd.dexassistant.ui.screen.ScreenCommandParser.parse(original)
            ?: if (youtubeNumeric.containsMatchIn(folded(VietnameseNormalizer.normalize(original))))
                com.khuongnd.dexassistant.ui.screen.ScreenCommand(com.khuongnd.dexassistant.ui.screen.ScreenVerb.INVALID)
            else return null
        if (screen.verb == com.khuongnd.dexassistant.ui.screen.ScreenVerb.INVALID && foregroundAppId == "youtube") {
            val action = engine.resolve(original, foregroundAppId)?.typedAction
            if (action?.action == ActionId.YOUTUBE_SELECT && action.params.containsKey("index")) return null
        }
        return screen
    }
    fun route(original: String, foregroundAppId: String? = null): RoutedCommand {
        val text = VietnameseNormalizer.normalize(original)
        if (CommandPrivacy.requiresLocalHandling(original)) return RoutedCommand(Route.UNSUPPORTED,text)
        val projection = folded(text)
        if (registry.interactions.match(original) != null) return RoutedCommand(Route.UNSUPPORTED,text,arguments=mapOf("reason" to "NO_PENDING_INTERACTION"))
        if (LocalCommandEngine.requiresBackend(text)) return RoutedCommand(Route.BACKEND_REQUIRED,text)
        close.matchEntire(projection)?.let { if (registry.appIdForAlias(it.groupValues[1]) != null) return RoutedCommand(Route.UNSUPPORTED,text) }
        val resolved = engine.resolve(original,foregroundAppId)
        if (resolved?.typedAction?.action == ActionId.APP_LAUNCH && allowedAppIds?.contains(resolved.typedAction.string("app")) == false)
            return RoutedCommand(Route.BACKEND_REQUIRED,text)
        if (resolved != null) return RoutedCommand(Route.LOCAL,text,LocalIntent.valueOf(resolved.typedAction.action.name),
            resolved.typedAction.params.mapValues { it.value.toString() },resolved)
        if (projection.startsWith("goi so ")) return RoutedCommand(Route.UNSUPPORTED,text,arguments=mapOf("reason" to "AMBIGUOUS_PHONE_NUMBER"))
        if (unsupported.matches(projection)) return RoutedCommand(Route.UNSUPPORTED,text)
        return RoutedCommand(Route.BACKEND_REQUIRED,text)
    }
    companion object {
        private val youtubeNumeric = Regex("(?:^| )(?:mo video so|chon video so|phat ket qua so)(?: |$)")
        private val close = Regex("(?:tat|dong) (.+)")
        private val unsupported = Regex("(?:chia doi man hinh|nhap .+)")
    }
}
