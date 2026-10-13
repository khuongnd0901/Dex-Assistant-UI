> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-006 natural voice: delayed grid

Owner clarifies the delayed action is HeyHey then “mở lưới”. Read-only
Fold3 logcat metadata from the installed app, 2026-10-04; no transcript,
audio, screenshots, credentials or history exported. No production changes.

| Session | STT ready→final | Final→dispatch | Final→terminal | Terminal |
| --- | ---: | ---: | ---: | --- |
| 7, 16:24:58 | 7205 ms | 25 ms | 57 ms | SCREEN_CONTINUE |
| 9, 16:25:41 | 14440 ms | 14 ms | 49 ms | SCREEN_CONTINUE |
| 8, follow-up | 8581 ms | 18 ms | 151 ms | DISPATCHED |
| 10, follow-up | 7094 ms | 43 ms | 161 ms | DISPATCHED |

These are callback intervals, not measured acoustic speech-end latency.
Metadata alone does not identify each transcript. Owner's grid report aligns
with SCREEN_CONTINUE, which also applies to subdivision/overlay refresh.
DISPATCHED is dispatch completion, not independent visual observation.

Source: VoiceAssistantService.onPartialResults updates VoiceLiveState only;
onResults consumes the final and dispatches via AssistantController. The
screen parser handles exact normalized “mở lưới” locally; GRID invokes
DexAccessibilityService.freshGrid, which snapshots grid geometry and shows the
overlay directly without requiring screenshot analysis or backend inference.

Confirmed: current sampled long waits precede the final result. Post-final
screen processing is tens of milliseconds for SCREEN_CONTINUE, not seconds.
The displayed text may be partial, so visible text is not proof final arrived.
Unknown: actual speech-end, partial arrival timestamps, USB signal/route
during these sessions, provider endpoint versus network contribution, and
physical first visible grid frame. Do not infer these from onEndOfSpeech alone.

Next: capture first-partial/stability and actual-route metadata; verify the
same short spoken grid command with known working input. Consider a narrowly
scoped reversible grid preview from stable exact partials only after accounting
for corrections/negation and final-result deduplication. Keep clicks and
sensitive actions gated by final/confirmation; no generic partial execution.

Existing read-only Android capture observer builds/lints and passes 64 unit
tests. Test APK is not original-signed, installed or run. No observer runtime
results claimed. T-006 remains IN_PROGRESS; broad voice/UI migration deferred.
