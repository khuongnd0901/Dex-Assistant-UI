# T-017 benchmark methodology (planned, not measured)

## Evidence tiers

1. **Host/source checks**: prove scripts, Kotlin API shape and dataset integrity only; never claim real-device latency.
2. **Fold3 deterministic WAV** (M2): same PCM16 mono 16k across each 1/2/4-thread run; 2 warm-ups, 5 measured decodes per sample. Create one recognizer per thread profile and release all native streams; report separately observed load time, decode latency/RTF, process CPU-time and PSS snapshots.
3. **Fold3 microphone A/B** (M3, not implemented): alternate Sherpa and actual configured Android RecognitionService (AB/BA), with identical scripted phrases but necessarily separate live takes. Do not claim same PCM for provider STT; do not claim that the service is always network-backed.
4. **100-session stability** (M4, not implemented): check mic handoff/KWS recovery, crash/ANR, retained native buffers, PSS over time and heat/battery changes, without disrupting DeX device owner.

## Collection protocol

- Use user-consented recordings of short Vietnamese commands, include negative/positive pairs ("bật" vs "tắt"), names, Vietnamese diacritics, mixed English app names, DeX screen indices and background music/noise.
- Proposed 200 authentic utterances: 40 system + 40 DeX + 40 media + 30 navigation + 20 multi-step + 30 noisy. If fewer, report actual N; do not synthesize speech and present as authentic.
- Use opaque IDs matching a private `references.csv`; no names, phone numbers, audio or private transcripts committed or transmitted. Files in `getExternalFilesDir` are on-device only; reviewers must check device access and consent.
- Measurement categories: first-observed model load vs warmed decode (filesystem caches and runtime initialization are uncontrolled; cold-load measurement NOT_IMPLEMENTED), clean/noisy, local vs cloud-provider, DeX connected vs disconnected.
- Baseline capture before/after: Android build/device SKU, model and weights hashes, process PSS, process CPU time, temperature/thermal service if available. Wait for cooldown between configurations.

## Latency semantics

- `decodeMs` in WAV runner is elapsed monotonic wall-clock for acceptWaveform + decode + getResult; excludes file-read, capture, voice endpoint wait, UI routing and action dispatch.
- `rtf = decodeMs / (audioSeconds * 1000)`; smaller means faster than playback.
- `processCpuMs` is process-wide CPU consumed over decode; native background threads and other app activity may contribute.
- `processPssAfterKb` is a snapshot; `maxObserved` is not an actual OS-level peak RSS.
- Median and nearest-rank p95/p99 must always be shown with population N; p99 of five runs provides little confidence.
- For live pipeline later, use independent speech-end marker where feasible; do not pretend `onEndOfSpeech` is a universal acoustic ground truth. Keep user-utterance time separate from endpoint/provider waiting time.
- Do not subtract timestamps from host, device, provider or backend clocks.

## Accuracy

- Normalize NFC, lowercase, remove punctuation, trim whitespace, **preserve Vietnamese accents and numbers**. Compute corpus micro-averaged WER (edit distance per word/total reference words), CER (edit distance per character/total reference characters), exact transcript rate.
- The first measured repetition per audio sample is scored; transcription disagreement across repeats is counted. Never cherry-pick the best hypothesis.
- Intent/slot accuracy must use the app's real `LocalCommandEngine` in a future **dry-run parser-only** test, with no `ActionRouter` calls. Mark these `NOT_MEASURED` until implemented.
- A few misrecognized short words can reverse commands (bật/tắt) or change numeric slots; track them separately from WER.
- If ground truth is missing, score `null` instead of assuming a correct transcript.

## Test and publication gates

- Existing `Android SpeechRecognizer` remains the production engine.
- Report M0 source audit vs M2/M3/M4 physical measurement separately and disclose any partial datasets/timeouts/crashes.
- Never publish personal audio/transcript datasets to GitHub. Maintain a local evidence trail outside the repo and only commit aggregate non-sensitive analysis after review.
- Requires owner approval for recording users, device APK/signing changes, license/weights distribution and any production STT change.
