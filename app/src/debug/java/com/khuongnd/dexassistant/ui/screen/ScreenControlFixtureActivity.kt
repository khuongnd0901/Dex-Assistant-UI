package com.khuongnd.dexassistant.ui.screen

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView

/** Debug-only secondary-display fixture; records gesture display/coordinates without a network path. */
class ScreenControlFixtureActivity : Activity() {
    private var canvasView: android.view.View? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val frame = FrameLayout(this)
        frame.addView(TextView(this).apply { text = "DeX numbered control fixture" }, FrameLayout.LayoutParams(600, 80, Gravity.TOP))
        frame.addView(Button(this).apply {
            tag = "fixturePopupButton"; text = "Popup"; setOnClickListener { AlertDialog.Builder(this@ScreenControlFixtureActivity)
                .setMessage("Close using numbered control").setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }.show() }
        }, FrameLayout.LayoutParams(240, 100, Gravity.CENTER))
        val canvasFixture = object : android.view.View(this) {
            private val paint = android.graphics.Paint()
            override fun onDraw(canvas: android.graphics.Canvas) {
                canvas.drawColor(android.graphics.Color.rgb(17, 17, 17))
                paint.color = android.graphics.Color.WHITE; paint.strokeWidth = 3f; paint.style = android.graphics.Paint.Style.STROKE
                canvas.drawRect(20f, 100f, 220f, 160f, paint)
                if (!canvasClosed) {
                    paint.strokeWidth = 9f
                    canvas.drawLine(244f, 24f, 316f, 96f, paint); canvas.drawLine(316f, 24f, 244f, 96f, paint)
                }
            }
            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_UP && event.x in 235f..325f && event.y in 15f..105f) {
                    canvasClosed = true; invalidate()
                }
                return true
            }
        }
        canvasView = canvasFixture
        frame.addView(canvasFixture, FrameLayout.LayoutParams(340, 200, Gravity.TOP or Gravity.RIGHT))
        canvasClosed = false
        setContentView(frame)
        active = this
    }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) lastTap = Tap(display?.displayId ?: -1, event.rawX, event.rawY)
        return super.dispatchTouchEvent(event)
    }
    override fun onDestroy() { if (active === this) active = null; super.onDestroy() }
    data class Tap(val displayId: Int, val x: Float, val y: Float)
    companion object {
        @Volatile var canvasClosed = false
        val canvasXBounds: ScreenRect?
            get() {
                val view = active?.canvasView ?: return null
                if (!view.isAttachedToWindow || !view.isLaidOut) return null
                val position = IntArray(2); view.getLocationOnScreen(position)
                return ScreenRect(position[0] + 235, position[1] + 15, position[0] + 325, position[1] + 105)
            }
        @Volatile var lastTap: Tap? = null
        @Volatile var active: ScreenControlFixtureActivity? = null
    }
}
