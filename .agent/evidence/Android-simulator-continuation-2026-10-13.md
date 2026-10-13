> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Android continuation — Windows simulator through WSL, 2026-10-13

Owner requests continuation of open Android tasks and one consolidated physical Z Fold3 campaign. Physical execution is now owned by [T-020](../plans/active/T-020-consolidated-fold3-acceptance.md). This session used Windows AVD only, no Fold3 commands/install, no real provider call or quota use.

## Changes and provenance

- T-007: selector optional `dex|foldable --emulator-only` bypasses Linux/Windows physical-device selection and phone property queries. Default Fold3 preference remains. Added connected-phone exclusion/argument/default regression tests; documented simulator invocation.
- T-014/T-018: session idle starts at Chat open; main-handler timeout ends/clears context without another user input, including before first turn. Default remains15min. Successful input/reply rearms expiry; explicit end removes expiry. Voice service receives an internal automatic-expiry hook for recognition/TTS/epoch cleanup and wake recovery, without duplicating explicit-end teardown.
- T-014/T-018: End Chat explicitly cancels the active OkHttp Call as well as Future. Cancellation cannot trigger another provider; interrupted provider iteration rejects work. Epoch/session/request guards continue to suppress late replies/errors. Internal client/credential/short-idle injection permits deterministic fixtures; production defaults and credential aliases remain unchanged.
- Added9offline instrumentation tests: Chat and Command HTTP429/401/403/408/503 exact mapping/single-request; disconnected transport; confirmed in-flight late-result rejection/restart; underlying HTTP cancellation/prompt restart; no cancellation fallback; automatic idle end/restart/reentrant cleanup; UUID-scoped preferences/Keystore isolation. Only placeholders enter mocked application interceptors; no `proceed` or live HTTP.
- T-017: restored15missing instrumentation/tool/doc/template files from retained Git revision `e5958b907574e147fe6a7151df8863dd73ede5b4`, plus provenance and3new dataset guards. No production STT/build/CI changes, model weights or private audio copied. This is the12-prompt pilot, not the expanded200 collector. Expanded source recorded at another host is unavailable locally. Remote check failed because configured `/tmp/dex-gh-cli/bin/gh` helper no longer exists; anonymous check requires authentication. No Git credential settings altered. Never resume/replace the retained7/200 private session with this pilot.

Android base HEAD: `48d676d350daa521d51aa23974aefc8912213e9a`, dirty checkout with pre-existing modifications retained. No commit/push, backend source change, reset/uninstall or signing/owner/grant mutation performed in this continuation.

## Validation executed

From canonical repository:

```sh
python3 -m unittest discover -s scripts/android/tests -v
bash -n scripts/android/select-test-target.sh
bash scripts/android/select-test-target.sh dex --emulator-only
```

PASS11tests (8provisioning,3selector),16.230s. Selector reports `TARGET=AVD ADB=windows PORT=5038 PROFILE=dex AVD=DeX_Command_Test_API35 SERIAL=127.0.0.1:5555 API=35`. Confirmed `ro.boot.qemu.avd_name` separately. Windows starter reported boot complete and1920x1080/160dpi freeform. No Samsung category.

From Android repository, JDK17 and Linux SDK:

```sh
env JAVA_HOME=/home/khuongnd/.local/opt/jdk17 ANDROID_HOME=/home/khuongnd/Android/Sdk ./gradlew test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1 --no-watch-fs
python3 -m unittest discover -s scripts/benchmarks -p 'test_*.py' -v
```

Final source PASS201Gradle tasks in6m3s (27executed/174up-to-date); app191tests each debug/release,0failures/errors/1expected skip each; DPC5each PASS. ASR host PASS15in0.055s, including total200 without group coverage rejection, orphan WAV and truncated WAV rejection. Earlier preparatory build PASS201in9m21s. First lifecycle compile failed recursive Kotlin inference for the idle runnable/endChat; explicit types corrected it before final PASS. No unverified compile failure hidden.

Installed final main/test APKs in place with `adb.exe -P 5038 -s 127.0.0.1:5555 install --no-incremental -r` and Windows paths `D:\dex-assistant-ui\app\build\outputs\apk\debug\app-debug.apk` / `D:\dex-assistant-ui\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk`; both Success. Existing AVD app and built APKs have original certificate SHA256 `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`. No uninstall/reset/provision flow.

Instrumentation: exact serial above, `shell am instrument -w -r -e class CLASS_LIST com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner`. CLASS_LIST consists of the following fully qualified classes/methods under `com.khuongnd.dexassistant.ui`:

- `DirectAiFaultDeviceTest`
- `DashboardLayoutTest`
- `ConfigCommandDeviceTest`
- `SherpaCorpusPcmTest`
- `SherpaDryRunScoreTest`
- `VoiceRuntimeDeviceTest#pinnedNativeModelLoadsDecodesSilenceAndReleases`
- `VoiceRuntimeDeviceTest#commandRecognizerEndpointHintsKeepVietnameseAndFinalResultContract`
- `VoiceRuntimeDeviceTest#commandRecognizerUsesVietnameseOnlineAllowedAndExcludesOurStub`
- `YoutubeSearchDeviceTest#searchIntentUsesPackageBoundResultsDeepLink`
- `NumberedScreenDeviceTest#numericAndMalformedFamiliesNeverBecomeBackendCommands`

