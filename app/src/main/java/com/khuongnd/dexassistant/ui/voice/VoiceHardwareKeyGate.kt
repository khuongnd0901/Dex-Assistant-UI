package com.khuongnd.dexassistant.ui.voice

import android.view.KeyEvent

/** Tracks only external ASSIST/VOICE_ASSIST presses; preserves paired consumption and typing. */
class VoiceHardwareKeyGate {
    private val consumed = mutableMapOf<Pair<Int, Int>, Long>()
    fun onKey(deviceId: Int, external: Boolean, keyCode: Int, action: Int, repeat: Int,
              cancelled: Boolean, downTime: Long, request: () -> Boolean): Boolean {
        if (!external || keyCode !in setOf(KeyEvent.KEYCODE_ASSIST, KeyEvent.KEYCODE_VOICE_ASSIST)) return false
        val key = deviceId to keyCode
        return when (action) {
            KeyEvent.ACTION_DOWN -> {
                if (consumed[key] == downTime) true
                else if (repeat == 0 && !cancelled) {
                    consumed.remove(key) // A fresh press replaces any lost release after unplug.
                    if (request()) { consumed[key] = downTime; true } else false
                } else false
            }
            KeyEvent.ACTION_UP -> if (consumed[key] == downTime) { consumed.remove(key); true } else false
            else -> false
        }
    }
    fun clear() = consumed.clear()
}
