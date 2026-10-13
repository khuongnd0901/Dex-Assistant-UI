> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

> Scheduling override 2026-10-13: owner requests Windows simulator tests through WSL during continuation. All remaining physical Z Fold3 execution is consolidated in [T-020](T-020-consolidated-fold3-acceptance.md), one prepared campaign. This overrides older immediate/separate device-run instructions, preserves acceptance gates and does not imply PASS.

# T-015 — Di chuyển dần sang action và UI ngữ nghĩa

> 2026-10-10 owner architecture override: IN_PROGRESS, Android semantic actions/latency retained; outstanding backend-first contract rollout/remote integration gates CANCELLED. See [T-018](T-018-direct-ai-final-fold3-acceptance.md) and [task board](../../TASKS.md). Earlier backend requirements/progress are historical and do not schedule new backend work.

## Outcome and boundaries

Triển khai theo lát cắt nhỏ từ [audit 2026-10-07](../../../docs/COMMAND_ARCHITECTURE_AUDIT_2026-10-07.md):
giữ đường local/backend cùng `TypedAction` và `ActionRouter`, thêm target UI
ngữ nghĩa trên Samsung DeX, giảm fallback lưới tự động, cải thiện khả năng mở
rộng lệnh và đo latency. Mỗi lát cắt phải có bằng chứng độc lập trước khi bắt
đầu lát cắt tiếp theo. Không thay backend Spring Boot, không đưa LLM trực tiếp
vào Android, không làm teach/replay tổng quát trong các phase bắt buộc. Không
thay thế nghiệm thu còn mở của T-003/T-006/T-009/T-010/T-011/T-014.

## Inputs and constraints

- Task: [T-015](../../TASKS.md); hợp đồng hiện hành:
  [PROTOCOL](../../../docs/PROTOCOL.md),
  [DEVICE_COMMANDS_V1](../../../docs/DEVICE_COMMANDS_V1.md),
  [LOCAL_CAPABILITIES_V1](../../../docs/LOCAL_CAPABILITIES_V1.md).
  D-010/D-011/D-016/D-023/D-027/D-028/D-030/D-031 tiếp tục có hiệu lực.
- Yêu cầu chủ dự án: dùng lệnh/API/Intent/DPC tối đa khi có đường được hỗ trợ;
  ưu tiên tự động hóa kiểm tra bằng CLI, Gradle/Maven, fixture và ADB. Ứng dụng
  runtime không phụ thuộc ADB, root hoặc shell. Device Owner đã được provision;
  chỉ dùng các quyền thực sự cấp được. Không reset máy, xóa dữ liệu/quyền,
  thay signing identity, thực hiện cuộc gọi thật hoặc thay backend endpoint để
  kiểm thử nếu không nằm trong kịch bản đã được chủ máy chỉ định.
- STT `vi-VN`; TTS chỉ phát tiếng Việt, ưu tiên tiếng Việt offline và dùng giọng
  qua mạng nếu khả dụng theo D-016. Khi không có giọng Việt, báo không khả dụng;
  không tự đổi ngôn ngữ. Lệnh/nhắc chọn/xác nhận/phản hồi đều bằng tiếng Việt.
  Partial STT không tự thực thi; lần bấm micro thứ hai là ngoại lệ rõ ràng đã
  có, với exactly-once guard.
- Giữ kiểm tra Samsung desktop duy nhất, package/window/display ID, bounds,
  overlay/obscuring, confirmation, revalidation, one-shot và trạng thái trung
  thực `DISPATCHED`/`VERIFIED`. Không upload cây UI, ảnh, danh bạ hoặc audio lên
  backend. Giữ Chat tách biệt với Command.
- Worktree hai repo đang có thay đổi khác. Chỉ chỉnh phần thuộc milestone, đọc
  diff trước/sau và không ghi đè công việc chưa commit. Backend-first cho mọi
  hợp đồng wire mới; Android cũ/schema 1 vẫn dùng được.

### Cấu trúc đích và luồng dữ liệu

```text
Android voice/VoiceAssistantService (wake → STT vi-VN → TTS tiếng Việt)
  → AssistantController → routing/CommandRouter
       ├─ routing/CommandRegistry + handler → TypedAction ─────┐
       └─ protocol/AssistantWebSocket → Spring Boot Planner      │
                                      → DEVICE_COMMAND decoder ─┤
                                                             ↓
                                     execution/ActionRouter + ActionCatalog
                                       ├─ actions/LocalActionExecutor → API/Intent/DPC
                                       └─ screen/SemanticUiResolver
                                            → screen/DesktopUiAccess
                                            → bounds gesture trên DeX
                                            → numbered targets → 24-cell grid
```

