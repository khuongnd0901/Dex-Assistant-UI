# Android current status

Updated 2026-10-23. Android is canonical under [D-037](DECISIONS.md#d-037--android-canonical-workspace-backend-retired).
Start at [AGENTS](../AGENTS.md), [TASKS](TASKS.md); open [workspace](../Dex-Assistant.code-workspace).
Backend development is retired; T-022 DONE, no backend checkout/runtime required.

Latest owner priority: **T-025 subscription migration IN_PROGRESS**. Continue existing
GitHub `feat/chatgpt-subscription-migration`, fetched at ba054d3; current worktree
`/home/khuongnd/worktrees/dex-subscription`. Original dirty main at
`/mnt/d/dex-assistant-ui` retained. Implementation commit `5f5bd25` pushed to the feature branch; remote SHA verified. No merge.

D-040 retires Gemini/Groq/API-key runtime, key editor/probes/dead settings. Command
and Chat use the branch's existing ChatGPT subscription provider directly; stored
legacy keys/preferences remain untouched. Explicit selection and account/model proofs,
local-first/privacy/confirmation/session guards remain. A legacy default requires
subscription setup; there is no provider/API-key fallback.

Final JDK17/Linux SDK test/lint/app+test APK PASS201tasks: app219tests/variant,
0failures/errors,1conditional skip; DPC5/variant PASS; lint0errors/32existing app warnings.
Windows API35 repository AVD,ADB5038,serial127.0.0.1:5555; in-place installs PASS.
Final scoped subscription lifecycle/settings/Keystore/SSE/component and local/private
command suites **13/13PASS,14.309s**. Obsolete LocalFirst UI helper corrected after
initial2failures; final batch passes. Whitespace and memory145Markdown/471links PASS.
[Current evidence/artifact hashes](evidence/T-025-subscription-only-2026-10-23.md),
[active plan](plans/active/T-025-chatgpt-subscription-migration.md).

No physical install/test or live-account/provider action this session. Last recorded
Fold3 trial app c89fa2b/original9ddb signing; earlier native OAuth/refresh/7models/
inference/tools/Chat/cancel/Chrome evidence is historical, not cleanup validation.
Next: coordinate original-signed
in-place Fold3 install and owner explicit selection through T-020. Natural voice/STT/
TTS/interrupt/confirmation,physical logout/reconnect,natural p95/matched PSS/soak open.
Draft PR2 remains unmerged; no release/DONE claim.

Prior unfinished priorities retained: T-021 → T-014 → T-017 → T-020 → T-018 →
T-006 → T-010 → T-011 → T-015 → T-016. Physical gates stay with T-020; cancelled
T-003/T-008/T-009/T-019 remain cancelled. T-017 corpus7/200 and expanded collector
recovery remain open. Prior T-021 performance and T-014 TTS checkpoints are linked
from TASKS; no natural/physical improvement inferred from current AVD components.
T-024 public MIT and T-022 canonical workspace remain DONE.

Windows target selector: `bash scripts/android/select-test-target.sh dex --emulator-only`.
Use its exact current ADB/PORT/SERIAL. [Tooling](../scripts/android/README.md),
[physical campaign](plans/active/T-020-consolidated-fold3-acceptance.md).
