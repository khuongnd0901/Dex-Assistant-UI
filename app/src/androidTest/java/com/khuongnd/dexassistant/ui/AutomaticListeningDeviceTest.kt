package com.khuongnd.dexassistant.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.voice.*
import org.junit.Assert.*
import org.junit.Test

class AutomaticListeningDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun waitFor(predicate: () -> Boolean) {
        val deadline=android.os.SystemClock.elapsedRealtime()+10000
        while(!predicate() && android.os.SystemClock.elapsedRealtime()<deadline) Thread.sleep(25)
        assertTrue("Bounded voice condition: state=${VoiceAssistantService.active?.currentVoiceState()} suppression=${context.getSharedPreferences("voice_policy",0).getString(VoiceAutoStart.KEY_SUPPRESSION,"")} allowed=${VoiceAutoStart.allowed(context)} health=${context.getSharedPreferences("voice_health",0).getString("state","")}",predicate())
    }
    private fun action(action: String) {
        instrumentation.runOnMainSync {
            if (action == VoiceAssistantService.ACTION_STOP) {
                // Remove a prior receipt so the wait proves this STOP was actually delivered.
                context.getSharedPreferences("voice_policy",0).edit().remove(VoiceAutoStart.KEY_SUPPRESSION).commit()
            }
            context.startService(Intent(context,VoiceAssistantService::class.java).setAction(action))
        }
        if (action == VoiceAssistantService.ACTION_STOP) {
            instrumentation.waitForIdleSync()
            waitFor { VoiceAssistantService.active == null && context.getSharedPreferences("voice_policy",0).getString(VoiceAutoStart.KEY_SUPPRESSION,"")=="USER_STOPPED" && context.getSharedPreferences("voice_health",0).getString("state","")=="STOPPED" }
        }
    }
    @Test fun foregroundOpenAutoStartsRecoveryHonorsExplicitPauseAndStop() {
        instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val policy=context.getSharedPreferences("voice_policy",0); val original=policy.all
        try {
            action(VoiceAssistantService.ACTION_STOP); waitFor { VoiceAssistantService.active==null }
            policy.edit().putBoolean("autoStart",true).putBoolean("wakeWord",true).remove(VoiceAutoStart.KEY_SUPPRESSION).commit()
            VoiceAutoStart.userResume(context)
            ActivityScenario.launch(MainActivity::class.java).use {
                waitFor { VoiceAssistantService.active?.currentVoiceState()==VoiceState.LISTENING && context.getSharedPreferences("voice_health",0).getString("detail","").orEmpty().startsWith("Listening for Hey Hey offline") }
                action(VoiceAssistantService.ACTION_PAUSE); waitFor { VoiceAssistantService.active?.currentVoiceState()==VoiceState.PAUSED }
                instrumentation.runOnMainSync { VoiceAutoStart.evaluate(context) }
                Thread.sleep(250); assertEquals(VoiceState.PAUSED,VoiceAssistantService.active?.currentVoiceState())
                action(VoiceAssistantService.ACTION_RESUME); waitFor { VoiceAssistantService.active?.currentVoiceState()==VoiceState.LISTENING && context.getSharedPreferences("voice_health",0).getString("detail","").orEmpty().startsWith("Listening for Hey Hey offline") }
                // Unintentional service loss preserves desired policy; recovery starts without user START.
                instrumentation.runOnMainSync { context.stopService(Intent(context,VoiceAssistantService::class.java)) }
                waitFor { VoiceAssistantService.active==null }
                instrumentation.runOnMainSync { VoiceAutoStart.evaluate(context) }
                waitFor { VoiceAssistantService.active?.currentVoiceState()==VoiceState.LISTENING && context.getSharedPreferences("voice_health",0).getString("detail","").orEmpty().startsWith("Listening for Hey Hey offline") }
                action(VoiceAssistantService.ACTION_STOP); waitFor { VoiceAssistantService.active==null }
                instrumentation.runOnMainSync { VoiceAutoStart.evaluate(context) }
                Thread.sleep(250); assertNull(VoiceAssistantService.active)
                assertEquals("USER_STOPPED",policy.getString(VoiceAutoStart.KEY_SUPPRESSION,""))
            }
        } finally {
            action(VoiceAssistantService.ACTION_STOP); waitFor { VoiceAssistantService.active==null }
            restoreVoicePolicy(context,original)
        }
    }
}