`SemanticUiResolver.kt` mới thuộc package Android `screen`; handler mới (nếu
cần) thuộc `routing`, không tạo executor thứ hai. Backend giữ `domain` cho
action/contract, `application` cho policy/plan và `adapter/out/llm` cho tool
mapping. `DexAccessibilityService` tiếp tục giữ dịch vụ, overlay và lưới.

## Decision questions and evidence

| Câu hỏi | Bằng chứng phải thu | Quy tắc chấp nhận |
| --- | --- | --- |
| Lệnh nào thực sự có đường API/Intent/DPC? | Capability hiện có, Android API chính thức, Fold3 readback | Chỉ quảng bá action khi có executor và kết quả trung thực; DPC không thay thế special access/Accessibility do người dùng cấp |
| Target UI cần thêm thuộc tính nào? | Fixture YouTube/Maps/ứng dụng thứ hai với cây node thực trên DeX | Bổ sung đúng thuộc tính thiếu; không thêm OCR, embedding hoặc upload tree |
| Resolver có phân biệt kết quả trùng tên không? | Cây thật, title/artist/context, ứng viên cạnh tranh, UI đổi trong khi chờ | Chỉ chọn ứng viên duy nhất sau revalidation; nếu hòa/mất context thì hỏi hoặc báo lỗi, không tự bấm |
| Fallback có giảm mà vẫn an toàn không? | Đếm lý do semantic success/ambiguity/numbered/grid trong thử nghiệm tự nhiên | Lưới tự động chỉ sau các mức ngữ nghĩa và đánh số; lệnh “mở lưới” trực tiếp vẫn hoạt động |
| STT/TTS Việt có hoạt động trên Fold3? | `vi-VN` recognizer/TTS capability, lời nói và lời đọc thực, log mã trạng thái | Tiếng Việt được nhận và phát; thiếu giọng/recognizer phải hiện lỗi đúng, không đổi sang tiếng Anh |
| Độ trễ nằm ở đâu? | Mốc monotonic tương quan Android và timer backend; số liệu nhiều phiên | Có đủ bảy khoảng và confirmation wait; không trộn đồng hồ khác máy hay suy diễn AVD thành Fold3 |

## Milestones

### M0 — Baseline và bộ ca nghiệm thu (LOW)

1. Chụp trạng thái source/diff và ghi fixture hiện có; lập ma trận action thực
   tế theo ưu tiên `API/Intent/DPC → semantic UI → gesture bounds → lưới do
   người dùng chọn`. Chạy kiểm tra bằng lệnh trên source hiện tại; không thay
   quyền hoặc dữ liệu của Fold3.
2. Xác nhận nguyên tắc STT/TTS Việt và baseline các lệnh YouTube, Maps, app
   launch, volume, media, grid; ghi riêng kết quả host/AVD/Fold3. Nếu Fold3
   chưa sẵn sàng, chỉ mở cổng host và giữ cổng vật lý còn mở.

### M1 — Hợp đồng action ngữ nghĩa (MEDIUM)

1. Backend công bố capability/schema 2 theo kiểu additive: action
   `CLICK_TARGET` có `target` gồm `appId` (app được cấu hình), `role` trong
   tập cho phép, `text`, `contentDescription`, `resourceId`, `contextText`,
   `ordinal` 1-based (chỉ dùng khi kết quả đã được lọc theo role/context).
   Các trường tùy chọn phải có ít nhất một thuộc tính định danh ngoài ordinal;
   giới hạn chiều dài, kiểu dữ liệu và trường thừa ở cả hai phía. Không có
   coordinates, script, bypass-confirmation hoặc tùy chọn execution policy.
2. Backend chỉ lập action 2 khi Android quảng bá schema/capability 2; Android
   schema 1 và client cũ tiếp tục nhận action cũ. Android decode cùng một
   `TypedAction`/`ActionRouter`; backend không thể xác nhận thay người dùng.
   Cập nhật protocol, fixtures và cả hai bộ contract test trước khi sử dụng
   planner thật. Chỉ quảng bá action sau khi M2 executor có mặt.
