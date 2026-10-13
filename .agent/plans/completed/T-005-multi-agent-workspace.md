> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-005 — Cross-repository multi-agent workspace

- Owner: main agent / product owner
- Status: `COMPLETE`
- Started: 2026-10-01
- Task: `.agent/TASKS.md` (`T-005`)

## Outcome and boundaries

Keep the backend and Android client as separate Git repositories while making
one main agent responsible for end-to-end outcomes. Backend and Android agents
may work in parallel within explicit repository ownership boundaries. Persist
the model in files so a new session can reconstruct it without chat history.

This work does not move either `.git` directory, merge histories, change the
application protocol, scaffold additional product behavior, or resolve the
pending T-004 Android product decisions.

## Inputs and constraints

- Backend repository: `/mnt/d/dex-assistant`.
- Android repository: `/mnt/d/dex-assistant-ui`.
- Canonical project memory and product requirements remain in the backend
  repository under `.agent/` and `docs/`.
- Canonical wire contract remains `docs/PROTOCOL.md` in the backend repository.
- Preserve unrelated uncommitted backend documentation and memory changes.
- Each repository owns its own commits, build outputs, and secret exclusions.

## Decision questions and evidence

1. **Workspace discovery:** a checked-in multi-root workspace must resolve both
   repositories from their current sibling layout.
2. **Role loading:** each role must have a concise entry point, linked sources
   of truth, ownership boundary, and validation obligations.
3. **Shared changes:** protocol, authentication, action/result, and end-to-end
   acceptance changes require main-agent coordination and validation in both
   repositories.
4. **Current Android handoff:** the UI repository must expose T-003/T-004 state
   without duplicating canonical decision history.
5. **Concurrency safety:** repository agents commit only in their owned repo;
   the main agent assigns non-overlapping work and integrates results.

## Milestones

1. Record T-005 and the accepted operating model.
2. Audit backend and Android context with the corresponding repository agents.
3. Add multi-root workspace and durable role instructions.
4. Add concise Android handoff memory linked to canonical backend sources.
5. Validate paths, links, clean generated content, and both repository diffs.

## Validation

- Parse the multi-root workspace file as JSON with comments where supported, or
  keep it strict JSON and parse it with an available JSON tool.
- Confirm every linked local path exists from the documented working location.
- Run `git diff --check` for files introduced or modified by T-005 in each repo.
- Confirm role files name their repository ownership and shared-contract rule.
- Confirm no credentials, build output, or nested Git repository was added.

## Risks and escalation

- A session opened with only one repository may not have write permission to
  the sibling repository; open the multi-root workspace or grant both roots.
- Parallel agents can produce incompatible contract changes; the main agent
  must serialize contract decisions before repository implementation diverges.
- Duplicated task state can become stale; Android memory is a handoff pointer,
  while backend `.agent/TASKS.md` remains canonical.
- T-004 still requires owner approval and device evidence; workspace refactoring
  must not represent Android baseline proposals as accepted.

## Progress and findings

- [x] Product owner selected one main agent plus backend and Android agents.
- [x] T-005 and this ExecPlan created.
- [x] Repository-agent context audits incorporated.
- [x] Workspace and role files added.
- [x] Cross-repository validation complete.

The backend audit established the Java/backend ownership boundary, selective
memory-loading order, Maven/WebSocket/container validation, and current T-004
blockers. The Android audit established the mobile ownership boundary, Gradle
and device validation, and identified the existing API-35 scaffold versus the
unaccepted API-36 T-004 proposal. Status files now expose that mismatch without
silently accepting either target.

Validation on 2026-10-01:

- Node parsed `Dex-Assistant.code-workspace` as strict JSON.
- Both `.git` directories, canonical protocol, and Android handoff paths exist.
- Targeted `git diff --check` passed in both repositories.
- Git status confirmed no nested repository or generated build output was added.

## Decision and requirement updates

- Record the accepted agent/repository ownership model in `.agent/DECISIONS.md`.
- Update `.agent/STATUS.md` without replacing T-004 as the active product task.
- Update this plan and T-005 with validation evidence before completion.
