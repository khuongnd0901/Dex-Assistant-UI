package com.khuongnd.dexassistant.ui

import android.app.UiAutomation
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.routing.CommandRegistry
import com.khuongnd.dexassistant.ui.screen.AppUiAdapters
import com.khuongnd.dexassistant.ui.screen.DesktopUiAccess
import com.khuongnd.dexassistant.ui.screen.DexAccessibilityService
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit read-only selected-DeX observation: no gestures, overlay, speech or state writes. */
class YoutubeSemanticObserverDeviceTest {
    @Test fun observeCurrentYoutubeCardsAndCaptionStructure() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("observeYoutubeSemantic") == "true")
        instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        var metadata: Bundle? = null
        instrumentation.runOnMainSync {
            val service = DexAccessibilityService.active ?: return@runOnMainSync
            val tree = DesktopUiAccess(service).tree() ?: return@runOnMainSync
            if (tree.packageName != CommandRegistry.default().appPackage("youtube")) return@runOnMainSync
            val cards = AppUiAdapters.youtubeResults(tree.nodes)
            val caption = Regex("caption|close|maximiz|minimiz|decor", RegexOption.IGNORE_CASE)
            val identifier = Regex("[A-Za-z0-9_.$:/-]{1,180}")
            metadata = Bundle().apply {
                putInt("displayId", tree.displayId)
                putInt("windowId", tree.windowId)
                putInt("nodeCount", tree.nodes.size)
                putInt("semanticCardCount", cards.size)
                putBoolean("unambiguousCards", AppUiAdapters.youtubeChoicesUnambiguous(cards))
                putInt("clickableCardCount", cards.count { it.node.clickable })
                val structures = tree.nodes.filter { caption.containsMatchIn(it.viewId) }
                    .map { node -> listOf(node.viewId, node.className).filter(identifier::matches).joinToString("|") }
                    .filter { it.isNotEmpty() }.distinct().take(64)
                putString("captionStructures", structures.joinToString(","))
                putInt("captionStructureCount", structures.size)
            }
        }
        assertNotNull("No bound service with a selected Samsung DeX YouTube tree", metadata)
        instrumentation.sendStatus(0, requireNotNull(metadata))
    }
}
