# T-017 benchmark harness restored to Android main

Restored 2026-10-13 from retained Git revision `e5958b907574e147fe6a7151df8863dd73ede5b4`
(`origin/task/T-017-sherpa-vi-asr-benchmark`). Only previously missing benchmark
instrumentation, host tools, documentation and public templates were copied.
Production STT, build configuration and CI were unchanged. Three additional host
acceptance-data tests check 200-sample group coverage, orphan WAV rejection and
truncated WAV rejection; generated silent fixtures do not measure ASR accuracy. No recordings,
transcripts, model weights or generated reports were copied.

This revision contains the 12-prompt pilot collector, not the expanded resumable
200-prompt collector described by canonical evidence dated 2026-10-10. That later
source lived at `/home/kunrg/Dex-Assistant-UI-T017` on a different host and was not
available in this checkout or its retained worktrees. Do not use this pilot to
resume the expanded `session-1791590650000`, infer 200-prompt completeness, or
replace that session's private artifacts. Recover the later source before its
expanded collection campaign.

Host preparation/scoring tests are runnable with:

```sh
python3 -m unittest discover -s scripts/benchmarks -p 'test_*.py'
```

Pure `SherpaCorpusPcmTest` and fixture methods in `SherpaDryRunScoreTest` can run
on the Windows AVD. Model inference and real capture remain explicit opt-in
Fold3-only fixtures; coordinate all physical runs through canonical T-020.
The methodology's earlier physical instructions are historical and are not
permission to run another physical campaign. Model redistribution/production
rights clearance and live A/B evidence remain open.
