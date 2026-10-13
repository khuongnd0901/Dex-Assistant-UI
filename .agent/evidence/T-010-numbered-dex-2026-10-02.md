> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-010 numbered DeX control — 2026-10-02

## Environment and read-only device evidence
Implementation checkout: /home/kunrg/Dex-Assistant-UI. Canonical memory:
/home/kunrg/DeX-Assistant. JDK17 available at
/home/kunrg/.local/opt/jdk-17.0.20.1+1; SDK /home/kunrg/Android/Sdk.

`adb devices -l` reports one connected Samsung SM_F926B. Read-only
`adb shell getprop ro.build.version.sdk` returns 35. `adb shell dumpsys display`
reports active EXTERNAL_DEX viewport displayId=2, 1920×1080. This is a snapshot,
not a hard-coded target ID or proof that Accessibility can click on that display.
`adb shell settings get secure enabled_accessibility_services` returns null.
No emulator was connected at discovery. No permission change, gesture, screenshot or user-data collection was performed
for this initial discovery evidence. Subsequent matching-signed upgrade is below.

## Automated verification
JDK17 pinned voice asset preparation passes (`/tmp/t010-assets.log`).
Corrected numbered-screen unit suite passes: 44 app + 5 DPC tests, zero
failures/errors/skips, including seven new screen tests. The first actual tests
caught selector suffix confusion and a rectangle falsely recognized as inverted
X; implementation was corrected while preserving fixture expectations. Final full JDK17/API35 command `testDebugUnitTest lint assembleDebug
assembleDebugAndroidTest` PASS,153 tasks/1m4s (`/tmp/t010-final-validated.log`);
previous corrected full run also PASS/2m33s. Both repositories `git diff --check`
PASS. Four-case instrumentation APK compiled; device execution pending.

Signed review artifacts (outside Git, original verified certificate):
- /tmp/t010-device-ready/t010-app.apk:98,410,786 bytes;
  SHA256 4a8c64e1234a29fc264dac613352383b320dac60aca3fb5be3e26ed79b88d03b
- /tmp/t010-device-ready/t010-test.apk:2,306,738 bytes;
  SHA256 4d536f9347fb3e877493634a31cda795252dca0821dbc1aac703fb11962ad544

On 2026-10-03 both matching-signed app/test installs return Success. DPC was
not replaced; main app data retained. Focused actual Fold3 instrumentation:
`NumberedScreenDeviceTest#numericAndMalformedFamiliesNeverBecomeBackendCommands`
PASS `OK (1 test)`,0.494s (`/tmp/t010-fold3-local-routing.log`). Numeric and compound
screen commands remain local with the backend disconnected; encrypted history
ciphertext is restored in finally and the key is not rotated. This is routing
and missing-permission behavior evidence, not a live socket packet capture or
physical gesture evidence. Owner-mediated Accessibility activation requested;
no shell force-enable performed. DeX gesture/positive speech/100-session
measurement remains open.
After the focused test, wireless ADB became offline then disappeared; subsequent
`adb devices -l` and `adb mdns services` show no device. No reconnectable new
port was discovered. Owner setup question remains pending; no gesture tests run.

