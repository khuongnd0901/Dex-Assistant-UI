> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-006 Vietnamese offline speech capability gate — 2026-10-02

## Scope

Continue the existing T-006/T-009 driving milestone. Measure platform-reported
vi-VN speech support without recording, speaking, downloading language packs,
or changing the production voice service. This is a capability diagnostic,
not real wake/STT/TTS acceptance. Physical Fold3 remains unmeasured.

## Model and runtime provenance

Primary sources accessed 2026-10-02:

- [Official Vietnamese offline transducer catalog](https://k2-fsa.github.io/sherpa/onnx/pretrained_models/offline-transducer/zipformer-transducer-models.html)
  lists the 2025 Vietnamese int8 and 2026 Vietnamese 30M int8 candidates.
- [30M upstream model card](https://huggingface.co/hynt/Zipformer-30M-RNNT-6000h)
  declares `cc-by-nc-nd-4.0`, revision
  `ad6f873b5f5b6ed083821253a88d81afed2505c4`. It is not selected for APK
  integration; do not assume the runtime license covers these weights.
- [2025 converted model card](https://huggingface.co/csukuangfj/sherpa-onnx-zipformer-vi-2025-04-20)
  revision `0fc3fea3ccd9c50b439755fa8a6aba546cb3a7d4` points to
  [its upstream model](https://huggingface.co/zzasdf/viet_iter3_pseudo_label),
  revision `e827965a37aab92a4455566fac49c0e80a23afef`. Upstream README declares
  `apache-2.0`; converted card supplies no separate license metadata.
  This is a candidate for an isolated prototype, not a final redistribution
  clearance: preserve upstream provenance, license/notice obligations, and
  verify conversion lineage before bundling.
- [Pinned runtime license](https://github.com/k2-fsa/sherpa-onnx/blob/v1.13.8/LICENSE)
  is Apache-2.0. [Release v1.13.8](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.8)
  provides an Android AAR. Runtime/model interoperability and API35 native
  loading are still untested; archive inspection alone does not establish them.

Release metadata: 2025 int8 archive is 60,181,038 bytes; 30M int8 archive is
26,442,384 bytes. Only the runtime AAR and 2025 archive were downloaded to
ignored `test-artifacts/vi-speech-research/`; no weights/dependency entered Git
or the production APK. No engine decision or shared protocol change was made.

## Artifact inspection

Downloaded bytes were hashed locally:

| Artifact | SHA-256 | Inspection |
| --- | --- | --- |
| `sherpa-onnx-1.13.8.aar` | `633c24321e06b1fe79feafa03ea16cbc0f8a286641e2da3559bac91bdb13bd96` | 50,129,134 bytes; native libraries for arm64-v8a, armeabi-v7a, x86, x86_64 |
| `vi-int8-2025-04-20.tar.bz2` | `48d0fdc9b3515eb9b00c4dfec2883207ee5ebe5c95b1959e7afce87fc3391938` | encoder 70,876,129; decoder 5,165,084; joiner 1,033,417 bytes |

AAR contains no LICENSE/NOTICE filename; the model archive contains README,
tokens, BPE and test WAVs but no LICENSE/NOTICE filename. Preserve separate
upstream license material before distribution. Fixture audio provenance still
requires review before use. Neither native libraries nor models were executed.

## Device diagnostic and validation

Android adds `SpeechCapabilityDeviceTest.reportVietnameseOfflineCapabilities`
and debug-only package visibility for recognition/TTS services. It checks the
explicit on-device service and bounded support callback (API31/33 gates), then
inventories exact vi-VN TTS voices and excludes network or missing-data voices.
The recognizer is destroyed and TTS engine shut down after the probe. No
`startListening`, synthesis, or language-pack download is invoked.

Retained Windows `ZFold3_API35`, emulator-5554, API35: runner `OK (1 test)`
in 0.654s. On-device STT service exists, but installed/pending lists are empty;
vi-VN appears only in the downloadable list. Google TTS advertises six offline
Vietnamese voices with missing data and five network voices; zero installed
offline voices. Both diagnostic outcomes are `UNAVAILABLE`. This measures
engine metadata, not recognition/playback, wake detection or Fold3 support.

JDK17/API35 `testDebugUnitTest lint assembleDebug assembleDebugAndroidTest`
passes (152 tasks, 1m32s). Existing unit regressions pass; no production behavior
was introduced. Backend executable source unchanged, so Maven rerun is not
required for this documentation/test-only slice.

## Next gate

Use the measured platform result to choose a bounded next experiment. For a
bundled prototype, verify 2025 conversion lineage/notices, load the pinned
runtime on x86_64 AVD and arm64 Fold3, then transcribe licensed Vietnamese
fixtures offline and measure latency/memory. ASR plus phrase matching remains
experimental; it cannot meet wake accuracy/soak acceptance without live audio
and noise measurements. Engine replacement remains the existing plan gate.

## Reproduce

From `/mnt/d/dex-assistant-ui`:

```bash
env JAVA_HOME=/home/khuongnd/.local/opt/jdk17 ANDROID_HOME=/home/khuongnd/Android/Sdk ./gradlew --no-daemon testDebugUnitTest lint assembleDebug assembleDebugAndroidTest
/mnt/d/Android/Sdk/platform-tools/adb.exe -s emulator-5554 install -r D:/dex-assistant-ui/app/build/outputs/apk/debug/app-debug.apk
/mnt/d/Android/Sdk/platform-tools/adb.exe -s emulator-5554 install -r D:/dex-assistant-ui/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
/mnt/d/Android/Sdk/platform-tools/adb.exe -s emulator-5554 shell am instrument -w -e class com.khuongnd.dexassistant.ui.SpeechCapabilityDeviceTest com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner
```

Install commands above target the retained AVD only. Do not install these local
debug APKs over physical Fold3's distinct signing identity. No device reset or
DPC replacement occurred. Both repositories pass `git diff --check`.
