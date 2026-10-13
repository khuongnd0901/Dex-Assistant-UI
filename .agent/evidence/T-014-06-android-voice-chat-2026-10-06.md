> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-014-06 Android voice Chat and TTS lifecycle — 2026-10-06

## Implemented

- Android voice final text now recognizes whole-utterance `Bắt đầu trò chuyện`
  and starts the existing Chat V1 session. Chat ordinary turns are sent only
  through `AssistantWebSocket.sendChatTurn`; they do not call Command dispatch.
- Chat replies are spoken through existing Vietnamese TTS. Successful playback
  reopens STT directly and emits the existing ready cue, without a wake phrase.
- Whole-utterance `Kết thúc trò chuyện` ends Chat and returns to Command wake
  standby. Stop/Pause/service cancellation ends the active Chat session; stale
  callbacks are guarded by Chat generation and feedback playback identity.
- Added pure routing tests for exact start/end utterances and command-like Chat
  text. Existing command routing code remains below the Chat-specific branch.

## Validation and evidence

- `git diff --check` in Android and canonical backend repositories: PASS.
- Attempted `./gradlew test --max-workers=1 --offline`: BLOCKED during Gradle
  startup with `What went wrong: 25.0.4.1`; host Java is Temurin 25.0.4.1 and
  Android build requires JDK17. Therefore Android unit tests, lint and APK
  assembly were not run successfully.
- Static review of current `VoiceAssistantService` confirms microphone handoff
  is sequential: app wake `AudioRecord` is stopped/released before external
  `SpeechRecognizer` starts; TTS occurs after recognition is destroyed. There
  is no app-owned capture path during TTS. The requested spoken exit during TTS
  therefore has no safe detector in this implementation. No parallel capture,
  new recognizer, echo-cancellation claim or physical test was introduced.

## Acceptance remaining

- Follow-up during T-014-07: JDK17 `./gradlew test lint assembleDebug --max-workers=1 --offline`
  PASS on the later source (app 133 tests/variant, DPC 5/variant, lint/APKs).
  The build exposed a non-exhaustive active-Chat `START` branch; it was fixed
  to route the text as an ordinary Chat turn. This supersedes only the earlier
  JDK17 build blocker, not the unverified spoken-interruption behavior.
- T-014-06 remains `IN_PROGRESS`: verify real TTS completion/cancellation and
  lifecycle regressions, and design/test
  a local spoken-exit detector with echo rejection that does not compete with
  the existing recognizer. If current hardware/audio routing cannot support it,
  report measured evidence and obtain a product decision before substituting
  button-only exit.
- This evidence is not a provider-backed demo, end-to-end host/AVD verification,
  physical spoken-exit/echo test, or completion of T-014.
