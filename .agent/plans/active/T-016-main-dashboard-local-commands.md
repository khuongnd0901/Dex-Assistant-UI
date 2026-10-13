> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

> Scheduling override 2026-10-13: owner requests Windows simulator tests through WSL during continuation. All remaining physical Z Fold3 execution is consolidated in [T-020](T-020-consolidated-fold3-acceptance.md), one prepared campaign. This overrides older immediate/separate device-run instructions, preserves acceptance gates and does not imply PASS.

# T-016 — DeX-first dashboard and visible local commands

## Outcome and boundaries

Opening the Android app immediately shows voice health, primary controls, transcript, manual command entry and grouped examples of supported local commands. DeX is the primary layout; Fold3 remains usable. Advanced controls remain reachable through one secondary menu. No action execution, protocol, authentication or permission semantics change.

## Inputs and constraints

- Canonical acceptance: [T-016](../../TASKS.md). Product behavior: [SRS](../../../docs/SRS.md) and D-035.
- Preserve the current T-015 worktrees and service-first voice flow. Keep Start/Resume, Pause and emergency Stop reachable. Transcript and microphone state remain RAM-only and credential-redacted.
- The main page itself has no scroll; a bounded command-list pane may scroll. A list tap populates manual entry but never submits it. Parameter placeholders must be replaced before Run is enabled.
- Only current local runtime actions are presented as supported. Conditional commands disclose their app, desktop or permission requirements; Bluetooth and unadvertised schema-2 actions are omitted. The full view covers every supported catalog pattern, not merely one phrase per command ID; a searchable user-goal taxonomy keeps aliases folded under a canonical phrase.

## Decision questions and evidence

| Question | Evidence and acceptance rule |
| --- | --- |
| Can the catalog drive a truthful command list? | Inspect `commands.json`, `CommandRegistry`, `ActionRouter.supportedActions()` and capability checks. Every displayed command ID exists in the loaded catalog and unsupported actions are absent. |
| Does “Xem tất cả” cover every supported way to say a command? | Compare displayed canonical-plus-alias patterns against the bundled supported catalog, including scoped variants. Search finds both primary and alternate phrases; selecting a displayed phrase only fills entry. |
| Does the layout keep essential controls usable? | Compose semantics/layout checks at DeX and compact Fold3 sizes; controls, transcript and command-list entry fit without clipping or page scroll. |
| Can examples avoid accidental execution? | UI test verifies selecting a sample changes entry only; Run uses existing `onSend`; unresolved placeholders cannot run. |

## Milestones

1. Record T-016, this ExecPlan and the accepted SRS/UI decision. Done when task, plan and requirements agree.
2. Build the Android command-list presentation from the catalog, with popular groups, full-list access, capability filtering and prerequisite labels. Done when model tests match the bundled catalog.
3. Rework the dashboard into a DeX-first responsive layout with secondary features in a menu. Done when UI tests cover selection, visibility and compact/wide layouts.
4. Run Android test/lint/build and, if a target is available, a read-only UI inspection on Fold3/AVD. Record actual results and remaining device limits.
5. Owner follow-up: replace one-example-per-command full view with all supported patterns, organized by user goal and searchable, with aliases folded under their main phrase. Done when coverage and UI tests pass on host and AVD.

## Validation

- JDK 17: `./gradlew test lint assembleDebug --max-workers=1 --offline` in Android repository.
- Relevant `DashboardLayoutTest` or equivalent Compose semantics checks, plus catalog/sample behavior tests.
- `git diff --check` in both repositories; review changed files for secrets, generated artifacts and T-015 overlap.
- Device evidence is required for physical DeX claims; host checks alone support only source/layout-level completion.

## Risks and escalation

- Small windows may leave too little height for the catalog: keep controls and transcript fixed and scroll only the catalog pane.
- Catalog metadata may drift: derive the full list from the loaded registry and test popular examples against it.
- Existing T-015 dirty files must not be overwritten. No owner decision remains for this UI scope; escalate only if a requirement forces a protocol or permission change.

## Progress and findings

- 2026-10-08: owner chose to hide secondary features from the main screen, show popular command groups with full-list access, prioritize DeX and fill manual entry on sample tap. Canonical task, SRS and D-035 updated.
- Android source now implements the dashboard, catalog-derived samples, runtime action filtering, independent list scrolling, secondary menu and Chat-active end control. The focused catalog routing test passes. JDK17 `./gradlew test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1` passed 201 tasks, with 155 app unit tests per variant, zero failures/errors and one expected skip each; lint and both APKs built. Scoped Android and canonical documentation diff checks pass.
- Follow-up 2026-10-08: the AVD had booted as TCP serial `127.0.0.1:5555`; the selector/launcher now validate `ro.boot.qemu.avd_name` and return it. The first focused layout run found a clipped “Xem tất cả” at 600×320 dp; Android switched that compact landscape width to side-by-side panes. Rebuild/reinstall and focused layout PASS 1/1 at all three viewports, plus typed-command smoke PASS 1/1. A 1920×1080 AVD screenshot shows the featured list and primary controls together. [Evidence](../../evidence/T-016-avd-ui-2026-10-08.md). Fold3/Samsung DeX acceptance remains open; T-016 stays `IN_PROGRESS`.
- Owner follow-up implemented: 47 supported definitions/180 catalog phrases in nine user-goal groups, with main phrases, folded variants and accent-insensitive search. Coverage derives from the catalog and confirms every displayed phrase routes LOCAL; unsupported Bluetooth is absent.
- AVD compact full-list checks exposed zero-height search/list at 320×480 and 600×320. Search moved inside the bounded list, and compact full-list mode reserves pane height for browsing. Selecting a phrase returns to the filled input without execution. Final JDK17 201-task test/lint/APK build PASS (156 app tests/variant, one expected skip each); AVD layout PASS across all three viewports. A 1920×1080 visual review led to left-aligned phrases and clearer group headings; final build/reinstall and AVD focused rerun PASS. [Evidence](../../evidence/T-016-full-local-catalog-2026-10-08.md). Fold3/Samsung DeX acceptance remains open.

## Decision and requirement updates

- [D-035](../../DECISIONS.md) and [SRS](../../../docs/SRS.md) record the accepted UI behavior. Update [T-016](../../TASKS.md) and [STATUS](../../STATUS.md) after validation.

- 2026-10-08 physical automated follow-up: current JDK17 full test/lint/APKs PASS201tasks; DashboardLayoutTest PASS on SM-F926B/API35 compact/wide viewports. Production MainActivity resumed/visible on Samsung DeX display2, freeform1209x680dp. [Evidence](../../evidence/Fold3-automated-2026-10-08.md). No pixel-level visual review; retain visual acceptance gate.
