# DeX Assistant — Android

[English](README.md) · **Tiếng Việt**

DeX Assistant là trợ lý Android **ưu tiên điều khiển bằng giọng nói**, phục vụ một người dùng trên **Samsung Galaxy Z Fold3 xuất hình qua Samsung DeX ra màn hình ngoài**. Kiến trúc hiện tại là **local-first**: lệnh đơn giản xử lý ngay trên máy; lệnh phức tạp và AI Chat gọi trực tiếp API của nhà cung cấp AI. **Không cần vận hành Spring Boot, WebSocket proxy riêng, Firebase hoặc EC2 để chạy Command/Chat trên Android.**

> **2026-10-13:** This Android repository is the canonical development/test workspace. Backend development is retired. Start with [AGENTS](AGENTS.md) and [current status](.agent/STATUS.md); open [Android workspace](Dex-Assistant.code-workspace). T-021 Windows simulator validation passed; actual Fold3/DeX gates remain IN_PROGRESS in T-020. No cloud resources were deleted.

## Các chức năng chính

- **Wake word offline:** sử dụng sherpa-onnx/KWS đóng gói trong APK, đánh thức bằng **“Hey Hey”**. Nhận dạng tiếng Việt sau đó dùng Android `SpeechRecognizer`; dịch vụ STT có thể cần Internet và có thể truyền âm thanh đến nhà cung cấp nhận dạng.
- **Local Command:** bộ mẫu câu tiếng Việt cấu hình sẵn giúp mở ứng dụng, điều khiển âm lượng, media, YouTube, Google Maps, màn hình DeX... Lệnh khớp local **không gọi AI API**.
- **AI Command:** khi không khớp cú pháp local hoặc cần hiểu câu lệnh phức tạp, Android gọi `MobileAiProxy` trực tiếp. AI chỉ được đề xuất function call trong danh sách khả năng cho phép. `DeviceCommandProtocol.decode` kiểm tra cấu trúc/tham số, sau đó `ActionRouter` thực thi theo quyền thiết bị và cơ chế xác nhận hiện có.
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
             Local Router                       MobileAiProxy
                   │                                  │
                   │                      Gemini REST / Groq API
                   │                                  │
                   │                    Parse + validate tool call
                   │                                  │
                   └──────────→ Android ActionRouter ←┘
                                        │
                           Kiểm tra quyền / xác nhận / DPC
                                        │
                                   Thực thi lệnh

“Bắt đầu trò chuyện” → Chat session Android → DirectAiChat → Gemini / Groq
                                             │
                                  Chỉ gửi text + ngữ cảnh giới hạn
                                  Không gửi tool điều khiển Android