3. File dự kiến: backend `domain/`, tool mapper/policy và tài liệu protocol;
   Android `routing/ActionCatalog.kt`, `protocol/DeviceCommandProtocol.kt`,
   `execution/ActionRouter.kt`.

### M2 — Semantic UI Resolver trên DeX (HIGH)

1. Thêm `SemanticUiResolver` thuần Kotlin dựa trên snapshot bounded hiện có.
   Giới hạn app/package và cửa sổ DeX trước, sau đó lần lượt xét resource ID
   chính xác, text/contentDescription chính xác, role/state, context cha/anh
   em, rồi xếp hạng xác định. Resource ID không được thắng khi text/context
   xung đột; nhiều ứng viên ngang mức là mơ hồ và phải hỏi chọn hoặc thất bại.
2. Lấy clickable ancestor phù hợp, không vượt cửa sổ/app; chụp lại tree và
   revalidate display/window/bounds/target ngay trước gesture. Không tự retry
   sau gesture; `DISPATCHED` không được gọi là phát thành công. Thử semantic
   trước fallback đánh số, rồi 24-cell grid chỉ ở điểm fallback hiện có; lệnh
   mở lưới trực tiếp giữ nguyên.
3. Mở rộng YouTube result adapter với title, artist/context và ordinal 1-based.
   Ca “Nơi này có anh” phải tìm kiếm, chờ UI, hỏi xác nhận tiếng Việt, chọn
   đúng thẻ; trùng tên/mất metadata phải báo mơ hồ hoặc đưa lựa chọn.
4. File dự kiến: Android `screen/DesktopUiAccess.kt`, `screen/AppUiAdapters.kt`,
   `screen/SemanticUiResolver.kt` mới, `execution/ActionRouter.kt`, fixture/test.

### M3 — Handler cục bộ có cấu hình (LOW–MEDIUM)

1. Giữ `commands.json`/`CommandRegistry` và action catalog. Chỉ trích handler
   cho chuỗi lệnh xác định có nhiều bước khi có ca sử dụng chứng minh; bắt đầu
   bằng YouTube search/select. Không biến config thành script/workflow hay đưa
   logic xác nhận vào JSON.
2. Giữ whole-input match, slot tiếng Việt gốc, từ chối mơ hồ, privacy guard,
   capability check và một lần backend fallback. Đo p95 matcher hiện có và sau
   thay đổi. File dự kiến: Android `routing/`, `execution/ActionRouter.kt`,
   `assets/commands.json`, test tương ứng.

### M4 — Latency instrumentation (LOW)

1. Liên kết metric theo request/epoch: `wakeLatency` (detected→STT ready),
   `sttLatency` (ready→final/manual submit, thêm speech end→final),
   `routeLatency`, `backendLatency` (ws send→plan), `planningLatency` trên
   Spring, `uiResolutionLatency`, `executionLatency`; confirmation wait riêng.
   Chỉ dùng monotonic timestamp trong cùng tiến trình, không trừ timestamp
   Android và server. Không ghi nội dung lệnh/UI/bí mật.
2. Ghi phân bố và terminal reason cho local/remote/semantic/fallback trong
   phiên tự nhiên trên Fold3. So sánh với baseline M0 và mục tiêu SRS p95 từ
   cuối câu nói đến lúc action bắt đầu; không đổi tiêu chí sản phẩm chỉ vì
   benchmark AVD tốt. File dự kiến: Android voice/controller/WebSocket/UI access,
   backend `CommandService` metric, tài liệu vận hành.

### M5 — Teach/replay tùy chọn (HIGH; ngoài phạm vi thực thi T-015)

Chỉ tạo task/ExecPlan riêng nếu chủ dự án yêu cầu sau khi M0–M4 được nghiệm thu.
Workflow khi đó dùng cùng action/validator/confirmation/display policy;
không triển khai embedding hoặc hệ thống dạy đầy đủ trong T-015.

## Validation

- Trên backend: `./mvnw verify` bằng Java 25; contract tests cho client cũ,
  schema 1/2, field thừa/sai kiểu/giới hạn, capability thiếu, plan tối đa tám
  action và không rò screen/contact/audio. Chạy `git diff --check`.
- Trên Android: `./gradlew test lint assembleDebug assembleDebugAndroidTest`
  bằng JDK 17/SDK hiện có; fixture cho selector trùng, thay cửa sổ/display,
  node bị che, context thiếu, cancel/timeout/late callback, một gesture duy
  nhất, local không gọi backend, composite gọi backend một lần, TTS Việt và
  partial không tự thực thi. Chạy `git diff --check`.
