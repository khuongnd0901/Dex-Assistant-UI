# Android current status

Updated 2026-10-13. Canonical tasks/acceptance are in [project status](../../DeX-Assistant/.agent/STATUS.md) and [T-020](../../DeX-Assistant/.agent/plans/active/T-020-consolidated-fold3-acceptance.md). Owner requests Windows simulator tests through WSL; all physical Z Fold3 execution belongs to one T-020 campaign. T-006/T-010/T-011/T-014/T-015/T-016/T-017/T-018 remain IN_PROGRESS.

## Verified source and simulator continuation

- Chat fixes: idle clock starts on session open; automatic15min teardown releases context before any first turn and after activity. Voice service uses an internal idle-expiry hook for cleanup/wake recovery. Explicit End Chat cancels Future and underlying HTTP Call, blocks cancellation fallback and keeps epoch-based stale-result rejection. Production default provider/settings/credential aliases/limits preserved.
- Added9offline transport/lifecycle/isolation fixtures with UUID-scoped preferences and credential aliases. No real key or provider request. Both Command and Chat error paths covered; confirmed in-flight cancellation, immediate fresh turn1, automatic expiry and reentrant teardown pass.
- JDK17/Linux SDK `./gradlew test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1 --no-watch-fs` PASS201tasks/6m3s. App191tests each debug/release,0failures/errors/1expected skip each; DPC5eachPASS. First recursive Kotlin-inference compile repaired before final PASS.
- Windows `DeX_Command_Test_API35`, API35, port5038, serial `127.0.0.1:5555`:26PASS/4assumption-SKIP/0FAIL,18.01s. Dashboard/config/local/numeric/YouTube intent/native-silence/Vietnamese intent/resolver and ASR pure fixtures pass. Actual-DeX semantic/Maps/pair and private real-corpus scoring are skipped, never accepted. AVD matcher p95=1.621113ms/1,000patterns.
- Main/test APKs updated in place on AVD only, original certificate9ddb7f47…abd83d03 retained. No reset/uninstall/provision/grant/role/signing changes, Fold3 command/install or live Gemini call. Owner authorized publication of this validated snapshot on main. See canonical project status for commit and remote verification. Existing dirty changes preserved.
- T-017 missing pilot-only benchmark source/tools/docs restored from pinned e5958b907574e147fe6a7151df8863dd73ede5b4; new corpus guards and retained host tests PASS15. No model/audio/production STT/build/CI changes. [Provenance](../docs/benchmarks/sherpa-vi-zfold3/RESTORE_PROVENANCE.md).

Exact commands, hashes, outcomes and limitations: [canonical evidence](../../DeX-Assistant/.agent/evidence/Android-simulator-continuation-2026-10-13.md). Logs/APKs remain temporary/ignored, not committed.

## Next action and gates

Resolve echo-safe spoken End Chat during TTS: existing mic ownership is sequential and recognition starts after TTS. Current idle/cancel fixes do not implement spoken interruption. Actual automatic-expiry microphone/wake recovery needs physical proof.

Recover expanded200 collector source recorded on another host before resuming retained7/200 session; current restored collector is the12-prompt pilot. Credential helper /tmp/dex-gh-cli/bin/gh restored from checksum-verified official release; GitHub authentication succeeds. Remote benchmark branch remains e5958b9/pilot only. No Git credential config changed. Production STT remains Google; full corpus/live A/B/stability and model-rights conclusion open.

Then schedule T-020 once full readiness is established: all natural mic/wake/USB/TTS/Chat, Samsung selected-display/grid/semantic/app targets, pixel acceptance, real provider/radio behavior,30samples per route,100natural sessions, ASR real corpus and8hsoak together. Never run broad legacy LocalFirstDeviceTest against owner history or turn off sole wireless ADB. Use canonical selector `dex --emulator-only` during preparation.

T-008/T-019 stay CANCELLED; no further microphone-selection or release-automation work. Prior physical sessions in older status are historical; rediscover device runtime/preflight at T-020. No backend runtime dependency. Preserve owner voice policy, history, original signing, Device Owner/Assistant role/Accessibility and grants.