Installed app APK was pulled read-only to /tmp solely for public signing
verification. Its certificate SHA-256 matches the existing local test key and
required identity `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
No key material was printed/copied into this repository.

## Required physical acceptance flow
1. Install a reviewed app/test artifact signed with the existing device-test
   certificate; retain the installed DPC and settings. User enables the service
   through Accessibility Settings. Never force-enable it through shell settings.
2. On DeX run the dedicated secondary-display fixture. Verify number labels do
   not steal focus or mouse input. Tap known cells and targets; fixture records
   display ID and delivered coordinates. Verify a no-node custom X control.
3. Speak Hey DeX, then the command after readiness. Test digits and spoken
   Vietnamese numbers, full grid and three splits. Split needs no new wake;
   click/cancel returns to wake. Invalid/compound numbers must produce no
   backend command.request and no gesture.
4. Move/resize windows, cover a target, disconnect DeX, revoke Accessibility,
   cancel, pause/stop and cause STT timeout. Old numbers/late callbacks must not
   click; overlay must disappear and phone must receive no fallback gesture.
5. Test popup X and YouTube close/skip when visible. Distinguish a dispatched
   gesture from a verified closed popup; save only sanitized outcome evidence.
6. Measure 100 sessions: wake-to-ready, command-end-to-dispatch p50/p95, CPU/PSS,
   battery/thermal, bitmap retention and overlay cleanup. Compare idle baseline.
   Do not infer spoken accuracy or resource stability from synthetic unit tests.

T-010 remains IN_PROGRESS until these observations are recorded. T-006/T-009
open voice/provider acceptance is not replaced by this feature.

## Connection and activation continuation
Owner restored connection at192.168.1.18:44105 and requested continuation.
SDK adb37.0.1/OpenScreen mDNS did not discover a service; targeted read-only
known-host port discovery found38149/44105. Connected SM_F926B identity confirmed.
Owner reported Accessibility enabled, but subsequent current-user0 settings
readback returns null and dumpsys Bound services:{} (service is installed).
Opened ACTION_ACCESSIBILITY_SETTINGS; owner clarification/main-toggle setup
pending. No force-grant and no desktop gesture attempted.

Actual user activation subsequently verified: secure enabled service matches
DexAccessibilityService and dumpsys reports it bound, capabilities161. First
four-case runner `/tmp/t010-fold3-desktop.log` has one routing pass and THREE
assumption skips (despite final OK4tests). Category correctly yields Desktop
1920×1080 display2; service static instance was null in instrumentation. No
hardware acceptance from this run. Runner default UiAutomation suppression is
being fixed with FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES plus bounded service
readiness; a configured-but-unbound service will fail instead of falsely skip.
Primary source checked2026-10-03:
https://developer.android.com/reference/android/app/UiAutomation#FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES
This test setting preserves user-enabled services; it does not grant permission.

## Actual runner and overlay defect
Revised runner preserves UiAutomation services, but normal instrumentation kills
the target app. Android reports the service in Crashed services, with grant still
present; no AndroidRuntime exception was observed. AOSP Android15 confirms
`am instrument --no-restart` and matching-signed package replacement recovery:
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/cmds/am/src/com/android/commands/am/Am.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/services/core/java/com/android/server/am/ActivityManagerService.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/services/accessibility/java/com/android/server/accessibility/AccessibilityManagerService.java#6257
Same-signed app replacement PASS; grant retained, Crashed services empty, bound
service restored and PID14931 unchanged through no-restart runs. No extra owner
grant was required. Test-only APK now requests settled display-sized fixture
bounds and verifies actual visible target coordinates BEFORE any gesture.
Latest guarded test SHA25614be0eb1ef45445fef240f6b79476ec0159a5350e07ce2d8eab35905e75f4046.

Actual guarded suite `/tmp/t010-fold3-guarded-desktop.log`:1routing pass,
3FAILURES (zero desktop skips), each SCREEN_OVERLAY_FAILED. System WindowManager
reports unknown token null when adding Accessibility overlay. No gesture was
issued. Android service-specific window-context/token repair in progress.
Latest full source/test build prior to token repair PASS153tasks/16s,
49unit tests, lint/app/test APKs and both diff checks.

Direct service createWindowContext repair built PASS153tasks/38s,49unit tests,
lint/app/test APKs. Matching-signed app SHA256
69c5357707979d0df067e4fe52f9a9e86a1cc23c4f533bd09fd6703219e4efcb
installed PASS; user grant/bound service retained, crash list empty. Actual rerun
`/tmp/t010-fold3-token-fixed-desktop.log`:1routing pass+3overlay failures,5.425s.
WindowManager still reports unknown token null. No gesture dispatched. Direct
window-context change alone does not resolve Samsung token injection; further
Android diagnosis is active.

Next repair retains the WindowManager returned by service.createDisplayContext
on all APIs, preserving explicit NOT_TOUCHABLE/NOT_FOCUSABLE flags. Public
API34 surface-host alternative was investigated but SDK35 exposes only
setView(width,height), not LayoutParams, so noninterception could not be proven;
that experiment was removed before installation. Production/test public
serviceInfo guards detect framework disconnection; sanitized DexScreen logs
contain only context/manager classes, display/API, exception class and connection
boolean. Full build PASS153tasks/33s,49unit tests zero failures/errors/skips;
log /tmp/t010-display-token-gradle.log. Matching-sign app/test installation and
actual guarded rerun underway.

Display-context app SHA256 bce278c9db0ed50f5b2109c5d483f0d894e385e3bc7a7c195b652d9bb8fb1308;
test SHA256 de98492826e72b876edc7de245a6ff5cb42e4f0aac8b2378318655c57ad03a7b.
Both matching-signed upgrades PASS. Actual /tmp/t010-fold3-display-context.log
1routing pass+3overlayfailures,5.346s. DexScreen context=AccessibilityContext,
manager=WindowManagerImpl, display2/API35; BadTokenException connected=true.
Public serviceInfo guard passes: framework connection exists, while both service
display/window contexts receive null token on this Samsung firmware. No gesture.
Investigate public surface-host input-region API before adopting that alternative.

API34+ repair uses public SurfaceControl.Builder + Surface(SurfaceControl) +
lockCanvas/unlockCanvasAndPost and attachAccessibilityOverlayToDisplay. It
creates no window/ViewRoot/input channel/focus target. API30-33 retains flagged
Accessibility WindowManager. Raw Surface/SurfaceControl cleanup detaches/releases
on redraw, clear and pre-image gesture. Primary SDK35 public methods verified:
https://developer.android.com/reference/android/view/Surface#Surface(android.view.SurfaceControl)
https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#attachAccessibilityOverlayToDisplay(int,android.view.SurfaceControl)
Full checks PASS153tasks/32s /tmp/t010-raw-surface-gradle.log. Matching-sign app
SHA256051d17fb1c052bc8a18431b5d730372fb01b52e1c95cddf195baa1c58964089c
install PASS. Strengthened test-only physical visibility/focus/passthrough checks
are being built before actual acceptance rerun. No image export/output permitted.

Strengthened raw test APK SHA2565e24f183e776b7dbda7f05d50287db75a700e0a15c643c2919f1c4a0c12d6483
matching-sign install PASS; build /tmp/t010-raw-surface-visible-gradle.log
PASS153tasks/10s,49unit/lint/app/test. Actual physical DeX run
/tmp/t010-fold3-raw-visible.log:3PASS+1FAIL,zero skips,6.105s. Grid case proves
RAM-only screenshot badge visibility, fixture retained focus, explicit desktop
gesture goes through LIVE number overlay to guarded fixture cell11, and normal
numbered click dispatch consumes session/releases surface. Split3/reject4/cancel
cleanup and numeric local-routing pass. Canvas no-node X fails detector candidate
assertion BEFORE any image-target tap. This is actual secondary-display gesture
evidence on Fold3, not yet spoken wake/STT or image-click acceptance.
Owner asked to exercise HeyDeX→grid→split→cancel while image issue is repaired.

Owner observation2026-10-03: in installed raw-surface build, requested real
HeyDeX→“hiện lưới”→“chia ô mười một”→“hủy” and replied
“Hiện số và nghe tiếp đúng”. This verifies real spoken wake, number display and
continued listening after subdivision by owner observation. It does not measure
latency/resources or verify a spoken target tap/YouTube close.

Test-only X diagnostic upgrade installed against unchanged raw production app.
Two targeted runs found a retained fixture image candidate but fresh-click
revalidation returned SCREEN_CHANGED, no gesture: /tmp/t010-fold3-x-diagnostic.log
2.078s and /tmp/t010-fold3-x-diagnostic-repeat.log1.968s. This distinguishes
intermittent candidate/phase issue from a subsequent rejection; precise screenshot
error/fingerprint/snapshot rejection cause is being diagnosed without removing
the pre-click guard. No image-target click has yet been accepted.

First patch diagnostic run missed candidate: scaled640x360, fixture region
(605,5)-(635,35),bright211,ink30x30,detector158ms,candidates1,fixtureCandidates0.
Thus this failure is not the1500ms analysis timeout; /tmp/t010-fold3-patch-diagnostic.log2.127s.
Repeated patch diagnostic PASS1test3.344s: it reflectively removed number overlay,
waited400ms, captured RAM patch (patchSame/snapshotSame/epochSame all true),
then paced1100ms and invoked normal image execute; DISPATCHED and canvasClosed
verified. /tmp/t010-fold3-patch-diagnostic-repeat.log. This proves actual image
target geometry/tap and stable patch once overlay removal settles, but is not
yet proof that unmodified immediate-revalidation production path works.
Candidate-phase regression and production overlay-removal completion timing
remain under repair; avoid attributing screenshot-rate cause without errorcode.

Phase-aware X detector now scans every center pixel and keeps axial quiet checks
outside thick strokes, with realistic3px glyph/nine-phase regression. New
production revalidation waits API35 public transactionCompleted (presented);
API34 committed then next Choreographer frame. Existing1500ms deadline and epoch/
snapshot guards cover wait; no gesture retries. Ordinary X test no longer
reflectively removes overlay and retains explicit1100ms screenshot/STT-like pacing.
Final JDK17 build /tmp/t010-presented-final-gradle.log PASS153tasks/14s,
45app+5DPC=50unit tests; lint/app/test assemblies and Androiddiffcheck PASS.
Superseded helper compile failures were corrected before this successful run.
Matching-sign app/test upgrade and ordinary physical rerun in progress.
Primary timing APIs:
https://developer.android.com/reference/android/view/SurfaceControl.Transaction#addTransactionCompletedListener(java.util.concurrent.Executor,java.util.function.Consumer)
https://developer.android.com/reference/android/view/SurfaceControl.Transaction#addTransactionCommittedListener(java.util.concurrent.Executor,android.view.SurfaceControl.TransactionCommittedListener)

Presented candidate app SHA256e0f20fe7138e764818ad9380d5c0161005087a6131216c97197ffa2039d5436f;
test SHA256c3db92531e8785194a1624ff3afa24b2ec814ad3f07c519968363b5ce860e13f.
Both matching-sign installs PASS. Normal fullsuite /tmp/t010-fold3-presented-normal.log
3PASS+1missingX8.709s; targeted /tmp/t010-fold3-presented-x-normal.log also
missingX2.857s. Detector actual486-488ms (not1500ms timeout). Diagnostic
currently captured with numberoverlay present, expectedfixture near top-right
(605,5)-(635,35) afterscale,bright201,ink28x30. Test-only clean-overlay/fixture
window-offset diagnosis next; do not loosen detector based on contaminated
frame or claim production presented image-click acceptance yet.

Clean-overlay geometry test /tmp/t010-fold3-clean-geometry.log2.834s found:
own decor/window/root bounds0..1920x1080; expectedcanvas region gives0candidates,
regions shifted+24/+32physical pixels give1candidate,bright184,ink26x25;
detector492ms,total2candidates. This supports a fixture geometry/inset offset
rather than analysis timeout or absent screenshot glyph. Current fixture caches
getLocationOnScreen during onDraw before window stabilization; Android agent
will recompute known canvas bounds after layout settlement and rerun ordinary
production path. Test APK matching-sign install PASS; no gesture in this run.

Fresh query-time fixture geometry build PASS153tasks/29s,50unit/lint/APKs;
/tmp/t010-fresh-fixture-geometry.log. Matching-sign app eacc0214d862de40f60bee5c4fb56468440e6e17a00c4c4fd329fb2ada6354b0
and test7b46c21dd2e5ed47e99573d23de6d8054069b824a4be5a1d1b7e81298a349b2f
upgrades PASS. /tmp/t010-fold3-fresh-geometry-normal.log8.644s:2PASS+2FAIL.
X bounds now match clean image candidate at scaled(605,15)-(635,45), but prepare
retained no X candidate; screenshot/fallback instrumentation next. Grid live
passthrough dispatch completed but fixture lastTap null, so latest run does NOT
verify grid delivery. Stronger pre-gesture fixture focus/window-occlusion guard
required before more taps. Owner asked to leave DeX idle during fixture testing.
Earlier successful grid/diagnostic X evidence remains historical, not a reason
to ignore these current failures.

Foreground-guard build PASS50unit/lint/APKs153tasks/26s. Matching-sign
app14e120bdce787a11f4ccf3fcf1efe9b4fc7c793c3be59c734683b7fde49442f2
and testeebf5b765cd64c8152790706819eeabd5d7e2a5f1fffaba6b05712430e2b5969
installed PASS. Owner explicitly left DeX idle. Actual
/tmp/t010-fold3-foreground-diag.log3PASS+1Xmissing8.582s; grid focused/unoccluded
passthrough and coordinates pass. Prepare analyzes2candidates in471-484ms,
no capture/deadline error. Test-only merge diag matching-sign upgrade PASS:
/tmp/t010-fold3-merge-diagnostic.log reports knownImageHashes0/retainedKnown0,
so no merge overlap evidence. Diagnostic comparison itself requires entire
expanded detector rectangle inside exact hitbox; integer downsample mapping
can extend2pixels beyond exacttop47 (floor15scaled maps45). Next: inspect
candidate centers inside known canvas hitbox, retaining exact physical hitbox
and foreground guards; never expand the allowed gesture area.

Ordinary guarded FULLSUITE PASS: /tmp/t010-fold3-center-selector-normal.log
OK4tests8.817s,zero failures/skips. Current production app14e120bdce787a11f4ccf3fcf1efe9b4fc7c793c3be59c734683b7fde49442f2;
updated test2f6608089d1144cc9a0a050873098ba5b0aab7ebdb59473513c532ba5e5e4d80
matching-sign install PASS. Known canvas selection correctly uses exact dispatch
center inside fresh hitbox plus >=50% candidate overlap, accommodating padded
bitmap rectangle integer rounding without expanding tap area. Production merge
was not changed: prior “missing X” also reflected overly strict test selection.
PASS covers visible/focus-preserving no-input number overlay, gesture through
LIVE overlay into focused/unoccluded known fixture, cell11 correct physical
display/coordinates and session consumption, split3/reject4/cancel cleanup,
no-node bitmap X normal live-overlay revalidation→DISPATCHED→canvasClosed,
and malformed/numeric local routing with backend disconnected. No diagnostic
pre-removal in accepted X path. Owner released from idle hold after this run.
Next: bounded fresh node identity checks against same-bounds replacement,
window/content invalidation,35s expiry cleanup and opt-in100prepare/clear metric
smoke. These do not replace required100spoken wake/STT latency/resource sessions
or real popup/YouTube/resize/disconnect/permission acceptance.

Hardening source/test candidate: refreshed bounded node signatures (identity,
window/path/resourceId/class/actions, hashed labels/description inRAM only),
same-main-turn changed/replaced button rejection, real window-change/35s expiry
and opt-in100prepare/clear metric smoke. Full50unit/lint/APKs PASS153tasks/29s
/tmp/t010-identity-lifecycle-gradle.log; test corrections/lint PASS108tasks/10s
/tmp/t010-lifecycle-metric-test.log. Actual candidate installation deferred
while owner attempts spoken popup/YouTube. No new physical pass inferred.

Owner now reports no response to HeyDeX, including “hey decks” near Fold3.
Read-only current voice_health:LISTENING,ListeningforHeyDeXoffline; voice FGS
active. AudioRecording app uid10325 session441 active,silenced=false,16kHz mono.
AudioPolicy input routes this VOICE_RECOGNITION capture to USB_DEVICE
(card1/device0), not builtin phone mic. Owner asked whether attached USB has a
real microphone. This is a concrete routing observation, not yet proof of
absent audio/acoustic model failure. Do not lower KWS thresholds without data.
Per-recorder setPreferredDevice can route KWS; system SpeechRecognizer owns
its own input. API33 EXTRA_AUDIO_SOURCE PCM pipe needs provider support/lifecycle
validation before coordinating both. No global setting force-change or input
architecture edit performed. Earlier owner gridflow confirmation is historical;
current wake reliability remains unresolved.

Owner correction: DeX/Live Transcribe receives microphone speech normally;
explicitly requests “Hey Hey” instead of HeyDeX. D-019 supersedes phrase and
defers D-018 route architecture. No production mic preference/PCM/provider
change performed; unused test-only provider probe removed without running.
Built APK keyword EXACT ▁HE Y ▁HE Y @HEY_HEY, oldkeyword absent.
JDK17 full50unit/lint/APKs PASS153tasks/32s /tmp/t010-hey-hey-gradle.log.
Owner explicitly requests commit/pushsource to GitHub; coherent source/docs
commits are being prepared, tasks remain IN_PROGRESS. Newphrase physical
verification and hardened lifecycle/100prepare smoke next.


## Stable content events and final expiry verification (2026-10-03)
Owner reported numbered labels/grid missing on a later HeyHey while YouTube open.
Cause: Accessibility service treated every content/scroll callback from the
current DeX window as session invalidation, even when target/window snapshot did
not change. It now compares fresh bounded node/window snapshot to frozen session
for content/scroll/topology notifications. Window state transition, target identity/
bounds, topology, display identity/geometry changes still invalidate. Screenshot
fingerprint and current-target checks remain before each gesture.

Final JDK17 unit/lint/app+androidTest build:57unit tests, zero failures/errors/
skips;153tasks28s (`/tmp/t010-stable-content-gradle.log`). Final signed Fold3 app
SHA256 `5219bb8586dbf81fc89d71329cf3e06e7fef47140a9f21f91264a02bbf025646`, test
SHA256 `c472f0cfc2533b9e5c2958b61b92fd95c468e70d9f93580483b373380d05435c`.
Integrated physical suite PASS7tests/66.241s zero failures/skips
(`/tmp/t010-stable-content-fold3-final.log`): six numbered/safety/lifecycle cases
including ten stable content notifications, 34s session-live and real35.5s expiry,
plus real Google STT provider/media-ready cue dispatch/finish with no focus takeover.
Cue routed to HDMI (type9). Hardware log confirms dispatch and finish within610ms;
actual human audibility still awaits owner confirmation. Previous early expiry was
caused by content callback and is corrected; no invalidation safety guard weakened.
