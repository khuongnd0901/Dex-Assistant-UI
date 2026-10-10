# T-010 owner-assisted acceptance preparation

Owner is now ready for the first natural10 batch and actual DeX resize/disconnect.
T-010 remains IN_PROGRESS until measured acceptance is complete. The latest owner
authorization permits natural100 and actual DeX resize/disconnect. Never
disconnect Wi-Fi or Wi-Fi ADB. Main coordinates each hands-on trial.

## Capture and resume

On this host, first confirm the current serial with `adb devices` (last observed
`192.168.1.3:45413`). Keep output outside Git:

```sh
python3 scripts/android/collect-t010-natural.py --serial 192.168.1.3:45413 --output /tmp/t010-natural
```

Start capture only when proceeding with the owner. It reads allowlisted metadata
and resource diagnostics; no audio, transcripts, screenshots, private Settings,
keys or raw logcat files are retained. Let it collect an idle baseline for60s,
perform the owner batch, then allow60s after grid cancellation/overlay cleanup
for comparison. Ctrl-C closes the capture and writes summary.json. Each restart
creates a new run; epoch numbers never merge across captures. Resume another
batch with the same command. Review all runs:

```sh
python3 scripts/android/collect-t010-natural.py --summarize --output /tmp/t010-natural
```

Summaries count observed correlated terminal candidates, never certify natural
sessions. Store owner observations separately using only run ID, trial number,
expected cell/outcome, pass/fail, and optional coarse failure category. Do not
record spoken transcripts, contact details or screenshots. Deduplicate sessions
across capture gaps using the owner trial ledger; do not add raw tap counts to
voice-session totals. Natural trials can update production voice history under
the owner's normal history policy. This fixture does not override or clear it.

## Safe desktop target, when the owner is ready

Build the test APK using JDK17/Android SDK; before installation verify the
installed instrumentation package and replacement APK certificates match.
Use an in-place matching-signed replacement only; never uninstall or reset app
or test-package data. Preparation initially installed nothing; the owner-ready trial now installs only
the matching-original-certificate test APK, preserving the production APK.

Once the owner agrees to begin, run this opt-in fixture with the current serial:

```sh
adb -s 192.168.1.3:45413 shell am instrument --no-restart -w -r \
  -e class com.khuongnd.dexassistant.ui.NumberedScreenDeviceTest#optInNaturalHundredSessionInteractiveFixture \
  -e naturalHundredFixture true -e naturalFixtureTapTarget 10 \
  -e naturalFixtureDurationSeconds 900 \
  com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner
```

It resolves the unique Samsung desktop category dynamically, rather than
hardcoding display2 or virtual6. It shows a debug-only harmless canvas, injects
no speech or gesture, and waits up to15minutes for10touch observations. Target
can be1..100; duration60..3600seconds. A passing instrumentation result indicates
only fixture execution/cleanup, not natural acceptance. Physical manual mouse
or finger input is also a touch observation. Closing ends the fixture; service
clear and fixture finish execute at the bounded wait completion.

Owner flow: wait for READY_FOR_OWNER_SPEECH, say “Hey Hey” through the normal wake path, then say
“hiện lưới”, then say “bấm ô 11” after the grid appears. Check intended cell and
actual harmless target touch. Repeat in measured batches, include “chia ô 2” followed
by “bấm ô 8” for subdivision, and clear/cancel between trials as planned. Check
visible mapping before speaking; fixture may not cover the entire desktop.
Owner ledger plus correlated wake/STT/dispatch/terminal evidence determines the
actual100natural sessions. Never replace these with injected finals or100touches.

For actual geometry/identity trials, use a harmless grid, change real DeX output
resolution or disconnect only the external display while retaining Wi-Fi ADB,
then attempt the old selection and verify no stale tap. Physical window resize
alone does not establish changed display geometry. Reconnect and prepare a new
grid before fresh selection. Record owner observation and callback metadata;
synthetic callback regressions remain separate evidence.

## Measurement limits

Wake→recognizer ready, recognizer speech_end→final/dispatch, final→dispatch and
final→terminal use existing elapsedRealtime metadata. speech_end is the Android
recognizer endpoint callback, not an independently measured acoustic speech end.
Missing samples stay missing; no new acceptance threshold is introduced.
Resources.jsonl holds PSS KB, battery level/temperature, rolling dumpsys CPU and
thermal status every15s. CPU is a diagnostic rolling sample, not per-command
CPU cost. PSS changes alone do not prove overlay/bitmap release; correlate actual
cleanup and existing overlay lifecycle tests. Compare idle before/after same-run
conditions; baseline measurements are not present until a capture actually runs.
No8h soak is required to complete this T-010 preparation (T-006 owns that gate).

## Read-only wake/STT runtime diagnosis

When natural wake does not respond, pause owner trials. A normal appuid
PAUSE_VOICE_SERVICE→RESUME_VOICE_SERVICE cycle can rearm capture without
production reinstall, grants or history changes; verify voice_policy hash
before/after. Capture startup metadata and active AudioPolicy route. This is a
recovery attempt, not proof of root cause or natural acceptance.

After the previous instrumentation fixture has finished, install only the
matching-original-certificate observer testAPK and run:

```sh
adb -s 192.168.1.3:45413 shell am instrument --no-restart -w -r \
  -e class com.khuongnd.dexassistant.ui.VoiceRuntimeObservationDeviceTest#observeVoiceRuntimeWithoutTranscriptOrActions \
  -e observeVoiceRuntime true -e voiceObservationSeconds 120 \
  com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner
```

This observes RAM-only enum state/route/audio reception, numeric RMS,
recognizer-ready and diagnostic flags. It neither starts speech capture nor
sends actions, and never accesses transcripts, history, credentials or PCM.
Only main coordinates an owner speech window. To bypass the wake detector for a
no-command diagnostic trial, main may invoke the documented normal
DIAGNOSTIC_LISTEN action under app UID while the observer is running. Diagnostic
speech is excluded from natural command acceptance. No threshold/model tuning
or forced microphone route should precede measured evidence.
