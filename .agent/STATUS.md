# Android repository status

Last synchronized: 2026-10-02. Canonical project memory and task ownership are
in `/mnt/d/DeX-Assistant/.agent/STATUS.md`; Android source is in this repository.
T-006/T-009 remain IN_PROGRESS. No commit, push, physical-device install or reset
was performed in this implementation session.

## Implemented: D-016 hybrid voice

- Offline “Hey DeX” uses sherpa-onnx 1.13.8 and the official GigaSpeech 3.3M
  2024-01-01 int8 KWS model, CPU one thread. The wake worker owns its native
  stream and AudioRecord (mono PCM16/16 kHz), stopping/releasing the microphone
  before command STT. Native model/stream are recreated for each wake session;
  initialization cost and resource implications need measurement before tuning.
- SpeechRecognizer requests `vi-VN`, permits the service's online path, and uses
  the configured external recognition service (excludes this app's assistant
  contract stub). Only final text dispatches; partial/duplicate/late results do
  not. Recognition has a 30-second watchdog. Ready callback plays a cue; say the
  command after that cue, not in the wake phrase's breath.
- Audio focus ducks media during command/feedback. Wake is suspended during STT,
  command execution and TTS. A partial wake lock keeps the opt-in active pipeline
  running when the screen sleeps; pause/stop/runtime error release it. This is
  a battery tradeoff, not a measured Fold3 power claim.
- Canned Vietnamese feedback prefers installed offline exact-locale voices,
  then network voices; missing data, initialization failure, playback error and
  timeout remain observable via `voice_health.ttsStatus` and wake health detail.
  No raw transcript/password is spoken or logged. Tone fallback resumes wake
  without claiming usable Vietnamese synthesis.
- The application-scoped AssistantCoordinator owns DPC/WebSocket/routing/history.
  Activity destruction detaches its observer; headless service dispatch survives.
  Request IDs enforce one command and one terminal callback, cancel queued
  dispatch, and invalidate old callbacks. Backend plans are correlated,
  deduplicated as a whole and executed serially. Timeout/disconnect never replay.
- Whole-input supported simple commands remain local. Complex/multiple clauses
  route entirely to backend. Credential-like commands remain guarded from the
  backend/Gemini and redacted in history; online STT audio is authorized, but
  password Wi-Fi provisioning and new backend tools are not implemented here.
- V1 wire messages/authentication/types are unchanged. Terminal feedback
  distinguishes verified local state, dispatched intents and unexecuted plans.

## Reproducible artifact preparation

Run `python3 scripts/prepare-voice-assets.py` before Gradle. AAR/model downloads
are pinned by SHA-256, extracted into ignored `.cache/voice`, and verified before
build; no runtime download or generated binary/audio is committed. Both CI
workflows prepare the same assets. Apache 2.0 runtime/model and ONNX Runtime MIT
license/third-party notices are packaged. Keyword encoding was checked against
upstream SentencePiece: `▁HE Y ▁DE X @HEY_DEX`.

Only `arm64-v8a` (Fold3) and `x86_64` (retained AVD) are packaged. Final app APK
is 98,203,815 bytes; binary size is not runtime RAM usage. See README and packaged
`assets/licenses/voice-PROVENANCE.txt` for hashes/source URLs.

## Verified in this session

- JDK17/API35 full `testDebugUnitTest lint assembleDebug
  assembleDebugAndroidTest` passes: final ABI-filtered build 153 tasks, 1m31s;
  37 app and 5 DPC unit tests, zero failures/errors. An earlier integrated full
  build also passed 153 tasks/2m5s. New tests cover session invalidation,
  queued cancellation, one-in-flight correlation, routing/execution/feedback
  states and pause/stop across active phases. `git diff --check` is required at
  final handoff.
- Retained Windows AVD `ZFold3_API35` / `emulator-5554` received app/test APK
  updates only, no DPC replacement/reset. Focused native/coordinator/protocol
  runner passes `OK (7 tests)`/42.135s: native silence decode/release over three
  cycles; continuous microphone pause/resume/stop; headless local dispatch;
  multi-clause atomic backend routing/privacy; queued cancel; offline RFC6455
  fixture with two ordered remote results, duplicate plan and reconnect/no replay.
- Final narrowed APK was installed and native + microphone lifecycle smoke
  passes `OK (2 tests)`/7.126s. Runner ends STOPPED. Native silence is a negative
  fixture, not positive “Hey DeX” detection/accuracy evidence.
- Legacy local-first/privacy/history/device-action regression and offline speech
  inventory pass `OK (9 tests)`/82.318s; volume/Wi-Fi restored and test history
  cleared. Existing offline vi-VN STT/TTS inventories remain UNAVAILABLE on AVD.
- Actual generic recognizer diagnostic selects GoogleTTSRecognitionService,
  reaches `onReadyForSpeech`, and ends at the 30-second bound without a terminal
  callback when no speech is supplied. This is invocation/lifecycle evidence;
  no Vietnamese transcript, language accuracy or online recognition is proven.
- Opt-in real backend connected and accepted a headless complex utterance, then
  returned AI_TIMEOUT in10.891s. Its success assertion correctly fails; it was
  not retried. Deterministic transport/action evidence passes, while real
  provider end-to-end execution remains unverified.
- Actual canned Vietnamese TTS diagnostic is pending final runner below; do not
  equate voice metadata with successful synthesis.

## Remaining acceptance and next action

Physical Fold3 is unavailable. Keep T-006/T-009 IN_PROGRESS: positive wake/audio
fixtures, wake accuracy/false activation soak, actual Vietnamese utterances,
headless/screen-off speech, reboot continuity, CPU/PSS RAM/pin/thermal baseline
with Maps+music, 100-session resource stability and live provider completion
remain unverified. Do not change signing or reset to fill this evidence gap.

Next: finish the bounded canned TTS diagnostic, then test wake/real Vietnamese
speech and resource behavior on the same-signed Fold3 when available. If its
selected system recognizer cannot handle vi-VN online, evaluate a separate
cloud STT adapter using measured failures; do not silently change engines.

## Foundation retained

API35 Fold3 Device Owner, assistant binding, permission reconciliation, reboot,
app-only update and notification controls previously passed the foundation
probe slice in canonical evidence `T-006-fold3-2026-10-01.md` and
`T-006-fold3-recovery-2026-10-02.md`. Those were short microphone probes, not
wake/STT/TTS acceptance. DPC has no backend credentials; signature-protected
permission/actions retain the established signing identity. Locally generated
current debug APKs are for this AVD and must not be installed on Fold3 under an
unrelated certificate. The encrypted signing backup remains in `signing/`;
never commit decrypted signing material.
