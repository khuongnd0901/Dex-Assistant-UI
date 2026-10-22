# Android current status

Updated 2026-10-22. Android is canonical under [D-037](DECISIONS.md#d-037--android-canonical-workspace-backend-retired).
Start at [AGENTS](../AGENTS.md), [TASKS](TASKS.md); use [workspace](../Dex-Assistant.code-workspace).
Backend development is retired; T-022 DONE. No backend checkout/runtime needed.

Latest owner priority: T-025 ChatGPT Subscription migration IN_PROGRESS.
Native independent OAuth/Keystore/session/model/Responses/SSE and provider/settings
implementation added; default selection and device commands remain code-gated.
[Plan](plans/active/T-025-chatgpt-subscription-migration.md),
[evidence/matrix](evidence/T-025-chatgpt-subscription-migration.md).
Public MIT eligibility verified independently; target consent/inference/tools not verified.
Host JDK17 test/lint/app+test APK PASS201tasks; app226tests/variant(1skip),
DPC5/variant;0lint errors; whitespace PASS. Original9ddb signed candidate verified.
Windows selector lacks Windows ADB; Linux5037 sees Fold3. Latest owner steering:
push first, then build/test real Fold3. Offline physical run is next; no install or
account/device acceptance claimed yet. Owner sign-in and physical/voice/tool/metrics
gates remain required before merge. No migration DONE/default claim.
Prior unfinished priorities below are retained; unrelated dirty icon/collector files
are excluded from migration commits. No reference POC/backend changes.

Prior owner request COMPLETE: T-024 public repository + MIT. License/README
commit59fc4c9 pushed to main; GitHub authenticated and anonymous metadata verify
PUBLIC/MIT; anonymous LICENSE matches local bytes and standard MIT template.
[Evidence](evidence/T-024-public-mit-2026-10-22.md). No app/runtime/device change.
Prior unfinished priorities and local app/collector changes remain retained.

Owner authorized execution and per-task commit/push in this order:
T-021 → T-014 → T-017 → T-020 → T-018 → T-006 → T-010 → T-011 → T-015 → T-016.
Fold3 acceptance resumed through T-020; subsequent instruction skips speaking
for now. Safe automated work proceeds; natural/manual gates remain open.
T-003/T-008/T-009/T-019 remain CANCELLED; no prototype acceptance/revival.

T-021 remains IN_PROGRESS. M0–M3 Windows implementation already verified:
synthetic Command p95 43.66–52.93% lower, allocation about46% lower; wake
allocation80–84% lower, native speed INCONCLUSIVE.
[Windows evidence](evidence/T-021-windows-response-performance-2026-10-13.md).
New physical SM-F926B/API35/DeXdisplay2 checkpoint: host201-task checks PASS,
26perf-tool tests PASS, benchmark2/2 and safe metric/fault17/17 PASS.
Offline candidate Command p95 12.174ms; no matching physical baseline or natural
improvement claim. Read-only Fold3 resource collector added; PSS snapshots have
different states and cannot prove savings.
[Physical checkpoint](evidence/T-021-fold3-automated-2026-10-13.md).

Physical candidate app source5472dff installed in place with original9ddb
certificate; original app/test privately retained. Provider keys/settings,
voice policy, roles/grants/UID/data inode unchanged. History ciphertext changed
after startup; before/after plaintext equality unmeasured. No live provider
calls, Wi-Fi toggle, data reset, permission or role changes.

T-014 chunk checkpoint verified: full201-task checks PASS, app204tests/variant;
physical offline Vietnamese chunk/cancel/same-ID fixture1/1 PASS,6.378s.
[Evidence](evidence/T-014-tts-chunks-2026-10-13.md). Parent remains IN_PROGRESS;
echo-safe spoken exit unsupported. Current physical app is this TTS candidate,
installed hash verified with original9ddb signing. Credential/settings/policy,
roles/grants/data inode retained. Streaming install lost wireless link; owner
reconnected and non-streaming retry succeeded. Physical ADB port5037, serial
rediscovered/reverified; use current devices list rather than historical port.

Next: T-017 expanded200 collector recovery: original dirty worktree `/home/kunrg/Dex-Assistant-UI-T017` found,
not yet migrated/validated. Retained natural corpus remains7/200.
Production STT Google, current provider acceptance Gemini-only; Sherpa rights
and recommendation open. Preserve accepted thresholds/signing/data/settings.

T-020 and all ten active tasks remain IN_PROGRESS. Natural wake/mic/USB/STT/TTS,
Chat interruption, Samsung targets/pixels,≥30samples per active route/candidate,
100natural sessions and8hsoak cannot be closed from offline automation.
Local Windows AVD selector: `bash scripts/android/select-test-target.sh dex --emulator-only`.
This Linux host has no `/mnt/d`; physical ADB currently uses5037.
[Tooling guide](../scripts/android/README.md), [physical plan](plans/active/T-020-consolidated-fold3-acceptance.md).
