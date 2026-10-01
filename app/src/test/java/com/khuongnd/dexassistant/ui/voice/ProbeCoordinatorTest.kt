package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.*
import org.junit.Test

class ProbeCoordinatorTest {
    @Test fun `resume schedules one probe and completion permits next probe`() {
        val coordinator = ProbeCoordinator()
        val first = coordinator.begin()!!
        assertNull(coordinator.begin())
        assertTrue(coordinator.complete(first))
        assertNotNull(coordinator.begin())
    }

    @Test fun `late result cannot overwrite paused or stopped health or a resumed probe`() {
        val coordinator = ProbeCoordinator()
        val cancelled = coordinator.begin()!!
        coordinator.cancel()
        val resumed = coordinator.begin()!!
        assertFalse(coordinator.isCurrent(cancelled))
        assertFalse(coordinator.complete(cancelled))
        assertTrue(coordinator.isCurrent(resumed))
        assertNull(coordinator.begin())
        assertTrue(coordinator.complete(resumed))
    }
}
