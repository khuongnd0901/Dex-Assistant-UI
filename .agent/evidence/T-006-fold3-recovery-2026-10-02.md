> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-006 Fold3 recovery repair — 2026-10-02

This report supplements the historical prerelease report; it does not overwrite
baseline measurements. Android implementation is `a5b7073`, locally built with
JDK17/API35. Current repair branch is `fix/t006-fold3-recovery` in both repos.

| Check | Result | Evidence/limit |
| --- | --- | --- |
| Android test/lint/assemble | PASS | 150 tasks; 23 distinct tests / 46 debug/release executions |
| Provisioning mock regressions | PASS | 8 methods / 23 scenarios after reboot correction; shell syntax/diff checks |
| Original key recovery | PASS | Actions run36897677667; only CMS-encrypted artifact exported; deleted afterward |
| Local APK signing | PASS | Both apksigner-verified certificate SHA256 `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03` |
| Encrypted GitHub key backup | PASS | Android commit7953727; passphrase separate Actions secret; local roundtrip cmp exact |
| Wi-Fi install both repaired APKs | PASS | Non-streaming native ADB; script advanced past installs to readiness/reboot |
| Pre-reboot owner/role/unlock/RECORD_AUDIO/binding | PASS | Strict script readiness completed before reboot request |
| Provisioning whole script | PASS after correction | Initial attempt exit1 preserved. Corrected rerun exit0; stable device identity across changed Wi-Fi port, changed boot ID and completed boot, strict readiness before/after reboot |
| Post-reboot runtime | PASS | User0 RUNNING_UNLOCKED; role/bound service, READY/LISTENING, real probe passed, mic FGS1001/type0x80 |
| Live Voice UI + emergency stop/start | PASS | Visible Voice row changed to STOPPED / Stopped by user, then LISTENING / successful probe without Refresh; stop removed microphone FGS, start restored type0x80 |
| Repaired notification pause/resume/stop | PASS | Owner operated all three. PAUSED / Paused by user stayed stable; resume LISTENING with successful probe; stop STOPPED and microphone service/FGS absent |
| Main-app-only permission reconciliation | PASS | Only app reinstalled; DPC lastPolicyApply1790874975468 → 1790875077788; READY/LISTENING and probe passed without Activity launch |
| CI test/lint/build/signature verification | PASS | Run36898323611 completed success at30df640; explicit final signing and expected certificate assertions passed. Original failed run remains historical |
| V1/backend/reconnect/Keystore/actions/latency | BLOCKED | Owner has no test backend |
| Real KWS/STT/TTS/continuous wake | OPEN | Runtime/model/pipeline not implemented in this repair slice |

Sanitized logs: ignored `test-artifacts/fold3-recovery-2026-10-02/`.
No factory reset, credential/audio dump, or backend protocol change.

Installed base APK SHA256 exactly matches locally signed files:

- App: `705c0754e48c67e3c52770c36bc3fb39eb7a604e503c65f7f36fa0d901e24d9c`.
- DPC: `b228845c948db966a84599f497630cadabc177a59f619855c181278eef091ca3`.

`after-first-reboot.sanitized.txt`, `installed-hashes.sanitized.txt` and
`main-app-update.sanitized.txt` contain these measurements. Corrected script rerun completed exit0; `provision-retest.sanitized.txt` and
`after-retest-reboot.sanitized.txt` record results. UI excerpts intentionally
include only allowlisted health rows, never credential fields.

The Device Owner row briefly showed `not verified` during administration
startup/rebinding despite shell owner proof; a later UI snapshot showed ready.
Live Voice health acceptance is separate from that asynchronous owner display.

Notification evidence: `notification-paused.sanitized.txt`,
`notification-resumed.sanitized.txt`, `notification-stopped.sanitized.txt`.
Final microphone state is STOPPED. Negative shell tests on the repaired APKs
also PASS: DPC service requires signature EXECUTE_DEVICE_ACTION, private
microphone service is non-exported (`negative-boundaries.sanitized.txt`).
A separate wrong-signed bindService harness remains untested.

UI capture after notification actions could not find the Voice row in the
active UI hierarchy, so no live notification-to-UI observation is inferred.
The earlier visible emergency stop/start Voice-row check passed without Refresh;
notification state and FGS acceptance uses direct allowlisted device health.

The downloaded CI bundle independently passed SHA256SUMS for both APKs and
local apksigner verification with the expected9ddb certificate. Device tests
used locally built/re-signed APKs from sourcea5b7073, not the later CI APK files.
CI output APKs are retained under ignored `ci-apks/`; no release was published.
