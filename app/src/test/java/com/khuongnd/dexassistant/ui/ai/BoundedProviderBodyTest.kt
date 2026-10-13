package com.khuongnd.dexassistant.ui.ai

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.Assert.*
import org.junit.Test

class BoundedProviderBodyTest {
    private fun failure(code: String, work: () -> Unit) {
        try { work(); fail("Expected bounded response rejection") }
        catch (error: MobileAiException) { assertEquals(code, error.code) }
    }
    @Test fun chatRetainsUtf16CharacterLimitForVietnameseSupplementaryAndBomContent() {
        val vietnamese = "ắ".repeat(65_536)
        vietnamese.toResponseBody().use { assertEquals(vietnamese, BoundedProviderBody.chat(it)) }
        val supplementary = "😀".repeat(32_768)
        supplementary.toResponseBody().use { assertEquals(supplementary, BoundedProviderBody.chat(it)) }
        val utf16 = vietnamese.toByteArray(Charsets.UTF_16)
        utf16.toResponseBody("application/json; charset=utf-16".toMediaType()).use {
            assertEquals(vietnamese, BoundedProviderBody.chat(it))
        }
        (vietnamese + "x").toResponseBody().use { body ->
            failure("CHAT_INVALID_OUTPUT") { BoundedProviderBody.chat(body) }
        }
    }
    @Test fun commandAcceptsBoundaryAndRejectsAnExtraByteInsteadOfParsingTruncatedPrefix() {
        " ".repeat(65_536).toResponseBody().use { assertEquals(65_536, BoundedProviderBody.command(it).length) }
        ("{}" + " ".repeat(65_535)).toResponseBody().use { body ->
            failure("INVALID_LLM_OUTPUT") { BoundedProviderBody.command(body) }
        }
    }
    @Test fun unknownLengthOversizedBodiesStopReadingAndResponseOwnerClosesOnce() {
        listOf("CHAT_INVALID_OUTPUT", "INVALID_LLM_OUTPUT").forEach { code ->
            var consumed = 0L
            var closed = 0
            val source = object : Source {
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val count = minOf(byteCount, 1024L)
                    sink.writeUtf8("x".repeat(count.toInt())); consumed += count
                    return count
                }
                override fun timeout() = Timeout.NONE
                override fun close() { closed++ }
            }.buffer()
            val body = object : ResponseBody() {
                override fun contentLength() = -1L
                override fun contentType(): MediaType? = null
                override fun source(): BufferedSource = source
            }
            body.use {
                failure(code) {
                    if (code.startsWith("CHAT")) BoundedProviderBody.chat(it) else BoundedProviderBody.command(it)
                }
                assertTrue("Cannot consume an unbounded body", consumed < 80_000)
                assertEquals(0, closed)
            }
            assertEquals(1, closed)
        }
    }
}
