> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-022 — Android canonical project memory and workspace

Created2026-10-13. DONE2026-10-13. Owner explicitly retires backend development;
future development/test sessions start from `/mnt/d/dex-assistant-ui`.

## Outcome and boundaries

Move necessary canonical Markdown/project memory to Android and make its
AGENTS/STATUS the independent session entry point. Preserve task IDs, cancelled
states, unfinished physical gates and sanitized evidence. Transfer minimal
selector/perf tooling needed to run documented Android checks without backend
checkout. Backend code/infra/credentials are untouched; no app/runtime changes,
APK installs, device tests, cloud deletion, signing or role/grant operations.

## Inputs and constraints

[Current status](../../STATUS.md), [task board](../../TASKS.md),
[ExecPlan standard](../../PLANS.md), relevant current Android documents and both
AGENTS govern work until handoff. Owner instruction overrides earlier backend
canonical-memory and backend-first contract rules. D-036 runtime stays Android-only.
Preserve both dirty worktrees; copy/verify before replacing old docs with redirects
or removing migrated originals. Avoid overwriting Android-local material; handle
collisions explicitly. Existing T-021 frozen app/test sources must not change.

## Decision questions and evidence

| Question | Evidence and rule |
| --- | --- |
| What must move? | Canonical memory/active plans/evidence/product+action+test docs and their link closure. Archive backend-only context; no required current document depends on backend checkout. |
| How preserve history/collisions? | Before/after manifest, source bytes retained outsideGit, destination collision comparison/explicit archive. Task states/IDs/thresholds retained, no automatic acceptance. |
| Can tests start from Android alone? | Local selector/perf scripts+dependencies; host tests and actual emulator-only selection from Android; no production commands/device install. |
| Where is authority after handoff? | Android AGENTS/STATUS/README/roles and single-folder workspace agree; old backend entry points redirect and forbid renewed backend work. Accepted choice in Android DECISIONS. |

## Milestones

1. M0 inventory linked docs/tools and collisions, freeze pre-migration snapshots.
2. M1 migrate memory/docs+dependencies and make Android-local links/entry points.
3. M2 transfer/adapt minimal test tooling, introduce single-folder Android workspace.
4. M3 validate links/authority/task continuity/tool tests and selector from Android;
   replace retired backend entry points with redirects, finalize task/status/evidence.

Android agent owns target repository edits; main agent coordinates canonical
choice/scope, source-side redirects and end-to-end validation. No concurrent
edits to the same files.

## Validation

From Android root: local-link/Markdown authority validation; Python selector/perf
host suites; shell syntax; `bash scripts/android/select-test-target.sh dex --emulator-only`.
Verify T-02120-file manifest unchanged, migrated artifacts/provenance, old entry
points and task IDs/states/gates; changed-file whitespace. No Gradle rerun needed
if app/DPC/Gradle/contracts remain unchanged; justify checks in evidence. Test
workspace standalone by auditing required paths/callers for backend checkout
references. Historical references are explicitly labelled and may link immutable
remote archived sources; they cannot become active prerequisites.

## Risks and escalation

Old docs contain obsolete backend-first directions; headers/active guidance must
clearly supersede them. Relative paths and script repo-root defaults can break.
Use explicit inventory mapping and no overwrites of distinct target files. Do
not migrate secrets/generated APK/model/audio/logs or restart cancelled tasks.
No approval needed for authorized reversible documentation/tool transfer; scope
expansion into app behavior or destructive infrastructure is not part of this task.

## Progress and findings

- [x] Owner canonical-ownership/workspace direction recorded; no equivalent task found.
- [x] M0 inventory/snapshots; exact source→target manifest and collision snapshots outsideGit.
- [x] M1 memory/docs migration and link repair;139MD/448local links PASS.
- [x] M2 standalone tooling/workspace; single Android root, selector/perf local dependencies.
- [x] M3 independent host/link/authority/hash checks and actual selector PASS;132mapped files verified before110source-file removals,14retired redirects/workspace entries; final handoff complete.

## Decision and requirement updates

Record D-037 in migrated Android DECISIONS: Android now owns project memory,
requirements, action contracts and acceptance; backend retired/frozen. Update
Android AGENTS/STATUS/TASKS/README and supersede outdated backend-first rules in
active docs. BRD/SRS behavior/latency/privacy stays as accepted under D-036;
keep historical requirements clearly labelled. Final T-022 DONE requires all
migration gates, not a claimed app runtime change.

Android handoff: [migration evidence](../../evidence/T-022-android-canonical-workspace-2026-10-13.md).
132mapping records (115MD/7docfixtures/10tools),43host tests PASS;20frozen T-021sources unchanged.
Actual selector booted isolated Windows AVD and applied disposable desktop setup;
actual selector retry PASS on serial127.0.0.1:5555/port5038/API35 after initial
Windows/WSL wrapper stayed attached; main agent ended only spawned wrappers,
retaining running AVD. No physical/provider/runtime/build/install action.

Final root verification: source-side retirement preserves frozen backend runtime;
Android owns canonical memory. T-022 complete; other task states and acceptance
remain unchanged. No commit/push. Final results in migration evidence.
