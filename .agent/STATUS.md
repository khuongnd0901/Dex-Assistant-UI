# Android current status

Updated 2026-10-13. This repository is the canonical development/test workspace
([D-037](DECISIONS.md#d-037--android-canonical-workspace-backend-retired)); backend
development is retired. Start at [AGENTS](../AGENTS.md), then [TASKS](TASKS.md).
Open [Android workspace](../Dex-Assistant.code-workspace). No backend checkout required.

[T-022 migration](plans/completed/T-022-android-canonical-workspace.md) DONE.
132files mapped and verified; canonical docs/memory plus local test tools moved;
old backend entry points redirect here.43host tests PASS; local links/Android-only
workspace checked, actual Windows API35 AVD selector PASS. T-02120frozen source
hashes unchanged. [Migration evidence](evidence/T-022-android-canonical-workspace-2026-10-13.md)
records snapshots/collision recovery and starter terminal-return retry limitation.
No app/runtime/build/install/physical/provider/cloud changes or commit/push.

Current planning priority: [T-021](plans/active/T-021-android-response-performance-ram.md) remains IN_PROGRESS.
Windows M0–M3 source/test/lint/APKs passed; safe AVD31PASS/4SKIP/0FAIL. Three A/B
rounds show synthetic Command p9543.66–52.93% lower and allocation~46% lower;
wake allocation80–84% lower, native timing INCONCLUSIVE. No natural voice,
Fold3/live-provider/PSS gains accepted. Exact hashes/commands/limits are in
[T-021 evidence](evidence/T-021-windows-response-performance-2026-10-13.md).

All actual Z Fold3 work remains deferred to [T-020](plans/active/T-020-consolidated-fold3-acceptance.md):
natural mic/wake/USB/STT/TTS/rearm/Chat interruption, Samsung DeX/pixels/targets,
real provider/radio,30natural samples per route,100sessions and8hsoak. T-006/
T-010/T-011/T-014/T-015/T-016/T-017/T-018 stay IN_PROGRESS; T-003/T-008/T-009/T-019
stay CANCELLED. Accepted thresholds and owner signing/data/settings/roles/grants remain.

Next Android readiness: echo-safe spoken End Chat during TTS (currently unsupported),
recover expanded200 ASR collector before resuming7/200 (restored12-prompt pilot
cannot replace it). Production STT Google; Gemini-only physical scope; Sherpa
rights/recommendation still open. Scoped window close/minimize/switch and
notification reading remain unavailable where documented.

Local simulator entry: `bash scripts/android/select-test-target.sh dex --emulator-only`.
[Tooling guide](../scripts/android/README.md). Detailed prior handoffs:
[Android](archive/pre-T022/ANDROID_STATUS_2026-10-13.md),
[former canonical](archive/pre-T022/backend-STATUS_2026-10-13.md).

Local sync 2026-10-13: pulled Android `5d06ef4` and restored the four pre-existing
T-019 source/test/guide changes. Historical local T-019 handoff is preserved in
[Android archive](archive/pre-T022/ANDROID_STATUS_2026-10-13.md). No device work or
new T-019 development. Merged-source JDK17/Linux SDK offline test, lint, debug
app/test APK assembly PASS; app200tests per variant,0failures/errors,1skip.
Validation log: `/tmp/dex-ui-pull-validation-20261013.log`. Owner authorized commit/push of this retained-source snapshot; pre-pull stash
and patch backup retained. Physical acceptance remains open under T-020.
