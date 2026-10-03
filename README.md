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
encodes “Hey Hey” using the original model BPE (`▁HE Y ▁HE Y`).

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
Start voice from administration or its assistant lifecycle, say “Hey Hey”,
wait for the rising two-note media readiness cue and “Sẵn sàng — nói lệnh”, then say the Vietnamese command. The cue plays once after the STT provider reports ready, without requesting audio focus or changing system volume, and stops on cancellation. Wake is offline;
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

### Numbered DeX screen control (T-010)

Open **Mở cài đặt Trợ năng DeX** in the app and explicitly enable the DeX
accessibility service. Connect Samsung DeX to an external display, start voice,
and say “Hey Hey”. The app prepares numbered clickable controls before the STT
ready cue. Repeated content notifications preserve the mapping when fresh node/window identity and bounds stay unchanged; changed targets invalidate it, and image targets are checked again before a tap. Say “bấm vào số một”, “bấm ô mười một”, “chia ô 11”, “hiện lưới”,
or “hủy”. Grid mode has 6 columns × 4 rows (1–24); subdivision is bounded to
three levels. A grid switch or subdivision starts another listening turn without
another wake phrase. Other commands continue through the existing local/backend
router after removing the numbered overlay.

The screen sub-router reserves malformed, negated and compound numeric commands
locally; it never sends them as `command.request`. It folds Vietnamese accents
only for this finite grammar, accepts digits and spoken numbers through 999, and
reports dispatch rather than claiming the clicked popup actually closed. No tap
is retried after timeout. The Samsung desktop category must identify exactly one
valid non-default display; missing/ambiguous desktop selection never falls back
to the phone.

Accessibility nodes with click actions are filtered by visibility, enabled state,
window occlusion and bounds. Node traversal is capped at 4,096 visits/40 levels,
256 node targets. Screenshot analysis is capped at 640×400 and 64 image targets;
it recognizes a finite set of isolated high-contrast X templates and small
axis-aligned outlined buttons, not arbitrary visual UI. Uncertain images return
no target and grid remains available. Images stay in RAM, are recycled after
analysis, and never enter logs, history, storage or the backend. Only bounded
selected-target fingerprints survive until session cleanup; an image target
requires a fresh matching patch before a gesture. STT retains the existing
online-permitted Vietnamese configuration.

Window/content/scroll changes, display resize/disconnection, expiry, cancellation,
STT errors, pause and stop invalidate mappings and remove the overlay. A tap
consumes its mapping before dispatch, preventing duplicate commands. External
screen behavior remains **unverified** until physical DeX tests pass.

The debug-only `ScreenControlFixtureActivity` has an accessible popup and a canvas
X/outline without clickable Accessibility nodes. After owner-mediated service
activation, run the opt-in instrumentation class
`com.khuongnd.dexassistant.ui.NumberedScreenDeviceTest`; its secondary-display
case skips if no desktop/service is available. A skip is not acceptance evidence.
The suite also checks numeric and malformed commands remain local with the
backend disconnected. Physical wake/STT, popup/YouTube, resize/disconnection,
permission revocation and 100-session CPU/RAM/latency acceptance remain necessary.

On 2026-10-03, matching-signed app/test updates were installed successfully on the
Fold3/API35 with external DeX, preserving the DPC. The focused numeric/malformed
routing case passes `OK (1 test)`/0.494s and restores the original encrypted
history without key rotation. This is disconnected local-routing evidence;
overlay, gesture, image-target and wake/STT acceptance still require manual
accessibility activation and the remaining physical tests.

