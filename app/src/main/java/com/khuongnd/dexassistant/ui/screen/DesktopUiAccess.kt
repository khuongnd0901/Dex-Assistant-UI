package com.khuongnd.dexassistant.ui.screen

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

/** Every tree read and gesture is explicitly bound to the unique Samsung desktop. */
class DesktopUiAccess(private val service: DexAccessibilityService) {
    private val handler = Handler(Looper.getMainLooper())
    data class Tree(val displayId: Int, val bounds: ScreenRect, val packageName: String,
        val windowId: Int, val windows: List<ScreenWindow>, val nodes: List<UiNodeRecord>)
    data class Target(val tree: Tree, val target: AppUiTarget) {
        fun unchanged(latest: Tree?): Boolean {
            if (latest == null || latest.displayId != tree.displayId || latest.bounds != tree.bounds ||
                latest.windowId != tree.windowId || latest.windows != tree.windows || latest.packageName != tree.packageName) return false
            val path = target.node.path
            fun subtree(value: Tree) = value.nodes.filter { it.path == path || it.path.startsWith("$path/") }
            return subtree(latest) == subtree(tree) && latest.nodes.any { it == target.node }
        }
    }
    data class AppWindow(val packageName: String, val bounds: ScreenRect)

    /** Read application task windows on the selected Samsung desktop, not the phone display. */
    fun applicationWindows(expectedDisplayId: Int? = null): List<AppWindow> = runCatching {
        val display = service.getSystemService(DisplayManager::class.java)
            .getDisplays(DexAccessibilityService.SAMSUNG_DESKTOP_CATEGORY)
            .filter { it.isValid && it.displayId != Display.DEFAULT_DISPLAY }.singleOrNull() ?: return emptyList()
        if (expectedDisplayId != null && display.displayId != expectedDisplayId) return emptyList()
        service.windowsOnAllDisplays[display.displayId].orEmpty()
            .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .mapNotNull { window ->
                val root = window.root ?: return@mapNotNull null
                try {
                    val packageName = root.packageName?.toString() ?: return@mapNotNull null
                    val rect = Rect(); window.getBoundsInScreen(rect)
                    AppWindow(packageName,ScreenRect(rect.left,rect.top,rect.right,rect.bottom))
                } finally { recycle(root) }
            }
    }.getOrDefault(emptyList())

