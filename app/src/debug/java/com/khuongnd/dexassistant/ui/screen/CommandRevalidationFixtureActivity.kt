package com.khuongnd.dexassistant.ui.screen

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout

/** Disposable instrumentation fixture with no preferences, history, network or external actions. */
class CommandRevalidationFixtureActivity : Activity() {
    private lateinit var target: Button
    var clicks = 0
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        target = Button(this).apply { text = ORIGINAL; setOnClickListener { clicks++ } }
        setContentView(FrameLayout(this).apply {
            addView(target, FrameLayout.LayoutParams(500, 180, Gravity.CENTER))
        })
        active = this
    }
    fun changeLabel() { target.text = CHANGED }
    override fun onDestroy() { if (active === this) active = null; super.onDestroy() }
    companion object {
        const val ORIGINAL = "T-011 original target"
        const val CHANGED = "T-011 changed target"
        @Volatile var active: CommandRevalidationFixtureActivity? = null
    }
}
