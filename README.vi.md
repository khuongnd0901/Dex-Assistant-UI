# DeX Assistant — Android

## ChatGPT Subscription (T-025)

AI Command và Chat dùng **gói ChatGPT** qua OAuth riêng của ứng dụng và Responses
endpoint công khai. Đã bỏ adapter Gemini/Groq và các provider API key cùng giao diện
cấu hình key. Key đã mã hóa trước đây được giữ nguyên, không dùng làm fallback.
Không cần backend, Codex CLI hay sao chép file đăng nhập.

Mở **More (⋮) → Settings**, chọn **Continue with ChatGPT**, tải/chọn model của tài
khoản, chạy **Thử suy luận** và **Thử function calling** (không thực thi hành động),
sau đó chủ động chọn Subscription cho Chat/AI Command. Xác minh tài khoản/model và
xác nhận hành động Android vẫn bắt buộc. Chỉ nút trong Settings mới mở trình duyệt.
Các yêu cầu dùng hạn mức chung của gói; không phải API không giới hạn.

OAuth, suy luận và lập kế hoạch đã được thử trên Fold3 trước đây. Nghiệm thu voice
tự nhiên, ngắt lời và hiệu năng vẫn **IN_PROGRESS**. Ngắt tài khoản xóa token, giữ định
danh cài đặt/đăng ký; nếu chưa xác nhận thu hồi từ xa, ngắt app trong ChatGPT Settings.
Dữ liệu và thiết lập cũ được giữ; lựa chọn Gemini cũ yêu cầu cấu hình Subscription
thay vì gọi API key. Xem [bằng chứng tiếp tục](.agent/evidence/T-025-subscription-only-2026-10-23.md)
và [ExecPlan](.agent/plans/active/T-025-chatgpt-subscription-migration.md).
Giữ [MIT notice](third-party/siwc-poc-LICENSE) của mã xác thực tham khảo.

[English](README.md) · **Tiếng Việt**

DeX Assistant là trợ lý Android **ưu tiên điều khiển bằng giọng nói**, phục vụ một người dùng trên **Samsung Galaxy Z Fold3 xuất hình qua Samsung DeX ra màn hình ngoài**. Kiến trúc hiện tại là **local-first**: lệnh đơn giản xử lý ngay trên máy; lệnh phức tạp và AI Chat gọi trực tiếp subscription của ChatGPT. **Không cần vận hành Spring Boot, WebSocket proxy riêng, Firebase hoặc EC2 để chạy Command/Chat trên Android.**

> **2026-10-13:** This Android repository is the canonical development/test workspace. Backend development is retired. Start with [AGENTS](AGENTS.md) and [current status](.agent/STATUS.md); open [Android workspace](Dex-Assistant.code-workspace). T-021 Windows simulator validation passed; actual Fold3/DeX gates remain IN_PROGRESS in T-020. No cloud resources were deleted.

## Các chức năng chính

- **Wake word offline:** sử dụng sherpa-onnx/KWS đóng gói trong APK, đánh thức bằng **“Hey Hey”**. Nhận dạng tiếng Việt sau đó dùng Android `SpeechRecognizer`; dịch vụ STT có thể cần Internet và có thể truyền âm thanh đến nhà cung cấp nhận dạng.
- **Local Command:** bộ mẫu câu tiếng Việt cấu hình sẵn giúp mở ứng dụng, điều khiển âm lượng, media, YouTube, Google Maps, màn hình DeX... Lệnh khớp local **không gọi AI API**.
- **AI Command:** khi không khớp cú pháp local hoặc cần hiểu câu lệnh phức tạp, Android gọi `ChatGptSubscriptionProvider` trực tiếp. AI chỉ được đề xuất function call trong danh sách khả năng cho phép. `DeviceCommandProtocol.decode` kiểm tra cấu trúc/tham số, sau đó `ActionRouter` thực thi theo quyền thiết bị và cơ chế xác nhận hiện có.
- **AI Chat trực tiếp:** nói **“bắt đầu trò chuyện”** để vào chế độ hội thoại, **“kết thúc trò chuyện”** để thoát. Phiên Chat nằm trong RAM Android, giữ tối đa 20 cặp lượt gần nhất, giới hạn độ dài và hết hạn sau 15 phút không hoạt động. Chat **không có quyền gọi tool điều khiển thiết bị**.
- **Điều khiển trên DeX:** kết hợp Accessibility với lưới số **6 cột × 4 hàng (24 ô)** cho những giao diện không đọc được UI node. Thiết kế thao tác trên **màn hình DeX bên ngoài**, không giả định có màn hình điện thoại để điều khiển riêng.
- **Device Owner / DPC:** module riêng `dpc/` hỗ trợ các thao tác đã giới hạn quyền theo Android, không cấp đặc quyền cho kết quả AI một cách tự động.

## Kiến trúc hiện tại

```text
Microphone → Wake word “Hey Hey” → STT tiếng Việt → Command Router
                                                      │
                   ┌──────────────────────────────────┼────────────────┐
                   │                                  │                │
                 LOCAL                        Cần suy luận AI       Từ chối
                   │                                  │
             Local Router                       Subscription provider
                   │                                  │
                   │                      ChatGPT OAuth / Responses SSE
                   │                                  │
                   │                    Parse + validate tool call
                   │                                  │
                   └──────────→ Android ActionRouter ←┘
                                        │
                           Kiểm tra quyền / xác nhận / DPC
                                        │
                                   Thực thi lệnh

“Bắt đầu trò chuyện” → Chat session Android → Subscription → Responses SSE
                                             │
                                  Chỉ gửi text + ngữ cảnh giới hạn
                                  Không gửi tool điều khiển Android
```

