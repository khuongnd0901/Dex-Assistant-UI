> Historical canonical handoff before workspace migration. Current authority: [Android STATUS](../../STATUS.md). Backend development is retired.

# Current status

Updated 2026-10-13 — continue Android tasks using Windows simulator through WSL; all real Z Fold3 tests are consolidated in **T-020 IN_PROGRESS**, [one physical campaign](../../plans/active/T-020-consolidated-fold3-acceptance.md). Android directly calls provider APIs; no backend runtime/WebSocket requirement (D-036).

## Migration priority — T-022

Owner retires backend development and requests future development/test from Android workspace. [T-022](../../plans/completed/T-022-android-canonical-workspace.md) IN_PROGRESS migrates canonical memory/docs and required test tools to `/mnt/d/dex-assistant-ui`; old backend entry points will become redirects. T-021 and other unfinished gates remain unchanged. Migration is documentation/tooling only; app/DPC/Gradle/source/APK/signing/device/settings/infra remain untouched. Next: inventory/link closure/collisions, transfer and validate standalone Android workflow.

## Current planning priority — T-021

[T-021](../../plans/active/T-021-android-response-performance-ram.md) remains **IN_PROGRESS**: requested simulator M0–M3 completed; owner defers all Fold3 work. [Verified evidence](../../evidence/T-021-windows-response-performance-2026-10-13.md) records commands/source/APKs, comparison and limitations.

- Latest frozen20source/test files: JDK17 test/lint/app+test APK PASS201tasks/6m3s; app200tests per debug/release,0failures/errors/1expected skip; DPC5eachPASS. Safe Windows API35 suites31PASS/4Samsung-or-private-corpusSKIP/0FAIL,8.04s; canonical19testsPASS.
- Three alternating baseline→candidate pairs (12benchmark test executions,1080Command/discovery trials and4500native frames per variant) confirm synthetic Command p9543.66–52.93%lower and fixture allocation45.85–45.95%lower; discovery96.79–99.30%lower; wake allocation80–84%lower. Native wake timingINCONCLUSIVE (−1.17%to+4.49%), no speed gain accepted. No acoustic/live-provider/Fold3/idle-PSS improvement inferred.
- Android changes: stage/original-route/status/action/HTTP/Chat metrics; invalidated recognizer catalog reuse; bounded serialized schemas/request-local first-key reuse; bounded body readers; worker PCM reuse/scrub; HTTP cancellation and exact Android JSONException mapping. Key rotation/deletion/fallback freshness, invalid output, queued/late cancellation and no fabricated screen dispatch all pass. Production STT/hints/model/timeouts/confirmation/privacy/retention/settings/signing/data/roles/grants preserved. Dashboard/VoiceRuntime class fixtures omitted because they touch owner policy/history; isolated suites cover affected behavior.
- Final matching-cert candidate installed on exact Windows AVD serial127.0.0.1:5555/port5038. Pulled app SHA c434bfeb…c0d15d and test13583c48…783f7c match saved APKs; source freeze matches. Raw artifacts remain outsideGit; no commit/push or physical/provider call. Final root link/plan/ID/source/APK/changed-whitespace checks PASS (81local links); pre-existing unrelated dirty files retained.

Next: all actual acoustic/STT/USB/TTS/rearm/Chat interruption, Samsung selected-display/DeX, production CPU/PSS/thermal/battery,30natural samples per active route/candidate,100natural candidate sessions and8hsoak through T-020 when owner resumes physical work. M4 R8/Profile tuning deferred until startup/ART evidence justifies it. Existing task gates remain open; earlier continuation/publication below is historical.

## Verified continuation

[Sanitized evidence](../../evidence/Android-simulator-continuation-2026-10-13.md) records exact changes, source/APK identities, commands and limits.

