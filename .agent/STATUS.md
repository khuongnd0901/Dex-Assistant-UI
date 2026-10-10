## 2026-10-10 — T-018 automatic Fold3 acceptance (IN_PROGRESS)

Main-agent physical results: Gemini Settings UI save/update/delete/readd, model
API discovery, auth probe, encrypted key equality and Activity recreation
persistence PASS1/1 (12.246s) at owner-requested providerTestRpm15. Text Chat
three context turns plus immediate-end event suppression PASS; invalid-credential
single error/no2s replay/session restart PASS. Local volume with implemented
cloud providers disabled and restored, direct30%, and distinct compound20%→60%
PASS1/1 (12.349s); compound emitted two executionLatency metrics for one request
and verified final60%. Volume restored. Already-enabled Wi-Fi idempotent local
enable VERIFIED/LOCAL PASS; no off/on cycle. Seven safe configuration guards PASS.
These are automatic typed/text tests, not natural speech or audible TTS evidence.

Postflight (main agent): voice policy byte-identical; permissions/data inodes,
Device Owner descriptors, Assistant role retained; Accessibility bound; private
provider-input file deleted. Owner history retained with reencrypted25308 bytes;
plaintext history equality was not measured. Key stayed encrypted on-device,
never in test arguments/source/logs. Owner approved private ADB input for this run.

Android changes are test/preflight/memory only. Retired obsolete backend WebSocket
acceptance fixtures explicitly with @Ignore; preserved local tests and fixed
current dashboard signature. Added DirectChatState deterministic15-minute expiry
boundary/clear test, guarded live Settings/Chat/Command fixtures and validated
providerTestRpm1..15 (default2;15→4100ms). Settings uses actual renderer semantics
without coroutine TestScope for --no-restart compatibility. Test history uses
unique prefs/key aliases; provider enable flags and test-invalid credential are
restored in finally. Production code and production quota policy were not changed.

Host JDK17 + Linux SDK offline ./gradlew test lint assembleDebug
assembleDebugAndroidTest --max-workers=1 PASS201 tasks (19s),
/tmp/t018-rpm15-build.log; final affected lint + assembleDebugAndroidTest PASS108
tasks (16s), /tmp/t018-local-cloud-disabled-build.log. App debug/release176 tests
each,0failures/errors,1skip; DirectAiChatTest9/9 PASS. Script bash -n and
git diff --check PASS. Main owns physical evidence and final task status.

T-018 remains IN_PROGRESS under owner automatic-only scope. Next: natural
Command/Chat/microphone/TTS acceptance when owner participates; Groq/live429
fallback, exact401/403 no-fallback, timeout, genuine in-flight voice cancellation,
latency samples and resource/soak gates remain OPEN. Immediate-end event suppression
does not establish a completed remote reply after cancel or voice-service rearm.
Retired backend fixtures are skipped, never accepted. No push in this session.

## 2026-10-09 — Android-only Command + Chat migration (source committed; physical acceptance OPEN)

Owner requested all AI Chat moved off backend and no backend runtime dependency before one final real Fold3 test. Source now includes `ai/DirectAiChat.kt` (Gemini/Groq text-only API, bounded 20-pair context, local time and privacy guards) and Android `AssistantCoordinator`-owned Chat session + async cancellation/late-result epoch gates. Voice start/end TTS flow retained. Existing Local Command and direct `MobileAiProxy` Command use guarded ActionRouter. `AssistantController` removed WebSocket listener/construction, backend provisioning/connection and external quotas; `VoiceAssistantService` no longer connects WebSocket; `MainActivity` has no backend URL/token/remote quota settings. Retained unused protocol fixture code is not an active backend call path.

Work includes Android commits ec8ab7d, f9c7c3a, ae26161, ff85379, f300b9c, e42422e, 2d02d69, 9fb3a88, d724583. CI: https://github.com/khuongnd0901/Dex-Assistant-UI/actions/runs/37878337122 ; verify terminal PASS before claiming. No physical Fold3 test performed via this GitHub-only connection. Device signing/data/permissions have not been changed. Do not mark task DONE without the single controlled physical acceptance and explicit proof of direct network operations. Canonical evidence: https://github.com/khuongnd0901/DeX-Assistant/blob/main/.agent/evidence/mobile-direct-M4-command-chat-backendless-2026-10-09.md ; Fold3 task https://github.com/khuongnd0901/DeX-Assistant/blob/main/.agent/plans/active/T-018-direct-ai-final-fold3-acceptance.md

## 2026-10-09 — Five-provider Settings + Direct AI Command pilot (SOURCE PUBLISHED, ACCEPTANCE OPEN)

User requirement extended to independent Enable/API key/default model for **Gemini, Groq, OpenAI (ChatGPT API), Grok (xAI), DeepSeek**, with selectable preferred provider and model list discovered from each provider's own models endpoint. Per-provider secrets stay on-device via Android Keystore, never in source/Git/build. Source includes \`MobileAiProxy\` Gemini/Groq tool-call adapters and same strict \`DeviceCommandProtocol.decode\` + \`ActionRouter\` execution path as backend. Direct Command opt-in defaults **OFF**; local command unchanged, Chat remains legacy WebSocket. OpenAI/Grok/DeepSeek are **configuration-only**, never silently routed to a paid API; model list does not certify Free Tier. Unimplemented chosen default fails closed.

