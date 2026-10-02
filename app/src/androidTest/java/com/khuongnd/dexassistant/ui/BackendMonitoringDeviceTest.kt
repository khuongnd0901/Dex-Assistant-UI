package com.khuongnd.dexassistant.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/** Requires compose.avd.yml and adb reverse tcp:8081 tcp:8081. */
class BackendMonitoringDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun authenticatedDockerQuotaAppearsOnAdministrationScreen() {
        compose.onNodeWithText("WebSocket endpoint").performScrollTo()
            .performTextClearance()
        compose.onNodeWithText("WebSocket endpoint").performTextInput("ws://127.0.0.1:8081/ws/v1/assistant")
        compose.onNodeWithText("Bearer token").performTextInput("avd-test-token")
        compose.onNodeWithText("Provision and connect").performScrollTo().performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("gemini-primary / configured-gemini:", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Snapshot: fresh").performScrollTo().assertExists()
        compose.onNodeWithText("ESTIMATED", substring = true).assertExists()
    }
}
