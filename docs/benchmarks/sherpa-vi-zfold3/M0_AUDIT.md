# T-017 M0 — Model/runtime/license audit (2026-10-08)

**Status: SOURCE-AUDITED; Android ARM64 runtime and model license clearance NOT VERIFIED.**

## Existing runtime and model

- Android app pins `sherpa-onnx-1.13.8.aar` and GigaSpeech KWS Zipformer 3.3M via `scripts/prepare-voice-assets.py`; provenance in `app/src/main/assets/licenses/voice-PROVENANCE.txt`.
- `voice/SherpaWakeEngine.kt` uses a `KeywordSpotter`, model type `zipformer2`, INT8 encoder/decoder/joiner, `numThreads=1`, `provider=cpu`, keyword `Hey Hey`. This is NOT a Vietnamese STT engine.
- `voice/CommandRecognizer.kt` resolves installed Android `RecognitionService`, `vi-VN`, and allows online service execution (`EXTRA_PREFER_OFFLINE=false`). No backend is used by local commands that route successfully, but STT can involve a provider.
- `VoiceAssistantService.kt` currently owns wake and external STT capture; benchmark MUST NOT replace it.

## Target model from primary sources

- Model name: `sherpa-onnx-zipformer-vi-30M-int8-2026-02-09` (offline RNN-transducer), NOT `sherpa-onnx-zipformer-vi-int8-2025-10-16`.
- [Sherpa official model documentation](https://k2-fsa.github.io/sherpa/onnx/pretrained_models/offline-transducer/zipformer-transducer-models.html) lists `encoder.int8.onnx`, `decoder.onnx`, `joiner.int8.onnx`, `tokens.txt`, and `bpe.model`, and a WAV decode example. A/B Android viability has not been established.
- [Upstream originating model](https://huggingface.co/hynt/Zipformer-30M-RNNT-6000h) documents 30M and published test WER/CPU examples. These are **not** Fold3 measurements or specific on-device INT8 WER.
- [Sherpa-converted weights mirror](https://huggingface.co/csukuangfj2/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09) states that model files come from the hynt repository.
- Kotlin tag [v1.13.8 OfflineRecognizer](https://github.com/k2-fsa/sherpa-onnx/blob/v1.13.8/sherpa-onnx/kotlin-api/OfflineRecognizer.kt) exposes `OfflineRecognizer(config=...)`, `OfflineModelConfig(transducer=...,tokens=...,numThreads=...,provider=...)`, `createStream`, `stream.acceptWaveform`, `decode`, `getResult`, `stream.release`, `recognizer.release`. **Source-level API compatible**, but native support for this exact model on Fold3 is NOT VERIFIED.
- Model downloads are NOT added to CI/runtime; test model files must be provided locally. At execution, report SHA-256 for each loaded model file. **Trusted expected hashes are not yet pinned against an authoritative manifest; merely recording a hash does not authenticate a download.**

## License / redistribution gate

- Runtime `sherpa-onnx` is Apache-2.0 (current project provenance).
- Originating Hugging Face model card states **CC-BY-NC-ND-4.0**. This is not an unrestricted commercial or redistribution license, and downstream conversion/quantization raises a rights question. The Sherpa-converted mirror has no separate license declaration in its model card. Do NOT infer Apache-2.0 for model weights.
- Do not commit model weights, publish an APK with this model bundled, or promote model to production until the model owner/rights holder confirms compatible permission. Private evaluation must comply with applicable license terms.

## Gate result and next work

- M0 source inventory/model identity/API check: **PASS (document review)**.
- Native load on physical SM-F926*: **NOT VERIFIED**.
- Reproducible upstream digest/asset integrity: **PENDING**.
- Redistribution/prod permission: **BLOCKED PENDING LICENSE CLEARANCE**.
- Proceed only with opt-in WAV test scaffolding and host scoring; no production migration. A real Fold3 is required for M2/M3/M4.
