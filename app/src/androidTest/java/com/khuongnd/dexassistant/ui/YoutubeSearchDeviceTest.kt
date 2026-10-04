package com.khuongnd.dexassistant.ui

import android.app.ActivityOptions
import android.app.SearchManager
import android.app.UiAutomation
import android.content.ContextWrapper
import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.actions.LocalActionExecutor
import com.khuongnd.dexassistant.ui.actions.LocalOutcome
import com.khuongnd.dexassistant.ui.actions.LocalResult
import com.khuongnd.dexassistant.ui.actions.YoutubeSearchIntents
import com.khuongnd.dexassistant.ui.device.DpcClient
import com.khuongnd.dexassistant.ui.routing.LocalCommandParser
import com.khuongnd.dexassistant.ui.screen.DexAccessibilityService
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class YoutubeSearchDeviceTest {
    @Test fun searchIntentUsesPackageBoundResultsDeepLink() {
        val query = "tìm nhạc sơn tùng & rock+pop"
        val intent = YoutubeSearchIntents.build(query)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(YoutubeSearchIntents.PACKAGE_NAME, intent.`package`)
        assertEquals("https", intent.data?.scheme)
        assertEquals("www.youtube.com", intent.data?.host)
        assertEquals("/results", intent.data?.path)
        assertEquals(query, intent.data?.getQueryParameter("search_query"))
        assertFalse(intent.hasExtra(SearchManager.QUERY))
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.flags)
        val browser = YoutubeSearchIntents.build(query, inApp = false)
        assertNull(browser.`package`)
        assertEquals(intent.data, browser.data)
    }

    /** Actual UI regression, opt-in because it opens YouTube and submits a public test query.
     * No coordinator/history/preferences/keys or voice lifecycle are touched. No autoplay.
     * UI text stays in RAM and only fixed-fixture equality is evaluated, never exported.
     */
    @Test fun warmYoutubeSearchDisplaysRequestedQueryOnDex() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Explicit live app-search opt-in required", arguments.getString("runYoutubeSearchUi") == "true")
        val observeOnly = arguments.getString("observeYoutubeOnly") == "true"
        val variant = arguments.getString("youtubeSearchVariant") ?: "executor_default"
        assertTrue("Unknown test variant", variant in setOf("executor_default", "results_view", "legacy_search", "legacy_default"))
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val context = instrumentation.targetContext
        val displays = context.getSystemService(DisplayManager::class.java)
        val desktops = displays.getDisplays(DexAccessibilityService.SAMSUNG_DESKTOP_CATEGORY)
            .filter { it.displayId != 0 }
        val service = DexAccessibilityService.active
        assertNotNull("Owner-enabled Accessibility service must already be bound", service)
        val command = LocalCommandParser().route(arguments.getString("youtubeFixtureCommand") ?: "tìm nhạc Sơn Tùng")
        assertTrue("Fixture must route to YouTube search", command.arguments.containsKey("query"))
        val query = arguments.getString("expectedYoutubeQuery") ?: command.arguments.getValue("query")
        val requestedDisplay = arguments.getString("youtubeDisplayId")?.toIntOrNull()
        fun reportWindows(stage: String) {
            // Samsung Desktop can resolve by ID/category while omitted from unfiltered getDisplays().
            val displayIds = (displays.displays.map { it.displayId } + desktops.map { it.displayId } +
                listOfNotNull(requestedDisplay)).distinct()
            displayIds.forEach { displayId ->
                val visible = youtubeHasVisibleNode(requireNotNull(service), displayId, null)
                val queryMatched = youtubeHasVisibleNode(requireNotNull(service), displayId, query)
                instrumentation.sendStatus(0, android.os.Bundle().apply {
                    putString("stage", stage)
                    putInt("displayId", displayId)
                    putInt("desktopCategory", if (desktops.any { it.displayId == displayId }) 1 else 0)
                    putInt("youtubeActiveVisible", if (visible) 1 else 0)
                    putInt("fixtureQueryVisible", if (queryMatched) 1 else 0)
                })
            }
        }
        reportWindows("before")
        val display = if (requestedDisplay != null) displays.getDisplay(requestedDisplay) else desktops.singleOrNull()
        assumeTrue("Specify a current display or require one DeX display", display != null)
        val displayId = requireNotNull(display).displayId
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle()
        val launch = context.packageManager.getLaunchIntentForPackage(YOUTUBE_PACKAGE)
        assertNotNull("YouTube must be installed for this regression", launch)
        if (!observeOnly) instrumentation.runOnMainSync {
            context.startActivity(requireNotNull(launch).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), options)
        }
        await("YouTube launch never became visible on DeX") {
            youtubeHasVisibleNode(requireNotNull(service), displayId, null)
        }
        if (!observeOnly) assertFalse("Fixture query already visible; choose a distinct public fixture to avoid stale success",
            youtubeHasVisibleNode(requireNotNull(service), displayId, query))
        val onDex = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) { baseContext.startActivity(intent, options) }
        }
        if (!observeOnly) instrumentation.runOnMainSync {
            if (variant == "executor_default") {
                var result: LocalResult? = null
                // Actual production executor and ordinary application context: no launch-display override.
                LocalActionExecutor(context, DpcClient(context)).execute(command) { result = it }
                assertEquals(LocalOutcome.DISPATCHED, result?.outcome)
            } else if (variant.startsWith("legacy")) {
                val intent = Intent(Intent.ACTION_SEARCH).setPackage(YOUTUBE_PACKAGE)
                    .putExtra(SearchManager.QUERY, command.arguments.getValue("query"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (variant == "legacy_default") context.startActivity(intent) else onDex.startActivity(intent)
            } else {
                val uri = Uri.Builder().scheme("https").authority("www.youtube.com")
                    .path("/results").appendQueryParameter("search_query", command.arguments.getValue("query")).build()
                onDex.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(YOUTUBE_PACKAGE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        try {
            await("Requested search query absent in actual visible YouTube UI") {
                youtubeHasVisibleNode(requireNotNull(service), displayId, query)
            }
        } finally {
            reportWindows("after")
        }
        // This proves the search query was displayed, not result relevance or playback.
    }

    private fun await(message: String, predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (predicate()) return
            Thread.sleep(100)
        }
        fail(message)
    }

    private fun youtubeHasVisibleNode(service: DexAccessibilityService, displayId: Int, query: String?): Boolean {
        var found = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            var remaining = 4096
            fun visit(node: AccessibilityNodeInfo?, depth: Int): Boolean {
                if (node == null) return false
                try {
                    if (depth > 40 || remaining-- <= 0) return false
                    val owned = node.packageName?.toString() == YOUTUBE_PACKAGE
                    val matches = query == null || listOf(node.text, node.contentDescription).any {
                        it?.toString()?.trim()?.lowercase(Locale.ROOT) == query
                    }
                    if (owned && node.isVisibleToUser && matches) return true
                    for (index in 0 until minOf(node.childCount, 128)) {
                        if (visit(node.getChild(index), depth + 1)) return true
                    }
                    return false
                } finally { @Suppress("DEPRECATION") node.recycle() }
            }
            found = service.windowsOnAllDisplays[displayId].orEmpty().any {
                it.isActive && visit(it.root, 0)
            }
        }
        return found
    }

    companion object { private const val YOUTUBE_PACKAGE = "com.google.android.youtube" }
}
