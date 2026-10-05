package com.khuongnd.dexassistant.ui.execution

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.khuongnd.dexassistant.ui.actions.LocalActionExecutor
import com.khuongnd.dexassistant.ui.actions.LocalOutcome
import com.khuongnd.dexassistant.ui.actions.LocalResult
import com.khuongnd.dexassistant.ui.interaction.*
import com.khuongnd.dexassistant.ui.routing.*
import com.khuongnd.dexassistant.ui.screen.*

/** One execution policy for config matches and backend DEVICE_COMMAND, without raw utterance parsing. */
class ActionRouter(private val context: Context, private val registry: CommandRegistry, private val executor: LocalActionExecutor) {
    data class Prompt(val parentId: String, val token: Long, val version: Int, val text: String)
    private data class Choice(val label: String, val execute: ((LocalResult) -> Unit) -> Unit)
    private val main = Handler(Looper.getMainLooper())
    private val contactWorker = java.util.concurrent.ThreadPoolExecutor(1, 1, 30, java.util.concurrent.TimeUnit.SECONDS,
        java.util.concurrent.LinkedBlockingQueue<Runnable>()).apply { allowCoreThreadTimeOut(true) }
    private var contactTask: java.util.concurrent.Future<*>? = null
    private var contactCancellation: android.os.CancellationSignal? = null
    private var closed = false
    private var generation = 0L
    private var parent: String? = null
    private var activeActionId = ""
    private var ownsOverlay = false
    private data class SearchContext(val parentId: String, val query: String)
    private var searchContext: SearchContext? = null
    private var pending: PendingInteraction<Choice>? = null
    private var completion: ((LocalResult) -> Unit)? = null
    private var promptCallback: ((Prompt) -> Unit)? = null
    private var timer: Runnable? = null
    private var observer: Runnable? = null
    private var stepTimer: Runnable? = null
    val awaitingInteraction get() = pending != null
    private fun ui() = DexAccessibilityService.active?.let(::DesktopUiAccess)
    fun foregroundAppId(): String? = ui()?.foregroundPackage()?.let(registry::appIdForPackage)
    fun supportedActions(): Set<ActionId> = ActionId.entries.toSet() - ActionId.BLUETOOTH_SET

