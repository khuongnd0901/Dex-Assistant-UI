package com.khuongnd.dexassistant.ui.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatVoiceRoutingTest {
    @Test fun startAndEndAreWholeUtterances() {
        assertEquals(ChatVoiceRouting.Route.START, ChatVoiceRouting.route("  Bắt đầu trò chuyện! "))
        assertEquals(ChatVoiceRouting.Route.END, ChatVoiceRouting.route("KẾT THÚC TRÒ CHUYỆN."))
        assertEquals(ChatVoiceRouting.Route.TURN, ChatVoiceRouting.route("hãy bắt đầu trò chuyện ngay"))
        assertEquals(ChatVoiceRouting.Route.TURN, ChatVoiceRouting.route("trong truyện có câu kết thúc trò chuyện"))
    }

    @Test fun ordinaryCommandLikeChatTextRemainsChatTurn() {
        assertEquals(ChatVoiceRouting.Route.TURN, ChatVoiceRouting.route("mở YouTube"))
    }
}
