# Dex Assistant UI

Android client for DeX Assistant. The project contains a Kotlin/Jetpack Compose
client plus a separate Device Policy Controller. Both target Android 15 (API
35), with API 30 as the minimum supported version for the Galaxy Z Fold3.

The Spring Boot backend and canonical protocol are maintained in the private
[`DeX-Assistant`](https://github.com/khuongnd0901/DeX-Assistant) repository.

## Prerequisites

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools
- Python 3.10+ for checksum-pinned build-time voice asset preparation

## Build and test

```bash
python3 scripts/prepare-voice-assets.py
./gradlew test lint assembleDebug
```

The preparation step downloads official sherpa-onnx1.13.8 and English
GigaSpeech3.3M int8 KWS archives, verifies pinned SHA-256 hashes, and extracts
only production model files to ignored `.cache/voice`. APKs include arm64-v8a
for Fold3 and x86_64 for the retained AVD; unused32-bit ABIs are excluded. CI performs this step
before Gradle. Use `--offline` to require existing verified archives. `clean`
does not remove this cache; no runtime download or vendor key is used.
License/provenance files are packaged in APK assets. The tracked keyword file
encodes “Hey DeX” using the original model BPE (`▁HE Y ▁DE X`).

On Windows, use `gradlew.bat` instead. Open the repository root in Android
Studio if the SDK path needs to be configured through `local.properties`.

Debug APKs are produced at `app/build/outputs/apk/debug/app-debug.apk` and
`dpc/build/outputs/apk/debug/dpc-debug.apk`.

## CI and device-test releases

`Android CI` runs `test lint assembleDebug` for pull requests and pushes to
`main`, then retains both debug APKs as short-lived workflow artifacts.

Maintainers can run `Device-test release` from the GitHub Actions page against
`main`. Supply a new tag matching `device-test-*`; the
workflow tests and builds both APKs, verifies their shared certificate, creates
`SHA256SUMS`, and publishes a GitHub prerelease tied to that commit.

The manual release requires the repository Actions secret
`DEVICE_TEST_KEYSTORE_BASE64`. It contains the base64-encoded existing test
keystore so device-test releases remain upgrade-compatible and retain
signature-protected app/DPC IPC. The workflow rejects any certificate other
than SHA-256
`9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
This is a test-only debug identity, not a production signing design. Never add
the keystore or its encoded value to Git, logs, artifacts, or release assets.

## Dedicated-device setup

Device Owner provisioning can reset or constrain a device. Use only the
dedicated Fold3 and back it up first. For a clean development device with no
accounts, install the DPC before the main app and provision it through ADB:

```bash
adb install dpc/build/outputs/apk/debug/dpc-debug.apk
adb shell dpm set-device-owner \
  com.khuongnd.dexassistant.dpc/.DexDeviceAdminReceiver
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.version.release
adb shell dpm list-owners
```

Start the backend with a generated token digest and remote fake execution:

```bash
DEX_DEVICE_TOKEN_SHA256=<sha256> DEX_FAKE_REMOTE_EXECUTION=true ./mvnw spring-boot:run
adb reverse tcp:8080 tcp:8080
```

Provision `ws://127.0.0.1:8080/ws/v1/assistant` and the corresponding raw token
in the app. Cleartext WebSocket is restricted to loopback; other endpoints must
use WSS.

## Security

Do not commit backend bearer tokens, signing keys, `local.properties`, or
environment-specific server URLs. The main app wraps its bearer token with an
AndroidKeyStore AES-GCM key and excludes app data from backup. The DPC has no
backend credential or network permission; its exported service requires a
signature-level permission, so both APKs must retain the same signing identity.

## Hybrid voice (T-006 / D-016)

Provision the default assistant and DPC as documented in canonical backend
`scripts/android/README.md`; no reset is authorized for the retained devices.
Start voice from administration or its assistant lifecycle, say “Hey DeX”,
wait for the readiness cue, then say the Vietnamese command. Wake is offline;
Android SpeechRecognizer vi-VN may send command audio, including sensitive
speech, to its recognition service. There is no per-command online consent.
Network loss can prevent recognition even when execution would be local.

Supported whole-input local commands execute on-device; complex/multi-clause
final text routes to the backend's current tools. Credentials/provisioning
remain guarded from backend/Gemini and are redacted from history; Wi-Fi
password association is not introduced by this voice milestone. Recognition
partial results never dispatch. Pause/Stop in the persistent notification
cancel recording/recognition; timeout/reconnect never automatically replay.

Vietnamese TTS prefers an installed offline voice and may use a network voice.
If no usable voice exists, health reports that limitation. App/YouTube/media
intents remain dispatch evidence, not proof of playback or visible completion.
Actual Fold3 audio, false activation, CPU/RAM, battery and thermal acceptance
must be recorded separately from AVD instrumentation and builds.
