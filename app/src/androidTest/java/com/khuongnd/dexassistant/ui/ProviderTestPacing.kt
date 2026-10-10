package com.khuongnd.dexassistant.ui

import androidx.test.platform.app.InstrumentationRegistry

/** Owner-configured live-test pacing, independent of production quota policy. */
internal class ProviderTestPacing private constructor(private val spacingMillis: Long) {
    fun delayBeforeRequest() = Thread.sleep(spacingMillis)

    companion object {
        fun fromArguments(): ProviderTestPacing {
            val raw = InstrumentationRegistry.getArguments().getString("providerTestRpm") ?: "2"
            val rpm = raw.toIntOrNull()
            require(rpm != null && rpm in 1..15) { "providerTestRpm must be an integer from 1 through 15" }
            val spacing = (60_000L + rpm - 1) / rpm + 100
            android.util.Log.i("DexDirectAcceptance", "providerTestRpm=$rpm spacingMillis=$spacing")
            return ProviderTestPacing(spacing)
        }
    }
}
