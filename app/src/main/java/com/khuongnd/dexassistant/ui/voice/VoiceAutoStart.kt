package com.khuongnd.dexassistant.ui.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** Shared process gate: bounded attempts, one pending start, no takeover of an active turn. */
class VoiceRecoveryGate {
    private var attempts = 0
    private var nextAttempt = 0L
    private var pendingUntil = 0L
    private var healthySince: Long? = null
    private var cooldownUntil = 0L
    fun reset() { attempts = 0; nextAttempt = 0; pendingUntil = 0; healthySince = null; cooldownUntil = 0 }
    @Synchronized fun admit(now: Long, allowed: Boolean, state: VoiceState?, recoverPaused: Boolean = false): Boolean {
        if (!allowed) return false
        if (state != null && state != VoiceState.ERROR && !(state == VoiceState.PAUSED && recoverPaused)) {
            pendingUntil = 0
            if (state == VoiceState.LISTENING) {
                if (healthySince == null) healthySince = now
                if (now - healthySince!! >= 30_000) { attempts = 0; nextAttempt = 0; cooldownUntil = 0 }
            } else healthySince = null
            return false
        }
        healthySince = null
        if (attempts >= 4) {
            if (now < cooldownUntil) return false
            attempts = 0; nextAttempt = 0; pendingUntil = 0; cooldownUntil = 0
        }
        if (now < pendingUntil || now < nextAttempt) return false
        val delay = longArrayOf(5_000, 15_000, 30_000, 60_000)[attempts]
        attempts++; pendingUntil = now + 10_000; nextAttempt = now + delay
        if (attempts == 4) cooldownUntil = now + 300_000
        return true
    }
}
object VoiceAutoStart {
    const val KEY_SUPPRESSION = "explicitSuppression"
    private val gate = VoiceRecoveryGate()
    fun allowed(context: Context): Boolean {
        val policy = context.getSharedPreferences(AssistantVoiceInteractionService.POLICY_PREFERENCES, Context.MODE_PRIVATE)
        return context.getSystemService(android.os.UserManager::class.java).isUserUnlocked && policy.getBoolean(AssistantVoiceInteractionService.KEY_AUTO_START, true) &&
            policy.getBoolean(AssistantVoiceInteractionService.KEY_WAKE_WORD, true) && policy.getString(KEY_SUPPRESSION, "").isNullOrBlank() &&
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    fun suppress(context: Context, reason: String) { context.getSharedPreferences(AssistantVoiceInteractionService.POLICY_PREFERENCES, Context.MODE_PRIVATE).edit().putString(KEY_SUPPRESSION, reason).commit() }
    fun userResume(context: Context) { context.getSharedPreferences(AssistantVoiceInteractionService.POLICY_PREFERENCES, Context.MODE_PRIVATE).edit().remove(KEY_SUPPRESSION).commit(); gate.reset() }
    fun evaluate(context: Context) {
        val audioMode = context.getSystemService(android.media.AudioManager::class.java).mode
        val communication = audioMode == android.media.AudioManager.MODE_IN_CALL || audioMode == android.media.AudioManager.MODE_IN_COMMUNICATION
        if (gate.admit(android.os.SystemClock.elapsedRealtime(), allowed(context) && !communication, VoiceAssistantService.active?.currentVoiceState(), recoverPaused = !communication)) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, VoiceAssistantService::class.java).setAction(VoiceAssistantService.ACTION_AUTO_START)) }
        }
    }
}
