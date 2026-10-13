> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

> Scheduling override 2026-10-13: owner requests Windows simulator tests through WSL during continuation. All remaining physical Z Fold3 execution is consolidated in [T-020](T-020-consolidated-fold3-acceptance.md), one prepared campaign. This overrides older immediate/separate device-run instructions, preserves acceptance gates and does not imply PASS.

# T-010 — Numbered DeX screen control

## Outcome and boundaries
Owner2026-10-03 explicitly accepts D-023: grid is a coordinate touch independent
of content/window changes; target-number clicks keep strict identity checks.
Verify same unique DeX/display dimensions, grant, current unexpired epoch and
exactly-once dispatch for grid. Earlier window-invalidation descriptions below
apply to numbered targets and historical grid behavior, superseded by D-023.
Implement the approved Vietnamese numbered desktop-control plan. Local screen
reading, bitmap analysis and gestures; no OCR/model/cloud image processing,
no new V1 tools, no automatic replay, no phone-display fallback. T-006/T-009
remain open independently. In the current workspace, Android checkout is
`/mnt/d/dex-assistant-ui`; canonical memory is `/mnt/d/DeX-Assistant`.

## Inputs and constraints
Task T-010 acceptance in TASKS.md; D-016 hybrid speech, SRS, PROTOCOL.md and
LOCAL_CAPABILITIES_V1.md. Fold3/API35 external Samsung DeX is the target.
User explicitly authorized implementation of this plan. Physical display access
and positive spoken wake are measurements, not assumptions.

## Decision questions and evidence
Primary sources checked 2026-10-02:
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- https://developer.android.com/reference/android/accessibilityservice/GestureDescription.Builder#setDisplayId(int)
- https://developer.samsung.com/samsung-dex/modify-optimizing.html
Android APIs support per-display windows/screenshot/gesture; Samsung desktop
category must be resolved dynamically. Device evidence must prove actual DeX
routing. Screenshot failure alone must preserve node/grid capability.

## Milestones
1. [x] Create task/plan, accepted local capability/requirements/decision records.
2. [x] Source: Accessibility/display selection, overlay, grid and secondary-display fixture.
3. [x] Source/unit: Node enumeration, bounded bitmap candidates and fixture tests.
4. [x] Source: Local Vietnamese parser, wake preparation and retained split turns;
   session invalidation and cleanup on every terminal path.
5. [x] Unit/lint/APKs and ordinary guarded Fold3 four-case fixture suite PASS.
6. [ ] Real Fold3 DeX flow and 100-session resource/latency measurement.
7. [ ] Owner correction: live YouTube/other-app grid persistence and coordinate
   dispatch despite content/window changes; display resize/disconnect still reject.
8. [x] Recognize “bấm ô số N” and “bấm vào ô số N” as coordinate-cell taps;
   malformed/compound requests still execute no gesture.

## Validation
Android JDK17: ./gradlew testDebugUnitTest lint assembleDebug
assembleDebugAndroidTest; git diff --check in both repositories. Run focused
instrumentation if an authorized emulator/device is available. Test grammar,
24-cell bounds, depth/coordinates, dedup/occlusion, stale/duplicate callbacks,
light/dark multiscale X and confusing texture. Instrumented fixture must prove
secondary-display tap and no command.request. Record exact limitations.

## Risks and escalation
Samsung firmware may hide windows/deny screenshots/gestures; fail locally.
False visual targets require conservative rejection and grid. Overlay never
captures focus/input. Window/display change invalidates, never renumbers and
executes old text. Physical installs must use the existing verified signing identity and preserve
DPC/data; no reset or shell force-grant. No owner approval needed for authorized
implementation/build/instrumentation; enabling Accessibility is an Android
user-mediated setup action. Connected Fold3/DeX was discovered read-only; prepare
reviewed matching-signed artifacts before requesting that setup action.

