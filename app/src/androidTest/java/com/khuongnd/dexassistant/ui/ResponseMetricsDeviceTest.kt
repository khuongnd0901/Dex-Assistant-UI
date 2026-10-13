package com.khuongnd.dexassistant.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.history.CommandHistoryStore
import com.khuongnd.dexassistant.ui.voice.CommandRecognizer
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Exercises real coordinator boundaries with captured Intent dispatch and isolated history. */
class ResponseMetricsDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @Test fun firstStatusPrecedesRealActionBoundaryWithoutSpeechEndAndIntentIsCaptured() {
        fixture { context, history, captured ->
            val lines = java.util.concurrent.CopyOnWriteArrayList<String>()
            val done = CountDownLatch(1)
            lateinit var core: AssistantCoordinator
            instrumentation.runOnMainSync {
                core = AssistantCoordinator(context, history, latencySink = {
                    lines += it; Log.i("DexLatency", it)
                    if (it.contains("finalToActionStartLatency")) assertTrue(captured.isEmpty())
                })
                core.dispatchVoice("mở chrome", progress = { _, _ -> }) {
                    assertEquals("DISPATCHED", it.status); done.countDown()
                }
            }
            assertTrue(done.await(5, TimeUnit.SECONDS))
            assertEquals(1, captured.size)
            assertEquals("com.android.chrome", captured.single().component?.packageName)
            assertEquals(1, lines.count { it.contains("metric=firstStatusLatency") })
            assertEquals(1, lines.count { it.contains("metric=finalToActionStartLatency") })
            assertFalse(lines.any { it.contains("metric=speechEndToActionStartLatency") })
            assertTrue(lines.last().contains("origin=local terminalReason=DISPATCHED"))
            assertTrue(lines.indexOfFirst { it.contains("firstStatusLatency") } < lines.indexOfFirst { it.contains("finalToActionStartLatency") })
        }
    }
    @Test fun emptyAndQueuedCancelCannotInventActionStartOrCompleteTwice() {
        fixture { context, history, captured ->
            val lines = java.util.concurrent.CopyOnWriteArrayList<String>()
            val done = CountDownLatch(2)
            instrumentation.runOnMainSync {
                val core = AssistantCoordinator(context, history, latencySink = { lines += it; Log.i("DexLatency", it) })
                core.dispatchVoice("", progress = { _, _ -> }) { assertEquals("NO_SPEECH", it.status); done.countDown() }
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                val core = AssistantCoordinator(context, history, latencySink = { lines += it; Log.i("DexLatency", it) })
                val id = core.dispatchVoice("mở chrome") { assertEquals("CANCELLED", it.status); done.countDown() }
                core.cancelVoice(id)
            }
            assertTrue(done.await(5, TimeUnit.SECONDS))
            assertTrue(captured.isEmpty())
            assertEquals(2, lines.count { it.contains("terminalReason=") })
            assertFalse(lines.any { it.contains("finalToActionStartLatency") })
        }
    }
    @Test fun recognizerCatalogRequeryPreservesConfiguredChoiceAndExcludesAppStub() {
        val target = instrumentation.targetContext
        val before = Settings.Secure.getString(target.contentResolver, "voice_recognition_service")
        CommandRecognizer.invalidateCatalog()
        val first = CommandRecognizer.component(target)
        assertEquals(first, CommandRecognizer.component(target))
        CommandRecognizer.invalidateCatalog()
        assertEquals(first, CommandRecognizer.component(target))
        assertNotEquals(target.packageName, first?.packageName)
        assertEquals(before, Settings.Secure.getString(target.contentResolver, "voice_recognition_service"))
    }
    @Test fun rawScreenCancellationInvalidSessionAndMissingDesktopNeverEmitActionStart() {
        val target = instrumentation.targetContext
        assertTrue(target.getSystemService(android.hardware.display.DisplayManager::class.java)
            .getDisplays(com.khuongnd.dexassistant.ui.screen.DexAccessibilityService.SAMSUNG_DESKTOP_CATEGORY).isEmpty())
        instrumentation.runOnMainSync {
            // Disposable unbound service: attach SDK base context only, never register/enable Accessibility.
            val service = com.khuongnd.dexassistant.ui.screen.DexAccessibilityService()
            ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java)
                .apply { isAccessible = true }.invoke(service, target)
            val commands = listOf(
                com.khuongnd.dexassistant.ui.screen.ScreenCommand(com.khuongnd.dexassistant.ui.screen.ScreenVerb.CANCEL) to "CANCELLED",
                com.khuongnd.dexassistant.ui.screen.ScreenCommand(com.khuongnd.dexassistant.ui.screen.ScreenVerb.INVALID) to "INVALID_SCREEN_COMMAND",
                com.khuongnd.dexassistant.ui.screen.ScreenCommand(com.khuongnd.dexassistant.ui.screen.ScreenVerb.GRID) to "DEX_DISPLAY_UNAVAILABLE",
                com.khuongnd.dexassistant.ui.screen.ScreenCommand(com.khuongnd.dexassistant.ui.screen.ScreenVerb.CLICK_CELL, 1) to "SCREEN_SESSION_EXPIRED")
            var starts = 0
            commands.forEach { (command, expected) ->
                var completed: String? = null
                service.execute(command, { starts++ }) { completed = it }
                assertEquals(expected, completed)
            }
            assertEquals(0, starts)
            service.clear()
        }
    }

    private fun fixture(block: (Context, CommandHistoryStore, MutableList<Intent>) -> Unit) {
        val target = instrumentation.targetContext
        val scope = "t021-metrics-${UUID.randomUUID()}-"
        val prefs = mutableSetOf<String>()
        val captured = java.util.concurrent.CopyOnWriteArrayList<Intent>()
        val context = object : ContextWrapper(target) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                synchronized(prefs) { prefs += name }
                return target.getSharedPreferences(scope + name, mode)
            }
            override fun startActivity(intent: Intent) { captured += Intent(intent) }
        }
        val history = CommandHistoryStore(context, "history", scope + "history-key")
        try { block(context, history, captured) }
        finally {
            history.flush(); history.clear()
            prefs.forEach { target.getSharedPreferences(scope + it, 0).edit().clear().commit() }
        }
    }
}
