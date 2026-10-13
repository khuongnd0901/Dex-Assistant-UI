> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-006/T-009 hybrid voice implementation — 2026-10-02

## Scope and current boundary

Owner approved D-016 and implementation: offline English Hey DeX wake,
online-allowed Vietnamese Android SpeechRecognizer, whole-input local/backend
routing and Vietnamese feedback. Owner confirms no Fold3 is available in this
session; only retained API35 Windows AVD acceptance is attempted. Physical wake,
Vietnamese accuracy, screen-off/boot, CPU/PSS/battery/thermal,100-command and8h
soak thresholds remain open. Do not mark T-006/T-009 DONE from builds or injected
text. Credential audio may reach system STT; credential text remains guarded
from application backend/LLM, redacted history/logs; provisioning not added.

## Artifact provenance and verification

- Runtime v1.13.8 AAR:50,129,134 bytes; SHA256
  `633c24321e06b1fe79feafa03ea16cbc0f8a286641e2da3559bac91bdb13bd96`.
  Official release metadata supplies the same digest. Contains arm64-v8a,
  armeabi-v7a,x86,x86_64 native libraries.
- English GigaSpeech KWS3.3M2024-01-01 archive:17,626,723 bytes; SHA256
  `f170013b4716e41b62b9bfd809687c207cef798ef9bc6534d524e17af9b6561a`.
  Original README declares Apache License2.0; ModelScope upstream API returns
  the same declaration. Selected int8 encoder4,807,159/decoder277,985/joiner
  163,380 bytes plus tokens. Neither upstream test WAVs nor BPE tooling bundled.
- ONNX Runtime native dependency version1.28.2 confirmed by pinned Android
  CMake configuration and x86_64 binary version string; MIT LICENSE and
  ThirdPartyNotices packaged separately.
- Original `bpe.model` encoded `HEY DEX` with sentencepiece0.2.1 into
  `▁HE Y ▁DE X`; tracked keyword file uses `@HEY_DEX`.
- Android build-time preparation script verifies both archives, extracts only
  named regular files into ignored `.cache/voice`, packages license text,
  provenance and original model README; no runtime download. CI/release build
  workflows prepare assets before Gradle. Existing cache survives clean.
- Script `--offline` passes with verified cache. Isolated negative checks reject
  missing cached artifact and tampered archive before extraction. Binaries are
  not tracked; local research dependencies remain ignored/outside source.