    fun tree(): Tree? = runCatching {
        val display = service.getSystemService(DisplayManager::class.java)
            .getDisplays(DexAccessibilityService.SAMSUNG_DESKTOP_CATEGORY)
            .filter { it.isValid && it.displayId != Display.DEFAULT_DISPLAY }.singleOrNull() ?: return null
        val size = android.graphics.Point(); display.getRealSize(size)
        val screen = ScreenRect(0, 0, size.x, size.y)
        val windows = service.windowsOnAllDisplays[display.displayId].orEmpty()
            .filter { it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }.sortedByDescending { it.layer }
        val chosen = windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && (it.isFocused || it.isActive) } ?: return null
        val topology = windows.map { window ->
            val rect = Rect(); window.getBoundsInScreen(rect)
            ScreenWindow(window.id,window.layer,window.type,ScreenRect(rect.left,rect.top,rect.right,rect.bottom),window.isFocused,window.isActive)
        }
        val obscuring = topology.filter { it.layer > chosen.layer }.map { it.bounds }
        val root = chosen.root ?: return null
        val packageName = root.packageName?.toString() ?: run { recycle(root); return null }
        val records = mutableListOf<UiNodeRecord>()
        var visited = 0
        fun walk(node: AccessibilityNodeInfo, path: String, parent: String?, depth: Int) {
            if (++visited > 4096 || depth > 40 || !node.refresh()) return
            val rect = Rect(); node.getBoundsInScreen(rect)
            val bounds = ScreenRect(rect.left,rect.top,rect.right,rect.bottom)
            if (node.isVisibleToUser && node.isEnabled && bounds.width > 0 && bounds.height > 0 && bounds.inside(screen) &&
                obscuring.none(bounds::intersects)) {
                records.add(UiNodeRecord(path,parent,bounds,node.text?.toString().orEmpty(),node.contentDescription?.toString().orEmpty(),
                    node.viewIdResourceName.orEmpty(),node.isClickable || node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK },
                    node.isScrollable,node.actionList.map { it.id }.toSet(),node.hashCode().toString()))
            }
            for (i in 0 until minOf(128,node.childCount)) {
                if (visited >= 4096) break
                node.getChild(i)?.let { child ->
                try { walk(child,"$path/$i",path,depth+1) } finally { recycle(child) }
                }
            }
        }
        try { walk(root,"root",null,0) } finally { recycle(root) }
        Tree(display.displayId,screen,packageName,chosen.id,topology,records.toList())
    }.getOrNull()

    /** Context routing needs window identity only; do not traverse/refresh thousands of nodes. */
    fun foregroundPackage(): String? = runCatching {
        val display = service.getSystemService(DisplayManager::class.java)
            .getDisplays(DexAccessibilityService.SAMSUNG_DESKTOP_CATEGORY)
            .filter { it.isValid && it.displayId != Display.DEFAULT_DISPLAY }.singleOrNull() ?: return null
        val chosen = service.windowsOnAllDisplays[display.displayId].orEmpty()
            .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && (it.isFocused || it.isActive) }
            .maxByOrNull { it.layer } ?: return null
        val root = chosen.root ?: return null
        try { root.packageName?.toString() } finally { recycle(root) }
    }.getOrNull()
    fun targets(text: String): List<Target> = tree()?.let { tree -> AppUiAdapters.textTargets(tree.nodes,text).map { Target(tree,it) } }.orEmpty()
    fun youtubeResults(): List<Target> = tree()?.takeIf { it.packageName == "com.google.android.youtube" }
        ?.let { tree -> AppUiAdapters.youtubeResults(tree.nodes).map { Target(tree,it) } }.orEmpty()
    fun fullscreen(enabled: Boolean): List<Target> = tree()?.takeIf { it.packageName == "com.google.android.youtube" }
        ?.let { tree -> AppUiAdapters.fullscreen(tree.nodes,enabled).map { Target(tree,it) } }.orEmpty()

    private fun revalidated(target: Target): Tree? {
        val latest = tree() ?: return null
        if (!target.unchanged(latest)) return null
        return latest
    }
    fun click(target: Target, complete: (String) -> Unit) {
        val latest = revalidated(target)
        if (latest == null) { complete("SCREEN_CHANGED"); return }
        // Scoped coordinates avoid ACTION_CLICK being rerouted to a newly focused phone window.
        val bounds = target.target.node.bounds
        gesture(latest.displayId,bounds.x,bounds.y,bounds.x,bounds.y,80,complete)
    }

    fun scroll(forward: Boolean, feedOnly: Boolean, complete: (String) -> Unit) {
        val frozen = tree()
        if (frozen == null) { complete("DEX_DISPLAY_UNAVAILABLE"); return }
        if (feedOnly && frozen.packageName !in setOf("com.zhiliaoapp.musically","com.ss.android.ugc.trill")) {
            complete("APP_CONTEXT_REQUIRED"); return
        }
        val feed = AppUiAdapters.feed(frozen.nodes,frozen.packageName)
        if (feedOnly && feed == null) { complete("APP_CONTEXT_REQUIRED"); return }
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        val supported = frozen.nodes.filter { action in it.actions &&
            (!feedOnly || it.path == feed?.path || it.path.startsWith("${feed?.path}/")) }
        // Multiple nested lists are ambiguous. Never retry a dispatch with a gesture.
        val node = supported.singleOrNull()
        if (node != null) {
            val target = Target(frozen,AppUiTarget(node,"cuộn"))
            if (revalidated(target) == null) { complete("SCREEN_CHANGED"); return }
            val window = service.windowsOnAllDisplays[frozen.displayId].orEmpty().firstOrNull { it.id == frozen.windowId }
            var current = window?.root
            try {
                node.path.removePrefix("root").trim('/').takeIf { it.isNotBlank() }?.split('/')?.forEach { index ->
                    val next = current?.getChild(index.toInt()); current?.let(::recycle); current = next
                }
                if (current?.refresh() != true) { complete("SCREEN_CHANGED"); return }
                complete(if (current?.performAction(action) == true) "DISPATCHED" else "SCROLL_UNAVAILABLE")
            } finally { current?.let(::recycle) }
            return
        }
        if (supported.isNotEmpty()) { complete("TARGET_AMBIGUOUS"); return }
        if (feed == null) { complete("SCROLL_UNAVAILABLE"); return }
        if (revalidated(Target(frozen,AppUiTarget(feed,"feed"))) == null) { complete("SCREEN_CHANGED"); return }
        val bounds = feed.bounds
        val top = bounds.top + bounds.height * .25f; val bottom = bounds.top + bounds.height * .75f
        gesture(frozen.displayId,bounds.x,if (forward) bottom else top,bounds.x,if (forward) top else bottom,350,complete)
    }
    fun openSettings(complete: (String) -> Unit) {
        val frozen = tree() ?: run { complete("DEX_DISPLAY_UNAVAILABLE"); return }
        val latest = tree() ?: run { complete("DEX_DISPLAY_UNAVAILABLE"); return }
        if (frozen.displayId != latest.displayId || frozen.bounds != latest.bounds) { complete("SCREEN_CHANGED"); return }
        val options = android.app.ActivityOptions.makeBasic().setLaunchDisplayId(frozen.displayId)
        val result = runCatching { service.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),options.toBundle()) }
        complete(if (result.isSuccess) "DISPATCHED" else "CAPABILITY_UNAVAILABLE")
    }
    private fun gesture(displayId: Int, x1: Float, y1: Float, x2: Float, y2: Float, duration: Long, complete: (String) -> Unit) {
        val path = Path().apply { moveTo(x1,y1); lineTo(x2,y2) }
        val gesture = GestureDescription.Builder().setDisplayId(displayId)
            .addStroke(GestureDescription.StrokeDescription(path,0,duration)).build()
        var terminal = false
        fun finish(status: String) { if (!terminal) { terminal = true; complete(status) } }
        val timeout = Runnable { finish("SCREEN_GESTURE_TIMEOUT") }
        handler.postDelayed(timeout,2000)
        val accepted = runCatching { service.dispatchGesture(gesture,object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) { handler.removeCallbacks(timeout); finish("DISPATCHED") }
            override fun onCancelled(gestureDescription: GestureDescription?) { handler.removeCallbacks(timeout); finish("SCREEN_GESTURE_CANCELLED") }
        },handler) }.getOrDefault(false)
        if (!accepted) { handler.removeCallbacks(timeout); finish("SCREEN_GESTURE_REJECTED") }
    }
    @Suppress("DEPRECATION") private fun recycle(node: AccessibilityNodeInfo) = node.recycle()
}
