> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-020 — Consolidated Z Fold3 acceptance

## Outcome and boundaries

Owner instruction 2026-10-13: continue Android work using Windows simulators through WSL; consolidate all required physical Z Fold3 tests into one task and one scheduled campaign. T-020 owns execution and evidence for the physical gates of T-006/T-010/T-011/T-014/T-015/T-016/T-017/T-018 and the T-021 performance/RAM follow-up. Original tasks keep implementation responsibility and remain IN_PROGRESS until their applicable gates pass. No backend acceptance, cancelled T-008/T-019 work, cloud teardown, or live calls/SMS.

“One campaign” means one prepared build/preflight, combined interactive cases, unattended soak and one postflight. It does not reduce the 200-utterance corpus, 100 natural sessions, 30 samples per active route or eight-hour soak. Failures require repair and focused retest; never promise one run will pass.

## Inputs and constraints

- [Task board](../../TASKS.md), [status](../../STATUS.md), existing active plans and their retained evidence govern acceptance; this plan changes test scheduling only.
- Android-only direct Gemini API/provider path remains current. No server/WebSocket prerequisites. Additional providers conditional on owner configuration.
- During preparation, select `dex --emulator-only` or `foldable --emulator-only` with [selector](../../../scripts/android/select-test-target.sh). Every ADB command uses confirmed emulator serial and Windows port 5038. Do not run physical tests now.
- Preserve original signing certificate, Device Owner, Assistant role, Accessibility consent, credentials, voice policy and history. No uninstall/reset/data-clear or owner grant changes.
- Private consented audio/reference corpus stays outside Git. Capture only redacted metadata. Owner participation is required for real speech, USB presses and visual acceptance.

## Decision questions and evidence

| Question | Evidence and objective rule |
| --- | --- |
| Is the build ready for one campaign? | Exact source/APK identity; host test/lint/app+test APK PASS; emulator regression results and known limitations recorded. Unsupported production behavior must be identified before device scheduling. |
| Can one run cover several tasks? | Label each natural trial with route/action/session IDs and linked gate IDs; reuse only evidence actually satisfying each gate. Injected callbacks never count as natural speech. |
| Can radio-off tests preserve access? | Independent connection verified before Wi-Fi/network-off tests; otherwise SKIP with reason and leave gate open. Wireless ADB alone is insufficient. |
| Is spoken exit during TTS implemented safely? | Implementation/design readiness checked before scheduling; actual capture/echo and late-callback proof required. Existing sequential mic/TTS and UI End Chat do not satisfy spoken interruption. |
| Can Sherpa be recommended? | Authentic full-corpus coverage, cold-load/live A/B, accuracy/intent, runtime/resource/stability and model distribution-rights conclusion; upstream or pilot clips alone insufficient. |

## Milestones

1. **Preparation on host/Windows AVD**: implement remaining simulator-feasible regressions; run JDK17 host checks and explicit emulator suites; record unresolved source/design issues. Freeze one candidate build only after readiness passes.
2. **Single device preflight**: confirm SM-F926*/API35, selected Samsung DeX display, matching app/DPC signing and retained user state. Install in place only if required; never reprovision existing owner device. Prepare safe desktop fixture and collectors once.
3. **Interactive combined campaign**: execute the matrix below, share harmless prompts/measurements where valid; retain first failures and SKIPs. No deferred hardware gate silently removed.
4. **Unattended measurements**: run eight-hour soak with safe local operations and resource/thermal capture; no quota-exhaustion loops. Natural counters count real accepted sessions only.
5. **Single postflight/report**: restore altered test configuration, verify retained signing/data/roles/grants/voice policy and compile one PASS/FAIL/SKIP report with build identity. Update each parent task from its own evidence. T-020 DONE only when every required applicable gate passes.

## Physical acceptance matrix

