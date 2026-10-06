package com.khuongnd.dexassistant.ui.voice

/** Whole-utterance mode controls. Chat text never falls through to Command dispatch. */
object ChatVoiceRouting {
    enum class Route { START, END, TURN }

    fun route(text: String): Route {
        val normalized = text.trim().lowercase(java.util.Locale.ROOT)
            .trimEnd { !it.isLetterOrDigit() }
        return when (normalized) {
            "bắt đầu trò chuyện" -> Route.START
            "kết thúc trò chuyện" -> Route.END
            else -> Route.TURN
        }
    }
}