Trong source còn tên enum `BACKEND_REQUIRED` từ kiến trúc cũ, nhưng hiện nó chỉ có nghĩa **“lệnh cần AI xử lý”**, không phải gửi qua server. Một số class WebSocket/protocol cũ vẫn tồn tại để tham chiếu hoặc phục vụ test, **không còn nằm trên đường chạy Command/Chat chính**.

## Kết nối AI

Chỉ hỗ trợ **ChatGPT Subscription**. Model lấy từ danh mục của tài khoản đã đăng nhập;
có tên trong danh mục chưa chứng minh quyền suy luận hoặc function calling. Stream lỗi,
thiếu, sai định dạng hoặc đã hủy không thực thi action hay ghi vào ngữ cảnh Chat.
Lỗi quota/xác thực/mạng được báo rõ, không chuyển provider. Local Command không phụ
thuộc AI; nhận dạng giọng nói Android vẫn có thể cần mạng.

## Cấu trúc source

| Thư mục / file | Chức năng |
|---|---|
| `app/` | Ứng dụng Android Kotlin, Jetpack Compose, Voice, Settings, Router |
| `dpc/` | APK Device Policy Controller độc lập |
| `app/src/main/java/com/khuongnd/dexassistant/ui/ai/` | `ChatGptSubscriptionProvider`, Responses SSE, typed tools, volatile Chat |
| `app/src/main/java/com/khuongnd/dexassistant/ui/security/ChatGptSettings.kt` | Lựa chọn/model/xác minh subscription; token OAuth riêng trong `auth/` |
| `app/src/main/java/com/khuongnd/dexassistant/ui/routing/` | Local Command parser, cấu hình lệnh, privacy gate |
| `app/src/main/java/com/khuongnd/dexassistant/ui/execution/ActionRouter.kt` | Kiểm tra và thực thi thao tác Android |
| `scripts/prepare-voice-assets.py` | Chuẩn bị voice models có xác minh checksum |
| `scripts/android/check-direct-ai-fold3.sh` | Script ADB kiểm tra **chỉ đọc** trước test Fold3 |
| `.agent/STATUS.md` | Lịch sử triển khai, milestone và hạng mục còn mở |

## Yêu cầu và build

- **JDK 17**, Android SDK Platform/Build Tools **35**, Python **3.10+**.
- `minSdk 30`, `targetSdk 35`; thiết bị mục tiêu là Samsung Z Fold3 chạy Android 15.
- Build chuẩn bị trước các thư viện/mô hình wake word đã ghim phiên bản và xác minh SHA-256 vào thư mục ignored `.cache/voice`.

```bash
python3 scripts/prepare-voice-assets.py
./gradlew --no-daemon --build-cache testDebugUnitTest lint assembleDebug
```

Nếu dùng Windows, thay `./gradlew` bằng `gradlew.bat`. APK sau build:

- `app/build/outputs/apk/debug/app-debug.apk`
- `dpc/build/outputs/apk/debug/dpc-debug.apk`

[Android CI](.github/workflows/android-ci.yml) tự chạy unit tests, lint, build và lưu APK artifacts. Workflow [Device-test release](.github/workflows/device-test-release.yml) có bước kiểm tra **chứng chỉ ký tương thích với bản đã cài trên Fold3**.

**Tuyệt đối không cài đè APK debug ký bằng key khác** lên thiết bị đã cấp Device Owner. Không uninstall/clear-data, reset DPC, thay signing hoặc tự bật/tắt Accessibility chỉ để test.

## Kiểm thử thiết bị thật — đang mở

Cần thực hiện **một lượt nghiệm thu cuối cùng trên Samsung Z Fold3 + DeX thật**, gồm: xác minh signing/giữ dữ liệu, Local Command khi AI offline, AI Command qua subscription, Chat tiếng Việt liên tiếp 3 lượt, hủy/tắt Chat và phản hồi trễ, lỗi 401/429/timeout, Wake/STT/TTS, quyền Device Owner/Accessibility và p50/p95 latency. Các kết quả từ CI hoặc AVD **không được thay thế** bằng chứng trên thiết bị thật.

- [Kế hoạch T-018 — Fold3 acceptance](.agent/plans/active/T-018-direct-ai-final-fold3-acceptance.md)
- [Evidence chuyển Command và Chat khỏi backend](.agent/evidence/mobile-direct-M4-command-chat-backendless-2026-10-09.md)
- [README cũ: thiết lập và lịch sử test](docs/README_legacy_2026-10-09.md) — **tài liệu lưu trữ, chứa hướng dẫn backend không còn phù hợp**

Repo [DeX-Assistant (Java/Spring Boot)](https://github.com/khuongnd0901/DeX-Assistant) vẫn được giữ làm lịch sử source/contract và tài liệu rollback. **Không mặc định rằng EC2, EBS hay DNS đang sử dụng đã được dừng hoặc xóa.**

Development/test entry point: [.agent/STATUS.md](.agent/STATUS.md). Windows simulator selection from this root: `bash scripts/android/select-test-target.sh dex --emulator-only`. [Local test tooling](scripts/android/README.md), [T-021 evidence](.agent/evidence/T-021-windows-response-performance-2026-10-13.md) and [T-020 physical campaign](.agent/plans/active/T-020-consolidated-fold3-acceptance.md) are canonical here.

## Giấy phép

[MIT](LICENSE) © 2026 Khuong Duy Nguyen. Các thành phần và tài nguyên mô hình bên thứ ba giữ giấy phép và thông báo bản quyền riêng.
