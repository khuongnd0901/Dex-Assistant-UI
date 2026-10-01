package com.khuongnd.dexassistant.ui.voice

import android.Manifest
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.UserManager
import android.os.Handler
import android.os.Looper
import com.khuongnd.dexassistant.ui.device.DpcClient
import android.service.voice.VoiceInteractionService
import androidx.core.content.ContextCompat

class AssistantVoiceInteractionService : VoiceInteractionService() {
    private val coordinator = AssistantStartCoordinator()
    private var unlockReceiver: BroadcastReceiver? = null
    private val handler = Handler(Looper.getMainLooper())
    private var active = false
    private var reconciliation = PermissionReconciliation()
    private var session = 0
    private val dpc = DpcClient(this) { reconcilePermissions() }
    private val bindingTimeout = Runnable {
        if (active && getSystemService(UserManager::class.java).isUserUnlocked) {
            retryReconciliation("DPC_BIND_TIMEOUT")
        }
    }

    override fun onReady() {
        super.onReady()
        handler.removeCallbacksAndMessages(null)
        active = true
        reconciliation = PermissionReconciliation()
        session++
        publishReadiness(AssistantReadiness.Blocked("DPC_PERMISSION_RECONCILING"))
        handler.postDelayed(bindingTimeout, 10_000L)
        val bound = runCatching { dpc.bind() }.getOrDefault(false)
        if (!bound) {
            handler.removeCallbacks(bindingTimeout)
            publishReadiness(AssistantReadiness.Blocked("DPC_BIND_FAILED"))
        }
        evaluateAndStart()
        if (!getSystemService(UserManager::class.java).isUserUnlocked && unlockReceiver == null) {
            unlockReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    reconcilePermissions()
                    evaluateAndStart()
                }
            }.also {
                ContextCompat.registerReceiver(this, it, IntentFilter(Intent.ACTION_USER_UNLOCKED),
                    ContextCompat.RECEIVER_NOT_EXPORTED)
            }
        }
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    override fun onShutdown() {
        cleanup()
        super.onShutdown()
    }

    private fun cleanup() {
        active = false
        session++
        handler.removeCallbacksAndMessages(null)
        dpc.unbind()
        unlockReceiver?.let { runCatching { unregisterReceiver(it) } }
        unlockReceiver = null
        coordinator.serviceStopped()
    }

    private fun reconcilePermissions() {
        if (!active) return
        if (!getSystemService(UserManager::class.java).isUserUnlocked) return
        handler.removeCallbacks(bindingTimeout)
        if (!reconciliation.begin()) return
        publishReadiness(AssistantReadiness.Blocked("DPC_PERMISSION_RECONCILING"))
        val currentSession = session
        dpc.applyPermissionProfile { result ->
            if (!active || currentSession != session) return@applyPermissionProfile
            if (reconciliation.finish(result.status, result.capabilities["deviceOwner"] == "GRANTED")) {
                evaluateAndStart()
            } else retryReconciliation(result.errorCode ?: "DPC_PERMISSION_PROFILE_FAILED")
        }
    }

    private fun retryReconciliation(reason: String) {
        if (!active || reconciliation.complete) return
        publishReadiness(AssistantReadiness.Blocked(reason))
        if (reconciliation.canRetry()) handler.postDelayed({ reconcilePermissions() }, 1_000L)
    }

    private fun evaluateAndStart() {
        if (!active) return
        if (!getSystemService(UserManager::class.java).isUserUnlocked) {
            publishReadiness(AssistantReadiness.Blocked("USER_LOCKED"))
            return
        }
        if (!reconciliation.complete) return
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
            runCatching { ContextCompat.startForegroundService(
                this,
                Intent(this, VoiceAssistantService::class.java).setAction(VoiceAssistantService.ACTION_START),
            ) }.onFailure {
                coordinator.serviceStopped()
                publishReadiness(AssistantReadiness.Blocked("VOICE_START_FAILED_${it.javaClass.simpleName}"))
            }
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
