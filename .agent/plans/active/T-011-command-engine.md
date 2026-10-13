> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

> Scheduling override 2026-10-13: owner requests Windows simulator tests through WSL during continuation. All remaining physical Z Fold3 execution is consolidated in [T-020](T-020-consolidated-fold3-acceptance.md), one prepared campaign. This overrides older immediate/separate device-run instructions, preserves acceptance gates and does not imply PASS.

# T-011 — Config-driven commands and AI bridge

> 2026-10-10 owner architecture override: IN_PROGRESS, local command/interaction/adapters retained; outstanding backend WebSocket/routing acceptance CANCELLED. See [T-018](T-018-direct-ai-final-fold3-acceptance.md) and [task board](../../TASKS.md). Earlier backend requirements/progress are historical and do not schedule new backend work.

## Outcome and boundaries
Implement the approved user plan: config-driven Vietnamese whole-input local recognition, typed actions shared with backend AI, voice-only selection/confirmation, YouTube/Maps/outgoing calls/browser/social catalog. Existing system/media/Wi-Fi and numbered/grid behavior remains guarded. Exclude account actions, incoming calls, itinerary edits, remote config, OCR and generalized workflows.

## Inputs and constraints
Canonical TASKS T-011; coordinates T-006/T-009/T-010 without closing their acceptance. Read docs/PROTOCOL.md and docs/DEVICE_COMMANDS_V1.md for the centrally owned wire/action contract. Existing working trees are dirty and must be preserved. No owner history/credential/permission/ADB changes. Actual call only with owner-selected recipient/time.

## Decision questions and evidence
- Android media/Maps/call API: consult official Android references supplied in approved plan; record API/permission limitations distinctly from device results.
- App UI adapters: accept only identifiable targets on selected desktop, revalidate before dispatch; unavailable is preferable to uncertain target.
- Compatibility: legacy clients receive legacy tools; new descriptor restricts DEVICE_COMMAND actions/apps. Test both sides.
- Recognition: original slots and exact whole-input resolution; ambiguous actions never execute. Measure 1,000 patterns on Fold3 before performance acceptance.

## Milestones
1. Typed action catalog, JSON registry/compiler/validator, baseline and negative tests.
2. Migrate local commands and original YouTube query extraction; executor no longer depends on parser aliases.
3. Pending voice selection/confirmation, TTS-success→STT continuation, 10s reply/30s step/120s total limits and cancellation.
4. Catalog executors and guarded app adapters, local alias configuration and onboarding permissions.
5. Additive capability descriptor and DEVICE_COMMAND validation/planning; shared Android router.
6. Host integration/build/lint tests; device spoken local/backend/select/confirm/cancel/timeout and app regressions.

## Validation
Backend Java25 ./mvnw verify. Android JDK17 ./gradlew test lint assembleDebug assembleDebugAndroidTest; git diff --check in both repos. Unit/fixtures for slot normalization, collision/validation/reload, interaction revalidation and duplicate/cancel, Maps/query encoding, app classification and typed remote policy. Rediscover ADB before safe read-only or isolated device validation; never clear owner history or toggle wireless ADB Wi-Fi. Fold3 spoken and actual-call acceptance is not inferred from compilation/injected callbacks.

## Risks and escalation
App UI/version and active media support vary; report DISPATCHED versus VERIFIED truthfully. Global accessibility actions lack display argument and stay unavailable unless scoped implementation is proven. TTS error cancels pending action. Missing physical device/owner-selected call leaves those acceptance open.

## Stability and performance review follow-up
Owner requests another quality pass prioritizing reliable execution, contention,
response time and resource use. Review Android session/callback/interaction
lifecycle, command matching and Accessibility hot paths alongside backend plan
admission/result concurrency. Fix demonstrated issues with regressions; retain
privacy, spoken confirmation and selected-display policies. Validate affected
modules and record host matcher timings separately from Fold3 end-to-end latency.
No device owner data/reset/install or automatic action replay is authorized.

