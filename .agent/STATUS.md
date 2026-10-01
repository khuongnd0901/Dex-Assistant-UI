# Android repository status

Last synchronized: 2026-10-02 (Fold3 recovery and session handoff)

## Canonical project memory

Product-wide status, tasks, decisions, requirements, and protocol are owned by
the backend repository at `/home/kunrg/DeX-Assistant` on the Fold3 test host. Start cross-repository work
from its `.agent/STATUS.md`. This file is a concise Android handoff, not a second
canonical task board.

## Current state

- The repository contains the Compose client (`com.khuongnd.dexassistant.ui`)
  and separate DPC (`com.khuongnd.dexassistant.dpc`).
- Current build configuration: compile/target API 35, min API 30, JDK 17.
- The client provisions a Keystore-wrapped token, enforces WSS except loopback,
  implements the V1 WebSocket/reconnect flow, launches apps, delegates Wi-Fi to
  the signature-protected DPC, and emits non-sensitive latency markers.
- The DPC verifies Device Owner status and observes Wi-Fi state before success.
- `test`, `lint`, and `assembleDebug` pass for both modules with JDK 17/API 35.
- T-006 now has app-scoped DPC permission enumeration with verified grant
  results, typed permission/capability IPC, and Device Owner
  boot/self-update recovery. Main-app updates now reconcile through the system-bound
  VoiceInteractionService and signature-protected permission-profile IPC; manifest
  cross-package package-change broadcasts were skipped by One UI and removed. The DPC target is fixed to
  `com.khuongnd.dexassistant.ui`; it does not receive backend credentials.
- The main APK is a qualified default-assistant candidate with voice interaction,
  session, and recognition services. Its private microphone FGS is started by
  the main app only after role/unlock/permission/policy readiness. Its persistent
  notification supports pause/resume, stop, and opening administration; health
  is persisted without command text or credentials.
- DPC boot/update recovery only reapplies permissions and records health; it does
  not start the cross-package microphone FGS. The daily chat controls were removed from the Activity. The remaining
  administration surface provisions/connects, shows basic owner/voice/backend
  health, applies the permission profile, and starts/stops voice service.
- Voice lifecycle/readiness transitions and permission result classification have unit
  coverage. Protocol V1 was not changed.
- Current branch is `main`; other branches were merged and deleted as requested.
  Android main is `b25269a`; backend main is `f590e91`.

## Relevant canonical tasks

- Backend `T-004` is `IN_PROGRESS`: owner decisions are accepted; actual Fold3
  firmware and Device Owner evidence remain.
- Backend `T-003` is `IN_PROGRESS`: automated implementation is complete, while
  on-device provisioning, actions, and latency remain unverified.
- Backend `T-005` establishes the main/backend/Android agent workflow.

## Blockers

- Physical Fold3 Android 15/API 35 passed Device Owner, shell IPC rejection,
  five adb reboot cycles, microphone probe/FGS, and notification controls in
  canonical `.agent/evidence/T-006-fold3-2026-10-01.md`. These are probe checks,
  not continuous voice acceptance. Startup reconciliation/UI fixes in this
  repair branch pass local test/lint/assemble; matching-signed APK physical
  update/reboot/probe/notification/live Voice UI acceptance now passes.
- sherpa-onnx 1.13.8 and the official GigaSpeech 3.3M model were not added: the
  exact model archive was not available locally and no unverified/placeholder
  asset was fabricated. Until those assets and checksums are installed, the
  service performs only a real microphone probe and reports wake runtime pending.
- STT, TTS, real wake detection, WebSocket voice handoff, device-credential
  gating, special-access Settings links, and full diagnostics remain T-006 work.
- API 35 testing validates the D-011 replacement for that failed DPC-start path:
  assistant role/binding, permission grant, self-started microphone FGS, real
  `AudioRecord` probe, and `LISTENING` persist across cold reboot. Fold3/One UI
  now has probe evidence, and no AccessibilityService is declared.

## Next action

Continue T-006 with offline KWS/STT/TTS, final-text handoff, credential gate,
special-access Settings links and diagnostics. Obtain a dedicated test backend
before V1/reconnect/actions/Keystore/latency acceptance. Keep T-006 IN_PROGRESS.
No factory reset is authorized. Fold3 last seen on ADB port `32881`; rediscover
the Wi-Fi debug port before use. Voice service ended STOPPED.

Committed repairs set up Android SDK tools in both workflows, restore the Gradle
wrapper executable bit, and bound permission IPC waits to ten seconds and startup
attempts to three. Probe completion is invalidated by pause/stop, so a late worker
cannot restore LISTENING after a user stop. Health observation attaches only
while the administration Activity is started.

## Validation

- 2026-10-01 recovery fixes: JDK 17 / API 35 `./gradlew --no-daemon test
  lint assembleDebug` passed (`BUILD SUCCESSFUL`, 150 tasks). App has 18 tests
  and DPC 5, each passed in debug and release (46 executions; no failures).
  This build includes binding-death/null-binding recovery and lifecycle/probe
  fixes. JVM coordinator tests do not establish Android callback delivery.
- Local debug signing identity is unrelated to the installed Fold3 identity;
  generated local APKs are compile evidence only and must not be installed on it.

- Passed: API 35 AVD provisioning script before/after cold reboot; role holder
  and bound voice component persisted, `RECORD_AUDIO` was policy-granted, health
  became `READY`/`LISTENING`, microphone FGS type was `0x80`, and the real probe
  passed without `ForegroundServiceStartNotAllowedException`.