Source commits include e6369fa, 2aaee47, 6471a32, 094172c, 1374048, 7be7bda, 8de7eee, a7b0cf3, 77eb221, e536f39, ddf4369, 9015444, 61e4c90, de5ba00. CI/physical/provider account acceptance must be verified independently, and source completion is not milestone completion. Canonical evidence: https://github.com/khuongnd0901/DeX-Assistant/blob/main/.agent/evidence/mobile-direct-M3-proxy-multiprovider-2026-10-09.md

## 2026-10-09 — Direct AI API provider Settings slice (SOURCE PUBLISHED; acceptance OPEN)

Owner requested API keys configured exclusively in the Android app UI (never committed to Git/source). Published provider-separated AES/GCM Android Keystore storage, model/enable preferences, Compose Settings controls for Gemini/Groq (masked key, save/update/delete, test connection), Gemini/Groq authentication-only REST probes and pure unit checks.

Android commits: d453c127, 936a0e2, 44948db, 05692e0, c03108f. Existing backend WebSocket Command/Chat routing, Device Owner, Accessibility, wake/STT/TTS, signing identity, and stored device data have **not** been intentionally changed. **CI PASS** for implementation commit c03108f: https://github.com/khuongnd0901/Dex-Assistant-UI/actions/runs/37874784801 — `testDebugUnitTest lint assembleDebug`, APK artifact upload. Host validation only; physical/key tests remain OPEN.

**NOT ACCEPTED / NOT TESTED:** real free-tier account/model compatibility, live Gemini/Groq call, real signed Fold3 provider Settings and persistence test, physical voice/command regressions. Model-list probe verifies only authorization, not inference or free pricing. Do not label M0/M1/M2 DONE. Canonical evidence: https://github.com/khuongnd0901/DeX-Assistant/blob/main/.agent/evidence/mobile-direct-M2-settings-2026-10-09.md .

## T-017 real pilot and source publication — 2026-10-08

Owner-consented local recording pilot:12 real references,180 Fold3 WAV decodes
PASS; WER5.263%, CER4.444%, exact transcript10/12. Parser-only exact typed
 action9/11 scorable references and wrong opposition1/11. Production remains
Google SpeechRecognizer; local Sherpa is benchmark-only. Existing voice control
was temporarily PAUSED and restored LISTENING. Original testAPK restored;
policy/history unchanged; no production source/app update, grants or roles changed.

