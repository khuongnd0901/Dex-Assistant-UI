# T-019 microphone routing acceptance

Canonical task/plan: `/home/kunrg/DeX-Assistant/.agent/plans/active/T-019-speech-aware-microphone.md`.
Status: CANCELLED by owner2026-10-10. This historical diagnostic runbook is retained with the prototype; no further T-019 acceptance is scheduled and no physical speech, multi-input or provider handoff acceptance is claimed.

Run only the selected `MicrophoneRoutingDeviceTest` cases with `--no-restart`,
a matching original-certificate test APK and the exact rediscovered ADB serial.
Never run broad legacy voice/device suites against owner data. The diagnostic
must pause normal LISTENING using its normal service action, then resume it in
finally; paused/stopped owner state must remain unchanged. It never dispatches
commands or writes history/settings. TTS-generated proof speech is briefly in
app cache and deleted in finally; captured microphone frames stay in memory.

```
adb -s SERIAL shell am instrument -w -r --no-restart \
  -e class com.khuongnd.dexassistant.ui.MicrophoneRoutingDeviceTest \
  com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner
```

Inspect raw instrumentation status: assumptions/skips do not certify hardware
acceptance. `DexMicrophoneTest` reports only numeric route IDs/types, silencing,
provider identity and known-PCM proof status; it never logs captured audio/text.
The route diagnostic records each candidate sequentially for about 500ms and
reports requested vs actual device. A successful preferred-device call alone
is not proof of routing. Duplicate internal logical microphones do not establish
multiple independent physical microphone streams.

Owner acceptance: connect external inputs, verify actual preferred routes,
speak Hey Hey separately into phone/external input, wait for recognizer-ready
cue and give a harmless command. Verify selected input remains through command
and conversation continuation. Unplug selected input and verify safe fallback;
repeat with supported Bluetooth input if exposed by the device. Check pause,
stop and external-provider failure release the recorder/pipe without late work.

Sequential scanning cannot listen to every microphone at the same instant.
Quiet routes dwell 600ms (including 200ms route settling); activity holds the
current route for 1800ms after activity stops. Stable hum-like activity is
explored after five seconds, whereas variable activity continues its hold.
The RMS/zero-crossing classifier is a heuristic, not a trained VAD; short wake
phrases entirely on an unobserved input can be missed, and music/noise can hold
a route. No universal Bluetooth, simultaneous multi-input or speech reliability
claim is supported by host tests. Natural physical acceptance is required.
