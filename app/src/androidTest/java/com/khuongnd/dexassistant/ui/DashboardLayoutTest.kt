package com.khuongnd.dexassistant.ui

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.khuongnd.dexassistant.ui.screen.DexAccessibilityService
import com.khuongnd.dexassistant.ui.theme.DexAssistantTheme
import org.junit.Test
import org.junit.Assert.*

/** Real renderer/semantics without Compose TestScope: compatible with --no-restart. */
@OptIn(ExperimentalComposeUiApi::class)
class DashboardLayoutTest {
    @Test fun compactAndWidePrimaryControlsFitWithoutScrollAndResumeIsReachable() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val policy = context.getSharedPreferences("voice_policy",0); val originalPolicy = policy.all
        policy.edit().putString(com.khuongnd.dexassistant.ui.voice.VoiceAutoStart.KEY_SUPPRESSION,"TEST_LAYOUT").commit()
        val display = context.getSystemService(DisplayManager::class.java).getDisplays(DexAccessibilityService.SAMSUNG_DESKTOP_CATEGORY).singleOrNull { it.isValid }
        val options = ActivityOptions.makeBasic()
        display?.let { val size = Point(); it.getRealSize(size); options.setLaunchDisplayId(it.displayId); options.setLaunchBounds(Rect(0,0,size.x,size.y)) }
        val dimensions = mutableStateOf(320 to 480); var resumes = 0; var diagnostics = 0; var sends = 0
        var view: ComposeView? = null
        fun root(view: View): ViewRootForTest? {
            if (view is ViewRootForTest) return view
            if (view is ViewGroup) for (i in 0 until view.childCount) root(view.getChildAt(i))?.let { return it }
            return null
        }
        try { ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java), options.toBundle()).use { scenario ->
            scenario.onActivity { activity ->
                view = ComposeView(activity).apply {
                    setContent { DexAssistantTheme { AssistantScreen(state=UiState(voiceState="PAUSED"), onRefresh={}, onApplyPermissions={}, onStartVoice={},
                        onStopVoice={}, onSend={ sends++ }, onClearHistory={},
                        modifier=Modifier.requiredSize(dimensions.value.first.dp, dimensions.value.second.dp), onResumeVoice={resumes++}, onDiagnostic={diagnostics++}) } }
                }
                activity.setContentView(view)
            }
            for (viewport in listOf(320 to 480, 800 to 400, 600 to 320)) {
                scenario.onActivity { dimensions.value = viewport }
                // Wait for actual renderer measure/layout, with no coroutine test dispatcher.
                val deadline = android.os.SystemClock.elapsedRealtime() + 3000
                var settled = false
                while (!settled && android.os.SystemClock.elapsedRealtime() < deadline) {
                    Thread.sleep(50)
                    scenario.onActivity {
                        val owner = view?.let(::root)
                        settled = owner != null && !owner.hasPendingMeasureOrLayout && owner.semanticsOwner.getAllSemanticsNodes(false).any { node -> node.config.getOrNull(SemanticsProperties.TestTag) == "dashboard" }
                    }
                }
                assertTrue("Renderer must settle", settled)
                scenario.onActivity {
                    val owner = requireNotNull(view?.let(::root))
                    val nodes = owner.semanticsOwner.getAllSemanticsNodes(false)
                    fun tagged(tag: String) = nodes.single { it.config.getOrNull(SemanticsProperties.TestTag) == tag }
                    val bounds = tagged("dashboard").boundsInRoot
                    for (tag in listOf("voiceStart","voicePause","voiceStop","advancedMenu","commandInput","runCommand","showAllCommands","transcript")) {
                        val control = tagged(tag).boundsInRoot
                        assertTrue("$tag outside viewport $viewport", control.width > 0 && control.height > 0 && control.left >= bounds.left && control.top >= bounds.top && control.right <= bounds.right && control.bottom <= bounds.bottom)
                        if (tag != "transcript") assertTrue("$tag clipped height", control.height >= 40f)
                    }
                    val card = tagged("transcript").boundsInRoot; val text = tagged("transcriptText").boundsInRoot
                    assertTrue("Transcript line must fit", text.height > 0 && text.top >= card.top && text.bottom <= card.bottom)
                    assertTrue("Only command pane may scroll", nodes.filter { it.config.contains(SemanticsActions.ScrollBy) }.all { it.boundsInRoot.left >= tagged("commandList").boundsInRoot.left })
                    if (viewport == 600 to 320) {
                        assertTrue(tagged("voiceStart").config[SemanticsActions.OnClick].action!!.invoke())
                    }
                }
                if (viewport == 320 to 480) {
                    scenario.onActivity {
                        val nodes = requireNotNull(view?.let(::root)).semanticsOwner.getAllSemanticsNodes(false)
                        assertTrue(nodes.single { it.config.getOrNull(SemanticsProperties.TestTag) == "showAllCommands" }
                            .config[SemanticsActions.OnClick].action!!.invoke())
                    }
                    val fullDeadline = android.os.SystemClock.elapsedRealtime() + 3_000
                    var fullReady = false
                    while (!fullReady && android.os.SystemClock.elapsedRealtime() < fullDeadline) {
                        Thread.sleep(50)
                        scenario.onActivity {
                            fullReady = view?.let(::root)?.semanticsOwner?.getAllSemanticsNodes(false)?.any {
                                it.config.getOrNull(SemanticsProperties.TestTag) == "commandSearch"
                            } == true
                        }
                    }
                    assertTrue("Full catalog did not open", fullReady)
                    scenario.onActivity {
                        val nodes = requireNotNull(view?.let(::root)).semanticsOwner.getAllSemanticsNodes(false)
                        fun tagged(tag: String) = nodes.firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == tag }
                            ?: error("Missing $tag; tags=${nodes.mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }}")
                        val bounds = tagged("dashboard").boundsInRoot
                        for (tag in listOf("commandSearch", "commandList")) {
                            val control = tagged(tag).boundsInRoot
                            assertTrue("$tag hidden in compact full list", control.height > 0 && control.bottom <= bounds.bottom)
                        }
                        assertTrue(tagged("showAllCommands").config[SemanticsActions.OnClick].action!!.invoke())
                    }
                    instrumentation.waitForIdleSync()
                }
            }
            assertEquals(1,resumes); assertEquals(0,diagnostics)
            scenario.onActivity {
                val nodes = requireNotNull(view?.let(::root)).semanticsOwner.getAllSemanticsNodes(false)
                assertTrue(nodes.single { it.config.getOrNull(SemanticsProperties.TestTag) == "showAllCommands" }
                    .config[SemanticsActions.OnClick].action!!.invoke())
            }
            val catalogDeadline = android.os.SystemClock.elapsedRealtime() + 3_000
            var catalogReady = false
            while (!catalogReady && android.os.SystemClock.elapsedRealtime() < catalogDeadline) {
                Thread.sleep(50)
                scenario.onActivity {
                    catalogReady = view?.let(::root)?.semanticsOwner?.getAllSemanticsNodes(false)?.any {
                        it.config.getOrNull(SemanticsProperties.TestTag) == "commandSearch"
                    } == true
                }
            }
            assertTrue("Full catalog did not open at 600x320", catalogReady)
            scenario.onActivity {
                val nodes = requireNotNull(view?.let(::root)).semanticsOwner.getAllSemanticsNodes(false)
                val example = nodes.single { it.config.getOrNull(SemanticsProperties.TestTag) == "example_open_app" }
                assertTrue(example.config[SemanticsActions.OnClick].action!!.invoke())
                assertEquals("Canonical tap must not run", 0, sends)
            }
            val canonicalDeadline = android.os.SystemClock.elapsedRealtime() + 3_000
            var canonicalReady = false
            while (!canonicalReady && android.os.SystemClock.elapsedRealtime() < canonicalDeadline) {
                Thread.sleep(50)
                scenario.onActivity {
                    canonicalReady = view?.let(::root)?.semanticsOwner?.getAllSemanticsNodes(false)?.any {
                        it.config.getOrNull(SemanticsProperties.TestTag) == "runCommand"
                    } == true
                }
            }
            assertTrue("Canonical choice did not reveal input", canonicalReady)
            scenario.onActivity {
                val nodes = requireNotNull(view?.let(::root)).semanticsOwner.getAllSemanticsNodes(false)
                val input = nodes.single { it.config.getOrNull(SemanticsProperties.TestTag) == "commandInput" }
                assertEquals("mở YouTube", input.config.getOrNull(SemanticsProperties.EditableText)?.text)
                assertTrue(nodes.single { it.config.getOrNull(SemanticsProperties.TestTag) == "showAllCommands" }
                    .config[SemanticsActions.OnClick].action!!.invoke())
            }
            catalogReady = false
            val aliasCatalogDeadline = android.os.SystemClock.elapsedRealtime() + 3_000
            while (!catalogReady && android.os.SystemClock.elapsedRealtime() < aliasCatalogDeadline) {
                Thread.sleep(50)
                scenario.onActivity {
                    catalogReady = view?.let(::root)?.semanticsOwner?.getAllSemanticsNodes(false)?.any {
                        it.config.getOrNull(SemanticsProperties.TestTag) == "commandSearch"
                    } == true
                }
            }
            assertTrue("Full catalog did not reopen", catalogReady)
            scenario.onActivity {
                val nodes = requireNotNull(view?.let(::root)).semanticsOwner.getAllSemanticsNodes(false)
                fun tagged(tag: String) = nodes.single { it.config.getOrNull(SemanticsProperties.TestTag) == tag }
                assertTrue("search=${tagged("commandSearch").boundsInRoot} list=${tagged("commandList").boundsInRoot}", tagged("commandSearch").boundsInRoot.height > 0)
                assertTrue(tagged("expand_open_app").config[SemanticsActions.OnClick].action!!.invoke())
            }
            val aliasDeadline = android.os.SystemClock.elapsedRealtime() + 3_000
            var aliasReady = false
            while (!aliasReady && android.os.SystemClock.elapsedRealtime() < aliasDeadline) {
                Thread.sleep(50)
                scenario.onActivity {
                    aliasReady = view?.let(::root)?.semanticsOwner?.getAllSemanticsNodes(false)?.any {
                        it.config.getOrNull(SemanticsProperties.TestTag) == "alias_open_app_1"
                    } == true
                }
            }
            assertTrue("Expanded alias did not appear", aliasReady)
            scenario.onActivity {
                val nodes = requireNotNull(view?.let(::root)).semanticsOwner.getAllSemanticsNodes(false)
                val alias = nodes.single { it.config.getOrNull(SemanticsProperties.TestTag) == "alias_open_app_1" }
                assertTrue(alias.config[SemanticsActions.OnClick].action!!.invoke())
                assertEquals("Alias tap must not run", 0, sends)
            }
            val inputDeadline = android.os.SystemClock.elapsedRealtime() + 3_000
            var inputReady = false
            while (!inputReady && android.os.SystemClock.elapsedRealtime() < inputDeadline) {
                Thread.sleep(50)
                scenario.onActivity {
                    inputReady = view?.let(::root)?.semanticsOwner?.getAllSemanticsNodes(false)?.any {
                        it.config.getOrNull(SemanticsProperties.TestTag) == "runCommand"
                    } == true
                }
            }
            assertTrue("Run input did not return after closing catalog", inputReady)
            scenario.onActivity {
                val nodes = requireNotNull(view?.let(::root)).semanticsOwner.getAllSemanticsNodes(false)
                val input = nodes.single { it.config.getOrNull(SemanticsProperties.TestTag) == "commandInput" }
                assertEquals("bật YouTube", input.config.getOrNull(SemanticsProperties.EditableText)?.text)
                val run = nodes.single { it.config.getOrNull(SemanticsProperties.TestTag) == "runCommand" }
                assertTrue(run.config[SemanticsActions.OnClick].action!!.invoke())
            }
            assertEquals(1, sends)
        } } finally { restoreVoicePolicy(context, originalPolicy) }
    }
}
