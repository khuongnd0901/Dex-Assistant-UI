> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

> Scheduling override 2026-10-13: owner requests Windows simulator tests through WSL during continuation. All remaining physical Z Fold3 execution is consolidated in [T-020](T-020-consolidated-fold3-acceptance.md), one prepared campaign. This overrides older immediate/separate device-run instructions, preserves acceptance gates and does not imply PASS.

# T-017 — Benchmark Sherpa Vietnamese ASR trên Galaxy Z Fold3

## Outcome and boundaries

Đánh giá có thể thay thế hoặc bổ sung Android `SpeechRecognizer` bằng Sherpa-ONNX Vietnamese ASR **chạy hoàn toàn local** trong DeX Assistant hay không. Kết quả phải là bằng chứng đo trên Galaxy Z Fold3 thực tế (SM-F926*, ARM64, Android 15/API 35), đủ để chủ dự án quyết định `KEEP_GOOGLE`, `SHERPA_PRIMARY` hoặc `HYBRID`.

**Đây là ExecPlan, chưa cho phép thay đổi production voice pipeline.** Trước khi triển khai phải xác minh model, runtime và license; benchmark phải được cô lập trong debug/test scope. Không thay wake phrase "Hey Hey", không thay backend, WebSocket, action contracts, Device Owner, DPC, Accessibility hoặc chính sách thực thi lệnh. Benchmark chỉ nhận dạng và tính điểm, không thực thi command/gesture/cuộc gọi/đổi Wi-Fi.

Task canonical: [T-017](../../TASKS.md). T-006/T-015/T-016 và các cổng nghiệm thu chưa hoàn thành tiếp tục độc lập; không chuyển ưu tiên hiện tại T-016 nếu chưa có chỉ đạo riêng.

## Inputs and constraints

