package com.khuongnd.dexassistant.ui

import android.app.UiAutomation
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.actions.LocalActionExecutor
import com.khuongnd.dexassistant.ui.actions.LocalOutcome
import com.khuongnd.dexassistant.ui.actions.LocalResult
import com.khuongnd.dexassistant.ui.device.DpcClient
import com.khuongnd.dexassistant.ui.execution.ActionRouter
import com.khuongnd.dexassistant.ui.routing.ActionCatalog
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import com.khuongnd.dexassistant.ui.screen.DexAccessibilityService
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Opens YouTube through production policy, observes indexes; never selects or gestures. */
class YoutubePreviewDeviceTest {
    @Test fun launchPreviewSurvivesFeedbackAndVoicePreparation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("runYoutubePreviewFixture") == "true")
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val context = instrumentation.targetContext
        val completed = CountDownLatch(1)
        var router: ActionRouter? = null
        var result: LocalResult? = null
        var service: DexAccessibilityService? = null
        instrumentation.runOnMainSync {
            service = DexAccessibilityService.active
            assertNotNull("Owner Accessibility must already be bound", service)
            val registry = CommandRegistry.load(context)
            router = ActionRouter(context, registry, LocalActionExecutor(context, DpcClient(context), registry::appPackage))
            router!!.execute("youtube-preview-fixture", ActionCatalog.validate("APP_LAUNCH", mapOf("app" to "youtube")), null) {
                result = it; completed.countDown()
            }
        }
        try {
            assertTrue("Bounded launch observation did not finish", completed.await(12, TimeUnit.SECONDS))
            assertEquals(LocalOutcome.VERIFIED, result?.outcome)
            assertEquals("YOUTUBE_INDEX_READY", result?.errorCode)
            instrumentation.runOnMainSync {
                val current = requireNotNull(service)
                val count = current.youtubePreviewCount
                assertTrue("Readable semantic card indexes required", count in 1..50)
                current.clearForFeedback()
                assertEquals(count, current.youtubePreviewCount)
                var readiness: String? = null
                current.prepareForVoiceCommand { readiness = it }
                assertEquals("READY", readiness)
                assertEquals(count, current.youtubePreviewCount)
                assertTrue("Index surface/window must be attached", current.youtubePreviewOverlayAttached)
                instrumentation.sendStatus(0, Bundle().apply {
                    putInt("semanticPreviewCount", count)
                    putBoolean("previewPreserved", true)
                    putBoolean("overlayAttached", true)
                    putBoolean("pixelsVerified", false)
                })
            }
        } finally { instrumentation.runOnMainSync { router?.close() } }
    }
}
