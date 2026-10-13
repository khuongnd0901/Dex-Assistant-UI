> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-017 benchmark preparation and Fold3 smoke — 2026-10-08

## Scope

Owner requested necessary preparation after confirming no authentic corpus yet.
Main coordinated Android-only preparation in a separate worktree on the existing
T-017 branch. Production KWS/STT/actions/contracts remain unchanged.

## Prepared and validated

- Worktree `/home/kunrg/Dex-Assistant-UI-T017`, branch
  `task/T-017-sherpa-vi-asr-benchmark`, base438679d; local changes not published.
- Pinned model revision83e140db6d23fbb8480fd5fb868f74ab80e7092c.
  Host verified upstream HF LFS SHA-256/file sizes and tokens Git blob identity.
  Downloader creates SHA-256 manifest; runner checks it before native load.
- 200 prompt slots in40/40/40/30/20/30 groups, private reference template,
  consent/source-aware corpus validator. Prompts are not recordings/references.
- Host Python12/12 PASS. Android JDK17 offline `test lint
  :app:assembleDebugAndroidTest` PASS144 tasks in3m44s. App156 tests per
  debug/release variant, one expected skip each; DPC5 per variant PASS.
- Test APK signed with the existing certificate
  `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
  Existing installed test APK saved privately for restoration. No production
  app APK built or replaced by this work.
- Final storage-capable source app lintDebug PASS29tasks in1m7s; all three
  affected repository diff checks PASS.
- First-observed model load is explicitly distinguished from controlled cold
  load. WAV duration boundary corrected; stream/model finally-release retained.

## Model and audio provenance / limitations

Model origin [hynt card](https://huggingface.co/hynt/Zipformer-30M-RNNT-6000h)
still declares CC-BY-NC-ND-4.0. Main interprets [section2(a)(1)](https://creativecommons.org/licenses/by-nc-nd/4.0/legalcode.en)
as permitting this private noncommercial reproduction/evaluation without public
sharing. Commercial use and distribution of converted weights remain uncleared.
No model/audio is included in an APK or committed/published.

Three upstream WAVs at the pinned converted repository revision are
PCM16 mono16kHz, durations3.74s/2.40s/1.55s. LFS hashes verified. Their upstream
README attributes them to `nguyenvulebinh/wav2vec2-base-vietnamese-250h/audio-test`.
These are compatibility/performance smoke only; no independent speaker-consent
or ground-truth audit, no command-corpus or accuracy acceptance claimed.

## Device execution

Physical target SM-F926B/API35/arm64-v8a. Two large-file ADB push attempts stalled;
transport was reconnected without toggling Wi-Fi/settings. Retried using512KB chunks: all four model files, verified-model.json and three
WAVs completed and device SHA-256 matched host. Only benchmark-specific paths
were written on device.

## Open acceptance

Authentic consented command corpus, raw/normalized WER/CER with references,
intent/slot, controlled cold-load, independent endpoint timing, Google/Sherpa
live A/B,100mic/KWS sessions and soak remain open. Private evaluation is not
commercial/distribution clearance. Keep T-017 IN_PROGRESS.

## Recovery and verified smoke

Initial streamed test install failed and ADB39911 disappeared. Owner reconnected
Fold3 at192.168.1.18:38585; readback proved the initial failed install left the
original test APK intact. SDK37 `install --no-streaming --no-incremental -r`
successfully installed the matching-signature test APK.

First external-files test failed before native load: Missing encoder.int8.onnx,
although shell/run-as could list the files. Exact cause was not established.
A test-only internal storage option and package/path/UID/readability diagnostics
were added; focused test APK build PASS49tasks in1m4s. Model/manifest/WAVs were
staged under target private files/sherpa-vi-benchmark using run-as and file mode600,
then SHA-256 verified. No system storage permission was changed.

Isolated test with `-e sherpaBenchmarkStorage internal` **PASS1/1 in12.692s**.
Correct target package/process UID10329,45 measured rows, three upstream WAVs,
1/2/4threads, two warmups and five measured repeats per WAV/profile.

| Threads | n | First-observed load ms | Decode median/p95 ms | RTF median/p95 | Max observed process PSS KB | CPU median/p95 ms |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 15 | 2251.88 | 66.33 / 103.72 | 0.02773 / 0.02997 | 302894 | 68 / 137 |
| 2 | 15 | 2440.23 | 53.54 / 81.48 | 0.02231 / 0.02481 | 306385 | 113 / 189 |
| 4 | 15 | 2518.91 | 55.53 / 77.98 | 0.02076 / 0.02867 | 309145 | 241 / 332 |

WER/CER/exact accuracy null, no verified ground truths. No transcription changes
across repeats, which does not establish correctness. Percentiles are nearest
rank over15 repeated measurements on only three unique clips. Decode excludes
capture/endpoint/action; first-observed load is not controlled cold load. CPU
and PSS are process-wide approximate observations, not model-only resource cost.

Six resource samples: thermal status0, battery91%, battery sensor30.9°C throughout;
process PSS142386–307649KB. Final PSS after native release and restoration156744KB.
No battery-cost/soak/100-session inference can be drawn from the brief run.

Saved original test APK restored SUCCESS and exact SHA-256 verified. State snapshot
(enabled Accessibility/master, assistant role, PID15795), voice_policy.xml and
command_history_v1.xml are byte-identical before/after. Accessibility bound,
DeX DPC still Device Owner. Production app lastUpdateTime remains21:22:17;
no production app install, microphone capture, action dispatch, reset, grant,
role or Wi-Fi toggle was performed. Local source changes remain uncommitted.

## Durable private artifacts

Private artifacts are preserved with directory mode700 at
`/home/kunrg/.local/share/dex-benchmarks/T017-20261008/`: pinned model/manifest,
three upstream WAVs, signed benchmark APK, saved original installed test APK,
preflight snapshots, build log and non-executing WAV/resource run helper.
Local model SHA-256:

| File | SHA-256 |
| --- | --- |
| encoder.int8.onnx | 8ef5286dd427eb108055c2ddc1982aa31e544706072d5ea228729292dacade68 |
| decoder.onnx | cf2aa385b82c9d5d40cd29c3188af52d0249b3b78f0d4b7eb84ad502d50c7e7f |
| joiner.int8.onnx | 7311d2e17b810ecea515d79c71cc4668af8759256a06fa01d27047772320c821 |
| tokens.txt | ca8171f8bbd516c050b627582f2125c8f5f1f6ed967ab41b0fa9aae2cf61b492 |

Next: prepare owner-consented authentic WAV/reference corpus using the200 prompt
slots and validator, then full M2 performance/accuracy and controlled cold load.
Implement live A/B before M3 and actual mic/KWS lifecycle/resource tests before
M4. Keep T-017 IN_PROGRESS; native compatibility is verified, full acceptance
and production choice are not. Original test APK is restored, so reinstall the
signed storage-capable benchmark test APK for the next isolated run.
