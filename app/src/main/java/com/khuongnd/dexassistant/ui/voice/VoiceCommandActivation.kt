package com.khuongnd.dexassistant.ui.voice

import android.Manifest
import android.app.role.RoleManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Looper
import android.os.SystemClock
import android.os.UserManager
import androidx.core.content.ContextCompat

/** Shared admission for assistant UI and external assistant buttons, with no policy reset. */
class VoiceActivationGate(private val debounceMs: Long = 600) {
    private var lastAccepted: Long? = null
    @Synchronized fun admit(now: Long, allowed: Boolean, state: VoiceState?): Boolean {
        if (!allowed || (!idle(state) && state != VoiceState.TRANSCRIBING)) return false
        // A press during STT submits the current utterance; the initial start
        // press must not debounce that explicit submit request.
        if (state == VoiceState.TRANSCRIBING) return true
        if (lastAccepted?.let { now - it < debounceMs } == true) return false
        lastAccepted = now
        return true
    }
    companion object {
        fun idle(state: VoiceState?): Boolean = state == null ||
            state in setOf(VoiceState.LISTENING, VoiceState.STOPPED, VoiceState.ERROR)
    }
}

object VoiceCommandActivation {
    private val gate = VoiceActivationGate()

    fun allowed(context: Context): Boolean {
        val audio = context.getSystemService(AudioManager::class.java)
        return context.getSystemService(UserManager::class.java).isUserUnlocked &&
            !context.getSystemService(KeyguardManager::class.java).isKeyguardLocked &&
            context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_ASSISTANT) &&
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED &&
            context.getSharedPreferences(VoiceAssistantService.HEALTH_PREFERENCES, Context.MODE_PRIVATE)
                .getString(VoiceAssistantService.KEY_ASSISTANT_READINESS, "") == "READY" &&
            VoiceAutoStart.allowed(context) &&
            audio.mode != AudioManager.MODE_IN_CALL && audio.mode != AudioManager.MODE_IN_COMMUNICATION
    }

    /** Main thread only, like SpeechRecognizer lifecycle. Returns admission, not recognition success. */
    fun request(context: Context): Boolean {
        if (Looper.myLooper() != Looper.getMainLooper()) return false
        val service = VoiceAssistantService.active
        if (!gate.admit(SystemClock.elapsedRealtime(), allowed(context), service?.currentVoiceState())) return false
        return if (service != null) {
            if (service.currentVoiceState() == VoiceState.TRANSCRIBING) service.submitCurrentPartial()
            else service.beginCommandListening()
        } else runCatching {
            ContextCompat.startForegroundService(context, Intent(context, VoiceAssistantService::class.java)
                .setAction(VoiceAssistantService.ACTION_COMMAND_LISTEN))
            true
        }.getOrDefault(false)
    }
}