    fun execute(parentId: String, action: TypedAction, prompt: ((Prompt) -> Unit)?, actionId: String = "local", localCommandId: String? = null, complete: (LocalResult) -> Unit) {
        if (closed) { complete(LocalResult(LocalOutcome.REJECTED,"ROUTER_CLOSED")); return }
        cancel()
        parent = parentId; activeActionId = actionId; completion = complete; promptCallback = prompt
        if (searchContext?.parentId != parentId) searchContext = null
        val token = generation
        stepTimer = Runnable { if (current(token)) finish(LocalResult(LocalOutcome.FAILED,"ACTION_TIMEOUT")) }
            .also { main.postDelayed(it,30_000) }
        val validated = runCatching { ActionCatalog.validate(action.action.name,action.params) }.getOrNull()
        if (validated == null) { finish(LocalResult(LocalOutcome.REJECTED,"INVALID_ARGUMENTS")); return }
        when (validated.action) {
            ActionId.CALL_CONTACT -> {
                if (!executor.contacts.hasPermission()) { finish(LocalResult(LocalOutcome.REJECTED,"CONTACTS_PERMISSION_REQUIRED")); return }
                val directCall = context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                val cancellation = android.os.CancellationSignal().also { contactCancellation = it }
                contactTask = contactWorker.submit {
                    val contacts = runCatching { executor.contacts.resolveExact(validated.string("contact"), cancellation) }
                    main.post {
                        if (!current(token)) return@post
                        contacts.fold(onSuccess = { candidates ->
                            choose(candidates.map { candidate -> Choice("${candidate.name.take(80)}, ${spokenNumber(candidate.number)}${dialerNotice()}") {
                                if ((context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) != directCall)
                                    it(LocalResult(LocalOutcome.REJECTED,"POLICY_CHANGED"))
                                else executor.executeConfirmedCall(candidate,it)
                            } })
                        }, onFailure = { finish(LocalResult(LocalOutcome.FAILED,"CONTACT_LOOKUP_FAILED")) })
                    }
                }
            }
            ActionId.CALL_NUMBER -> {
                val directCall = context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                choose(listOf(Choice("số ${spokenNumber(validated.string("number"))}${dialerNotice()}") {
                    if ((context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) != directCall)
                        it(LocalResult(LocalOutcome.REJECTED,"POLICY_CHANGED"))
                    else executor.executeConfirmedNumber(validated.string("number"),it)
                }))
            }
            ActionId.CLICK_TEXT -> chooseUi(ui()?.targets(validated.string("text")).orEmpty())
            ActionId.YOUTUBE_SEARCH -> executor.execute(validated) { result ->
                if (current(token)) {
                    if (result.outcome == LocalOutcome.DISPATCHED) searchContext = SearchContext(parentId,validated.string("query"))
                    finish(result)
                }
            }
            ActionId.YOUTUBE_SELECT -> {
                val expected = searchContext?.takeIf { it.parentId == parentId }
                if (expected != null) observeYoutube(token, expected.query, SystemClock.elapsedRealtime()+10_000, validated)
                else selectYoutube(ui()?.youtubeResults().orEmpty(),validated,token)
            }
            ActionId.YOUTUBE_FULLSCREEN -> chooseUi(ui()?.fullscreen(validated.boolean("enabled")).orEmpty())
            ActionId.YOUTUBE_PLAY_QUERY -> {
                executor.execute(ActionCatalog.validate("YOUTUBE_SEARCH",mapOf("query" to validated.string("query")))) { result ->
                    if (!current(token)) return@execute
                    if (result.outcome !in setOf(LocalOutcome.DISPATCHED,LocalOutcome.VERIFIED)) finish(result)
                    else observeYoutube(token,validated.string("query"),SystemClock.elapsedRealtime()+10_000)
                }
            }
            ActionId.MEDIA_SEEK -> {
                if (foregroundAppId() !in setOf("youtube", "youtube_music")) finish(LocalResult(LocalOutcome.REJECTED,"APP_CONTEXT_REQUIRED"))
                else executor.execute(validated) { if (current(token)) finish(it) }
            }
            ActionId.WEB_SEARCH -> {
                if (localCommandId == "web_search_in_chrome") {
                    if (foregroundAppId() != "chrome") finish(LocalResult(LocalOutcome.REJECTED,"APP_CONTEXT_REQUIRED"))
                    else finish(executor.executeWebSearchInApp("địa điểm ${validated.string("query")}", registry.appPackage("chrome")))
                } else executor.execute(validated) { if (current(token)) finish(it) }
            }
            ActionId.APP_PAIR_LAUNCH -> {
                val access = ui()
                val desktop = access?.tree()
                if (access == null || desktop == null) finish(LocalResult(LocalOutcome.REJECTED,"DEX_DISPLAY_UNAVAILABLE"))
                else if (registry.appPackage(validated.string("firstApp")) == null || registry.appPackage(validated.string("secondApp")) == null)
                    finish(LocalResult(LocalOutcome.REJECTED,"APP_NOT_CONFIGURED"))
                else executor.launchAppPair(validated.string("firstApp"),validated.string("secondApp"),desktop.displayId,desktop.bounds,{
                    current(token) && ui()?.tree()?.let { it.displayId == desktop.displayId && it.bounds == desktop.bounds } == true
                }) { result ->
                    if (!current(token)) return@launchAppPair
                    if (result.outcome != LocalOutcome.DISPATCHED) finish(result)
                    else observeAppPair(token,validated.string("firstApp"),validated.string("secondApp"),desktop.displayId,desktop.bounds,
                        SystemClock.elapsedRealtime()+8_000)
                }
            }
            ActionId.SCROLL, ActionId.FEED_NEXT, ActionId.FEED_PREVIOUS -> {
                val access = ui()
                if (access == null) finish(LocalResult(LocalOutcome.REJECTED,"ACCESSIBILITY_PERMISSION_REQUIRED"))
                else access.scroll(if (validated.action == ActionId.SCROLL) validated.string("direction") == "FORWARD" else validated.action == ActionId.FEED_NEXT,
                    validated.action != ActionId.SCROLL) { if (current(token)) finish(screenResult(it)) }
            }
            ActionId.SYSTEM_ACTION -> {
                // Global accessibility actions have no display parameter; do not run on the phone by accident.
                val access = ui()
                if (access?.tree() == null) finish(LocalResult(LocalOutcome.REJECTED,"DEX_DISPLAY_UNAVAILABLE"))
                else if (validated.string("operation") == "SETTINGS") access.openSettings { if (current(token)) finish(screenResult(it)) }
                else finish(LocalResult(LocalOutcome.REJECTED,"CAPABILITY_UNAVAILABLE"))
            }
            ActionId.APP_LAUNCH -> if (registry.appPackage(validated.string("app")) == null) finish(LocalResult(LocalOutcome.REJECTED,"APP_NOT_CONFIGURED"))
                else executor.execute(validated) { if (current(token)) finish(it) }
            else -> executor.execute(validated) { if (current(token)) finish(it) }
        }
    }
    private fun dialerNotice() = if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) ""
        else ", thiếu quyền gọi nên sẽ mở màn hình quay số"
    private fun spokenNumber(number: String) = number.toCharArray().joinToString(" ")
    private fun observeYoutube(token: Long, query: String, deadline: Long, selection: TypedAction? = null) {
        if (!current(token)) return
        val access = ui()
        val tree = access?.tree()
        val queryVisible = tree?.packageName == "com.google.android.youtube" && tree.nodes.any {
            AppUiAdapters.label(it.text) == AppUiAdapters.label(query) && ("search" in it.viewId || "edit" in it.viewId)
        }
        val results = if (queryVisible && tree != null) AppUiAdapters.youtubeResults(tree.nodes).map { DesktopUiAccess.Target(tree,it) } else emptyList()
        if (results.isNotEmpty()) {
            if (selection == null) chooseUi(results) else selectYoutube(results,selection,token)
            return
        }
        if (SystemClock.elapsedRealtime() >= deadline) {
            if (queryVisible) numberedFallback(token) else finish(LocalResult(LocalOutcome.REJECTED,"YOUTUBE_RESULTS_UNAVAILABLE"))
            return
        }
        observer = Runnable { observeYoutube(token,query,deadline,selection) }.also { main.postDelayed(it,250) }
    }
    private fun observeAppPair(token: Long, firstApp: String, secondApp: String, displayId: Int, display: ScreenRect, deadline: Long) {
        if (!current(token)) return
        val firstPackage = registry.appPackage(firstApp)
        val secondPackage = registry.appPackage(secondApp)
        val windows = ui()?.applicationWindows(displayId).orEmpty()
        val arranged = AppPairLayout.verified(windows,firstPackage,secondPackage,display)
        if (arranged) { finish(LocalResult(LocalOutcome.VERIFIED,"APP_PAIR_VERIFIED")); return }
        val first = windows.filter { it.packageName == firstPackage }
        val second = windows.filter { it.packageName == secondPackage }
        if (SystemClock.elapsedRealtime() >= deadline) {
            finish(LocalResult(LocalOutcome.FAILED,if (first.isNotEmpty() && second.isNotEmpty()) "APP_PAIR_LAYOUT_UNVERIFIED" else "APP_PAIR_WINDOW_MISSING")); return
        }
        observer = Runnable { observeAppPair(token,firstApp,secondApp,displayId,display,deadline) }
            .also { main.postDelayed(it,250) }
    }
    private fun selectYoutube(results: List<DesktopUiAccess.Target>, selection: TypedAction, token: Long) {
        val filtered = if (selection.params.containsKey("index")) listOfNotNull(results.getOrNull(selection.int("index")-1))
            else results.filter { AppUiAdapters.label(it.target.label) == AppUiAdapters.label(selection.string("title")) }
        if (filtered.isEmpty()) numberedFallback(token) else chooseUi(filtered)
    }
    private fun chooseUi(targets: List<DesktopUiAccess.Target>) {
        if (targets.isEmpty()) { finish(LocalResult(LocalOutcome.REJECTED,if (ui() == null) "ACCESSIBILITY_PERMISSION_REQUIRED" else "TARGET_NOT_FOUND")); return }
        if (targets.size > 50) { finish(LocalResult(LocalOutcome.REJECTED,"TARGET_TOO_MANY")); return }
        if (targets.size > 1) ownsOverlay = DexAccessibilityService.active?.showNamedTargets(targets.map { it.target.node.bounds }) == true
        choose(targets.take(50).map { target -> Choice(target.target.label) { complete ->
            val access = ui()
            if (access == null) complete(LocalResult(LocalOutcome.REJECTED,"ACCESSIBILITY_PERMISSION_REQUIRED"))
            else access.click(target) { complete(screenResult(it)) }
        } })
    }
    private fun numberedFallback(token: Long) {
        val service = DexAccessibilityService.active
        if (service == null) { finish(LocalResult(LocalOutcome.REJECTED,"ACCESSIBILITY_PERMISSION_REQUIRED")); return }
        if (ui()?.foregroundPackage() != "com.google.android.youtube") { finish(LocalResult(LocalOutcome.REJECTED,"APP_CONTEXT_REQUIRED")); return }
        ownsOverlay = true
        service.prepare(isCurrent={ current(token) }) { status ->
            if (!current(token)) return@prepare
            if (status != "READY") { finish(LocalResult(LocalOutcome.REJECTED,status)); return@prepare }
            val count = service.numberedTargetCount()
            if (count == 0) { finish(LocalResult(LocalOutcome.REJECTED,"TARGET_NOT_FOUND")); return@prepare }
            choose((1..minOf(count,50)).map { index -> Choice("mục số $index trên màn hình") { complete ->
                service.execute(ScreenCommand(ScreenVerb.CLICK_TARGET,index)) { complete(screenResult(it)) }
            } })
        }
    }
    private fun choose(choices: List<Choice>) {
        if (choices.size > 50) { finish(LocalResult(LocalOutcome.REJECTED,"TARGET_TOO_MANY")); return }
        if (choices.isEmpty()) { finish(LocalResult(LocalOutcome.REJECTED,"TARGET_NOT_FOUND")); return }
        if (promptCallback == null) { finish(LocalResult(LocalOutcome.REJECTED,"VOICE_INTERACTION_REQUIRED")); return }
        stepTimer?.let(main::removeCallbacks); stepTimer = null
        pending = PendingInteraction(parent!!,activeActionId,choices)
        emitPrompt(if (choices.size == 1) confirmation(choices.single()) else pagePrompt(pending!!))
    }
    private fun pagePrompt(interaction: PendingInteraction<Choice>) = interaction.pageChoices.mapIndexed { index, choice ->
        "${interaction.page * 5 + index + 1}: ${choice.label.take(100)}"
    }.joinToString(". ","Có ${interaction.choiceCount} lựa chọn. ",
        ". Hãy nói chọn số, đọc lựa chọn tiếp theo, hoặc hủy.")
    private fun confirmation(choice: Choice) = "Bạn chọn ${choice.label.take(200)}. Hãy nói xác nhận, hoặc hủy."
    private fun emitPrompt(text: String) {
        timer?.let(main::removeCallbacks); timer = null
        val interaction = pending ?: return
        // TTS callback loss must not leave the parent command blocked forever.
        val token = generation
        timer = Runnable { if (current(token)) finish(LocalResult(LocalOutcome.REJECTED,"TTS_TIMEOUT")) }
            .also { main.postDelayed(it,30_000) }
        promptCallback?.invoke(Prompt(parent!!,token,interaction.promptVersion,text))
    }
    fun promptFinished(prompt: Prompt, success: Boolean): Boolean {
        if (!current(prompt.token) || parent != prompt.parentId) return false
        val interaction = pending ?: return false
        if (prompt.version != interaction.promptVersion || !interaction.waitingForTts) return false
        timer?.let(main::removeCallbacks)
        if (!interaction.ttsFinished(prompt.version,success,SystemClock.elapsedRealtime())) {
            finish(LocalResult(LocalOutcome.REJECTED,"TTS_FAILED")); return false
        }
        timer = Runnable { if (current(prompt.token) && interaction.expired(SystemClock.elapsedRealtime())) finish(LocalResult(LocalOutcome.REJECTED,"INTERACTION_TIMEOUT")) }
            .also { main.postDelayed(it,10_000) }
        return true
    }
    /** False means a new command; caller abandons the parent and routes this final once. */
    fun continueInteraction(text: String): Boolean {
        val interaction = pending ?: return false
        // A late STT final from before this prompt cannot replace or confirm it.
        if (interaction.waitingForTts) return true
        val reply = when (val parsed = registry.interactions.match(text)) {
            InteractionReply.Confirm -> SpokenReply.Confirm
            InteractionReply.Cancel -> SpokenReply.Cancel
            InteractionReply.Next -> SpokenReply.Next
            InteractionReply.Previous -> SpokenReply.Previous
            is InteractionReply.Select -> SpokenReply.Select(parsed.index)
            null -> null
        }
        when (val result = interaction.reply(reply,SystemClock.elapsedRealtime())) {
            InteractionResult.NewCommand -> { cancel(); return false }
            InteractionResult.Cancelled -> finish(LocalResult(LocalOutcome.REJECTED,"CANCELLED"))
            InteractionResult.Invalid -> { interaction.reprompt(); emitPrompt(if (interaction.selecting) pagePrompt(interaction) else confirmation(interaction.selectedValue!!)) }
            is InteractionResult.PageChanged -> emitPrompt(pagePrompt(interaction))
            is InteractionResult.Selected -> emitPrompt(confirmation(result.value))
            is InteractionResult.Confirmed -> {
                pending = null; timer?.let(main::removeCallbacks); timer = null
                val token = generation
                stepTimer = Runnable { if (current(token)) finish(LocalResult(LocalOutcome.FAILED,"ACTION_TIMEOUT")) }
                    .also { main.postDelayed(it,30_000) }
                result.value.execute { if (current(token)) finish(it) }
            }
        }
        return true
    }
    fun cancel() {
        if (ownsOverlay) DexAccessibilityService.active?.clear()
        ownsOverlay = false
        contactCancellation?.cancel(); contactCancellation = null
        contactTask?.cancel(true); contactTask = null
        contactWorker.purge() // Remove cancelled queued lookups instead of retaining recipient data.
        generation++; pending?.cancel(); pending = null; parent = null; completion = null; promptCallback = null
        timer?.let(main::removeCallbacks); timer = null
        observer?.let(main::removeCallbacks); observer = null
        stepTimer?.let(main::removeCallbacks); stepTimer = null
    }
    /** Application lifetime normally owns this router; only final disposal/replacement closes it. */
    fun close() {
        cancel()
        closed = true
        contactWorker.shutdownNow()
    }
    private fun current(token: Long) = generation == token && parent != null
    private fun finish(result: LocalResult) { val callback = completion; cancel(); callback?.invoke(result) }
    private fun screenResult(status: String) = if (status == "DISPATCHED") LocalResult(LocalOutcome.DISPATCHED) else LocalResult(LocalOutcome.REJECTED,status)
}
