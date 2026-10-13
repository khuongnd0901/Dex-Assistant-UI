> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Fold3 automated acceptance — 2026-10-08

Owner requested pending physical-device tests and selected automated tests only.
This extends existing tasks; natural speech, manual USB-mic press, live Chat
interruption and long natural-session/soak gates remain separate.

## Target and build

Verified Linux ADB target `192.168.1.18:39911`, Samsung SM-F926B,
Android 15/API35; Samsung desktop display2 observed. Android checkout
`/home/kunrg/Dex-Assistant-UI`, HEAD `2e7ecd7`; backend HEAD `efed7a2`.

Backend JDK25 `bash ./mvnw -B -ntp verify`: PASS78 tests, zero failures/errors,
1 expected skip; JAR built. Log `/tmp/fold3-20261008-backend-verify.log`.
Android JDK17 `test lint assembleDebug assembleDebugAndroidTest`:
PASS201 Gradle tasks, 5m13s; app156 tests per variant, zero failures/errors,
1 expected skip each. Log `/tmp/Fold3-20261008-android-build.log`.

Installed certificate verified as
`9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
Default build certificate differs; private artifact copies re-signed with
existing device-test identity and verified before replace installation.
No uninstall, app-data clear, role/permission change or Wi-Fi toggle authorized
by this run. Owner history/policy snapshots remain private in `/tmp`.

Isolated Java backend binds loopback8080 with public RAM-only test identity,
external quota DB `/tmp/fold3-20261008-quota/ai-quota`, and temporary ADB
reverse8081→8080. Ignored `.env` not edited; owner credential not provisioned.
Readiness HTTP200 UP; missing/invalid WebSocket bearer upgrades both HTTP401. Runtime log `/tmp/fold3-20261008-backend-runtime.log`.

## Results

Every invocation selected the physical serial and used `am instrument --no-restart`.

| Task/area | Actual result |
| --- | --- |
| T-011 ConfigCommand | 9 PASS,1 expected AOSP-only SKIP; 1,000-pattern/300-sample p95 0.370469ms. Maps public query DISPATCHED on display2; Maps+YouTube APP_PAIR_VERIFIED with disjoint bounds Maps[0,0,906,1020],YouTube[960,0,1920,1080]. |
| T-010 grid | Initial selected10-case run9PASS/1FAIL:100 prepare/clear expected READY but got SCREEN_CHANGED. Snapshot showed window topology6→5. Focused100-cycle rerun PASS1/1 in110.086s. Preserve initial failure; not100 natural sessions. |
| T-006 subtitles | Selected4 fixture cases PASS4/4. |
| T-016 dashboard | Physical Compose viewport fixture PASS1/1, including fill-only sample selection. Production MainActivity resumed on display2,freeform[60,60,1873,1080],240dpi/1209×680dp, HAS_DRAWN/on-screen/visible. |
| Voice runtime | Native wake model load/silence/release, Vietnamese recognizer resolver, command endpoint hints and bounded Vietnamese TTS diagnostic PASS4/4. No speech recognition accuracy claimed. |
| T-015 YouTube preview | FAIL expected VERIFIED, actual REJECTED; focused rerun same failure. Read-only semantic observer PASS but found0cards; observation success is not preview acceptance. |
| T-003/T-009 real backend | Isolated CoordinatorBackendDeviceTest FAIL expected DISPATCHED, actual APP_PAIR_WINDOW_MISSING; one bounded rerun same failure. Runtime used real gemini,ai-proxy profiles. Terminal originates pair layout verification; provider action params/causal root cause not established. |
| T-006 direct mic diagnostic | FAIL at obsolete fixture assertion “Busy STT must reject a second trigger”. T-006 now intentionally accepts second trigger during TRANSCRIBING to submit partial. This assertion mismatch does not establish a runtime defect or current mic acceptance. |

Original and focused rerun logs: `/tmp/Fold3-20261008-{config,grid,subtitle,dashboard,voice,youtube-preview,youtube-observer,backend,hardware}.log` and `*-rerun.log`.
Initial33 cases:28PASS/4FAIL/1expectedSKIP. Focused4 reruns:2PASS/2FAIL.
Resource rerun100cycles:prepare p50/p95=671/722ms,max736ms;
PSSinitial/final194308/199942KB,peak sampled210416KB;CPU65416ms,
elapsed107628ms. Not natural speech/end-to-end latency or soak acceptance.
Final voice health LISTENING/HeyHey offline/TTS_VI_OFFLINE_READY. The TTS
fixture reports synthesis outcome and does not itself guarantee audible speech.
App SHA256 `866bfa94b642e92110ab815a8aefbfab0fc4164b766be0b5ca9fd307895c8aab`;
test SHA256 `eb7a5b4df02ea9177dd25bcf98e063498cd6e56ab3fdf1eff8d537be07d5dee8`.
Detailed Android evidence:
[Fold3-automatic-2026-10-08.md](Fold3-automatic-2026-10-08.md).

No production source changes or test assertion repairs were made. Failures remain
follow-up work within their existing tasks; do not mark those tasks DONE.

## Preservation and cleanup

Owner voice policy readback byte-identical. History remained55 records with
identical IDs/timestamps; ciphertext differs because history reads persist and
re-encrypt. This is not a byte-identical history assertion or proof of every
plaintext field. No owner key removal reported; Accessibility enabled/bound.
Temporary ADB reverse8081 removed and isolated Java PID4014 stopped after final
backend result. No owner endpoint or credential configuration changed.


## Limits

T-017 Sherpa benchmark prerequisites remain absent: selected model/trusted
hashes, cleared rights and consented corpus. Current Android main checkout
has no benchmark runner; PR CI does not establish native ASR on Fold3.
Wi-Fi-off acceptance deferred while sole ADB transport uses Wi-Fi.
Initial battery98%,30.2C; thermal status0/AP31.6C. Single baseline snapshot is
not battery/thermal performance acceptance.
