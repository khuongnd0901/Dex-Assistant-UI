package com.khuongnd.dexassistant.ui.routing

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Normalizer
import java.util.Collections
import java.util.Locale

private val vietnameseLocale = Locale.forLanguageTag("vi-VN")
private val combiningMarks = Regex("\\p{M}+")
internal fun folded(value: String): String = Normalizer.normalize(value.lowercase(vietnameseLocale), Normalizer.Form.NFD)
    .replace(combiningMarks, "").replace('đ', 'd')

sealed class InteractionReply {
    data object Confirm : InteractionReply()
    data object Cancel : InteractionReply()
    data object Next : InteractionReply()
    data object Previous : InteractionReply()
    data class Select(val index: Int) : InteractionReply()
}
class InteractionPhrases internal constructor(confirm: List<String>, cancel: List<String>, select: List<String>, next: List<String> = emptyList(), previous: List<String> = emptyList()) {
    private val whitespace = Regex("\\s+")
    private val confirms = confirm.map { folded(it) }.toSet()
    private val cancels = cancel.map { folded(it) }.toSet()
    private val nextPages = next.map(::folded).toSet()
    private val previousPages = previous.map(::folded).toSet()
    // Compile by parts, since Regex.escape wraps the entire pattern in one quotation.
    private val selectionPatterns = select.map { value ->
        val parts = folded(value).split("{index}")
        Regex("^${Regex.escape(parts[0])}(.+?)${Regex.escape(parts[1])}$")
    }
    fun match(text: String): InteractionReply? {
        val normalized = folded(Normalizer.normalize(text, Normalizer.Form.NFC).trim().replace(whitespace, " "))
        if (normalized in confirms) return InteractionReply.Confirm
        if (normalized in cancels) return InteractionReply.Cancel
        if (normalized in nextPages) return InteractionReply.Next
        if (normalized in previousPages) return InteractionReply.Previous
        return selectionPatterns.firstNotNullOfOrNull { regex -> regex.matchEntire(normalized)?.groupValues?.get(1)?.let(::spokenInteger) }
            ?.takeIf { it in 1..50 }?.let(InteractionReply::Select)
    }
}

internal enum class SlotType { TEXT, APP_ALIAS, INTEGER, PERCENT, DURATION, PHONE_NUMBER }
internal data class Slot(val type: SlotType, val required: Boolean)
internal data class CompiledPattern(
    val commandId: String, val action: String, val regex: Regex, val exactRegex: Regex, val slotNames: List<String>,
    val slots: Map<String, Slot>, val args: Map<String, Any>, val scope: Set<String>,
    val literalSpecificity: Int, val typedSpecificity: Int, val priority: Int, val prefix: String, val allowCompound: Boolean,
)
data class ConfiguredApp(val id: String, val packageName: String, val aliases: List<String>)