```

Trong source còn tên enum `BACKEND_REQUIRED` từ kiến trúc cũ, nhưng hiện nó chỉ có nghĩa **“lệnh cần AI xử lý”**, không phải gửi qua server. Một số class WebSocket/protocol cũ vẫn tồn tại để tham chiếu hoặc phục vụ test, **không còn nằm trên đường chạy Command/Chat chính**.

## Cấu hình nhà cung cấp AI

| Provider | Bật/tắt, API key và model mặc định | Lấy danh sách model | AI Command trực tiếp | AI Chat trực tiếp |
|---|---|---|---|---|
| **Gemini** (Google) | Có | Có | Đã viết adapter | Đã viết adapter |
| **Groq** (GroqCloud) | Có | Có | Đã viết adapter | Đã viết adapter |
| **OpenAI** (ChatGPT API) | Có | Có | Chưa kích hoạt | Chưa kích hoạt |
| **Grok** (xAI) | Có | Có | Chưa kích hoạt | Chưa kích hoạt |
| **DeepSeek** | Có | Có | Chưa kích hoạt | Chưa kích hoạt |

**Grok của xAI khác với GroqCloud.** Một model xuất hiện ở API danh sách model **không chứng minh model miễn phí, hỗ trợ tool calling hay không phát sinh chi phí**. Hiện chỉ Gemini và Groq có adapter Command/Chat trực tiếp. Để gửi request, cần API key, Enable provider, model đã lưu và **người dùng chủ động xác nhận model thuộc Free Tier của tài khoản**. Nếu provider ưu tiên chưa có adapter, ứng dụng từ chối, không tự chuyển sang API trả phí hoặc nhà cung cấp khác.

### Thiết lập ngay trong giao diện ứng dụng

Vào **Thêm (⋮) → Cài đặt → AI Providers**:

1. Chọn một provider: **Gemini, Groq, OpenAI, Grok hoặc DeepSeek**.
2. Nhập **API key** ở ô được che ký tự → **Lưu / cập nhật**. Có thể **Kiểm tra API key** (chỉ xác thực/lấy danh sách model, không chạy AI inference) hoặc **Xóa key**.
3. Chọn **Tải model từ API**, chọn/nhập **Model ID mặc định của provider** rồi **Lưu model**.
4. Bật **Enable**, sau khi đã kiểm tra bảng giá và quota của tài khoản thì xác nhận **Free Tier** cho model đó.
5. Chọn **Đặt làm provider ưu tiên**. Muốn dùng AI Command và AI Chat ngay, chọn **Gemini hoặc Groq** cho đến khi các adapter còn lại được triển khai.

Mỗi key được nhập **khi ứng dụng đang chạy**, lưu riêng theo provider bằng **Android Keystore + AES-GCM** trên máy. Không hardcode API key trong source, `BuildConfig`, APK, Git hoặc CI. Đổi key/model **không phải build lại APK**. Do key nằm trên thiết bị của người dùng, key vẫn có thể bị lấy ra từ máy bị xâm nhập; chủ sở hữu chấp nhận rủi ro này.

**Chi phí mục tiêu:** chỉ dùng các model và tài khoản thật sự đủ điều kiện Free Tier. Không thể bảo đảm luôn 0 đồng vì điều kiện tài khoản, hạn mức, bảng giá và model có thể thay đổi. Nếu chưa cấu hình key, provider bị tắt hoặc hết quota, ứng dụng có thể trả về lỗi AI; **không tự gọi backend**. Local Command không phụ thuộc AI API, nhưng bước STT vẫn có thể cần Internet.

## Cấu trúc source

| Thư mục / file | Chức năng |
|---|---|
| `app/` | Ứng dụng Android Kotlin, Jetpack Compose, Voice, Settings, Router |
| `dpc/` | APK Device Policy Controller độc lập |
| `app/src/main/java/com/khuongnd/dexassistant/ui/ai/` | `MobileAiProxy`, `DirectAiChat`, `AiKeyProbe`, tool schemas |
| `app/src/main/java/com/khuongnd/dexassistant/ui/security/AiCredentialStore.kt` | Quản lý key/model/bật tắt từng provider |
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

Cần thực hiện **một lượt nghiệm thu cuối cùng trên Samsung Z Fold3 + DeX thật**, gồm: xác minh signing/giữ dữ liệu, Local Command khi AI offline, AI Command Gemini/Groq bằng key thật, Chat tiếng Việt liên tiếp 3 lượt, hủy/tắt Chat và phản hồi trễ, lỗi 401/429/timeout, Wake/STT/TTS, quyền Device Owner/Accessibility và p50/p95 latency. Các kết quả từ CI hoặc AVD **không được thay thế** bằng chứng trên thiết bị thật.

- [Kế hoạch T-018 — Fold3 acceptance](.agent/plans/active/T-018-direct-ai-final-fold3-acceptance.md)
- [Evidence chuyển Command và Chat khỏi backend](.agent/evidence/mobile-direct-M4-command-chat-backendless-2026-10-09.md)
- [README cũ: thiết lập và lịch sử test](docs/README_legacy_2026-10-09.md) — **tài liệu lưu trữ, chứa hướng dẫn backend không còn phù hợp**

Repo [DeX-Assistant (Java/Spring Boot)](https://github.com/khuongnd0901/DeX-Assistant) vẫn được giữ làm lịch sử source/contract và tài liệu rollback. **Không mặc định rằng EC2, EBS hay DNS đang sử dụng đã được dừng hoặc xóa.**

Development/test entry point: [.agent/STATUS.md](.agent/STATUS.md). Windows simulator selection from this root: `bash scripts/android/select-test-target.sh dex --emulator-only`. [Local test tooling](scripts/android/README.md), [T-021 evidence](.agent/evidence/T-021-windows-response-performance-2026-10-13.md) and [T-020 physical campaign](.agent/plans/active/T-020-consolidated-fold3-acceptance.md) are canonical here.
