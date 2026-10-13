> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-006 DeX STT subtitles — implementation and acceptance

Date: 2026-10-05. Canonical backend `/home/kunrg/DeX-Assistant`; Android
`/home/kunrg/Dex-Assistant-UI`. Scope is D-031 subtitle slice, coordinated with
T-010; broader T-006 voice reliability/resource/soak criteria remain open.
No backend/provider/protocol/microphone policy changes. No commit or push.

## Implementation

- `voice/VoiceSubtitles.kt`: volatile identity-guarded phase/transcript state,
  existing credential redaction, bounded latest tail and monotonic final deadline.
- `voice/VoiceAssistantService.kt`: command-only preparing/ready/partial/end/final
  events for wake/button and interaction listening; partial never dispatches;
  existing exactly-once final guard retained. Explicit cancellation and STT errors
  clear; ordinary TTS/standby and grid cleanup preserve final until expiry.
- `voice/VoiceSessionGuard.kt`: process-wide increasing identities prevent stale
  state from crossing service instance recreation.
- `screen/DexSubtitleRenderer.kt`: independent unique-Samsung-DeX renderer,
  API34+ composition surface with layer above numbers, API30–33 nonfocusable and
  nontouchable overlay. White 24sp transcript, two latest lines and compact status;
  density/insets-aware bottom layout. Reuses drawing resources.
- Both preparation and image revalidation screenshot paths suspend subtitles
  behind shared compositor detachment barrier, reference-count pending captures
  and restore latest valid RAM state without extending expiry. Teardown,
  interruption, missing/ambiguous display suppress old turns; a fresh turn is
  required. Overlay failure cannot prevent normal command routing.
- Telemetry contains phase, turn, hasText, display and timing metadata only;
  transcript/audio/screenshots are not exported by acceptance tests.

## Host acceptance

JDK17 `/home/kunrg/.local/opt/jdk-17.0.20.1+1`, SDK
`/home/kunrg/Android/Sdk`:

```bash
./gradlew test lint assembleDebug assembleDebugAndroidTest --max-workers=1
```

Final frozen build PASS 1m3s, 201 tasks. App tests: 124 debug + 124 release,
zero failures/errors/skips. Full aggregate lint and both application/test APK
assembly pass, including unchanged DPC. Log `/tmp/subtitle-build-frozen.log`.
Unit coverage includes phases/revised/empty/long Vietnamese/credential text,
final deadline, stale events/timers/new turns and service guard identity.
Existing final-consumption/session and voice tests remain passing.
Android `git diff --check` passes.

## Physical acceptance

Rediscovered wireless ADB `192.168.1.18:40725`: SM_F926B Fold3. Original
certificate APK install-r preserves app data/grants/signing identity; no reset,
permission provisioning or broad legacy owner-store-mutating suite authorized.
Application/test artifacts under `/tmp/dex-fold3-20261005/subtitle-*.apk`.

Initial focused `SubtitleDeviceTest`: 2/2 PASS 9.666s on original-cert installed
app SHA256 `1f90453d0145092d6a74c496b74d78e91247d1ce927507f0448261cc1782cfae`.
Test SHA256 `121641eb1d22322acdc7db514778184edd0ba5f0a1cd00f6aa26545c31f1d5ea`.
Log `/tmp/dex-fold3-20261005/subtitle-device.log`.

Initial natural trial **FAIL for repeat-turn acceptance**: owner saw subtitles
on first turn only and reported persistent numbers in grid-cell2 region.
Epoch1 partial callback at4923145ms, render4923178ms; final4927709ms,
render4927720ms: visible partial led rendered final by4542ms. Subsequent epochs
received nonempty partial callbacks with no subtitle presentation.
Read-only metadata proved `captures=0`, `detachInFlight=true`, `closed=false`:
no-overlay double-Choreographer-frame barrier stalled in background without
ViewRoot. Fix bypasses compositor waiting when no surface/window exists;
actual surface detach still uses completed/committed transaction barrier.
New focused regression tests this exact background/no-overlay condition.

SurfaceFlinger listing also showed one old numbered surface still parented
while many older surfaces were detached. Late asynchronous framework attach is
suspected for persistent numbers, not conclusively proven. All cleanup/detach
transactions now hide the surface before unparenting, preventing late attachment
from displaying stale content. Valid live grid subdivision retention is preserved.

