> Migrated to Android on 2026-10-13 ([D-037](../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Kết quả verify Z Fold3 trên Samsung DeX — 2026-10-05

## Kết luận

Ba lỗi người dùng báo đã được sửa, cài bản mới lên Fold3 và được người dùng
xác nhận: **“Cả ba đã đúng”**.

| Luồng | Kết quả sau sửa |
| --- | --- |
| “Mở song song Google Maps và YouTube” | Hai cửa sổ cạnh nhau; kết quả `APP_PAIR_VERIFIED` được đọc thành công, không còn báo thất bại sai. |
| “Tìm địa chỉ Phà Cát Lái” | Nhận dạng thành lệnh cục bộ `SEARCH_PLACE`, gửi tới Maps trên màn hình DeX; kiểm tra thiết bị thấy truy vấn công cộng trong Maps. |
| “Mở lưới → chia ô 2 → bấm ô 8” | Test qua coordinator bấm đúng ô 8 bên trong ô 2; người dùng xác nhận thao tác trên lưới đã chia đúng. |

Đây là kết quả cho các lỗi trên, không đóng toàn bộ T-011/T-010 hoặc các gate
giọng nói, provider, tài nguyên và độ bền còn mở.

## Thiết bị và bản cài

- Thiết bị thật: Galaxy Z Fold3 `SM-F926B`, Android 15/API 35.
- Samsung DeX: 1920×1080, màn hình ứng dụng được chọn là display 2.
- App cập nhật bằng `install -r` với chữ ký gốc; hash APK đọc trên thiết bị
  khớp artifact đã ký. Không uninstall hoặc reset thiết bị.
- Device Owner, vai trò trợ lý và Accessibility đã có sẵn. Không tự động sửa
  quyền, credentials, cấu hình micro hoặc Wi-Fi.

| Artifact sau sửa | SHA-256 |
| --- | --- |
| App APK đã cài | `3b5fcb8d7fb9f077ebb4f6847489efb153aa056f9cc277b138ff98454094a967` |
| Test APK cuối | `7c7b8d91acd7495476e9348854d9cd5f7253d09dd410c23f6e1d5e18fb0ecc73` |

## Kiểm tra tự động sau sửa

| Kiểm tra | Kết quả |
| --- | --- |
| Android unit tests debug | 120 PASS, không failure/error/skip. |
| Android unit tests release | 120 PASS, không failure/error/skip. |
| App lint, debug APK và test APK | PASS; lượt build sau sửa 69 giây. |
| Test APK/lint sau sửa fixture touch | PASS, 18 giây; không thay production. |
| App-pair Maps/YouTube trên Fold3 | PASS, `APP_PAIR_VERIFIED`; hai cửa sổ 960×1080 trên display 2. |
| Tìm địa chỉ qua coordinator | PASS, `DISPATCHED`; xác nhận truy vấn Phà Cát Lái hiển thị trong Maps trên display 2. |
| Grid qua coordinator | PASS, 1 test/5,170 giây; câu có/không khoảng trắng, chuẩn bị lượt voice mới và tiếp nối liên tục đều dùng vùng con. |
| Guard lưới | PASS, 4 tests/54,102 giây; giới hạn chia ô, hủy, preparation bị hủy, target thay đổi/thay thế và hết hạn thật 35 giây. |
| Whitespace | PASS ở hai repository; backend kiểm tra có hỗ trợ CRLF. |

Tổng các ca thiết bị sau sửa: **7 ca PASS**, gồm pair, Maps, coordinator grid
và 4 ca guard. Các test coordinator dùng history riêng dùng một lần, không xóa
history của người dùng.

Tọa độ được kiểm tra trong fixture 1920×1080:

- Ô 2: `(320,0)–(640,270)`.
- Ô 8 bên trong ô 2: `(373,67)–(426,135)`, tâm `(399,5;101)`.
- Ô 8 của lưới toàn màn hình: tâm `(480;405)`; test phân biệt hai vị trí này.
- Bấm tiêu thụ session đúng một lần. “Mở lưới” mới chủ động trở về lưới toàn
  màn hình; chuẩn bị voice không gia hạn session đã chia.

## Xác nhận bằng giọng nói thật

Người dùng thử bản mới và xác nhận cả ba luồng đúng. Log voice thật ghi nhận:

- 22:16:58: pair `APP_PAIR_VERIFIED`, Maps nửa trái và YouTube nửa phải.
- Các lệnh Maps tiếp theo: `DISPATCHED`.
- 22:17:42: grid `SCREEN_CONTINUE`; 22:17:51: chia vùng, depth 1;
  22:17:59: tap vùng con `DISPATCHED`.

Log tự nhiên quan sát được parent ô 14 và child ô 8, không phải parent ô 2.
Không lưu transcript nên không suy luận câu nói chính xác từ log này. Chuỗi
parent ô 2 → child ô 8 được chứng minh riêng bằng fixture coordinator;
lượt tự nhiên chứng minh thao tác dùng vùng con hiện tại.

## Lỗi trước sửa và giới hạn còn lại

- Lượt verify trước sửa: 17 ca, **15 PASS/1 SKIP/1 FAIL**. Gemini compound lỗi
  `APP_PAIR_WINDOW_MISSING`; chưa verify lại luồng provider đó sau sửa.
- Lượt đầu kiểm tra ba ca sau sửa: 2 PASS/1 lỗi fixture ghi nhận touch. Fixture
  được sửa để nhận touch ở vùng trống, chờ `ACTION_UP` có giới hạn và giãn cách
  các trường hợp độc lập; không retry gesture hoặc bỏ assertion tọa độ.
- Nguyên nhân đầy đủ của lần bấm sai trong chuỗi nghe liên tục ban đầu chưa
  được chứng minh. Bản mới có log verb/number/depth/tọa độ để đối chiếu mà không
  xuất nội dung màn hình hoặc transcript.
- STT còn chậm: sáu phiên trước sửa đo ready→final **6,55–10,34 giây**,
  speech-end→final **3–18 ms**, final→terminal **25–71 ms**. Chưa đo thời điểm
  kết thúc âm thanh thực hoặc partial; chưa kết luận nguyên nhân micro/provider.
- Matcher Fold3 ở lượt trước sửa: p95 **0,349 ms/1.000 patterns**, đạt ≤5 ms.
  Không phải phép đo mới sau sửa.
- 100 phiên tự nhiên, độ chính xác wake trong yên tĩnh/tiếng ồn, tài nguyên,
  pin và soak 8 giờ vẫn chưa hoàn tất. Snapshot bộ nhớ/pin không đóng các gate này.
- Backend trước đó build JAR và pass 63 tests; không thay backend trong bản sửa.
- Không commit/push trong đợt sửa và lưu báo cáo này.

Samsung hỗ trợ `am instrument --no-restart`; probe và các lượt sau sửa giữ
Accessibility bind. Chạy instrumentation có restart từng làm mất bind;
không thay quyền bằng ADB để che lỗi readiness.

## Tham chiếu

- [Evidence chi tiết, các lượt thất bại và lệnh tái hiện](../.agent/evidence/T-011-fold3-dex-2026-10-05.md).
- [Task board](../.agent/TASKS.md): T-011, T-010, T-006 và T-009 vẫn `IN_PROGRESS`
  cho các acceptance rộng hơn.
- [T-011 ExecPlan](../.agent/plans/active/T-011-command-engine.md).
- [T-010 ExecPlan](../.agent/plans/active/T-010-numbered-dex-control.md).