## Progress and findings
Android implementation delegated under main-agent role with exclusive Android
file ownership. Canonical docs owned by main agent. Final JDK17/API35 `testDebugUnitTest lint assembleDebug assembleDebugAndroidTest`
PASS,153 tasks/1m4s,44 app+5DPC unit tests (zero failures/errors/skips).
Log: /tmp/t010-final-validated.log; corrected first full run also PASS/2m33s.
Both repo diff checks pass. Signed reviewed app/test artifacts prepared in
/tmp/t010-device-ready with original verified device-test certificate; main-app
and test matching-signed installs PASS. Focused Fold3 numeric/local routing case
PASS1test/0.494s; Accessibility activation requested. Four runnable device cases
are prepared; physical voice,
window/permission/display-change acceptance and 100-session measurements pending.
Wireless ADB restored; owner activation confirmed. Same-signed replacement
recovers crashed service marker; --no-restart avoids instrumentation force-stop.
Actual guarded fixture suite:1routing pass+3SCREEN_OVERLAY_FAILED (null token).
No gesture attempted in those failed runs. Both service window/display-context
repairs still fail on Samsung with connected=true. API34+ public raw composition
Surface/SurfaceControl overlay now builds PASS, preserving no-input semantics;
API30-33 uses flagged Accessibility WindowManager. Matching-sign install and
actual fixture confirms visibility/focus/live passthrough and correct cell11
physical gesture; split/cancel/local routing pass. Canvas X candidate is phase
sensitive; image click passed only diagnostic pre-removal pacing so production
revalidation timing is being repaired. Owner confirms real spoken HeyDeX/grid/
split and continued listening. Popup/YouTube/100-session acceptance remains open.
See .agent/evidence/T-010-numbered-dex-2026-10-02.md.

## Decision and requirement updates
D-017, SRS numbered-screen section and LOCAL_CAPABILITIES_V1 numbered extension.
Wire V1 and general CLICK_UI/INPUT_TEXT remain unchanged/unsupported remotely.

## Verified hardware milestone and remaining hardening
Ordinary full suite /tmp/t010-fold3-center-selector-normal.log PASS4tests8.817s
zero failures/skips, including actual visible/live passthrough overlay, cell11
correct display/coordinates, split/cancel and image-only X verified closed.
Owner confirms real spoken wake/grid/split continued listening.50unit/lint/APKs
PASS. Continue bounded fresh node identity/same-bounds invalidation, window/expiry
checks and opt-in100prepare metric smoke; these do not replace100spoken sessions
or remaining real popup/YouTube/display/permission acceptance.

## Latest owner steering — HeyHey and GitHub source
D-019 explicitly replaces HeyDeX with HeyHey for current wake. Retain original
model/threshold/system mic/online STT; D-018 mic architecture prototype deferred.
New keyword packaging and50unit/lint/APKs PASS32s. Owner explicitly requests
commit/push both repositories. Do not mark DONE: newphrase, hardened device
cases, real popup/YouTube and100spoken/resource gates remain open.


### Coordinated five-part correction (2026-10-03)
Owner approved fixing wake/STT audio focus, proper pause/resume, transient
recognition text/non-executing diagnostic, adaptive no-scroll one-page UI,
and tests/commit/push. Implementation/validation details live in T-006 ExecPlan.
T-010 screen semantics and remaining physical acceptance are unchanged.
HeyHey signed app/test install succeeded before this correction; actual provider
handoff pauses due to competing audio focus, not absent KWS detection.

### Current display-event correction
Integrated six-case follow-up after refresh-failure exclusion reads targets again:
4PASS and2guarded focus failures, owner confirms covering Settings/dialog. Closing
cover makes grid rerunPASS; canvas preparation expires during epoch change.
Display callbacks were unconditionally clearing on unrelated phone metadata.
Scoped correction validates unique valid Samsung desktop ID/geometry against
session/preparing snapshot; window/content invalidation and stale gesture guards
remain intact. Fixture additionally waits bounded Accessibility snapshot stability
before first prepare, without retries. Unit tests cover unrelated metadata, resize,
replacement, duplicate desktop and removal. Full unit/lint/APKs PASS28s153tasks
(`/tmp/t010-display-identity-final.log`); physical final rerun7cases6PASS/1FAIL63.017s zero skips. No-nodeX/grid/fresh-node
and automatic listening pass; content event2048 legitimately invalidates session
before34s still-live assertion, so uninterrupted final35s expiry remains open.
Preserve cleanup and content guards; do not force a stale tap or infer PASS.

### Post-YouTube overlay repro (2026-10-03)
Owner previously reports HeyHey after YouTube shows number labels, but latest run
says a subsequent HeyHey shows neither labels nor grid. Treat as inconsistent
physical reproduction, not yet diagnosed. Correlate provider readiness, transcript,
sanitized prepare result/session epoch and actual overlay before tap. “Mở YouTube”
is supported; direct “tắt YouTube” is not. After HeyHey, say “bấm số N” for
numbered targets; say “hiện lưới” for full grid; no automatic subdivision occurs
without “chia ô N”. Preserve existing window/content invalidation and stale-tap
safety while collecting a repeat.


