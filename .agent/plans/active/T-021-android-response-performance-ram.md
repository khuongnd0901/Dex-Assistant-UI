> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-021 — Tối ưu phản hồi lệnh và RAM Android

Ngày tạo: 2026-10-13. Trạng thái: **IN_PROGRESS**; owner cho phép triển khai
theo thứ tự ưu tiên, kiểm thử Windows simulator trước; Fold3 để T-020 sau.

## Outcome and boundaries

Ưu tiên theo thứ tự: (1) giảm thời gian từ khi người dùng nói xong đến khi
action bắt đầu hoặc nghe thấy câu trả lời; (2) phản hồi trạng thái và sẵn sàng
nhận lệnh tiếp theo nhanh; (3) giảm RAM, cấp phát và CPU nền mà không làm chậm
hai mục tiêu đầu. Báo nhận lệnh nhanh không được thay thế kết quả thực thi.

Phạm vi: Android voice/STT/TTS, coordinator/router, direct Gemini transport,
Accessibility/screenshot/overlay, history/UI và lifecycle của tài nguyên.
Repository này giữ task/plan và công cụ tổng hợp; Android implementation ở
`/mnt/d/dex-assistant-ui`. Main agent phối hợp phép đo; Android agent sở hữu
thay đổi app theo ranh giới hiện có.

Không đưa backend/WebSocket trở lại runtime; không khôi phục T-008/T-019;
không đổi production STT sang Sherpa, model/provider, quota, chính sách giữ
lịch sử/Chat, xác nhận hoặc mic routing trong task này. Streaming Chat và đổi
kiến trúc mic/TTS chỉ là hướng nghiên cứu nếu đo đạc chứng minh cần thiết;
khi đó cập nhật task liên quan trước khi mở rộng implementation.

## Inputs and constraints

- [Task board](../../TASKS.md), [status](../../STATUS.md), [BRD](../../../docs/BRD.md),
  [SRS](../../../docs/SRS.md), [delivery plan](../../../docs/PLAN.md) và
  [decisions](../../DECISIONS.md): D-024 giữ wake model theo service lifetime,
  D-025 history IO ngoài đường hoàn tất lệnh, D-030 Chat chỉ trong RAM,
  D-036 Android-only. Các đoạn SRS/backend cũ bị D-036 thay thế không phải
  runtime prerequisite; giữ mục tiêu latency Android còn áp dụng.
- [Phép đo T-015](../../../docs/T015_LATENCY_MEASUREMENT.md) và
  [evidence hiện tại](../../evidence/Android-simulator-continuation-2026-10-13.md)
  là điểm xuất phát, không phải baseline hoàn chỉnh của T-021.
- Production Google STT `vi-VN`; Gemini là provider nghiệm thu hiện tại.
  Giữ signing/data/Device Owner/Assistant role/Accessibility/grants và settings.
  Ordinary partial chỉ hiển thị; chỉ final hoặc lần bấm mic thứ hai rõ ràng
  được commit. Không bỏ xác nhận/revalidation để tăng tốc.
- Chuẩn bị bằng host/Windows AVD qua WSL, selector `dex --emulator-only`.
  Mọi physical baseline/A-B/soak thực hiện trong
  [T-020](T-020-consolidated-fold3-acceptance.md), không tạo chiến dịch Fold3 riêng.
- Không chốt mức RAM tuyệt đối hoặc hứa tốc độ provider khi chưa đo.
  Không chạy benchmark/heap dump chứa dữ liệu thật rồi đưa vào Git.

### Hiện trạng đã đọc từ source, cần đo để kết luận

Các đường source sau tương đối với `/mnt/d/dex-assistant-ui`:

