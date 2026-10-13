> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-006 second USB microphone press — Fold3 follow-up (2026-10-06)

## Build and install

- Device: Galaxy Z Fold3 SM-F926B, Android 15/API 35, ADB
  `192.168.1.18:45283`.
- JDK17 `./gradlew test lint assembleDebug --max-workers=1 --offline`: PASS,
  app 133 unit tests per debug/release variant, zero failures/errors and one
  opt-in skip per variant; aggregate lint and APK build PASS.
- Built APK SHA-256:
  `7624425c7cd745d4b9987922b4324c15116194593afe768fcf617c6a4ef560f2`.
  Signed/installed APK SHA-256:
  `d0fced0a153d1234e688b439f8fd514596f6b8e7f29313b98bed4e0d08c701b8`.
  Certificate SHA-256 matches the existing Fold3 test identity:
  `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
- Two ADB incremental installs reported success but the app package was later
  absent. `adb install --no-incremental -r` persisted through a follow-up query.
  No manual uninstall or clear-data command ran; app-data retention is unknown.
- After reinstall, Android initially reported RECORD_AUDIO denied and
  Accessibility unbound. The owner manually enabled Accessibility and granted
  Microphone. Latest readback confirmed `RECORD_AUDIO: granted=true` and the
  DeX Accessibility service enabled/bound. No permission was changed by ADB.

## Physical button observations

- Owner reports pressing the USB Micro button a second time while the partial
  command was visible. Metadata-only voice log shows partial callbacks at
  22:49:09.008, 22:49:09.053, 22:49:09.150 and 22:49:09.691; Google speech end
  at 22:49:13.847; Google final at 22:49:13.861; dispatch at 22:49:13.881.
  There was no `stt_manual_submit` stage. The utterance was dispatched only
  after Google final and ended safely as `INVALID_SCREEN_COMMAND` for the
  intentionally invalid “bấm ô 999” test phrase. Thus second-press submission
  is not physically accepted yet; event delivery/guard path remains unresolved.
- A filtered read-only `getevent` capture on the USB composite consumer-control
  node `/dev/input/event13` recorded `KEY_VOICECOMMAND` down/up. At that later
  event the voice service was `PAUSED by user`, so Pause policy correctly
  rejected activation. It does not prove key delivery during active STT.
- After the owner resumed listening and restored microphone/Accessibility,
  readback showed service `LISTENING`, RECORD_AUDIO allowed and Accessibility
  enabled/bound. Initially `cmd role get-role-holders android.app.role.ASSISTANT`
  returned `com.google.android.googlequicksearchbox`, so DeX reported not ready.
  ADB inspection found DeX listed in Samsung's “Digital assistant app” chooser.
  During UI navigation, a tap appears to have selected DeX unintentionally;
  readback now returns `com.khuongnd.dexassistant.ui`, and app health is
  `READY`/`TRANSCRIBING`. The owner was asked whether to keep DeX selected for
  testing or restore Google. No USB attempt followed this role change.
- Owner confirmed keeping DeX as the assistant role for a fresh trial. RoleManager
  reports `com.khuongnd.dexassistant.ui`; health reads `READY`, and voice was
  `TRANSCRIBING` at trial start. Metadata-only log captured three recognition
  epochs with `stt_manual_submit status=PARTIAL`, each 13–14 ms before its one
  `command_dispatch`. No `stt_final` appears after those submits in the captured
  epochs. The terminal statuses were `INVALID_SCREEN_COMMAND`, `SEND_FAILED`,
  and `VERIFIED`. The last result may indicate a real action, so the owner was
  asked whether the screen changed; no further trial was run. Do not count this
  as safe-phrase acceptance until resolved. A simultaneous filtered `getevent`
  session was interrupted before its pipe flushed, so the raw key code was not
  correlated to these manual-submit events.
- Preserve the existing Pause/Stop and assistant-role guards.

## Remaining acceptance

Resolve whether the `VERIFIED` result changed the screen, then repeat a known
safe invalid phrase if needed. Correlate USB key metadata with `DexVoiceTiming`.
Observed manual submits preceded dispatch by 13–14 ms, with one dispatch per
epoch and no subsequent Google final; raw key correlation and safe outcome of
the last attempt remain unresolved.
