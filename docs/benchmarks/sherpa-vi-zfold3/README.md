# T-017 — Sherpa Vietnamese ASR benchmark on Samsung Z Fold3

**State: Fold3 native WAV smoke passed; full A/B acceptance remains open.** This work is isolated to `androidTest` and host tools. Production KWS/STT/Accessibility/Device Owner remain untouched. No actions are executed by the test.

Read [M0 audit](M0_AUDIT.md) and [methodology](METHODOLOGY.md) before any device run. Model rights must be verified; no weights are stored in GitHub or packaged into production APK.

## Inputs on physical Fold3

The target Android package is `com.khuongnd.dexassistant.ui`. Place files under:

```
/sdcard/Android/data/com.khuongnd.dexassistant.ui/files/
  sherpa-vi-model/
    encoder.int8.onnx
    decoder.onnx
    joiner.int8.onnx
    tokens.txt
  sherpa-vi-wav/
    sample001.wav
    sample002.wav
    ...
```

Model identity: `sherpa-onnx-zipformer-vi-30M-int8-2026-02-09` as listed in [Sherpa upstream documentation](https://k2-fsa.github.io/sherpa/onnx/pretrained_models/offline-transducer/zipformer-transducer-models.html). Keep weights and recordings on the consenting local test device only. Confirm exact hashes and license from the model provider before use. Never substitute another model without updating audit.

Each WAV must be PCM16 mono 16 kHz, WAV RIFF, 0.01–60 seconds; filenames must be opaque sample IDs without personal data. All audio is used only for testing. Do not include test audio in git commits or GitHub Actions.

### Build on Android development host

```bash
# From Dex-Assistant-UI; JDK 17, SDK/API 35.
python3 scripts/prepare-voice-assets.py --offline  # or without --offline to prepare pinned KWS once
./gradlew :app:assembleDebugAndroidTest :app:assembleDebug --max-workers=1
python3 -m unittest discover -s scripts/benchmarks -p 'test_*.py'
```

### Safe device preflight (read-only)

```bash
adb devices -l
adb -s "$DEX_TEST_SERIAL" shell getprop ro.product.model
adb -s "$DEX_TEST_SERIAL" shell getprop ro.build.version.sdk
adb -s "$DEX_TEST_SERIAL" shell dumpsys device_policy
adb -s "$DEX_TEST_SERIAL" shell dumpsys accessibility
adb -s "$DEX_TEST_SERIAL" shell dumpsys meminfo com.khuongnd.dexassistant.ui
```

Use the existing signed app/test APKs: do not uninstall/reset/provision or install mismatching signing certificates. If the instrumentation package is missing, installation requires a separate in-place preservation review; do not automatically run any destructive install.

### Opt-in WAV run (do not run broad instrumentation suite)

```bash
adb -s "$DEX_TEST_SERIAL" shell am instrument --no-restart -w \
  -e runSherpaAsrBenchmark true \
  -e class com.khuongnd.dexassistant.ui.SherpaAsrBenchmarkDeviceTest \
  com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner
```

The test rejects non-Z Fold3, explicitly opts in, requests `FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES`, reads only local WAV/model files, doesn't open a mic or dispatch any device action, and writes a JSON report to `sherpa-vi-results/wav-benchmark-*.json` in the target app external-files directory. The instrumentation status reports the exact path. It does not change system rights or production preferences.

Pull the generated report only to a private local directory (no GitHub upload):

```bash
adb -s "$DEX_TEST_SERIAL" pull \
  /sdcard/Android/data/com.khuongnd.dexassistant.ui/files/sherpa-vi-results/ \
  ./private-benchmark-results/
```

Example references stored locally at `private-references.csv`:

```csv
sampleId,reference
sample001,Bật Wi-Fi
sample002,Tăng âm lượng
```

Process on the development host:

```bash
python3 scripts/benchmarks/sherpa_vi_score.py \
  ./private-benchmark-results/wav-benchmark-REPLACE.json \
  ./private-references.csv --output ./private-benchmark-results/summary.json
```

This outputs transcript-free summary metrics with WER/CER where references exist. Raw reports contain transcripts: keep private and delete when no longer necessary.

## Not yet implemented / acceptance blockers

- Google SpeechRecognizer vs Sherpa **live microphone A/B** (M3) is NOT implemented.
- 200 real utterances with consent, matched order/contexts, ground truths: NOT COLLECTED.
- Physical native WAV smoke: PASS; see [RESULTS.md](RESULTS.md). Full corpus/cold-load performance remains open.
- Battery/thermal comparisons and 100-session mic/KWS stability: NOT MEASURED.
- Intent/slot accuracy: NOT MEASURED (the WAV tool does not execute or call production command router).
- CPU/PSS samples are process-wide approximate observations, not a complete energy profile.
- Upstream revision/file digest pinning is implemented. Commercial/distribution clearance remains open; see M0 audit.

No `KEEP_GOOGLE`/`SHERPA_PRIMARY`/`HYBRID` recommendation may be claimed from the present tooling alone.

## Reproducible private preparation

```bash
python3 scripts/benchmarks/prepare_sherpa_vi_model.py /tmp/T017-private/model --private-noncommercial-evaluation
python3 scripts/benchmarks/validate_sherpa_vi_corpus.py /tmp/T017-private/audio /tmp/T017-private/references.csv
```

Downloader pins HF revision `83e140db6d23fbb8480fd5fb868f74ab80e7092c`.
ONNX files verify authoritative LFS SHA-256 and length; tokens verify the Git
blob ID and length, then record SHA-256 in private `verified-model.json`.
No archive extraction, device push or download during inference occurs.
Private noncommercial evaluation does not clear production redistribution rights.

Copy `references.template.csv` outside the checkout and replace its example.
Consent defaults to `NO`: only actual speaker permission authorizes `yes`.
Use opaque IDs, NFC Vietnamese reference text, group and `human_recording`.
Keep consent records and all audio/references private. The validator reports
missing coverage and rejects absent consent, mismatched IDs and wrong WAV format.
Scorer accepts these extra columns. Upstream sample WAVs may verify native decode
only; they do not establish command accuracy, consented corpus coverage or M3.

The runner records **first-observed model load** once per thread profile. Neither
OS file caches nor existing runtime initialization are controlled, so this is
not a cold-load measurement. It does two warmups and five measured decodes per
sample/profile; it cannot establish mic lifecycle/100-session stability.
Push `verified-model.json` alongside the four model files. The runner verifies
the pinned revision and all file SHA-256 values against that private manifest
before native load. The manifest must come from the verified host downloader.

`prompts.template.csv` contains200 prompt slots from the current command catalog,
with40/40/40/30/20/30 required groups. Repeated prompts permit separate speakers
and conditions; placeholders must be filled privately before reading. Prompts
are not recordings or verified reference transcripts. Catalog IDs do not
assert successful routing; multi-step prompts may require remote interpretation.
Noisy samples must be recorded safely while stationary, never while driving.

## Fold3 execution preparation checkpoint

Local worktree: `/home/kunrg/Dex-Assistant-UI-T017` on the existing T-017 branch.
Host12 tests and JDK17 `test lint :app:assembleDebugAndroidTest` passed144 tasks
on2026-10-08. Production app APK was not replaced. Only a matching-certificate
copy of the test APK is used for the isolated benchmark, after saving the
previous installed test APK for restoration. Do not install the old branch's
production app over a newer main app.

Private prepared model manifest and upstream smoke WAVs live outside Git at
`/tmp/T017-private/`. Exact upstream revisions/hashes are preserved there.
Acceptance recordings instead require `validate_sherpa_vi_corpus.py` before
copying the files to `sherpa-vi-wav`; use a separate directory so upstream smoke
samples do not mix with the authentic corpus. The existing runner reads every
WAV in its directory. Replace the smoke directory only after archiving needed
private files and checking that the destination has no unrelated recordings.

Run only the explicit opt-in test from the command above. Retain raw reports
privately; transcript-free summaries can be reviewed for publication. Scoring
without references must report WER/CER as null. Tests still do not provide
Google live A/B, independent speech endpoint timing or100 mic/KWS sessions.

Historical install/ADB interruption recovered after owner reconnect.
Native internal-storage smoke and restoration now verified below.

## Successful Fold3 private-storage run

The external-files attempt could not see the model on this device. Use
`-e sherpaBenchmarkStorage internal` with the explicit opt-in test. Target root:
`/data/user/0/com.khuongnd.dexassistant.ui/files/sherpa-vi-benchmark/`.
Stage into its model/WAV subdirectories as the target UID using `run-as`; retain
private file permissions. No storage permission/grant is needed. Pull the
reported JSON through `adb exec-out run-as com.khuongnd.dexassistant.ui cat PATH`
into a private local file. The private manifest is required in either mode.
The verified smoke and restoration results are in [RESULTS.md](RESULTS.md).
Use current SDK ADB with `install --no-streaming --no-incremental -r` for the
signed test APK; preserve and restore the original test APK. Never replace the
production app with an older benchmark-branch build.

## Consented real-speech pilot (test-only collector)

`SherpaRealCorpusCollectorTest` uses the existing installed debug fixture activity;
no replacement target app is needed. Requires existing RECORD_AUDIO permission,
physical Fold3 and production voice already PAUSED by the owner. The collector
never pauses/resumes production itself and never invokes Google/backend/routing.

```bash
# Main coordinator handles matching-signature test APK and state preservation.
adb -s "$DEX_TEST_SERIAL" shell am instrument --no-restart -w \
  -e class com.khuongnd.dexassistant.ui.SherpaCorpusPcmTest \
  com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner
# First run the collector UI/cancel guard with no microphone opened:
adb -s "$DEX_TEST_SERIAL" shell am instrument --no-restart -w \
  -e class com.khuongnd.dexassistant.ui.SherpaRealCorpusCollectorTest \
  -e recordLocalCorpusConsent true -e collectorUiSmoke true -e collectorDisplayId 2 \
  com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner
# Remove collectorUiSmoke for the owner-operated recording session.
```

The owner taps Record, reads the displayed phrase and taps Stop (15-second cap).
Playback is local only and requires production still PAUSED. The owner types the
actual spoken reference and checks its verification box before Confirm; prompts
are never automatically promoted to ground truth. Retry/Skip/Cancel are explicit.
Silenced, too short or negligible-energy recordings are rejected. Read errors and
voice reactivation reject the take. Closing the fixture stops/release capture.

Accepted files live privately under target `files/sherpa-vi-real-corpus/session-TIMESTAMP`:
WAVs, manually reviewed `references.csv`, source/route/sample-rate/silencing and
energy metadata. Upstream smoke files remain separate. Twelve pilot phrases do
not meet the 200-utterance corpus or live Google A/B acceptance. Stop tap is not
an acoustic speech-end marker; endpoint timing remains NOT_MEASURED. The 20-minute
session timeout reports failure; skipped-all reports no valid corpus. No raw
PCM or transcript is logged. The main coordinator restores the owner's prior
voice policy after collection.

Decode accepted audio with the existing WAV runner using
`-e sherpaBenchmarkStorage internal -e sherpaRealCorpusSession session-TIMESTAMP`.
It validates a numeric13-digit session ID and keeps the trusted model directory
and benchmark outputs under `files/sherpa-vi-benchmark`. Pull recordings/references
via target `run-as` into a private local directory and run the corpus validator
and scorer there. Never commit or upload these files.

The main coordinator may explicitly use `T017VoiceControlTest` with
`-e t017VoiceControl pause` or `resume` to invoke the existing dashboard facade.
This independent opt-in test admits only an already-running LISTENING/PAUSED
service, waits at most5seconds for the requested state and reports public states.
It never starts/connects/provisions the assistant. The recorder does not call
this helper. Coordinator announces the temporary pause, records the original
state/policy and restores LISTENING only when that was the original state. This
is reversible mic release for collection, not a production pipeline migration.

`SherpaDryRunScoreTest` performs parser-only intent/typed-slot scoring of the real
pilot, using the installed target's commands asset and actual LocalCommandEngine
with foreground context null. It constructs no controller, executor, router or
backend. Fixture tests cover word-number percent references that do not parse,
wrong opposite actions and scoring first repetition only. To score, opt in with
`runSherpaDryRunScore=true`, `sherpaRealCorpusSession=session-TIMESTAMP` and
`sherpaWavReport=wav-benchmark-TIMESTAMP.json`.

Aggregate metrics expose population and denominators: reference parse failures
are unscorable, never counted as a correct null/null match; exact command ID and
exact TypedAction equality use the scorable-reference denominator. Typed parameter
map equality uses only same-command-ID pairs and is strict (case/diacritics/text
are not silently folded). Known opposing operations are counted separately; this
is not an exhaustive wrong-action detector. Null foreground leaves scoped commands
unscorable. These parser metrics do not establish actual safe routing/execution,
Google comparison or 200-utterance acceptance. Output contains no sample IDs,
references, hypotheses or parameter values.
