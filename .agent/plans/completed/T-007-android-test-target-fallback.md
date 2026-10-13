> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-007 Android test target fallback

## Outcome and boundaries

Make the existing Windows API 35 AVDs usable for Android test work whenever a
real Galaxy Z Fold3 is absent. Prefer a connected Fold3; otherwise select a
desktop/freeform AVD for DeX layout tests or the foldable AVD for hinge/layout
tests. Do not treat AOSP emulator results as Samsung DeX or physical microphone
acceptance. The original T-007 provisioning plan remains completed.

## Inputs and constraints

Windows SDK is `D:\Android\Sdk`; `sdkmanager.bat` installs SDK packages,
`platform-tools\adb.exe` selects devices, and `emulator\emulator.exe` runs an
AVD. Existing `DeX_Command_Test_API35` is 1920×1080/160 dpi with freeform;
`ZFold3_API35` is a 1768×2208/420 dpi generic 7.6-inch foldable. Both use
Android 15/API 35 Google APIs x86_64. Gradle in WSL needs the Linux SDK and
JDK17. Preserve existing phone and AVD data and all unrelated worktree edits.

## Decision questions and evidence

- Detect an actual connected Fold3 by model and API, not by a stale network
  endpoint. Require exact serial targeting.
- Use the desktop AVD for DeX-like window regression and the foldable AVD for
  handset/hinge regression; record that neither has Samsung display categories.
- Verify SDK paths and AVD configuration from disk, then boot/select an AVD and
  read its actual API, display metrics, freeform state and ADB identity.

## Milestones

1. Update a target selector and test documentation in the canonical repository.
2. Add the same entry point to the Android agent guide and current status.
3. Validate Bash syntax/selection and boot the desktop AVD without resetting
   either AVD or changing Fold3 state.
4. Record measured results and any blocked physical or SDK-dependent gates in
   T-007/STATUS; close this follow-up only when selection is verified.

## Validation

`bash -n scripts/android/select-test-target.sh`, direct selector output,
Windows ADB serial/AVD/API/metrics readback, and `git diff --check` in both
repositories. Do not run owner-state-sensitive instrumentation just to verify
target selection.

## Risks and escalation

The emulator does not emulate One UI, Samsung DeX, physical USB mouse input,
OEM background policy or real microphones. Keep device acceptance open. Do not
wipe or overwrite a phone/AVD when a package signature conflicts.

## Progress and findings

- [x] Inspected SDK tools and both AVD profiles.
- [x] Added selector and cross-repository documentation.
- [x] Desktop AVD boot/readback and selector behavior confirmed.
- [x] Final validation and status recorded.

Completed 2026-10-06. With no connected Fold3, the selector returned
`DeX_Command_Test_API35`/`emulator-5554` through Windows ADB port 5038.
Readback: Android API35, physical size1920×1080, density160, freeform flag1.
`start-dex-test.ps1` reported boot complete; Bash syntax for selector/runner,
PowerShell parse checks for modified lifecycle scripts and both-repo diff checks
passed. A Linux ADB direct connection to Windows emulator `127.0.0.1:5555`
was refused on this WSL host, while Windows `adb.exe -P 5038` found the AVD.
The generic foldable AVD profile was inspected on disk (1768×2208/420dpi) and
was previously booted under original T-007; its revised port path was not
booted in this follow-up. No app tests or physical Fold3 acceptance ran.

## Decision and requirement updates

No product or protocol change. Update only test-target guidance and T-007
evidence/status; leave device-specific acceptance criteria intact.
