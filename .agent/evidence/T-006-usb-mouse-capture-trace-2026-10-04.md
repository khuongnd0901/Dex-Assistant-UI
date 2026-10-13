> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-006 USB mouse microphone trace

Owner confirms the mouse has a microphone; reports pointer drifting and
intermittent simultaneous microphone/pointer stalls. Metadata-only ADB trace;
no settings, permissions, routes, production code or APK changed.

Connected USB device VID6421/PID4133 reports audio control/stream interfaces
(class1/subclasses1,2) and HID keyboard/mouse interfaces (class3/subclass1,
protocols1,2). ALSA reports input=true/output=false; USB input PCM16/16kHz.
This is a genuine composite audio/HID device, not an app misclassification.
Source searches find no UsbManager/claimInterface/openDevice/bulkTransfer
in app main source. Audio capture uses Android AudioRecord, not a claim over
the entire USB peripheral. Hardware/firmware interaction remains possible.

## Observed handoff

16:32:06/16:33:11: DeX UID10325/session1049, active, silenced=false;
patch210 sources USB input port205/card1/device0, mono16kPCM16.
16:34:32.970: DeX session1049 stops.
16:34:33.153: Google STT UID10284/session1057 begins, 183ms later.
16:34:34: Google alone active, silenced=false; patch213 uses the SAME USB
input port205. 16:34:41.786 Google stops. DeX session1097 resumes
16:34:43.768; 16:34:45 snapshot again USB/unsilenced.
No concurrent capture or policy-silencing seen in these snapshots/events.
Source stops/releases wake recorder before posting STT start.

AudioFlinger historical thread counters show input frames progressed:
I/O350 frames2363520; I/O358 frames137280. These do not establish signal
quality, absence of intermittent stalls, or acoustic speech-end timestamps.

Latest session15 STTready4300418→final4311656 =11238ms; final→terminal
43ms DISPATCHED. Session14 final→terminal30ms SEND_FAILED. Transcripts not
exported; cannot infer exact spoken commands from terminal codes. Backend
failure diagnosis is separate from microphone contention.

## USB and pointer evidence

USB/HID removal16:31:39 and re-add16:31:51; owner explicitly confirms
manual unplug/replug. Do NOT claim spontaneous reset, hub power failure or
bandwidth overload from this sequence. No additional automatic disconnect
established in inspected tagged records.

20-second getevent aggregate on relative mouse event15 found REL_X=0,
REL_Y=0. No key/button data exported. Drift/stall was not reproduced in
that observation window; cannot rule it out or attribute to firmware.
No in-app observer installed/run; previous observer build-only result retained.

## Conclusion / next controlled experiment

App uses USB mic during wake and Google STT also uses USB mic during this
trace. No evidence that AudioRecord exclusively claims mouse HID or two apps
compete for this microphone in the observed handoff. USB signal quality,
physical controls (push-to-talk/mute/air-mouse mode), firmware/hub issues and
provider endpoint/network remain unknown.

Compare same connected mouse with USB audio routing disabled by the owner,
verify builtin route for both wake and STT, and repeat identical short speech
while observing pointer. Then restore setting and compare USB route. This
isolates USB capture while keeping HID connected; document output-routing
effects and avoid changing the setting silently. Root cause requires actual
stall reproduction/time alignment, not the current unsilenced flag alone.

T-006 remains IN_PROGRESS. Broad migration is deferred.