- Passed: `env JAVA_HOME=/home/khuongnd/.local/opt/jdk17
  ANDROID_HOME=/home/khuongnd/Android/Sdk ./gradlew test lint assembleDebug`
  (`BUILD SUCCESSFUL`, 150 tasks).
- Passed: `git diff --check`.
- APK SHA-256: app
  `d6ada73dca07a823e2833a61908244e891b6a8a6dc64896564052c84ccee07a9`;
  DPC `17377af836a71fdafc184f33441f6fc0560ef07fadd6440f9fb19ea7074fa76b`.
- GitHub prerelease `fold3-device-test-2026-10-01` publishes both verified
  debug APKs for physical Fold3 testing; generated binaries remain outside Git.
- `apksigner verify --print-certs` passed for both APKs; both use certificate
  SHA-256 `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
- Windows `ZFold3_API35` (Google APIs x86_64) booted as Android 15/API 35;
  both APKs installed and MainActivity cold-launched without crash/ANR. The
  emulator reported CLOSED/HALF_OPENED/OPENED but kept 1768x2208 guest metrics.
  Device Owner and runtime permission checks passed before the final clean reset.

## Resume commands

- Build/validate: `env JAVA_HOME=/home/khuongnd/.local/opt/jdk17
  ANDROID_HOME=/home/khuongnd/Android/Sdk ./gradlew test lint assembleDebug`.
- Start the retained AVD from the backend repo with
  `powershell.exe -ExecutionPolicy Bypass -File scripts/android/start-zfold3.ps1`.
- Provision/verify using the backend script documented in its
  `scripts/android/README.md`. The AVD is currently stopped and retains the
  successful Device Owner/default-assistant state; do not reset casually.

## Authorized local signing recovery — 2026-10-02

The owner authorized recovery of DEVICE_TEST_KEYSTORE_BASE64 for local signing.
Workflow run 36897677667 exported only CMS-encrypted signing material, encrypted
for a locally generated recipient whose private key never left this host.
The recovered keystore certificate and both locally re-signed app/DPC APKs were
verified with SHA-256 fingerprint
`9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`,
matching the installed prerelease signing identity. Signing material remains
outside the repositories in a host-local directory with restrictive permissions.
Temporary recovery workflow logic was removed. Physical acceptance of these
re-signed recovery APKs now passes the foundation recovery slice; full wake
and backend acceptance remains open.

## Physical recovery repair validation — 2026-10-02

Both locally re-signed APK hashes matched installed Fold3 base APKs. Corrected
backend provisioning script passed before/after a new boot and recovered the
same device across a changed Wi-Fi ADB port. Main-app-only reinstall advanced
DPC policy timestamp1790874975468 → 1790875077788 without replacing DPC or
launching Activity. READY/LISTENING, short AudioRecord probe and mic FGS type0x80
passed. Visible Voice UI emergency stop/start updated without Refresh.
Owner-operated notification pause/resume/stop and negative shell service
permission/export checks passed; final microphone state STOPPED. Notification
UI hierarchy capture was unavailable, so direct notification-to-UI live updates
are not separately claimed. Full report is canonical backend
`.agent/evidence/T-006-fold3-recovery-2026-10-02.md`.

Workflow30df640 explicitly signs final APKs after checking the restored key;
run36898323611 passed test/lint/build/expected certificate checks. Encrypted
keystore backup is stored in `signing/`, passphrase in separate Actions secret.
No factory reset, backend protocol change, or full voice/backend acceptance.


## Session handoff — 2026-10-02

- Android main `b25269af661b9cb710aacd40d0fae123f6e78dd9`; backend main
  `f590e913f713c2b9e7f9ad7287355870e9ea9b9d`. Only `main` remains locally
  and remotely; both trees were clean at handoff.
- Before CI optimization, local `test lint assembleDebug` passed150 tasks;
  23 distinct tests ran in both variants (46 executions). CI run36898323611
  passed explicit signing and expected certificate verification; no release was
  published.
- CI speed update commit `cda699f`: both workflows now use
  `testDebugUnitTest lint assembleDebug --build-cache`. Android CI also ignores
  docs-only pushes/PRs: Markdown files, `docs/**`, and `.agent/**`; code/config
  changes still trigger it. This runs all 23 shared
  unit tests once on the Debug variant, which matches the only APK artifact and
  retains `run-as` access for device health collection. It avoids the duplicate
  Release test compilation; no Release APK was previously produced. Local warm
  rerun passed in34s (100/102 tasks up-to-date). GitHub CI run36900750938 passed
  the Gradle step in4m34s vs5m19s baseline (45s faster, one measurement); 11
  tasks reported FROM-CACHE. `lint` and `assembleDebug` remain enabled.
- Signing certificate SHA-256:
  `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
  Local keystore: `/home/kunrg/.local/share/dex-signing/device-test.keystore`.
  Encrypted tracked backup `signing/device-test.keystore.enc`; passphrase in
  Actions secret `DEVICE_TEST_BACKUP_PASSPHRASE`. Never commit decrypted material.
- Physical repair checks pass: app-only permission reconciliation; provisioning
  across reboot and changed ADB port; probe/FGS; UI emergency stop/start;
  notification pause/resume/stop; shell permission/export boundaries. Final
  voice state STOPPED. Detailed limits and evidence are in backend
  `.agent/evidence/T-006-fold3-recovery-2026-10-02.md`.
- Full wake recognition, STT/TTS, credential gate, diagnostics and backend
  dependent acceptance are still open. Do not factory reset without explicit
  owner confirmation.