Result: runner `OK (30 tests)` in18.01s, **26PASS/4assumption-SKIP/0FAIL**; count only terminal class/test statuses, not custom metadata status0 frames.

| Suite | PASS | SKIP |
| --- | ---: | ---: |
| Direct AI faults/cancel/idle/isolation | 9 | 0 |
| Dashboard layout/tap-only examples/Run | 1 | 0 |
| Config local engine/safety | 7 | 3 |
| ASR PCM/WAV guards | 2 | 0 |
| ASR parser scoring | 2 | 1 |
| Native KWS silence + Vietnamese intent/resolver | 3 | 0 |
| YouTube results intent | 1 | 0 |
| Numeric/malformed local routing | 1 | 0 |

Three Config skips: selected-desktop semantic target, physical Maps address and physical Maps+YouTube pair. One ASR skip: real private-corpus scoring (no opt-in/artifacts). These are not accepted. AVD matcher p95=1.621113ms/1,000patterns/300warm samples (`fold3Target=false`); external recognizer `com.google.android.tts/com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService`. No acoustic accuracy/real speech timing claim.

New isolation regression verifies owner provider keys/settings/voice preferences unchanged during/after mocked fixture setup and original alias presence preserved. Credential aliases and preference namespaces are UUID-scoped; credential aliases are deleted and fixture preferences cleared in cleanup. History fixtures use separate non-production aliases and clear their records. This narrower assertion is not a full physical postflight. Dashboard/control/pure parser fixtures do not establish actual Samsung display behavior.

## Candidate identities

| Artifact/source | SHA256 |
| --- | --- |
| app-debug.apk | `42ac63c86a1a66649558e7497af7c90a0f07295e45b0141a252f8e57860dd006` |
| app-debug-androidTest.apk | `333e2f3a4157390deab4700b4fb9f1e83e852357cef85953747e026d03c9aef3` |
| AssistantController.kt | `fdaec043d3c1ac988792585e10041f03e2e0d396b581b31daf74b1036be6e2ac` |
| DirectAiChat.kt | `d859288480fdd983065fa26a1bd68747ea66d04598a570348f5866a22567b0b4` |
| AiCredentialStore.kt | `ed9c4a87785d1b9be707124dcdecde97131f6af810f71961361dd8f02507d457` |
| VoiceAssistantService.kt | `6b5eb7c33a90c2285339fbe135b24a6ca6af0c0a9782f3dd76b147773477d109` |
| DirectAiFaultDeviceTest.kt | `74c7d948ebbc24a611953b1a9c069a9a4d6a1353a9d94f2df47a378c1aa2dc92` |

Generated APKs/raw logs/private data remain outside Git. Local build/test logs: `/tmp/android-continue-final-build.log`, `/tmp/dex-simulator-instrumentation.log`, `/tmp/t017-host-tests.log`, `/tmp/dex-selector-tests.log`; temporary paths are not durable acceptance evidence themselves. This sanitized report preserves outcomes.

## Remaining readiness and physical gates

T-014 still cannot hear spoken End Chat during TTS: recognizer starts only after playback. Concurrent capture/echo safeguards require further design and hardware validation; new timer/cancel tests do not implement this feature. Actual automatic-expiry mic/wake recovery still needs physical evidence.

Recover expanded200 collector before expanded corpus continuation; live ASR A/B, full authentic corpus, cold load/stability and distribution-rights conclusion remain open. Google production STT retained. Current restorable pilot is explicitly distinct.

All natural wake/USB/voice/TTS/selected Samsung display/grid/app targets/pixel review,30natural samples per active route,100natural sessions,radio cycles with independent connection and8hsoak are tracked solely by T-020 F01–F12. Parent tasks remain IN_PROGRESS. Full device-campaign readiness is not yet asserted.

Final documentation/local-link and changed-file whitespace checks PASS in both repositories. Full Android workspace whitespace check reports pre-existing tracked Windows JVM crash/replay logs with trailing whitespace; those unrelated files were preserved. No generated log was added or committed by this continuation.

## Owner-authorized source publication — 2026-10-13

Android source commit `3f2bea85cd7dd8215639ae2080f0d2d856e738a9` and canonical source/task/tooling commit `7d3611e0f505d4750e34f0c47fd873312fb1efb4` pushed to `origin/main`; `git ls-remote origin refs/heads/main` equals corresponding local HEAD at verification. A canonical verification-note commit follows. Application/fault source hashes match the tested candidate; staged whitespace/credential scans PASS. Original Git file modes retained; mixed existing line endings preserved in index to exclude unrelated EOL churn. Existing EOL-only changes and JVM logs remain local; logical unstaged diff is empty in both repositories. No secret/generated model/audio/APK/log included. Physical acceptance remains open. Credential helper restored/authenticated; remote benchmark branch remains e5958b9/pilot only.