Fixed final full build `./gradlew test lint assembleDebug assembleDebugAndroidTest
--max-workers=1` PASS56s (201 tasks), 124 tests/variant. Log
`/tmp/subtitle-hidden-detach-build.log`. Incremental test APK/app lint including
read-only inspector PASS13s (`/tmp/subtitle-test-extra-build.log`).
Original-cert install-r succeeded for both; current app SHA256
`9526b0781429bcc2c1f3601e4209755529edad58dfebf1e4b7e203ed8ff84c2d`;
test SHA256 `959a9c98df0fe55593436bdd779438571ef5bb4835c9cae44e65771a7bb737cf`.

Focused `--no-restart` run: **4 PASS / 1 SKIP**,16.780s; the compact grid test
was assumption-skipped because its explicit fixture opt-in was omitted. Log
`/tmp/dex-fold3-20261005/subtitle-fixed-device.log`. Four subtitle cases prove:
background/no-overlay barrier and next turn; production preparation/image-patch
revalidation/fixture-only tap with live subtitle; actual pixel exclusion/restoration
and grid subdivision/focus/cleanup; overlapping captures/new turn/interrupt/cancel/
final three-second deadline. No owner stores changed.

Exact compact grid rerun with `runVoiceGridFixture=true`: **1 PASS**,5.135s.
Log `/tmp/dex-fold3-20261005/subtitle-fixed-grid.log`. Valid ô2→childô8 target,
fresh preparation, repeated coordinator commands and fixture gesture preserved.
Combined coverage is four subtitle cases plus one separately verified grid case,
not a five-pass claim for the first run.

Owner fixed natural repeat-turn rerun **PASS for core UX** on app SHA9526b078…84c2d:
owner confirms all three requested turns show subtitles, final stays about three
seconds, and stale numbers in grid-cell2 region disappear. Metadata additionally
shows five turns with nonempty LISTENING presentation before FINAL: turns3/6/9/
12/13 lead16208/5864/9037/7372/9938ms respectively. Continuous grid12→13 and
explicit cancellation also appear. This measures callback/presentation metadata,
not acoustic STT endpoint latency, and no latency improvement is claimed.
Log `/tmp/dex-fold3-20261005/subtitle-fixed-natural-metadata.log`.
The original repeat-turn failure remains recorded above.

A final RAM-only correction clears the renderer drawing closure on disposal,
releasing sanitized text references at expiry/cancel. Final APK and focused rerun
are tracked separately below; the owner's natural evidence belongs to SHA9526,
not a claim of testing a byte-identical final artifact.

## Limits and next acceptance

API30–33 renderer has build/static coverage; Fold3 API35 runtime does not prove
older renderer behavior. Natural partial cadence depends on selected existing
STT provider. Synthetic callbacks prove lifecycle/rendering, not speech streaming.
Resize/disconnect, natural interaction reply, wake accuracy, microphone routes,
100-command latency/resource acceptance and eight-hour soak remain separate gates.

## Final installed artifact and state

Final RAM-closure cleanup full Gradle test/lint/APKs **PASS57s**,124 tests per
variant (`/tmp/subtitle-ram-cleanup-build.log`). Final test-only build/lint PASS11s.
Original-cert application/test install-r succeeded. Final application SHA256
`29ae7463c67dcde4ce2a4c51d4be9718d9c11954d81f0680c3eb57980dcb7997`;
test SHA256 `b4826e464ea650580a75b0e016038519f98ef2d91bb8819982a23d9a720d07ea`.
Final exact four subtitle device cases **PASS4/4 16.688s** including assertions
that canceled/expired renderer no longer retains its drawing transcript closure.
Log `/tmp/dex-fold3-20261005/subtitle-final-device.log`. The production source
delta from owner-tested SHA9526 is solely `drawing = null` in renderer disposal;
final APK natural trial was not repeated. Core UX was owner-accepted on the
preceding artifact; final artifact has full host and focused physical coverage.

Canonical task/plan/STATUS, SRS and Android voice guide/status updated. Local
links and CRLF-aware diff checks PASS in canonical repo; Android diff check PASS.
T-006 remains IN_PROGRESS with core subtitle implementation/acceptance checked,
physical resize/disconnect/selection-confirmation and broader reliability/resource/
soak gates explicitly open. No commit/push. No provider/latency reduction claim.

Preparation's pre-existing numbered-overlay removal remains a queued compositor
transaction before capture; unlike image revalidation it does not establish a
separate completed numbered-layer barrier. Production image fixture passes, but
exhaustive numbered-layer preparation timing is not claimed. Subtitle-layer
suspension itself uses the verified shared barrier.
