> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-017 expanded corpus — 2026-10-10

State: IN_PROGRESS; owner-ended seven-take batch measured and restoration verified.

Owner requested continuation and explicitly confirmed readiness to record additional local audio/transcripts. Android benchmark worktree `/home/kunrg/Dex-Assistant-UI-T017` stays isolated from main; production STT remains Google.

Expanded opt-in test-only collector has200 public prompts, pinned manifest, resumable confirmed takes, no-overwrite/orphan protection and explicit actual simulated-noise attestation. Reviewed reference remains manually entered after playback; prompt is not ground truth. No provider upload or action dispatch.

Host: JDK17 `test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1` PASS201 tasks in5m27s. App161 tests per variant, zero failures/one expected skip; five resume/path/orphan/no-overwrite tests per variant PASS. Python15PASS. First build overlapped edits and failed; final-source full validation PASS. Test helper source set is test/androidTest only.

Fold3 SM-F926B/API35 at rediscovered ADB endpoint; DeX display2 1920x1080. Original app/test APK, policy/history/health, role/DPC/Accessibility baseline retained privately. Installed target dex API compatibility and matching certificate verified. Only matching-cert testAPK installed. Public voice control LISTENING→PAUSED PASS1/1. Full200 no-mic UI smoke PASS1/1, microphoneOpened=false. Owner-operated session used --no-restart and explicit taps. Collection PASS1/1 in205.636s, ended by Cancel with seven confirmed human recordings, all system group. Partial corpus validator PASS; coverageComplete=false. Physical same-session resume/no-mic smoke PASS1/1 retained all seven, microphoneOpened=false.

Private directory `/home/kunrg/.local/share/dex-benchmarks/T017-expanded-20261010`, session `session-1791590650000`. No private recordings/transcripts/logs/model weights/APKs committed. Original testAPK SHA256 `eb7a5b4df02ea9177dd25bcf98e063498cd6e56ab3fdf1eff8d537be07d5dee8` restored and verified exact. Target app SHA256 unchanged; voice LISTENING, policy/history/Assistant role byte-identical; Accessibility enabled and bound, no crashed service. DPC policy/admin unchanged; dump differs only LockGuard counter/timing statistics.

## Local WAV results

Seven new utterances, five measured repeats after two warmups per thread profile:105 measurements total. Native WAV test PASS1/1 in31.093s, pinned runtime1.13.8/model revision83e140db6d23fbb8480fd5fb868f74ab80e7092c with verified hashes.

| Threads | Decode median ms | p95 ms | First-observed load ms | Max process PSS KB |
| --- | --- | --- | --- | --- |
| 1 | 120.48 | 317.66 | 2990.15 | 333632 |
| 2 | 84.64 | 214.58 | 3333.90 | 343067 |
| 4 | 90.39 | 229.48 | 3505.54 | 345243 |

All profiles WER27.273%, CER0%, exact normalized transcript4/7; no repeat disagreements. WER differences are word-spacing/token boundaries; CER explicitly excludes spaces. Do not interpret CER0 as exact transcript equality. First measured repeat scores accuracy;35 timing observations per profile repeat only seven distinct utterances. Filesystem/cache not cold-controlled; PSS is whole process. No live endpoint or Google comparison.

Dry-run parser suite PASS3/3: six scorable references, exact typed action6/6, one reference unscorable; zero opposition errors in this small batch. No routing/execution. Prior12 pilot remains separate, including its opposition error; new results do not erase it.

Expanded session has7/200 accepted (system7, all other groups0); do not add prior pilot without deduplication/mapping. Resume this same session for missing prompts. Audio/transcripts remain local/private. No production change, commit or push.

 Full200 coverage, cold load, live Google/Sherpa A/B, independent speech-end,100mic/KWS sessions, soak and distribution rights remain open. Do not mark DONE.