### Stable-content invalidation fix (2026-10-03)
Owner says overlay absent on HeyHey after YouTube. Root cause in event handler: all
content/scroll notifications from existing window directly cleared prepare/session,
even when target/window snapshot was unchanged. Changed to bounded snapshot compare
for content/scroll/topology events; window-state change remains unconditional.
Display uniqueness/geometry checks and fresh screenshot/target patch before tap
remain. Regression proves100identical snapshots preserve, changed bounds/node/window/
missing snapshots invalidate. Physical Fold3 suite PASS7/66.241s including six screen
cases, ten injected stable content notifications and real35.5s expiry; 57unit tests,
lint/APKs PASS. Owner must naturally confirm after-YouTube overlays on final build.

### Latest owner GRID false-change report — OPEN (2026-10-03)
Owner repeatedly hears “Màn hình đã thay đổi” for “hiện lưới” while YouTube looks
stationary. The previous stable-content event fix and 7-case fixture suite pass,
but do not prove this natural YouTube path. `prepare()` and `execute()` still
compare complete snapshots; signatures include clickable node hashes/text/desc
and dynamic YouTube accessibility content can differ with unchanged geometry.
Historical next step at the 2026-10-03 report: add sanitized mismatch
categories/counts (no text/screenshot), prove exact rejection stage, and let a
fresh explicit GRID rebase only on a valid unique DeX
display/geometry/window safety check; GRID draws an overlay but never taps. Keep
numbered target identity, foreground/occlusion and screenshot patch checks strict.
Unit + Fold3 screen cases, then owner natural YouTube confirmation. See
`.agent/SESSION_HANDOFF.md` for all open tasks and measured baseline.

### Fresh GRID correction continuation
Structural window/display snapshots are separated from node identity. Explicit
GRID creates a new session even after old target mapping is invalidated, while
grid taps retain window ID/layer/type/bounds/focus/active checks. Numbered target
identity and image-patch checks remain strict. Host build PASS201tasks; Fold3
11-case screen/voice/recovery suite PASS101.777s. Owner additionally requests
live YouTube/another installed app grid tests; these are in progress and do not
replace spoken/100-session acceptance. See
`../../evidence/T-006-T-010-handoff-continuation-2026-10-03.md`.

### 2026-10-04 resumable checkpoint (supersedes older next actions)
D-023/D-024 implemented and installed:59units/build/lint;9screen and corrected
5voice/recovery hardware passes; live YouTube coordinate dispatch passes. Settings
result unknown. Warm feedback-terminal→mic150–175ms,4s watchdog verified. Remaining
controlled command-terminal delay1.5–1.6s: measure synchronous encrypted-history
record/read before proposing FIFO background work; no history change accepted yet.
Task remains IN_PROGRESS. See ../../SESSION_HANDOFF.md and continuation evidence
for exact resume steps, artifacts, dirty trees and unverified acceptance.

### Resumed Settings validation — 2026-10-04

Existing live Settings first-badge assertion initially fails0.740s in pre-existing
WiFi-debug pane, before tap. Test-only RAM crop counts for24badges added; original
assertion unchanged. Explicitly opened Settings on selected DeX and rerun passes
1test/6.497s: visible grid at both captures,5s persistence, subdivision, one
coordinate dispatch, release and duplicate rejection. Immediate repeat without
reopening fails foreground prerequisite0.060s after intentional tap changes focus.
First failure cause remains unknown; no renderer change or stability/speech claim.
D-025 history latency implementation is coordinated by T-006/T-009.

### Final measured history slice — 2026-10-04
D-025 implemented and measured:full64debug+64release units/lint/app/test PASS;
controlled final→terminal1852/1464/1639ms→40/41/38ms. Five isolated history,
four voice andautomatic cases pass. Combined19case18PASS/1late-window canvas
expiry;unchanged focused canvas +9screen rerun pass. Final live YouTube6.460s/
Settings6.480s pass. Preserve failure evidence;no19/19 or natural speech inference.
Matching-original installs preserve DPC/data/Accessibility;finalLISTENING.
See ../../evidence/T-006-history-latency-2026-10-04.md for exact artifact hashes,
commands and remaining acceptance. Task remains IN_PROGRESS.

