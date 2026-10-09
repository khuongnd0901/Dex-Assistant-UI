package com.khuongnd.dexassistant.ui.ai

import com.khuongnd.dexassistant.ui.security.AiProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiKeyProbeTest {
    @Test
    fun everyProviderUsesFixedTlsEndpointWithoutCredentialsInUrl() {
        val urls = AiProvider.entries.map(AiKeyProbe::modelsEndpoint)
        assertEquals(5, urls.distinct().size)
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

    @Test
    fun geminiModelListStripsPrefixAndFiltersModelsWithoutGenerateContent() {
        val body = JSONObject("""{"models":[{"name":"models/gemini-example","supportedGenerationMethods":["generateContent"]},{"name":"models/embedding-only","supportedGenerationMethods":["embedContent"]}]}""")
        assertEquals(listOf("gemini-example"), AiKeyProbe.parseModels(AiProvider.GEMINI, body))
    }

    @Test
    fun openAiCompatibleModelListsKeepAccountIds() {
        val body = JSONObject("""{"data":[{"id":"model-2"},{"id":"model-1"}]}""")
        for (provider in AiProvider.entries.filter { it != AiProvider.GEMINI }) {
            assertEquals(listOf("model-1", "model-2"), AiKeyProbe.parseModels(provider, body))
        }
    }

    @Test
    fun unimplementedPaidAdaptersStayDisabledForAutomaticRouting() {
        for (provider in listOf(AiProvider.OPENAI, AiProvider.GROK, AiProvider.DEEPSEEK)) {
            assertFalse(provider.supportsDirectCommands)
        }
        assertTrue(AiProvider.GEMINI.supportsDirectCommands)
        assertTrue(AiProvider.GROQ.supportsDirectCommands)
    }
}
