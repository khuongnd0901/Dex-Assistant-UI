> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-002 — Java backend first slice

- Status: `COMPLETE`
- Started: 2026-09-30
- Depends on: completed T-001 and decisions D-002 through D-006

## Outcome and boundaries

Build a locally runnable/containerized Java backend with authenticated raw JSON
WebSocket, typed execution plans, deterministic fake planning, optional Gemini
planning, dummy remote-tool results, privacy policy, and operational metrics.
Exclude Android source, real device actions, Redis, and OCI provisioning.

## Inputs and constraints

- Implement the accepted backend architecture in `.agent/DECISIONS.md`.
- Meet T-002 acceptance criteria and the protocol/security requirements in SRS.
- Preserve secrets outside source, logs, test fixtures, and container layers.

## Implementation decisions

- Java 25, Maven, Spring Boot 4.0.8, Spring AI 2.0.1.
- Domain/application ports with inbound WebSocket and outbound fake/Gemini
  adapters; in-memory six-turn/30-minute conversation state.
- Versioned 16-KiB JSON envelopes and opaque bearer authentication.
- Multi-stage non-root container for AMD64/ARM64; local and gemini profiles.

## Milestones

1. Build and package boundaries compile with configuration validation.
2. Domain types, policies, fake planner, memory, and orchestration pass unit tests.
3. Authenticated WebSocket protocol passes integration/security tests.
4. Gemini adapter compiles and its mapping is covered without a live secret.
5. Container builds and the documented local demo/validation succeeds.

## Validation

- `./mvnw verify`
- `docker build -t dex-assistant-backend:test .`
- Start local profile and complete authenticated WebSocket command round trip.
- Confirm repository/container do not include private keys or API credentials.

## Risks and escalation

- Spring AI APIs may require adapter adjustments while preserving the Planner
  port and protocol.
- Live Gemini latency/quota is external and must not make deterministic CI fail.
- End-to-end two-second latency remains for T-003 device validation.

## Progress and findings

- [x] Baseline decisions and SRS synchronized.
- [x] Project scaffold and package boundaries.
- [x] Domain/application implementation.
- [x] WebSocket/security implementation.
- [x] Gemini adapter.
- [x] Tests, container, and demo validation.

Validated on 2026-09-30:

- `./mvnw verify`: 16 tests passed, with no failures or errors.
- `BackendOverheadTest`: fake-planner p95 remained below the 100 ms budget.
- `docker build -t dex-assistant-backend:test .`: succeeded.
- Container readiness returned `{"status":"UP"}` as UID/GID `10001:10001`;
  the image health command succeeded and the temporary container was removed.
- `docker compose config -q`: succeeded with a test digest.
- Live Gemini was not called because no API credential was provided; the adapter
  tool-call mapping and fail-closed validation are covered with a deterministic
  `ChatModel` test stub and live use remains opt-in.

## Decision and requirement updates

Update D-002 through D-006 only if implementation evidence invalidates a choice.
Record validation in this plan and `.agent/STATUS.md`; move it to completed only
when T-002 meets its acceptance criteria.