| Gate | Parent | Combined cases and completion evidence |
| --- | --- | --- |
| F01 | T-006/T-010 | Nearby Hey Hey first/repeated wake, actual mic capture/rearm and ready cue; metadata diagnosis of reported no-response. State LISTENING alone is not proof. |
| F02 | T-006 | USB/mouse second press correlated with raw key and manual-submit epoch; nonblank partial dispatch exactly once, EMPTY dispatch zero, late finals ignored. Bluetooth wheel transport when available, otherwise explicitly unverified. |
| F03 | T-006/T-010 | Natural STT partial/final subtitle timing/retention; selection/confirmation/cancel lifecycle; real DeX resize, display disconnect/reconnect and stale-coordinate fail-closed behavior. |
| F04 | T-010 | Natural grid/labels/subdivision/tap/cancel, popup/YouTube target safety and 100 natural sessions with measured latency/resources. Synthetic prepare cycles do not count. |
| F05 | T-011 | Natural local catalog/Maps/media/browser/app-pair/context routing, selected display and actual window bounds, confirmation/cancel/timeout/revalidation. Physical matcher p95 <=5ms/1,000 patterns plus CPU/RAM/battery. No real calls. |
| F06 | T-015 | YouTube search/title+artist/card indexes and numbered selection; reject/stale preview regressions, semantic target revalidation, supported local actions, Vietnamese STT/TTS and all seven latency metrics. Window-close/minimize stay unavailable until scoped executor proven. |
| F07 | T-016 | Owner visual review on Fold3 folded/unfolded and Samsung DeX: pixels/clipping, main controls/transcript/catalog access, fill-only examples and explicit Run. AVD layout PASS is supporting evidence only. |
| F08 | T-014/T-018 | Real three-turn Vietnamese Chat with TTS then automatic listening without wake; start/end while listening/waiting/speaking; confirmed in-flight UI End Chat, no late TTS/action/context revival, echo-safe spoken exit during TTS and Command regression. Unsupported spoken-exit remains FAIL/open. |
| F09 | T-018 | Direct Gemini harmless simple/compound Command, local cloud-off and text/voice Chat; invalid credential/restoration, timeout and no replay. 429/auth/fault injection host/AVD evidence reused; do not exhaust live quota. Actual network-off and Wi-Fi off/on only with independent connection. |
| F10 | T-006/T-014/T-015/T-018/T-021 | >=30 natural samples per active local/direct Command/direct Chat route; speech/recognizer/provider/action/TTS stage timing, p50/p95, errors and CPU/PSS/thermal/battery. Confirmation wait reported separately; reuse valid F04 samples. For T-021, preserve baseline/candidate source/APK identities and run matched A/B blocks in this campaign; acoustic endpoint, app/provider/first-response timing and RAM/allocation deltas must be distinguished. |
| F11 | T-017 | Resume consented 200-slot real Vietnamese corpus (retained 7/200 expanded takes), quiet/noisy coverage and actual references; cold-load WAV decode, Google/Sherpa non-executing live A/B, WER/CER/intent/slot/RTF and 100-session reliability. No production STT migration. |
| F12 | T-006/T-021 | Eight-hour soak and background/boot/package/process recovery; mic FGS, stop/pause/resume, truthful health and resources, no unexpected replay. Keep existing user data/roles/grants; T-021 also checks bounded session resources, threads/FD and memory after equivalent idle cleanup. |

T-021 planning follow-up (2026-10-13): [performance/RAM plan](T-021-android-response-performance-ram.md) adds matched baseline/candidate measurement within this campaign; it does not start a separate physical run. Preparation retains the baseline artifact, metric tooling and candidate before device scheduling. Natural baseline trials do not count toward candidate reliability gates; valid candidate trials may satisfy multiple existing gates. Owner subsequently authorized T-021 implementation in priority order, Windows simulator first; T-021 is IN_PROGRESS with synthetic AVD preparation. All Fold3 acoustic/DeX/natural/resource acceptance remains deferred here, and no new physical result is claimed.

T-021 preparation now verified: [evidence](../../evidence/T-021-windows-response-performance-2026-10-13.md) records full exact-source201-task Android checks, safe Windows31PASS/4SKIP and three alternating A/B pairs. Command synthetic p95/allocation and discovery improve; wake allocation improves but native timing is INCONCLUSIVE. Final candidate and baseline APKs retained for future physical comparison. No Fold3/natural/PSS/soak gate is completed, and owner explicitly defers physical work.

## Validation

Preparation: Android JDK17 `./gradlew test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1`; relevant Windows AVD instrumentation with exact verified serial; canonical `python3 -m unittest discover -s scripts/android/tests -v`, shell syntax and CRLF-aware diff checks. Evidence records executed commands/results, never proposed commands as PASS.

Device report records gate ID, parent task, candidate build/APK identity, measured outcome and evidence path. Missing voice/USB/radio/visual/resource evidence stays OPEN/SKIP; do not mark parents DONE from compilation.

## Risks and escalation

Samsung display/mic/OEM restrictions are not reproducible on AOSP AVD. One combined campaign requires owner availability and enough time for corpus and soak. TTS interruption requires implementation readiness before hardware acceptance. If no independent device link, defer destructive-to-connectivity cases. Do not alter settings, mic thresholds, models, signing or provider commitments to manufacture a pass.

## Progress and findings

- [x] Owner scheduling instruction recorded 2026-10-13 and all physical gates assigned to this task.
- [x] Current continuation validated: host201-task build/test/lint/APKs; tooling11PASS; ASR host15PASS; Windows AVD26PASS/4SKIP/0FAIL in18.01s. [Evidence](../../evidence/Android-simulator-continuation-2026-10-13.md).
- [ ] Full candidate readiness: echo-safe spoken exit during TTS and expanded200 collector recovery remain open.
- [ ] Candidate freeze and physical scheduling with owner.
- [ ] F01–F12 measured on actual device.
- [ ] Postflight, evidence reconciliation and parent closure.

Preparation found two simulator-feasible Chat defects: idle clock did not start until first input, and Future cancellation did not cancel the HTTP Call. Fixes now include automatic teardown and a voice-service idle-expiry cleanup hook; host exact-boundary and AVD idle/cancel/restart/isolation tests PASS; actual mic/wake recovery remains physical. Restored T-017 pilot harness is pinned to retained revision e5958b9. Expanded200 collector is unavailable locally, and publication follow-up restored the missing credential helper; remote branch remains e5958b9/pilot only. Restore the later collector from its original host before expanded collection.

No physical test is performed in this continuation. Previous evidence remains historical; counters are not reset or artificially completed.

## Decision and requirement updates

No product acceptance threshold or architecture changes. Update TASKS/STATUS and parent plan scheduling references; keep D-036 Android-only direction. A future accepted STT/runtime or mic/TTS architecture change requires its existing parent plan and decision record.