### 2026-10-05 owner subdivision regression (in progress)

Owner reports grid -> split cell2 -> tap cell8 targets old full-grid cell8.
Investigate compact STT number grammar and actual wake/preparation/coordinator
continuation. Acceptance: descendant cell8 inside parent cell2 dispatches once
through real coordinator with local-only routing; preserve subdivision across
voice preparation but reject expired/display-changed sessions. Explicit GRID
resets; cancel/pause/stop and stale semantic target guards stay intact.
Natural voice demo required after host and physical fixture checks.

Correction checkpoint:120unit tests/variant + app lint/APKs PASS. Current
original-cert Fold3 app installed; coordinator compact/spaced/fresh-prepare/
continuous grid2->child8 PASS5.170s with actual touch coordinates. Explicit
GRID reset and consume-once validated. Guard regression4PASS54.102s includes
actual35s expiry, stale target, cancel/preparation and depth limit. Owner
confirms subdivided tap corrected; original continuous wrong-cell cause unproven.
Evidence: .agent/evidence/T-011-fold3-dex-2026-10-05.md.

### Resumed 2026-10-10 — current scope

Owner cancels T-008 and prioritizes T-010. Canonical workspace is now
`/home/kunrg/DeX-Assistant`; Android is `/home/kunrg/Dex-Assistant-UI`.
Retain previous automatic-only preference. Connected Fold3 is rediscovered,
not assumed from historical ADB endpoints. Android agent owns device/source
validation; main agent owns canonical memory. Reconcile remaining display
resize/disconnect guards and lifecycle coverage, then focused physical tests.
Do not disconnect sole wireless ADB or infer natural100-session acceptance
from synthetic cycles. Preserve original signing, owner data, roles and grants.
Milestone8 is already covered by implemented grammar/owner-confirmed correction
and prior tests; milestone6 remains open. Milestone7 has earlier live app evidence
but actual resize/disconnect acceptance must be distinguished from injected tests.

Validation checkpoint: JDK17 full test/lint/APKs PASS201tasks21s; current
Fold3 selected4/4 PASS54.089s, including added simulated stale snapshot
display-callback guard, actual fixture coordinate/consume-once, subdivision
coordinator continuity and real35.5s expiry. Only matching-original-signature
testAPK installed. No production edit or real display configuration change.
Milestones6/7 remain open for natural population and actual resize/disconnect.
See ../../evidence/T-010-fold3-automatic-2026-10-10.md for preserved state,
initial old-APK invocation and exact limitations.

### Owner-assisted final acceptance resumed2026-10-10

Owner explicitly agrees100 natural HeyHey/number/grid/tap sessions and actual
DeX resize/disconnect. This supersedes previous automatic-only preference for
T-010. Prepare privacy-safe measured batches with real microphone input,
wake-to-ready and command-end-to-dispatch latency, CPU/PSS,battery/thermal
and overlay/bitmap cleanup versus idle. Label recognizer-final timing separately
from acoustic end. SRS2.5 requires100 sessions; 8hsoak is a separateT-006
gate and is not newly imposed onT-010. Keep actual display disconnect separate
from Wi-Fi disconnect so wirelessADB can remain available. Do not markDONE
without captured measurements and physical safety acceptance.


### Measurement preparation completed2026-10-10

Latest owner direction is prepare only. Android collector allowlists metadata
without transcripts/audio/screenshots; separate run directories prevent epoch
collision, parent summaries retain per-run results. Reports wake→ready, recognizer
speech_end→final/dispatch and final→dispatch/terminal where present; missing
samples remain missing, acoustic endpoint is not measured. PSS/battery/thermal
and rolling CPU diagnostics do not prove bitmap/overlay cleanup alone.
Harmless desktop fixture accepts configurable1..100 touch observations (default10)
and60..3600s timeout (default900), injects no speech/gesture. Owner observation
and correlated metadata must establish actual natural trials; tap counts alone
never establish100sessions. Normal production voice history policy applies.
Python6PASS; scoped JDK17 lint/testAPK PASS99tasks1m58s; main repeat PASS99tasks39s.
No install, mic, actual gestures or display changes this continuation.
Runbook: /home/kunrg/Dex-Assistant-UI/scripts/android/T010-NATURAL-ACCEPTANCE.md.
Milestones6/7 remain open; await owner readiness for measured batches and actual
display change while retaining wirelessADB. See preparation evidence.