## Progress and findings
- [x] Read canonical/Android state and relevant source; existing uncommitted work recorded.
- [x] T-011 created, central shared contract defined before delegation.
- [x] Registry/migration: schema-1 bundled catalog, 47 definitions/151 patterns, typed original slots, immutable reload and prefix index.
- [x] Voice interactions implemented: shared parent/action continuation, TTS-success gating, pagination, consume/revalidation and cancellation deadlines. Physical speech acceptance remains open.
- [x] Catalog/adapters implemented with local aliases/onboarding; uncertain app targets/display/media capabilities return typed unavailable. App/device acceptance remains open.
- [x] Backend bridge: strict capability descriptor, provider-filtered DEVICE_COMMAND, shared fixtures and Android ActionRouter; legacy V1 compatibility tested.
- [x] Host validation: backend verify 43 tests; Android test/lint/app+test APK 105 debug + 105 release tests; schema/fixture/diff checks pass. See linked evidence.
- [x] Isolated AVD: six runtime tests pass, one DeX fixture skips; freeform layout demonstrated. Full-prefix index AVD p95 2.060ms after initial 14.968ms.
- [ ] Physical acceptance/performance: Fold3 unavailable per owner; actual calls require selected recipient/time.

## Decision and requirement updates
Update protocol/capabilities/operational docs and Android local-command docs to actual implementation. Record durable policy/contract in DECISIONS after implementation validation. Keep STATUS/TASKS aligned with evidence.


### 2026-10-05 implementation and emulator verification
Owner cannot connect the physical Fold3 and requested the Windows SDK emulator.
SDK Manager runs with process-local JDK21 and reports 22.0. A dedicated marked
`DeX_Command_Test_API35` uses installed API35 Google APIs x86_64; 1920×1080/160dpi,
keyboard, freeform/resizable flags. The retained foldable AVD was only booted and
stopped without wiping/installing/config changes. Script and reproducible focused
tests: `scripts/android/start-dex-test.ps1`, `scripts/android/README.md`.

Android runtime verification found JVM/ICU incompatibility in unescaped closing
placeholder braces; both braces now escaped. Reload catches Android's checked
JSONException as well as JVM exceptions. Duplicate TTS completion is ignored
without cancelling a valid pending prompt. Nearby Maps query keeps the spoken
nearby qualifier. Crowded-prefix benchmark led to full-literal-prefix indexing;
normalization/whole-input selection semantics remain unchanged. Focused AVD regression covers these runtime distinctions and
cancelled calls without dispatch. Do not infer physical speech/actual calls.

Official references accessed 2026-10-05:
- https://developer.samsung.com/samsung-dex/testing.html — AVD matching display/freeform conditions, not Samsung firmware or OEM display category.
- https://developer.android.com/studio/run/managing-avds — AVD components/profiles.
- https://developer.android.com/guide/components/google-maps-intents — Maps search/navigation intents.
- https://developer.android.com/guide/components/intents-common — call versus dialer behavior.

Host/AVD final commands and measurements are recorded in
`.agent/evidence/T-011-host-avd-2026-10-05.md`. Physical acceptance and Fold3 p95
remain open. D-027 records the accepted architecture; protocol, capability,
SRS and operational documentation now describe implemented policy.

### 2026-10-05 owner follow-up: hands-free navigation and app closing
Owner reports the Fold3 runs Android 15 and requests voice commands to close
YouTube, Chrome, Maps, Facebook, TikTok and similar apps, plus more convenient
hands-free phrases. Added app-switching, media, Maps-nearby, web-search, scroll,
and TikTok feed synonyms to the bundled catalog/docs. This is a phrase-only
catalog edit; matcher/runtime acceptance is pending. The exact close-app meaning
is awaiting owner clarification (DeX window close vs Settings force-stop vs both).
On Android 14+, public `killBackgroundProcesses`/`getAppTasks` APIs cannot stop
or manage another app's task. The viable close-window design must locate the
actual X control for the selected DeX window, announce it, receive spoken
confirmation, revalidate the window/control, and click only on the selected
desktop. ADB currently sees only the API35 AVD, which cannot verify Samsung's
DeX titlebar Accessibility node; keep close-app unsupported until physical
Android 15 DeX evidence exists. Source references are in
`docs/LOCAL_CAPABILITIES_V1.md` and `docs/DEVICE_COMMANDS_V1.md`.

### 2026-10-05 owner follow-up: window, Maps, and notification commands
Owner also requested voice minimize, previous/next app switching, saved
home/work navigation, and spoken new notifications. Existing
`đưa tôi về {destination}` resolves `nhà`/`công ty` only through locally saved
addresses, so those utterances are already supported when configured. Minimize
and app switching remain unavailable until DeX window controls can be scoped to
and revalidated on the selected desktop; Android global accessibility actions
have no display parameter. Notification reading is not implemented: it needs
explicit Notification Access, an on-demand voice-only response, and guarantees
that text never reaches backend/history, with privacy/unlock behavior reviewed.
See Android `docs/LOCAL_COMMANDS.md`; do not advertise these as executable until
implemented and verified.

