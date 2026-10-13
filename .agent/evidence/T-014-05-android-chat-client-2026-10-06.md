> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-014-05 — Android Chat session/protocol client (2026-10-06)

## Scope and output

Android agent implemented milestone 4 in `/mnt/d/dex-assistant-ui` against the
canonical `docs/PROTOCOL.md` Chat schema 1 contract. Changed files:
`AssistantWebSocket.kt`, new `ChatSession.kt`,
`AssistantWebSocketConcurrencyTest.kt`, and new `ChatSessionTest.kt` under the
app protocol source/test packages. No backend source or wire contract changed.

- A new Chat start creates a random session UUID; turns reuse it and include
  final text plus local timestamp/IANA zone. Start, turn, end, and Chat error
  messages use the existing authenticated V1 WebSocket envelope.
- The client accepts only correlated replies for the active session/request,
  permits one in-flight turn, rejects duplicate/late replies, and invalidates
  Chat state on end, disconnect, reconnect, and protocol failure. A new session
  never replays an old turn. A null-session start error and immediate new start
  after end are covered by focused tests.
- Chat events use a separate callback. Chat response text is not converted to
  `execution.plan`, `DEVICE_COMMAND`, Command action callbacks, or durable
  Command history. Malformed Chat frame reporting uses a fixed error string,
  without reflecting raw content.

## Validation

- JDK 17 `./gradlew test --max-workers=1 --offline` on the final source:
  **PASS** in 1m18s; app 130 Debug + 130 Release tests, DPC 5 Debug + 5
  Release tests, zero failures/errors.
- JDK 17 `./gradlew lint assembleDebug assembleDebugAndroidTest
  --max-workers=1 --offline` on the final source: **PASS** in 3m12s, 143
  tasks. An earlier combined `test lint assembleDebug assembleDebugAndroidTest`
  run also passed in 8m42s but started before the final invalid-session fix;
  the two final-source runs above are the completion evidence.
- `git diff --check` in Android and CRLF-aware `git diff --check` in the
  canonical backend checkout: **PASS**. No commit or push.
- Built SHA-256: app debug
  `ed33962dcc2cbbe57393fd396f5a5b905d85df249c35259a67b0829645695465`,
  app androidTest
  `3632187ade1963ae7c2748355a743bed085a589175455817b1f4aa5cc088a308`,
  DPC debug
  `8273638a8182238752d9ee35fe2a97fc4e8ff3b8076791521b2ab3e45308a0de`.
- The prior Android baseline resource merge NPE came from stale generated
  incremental merger state. The agent moved the affected generated directories
  to `/tmp`; no tracked build configuration was changed. The full final-source
  build then passed.

## Boundary

This task verifies the Android Chat protocol client and Command isolation in
host tests/builds. It does not expose a runnable voice Chat UI, speak responses,
or verify TTS, wake/STT lifecycle, provider-backed turns, AVD, or Fold3 behavior.
Those gates remain open in T-014; no next subtask was started.
