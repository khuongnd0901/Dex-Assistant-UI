package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.*
import org.junit.Test

class MicrophonePcmTest {
    @Test fun onlyMeasuredProviderVersionAcceptsSourceInjection() {
        val service = "com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService"
        assertTrue(MicrophonePcmCapability.supports(35, "com.google.android.tts", service, 210673049))
        assertFalse(MicrophonePcmCapability.supports(32, "com.google.android.tts", service, 210673049))
        assertFalse(MicrophonePcmCapability.supports(35, "other", service, 210673049))
        assertFalse(MicrophonePcmCapability.supports(35, "com.google.android.tts", "other", 210673049))
        assertFalse(MicrophonePcmCapability.supports(35, "com.google.android.tts", service, 210673050))
    }
    @Test fun cueAndSilenceAloneDoNotEndBeforeSpeech() {
        val endpoint = PcmSpeechEndpoint()
        val cue = ShortArray(320) { 8000 }
        for (now in 20L..400L step 20) assertFalse(endpoint.shouldEnd(cue, 320, now))
        for (now in 420L..10000L step 20) assertFalse(endpoint.shouldEnd(ShortArray(320), 320, now))
    }
    @Test fun speechGapClosesOnlyAfterBoundAndResetsOnResumedSpeech() {
        val endpoint = PcmSpeechEndpoint()
        val speech = ShortArray(320) { 4000 }
        for (now in 1000L..2000L step 20) assertFalse(endpoint.shouldEnd(speech, 320, now))
        repeat(74) { assertFalse(endpoint.shouldEnd(ShortArray(320), 320, 4000)) }
        assertFalse(endpoint.shouldEnd(speech, 320, 4000))
        repeat(74) { assertFalse(endpoint.shouldEnd(ShortArray(320), 320, 5000)) }
        assertTrue(endpoint.shouldEnd(ShortArray(320), 320, 5000))
    }
}