Primary sources accessed2026-10-02:
[official KWS catalog](https://k2-fsa.github.io/sherpa/onnx/kws/pretrained_models/index.html),
[runtime license](https://github.com/k2-fsa/sherpa-onnx/blob/v1.13.8/LICENSE),
[release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.8),
[model source](https://www.modelscope.cn/models/pkufool/sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01),
[model metadata API](https://www.modelscope.cn/api/v1/models/pkufool/sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01).

## Implementation and validation

ProtocolV1 unchanged; backend executable source unchanged, no Maven rerun
required for docs-only changes. Android implementation:
- Serial native wake worker owns recorder/stream/model; mic released before STT,
  focus ducks music, cue follows recognizer readiness,30s watchdog and epochs
  invalidate late/duplicate callbacks. Continuous listening holds partial wake
  lock while active; pause/stop/error release it. Battery impact remains unmeasured.
- Application singleton coordinator outlives Activity observers. Correlated
  terminal callbacks, one command admission, queued cancellation, whole-input
  routing and ordered current remote tools; duplicate plans suppressed, reconnect
  never replays. Existing credential and history protections retained.
- Canned vi-VN TTS chooses installed offline voice or network fallback; capability
  and synthesis failure/timeout statuses persist independently of wake state.
  Model currently initializes/releases for each wake session, avoiding retained
  inactive native resources; initialization overhead needs real-device measurement.

JDK17/API35 final `testDebugUnitTest lint assembleDebug assembleDebugAndroidTest`
passes. Unit XML:app37+DPC5=42 tests, zero failures/errors. APK narrowed to
arm64-v8a+x86_64:98,203,815 bytes vs158,205,380 before ABI filtering. Five wake
assets and runtime/model/ONNX licenses are present; no WAV bundled. APK size is
not runtime RAM. Final command/log and runtime follow-up below.

Retained API35 AVD focused runtime/coordinator/protocol runner: `OK(7tests)` in
42.135s. Native KWS loads, decodes synthetic silence without wake, releases over
three cycles; continuous AudioRecord/FGS and pause/resume/stop pass. Headless
local execution/readback, credential redaction, queued cancellation and atomic
multi-clause backend routing pass. Local RFC6455 fixture verifies two ordered
real intent dispatches, duplicate-plan suppression, correlated completion and
reconnect without command replay. Fixture is not live Gemini evidence.

Real SpeechRecognizer session selected
`com.google.android.tts/com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService`.
Ready callback=true; no test speech supplied; diagnostic CALLBACK_TIMEOUT after
30s. This proves bounded invocation/readiness, not vi-VN accuracy or successful
online processing. Hey DeX spoken activation and complete audio-to-result flow
remain unverified.

## Additional integration results

Legacy local-first privacy/history/device suite plus offline inventory: `OK(9
tests)` in82.318s. Volume/Wi-Fi restored and history cleared. Earlier capability
inventory still reports vi-VN STT offline data absent and no installed offline
Vietnamese TTS voice; five network voices advertised.

Opt-in real Docker/Gemini headless integration failed its required DISPATCHED
assertion:actual terminal `AI_TIMEOUT` in10.891s. Connection and correlated
terminal callback worked, but no live action completion is proven. Provider
10s deadline was not changed, no automatic/manual replay attempted, and no new
provider/free-tier explanation is inferred. Raw sanitized runner output remains
host-local `/tmp/dex-hybrid-live-backend-avd.log`. Existing live cloud acceptance
remains open under T-009; deterministic fixture evidence is separate.

Final main-agent full Gradle validation after ABI narrowing: `BUILD SUCCESSFUL
in1m31s`,153 tasks (12executed,141up-to-date), log
`/tmp/dex-hybrid-final-build.log`. Earlier integrated source build passed2m5s,
153tasks. Owner confirmed AVD-only testing; no physical update or reset.

## Reproduce / remaining device gate

Android root:

```bash
python3 scripts/prepare-voice-assets.py
env JAVA_HOME=/home/khuongnd/.local/opt/jdk17 ANDROID_HOME=/home/khuongnd/Android/Sdk ./gradlew --no-daemon testDebugUnitTest lint assembleDebug assembleDebugAndroidTest
/mnt/d/Android/Sdk/platform-tools/adb.exe -s emulator-5554 install -r D:/dex-assistant-ui/app/build/outputs/apk/debug/app-debug.apk
/mnt/d/Android/Sdk/platform-tools/adb.exe -s emulator-5554 install -r D:/dex-assistant-ui/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
/mnt/d/Android/Sdk/platform-tools/adb.exe -s emulator-5554 shell am instrument -w -e class com.khuongnd.dexassistant.ui.VoiceRuntimeDeviceTest,com.khuongnd.dexassistant.ui.CoordinatorDeviceTest,com.khuongnd.dexassistant.ui.CoordinatorProtocolDeviceTest com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner
```

Live provider check is opt-in via `CoordinatorBackendDeviceTest` and
`backendEndpoint=ws://127.0.0.1:8081/ws/v1/assistant` plus ADB reverse8081;
uses only the public isolated `avd-test-token`. It intentionally requires
DISPATCHED and must fail for AI_TIMEOUT rather than claiming acceptance.
Do not include it in routine offline instrumentation runs.

On a matching-signed physical Fold3, measure a controlled Maps+music baseline
then wake-enabled workload with `adb shell dumpsys meminfo
com.khuongnd.dexassistant.ui`, `adb shell dumpsys cpuinfo`, `adb shell dumpsys
battery`, and `adb shell dumpsys thermalservice`. Use repeated samples and the
same brightness/network/temperature/workload; report STT provider process cost
separately. Record100-command completion, recognition accuracy and p95 latency,
100 quiet/moderate-noise wakes and8h false-activation soak. No CPU/RAM/battery
threshold is claimed from APK size or short native smoke checks.
