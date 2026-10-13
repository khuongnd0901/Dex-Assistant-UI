# DeX Assistant Android agent guide

Start every session with [.agent/STATUS.md](.agent/STATUS.md). This Android
repository is the canonical source for project memory, requirements, action
contracts, plans and evidence. Backend development is retired under
[D-037](.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired).
No backend checkout or runtime is required for development or tests.
Open [Dex-Assistant.code-workspace](Dex-Assistant.code-workspace) for this repository.

## Request triage and working protocol

Before editing, read the active task and acceptance criteria in
[.agent/TASKS.md](.agent/TASKS.md). Questions, read-only reviews and trivial
local corrections need no task. Source/config/test/contract/durable documentation
changes and reproducible bugs need a stable T-### task; search for the same outcome
before creating one. Extend an existing matching task. Distinct outcomes get
separate tasks; record changed priority in STATUS without silently replacing
unfinished work.

Create an ExecPlan when work spans modules/layers, changes architecture,
security/authentication/protocol/persistence/deployment/public contracts,
is a significant refactor/migration/end-to-end feature, has important uncertainty,
needs multiple milestones, is high-risk/long-running, or explicitly requests a
plan. Follow [.agent/PLANS.md](.agent/PLANS.md). State classification only when
creating an artifact: “Direct change — no task needed”, “Task T-### — localized
work”, or “Task T-### + ExecPlan — multi-step/high-risk work”.

Use [BRD](docs/BRD.md), [SRS](docs/SRS.md), the relevant action contracts and
[DECISIONS](.agent/DECISIONS.md) for product/technical choices. Record consequential
choices in DECISIONS; routine implementation details belong in evidence/plan.
[Original delivery plan](docs/PLAN.md) and backend-only historical notes are
archival. They cannot override current Android decisions or restart cancelled
T-003/T-008/T-009/T-019 scopes. Current source and repository memory govern
handoff; do not use chat history to override them.

## Ownership and contracts

One Android agent owns end-to-end source, task/status/decision maintenance,
contracts and validation. See [role](.agent/roles/main-agent.md). Delegate only
with explicit, non-overlapping file ownership. Android action/result changes
are decided and tested here against the Android executor and local contract
fixtures. [WebSocket protocol](docs/PROTOCOL.md) is legacy history; no
backend-first protocol workflow or server implementation is required.

## Build and test targets

Use JDK 17, the Gradle wrapper and Linux SDK `/home/khuongnd/Android/Sdk` in WSL.
Current target is Android 15/API 35 with minSdk 30. Run applicable `./gradlew test`,
`./gradlew lint`, `./gradlew assembleDebug` and `git diff --check`.

Windows simulator first; select from this repository:

```bash
bash scripts/android/select-test-target.sh dex --emulator-only
```

Use `foldable --emulator-only` for hinge/layout checks. Follow exact reported
ADB/PORT/SERIAL; Windows ADB uses 5038, Linux uses 5037. Setup, commands and limits:
[scripts/android/README.md](scripts/android/README.md). Actual Samsung DeX,
natural wake/mic/STT/TTS, Fold3 PSS and soak remain coordinated under
[T-020](.agent/plans/active/T-020-consolidated-fold3-acceptance.md), deferred by owner.
Never mark physical gates complete from simulator/build evidence. Preserve owner
history/settings/credentials, original signing, Device Owner/Assistant role,
Accessibility/grants and sole wireless ADB. No broad destructive legacy device
fixtures, resets, uninstall, permission/role changes or live provider calls
without the relevant authorized task and reviewed flow.

## Definition of Done and handoff

DONE requires task-specific acceptance, applicable successful build/tests,
meaningful tests for new behavior (or documented infeasibility), runnable demo
or verification for user-visible behavior, updated contracts/config/docs,
recorded validation and explicit limitations. Compilation alone is insufficient.
Keep BLOCKED/IN_PROGRESS if required verification is missing. Add no secrets,
generated artifacts or unrelated changes to commits.

Before ending, replace STATUS with concise verified state, next action, blockers
and validation; update TASKS and the active plan. Keep memory concise, preserving
historical evidence separately instead of appending session logs.