| Điểm khảo sát | Bằng chứng và ý nghĩa |
| --- | --- |
| `app/src/main/java/com/khuongnd/dexassistant/ui/voice/CommandRecognizer.kt` | Hint silence hiện là complete 1.500 ms/possibly-complete 1.000 ms cho Command. Chưa chứng minh Google service áp dụng; Chat không dùng hint này. |
| `.../voice/VoiceAssistantService.kt`, `.../voice/SherpaWakeEngine.kt` | Wake model đã reuse, mic được release trước STT, screen preparation không chặn khởi động STT. Wake `accept` vẫn tạo `FloatArray` theo frame; đây là ứng viên đo allocation, chưa phải leak đã xác nhận. |
| `.../AssistantController.kt`, `.../ai/MobileAiProxy.kt`, `.../ai/DirectAiChat.kt` | Command/Chat dùng executor riêng và client lazy riêng; Command call timeout 15 s, Chat 20 s. Chat trả về toàn bộ JSON rồi TTS, chưa streaming. Timeout là giới hạn lỗi, không phải latency bình thường. |
| `.../history/HistoryWorkQueue.kt`, `.../screen/DexAccessibilityService.kt` | History đã có FIFO worker; screenshot có copy/scale/recycle và HardwareBuffer cleanup. Cần đo queue/backlog, peak graphics/native và độ trễ trên main thread. |
| `app/build.gradle.kts`, `.../DexAssistantApplication.kt` | Release chưa minify; catalog compile/load lúc Application start. Chỉ thử R8/Baseline Profile hoặc đổi thời điểm load nếu startup/trace cho thấy đáng làm. |
| [Latency summarizer](../../../scripts/perf/summarize-command-latency.py) | Android phát `path=direct`, nhưng CLI/terminal allowlist hiện chỉ có local/remote/semantic/fallback. Direct terminal chưa được đếm đầy đủ; Chat/provider stage cũng cần coverage riêng. |

Evidence lịch sử T-006/T-011 từng ghi ready→final 6,55–10,34 s,
speech-end callback→final 3–18 ms và final→terminal 25–71 ms. Điều này gợi ý
điều tra endpoint/nhận giọng nói trước, nhưng không chứng minh acoustic
speech-end→action đã nhanh. T-015 chỉ có tám mẫu natural, không đủ nghiệm thu.
AVD matcher p95 1,621 ms/1.000 patterns không phải hiệu năng Fold3.

## Decision questions and evidence

Nguồn chính thức tham khảo ngày 2026-10-13:

- [RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent):
  silence extras có thể bị implementation bỏ qua hoặc gây kết quả ngoài dự kiến.