- Dùng `scripts/android/select-test-target.sh dex`, ADB với serial do script
  chọn và instrumentation đã giới hạn tác động. Kiểm tra role/permission/DPC
  bằng readback trước; không xóa app data hay thu hồi quyền để tạo fixture.
  AVD chỉ chứng minh logic. Fold3 thật phải kiểm tra YouTube, Maps, app pair,
  semantic click trùng tên, fallback/grid, selected display, STT/TTS Việt,
  nút micro thứ hai, Chat isolation và latency phiên nói tự nhiên. Không gọi
  điện thật trong test tự động. Kết quả phải ghi PASS/FAIL/SKIP và lý do.
- Mỗi milestone cập nhật task/status, bằng chứng và tài liệu contract tương ứng.
  Chỉ đánh dấu DONE khi build, test, demo thực tế và cổng Fold3 áp dụng đã chạy;
  compilation hoặc fixture đơn lẻ không đủ.

## Risks and escalation

- YouTube/OEM thay cây Accessibility: từ chối an toàn và ghi lý do, không bấm
  ứng viên điểm thấp hoặc chuyển sang phone display. Thay đổi ngưỡng/role cần
  fixture thực, không hiệu chỉnh bằng một trường hợp duy nhất.
- App launch, DPC, SpeechRecognizer và TTS phụ thuộc thiết bị/dịch vụ; báo
  `DISPATCHED`, `VERIFIED`, `UNAVAILABLE` đúng thực tế. Device Owner không thể
  ép cấp Accessibility, assistant role hay mọi special access.
- Nếu hợp đồng mới ảnh hưởng tương thích, hoàn tất backend/fixture trước khi
  quảng bá capability từ Android. Cần chủ dự án phê duyệt nếu thay mục tiêu
  SRS, provider/cost, retention, quyền vượt phạm vi hiện có hoặc hành động có
  tác động ngoài các ca nghiệm thu đã nêu.

## Progress and findings

- [x] 2026-10-07: đọc mã nguồn và ranh giới D-010/D-016/D-023/D-027;
  [báo cáo audit](../../../docs/COMMAND_ARCHITECTURE_AUDIT_2026-10-07.md)
  lưu các chức năng đã có, khoảng trống và ý tưởng tham khảo.
- [x] T-015/ExecPlan được tạo theo yêu cầu lập kế hoạch; chưa sửa mã nguồn,
  hợp đồng runtime, permission hoặc thiết bị.
- [x] 2026-10-07 M0 source/fixture/action baseline và ma trận ca nghiệm thu:
  [bằng chứng M0](../../evidence/T-015-M0-baseline-2026-10-07.md). Catalog
  checkout hiện tại có 49 definitions/179 patterns; số 177 trong audit là
  snapshot cũ. Không có Fold3 tại ADB khi chụp M0; ca physical mới vẫn mở.
- [x] 2026-10-07 M1 hợp đồng additive schema 2, `CLICK_TARGET` validator và
  fixture chung hai repo; backend Java25 verify và Android JDK17 test/lint/APK
  PASS. Android runtime vẫn quảng bá schema 1 và chặn execution cho đến M2.
  [Bằng chứng M1](../../evidence/T-015-M1-contract-2026-10-07.md).
- [ ] M2: resolver và host tests/build đã triển khai; Fold3 semantic acceptance,
  capability schema 2 và kiểm tra lại sau khi bật vẫn mở. Xem
  [bằng chứng M2 host](../../evidence/T-015-M2-host-2026-10-07.md).
- [ ] M3: handler YouTube và host validation đã hoàn thành; Fold3 local/backend,
  fallback và regression vẫn mở. Xem [bằng chứng M3 host](../../evidence/T-015-M3-host-2026-10-07.md).
- [ ] M4: existing instrumentation validated; actual action-start forwarding,
  semantic revalidation timing and interaction epoch correlation repaired.
  Backend verify PASS77 (1 skip), Android test/lint/app+test APK PASS201 tasks;
  log-summary tests PASS4. Fold3 matching-cert APK installed in place; natural
  latency sample8 local requests captured; callback→action-start sample p95
  92.822ms, insufficient for SRS acceptance. Owner reported intermittent wake
  miss then spontaneous recovery; regression remains open. Owner explicitly
  requested this M4 follow-up while
  M2/M3 physical gates remain open. See [M4 evidence](../../evidence/T-015-M4-latency-2026-10-07.md).

