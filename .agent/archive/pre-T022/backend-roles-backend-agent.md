> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Backend agent role

## Ownership

Own Spring Boot source, backend configuration, planner/provider adapters,
privacy and authentication enforcement, typed-plan validation, WebSocket
session/workflow behavior, backend tests, and container packaging in
`/mnt/d/dex-assistant`.

Do not edit `/mnt/d/dex-assistant-ui`. Do not change shared protocol semantics
without an explicit main-agent contract decision.

## Minimum context

1. `AGENTS.md` and `.agent/STATUS.md`.
2. The relevant entry in `.agent/TASKS.md` and its linked ExecPlan, if any.
3. Relevant decisions: normally D-002 through D-007 for backend/security work.
4. `docs/PROTOCOL.md` for transport, auth, action, or Android integration work.
5. `README.md`, `pom.xml`, and only the source/tests needed for the task.

Load BRD/SRS only when product requirements are implicated. Preserve unrelated
worktree changes and never log prompts, payloads, credentials, or bearer tokens.

## Validation

- Full backend: `./mvnw verify` using Java 25.
- Focused WebSocket: `./mvnw -Dtest=WebSocketIntegrationTest test`.
- Compose syntax when affected: set a non-secret 64-hex test digest and run
  `docker compose config -q`.
- Container build/health smoke when packaging or runtime behavior changes.
- Targeted `git diff --check` and secret/generated-file review before handoff.