- Android repo: [Dex-Assistant-UI](https://github.com/khuongnd0901/Dex-Assistant-UI), đặc biệt `scripts/prepare-voice-assets.py`, `app/build.gradle.kts`, `voice/SherpaWakeEngine.kt`, `voice/CommandRecognizer.kt`, `voice/VoiceAssistantService.kt`, `routing/LocalCommandEngine.kt`; canonical timing: [T-015 latency](../../../docs/T015_LATENCY_MEASUREMENT.md).
- Hiện trạng đã đọc 2026-10-08: `sherpa-onnx 1.13.8` + KWS `sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01` (INT8, `Hey Hey`, one CPU thread). STT lệnh dùng external `SpeechRecognizer` với `vi-VN`, `EXTRA_PREFER_OFFLINE=false`. **KWS hiện tại không phải Vietnamese ASR**.
- Primary candidate cần **xác minh lại từ nguồn chính thức**: `sherpa-onnx-zipformer-vi-30M-int8-2026-02-09`; không nhầm với `sherpa-onnx-zipformer-vi-int8-2025-10-16`, bản 150M, model streaming FP16 hoặc KWS 3.3M.
- Primary references cần lưu URL, version/hash và ngày truy cập trong evidence: [sherpa-onnx source](https://github.com/k2-fsa/sherpa-onnx), [model release index](https://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models), trang model gốc/model card và [Android speech APIs](https://developer.android.com/reference/android/speech/SpeechRecognizer).
- Kiểm tra **license riêng của model weights** và các điều khoản commercial/derivatives/distribution; Apache-2.0 của runtime không tự động cấp quyền cho model. Nếu quyền phân phối không rõ, chỉ nghiên cứu hợp lệ, không đưa weights vào APK phát hành và báo blocker.
- Chỉ sử dụng asset có checksum SHA-256 được chốt sau khi kiểm tra model thực tế; giải nén an toàn, không tự động runtime download, không push model/audio/credential/binary vào repo.
- Benchmark trên thiết bị thật mới có giá trị nghiệm thu. Host/AVD chỉ kiểm tra chức năng, không được gắn nhãn là kết quả Fold3. Không thay đổi quyền, signing identity, owner, assistant role hay reset app/device.
- Dataset/recordings thu thập có đồng ý người nói, lưu cục bộ theo chính sách riêng; không commit raw audio, personal transcripts hoặc log nhạy cảm. Không tự upload transcript/PCM tới backend hoặc dịch vụ AI.

## Decision questions and evidence

| Câu hỏi | Evidence bắt buộc | Quy tắc chấp nhận |
| --- | --- | --- |
| Model đúng bản 30M INT8 và tương thích Android/runtime? | Official release/model files, SHA-256, config, thử load trên ARM64, phiên bản AAR | Đúng phiên bản, chạy được; nếu 1.13.8 không tương thích phải có adapter debug-only được phê duyệt, không thay KWS production |
| Model có hợp pháp để đóng gói/phân phối? | Model card/LICENSE gốc và bundle, runtime NOTICE | Chưa xác minh thì không đóng gói release hoặc suy diễn giấy phép |
| Sherpa có nhận đúng lệnh và tham số? | Transcript + reference + intent/slot scoring trên dataset có version | Báo WER/CER, exact intent, slot accuracy theo nhóm; thống kê false activation và nhầm cặp đối nghĩa như bật/tắt |
| Sherpa có nhanh hơn ở trải nghiệm thực tế? | Decode, RTF, VAD/endpointer, speech end→final, final→route, user-perceived timing trên Fold3 | So sánh p50/p95; không lấy `STT ready→final` làm inference time; không so clock khác nguồn |
| CPU/RAM/nhiệt khi dùng DeX? | `dumpsys meminfo`, CPU samples, battery/thermal, cold/warm và 100 sessions | Có baseline vs candidate, công khai delta và nhiệt/throttling; không chỉ đo một lượt |
| Google STT và Sherpa có so sánh công bằng? | Cùng script lời nói, thiết bị/mic, tình huống clean/noisy, luân phiên thứ tự engine | WAV deterministic chỉ áp dụng khi audio injection được hỗ trợ; Google SpeechRecognizer thông thường dùng live microphone, không tuyên bố same PCM |
| Có thể sử dụng mà không phá hiện trạng? | Debug-only runner, non-execution contract tests, app/DPC checks | Không dispatch action, không sửa voice production, không reset quyền/owner, không side effect |

## Design: benchmark harness (WAV/preparation implemented; live A/B pending)

- Android agent tạo **debug/test-only** `AsrBenchmarkRunner`/small benchmark screen hoặc thiết kế tương đương; main agent review boundaries. Engine abstraction `BenchmarkAsrEngine` riêng cho Sherpa and Google; không tái cấu trúc `VoiceAssistantService` cho PoC.
- Sherpa WAV: PCM mono 16 kHz, đoạn WAV đã có reference, load model một lần/mỗi thread profile, warm-up riêng, decode nhiều lần có kiểm soát. Chạy `numThreads=1,2,4`, báo cold/warm model load, inference latency p50/p95/p99, RTF, model/process PSS and CPU; giữ nguyên config thực tế trong kết quả. Kiểm tra mô hình offline/streaming API trước khi chọn loại recognizer; không hardcode sai model type/file names.
- Live A/B: Android SpeechRecognizer và Sherpa dùng mic vật lý (không mở hai engine chiếm mic cùng lúc). Counterbalance thứ tự AB/BA, cùng bộ câu, nhiều phiên, network status và thiết bị/DeX mode ghi riêng. Không so trực tiếp WAV Sherpa với live Google như thể cùng đường âm thanh.
- Khi thử nghiệm kết thúc phát âm: đo audio speech-end marker độc lập (VAD / annotation) nếu khả thi, speech end→endpoint, endpoint→final, final→intent, wake→mic-ready; đánh dấu metric thiếu là `N/A`, không lấy callback provider làm ground-truth khách quan cho mọi engine.
- Corpus tối thiểu theo mục tiêu **200 audio utterances thật**: 40 system, 40 DeX/UI, 40 YouTube/media, 30 Maps/navigation, 20 multi-step, 30 noisy driving-like. Tạo prompts/reference/metadata không nhạy cảm trong repo; audio tùy chọn thu offline theo consent, không sinh TTS rồi gắn nhãn giọng người thật. Nếu thực tế chưa đủ 200, ghi số lượng thật và đánh dấu coverage thiếu.
- Score: Unicode Vietnamese NFC, lowercase, thống nhất dấu câu/số theo policy có version; tính WER/CER cả raw và normalized khi cần, exact intent accuracy, required slot accuracy, wrong-action-risk (bật/tắt, gọi số khác, lựa chọn sai). `LocalCommandEngine` chạy ở **dry-run** chỉ parse, tuyệt đối không gửi `TypedAction` tới `ActionRouter`.
- Resource: baseline app idle + KWS existing, benchmark Google, Sherpa warm/cold, DeX on/off khi có thể. Lấy `dumpsys meminfo`/CPU/battery/thermal theo sampling nhất quán; chú thích độ chính xác thấp của số pin theo phiên ngắn, kiểm soát nóng máy trước lượt đo.
- Reliability: ít nhất 100 lifecycle start/cancel/restart sessions, chủ động kiểm tra mic conflict, wake engine resume, KWS miss sau nhiều lượt, memory growth/crash/ANR. Không chạy test suite làm force-stop Accessibility: ưu tiên debug harness hoặc `am instrument --no-restart` đã xác minh nếu phù hợp; không gọi reset scripts.
- Báo cáo theo kết quả đo, không tạo p95/p99 có ý nghĩa thống kê từ mẫu quá nhỏ; n và phân phối lỗi bắt buộc ghi cùng percentile. Nếu không có Fold3 thật hoặc user chưa đọc câu, để `BLOCKED/NOT_VERIFIED` thay vì thay bằng host/AVD.

## Milestones

1. **M0 — Source/model/license audit**: ghi SHA/release/nguồn, inventory Android runtime/KWS/STT, lựa chọn phiên bản được xác minh, test-harness design; phân loại license, compatibility và blocker. Chỉ hoàn thành khi mọi giả định quan trọng được xác minh hoặc ghi rõ chưa biết.
2. **M1 — Debug harness and corpus spec**: triển khai dry-run benchmark, WAV loader và scoring fixtures/CLI output; không thay production pipeline. Unit tests cho scoring, dataset validation, không execute action và resource cleanup PASS.
3. **M2 — Deterministic WAV performance on Fold3**: phân nhóm 1/2/4 threads, cold/warm, repeat/warm-up, WER/CER, inference/RTF, peak memory; capture device info và exact model hash; no live claims.
4. **M3 — Live A/B accuracy/latency on Fold3**: ít nhất một corpus live có reference, recording consent, sạch/nhiễu, alternate order, stage timings, Google/Sherpa matched test protocol. Thu đủ 200 utterances hoặc báo chưa đủ/NOT_VERIFIED.
5. **M4 — Stability and resource soak**: ít nhất 100 sessions, KWS wake recovery, mic contention, PSS/CPU/battery/thermal baseline and under test, crashes/ANRs, DeX state; đo cách ly không reset user app.
6. **M5 — Comparison and decision review**: `KEEP_GOOGLE` / `SHERPA_PRIMARY` / `HYBRID` với trade-off accuracy/latency/memory/stability/network/license, cấu hình fallback và rollback plan **đề xuất, chưa triển khai**. Chủ dự án duyệt riêng mọi thay đổi production.

## Validation and deliverables

Khi bắt đầu triển khai, tạo trong **Android repo** (không tạo sớm kết quả giả) `docs/benchmarks/sherpa-vi-zfold3/`:
- `README.md`: setup, model SHA, thiết bị/ADB và cách tái lập;
- `METHODOLOGY.md`: sample selection, normalization, threat-to-validity, warm-up, timing, A/B;
- `RESULTS.md` và `COMPARISON.md`: evidence + trade-off, chỉ ghi số đo thật;
- `results.json` và `results.csv`: generated outputs từ tool, không commit dữ liệu nhạy cảm; repo chỉ giữ schema/templates khi chưa đo;
- scripts/fixtures kiểm tra định dạng/score; capture nguồn evidence và lỗi/timing missing.

Phần source/test: Android JDK17 `./gradlew test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1` nếu dependencies có sẵn; mô tả rõ các bước không chạy được. `git diff --check`, review private-data exclusions, model-license compliance, validation no-side-effects. Với Fold3: đọc model/ABI/API/signature/DPC/Accessibility state trước/sau, không chỉnh quyền hay cài APK khác signature, thiết bị phải được owner chấp thuận trước test tương tác.

Điều kiện nghiệm thu: cả hai engine có **số đo thực trên Fold3**, accuracy + latency + resource + stability, build/host validation, không side effect và báo cáo tái lập được. Thiếu một nhóm evidence thì giữ task `IN_PROGRESS` hoặc `BLOCKED`, không `DONE`.

## Risks and escalation

- **License / redistribution**: nếu model gốc hạn chế thương mại/phái sinh, xin duyệt và xác minh pháp lý trước khi nhúng vào APK dùng lâu dài.
- **AAR / JNI collision**: Sherpa KWS production sử dụng version pin 1.13.8; nếu ASR yêu cầu khác phải cô lập debug runtime hoặc dừng để duyệt, không nâng cấp production tùy tiện.
- **High memory / thermal on Snapdragon 888**: cap thread, test warm/cold, giảm số lần model load, abort soak khi quá nhiệt/hang; không đặt ngưỡng inference giả định.
- **Google STT service and network**: provider có thể tự chọn online/offline, hỗ trợ hint không chắc chắn; record actual service và network state, không khẳng định Google luôn chạy online.
- **Audio/UI privacy**: file cá nhân giữ local có kiểm soát, không upload; benchmark không thực thi lệnh dù transcript có câu `gọi điện` hay `tắt Wi-Fi`.
- **Instrumentation app lifecycle**: tránh `am instrument` mặc định force-stop đang làm Accessibility marked crashed, bảo toàn Device Owner/app data và state trước/sau.
- **False comparison**: WER published upstream khác WER Android INT8; CPU host không phải Z Fold3; inference latency khác endpoint/user-perceived latency.

Escalate cho owner nếu cần: cấp quyền ghi audio thật, thay AAR, lựa chọn model thay thế, tăng dung lượng APK, quyền phân phối weights, thay đổi production routing hay chạy test ảnh hưởng hệ thống.

## Progress and findings

- **2026-10-10 — owner resumed T-017**: expanded resumable200 test-only collector, manifest pin, no-overwrite/orphan guards, real noisy-condition attestation. JDK17 full test/lint/app+testAPK PASS201 tasks; Python15PASS, resume5/5 per variant. Fold3 test-only install/no-mic smoke PASS; owner recorded7 system utterances,105 WAV measurements PASS. WER27.273%, CER0% (spacing), parser6/6 scorable typed actions,0opposition. Same-session no-mic resume retained7 PASS; original testAPK/LISTENING/policy/history/roles restoration verified. Full session7/200 only. [Evidence](../../evidence/T-017-expanded-corpus-2026-10-10.md). M2 partial; M3–M5 remain open.

- **2026-10-08 — real voice pilot authorized**: owner consented to local audio/transcripts and will read on Fold3. Android test-only collector uses existing target debug fixture, explicit recording controls and actual-reference review;12-prompt pilot only. Separate coordinator pause/resume via existing app control releases the mic temporarily, with policy/role/history readback after restoration. No provider upload. Require valid nonsilenced audio and real user taps; live A/B/independent speech-end/200coverage remain open.

- **2026-10-08 — recovered Fold3 native smoke PASS**: owner reconnected38585; SDK37 file-based test-only install SUCCESS. External-file visibility failure resolved with test-only internal-storage staging; focused APK49tasks PASS. Opt-in Fold3 WAV test1/1 PASS12.692s,45 measurements on3upstream clips across1/2/4threads. Median66.33/53.54/55.53ms, p95 103.72/81.48/77.98ms; WER/CER null. Original testAPK restored exact hash; state/policy/history byte-identical, Accessibility bound, DPC/role/app timestamp unchanged. See [evidence](../../evidence/T-017-preparation-smoke-2026-10-08.md). Native gate PASS, M2partial smoke only; full corpus/live A/B/stability/rights open.

- **2026-10-08 — preparation execution**: [evidence](../../evidence/T-017-preparation-smoke-2026-10-08.md) records pinned upstream hash gate,200 prompt slots/corpus validator, Python12PASS, Android test/lint/testAPK144tasks PASS. Model/manifest and3upstream smoke WAVs verified on Fold3, matching-cert testAPK ready. Streamed install failed and ADB endpoint closed; native inference NOT RUN. No production APK install or production code changes. Need active ADB endpoint for test-only install/smoke/restore/state readback; acceptance corpus/live A/B remain open.

- **2026-10-08 — requested execution preflight**: physical Fold3 is now available; benchmark branch438679d scorer PASS6/6. Device has no model/WAV inputs and installed test APK lacks the benchmark class. Owner confirms no corpus/reference yet. No inference/install/live capture ran. [Evidence](../../evidence/T-017-preflight-2026-10-08.md) records read-only resource snapshot and next prerequisites; M2–M5 NOT VERIFIED.

- **2026-10-08 — initial PLANNED record**: yêu cầu ban đầu lập kế hoạch; chưa triển khai code hoặc benchmark ở mốc này.
- **2026-10-08 — IN_PROGRESS, M0 source audit/M1 scaffolding**: Android [draft PR #1](https://github.com/khuongnd0901/Dex-Assistant-UI/pull/1) implements opt-in non-executing WAV runner, host WER/CER scorer and tests, reproducible setup/methodology, Android instrumentation compile step in PR CI. [M0 audit](https://github.com/khuongnd0901/Dex-Assistant-UI/blob/task/T-017-sherpa-vi-asr-benchmark/docs/benchmarks/sherpa-vi-zfold3/M0_AUDIT.md): official offline transducer files verified by documentation; Kotlin v1.13.8 API verified at source level; model source license CC-BY-NC-ND-4.0 creates distribution clearance blocker. Native load, authoritative file hashes, owner-consented corpus and Android CI run #26 PASS for app build/lint, instrumentation compile and host scorer tests; native on-device execution remains NOT VERIFIED. No model weights, no APK install or live device/permission action. M2 WAV Fold3, M3 Google-vs-Sherpa live A/B, M4 long soak, M5 production choice NOT VERIFIED.
- Next: collect consented authentic WAV/reference corpus using prepared200 prompts and validator, complete full M2 accuracy/performance/cold-load, then implement separate live A/B and100mic/KWS session harness. Native load and upstream WAV smoke verified; original test APK restored. Commercial/distribution rights remain open.

## Decision and requirement updates

- Chưa có quyết định thay thế STT. Không ghi `.agent/DECISIONS.md` hoặc thay BRD/SRS chỉ vì đã lập kế hoạch benchmark.
- Sau khi owner duyệt kết luận và có physical evidence: cập nhật [TASKS](../../TASKS.md), [STATUS](../../STATUS.md), `DECISIONS.md` (nếu nhận quyết định), tài liệu Android và những phần SRS bị ảnh hưởng.

- 2026-10-08: real pilot completed12 recordings/180 decodes; WER5.263%, CER4.444%; parser opposition1/11. Restored original voice/testAPK/policy/history. See [evidence](../../evidence/T-017-real-voice-pilot-2026-10-08.md); M2partial, M3–M5 open.


## Windows simulator continuation — 2026-10-13

Continuation 2026-10-13: restored missing pilot-only benchmark source/tools/docs from pinned e5958b9; added3corpus guards. Host15PASS, AVD PCM2PASS + parser-score2PASS/1real-data SKIP; testAPK/lint/full host PASS. Expanded200 collector source unavailable on this host; Git remote access blocked by missing credential helper. Recover later collector before resuming7/200, never replace it with pilot. Production STT unchanged. [Evidence](../../evidence/Android-simulator-continuation-2026-10-13.md).
