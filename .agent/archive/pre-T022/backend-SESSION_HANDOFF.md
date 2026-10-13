> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Session handoff — 2026-10-05

Start with `.agent/STATUS.md`, then read the relevant task and linked active
ExecPlan. This file holds only resume constraints; dated evidence contains the
full test commands, results, and artifact hashes.

## First next step

T-009: isolate legacy Android local/offline and monitoring/reconnect tests
before physical execution. Older tests may clear owner history or alter
wireless ADB. Preserve owner history, credentials/keys, permissions, and ADB
state. Review these latest recorded reports first:

- [Java JAR/Gemini Fold3 integration](../../evidence/T-009-java-jar-backend-2026-10-04.md)
- [Quota and reconnect continuation](../../evidence/T-009-handoff-quota-2026-10-04.md)

## Resume boundaries

- The backend JAR, log, `/tmp` database/artifacts, ADB reverse, and Fold3
  endpoint reported on 2026-10-04 are historical. Rediscover and verify before
  reuse; do not start a second backend on an occupied port.
- Runtime backend tests used a public isolated test identity and external quota
  database. They did not provision production credentials or establish account
  billing/free-tier status.
- Keep the original signing identity, DPC/owner state, accessibility grant,
  owner history and protected preferences intact. No reset or credential export.
- Natural spoken wake/command accuracy, cue audibility, 100-session/resource
  gates, and broader T-006/T-010 acceptance remain open.
- T-008 still lacks its CI/release workflow and live release verification.
- `/mnt/d/DeX-Assistant` is the canonical repository;
  `/mnt/d/dex-assistant-ui` is the Android repository. Read its `AGENTS.md` and
  status before cross-repository work.

No device/backend validation was performed during this handoff refresh.
