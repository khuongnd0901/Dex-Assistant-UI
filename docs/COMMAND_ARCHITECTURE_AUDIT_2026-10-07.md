> Migrated to Android on 2026-10-13 ([D-037](../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Kiểm toán kiến trúc lệnh và điều khiển DeX — 2026-10-07

Tài liệu này ghi lại kết quả đọc mã nguồn của backend Spring Boot và Android tại
`/mnt/d/dex-assistant-ui`. Đây là đánh giá kiến trúc, không phải bằng chứng rằng
các thay đổi đề xuất đã được triển khai hoặc chạy trên thiết bị. Kế hoạch thực
hiện và các cổng nghiệm thu nằm trong
[T-015 ExecPlan](../.agent/plans/active/T-015-semantic-command-architecture.md).

## Kiến trúc hiện tại

```text
Hey Hey / nút micro → VoiceAssistantService → SpeechRecognizer vi-VN
  → final text hoặc partial được gửi bằng lần bấm micro thứ hai
  → AssistantController
      ├─ ScreenCommandParser → DexAccessibilityService (lệnh số/lưới)
      ├─ CommandRouter → CommandRegistry → TypedAction ─┐
      └─ AssistantWebSocket → Spring Boot → LLM planner    │
                              → DEVICE_COMMAND → decode ─┤
                                                        ↓
                                                   ActionRouter
                                          ├─ LocalActionExecutor / DPC
                                          └─ DesktopUiAccess / Accessibility
```

- Wake word được nhận offline bằng `SherpaWakeEngine`; micro chuyển tuần tự từ
  wake capture sang Android `SpeechRecognizer`, TTS, rồi quay lại wake capture.
  Partial STT đang phục vụ phụ đề; chỉ final hoặc thao tác bấm micro lần hai
  rõ ràng mới vào đường xử lý lệnh. Chat có route riêng và không gửi device action.
- `CommandRegistry`, `LocalCommandEngine`, `ActionCatalog` và `ActionRouter` đã
  cung cấp local fast path, cấu hình mẫu câu, action có kiểu và cùng một chính
  sách thực thi cho local và remote `DEVICE_COMMAND`. Lệnh local nhận diện được
  không cần backend; lệnh không khớp hoặc nhiều vế đi backend đúng một lần.
- Backend giữ quyền lập kế hoạch AI qua WebSocket V1 có xác thực. `CommandService`,
  `SafeToolCallMapper` và `ClientPlanPolicy` kiểm tra action; Android giải mã,
  kiểm tra lại quyền và capability, rồi gửi `action.result`. Chat và Command
  tách biệt theo D-030. Không đưa Gemini trực tiếp vào Android.
- `DesktopUiAccess` chỉ đọc desktop Samsung được chọn, giữ package/window/display
  identity, bounds, path/parent/text/description/resource ID/actions, loại node
  bị che và kiểm tra lại cây trước gesture có display ID. `AppUiAdapters` đã có
  tìm text chính xác, ancestor có thể bấm và nhận diện kết quả YouTube.

Nguồn mã chính: Android
[`VoiceAssistantService.kt`](https://github.com/khuongnd0901/Dex-Assistant-UI/blob/main/app/src/main/java/com/khuongnd/dexassistant/ui/voice/VoiceAssistantService.kt),
`AssistantController.kt`, `routing/`, `execution/ActionRouter.kt`,
`screen/DesktopUiAccess.kt`, `screen/DexAccessibilityService.kt`; backend
`domain/`, `application/CommandService.java`, `adapter/out/llm/`,
`adapter/in/websocket/`. Hợp đồng chuẩn:
[PROTOCOL.md](PROTOCOL.md), [DEVICE_COMMANDS_V1.md](DEVICE_COMMANDS_V1.md),
[LOCAL_CAPABILITIES_V1.md](LOCAL_CAPABILITIES_V1.md).

## Khi nào lưới 24 ô xuất hiện

1. “Mở/hiện lưới” gọi `freshGrid()` và tạo lưới 6×4 theo yêu cầu người dùng.
   “Chia ô” chia tiếp đến độ sâu tối đa ba; lệnh bấm ô dùng tọa độ của màn hình
   DeX hiện tại và cần phiên chưa hết hạn.
2. `prepareForVoiceCommand()` gọi `prepare()`. Sau khi hợp nhất mục tiêu từ
   Accessibility và ảnh chụp, nếu danh sách rỗng thì phiên đánh số dùng cả
   desktop làm lưới. Chuẩn bị này không tự bấm.
3. Trong `ActionRouter`, chọn kết quả YouTube gọi `numberedFallback()` khi
   không tìm được kết quả, hoặc hết thời gian quan sát trong khi query còn hiện.
   Hệ thống hỏi chọn/xác nhận; không tự chọn ô.

Nhãn node/ảnh được kiểm tra lại trước khi bấm và chỉ dùng một lần. Lưới cố ý
giữ tọa độ khi nội dung/cửa sổ đổi; chỉ đổi/mất display, mất quyền, hủy hoặc
hết hạn mới vô hiệu hóa theo D-023. Lưới trực tiếp cần được giữ; fallback tự
động nên đứng sau bộ phân giải UI ngữ nghĩa và mục tiêu đánh số.

## Ý tưởng tham khảo và ánh xạ

| Ưu tiên | CURRENT | REFERENCE | PROPOSED | BENEFIT | RISK |
| --- | --- | --- | --- | --- | --- |
| P0 | Android `TypedAction`/`ActionRouter`; backend `DeviceCommand`/`ExecutionPlan` | [Jarvis Lite parser/action](https://github.com/anvinbiju-lab/Jarvis-Lite), [Aira validator/executor](https://github.com/dhineshbuilder/Aira) | Mở rộng hợp đồng target ngữ nghĩa và validation; giữ executor chung | Chính sách nhất quán | Lệch schema giữa hai repo |
| P0 | `DesktopUiAccess`, `UiNodeRecord`, `AppUiAdapters` | [FlowPilot UITreeParser](https://github.com/OdinStack/FlowPilot/blob/main/app/src/main/java/com/flowpilot/accessibility/UITreeParser.kt), [NodeMatcher](https://github.com/OdinStack/FlowPilot/blob/main/app/src/main/java/com/flowpilot/accessibility/NodeMatcher.kt) | Khớp resource ID, text/description, role/state, cha/anh em; xếp hạng ứng viên và từ chối mơ hồ | Giảm nhắc dùng lưới, tăng độ đúng | Chọn sai nếu xếp hạng lỏng |
| P0 | Gesture DeX có display ID và kiểm tra lại node | [FlowPilot ActionExecutor](https://github.com/OdinStack/FlowPilot/blob/main/app/src/main/java/com/flowpilot/accessibility/ActionExecutor.kt) | Bấm bounds hiện tại sau revalidation, giữ xác nhận và one-shot | Không có tọa độ cố định trong plan | Gesture DISPATCHED chưa chứng minh UI đổi |
| P1 | `CommandRegistry`/`LocalCommandEngine` | [Jarvis Lite RuleBasedParser](https://github.com/anvinbiju-lab/Jarvis-Lite/blob/main/app/src/main/java/com/example/parser/RuleBasedParser.kt) | Chỉ thêm handler nhỏ cho lệnh xác định nhiều bước | Dễ mở rộng | Trùng pattern/ưu tiên sai |
| P1 | Backend `ClientPlanPolicy`, Android `ActionCatalog` | [Aira AutomationValidator](https://github.com/dhineshbuilder/Aira/blob/main/app/src/main/java/com/aira/app/automation/AutomationValidator.kt) | Kiểm tra target ở cả hai phía; Android giữ quyền xác nhận và live revalidation | Từ chối plan thiếu dữ liệu rõ ràng | Phức tạp hợp đồng |
| P1 | `DexVoiceTiming`, `DexMetric`, Spring planning timer | [AVA VoiceRecognizer](https://github.com/crftsmnd/ava-android-assistant/blob/main/app/src/main/java/com/ava/ai/ai/voice/VoiceRecognizer.kt) | Ghép các mốc thành latency theo từng giai đoạn | Tìm đúng điểm nghẽn | Sai số khi trộn đồng hồ |
| P2 | Adapter YouTube hiện có | [Jarvis Lite app automation](https://github.com/anvinbiju-lab/Jarvis-Lite) | Chọn video theo tiêu đề, nghệ sĩ/ngữ cảnh và thứ tự, dưới xác nhận | Giải quyết ca YouTube cụ thể | UI YouTube đổi |
| P3 | Chưa có workflow tổng quát | [FlowPilot workflow](https://github.com/OdinStack/FlowPilot), [AVA custom commands](https://github.com/crftsmnd/ava-android-assistant) | Hoãn teach/replay; nếu triển khai, dùng action/validator chung | Tái sử dụng đường an toàn | Replay mục tiêu cũ, persistence |

Không sao chép FlowPilot nguyên trạng: screen signature nông của dự án đó không
thay thế được kiểm tra display/window/target hiện có; gesture theo kích thước
màn hình chung không bảo đảm tác động lên DeX. Không lấy backend Node.js của
Aira, Gemini trên Android của Jarvis Lite, hay thêm các AI backend của AVA.

## Quyết định thiết kế cho kế hoạch tiếp theo

- Ưu tiên thực thi bằng Android API, Intent hoặc DPC khi có đường hỗ trợ và
  kiểm tra trạng thái; sau đó mới phân giải UI ngữ nghĩa, gesture trên bounds
  hiện tại, và cuối cùng lưới theo lựa chọn của người dùng. “Dùng lệnh nhiều
  nhất có thể” cũng áp dụng cho build, fixture và ADB/CLI validation; APK chạy
  bình thường không phụ thuộc shell, ADB hoặc root.
- Device Owner DPC chỉ quản lý quyền runtime hợp lệ của app và các hành động
  DPC đã xác minh. Accessibility, assistant role, notification/special access
  vẫn theo giới hạn Android và sự cấp quyền của người dùng; không suy diễn rằng
  Device Owner có thể ép cấp mọi quyền hoặc thực thi mọi global action.
- STT đặt `vi-VN`; TTS phải chọn giọng tiếng Việt khả dụng và báo lỗi trung thực
  nếu không có. Lời xác nhận, chọn mục, lỗi và kết quả phải nói tiếng Việt. Giữ
  ưu tiên TTS tiếng Việt offline, cho phép giọng qua mạng theo D-016; không tự
  đổi sang giọng ngôn ngữ khác. Partial chỉ hiển thị hoặc phân loại không tác
  dụng phụ; lần bấm micro thứ hai vẫn là cơ chế submit rõ ràng.
- Các action tổng quát như `OPEN_URI`, `TYPE`, `BACK`, `HOME`, `RUN_WORKFLOW`
  chưa được quảng bá chỉ vì có tên trong kiến trúc mục tiêu. `WAIT_FOR_UI` là
  điều kiện chờ có giới hạn; `ASK_USER` dùng interaction hiện có. Giữ chỉ số
  kết quả YouTube bắt đầu từ 1 trong schema 1.

Ví dụ “Mở YouTube, tìm Nơi này có anh và phát kết quả đầu tiên”: backend tạo
bước tìm kiếm và target ngữ nghĩa có tiêu đề, ngữ cảnh “Sơn Tùng M-TP” và thứ
tự 1 khi có. Android quan sát kết quả YouTube, xác định duy nhất một thẻ,
đọc lời xác nhận tiếng Việt, kiểm tra lại cây rồi bấm bounds trên DeX. Nếu
không phân biệt được, hỏi chọn hoặc dùng fallback đánh số; plan không mang
tọa độ. Đường `YOUTUBE_PLAY_QUERY` hiện đã tìm, chờ và hỏi chọn, nên đây là
mở rộng có kiểm soát của chức năng sẵn có.

## Đo đạc và phần chưa chứng minh

Đo `wakeLatency` (wake detected→STT ready), `sttLatency` (STT ready→final hoặc
manual submit; kèm speech end→final), `routeLatency` (text chốt→route),
`backendLatency` (ws.send→plan received), `planningLatency` (Spring admission→
plan được kiểm tra), `uiResolutionLatency` (bắt đầu đọc cây→ứng viên/lựa chọn/
thất bại), `executionLatency` (action start→terminal); thời gian đợi người
dùng xác nhận là một chỉ số riêng. Dùng đồng hồ monotonic trong từng tiến
trình; không trừ timestamp Android và server trực tiếp. Không ghi transcript,
nội dung màn hình hoặc bí mật trong metric.

Chưa có bằng chứng thực thi semantic resolver, chưa có thử nghiệm Fold3 cho
luồng YouTube theo nghệ sĩ/ngữ cảnh, và chưa có số liệu bảy khoảng latency
đồng bộ. AVD không chứng minh hành vi Samsung DeX hoặc giọng nói tự nhiên.
