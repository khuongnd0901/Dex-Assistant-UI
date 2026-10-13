> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-003 — Android text-first vertical slice

> 2026-10-10 owner architecture override: CANCELLED: original backend text-slice outcome superseded; Android Wi-Fi/text verification transfers to T-018. See [T-018](T-018-direct-ai-final-fold3-acceptance.md) and [task board](../../TASKS.md). Earlier backend requirements/progress are historical and do not schedule new backend work.

- Owner: main agent with Android implementation in `/mnt/d/dex-assistant-ui`
- Status: `IN_PROGRESS`
- Started: 2026-10-01

## Outcome and boundaries

Deliver two Android APKs: a user-facing text/WebSocket client and a separate
Device Policy Controller. Prove `LAUNCH_APP` and zero-touch `TOGGLE_WIFI` from
text through the backend V1 protocol on the dedicated Fold3. Exclude voice,
wake word, Accessibility, split-screen, `CONNECT_WIFI`, Play distribution, and
production EMM enrollment.

## Inputs and constraints

- Canonical protocol: `docs/PROTOCOL.md`; decisions D-003, D-004, D-006-D-009.
- Compile/target API 35, min API 30, JDK 17.
- The main app owns backend credentials; the DPC never receives them.
- Both APKs use the same signing certificate. DPC IPC is explicit and protected
  by a signature permission.
- Fold3/API 35 and Device Owner provisioning have since been verified; see
  current progress below. Remaining T-003 device acceptance is listed there.

## Decision questions and evidence

- Device Owner eligibility: verify the DPC package is owner after clean-device
  ADB provisioning and record Fold3 API/release.
- Wi-Fi execution: call the documented Device Owner exemption and observe the
  requested Wi-Fi state before reporting success.
- Protocol retry: retain one request ID for an explicit retry; never silently
  resubmit an ambiguous in-flight command after disconnect.
- Credential lifecycle: encrypt the bearer token with a non-exportable
  AndroidKeyStore AES-GCM key and exclude storage from backup.

## Milestones

1. Canonical decisions/protocol and remote FakePlanner mode are synchronized.
2. Separate DPC builds with signature-protected typed IPC and Wi-Fi tests.
3. Main app builds with provisioning, WebSocket state, launch action, DPC IPC,
   and unit tests.
4. Backend and Android automated verification passes.
5. Fold3 provisioning and end-to-end measurement pass.

## Validation

- Backend: `./mvnw verify`.
- Android: `./gradlew test lint assembleDebug` with Platform 35 installed.
- Device: record build API/release and device-owner state; execute launch and
  Wi-Fi on/off commands through FakePlanner remote mode.
- Security: wrong-signature callers cannot bind; no token, endpoint, signing
  material, or local SDK path is committed.

## Risks and escalation

- Device Owner provisioning can require a clean/factory-reset device.
- OEM firmware may differ from SDK behavior; do not claim success before the
  Fold3 test.
- Losing the owner/signing identity can require reprovisioning/reset.

## Progress and findings

- [x] Owner selected separate Device Owner DPC and API 35 baseline.
- [x] Canonical/backend support complete.
- [x] Android automated validation complete.
- [x] Fold3 provisioning and later isolated Java JAR/Gemini compound-action
  integration verified; see `.agent/evidence/T-009-java-jar-backend-2026-10-04.md`.
- [ ] T-003 acceptance complete: zero-touch Wi-Fi on/off and the text-path demo/
  latency evidence are not yet recorded against this task's acceptance criteria.

2026-10-06 ADB AVD acceptance: DPC Wi-Fi off/on both returned VERIFIED through
the app, with the initial enabled state restored. The real app UI sent `open
chrome` through a local remote-FakePlanner backend; Chrome launched and app
history recorded DISPATCHED. `ws.send`→`action.result` measured 173 ms on AVD.
Physical Fold3 demo and latency remain unverified; see
`.agent/evidence/T-003-adb-check-2026-10-06.md`.

Initial automated validation on 2026-10-01 passed backend `./mvnw verify` (17
tests) and Android `test lint assembleDebug` for both modules on JDK 17/API 35;
APK inspection confirmed matching certificates and the intended permission
boundary. Later Fold3 provisioning and real-backend compound-action evidence
advance device validation, but do not demonstrate the required DPC Wi-Fi toggle
or the complete T-003 text round trip and latency measurement. Keep this plan
active until those specific acceptance items are evidenced.

The next-session orchestration, safety gate, parallel backend/Android agent
assignments, device commands, evidence requirements, and closeout rules are in
`.agent/NEXT_SESSION.md`.

## Decision and requirement updates

Update T-003/T-004, D-009, protocol error codes, Android handoff status, and
BRD/SRS constraints. Keep the task in progress until device evidence exists.
