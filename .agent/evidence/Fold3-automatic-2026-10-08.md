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

Artifact SHA-256:
- app:866bfa94b642e92110ab815a8aefbfab0fc4164b766be0b5ca9fd307895c8aab
- test:eb7a5b4df02ea9177dd25bcf98e063498cd6e56ab3fdf1eff8d537be07d5dee8

Logs: `/tmp/Fold3-20261008-android-build.log`, selected suites config/grid/
subtitle/dashboard/voice/youtube-preview/youtube-observer/backend/hardware;
focused grid-resource-rerun/youtube-preview-rerun/youtube-observer-rerun/
backend-rerun; resource-metrics; production dashboard activity/window readback.
Initial results and failed reruns retained; no broad legacy suites executed.
