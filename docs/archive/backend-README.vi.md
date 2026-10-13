> Retired backend project document; preserved only as historical provenance. Current Android workspace authority is [AGENTS](../../AGENTS.md) and [STATUS](../../.agent/STATUS.md).

# DeX Assistant — Backend cũ và tài liệu kiến trúc

[English](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/README.md) · **Tiếng Việt**

Repository này lưu **backend Java/Spring Boot của kiến trúc cũ**, hợp đồng giao tiếp, test fixtures, công cụ kiểm thử và các kế hoạch agent của dự án **DeX Assistant**.

> **Hiện trạng ngày 09/10/2026:** Ứng dụng Android trong repo [**Dex-Assistant-UI**](https://github.com/khuongnd0901/Dex-Assistant-UI) đã có source cho **Local Command, AI Command và AI Chat gọi trực tiếp API**, không cần đi qua Spring Boot/WebSocket riêng. **Backend Java không còn nằm trong luồng thực thi Command/Chat chính của app Android**. Repo này vẫn được giữ làm nơi tham khảo kiến trúc, lịch sử schema, kiểm thử hồi quy và khả năng khôi phục source nếu cần. **Chưa nghiệm thu hoàn tất trên Samsung Z Fold3/DeX thật** và việc loại backend khỏi source không có nghĩa EC2/dịch vụ cloud đang chạy đã được tắt.

[README Android — English](https://github.com/khuongnd0901/Dex-Assistant-UI/blob/main/README.md) · [README Android — Tiếng Việt](https://github.com/khuongnd0901/Dex-Assistant-UI/blob/main/README.vi.md)

## Repo này còn chứa những gì?

Source backend trước đây bao gồm:

- **Java 25 / Spring Boot 4.0.8** và các thư viện Spring AI.
- Giao thức **JSON WebSocket có xác thực**, correlation ID, execution plan và kiểm tra typed command.
- Adapter **Gemini/OpenAI**, AI proxy có ghi nhận quota, quản lý hạn mức cục bộ/H2.
- Contract fixtures và test cho action schema, capability, Chat, quota và WebSocket.
- Tài liệu thiết kế, nhật ký quyết định, milestone, evidence và kế hoạch chuyển đổi/kiểm thử thiết bị.

Có thể tiếp tục **build và kiểm thử các thành phần cũ độc lập** để tham chiếu. Chúng **không còn là yêu cầu để cài đặt hoặc chạy Android** và ứng dụng hiện tại không tự kết nối tới backend.

## Kiến trúc trước và sau khi chuyển đổi

### Trước đây (backend được lưu trong repo này)

```text
Android → WebSocket có xác thực → Spring Boot → AI Provider
                                          ↓
                                 Typed execution plan
                                          ↓
                                    Android executor
```

### Source Android hiện tại

```text
Giọng nói/STT → Android Command Router
                     ├── Lệnh nhận diện local → Android ActionRouter
                     └── Lệnh cần AI        → Android MobileAiProxy
                                                ├── Gemini REST
                                                └── Groq API
                                                      ↓
                                           Typed action đã kiểm tra
                                                      ↓
                                               ActionRouter

AI Chat → Android DirectAiChat → Gemini/Groq (chỉ text, không có tool thiết bị)
```

Trong Android còn tên enum `BACKEND_REQUIRED` do kế thừa kiến trúc cũ. Hiện nó mang nghĩa **“lệnh cần AI diễn giải”**, không có nghĩa phải mở kết nối WebSocket tới Spring Boot. Các lớp protocol/WebSocket cũ vẫn có thể tồn tại làm tài liệu hoặc phục vụ test.

## Quản lý AI API trên Android

| Provider | Có thể cấu hình key, Enable và model mặc định | Adapter Direct Command/Chat |
|---|---|---|
| Gemini | Có | Đã triển khai trong Android |
| Groq (GroqCloud) | Có | Đã triển khai |
| OpenAI (ChatGPT API) | Có | Mới có cấu hình, chưa kích hoạt |
| Grok (xAI) | Có | Mới có cấu hình, chưa kích hoạt |
| DeepSeek | Có | Mới có cấu hình, chưa kích hoạt |

**Groq và Grok là hai nhà cung cấp khác nhau.** Người dùng tự nhập API key qua giao diện Android, khóa được lưu bằng Android Keystore. **Luồng Android trực tiếp không cần biến môi trường API key trên backend**. Free Tier phụ thuộc tài khoản và model; danh sách model từ API không đồng nghĩa được sử dụng miễn phí. Các adapter chưa hỗ trợ hoặc có nguy cơ phát sinh phí không được âm thầm gọi thay thế.

Chi tiết cấu hình, cách dùng và build phiên bản hiện tại: xem [README Android tiếng Việt](https://github.com/khuongnd0901/Dex-Assistant-UI/blob/main/README.vi.md) hoặc [English](https://github.com/khuongnd0901/Dex-Assistant-UI/blob/main/README.md).

## Build và kiểm thử backend cũ (không bắt buộc)

Yêu cầu: **JDK 25**, Maven wrapper và dependencies trong `pom.xml`.

```bash
./mvnw verify
```

Hướng dẫn cũ về chạy server, tạo device token, Docker Compose, WebSocket loopback, cấu hình quota, profile và API key được lưu tại [README trước khi migration](backend-README_legacy_2026-10-09.md). **Đây là tài liệu cho backend cũ, không phải cách thiết lập Android hiện tại.** Không commit khóa API, bearer token, keystore, mật khẩu hoặc dữ liệu cá nhân lên Git.

Tài liệu đáng chú ý:

| Tài liệu | Nội dung |
|---|---|
| [`../PROTOCOL.md`](../PROTOCOL.md) | Giao thức WebSocket và correlation cũ |
| [`../DEVICE_COMMANDS_V1.md`](../DEVICE_COMMANDS_V1.md) | Contract typed action V1 |
| [`../DEVICE_COMMANDS_V2.md`](../DEVICE_COMMANDS_V2.md) | Contract mở rộng/semantic |
| [`https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/docs/contract-fixtures`](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/docs/contract-fixtures) | Bộ dữ liệu test chung Java/Android |
| [`../../.agent/STATUS.md`](../../.agent/STATUS.md) | Lịch sử dự án, gồm cả milestone backend trước đây |
| [`../../.agent/plans/proposed/RFC-2026-10-09-mobile-managed-ai.md`](../../.agent/plans/proposed/RFC-2026-10-09-mobile-managed-ai.md) | Các quyết định chuyển sang API trực tiếp; cần đối chiếu các cập nhật mới |
| [`../../.agent/evidence/mobile-direct-M4-command-chat-backendless-2026-10-09.md`](../../.agent/evidence/mobile-direct-M4-command-chat-backendless-2026-10-09.md) | Evidence phần source đã chuyển và các gate còn mở |
| [`../../.agent/plans/active/T-018-direct-ai-final-fold3-acceptance.md`](../../.agent/plans/active/T-018-direct-ai-final-fold3-acceptance.md) | Kế hoạch nghiệm thu trên Z Fold3 thật |

## Trạng thái migration và hạ tầng

| Hạng mục | Trạng thái 09/10/2026 |
|---|---|
| Không gọi backend trong nhánh AI Command Android | **Đã chuyển source** |
| Không gọi backend trong AI Chat Android | **Đã chuyển source** |
| Android unit test, lint và build APK | **PASS** — [CI `37878690108`](https://github.com/khuongnd0901/Dex-Assistant-UI/actions/runs/37878690108), commit `3715b36` |
| Test AI inference/tool call bằng tài khoản và API key thật | **Chưa nghiệm thu** |
| Nghiệm thu một lượt cuối trên Samsung Z Fold3 + DeX | **OPEN — T-018** |
| Dừng/xóa EC2, EBS, DNS hoặc tài nguyên cloud liên quan | **Chưa thực hiện hoặc xác minh trong quá trình sửa source** |

**Không dừng hoặc xóa hạ tầng đang chạy chỉ dựa trên việc source không còn phụ thuộc backend.** Cần có kết quả test thiết bị thật và quyền phê duyệt rõ ràng trước khi thay đổi hạ tầng. Khi test phải giữ nguyên signing certificate, dữ liệu app, Device Owner, Accessibility và API key cá nhân.
