> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-006/T-009 Vietnamese driving feasibility — 2026-10-02

Read-only Android source audit and primary-document research; no real speech
recognition or physical-device performance is established by this report.

## Source findings

Android root: `/mnt/d/dex-assistant-ui`.
- `app/src/main/java/com/khuongnd/dexassistant/ui/voice/VoiceAssistantService.kt`
  reads one 320-sample buffer at 16 kHz and releases AudioRecord. LISTENING
  still means a probe, not continuous wake detection. No bundled speech assets
  or native runtime dependency exists in `app/build.gradle.kts`.
- `voice/AssistantRecognitionService.kt` always returns ERROR_CLIENT.
  No STT/TTS implementation exists. `AssistantController` belongs to Activity;
  voice has no final-text handoff or Activity-independent coordinator.
- `routing/LocalRouting.kt` sends Wi-Fi connection phrases to BACKEND_REQUIRED.
  History redaction consumes only one password token. This violates D-015 for
  spoken credentials; fail-closed routing and whole-utterance redaction precede
  voice implementation. No Wi-Fi association executor exists.
- Maps, YouTube Music and Zalo aliases/adapters are absent. Generic media keys
  and YouTube search are DISPATCHED only. No confirm/cancel/online consent
  dialogue exists.
- Windows ADB devices check: emulator-5554 available, no physical Fold3 listed.
  No resets, installs, recordings or device-state changes during this audit.

## Official evidence (accessed 2026-10-02)

- [Sherpa KWS](https://k2-fsa.github.io/sherpa/onnx/kws/index.html): published
  KWS models are English, Chinese, or Chinese/English. Customized keywords
  do not establish Vietnamese acoustic support. English GigaSpeech is not
  evidence for “a lô a lô”.
- [Vietnamese ASR models](https://k2-fsa.github.io/sherpa/onnx/pretrained_models/offline-transducer/zipformer-transducer-models.html):
  Vietnamese offline Zipformer models, including int8, have microphone/VAD
  examples. Candidate for local STT and experimental VAD+phrase matching;
  offline ASR is not a proven low-power KWS engine. Model-specific license,
  pinned-runtime compatibility, APK size and performance remain unchecked.
- [Runtime license](https://github.com/k2-fsa/sherpa-onnx/blob/master/LICENSE):
  inspect separately from model provenance/license before redistribution.
- [Android SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer):
  generic recognition may stream audio remotely and is unsuitable for continuous
  recognition. Use explicit on-device creation and support checks; never assume
  installed Vietnamese support or silently use the generic recognizer. Explicit
  on-device creation requires API 31; minimum API 30 needs a bundled engine
  or a truthful unavailable path. EXTRA_PREFER_OFFLINE alone can be ignored:
  [RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent).
- [TTS Voice](https://developer.android.com/reference/android/speech/tts/Voice):
  check Vietnamese voice availability and isNetworkConnectionRequired before
  selecting offline synthesis; actual device playback still needs validation.
- [Maps intents](https://developer.android.com/guide/components/google-maps-intents):
  documented navigation/search dispatch exists. It supplies no evidence here
  for reading ETA, stopping navigation or verified route completion.
- [MediaSessionManager](https://developer.android.com/reference/android/media/session/MediaSessionManager):
  access to other apps' active sessions requires privileged media access or an
  enabled notification listener. Ordinary generic media-key dispatch cannot
  establish target-app playback.
- [Zalo developers](https://developers.zalo.me/docs): official OA/ZBS/Social
  interfaces do not by themselves prove zero-touch personal-account messaging
  or calling. Those actions remain investigation/backlog, no automation promise.

## Coordinated milestones and gates

1. Privacy: reject credential-bearing/provisioning utterances locally before
   remote serialization; redact entire sensitive utterance before history.
   Unit cases include multiword passwords, aliases and unrelated remote requests.
2. Device speech feasibility: instrument on-device vi-VN STT/TTS support without
   recording/storage/upload; evaluate model license/version/ABI and size. AVD
   informs compatibility, Fold3 informs availability and performance.
3. Engine gate: prototype Vietnamese bundled ASR+VAD phrase detection only after
   license/runtime verification; compare exact wake phrase and resource cost.
   Existing English model pin remains historical, not Vietnamese acceptance.
4. Voice coordinator: one microphone owner, bounded audio in memory, wake -> STT
   -> local router -> TTS, no Activity dependency; pause/stop cancels callbacks,
   no self-trigger from TTS, no replay on reconnect.
5. Dialogue and Wi-Fi: pending action with cancel/confirm/timeouts; online STT
   requires fresh explicit voice consent and a fresh non-sensitive recording.
   Credential dialogue never leaves device; separate typed signature DPC IPC,
   literal SSID/password preservation and observed association (not enable state).
6. Integrations: app opening and media first; Maps navigation dispatch only with
   resolved destination; personal Zalo actions excluded until documented/tested.

Retain T-006 thresholds: 100 quiet calls >=95%, 100 moderate-noise >=90%, eight
hours <=1 false wake; 100 commands >=99% completion, final-text-to-action p95
<2s. Report STT accuracy, wake/STT latency, memory, APK size, battery and music
interference separately. No values measured yet. Model/provider/security
commitment changes require owner review under the existing ExecPlan.

## Privacy implementation slice

Android adds a shared CommandPrivacy guard to parser and controller.send before
WebSocket dispatch. Credential keywords and Wi-Fi provisioning phrases produce
a truthful LOCAL_ONLY_UNSUPPORTED local response until provisioning exists.
History redacts the entire utterance and outcome before encryption; decrypted
legacy entries are sanitized before display/re-persistence. This conservative
grammar can reject benign credential-related requests and cannot recognize
all arbitrary secrets. No voice/dialogue/provisioning capability is implied.

Canonical CONNECT_WIFI row, D-015 consequences and SRS were synchronized. No
WebSocket schema, remote action, backend source or provider configuration changed.

Validation for the final privacy revision:
- Android JDK17/API35 `./gradlew --no-daemon testDebugUnitTest lint
  assembleDebug assembleDebugAndroidTest`: BUILD SUCCESSFUL, 152 tasks, 1m47s.
  App 29 + DPC 5 unit tests, zero failures/errors.
- Windows AVD emulator-5554: app/test APK updates succeeded; focused
  `LocalFirstDeviceTest#privateCommandIsRejectedLocallyAndHistoryIsRedacted`
  and `#historicalSensitiveEntryIsRedactedOnReadAndPersist` report
  `OK (2 tests)` in 9.218s. Real UI/controller path and actual Keystore storage
  were exercised with synthetic credentials, not voice/audio input. No reset,
  DPC update or physical-device change.
- Both repositories `git diff --check`: PASS; local Markdown targets: PASS.
- Maven not rerun: backend executable code unchanged. Owner subsequently authorized commit/push of this validated slice.
