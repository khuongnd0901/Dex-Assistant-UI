package com.khuongnd.dexassistant.ui

import org.junit.Ignore
import org.junit.Test

/** Retired backend transport acceptance; direct AI requires its own device evidence. */
@Ignore("T-018: Android no longer provisions or connects a backend WebSocket")
class CoordinatorProtocolDeviceTest {
    @Test fun correlatedPlanIsSerialDeduplicatedAndNotReplayedAfterReconnect() = Unit
}
