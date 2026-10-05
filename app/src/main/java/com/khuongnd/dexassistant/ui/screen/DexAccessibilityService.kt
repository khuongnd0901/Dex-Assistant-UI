package com.khuongnd.dexassistant.ui.screen

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.Surface
import android.view.SurfaceControl
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import java.util.UUID
import java.util.concurrent.Executors
import com.khuongnd.dexassistant.ui.voice.VoiceCommandActivation
import com.khuongnd.dexassistant.ui.voice.VoiceHardwareKeyGate

/** Display identity and immutable session mapping are checked again immediately before a gesture. */
class DexAccessibilityService : AccessibilityService(), DisplayManager.DisplayListener {
    private val handler = Handler(Looper.getMainLooper())
    private val voiceKeys = VoiceHardwareKeyGate()
    private val analyzer = Executors.newSingleThreadExecutor()
    private val displays get() = getSystemService(DisplayManager::class.java)
    private var epoch = 0L
    private val gestureGate = ScreenSessionGate()
    private var session: Session? = null
    private var preparingDisplayId: Int? = null
    private var preparingSnapshot: ScreenSnapshot? = null
    private var overlay: View? = null
    private var windowManager: WindowManager? = null
    private var overlaySurface: Surface? = null
    private var overlayControl: SurfaceControl? = null
    private val expiry = Runnable { clear() }
    private data class Session(val id: String, val snapshot: ScreenSnapshot, val grid: ScreenRect?, val depth: Int)

    override fun onServiceConnected() {
        active = this
        displays.registerDisplayListener(this, handler)
    }
    override fun onInterrupt() = clear()
    override fun onDestroy() {
        voiceKeys.clear()
        if (active === this) active = null
        displays.unregisterDisplayListener(this)
        clear(); analyzer.shutdownNow(); super.onDestroy()
    }
    override fun onKeyEvent(event: KeyEvent): Boolean = voiceKeys.onKey(
        event.deviceId, event.device?.isExternal == true, event.keyCode, event.action,
        event.repeatCount, event.isCanceled, event.downTime,
    ) { VoiceCommandActivation.request(this) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val current = session
        // A grid is an explicit coordinate tool, not a frozen app/node map. A popup,
        // scroll or app switch does not change its cell coordinates on this display.
        if (current?.grid != null) { validateDesktopDisplay(); return }
        val displayId = current?.snapshot?.displayId ?: preparingDisplayId ?: return
        if (event?.eventType in setOf(AccessibilityEvent.TYPE_WINDOWS_CHANGED, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_VIEW_SCROLLED)) {
            val changedWindow = windowsOnAllDisplays[displayId].orEmpty().firstOrNull { it.id == event?.windowId }
            if (changedWindow?.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) return
            val expected = current?.snapshot ?: preparingSnapshot ?: return
            // Video/clock content events do not imply target movement. Compare the frozen
            // node/window identity for both preparation and active sessions; image pixels
            // are separately revalidated from a fresh screenshot immediately before a tap.
            val stateChange = event?.eventType in setOf(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED) && changedWindow != null
            val grid = current?.grid != null
            if (stateChange || !matches(expected, snapshot(grid), grid, "content")) {
                android.util.Log.i("DexScreen", "session.invalidated stage=snapshot eventType=${event?.eventType}")
                clear()
            }
        }
    }

    override fun onDisplayAdded(displayId: Int) = validateDesktopDisplay()
    override fun onDisplayRemoved(displayId: Int) = validateDesktopDisplay()
    override fun onDisplayChanged(displayId: Int) = validateDesktopDisplay()
    private fun validateDesktopDisplay() {
        val expected = session?.snapshot ?: preparingSnapshot ?: return
        val desktops = displays.getDisplays(SAMSUNG_DESKTOP_CATEGORY).filter { it.isValid }.map { display ->
            val size = android.graphics.Point(); display.getRealSize(size)
            display.displayId to ScreenRect(0,0,size.x,size.y)
        }
        if (!ScreenDisplays.matches(expected.displayId, expected.bounds, desktops)) {
            android.util.Log.i("DexScreen", "session.invalidated stage=display_identity_geometry")
            clear()
        }
    }

