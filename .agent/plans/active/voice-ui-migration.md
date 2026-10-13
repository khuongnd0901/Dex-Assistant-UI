> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Đánh giá và migration voice/UI cho DeX Assistant

**Trạng thái 2026-10-05:** đây là đề xuất kiến trúc, chưa phải kế hoạch triển
khai được chấp thuận. Chưa có phase migration nào bắt đầu; các thay đổi đã làm
thuộc acceptance hiện hành của T-006/T-010. Phần mô tả source bên dưới là snapshot
phân tích cũ; kiểm tra lại Android source trước khi dùng class/line references.
Nếu owner chấp thuận phase mới, cập nhật task/ExecPlan tương ứng trước khi làm.

**Đề xuất đúng về hướng dài hạn, nhưng chưa nên triển khai toàn bộ.** Tại thời điểm đánh giá, source đã có partial transcript, KWS model reuse, voice/screen chạy song song, dynamic numbered overlay và nhiều kiểm tra chống click stale. Đây là ghi nhận lịch sử, không thay thế việc kiểm tra source hiện tại.

Ưu tiên phù hợp nhất là **đo pipeline lời nói tự nhiên → tối ưu recognizer hiện tại → bổ sung semantic Accessibility locator**. Single AudioRecord, VAD và streaming STT là nhánh tiếp theo nếu số đo chứng minh cần thiết; OCR và Vision phục vụ tăng khả năng tìm target, không mặc nhiên giúp giảm latency.

Các lựa chọn đã chốt với bạn:

- Đo rồi tối ưu dần.
- Wake audio giữ local.
- Tự động khi lệnh và target rõ; hỏi xác nhận lại với gọi, xóa, gửi, mua và thao tác nhạy cảm tương đương.

## A. Current Architecture

### Voice thực tế

```text
AssistantVoiceInteractionService
  → kiểm tra assistant role/quyền/unlock/recovery
  → VoiceAssistantService [microphone FGS]

LISTENING
  AudioRecord do app tạo
  16 kHz, mono, PCM16, đọc mỗi 320 samples ≈ 20 ms
    → wakeWorker
    → SherpaWakeEngine.accept(raw PCM)
    → phát hiện Hey Hey

  stop + release AudioRecord trên wakeWorker
    → post sang main thread
    → startRecognition(epoch)
        ├─ chọn RecognitionService bên ngoài
        ├─ create SpeechRecognizer
        ├─ startListening(vi-VN, free-form, partial=true)
        └─ watchdog 30 giây

    đồng thời:
        DexAccessibilityService.prepare()
        → snapshot nodes + screenshot/local image detector
        → numbered targets hoặc grid

onPartialResults
  → transcript che thông tin nhạy cảm, giữ RAM
  → hiển thị UI
  → chưa parse/execute

onResults
  → consumeFinal chống duplicate
  → cancel/destroy recognizer
  → AssistantCoordinator.dispatchVoice(final text)
      ├─ ScreenCommandParser: lệnh số/grid
      ├─ privacy guard
      ├─ local routing → LocalActionExecutor/DPC
      └─ backend-required → WebSocket → Gemini/proxy → actions

terminal
  → canned TTS, watchdog 4 giây
  → trở lại wake
  → reuse KWS model, tạo stream mới và AudioRecord mới
```

Điểm handoff nằm trong `VoiceAssistantService`; recognizer lifecycle cũng thuộc service trong snapshot được phân tích. Kiểm tra lại Android source trước khi dùng vị trí dòng/class này để triển khai.

**Không tìm thấy delay cố định 500 ms trong wake → STT.** Các mốc 1.500 ms, 30 giây và 4 giây lần lượt thuộc screen preparation, STT watchdog và feedback watchdog; không thể coi chúng là thời gian chờ luôn xảy ra.

Recognizer hiện **cho phép online**, không ép on-device. `EXTRA_PREFER_OFFLINE=false` là hint; source không chứng minh recognizer thực tế luôn dùng cloud. App không có adapter streaming PCM riêng. `AssistantRecognitionService` chỉ là stub phục vụ assistant contract.

