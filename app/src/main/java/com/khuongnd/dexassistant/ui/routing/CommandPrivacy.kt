package com.khuongnd.dexassistant.ui.routing

/** Local-only boundary applied before ambiguity handling or backend routing. */
object CommandPrivacy {
    private val credentials = Regex("(?:\\b(?:password|passwd|passphrase|bearer|token|secret|api[ _-]?key)\\b|mật[ -]khẩu|mat[ -]khau|mã truy cập|ma truy cap)")
    private val provisioning = Regex("(?:kết nối(?: lại)?|ket noi(?: lai)?|vào|vao|truy cập|truy cap|đăng nhập|dang nhap|tham gia|kết nối vào|ket noi vao)\\s+(?:mạng\\s+|mang\\s+)?(?:wi[ -]?fi|wifi|mạng|mang)\\b")

    fun requiresLocalHandling(text: String): Boolean {
        val normalized = VietnameseNormalizer.normalize(text)
        return credentials.containsMatchIn(normalized) || provisioning.containsMatchIn(normalized)
    }

    fun redact(text: String): String = if (requiresLocalHandling(text)) "[REDACTED]" else text
}