### 2026-10-05 owner follow-up: two-app DeX and intent-directed routing
Owner requests a voice command to place any two configured apps side-by-side in
DeX, then continue addressing each by command intent while both remain open. The
example is Maps “đưa tôi về nhà” and YouTube “mở nhạc Sơn Tùng”. Add additive
typed `APP_PAIR_LAUNCH(firstApp, secondApp)` to schema-1 DEVICE_COMMAND; Android
must require the Samsung desktop category, launch both configured app tasks on
that display with adjacent/half-display bounds, and report success only after
Accessibility observes two distinct app windows with side-by-side bounds. Partial
launch, unsupported/freeform-restricted apps, missing display, or ambiguous app
aliases must report failure without claiming split success. Do not use global
Recents or alter the phone screen.

Add config phrase “chia màn hình {app1} và {app2}” and map “mở nhạc {query}” to
YouTube's existing fixed play-query flow. NAVIGATE already routes “đưa tôi về
{destination}” independently of focused app and resolves `nhà`/`công ty` through
local saved aliases. For all other generic actions, keep foreground scope as a
context signal, but explicit action/pattern intent takes precedence; ambiguity
must still fail closed. Only apps in the APK catalog/config and installed on the
device can be paired. Validate host protocol compatibility and AVD launch geometry;
real Samsung DeX split/freeform placement and hands-free command pairing remain
physical acceptance.

Primary references (checked 2026-10-05): Android's multi-window guide permits
`FLAG_ACTIVITY_LAUNCH_ADJACENT | FLAG_ACTIVITY_NEW_TASK` on API 32+ and warns
OEMs can override behavior; Samsung's DeX guide describes DeX as Android freeform
multi-window and says apps that refuse resizing can remain fixed-size. Links:
https://developer.android.com/develop/ui/views/layout/support-multi-window-mode
https://developer.samsung.com/samsung-dex/faq.html
https://developer.samsung.com/samsung-dex/modify-optimizing.html

### 2026-10-05 app-pair implementation result

Implemented typed `APP_PAIR_LAUNCH`, config phrase matching, distinct configured
app validation, Samsung display-scoped adjacent launches, and Accessibility
readback of both non-overlapping app windows. Added intent-directed tests for
Maps home and YouTube music commands while either pane is foreground. Backend
targeted tests, Android unit tests (109 app tests), and debug/test APK assembly
pass. See the evidence record for commands and exact results.

`:app:lintDebug` passed. Chrome foreground `tìm địa điểm {query}` now routes to
an explicit Chrome Google-search intent after foreground revalidation; the same
phrase in Maps routes to Maps. The data-driven 110-test Android unit suite,
debug APK assembly and app-module lint pass. The aggregate root lint task stalled
in unchanged DPC analysis and was stopped. Remaining validation: the current AVD instrumentation install was rejected
because the existing package has a different signing certificate, so it was
left untouched to preserve its data. The combined lint run stalled in
`:dpc:lintAnalyzeDebug` for more than ten minutes and was stopped. Only the AOSP
API35 AVD is attached; Samsung DeX layout, Fold3 voice routing, and physical
acceptance remain open. Keep T-011 `IN_PROGRESS`.

### 2026-10-05 stability implementation and verification
See `.agent/evidence/T-011-stability-review-2026-10-05.md` for prioritized
findings/fixes/regressions. Android117 cases per variant, APKs/app lint pass;
six current isolated API35 AVD cases pass/one fixture skips, p95=3.235ms.
Backend pending/admission/result/deadline/provider/store fixes are implemented;
final verify57 tests/JAR PASS. Protocol documents command-only quota, UTF-8 limits,
120s pending wait and per-connection replay boundary. Main reviewed cross-repo
callback/publication ordering. Physical DeX/speech/resources remain open;
contact revalidation and backend high-cardinality limits recorded as residuals.

### 2026-10-05 GitHub publication
Owner explicitly authorized commit/push in both repositories. Published
implementation commits to origin/main: backend60414f0 and Android15a64d3.
Reviewed53 backend and51 Android substantive/new files; existing15 backend
and36 Android EOL-only modifications remain local. Credential/artifact scan
found no known key patterns or build/database/signing files among staged paths.
Shared fixture bytes identical; staged whitespace checks pass with CRLF allowed.
Only an extra blank EOF was removed from SeekBounds after prior verification;
no behavior changed and earlier57 backend /117-per-variant Android test results
remain applicable. No force push, release, device install or data reset.
T-011 remains IN_PROGRESS for physical acceptance.

### 2026-10-05 physical Fold3 resumption

