package com.khuongnd.dexassistant.ui.voice

import android.Manifest
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.UserManager
import android.service.voice.VoiceInteractionService
import androidx.core.content.ContextCompat

class AssistantVoiceInteractionService : VoiceInteractionService() {
    private val coordinator = AssistantStartCoordinator()
    private var unlockReceiver: BroadcastReceiver? = null

    override fun onReady() {
        super.onReady()
        evaluateAndStart()
        if (!getSystemService(UserManager::class.java).isUserUnlocked && unlockReceiver == null) {
            unlockReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) = evaluateAndStart()
            }.also {
                ContextCompat.registerReceiver(this, it, IntentFilter(Intent.ACTION_USER_UNLOCKED),
                    ContextCompat.RECEIVER_NOT_EXPORTED)
            }
        }
    }

    override fun onShutdown() {
        unlockReceiver?.let { runCatching { unregisterReceiver(it) } }
        unlockReceiver = null
        coordinator.serviceStopped()
        super.onShutdown()
    }

    private fun evaluateAndStart() {
        val roleManager = getSystemService(RoleManager::class.java)
        val policy = getSharedPreferences(POLICY_PREFERENCES, MODE_PRIVATE)
        val readiness = AssistantReadinessEvaluator.evaluate(
            AssistantReadinessInput(
                assistantRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT),
                userUnlocked = getSystemService(UserManager::class.java).isUserUnlocked,
                recordAudioGranted = granted(Manifest.permission.RECORD_AUDIO),
                foregroundServiceGranted = granted(Manifest.permission.FOREGROUND_SERVICE),
                autoStartEnabled = policy.getBoolean(KEY_AUTO_START, true),
                wakeWordEnabled = policy.getBoolean(KEY_WAKE_WORD, true),
            ),
        )
        publishReadiness(readiness)
        if (coordinator.shouldStart(readiness)) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, VoiceAssistantService::class.java).setAction(VoiceAssistantService.ACTION_START),
            )
        }
    }

    private fun granted(permission: String) =
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun publishReadiness(readiness: AssistantReadiness) {
        val detail = if (readiness is AssistantReadiness.Ready) "READY" else (readiness as AssistantReadiness.Blocked).reason
        getSharedPreferences(VoiceAssistantService.HEALTH_PREFERENCES, MODE_PRIVATE).edit()
            .putString(VoiceAssistantService.KEY_ASSISTANT_READINESS, detail)
            .putLong(VoiceAssistantService.KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    companion object {
        const val POLICY_PREFERENCES = "voice_policy"
        const val KEY_AUTO_START = "autoStart"
        const val KEY_WAKE_WORD = "wakeWord"
    }
}