### Điều khiển màn hình thực tế

```text
Wake
  → prepare trên đúng Samsung DeX display
  → duyệt Accessibility tree có giới hạn
      visible + enabled + clickable/ACTION_CLICK
      bounds + window topology + identity signatures
  → screenshot
      detector X và đường viền hình chữ nhật
  → merge targets
      có target: đánh số tại target
      không có: grid 6×4
  → final command chọn số
  → kiểm tra mapping/display/window/identity/patch
  → dispatchGesture(center, 80 ms)
  → DISPATCHED
```

Snapshot khi viết đề xuất: source **không phụ thuộc chỉ vào node ID**; node thiếu `viewIdResourceName` vẫn có thể được đánh số. Text/contentDescription chủ yếu được hash vào identity thay vì giữ để semantic matching. Kiểm tra implementation hiện tại trước khi dựa vào nhận định này. Class liên quan: `DexAccessibilityService`.

`DISPATCHED` chứng minh gesture đã hoàn tất theo callback, chưa chứng minh ứng dụng thực hiện đúng ý định.

## B. Bottlenecks Found

| Bottleneck/gap | Evidence | Tác động dự kiến | Mức độ và độ chắc chắn |
|---|---|---|---|
| Chuyển microphone ownership | Wake recorder phải stop/release trước khi start external recognizer | Có khoảng trống capture; có thể mất đầu câu nói liền | Cao đối với UX liền mạch; đường đi xác nhận, duration chưa đo tự nhiên |
| Recreate recognizer mỗi lượt | `createSpeechRecognizer()` trong `startRecognition()`; destroy ở final/error | Có thể tăng chi phí khởi tạo/binding | Trung bình; chưa chứng minh đây là phần lớn 500 ms |
| Chờ final mới dispatch | `onPartialResults()` chỉ cập nhật UI; `onResults()` mới gọi coordinator | Endpoint và recognition finalization nằm trên đường execute | Cao; dependency xác nhận, số giây thực tế chưa đo |
| Không kiểm soát endpoint | Không VAD riêng, không silence extras, không `stopListening()` chủ động | Thời gian kết thúc phụ thuộc RecognitionService | Cao nếu benchmark xác nhận endpoint chậm |
| STT có giới hạn 30 giây toàn phiên | Watchdog tính từ lúc khởi động recognizer | Câu dài hoặc mạng chậm có thể bị cắt | Cao với command dài; giới hạn xác nhận từ code |
| Tree traversal trên main thread | `refresh()` từng node; tối đa 4.096 nodes; snapshot được gọi lại khi event/execute | Có thể làm chậm callback STT và UI dưới event storm | Trung bình; cần Perfetto để xác nhận duration/jank |
| Screenshot vẫn chạy khi đã có nodes | `prepare()` luôn thử screenshot, deadline 1.500 ms | Tăng CPU/RAM; numbered mapping chờ finish | Trung bình; không còn trực tiếp chặn STT readiness |
| Thiếu semantic locator | Target chủ yếu là bounds; parser dành family “bấm” cho numbered commands | “Bấm nút tạm dừng” bị từ chối trước locator | Cao; thiếu chức năng xác nhận |
| Không có ScreenModel cache dùng lại | Có snapshot theo session; ngoài session event không rebuild model | Có thể lặp traversal, mất cơ hội lookup nhanh | Trung bình; cần đo trước khi tăng caching |
| History từng gây latency lớn | FIFO đã chuyển IO/encryption khỏi terminal path | Bottleneck cũ đã xử lý | Đã đo và đã sửa |

Bằng chứng history: controlled final → terminal giảm từ **1.464–1.852 ms xuống 38–41 ms**. Đây là test final được inject, không phải số đo speech-end → action tự nhiên. [Evidence history](../../evidence/T-006-history-latency-2026-10-04.md).

Các số đo warm re-arm **150–175 ms** cũng là feedback-terminal → capture, không phải wake-detected → STT-ready. Không dùng chúng để kết luận vấn đề bạn mô tả đã được giải quyết.

## C. What We Should Keep

