package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.*
import org.junit.Test

class MicrophoneScanPolicyTest {
    private val silence = ShortArray(320)
    private val activity = ShortArray(320) { (kotlin.math.sin(it * 0.14) * 6000).toInt().toShort() }
    @Test fun silentRouteRotatesOnlyAfterDwell() {
        val policy = MicrophoneScanPolicy().also { it.changed(0) }
        assertFalse(policy.observe(silence, 320, 599, false))
        assertTrue(policy.observe(silence, 320, 600, false))
    }
    @Test fun activityNeedsConsecutiveFramesAndHoldsThroughWordGap() {
        val policy = MicrophoneScanPolicy().also { it.changed(0) }
        for (now in listOf(560L, 580L, 600L)) assertFalse(policy.observe(activity, 320, now, false))
        assertFalse(policy.observe(silence, 320, 2000, false))
        assertTrue(policy.observe(silence, 320, 2400, false))
    }
    @Test fun continuousNoiseCannotStarveOtherInputs() {
        val policy = MicrophoneScanPolicy().also { it.changed(0) }
        for (now in 20L..5000L step 20) assertFalse(policy.observe(activity, 320, now, false))
        assertTrue(policy.observe(activity, 320, 5020, false))
    }
    @Test fun variableActivityIsNotCutAtFiveSeconds() {
        val policy = MicrophoneScanPolicy().also { it.changed(0) }
        for (now in 20L..8000L step 20) {
            val gain = if (now % 1000 < 500) 1 else 2
            val varying = ShortArray(320) { (activity[it] * gain).toShort() }
            assertFalse(policy.observe(varying, 320, now, false))
        }
    }
    @Test fun dcAndSingleSpikeDoNotHoldRoute() {
        val policy = MicrophoneScanPolicy().also { it.changed(0) }
        assertTrue(policy.observe(ShortArray(320) { 10000 }, 320, 600, false))
        policy.changed(1000)
        assertFalse(policy.observe(activity, 320, 1580, false))
        assertTrue(policy.observe(silence, 320, 1600, false))
    }
    @Test fun silencedCaptureDoesNotAcquireActivityLock() {
        val policy = MicrophoneScanPolicy().also { it.changed(0) }
        assertTrue(policy.observe(activity, 320, 600, true))
    }
    @Test fun routeChangeClearsHoldAndNoise() {
        val policy = MicrophoneScanPolicy().also { it.changed(0) }
        for (now in listOf(560L, 580L, 600L)) policy.observe(activity, 320, now, false)
        policy.changed(700)
        assertTrue(policy.observe(silence, 320, 1300, false))
    }
}
