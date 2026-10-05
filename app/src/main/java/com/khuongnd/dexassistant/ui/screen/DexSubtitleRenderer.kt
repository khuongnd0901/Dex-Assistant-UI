package com.khuongnd.dexassistant.ui.screen

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.*
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.SystemClock
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.*
import com.khuongnd.dexassistant.ui.voice.*

/** Separate ownership from grid sessions; never creates an input channel on API 34+. */
internal class DexSubtitleRenderer(private val service: AccessibilityService) {
    private var control: SurfaceControl? = null
    private var surface: Surface? = null
    private var view: View? = null
    private var manager: WindowManager? = null
    private var displayId: Int? = null
    private var size: Point? = null
    private var suppressed: Long? = null
    private var captures = 0
    private var boundTurn: Long? = null
    private var boundDisplay: Int? = null
    private var drawing: ((Canvas) -> Unit)? = null
    private var closed = false
    private var detachInFlight = false
    private val detachWaiters = mutableListOf<() -> Unit>()
    private var last: SubtitleState? = null
    fun render(state: SubtitleState?) {
        try { renderSafely(state) } catch (error: Exception) {
            android.util.Log.w("DexSubtitles", "render.failed turn=${state?.turn} type=${error.javaClass.simpleName}")
            suppressed = state?.turn; dispose()
        }
    }
    private fun renderSafely(state: SubtitleState?) {
        if (closed) return
        if (state?.turn != boundTurn) { boundTurn = state?.turn; boundDisplay = null }
        val display = service.getSystemService(DisplayManager::class.java)
            .getDisplays(DexAccessibilityService.SAMSUNG_DESKTOP_CATEGORY)
            .filter { it.isValid && it.displayId != Display.DEFAULT_DISPLAY }.singleOrNull()
        if (display == null || (boundDisplay != null && boundDisplay != display.displayId)) {
            suppressed = state?.turn; dispose(); return
        }
        if (state == null || state.turn == suppressed || captures > 0 || detachInFlight ||
            state.expiresAt?.let { SystemClock.elapsedRealtime() >= it } == true) { dispose(); return }
        boundDisplay = display.displayId
        val bounds = Point().also(display::getRealSize)
        if (bounds.x <= 0 || bounds.y <= 0) { suppress(); return }
        if (last == state && size == bounds && displayId == display.displayId) return
        val context = service.createDisplayContext(display)
        if (size != bounds || displayId != display.displayId) dispose()
        val draw: (Canvas) -> Unit = { canvas ->
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            val metrics = context.resources.displayMetrics
            val density = metrics.density
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 24 * metrics.scaledDensity }
            val width = (bounds.x * .8f - 32 * density).toInt().coerceAtLeast(1)
            val fullText = StaticLayout.Builder.obtain(state.text, 0, state.text.length, paint, width).build()
            val tail = if (fullText.lineCount > 2) state.text.substring(fullText.getLineStart(fullText.lineCount - 2)) else state.text
            val text = StaticLayout.Builder.obtain(tail, 0, tail.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(2).build()
            val status = when (state.phase) { SubtitlePhase.PREPARING -> "Đang chuẩn bị…"; SubtitlePhase.LISTENING -> "Đang nghe…"
                SubtitlePhase.RECOGNIZING -> "Đang nhận dạng…"; SubtitlePhase.FINAL -> "Đã nhận dạng" }
            val insets = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.windowInsets
                .getInsets(WindowInsets.Type.systemBars()).bottom
            val bottom = bounds.y - if (insets > 0) insets + 24 * density else 72 * density
            val height = (text.height + 48 * density).coerceAtMost(bottom.coerceAtLeast(0f))
            val top = (bottom - height).coerceAtLeast(0f)
            val left = (bounds.x - width) / 2f
            val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 0, 0, 0) }
            canvas.drawRoundRect(left - 16*density, top, left + width + 16*density, bottom, 12*density, 12*density, background)
            val statusPaint = TextPaint(paint).apply { textSize = 14 * metrics.scaledDensity }
            canvas.drawText(status, (bounds.x - statusPaint.measureText(status))/2f, top + 22*density, statusPaint)
            canvas.save(); canvas.translate(left, top + 32*density); text.draw(canvas); canvas.restore()
        }
        drawing = draw
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                if (control == null) {
                    control = SurfaceControl.Builder().setName("DeX command subtitles").setBufferSize(bounds.x, bounds.y)
                        .setFormat(PixelFormat.RGBA_8888).setOpaque(false).build()
                    surface = Surface(control!!)
                    service.attachAccessibilityOverlayToDisplay(display.displayId, control!!)
                    SurfaceControl.Transaction().use { it.setLayer(control!!, Int.MAX_VALUE).setVisibility(control!!, true).apply() }
                }
                val canvas = surface!!.lockCanvas(null)
                try { draw(canvas) } finally { surface!!.unlockCanvasAndPost(canvas) }
            } else {
                if (view == null) {
                    manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    view = object : View(context) { override fun onDraw(canvas: Canvas) { runCatching { drawing?.invoke(canvas) } } }
                    manager!!.addView(view, WindowManager.LayoutParams(bounds.x, bounds.y, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.LEFT })
                } else view!!.invalidate()
            }
            last = state; size = bounds; displayId = display.displayId
            android.util.Log.i("DexSubtitles", "present turn=${state.turn} phase=${state.phase} hasText=${state.text.isNotEmpty()} display=${display.displayId} atMs=${SystemClock.elapsedRealtime()}")
        } catch (_: Exception) { suppressed = state.turn; dispose() }
    }
    fun close() { closed = true; suppress() }
    fun suppress() { suppressed = VoiceSubtitles.store.state.value?.turn; dispose() }
    /** Reference counted across overlapping captures; restoration always reads current RAM state. */
    fun suspendCapture(detached: () -> Unit): () -> Unit {
        captures++
        var released = false
        val release = { if (!released) { released = true; captures--; render(VoiceSubtitles.store.state.value) } }
        detachWaiters.add(detached)
        if (detachInFlight) return release
        detachInFlight = true
        val committed = {
            detachInFlight = false
            val waiters = detachWaiters.toList(); detachWaiters.clear()
            waiters.forEach { it() }
            if (captures == 0) render(VoiceSubtitles.store.state.value)
        }
        val old = control
        val hadWindow = view != null
        try {
        if (old != null && Build.VERSION.SDK_INT >= 34) {
            control = null
            SurfaceControl.Transaction().use { transaction ->
                transaction.setVisibility(old, false).reparent(old, null)
                if (Build.VERSION.SDK_INT >= 35) transaction.addTransactionCompletedListener(service.mainExecutor) { committed() }
                else transaction.addTransactionCommittedListener(service.mainExecutor, SurfaceControl.TransactionCommittedListener {
                    Choreographer.getInstance().postFrameCallback { committed() }
                })
                transaction.apply()
            }
            surface?.release(); surface = null; old.release(); last = null; size = null; displayId = null
        } else {
            dispose()
            if (hadWindow) Choreographer.getInstance().postFrameCallback { Choreographer.getInstance().postFrameCallback { committed() } }
            else service.mainExecutor.execute { committed() } // No compositor content exists to detach.
        }
        } catch (error: Exception) { detachInFlight = false; detachWaiters.clear(); release(); suppress(); throw error }
        return release
    }
    fun aboveNumbers() {
        if (Build.VERSION.SDK_INT < 34 && view != null) {
            val latest = VoiceSubtitles.store.state.value
            dispose(); render(latest)
        }
    }
    fun dispose() {
        control?.let { runCatching { SurfaceControl.Transaction().use { tx -> tx.setVisibility(it, false).reparent(it, null).apply() } } }
        surface?.release(); control?.release(); surface = null; control = null
        view?.let { runCatching { manager?.removeViewImmediate(it) } }; view = null; manager = null
        last = null; size = null; displayId = null; drawing = null
    }
}
