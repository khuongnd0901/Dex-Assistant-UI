# Current Android status — 2026-10-05

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