## Decision and requirement updates

Khi thực thi, cập nhật `docs/PROTOCOL.md`, `docs/DEVICE_COMMANDS_V1.md`,
`docs/LOCAL_CAPABILITIES_V1.md`, `docs/SRS.md`, fixture và hướng dẫn thiết bị
theo kết quả thật. Quyết định bền vững mới đi vào `.agent/DECISIONS.md` sau
khi được chấp nhận; không coi mẫu schema/role trong plan là hợp đồng đã chạy.

## Follow-up regression — YouTube spoken result index (2026-10-07)

Owner reported “mở video số 1” rejected as INVALID_SCREEN_COMMAND. Inspection
shows the generic screen grammar reserves embedded “số N” before the typed
registry; registry also lacks this opening alias. Same T-015 outcome: repair
valid YouTube result selection through common ActionRouter, preserving scoped
foreground, spoken confirmation/live revalidation and local rejection of
malformed/out-of-context/numeric/composite commands. Acceptance: exact owner
phrase and existing select aliases route YOUTUBE_SELECT; invalid indices and
screen grammar still reject locally; focused tests/full Android checks pass;
matching-cert in-place APK and owner/device result remain required. Work in
progress; prior M4 statistics are from the installed earlier APK.

Owner additionally reported “chia ô số 4” after opening grid rejected as
INVALID_SCREEN_COMMAND. Existing subdivision grammar only accepts “chia ô 4”.
Extend optional numeric marker in exact whole-input subdivision grammar,
preserving 1–24 bounds, selected display/session guards and malformed/composite
local rejection. Include spaced/compact/spoken-number regression fixtures and
physical owner rerun on final APK.

## Owner clarification — indexes immediately after app launch (2026-10-07)

The original M2/M3 wording covers semantic search/select and numbered fallback,
not automatic video numbering immediately after APP_LAUNCH. Owner explicitly
requested this launch behavior after observing missing indexes on YouTube.
Extend the existing T-015 outcome with the first supported adapter, YouTube:
open app on selected Samsung DeX → bounded wait for accessible video cards →
show stable 1-based indexes over those cards → accept spoken index through
TypedAction/ActionRouter with confirmation and live revalidation. Keep the RAM
candidate mapping stable across voice preparation/new parent command; stale
window/display/card/list identity must reject or rebuild visibly before choosing,
never silently choose a different card. Unsupported/no-card trees get honest
feedback; no automatic grid or blind click. Other adapters require their own
verified candidate semantics. Tests must cover visible numbering and same-card
selection, mismatch/change, no-card, duplicate metadata and cancellation. Physical
YouTube acceptance remains required on final APK.

Owner also requested “mở tiếng” as an unmute alias (existing bật tiếng/bật âm
thanh supported), and reiterated “tắt YouTube”. App close belongs to the existing
T-011 close-window outcome; inspect actual selected-DeX caption control before
adding an executor. Keep it distinct from force-stop and media pause. Do not
advertise an unimplemented close capability.

Owner requested STT improvement: controlled command-only silence endpoint hints
1500ms complete/1000ms possibly complete, baseline Chat/diagnostic options,
final-only dispatch. Host build passed; effect is unproven until Fold3 comparison
because external recognizer may ignore the hints. Preserve explicit manual
second-mic submission and all wake/provider/mic policies.


### Final source publication checkpoint - 2026-10-07

Owner authorized commit/push in both repositories. Backend Java25 verify:
78 tests, zero failures/errors, 1 expected skip; focused schema2 contract4 PASS.
Android final JDK17 tests/lint/app+test APK:201 tasks PASS,154 app tests per
variant (zero failures/errors,1 expected skip),DPC5 per variant PASS. Shared
schema2 fixture byte comparison PASS; latency summary unit tests4 PASS.
Owner confirmed closing the YouTube DeX window. The additive APP_CLOSE_WINDOW
schema2 contract is strict and configured-app scoped; Android keeps it
unadvertised and CAPABILITY_UNAVAILABLE pending caption evidence and a safe
executor. This clarification supersedes the earlier pending meaning.
T-015 preview/numeric/search/unmute/STT-hint source passes host checks; latest
APK installation and physical acceptance remain unverified. Tasks stay
IN_PROGRESS; publication does not close device or latency acceptance gates.