- Sherpa KWS nhận raw PCM được; app đã sở hữu AudioRecord. Đây là nền tảng phù hợp nếu cần shared capture.
- Reuse native KWS model, renew stream và worker ownership.
- Voice state machine, session epoch, duplicate-final guard, Pause/Stop và assistant-role recovery.
- Không cạnh tranh audio focus với external recognizer; feedback có watchdog.
- Local-first routing và xử lý nguyên câu nhiều vế trước execution.
- Privacy guard, transcript RAM, encrypted-history FIFO.
- Screen preparation song song với STT.
- Dynamic numbered overlay, recursive grid và đúng DeX display.
- Kiểm tra stale mapping, identity, geometry, foreground occlusion và image patch.
- Phân biệt `DISPATCHED`/`VERIFIED`; không tự replay sau disconnect.

Đặc biệt, **“dừng nhạc” đã có đường media key local**. Không nên bắt lệnh này phải scan màn hình để tìm nút Stop. “Bấm nút tạm dừng” mới là yêu cầu tương tác UI cần semantic locator.

## D. Recommended Target Architecture

### Kiến trúc ưu tiên trước mắt

```text
VoiceAssistantService
  → SystemSpeechRecognizerEngine
      partial → local PREPARE, không side effect
      final   → privacy + whole-command routing
                    ├─ native API/media/DPC
                    ├─ Semantic UI Locator
                    └─ backend planner cho request phức tạp

Trong lúc user nói:
  wake-triggered ScreenModel refresh
    → Accessibility metadata
    → cập nhật có debounce trong active turn

Semantic UI Locator
  → Accessibility exact/synonym/context match
  → target duy nhất, đủ bằng chứng: execution policy
  → nhiều candidate: numbered overlay để chọn
  → thiếu semantic: visual fallback khi được bật
  → không tìm được: grid cuối cùng

Execution policy
  → thao tác nhạy cảm: confirmation gắn vào action/target
  → revalidate target
  → ACTION_CLICK hoặc coordinate gesture
  → quan sát kết quả khi có tín hiệu đáng tin
```

Không cần OCR/Vision để bắt đầu đạt semantic click trên các app expose text/contentDescription.

### Nhánh audio nếu KPI hiện tại chưa đạt

```text
Một AudioCaptureSession
  → PCM frames có sample offset
      ├─ Sherpa KWS lúc WAIT_WAKE
      └─ ring buffer RAM

Wake detected
  → xác định command boundary
  → command-only buffered suffix + live PCM
      ├─ Local VAD
      └─ PcmSpeechToTextEngine
          partial → PREPARE
          endpoint → finish input
          final → COMMIT
```

**Single AudioRecord không bắt buộc đồng nghĩa với cloud STT mới.** API 33 có `EXTRA_AUDIO_SOURCE` để truyền nguồn audio đã mở cho recognizer hệ thống. Tuy nhiên, recognizer có thể không hỗ trợ và tự mở microphone; vì vậy phải thử trên đúng provider/Fold3, không chỉ kiểm tra API level. Segmented session cũng phụ thuộc implementation. [RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent).

Abstraction nên tách rõ:

- `SpeechToTextEngine`: session, partial/final/error, finish/cancel và capability.
- System engine hiện tại tự capture.
- PCM engine nhận audio từ app; chỉ chọn khi capability thực sự đạt.

### Ring buffer và VAD

- Prototype ring **800 ms**, có thể benchmark thêm 500 ms.
- 16 kHz × 2 bytes × 0,8 giây = **25.600 bytes PCM**, chưa gồm queue/model/bitmap.
- Một capture worker ghi ring; gửi frames qua queue có giới hạn. Network không được block capture.
- Pause/Stop/cancel xóa buffer, đóng session; không ghi audio xuống disk.
- Không gửi toàn bộ pre-roll vì có thể chứa Hey Hey.

