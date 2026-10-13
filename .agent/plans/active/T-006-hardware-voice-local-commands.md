> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

> Scheduling override 2026-10-13: owner requests Windows simulator tests through WSL during continuation. All remaining physical Z Fold3 execution is consolidated in [T-020](T-020-consolidated-fold3-acceptance.md), one prepared campaign. This overrides older immediate/separate device-run instructions, preserves acceptance gates and does not imply PASS.

# Hardware voice activation and richer local commands

## Outcome and boundaries

T-006: a dedicated mic/assistant button starts the existing Vietnamese command
session without speaking HeyHey. T-009: local grammar accepts useful variants
of existing actions, including “tăng âm lượng 100%” as absolute SET100.
Keep wake→STT microphone stop/release ordering, lifecycle/session guards and
local/backend privacy/negation/multi-intent routing. No new streaming ASR,
provider, autoplay, force-stop, call interception, or arbitrary key capture.
Saved broader voice/UI migration remains deferred.

## Inputs and constraints

Owner uses a mouse mic button that toggles listening on one press; steering
wheel button connects by Bluetooth directly. Device key/event delivery and
car compatibility are unknown until measured. Existing assistant role is held;
session service returns an inert VoiceInteractionSession. Accessibility lacks
filter-key capability. Volume normalizer strips trailing %, and existing
absolute grammar only handles “đặt âm lượng N phần trăm”.

Acceptance: ready/unlocked authorized idle service accepts a dedicated button
once; wake AudioRecord stops/releases before STT; repeated/busy triggers do not
create concurrent recognizers; unrelated keyboard/mouse/media/volume buttons
retain behavior. Default-assistant session activates the same path. Hardware
must actually deliver a supported key/session invocation; do not claim every
Bluetooth steering wheel works. Parser handles adjacent %, spaced %, Vietnamese
units and target synonyms within0..100, preserving step commands and rejecting
unsafe/ambiguous malformed, negative/out-of-range or multi-action cases.

## Decision questions and evidence

- Dedicated ASSIST/VOICE_ASSIST keys via Accessibility or default assistant
  session invocation: official Android APIs plus sanitized device key trace.
- Generic headset/media/micmute keys: not mapped automatically; require measured
  hardware-specific intent before binding, avoid hijacking existing controls.
- Manual activation while explicitly paused/stopped: retain suppression; require
  owner Resume. Button does not silently reset policy or permission settings.
- “tăng âm lượng100%”: absolute volume100, not multiply current volume. Explicit
  relative-percent phrasing remains unsupported until defined separately.

Primary references accessed2026-10-04:
https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo
https://developer.android.com/reference/android/service/voice/VoiceInteractionSession
https://developer.android.com/reference/android/service/voice/VoiceInteractionService
https://developer.android.com/reference/android/speech/RecognizerIntent#ACTION_WEB_SEARCH

## Milestones

1. Trace physical mic button metadata; inspect current lifecycle and grammar.
2. Add shared button activation and default-assistant/session/key entry paths;
   preserve guards, key balance and stop/release-before-recognition ordering.
3. Expand strict local grammar and document supported phrases; meaningful units
   for percentages/Unicode/symbols/privacy/negation/compound/unknown routing.
4. Single integrated JDK17 unit/lint/app+test build, original-cert install, device
   trigger and volume checks. Volume test restores original volume/mute state.
5. Record verified results/limitations; commit and push both repositories under
   persistent owner authorization. T-006/T-009 broader gates stay open.

## Validation

JDK17 Gradle test/lint/assembleDebug/assembleDebugAndroidTest and gitdiffcheck.
Unit/device evidence separates injected key/session callbacks from real mouse
button, real Bluetooth wheel (not available), and actual STT/action results.
Numeric mic/key/stage metadata only; no transcripts/audio/ordinary typing or
owner history/config/key deletion. Percentage device test uses local executor,
reads actual STREAM_MUSIC value, restores original volume and mute afterward.

## Risks and escalation

