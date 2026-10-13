> Retired backend project document; preserved only as historical provenance. Current Android workspace authority is [AGENTS](../../AGENTS.md) and [STATUS](../../.agent/STATUS.md).

# DeX Assistant agent guide

Use `.agent/STATUS.md` as the entry point for every new session. Read only the
files linked from it that are relevant to the current task.

## Cross-repository roles

This repository is the canonical project-memory and backend repository. The
Android repository is normally checked out at `/mnt/d/dex-assistant-ui`. Open
`Dex-Assistant.code-workspace` when a task spans both repositories.

- The main agent owns end-to-end outcomes, task decomposition, shared-contract
  decisions, integration validation, and final status. See
  `.agent/roles/main-agent.md`.
- The backend agent owns changes only in this repository. See
  `.agent/roles/backend-agent.md`.
- The Android agent owns changes only in the Android repository. Its durable
  entry point is `/mnt/d/dex-assistant-ui/AGENTS.md`; the mirrored role boundary
  is summarized in `.agent/roles/android-agent.md`.

Repository agents may run in parallel only for non-overlapping work. Changes to
the WebSocket protocol, authentication, action/result types, or end-to-end
acceptance criteria require main-agent coordination and validation in both
repositories.



## Request triage

For every new user request, classify the work before editing files.

### 1. No task record required

Do not create a task for:

- Questions, explanations, brainstorming, or status requests.
- Read-only reviews or investigations whose results have no durable follow-up.
- Trivial corrections that are local, low-risk, and completed in the current
  turn, such as a typo or formatting-only change.

Perform the work directly and report the result.

### 2. Task required, ExecPlan not required

Create or update a task in `.agent/TASKS.md` when the request:

- Changes source code, configuration, tests, contracts, or durable documentation.
- Introduces behavior that needs acceptance criteria.
- Produces follow-up work that may continue in another session.
- Fixes a reproducible bug.
- Requires validation before it can be considered complete.

A task does not need an ExecPlan when the change is localized, the solution is
well understood, risks are low, and validation can be completed in one short
work cycle.

Before creating a new task, search `.agent/TASKS.md` for an existing task with
the same outcome. Extend the existing task instead of creating a duplicate.

### 3. Task and ExecPlan required

Create a task and an ExecPlan under `.agent/plans/active/` when any of these
conditions apply:

- The work spans multiple modules, applications, or system layers.
- It changes architecture, security, authentication, protocol, persistence,
  deployment, or public API contracts.
- It is a migration, significant refactor, or new end-to-end feature.
- Important technical uncertainty requires research or a prototype.
- The work needs multiple independently verifiable milestones.
- The work is high-risk, difficult to reverse, or likely to span sessions.
- The user explicitly requests a plan or asks for a long-running task.

Follow `.agent/PLANS.md` when creating or maintaining an ExecPlan.

### 4. Relationship to the active task

- If the request contributes to the same outcome as the active task, update that
  task instead of creating another one.
- If it is a distinct outcome, create a separate task.
- Do not silently replace an unfinished active task. Record the new priority in
  `.agent/STATUS.md`.
- A follow-up correction discovered while implementing a task belongs to that
  task unless it is independently valuable or substantially expands scope.

### 5. Triage output

Do not ask the user to choose between task and ExecPlan when the repository
provides enough evidence to decide.

Briefly state the classification only when creating a new durable artifact:

- `Direct change — no task needed`
- `Task T-### — localized work`
- `Task T-### + ExecPlan — multi-step/high-risk work`

When uncertain, create a task. Create an ExecPlan only when at least one
ExecPlan condition clearly applies.


## Working protocol

- Before changing code, identify the active task in `.agent/TASKS.md` and its
  acceptance criteria. If no task matches, add one with a stable `T-###` ID.
- Use `docs/BRD.md` for product scope, `docs/SRS.md` for system
  requirements, and `docs/PLAN.md` for the original delivery phases.
- Record durable technical or product choices in `.agent/DECISIONS.md`. Do not
  store routine implementation details there.
- Before ending work, update `.agent/STATUS.md` with the verified current state,
  next action, blockers, and validation performed; update the active task.
- Keep memory concise. Replace stale status instead of appending session logs.
- Do not treat chat history as project truth when repository documents disagree.



## Definition of Done

A task may be marked `DONE` only when all applicable conditions are met:

- The task-specific acceptance criteria are satisfied.
- The affected module builds successfully.
- Relevant automated tests pass.
- New behavior has tests, or the reason tests are not feasible is documented.
- User-visible behavior is verified through a runnable command or demo flow.
- No secrets, credentials, generated files, or unrelated changes are committed.
- Public contracts, configuration examples, and operational documentation are
  updated when affected.
- Relevant validation commands and their results are recorded in the active
  ExecPlan or `.agent/STATUS.md`.
- `.agent/TASKS.md` and `.agent/STATUS.md` reflect the actual final state.
- Remaining limitations or unverified behavior are stated explicitly.

Compilation alone is not completion. Do not mark a task `DONE` when required
verification has not run; use `BLOCKED` or keep it `IN_PROGRESS` and record why.
