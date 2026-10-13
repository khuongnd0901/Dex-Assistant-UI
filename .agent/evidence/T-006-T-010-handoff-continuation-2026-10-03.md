> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-006/T-010 handoff continuation — 2026-10-03

## Scope
Latest owner report: GRID appears, but spoken cell selection rejects with
screen-changed feedback. Owner explicitly requests coordinate touch independent
of underlying app/window changes (D-023), supports autonomous changes/testing
while away, and reports long command execution and5–10s wake wait after errors.
Previous grid-window rejection fixture is historical and superseded for grid.
New coverage must prove coordinate delivery while content/window changes;
target-number identity checks remain strict.

Continue the existing GRID false-change correction and measure wake re-arm after
failure feedback. Android implementation is delegated to the Android owner;
canonical evidence and physical integration remain with the main agent.
No backend/protocol change or new task is needed for these corrections.

Canonical fetch succeeded; starting HEAD and origin/main both resolve to
`16960603d8483c97d7fc293dbfa1578d85adb17f`, with a clean worktree.

## Wireless ADB trace

Owner reports Fold3/DeX and host are on Wi-Fi but ADB cannot connect, and asks
for a network trace. Read-only host checks established:

- Host `wlo1` is UP at `192.168.1.19/24`; route to last-known phone IP
  `192.168.1.18` uses that interface directly. Default gateway is `192.168.1.1`.
- Gateway ping passed 2/2, approximately 0.7 ms. Phone ping failed 3/3 with
  locally generated Destination Host Unreachable.
- Neighbor discovery for `.18` is `FAILED` after six probes: failure precedes
  TCP/ADB authentication. `adb connect 192.168.1.18:42331` reports No route to host.
- Both system ADB and SDK ADB mDNS discovery returned no services; device list
  is empty. No daemon reset, network reconfiguration or pairing reset performed.
- Wi-Fi scan shows two BSSIDs sharing the connected SSID. This alone does not
  prove isolation. Changed phone IP, unavailable phone or AP isolation remain
  hypotheses; requested current Wireless debugging IP:port from the owner.

Physical installation, wake-stage measurements and new device tests have not run.
Prior signed-device fixture passes remain historical evidence only.

Owner confirmed current endpoint `192.168.1.18:42621`. Connection to that endpoint
also returns No route to host, with ARP INCOMPLETE and no ping replies. The phone
IP is therefore confirmed; a stale port alone does not explain this failure.
Packet capture could not run because `sudo -n tcpdump` requires a password.
Asked owner to reconnect phone Wi-Fi and report its resulting endpoint; no host
Wi-Fi/AP change was made, to preserve the active connection.

After owner reconnected phone Wi-Fi and supplied `192.168.1.18:36721`, ADB
connected successfully and phone ping passed 2/2 (49–172 ms). Recovery followed
phone Wi-Fi reconnection; root cause of the transient ARP loss is not proven.
Readback confirms SM-F926B/API35, active EXTERNAL_DEX display2 at1920×1080,
Accessibility enabled and bound with capabilities161, crash list empty.
No forced Accessibility grant, ADB pairing reset or router change was needed.

## First corrected build and physical regression

JDK17 full Android build PASS201 tasks/3m1s (`/tmp/t010-grid-refresh-gradle.log`).
Main review confirmed fresh explicit GRID is handled before missing-session
rejection; structural windows are separated from node labels/identities. Grid
checks display/bounds/window ID/layer/type/focus/active state; numbered targets
still require full identity and image targets still require fresh patch checks.
Diagnostics emit categories/counts only. No KWS model/routing optimization.

Original certificate verified for both artifacts; matching-signed replacement
installs PASS, preserving DPC/data/Accessibility:
- app SHA256 `f5c05bc48180a2bba780f5c5be515a22923907c75ac0eb23bcfcff776b26dc81`
- test SHA256 `e7bb44c70dc3989b490b83aecd4076dec451a7336cb1506e710cd2df4e6432b9`

Fold3 `am instrument --no-restart` suite PASS11/101.777s, zero skips/failures,
log `/tmp/t010-grid-refresh-fold3.log`: seven selected numbered-screen tests,
three VoiceHandoffDeviceTest cases and AutomaticListeningDeviceTest. New live
fixture GRID survives changed labels, rejects a covering new window without tap;
strict same-bounds replaced numbered targets reject. Existing coordinates,
subdivision, bitmap-X, real expiry, routing, provider and recovery checks pass.