Instrumentation obtains UiAutomation with
[`FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES`](https://developer.android.com/reference/android/app/UiAutomation#FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
so the owner's real Accessibility service can continue running. Desktop tests
wait up to ten seconds for an enabled service to bind and fail if it does not;
only missing desktop or missing owner activation is a prerequisite skip. The
initial Fold3 desktop runner's three skipped cases are not hardware acceptance.

Run this suite against the already-running app with `--no-restart`; ordinary
instrumentation force-stops its target and can leave an enabled Accessibility
service marked crashed. Confirm the expected service is bound and the app PID is
alive before the command. Do not use `-S` or force-stop as setup.

```bash
DEX_TEST_SERIAL='<connected-Fold3-serial>'
adb -s "$DEX_TEST_SERIAL" shell pidof com.khuongnd.dexassistant.ui
adb -s "$DEX_TEST_SERIAL" shell dumpsys accessibility
adb -s "$DEX_TEST_SERIAL" shell am instrument --no-restart -w \
  -e class com.khuongnd.dexassistant.ui.NumberedScreenDeviceTest \
  com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner
```

The revised Fold3 runner produced three binding assertion failures and one local
routing pass; no desktop gesture ran. These are harness/lifecycle failures, not
yet product gesture evidence. A matching-signed app replacement can clear the
package crash marker and rebind the existing owner-enabled service without
rewriting the grant, replacing DPC or clearing data. See AOSP Android 15
[`Am` option parsing](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/cmds/am/src/com/android/commands/am/Am.java#192),
[`ActivityManagerService` instrumentation lifecycle](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/services/core/java/com/android/server/am/ActivityManagerService.java#16588), and
[`AccessibilityManagerService` package-update rebinding](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/services/accessibility/java/com/android/server/accessibility/AccessibilityManagerService.java#6257).

The desktop fixture tests request display-sized launch bounds and wait for actual
window bounds to settle before preparing numbers. Before any grid tap, the test
checks the fixture's actual display and that its visible screen bounds contain
the selected cell center; floating DeX windows outside that center cause failure
without dispatch. Canvas-X taps are similarly checked against the visible fixture.

Latest normal Fold3/API35 guarded suite passes all four cases without skips
(8.817s, `/tmp/t010-fold3-center-selector-normal.log`): visible raw overlay,
focused/unoccluded secondary-display gesture, fresh canvas-X image target,
subdivision/cancel and local routing. The owner also verified spoken wake →
grid → subdivision → cancel with continued listening. Real popup/YouTube,
spoken target taps and 100 spoken sessions remain open.

Additional lifecycle tests exercise same-turn changed/replaced nodes, window
changes and real 35-second expiry. The resource smoke is explicitly opt-in;
select only its method and pass `-e screenResourceSmoke true` with the same
`--no-restart` runner. It performs 100 preparation/clear cycles with no taps and
logs elapsed/process CPU time and initial/final PSS. This synthetic test is
separate from 100 spoken-session acceptance. Tests never grant Accessibility,
change DPC, clear owner history or export screenshot images.

## Voice handoff and compact dashboard (D-020)

The dashboard keeps voice controls and masked recognition text on one page.
Configuration, quota, history, manual commands and full text open dialogs. It
supports 320×480dp narrow windows and uses two columns from 700dp width.
“Tiếp tục” resumes an actual paused voice session. “Nghe thử” recognizes and
shows text without executing commands or sending a backend command; it restores
prior listening/paused/stopped state. This diagnostic still uses the configured
online-permitted system recognizer. Transcript is held only in RAM, masked for
credential cues, and cleared on explicit stop/new command. No audio is retained.

The app requests audio focus only for its own feedback, yielding STT focus to its
provider. App capture reports route, level and silenced status; system STT manages
its own route. Run `VoiceHandoffDeviceTest` and `DashboardLayoutTest` using the
existing matching-signed instrumentation workflow. These three checks pass on Fold3 (5.081s,
`/tmp/t006-dashboard-fold3-renderer-tests.log`), and the owner confirms real
Hey Hey recognition, visible text and the full dashboard. Uninterrupted repeated
wake-to-command and popup/YouTube acceptance remain open.

## Automatic listening (D-021)

Opening the app starts Hey Hey listening automatically when microphone permission
and automatic-listening policy allow it. The default-assistant lifecycle checks
unintentional absence/errors with four-attempt bursts and five-minute cooldown; active command turns are
left alone. Default-assistant recovery requires held role and completed permission
reconciliation; foreground app opening uses its foreground eligibility with
unlock/microphone/policy checks. Tạm dừng and Dừng deliberately suppress automatic starts across
reopen/boot. Tiếp tục or Bắt đầu explicitly restores listening. External audio
interruptions can recover once call/communication mode ends.

The app displays actual Accessibility state and links to Settings; it cannot
promise to force-enable the service. Its main switch enables control; an optional
Accessibility shortcut can also toggle it off. `AutomaticListeningDeviceTest`
checks auto-open/recovery/manual controls and restores the original voice policy.
Current source passes55unit tests, lint and assemblies; physical D-021 auto-open,
recovery and manual pause/stop controls pass. Integrated desktop/automatic run
has6passes/1expiry timing failure: a real content event correctly invalidated the
session at7s, so quiet35s-expiry proof remains open. Safety invalidation is retained. Use the matching-signed preserved-process workflow without reset.
