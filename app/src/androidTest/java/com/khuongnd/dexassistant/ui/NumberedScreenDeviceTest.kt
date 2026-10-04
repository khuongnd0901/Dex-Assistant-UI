package com.khuongnd.dexassistant.ui

import android.app.ActivityOptions
import android.app.UiAutomation
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.view.Surface
import android.view.SurfaceControl
import android.content.ComponentName
import android.provider.Settings
import android.os.SystemClock
import android.view.Display
import android.graphics.Rect
import android.graphics.Point
import android.content.Intent
import android.hardware.display.DisplayManager
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.history.CommandHistoryStore
import com.khuongnd.dexassistant.ui.screen.*
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.Before
import org.junit.Assume.assumeTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NumberedScreenDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    @Before fun preserveOwnerEnabledAccessibilityServices() {
        // Android's default UiAutomation suppresses real services. Do not change owner permissions.
        assertNotNull(instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES))
    }
    private fun desktopPrerequisites(): Pair<DexAccessibilityService, Display> {
        val display = context.getSystemService(DisplayManager::class.java)
            .getDisplays(DexAccessibilityService.SAMSUNG_DESKTOP_CATEGORY).singleOrNull { it.displayId != 0 }
        assumeNotNull(display)
        val expected = ComponentName(context, DexAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').mapNotNull(ComponentName::unflattenFromString).contains(expected)
        assumeTrue("Owner has not enabled the DeX accessibility service", enabled)
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (DexAccessibilityService.active == null && SystemClock.uptimeMillis() < deadline) Thread.sleep(50)
        val service = DexAccessibilityService.active
        assertNotNull("Enabled DeX accessibility service did not bind within 10 seconds; this is a failure, not a skip", service)
        assertNotNull("Bound service instance lost its framework Accessibility connection", service!!.serviceInfo)
        return service to requireNotNull(display)
    }
    private fun launchFixture(display: Display) {
        val size = Point(); display.getRealSize(size)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(display.displayId)
            .setLaunchBounds(Rect(0, 0, size.x, size.y))
        context.startActivity(Intent(context, ScreenControlFixtureActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), options.toBundle())
        // Initial resize/content events must settle before freezing the session mapping.
        var previous: Rect? = null; var stableSince = 0L
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (SystemClock.uptimeMillis() < deadline) {
            var rect: Rect? = null
            instrumentation.runOnMainSync {
                val activity = ScreenControlFixtureActivity.active
                if (activity?.display?.displayId == display.displayId) {
                    rect = fixtureVisibleBounds(activity)
                }
            }
            val now = SystemClock.uptimeMillis()
            if (rect != null && rect == previous) {
                if (now - stableSince >= 700) { awaitStableAccessibilitySnapshot(); return }
            } else { previous = rect; stableSince = now }
            Thread.sleep(50)
        }
        fail("Fixture did not settle on the expected desktop display within five seconds")
    }
    private fun awaitStableAccessibilitySnapshot() {
        val service = requireNotNull(DexAccessibilityService.active)
        val method = service.javaClass.getDeclaredMethod("snapshot").apply { isAccessible = true }
        var previous: Any? = null; var stableSince = SystemClock.uptimeMillis()
        val deadline = stableSince + 5000
        while (SystemClock.uptimeMillis() < deadline) {
            var current: Any? = null
            instrumentation.runOnMainSync { current = method.invoke(service) }
            val now = SystemClock.uptimeMillis()
            if (current != null && current == previous) { if (now - stableSince >= 900) return }
            else { previous = current; stableSince = now }
            Thread.sleep(150)
        }
        fail("Fixture Accessibility snapshot did not settle within five seconds")
    }
    private fun fixtureVisibleBounds(activity: ScreenControlFixtureActivity): Rect? {
        val decor = activity.window.decorView
        val global = Rect(); val local = Rect()
        if (!decor.getGlobalVisibleRect(global) || !decor.getLocalVisibleRect(local) || local.isEmpty) return null
        // Global view-tree coordinates may exclude a floating window's screen offset.
        val location = IntArray(2); decor.getLocationOnScreen(location)
        local.offset(location[0], location[1])
        return local
    }
    private fun assertFixtureContains(displayId: Int, target: ScreenRect) {
        instrumentation.runOnMainSync {
            val activity = requireNotNull(ScreenControlFixtureActivity.active)
            assertEquals("Fixture must be on gesture display", displayId, activity.display?.displayId)
            val visible = requireNotNull(fixtureVisibleBounds(activity)) { "Fixture decor must be visible" }
            assertTrue("Refuse gesture outside actual fixture bounds: $visible", visible.contains(target.x.toInt(), target.y.toInt()))
            assertTrue("Refuse gesture while fixture lacks window focus", activity.window.decorView.hasWindowFocus())
            val service = requireNotNull(DexAccessibilityService.active)
            val windows = service.windowsOnAllDisplays[displayId].orEmpty()
            fun ownsFixture(node: android.view.accessibility.AccessibilityNodeInfo?, depth: Int = 0): Boolean {
                if (node == null || depth > 20) return false
                try {
                    if (node.packageName?.toString() == context.packageName && node.text?.toString() == "DeX numbered control fixture") return true
                    for (index in 0 until minOf(node.childCount, 128)) if (ownsFixture(node.getChild(index), depth + 1)) return true
                    return false
                } finally { @Suppress("DEPRECATION") node.recycle() }
            }
            val fixture = windows.singleOrNull { ownsFixture(it.root) }
            assertNotNull("Refuse gesture without unique Accessibility fixture window", fixture)
            val fixtureBounds = Rect(); fixture!!.getBoundsInScreen(fixtureBounds)
            assertTrue("Refuse gesture outside Accessibility fixture window", fixtureBounds.contains(target.x.toInt(), target.y.toInt()))
            assertFalse("Refuse gesture covered by a higher-layer window", windows.any { window ->
                if (window.layer <= fixture.layer || window.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) false
                else { val bounds = Rect(); window.getBoundsInScreen(bounds); bounds.contains(target.x.toInt(), target.y.toInt()) }
            })
        }
    }
    private fun assertLiveGridOverlay(service: DexAccessibilityService, displayId: Int, fixtureFocus: Boolean = true) {
        instrumentation.runOnMainSync {
            if (fixtureFocus) assertTrue("Composition overlay must preserve fixture focus", ScreenControlFixtureActivity.active!!.window.decorView.hasWindowFocus())
            if (Build.VERSION.SDK_INT >= 34) {
                val surface = service.javaClass.getDeclaredField("overlaySurface").apply { isAccessible = true }.get(service) as? Surface
                val control = service.javaClass.getDeclaredField("overlayControl").apply { isAccessible = true }.get(service) as? SurfaceControl
                assertTrue("Overlay producer/control must be valid", surface?.isValid == true && control?.isValid == true)
            }
        }
        Thread.sleep(500) // Screenshot rate-limit and compositor settling, never a gesture retry.
        val done = CountDownLatch(1); var visible = false; var error = ""
        instrumentation.runOnMainSync {
            service.takeScreenshot(displayId, context.mainExecutor, object : AccessibilityService.TakeScreenshotCallback {
                override fun onFailure(errorCode: Int) { error = "screenshot:$errorCode"; done.countDown() }
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    var hardware: Bitmap? = null; var bitmap: Bitmap? = null
                    try {
                        hardware = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                        bitmap = hardware?.copy(Bitmap.Config.ARGB_8888, false)
                        if (bitmap != null) {
                            // RAM-only fixed badge crops, never screenshot content/export.
                            // Keep the original first-badge assertion while diagnosing occlusion.
                            val bounds = ScreenRect(0, 0, bitmap.width, bitmap.height)
                            val counts = (1..24).map { index ->
                                val cell = requireNotNull(bounds.cell(index))
                                var yellow = 0; var black = 0
                                for (y in cell.top + 2 until minOf(cell.top + 28, bitmap.height)) {
                                    for (x in cell.left + 2 until minOf(cell.left + 40, bitmap.width)) {
                                        val pixel = bitmap.getPixel(x, y)
                                        val red = pixel shr 16 and 255; val green = pixel shr 8 and 255; val blue = pixel and 255
                                        if (red > 180 && green > 180 && blue < 80) yellow++
                                        if (red < 20 && green < 20 && blue < 20) black++
                                    }
                                }
                                yellow to black
                            }
                            android.util.Log.i("DexScreenTest", "gridBadgeCandidates display=$displayId counts=" + counts.mapIndexed { index, count -> "${index + 1}:${count.first}/${count.second}" }.joinToString(","))
                            visible = counts.first().first >= 4 && counts.first().second >= 40
                        }
                    } catch (failure: RuntimeException) { error = failure.javaClass.simpleName }
                    finally { bitmap?.recycle(); hardware?.recycle(); result.hardwareBuffer.close(); done.countDown() }
                }
            })
        }
        assertTrue(done.await(5, TimeUnit.SECONDS))
        assertTrue("Grid badge must actually appear in the local screenshot ($error)", visible)
    }
    private fun assertLiveOverlayPassThrough(service: DexAccessibilityService, displayId: Int, target: ScreenRect) {
        assertFixtureContains(displayId, target)
        val done = CountDownLatch(1); var completed = false
        val path = Path().apply { moveTo(target.x, target.y) }
        val gesture = GestureDescription.Builder().setDisplayId(displayId)
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80)).build()
        ScreenControlFixtureActivity.lastTap = null
        instrumentation.runOnMainSync {
            assertTrue(service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(description: GestureDescription?) { completed = true; done.countDown() }
                override fun onCancelled(description: GestureDescription?) { done.countDown() }
            }, null))
        }
        assertTrue(done.await(5, TimeUnit.SECONDS)); assertTrue(completed)
        val tap = requireNotNull(ScreenControlFixtureActivity.lastTap)
        assertEquals(displayId, tap.displayId); assertEquals(target.x, tap.x, 2f); assertEquals(target.y, tap.y, 2f)
        if (Build.VERSION.SDK_INT >= 34) {
            val control = service.javaClass.getDeclaredField("overlayControl").apply { isAccessible = true }.get(service) as? SurfaceControl
            assertTrue("Pass-through must occur with overlay still alive", control?.isValid == true)
        }
    }
    private fun assertOverlayReleased(service: DexAccessibilityService) {
        for (name in listOf("overlay", "overlaySurface", "overlayControl")) {
            assertNull("$name must be released on cleanup", service.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(service))
        }
    }
    private fun fixtureImageDiagnostics(service: DexAccessibilityService, displayId: Int, bounds: ScreenRect): String {
        var geometry = ""
        instrumentation.runOnMainSync {
            service.clear()
            val activity = requireNotNull(ScreenControlFixtureActivity.active)
            val decor = activity.window.decorView
            val location = IntArray(2); decor.getLocationOnScreen(location)
            val visible = Rect(); decor.getGlobalVisibleRect(visible)
            val ownWindows = service.windowsOnAllDisplays[displayId].orEmpty().mapNotNull { window ->
                val root = window.root ?: return@mapNotNull null
                if (root.packageName?.toString() != context.packageName) return@mapNotNull null
                val windowBounds = Rect(); window.getBoundsInScreen(windowBounds)
                val rootBounds = Rect(); root.getBoundsInScreen(rootBounds)
                "window=$windowBounds/root=$rootBounds"
            }
            geometry = "decorLocation=${location.contentToString()},decorVisible=$visible,ownWindows=$ownWindows"
        }
        Thread.sleep(400)
        val done = CountDownLatch(1); var bitmap: Bitmap? = null; var failure = ""
        instrumentation.runOnMainSync {
            service.takeScreenshot(displayId, context.mainExecutor, object : AccessibilityService.TakeScreenshotCallback {
                override fun onFailure(errorCode: Int) { failure = "screenshot:$errorCode"; done.countDown() }
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    var hardware: Bitmap? = null
                    try { hardware = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace); bitmap = hardware?.copy(Bitmap.Config.ARGB_8888, false) }
                    catch (error: RuntimeException) { failure = error.javaClass.simpleName }
                    finally { hardware?.recycle(); result.hardwareBuffer.close(); done.countDown() }
                }
            })
        }
        if (!done.await(5, TimeUnit.SECONDS)) return "diagnostic-timeout"
        val full = bitmap ?: return failure
        var small: Bitmap? = null
        return try {
            val scale = minOf(1f, 640f / full.width, 400f / full.height)
            small = Bitmap.createScaledBitmap(full, (full.width * scale).toInt(), (full.height * scale).toInt(), false)
            val pixels = IntArray(small.width * small.height)
            small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
            val region = ScreenRect((bounds.left * scale).toInt(), (bounds.top * scale).toInt(), (bounds.right * scale).toInt(), (bounds.bottom * scale).toInt())
            val started = SystemClock.elapsedRealtime()
            val found = ScreenImageDetector.detect(pixels, small.width, small.height)
            val duration = SystemClock.elapsedRealtime() - started
            val regions = listOf(0, 24, 32, 48, 64, 96).map { offset ->
                val shifted = ScreenRect(region.left, region.top + (offset * scale).toInt(), region.right, region.bottom + (offset * scale).toInt())
                var bright = 0; var minX = shifted.right; var minY = shifted.bottom; var maxX = shifted.left; var maxY = shifted.top
                for (y in shifted.top.coerceAtLeast(0) until shifted.bottom.coerceAtMost(small.height))
                    for (x in shifted.left.coerceAtLeast(0) until shifted.right.coerceAtMost(small.width)) {
                        val p = pixels[y * small.width + x]
                        if (((p shr 16 and 255) + (p shr 8 and 255) + (p and 255)) / 3 > 160) {
                            bright++; minX = minOf(minX, x); minY = minOf(minY, y); maxX = maxOf(maxX, x); maxY = maxOf(maxY, y)
                        }
                    }
                "offset=$offset:bright=$bright,ink=${maxX-minX+1}x${maxY-minY+1},candidates=${found.count { it.inside(shifted) }}"
            }
            "overlayCleared=true,$geometry,scaled=${small.width}x${small.height},fixture=$region,regions=$regions,detectorMs=$duration,totalCandidates=${found.size}"
        } finally { if (small !== full) small?.recycle(); full.recycle() }
    }
    private fun command(core: AssistantCoordinator, text: String): String {
        val done = CountDownLatch(1); var status = ""
        instrumentation.runOnMainSync { core.dispatchVoice(text) { status = it.status; done.countDown() } }
        assertTrue(done.await(5, TimeUnit.SECONDS))
        return status
    }
    @Test fun numericAndMalformedFamiliesNeverBecomeBackendCommands() {
        val core = AssistantController.shared(context)
        val historyPreferences = context.getSharedPreferences("command_history_v1", android.content.Context.MODE_PRIVATE)
        CommandHistoryStore(context).flush() // Snapshot only after prior writes have drained.
        val originalRecords = historyPreferences.getString("records", null)
        instrumentation.runOnMainSync { core.disconnect(); DexAccessibilityService.active?.clear() }
        try {
            for (text in listOf("bấm ô mười một", "bấm ô 11 rồi mở youtube", "hủy rồi mở youtube", "không hiện lưới")) {
                val status = command(core, text)
                assertTrue(status in setOf("ACCESSIBILITY_PERMISSION_REQUIRED", "SCREEN_SESSION_EXPIRED", "INVALID_SCREEN_COMMAND"))
                assertEquals("LOCAL", CommandHistoryStore(context).read().first().route)
                assertFalse(status == "SEND_FAILED")
            }
        } finally {
            // Preserve existing ciphertext AND its Keystore key; clearHistory would rotate/delete the key.
            CommandHistoryStore(context).flush() // Drain queued writes before restoring owner ciphertext/key.
            val editor = historyPreferences.edit()
            if (originalRecords == null) editor.remove("records") else editor.putString("records", originalRecords)
            assertTrue(editor.commit())
        }
    }
    @Test fun threeSubdivisionsThenRejectFourthAndCancelCleansSession() {
        val (service, display) = desktopPrerequisites()
        launchFixture(display)
        try {
            var status = prepare(service)
            assertEquals("READY", status)
            instrumentation.runOnMainSync { service.execute(ScreenCommand(ScreenVerb.GRID)) { status = it }
            }
            repeat(3) {
                instrumentation.runOnMainSync { service.execute(ScreenCommand(ScreenVerb.SUBDIVIDE, 11)) { status = it } }
                assertEquals("SCREEN_CONTINUE", status)
            }
            instrumentation.runOnMainSync { service.execute(ScreenCommand(ScreenVerb.SUBDIVIDE, 11)) { status = it } }
            assertEquals("INVALID_SCREEN_CELL", status)
            assertEquals("READY", prepare(service))
            instrumentation.runOnMainSync { service.execute(ScreenCommand(ScreenVerb.CANCEL)) { status = it } }
            assertEquals("CANCELLED", status)
            assertOverlayReleased(service)
            instrumentation.runOnMainSync { service.execute(ScreenCommand(ScreenVerb.CLICK_CELL, 11)) { status = it } }
            assertEquals("SCREEN_SESSION_EXPIRED", status)
        } finally { instrumentation.runOnMainSync { service.clear(); ScreenControlFixtureActivity.active?.finish() } }
    }

    @Test fun canvasXWithoutAccessibilityNodeRequiresFreshPatchAndDispatchesLocally() {
        val (service, display) = desktopPrerequisites()
        launchFixture(display)
        try {
            assertEquals("READY", prepare(service!!))
            var freshBounds: ScreenRect? = null
            instrumentation.runOnMainSync { freshBounds = ScreenControlFixtureActivity.canvasXBounds }
            val expected = requireNotNull(freshBounds)
            assertFixtureContains(display.displayId, expected)
            // Test-only reflection avoids adding a production inspection/execution endpoint.
            val field = service.javaClass.getDeclaredField("imageTargets").apply { isAccessible = true }
            val fingerprints = service.javaClass.getDeclaredField("imageFingerprints").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST") val targets = field.get(service) as List<ScreenRect>
            @Suppress("UNCHECKED_CAST") val hashes = fingerprints.get(service) as Map<ScreenRect, Long>
            fun targetsFixture(rect: ScreenRect): Boolean {
                if (rect.x < expected.left || rect.x >= expected.right || rect.y < expected.top || rect.y >= expected.bottom) return false
                val intersection = (minOf(rect.right, expected.right) - maxOf(rect.left, expected.left)).coerceAtLeast(0).toLong() *
                    (minOf(rect.bottom, expected.bottom) - maxOf(rect.top, expected.top)).coerceAtLeast(0)
                return intersection * 2 >= rect.width.toLong() * rect.height
            }
            val index = targets.indexOfFirst { hashes.containsKey(it) && targetsFixture(it) }
            if (index < 0) {
                val known = hashes.keys.filter(::targetsFixture)
                val overlaps = known.map { image -> targets.filter { it.intersects(image) }.map { node ->
                    val intersection = (minOf(image.right, node.right) - maxOf(image.left, node.left)).coerceAtLeast(0).toLong() *
                        (minOf(image.bottom, node.bottom) - maxOf(image.top, node.top)).coerceAtLeast(0)
                    "imageFraction=${intersection.toDouble() / (image.width.toLong() * image.height)},targetFraction=${intersection.toDouble() / (node.width.toLong() * node.height)}"
                } }
                android.util.Log.i("DexScreenTest", "fixture.merge knownImageHashes=${known.size} retainedKnown=${targets.count(::targetsFixture)} boundaryDeltas=${known.map { listOf(it.left-expected.left,it.top-expected.top,it.right-expected.right,it.bottom-expected.bottom) }} overlapFractions=$overlaps")
                val diagnostic = fixtureImageDiagnostics(service, display.displayId, expected)
                fail("Bounded image detector must find the fixture X ($diagnostic)")
            }
            assertFixtureContains(display.displayId, targets[index])
            val done = CountDownLatch(1); var status = ""
            // Explicit screenshot pacing only; execute must itself remove the live overlay safely.
            Thread.sleep(1100)
            instrumentation.runOnMainSync { service.execute(ScreenCommand(ScreenVerb.CLICK_TARGET, index + 1)) { status = it; done.countDown() } }
            assertTrue(done.await(5, TimeUnit.SECONDS)); assertEquals("DISPATCHED", status)
            assertTrue(ScreenControlFixtureActivity.canvasClosed)
        } finally { instrumentation.runOnMainSync { service!!.clear(); ScreenControlFixtureActivity.active?.finish() } }
    }
    @Test fun liveForegroundGridPersistsThenCancelsOrDispatchesOnce() {
        val expectedPackage = InstrumentationRegistry.getArguments().getString("screenLivePackage")
        assumeTrue("Explicit live foreground app opt-in required", !expectedPackage.isNullOrBlank())
        val (service, display) = desktopPrerequisites()
        fun assertExpectedForeground() {
            instrumentation.runOnMainSync {
                val focused = service.windowsOnAllDisplays[display.displayId].orEmpty()
                    .filter { it.type != android.view.accessibility.AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY && it.isFocused }
                    .singleOrNull()
                assertNotNull("Live app must have unique focused desktop window", focused)
                val root = focused!!.root
                try {
                    val actualPackage = root?.packageName?.toString()
                    assertTrue("Focused desktop app must match explicit opt-in: actualPackage=$actualPackage expectedPackage=$expectedPackage windowId=${focused.id} displayId=${display.displayId}", actualPackage == expectedPackage)
                }
                finally { @Suppress("DEPRECATION") root?.recycle() }
            }
        }
        assertExpectedForeground()
        try {
            var status = ""
            instrumentation.runOnMainSync {
                service.clear()
                service.execute(ScreenCommand(ScreenVerb.GRID)) { status = it }
            }
            assertEquals("SCREEN_CONTINUE", status)
            assertLiveGridOverlay(service, display.displayId, fixtureFocus = false)
            SystemClock.sleep(5_000)
            assertExpectedForeground()
            instrumentation.runOnMainSync {
                val current = service.javaClass.getDeclaredField("session").apply { isAccessible = true }.get(service)
                assertNotNull("Live grid must survive five seconds of real app content events", current)
                assertNotNull(current!!.javaClass.getDeclaredField("grid").apply { isAccessible = true }.get(current))
            }
            assertLiveGridOverlay(service, display.displayId, fixtureFocus = false)
            if (InstrumentationRegistry.getArguments().getString("screenLiveTap") == "true") {
                assertExpectedForeground()
                instrumentation.runOnMainSync { service.execute(ScreenCommand(ScreenVerb.SUBDIVIDE, 1)) { status = it } }
                assertEquals("SCREEN_CONTINUE", status)
                val done = CountDownLatch(1)
                instrumentation.runOnMainSync {
                    service.execute(ScreenCommand(ScreenVerb.CLICK_CELL, 1)) { status = it; done.countDown() }
                }
                assertTrue(done.await(5, TimeUnit.SECONDS)); assertEquals("DISPATCHED", status)
                instrumentation.runOnMainSync {
                    assertOverlayReleased(service)
                    service.execute(ScreenCommand(ScreenVerb.CLICK_CELL, 1)) { status = it }
                }
                assertEquals("SCREEN_SESSION_EXPIRED", status)
            } else {
                instrumentation.runOnMainSync {
                    service.execute(ScreenCommand(ScreenVerb.CANCEL)) { status = it }
                    assertOverlayReleased(service)
                }
                assertEquals("CANCELLED", status)
                assertExpectedForeground()
            }
        } finally { instrumentation.runOnMainSync { service.clear() } }
    }
    @Test fun freshGridSurvivesContentAndWindowChangesThenDispatchesOnce() {
        val (service, display) = desktopPrerequisites()
        launchFixture(display)
        try {
            var status = ""
            ScreenControlFixtureActivity.lastTap = null
            instrumentation.runOnMainSync {
                service.clear()
                service.execute(ScreenCommand(ScreenVerb.GRID)) { status = it }
            }
            assertEquals("SCREEN_CONTINUE", status)
            instrumentation.runOnMainSync {
                val button = ScreenControlFixtureActivity.active!!.window.decorView
                    .findViewWithTag<android.widget.Button>("fixturePopupButton")
                button.text = "Dynamic playback label"
            }
            Thread.sleep(500)
            instrumentation.runOnMainSync {
                val notification = android.view.accessibility.AccessibilityEvent.obtain(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
                try { service.onAccessibilityEvent(notification) } finally { notification.recycle() }
                service.execute(ScreenCommand(ScreenVerb.SUBDIVIDE, 11)) { status = it }
            }
            assertEquals("SCREEN_CONTINUE", status)
            assertNull(ScreenControlFixtureActivity.lastTap)
            var dialog: android.app.AlertDialog? = null
            instrumentation.runOnMainSync {
                dialog = android.app.AlertDialog.Builder(ScreenControlFixtureActivity.active!!)
                    .setMessage("Grid window changed").setPositiveButton("Close", null).show()
            }
            Thread.sleep(500)
            instrumentation.runOnMainSync {
                assertNotNull(service.javaClass.getDeclaredField("session").apply { isAccessible = true }.get(service))
                service.execute(ScreenCommand(ScreenVerb.SUBDIVIDE, 11)) { status = it }
            }
            assertEquals("SCREEN_CONTINUE", status)
            instrumentation.runOnMainSync { dialog!!.dismiss() }
            Thread.sleep(500)
            val size = Point(); display.getRealSize(size)
            val expected = ScreenRect(0, 0, size.x, size.y).cell(11)!!.cell(11)!!.cell(11)!!
            assertFixtureContains(display.displayId, expected)
            val done = CountDownLatch(1)
            instrumentation.runOnMainSync {
                service.execute(ScreenCommand(ScreenVerb.CLICK_CELL, 11)) { status = it; done.countDown() }
            }
            assertTrue(done.await(5, TimeUnit.SECONDS)); assertEquals("DISPATCHED", status)
            val tap = requireNotNull(ScreenControlFixtureActivity.lastTap)
            assertEquals(display.displayId, tap.displayId)
            assertEquals(expected.x, tap.x, 2f); assertEquals(expected.y, tap.y, 2f)
            assertOverlayReleased(service)
            instrumentation.runOnMainSync { service.execute(ScreenCommand(ScreenVerb.CLICK_CELL, 11)) { status = it } }
            assertEquals("SCREEN_SESSION_EXPIRED", status)
        } finally { instrumentation.runOnMainSync { service.clear(); ScreenControlFixtureActivity.active?.finish() } }
    }
    @Test fun latePreparationCannotReviveCancelledVoiceOwnership() {
        val (service, display) = desktopPrerequisites()
        launchFixture(display)
        try {
            var valid = true
            var status = ""
            val done = CountDownLatch(1)
            instrumentation.runOnMainSync {
                service.prepare(isCurrent = { valid }) { status = it; done.countDown() }
                valid = false // Final/cancel takes ownership before asynchronous screenshot callback.
            }
            assertTrue(done.await(5, TimeUnit.SECONDS))
            assertEquals("SCREEN_SESSION_EXPIRED", status)
            instrumentation.runOnMainSync {
                assertNull(service.javaClass.getDeclaredField("session").apply { isAccessible = true }.get(service))
                assertOverlayReleased(service)
            }
        } finally { instrumentation.runOnMainSync { service.clear(); ScreenControlFixtureActivity.active?.finish() } }
    }
    @Test fun latePreparationCannotClearNewExplicitGrid() {
        val (service, display) = desktopPrerequisites()
        launchFixture(display)
        try {
            val done = CountDownLatch(1)
            var oldStatus = ""
            var gridStatus = ""
            var freshSession: Any? = null
            val field = service.javaClass.getDeclaredField("session").apply { isAccessible = true }
            instrumentation.runOnMainSync {
                service.prepare { oldStatus = it; done.countDown() }
                service.execute(ScreenCommand(ScreenVerb.GRID)) { gridStatus = it }
                freshSession = field.get(service)
            }
            assertEquals("SCREEN_CONTINUE", gridStatus)
            assertTrue(done.await(5, TimeUnit.SECONDS))
            assertEquals("SCREEN_SESSION_EXPIRED", oldStatus)
            instrumentation.runOnMainSync { assertSame("Old screenshot terminal cannot clear fresh grid", freshSession, field.get(service)) }
        } finally { instrumentation.runOnMainSync { service.clear(); ScreenControlFixtureActivity.active?.finish() } }
    }
    @Test fun changedAndReplacedSameBoundsNodeCannotDispatchBeforeEventDelivery() {
        val (service, display) = desktopPrerequisites()
        launchFixture(display)
        try {
            repeat(2) { replacement ->
                assertEquals("READY", prepare(service))
                var status = ""
                ScreenControlFixtureActivity.lastTap = null
                instrumentation.runOnMainSync {
                    val activity = requireNotNull(ScreenControlFixtureActivity.active)
                    val button = activity.window.decorView.findViewWithTag<android.widget.Button>("fixturePopupButton")
                    if (replacement == 0) button.text = "Changed fixture action"
                    else {
                        val parent = button.parent as android.view.ViewGroup
                        val index = parent.indexOfChild(button); val params = button.layoutParams
                        parent.removeView(button)
                        parent.addView(android.widget.Button(activity).apply { tag = "fixturePopupButton"; text = "Replacement fixture action" }, index, params)
                        parent.measure(android.view.View.MeasureSpec.makeMeasureSpec(parent.width, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(parent.height, android.view.View.MeasureSpec.EXACTLY))
                        parent.layout(parent.left, parent.top, parent.right, parent.bottom)
                    }
                    // Same main turn: invalidation must not depend on a delayed accessibility event.
                    service.execute(ScreenCommand(ScreenVerb.CLICK_TARGET, 1)) { status = it }
                }
                assertEquals("SCREEN_CHANGED", status)
                assertNull(ScreenControlFixtureActivity.lastTap); assertOverlayReleased(service)
                Thread.sleep(500)
            }
        } finally { instrumentation.runOnMainSync { service.clear(); ScreenControlFixtureActivity.active?.finish() } }
    }
    @Test fun changedWindowAndRealSessionExpiryCleanUpWithoutTap() {
        val (service, display) = desktopPrerequisites()
        launchFixture(display)
        try {
            assertEquals("READY", prepare(service))
            instrumentation.runOnMainSync {
                android.app.AlertDialog.Builder(ScreenControlFixtureActivity.active!!).setMessage("Fixture changed window").setPositiveButton("Close", null).show()
            }
            Thread.sleep(500)
            var status = ""
            instrumentation.runOnMainSync { service.execute(ScreenCommand(ScreenVerb.CLICK_CELL, 11)) { status = it } }
            assertTrue(status in setOf("SCREEN_CHANGED", "SCREEN_SESSION_EXPIRED")); assertOverlayReleased(service)
            instrumentation.runOnMainSync { ScreenControlFixtureActivity.active?.finish() }
            launchFixture(display)
            ScreenControlFixtureActivity.lastTap = null
            assertEquals("READY", prepare(service))
            instrumentation.runOnMainSync {
                val field = service.javaClass.getDeclaredField("session").apply { isAccessible = true }
                val frozen = requireNotNull(field.get(service))
                repeat(10) {
                    val notification = android.view.accessibility.AccessibilityEvent.obtain(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
                    try { service.onAccessibilityEvent(notification) } finally { notification.recycle() }
                    assertSame("Stable content notification must preserve prepared mapping", frozen, field.get(service))
                }
            }
            // Confirm the session survives until near its deadline, then the actual timer expires.
            SystemClock.sleep(34_000)
            instrumentation.runOnMainSync {
                assertNotNull(service.javaClass.getDeclaredField("session").apply { isAccessible = true }.get(service))
                if (Build.VERSION.SDK_INT >= 34) assertNotNull(service.javaClass.getDeclaredField("overlayControl").apply { isAccessible = true }.get(service))
            }
            SystemClock.sleep(1_500)
            instrumentation.runOnMainSync { service.execute(ScreenCommand(ScreenVerb.CLICK_CELL, 11)) { status = it } }
            assertEquals("SCREEN_SESSION_EXPIRED", status); assertOverlayReleased(service)
            assertNull(ScreenControlFixtureActivity.lastTap)
        } finally { instrumentation.runOnMainSync { service.clear(); ScreenControlFixtureActivity.active?.finish() } }
    }
    @Test fun optInHundredPrepareClearResourceSmoke() {
        assumeTrue("Explicit opt-in synthetic resource measurement", InstrumentationRegistry.getArguments().getString("screenResourceSmoke") == "true")
        val (service, display) = desktopPrerequisites()
        launchFixture(display)
        val memory = android.os.Debug.MemoryInfo(); android.os.Debug.getMemoryInfo(memory)
        val initialPss = memory.totalPss
        val started = SystemClock.elapsedRealtime(); val initialCpu = android.os.Process.getElapsedCpuTime()
        val timings = mutableListOf<Long>(); val pssSamples = mutableListOf(initialPss)
        try {
            repeat(100) { index ->
                val preparationStarted = SystemClock.elapsedRealtime()
                assertEquals("READY", prepare(service))
                timings.add(SystemClock.elapsedRealtime() - preparationStarted)
                if ((index + 1) % 10 == 0) { android.os.Debug.getMemoryInfo(memory); pssSamples.add(memory.totalPss) }
                instrumentation.runOnMainSync { service.clear() }; assertOverlayReleased(service)
                Thread.sleep(400) // Explicit preparation pacing; no gestures/retries.
            }
            android.os.Debug.getMemoryInfo(memory)
            val sorted = timings.sorted()
            android.util.Log.i("DexScreenTest", "syntheticPrepareClear sessions=100 prepareP50Ms=${sorted[49]} prepareP95Ms=${sorted[94]} prepareMaxMs=${sorted.last()} peakSampledPssKb=${pssSamples.maxOrNull()} pssSamplesKb=$pssSamples elapsedMs=${SystemClock.elapsedRealtime()-started} cpuMs=${android.os.Process.getElapsedCpuTime()-initialCpu} initialPssKb=$initialPss finalPssKb=${memory.totalPss}")
        } finally { instrumentation.runOnMainSync { service.clear(); ScreenControlFixtureActivity.active?.finish() } }
    }
    private fun prepare(service: DexAccessibilityService): String {
        val done = CountDownLatch(1); var status = ""
        instrumentation.runOnMainSync { service.prepare { status = it; done.countDown() } }
        assertTrue(done.await(5, TimeUnit.SECONDS)); return status
    }
    /** Requires explicit accessibility activation and an actual Samsung desktop display.
     * A skipped test is not hardware evidence. Never enables permissions or substitutes the phone. */
    @Test fun secondaryDisplayGridGestureHasCorrectCoordinatesAndConsumesSession() {
        val (service, display) = desktopPrerequisites()
        launchFixture(display)
        try {
            val prepared = CountDownLatch(1); var status = ""
            instrumentation.runOnMainSync { service!!.prepare { status = it; prepared.countDown() } }
            assertTrue(prepared.await(5, TimeUnit.SECONDS)); assertEquals("READY", status)
            instrumentation.runOnMainSync { service!!.execute(ScreenCommand(ScreenVerb.GRID)) { status = it } }
            assertEquals("SCREEN_CONTINUE", status)
            assertLiveGridOverlay(service, display.displayId)
            val size = Point(); display.getRealSize(size)
            val expected = ScreenRect(0, 0, size.x, size.y).cell(11)!!
            assertFixtureContains(display.displayId, expected)
            assertLiveOverlayPassThrough(service, display.displayId, expected)
            val done = CountDownLatch(1)
            ScreenControlFixtureActivity.lastTap = null
            instrumentation.runOnMainSync { service!!.execute(ScreenCommand(ScreenVerb.CLICK_CELL, 11)) { status = it; done.countDown() } }
            assertTrue(done.await(5, TimeUnit.SECONDS)); assertEquals("DISPATCHED", status)
            assertOverlayReleased(service)
            val tap = requireNotNull(ScreenControlFixtureActivity.lastTap)
            assertEquals(display.displayId, tap.displayId); assertEquals(expected.x, tap.x, 2f); assertEquals(expected.y, tap.y, 2f)
            instrumentation.runOnMainSync { service!!.execute(ScreenCommand(ScreenVerb.CLICK_CELL, 11)) { status = it } }
            assertEquals("SCREEN_SESSION_EXPIRED", status)
        } finally { instrumentation.runOnMainSync { service!!.clear(); ScreenControlFixtureActivity.active?.finish() } }
    }
}
