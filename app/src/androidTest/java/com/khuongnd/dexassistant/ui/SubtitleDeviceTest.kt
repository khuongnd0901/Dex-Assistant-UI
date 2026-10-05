package com.khuongnd.dexassistant.ui

import android.accessibilityservice.AccessibilityService
import android.app.ActivityOptions
import android.app.UiAutomation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.SystemClock
import android.view.SurfaceControl
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.screen.*
import com.khuongnd.dexassistant.ui.voice.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Opt-in disposable display fixture. No transcripts/screenshots are exported or owner stores changed. */
class SubtitleDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun field(target: Any, name: String): Any? = target.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(target)
    private fun prerequisites(): Pair<DexAccessibilityService, android.view.Display> {
        assumeTrue("Explicit subtitle fixture opt-in required", InstrumentationRegistry.getArguments().getString("runSubtitleFixture") == "true")
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val display = context.getSystemService(DisplayManager::class.java)
            .getDisplays(DexAccessibilityService.SAMSUNG_DESKTOP_CATEGORY).filter { it.isValid && it.displayId != 0 }.singleOrNull()
        assertNotNull("Actual unique Samsung DeX display required", display)
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (DexAccessibilityService.active == null && SystemClock.uptimeMillis() < deadline) Thread.sleep(50)
        val service = requireNotNull(DexAccessibilityService.active) { "Owner-enabled Accessibility must already be bound" }
        assertNotNull(service.serviceInfo)
        val size = Point().also(display!!::getRealSize)
        context.startActivity(Intent(context, ScreenControlFixtureActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            ActivityOptions.makeBasic().setLaunchDisplayId(display.displayId).setLaunchBounds(Rect(0, 0, size.x, size.y)).toBundle())
        val fixtureDeadline = SystemClock.uptimeMillis() + 5000
        while (ScreenControlFixtureActivity.active?.display?.displayId != display.displayId && SystemClock.uptimeMillis() < fixtureDeadline) Thread.sleep(50)
        main {
            val activity = requireNotNull(ScreenControlFixtureActivity.active)
            assertEquals(display.displayId, activity.display?.displayId)
            // Static white disposable content makes overlay pixels measurable without owner app content.
            activity.setContentView(android.view.View(activity).apply { setBackgroundColor(android.graphics.Color.WHITE); isClickable = true })
            service.clear(); VoiceSubtitles.store.clear()
        }
        Thread.sleep(1200)
        return service to display
    }
    private fun renderer(service: DexAccessibilityService) = field(service, "subtitleRenderer") as DexSubtitleRenderer
    private fun isVisible(renderer: DexSubtitleRenderer): Boolean = (field(renderer, "control") as? SurfaceControl)?.isValid == true || field(renderer, "view") != null
    private fun cropHash(service: DexAccessibilityService, display: Int): Long {
        Thread.sleep(450) // Accessibility screenshot rate limit, never a gesture retry.
        val done = CountDownLatch(1); var hash = 0L; var failure: String? = null
        main { service.takeScreenshot(display, context.mainExecutor, object : AccessibilityService.TakeScreenshotCallback {
            override fun onFailure(errorCode: Int) { failure = "capture error $errorCode"; done.countDown() }
            override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                var hardware: Bitmap? = null; var bitmap: Bitmap? = null
                try {
                    hardware = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                    bitmap = hardware?.copy(Bitmap.Config.ARGB_8888, false)
                    val image = requireNotNull(bitmap)
                    // Bottom-centered crop excludes clock/taskbar and all owner content.
                    hash = 1125899906842597L
                    for (y in image.height * 2 / 3 until image.height - 100 step 3)
                        for (x in image.width / 5 until image.width * 4 / 5 step 3) hash = hash * 31 + image.getPixel(x, y)
                } catch (error: Exception) { failure = error.javaClass.simpleName }
                finally { bitmap?.recycle(); hardware?.recycle(); result.hardwareBuffer.close(); done.countDown() }
            }
        }) }
        assertTrue(done.await(5, TimeUnit.SECONDS)); assertNull(failure)
        return hash
    }
    @Test fun subtitlesCoexistWithGridAndCompositorSuspensionExcludesPixels() {
        val (service, display) = prerequisites(); val renderer = renderer(service)
        try {
            main { service.execute(ScreenCommand(ScreenVerb.GRID)) { assertEquals("SCREEN_CONTINUE", it) } }
            val baseline = cropHash(service, display.displayId)
            main {
                VoiceSubtitles.store.begin(900001); VoiceSubtitles.store.phase(900001, SubtitlePhase.LISTENING)
                VoiceSubtitles.store.text(900001, "Phụ đề kiểm tra tiếng Việt", false, SystemClock.elapsedRealtime())
            }
            val visible = cropHash(service, display.displayId)
            assertNotEquals("Subtitle must actually contribute visible pixels", baseline, visible)
            main {
                assertTrue(isVisible(renderer)); assertNotNull(field(service, "session"))
                assertTrue(ScreenControlFixtureActivity.active!!.window.decorView.hasWindowFocus())
                service.execute(ScreenCommand(ScreenVerb.SUBDIVIDE, 2)) { assertEquals("SCREEN_CONTINUE", it) }
                service.clear(); assertTrue("Grid cleanup must preserve subtitle", isVisible(renderer))
            }
            val noGridVisible = cropHash(service, display.displayId)
            var release: (() -> Unit)? = null; val detached = CountDownLatch(1)
            main { release = renderer.suspendCapture { detached.countDown() } }
            assertTrue(detached.await(5, TimeUnit.SECONDS))
            main { assertFalse(isVisible(renderer)); VoiceSubtitles.store.text(900001, "Giả thuyết mới", false, SystemClock.elapsedRealtime()); assertFalse(isVisible(renderer)) }
            val excluded = cropHash(service, display.displayId)
            assertNotEquals(noGridVisible, excluded)
            main { release!!.invoke(); assertTrue(isVisible(renderer)); assertEquals("Giả thuyết mới", VoiceSubtitles.store.state.value?.text) }
            assertNotEquals(excluded, cropHash(service, display.displayId))
        } finally { main { VoiceSubtitles.store.clear(); service.clear(); ScreenControlFixtureActivity.active?.finish() } }
    }
    @Test fun optInReadOnlyLivePresentationMetadata() {
        assumeTrue("Explicit read-only live metadata opt-in required", InstrumentationRegistry.getArguments().getString("inspectSubtitleMetadata") == "true")
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        main {
            val service = requireNotNull(DexAccessibilityService.active)
            val renderer = renderer(service)
            val state = VoiceSubtitles.store.state.value
            val names = listOf("captures", "detachInFlight", "closed", "suppressed", "boundTurn", "boundDisplay")
            val session = field(service, "session")
            android.util.Log.i("DexSubtitleTest", "screen active=${session != null} depth=${session?.let { field(it, "depth") }} grid=${session?.let { field(it, "grid") }} control=${(field(service, "overlayControl") as? SurfaceControl)?.isValid}")
            android.util.Log.i("DexSubtitleTest", "live phase=${state?.phase} turn=${state?.turn} visible=${isVisible(renderer)} " + names.joinToString(" ") { "$it=${field(renderer, it)}" })
        }
    }

    @Test fun backgroundNoOverlayCaptureBarrierCompletesAndNextTurnRenders() {
        val (service, _) = prerequisites(); val renderer = renderer(service)
        try {
            main { VoiceSubtitles.store.clear(); service.clear(); ScreenControlFixtureActivity.active?.finish() }
            Thread.sleep(700)
            val ready = CountDownLatch(1); var release: (() -> Unit)? = null
            main { assertFalse(isVisible(renderer)); release = renderer.suspendCapture { ready.countDown() } }
            assertTrue("No overlay has nothing to detach: callback must complete even without app ViewRoot", ready.await(2, TimeUnit.SECONDS))
            main {
                release!!.invoke()
                assertEquals(false, field(renderer, "detachInFlight"))
                VoiceSubtitles.store.begin(900007); VoiceSubtitles.store.text(900007, "Lượt mới từ background", false, SystemClock.elapsedRealtime())
                assertTrue(isVisible(renderer))
            }
        } finally { main { VoiceSubtitles.store.clear(); service.clear(); ScreenControlFixtureActivity.active?.finish() } }
    }

    @Test fun productionImagePreparationAndRevalidationKeepLiveSubtitleIndependent() {
        assumeTrue("Explicit subtitle fixture opt-in required", InstrumentationRegistry.getArguments().getString("runSubtitleFixture") == "true")
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val service = requireNotNull(DexAccessibilityService.active)
        try {
            main {
                VoiceSubtitles.store.begin(900006)
                VoiceSubtitles.store.text(900006, "Phụ đề trong capture mục tiêu", false, SystemClock.elapsedRealtime())
            }
            // Reuse the existing owner-safe canvas-X fixture: actual preparation screenshot,
            // retained patch fingerprint, revalidation capture and one fixture-only gesture.
            NumberedScreenDeviceTest().canvasXWithoutAccessibilityNodeRequiresFreshPatchAndDispatchesLocally()
            main {
                assertEquals(900006L, VoiceSubtitles.store.state.value?.turn)
                assertEquals(0, field(renderer(service), "captures"))
                assertTrue("Production grid/tap cleanup preserves current subtitle", isVisible(renderer(service)))
            }
        } finally { main { VoiceSubtitles.store.clear(); service.clear(); ScreenControlFixtureActivity.active?.finish() } }
    }

    @Test fun overlappingCapturesCancellationExpiryAndInterruptNeverRestoreOldTurn() {
        val (service, _) = prerequisites(); val renderer = renderer(service)
        try {
            main { VoiceSubtitles.store.begin(900002); VoiceSubtitles.store.text(900002, "Lượt đầu", false, SystemClock.elapsedRealtime()) }
            var first: (() -> Unit)? = null; var second: (() -> Unit)? = null
            val ready = CountDownLatch(2)
            main { first = renderer.suspendCapture { ready.countDown() }; second = renderer.suspendCapture { ready.countDown() } }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            main {
                VoiceSubtitles.store.begin(900003); VoiceSubtitles.store.text(900003, "Lượt mới", false, SystemClock.elapsedRealtime())
                first!!.invoke(); assertFalse("Second pending capture holds suspension", isVisible(renderer))
                second!!.invoke(); assertTrue(isVisible(renderer)); assertEquals(900003L, VoiceSubtitles.store.state.value?.turn)
                service.onInterrupt(); assertFalse(isVisible(renderer))
                VoiceSubtitles.store.text(900003, "Callback muộn", false, SystemClock.elapsedRealtime()); assertFalse(isVisible(renderer))
                VoiceSubtitles.store.begin(900004); VoiceSubtitles.store.text(900004, "Final", true, SystemClock.elapsedRealtime())
                assertTrue(isVisible(renderer))
            }
            Thread.sleep(2800); main { assertTrue("Final retained before 3s deadline", isVisible(renderer)); service.clear(); assertTrue(isVisible(renderer)) }
            Thread.sleep(500); main { assertFalse("Independent expiry clears final", isVisible(renderer)); assertNull(VoiceSubtitles.store.state.value); assertNull("Expired renderer releases transcript closure", field(renderer, "drawing")) }
            val canceled = CountDownLatch(1); var end: (() -> Unit)? = null
            main { VoiceSubtitles.store.begin(900005); end = renderer.suspendCapture { canceled.countDown() }; VoiceSubtitles.store.clear() }
            assertTrue(canceled.await(5, TimeUnit.SECONDS)); main { end!!.invoke(); assertFalse(isVisible(renderer)); assertNull("Canceled renderer releases transcript closure", field(renderer, "drawing")) }
        } finally { main { VoiceSubtitles.store.clear(); service.clear(); ScreenControlFixtureActivity.active?.finish() } }
    }
}
