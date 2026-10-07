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
    private val youtube = YoutubeCommandHandler()
    private var pending: PendingInteraction<Choice>? = null
    private var completion: ((LocalResult) -> Unit)? = null
    private var promptCallback: ((Prompt) -> Unit)? = null
    private var metricCallback: ((String, Long) -> Unit)? = null
    private fun metric(name: String) { metricCallback?.invoke(name, SystemClock.elapsedRealtimeNanos()) }
    private fun executeNative(action: TypedAction, complete: (LocalResult) -> Unit) {
        metric("action_start")
        executor.execute(action, complete)
    }
    private var timer: Runnable? = null
    private var observer: Runnable? = null
    private var stepTimer: Runnable? = null
    val awaitingInteraction get() = pending != null
    private fun ui() = DexAccessibilityService.active?.let(::DesktopUiAccess)
    fun foregroundAppId(): String? = ui()?.foregroundPackage()?.let(registry::appIdForPackage)
    fun supportedActions(): Set<ActionId> = ActionId.entries.toSet() - setOf(ActionId.BLUETOOTH_SET, ActionId.CLICK_TARGET, ActionId.APP_CLOSE_WINDOW)

    fun execute(parentId: String, action: TypedAction, prompt: ((Prompt) -> Unit)?, actionId: String = "local", localCommandId: String? = null, metric: ((String, Long) -> Unit)? = null, complete: (LocalResult) -> Unit) {
        if (closed) { complete(LocalResult(LocalOutcome.REJECTED,"ROUTER_CLOSED")); return }
        cancel()
        parent = parentId; activeActionId = actionId; completion = complete; promptCallback = prompt; metricCallback = metric
        youtube.clearForParent(parentId)
        val token = generation
        stepTimer = Runnable { if (current(token)) finish(LocalResult(LocalOutcome.FAILED,"ACTION_TIMEOUT")) }
            .also { main.postDelayed(it,30_000) }
        val validated = runCatching { ActionCatalog.validate(action.action.name,action.params,
            if (action.action in ActionCatalog.schemaTwoOnlyActions) ActionCatalog.semanticSchemaVersion else ActionCatalog.schemaVersion) }.getOrNull()
        if (validated == null) { finish(LocalResult(LocalOutcome.REJECTED,"INVALID_ARGUMENTS")); return }
        when (validated.action) {
            ActionId.APP_CLOSE_WINDOW -> finish(LocalResult(LocalOutcome.REJECTED, "CAPABILITY_UNAVAILABLE"))
            ActionId.APP_LAUNCH -> {
                executeNative(validated) { result ->
                    if (current(token)) {
                        if (validated.string("app") == "youtube" && result.outcome == LocalOutcome.DISPATCHED)
                            observeYoutubePreview(token, null, SystemClock.elapsedRealtime() + 10_000)
                        else finish(result)
                    }
                }
            }
            ActionId.CLICK_TARGET -> {
                val spec = validated.params["target"] as Map<*, *>
                val appPackage = registry.appPackage(spec["appId"] as String)
                val access = ui()
                if (appPackage == null) finish(LocalResult(LocalOutcome.REJECTED,"APP_NOT_CONFIGURED"))
                else if (access == null) finish(LocalResult(LocalOutcome.REJECTED,"ACCESSIBILITY_PERMISSION_REQUIRED"))
                else if (access.tree()?.packageName != appPackage) finish(LocalResult(LocalOutcome.REJECTED,"APP_CONTEXT_REQUIRED"))
                else {
                    val selector = SemanticUiResolver.Selector(
                        resourceId=spec["resourceId"] as? String, text=spec["text"] as? String,
                        contentDescription=spec["contentDescription"] as? String,
                        contextText=spec["contextText"] as? String, role=spec["role"] as? String,
                        ordinal=spec["ordinal"] as? Int)
                    metric("ui_resolution_start")
                    val targets = access.semanticTargets(appPackage,selector)
                    metric("ui_resolution_end")
                    chooseUi(targets)
                }
            }
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
                                else { metric("action_start"); executor.executeConfirmedCall(candidate,it) }
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
                    else { metric("action_start"); executor.executeConfirmedNumber(validated.string("number"),it) }
                }))
            }
            ActionId.CLICK_TEXT -> {
                metric("ui_resolution_start")
                val targets = ui()?.targets(validated.string("text")).orEmpty()
                metric("ui_resolution_end")
                chooseUi(targets)
            }
            ActionId.YOUTUBE_SEARCH -> executeNative(validated) { result ->
                if (current(token)) {
                    if (result.outcome == LocalOutcome.DISPATCHED) {
                        youtube.searched(parentId,validated.string("query"))
                        observeYoutubePreview(token, validated.string("query"), SystemClock.elapsedRealtime() + 10_000)
                    } else finish(result)
                }
            }
            ActionId.YOUTUBE_SELECT -> {
                metric("path_semantic")
                if (validated.params.containsKey("index")) {
                    when (val preview = DexAccessibilityService.active?.consumeYoutubePreview()) {
                        is SemanticPreviewSession.Selection.Ready -> {
                            if (validated.int("index") !in 1..preview.targets.size) finish(LocalResult(LocalOutcome.REJECTED,"TARGET_NOT_FOUND"))
                            else selectYoutube(preview.targets, validated, token)
                            return
                        }
                        SemanticPreviewSession.Selection.Stale -> { finish(LocalResult(LocalOutcome.REJECTED,"SCREEN_CHANGED")); return }
                        else -> Unit
                    }
                }
                val expected = youtube.queryForSelection(parentId)
                if (expected != null) observeYoutube(token, expected, SystemClock.elapsedRealtime()+10_000, validated)
                else {
                    metric("ui_resolution_start")
                    val results = ui()?.youtubeResults().orEmpty()
                    metric("ui_resolution_end")
                    selectYoutube(results,validated,token)
                }
            }
            ActionId.YOUTUBE_FULLSCREEN -> chooseUi(ui()?.fullscreen(validated.boolean("enabled")).orEmpty())
            ActionId.YOUTUBE_PLAY_QUERY -> {
                executeNative(ActionCatalog.validate("YOUTUBE_SEARCH",mapOf("query" to validated.string("query")))) { result ->
                    if (!current(token)) return@executeNative
                    if (result.outcome !in setOf(LocalOutcome.DISPATCHED,LocalOutcome.VERIFIED)) finish(result)
                    else observeYoutube(token,validated.string("query"),SystemClock.elapsedRealtime()+10_000)
                }
            }
            ActionId.MEDIA_SEEK -> {
                if (foregroundAppId() !in setOf("youtube", "youtube_music")) finish(LocalResult(LocalOutcome.REJECTED,"APP_CONTEXT_REQUIRED"))
                else executeNative(validated) { if (current(token)) finish(it) }
            }
            ActionId.WEB_SEARCH -> {
                if (localCommandId == "web_search_in_chrome") {
                    if (foregroundAppId() != "chrome") finish(LocalResult(LocalOutcome.REJECTED,"APP_CONTEXT_REQUIRED"))
                    else { metric("action_start"); finish(executor.executeWebSearchInApp("địa điểm ${validated.string("query")}", registry.appPackage("chrome"))) }
                } else executeNative(validated) { if (current(token)) finish(it) }
            }
            ActionId.APP_PAIR_LAUNCH -> {
                val access = ui()
                val desktop = access?.desktop()
                if (access == null || desktop == null) finish(LocalResult(LocalOutcome.REJECTED,"DEX_DISPLAY_UNAVAILABLE"))
                else if (registry.appPackage(validated.string("firstApp")) == null || registry.appPackage(validated.string("secondApp")) == null)
                    finish(LocalResult(LocalOutcome.REJECTED,"APP_NOT_CONFIGURED"))
                else { metric("action_start"); executor.launchAppPair(validated.string("firstApp"),validated.string("secondApp"),desktop.displayId,desktop.bounds,{
                    current(token) && ui()?.desktop() == desktop
                }) { result ->
                    if (!current(token)) return@launchAppPair
                    if (result.outcome != LocalOutcome.DISPATCHED) finish(result)
                    else observeAppPair(token,validated.string("firstApp"),validated.string("secondApp"),desktop.displayId,desktop.bounds,
                        SystemClock.elapsedRealtime()+8_000)
                } }
            }
            ActionId.SCROLL, ActionId.FEED_NEXT, ActionId.FEED_PREVIOUS -> {
                val access = ui()
                if (access == null) finish(LocalResult(LocalOutcome.REJECTED,"ACCESSIBILITY_PERMISSION_REQUIRED"))
                else { metric("action_start"); access.scroll(if (validated.action == ActionId.SCROLL) validated.string("direction") == "FORWARD" else validated.action == ActionId.FEED_NEXT,
                    validated.action != ActionId.SCROLL) { if (current(token)) finish(screenResult(it)) }
                }
            }
            ActionId.SYSTEM_ACTION -> {
                // Global accessibility actions have no display parameter; do not run on the phone by accident.
                val access = ui()
                if (access?.tree() == null) finish(LocalResult(LocalOutcome.REJECTED,"DEX_DISPLAY_UNAVAILABLE"))
                else if (validated.string("operation") == "SETTINGS") { metric("action_start"); access.openSettings { if (current(token)) finish(screenResult(it)) } }
                else finish(LocalResult(LocalOutcome.REJECTED,"CAPABILITY_UNAVAILABLE"))
            }
            ActionId.SEARCH_PLACE -> {
                val desktop = ui()?.desktop()
                if (desktop == null) finish(LocalResult(LocalOutcome.REJECTED,"DEX_DISPLAY_UNAVAILABLE"))
                else { metric("action_start"); finish(executor.executePlaceOnDesktop(validated.string("query"), desktop.displayId)) }
            }
            ActionId.APP_LAUNCH -> if (registry.appPackage(validated.string("app")) == null) finish(LocalResult(LocalOutcome.REJECTED,"APP_NOT_CONFIGURED"))
                else executeNative(validated) { if (current(token)) finish(it) }
            else -> executeNative(validated) { if (current(token)) finish(it) }
        }
    }
    private fun dialerNotice() = if (context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) ""
        else ", thiếu quyền gọi nên sẽ mở màn hình quay số"
    private fun spokenNumber(number: String) = number.toCharArray().joinToString(" ")
    private fun observeYoutube(token: Long, query: String, deadline: Long, selection: TypedAction? = null) {
        if (!current(token)) return
        metric("path_semantic")
        metric("ui_resolution_start")
        val access = ui()
        val tree = access?.tree()
        val queryVisible = tree?.packageName == "com.google.android.youtube" && tree.nodes.any {
            AppUiAdapters.label(it.text) == AppUiAdapters.label(query) && ("search" in it.viewId || "edit" in it.viewId)
        }
        val results = tree?.takeIf { queryVisible }?.let { snapshot ->
            AppUiAdapters.youtubeResults(snapshot.nodes).map { DesktopUiAccess.Target(snapshot,it) }
        }.orEmpty()
        metric("ui_resolution_end")
        when (youtube.observe(results.map { it.target }, queryVisible, SystemClock.elapsedRealtime() >= deadline, selection)) {
            YoutubeCommandHandler.Observation.CHOOSE -> if (selection == null) chooseUi(results) else selectYoutube(results,selection,token)
            YoutubeCommandHandler.Observation.AMBIGUOUS -> finish(LocalResult(LocalOutcome.REJECTED,"TARGET_AMBIGUOUS"))
            YoutubeCommandHandler.Observation.NUMBERED_FALLBACK -> numberedFallback(token)
            YoutubeCommandHandler.Observation.UNAVAILABLE -> finish(LocalResult(LocalOutcome.REJECTED,"YOUTUBE_RESULTS_UNAVAILABLE"))
            YoutubeCommandHandler.Observation.WAIT -> observer = Runnable { observeYoutube(token,query,deadline,selection) }
                .also { main.postDelayed(it,250) }
        }
    }
    private fun observeYoutubePreview(token: Long, query: String?, deadline: Long) {
        if (!current(token)) return
        metric("path_semantic"); metric("ui_resolution_start")
        val tree = ui()?.tree()
        val queryVisible = tree != null && tree.packageName == registry.appPackage("youtube") && (query == null || tree.nodes.any {
            AppUiAdapters.label(it.text) == AppUiAdapters.label(query) && ("search" in it.viewId || "edit" in it.viewId)
        })
        val targets = tree?.takeIf { queryVisible }?.let { snapshot ->
            AppUiAdapters.youtubeResults(snapshot.nodes).map { DesktopUiAccess.Target(snapshot, it) }
        }.orEmpty()
        metric("ui_resolution_end")
        when (youtube.preview(targets.map { it.target }, query != null, queryVisible, SystemClock.elapsedRealtime() >= deadline)) {
            YoutubeCommandHandler.Observation.CHOOSE -> {
                if (DexAccessibilityService.active?.showYoutubePreview(targets) == true)
                    finish(LocalResult(LocalOutcome.VERIFIED, if (query == null) "YOUTUBE_INDEX_READY" else "YOUTUBE_RESULTS_READY"))
                else finish(LocalResult(LocalOutcome.REJECTED,"SCREEN_CHANGED"))
                return
            }
            YoutubeCommandHandler.Observation.AMBIGUOUS -> { finish(LocalResult(LocalOutcome.REJECTED,"TARGET_AMBIGUOUS")); return }
            YoutubeCommandHandler.Observation.UNAVAILABLE -> {
                finish(LocalResult(LocalOutcome.REJECTED, if (query != null && !queryVisible) "YOUTUBE_SEARCH_UNVERIFIED" else "TARGET_NOT_FOUND"))
                return
            }
            else -> Unit
        }
        observer = Runnable { observeYoutubePreview(token, query, deadline) }.also { main.postDelayed(it, 250) }
    }
    private fun observeAppPair(token: Long, firstApp: String, secondApp: String, displayId: Int, display: ScreenRect, deadline: Long) {
        if (!current(token)) return
        val firstPackage = registry.appPackage(firstApp)
        val secondPackage = registry.appPackage(secondApp)
        val windows = ui()?.applicationWindows(displayId).orEmpty()
        val arranged = AppPairLayout.verified(windows,firstPackage,secondPackage,display)
        if (arranged || SystemClock.elapsedRealtime() >= deadline) {
            val scoped = windows.filter { it.packageName == registry.appPackage(firstApp) || it.packageName == registry.appPackage(secondApp) }
            android.util.Log.i("DexAppPair", "observation displayId=$displayId verified=$arranged scopedWindows=" +
                scoped.joinToString(";") { "${it.packageName}:${it.bounds.left},${it.bounds.top},${it.bounds.right},${it.bounds.bottom}" })
        }
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
        when (val selected = youtube.select(results.map { it.target }, selection)) {
            is YoutubeCommandHandler.Selection.Matches -> chooseUi(selected.indexes.map(results::get))
            YoutubeCommandHandler.Selection.Ambiguous -> finish(LocalResult(LocalOutcome.REJECTED,"TARGET_AMBIGUOUS"))
            YoutubeCommandHandler.Selection.NotFound -> numberedFallback(token)
        }
    }
    private fun chooseUi(targets: List<DesktopUiAccess.Target>) {
        metric("path_semantic")
        if (targets.isEmpty()) { finish(LocalResult(LocalOutcome.REJECTED,if (ui() == null) "ACCESSIBILITY_PERMISSION_REQUIRED" else "TARGET_NOT_FOUND")); return }
        if (targets.size > 50) { finish(LocalResult(LocalOutcome.REJECTED,"TARGET_TOO_MANY")); return }
        if (targets.size > 1) ownsOverlay = DexAccessibilityService.active?.showNamedTargets(targets.map { it.target.node.bounds }) == true
        choose(targets.take(50).map { target -> Choice(listOf(target.target.label,target.target.contextText)
            .filter { it.isNotBlank() }.joinToString(", ")) { complete ->
            val access = ui()
            if (access == null) complete(LocalResult(LocalOutcome.REJECTED,"ACCESSIBILITY_PERMISSION_REQUIRED"))
            else { access.click(target, beforeDispatch = { metric("action_start") },
                resolutionMetric = ::metric) { complete(screenResult(it)) } }
        } })
    }
    private fun numberedFallback(token: Long) {
        metric("path_fallback")
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
                metric("action_start")
                service.execute(ScreenCommand(ScreenVerb.CLICK_TARGET,index)) { complete(screenResult(it)) }
            } })
        }
    }
    private fun choose(choices: List<Choice>) {
        if (choices.size > 50) { finish(LocalResult(LocalOutcome.REJECTED,"TARGET_TOO_MANY")); return }
        if (choices.isEmpty()) { finish(LocalResult(LocalOutcome.REJECTED,"TARGET_NOT_FOUND")); return }
        if (promptCallback == null) { finish(LocalResult(LocalOutcome.REJECTED,"VOICE_INTERACTION_REQUIRED")); return }
        stepTimer?.let(main::removeCallbacks); stepTimer = null
        metric("confirmation_start")
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
                metric("confirmation_end")
                metric("execution_resume")
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
        if (pending != null) metric("confirmation_end")
        if (ownsOverlay) DexAccessibilityService.active?.clear()
        ownsOverlay = false
        contactCancellation?.cancel(); contactCancellation = null
        contactTask?.cancel(true); contactTask = null
        contactWorker.purge() // Remove cancelled queued lookups instead of retaining recipient data.
        generation++; pending?.cancel(); pending = null; parent = null; completion = null; promptCallback = null; metricCallback = null
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
