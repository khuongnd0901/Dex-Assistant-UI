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
        val dimensions = mutableStateOf(320 to 480); var resumes = 0; var diagnostics = 0
        var view: ComposeView? = null
        fun root(view: View): ViewRootForTest? {
            if (view is ViewRootForTest) return view
            if (view is ViewGroup) for (i in 0 until view.childCount) root(view.getChildAt(i))?.let { return it }
            return null
        }
        try { ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java), options.toBundle()).use { scenario ->
            scenario.onActivity { activity ->
                view = ComposeView(activity).apply {
                    setContent { DexAssistantTheme { AssistantScreen(UiState(voiceState="PAUSED"), { _, _ -> }, {}, {}, {}, {}, {}, {}, {},
                        Modifier.requiredSize(dimensions.value.first.dp, dimensions.value.second.dp), onResumeVoice={resumes++}, onDiagnostic={diagnostics++}) } }
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
                    for (tag in listOf("voiceStart","voicePause","voiceStop","diagnosticListen","settings","quota","history","command","transcript")) {
                        val control = tagged(tag).boundsInRoot
                        assertTrue("$tag outside viewport $viewport", control.width > 0 && control.height > 0 && control.left >= bounds.left && control.top >= bounds.top && control.right <= bounds.right && control.bottom <= bounds.bottom)
                        if (tag != "transcript") assertTrue("$tag clipped height", control.height >= 40f)
                    }
                    val card = tagged("transcript").boundsInRoot; val text = tagged("transcriptText").boundsInRoot
                    assertTrue("Transcript line must fit", text.height > 0 && text.top >= card.top && text.bottom <= card.bottom)
                    assertFalse("Main page must have no scroll", nodes.any { it.config.contains(SemanticsActions.ScrollBy) })
                    if (viewport == 600 to 320) {
                        assertTrue(tagged("voiceStart").config[SemanticsActions.OnClick].action!!.invoke())
                        assertTrue(tagged("diagnosticListen").config[SemanticsActions.OnClick].action!!.invoke())
                    }
                }
            }
            assertEquals(1,resumes); assertEquals(1,diagnostics)
        } } finally { restoreVoicePolicy(context, originalPolicy) }
    }
}