Some hardware buttons only toggle mic internally or are consumed by headunit/
Bluetooth stack; filtering cannot create absent events. Android may route some
buttons only to default assistant or a single enabled key-filter service.
Do not infer broad compatibility from ADB key injection. If a custom binding
is needed, discuss measured key/device and scope with owner before hijacking a
generic key. Do not change media-session ownership, call handling or costs.

## Progress and findings

- [x] Source confirms inert assistant session and trailing-percent parser bug.
- [x] Owner clarified one-press mouse and direct Bluetooth wheel.
- [x] Physical chooser trace establishes speech WEB_SEARCH activation; raw transport unknown.
- Owner physical mouse press shows Bixby/Google resolver; ROLE_ASSISTANT is
  already DeX, but ACTION_VOICE_COMMAND candidates are only Google/Bixby.
  App currently declares no VOICE_COMMAND activity. This confirms Android voice
  activation reaches a chooser, not exact underlying raw key/HFP transport.
  Initial VOICE_COMMAND activity registered and passed package query, but owner
  physical chooser still excluded DeX. Live ResolverActivity trace established
  android.speech.action.WEB_SEARCH, which had only Google/Bixby candidates. Add
  precisely that RecognizerIntent action to shared guarded entry; reject generic
  SEARCH and android.intent.action.WEB_SEARCH. Owner selects DeX in chooser.
  Ordinary-key trace recorded no supported events.
- [x] Hardware entry/guards, strict local grammar, frozen integrated build/lint.
  75debug+75release units PASS; matching-cert APK installed/hash verified.
  Resolver PASS0.018s, volume40/100 +restore PASS0.071s, actual STT/release/
  busy/cancel/wake-resume PASS2.143s. No device test skipped.
- [x] Follow-up physical mouse acceptance PASS: DeX visible, listening and opens YouTube.
  First trial FAIL retained; exact speech WEB_SEARCH addition fixes observed route.
  Owner requested Always; preferred handler readback DeX mAlways=true verified;
  subsequent owner physical test successful.
- [ ] Actual Bluetooth wheel/SCO unverified (requires vehicle hardware).
- [x] Automated device slice and command/hardware documentation.
- [x] Both-repository publication checkpoint; final evidence update also published.

Evidence: `.agent/evidence/T-006-T-009-hardware-local-commands-2026-10-04.md`.

## Follow-up: second mic press submits the current utterance (2026-10-06)

### Outcome and boundaries

The owner confirms the USB mouse mic button already replaces HeyHey for
starting command recognition. A second press during an active Vietnamese STT
turn should submit the latest nonblank partial transcript immediately, without
waiting for Google's final callback. Apply this to ordinary commands, numbered
selection/confirmation replies, and Chat turns while STT is listening. Keep
normal final-result behavior when no second press occurs. No backend/protocol,
provider, microphone-route, or generic media-key change is planned.

### Inputs and constraints

Current `VoiceCommandActivation` admits idle states only, so a press during
`TRANSCRIBING` is rejected. `VoiceAssistantService.CommandListener` displays
partials but routes only `onResults`; recognizer cancellation currently discards
the turn. Android permits zero partial callbacks, and `stopListening()` still
waits for `onResults`/`onError`, so immediate submission must use a captured
partial rather than rely on a forced final result. Preserve readiness/unlock/
pause/stop guards, the button debounce, one recognizer per turn, and the
existing no-action behavior when no text is available.

### Decision questions and evidence

- Owner selected immediate partial submission over waiting for Google's final
  result, and selected all active STT turns rather than ordinary commands only.
- Verify the second physical press reaches the same supported assistant entry
  while Google STT owns the microphone. If Android does not deliver it, record
  the measured route and do not claim device acceptance from injected presses.
- The existing D-023/D-031 partial-no-dispatch rule needs an explicit exception
  for this user-triggered submission; ordinary partial callbacks stay display
  only. Update those durable decisions when implementing the behavior.

Android references accessed 2026-10-06:
https://developer.android.com/reference/android/speech/RecognitionListener
https://developer.android.com/reference/android/speech/SpeechRecognizer

### Milestones

1. Extend guarded button admission so an active STT turn receives a submit
   request, while busy routing/execution/speaking and suppressed states stay
   rejected. Do not interpret a second press as a new recognizer request.
