# Lệnh local từ catalog JSON

Catalog nằm tại `app/src/main/assets/commands.json`. `CommandRegistry` kiểm tra và
compile một lần khi khởi động; engine match toàn câu STT cuối qua prefix index và
regex đã compile. Thêm alias, synonym hoặc pattern của action đã có chỉ cần sửa
catalog. Capability mới vẫn cần executor Kotlin và validation. Config không chứa
script, workflow, quyền hoặc cờ bỏ qua xác nhận.

| Nhóm | Câu mẫu | Điều kiện |
|---|---|---|
| App | “Mở YouTube”, “Mở ứng dụng Chrome”, “Chuyển sang Facebook”, “Mở lại TikTok”, “Mở YouTube Music”, “Khởi động bản đồ” | ID/package/alias lấy từ config; app cần được cài |
| DeX app pair | “Chia màn hình Google Maps và YouTube”, “Chia đôi màn hình Chrome với Spotify” | Chỉ app đã khai báo/cài đặt; yêu cầu màn hình Samsung DeX được nhận diện; báo thành công sau khi xác minh hai cửa sổ cạnh nhau |
| YouTube search | “Nhạc Sơn Tùng”, “Tìm video mèo”, “Mở YouTube tìm Sơn Tùng”, “Tìm YouTube bài Nơi này có anh” | Query là nội dung được trích, giữ viết hoa và dấu: `Sơn Tùng`, `Nơi này có anh`; không còn toàn câu làm query |
| YouTube play/select | “Phát trên YouTube Sơn Tùng”, “Chọn video số hai”, “Chọn video Tên video”, “Toàn màn hình”, “Thoát toàn màn hình” | Selection/fullscreen cần YouTube foreground và Accessibility; playback/query flow cần target xác định và xác nhận bằng giọng nói |
| Split-pane intent | “Mở nhạc Sơn Tùng”, “Đưa tôi về nhà” | “Mở nhạc” chạy flow tìm/chọn/xác nhận YouTube; “đưa tôi về {destination}” gọi tuyến Maps bằng alias địa chỉ local, bất kể pane nào đang focus |
| Media | “Phát nhạc”, “Tiếp tục phát”, “Tạm dừng”, “Dừng phát”, “Bài sau”, “Video vừa rồi” | Media session/key có thể chỉ chứng minh dispatch, không chứng minh playback |
| Seek | “Tua tới 30 giây”, “Tua lùi hai phút” | YouTube/YouTube Music foreground; 1–3600 giây và media session cho phép seek |
| Maps | “Tìm địa chỉ Phà Cát Lái”, “Tìm địa điểm quán cà phê”, “Tìm xung quanh bệnh viện”, “Chỉ đường đến 12 Nguyễn Trãi”, “Đưa tôi về nhà”, “Đưa tôi về công ty”, “Đi bộ đến Nhà hát Lớn” | Câu “đưa tôi về {destination}” đã có; nhà/công ty chỉ dùng địa chỉ đã cấu hình local |
| Phone | “Gọi cho Nguyễn Văn A”, “Gọi số không chín một hai ba bốn” | Lookup và alias local; chọn người/số rồi xác nhận ở lượt nói tiếp theo; không đoán cách đọc mơ hồ |
| Google | “Tìm Google Việt Nam”, “Tra cứu Kotlin”, “Tìm trên web Android 15” | HTTPS query encoded từ slot |
| Feed/scroll | “Cuộn xuống”, “Lướt tiếp”, “Lướt lên”, “Xem nội dung trước”, “Video tiếp theo”, “Video sau”, “Video vừa rồi” | TikTok foreground dùng feed next/previous; app khác dùng media next/previous. Scroll cần target trên desktop được xác định |
| System/screen | “Quay lại”, “Về màn hình chính”, “Mở cài đặt”, “Mở thông báo”, “Bấm Tiếp tục” | Global Back/Home/Notifications cần bảo đảm đúng DeX display; click text cần chọn target và xác nhận. Numbered/grid giữ parser riêng |
| Volume | “Tăng âm lượng”, “Giảm âm lượng 2 bước”, “Chỉnh âm lượng về 40%”, “Âm lượng tối đa”, “Tắt tiếng” | 1–10 bước hoặc mức tuyệt đối 0–100%; mute không tự tăng mức 0% |
| Brightness | “Tăng độ sáng”, “Đặt độ sáng 60 phần trăm”, “Bật độ sáng tự động” | Quyền ghi cài đặt; không suy ra độ sáng màn hình ngoài |
| Wi-Fi/Bluetooth | “Bật Wi-Fi”, “Tắt Bluetooth” | Wi-Fi phụ thuộc DPC; Bluetooth chưa có path API35 được xác minh nên capability unavailable |

“Tìm địa chỉ Phà Cát Lái” match local `SEARCH_PLACE`, giữ query `Phà Cát Lái`
và mở Maps bằng Intent có package và `launchDisplayId` của màn hình DeX được
chọn, kể cả khi YouTube đang focus trong cặp Maps/YouTube. Cũng có thể nói
“tìm địa chỉ Phà Cát Lái trong Google Maps”. `DISPATCHED` chứng minh đã gửi
Intent; kiểm tra kết quả tìm kiếm vẫn cần chạy trên phiên bản Maps thực tế.