Tooling and aggregate evidence are published on
[task/T-017-sherpa-vi-asr-benchmark](https://github.com/khuongnd0901/Dex-Assistant-UI/tree/task/T-017-sherpa-vi-asr-benchmark);
[results and limitations](https://github.com/khuongnd0901/Dex-Assistant-UI/blob/task/T-017-sherpa-vi-asr-benchmark/docs/benchmarks/sherpa-vi-zfold3/RESULTS.md).
Python12PASS; focused Android testAPK/lint58tasks PASS; physical PCM2/2,
no-mic UI smoke1/1, collector1/1 and parser scoring3/3 PASS. No private audio,
reference transcripts, raw logs, model weights or APKs published. Branch remains
separate from main. T-017 IN_PROGRESS:200 utterances/noisy coverage, live Google/
Sherpa A/B, independent endpoint timing, controlled cold load,100mic/KWS sessions,
soak and commercial/distribution rights remain open. Preserve other tasks.

## 2026-10-08 Fold3 automatic-only current-source physical validation

Source HEAD `2e7ecd7`; SM-F926B/API35, Samsung DeX display2 (1920x1080).
JDK17 full test/lint/app+test assembly PASS201 tasks in5m13s; app156 tests
per debug/release, zero failures/errors,1 expected skip each. Current app/test
APK copies signed with existing certificate `9ddb7f47…abd83d03`, both
`install --no-incremental -r` SUCCESS. No uninstall/data clear/grant/role change.

Initial selected instrumentation33 cases:28 PASS,4 FAIL,1 expected AOSP-only
skip. Config9 PASS: public Maps query visible/DISPATCHED display2; Maps+YouTube
pair VERIFIED with disjoint windows; 1000-pattern/300sample warm matcher
p95=0.370469ms. Scoped grid9/10 PASS (real35s expiry, synthetic gestures on
fixture only);100prepare-clear initially SCREEN_CHANGED, log shows window
 topology6→5; focused rerun PASS1/1 in110.086s. Synthetic100cycle metrics:
prepare p50=671ms,p95=722ms,max736ms; initial/final PSS194308/199942KB,
peak sampled210416KB; elapsed107628ms,CPU65416ms. This is not100natural
voice sessions or an SRS latency/resource acceptance population.

Subtitle4/4 PASS; dashboard renderer1/1 PASS across320x480,800x400,600x320.
Separate production MainActivity launch/readback: resumed on display2,
freeform bounds[60,60,1873,1080],240dpi,1209x680dp; surface HAS_DRAWN,
shown/on-screen/visible. Native model silence/release, endpoint hints, external
recognizer resolver and canned Vietnamese TTS diagnostic4/4 PASS. TTS case
reports outcome, not guaranteed successful audible synthesis.

YouTube preview FAIL initially and focused rerun (expected VERIFIED,REJECTED);
read-only observer PASS both times but0semantic cards,45nodes,0caption structures.
Therefore current actual YouTube indexing/overlay readiness is unverified.
Isolated real-provider backend WS/headless fixture FAIL twice with terminal
APP_PAIR_WINDOW_MISSING versus expected DISPATCHED. Hardware handoff fixture
FAIL at obsolete second-trigger rejection assertion: current T-006 deliberately
accepts TRANSCRIBING to submit partial. Prior recorder-release/readiness checks
passed; this fixture does not establish current complete handoff acceptance.
No production or fixture edits made during this test request.

Owner policy byte-identical before/after, assistant role remains this app,
Accessibility remains enabled/bound. History55 records with exact same ordered
IDs/timestamps before/after; ciphertext changed because normal history reads
re-encrypt. Full decrypted content equality was not measured. No natural speech,
physical microphone-button gesture, calls, wireless Wi-Fi toggles, Chat or
production provisioning tests. Canonical task/evidence updates belong to main.
Raw logs/artifact copies remain private under `/tmp/Fold3-20261008-*`.

# Current Android status - 2026-10-08

## T-016 dashboard redesign - full local catalog host/AVD verified, physical DeX pending

Main view keeps voice controls, status, transcript, typed command and featured local examples. Full catalog now derives all 47 supported definitions/180 patterns from commands.json (3 unsupported Bluetooth patterns excluded), grouped in nine ordered intent groups with canonical phrase, expandable aliases, context/permission notes and accent-insensitive search. Tap fills only; short full-catalog mode auto-returns to visible input/Run after a phrase is chosen. The main page stays fixed and only the command list scrolls. AOSP AVD DeX_Command_Test_API35/API35, Windows ADB 5038 serial 127.0.0.1:5555: latest DashboardLayoutTest PASS1/1 across 320x480, 800x400, 600x320, including compact full view, canonical/alias fill and no send until Run. App/test APKs installed with `install -r`; no data clear or Fold3 mutation. JDK17 final `./gradlew test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1` PASS201 tasks; app unit tests156/debug and156/release, zero failures/errors and one expected skip each. Catalog unit test derives and checks exact supported pattern count, all phrase examples route LOCAL, nine groups and accentless search. Final visual polish: canonical and alias phrases are left-aligned within full-width touch targets; group headings use titleSmall with spacing between groups. JDK17 `:app:assembleDebug` PASS38 tasks and AVD `DashboardLayoutTest` PASS1/1 after this change. AOSP AVD cannot establish actual Samsung DeX placement or mic behavior; physical Fold3 acceptance remains open.

## T-015 current host implementation — host verified, Fold3 acceptance pending

M4 monotonic metrics correlate request/epoch, include actual router dispatch,
semantic resolution/revalidation, confirmation wait, bounded paths and terminals.
Earlier physical original-M4 sample:8local requests,6DISPATCHED/2SCREEN_CONTINUE,
speech-end callback→action p95 92.822ms; small sample is not SRS acceptance.
Owner's transient wake miss recovered; cause remains open. No wake model,
threshold or microphone-route change.

Current source additionally fixes configured YouTube numeric selection before
the reserved screen grammar, `chia ô số 4`, and UNMUTE `mở tiếng`/`mở âm thanh`.
Command-only STT complete/possibly-complete silence hints1500L/1000L retain
final/manual-submit handling and baseline Chat/diagnostic recognition. Providers
may ignore the hints; natural comparison has not run.

YouTube launch/search now bounded-observe selected DeX cards, exact query field
for search, prepare semantic indexes and report readiness without claiming
playback or rendered pixels. Service-owned RAM map expires after35s without
extension by voice preparation; complete ordering/window/card identity is
validated before atomic consumption into common spoken confirmation. Expired/
changed/consumed maps retain a stale marker until explicit new command; raw
`bấm số` cannot bypass confirmation. Feedback and voice preparation preserve
active map; cancellation/Stop/Pause/display/window/card change clear it.

Final JDK17 `./gradlew test lint assembleDebug assembleDebugAndroidTest
--max-workers=1 --offline` PASS1m21s (201 tasks): app154 tests per variant,
zero failures/errors and1 opt-in skip each; DPC5 per variant PASS. Focused
semantic-session/engine/YouTube tests PASS; `git diff --check` PASS. Final log
`/tmp/T011-close-contract-android-build.log` (final close-contract source);
auto-index source also passed `/tmp/T015-preview-scoped-final-build.log`. New opt-in physical
fixtures: YoutubePreviewDeviceTest opens YouTube and checks map/overlay attachment
through feedback/preparation without selection/gesture (pixelsVerified=false);
YoutubeSemanticObserverDeviceTest emits only scoped card counts/caption resource
IDs/classes; VoiceRuntimeDeviceTest checks endpoint-hint Long extras.
T-011 APP_CLOSE_WINDOW contract-only support now matches backend schema2:
strict configured `{app}`, schema1 rejection and copied shared fixture. Runtime
still advertises schema1 and excludes APP_CLOSE_WINDOW/CLICK_TARGET; direct close
execution returns CAPABILITY_UNAVAILABLE. No close command/guessed caption control
was added. Final contract-source test/lint/app+test APK validation PASS; actual close executor
and physical acceptance remain pending structural evidence.

Main agent owns signing/install and natural Fold3 launch/search/index-confirmation,
STT comparison, semantic/fallback/backend populations and physical regressions.
Preserve owner data, keys, grants and existing unrelated changes. Canonical task/
plan/evidence remains in backend repository. No commit/device mutation by this
Android agent.

## T-015 M2 semantic DeX resolver — host implementation, Fold3 acceptance pending

Android now has a bounded local CLICK_TARGET resolver: configured package and selected Samsung DeX window, exact selector fields, role and card-local context, clickable ancestor, deterministic ordinal after filtering, spoken choice/confirmation, live tree revalidation and one gesture. YouTube cards retain artist/context for spoken choices; duplicate titles with missing or identical artist metadata fail as TARGET_AMBIGUOUS before any gesture. Production still advertises schema 1 and excludes CLICK_TARGET until physical Fold3 acceptance. Next: run semantic fixture and natural YouTube/Maps/paired-window acceptance on Fold3, then enable schema 2 only after readiness is proven.

Validation: JDK17 offline `./gradlew test lint assembleDebug assembleDebugAndroidTest --max-workers=1` PASS (201 tasks); final source rerun `./gradlew test assembleDebug --max-workers=1` PASS (131 tasks), including 8 resolver/adapter tests. Scoped diff check PASS. No ADB device was available; physical DeX execution remains unverified.

## T-015 M1 contract — verified

Schema-2 validation and the shared fixture passed on both repositories. M2 adds direct local semantic execution under selected DeX scope, spoken confirmation and live revalidation. Remote capability remains disabled at schema 1 until Fold3 acceptance; see the M2 status above and canonical T-015 evidence.

## T-006 second USB mic press submits current STT partial — IN_PROGRESS

Implemented the owner-requested second supported hardware-mic invocation during
`TRANSCRIBING`: retain latest nonblank partial for the active epoch, consume the
turn once, destroy Google STT and pass text through the existing final-text
handler (ordinary commands, selection/confirmation and Chat). Empty partial
executes nothing; subsequent partial/final/error callbacks are stale. Added
host admission/exactly-once regression assertions. JDK17
`./gradlew test lint assembleDebug --max-workers=1 --offline` PASS: 133 app
tests per debug/release variant, zero failures/errors and one opt-in skip each;
aggregate lint and APK build PASS. APK SHA-256
`d0fced0a153d1234e688b439f8fd514596f6b8e7f29313b98bed4e0d08c701b8`, signed
with the Fold3's existing certificate `9ddb7f47…abd83d03`. Two incremental
installs reported success but the package later disappeared; explicit
`adb install --no-incremental -r` persisted through a follow-up query. No manual
uninstall or clear-data command ran; data retention remains unknown. Owner
re-enabled Accessibility and granted RECORD_AUDIO; Android confirms both.
Initial owner attempt while a partial was visible had no `stt_manual_submit`:
Google final arrived at22:49:13.861 and command dispatch followed at22:49:13.881.
A later `KEY_VOICECOMMAND` down/up was captured while state was `PAUSED by user`,
so Pause guard rejected it.
Samsung chooser inspection found DeX Assistant as a digital assistant.
During ADB UI navigation a tap appears to have selected it without owner
intent; RoleManager now reports DeX, app readiness is READY and voice was
TRANSCRIBING. Owner confirmed keeping DeX for the test. Three recognition
epochs logged `stt_manual_submit` and a single dispatch 13–14 ms later, with no
later Google final. Terminal results include `INVALID_SCREEN_COMMAND`,
`SEND_FAILED`, and `VERIFIED`. The last may mean the screen changed; owner was
asked to check. Filtered raw USB capture was interrupted before its pipe
flushed. See canonical evidence
`/home/kunrg/DeX-Assistant/.agent/evidence/T-006-second-mic-fold3-2026-10-06.md`.

## Fold3 physical rerun — 2026-10-06

Owner connected SM-F926B/API35 at `192.168.1.18:34393`. Current source
`a3ae17a` built with JDK17: unit tests, lint, app/test APKs PASS; APKs signed
with existing test identity and installed via `-r`, preserving data. T-011
ConfigCommand 8PASS/1 AOSP-only SKIP; Maps address dispatch and Maps+YouTube
pair PASS on DeX display2; matcher p95 0.352ms. T-010 second full grid run
11PASS/1 opt-in SKIP, synthetic100 prepare/clear PASS. T-006 subtitles PASS4/1
metadata SKIP. Native model/resolver and canned offline Vietnamese TTS PASS;
STT exact offline `vi-VN` unavailable and real recognizer produced no callback
within30s without supplied speech. T-003 DPC Wi-Fi instrumentation lost wireless
ADB during its Wi-Fi toggle; device later answered LAN ping, but Wi-Fi state
could not be read back and old ADB port refused connections. Keep T-003 open.
T-014 active Chat/spoken exit/echo and natural Command/resource gates remain
open. Canonical details: `/home/kunrg/DeX-Assistant/.agent/evidence/Fold3-rerun-2026-10-06.md`.
Owner reconnected ADB at port `45283`; Wi-Fi is confirmed enabled. Direct mic
handoff, volume restoration, dashboard layout and read-only capture observer
also PASS. Natural utterance/Chat acceptance remains open.

## T-007 Android test target fallback — verified

The canonical `scripts/android/select-test-target.sh` prefers a connected
Galaxy Z Fold3 `SM-F926*`/API35 and otherwise starts the Windows API35 DeX-like
or foldable AVD. This checkout's `AGENTS.md` now records the D-drive SDK path,
actual `adb.exe` path, Linux Gradle SDK and emulator limitations. See backend
`scripts/android/README.md` and completed T-007 follow-up plan. On this host
the selector returned `DeX_Command_Test_API35`/`emulator-5554`; Windows ADB
port5038 confirmed API35, 1920×1080/160dpi and freeform=1. Linux ADB could
not reach Windows AVD localhost:5555. No Fold3 or AVD data was reset.

## T-014-09 Fold3 natural voice Chat acceptance — IN_PROGRESS

This execution container has no `adb` command or configured Android SDK;
`adb devices -l` failed with `command not found`. No physical Fold3 session,
microphone route, natural wake/three-turn Chat, real TTS, spoken interruption,
echo rejection, latency/resource, or Command regression was verified here.

Added an on-screen `Kết thúc trò chuyện` control in `MainActivity`. It sends a
local `ACTION_END_CHAT` only while the voice service exists. During an active
Chat session, the service requests `USER` end, invalidates pending callbacks,
cancels STT/TTS and audio focus, then returns to wake-word standby. The control
has not been run on device. Spoken exit during TTS is still unsupported by the
sequential microphone path; no echo-safe detector was added in this slice.

Validation: JDK17 `./gradlew test lint assembleDebug --max-workers=1 --offline`
failed before any task ran: `SDK location not found`, with no `ANDROID_HOME` or
`local.properties`. `git diff --check` PASS. No APK was produced or installed;
no commit/push. Keep T-014-09 IN_PROGRESS. Main agent owns canonical task and
evidence updates; stop after this task.

## T-014-07 host Chat integration demo — Android slice verified

Opt-in `ChatLiveDemoTest` connects the production Android `AssistantWebSocket`
over a real loopback WebSocket to the canonical backend fixture. On 2026-10-06,
the focused JDK17 host run PASS: three correlated Chat turns returned the exact
time/story/follow-up fixture replies, each completed an explicitly injected TTS
callback, and no Command action callback fired. The Chat observer path in
`AssistantController` does not call the Command history store. USER end was acknowledged; a
cancelled turn was not replayed after disconnect, and reconnect started a new
session after an 800 ms demo-only wait for asynchronous server close. The client
does not expose close completion, so this wait does not prove deterministic
reconnect timing. XML output: `/tmp/T-014-07-android-live-demo.xml` (one test,
zero failures; session and request IDs in stdout). Final-source JDK17
`./gradlew test lint assembleDebug --max-workers=1 --offline` PASS in 4m26s:
app 133 tests/variant (one opt-in live test skipped without fixture), DPC 5
tests/variant, lint and both debug APKs; CRLF-aware `git diff --check` PASS.
App APK SHA256 `47b3ad2b535250b28cabd566479252deec785e6a9f95574dc06ec6ffcd73038a`.
`VoiceAssistantService` also
needed the active-Chat `START` routing branch to compile with JDK17; it now sends
that utterance as an ordinary Chat turn. Canonical main agent owns final
T-014-07 evidence/status. No AVD,
physical microphone or real TTS was exercised. No commit/push or owner device
change. T-014-06 remains open for spoken exit during TTS.

## T-014-06 voice Chat/TTS — IN_PROGRESS

Whole-utterance Chat entry/exit, Chat-only turn routing, TTS reply playback and
wake-free follow-up listening are wired in the current worktree. Stop/Pause
cancellation invalidates Chat locally and requests remote end. Exact phrase
routing tests were added. `git diff --check` passes. `./gradlew test` was
attempted but Gradle stopped at startup because host Java is25.0.4.1 while the
Android build requires JDK17; lint/APK build were not run. Spoken exit during
TTS remains open: current capture is sequential and no echo-safe detector has
been implemented or verified. Canonical evidence:
`/mnt/d/DeX-Assistant/.agent/evidence/T-014-06-android-voice-chat-2026-10-06.md`.
Do not begin T-014-07 until requested. No commit/push or device changes.

## T-014-05 Chat session/protocol client — DONE

Android Chat V1 now has a volatile session UUID, start/turn/end/error frames,
correlated active-turn response parsing and disconnect/end invalidation in
`ChatSession`/`AssistantWebSocket`. Chat events stay separate from Command
actions and history. Focused lifecycle/WebSocket tests cover stale replies,
one in-flight turn, nullable start error, invalid session and Command isolation.
Final-source JDK17 `./gradlew test --max-workers=1 --offline` PASS: app130 tests
and DPC5 tests per variant. `./gradlew lint assembleDebug
assembleDebugAndroidTest --max-workers=1 --offline` PASS143 tasks; `git diff
--check` PASS. The old resource merge NPE was stale generated Gradle state;
moving its generated merger directories to `/tmp` allowed a clean build with
no tracked config edit. No commit/push or physical install. Canonical evidence:
`/mnt/d/DeX-Assistant/.agent/evidence/T-014-05-android-chat-client-2026-10-06.md`.
TTS, voice orchestration and runnable demo remain open; stop before the next
T-014 subtask unless separately requested.

## T-006 DeX STT subtitles — implementation and focused acceptance

Command subtitles now use guarded RAM-only redacted turn state, preparing/ready/
recognizing status, revised partials and independent three-second final expiry.
A separate selected-DeX renderer is above numbers; grid cleanup does not own
subtitles. Screenshot preparation and image revalidation suspend presentation
through a reference-counted compositor detachment barrier. Interruption/unbind/
destroy clear the store; display identity loss suppresses the current turn.

JDK17 `./gradlew test lint assembleDebug assembleDebugAndroidTest --max-workers=1`
PASS57s,124 app tests per variant, including aggregate app/DPC lint. CRLF-aware
`git diff --check` PASS. Original-cert app/test installed-r without reset; app
SHA25629ae7463c67dcde4ce2a4c51d4be9718d9c11954d81f0680c3eb57980dcb7997.
Final four subtitle fixtures PASS4/4 16.688s, including renderer closure release
on final expiry/cancellation. Latest test-only build/app lint PASS11s.
Physical Fold3 API35 DeX focused run4PASS1SKIP16.780s: four subtitle fixtures
pass, compact grid lacked explicit opt-in. Exact compact grid rerun with
`runVoiceGridFixture=true` PASS1/1 5.135s. Subtitle fixtures prove visible pixels,
focus, grid independence, suspended screenshot exclusion, production image
preparation/revalidation+gesture, overlap/latest/cancel/expiry/interrupt and
background no-overlay barrier completion. No broad owner-state-mutating suite.

Initial owner trial found subtitles vanished after first turn; read-only metadata
proved captures=0/detachInFlight=true. A no-overlay Choreographer wait stalled in
the background. Fixed no-op barriers to post completion directly; actual overlays
retain compositor barriers. All surface teardown now hides before detaching to
prevent delayed platform attachment resurrecting old numbers. First failed trial
remains evidence. Owner confirms subtitles on all three repeated natural turns,
three-second final retention and stale numbers gone. Metadata on prior fixed APK
9526b078…84c2d proves five natural/continuous turns with partial rendering before
final by5856–16200ms. The final APK differs only by releasing its obsolete drawing
closure during teardown; four device fixtures rerun on final hash pass. Natural
speech was not rerun on that last privacy-only APK. Legacy API30-33/34 renderer and resize/disconnect not
physically verified; pre-existing queued number detachment during preparation is
not an exhaustive compositor-race guarantee. T-006 overall remains IN_PROGRESS.

Evidence/logs: `/tmp/dex-fold3-20261005/subtitle-fixed-device.log`,
`subtitle-fixed-grid.log`, `subtitle-natural-metadata.log`,
`subtitle-fixed-natural-metadata.log`; build `/tmp/subtitle-ram-cleanup-build.log`; final device
`/tmp/dex-fold3-20261005/subtitle-final-device.log`.
Canonical main agent owns task/plan/SRS/evidence updates. No commit/push; preserve
prior dirty work and owner keys/history/grants.


Canonical tasks/decisions/protocol: `/mnt/d/DeX-Assistant/.agent/STATUS.md`.
Current priority T-011; T-006/T-009/T-010 physical acceptance remains open.
Owner-authorized implementation commit `15a64d3` pushed to origin/main on2026-10-05;
backend companion `60414f0` also published. Existing EOL-only changes remain local. No
release or physical APK install in this implementation. Preserve dirty
changes, original signing certificate, owner history, credentials and grants.

## Physical Fold3 verification2026-10-05
Current source built on this Linux host/JDK17:117tests per variant + app lint/
APKs PASS; original certificate used for install-r without reset. Test-only
readiness/app-pair additions build/lint PASS. Coordinated physical17case run
15PASS/1SKIP/1FAIL: matcher p95=0.349ms, semantic change/cancel/duplicate guards,
eight selected-DeX grid fixtures and Maps+YouTube APP_PAIR_VERIFIED on display2
with two960×1080 panes. Earlier first pair CANCELLED remains a cold-launch
limitation. Real Gemini compound FAIL APP_PAIR_WINDOW_MISSING; no E2E pass.
Samsung instrumentation restarts lose Accessibility binding; owner Settings
toggle during explicit180s readiness restores it (default10s unchanged).
Four voice runtime cases PASS, offline TTS synthesized. Natural wake/STT logs
show DISPATCHED/SCREEN_CONTINUE and permission/invalid-command errors; owner
confirms commands/cue but STT slow: ready→final6.55–10.34s versus speech-end
→final3–18ms and final→terminal25–71ms. Partial/acoustic end unmeasured;
next metadata/controlled endpoint diagnosis. No natural reliability/soak claim.
ADB remains192.168.1.18:40725; actual Android path `/home/kunrg/Dex-Assistant-UI`.
Evidence `/home/kunrg/DeX-Assistant/.agent/evidence/T-011-fold3-dex-2026-10-05.md`.
Older device-unavailable statements below are historical. No commit/push.

## Owner natural-flow corrections — physical checks and owner trial pass
T-011/T-010: APP_PAIR_VERIFIED now speaks success; app-pair guard checks
selected desktop identity/geometry independently of transient focused roots.
“Tìm địa chỉ Phà Cát Lái” matches local SEARCH_PLACE with original query;
Maps intent explicitly targets selected DeX. Compact ô2/ô8 supported; fresh
voice preparation/button activation retains valid live subdivision without
extending expiry. Explicit GRID resets; Pause/Stop/cancel still clear.
Host120tests per variant/app lint/APKs PASS69s; final test-only repair build/
lint PASS18s. Main signed/installed current app and verified device hash
3b5fcb8d…a967. Maps public query visible on selecteddisplay2 and pair VERIFIED
PASS; coordinator compact/spaced/fresh-prepare/continuous grid2→child8 tap
PASS5.170s, four safety/actual expiry cases PASS54.102s. Initial touch capture
fixture failure repaired test-only, no gesture retries/production tap changes.
Original continuous wrong-cell cause unproven; safe depth/geometry logs added.
Owner confirms all three corrected outcomes; live pair/descendant tap logs
corroborate. STT delay remains open. --no-restart
verified on Samsung preserves Accessibility. Evidence linked above. No commit.

## Latest stability review
Android117 unit tests in each debug/release variant, app lint and APK assembly
pass with Windows JDK21 and `--max-workers=1`. Fixed old WebSocket/TTS callback
and reconnect races, trusted local Chrome ID separation, controller late-action
identity checks, cancellable contact queue/idle worker, lazy Unicode offsets,
regex reuse and bounded Accessibility traversal. Six current APK-pair runtime
cases pass on isolated API35 emulator-5560, one DeX fixture skips; p95=3.235ms
for1000patterns. New AVD stopped; existing emulator-5556/data retained.
Historical signing-block/110-case results below are superseded for core runtime
validation, not Samsung DeX placement. Physical end-to-end speech/resource tests
and synchronous confirmed-contact provider latency remain open. Backend final Java25 verify57 tests and JAR packaging pass. Evidence:
`/mnt/d/DeX-Assistant/.agent/evidence/T-011-stability-review-2026-10-05.md`.

## T-011 implementation
- Application initializes immutable schema-1 `commands.json` registry once:
  48 definitions/176 patterns; strict validation, folded alias collision checks,
  original Unicode slots, full-literal-prefix index and last-good reload.
- Local engine and strict DEVICE_COMMAND use the same typed ActionRouter.
  Automatic AI fallback for unmatched/composite input; privacy, malformed phone/
  numbered commands and out-of-context interaction phrases fail locally.
- Voice interaction retains parent/action IDs, reads five choices per page,
  requires later spoken confirmation for every call/semantic click, revalidates
  recipient/target, consumes before dispatch, cancels on new command/Pause/Stop/
  STT/TTS error/timeout. Reply10s after successful TTS, step30s, total120s.
- Catalog: config apps, YouTube query/select/player/fullscreen/seek, Maps search/
  driving/walking, outgoing contact/number calls, Google search and social feeds.
  Private home/work/contact aliases and permission setup are in Settings.
- UI access requires the unique Samsung desktop category. Settings uses explicit
  launch display; unscoped Back/Home/Notifications and Bluetooth are unavailable.
  Media seek needs verified YouTube/Music foreground and supported active session.
  DISPATCHED proves delivery, not playback, arrival or connected call.
  Owner requests a spoken two-app DeX layout and intent-directed voice routing.
  APP_PAIR_LAUNCH uses configured IDs, the unique DeX display and observed
  disjoint app windows before VERIFIED. “Mở nhạc {query}” resolves to YouTube
  regardless of focused pane; Maps home/work remains a local saved alias.
  When Chrome is foreground, “tìm địa điểm {query}” resolves to WEB_SEARCH,
  explicitly targets configured Chrome, and rechecks foreground before dispatch;
  the same phrase in Maps remains a Maps search. Android app-pair/Chrome-context
  command tests and APK build pass. The current AVD instrumentation
  install was rejected because its existing package has a different signature;
  it was left installed with its data intact. `:app:lintDebug` passed; aggregate
  lint stalled during unchanged DPC analysis.
  Physical Samsung DeX placement and voice acceptance remain open.
- Owner requested window minimize, previous/next app switching, home/work and
  spoken new notifications. Home/work is already supported by saved local aliases
  and `đưa tôi về {destination}`. Minimize/switch need display-scoped DeX window
  controls; notification speech needs explicit Notification Access and an on-demand
  TTS path that excludes content from history/backend. These remain unavailable.
- Backend capability descriptor is action schema1 + supported IDs/configured apps;
  no contacts/place data/screens uploaded. Legacy V1 decoding is still validated.

## Verified host/AVD slice
Historical pre-app-pair baseline: JDK17 `./gradlew test lint assembleDebug
assembleDebugAndroidTest` passed with 105 app debug and 105 release tests. For
the current app-pair changes, Windows JDK21 `gradlew.bat test assembleDebug
assembleDebugAndroidTest` passed with 109 app unit tests and assembled both APKs.
After the Chrome-context route, `gradlew.bat test assembleDebug :app:lintDebug`
passed with 110 app unit tests, APK assembly, and app-module lint.
Focused backend tests `DeviceCommandTest,DeviceCommandContractTest,
DeviceCommandPlannerTest` passed; the earlier backend verify baseline passed 43
tests. Shared DEVICE_COMMAND fixture comparison and catalog/schema JSON parse
pass. `:app:lintDebug` passed; aggregate lint stalled at
`:dpc:lintAnalyzeDebug` and was stopped (DPC is unchanged).

Owner reports physical Fold3 unavailable; Windows SDK Manager22.0 with process
JDK21. Dedicated marked DeX_Command_Test_API35 API35 Google APIs x86_64 boots
1920×1080/160dpi, freeform/resizable; no Device Owner, Accessibility or mic/role
provision. Main/test APK installed only here. Six focused ConfigCommandDeviceTest
runtime cases PASS1.964s; one selected-DeX fixture assumption-skipped. Calls were
rejected/cancelled with injected TTS callbacks; no call/gesture/Wi-Fi dispatch,
contact query, grants or owner history changes. AVD p95=2.060ms/1,000patterns
(host debug0.209ms/release0.193ms); not Fold3 acceptance. Initial AVD ICU brace
regex crash fixed; malformed checked JSONException reload and duplicate TTS
completion guarded. Full-prefix index avoids scanning1,000same-first-word patterns.
MainActivity verified in1200×820dp freeform window and left open on emulator-5556.
An attempted current `connectedDebugAndroidTest` ran zero cases because the
emulator's installed `com.khuongnd.dexassistant.ui` package has a different
signature (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). The app was not uninstalled
and no emulator data was cleared; current app-pair device tests therefore remain
unverified. The prior six-case AVD result above belongs to the earlier frozen
APK pair and does not validate this follow-up.
Reproduction: backend `scripts/android/README.md`; evidence:
`/mnt/d/DeX-Assistant/.agent/evidence/T-011-host-avd-2026-10-05.md`.

## Retained prior evidence and guards
D-023 explicit coordinate grid survives content/focus changes on the same desktop
geometry, while numbered/semantic target identity remains strict. D-024 prepares
numbers alongside STT and keeps worker-owned native KWS with fresh streams;
4s canned feedback bound remains. D-025 encrypted FIFO history keeps IO off
terminal path; ordered clear, redaction, corrupt-data preservation and stale UI
publication guards remain. Controlled Fold3 final→terminal improved from
1852/1464/1639ms to40/41/38ms; not natural-speech latency.

D-026 observed mouse RecognizerIntent.ACTION_WEB_SEARCH chooser registration,
owner Always selection and physical repeat succeeded. Wheel/SCO still unverified.
Prior warm YouTube VIEW query and live grid YouTube/Settings fixtures passed;
previous combined19-case18/19 expiry failure remains prior evidence, with focused
reruns passing without relaxed guards. New extracted query semantics are T-011.
Isolated Fold3 Java JAR/Gemini compound and quota/reconnect tests passed before
this session; that is not current provider readiness/provisioning/voice acceptance.
See canonical T-006/T-009/T-010 evidence, not ephemeral old ADB/process state.

## Next action and limitations
When Fold3 is available: natural local/backend voice, select/confirm/cancel/
TTS timeout/partial duplicates, actual app-version targets/seek/feeds/Maps,
recipient changes and1,000-pattern Fold3 p95≤5ms. Actual call only at owner-chosen
recipient/time. Complete wake/cue/latency/resource/100session/soak gates separately.
Legacy LocalFirstDeviceTest clears owner history; never run broad old device suites
or Wi-Fi disconnect tests against owner state. Rediscover ADB, keys/signing and
backend readiness; no stale endpoint assumption. Production DeX guard must remain
strict: AOSP freeform can verify layout/core policies, not Samsung desktop actions.