Initial timings on controlled local-final regression: feedback terminal at
830413ms, wake requested830416ms, model ready830995ms, recording831026ms,
first positive capture831139ms: terminal→capture726ms. This is not the owner's
natural failure reproduction. First installed artifact lacks subsequent
feedback_requested/terminal-duration instrumentation; rebuild before that measure.

Owner explicitly requests live YouTube/another installed app grid validation.
Prepare non-tapping live foreground instrumentation and verify visual grid in
RAM; fixture success alone does not establish this requested acceptance.

## Coordinate and latency correction under latest owner instruction

Owner now reports live grid appears but cell selection fails, requests coordinate
touch regardless of app content/window changes, and delegates autonomous work
while away. D-023/D-024 record the updated boundaries. Source review found:
- Full window/node snapshot still rejected coordinate-cell taps.
- Exact phrase “bấm ô số N” was not accepted by the screen grammar.
- Wake→STT waited for screen screenshot/analysis up to1500ms.
- Model reload costs575–595ms in three earlier warm cycles; current cold boot
  logs model ready3151ms and first capture3341ms.
- Canned feedback watchdog was25s; known unsupported “tắt YouTube” went through
  backend routing, allowing provider timeouts before reporting failure.

Android source now uses display-only geometry checks for grid cells, accepts
“bấm [vào] ô số N”, starts STT alongside guarded preparation, keeps one
worker-owned KWS model with fresh streams, bounds canned feedback at4s, and
rejects exact known close-app requests locally. Target identities/patches remain
strict; no partial-text execution or phone fallback. Monotonic metadata records
STT request/ready/speech end/final/dispatch/terminal and TTS queue/start/terminal.

Frozen JDK17 full build PASS153tasks/2m12s,59debug units54app+5DPC, zero
failures/errors/skips (`/tmp/t010-coordinate-voice-final-gradle.log`). Final test
APK/lint rebuild PASS108tasks/22s (`/tmp/t010-coordinate-voice-apk-frozen.log`).
Original certificate verified for reviewed artifacts:
- app SHA256 `a6b164e94fe9c93efc3672e4a50701bc02a6e41b230bddc7fa12a963cffbe6d1`
- test SHA256 `a52568c5f8c6fea76a8b5ebbd04896df65ce4ec05107f3e91e2cfba72ef62cc8`

Deployment network degraded: current ADB endpoint192.168.1.18:35505, model/API/
DeX2 confirmed, Accessibility bound/capabilities161/crash list empty. Upload TCP
showed≈7% retransmissions/cwnd2/≈40KBps, despite device responding and ample disk.
Cancelled incomplete installer, restarted ADB using SDK37.0.1, then used explicit
zstd push of unchanged signed APK. Temporarily set host Wi-Fi powersave disable
and reactivated the existing connection; original profile value was default(0).
Restore default and reactivate at end of validation. No router/phone network
configuration, pairing key, permission or package data change performed.
Coordinate build installation/new physical tests are pending at this checkpoint.

## 2026-10-04 checkpoint — physical results and unfinished latency step

Production signed coordinate/latency APK installed after successful checksum-verified
zstd upload. Current last working SDK37 ADB endpoint42789; checkpoint device list
is empty. Nine selected screen cases passed; initial combined runner then failed
voice test harness timing/startForegroundService ordering. Corrected test-only
foreground start/STOP barrier/provider-ready fixes passed5voice/recovery cases in
37.615s. Corrected signed test SHA256
`0cf1eea8a245badec074cccef4d06cb007ccfe047ad7c1fba5d75bc03de0cb17`.
Live YouTube grid persistence/subdivision/coordinate dispatch passed1/6.356s.
Settings test result unavailable: checkpoint /tmp log absent, not acceptance.

Warm feedback-terminal→capture150/151/175ms; model reuse7/10/13ms. Missing-TTS
callback watchdog4022ms, request→capture4172ms. Controlled final→dispatch5–6ms,
but dispatch→terminal1529/1626/1607ms. Main-thread whole encrypted history
record+read/persist suspected (60entries), not proven; measure before changing.
No history optimization/D-025 yet. Resume steps and exact inventory in
SESSION_HANDOFF.md. Historical /tmp logs are unavailable in checkpoint filesystem;
these are recorded prior verified results, not new reruns. Host powersave profile
restored to default0; both repositories remain intentionally uncommitted.
Owner requested checkpoint for next session; broader T-006/T-010 acceptance open.