    /** Screenshot timeout and failures leave accessibility/grid available. */
    fun prepare(isCurrent: () -> Boolean = { true }, complete: (String) -> Unit) {
        clear()
        val token = epoch
        val original = snapshot()
        if (original == null) { complete("DEX_DISPLAY_UNAVAILABLE"); return }
        preparingDisplayId = original.displayId; preparingSnapshot = original
        var completed = false
        fun finish(image: List<ScreenRect> = emptyList(), fingerprints: Map<ScreenRect, Long> = emptyMap()) {
            if (completed) return
            completed = true
            if (epoch != token) { complete("SCREEN_SESSION_EXPIRED"); return }
            if (!isCurrent()) { clear(); complete("SCREEN_SESSION_EXPIRED"); return }
            preparingDisplayId = null; preparingSnapshot = null
            val latest = snapshot()
            if (!matches(original, latest, false, "prepare")) { complete("SCREEN_CHANGED"); return }
            val targets = ScreenTargets.merge(original.targets, image)
            session = Session(UUID.randomUUID().toString(), original, if (targets.isEmpty()) original.bounds else null, 0)
            gestureGate.activate(session!!.id)
            imageTargets = targets
            imageFingerprints = fingerprints
            runCatching { show() }.onSuccess {
                handler.postDelayed(expiry, 35_000); complete("READY")
            }.onFailure { reportOverlayFailure(it); clear(); complete("SCREEN_OVERLAY_FAILED") }
        }
        val fallback = Runnable { android.util.Log.i("DexScreen", "prepare.fallback reason=deadline"); finish() }
        handler.postDelayed(fallback, 1500)
        runCatching {
            takeScreenshot(original.displayId, mainExecutor, object : TakeScreenshotCallback {
                override fun onFailure(errorCode: Int) { android.util.Log.i("DexScreen", "prepare.fallback captureError=$errorCode"); handler.removeCallbacks(fallback); finish() }
                override fun onSuccess(screenshot: ScreenshotResult) {
                    if (epoch != token || completed) { screenshot.hardwareBuffer.close(); return }
                    val bitmap = copyScreenshot(screenshot)
                    if (bitmap == null) { handler.removeCallbacks(fallback); finish(); return }
                    val analysis = Runnable {
                        val started = android.os.SystemClock.elapsedRealtime()
                        var fingerprints: Map<ScreenRect, Long> = emptyMap()
                        val found = runCatching {
                            val scale = minOf(1f, 640f / bitmap.width, 400f / bitmap.height)
                            val small = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1),
                                (bitmap.height * scale).toInt().coerceAtLeast(1), false)
                            try {
                                val pixels = IntArray(small.width * small.height)
                                small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
                                ScreenImageDetector.detect(pixels, small.width, small.height).map {
                                    ScreenRect((it.left / scale).toInt(), (it.top / scale).toInt(),
                                        (it.right / scale).toInt(), (it.bottom / scale).toInt())
                                }.filter { it.inside(original.bounds) }
                            } finally { if (small !== bitmap) small.recycle() }
                        }.getOrDefault(emptyList())
                        fingerprints = runCatching { found.associateWith { fingerprint(bitmap, it) } }.getOrDefault(emptyMap())
                        bitmap.recycle()
                        android.util.Log.i("DexScreen", "prepare.analysis candidates=${found.size} durationMs=${android.os.SystemClock.elapsedRealtime() - started}")
                        handler.post { handler.removeCallbacks(fallback); finish(found, fingerprints) }
                    }
                    runCatching { analyzer.execute(analysis) }.onFailure {
                        bitmap.recycle(); handler.removeCallbacks(fallback); finish()
                    }
                }
            })
        }.onFailure { handler.removeCallbacks(fallback); finish() }
    }
    private var imageTargets: List<ScreenRect> = emptyList()
    private var imageFingerprints: Map<ScreenRect, Long> = emptyMap()
    fun numberedTargetCount(): Int = if (session?.grid == null) imageTargets.size else 0
    fun showNamedTargets(targets: List<ScreenRect>): Boolean {
        clear()
        val original = snapshot() ?: return false
        if (targets.isEmpty() || targets.any { !it.inside(original.bounds) }) return false
        session = Session(UUID.randomUUID().toString(), original, null, 0)
        gestureGate.activate(session!!.id)
        imageTargets = targets.toList()
        return runCatching { show(); handler.postDelayed(expiry, 35_000); true }.getOrElse { clear(); false }
    }

    fun clear() {
        epoch++
        gestureGate.invalidate()
        handler.removeCallbacks(expiry)
        removeOverlay()
        session = null; preparingDisplayId = null; preparingSnapshot = null; imageTargets = emptyList(); imageFingerprints = emptyMap()
    }
    fun execute(command: ScreenCommand, complete: (String) -> Unit) {
        if (command.verb == ScreenVerb.CANCEL) { clear(); complete("CANCELLED"); return }
        if (command.verb == ScreenVerb.INVALID) { clear(); complete("INVALID_SCREEN_COMMAND"); return }
        if (command.verb == ScreenVerb.GRID) { freshGrid(complete); return }
        val current = session
        if (current == null) { complete("SCREEN_SESSION_EXPIRED"); return }
        if (!gestureGate.isCurrent(current.id) || !matches(current.snapshot, snapshot(current.grid != null), current.grid != null, "execute")) { clear(); complete("SCREEN_CHANGED"); return }
        val number = command.number ?: 0
        if (command.verb == ScreenVerb.SUBDIVIDE) {
            val cell = current.grid?.cell(number)
            if (cell == null || current.depth >= 3) { clear(); complete("INVALID_SCREEN_CELL"); return }
            session = current.copy(grid = cell, depth = current.depth + 1)
            handler.removeCallbacks(expiry); handler.postDelayed(expiry, 35_000)
            refreshOverlay(complete); return
        }
        val target = when (command.verb) {
            ScreenVerb.CLICK_CELL -> current.grid?.cell(number)
            ScreenVerb.CLICK_TARGET -> if (current.grid == null) imageTargets.getOrNull(number - 1) else null
            else -> null
        }
        if (target == null) { clear(); complete("INVALID_SCREEN_NUMBER"); return }
        if (!gestureGate.consume(current.id)) { clear(); complete("SCREEN_SESSION_EXPIRED"); return }
        val expectedFingerprint = imageFingerprints[target]
        if (expectedFingerprint != null) {
            val token = epoch
            // Remove number badge before screenshot so it cannot pollute the retained patch signature.
            var finished = false
            val timeout = Runnable { if (!finished) {
                finished = true
                if (epoch == token) { clear(); complete("SCREEN_REVALIDATION_TIMEOUT") }
                else complete("SCREEN_SESSION_EXPIRED")
            } }
            handler.postDelayed(timeout, 1500)
            fun reject(reason: String) {
                if (finished) return
                finished = true; handler.removeCallbacks(timeout)
                android.util.Log.i("DexScreen", "revalidation.rejected reason=$reason")
                if (epoch == token) { clear(); complete("SCREEN_CHANGED") } else complete("SCREEN_SESSION_EXPIRED")
            }
            runCatching { removeOverlayAfterCommit {
                if (!finished) {
                    if (token != epoch || !matches(current.snapshot, snapshot(), false, "image_revalidation")) reject("session_changed")
                    else runCatching { takeScreenshot(current.snapshot.displayId, mainExecutor, object : TakeScreenshotCallback {
                        override fun onFailure(errorCode: Int) = reject("capture_error_$errorCode")
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            val bitmap = copyScreenshot(screenshot)
                            val same = bitmap != null && runCatching { fingerprint(bitmap, target) == expectedFingerprint }.getOrDefault(false)
                            bitmap?.recycle()
                            if (finished) return
                            if (!same) { reject("patch_changed"); return }
                            if (token != epoch || !matches(current.snapshot, snapshot(), false, "image_revalidation")) { reject("session_changed"); return }
                            finished = true; handler.removeCallbacks(timeout)
                            sendGesture(current.snapshot.displayId, target, complete)
                        }
                    }) }.onFailure { reject("capture_exception_${it.javaClass.simpleName}") }
                }
            } }.onFailure { reject("detach_exception_${it.javaClass.simpleName}") }
        } else sendGesture(current.snapshot.displayId, target, complete)
    }
    /** Explicit presentation never executes a tap or reuses stale numbered target identities. */
    private fun freshGrid(complete: (String) -> Unit) {
        val previous = session?.snapshot
        clear()
        val fresh = snapshot(true)
        if (fresh == null) { complete("DEX_DISPLAY_UNAVAILABLE"); return }
        if ((previous != null && !matches(previous, fresh, true, "grid_rebase")) ||
            !matches(fresh, snapshot(true), true, "grid_prepare")) {
            complete("SCREEN_CHANGED"); return
        }
        session = Session(UUID.randomUUID().toString(), fresh, fresh.bounds, 0)
        gestureGate.activate(session!!.id)
        refreshOverlay(complete)
    }
    private fun matches(expected: ScreenSnapshot, fresh: ScreenSnapshot?, grid: Boolean, stage: String): Boolean {
        val mismatch = expected.mismatch(fresh, grid) ?: return true
        // Categories and counts only: no text, identity/content hashes, pixels or transcripts.
        android.util.Log.i("DexScreen", "snapshot.mismatch stage=$stage field=$mismatch grid=$grid " +
            "windows=${expected.windows.size}/${fresh?.windows?.size ?: -1} " +
            "targets=${expected.targets.size}/${fresh?.targets?.size ?: -1} " +
            "identities=${expected.identities.size}/${fresh?.identities?.size ?: -1}")
        return false
    }
    private fun copyScreenshot(screenshot: ScreenshotResult): Bitmap? {
        val buffer = screenshot.hardwareBuffer
        var hardware: Bitmap? = null
        return try {
            hardware = Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace)
            hardware?.copy(Bitmap.Config.ARGB_8888, false)
        } catch (_: RuntimeException) { null }
        finally { hardware?.recycle(); buffer.close() }
    }
    private fun fingerprint(bitmap: Bitmap, rect: ScreenRect): Long {
        require(rect.left >= 0 && rect.top >= 0 && rect.right <= bitmap.width && rect.bottom <= bitmap.height)
        var hash = 1125899906842597L
        for (y in rect.top until rect.bottom step maxOf(1, rect.height / 24))
            for (x in rect.left until rect.right step maxOf(1, rect.width / 24)) hash = hash * 31 + bitmap.getPixel(x, y)
        return hash
    }
    private fun sendGesture(displayId: Int, target: ScreenRect, complete: (String) -> Unit) {
        val path = Path().apply { moveTo(target.x, target.y) }
        val gesture = GestureDescription.Builder().setDisplayId(displayId)
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80)).build()
        // Consume session before dispatch: duplicate or late commands can never produce a second tap.
        clear()
        var terminal = false
        fun finish(status: String) { if (!terminal) { terminal = true; complete(status) } }
        val timeout = Runnable { finish("SCREEN_GESTURE_TIMEOUT") }
        handler.postDelayed(timeout, 2000)
        val accepted = runCatching { dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) { handler.removeCallbacks(timeout); finish("DISPATCHED") }
            override fun onCancelled(gestureDescription: GestureDescription?) { handler.removeCallbacks(timeout); finish("SCREEN_GESTURE_CANCELLED") }
        }, handler) }.getOrDefault(false)
        if (!accepted) { handler.removeCallbacks(timeout); finish("SCREEN_GESTURE_REJECTED") }
    }
    private fun refreshOverlay(complete: (String) -> Unit) {
        runCatching { show() }.onSuccess {
            handler.removeCallbacks(expiry); handler.postDelayed(expiry, 35_000)
            complete("SCREEN_CONTINUE")
        }
            .onFailure { reportOverlayFailure(it); clear(); complete("SCREEN_OVERLAY_FAILED") }
    }
    private fun snapshot(): ScreenSnapshot? = snapshot(false)
    private fun snapshot(grid: Boolean): ScreenSnapshot? = runCatching { collectSnapshot(grid) }.onFailure {
        android.util.Log.i("DexScreen", "snapshot.failed stage=collect class=${it.javaClass.simpleName}")
    }.getOrNull()
    private fun collectSnapshot(grid: Boolean): ScreenSnapshot? {
        val candidates = displays.getDisplays(SAMSUNG_DESKTOP_CATEGORY).filter { it.displayId != Display.DEFAULT_DISPLAY && it.isValid }
        val display = candidates.singleOrNull() ?: return null
        val size = android.graphics.Point(); display.getRealSize(size)
        val screen = ScreenRect(0, 0, size.x, size.y)
        if (screen.width <= 0 || screen.height <= 0) return null
        if (grid) return ScreenSnapshot(display.displayId, screen, emptyList(), emptyList(), emptyList())
        val windows = windowsOnAllDisplays[display.displayId].orEmpty()
            .filter { it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }.sortedByDescending { it.layer }
        if (screen.width <= 0 || screen.height <= 0 || windows.isEmpty() || windows.size > 64) return null
        val topology = mutableListOf<ScreenWindow>()
        val foreground = mutableListOf<ScreenRect>(); val targets = mutableListOf<ScreenRect>(); val signatures = mutableListOf<String>()
        var visited = 0; var skippedRefresh = 0
        windows.take(64).forEach { window ->
            val bounds = Rect(); window.getBoundsInScreen(bounds)
            val windowRect = ScreenRect(bounds.left, bounds.top, bounds.right, bounds.bottom)
            topology.add(ScreenWindow(window.id, window.layer, window.type, windowRect, window.isFocused, window.isActive))
            val root = if (grid) null else window.root
            fun walk(node: android.view.accessibility.AccessibilityNodeInfo, depth: Int, path: String) {
                if (depth > 40 || targets.size >= 256 || visited >= 4096) return
                visited++
                if (!node.refresh()) { skippedRefresh++; return }
                val rect = Rect(); node.getBoundsInScreen(rect)
                if (node.isVisibleToUser && node.isEnabled && (node.isClickable || node.actionList.any { it.id == android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK })) {
                    val target = ScreenRect(rect.left, rect.top, rect.right, rect.bottom)
                    val visibleTargets = ScreenTargets.visible(listOf(target), foreground, screen)
                    targets.addAll(visibleTargets)
                    if (visibleTargets.isNotEmpty()) signatures.add("node:${window.id}:$path:${node.hashCode()}:${node.viewIdResourceName}:${node.className}:${node.actionList.map { it.id }.sorted()}:${node.text?.toString()?.hashCode()}:${node.contentDescription?.toString()?.hashCode()}")
                }
                for (index in 0 until minOf(node.childCount, 128)) node.getChild(index)?.let { child ->
                    try { walk(child, depth + 1, "$path/$index") } finally { @Suppress("DEPRECATION") child.recycle() }
                }
            }
            if (root != null) try { walk(root, 0, "root") } finally { @Suppress("DEPRECATION") root.recycle() }
            foreground.add(windowRect)
        }
        if (skippedRefresh > 0) android.util.Log.i("DexScreen", "snapshot.nodes visited=$visited skippedRefresh=$skippedRefresh targets=${targets.size}")
        return ScreenSnapshot(display.displayId, screen, ScreenTargets.merge(targets, emptyList()), topology, signatures)
    }
    private fun reportOverlayFailure(error: Throwable) {
        // Exception messages/stack may contain window text or platform data; log class only.
        val connected = runCatching { serviceInfo != null }.getOrDefault(false)
        android.util.Log.w("DexScreen", "overlay.failed type=${error.javaClass.simpleName} connected=$connected")
    }
    // A screenshot must observe detachment, not merely a queued compositor transaction.
    private fun removeOverlayAfterCommit(detached: () -> Unit) {
        val control = overlayControl
        if (Build.VERSION.SDK_INT < 34 || control == null) { removeOverlay(); detached(); return }
        val surface = overlaySurface
        overlayControl = null; overlaySurface = null
        try {
            SurfaceControl.Transaction().use { transaction ->
                transaction.reparent(control, null)
                if (Build.VERSION.SDK_INT >= 35) transaction.addTransactionCompletedListener(mainExecutor) { detached() }
                else transaction.addTransactionCommittedListener(mainExecutor, SurfaceControl.TransactionCommittedListener {
                    android.view.Choreographer.getInstance().postFrameCallback { detached() }
                })
                transaction.apply()
            }
        } finally { surface?.release(); control.release() }
    }
    private fun removeOverlay() {
        overlayControl?.let { control -> runCatching {
            SurfaceControl.Transaction().use { it.reparent(control, null).apply() }
        } }
        runCatching { overlaySurface?.release() }
        runCatching { overlayControl?.release() }
        overlaySurface = null; overlayControl = null
        overlay?.let { runCatching { windowManager?.removeViewImmediate(it) } }
        overlay = null; windowManager = null
    }
    private fun drawNumbers(canvas: Canvas, current: Session) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val cells = current.grid?.let { region -> (1..24).mapNotNull(region::cell) } ?: imageTargets
            cells.forEachIndexed { index, target ->
                if (current.grid != null) {
                    paint.color = Color.argb(150, 255, 255, 255); paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
                    canvas.drawRect(target.left.toFloat(), target.top.toFloat(), target.right.toFloat(), target.bottom.toFloat(), paint)
                }
                paint.style = Paint.Style.FILL; paint.color = Color.BLACK
                val x = target.left.toFloat().coerceAtMost(current.snapshot.bounds.right - 42f)
                val badgeTop = if (current.grid != null) target.top else if (target.top >= 30) target.top - 30 else target.bottom
                val y = badgeTop.toFloat().coerceAtMost(current.snapshot.bounds.bottom - 30f)
                canvas.drawRoundRect(x, y, x + 42, y + 30, 5f, 5f, paint)
                paint.color = Color.YELLOW; paint.textSize = 24f
                canvas.drawText("${index + 1}", x + 5, y + 24, paint)
            }
    }
    private fun show() {
        val current = session ?: return
        removeOverlay()
        check(serviceInfo != null) { "Accessibility connection unavailable" }
        val display = displays.getDisplay(current.snapshot.displayId) ?: error("Display removed")
        if (Build.VERSION.SDK_INT >= 34) {
            // This is composition only: no ViewRoot, window, input channel or focus target.
            // The public Accessibility API authorizes attachment to exactly this display.
            val control = SurfaceControl.Builder().setName("DeX numbered accessibility overlay")
                .setBufferSize(current.snapshot.bounds.width, current.snapshot.bounds.height)
                .setFormat(PixelFormat.RGBA_8888).setOpaque(false).build()
            overlayControl = control
            val surface = Surface(control)
            overlaySurface = surface
            check(surface.isValid && control.isValid) { "Accessibility surface unavailable" }
            val canvas = surface.lockCanvas(null)
            try {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                drawNumbers(canvas, current)
            } finally { surface.unlockCanvasAndPost(canvas) }
            attachAccessibilityOverlayToDisplay(display.displayId, control)
            SurfaceControl.Transaction().use { transaction ->
                transaction.setPosition(control, 0f, 0f).setLayer(control, Int.MAX_VALUE)
                    .setVisibility(control, true).apply()
            }
            android.util.Log.i("DexScreen", "overlay.renderer RAW_SURFACE display=${display.displayId} api=${Build.VERSION.SDK_INT}")
            return
        }
        // The service display-context override installs the per-display Accessibility
        // token on its WindowManager. Deriving another window context can lose that token
        // on vendor builds. Keep this exact manager for add/remove across all supported APIs.
        val displayContext = createDisplayContext(display)
        windowManager = displayContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        android.util.Log.i("DexScreen", "overlay.context type=${displayContext.javaClass.simpleName} manager=${windowManager!!.javaClass.simpleName} display=${display.displayId} api=${Build.VERSION.SDK_INT}")
        overlay = object : View(displayContext) {
            override fun onDraw(canvas: Canvas) {
                drawNumbers(canvas, current)
            }
        }
        val params = WindowManager.LayoutParams(current.snapshot.bounds.width, current.snapshot.bounds.height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.LEFT; x = 0; y = 0 }
        windowManager!!.addView(overlay, params)
    }
    companion object {
        const val SAMSUNG_DESKTOP_CATEGORY = "com.samsung.android.hardware.display.category.DESKTOP"
        @Volatile var active: DexAccessibilityService? = null
            private set
    }
}