- T-014/T-018: fixed idle start before first Chat turn, automatic15min session/context cleanup, voice-service automatic-expiry cleanup hook, explicit active HTTP cancellation and no cancellation fallback. Existing late-result epoch guards retained. Default credentials/signing/provider/session bounds unchanged.
- Final JDK17 `test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1 --no-watch-fs` PASS201tasks/6m3s. App191tests each debug/release:0failures/errors/1expected skip each; DPC5eachPASS.
- Windows API35 `DeX_Command_Test_API35` through ADB5038:26PASS/4assumption-SKIP/0FAIL,18.01s. All9new direct Chat/Command faults/idle/cancel/restart/isolation fixtures PASS; dashboard1, config7, ASR PCM2+parser2, native silence/Vietnamese intent/resolver3, YouTube intent1 and numeric routing1 PASS. Three actual-DeX cases and one real-corpus scoring case SKIP. AVD matcher p95=1.621113ms/1,000patterns, not Fold3 performance.
- T-007: `--emulator-only` selector skips physical devices/queries on Linux and Windows. Actual matching AVD selection PASS; tooling suite11PASS. Default selector behavior retained.
- T-017: restored missing12-prompt pilot harness/tools/docs from pinned e5958b9, added3corpus validation guards; host15PASS. No models/audio/runtime/build/CI copied or changed. Expanded200 collector is unavailable locally. Credential helper has now been restored and authenticated; remote benchmark branch remains pinned e5958b9 (pilot only). Pilot must not replace/resume the retained expanded7/200 session.

Only matching-cert app/test APKs updated in place on Windows AVD. No physical device test/install, reset/uninstall, role/grant/owner change, live provider call or cloud operation. Owner-authorized source publication completed2026-10-13 on both main branches; remote SHAs verified against local HEAD with git ls-remote. Preserve both repositories' substantial pre-existing modifications. Android source commit3f2bea8, based on48d676d; exact candidate hashes in evidence. Temporary logs/APKs remain outside Git. Current links/changed-file whitespace checks PASS; full Android workspace check flags pre-existing tracked JVM crash/replay logs, preserved without edits.

## Remaining work and scheduling

T-006/T-010/T-011/T-014/T-015/T-016/T-017/T-018 remain IN_PROGRESS. Their physical execution belongs only to T-020 F01–F12; original acceptance thresholds stay unchanged. Current host/AVD work passed, but full physical-campaign readiness is not yet asserted.

Next implementation/readiness: design echo-safe spoken End Chat during TTS (currently unsupported because recognizer resumes after playback); recover expanded200 ASR collector before full corpus continuation. Actual automatic-expiry mic/wake rearm remains a physical gate. Scoped window close/minimize/switch and notification reading remain unavailable/unimplemented where previously documented; no capability enabled from AVD evidence.

Then one prepared campaign: nearby/repeated Hey Hey and reported no-response diagnosis, actual USB second press/EMPTY, natural STT/TTS/Chat interruption, selected Samsung DeX resize/disconnect/grid/targets/catalog/app pair, Fold3/DeX pixel review, direct Gemini real routes/errors and radio cycle with independent connection,30natural samples per active route,100natural sessions, authentic ASR corpus/live A/B/cold-load/stability and8hsoak. No synthetic counter completes a natural gate; failures require repair/focused retest.

Retained baselines: T-010 last natural attempt accepted0sessions, automatic4PASS; T-01812automatic physical cases PASS plus Gemini-only3case repeat at15RPM; T-017 expanded7/200 with105decodes, earlier12-recording pilot separate. See existing task evidence for historical results; owner data/settings/roles/signing retained and must be checked at future pre/postflight.

T-003/T-009 CANCELLED as backend-dependent outcomes; T-008/T-019 CANCELLED by owner. No renewed implementation/acceptance for cancelled tasks. Backend historical DONE evidence retained. Current provider scope Gemini only; additional provider acceptance conditional future work. Production STT Google; Sherpa recommendation/distribution-rights conclusion still open.

## Source publication checkpoint — 2026-10-13

Android source `3f2bea85cd7dd8215639ae2080f0d2d856e738a9` and canonical source/task/tooling `7d3611e0f505d4750e34f0c47fd873312fb1efb4` pushed to origin/main and independently verified. This verification-note commit follows the canonical source checkpoint. Snapshot source hashes match the tested candidate; staged whitespace/credential-pattern checks PASS and new-file modes corrected to retained Git modes. Existing EOL-only worktree changes and JVM logs kept local; no generated APK/model/audio/key/log added. No functional unstaged changes remain. T-020 and parent acceptance tasks stay IN_PROGRESS.

Git credential helper restored from checksum-verified official gh2.65.0 archive; authenticated existing account without credential-config changes. Remote T-017 branch confirmed e5958b9/pilot only; expanded collector still requires original-host recovery.
