# T-017 Fold3 WAV smoke results — 2026-10-08

Native load and offline inference **PASS** on SM-F926B/API35/arm64-v8a,
sherpa-onnx1.13.8, CPU provider, model revision83e140db6d23fbb8480fd5fb868f74ab80e7092c.
The isolated runner passed1/1 test in12.692s and wrote45 measured rows.
Three upstream PCM16 mono16k WAVs (3.74s,2.40s,1.55s), two warmups per profile,
five measured repetitions per WAV/profile. There are only three unique samples;
these are compatibility/performance smoke, not a command-accuracy population.

| Threads | n measured | First-observed load ms | Decode median ms | Decode p95 ms | RTF median | RTF p95 | Max observed process PSS KB | CPU median/p95 ms |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 15 | 2251.88 | 66.33 | 103.72 | 0.02773 | 0.02997 | 302894 | 68 / 137 |
| 2 | 15 | 2440.23 | 53.54 | 81.48 | 0.02231 | 0.02481 | 306385 | 113 / 189 |
| 4 | 15 | 2518.91 | 55.53 | 77.98 | 0.02076 | 0.02867 | 309145 | 241 / 332 |

Percentiles are nearest rank over15 warm repeated measurements per profile;
small and repeated sample population limits generalization. `decodeMs` includes
acceptWaveform/decode/getResult; excludes microphone/endpoint/UI/action time.
Load is first-observed, not cold-controlled. CPU/PSS are process-wide and include
existing background app work. Max observed PSS is a sampled value, not true peak.
Transcripts were stable across repetitions, which does not establish correctness.
WER/CER/exact accuracy are null: no verified ground truths were supplied.

Six2-second-interval resource snapshots: thermal status0 throughout, battery91%
and30.9°C throughout. Sampled process PSS142386–307649KB; final readback after
release/restoration156744KB. This brief observation does not establish battery
cost, long-run memory stability, CPU utilization or mic/KWS recovery.

The first external-files attempt failed before native load because the runner
could not see the model. Staging into target private files via run-as and using
`-e sherpaBenchmarkStorage internal` resolved it; root cause of external access
was not established. No storage/system permission was granted. Wi-Fi ADB was
reconnected by owner; SDK37 file-based test-only install succeeded.

Original installed test APK restored successfully and exact SHA-256 verified.
Voice policy/history and enabled Accessibility/assistant role/PID snapshots are
byte-identical before/after; service is still bound, DPC remains Device Owner.
Production app update timestamp remains unchanged. No mic or action executed.
Private model/WAV/raw report/transcripts and aggregate JSON/CSV remain at
`/home/kunrg/.local/share/dex-benchmarks/T017-20261008/`, outside Git.

M2 is **partial smoke only**. Authentic consented command corpus, ground truth,
intent/slot, controlled cold load, live Google/Sherpa A/B, independent speech-end
markers,100mic/KWS sessions, soak and commercial/distribution rights remain open.
No production engine or KEEP_GOOGLE/SHERPA_PRIMARY/HYBRID decision is made.

Final storage-capable runner app lintDebug PASS29tasks; git diff checks PASS.

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
