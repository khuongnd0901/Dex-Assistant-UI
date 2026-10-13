> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-017 real-voice pilot — 2026-10-08

Owner consented to local recording/transcripts and personally recorded/reviewed12 references on Fold3 SM-F926B/API35. Test-only collector used explicit Record/Stop/Playback/Confirm; production voice was temporarily PAUSED through its existing control and restored to LISTENING. No provider upload or command dispatch. Separate private session1791473782493:12 PCM16 mono16k WAVs,37.5s total, observed routeDeviceType11, nonsilenced capture. Coverage system5/dex_ui4/media2/maps1/multi_step0/noisy0;200-sample acceptance NOT met.

Pinned Sherpa30M int8 revision83e140db6d23fbb8480fd5fb868f74ab80e7092c, runtime1.13.8, verified manifests. Two warmups/profile;12 WAVs ×5 repeats ×3 thread profiles =180 measurements. WAV test PASS1/1 (30.527s). These are file decode timings, not live endpoint/action latency or Google A/B.

| Threads | Decode median ms | p95 ms | First observed load ms | Max process PSS KB |
| --- | --- | --- | --- | --- |
| 1 | 86.23 | 114.64 | 3181.52 | 317745 |
| 2 | 69.32 | 91.33 | 2497.47 | 320233 |
| 4 | 65.29 | 88.21 | 2548.43 | 321205 |

All profiles: WER5.263%, CER4.444%, exact transcript10/12, no repeat disagreement. NFC/casefold/punctuation normalization preserves accents/digits; first measured repeat for accuracy. Filesystem/cache not cold-controlled. PSS is whole app process, not isolated model memory.

Installed registry + LocalCommandEngine parser-only scoring PASS3/3 including2 fixture tests. No routing/execution. One reference unparseable with foregroundAppId=null, excluded:11 scorable; exact command10/11, exact typed action9/11, exact parameters9/10 conditioned on same command, wrong opposition1/11. The opposition error is material despite low WER; no production selection recommendation from this pilot.

Validation: Python12PASS; focused Android testAPK/lint58tasks PASS; PCM fixtures2/2, no-mic UI smoke1/1, collector1/1 (207.43s,12 accepted), coordinator pause/resume1/1 each. Original testAPK restored SHA256 eb7a5b4df02ea9177dd25bcf98e063498cd6e56ab3fdf1eff8d537be07d5dee8. Policy and command_history_v1 byte-identical to immediately-before-control snapshots. PID15795, production lastUpdateTime2026-10-08 21:22:17; Assistant role and enabled DeX Accessibility preserved. No production APK/source changes. Natural wake recovery/soak not tested.

Owner-reported immediate USB feedback investigated separately: installed STT uses Google SpeechRecognizer provider com.google.android.tts; local Sherpa is benchmark-only. Several logged epochs show ready then manual-submit EMPTY within0.2–0.3s. Duplicate physical invocation is a hypothesis pending key-event correlation; T-006 follow-up, no fix claimed.

Private audio/references/raw transcripts/logs: /home/kunrg/.local/share/dex-benchmarks/T017-real-20261008/ (directory700/files600), plus app-private device session. Nothing uploaded/committed. M2 partial pilot;200 coverage/noisy corpus, controlled cold-load, live Google/Sherpa A/B, independent speech-end timing,100 mic/KWS sessions,8h soak and model commercial/distribution rights remain open. T-017 IN_PROGRESS.