2. Keep only the latest partial text for the current epoch. On submit, consume
   the turn once, cancel/destroy STT, and send nonblank text through the same
   final-text routing path used by `onResults`. Ignore later callbacks. With
   no text, end listening with existing no-speech feedback and no dispatch.
3. Update transient subtitle/ready state and metadata-only timing for manual
   submit. Add tests for valid/empty partial, fast repeat, late final/error,
   epoch replacement, and command/selection/confirmation/Chat routing.
4. Run JDK17 Android unit tests, lint and APK build; then validate the actual
   USB mic button on Fold3, including time from second press to dispatch and
   exactly-once execution. Record unavailable SDK/ADB/device gates as open.

### Validation and acceptance

Pass `./gradlew test lint assembleDebug --max-workers=1` and `git diff --check`
in an Android SDK/JDK17 environment. A real two-press USB-mouse turn must route
the visible partial promptly, with no later duplicate from Google. Empty
partial must execute nothing. One-press/HeyHey final-result behavior, selection,
confirmation and Chat follow-ups must still work. Keep T-006 `IN_PROGRESS` until
physical acceptance; a host build alone is insufficient.

### Risks and progress

Partial text may be incomplete or wrong, and Google may provide none. This is
the owner-selected speed/accuracy tradeoff. The button may be consumed by the
recognition service on its second press; measure that before changing key
bindings.

Implementation update 2026-10-06: Android now admits a second supported
hardware invocation during `TRANSCRIBING`, captures the latest nonblank partial
for the current recognition epoch, consumes the epoch once, destroys the
recognizer and routes the captured text through the same final-text handler as
Google `onResults`. Empty partial uses existing no-speech feedback; late final,
error and partial callbacks are ignored after recognizer teardown. Regression
assertions were added. JDK17 `./gradlew test lint assembleDebug --max-workers=1
--offline` PASS on 2026-10-06: 133 unit tests per app variant, zero
failures/errors and one opt-in skip per variant; aggregate lint and APK build
PASS. APK was signed with the existing Fold3 test certificate. Two ADB
incremental installs reported success but the package later disappeared;
`adb install --no-incremental -r` persisted through a follow-up package query.
No manual uninstall or clear-data command ran; data retention remains unknown.
After owner re-enabled Accessibility, Android confirmed its service enabled and
bound, and owner granted RECORD_AUDIO; readback confirms it. No permissions
were changed by automation. The owner performed a physical second press during
a partial, but metadata shows no `stt_manual_submit`; Google final arrived and
the safe invalid-screen test command dispatched afterward. A USB consumer-control
`KEY_VOICECOMMAND` down/up was captured during a separate attempt when the
service was `PAUSED by user`, so it was correctly rejected. A fresh attempt
while actively listening remains necessary. Samsung's default-app chooser does
list DeX Assistant. During navigation to that chooser, an ADB UI tap appears to
have selected it unintentionally. RoleManager now reports DeX Assistant and app
health reported READY/TRANSCRIBING. Owner confirmed keeping DeX. Three trial
epochs logged manual partial submit 13–14 ms before one dispatch each, with no
subsequent Google final. Terminal results were INVALID_SCREEN_COMMAND,
SEND_FAILED and VERIFIED; the owner was asked whether the last result changed
the screen. The filtered raw USB capture was interrupted before its pipe
flushed. No further trial ran; key correlation and safe outcome remain open.
Full Fold3 evidence:
`.agent/evidence/T-006-second-mic-fold3-2026-10-06.md`. D-023/D-024/D-026/D-031
and SRS wording were aligned; backend protocol is unchanged.

### Decision and requirement updates

Decision/SRS exception is recorded in D-023/D-024/D-026/D-031 and `docs/SRS.md`.
T-006/STATUS record the passing host checks and remaining physical gates.

## Decision and requirement updates

Record durable activation policy in DECISIONS after implemented/validated;
update T-006/T-009 and STATUS with exact compatibility evidence and limitations.
User-facing local command list goes in Android docs/LOCAL_COMMANDS.md.
