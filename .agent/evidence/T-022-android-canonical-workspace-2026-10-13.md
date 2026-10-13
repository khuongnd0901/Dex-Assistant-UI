# T-022 Android canonical workspace migration evidence

2026-10-13. **T-022 DONE**. Android owns canonical project memory and future
development/test entry points. Main-agent source retirement and independent
validation complete. [Plan](../plans/completed/T-022-android-canonical-workspace.md),
[decision](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired),
[current status](../STATUS.md).

## Migration and continuity

- 132 source→target mapping entries:115Markdown,7documentation JSON fixtures,
  10selector/performance tool/test files. External exact-byte snapshots and
  source/targetSHA256 mapping: `/mnt/d/dex-t022-evidence/migration-map.json`,
  `backend-before/`, `android-before/`, `collisions/`, `root-before/`.
- Canonical STATUS/TASKS/PLANS/DECISIONS/CONTEXT/roles, task plans and sanitized
  evidence transferred. Existing Android evidence and docs retained. All22task
  state entries match source before retirement, including CANCELLED and physical
  IN_PROGRESS gates. No historical DONE claim was promoted to current acceptance.
- Distinct legacy README collision preserved as
  [backend README history](../../docs/archive/backend-README_legacy_2026-10-09.md);
  Android legacy README preserved. Backend root README/README.vi/AGENTS and original
  Android-tooling README separately archived, clearly marked retired. Backend-only
  instructions are historical; local current links require no backend checkout.
- New AGENTS/STATUS/roles and single-folder `Dex-Assistant.code-workspace` designate
  Android ownership. README variants, LOCAL_COMMANDS, mobile migration handoff and
  current product notes now route here. D-037 records the accepted owner direction.
- Local moved scripts:selector, both Windows starters, selector host suite,
  sanitized AVD snapshot, command latency and paired-response benchmark summaries
  with their3suites. They already resolve local dependencies; no root-default
  modification needed. No backend Java/Docker/provision/reset/install helper copied.
- Markdown links rebased to local targets. Backend-code-only references use
  immutable historical Git links at published7d3611e0f505d4750e34f0c47fd873312fb1efb4;
  current task acceptance does not depend on executing archived server instructions.

## Snapshot recovery

A slow first copy pass finished before a duplicate migration-script start was
noticed. The duplicate was stopped before target authority edits; it replaced
first Android STATUS/README snapshot with canonical copies. Exact original
Android STATUS recovered from the retained T-021 frozen-source tar, SHA256
`f035009e7ee7ccf75fa67729b6e08c75837fd84e62f6bbff8259598636720517` (8516bytes),
and raw `android-before/.agent/STATUS.md` restored. Android README source bytes
were still intact before target edits and retained externally. Both detailed
handoffs archived with rebased links:
[Android](../archive/pre-T022/ANDROID_STATUS_2026-10-13.md),
[former canonical](../archive/pre-T022/backend-STATUS_2026-10-13.md).
Current STATUS is concise; recovered archive is provenance, not renewed authority.

## Validation from Android root

| Command/check | Result |
| --- | --- |
| `python3 scripts/project/validate_project_memory.py` | PASS139Markdown/448local links; Android authority and single root workspace |
| Validator negative probes in temporary fixture | PASS: missing/escaping links, obsolete authority and extra workspace roots detected |
| `python3 -m unittest discover -s scripts/android/tests -p 'test_*.py' -v` | PASS3selector tests |
| `python3 -m unittest discover -s scripts/perf -p 'test_*.py' -v` | PASS19snapshot/latency/response tests |
| `python3 -m unittest discover -s scripts/benchmarks -p 'test_*.py' -v` | PASS15existing ASR preparation/corpus/scoring tests |
| `python3 -m unittest discover -s scripts/android -p 'test_collect_t010_natural.py' -v` | PASS6existing collector/privacy tests |
| `bash -n scripts/android/select-test-target.sh` | PASS |
| Windows PowerShell parser:both moved `start-*.ps1` | PASS |
| Original snapshot/source manifest checks | PASS132source snapshots;22task states unchanged;20T-021source SHA256 unchanged |
| Scoped changed-file Git whitespace check | PASS; broad check flags retained pre-existing CRLF-only docs, left unchanged |
| Migration mapping artifact audit | PASS:no APK/AAB/model/audio/key/raw logs moved into repository |

Main agent independently reran selector3+perf19tests and139MD/448link audit, all
PASS. Actual `bash scripts/android/select-test-target.sh dex --emulator-only`
started the isolated Windows AVD because it was unavailable. Starter reported
`BOOT_COMPLETED=1` and applied its existing disposable desktop/freeform settings;
initial Windows/WSL starter stayed attached for over3minutes after boot completed.
Main agent terminated only the spawned Linux shell/init wrappers (15058/15144),
leaving emulator running. Repeated selector on the existing AVD completed with
exit0: `TARGET=AVD ADB=windows PORT=5038 PROFILE=dex AVD=DeX_Command_Test_API35 SERIAL=127.0.0.1:5555 API=35`.
This documents a terminal-return limitation when starting an absent AVD; retrying
selection against the booted AVD works. Source-side retirement and final independent verification are complete. No physical device
queries/tests or APK install were authorized/run for this migration.

Unchanged pre-existing `scripts/android/check-direct-ai-fold3.sh` contains CRLF:
raw `bash -n` rejects its case statement; read-only CRLF-normalized stdin syntax
passes. This existing physical-preflight issue is preserved and disclosed;
it does not affect the moved selector or this simulator-only workflow.

## Limits and next handoff

No app/DPC/Gradle/instrumentation/CI/runtime source changed, no APK build/install,
physical/provider/cloud action, signing/data/role/grant change or commit/push.
Gradle not rerun because source/build/contracts in app remain unchanged;
20frozen T-021app/test SHA256 identities verified. JSON moved only to documentation,
leaving Android app contract fixtures intact. Raw evidence remains outside Git.

T-021 synthetic responsiveness/allocation conclusions and all T-020natural
mic/STT/TTS/DeX/provider/PSS/100session/8hsoak gates remain unchanged. Future sessions start here. T-022 migration is complete; other acceptance gates remain open.

## Final source retirement and review

Main agent verified132source/target/exact external snapshot hashes before retiring
source.110relocated `.agent/`/`docs/` files removed;14core redirects/workspace entries
now lead to Android. Backend Java/config/infra/runtime and frozen scripts unchanged
(except tooling README redirect); source workspace also opens Android only.
External `/mnt/d/dex-t022-evidence/source-retirement.json` records exact scope.
Independent selector3+perf19suites, Markdown/authority/workspace check, source
continuity and old redirects PASS. All22task IDs/states preserved exceptT-022 now
DONE; T-02120frozen app/test hashes unchanged. Final plan moved to completed.
Final mapping and review results retained externally in `migration-map-final.json`
and `final-review.json`; Git remains uncommitted/unpushed.

Final post-handoff audit:139Markdown/454local links PASS;39retired entry-point
links PASS;22task-state entries preserve all states exceptT-022DONE;20T-021
source hashes PASS. Root independently verified all43host tests. Scoped Git
whitespace and moved selector shell syntax PASS. No remaining migration blocker.