- [Android memory management](https://developer.android.com/topic/performance/memory-management):
  dùng PSS để phân bổ shared memory; RSS phù hợp theo dõi biến động nhanh hơn.
- [App code memory](https://developer.android.com/topic/performance/memory/guide/app-code):
  `dumpsys meminfo` phân biệt Java/native/code footprint.
- [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview):
  benchmark target nên profileable, non-debuggable, gần release; ghi rõ compilation mode.
- [Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/overview):
  chỉ nhận lợi ích sau khi kiểm chứng profile được áp dụng trên cách cài APK thực tế.

| Câu hỏi | Bằng chứng cần thu và quy tắc lựa chọn |
| --- | --- |
| Chờ ở STT, app hay cloud? | Trace monotonic theo stage, queue wait/main-thread busy/provider timing; ưu tiên stage chiếm p95 lớn nhất. Không suy ra acoustic endpoint từ callback. |
| Endpoint nhanh hơn có cắt mất câu? | A/B hint hiện tại với không hint, sau đó mới thử cấu hình khác trên câu ngắn/dài, số, tên riêng và ngập ngừng. Chỉ giữ nếu latency tốt hơn, intent/slot không giảm và không tăng premature final/EMPTY/no-response. |
| Giảm RAM có làm wake/rearm chậm? | PSS/RSS/Java/native/graphics theo trạng thái, model load/reuse và rearm p95. Giữ model warm khi active; ưu tiên buffer reuse và cleanup tài nguyên phiên. Không unload/reload model mỗi lệnh. |
| UI/screen work có chặn lệnh đơn giản? | Trace callback, traversal, screenshot copy/scale, history publication và Compose. Chỉ defer/coalesce việc không cần cho quyết định; giữ live target revalidation và selected display. |
| HTTP có chi phí tránh được? | Request build, queue, DNS/connect/TLS, first-byte/full-body/parse; xem xét client/pool reuse có giới hạn và đọc body có size cap trước khi giữ toàn bộ. Không cache kết quả action, không tự replay, không đổi timeout/provider để tạo số đẹp. |
| Release tuning có đáng làm? | A/B cùng device/build type/compilation, verify JNI/ONNX/serialization/DPC/Accessibility. Chỉ áp dụng R8/profile nếu latency hoặc RAM cải thiện đo được và lifecycle/regression pass. |

### Metric và tiêu chí nghiệm thu

Mỗi trial ghi route gốc (`local`, direct Command, direct Chat), loại executor
(API/Intent/semantic/grid), cold/warm, build/process identity, epoch/request ID,
kết quả và metric missing. Direct chuyển sang semantic vẫn giữ nguồn direct.
Không log transcript/prompt/key/UI text. Không trừ timestamp giữa process.

| Metric | Định nghĩa và gate |
| --- | --- |
| End-of-speech→action start | Giữ SRS p95 **<2 s** trên Fold3 cho Command còn áp dụng; báo riêng local/direct/semantic/grid. Ghi cả acoustic end với mốc tham chiếu độc lập khi có và recognizer end callback; thiếu acoustic reference thì chỉ công bố callback metric. Confirmation wait tách riêng. |
| Final accepted→first status | Mục tiêu tối ưu nội bộ p95 ≤100 ms cho trạng thái nhận lệnh/đang xử lý; không xem đây là hoàn tất action. |
| Final accepted→local action start | Mục tiêu tối ưu nội bộ p95 ≤100 ms với API/Intent đơn giản, không có xác nhận; semantic/screen báo riêng. Không lấy matcher thay toàn bộ route. |
| Wake→STT ready; terminal→capture rearmed | Báo p50/p95 cold/warm và tỷ lệ actual capture/no-response. Loại bỏ chờ do app đã chứng minh, không chỉ nhìn state LISTENING. |
| Chat first response | Đo final accepted→text reply, TTS queued→start, acoustic end→TTS audible start khi có reference; ack và TTFT/first-byte provider báo riêng. Chat không có action-start nên không ép vào Command metric. |
| Matcher | Giữ Fold3 warm p95 ≤5 ms/1.000 patterns của T-011. |
| RAM và tải nền | PSS/RSS, Java/native/graphics/code, allocation/GC, threads/FD, CPU, thermal theo cold startup, idle LISTENING, STT, action/screenshot, Chat, Pause/Stop. App/DPC process tách riêng; Google STT/TTS external process báo riêng khi đo được. |

Acceptance của T-021: ít nhất một bottleneck phản hồi được giảm có thể lặp
lại, vượt nhiễu giữa các run; có giảm PSS hoặc allocation/peak RAM ở workload
đã xác định và không tăng latency/no-response/intent errors. Mục tiêu giảm
10–20% idle/peak PSS chỉ là hướng khảo sát, không phải cam kết khi chưa có baseline.
M0 ghi giá trị baseline, mức nhiễu và mục tiêu cụ thể trước khi sửa; nếu kết quả
không tách được khỏi nhiễu thì kết luận INCONCLUSIVE, không ghi PASS.

Mỗi route active cần ≥30 mẫu natural mỗi candidate so sánh; ≥3 block lặp lại
cùng prompt set/độ dài, xen kẽ baseline/candidate và cùng network/thermal/settings.
Phân phối cold và warm riêng. Baseline APK/source phải giữ lại để A/B trong
cùng T-020; historical sample chỉ làm tham khảo. Báo đủ success/failure/timeout/
cancel/no-response, không loại lỗi để làm p95 đẹp. Workload overhead của
collector đo riêng; heap/allocation profiling chạy tách khỏi timing chính.

Sau ≥100 natural sessions của candidate và soak 8 h T-020: không crash/ANR/OOM,
không monotonic growth của tài nguyên phiên sau cleanup; thread/FD/cache
không tăng không giới hạn. Chụp memory sau cùng trạng thái idle/quiescence;
retained warm model và allocator capacity phải được giải thích. Không bắt PSS
trở về tuyệt đối ngay lập tức và không gọi forced GC trong runtime để đạt gate.
T-021 chỉ DONE khi các gate áp dụng ở trên và Definition of Done đã qua;
ngưỡng Command <2 s chưa đạt phải ghi rõ và giữ task mở.

## Milestones

1. **M0 — Phép đo và baseline có thể tái hiện.** Hoàn thiện direct/Chat metric,
   correlation và summarizer; đo stage/queue/resource trên host/AVD, lưu baseline
   APK/source identity và workload. Chốt bottleneck, target và noise budget;
   physical baseline chưa có phải ghi pending T-020. Exit: fixture không mất
   direct terminal, missing không thành zero, cancel/errors đầy đủ; không secret.
2. **M1 — Giảm chờ nhận lệnh/phản hồi.** Điều tra recognizer discovery/create,
   mic handoff, endpoint, first status và TTS/rearm. Chuẩn bị A/B Command hints;
   tránh kết luận hint có tác dụng từ unit test. Giữ warm model và commit rules.
   Exit host/AVD: lifecycle/cancel/late callback/EMPTY/long-pause fixtures pass;
   tốc độ/accuracy thật còn pending F01/F02/F08/F10.
3. **M2 — Rút ngắn đường xử lý lệnh.** Theo trace, giảm main-thread work,
   duplicate health/catalog/UI publication, history refresh, screen preparation
   và HTTP setup/parse. Cache chỉ dữ liệu bất biến hoặc có invalidation rõ;
   không reuse target/action plan sau đổi màn hình. Exit: routing/confirmation/
   privacy/local-offline regressions pass, trace chứng minh bỏ được chờ đo thấy.
4. **M3 — Giảm allocation và tài nguyên giữ lại.** Đo rồi xử lý wake frame
   FloatArray, bitmap copy/scale, response body, queue/callback/context lifetime;
   kiểm tra cleanup success/error/cancel/expiry/Pause/Stop/service destroy và
   screen disconnect. Queue limit phải có backpressure/error rõ, không âm thầm
   bỏ Command history. Exit: giảm allocation hoặc peak trên workload tương đương,
   ownership/single-release/late-callback tests pass; không đổi retention policy.
5. **M4 — Startup/release tuning có điều kiện.** Chỉ thực hiện nếu M0–M3 còn
   bottleneck ART/startup/code RAM: prototype R8/resource shrink/Baseline Profile,
   compile-mode A/B và JNI keep rules. Không đổi signing hoặc uninstall app owner
   để cài benchmark variant. Exit: benefit đo được và release functional smoke;
   hoặc ghi SKIP kèm lý do không đáng đầu tư. Đây là tuning, không mở lại T-008.
6. **M5 — T-020 A/B và chốt kết quả.** Freeze candidate sau host/AVD; chạy
   baseline/candidate F10 trong cùng chiến dịch, ghép F01/F02/F05/F06/F08/F12,
   dùng natural sessions hợp lệ chung cho RAM/soak. Nếu fail, rollback/focused
   retest trong điều phối T-020; không đóng task từ AVD. Exit: report before/after,
   số mẫu/missing/errors/thermal/resource, SRS gates, postflight và limitations.

Các sửa lỗi thuộc T-014/T-018/T-017 vẫn cập nhật parent task; T-021 sở hữu tối ưu
và đo A/B, không sao chép requirement hoặc âm thầm đóng task liên quan.

## Validation

Khi triển khai Android, chạy từ `/mnt/d/dex-assistant-ui` với JDK17/Linux SDK:

```bash
env JAVA_HOME=/home/khuongnd/.local/opt/jdk17 ANDROID_HOME=/home/khuongnd/Android/Sdk ./gradlew test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1 --no-watch-fs
```

Khi thay công cụ latency, từ canonical repository:

```bash
python3 -m unittest discover -s scripts/perf -p 'test_command_latency_summary.py' -v
scripts/android/select-test-target.sh dex --emulator-only
```

AVD dùng exact `ADB`/`SERIAL` selector trả về, Windows server 5038; chạy suites
bị ảnh hưởng về voice lifecycle/direct transport/Chat isolation/config/matcher/
numeric/YouTube/dashboard. Không chạy broad fixture chạm owner history.
Benchmark module/class/collector mới phải ghi runnable command trong M0;
không giả định đã có Macrobenchmark hoặc `--path direct` trước implementation.

Trong T-020, thu `DexLatency`/`DexVoiceTiming` filtered và Perfetto metadata
theo run; dùng `adb -s <SERIAL> shell dumpsys meminfo com.khuongnd.dexassistant.ui`
cho snapshot. Snapshot PSS ngoài cửa sổ timing, soak khoảng 60 s/lần; dùng RSS
sampling nhẹ khi cần nhìn peak, đo collector overhead. Không thu log toàn bộ
provider payload. Heap/trace thô có thể chứa dữ liệu riêng, giữ ngoài Git;
chỉ report số liệu đã sanitize. Release/benchmark phải ghi build type, signing,
profile/compilation và thermal, không so debug baseline với release candidate.

Mỗi milestone ghi command thực chạy và PASS/FAIL/SKIP; checks hiện chỉ là
đề xuất. Plan-only validation: local links, ID uniqueness, đủ tám sections,
whitespace và consistency TASKS/STATUS/T-020. Không cần Gradle cho thay đổi docs.

## Risks and escalation

- Endpoint quá ngắn làm sai slot; reuse recognizer/model sai epoch gây mic busy
  hoặc late dispatch; cache screen sai làm tap nhầm. Mọi tối ưu phải giữ guards.
- PSS/native pool dao động, debug profiler/collector và thermal làm lệch A/B.
  Nếu không khống chế được, báo chưa kết luận và đo lại, không chọn số thuận lợi.
- Cloud/network chi phối latency; giảm timeout chỉ làm lỗi sớm hơn. Streaming
  cần protocol/error/context/echo design, không cho partial tool call execute.
- R8 có thể phá JNI/ONNX; benchmark default debug signing/process kill không
  phù hợp app owner. Không tự reset/provision/đổi signing để benchmark.
- Nếu cần đổi provider/model/cost, STT/mic architecture, confirmation/security
  hoặc retention commitment, trình owner phương án cụ thể và tác động; không
  cần xin lại quyền cho khảo sát hay tối ưu nội bộ giữ nguyên các ràng buộc.

## Progress and findings

- [x] Đọc canonical/Android status, task/requirements/decisions liên quan và
  source hot paths; xác định chưa có task riêng cho outcome tối ưu Android/RAM.
- [x] Tạo plan theo ưu tiên phản hồi lệnh; ghi gaps direct metrics, STT hints,
  allocation candidates, release tuning và lịch đo chung T-020.
- [x] Kiểm tra tài liệu Android chính thức, truy cập 2026-10-13.
- [x] M0 host/AVD: artifact/source baseline retained; direct/Chat/status/action/provider
  metrics plus original-route/missing accounting; canonical tools19testsPASS;
  synthetic workload and noise/targets fixed before optimization.
- [x] M1 simulator: recognizer catalog reuse/invalidation + Secure-choice reread;
  final/partial/endpoint defaults unchanged, actual microphone/accuracy pending.
- [x] M2 simulator: bounded serialized schemas, safely quoted request fields,
  request-local first-key reuse, true action-start hooks and error/cancel guards.
  Android JSONException mapping bug repaired with exact AVD regressions.
- [x] M3 simulator: exact-size PCM reuse/erase on capture exit/reset/close,
  bounded charset/BOM-aware body reads, closed/late callback cleanup guards.
- [x] M4 conditional decision: defer R8/Baseline Profile until startup/ART evidence
  and protected-signing benchmark path justify it; no release flags changed.
- [ ] M5 physical A/B/natural/PSS/accuracy/soak/report through T-020; owner deferred.

[Verified evidence](../../evidence/T-021-windows-response-performance-2026-10-13.md)
records full commands, safe suite selection, prior failures, source/APK identities
and limitations. Latest frozen20sourcefiles: JDK17 test/lint/app+test APK
PASS201tasks/6m3s; app200tests each debug/release,0failures/errors/1expected skip,
DPC5eachPASS. Windows DeX_Command_Test_API35/API35/ADB5038/serial127.0.0.1:5555:
31PASS/4physical-or-private-dataSKIP/0FAIL,8.04s. Owner-data-touching Dashboard/
VoiceRuntime class fixtures intentionally omitted; no UI/endpoint change.

Three alternating retained baseline→finalcandidate rounds (same benchmark
source hash) PASS12benchmark test executions,1080Command/discovery trials and
4500native frames per variant. Command p95 pairs ms:34.149→16.073,
36.288→20.446,35.501→18.268; every pair/all9blocks improve beyond screening
noise budget (43.66–52.93%). Whole-fixture allocation Command reduces45.85–45.95%
(about39.6MB→21.4MB/120requests), wake80–84% (499per-frame arrays removed per
500frame block; first allocation remains). Discovery p95 reduces96.79–99.30%.
Native wake timing is INCONCLUSIVE across all3pairs (−1.17%to+4.49%variation,
within10%noise budget); do not claim wake speed gain from standalone runs.

Exact final APKs remain installed in place on AVD, pulled installed hashes
match candidate and original certificate retained. No real provider/mic/
physical device/owner settings/history/grant changes or commit/push. PSS after
fixture was unavailable because process stopped: not zero RAM or idle savings.

Next: T-020 readiness/scheduling when owner resumes physical work; actual
acoustic end→action/TTS, Google endpoint A/B/intent accuracy, natural wake/USB/
rearm/Chat interruption, selected Samsung display, production PSS/CPU/thermal/
battery and100natural sessions/8hsoak remain unverified. Existing parent gates
are not closed by these synthetic results; T-021 remains IN_PROGRESS.

## Decision and requirement updates

Cập nhật TASKS/STATUS và coverage F10/F12 của T-020 ngay khi tạo plan. Khi M0
chốt target nội bộ, ghi ở đây với baseline; không giảm SRS hoặc số mẫu hiện có.
Chỉ thêm DECISIONS khi chấp nhận lựa chọn kỹ thuật/product bền vững sau evidence;
BRD/SRS chỉ sửa nếu lựa chọn được chấp nhận làm thay đổi requirement.
Hiện chưa có quyết định kiến trúc/provider/retention mới.
