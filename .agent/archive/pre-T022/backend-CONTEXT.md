> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Stable project context

## Product and system boundary

DeX Assistant is a single-user assistant for a Samsung Galaxy Z Fold3 running
DeX. Android owns microphone/UI/device execution. The backend provides the
authenticated planning and metered provider boundary; provider credentials do
not belong in the APK. Device actions are validated and executed on Android.

The Android app now includes service-first voice, local deterministic command
routing, DeX screen control, and a separate Device Owner DPC. These features
have verified slices, but their full physical and release acceptance is still
tracked in active tasks; see `.agent/STATUS.md` and `.agent/TASKS.md`.

The backend exposes protocol V1 over authenticated WebSocket at
`/ws/v1/assistant`; the canonical contract is `docs/PROTOCOL.md`. Local command
capabilities are documented in `docs/LOCAL_CAPABILITIES_V1.md`. The Gemini
Java JAR/Fold3 integration and configured RPM exhaustion/reconnect slices have
passed, but this does not establish production provisioning, account billing,
or complete T-009 acceptance.

## Product constraints

- One user; multi-tenancy is out of scope.
- Target interaction is hands-free and context-aware.
- The desired post-speech action latency is below two seconds; natural-speech
  latency and accuracy remain unverified against the full acceptance set.
- The original BRD targets zero monthly operating cost; check current owner
  decisions before treating provider costs or free-tier availability as settled.
- Transport must be encrypted and authenticated in production.
- Audio, credentials, and owner-protected state must remain within the accepted
  privacy/security boundaries recorded in `.agent/DECISIONS.md` and `docs/SRS.md`.

## Source routing

- Product goals and boundaries: `docs/BRD.md`
- Requirements: `docs/SRS.md`
- Original delivery roadmap: `docs/PLAN.md` (historical sequencing; current
  priorities are in `.agent/STATUS.md`)
- Current execution state: `.agent/STATUS.md`
- Work queue and acceptance criteria: `.agent/TASKS.md`
- Accepted decisions: `.agent/DECISIONS.md`
- Multi-step work: the active task's linked plan under `.agent/plans/active/`
- Reproducible results: `.agent/evidence/`

## Known open validation

- T-003: Fold3 zero-touch Wi-Fi and complete text-path round trip/latency.
- T-004: clarify baseline-plan exit criteria against downstream device tests.
- T-006/T-010: natural spoken behavior, wake/cue reliability, and physical
  session/resource acceptance.
- T-008: Android CI and signed device-test release automation.
- T-009: remaining backend/provider, local/offline, reconnect, and physical
  acceptance. Live tests used an isolated test identity and do not prove
  production credential setup or account billing.

The voice/UI migration document is a proposal, not accepted implementation
scope. Do not use the original text-first delivery sequence in `docs/PLAN.md`
to override the current task board.