/** Immutable compiled catalog; JSON and regex compilation occur only when loading. */
class CommandRegistry private constructor(
    val configVersion: Int,
    val apps: List<ConfiguredApp>,
    val interactions: InteractionPhrases,
    internal val prefixIndex: Map<String, List<CompiledPattern>>,
) {
    val appIds: Set<String> = Collections.unmodifiableSet(apps.map { it.id }.toSet())
    private val packages = apps.associate { it.id to it.packageName }
    private val aliases = apps.flatMap { app -> app.aliases.map { folded(it) to app.id } }.toMap()
    private val longestPrefix = prefixIndex.keys.maxOfOrNull(String::length) ?: 0
    internal fun candidatesFor(projection: String): List<CompiledPattern> = buildList {
        addAll(prefixIndex[""].orEmpty())
        // Index the full literal prefix: a crowded first word must not scan every pattern.
        for (end in 1..minOf(projection.length, longestPrefix)) {
            prefixIndex[projection.substring(0, end)]?.let(::addAll)
        }
    }
    fun appPackage(id: String): String? = packages[id]
    fun appIdForAlias(alias: String): String? = aliases[folded(alias.trim())]
    fun appIdForPackage(packageName: String): String? = apps.firstOrNull { it.packageName == packageName }?.id

    companion object {
        @Volatile private var bundled: CommandRegistry? = null
        @Synchronized fun load(context: Context): CommandRegistry = bundled ?: context.assets.open("commands.json").bufferedReader().use {
            fromJson(it.readText(), com.khuongnd.dexassistant.ui.BuildConfig.VERSION_CODE)
        }.also { bundled = it }
        fun default(): CommandRegistry = bundled ?: bundledForTests().also { bundled = it }
        fun bundledForTests(): CommandRegistry {
            val file = listOf(File("src/main/assets/commands.json"), File("app/src/main/assets/commands.json"))
                .firstOrNull { it.isFile } ?: error("Initialize CommandRegistry.load(context) before using default parser")
            return fromJson(file.readText())
        }
        fun fromJson(json: String, appVersion: Int = 1): CommandRegistry {
            val root = JSONObject(json)
            root.fields("schemaVersion", "configVersion", "minimumAppVersion", "apps", "commands", "interactions")
            require(root.strictInt("schemaVersion") == 1)
            val version = root.strictInt("configVersion").also { require(it > 0) }
            require(root.strictInt("minimumAppVersion") in 1..appVersion)
            val appIds = mutableSetOf<String>(); val aliasKeys = mutableSetOf<String>()
            val apps = root.getJSONArray("apps").objects().map { obj ->
                obj.fields("id", "packageName", "aliases")
                val id = obj.strictString("id").also { require(it.matches(Regex("[a-z][a-z0-9_]{0,63}")) && appIds.add(it)) }
                val pkg = obj.strictString("packageName").also { require(it.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))) }
                val aliases = obj.getJSONArray("aliases").strings().map { Normalizer.normalize(it,Normalizer.Form.NFC) }.also { require(it.size in 1..100) }
                aliases.forEach { require(it.isNotBlank() && it == it.trim() && !it.contains(Regex("\\s{2,}")) && aliasKeys.add(folded(it))) { "Alias collision" } }
                ConfiguredApp(id, pkg, Collections.unmodifiableList(aliases))
            }
            require(apps.size <= 200)
            val interaction = root.getJSONObject("interactions")
            interaction.fields("confirm", "cancel", "select", "next", "previous")
            val confirms = interaction.getJSONArray("confirm").strings(); val cancels = interaction.getJSONArray("cancel").strings()
            val selects = interaction.getJSONArray("select").strings()
            val next = if (interaction.has("next")) interaction.getJSONArray("next").strings() else emptyList()
            val previous = if (interaction.has("previous")) interaction.getJSONArray("previous").strings() else emptyList()
            val replies = confirms + cancels + next + previous
            require(confirms.isNotEmpty() && cancels.isNotEmpty() && selects.isNotEmpty())
            require(replies.map(::folded).distinct().size == replies.size)
            require(replies.all { it.isNotBlank() && '{' !in it && '}' !in it })
            require(selects.all { it.split("{index}").size == 2 && it.replace("{index}", "").none { c -> c == '{' || c == '}' } })
            val patterns = mutableListOf<CompiledPattern>(); val ids = mutableSetOf<String>(); val conflicts = mutableSetOf<String>()
            root.getJSONArray("commands").objects().forEach { obj ->
                obj.fields("id", "action", "patterns", "slots", "args", "scope", "priority", "allowCompound")
                val id = obj.strictString("id").also { require(it.matches(Regex("[a-z][a-z0-9_]{0,63}")) && ids.add(it)) }
                val action = obj.strictString("action").also { ActionId.valueOf(it) }
                val allowCompound = if (obj.has("allowCompound")) obj.get("allowCompound").let { require(it is Boolean); it } else false
                for (field in listOf("slots", "args", "scope")) if (obj.has(field)) require(obj.get(field) is JSONObject)
                val slotObject = obj.optJSONObject("slots") ?: JSONObject()
                val slots = slotObject.keys().asSequence().associateWith { name ->
                    require(name.matches(Regex("[a-zA-Z][a-zA-Z0-9]*")))
                    val spec = slotObject.getJSONObject(name); spec.fields("type", "required")
                    Slot(SlotType.valueOf(spec.strictString("type")), spec.get("required").let { require(it is Boolean && it); it })
                }
                val args = (obj.optJSONObject("args") ?: JSONObject()).primitiveMap()
                require(args.keys.intersect(slots.keys).isEmpty())
                val scope = obj.optJSONObject("scope")?.let { scopeObj ->
                    scopeObj.fields("apps"); scopeObj.getJSONArray("apps").strings().toSet().also { require(it.isNotEmpty() && appIds.containsAll(it)) }
                } ?: emptySet()
                val priority = if (obj.has("priority")) obj.strictInt("priority").also { require(it in -100..100) } else 0
                val examples = slots.mapValues { (name, slot) -> when (slot.type) {
                    SlotType.TEXT -> "example"
                    SlotType.APP_ALIAS -> when {
                        action == "APP_PAIR_LAUNCH" && slots.containsKey("firstApp") && slots.containsKey("secondApp") ->
                            if (name == "firstApp") apps.firstOrNull()?.id ?: error("App slot needs apps")
                            else apps.getOrNull(1)?.id ?: error("App pair needs two configured apps")
                        else -> apps.firstOrNull()?.id ?: error("App slot needs apps")
                    }
                    SlotType.INTEGER -> 1
                    SlotType.PERCENT -> 50
                    SlotType.DURATION -> 30
                    SlotType.PHONE_NUMBER -> "0123456789"
                } }
                ActionCatalog.validate(action, args + examples)
                if (allowCompound) {
                    require(action == "APP_PAIR_LAUNCH" && slots.values.none { it.type == SlotType.TEXT })
                }
                if (action == "APP_LAUNCH" && args.containsKey("app")) require(args["app"] in appIds)
                if (action == "APP_PAIR_LAUNCH") {
                    require("firstApp" in slots && "secondApp" in slots)
                    require(apps.size >= 2)
                }
                val texts = obj.getJSONArray("patterns").strings().also { require(it.isNotEmpty()) }
                texts.forEach { rawPattern ->
                    val pattern = Normalizer.normalize(rawPattern,Normalizer.Form.NFC)
                    require(pattern.isNotBlank() && pattern.length <= 500 && pattern == pattern.trim() && !pattern.contains(Regex("\\s{2,}")))
                    val foldedPattern = folded(pattern)
                    val placeholders = Regex("\\{([a-zA-Z][a-zA-Z0-9]*)\\}").findAll(foldedPattern).toList()
                    val placeholderNames = placeholders.map { it.groupValues[1] }
                    val canonicalNames = placeholderNames.map { placeholder ->
                        slots.keys.singleOrNull { it.equals(placeholder, ignoreCase = true) }
                            ?: throw IllegalArgumentException("Placeholder has no matching slot: $placeholder")
                    }
                    require(canonicalNames.distinct().size == canonicalNames.size && canonicalNames.toSet() == slots.keys)
                    require(foldedPattern.replace(Regex("\\{[a-zA-Z][a-zA-Z0-9]*\\}"), "").none { it == '{' || it == '}' })
                    require(conflicts.add("${foldedPattern.replace(Regex("\\{[a-zA-Z][a-zA-Z0-9]*\\}"), "{slot}")}|${scope.sorted()}")) { "Pattern conflict: $pattern" }
                    var end = 0; val regex = StringBuilder("^"); val exact = StringBuilder("^"); var literal = 0
                    placeholders.forEach { match ->
                        val part = foldedPattern.substring(end, match.range.first); literal += part.count { !it.isWhitespace() }
                        regex.append(Regex.escape(part)).append("(.+?)"); exact.append(Regex.escape(pattern.substring(end, match.range.first))).append("(.+?)"); end = match.range.last + 1
                    }
                    val tail = foldedPattern.substring(end); literal += tail.count { !it.isWhitespace() }; regex.append(Regex.escape(tail)).append('$'); exact.append(Regex.escape(pattern.substring(end))).append('$')
                    patterns += CompiledPattern(id, action, Regex(regex.toString()), Regex(exact.toString(),RegexOption.IGNORE_CASE), canonicalNames, slots, args, scope,
                        literal, slots.values.count { it.type != SlotType.TEXT }, priority,
                        foldedPattern.substringBefore('{'),allowCompound)
                }
            }
            require(patterns.size in 1..5000)
            val index = patterns.groupBy { it.prefix }.mapValues { Collections.unmodifiableList(it.value) }
            return CommandRegistry(version, Collections.unmodifiableList(apps), InteractionPhrases(confirms,cancels,selects,next,previous), Collections.unmodifiableMap(index))
        }
    }
}

