# T-017 — Sherpa Vietnamese ASR benchmark on Samsung Z Fold3

**State: benchmark tooling only; no results yet.** This work is isolated to `androidTest` and host tools. Production KWS/STT/Accessibility/Device Owner remain untouched. No actions are executed by the test.

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
- Physical Fold3 performance, native model load and inferred RTF: NOT MEASURED.
- Battery/thermal comparisons and 100-session mic/KWS stability: NOT MEASURED.
- Intent/slot accuracy: NOT MEASURED (the WAV tool does not execute or call production command router).
- CPU/PSS samples are process-wide approximate observations, not a complete energy profile.
- License verification and authoritative file digest pinning remain open; see M0 audit.

No `KEEP_GOOGLE`/`SHERPA_PRIMARY`/`HYBRID` recommendation may be claimed from the present tooling alone.
