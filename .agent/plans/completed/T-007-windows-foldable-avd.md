> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-007 Windows foldable AVD ExecPlan

## Outcome and boundaries

Provision `ZFold3_API35` with Windows-native Android Emulator and ADB, stored at
`D:\Android\AVD`, using Android 15/API 35 Google APIs x86_64 and the closest
available 7.6-inch fold-in/outer-display profile. Test current Android APKs and
create reusable scripts under `scripts/android/`. This is not Samsung One UI or
physical Fold3 validation. Accessibility capabilities absent from the current
APK are reported, not fabricated. Wipe/reset is limited to this new AVD.

## Inputs and constraints

- Windows SDK `D:\Android\Sdk`; AVD home `D:\Android\AVD`.
- Use Windows executables from WSL; do not install a Linux emulator or change
  API level.
- Preserve unrelated dirty work in both repositories and do not commit.
- Device Owner provisioning requires a clean AVD and must be sequenced before
  main-app installation when exercised.

## Decision questions and evidence

1. Verify SDK/emulator/ADB versions and exact installed API 35 image.
2. Reuse a matching AVD or create `ZFold3_API35` from the closest foldable
   hardware profile; inspect `config.ini` rather than assuming dimensions.
3. Discover fold-state commands/capabilities from the running system before
   changing state.
4. Build/install/launch current APKs, capture UI/log/activity/accessibility/DPC
   evidence, and identify missing app capabilities truthfully.

## Milestones

1. Inspect environment and create task/plan.
2. Create/configure and boot the Windows AVD.
3. Confirm Android 15/API 35 and fold-state support.
4. Build, install, launch, and test current APKs in supported states.
5. Add reusable lifecycle/test scripts and README.
6. Run scripts, review artifacts, and update task/status with limitations.

## Validation

- Windows ADB reports one intended emulator in `device` state and
  `sys.boot_completed=1`.
- Release is 15 and SDK is 35.
- PowerShell scripts are parseable and status/start behavior is idempotent.
- `test-android.sh` passes shell syntax, builds/discovers the APK, and produces
  the required `test-artifacts/` tree and report.
- `git diff --check` and generated/secret review pass.

## Risks and escalation

- Hypervisor/GPU or Windows process-launch issues may prevent boot; diagnose
  with emulator output and ADB before changing graphics mode.
- Emulator fold states and dimensions may differ from Fold3 hardware/One UI.
- Reset destroys only this AVD's data; never target another AVD or system image.
- User-mediated Accessibility enablement is not bypassed.

## Progress and findings

- [x] ADB 37.0.1 and Emulator 37.1.11 detected on Windows.
- [x] API 35 Google APIs x86_64 system image detected.
- [x] No existing AVD; closest catalog profile is `7.6in Foldable`.
- [x] AVD creation/configuration and boot.
- [x] App/device-state testing and artifacts.
- [x] Reusable scripts and final validation.

Completed 2026-10-01. `ZFold3_API35` booted with Windows Emulator 37.1.11 as
Android 15/API 35. Start/status/stop/reset scripts and `test-android.sh` were
executed; PowerShell parse checks, Bash syntax, and `git diff --check` passed.
The final reset left the AVD clean, booted, and without owners or test packages.

The profile reports CLOSED/HALF_OPENED/OPENED, but guest metrics remained
1768x2208 and folded/unfolded images were identical. The app launched without
crash/ANR; its emergency button was clipped. No AccessibilityService exists.
Device Owner and permission grants passed before reset, while manual and boot
voice startup failed across the separate DPC/main-app package boundary. Detailed
evidence remains in ignored local `test-artifacts/`.

## Decision and requirement updates

No product contract change is expected. Record durable emulator limitations in
the test report and canonical status; physical Fold3 acceptance remains open.