/** A rejected reload leaves the exact previous immutable registry in service. */
class CommandRegistryHolder(initial: CommandRegistry) {
    @Volatile var current: CommandRegistry = initial; private set
    fun reload(json: String, appVersion: Int = 1): Boolean = try {
        val replacement = CommandRegistry.fromJson(json, appVersion)
        current = replacement; true
    } catch (_: Exception) { false } // Android org.json.JSONException is checked, unlike the JVM fixture library.
}

private fun JSONObject.fields(vararg allowed: String) { require(keys().asSequence().all { it in allowed }) { "Unknown config field" } }
private fun JSONObject.strictInt(key: String): Int = get(key).let { require(it is Int); it }
private fun JSONObject.strictString(key: String): String = get(key).let { require(it is String); it }
private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { get(it).let { value -> require(value is JSONObject); value } }
private fun JSONArray.strings(): List<String> = (0 until length()).map { get(it).let { value -> require(value is String); value } }
private fun JSONObject.primitiveMap(): Map<String, Any> = keys().asSequence().associateWith { key -> get(key).also { require(it is String || it is Int || it is Boolean) } }

private val spokenDigits = mapOf("mot" to 1, "hai" to 2, "ba" to 3, "bon" to 4, "nam" to 5,
    "sau" to 6, "bay" to 7, "tam" to 8, "chin" to 9)
private val integerDigits = Regex("(?:0|[1-9][0-9]*)")
internal fun spokenInteger(value: String): Int? {
    if (integerDigits.matches(value)) return value.toIntOrNull()
    spokenDigits[value]?.let { return it }
    if (value == "muoi") return 10
    val words = value.split(' ')
    fun unit(word: String): Int? = spokenDigits[word] ?: when (word) { "mot" -> 1; "tu" -> 4; "lam" -> 5; else -> null }
    if (words.size == 2 && words[0] == "muoi") return unit(words[1])?.plus(10)
    if (words.size in 2..3 && words[1] == "muoi") {
        val tens = spokenDigits[words[0]]?.takeIf { it in 2..9 } ?: return null
        val remainder = if (words.size == 2) 0 else unit(words[2]) ?: return null
        return tens * 10 + remainder
    }
    return null
}
