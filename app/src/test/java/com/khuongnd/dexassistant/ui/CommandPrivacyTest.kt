package com.khuongnd.dexassistant.ui

import com.khuongnd.dexassistant.ui.routing.CommandPrivacy
import com.khuongnd.dexassistant.ui.routing.LocalCommandParser
import com.khuongnd.dexassistant.ui.routing.Route
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandPrivacyTest {
    @Test fun privateCommandsNeverBecomeBackendCommandsEvenWhenAmbiguous() {
        listOf("kết nối wifi nhà", "kết nối lại mạng vừa dùng", "truy cập Wi-Fi abc, mật khẩu nhiều từ", "mật-khẩu nhiều từ", "vào mạng nhà", "truy cập Wi-Fi văn phòng",
            "đừng kết nối wifi nhà", "bật wifi và kết nối wifi nhà",
            "ket noi wifi nha", "MẬT KHẨU là nhiều từ có dấu", "password: several secret words",
            "bearer abc def", "api_key = abc def", "token abc").forEach {
            assertEquals(it, Route.UNSUPPORTED, LocalCommandParser().route(it).route)
            assertEquals(it, "[REDACTED]", CommandPrivacy.redact(it))
        }
    }

    @Test fun ordinaryCommandsRetainTheirRoutingAndHistory() {
        listOf("thời tiết hôm nay", "tìm quán cà phê gần đây", "bật wifi và mở youtube").forEach {
            assertEquals(Route.BACKEND_REQUIRED, LocalCommandParser().route(it).route)
            assertEquals(it, CommandPrivacy.redact(it))
        }
        assertEquals(Route.LOCAL, LocalCommandParser().route("bật wifi").route)
    }
}
