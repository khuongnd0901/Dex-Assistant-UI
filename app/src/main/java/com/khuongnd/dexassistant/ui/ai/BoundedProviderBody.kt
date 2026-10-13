package com.khuongnd.dexassistant.ui.ai

import okhttp3.ResponseBody

/** Response.use owns closing. Readers stop before accumulating an unbounded provider response. */
internal object BoundedProviderBody {
    private const val COMMAND_BYTES = 65_536L
    private const val CHAT_CHARACTERS = 65_536
    fun command(body: ResponseBody): String {
        if (body.source().request(COMMAND_BYTES + 1)) throw MobileAiException("INVALID_LLM_OUTPUT")
        return body.string()
    }
    fun chat(body: ResponseBody): String {
        // charStream preserves OkHttp's declared charset/BOM handling and the existing UTF-16 limit.
        val reader = body.charStream()
        val buffer = CharArray(4096)
        val output = StringBuilder()
        while (true) {
            val count = reader.read(buffer, 0, minOf(buffer.size, CHAT_CHARACTERS - output.length + 1))
            if (count < 0) return output.toString()
            if (count == 0) continue
            if (output.length + count > CHAT_CHARACTERS) throw MobileAiException("CHAT_INVALID_OUTPUT")
            output.append(buffer, 0, count)
        }
    }
}
