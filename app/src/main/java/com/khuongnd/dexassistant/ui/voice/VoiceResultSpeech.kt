package com.khuongnd.dexassistant.ui.voice

/** Terminal status speech shared by the natural voice path and its regression tests. */
object VoiceResultSpeech {
    fun message(status: String): String = when (status) {
        "SUCCESS", "VERIFIED", "COMPLETED", "APP_PAIR_VERIFIED" -> "Đã thực hiện."
        "DISPATCHED" -> "Đã gửi yêu cầu."
        "CANCELLED" -> "Đã hủy."
        "ACCESSIBILITY_PERMISSION_REQUIRED" -> "Hãy bật dịch vụ Trợ năng DeX trong cài đặt."
        "SCREEN_SESSION_EXPIRED", "SCREEN_CHANGED" -> "Màn hình đã thay đổi. Hãy gọi lại."
        "INVALID_SCREEN_NUMBER", "INVALID_SCREEN_CELL", "INVALID_SCREEN_COMMAND" -> "Số hoặc lệnh chưa hợp lệ. Hãy gọi lại."
        "DIALER_OPENED" -> "Đã mở màn hình quay số, chưa gọi."
        "DIRECTIONS_OPENED" -> "Đã mở chỉ đường, chưa bắt đầu dẫn đường."
        "PLACE_ALIAS_NOT_CONFIGURED" -> "Chưa có địa chỉ nhà hoặc công ty. Hãy cấu hình địa chỉ trong cài đặt DeX."
        "CONTACTS_PERMISSION_REQUIRED" -> "Hãy cấp quyền danh bạ trong cài đặt DeX."
        "INTERACTION_TIMEOUT", "TTS_FAILED", "TTS_TIMEOUT", "COMMAND_TIMEOUT" -> "Đã hủy vì hết thời gian hoặc không đọc được lời xác nhận."
        "AMBIGUOUS_PHONE_NUMBER" -> "Số điện thoại chưa rõ. Hãy đọc lại từng chữ số."
        "NO_PENDING_INTERACTION" -> "Không có yêu cầu đang chờ xác nhận hoặc lựa chọn."
        "NOTIFICATION_ACCESS_REQUIRED" -> "Hãy bật quyền truy cập thông báo trong cài đặt DeX để tua nhạc."
        "POLICY_CHANGED" -> "Quyền gọi đã thay đổi. Đã hủy yêu cầu."
        "RECIPIENT_CHANGED" -> "Người nhận đã thay đổi. Đã hủy gọi."
        "TARGET_TOO_MANY" -> "Có quá nhiều lựa chọn. Hãy nói tên đầy đủ hoặc số điện thoại cụ thể hơn."
        "YOUTUBE_RESULTS_UNAVAILABLE" -> "Chưa đọc được kết quả tìm YouTube trên màn hình DeX. Hãy thử lại."
        "APP_CONTEXT_REQUIRED" -> "Hãy mở đúng ứng dụng trên màn hình DeX."
        "CAPABILITY_UNAVAILABLE" -> "Chức năng này chưa sẵn sàng."
        "PLANNED" -> "Đã lập kế hoạch, chưa thực hiện trên máy."
        "UNSUPPORTED", "LOCAL_ONLY_UNSUPPORTED" -> "Lệnh này chưa được hỗ trợ."
        "AI_UNAVAILABLE", "AI_TIMEOUT", "AI_QUOTA_EXHAUSTED", "SEND_FAILED" -> "Khó quá, không làm được."
        else -> "Không thực hiện được. Hãy thử lại."
    }

}