Current original-cert APK installed with data retained; host117tests/variant,
app lint/APKs PASS; backend63tests/JAR PASS. Physical coordinated17cases:
15PASS/1SKIP/1FAIL. Config/semantic cancellation and eight selected-DeX grid
fixtures PASS; matcher p95=0.349ms; Maps/YouTube pair VERIFIED at960×1080per
pane on display2. Earlier initial pair CANCELLED remains an unclosed cold-launch
limitation. Configured Gemini compound FAIL APP_PAIR_WINDOW_MISSING. Owner
voice smoke executes with audible cue but slow STT: ready→final6.55–10.34s,
speech-end→final3–18ms, final→terminal25–71ms. Need acoustic-end/partial timing
to diagnose endpoint delay; no latency/reliability/resource/soak closure.

Samsung instrumentation restarts invalidate Accessibility binding. Owner toggle
via Settings during explicit bounded readiness restores it; test-only bind
window and actual app-pair test added without permission or production changes.
Focused test APK/lint PASS after final changes. Task remains IN_PROGRESS.
Full result/failures/commands/limitations:
`.agent/evidence/T-011-fold3-dex-2026-10-05.md`. No commit/push.

### Owner natural-use corrections2026-10-05 (in progress)

Exact “mở song song Google Maps và YouTube” opens panes but speaks failure;
log terminal APP_PAIR_VERIFIED exposes missing voice-status translation.
“tìm địa chỉ Phà Cát Lái” is absent from bundled Maps patterns and can reach
backend rather than local Maps. Preserve typed action contracts and privacy.
Also coordinate T-010 “mở lưới → chia ô2 → bấm ô8”: compact number grammar
and wake preparation must retain the unexpired subdivided coordinate session,
while explicit GRID resets it and expired/stale display cases remain rejected.

Acceptance: pure speech-result/catalog/grid regressions, actual coordinator
grid2→subcell8 fixture tap, Maps search public fixture on paired selected DeX,
truthful pair spoken status/cold launch, host tests/app lint/APKs and physical
matching-sign install/demo. Existing successful direct-service fixtures do not
close these new natural-flow bugs. Android agent owns source/tests; main owns
canonical documents/integration/physical run. No protocol change planned.

Correction source/host/device checkpoint:120tests per variant/app lint/APKs
PASS; original-cert current app installed/hash verified. Physical pair and
selected-display public Maps query PASS; grid coordinator descendant tap
PASS5.170s, four safety/expiry cases PASS54.102s. Initial touch-fixture failure
was test capture/coalescing, corrected without production gesture changes or
retries. --no-restart keeps Accessibility bound. Owner confirms all three
correction outcomes; continuous wrong-cell original cause remains unproven. Metadata can now
distinguish grammar/grid depth/gesture coordinates; STT delay remains open.

Final owner natural acceptance: all three reported flows correct on installed
currentAPK. Live pair verified and descendant tap geometry observed. See
physical evidence for exact parent-number distinction and remaining gates.

### 2026-10-06 ADB simulator follow-up

The existing `ConfigCommandDeviceTest` ran on `DeX_Command_Test_API35`
(Android15/API35, 1920×1080/160dpi): 6 passed, 3 explicit assumption skips,
0 failures. The 1,000-pattern AVD matcher p95 was 0.352ms. Samsung display and
Fold3-only fixtures remain open; do not count skips as DeX verification. No APK
install or app-data/permission change was made. The launcher timed out because
the emulator registered on ADB port5037 instead of configured5038; the existing
emulator was tested directly on5037. Full evidence:
`.agent/evidence/T-011-avd-adb-2026-10-06.md`. T-011 remains IN_PROGRESS.


### Final source publication checkpoint - 2026-10-07

Owner authorized commit/push in both repositories. Backend Java25 verify:
78 tests, zero failures/errors, 1 expected skip; focused schema2 contract4 PASS.
Android final JDK17 tests/lint/app+test APK:201 tasks PASS,154 app tests per
variant (zero failures/errors,1 expected skip),DPC5 per variant PASS. Shared
schema2 fixture byte comparison PASS; latency summary unit tests4 PASS.
Owner confirmed closing the YouTube DeX window. The additive APP_CLOSE_WINDOW
schema2 contract is strict and configured-app scoped; Android keeps it
unadvertised and CAPABILITY_UNAVAILABLE pending caption evidence and a safe
executor. This clarification supersedes the earlier pending meaning.
T-015 preview/numeric/search/unmute/STT-hint source passes host checks; latest
APK installation and physical acceptance remain unverified. Tasks stay
IN_PROGRESS; publication does not close device or latency acceptance gates.