Lưới hỗ trợ “mở lưới” → “chia ô 2” → “bấm ô 8”; STT viết sát chữ số như
“chia ô2”, “bấm ô8” cũng được nhận dạng. Ô 8 sau khi chia thuộc bên trong ô 2,
không phải ô 8 của toàn desktop. Lưới và mức chia còn hiệu lực được giữ khi
bắt đầu lượt nghe/activation mới trên cùng display và geometry; chuẩn bị lượt
nghe không gia hạn expiry 35 giây. “Mở lưới” chủ động reset về toàn desktop.
Pause/Stop, cancel, đổi display/geometry và expiry xóa session; một tap tiêu
thụ session, không cho phép lặp gesture từ mapping cũ. Numbered target vẫn
kiểm tra cửa sổ/node identity riêng. Câu ghép “chia ô2 rồi bấm ô8” bị từ chối
nguyên câu, không thực thi vế đầu.

Literal và alias hỗ trợ accent folding sau kiểm tra collision; slot text giữ NFC,
case, dấu tiếng Việt và ký tự query có ý nghĩa. Alias app phải khớp nguyên slot;
không fuzzy tên contact, nơi đến hoặc app. “Tăng âm lượng 100%” là đặt mức cuối
100%, không cộng 100%. Percent cần số nguyên 0–100 với `%` hoặc “phần trăm”.

Câu phủ định, compound chưa có named flow và câu không match duy nhất chuyển
backend AI tự động. Không thực thi riêng vế đầu. Credential/Wi-Fi provisioning
vẫn bị giữ local trước mọi backend route. “Tắt/đóng” app hiện chưa thực thi:
Android 14 trở lên chỉ cho app bên thứ ba gọi `killBackgroundProcesses` lên
chính tiến trình của mình; app-task API cũng chỉ quản lý task của ứng dụng gọi.
Force-stop qua màn hình cài đặt mạnh hơn thao tác đóng cửa sổ DeX. Đóng cửa sổ
DeX cần tìm đúng nút X của cửa sổ hiện hành trên desktop, đọc lại và xác nhận,
rồi kiểm tra lại target trước khi bấm. Cần chốt nghĩa lệnh và kiểm tra node DeX
trên Fold3 Android 15 trước khi bật; không đoán target hoặc điều khiển màn hình
điện thoại. “Bấm số abc” không được chuyển thành click text.
Partial STT chỉ preview, không query contacts, gửi backend hoặc dispatch action.

Lượt interaction chỉ có hiệu lực khi đang chờ: “chọn số hai”, “xác nhận”, “hủy”,
“đọc lựa chọn tiếp theo”, “lựa chọn trước”. Các phrase match toàn input (NFC,
case và whitespace); “hãy xác nhận”, “xác nhận rồi gọi” không phải xác nhận.
Mỗi trang đọc tối đa năm lựa chọn. Sau TTS thành công có 10 giây trả lời;
Pause/Stop, lỗi TTS, cancel hoặc timeout hủy action. Contact/UI target được kiểm
tra lại ngay trước dispatch. Call và semantic click luôn cần lượt nói xác nhận
sau, kể cả action từ backend. Android permission đặc biệt vẫn qua onboarding.

“Thu nhỏ cửa sổ”, “đổi sang ứng dụng trước/sau” và “đọc thông báo mới thành tiếng”
chưa được bật. Thu nhỏ/chuyển cửa sổ cần thao tác DeX có display scope đã xác minh;
Accessibility global actions hiện không bảo đảm không tác động màn hình điện thoại.
Đọc thông báo cần quyền Notification Access riêng, quy định mở khóa/riêng tư, TTS
giới hạn nội dung và bảo đảm văn bản không vào backend hoặc lịch sử. Không đọc tự
động khi vừa nhận thông báo.

`DEVICE_COMMAND` dùng typed action schema do Android sở hữu (xem [schema 1](DEVICE_COMMANDS_V1.md) và [schema 2](DEVICE_COMMANDS_V2.md)); backend đã ngừng phát triển; Android validate lại và
đưa qua cùng ActionRouter. Descriptor chỉ quảng cáo action/ID app có trong APK;
config không thay permission/confirmation policy. Success `DISPATCHED` chỉ cho
biết Intent/key/gesture đã gửi; chỉ báo `VERIFIED` khi có readback trạng thái.

Loader reject unknown field/action, sai primitive type/version/limit,
duplicate ID/alias sau accent folding, placeholder và pattern conflict. Reload
lỗi giữ registry immutable trước đó; bundled config được kiểm tra bằng JVM tests.
Remote config delivery chưa triển khai. Unit/build verification không thay thế
voice/app/device acceptance trên Fold3; benchmark 1.000 patterns p95 ≤5 ms và
natural spoken flows vẫn cần đo trên thiết bị.


“Gọi số ...” với cách đọc mơ hồ bị từ chối local và yêu cầu đọc lại từng chữ số;
không gửi AI để đoán số. Xác nhận/chọn ngoài pending cũng từ chối local.
Settings dùng launchDisplayId của desktop được chọn; Back/Home/Notifications
hiện unavailable vì chưa có global action bảo đảm đúng display. Desktop AOSP
1920×1080/160 dpi chỉ dùng regression/layout, không có category Samsung DeX.
Xem [hướng dẫn AVD](../scripts/android/README.md) và [evidence T-011](../.agent/evidence/T-011-host-avd-2026-10-05.md) trong repo Android này.
