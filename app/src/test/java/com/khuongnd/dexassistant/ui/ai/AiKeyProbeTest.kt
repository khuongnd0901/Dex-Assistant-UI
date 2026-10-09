package com.khuongnd.dexassistant.ui.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiKeyProbeTest {
    @Test
    fun providerEndpointsUseTlsAndDoNotContainKeysOrModelNames() {
        val urls = listOf(AiKeyProbe.GEMINI_MODELS_URL, AiKeyProbe.GROQ_MODELS_URL)
        assertTrue(urls.all { it.startsWith("https://") })
        assertTrue(urls.none { it.contains("?") || it.contains("generateContent") || it.contains("chat/completions") })
    }

    @Test
    fun responseClassificationNeverLabelsErrorsAuthenticated() {
        assertEquals(ProbeStatus.AUTHENTICATED, AiKeyProbe.statusFor(200))
        assertEquals(ProbeStatus.INVALID_KEY, AiKeyProbe.statusFor(401))
        assertEquals(ProbeStatus.ACCESS_DENIED, AiKeyProbe.statusFor(403))
        assertEquals(ProbeStatus.QUOTA_LIMITED, AiKeyProbe.statusFor(429))
        for (code in listOf(400, 404, 408, 500, 502, 503)) {
            assertFalse(AiKeyProbe.statusFor(code) == ProbeStatus.AUTHENTICATED)
        }
    }
}
