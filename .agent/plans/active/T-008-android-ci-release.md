> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-008 Android CI and device-test release automation

## Cancellation — 2026-10-10

Owner explicitly cancelled T-008. Remaining CI/release acceptance is withdrawn;
existing workflows and historical evidence are retained. No release dispatch or
signing-secret changes are scheduled. Unchecked gates below remain unverified,
not completed.

## Outcome and boundaries

Add GitHub Actions to the Android repository so routine changes are validated
and an authorized manual run can publish the app/DPC debug APK pair for physical
device testing. This task automates test artifacts only. Production signing,
Play distribution, backend deployment, and protocol changes are excluded.

## Inputs and constraints

- Canonical task state lives in the backend repository; workflow code lives in
  `/mnt/d/dex-assistant-ui`.
- Android uses JDK 17, compile/target API 35, min API 30, and Gradle Wrapper 8.9.
- App/DPC signature equality is a functional security boundary for IPC.
- Existing device-test APKs use certificate SHA-256
  `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
- Signing keys and encoded keystores must never enter Git or workflow logs.

## Decision questions and evidence

1. How is the test signing identity preserved across ephemeral runners?
   Store the existing test keystore as an encrypted repository Actions secret,
   restore it only for trusted builds, and fail manual releases if unavailable.
   Acceptance: released APKs both match the recorded certificate fingerprint.
2. How are release writes constrained?
   Keep default workflow permissions read-only and grant `contents: write` only
   to the manual release job. Acceptance: PR/push jobs cannot create releases.
3. How is an artifact traced?
   Release notes and checksum metadata record tag, commit SHA, certificate, and
   test-only status. Acceptance: each asset can be mapped to one workflow run.

Primary references consulted 2026-10-01: GitHub workflow syntax and permission
documentation, `GITHUB_TOKEN` authentication guidance, workflow artifact
documentation, and repository Actions secret REST API documentation.

## Milestones

1. Add CI and manual release workflows plus concise maintainer documentation.
2. Configure the encrypted repository test-keystore secret without exposing it.
3. Validate locally, push both repositories, and observe a successful CI run.
4. Dispatch a device-test release and verify assets, checksums, and certificates.

## Validation

- Parse workflow YAML and inspect action versions/permissions/triggers.
- Run `./gradlew test lint assembleDebug` locally.
- Run `git diff --check` and confirm no keystore/generated APK is tracked.
- Inspect both APK certificates with `apksigner verify --print-certs`.
- Verify GitHub Actions run conclusion, release target SHA, asset list, sizes,
  checksums, and prerelease flag through GitHub API.

## Risks and escalation

- The current test key is a debug identity and is unsuitable for production.
- Replacing or losing the key prevents in-place upgrades and breaks signature
  IPC; a Device Owner installation may then require reprovisioning.
- Fork pull requests do not receive secrets; they may validate with an ephemeral
  debug key but can never publish a release.
- Stop and request owner input before introducing a production key, external
  distribution, or automatic release on untrusted events.

## Progress and findings

- [x] Task and ExecPlan created.
- [x] CI/manual release workflows exist in the Android checkout; historical
  signing repair/publication is recorded in T-006 recovery evidence.
- [x] Historical signing material configuration demonstrated: encrypted key
  backup with a separate Actions passphrase secret; successful signing run.
  Current release workflow requires `DEVICE_TEST_KEYSTORE_BASE64`; present-day
  availability of this specific secret has not been reverified.
- [x] Historical CI/signing run36898323611 at30df640 passed; downloaded APK
  checksums and expected app/DPC certificate were independently verified.
- [ ] Reverify selected current commit through CI and the manual release build,
  including release-specific secret availability and failure when absent.
- [ ] Live automated prerelease assets, target SHA, checksums, certificate,
  permissions and prerelease flag verified. The historical downloaded CI bundle
  was not a published release; the earlier manually delivered prerelease does
  not satisfy this automation gate.

Reconciled 2026-10-05 against Android `.github/workflows/android-ci.yml`,
`.github/workflows/device-test-release.yml` and
`../../evidence/T-006-fold3-recovery-2026-10-02.md`. No live GitHub run,
secret change or release publication performed during reconciliation.
T-008 was IN_PROGRESS at that checkpoint; owner cancelled it on2026-10-10.

## Decision and requirement updates

Record the accepted test-release boundary in `.agent/DECISIONS.md` only if the
implemented workflow and live validation succeed. This earlier completion condition is superseded by owner cancellation.
