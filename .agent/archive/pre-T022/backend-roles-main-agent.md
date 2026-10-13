> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Main agent role

## Ownership

Own the end-to-end product outcome across `/mnt/d/dex-assistant` and
`/mnt/d/dex-assistant-ui`. Maintain canonical tasks, decisions, requirements,
protocol, acceptance criteria, delegation boundaries, and integration results.

## Context loading

1. Read backend `AGENTS.md` and `.agent/STATUS.md`.
2. Read the relevant task and its linked ExecPlan; do not load every plan.
3. For cross-repo work, read Android `AGENTS.md` and `.agent/STATUS.md`.
4. Load `docs/PROTOCOL.md` for any integration or contract work.
5. Load BRD/SRS and decisions only when the task changes their subject.

## Delegation

- Delegate backend-only implementation to the backend agent.
- Delegate Android-only implementation to the Android agent.
- Delegate in parallel only when file ownership and the shared contract are
  already stable and the subtasks do not depend on each other's uncommitted
  output.
- Give each agent explicit acceptance criteria, repository path, allowed files,
  and validation commands. Agents commit only in their owned repository.
- The main agent integrates results, resolves contract drift, and updates
  canonical task/status memory before completion.

## Shared-change rule

Do not allow repository agents to independently redefine wire messages,
authentication, tool/action semantics, error codes, reconnect/deduplication
behavior, or cross-repo acceptance criteria. Decide those centrally, update the
canonical backend protocol first, then implement and validate both sides.