KWS wrapper hiện chỉ trả Boolean. API Sherpa đã pin có token timestamps, nhưng chúng không tự chứng minh boundary kết thúc wake phrase chính xác. Cần mở rộng kết quả và kiểm chứng boundary trước khi gửi buffered suffix. [Sherpa v1.13.8](https://raw.githubusercontent.com/k2-fsa/sherpa-onnx/v1.13.8/sherpa-onnx/kotlin-api/KeywordSpotter.kt).

**Có trade-off chưa thể bỏ qua:** giữ wake audio local tuyệt đối và giữ trọn command nói liền cần tách được boundary đáng tin. Nếu prototype chưa đạt điều đó, giữ cue/handoff hiện tại; không upload cả ring để “sửa” mất đầu câu.

VAD chỉ hoạt động trong command session. Silence 300–700 ms là dải thử nghiệm, không phải default cho mọi câu. Câu dài/ngắt tự nhiên phải được dùng để chọn endpoint; stable partial không chứng minh người nói đã kết thúc.

## E. Migration Plan

### Phase 0 — Đo pipeline hiện tại, không đổi behavior

Bổ sung các khoảng còn thiếu: stop/release recorder, main-queue wait, component lookup, recognizer create/start, first partial, parser, target resolve và gesture.

Chạy benchmark lời nói thật và tách local command khỏi backend command.

**Deliverable:** phân bố p50/p95, lỗi mất đầu câu, endpoint truncation, CPU/RAM và timeline từng turn. Chỉ chuyển phase với baseline hợp lệ.

### Phase 1 — Tối ưu system recognizer có kiểm chứng

- Tách lifecycle recognizer khỏi orchestration service.
- Thử giữ recognizer giữa các phiên **đã kết thúc bình thường**; lỗi/cancel/timeout thì destroy và recreate.
- Không chạy hai phiên cùng lúc; vẫn xử lý trên main thread và chặn late callbacks.
- Dùng partial cho parser preview/local metadata preparation; final luôn parse lại toàn câu.
- Không resolve destination qua network, gửi partial lên backend hoặc execute từ partial.
- Thêm giới hạn phiên cấu hình được; thử 60 giây cho tập câu dài, vẫn giữ Stop/timeout rõ ràng.
- Silence extras chỉ là experiment riêng, không bật mặc định.

Lifecycle reuse không khắc phục khoảng trống microphone. Nó chỉ được giữ nếu số đo cho thấy cải thiện và không tăng `STT_BUSY`, lỗi hoặc callback sai phiên. Android yêu cầu chờ terminal result/error trước khi bắt đầu lượt tiếp theo sau `stopListening()`. [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer).

**Gate:** giảm latency quan sát được mà accuracy/entity retention không suy giảm.

### Phase 2 — Semantic Accessibility locator + cache giới hạn

- Giữ metadata text/contentDescription/class/bounds/action/path/context trong RAM.
- Thu thập cả label ở node con không clickable; liên kết với actionable ancestor.
- Bổ sung grammar riêng cho “bấm nút …”; không nới parser numbered bằng substring matching.
- Ưu tiên exact label và synonym dictionary có kiểm thử; contextual matching chỉ dùng dữ liệu đủ phân biệt.
- Snapshot tại wake; event khi idle chỉ đánh dấu dirty. Active turn debounce khởi điểm 150 ms, một refresh đang chạy và một refresh pending.
- Matching chạy trên immutable metadata; không chuyển live NodeInfo sang background để giữ lâu.
- Cache hỗ trợ preparation; trước click vẫn lấy trạng thái mới để revalidate.
- Reuse overlay hiện tại cho candidate selection; giữ mapping bất biến trong một phiên lựa chọn.

**Gate:** target không có ID vẫn resolve được; ambiguity và stale target không gây click.

### Phase 3 — Prototype single capture, chưa thay production

Chỉ mở khi Phase 0–1 còn cho thấy mất đầu command hoặc latency vượt KPI.

- Thử PCM pipe qua `EXTRA_AUDIO_SOURCE` trên API 33+.
- Xác nhận recognizer thực sự dùng pipe, không mở capture cạnh tranh.
- Thử ring buffer, boundary trimming và VAD.
- API 30–32 giữ system handoff hiện tại.
- Nếu pipe không đạt, kết thúc prototype bằng báo cáo; cloud riêng cần một quyết định provider/chi phí/privacy/transport tiếp theo.

Không đưa audio vào WebSocket V1 hiện tại. Contract hiện chỉ phục vụ final text/plans/results; custom cloud STT sẽ cần đường truyền riêng.

### Phase 4 — OCR fallback theo nhu cầu

Chỉ bổ sung sau khi tập test chứng minh thiếu Accessibility semantics ở những màn hình quan trọng.

- Dùng ML Kit Latin bundled, offline ngay từ lần đầu.
- Capture khi semantic path thất bại hoặc cần clarification.
- Giữ text/bounds trong RAM; cache theo screen revision, không OCR mỗi content event.
- Không dùng ảnh downsample 640×400 mặc định của detector hiện tại cho OCR chữ nhỏ.
- OCR tìm được chữ chưa đồng nghĩa tìm được button: cần context/actionable region; không đủ bằng chứng thì numbered confirmation.

ML Kit liệt kê tiếng Việt trong nhóm được hỗ trợ; bundled Latin model tăng khoảng 4 MB mỗi architecture theo tài liệu, cần đo APK thực tế. [Languages](https://developers.google.com/ml-kit/vision/text-recognition/v2/languages), [Android setup](https://developers.google.com/ml-kit/vision/text-recognition/v2/android).

API 34 có `takeScreenshotOfWindow()` giúp capture nội dung window dưới accessibility overlay; cần kiểm chứng transform từ window bitmap sang tọa độ DeX. Các API cũ tiếp tục đường capture/display và detach overlay đã có. [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService).

### Phase 5 — Visual icons; Vision là milestone tùy chọn

- Mở rộng local detector theo những icon thực sự thiếu trong benchmark; không triển khai classifier mọi icon ngay.
- Icon detector trả candidate, không tự coi prediction là xác nhận target.
- Vision chỉ bật sau quyết định riêng về upload screenshot, provider và ngân sách.
- Output Vision là candidate gắn screen revision; phải qua local revalidation và execution policy.
- Giữ provider interface độc lập. Không dùng model confidence kiểu `0.92` làm bằng chứng đủ để click.

## F. File/Class Impact

| Phase | Existing components cần chỉnh | Components mới | Remove/deprecate |
|---|---|---|---|
| 0 | `VoiceAssistantService`, `AssistantCoordinator`, `AssistantWebSocket`, `DexAccessibilityService` | `VoiceTurnTrace`, tổng hợp benchmark | Không |
| 1 | `VoiceAssistantService`, `CommandRecognizer`, session guard | `SpeechToTextEngine`, `SystemSpeechRecognizerEngine`, `CommandPreview` | Chuyển trách nhiệm lifecycle khỏi service; chưa xóa system STT |
| 2 | `DexAccessibilityService`, `NumberedScreen`, screen parser/coordinator | `UiTarget`, `ScreenModel`, `SemanticUiLocator`, `UiExecutionPolicy`, `PendingConfirmation` | Không xóa numbered/grid |
| 3 | Wake engine/service, STT adapter | `AudioCaptureSession`, `PcmRingBuffer`, `PcmSpeechToTextEngine`, `EndpointController` | Handoff cũ vẫn là fallback |
| 4 | Screenshot preparation và target merge | `OcrTargetSource` | Không |
| 5 | `ScreenImageDetector`, locator orchestration | Icon source; interface Vision khi milestone được chốt | Không |

Không thêm public wire contract ở Phase 0–2. Khi triển khai, cập nhật acceptance/ExecPlan của T-006/T-010 và phần phối hợp T-009; chưa tạo hoặc sửa durable artifacts trong review này.

## G. Trade-offs

| Hướng | Lợi ích | Chi phí/risk |
|---|---|---|
| Recognizer lifecycle | Có thể giảm warm-start, thay đổi tương đối nhỏ | Binding/provider behavior chưa biết; tăng yêu cầu xử lý stale callbacks |
| Partial PREPARE | Overlap một phần công việc, giữ accuracy final | Không giảm endpoint; có thể không đem lợi ích lớn khi local route vốn nhanh |
| Semantic Accessibility | Nhanh/offline, tăng UX rõ rệt, không thêm model lớn | Label mơ hồ, nested nodes, nhiều nút giống nhau |
| ScreenModel cache | Giảm traversal lặp, chuẩn bị khi user nói | Staleness, event storm, metadata có thể nhạy cảm |
| Single capture + ring | Giảm capture gap, giữ audio trong lúc STT khởi tạo | Refactor lifecycle/focus, backpressure và wake-boundary privacy |
| Local VAD | Kiểm soát endpoint khi app có PCM | False endpoint trong pause/noise; không quyết định được hoàn chỉnh ngữ nghĩa |
| Streaming cloud STT | Có thể overlap recognition, phù hợp câu dài nếu model đạt | Mạng, tiền, audio privacy, quota, server latency; accuracy phải đo |
| OCR | Đọc được text thiếu nodes, offline | Capture/RAM/APK tăng; OCR box không phải button identity |
| Icon/Vision | Mở rộng coverage | Theme variation, false target; Vision thêm latency/privacy/cost |

Không duy trì STT/OCR/Vision 24/7. Ngoài active command, chỉ KWS và capture hiện tại hoạt động; screen events chỉ đánh dấu dirty. Single capture tương lai cũng phải ngừng hoặc chuyển trạng thái an toàn khi feedback/interrupt, tránh thu TTS làm command mới.

## H. Benchmark Plan

### Các timestamp

Tận dụng `DexVoiceTiming`, `DexMetric`, `DexScreen`, thống nhất monotonic clock và liên kết `epoch → requestId → actionId`.

| Mốc | Định nghĩa |
|---|---|
| T0 | Wake onset theo stimulus tham chiếu; không giả vờ app hiện đã biết |
| T1 | KWS detected |
| T2 | Command audio onset theo stimulus tham chiếu |
| T3 | First partial |
| T4 | Speech end: tách acoustic reference và provider/VAD decision |
| T5 | Final transcript |
| T6 | Final intent resolved |
| T7 | UI target resolved |
| T8 | Actual action/gesture dispatch |
| T9 | Action observed, nếu có tín hiệu kiểm chứng |

Thêm recorder stop/release, recognizer start/ready và capture-active.

**T2 có thể xảy ra trước T1 khi user nói liền.** `onBeginningOfSpeech()` không thay thế onset acoustic chuẩn; gesture `onCompleted()` không thay thế T9.

Không log transcript/audio/screenshot. Với benchmark dùng fixture hợp lệ, transcript ground truth và accuracy được đánh giá riêng; log runtime chỉ có duration/status/count.

### Voice cases

- Hey Hey riêng; Hey Hey + command không nghỉ.
- Khoảng nghỉ 0/100/300/500 ms sau wake.
- Command local ngắn, nhiều entity, nhiều vế.
- Câu dài 15–45 giây, ngắt tự nhiên 300–1.500 ms.
- Đổi ý/phủ định ở cuối câu.
- Quiet, tiếng xe/motor/radio; cue có thể lọt vào recognizer.
- Offline, mạng yếu, disconnect trong phiên.
- Lặp turn, Pause/Stop, timeout và late callbacks.

“Mở Maps” có thể dùng đo STT/entity; hiện alias local chỉ có YouTube/Chrome. Câu tìm quán rồi chỉ đường còn cần năng lực search/navigation, không thể tính là action thành công chỉ vì STT đúng.

### UI cases

Text button, accessible icon, không ID, label ở child, hai nút cùng tên, scroll, popup che target, thay node cùng bounds, screen đổi trước click, overlay có mặt, screenshot failure, canvas text và icon-only.

Sensitive confirmation test dùng fixture gọi/xóa/gửi/mua, không giao dịch thật.

### KPI khởi điểm

Đây là **mục tiêu nghiệm thu**, chưa phải performance đã đo:

| Metric | Mục tiêu |
|---|---|
| Wake-detected → STT-ready, warm system path | p95 ≤ 300 ms |
| Speech-end → dispatch, local short command | p50 ≤ 600 ms; p95 ≤ 1.000 ms |
| Final → dispatch, local Accessibility path | p95 ≤ 200 ms |
| Accessibility resolve + revalidation | p95 ≤ 150 ms |
| Accuracy intent/entity | Không giảm so baseline trên cùng tập |
| Mất đầu command nói liền | ≤ 1% trên tập nghiệm thu đủ mẫu |
| Cắt sai câu dài/natural pause | ≤ 1% trên tập nghiệm thu đủ mẫu |
| Click stale/duplicate; execute từ partial | 0 trong toàn bộ regression |
| Sensitive action thiếu confirmation | 0 |
| Idle CPU/battery Phase 0–2 | Không tăng quá 10% tương đối so baseline |
| Retained RAM tăng Phase 0–2 | ≤ 10 MB; đo peak screenshot riêng |

Tách timing dispatch khỏi timing observed. Backend/LLM và Vision có báo cáo riêng, không gộp vào KPI local ≤1 giây.

Baseline tối thiểu 100 lượt nói với nhóm short/long/pause/noise được báo cáo riêng. Những KPI lỗi 1% cần tập mở rộng phù hợp; 100 lượt tổng hoặc một soak không đủ chứng minh độ tin cậy chung. A/B giữ nguyên device, route, model, network condition và signing/data; không bỏ failed runs khỏi thống kê.

## I. Risks / Unknowns

- Chưa có trace natural speech đủ để xác định nguyên nhân chính của 500 ms và vài giây sau khi nói.
- Không biết internal streaming/endpoint của RecognitionService đang chọn.
- Chưa biết PCM injection được provider Fold3 hỗ trợ thực sự.
- Chưa có wake-end boundary đáng tin để trim pre-roll mà không cắt command.
- Chưa đo traversal/event storm/capture-copy trên main thread bằng profiler.
- Semantic confidence chưa có calibration; nên dùng evidence tiers, không invent score.
- Chưa có severity/confirmation flow tái sử dụng.
- Cache TTL không bảo đảm freshness; phải revalidate ngay trước execution.
- `ACTION_CLICK=false` có thể fallback gesture sau kiểm tra lại; `true` mà chưa quan sát được kết quả **không tự tap thêm**, tránh double execution.
- Command conversational cần conversation/planning capability ngoài STT. Backend hiện chưa thực thi đầy đủ mọi tool; Android remote executor chủ yếu launch-app và Wi-Fi.
- Vision/upload screenshot không nằm trong lựa chọn privacy đã chốt; vẫn là bước quyết định riêng.

Confirmation sẽ gắn vào action, target và screen revision, hết hạn sau 10 giây; thay đổi target/cancel/timeout hủy confirmation. Chỉ final confirmation hợp lệ được commit; không coi “đồng ý” trong một câu khác là xác nhận. Khi target mơ hồ, dùng numbered candidates thay vì đoán.

## J. Recommended First Change

**Bổ sung telemetry còn thiếu và chạy baseline lời nói tự nhiên trên Fold3.**

- **Benefit:** phân biệt capture gap, recognizer startup, endpoint, finalization, main-thread stall và backend latency; chọn đúng thay đổi có hiệu quả lớn nhất.
- **Cost:** nhỏ hơn audio refactor; tận dụng phần lớn logging hiện có.
- **Risk:** thấp vì không đổi recognizer, model, threshold hay execution policy.
- **Verify:** metadata không chứa nội dung nhạy cảm; trace đủ mốc/correlation, regressions giữ nguyên behavior, benchmark có cả câu ngắn và dài.

Sau baseline, ưu tiên recognizer lifecycle nếu startup chiếm đáng kể; ưu tiên endpoint/PCM prototype nếu mất đầu câu hoặc speech-end → final là phần lớn. Semantic Accessibility locator là cải tiến chức năng kế tiếp có giá trị rõ nhất.

**Chưa có cơ sở để chuyển thẳng toàn bộ pipeline sang cloud STT, OCR và Vision.** Migration được quyết định theo bằng chứng từng phase, giữ các component đang tốt và không đổi accuracy/safety lấy một con số latency đẹp.
