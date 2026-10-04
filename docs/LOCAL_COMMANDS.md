# Lệnh tiếng Việt xử lý local

Các câu dưới được nhận dạng từ bản STT cuối và xử lý trên Android, không cần
backend. Gọi “Hey hey”, hoặc dùng nút nghe khi được hỗ trợ, đợi trạng thái sẵn
sàng rồi nói. Tốc độ nhận dạng cuối vẫn phụ thuộc SpeechRecognizer và microphone.
Viết hoa, khoảng trắng, dấu câu cuối và “hãy”, “vui lòng”, “giúp tôi” được chuẩn hoá.

| Chức năng | Câu lệnh mẫu | Giới hạn |
|---|---|---|
| Mở ứng dụng | “Mở YouTube”, “Bật You Tube”, “Khởi động Chrome”, “Mở Google Chrome” | Chỉ YouTube và Chrome; cần đã cài app |
| Tăng/giảm tiếng từng bước | “Tăng âm lượng”, “Giảm âm lượng 2 bước” | 1–10 bước; mặc định 1; âm lượng media hệ thống |
| Đặt mức tiếng | “Đặt âm lượng 50 phần trăm”, “Tăng âm lượng 100%”, “Giảm âm lượng xuống 20 %”, “Chỉnh âm lượng về 40%” | Đặt **mức tuyệt đối** 0–100%; làm tròn theo mức hệ thống |
| Mức tiếng cao/thấp nhất | “Âm lượng tối đa”, “Tăng âm lượng tối đa”, “Âm lượng tối thiểu” | Đặt 100% hoặc 0% |
| Tắt/bật tiếng | “Tắt tiếng”, “Tắt âm thanh”, “Bật tiếng”, “Bật âm thanh”, “Bỏ tắt tiếng” | Điều khiển mute của media; bật tiếng không tự tăng mức 0% |
| Phát/tiếp tục | “Phát nhạc”, “Phát video”, “Tiếp tục nhạc”, “Tiếp tục video” | Gửi media key tới phiên media đang hoạt động |
| Tạm dừng/dừng | “Tạm dừng nhạc”, “Tạm dừng video”, “Tạm dừng”, “Dừng video”, “Dừng nhạc” | Tuỳ app media đáp ứng; không đóng app |
| Chuyển bài/video | “Bài tiếp theo”, “Video tiếp theo”, “Chuyển bài tiếp theo”, “Bài trước”, “Video trước”, “Quay lại bài trước” | Tuỳ phiên media đang hoạt động |
| Tìm YouTube | “Nhạc Sơn Tùng”, “Tìm nhạc Sơn Tùng”, “Tìm kiếm video mèo”, “Tìm Sơn Tùng trên YouTube” | Mở trang kết quả; giữ cả câu chuẩn hoá làm query; không tự chọn/phát video |
| Độ sáng | “Tăng độ sáng”, “Giảm độ sáng”, “Đặt độ sáng 60%”, “Điều chỉnh độ sáng về 40 phần trăm” | Cần quyền ghi cài đặt hệ thống; không bảo đảm đổi độ sáng màn hình DeX ngoài |
| Độ sáng tự động | “Bật độ sáng tự động”, “Tắt độ sáng tự động” | Cần quyền ghi cài đặt hệ thống |
| Wi-Fi | “Bật wifi”, “Mở wi-fi”, “Tắt wifi” | Phụ thuộc DPC/quyền quản lý thiết bị; không tự cấp quyền |

Mức phần trăm nhận các động từ “đặt”, “chỉnh”, “điều chỉnh”, “tăng”, “giảm”,
tuỳ chọn “lên”, “xuống”, “đến”, “về”, rồi số nguyên 0–100 kèm “%” hoặc
“phần trăm”. Ví dụ “tăng âm lượng 100%” đặt mức cuối bằng 100%, không cộng 100%.
“Tăng âm lượng thêm 10%” chưa có semantics tăng tương đối theo phần trăm và
không thực hiện local. Số ngoài giới hạn, số âm, câu phủ định và câu nhiều lệnh
không thực hiện local. Câu có thông tin credential được chặn trước khi gửi backend.

“Tắt/đóng YouTube” và “tắt/đóng Chrome” hiện trả unsupported; chưa có đóng cửa sổ
hay force-stop. Bluetooth có parser cũ nhưng executor hiện trả
`CAPABILITY_UNAVAILABLE`, nên chưa xem là chức năng sử dụng được. Các nút thích,
đăng ký kênh, tua theo giây, chọn/phát kết quả tìm kiếm chưa có lệnh local trực tiếp.
