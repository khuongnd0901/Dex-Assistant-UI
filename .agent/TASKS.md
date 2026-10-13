> Migrated to Android on 2026-10-13 ([D-037](DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Task board

Current owner priority (2026-10-13): T-021 → T-014 → T-017 → T-020 →
T-018 → T-006 → T-010 → T-011 → T-015 → T-016. Commit/push each verified
task checkpoint. Physical Fold3 execution resumed under T-020; owner then
skipped speaking for now. Natural/visual/manual gates remain open; this does
not mark any task DONE or restart cancelled work.

Physical acceptance follow-up (2026-10-08): owner requested automated-only
Fold3 runs for existing tasks. [Evidence](evidence/Fold3-automated-2026-10-08.md)
records current build/install, Config9PASS/1SKIP and matcher0.370469ms, Maps+pair
VERIFIED, subtitles4PASS, dashboard1PASS, voice4PASS, grid initial failure plus
100-cycle isolated rerunPASS. T-015 preview REJECTED and T-003/T-009 backend
APP_PAIR_WINDOW_MISSING reproduce. T-006 direct-mic fixture has obsolete busy
second-trigger assertion. Preserve task states and natural-voice/soak/ASR gates;
next follow-ups belong to these existing tasks, not a new task.


States: `BACKLOG`, `PLANNED`, `READY`, `IN_PROGRESS`, `BLOCKED`, `CANCELLED`, `DONE`.
`PLANNED` means an ExecPlan exists but implementation has not started.

## T-022 — Move canonical project memory and development entry point to Android

- State: `DONE` (2026-10-13; Android canonical workspace and retired backend redirects verified).
- Planning: [ExecPlan](plans/completed/T-022-android-canonical-workspace.md).
- Goal: Android repository owns canonical AGENTS/STATUS/TASKS/PLANS/DECISIONS, product/action/operational docs, active acceptance/evidence and locally runnable test tooling; future contributors need no backend checkout.
- Acceptance: necessary Markdown and linked local dependencies migrated without losing existing modifications/history/task IDs or gates; Android-only workspace/README/agent entry points; local selector/perf tooling and relevant host checks pass from Android; no active required backend paths or broken local links; old backend entry points redirect to Android and backend development is retired. Preserve app/DPC/runtime/signing/data/roles/grants and all T-021 source/APK identities. No device installs/physical tests/provider/cloud operation required for documentation migration.
- Scope: documentation/project-memory ownership and minimal tooling required for standalone Android development/test; historical backend artifacts remain explicitly historical, not active requirements.
- Verified:132mapped files (115Markdown/7documentation JSON/10tools);43host tests PASS; Android-only workspace/authority and local links PASS; actual API35 Windows AVD selection PASS on127.0.0.1:5555/5038. Source/target/snapshot hashes verified before110source removals and14redirect/workspace entries. T-02120source hashes unchanged; no app/runtime/build/install/physical/provider/cloud/commit/push. Initial starter terminal-return limitation and retry documented. [Evidence](evidence/T-022-android-canonical-workspace-2026-10-13.md).
- Next: future development/test starts at Android AGENTS/STATUS. T-021/T-020 and other incomplete acceptance stay open.

## T-021 — Tối ưu phản hồi lệnh và RAM Android

- Resumed physical checkpoint2026-10-13: owner resumed Fold3, then skipped speaking. Source5472dff host201-task checks PASS; perf tools26PASS; physical offline benchmark2/2 + safe metric/fault17/17PASS. Candidate Command p9512.174ms; matching physical baseline unavailable, no natural/PSS gain accepted. Added guarded read-only Fold3 resource collector and explicit single-run hardware reporting. Original signing/keys/settings/policy/roles/grants retained; history ciphertext changed, plaintext equality unmeasured. [Evidence](evidence/T-021-fold3-automated-2026-10-13.md). Remains IN_PROGRESS.

- State: `IN_PROGRESS` (2026-10-13; simulator M0–M3 verified; owner defers all Fold3 acceptance).
- Planning: [ExecPlan](plans/active/T-021-android-response-performance-ram.md).
- Goal: prioritize end-of-speech→action/first audible answer and next-command readiness; reduce Android RAM/allocation/background CPU without slowing response or weakening safety.
- Acceptance: reproducible stage/queue/resource baseline; complete direct Command/Chat metrics; measured repeatable response improvement and PSS or allocation/peak reduction beyond run noise; no accuracy/no-response/lifecycle regression. Preserve SRS Command p95 <2s and Fold3 matcher p95 ≤5ms/1,000 patterns; ≥30 natural samples per active route/candidate, ≥100 natural candidate sessions and 8h soak through T-020. Relevant Android test/lint/build and runnable behavior pass; incomplete physical gates keep task open.
- Boundaries: Android-only runtime/Gemini/Google production STT, original signing/data/roles/grants, local routing/confirmation/privacy/history/Chat limits retained. No cancelled T-008/T-019 work, backend revival or STT/model/provider migration.
- Priority: simulator implementation completed in requested order; earlier IN_PROGRESS tasks retained. All physical baseline/A-B/resource/soak evidence deferred to T-020 F10/F12 and relevant voice/routing gates.
- Evidence: [Windows response/RAM validation](evidence/T-021-windows-response-performance-2026-10-13.md), exact baseline/final source/APKs and failed-then-fixed Android JSON mapping included.
- Verified: latest20-file frozen JDK17 full test/lint/app+test APK PASS201tasks/6m3s; app200tests/variant,0failures/errors/1expected skip; DPC5eachPASS. Safe Windows API35 suites31PASS/4Samsung-or-private-corpusSKIP/0FAIL/8.04s; canonical19testsPASS. Three alternating A/B rounds12benchmark executionsPASS: Command p9543.66–52.93%lower and allocation45.85–45.95%lower; discovery96.79–99.30%lower; wake allocation80–84%lower, native timingINCONCLUSIVE. Synthetic components only; no natural/Fold3/live-provider/idle-PSS claim. Installed candidate hashes verified; original certificate/data/settings preserved, no commit/push.
- Changes: direct/Chat/status/action/provider metrics, recognizer-catalog invalidation, immutable bounded schema reuse, request-local first-key reuse, bounded response reads, exact-size PCM reuse/scrub, active HTTP cancel/no cancellation fallback and Android JSONException fail-closed mapping. Production endpoint/model/confirmation/retention defaults unchanged.
- Next: physical acoustic/STT/USB/TTS/DeX/natural/PSS/thermal/soak only through T-020 when owner resumes. M4 release tuning deferred absent startup/ART evidence. Dashboard/VoiceRuntime classes omitted because setup touches owner policy/history; isolated replacement fixtures pass.

## T-020 — Nghiệm thu Z Fold3 tập trung cho toàn bộ task Android

- Publication 2026-10-13: owner-authorized Android source3f2bea8 and canonical source/tooling7d3611e pushed to origin/main, remote SHAs verified. Validation and physical gates unchanged; IN_PROGRESS. See [publication checkpoint](STATUS.md#source-publication-checkpoint--2026-10-13).

- State: `IN_PROGRESS` (2026-10-13; preparation only).
- Planning: [ExecPlan](plans/active/T-020-consolidated-fold3-acceptance.md).
- Goal: one prepared physical campaign covers all remaining Fold3 gates from T-006/T-010/T-011/T-014/T-015/T-016/T-017/T-018; implementation and Windows AVD work continue under parent tasks.
- Acceptance: host/AVD readiness and exact candidate build recorded; physical F01–F12 (natural wake/USB/STT/grid/local/semantic/UI/Chat/direct AI/latency/ASR/8h soak), private corpus and pre/postflight evidence; no backend or cancelled T-008/T-019 work. Preserve existing numerical thresholds and owner data/roles/grants. Parent tasks remain IN_PROGRESS until applicable gates pass.
- Scheduling: no physical device tests during preparation; Windows emulator via WSL only. All older instructions to immediately resume separate Fold3 runs are superseded by this combined campaign. One campaign includes interactive and unattended phases; failures still require focused retest.
- Verified: current host test/lint/APKs PASS201, app191tests/variant; tooling11PASS; ASR host15PASS; Windows AVD26PASS/4physical-or-real-data SKIP/0FAIL,18.01s. [Evidence](evidence/Android-simulator-continuation-2026-10-13.md).
- Next: resolve echo-safe spoken exit during TTS and recover expanded200 collector source; finish full readiness before scheduling the single physical campaign. Current fixes/build are validated, not a full physical candidate acceptance.

## Architecture reconciliation — 2026-10-10

Owner confirms Android-only AI/provider proxy and Wi-Fi execution; Spring Boot/WebSocket is no longer a product runtime dependency. This update supersedes older backend acceptance/next-action text below. `CANCELLED` means obsolete scope withdrawn, not acceptance passed. Historical DONE tasks and evidence are retained.

- T-003 and T-009: CANCELLED as backend-dependent outcomes; remaining Android Wi-Fi/local/provider/error/monitoring verification transfers to T-018.
- T-006, T-011, T-014, T-015: remain IN_PROGRESS for Android behavior. Backend provisioning/reconnect, remote WebSocket routing, server Chat/proxy/quota/resource tests, backend-first schema rollout and server E2E gates are CANCELLED. Required direct-provider Command/Chat integration transfers to T-018; existing local execution, confirmation, privacy, voice and device safety gates remain.
- T-008 CANCELLED by owner on2026-10-10; T-010, T-016, T-017 remain IN_PROGRESS. T-001/T-002/T-012/T-013 stay DONE as historical completed work; no renewed backend work scheduled.
- No source migration, deployment shutdown or device acceptance is claimed by this documentation update.

## T-018 — Android-only migration follow-up and final Fold3 acceptance

- T-021 simulator follow-up2026-10-13: Android checked JSONException vs host RuntimeException mapping defect repaired. Exact-source test/lint/APKs and safe AVD malformed/empty/oversize/error/cancel/key lifecycle fixtures PASS under T-021; no new live/physical acceptance. See [T-021 evidence](evidence/T-021-windows-response-performance-2026-10-13.md).

- Continuation 2026-10-13: simulator offline faults/cancellation/expiry/isolation PASS9/9; full JDK17 test/lint/app+test APK PASS201tasks (app191tests/variant). HTTP429/auth/timeout no-replay and confirmed in-flight stale-result/restart gates now have deterministic evidence. No new live provider/physical acceptance. [Evidence](evidence/Android-simulator-continuation-2026-10-13.md).

- Scheduling update 2026-10-13: all remaining real Fold3 tests are coordinated only through [T-020](plans/active/T-020-consolidated-fold3-acceptance.md). Continue implementation/host/Windows AVD work here; no separate physical run. Acceptance thresholds remain unchanged.

- State: `IN_PROGRESS` (2026-10-10); owner resumed and selected automatic-only acceptance this turn. Production source49cb5ad installed on Fold3 with original signature; Android test-only/preflight/status commit `5c6b114` published to origin/main.
- Planning: [ExecPlan](plans/active/T-018-direct-ai-final-fold3-acceptance.md).
- Goal: verify Android owns provider/proxy orchestration and local Wi-Fi/DPC execution, with no required backend server/WebSocket.
- Acceptance: migrated Android source/test/lint/APK; local/redacted provider settings; local Wi-Fi/deterministic commands without backend; guarded direct Command; volatile bounded text-only Chat; faults/cancel/no replay; physical natural voice/latency/resources. Preserve signing/data/roles/grants; retain prior voice/grid/ASR gates.
- Verified:12distinct selected automatic physical cases PASS after fixture repairs:7safe guards, Settings key lifecycle/discovery/persistence, local with cloud disabled/direct simple/two-action compound volume, idempotent Wi-Fi enable,3turn text Chat/context/cancel, invalid credential/error/no replay/restart. JDK17 full host checks PASS201tasks, app176tests per variant; final affected lint/testAPK PASS108tasks. [Evidence](evidence/T-018-direct-ai-fold3-2026-10-10.md).
- Gemini-only follow-up: owner confirms only Gemini available. Selected3case physical repeat PASS3/3,61.312s at15RPM: direct simple/two-step compound,3turn text Chat/cancel, invalid credential/restart. Provider settings/policy/role/Accessibility retained; no source/install changes. These repeat existing cases.
- Configuration: owner authorizes `.env` Gemini key/model through private ADB input into real masked Settings; input deleted, configured key/model intentionally retained. `gemini-3.5-flash-lite`;15RPM test pacing>=4100ms. No secrets/raw artifacts committed.
- Next: deterministic429/auth transport/timeout and confirmed in-flight cancellation; owner-assisted mic/voice/TTS/UI End Chat,30natural samples per route/latency/resources. Owner now scopes current provider acceptance to Gemini only; Groq/fallback is conditional future work when configured. Network-off/Wi-Fi cycle needs independent connection. Natural100sessions/soak and existing gates remain open. No cloud stop/delete authorized.
- Postflight: Device Owner/signing/grants/Assistant role/data inodes retained; Accessibility bound; voice policy byte-identical; history retained/re-encrypted, plaintext equality unmeasured. Only isolated histories used by fixtures.

## T-017 — Benchmark Sherpa Vietnamese ASR trên Samsung Z Fold3

- Continuation 2026-10-13: restored missing pilot-only benchmark source/tools/docs from pinned e5958b9; added3corpus guards. Host15PASS, AVD PCM2PASS + parser-score2PASS/1real-data SKIP; testAPK/lint/full host PASS. Expanded200 collector source unavailable on this host; Credential helper restored/authenticated during publication; remote branch still e5958b9/pilot only. Recover later collector before resuming7/200, never replace it with pilot. Production STT unchanged. [Evidence](evidence/Android-simulator-continuation-2026-10-13.md).

- Scheduling update 2026-10-13: all remaining real Fold3 tests are coordinated only through [T-020](plans/active/T-020-consolidated-fold3-acceptance.md). Continue implementation/host/Windows AVD work here; no separate physical run. Acceptance thresholds remain unchanged.

- 2026-10-10 owner resumed T-017 and confirmed readiness for additional local recordings. Expanded resumable200 collector in isolated Android worktree; JDK17 full test/lint/app+test APK PASS201tasks, Python15PASS, resume5/5 per variant. Physical collection7 real system utterances/105decodes PASS; resume/no-mic PASS retains7. WER27.273%,CER0% (word-spacing differences), parser typed-action6/6 scorable,0opposition. Original testAPK/voice/policy/history/roles restored and verified. [Evidence](evidence/T-017-expanded-corpus-2026-10-10.md). IN_PROGRESS; expanded session7/200, full corpus/live/stability acceptance open.

- Real pilot completed:12 accepted owner recordings/180 WAV measurements; WER5.263%, CER4.444%, parser opposition1/11. Restored LISTENING/original testAPK/policy/history. [Evidence](evidence/T-017-real-voice-pilot-2026-10-08.md). IN_PROGRESS; full coverage/live/stability gates open.

- Real-voice follow-up: owner explicitly consented to reading on Fold3 and local audio/transcript storage. Prepare12-prompt pilot collector in test scope, user-tap Record/Stop, playback/edit/confirmed actual reference, reject silenced/invalid takes, separate private corpus. Temporarily pause/resume existing voice through its public app control to avoid mic contention; no Google upload or command dispatch. Pilot WER/CER requires actual accepted recordings; keep IN_PROGRESS.

- Preparation/smoke2026-10-08: [evidence](evidence/T-017-preparation-smoke-2026-10-08.md): pinned model manifest and3upstream WAVs verified;200 prompt slots/consent validator ready. Python12PASS, Android test/lint/testAPK144tasks PASS plus internal fallback49tasks PASS. Fold3 native smoke PASS45 measurements; median decode1/2/4threads66.33/53.54/55.53ms, p95 103.72/81.48/77.98ms. Original testAPK restored exact hash; policy/history/state snapshots byte-identical. M2partial smoke, authentic corpus/accuracy/live A/B/100sessions/rights still open. IN_PROGRESS.

- State: `IN_PROGRESS` (2026-10-08); M0 source/hash and native smoke verified, distribution rights open; M1 WAV harness/preparation **host build/lint/tests PASS**, M2 partial Fold3 smoke PASS; full authentic-corpus coverage/live/stability acceptance remain open.
- Planning: [ExecPlan T-017](plans/active/T-017-sherpa-vietnamese-asr-fold3-benchmark.md).
- Android implementation: [draft PR #1](https://github.com/khuongnd0901/Dex-Assistant-UI/pull/1) on `task/T-017-sherpa-vi-asr-benchmark`. Adds opt-in offline-WAV instrumentation, host WER/CER/RTF scorer/tests, model/license audit, measurement protocol, and CI compile/test steps. No production changes and no model weights checked in.
- M0 evidence: [Android M0 audit](https://github.com/khuongnd0901/Dex-Assistant-UI/blob/task/T-017-sherpa-vi-asr-benchmark/docs/benchmarks/sherpa-vi-zfold3/M0_AUDIT.md). Upstream 30M model is offline transducer; sherpa-onnx v1.13.8 Kotlin OfflineRecognizer API supports intended calls at source level. Original weights card says CC-BY-NC-ND-4.0. Authoritative file hashes and actual ARM64 native load now verified by private Fold3 smoke; model-specific commercial/distribution clearance remains open.
- Scope/acceptance: Compare non-executing Sherpa vs Google SpeechRecognizer on actual Fold3, with reference WER/CER, intent/slot, speech endpoint/end-to-end timing, decode/RTF, CPU/PSS/thermal, and 100-session reliability. M2 WAV native/performance smoke PASS on3upstream clips; 12-sample real pilot measured; full-corpus M2, live mic A/B (M3), stability (M4) and final recommendation (M5) remain NOT VERIFIED. No data reset, permission/DPC/owner/Accessibility changes.
- Verification: [Android CI run #26](https://github.com/khuongnd0901/Dex-Assistant-UI/actions/runs/37752381206) PASS: app build/lint, instrumentation compilation, WER/CER score unit tests, APK artifact. Does NOT establish actual model inference or device safety. Next: collect consented corpus, full M2/cold-load and separate M3/M4 harness; distribution review remains open; keep PR draft. T-015/T-016 remain IN_PROGRESS.

## T-016 — Thiết kế lại màn hình chính và hiển thị lệnh local

- Continuation 2026-10-13: current-source dashboard compact/wide controls, fill-only examples and explicit Run Windows AVD PASS1/1; full host test/lint/APKs PASS. Samsung pixel/visual gate stays in T-020. [Evidence](evidence/Android-simulator-continuation-2026-10-13.md).

- Scheduling update 2026-10-13: all remaining real Fold3 tests are coordinated only through [T-020](plans/active/T-020-consolidated-fold3-acceptance.md). Continue implementation/host/Windows AVD work here; no separate physical run. Acceptance thresholds remain unchanged.

- State: `IN_PROGRESS`.
- Planning: [ExecPlan](plans/active/T-016-main-dashboard-local-commands.md).
- Goal: ưu tiên cửa sổ DeX; mở ứng dụng thấy trạng thái, điều khiển giọng nói, transcript, ô nhập và nhóm lệnh local phổ biến. Fold3 vẫn dùng được.
- Acceptance:
  - Start/resume, pause, emergency stop, trạng thái và transcript luôn truy cập được; nhóm lệnh mẫu xuất hiện ngay khi mở. “Xem tất cả” bao phủ mọi mẫu/cách nói của catalog local được runtime hỗ trợ, phân nhóm theo mục đích, có tìm kiếm và cách nói khác thu gọn.
  - Chạm ví dụ chỉ điền ô nhập; chỉ nút Chạy mới gửi lệnh qua controller hiện tại. Mẫu chưa điền tham số không được chạy.
  - Các lệnh không được runtime hỗ trợ không được quảng bá là dùng được; yêu cầu ứng dụng/ngữ cảnh/quyền được ghi rõ.
  - Cấu hình, quota, lịch sử và chẩn đoán chuyển vào mục phụ, giữ nguyên chức năng; nút kết thúc Chat chỉ xuất hiện khi Chat hoạt động.
  - Bố cục DeX và Fold3 không che điều khiển chính, transcript hay đường vào danh sách; test/lint/build Android pass và kiểm tra thiết bị được ghi nhận riêng.
- Current progress (2026-10-08): Android dashboard, catalog-derived examples and secondary menu implemented. JDK17 full test/lint/APK PASS 201 tasks; app unit tests 155/variant, zero failures/errors and one expected skip each. The DeX AOSP AVD was recovered through loopback ADB, and [AVD evidence](evidence/T-016-avd-ui-2026-10-08.md) records `DashboardLayoutTest` PASS 1/1 across 320×480, 800×400 and 600×320 dp, plus explicit Run/tap-only behavior. The first compact layout failure was repaired and retested. Fold3/Samsung DeX acceptance remains open; keep `IN_PROGRESS`. T-015 dirty work preserved.
- Owner follow-up 2026-10-08: [full catalog evidence](evidence/T-016-full-local-catalog-2026-10-08.md) confirms 47 supported definitions/180 phrases in nine groups, with expandable alternatives and accent-insensitive search. Catalog-derived coverage/routing tests, final JDK17 201-task test/lint/APK build and AVD layout/tap checks PASS. Final 1920×1080 visual review improved alignment/group headings and focused AVD test passed again. Samsung DeX/Fold3 acceptance remains open; keep `IN_PROGRESS`.

- Physical follow-up2026-10-08: DashboardLayoutTest PASS on Fold3 across compact/wide sizes and fill-only behavior; production MainActivity visible/resumed in DeX display2 freeform1209×680dp. Current full test/lint/APKs PASS201tasks. [Physical evidence](evidence/Fold3-automated-2026-10-08.md) supersedes unavailable-device notes; actual pixel/clipping visual review not performed in automated-only run, so retain IN_PROGRESS pending visual acceptance.

## T-015 — Phát triển dần action và điều khiển UI ngữ nghĩa trên DeX

- Continuation 2026-10-13: full host regressions/lint/APKs PASS; YouTube package-bound encoded results intent AVD PASS1/1. Existing numeric guards retained; semantic/preview/video-card behavior on actual DeX remains T-020. [Evidence](evidence/Android-simulator-continuation-2026-10-13.md).

- Scheduling update 2026-10-13: all remaining real Fold3 tests are coordinated only through [T-020](plans/active/T-020-consolidated-fold3-acceptance.md). Continue implementation/host/Windows AVD work here; no separate physical run. Acceptance thresholds remain unchanged.

- Scope update 2026-10-10: backend-specific remaining milestones CANCELLED; Android/local/voice acceptance remains IN_PROGRESS. Direct AI integration is coordinated under T-018. Older backend dependencies and next actions below are historical.

- State: `IN_PROGRESS`; M0/M1 hoàn thành; M2 resolver và M3 handler YouTube đã qua host build/test, cổng Fold3 còn mở; M4 instrumentation đã có và host validation PASS; 8 mẫu Fold3 local đã đo; đang sửa regression lệnh số, thử endpoint STT và bổ sung index video ngay sau mở YouTube.
- Priority: kiến trúc mới theo yêu cầu 2026-10-07; triển khai từng milestone sau khi có baseline. Không thay thế các cổng nghiệm thu còn mở của T-003/T-006/T-009/T-010/T-011/T-014.
- Planning: [ExecPlan T-015](plans/active/T-015-semantic-command-architecture.md); [báo cáo audit](../docs/COMMAND_ARCHITECTURE_AUDIT_2026-10-07.md).
- Goal: mở rộng đường `TypedAction`/`ActionRouter` local và Spring Boot LLM chung; giải quyết target UI theo ngữ nghĩa trên đúng Samsung DeX trước fallback đánh số/lưới; tăng lệnh xác định có thể chạy local; đo latency từng giai đoạn.
- Constraints: ưu tiên Android API/Intent/DPC đã xác minh rồi mới tới UI/gesture/lưới; dùng CLI/ADB để kiểm tra nhiều nhất có thể, nhưng runtime không phụ thuộc ADB/root/shell. Device Owner chỉ cấp quyền runtime hợp lệ, không thay quyền Accessibility/assistant role/special access do người dùng cấp. STT `vi-VN` và TTS tiếng Việt bắt buộc; partial không tự thực thi, ngoại trừ lần bấm micro thứ hai rõ ràng hiện có. Giữ xác nhận, selected display, privacy, Chat isolation và V1 compatibility.
- Acceptance: M0 baseline; M1 hợp đồng additive backend-first và Android validator chung; M2 resolver revalidate/fail closed và ca YouTube title+artist trên Fold3; M3 registry refinement không phá local/backend routing; M4 đủ bảy metric latency và phép đo Fold3; build/tests/contract fixtures, lệnh demo và regression DeX/grid/STT/TTS Việt PASS. AVD không thay nghiệm thu thiết bị thật. M5 teach/replay nằm ngoài phạm vi thực thi task này.
- Current progress: [M0 baseline](evidence/T-015-M0-baseline-2026-10-07.md), [M1 contract](evidence/T-015-M1-contract-2026-10-07.md), [M2 host](evidence/T-015-M2-host-2026-10-07.md), [M3 host](evidence/T-015-M3-host-2026-10-07.md). M3 extracted deterministic YouTube search/select decisions into a handler; 4 focused tests PASS. Android JDK17 full test/lint/app and test APK PASS (201 tasks); final handler/benchmark/assembleDebug PASS (44 tasks). Host 1,000-pattern p95 before/after 0.223/0.096 ms, not a Fold3 result. Fold3 unavailable in ADB, so M2/M3 physical acceptance remains open. Runtime still advertises schema 1 and withholds `CLICK_TARGET`. M4 follow-up repaired actual action-start forwarding, semantic live revalidation timing and confirmation epoch correlation; backend Java25 verify PASS77 (1 expected skip), Android JDK17 test/lint/app+test APK PASS201 tasks (145 app tests/variant, 1 expected skip; DPC5/variant). Four log-summary tests PASS. Signed matching-certificate APK updated in place on Fold3/API35 successfully; 8 natural local requests captured (6DISPATCHED/2SCREEN_CONTINUE), speech-end callback→action-start sample p95 92.822ms; insufficient for SRS acceptance. Owner reported transient Hey Hey miss then spontaneous recovery; root cause remains open. See [M4 evidence](evidence/T-015-M4-latency-2026-10-07.md). Owner follow-up regression: “mở video số 1” rejected by generic screen numeric guard before registry; scoped YouTube selection repair IN_PROGRESS. Additional accepted follow-up: auto-show stable video-card indexes after opening YouTube; preserve same-card confirmation/revalidation across voice preparation. Command-only STT endpoint-hint prototype host PASS, effect pending physical comparison. Add mở tiếng/mở âm thanh aliases. Next: coherent latest APK/test and physical rerun; retain M2/M3 schema2 and regression gates.

## T-012 — Preserve finalized GraalVM Native Image audit

- State: `DONE`
- Scope: documentation only; save the finalized 2026-10-05 read-only audit in
  `docs/GRAALVM_NATIVE_IMAGE_AUDIT.md`. No migration implementation authorized.
- Planning: localized documentation work; no ExecPlan required for saving the
  completed audit. A future migration requires a separate task and ExecPlan.
- Acceptance:
  - Preserve the baseline, findings, dependency/version matrices, proposed
    phases, effort, open questions and primary-source references.
  - Clearly distinguish historical JVM evidence from unverified native behavior
    and proposed commands from executed validation.
  - Validate document structure, local links and whitespace; update STATUS
    without replacing unfinished T-011 or other implementation acceptance.
- Validation: 12 expected top-level sections, all local links resolved, audit
  distinguishes executed JVM evidence from proposed native commands; whitespace
  and link checks pass. Application builds/tests are not applicable.

## T-001 — Validate initial backend technical baseline

- State: `DONE`
- Planning: completed at
  `.agent/plans/completed/T-001-technical-baseline.md`.
- Goal: replace backend-affecting assumptions with verified implementation
  choices and identify platform assumptions requiring later device validation.
- Scope: LLM/provider direction, OCI constraints, authentication, Spring
  Boot/Java compatibility, latency budgeting, and classification of remaining
  Android feasibility questions.
- Acceptance:
  - Findings cite current primary documentation.
  - Each accepted choice is recorded in `.agent/DECISIONS.md`.
  - Any conflict with BRD/SRS is presented for owner approval.
  - No application source code is scaffolded before the baseline choices are
    accepted.
- Current progress:
  - ExecPlan created at
    `.agent/plans/completed/T-001-technical-baseline.md`.
  - Verification questions, official-source classes, evidence requirements, and
    acceptance rules are defined for all baseline topics.
  - Official evidence was collected for the backend-affecting baseline and
    accepted as D-002 through D-006 after product-owner approval of the plan.
  - SRS assumptions for versions, OCI capacity, authentication, privacy,
    persistence, Wi-Fi credentials, and latency were synchronized.
  - Implementation resolved the backend-affecting scope. Android-specific
    evidence that was not needed for the backend was extracted to T-004 rather
    than being represented as verified.

## T-002 — Scaffold backend brain

- State: `DONE`
- Depends on: `T-001`
- Planning: completed at `.agent/plans/completed/T-002-java-backend.md`.
- Goal: create a locally runnable backend that accepts text and returns a typed
  execution plan using dummy tools.
- Acceptance:
  - The project builds and its tests pass with documented commands.
  - A WebSocket endpoint authenticates a client connection.
  - Dummy tool routing has automated tests for representative commands.
  - No secret is committed to the repository.
- Result:
  - Java 25/Spring Boot 4 modular backend builds locally and as a non-root
    container.
  - Authenticated raw WebSocket supports typed plans, errors, deduplication,
    limits, and remote action results.
  - Fake planner and optional Gemini adapter are implemented and tested.
  - Public protocol and local/container commands are documented.

## T-003 — Implement Android text-first vertical slice

- Cancellation: backend-dependent acceptance is withdrawn by owner direction. Remaining Android functionality and verification transfer to T-018; historical progress below is retained, not current next actions.

- State: `CANCELLED` (2026-10-10; superseded by Android-only T-018)
- Depends on: `T-001`, `T-002`, `T-004`
- Planning: `.agent/plans/active/T-003-android-text-slice.md`.
- Goal: connect an Android app to the retained backend AI/orchestration boundary
  and complete a text-first command round trip through real `LAUNCH_APP` and
  Device Owner `TOGGLE_WIFI` execution. Direct mobile calls to Gemini/OpenAI,
  voice, wake word, Accessibility, split-screen, and Wi-Fi provisioning are
  outside this slice.
- Acceptance:
  - Main and DPC APKs build with compile/target API 35 and min API 30; tests and
    lint pass.
  - The main app provisions a Keystore-wrapped token, authenticates over the V1
    WebSocket, handles reconnect visibly, and does not duplicate commands.
  - A signature-protected DPC service executes zero-touch Wi-Fi toggle only as
    Device Owner; launch-app and failure results are reported truthfully.
  - A Fold3 demo completes text -> plan -> action -> result -> completion and
    records text-path latency. Voice latency remains unverified.
- Current progress:
  - Decisions, protocol support, Android client/DPC implementation, and
    automated validation are complete; historical baseline details are in the
    T-003 ExecPlan.
  - Fold3 Device Owner/assistant provisioning was verified without reset. A later
    isolated real Java JAR/Gemini Fold3 integration completed a compound action
    request as `DISPATCHED`; see `.agent/evidence/T-009-java-jar-backend-2026-10-04.md`.
  - T-003 remains `IN_PROGRESS`: zero-touch Wi-Fi on/off and the task's complete
    text-path demo/latency acceptance are not documented as complete.
  - 2026-10-06 ADB AVD acceptance: DPC Device Owner Wi-Fi off/on both verified
    through the app; original Wi-Fi enabled state restored. Real UI→backend
    remote FakePlanner→Chrome action round trip completed as `DISPATCHED`; AVD
    `ws.send`→`action.result` was 173 ms. Fold3 demo/latency acceptance remains
    open. See `.agent/evidence/T-003-adb-check-2026-10-06.md`.
  - Fold3 rerun2026-10-06 began the DPC Wi-Fi fixture from enabled state, but
    wireless ADB dropped during its toggle and no instrumentation result/readback
    was received. After owner reconnected ADB, readback verified Wi-Fi enabled,
    but the instrumentation result remains missing and is not counted as a
    test pass. Full Fold3 text/backend/action demo remains open. See
    [physical rerun evidence](evidence/Fold3-rerun-2026-10-06.md).

## T-004 — Validate Android technical baseline

- State: `DONE`
- Depends on: `T-001`, `T-002`
- Planning: completed at `.agent/plans/completed/T-004-android-baseline.md`; baseline research covered Android platform, security, licensing, and
  device-behavior boundaries.
- Goal: replace remaining Android-specific assumptions with accepted choices
  before scaffolding or native-control implementation.
- Scope: Fold3 Android/API target, Wi-Fi restrictions, split-screen/DeX,
  foreground-service and microphone rules, wake-word options/licensing, Android
  Keystore token provisioning, and the device-side latency test plan.
- Acceptance:
  - Findings cite current Android, Samsung, and vendor primary documentation.
  - Capability matrices distinguish ordinary-app, user-mediated, device-owner,
    and unsupported behavior.
  - Accepted choices are recorded in `.agent/DECISIONS.md` and conflicts update
    SRS after owner approval.
  - T-003 has a decision-complete API target, security bootstrap, and validation
    matrix; no Android source is scaffolded first.
- Current progress:
  - ExecPlan created at
    `.agent/plans/completed/T-004-android-baseline.md` on 2026-10-01.
  - First-party evidence collection and capability-matrix drafting are complete
    in the completed ExecPlan.
  - Product owner accepted Device Owner zero-touch, a separate DPC APK,
    compile/target API 35 with min API 30, and a two-action text-first T-003.
  - Closeout2026-10-05: all baseline research/decision/documentation criteria
    are satisfied. Actual Fold3 API35 and Device Owner/assistant foundation have
    historical T-006 evidence; Wi-Fi/text execution remains T-003, natural voice
    and resource/soak T-006, provider/offline T-009, grid T-010, and app-pair/
    adapters/physical p95 T-011. None of those tasks is closed by this baseline.
  - Validation: required sections/local references and task/status consistency
    checked; whitespace check passes. No source change, build or new device test.

## T-005 — Establish cross-repository agent workspace

- State: `DONE`
- Planning: completed at
  `.agent/plans/completed/T-005-multi-agent-workspace.md`.
- Goal: make one main agent coordinate end-to-end work while backend and Android
  agents own their respective repositories during parallel implementation.
- Scope: multi-root workspace metadata, durable role instructions, repository
  ownership boundaries, context-loading rules, task handoff, and cross-repo
  validation. The two Git repositories and their histories remain separate.
- Acceptance:
  - A main-agent entry point can discover both local repositories and the
    canonical project memory without relying on chat history.
  - Backend and Android role instructions load only the documents relevant to
    their work and identify shared-contract escalation rules.
  - Current T-003/T-004 state and blockers are visible from the Android repo.
  - Each repository retains its own build, commits, status, and validation.
  - Workspace and documentation checks pass without overwriting unrelated
    uncommitted work.
- Current progress:
  - Product owner selected the main-agent plus two repository-agent model.
  - Backend and Android agents audited their minimum context, ownership, current
    blockers, and validation commands.
  - Multi-root workspace, durable role instructions, and Android handoff memory
    were added and validated without moving either repository.

## T-006 — Run DeX Assistant headlessly with app-scoped DPC control

- Continuation 2026-10-13: current native KWS load/decode-silence/release, Vietnamese endpoint-intent and external recognizer resolver Windows AVD PASS3/3; full host test/lint/APKs PASS. Real microphone/USB/OEM lifecycle/8hsoak remain T-020. [Evidence](evidence/Android-simulator-continuation-2026-10-13.md).

- Scheduling update 2026-10-13: all remaining real Fold3 tests are coordinated only through [T-020](plans/active/T-020-consolidated-fold3-acceptance.md). Continue implementation/host/Windows AVD work here; no separate physical run. Acceptance thresholds remain unchanged.

- Scope update 2026-10-10: backend-specific remaining milestones CANCELLED; Android/local/voice acceptance remains IN_PROGRESS. Direct AI integration is coordinated under T-018. Older backend dependencies and next actions below are historical.
- STT subtitle follow-up (owner 2026-10-05): `IN_PROGRESS`; implemented,
  built and installed with original certificate. Host 124 tests/variant + full
  lint/APKs PASS; fixed physical 4 subtitle cases PASS and separate compact grid
  PASS. Initial natural repeat-turn FAIL repaired (background no-overlay barrier
  stall); owner fixed three-turn rerun PASS: partial/final retention and stale
  number cleanup confirmed. Final RAM cleanup host/physical4-case PASS.
  DeX-only bottom status/partial/final, RAM
  redaction, independent renderer, capture suspension and final three-second
  retention follow D-031. See
  [implementation checklist](plans/active/T-006-headless-voice-dpc.md#dex-stt-subtitles-2026-10-05)
  and [acceptance evidence](evidence/T-006-dex-stt-subtitles-2026-10-05.md).
  - [x] Save scope, selected preferences and implementation/acceptance plan.
  - [x] Implement transient, redacted, turn-guarded subtitle state and STT events.
  - [x] Implement independent DeX overlay and screenshot-safe suspension.
  - [x] Pass unit/focused instrumentation/build/full lint and compact grid regression.
  - [x] Verify repaired repeat-turn natural partial-before-final and three-second
    final retention; owner confirms three turns and stale number cleanup.
  - [ ] Remaining physical resize/disconnect and selection/confirmation lifecycle
    gates; broader T-006 reliability/resource/soak acceptance remains open.
  - [x] AOSP AVD ADB checks 2026-10-06: focused STT partial/final diagnostic,
    malformed/numeric local-routing, and voice-button resolver instrumentation
    PASS 1/1 each; JDK17 unit/lint/app+test APK build PASS153 tasks. Samsung
    DeX display fixtures and live button handoff were not eligible on this AVD;
    physical gates remain open. See
    [AVD evidence](evidence/T-006-avd-adb-2026-10-06.md).
  - Fold3 rerun2026-10-06: subtitle fixture PASS4/1 expected metadata opt-in
    SKIP; native wake model/resolver and canned offline Vietnamese TTS diagnostics
    PASS. Real recognizer entered ready but returned no callback in30s without
    supplied speech. Resize/disconnect, natural selection/confirmation, natural
    latency and resource/soak remain open. See
    [physical rerun evidence](evidence/Fold3-rerun-2026-10-06.md).
  - Post-reconnect handoff probe PASS: the physical listening service released
    the native wake `AudioRecord` before external STT, rejected a duplicate
    trigger while busy, canceled cleanly, and restored wake capture; policy and
    history snapshots matched. Natural speech/partial/final and endpoint latency
    remain open. Same evidence link above.
- Hardware activation requested: mouse mic button (one press) and Bluetooth
  wheel voice button should enter existing command listening like HeyHey.
  Preserve idle/readiness/unlock/suppression and microphone release guards;
  no unrelated key/media control interception. Physical chooser observed; raw
  transport and wheel compatibility unverified. Implemented/installed2026-10-04:
  resolver PASS0.018s, actual STT handoff/release/cancel PASS2.143s; owner mouse
  first trial FAIL: only Bixby/Google. Follow-up actual WEB_SEARCH route
  installed, resolver PASS0.010s, owner physical mouse→listen→YouTube PASS.
  Owner-requested Always preferred handler DeX mAlways=true verified. Evidence
  `.agent/evidence/T-006-T-009-hardware-local-commands-2026-10-04.md`.
  ExecPlan `.agent/plans/active/T-006-hardware-voice-local-commands.md` coordinates
  voice implementation and T-009 strict local percentage/synonym correction.
- Owner-requested second-press submit (2026-10-06): Android implementation is
  complete; JDK17 host test/lint/APK validation passes. While Google STT is listening, a second
  USB-mic press submits the latest nonblank partial through the existing
  final-text route for commands, selection/confirmation and Chat. Empty partial
  executes nothing; session consumption and recognizer teardown reject late
  Google callbacks. One-press final behavior and readiness/suppression guards
  are preserved. JDK17 `./gradlew test lint assembleDebug --max-workers=1 --offline`
  PASS: 133 unit tests per app variant, zero failures/errors, one opt-in skip
  per variant; aggregate lint and APK build PASS. APK signed with the Fold3's
  existing test certificate (SHA-256 `9ddb7f47…abd83d03`). Two ADB incremental
  installs reported success but the package later disappeared; an explicit
  `adb install --no-incremental -r` stayed installed on a follow-up query. No
  manual uninstall or clear-data command ran; data retention remains unknown.
  Owner restored RECORD_AUDIO and Accessibility after the package reinstalled;
  both read back granted/enabled. Fold3 physical second press was confirmed by
  owner but yielded no `stt_manual_submit`; dispatch followed Google final.
  A separate USB `KEY_VOICECOMMAND` event was captured while voice state was
  PAUSED, so the policy guard correctly rejected it. A later active-listening
  trial triggered manual submission, but raw USB key correlation remains
  unverified. Samsung's
  chooser lists DeX Assistant. During ADB UI navigation to help locate it, a tap
  appears to have selected DeX unintentionally; RoleManager now reports DeX and
  app health is READY/TRANSCRIBING. Owner confirmed keeping DeX. Three epochs
  logged manual submit before one dispatch each (13–14 ms gap), with no later
  Google final. Terminals were INVALID_SCREEN_COMMAND, SEND_FAILED and VERIFIED;
  the last may indicate a screen action, so owner was asked if the display
  changed. The filtered raw USB capture was interrupted before its pipe
  flushed. Key correlation and safe-phrase acceptance remain open; no further
  attempt ran. Plan
  and gates:
  [hardware voice ExecPlan](plans/active/T-006-hardware-voice-local-commands.md#follow-up-second-mic-press-submits-the-current-utterance-2026-10-06).
  Physical build/install and nonacceptance evidence:
  [Fold3 second-press run](evidence/T-006-second-mic-fold3-2026-10-06.md).
- YouTube correction verified2026-10-04: replaced ACTION_SEARCH with package-bound
  results VIEW, original-cert app installed/hash verified. Frozen build/lint
  66debug+66release units PASS;device intent contract PASS0.011s,normal-context
  warm executor alternate query PASS1.382s and exact “tìm nhạc Sơn Tùng”
  PASS0.496s (Desktop2 query0→1). Configuration/credential prefs unchanged.
  Test-only metadata correction lint/testAPK PASS;no predicate relaxation.
  Evidence `.agent/evidence/T-006-youtube-search-fix-2026-10-04.md`.
  The deferred voice/UI migration proposal is documented in
  `.agent/plans/active/voice-ui-migration.md`; it is a recommendation only and
  has no implementation milestone started. Track any approved implementation
  through the existing T-006/T-010 acceptance before treating it as active work.
  Owner requests commit/push all related work in both repos, including full
  deferred voice/UI A–J plan. Broader T-006 stays IN_PROGRESS;natural spoken
  latency, content relevance, absent-app browser fallback remain unverified.
- USB trace: owner confirms mouse microphone and pointer drift/stalls. Wake and
  Google both use same USB input, silenced=false; wake stop→Google start183ms,
  no observed overlap. USB disconnect was manual;20s pointer drift not reproduced.
  See `.agent/evidence/T-006-usb-mouse-capture-trace-2026-10-04.md`.
  Next isolate USB capture versus builtin while HID remains connected; no route
  settings or production source changed. Do not infer USB hardware root cause.
- Owner clarifies delay for “mở lưới”: latest SCREEN_CONTINUE ready→final
  7205/14440ms, final→terminal57/49ms. Partial text is display-only; GRID
  uses local freshGrid without screenshot/backend wait. Acoustic end/partial
  timing/first visible frame unmeasured. See
  `.agent/evidence/T-006-natural-voice-grid-2026-10-04.md`; no production fix yet.
- USB diagnosis2026-10-04: owner reports mouse attachment causes Micro USB label.
  Android exposes USB Composite Device as a real audio input (PCM16/16kHz);
  VoiceAssistantService uses default routing and labels routed USB input Micro USB.
  Cannot identify mouse versus hub/dongle from this name or conclude useful signal.
  Latest audio-policy snapshot has no capture patch; current capture route unproven.
  Proposed built-in default plus explicit USB selection/actual-route verification;
  external Google SpeechRecognizer routing needs separate validation. No route changed.
- Owner natural-use regression2026-10-04: HeyHey→mởYouTube sometimes no action,
  later action delayed and wake becomes unusable. Live PID4286 logs: first STT
  callback timeout30s; second ready→final27.164s/SEND_FAILED; third17.520s/
  DISPATCHED with final→terminal69ms. Warm rearm117ms then no new wake observed.
  Diagnose provider endpoint/network/cue and live KWS capture independently;
  LISTENING/unsilenced alone is not accuracy evidence. No production fixes yet.
  Acceptance for diagnosis: metadata-only live capture/worker/STT stage evidence,
  no owner history/credential/permission/route/model/threshold changes; distinguish
  actual spoken results from injected callbacks before choosing a fix.
- Resumed2026-10-04: D-025 FIFO history validated;controlled final→terminal
 1852/1464/1639ms→40/41/38ms.64debug+64release units/build/lint pass.
 Combined19case18PASS/1guarded canvas-expiry failure;focused canvas and
 unchanged9screen rerun pass. Final live YouTube6.460s/Settings6.480s pass.
 Original certificate installed,DPC/data/Accessibility preserved. Broader gates
 remain open. See `.agent/evidence/T-006-history-latency-2026-10-04.md`.


- Physical follow-up2026-10-05: current APK owner confirms HeyHey→YouTube and grid/cancel smoke with audible listening cue. Six real sessions ready→final6.55–10.34s, speech_end→final3–18ms, final→terminal25–71ms. Owner reports STT is slow; no natural latency/reliability gate closed. Metadata places delay before endpoint callback; actual acoustic end/partial timing and mic/provider comparison remain next diagnostic. Four voice runtime tests PASS. Evidence `.agent/evidence/T-011-fold3-dex-2026-10-05.md`.
- State: `IN_PROGRESS`
- Depends on: `T-003`, `T-004`
- Planning: `.agent/plans/active/T-006-headless-voice-dpc.md`.
- Goal: make the dedicated-device client service-first: recover after reboot,
  listen for the offline “Hey Hey” wake phrase (D-019), transcribe a Vietnamese command,
  send only final text through protocol V1, and expose a minimal authenticated
  administration and permission-health UI.
- Acceptance:
  - The DPC grants only grantable runtime permissions requested by
    `com.khuongnd.dexassistant.ui`, reports every permission truthfully, and
    exposes signature-protected typed permission/capability IPC without owning
    the microphone lifecycle.
  - A microphone foreground service implements the defined voice state machine,
    persistent notification controls, boot/package recovery, reconnect/no-replay
    behavior, TTS feedback, diagnostics, and a working emergency stop.
  - Wake word assets and runtime are pinned and offline; wake audio is neither
    persisted nor sent to the backend, and protocol V1 remains unchanged.
  - The main Activity is a compact single-page voice dashboard without scrolling,
    with visible start/resume/pause/stop, transient partial/final command text and
    a non-executing STT diagnostic. Configuration/history open in dialogs;
    Keystore handling and credential redaction remain intact.
  - DeX STT subtitles follow D-031: transient partial/final text and truthful
    recognition status are visible over other apps without intercepting input;
    final text hides after three seconds or a replacement/cancellation. Grid
    cleanup must not erase the subtitle prematurely, screenshots must exclude
    it, and stale callbacks/timers must not revive or erase a newer turn.
    Natural partial-before-final behavior requires separate Fold3 evidence.
  - Wake-to-STT handoff does not pause when the selected recognizer requests
    audio focus; genuine interruptions remain observable and resume works.
  - Automated backend and Android validation passes; device-only reboot, wake,
    microphone-FGS, latency, and reliability criteria remain explicitly open
    until measured on the Fold3.
- Current progress:
  - Checklist reconciliation2026-10-05: implemented KWS/STT/status TTS,
    credential/Settings/diagnostics and one-page dashboard are checked with
    historical evidence. Natural accuracy, cue, timing/re-arm, 100-command
    resource/latency and eight-hour soak gates remain open; Fold3 unavailable.
  - Product scope and locked assumptions were supplied by the owner.
  - Handoff continuation2026-10-03: Android GRID/timing correction passes full
    host build and11 Fold3 regressions. Controlled feedback terminal→first mic
    read726ms; natural slow-wake report remains unproven. Network ARP failure
    recovered after owner reconnects Wi-Fi; no pairing/reset/grant change.
    See `.agent/evidence/T-006-T-010-handoff-continuation-2026-10-03.md`.
  - D-011 replaces the failed cross-UID DPC-start path with a system-bound
    `VoiceInteractionService`; DPC recovery now only applies permissions and
    records health. API 35 role persistence, microphone FGS self-start, and a
    real `AudioRecord` probe pass across cold reboot.
  - Backend `./mvnw verify` and Android `test lint assembleDebug` pass. Actual
    sherpa KWS/model assets, KWS/STT/TTS and text handoff, credential gating,
    special-access links, richer diagnostics, and full Fold3 voice acceptance
    remain.
  - A prerelease containing the two verified debug APKs for Fold3 testing is
    published as `fold3-device-test-2026-10-01`; binaries remain outside Git.
  - The Android test README documents checksum verification, wireless ADB, and
    no-build provisioning directly from the two prerelease APKs.
  - Physical Fold3 testing on 2026-10-01 uses the prerelease APKs without
    rebuilding or modifying Android source. Release hashes/common certificate,
    Device Owner without reset, assistant binding, policy-fixed runtime grants,
    short microphone probe over 5/5 full reboot cycles, emergency stop/restart,
    process crash/rebind, and main-app package replacement recovery are
    verified. Owner-operated notification pause/resume/stop also pass;
    detailed results: `.agent/evidence/T-006-fold3-2026-10-01.md`.
  - Historical prerelease DPC main-app package-update recovery FAILED: broadcasts to RecoveryReceiver
    are skipped by background execution policy, with unchanged policy timestamp.
    DPC boot recovery passed. Startup signature-IPC reconciliation now passes
    physical main-app-only update verification.
  - The unmodified provisioning script has a Samsung unlock-check false positive
    and a fixed-wireless-port reconnect limitation. Independent unlock evidence
    and an ignored ADB wrapper were used in the original test. Corrected strict
    unlock/identity/reboot checks now pass mock and actual-device verification.
  - Owner authorized Android clone/source repair and local signing-key recovery.
    Local Android test/lint/assemble passes; both repaired APKs were re-signed
    with the original certificate and installed on Fold3 over Wi-Fi. Encrypted
    keystore backup is retained in Android Git; raw signing material stays local.
    The initial provisioning attempt failed at shell reboot; the corrected rerun
    passes new-boot/changed-port recovery and strict readiness. Main-app-only
    update advances DPC policy health; live Voice UI stop/start passes. Repaired
    notification controls and negative service-boundary checks pass; final mic
    state STOPPED. Corrected CI36898323611 also passes. Repair evidence:
    `.agent/evidence/T-006-fold3-recovery-2026-10-02.md`.
  - Vietnamese driving feasibility is recorded in
    `.agent/evidence/T-006-vi-driving-feasibility-2026-10-02.md`. D-015 wake
    phrase supersedes the English product phrase; Vietnamese engine selection
    remains unverified. Coordinated driving milestones added to T-006/T-009.
  - D-016 hybrid source/build milestone implemented: English Hey DeX native KWS,
    online-allowed system vi-VN STT, shared headless coordinator and bounded
    canned TTS.42unit tests/build/lint pass, focused7+legacy9 AVD checks pass.
    STT readiness is observed without supplied speech; real voice/resource/8h
    soak remain unverified. Live Gemini acceptance failed AI_TIMEOUT10.891s.
    Evidence: `.agent/evidence/T-006-hybrid-voice-2026-10-02.md`; task remains
    IN_PROGRESS and credential-text/history guards are retained.
  - vi-VN capability gate now has a runnable read-only Android probe. Retained
    API35 AVD has no installed offline Vietnamese STT/TTS data; build/unit/lint
    and diagnostic runner pass. Runtime/model artifact and license provenance
    recorded in `.agent/evidence/T-006-vi-speech-capability-2026-10-02.md`.
    No production engine added; next is isolated model/runtime loading and
    licensed-fixture transcription, plus physical Fold3 measurement.
  - Historical 2026-10-02 checkpoint: KWS/STT/TTS, credential gating, Settings
    links and richer diagnostics were then absent/incomplete; the owner had no
    test backend, so backend/reconnect/Keystore and action/latency checks were
    blocked. Later T-009 backend evidence supersedes the no-backend blocker; the
    broader T-006 acceptance remains open. No completion is inferred from the
    short microphone probe.

- Current wake correction (D-019): owner confirms Live Transcribe receives
  speech and explicitly replaces HeyDeX with HeyHey. New keyword/source/UI
  and APK asset verified;50unit/lint/APKs PASS. Microphone routing/model/threshold
  unchanged; D-018 prototype deferred/removed. Physical HeyHey and all original
  real voice/resource/soak criteria remain open.

- Owner reports first wake opensYouTube but later wake has inconsistently shown
  labels/grid. Fixed every-content-event invalidation by comparing frozen mappings.
  Implemented bounded360ms media cue + visual-ready state at provider ready;57unit
  tests and seven Fold3 tests pass. New owner issue: long re-arm delay after TTS
  failure; needs sanitized stage timing to locate model-load vs recorder delay.
  Audibility remains unconfirmed. Direct app-close unsupported; numberedX works.
  Android commit69f2855 pushed.
- Approved D-020/D-021 correction: feedback-only focus ownership, RAM-only
  masked partial/final transcript, non-executing diagnostic, one-page adaptive
  dashboard and default automatic startup/bounded recovery implemented. Latest
  JDK17 full55unit/lint/app+test APK PASS; Fold3 three handoff/final-loop cases
  and real layout PASS, owner confirms actualHeyHey/transcript/UI. Automatic
  foreground startup/recovery/manualPauseStop case PASS1test3.478s after fixing
  testSTOPdelivery race. Repeated uninterrupted positive spoken wake and original
  boot/resource/accuracy acceptance remain open. Detailed evidence:
  `.agent/evidence/T-006-voice-dashboard-2026-10-03.md`.

## T-007 — Establish reusable Windows Fold3-like API 35 emulator testing

- Follow-up 2026-10-13: added optional `--emulator-only` to `scripts/android/select-test-target.sh` for owner-requested Windows simulator runs through WSL. Three selector regressions plus eight provisioning regressions PASS11/11; actual selector returns Windows API35 `DeX_Command_Test_API35`, loopback serial, port5038. No physical device query/selection in this mode; legacy default preference retained. Physical acceptance scheduling is T-020.

- State: `DONE` (original T-007 and 2026-10-06 fallback follow-up)
- Depends on: `T-003`, `T-004`, `T-006`
- Planning: completed at `.agent/plans/completed/T-007-windows-foldable-avd.md`.
- Goal: create and validate `ZFold3_API35` using the Windows Android Emulator,
  API 35 Google APIs x86_64 image, D-drive SDK/AVD storage, and a 7.6-inch
  foldable profile; provide reusable lifecycle and test-evidence scripts.
- Acceptance:
  - No Linux emulator, C-drive AVD, or API fallback is used.
  - The AVD boots as Android 15/API 35 and exposes documented fold states where
    supported; the report distinguishes AOSP/Google APIs from Samsung firmware.
  - Current Android APKs build/install/launch and automated fold/unfold evidence,
    UI hierarchy, screenshots, logcat, activity/accessibility/DPC state are
    collected without bypassing user-mediated platform behavior.
  - `scripts/android/` contains start, stop, reset, status, WSL test runner, and
    concise usage documentation; reset targets only this AVD.
- Result:
  - Windows AVD `ZFold3_API35` uses API 35 Google APIs x86_64 and the generic
    7.6-inch foldable profile from `D:\Android\AVD`.
  - Start/status/stop/reset and the WSL test runner were exercised. Reset left
    the AVD clean and booted on Android 15/API 35.
  - App launch passed without crash/ANR. Evidence exposed a clipped admin UI,
    unchanged guest metrics across fold/unfold, no AccessibilityService, and
    failed cross-package/boot voice startup; these remain product work.
- 2026-10-06 follow-up: record the Windows SDK/ADB paths in both repositories
  and select Fold3 `SM-F926*`/API35 when connected, otherwise start the existing
  DeX desktop AVD or foldable AVD by test profile. Keep exact serial targeting
  and separate AVD from physical Samsung acceptance. See
  [fallback evidence/plan](plans/completed/T-007-android-test-target-fallback.md).
  Selector chose `DeX_Command_Test_API35` as `emulator-5554`; readback verified
  API35, 1920×1080/160dpi and freeform=1. Linux ADB could not reach Windows
  AVD localhost:5555; Windows ADB on separate port5038 did. Fold3 was absent.

## T-008 — Automate Android CI and device-test GitHub releases

- State: `CANCELLED`
- Cancellation2026-10-10: owner explicitly cancels remaining CI/release automation acceptance. Existing workflows and historical evidence are retained; no new release or signing-secret work is scheduled.
- Depends on: `T-006`
- Planning: `.agent/plans/active/T-008-android-ci-release.md`.
- Goal: validate every Android change and publish reproducible, no-build
  device-test APK pairs from GitHub Actions without committing signing material
  or generated binaries.
- Acceptance:
  - Pull requests and pushes to `main` run Android unit tests, lint, and both
    debug APK assemblies on JDK 17/API 35.
  - A manual workflow creates a prerelease tied to an explicit tag and the
    selected commit, containing renamed app/DPC APKs plus SHA-256 checksums.
  - Device-test releases use the existing shared test signing identity; the
    workflow fails rather than publishing when the signing secret is absent or
    the two APK certificate fingerprints differ.
  - GitHub permissions are least privilege, signing material stays in Actions
    secrets, and documentation distinguishes test signing from production.
  - The committed workflow is syntax-checked, a live CI/release run succeeds,
    and the resulting release assets and certificate fingerprint are verified.
- Current progress:
  - Checklist reconciled2026-10-05: Android CI/manual release workflows exist;
    historical signing configuration and run36898323611 at30df640 pass with
    downloaded APK checksum/certificate verification (T-006 recovery evidence).
  - Remaining: reverify current selected commit, release-specific signing secret
    and failure gates; publish/verify automated prerelease assets, target SHA,
    checksum, certificates and permissions. Historical CI bundle was not a
    published release. No live CI/release action in this documentation cycle.

## T-009 — Add local-first command routing and metered AI proxy observability

- Cancellation: backend-dependent acceptance is withdrawn by owner direction. Remaining Android functionality and verification transfer to T-018; historical progress below is retained, not current next actions.

- Richer local commands requested2026-10-04: preserve trailing %, accept
  “tăng âm lượng100%” as absolute SET100; add synonyms of existing verified
  volume/media/brightness/app/search actions. Preserve privacy, negation,
  multi-intent, unsupported/out-of-range routing and percentage-vs-step meaning.
  No force-stop or new Bluetooth action claims. Acceptance: focused parser
  units, integrated build/lint, real media volume readback with original
  volume/mute restoration, updated command list; coordinates with T-006 hardware
  activation ExecPlan `.agent/plans/active/T-006-hardware-voice-local-commands.md`.
  Implemented/installed:75debug+75release units/lint/build PASS; actual local
  volume40/100 readback and original volume/mute restore PASS0.071s, not skipped.
  Evidence `.agent/evidence/T-006-T-009-hardware-local-commands-2026-10-04.md`.

- Handoff continuation2026-10-04: live Java quota used0→1→2, third request
  AI_QUOTA_EXHAUSTED with unchanged counters; reconnect snapshot continuity PASS.
  Six focused quota/fallback tests PASS. No device actions or owner-state changes.
  Physical legacy tests require isolation before execution. See
  `.agent/evidence/T-009-handoff-quota-2026-10-04.md`.

- Current2026-10-04: ownerupdatedkey;Models.getHTTP200 andlatestJavaJAR
  readiness200UP. IsolatedFold3 realGemini compound/headless flowPASS1test4.186s
  (DISPATCHED,detachedobserver/ownerprefs/keypreservation assertionsPASS).
  Negativeauth/quota snapshotPASS;noDocker,productionAPKunchanged. Existing
  wrappedHTTPclassification fixverified28Maven+4launcher tests. Broadercriteria
  remainopen;previous401 isresolved,notcurrentblocker.
  See `.agent/evidence/T-009-java-jar-backend-2026-10-04.md`; session-clear
  resume commands and next priorities saved in `.agent/SESSION_HANDOFF.md`.


- State: `CANCELLED` (2026-10-10; superseded by Android-only T-018)
- Depends on: `T-003`, `T-004`; coordinates with `T-006`
- Planning: `.agent/plans/active/T-009-local-first-ai-proxy.md`.
- Goal: let Android recognize and execute supported deterministic commands
  locally, use the backend only for requests outside that capability, and make
  provider quota/use and sanitized command outcomes observable on-device.
- Scope: local capability classification and execution, explicit backend
  fallback/unavailable behavior, a backend AI-provider proxy with per-provider
  rate/token budgets, a mobile monitoring screen, and bounded local diagnostic
  history. Provider credentials remain outside the APK.
- Acceptance:
  - A versioned capability/routing contract deterministically distinguishes
    local commands, backend-required requests, and unsupported requests without
    sending local-only commands to the backend.
  - Core supported commands remain usable when the backend is unavailable; a
    backend-required command receives the Vietnamese equivalent of “Khó quá,
    không làm được” with a stable machine-readable reason.
  - Vietnamese local commands cover the accepted deterministic API/DPC set;
    media phrases such as `nhạc lái xe` can open YouTube search for related
    titles without backend access or a fixed playlist.
  - Every Gemini/OpenAI call goes through one backend proxy boundary that
    enforces configurable RPM and daily request/token budgets, randomly selects
    an available provider with budget, falls back to another eligible provider,
    and never exceeds any provider's independent refill window.
  - Android displays backend reachability and per-provider budget usage,
    remaining allowance/reset information when known, and stale/unknown state
    truthfully.
  - Android retains original recognized command text and diagnostic outcomes in
    encrypted, user-clearable local storage until the earlier configurable
    limit: seven days or 100 commands by default. Audio, credentials, secrets,
    and raw provider prompts/responses remain excluded.
  - Backend and Android tests cover local routing, offline behavior, proxy
    accounting/limit failures, monitoring, redaction, retention, and the
    affected end-to-end flows.
- Current progress:
  - Change request accepted and cross-repository ExecPlan created.
  - Owner's Vietnamese hands-free driving command expansion is captured in
    `docs/VI_VOICE_DRIVING_HANDOFF.md` (D-015). Next action is coordinated
    T-006 voice feasibility and T-009 capability planning; no new implementation
    or completion evidence is claimed by saving this handoff.
  - Driving privacy first slice passes Android JDK17/API35 build/lint, 34 unit
    tests and two focused AVD tests. Credential/provisioning text cannot reach
    backend through controller/parser; full sensitive text/outcome is redacted
    before storage and legacy read/re-persistence. Canonical capability/SRS
    updated; real wake/STT/TTS and physical acceptance remain open. Evidence:
    `.agent/evidence/T-006-vi-driving-feasibility-2026-10-02.md`.
  - Owner accepted the initial Vietnamese command set, local YouTube search,
    bounded original-text retention, automatic provider fallback, independent
    quota refill windows, random provider selection, and no manual counter reset
    (D-013).
  - Wave 1 backend/Android inventories completed read-only. D-014 now locks the
    `vi-local-v1` capability matrix, additive V1 quota snapshot, stable AI
    errors, and durable provider accounting/fallback contract.
  - Shared protocol fixtures are committed before parallel source work; backend
    and Android automated implementations/validation are complete.
  - Android commit `72d0ece` passes full Gradle test/lint/debug assembly. Backend
    proxy/quota/monitoring implementation passes Java 25 `./mvnw verify`.
    T-009 remains IN_PROGRESS pending Wave 4 live provider and Fold3 evidence.
  - AVD/Docker Wave 4 subset now passes seven instrumentation tests and live
    Gemini planning/atomic local-budget exhaustion/restart persistence. Runtime
    fixes pass 27 backend tests. See
    `.agent/evidence/T-009-avd-docker-2026-10-02.md` for precise remaining
    acceptance; core implementation is not yet the complete T-009 outcome.
  - Owner-requested ADB simulator retest 2026-10-06: local volume/Wi-Fi,
    offline reasoning and history-worker terminal-independence checks PASS.
    Offline submit-to-SEND_FAILED state measured306ms. The prior 10s timeout
    was a stale test predicate: it waited for AI_UNAVAILABLE as the newest
    record, while finish writes SEND_FAILED afterward. History check passes
    with the status branch matching service availability; UiAutomation disables
    Accessibility during instrumentation, then the user-enabled DeX component
    was restored and verified bound afterward. Dashboard error text is only
    available in Details, leaving immediate visible feedback open. Monitoring,
    provider and physical gates remain open. See
    [AVD evidence](evidence/T-009-avd-adb-2026-10-06.md).
  - Fold3 rerun2026-10-06: Vietnamese offline STT inventory unavailable (no
    installed exact `vi-VN` model); Vietnamese offline TTS voices available and
    canned synthesis succeeds. Real recognizer became ready but had no callback
    without supplied speech. Provider fallback and isolated real-provider E2E
    were not run. See
    [physical rerun evidence](evidence/Fold3-rerun-2026-10-06.md).
  - Same Fold3 run: numeric local volume 40%/100% PASS with original volume and
    mute restored; compact/wide dashboard layout PASS. Owner history was not
    cleared. Same evidence link above.

- Physical2026-10-05 current isolated Java25/Gemini compound test FAIL APP_PAIR_WINDOW_MISSING; authenticated round trip reaches actual Android execution but no successful E2E completion. Unique test stores/RAM token preserve owner credential/history aliases; no Wi-Fi disconnect. Current backend63tests/JAR PASS. Prior successes remain historical. Evidence `.agent/evidence/T-011-fold3-dex-2026-10-05.md`.
- Fold3 rerun2026-10-06: `ConfigCommandDeviceTest` 8PASS/1 AOSP-only SKIP;
  matcher p95 0.352ms. Maps address query visibly DISPATCHED and Maps+YouTube
  pair APP_PAIR_VERIFIED on display2. Natural voice/selection/confirmation,
  backend end-to-end and physical resource/soak gates remain open. See
  [rerun evidence](evidence/Fold3-rerun-2026-10-06.md).
- Post-reconnect read-only voice capture observer PASS10s: service LISTENING,
  recording and not silenced, one native wake worker, RMS0.0 and no wake event.
  This does not prove wake accuracy or natural voice routing. Same evidence link.

## T-010 — Local numbered DeX screen control

- Continuation 2026-10-13: current numeric/malformed local-only routing Windows AVD PASS1/1 and native silence/model fixtures PASS. No reproducible simulator Hey Hey acoustic failure established; natural wake/actual geometry/100sessions remain T-020. [Evidence](evidence/Android-simulator-continuation-2026-10-13.md).

- Scheduling update 2026-10-13: all remaining real Fold3 tests are coordinated only through [T-020](plans/active/T-020-consolidated-fold3-acceptance.md). Continue implementation/host/Windows AVD work here; no separate physical run. Acceptance thresholds remain unchanged.
- Preparation continuation2026-10-10: owner chooses tooling only. Privacy-filtered timing/resource collector, isolated run summaries and configurable10-touch safe fixture ready; Python6PASS, JDK17 app lint/testAPK PASS99tasks1m58s. No install/mic/display change. Natural100 and actual resize/disconnect remain OPEN; raw taps/correlated terminals never certify natural acceptance. [Evidence](evidence/T-010-natural-preparation-2026-10-10.md).
- Resumed2026-10-10 by owner; current priority. Automatic Fold3 selected4/4 PASS54.089s: simulated stale display callback guards, content/window coordinate/consume-once, coordinator subdivision and real expiry. Host test/lint/APKs PASS201tasks21s; only original-cert testAPK updated. Owner now agrees hands-on100 natural sessions and real display-change acceptance. Actual resize/disconnect and natural100-session latency/resource gates remain open; 8h soak stays with T-006. [Evidence](evidence/T-010-fold3-automatic-2026-10-10.md).
- ADB simulator follow-up 2026-10-06: API35 `DeX_Command_Test_API35`
  numeric/malformed local-only routing fixture PASS1/1. Secondary-display
  coordinate gesture fixture assumption-skipped (`displayId=0` only); no DeX
  gesture was attempted. This does not close physical display acceptance.
  See `.agent/evidence/T-010-avd-adb-2026-10-06.md`.
- Fold3 rerun2026-10-06: second 12-test grid invocation passed11 with one
  expected live-foreground opt-in SKIP; synthetic100 prepare/clear smoke PASS.
  One suite-only expiry/content-notification assertion failed first, then the
  exact isolated rerun and later full rerun passed. Subtitle fixture PASS4/1
  metadata opt-in SKIP. Natural100-session/soak and resize/disconnect gates
  remain open. See [rerun evidence](evidence/Fold3-rerun-2026-10-06.md).
- Resumed2026-10-04: D-025 FIFO history validated;controlled final→terminal
 1852/1464/1639ms→40/41/38ms.64debug+64release units/build/lint pass.
 Combined19case18PASS/1guarded canvas-expiry failure;focused canvas and
 unchanged9screen rerun pass. Final live YouTube6.460s/Settings6.480s pass.
 Original certificate installed,DPC/data/Accessibility preserved. Broader gates
 remain open. See `.agent/evidence/T-006-history-latency-2026-10-04.md`.


- Owner correction2026-10-05 accepted after current APK install: compact cell grammar and unexpired subdivision preservation implemented; coordinator cell2→child8 actual tap PASS, four safety/expiry cases PASS. Owner confirms subdivided tap now correct; natural metadata records descendant child8 rather than old full-grid8. Broader100-session/resource/latency gates remain open. Evidence `.agent/evidence/T-011-fold3-dex-2026-10-05.md`.

- State: `IN_PROGRESS`
- Coordinates with: T-006 wake/STT lifecycle and T-009 local routing; their
  unfinished acceptance remains unchanged.
- Planning: `.agent/plans/active/T-010-numbered-dex-control.md`.
- Goal: after Hey Hey (D-019), number clickable desktop targets locally, with a 6×4
  recursive grid fallback and Vietnamese number commands.
- Acceptance:
  - Explicit user-enabled AccessibilityService reads/captures/gestures only on
    the dynamically selected Samsung desktop display; never falls back to phone.
  - Non-intercepting numbered overlay prepares alongside STT readiness (D-024);
    late preparation cannot revive cancelled turns. Immutable
    target mapping invalidates on window/display changes and before stale taps.
    Grid cells use fixed desktop coordinates across content/window changes
    (owner-approved D-023); display identity/size, grant and expiry still gate taps.
  - Visible enabled clickable Accessibility nodes plus bounded local bitmap
    contours/X detection are deduplicated; uncertain detection falls back to grid.
  - Grid has 24 cells and at most three subdivisions; split keeps listening,
    click/cancel ends session; pause/stop/STT failure/timeout removes overlay.
  - Whole-input digit/Vietnamese-number commands, malformed and compound numbered
    commands stay local; V1/backend tools are unchanged. Click is DISPATCHED.
  - JDK17 unit, lint, debug/test APK builds and diff checks pass; runnable
    secondary-display tests cover coordinates, lifecycle and no backend routing.
  - Physical Fold3/API35 DeX wake/number/tap, popup/YouTube controls and 100-session
    latency/resource evidence are required before DONE.
- Latest correction: fresh explicit GRID separates structural window checks from dynamic
  node labels; host build and11 Fold3 regressions PASS101.777s. Owner requests
  live YouTube/another app grid validation, now in progress; no natural-use
  acceptance inferred from fixtures. See continuation evidence linked in T-006.
- Earlier progress: Android service/display overlay/grid, bounded node/image targets,
  local parser/coordinator/wake cleanup and debug fixture implemented. JDK17
  unit/lint/app+test APK build PASS;50unit tests pass. Matching-signed Fold3
  upgrades preserve grant/data/DPC. Ordinary guarded full device suite PASS4tests
  (8.817s): actual visible/focus-preserving/live passthrough overlay, correct
  desktop cell11, split/cancel cleanup and no-node X fresh click/closed fixture,
  local routing. Owner verifies real spoken wake/grid/split/continued listening.
  Fresh node identity/window invalidation/real35s expiry now physically PASS.
  Later full fixture suite7PASS/66.241s includes stable content notifications, real
  expiry and provider cue. Despite that fixture result, owner reports “hiện lưới”
  returns “Màn hình đã thay đổi” on visually static YouTube. Strict full Snapshot
  comparisons in `prepare()`/`execute()` likely still see dynamic clickable node
  signatures; diagnose sanitized field deltas and allow a fresh explicit GRID only
  when current DeX display/bounds/topology are safe. Preserve stale-click guards.
  Natural YouTube and spoken100-session acceptance remain open; opt-in100prepare
  synthetic smoke unrun.
  See `.agent/evidence/T-010-numbered-dex-2026-10-02.md`.

- Physical2026-10-05 eight current isolated DeX grid fixture cases PASS, including visible overlay/pass-through gesture, coordinates/one-shot, changed target/canvas and actual expiry cleanup. Owner confirms natural grid/cancel smoke but reports STT latency;100natural sessions/resources remain open. Evidence `.agent/evidence/T-011-fold3-dex-2026-10-05.md`.

## Completed

- Project requirements loaded from BRD, SRS, and implementation plan.
- Durable agent memory structure initialized.
- T-001 accepted the backend baseline as decisions D-002 through D-006.
- T-002 delivered and validated the local/container Java backend first slice.
- T-005 established the cross-repository main/backend/Android agent workflow.

## T-011 — Config-driven local command engine and backend AI routing

- Continuation 2026-10-13: Windows AVD Config7PASS/3physical SKIP; matcher p95=1.621113ms/1,000patterns/300warm samples (AVD only). Local safety/registry tests and full build/lint PASS; scoped Samsung routing remains T-020. [Evidence](evidence/Android-simulator-continuation-2026-10-13.md).

- Scheduling update 2026-10-13: all remaining real Fold3 tests are coordinated only through [T-020](plans/active/T-020-consolidated-fold3-acceptance.md). Continue implementation/host/Windows AVD work here; no separate physical run. Acceptance thresholds remain unchanged.

- Scope update 2026-10-10: backend-specific remaining milestones CANCELLED; Android/local/voice acceptance remains IN_PROGRESS. Direct AI integration is coordinated under T-018. Older backend dependencies and next actions below are historical.

- State: `IN_PROGRESS`
- Priority: current implementation; does not replace unfinished T-006/T-009/T-010 acceptance.
- Planning: `.agent/plans/active/T-011-command-engine.md`.
- Goal: immutable validated JSON command registry, typed shared Android ActionRouter, voice selection/confirmation, selected app/Maps/call/browser/feed catalog and capability-filtered backend DEVICE_COMMAND; allow two configured apps to run side-by-side in Samsung DeX and route commands by explicit intent even while both are open.
- Acceptance: whole-input matching preserves original slots; aliases/patterns editable in config; invalid reload retains registry; partial has no effects; local commands stay local; unmatched/composite routes backend once; every call/semantic click requires subsequent whole-input spoken confirmation with revalidation; timeout/cancel/duplicate guards; selected desktop UI access only; typed remote arguments cannot override policy; additive V1 old-client compatibility; configured app-pair launch stays on selected DeX display and reports success only after both distinct windows are observed side-by-side; “mở nhạc {query}” resolves to YouTube play-query while Maps home/work continues to resolve locally; Chrome foreground “tìm địa điểm {query}” targets Google search in Chrome while Maps foreground uses Maps; builds/app lint/unit suites pass; Fold3 spoken flows and p95 ≤5 ms/1,000 patterns measured separately.
- Constraints: preserve owner history/credentials/ADB; no actual call without owner-selected recipient/time; no account actions, incoming calls, remote config, OCR or generalized workflow engine.
- Physical verification2026-10-05: current original-cert app installed on SM-F926B Android15 DeX1920×1080. Host117tests/variant + app lint/build PASS; backend63tests/JAR PASS. Shared17case physical run15PASS/1SKIP/1FAIL: config/semantic target + eight grid fixtures PASS, matcher p95=0.349ms, Maps+YouTube APP_PAIR_VERIFIED with two960×1080 panes on display2. Earlier app-pair CANCELLED retained as cold-launch limitation. Gemini compound FAIL APP_PAIR_WINDOW_MISSING; natural voice/resource gates remain open. Samsung instrumentation loses Accessibility bind; owner toggles in Settings during bounded readiness. Test-only readiness/app-pair regressions added. Evidence `.agent/evidence/T-011-fold3-dex-2026-10-05.md`.
- Owner regression follow-up2026-10-05: “mở song song Google Maps và YouTube” opens paired panes but speaks failure, Maps address command unavailable, and “mở lưới → chia ô2 → bấm ô8” uses old full-grid cell8. Continue existing T-011/T-010 plans. Correction acceptance: truthful APP_PAIR_VERIFIED spoken success, scoped pair launch/recheck independent of transient focus; Maps address phrase routed locally to configured Maps on selected display with truthful dispatch; retain subdivided grid across actual STT/coordinator continuation and fresh wake where unexpired, tap new subcell8 once, preserve display/expiry/cancel/stale-target guards. Host regressions/build/lint plus actual Fold3 lifecycle/correct-coordinate checks required; user-visible natural trial recorded separately.
- Correction validation2026-10-05: Android120tests/variant, app lint/APKs PASS; original-cert updated app installed/hash verified. Pair and exact public Maps query PASS on selected display2; coordinator fresh/continuous/compact grid2→child8 PASS5.170s, four safety/actual expiry guards PASS54.102s. Truthful pair speech has regression. Owner natural recheck confirms all three corrected outcomes; original continuous wrong-cell root still unproven, metadata logging verifies descendant grid tap. No commit/push, task remains IN_PROGRESS.
- Verification report saved at `docs/FOLD3_DEX_VERIFICATION_2026-10-05.md` per owner request; final corrections, owner acceptance, checks/artifacts, earlier failures and remaining gates recorded. Documentation structure/local links/whitespace PASS; application rebuild not applicable to this save.
- Progress: implemented typed JSON engine/catalog, shared local/remote ActionRouter and voice confirmation/selection, guarded app adapters/local aliases/onboarding, capability-filtered DEVICE_COMMAND and V1 compatibility. Latest Chrome-context search slice: Android 110 unit tests, APK assembly and `:app:lintDebug` pass, including intent routing to Chrome only when Chrome is foreground and Maps search when Maps is foreground. Aggregate lint stalled at unchanged DPC analysis. Targeted backend tests, JSON validation and fixture comparison pass; earlier JDK17/Java25 results are historical baseline.
- AVD: user reports Fold3 unavailable; dedicated API35 desktop approximation1920×1080/160dpi/freeform boots. Earlier frozen app/test APK pair: six focused runtime tests PASS1.964s, one actual-DeX fixture skipped; benchmark1,000patterns p95=2.060ms (not Fold3). Current instrumentation could not install because the existing AVD app has a different signature; it was not removed and its data was preserved. Runtime ICU placeholder bug fixed; full-prefix indexing avoids crowded first-word scans. No owner history/credentials/permissions/Wi-Fi/calls altered.
- Quality review2026-10-05: corrected session/callback/admission/pending races, cancellation and hot-path allocation/traversal; added regressions. Android117 tests per variant, app lint/APKs pass; six current isolated API35 AVD runtime cases pass, one DeX fixture skipped; p95=3.235ms/1000patterns. Backend verify57 tests/JAR pass. Evidence: `.agent/evidence/T-011-stability-review-2026-10-05.md`.
- ADB simulator follow-up2026-10-06: existing `ConfigCommandDeviceTest` on `DeX_Command_Test_API35` API35/Android15 returned 6PASS/3 assumption-SKIP/0FAIL; AVD matcher p95=0.352ms/1,000 patterns. No APK reinstall, data reset, or permission change. Samsung-display and Fold3-only fixtures remain open; this does not close physical acceptance. Evidence: `.agent/evidence/T-011-avd-adb-2026-10-06.md`.
- Next: Fold3 spoken local/backend/select/confirm/cancel/timeout, selected-display/app/media/Maps/recipient revalidation, physical p95 and CPU/RAM/battery/soak. Back/Home/Notifications stay unavailable until scoped execution proven.
- Owner clarified the target Fold3 runs Android 15 and requested voice commands to close YouTube, Chrome, Maps, Facebook, TikTok, etc. Android 14+ public process/task APIs cannot stop or close another app's task. Window-close must use the selected DeX window's accessible X control and confirmation. ADB presently exposes only the API35 AVD; no Fold3 DeX accessibility tree is available to verify the close target. Clarification pending on whether “tắt app” means close the DeX window (recommended), force-stop via Settings, or both as separate commands. Added hands-free app switching, nearby search, media, scrolling, and web-search synonyms in the Android catalog; current full local command-engine unit suite passes, but natural speech remains unverified.
- Owner additionally requested “thu nhỏ cửa sổ”, switch to previous/next app, navigate home/work, and speak newly received notifications. Home/work navigation is already handled by `đưa tôi về {destination}` plus locally saved aliases. Minimize/app-switch must remain unavailable until the selected DeX desktop exposes revalidated scoped window controls; global accessibility actions could affect the phone display. Notification reading is not implemented: requires explicit Notification Access and a privacy-safe on-demand TTS path with content excluded from backend/history. Android docs now describe these boundaries; implement and verify each capability in guarded slices, preserving T-011.
- Owner requests a spoken two-app DeX layout for any two configured apps, plus intent-directed commands within that layout (Maps home, YouTube music). Implemented `APP_PAIR_LAUNCH` as an additive DEVICE_COMMAND action using package IDs from client config, explicit selected DeX display and observed window bounds; do not infer routing from focus alone. Apps outside the configured catalog remain unavailable until configured. Android unit/build and backend protocol tests pass. Chrome foreground “tìm địa điểm vui chơi gần đây” now targets encoded Google search in configured Chrome; Maps foreground routes that wording to Maps. Package-targeting still needs device confirmation. The API35 AVD instrumentation install was blocked by signature mismatch; Samsung desktop routing/placement and Fold3 acceptance remain open.
- Publication2026-10-05: owner-authorized implementation commits pushed to origin/main: backend60414f0, Android15a64d3. Shared fixture identical; staged whitespace check passes with CRLF support. Existing EOL-only local changes preserved outside commits; no generated/credential material included. Physical acceptance stays open.
- Evidence: `.agent/evidence/T-011-host-avd-2026-10-05.md`; reproducible AVD `scripts/android/start-dex-test.ps1` and `scripts/android/README.md`. Existing dirty changes preserved; no commit/release/physical install.

## T-013 — Backend quality and constrained Docker tuning

- State: `DONE`
- Planning: `.agent/plans/completed/T-013-resource-tuning.md`.
- Scope: backend quality/resource fixes and repeatable Docker measurements at 0.1 CPU / 1 GiB RAM; no Android/protocol/provider migration.
- Acceptance: measure baseline and tuned startup, memory, CPU throttling and authenticated command latency under enforced limits; bounded representative workload and idle/soak complete without OOM; fixes have relevant regressions; Java25 verify and container smoke pass; reproducible configuration/script and honest measured limitations documented.
- Preserve existing dirty files and all unfinished device acceptance.
- Result: full Java25 verify63 tests/JAR PASS (identical Linux source copy); workspace package retry and final local Docker build PASS. Same-image JVM tuning reduces peak236→153MiB and startup129→58s; final code run readiness77s, peak154MiB, first1.30s/p954.75ms,24/24 commands plus device-result/rate-limit/health checks PASS, no OOM. See `.agent/evidence/T-013-resource-tuning-2026-10-05.md`.
- Reproduce: `docs/RESOURCE_TUNING.md`, `compose.low-resource.yml`, `scripts/perf/docker-resource-test.py`. Short fake-planner single-client evidence only; real provider/Android/long soak remain unverified. No commit/push.

## T-014 — Free-form AI voice conversation sessions

- Priority continuation2026-10-13: TTS replies now split by runtime input limit, preserve surrogate pairs and complete only after all chunks; unique chunk/generation IDs reject stale/duplicate/cancel callbacks. Full201-task test/lint/APKs PASS, app204tests/variant;4new playback tests PASS each. Physical offline Vietnamese TTS chunk/cancel/same-ID fixture PASS1/1,6.378s; installed hashes and original identity/policy/credentials retained. [Evidence](evidence/T-014-tts-chunks-2026-10-13.md). Spoken exit during TTS still unsupported; natural F08/parent remain IN_PROGRESS.

- Continuation 2026-10-13: fixed idle clock before first turn, automatic15min session/context teardown and active HTTP cancellation/no cancellation fallback. Voice service automatic-expiry cleanup hook added. Host exact-boundary test and Windows AVD idle/cancel/restart fixtures PASS; full build/lint PASS. Spoken exit during TTS remains unsupported, requires design and T-020 physical proof. [Evidence](evidence/Android-simulator-continuation-2026-10-13.md).

- Scheduling update 2026-10-13: all remaining real Fold3 tests are coordinated only through [T-020](plans/active/T-020-consolidated-fold3-acceptance.md). Continue implementation/host/Windows AVD work here; no separate physical run. Acceptance thresholds remain unchanged.

- Scope update 2026-10-10: backend-specific remaining milestones CANCELLED; Android/local/voice acceptance remains IN_PROGRESS. Direct AI integration is coordinated under T-018. Older backend dependencies and next actions below are historical.

- State: `IN_PROGRESS`
- Priority: owner requested only T-014-09 (Fold3 natural voice Chat acceptance); mobile agent owns Android/device execution and main agent owns integration evidence/status. T-014-06 Android voice/TTS lifecycle and T-014-08 resource/provider acceptance remain IN_PROGRESS. T-014-08 one-session provider-backed three-turn demo PASS after the owner raised local RPM 2→10; earlier timeouts and Android/resource/account checks remain open. See [evidence](evidence/T-014-08-provider-chat-2026-10-06.md). Stop after T-014-09; do not move to another subtask. Does not replace unfinished T-011 or device acceptance in T-003/T-006/T-009/T-010.
- Planning: `.agent/plans/active/T-014-free-form-voice-chat.md`.
- Detailed execution tracker: [T-014 subtasks](tasks/T-014-free-form-voice-chat-tasks.md). T-014-01 through T-014-05 and T-014-07 are DONE 2026-10-06; T-014-06 mobile voice/TTS wiring remains IN_PROGRESS. [T-014-07 evidence](evidence/T-014-07-host-chat-demo-2026-10-06.md) records a passing three-turn host demo through the real WebSocket client and deterministic backend fixture, with injected TTS callbacks. JDK17 Android test/lint/APK now pass; spoken exit during TTS remains open because current mic ownership is sequential and no echo-safe detector or physical validation exists. Owner keeps personal scope, current Gemini API-through-metered-proxy path and BRD $0/month target, and directed skipping provider-policy review. Deployed project/model/quota/billing are unverified; no compliance claim is made. Owner specifies one user/active session, one conversation ID per session, explicit start/end, and no action dispatch in Chat. Accepted bounds remain idle15 minutes, disconnect/Stop/Pause termination, fresh reconnect, 200 turns, one in-flight turn, 20 pairs/8,000 prompt tokens, 2,000 input code points and output1,024 tokens or4,000 code points. Backend and prior Android evidence remain linked in the tracker.
- Goal: add a conversational voice mode where the user and AI can exchange multiple turns in one session, retain bounded context, and have Android speak free-form AI responses through TTS, while keeping device-control actions on the existing guarded route.
- Acceptance:
  - Add an additive, versioned chat request/response protocol distinct from action execution; preserve existing V1 command clients and `DEVICE_COMMAND` policy.
  - Android creates/reuses a session ID for successive turns, supports new/end/timeout behavior, and presents connection, listening, speaking, cancellation, and error states truthfully.
  - Backend provides bounded, isolated session-only volatile context for chat turns and does not require a device tool call to answer. Release context on end/reset/expiry; do not restore it after app/backend restart or write Chat content to durable storage/logs/Command history. Lifecycle limits and identity isolation are explicitly decided before implementation.
  - Android receives assistant text and reads it with TTS; long responses, interruption, playback completion, follow-up listening, and errors cannot revive stale turns or overlap sessions.
  - Provide local time/time zone as explicit context when needed; test follow-ups such as “What time is it?” then “Tell a child-friendly story appropriate for this time of day.”
  - Chat-only turns cannot invoke device actions. Explicit supported device commands continue through current local-first routing and existing Android confirmation/capability checks.
  - Privacy, retention, sensitive-input handling, output limits, and provider failure behavior are documented; no raw audio is added to the backend protocol or history.
  - Backend/Android automated tests, protocol compatibility checks, builds/lint, and a runnable end-to-end voice chat demo pass; physical latency/resource limits are measured or recorded as pending.
- Constraints: preserve T-011 shared action semantics and legacy compatibility; do not persist conversation content beyond the owner-approved policy; do not treat backend conversation memory as durable across restart until designed and validated.
- Accepted routing (D-030, 2026-10-05): explicit separate Chat and Command modes; no automatic unmatched-command fallback into Chat. Chat cannot dispatch device actions; users explicitly switch to Command mode for device control.
- Accepted retention (D-030, 2026-10-05): Chat context exists only in the session in volatile memory, with no long-term Chat history. Existing Command history is unaffected; provider-side retention remains a separate policy question.
- Accepted voice UX (D-030, 2026-10-05): “Hey hey” then “Bắt đầu trò chuyện” enters Chat; following turns need no wake phrase, with listening reopened and cued after TTS. “Kết thúc trò chuyện” needs no wake phrase even during TTS, stops playback/pending work, ends the session, releases context and returns to Command wake-word standby. Include an on-screen stop control. Test local exit handling, TTS echo rejection and late callbacks; validate spoken interruption on hardware.
- Planning refinement2026-10-05: host/AVD milestones, proposed route/state/protocol/session limits and physical acceptance queue detailed in ExecPlan; no source implementation started.
- Remaining product/acceptance work: lifecycle and numerical bounds were accepted in T-014-02 and enforced for the backend in T-014-04. Android voice/TTS wiring is partial in T-014-06; the T-014-07 deterministic host demo passed with injected TTS callbacks. Long-response chunking, real TTS/mic voice demo, echo-safe spoken exit during TTS, provider-side data retention, deployed account/model/quota/billing and physical interruption remain unverified. Do not infer provider-compliance or end-to-end voice acceptance from this host fixture.
- T-014-09 Fold3 acceptance is `IN_PROGRESS`: mobile agent added an on-screen Chat end control; diff check PASS, but Android SDK/ADB were unavailable so JDK17 test/lint/APK and physical voice/resource/Command checks did not run. Spoken exit during TTS remains unsupported. See [T-014-09 evidence](evidence/T-014-09-fold3-voice-chat-2026-10-06.md); stop at this subtask.
- Fold3 rerun2026-10-06 supersedes the ADB-unavailable environment note for
  build/device access: current app/test APK built, original-cert installed and
  diagnostics ran. Active Chat turns, natural start/follow-ups, spoken exit
  during TTS, echo/interruption and Command regression were not exercised;
  wireless ADB disconnected during the T-003 Wi-Fi toggle. See
  [physical rerun evidence](evidence/Fold3-rerun-2026-10-06.md); remain
  IN_PROGRESS at T-014-09.


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

## T-019 — Chọn micro có tín hiệu giọng nói
- State: `CANCELLED` (2026-10-10; owner explicitly cancelled T-019). Remaining milestones and physical acceptance are withdrawn; existing prototype/evidence retained, not accepted or marked DONE. T-010 remains unfinished.
- Outcome: default to Fold3 built-in microphone; when multiple microphones are available, use the input carrying speech. Owner asks about simultaneous wired/USB/Bluetooth capture; establish actual supported routing rather than promise all-input concurrency.
- Acceptance: establish Android concurrent-input limitations; implement stable speech-aware selection where supported, safe fallback and hotplug/lifecycle cleanup; preserve wake/STT handoff, privacy and owner settings; automated tests/build/lint pass; runnable physical multi-mic verification documented, with unverified hardware behavior explicitly open.
- Publication checkpoint: existing Android prototype retained in `48d676d`; exact snapshot JDK17 test/lint/app+test APK PASS201tasks, Python6PASS. No physical acceptance or DONE claim; later concurrent edits excluded.
- Plan: [T-019](plans/active/T-019-speech-aware-microphone.md).
