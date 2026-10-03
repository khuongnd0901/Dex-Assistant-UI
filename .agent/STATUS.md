# Android repository status

Updated 2026-10-03. Canonical tasks/contracts/acceptance remain in
[/home/kunrg/DeX-Assistant/.agent/STATUS.md](/home/kunrg/DeX-Assistant/.agent/STATUS.md).
T-006/T-009/T-010 remain IN_PROGRESS. V1/backend tools and DPC/signing identity
are unchanged. No reset, global microphone routing change or provider replacement.

## Current follow-up: audible command readiness

Owner confirmed two successive real Hey Hey sessions: opening YouTube and then
showing numbered controls. Canned TTS is audible but the former 80 ms notification
ready beep was not heard. Read-only device evidence shows notification/music both
unmuted; a mute explanation is not established. Readiness now dispatches one
360 ms rising two-note media PCM cue only after provider `onReadyForSpeech`, with
visible “Sẵn sàng — nói lệnh”. It requests no audio focus and changes no volume or
route. AudioTrack is released within 610 ms or immediately on turn cancellation;
duplicate/stale readiness callbacks cannot replay/reactivate it. Sanitized cue
status/output-device type only are logged, with no captured audio/transcript.
Latest full checks and Fold3 provider playback test pass (see validation below).
The provider test verifies cue dispatch, bounded finish and HDMI route9 without
held app focus. Owner-perceived audibility still requires confirmation; successful
AudioTrack playback alone is not proof of hearing the cue. “Tắt YouTube” remains outside
the current local capability contract; numbered close-X tapping is the existing
workflow. No app-close/force-kill/back/home action was added.

## Current follow-up: stable content events

Video/clock content notifications now compare the fresh Accessibility snapshot
against the frozen preparing/active mapping. Identical bounds, node identity and
window topology preserve the session; changed/missing snapshots invalidate it.
Target-window state changes and desktop identity/geometry guards remain active.
Image-derived taps still remove the overlay and verify fresh screenshot patches
before dispatch; no automatic renumbering or retry was introduced. Unit coverage
includes recurring stable events and changed bounds/identity/topology; the device
expiry case also injects stable content events before waiting for the real timer.
JDK 17 full checks passed: 57 unit tests, lint, app/test assembly and diff check
(`/tmp/t010-stable-content-gradle.log`).
Latest Fold3 six numbered cases plus the real-provider cue case pass:7tests,
66.241s,zero failures/skips (`/tmp/t010-stable-content-fold3-final.log`).
The expiry case preserves ten injected stable content notifications and remains
active at34s before actual35s cleanup; changed window/node guards still pass.

## Current source: D-021 automatic listening

Foreground app opening and the existing default-assistant readiness loop request
listening automatically when policy/microphone permission/unlock allow it. Shared
recovery guard admits four-attempt bursts with backoff, a pending-start bound and
five-minute cooldown before a fresh burst;
active transcription/routing/feedback is never taken over. The default-assistant
loop requires held role and completed permission reconciliation; foreground
Activity auto-start uses foreground eligibility plus unlocked/mic/policy gates. External pause can
resume when no deliberate suppression and no call/communication audio mode.
User Pause and emergency Stop persist suppression across reopen/boot; only
explicit Start/Resume clears it. No new permission/DPC/global microphone setting.
Accessibility remains owner-mediated: actual service status and Settings explain
service switch vs optional shortcut. System call-stack evidence attributes recent
disabling to the Accessibility shortcut, not package update or instrumentation.

`AutomaticListeningDeviceTest` covers foreground auto-open, unintentional loss
recovery, explicit pause/stop/resume. Provider test also checks recovery cannot
take over active STT. Voice tests back up/restore policy; UI fixture temporarily
suppresses auto-start and restores it. All tests preserve enabled services.
D-021 physical auto-open/recovery/manual-pause/stop test passes on Fold3:
`/tmp/t006-autolistening-fold3-stop-barrier.log`,1test/3.478s. Its initial failed
wait was a test STOP-delivery ordering issue; the corrected helper waits actual
STOP suppression/health/service termination before clearing policy. Current
integrated automatic case passes again; production recovery is not weakened.

## Current source: D-020 handoff and single-page dashboard

App requests audio focus only for its own feedback, yielding capture/focus to
system STT. Per-request generations and session epochs reject stale callbacks;
genuine owned feedback interruptions pause distinctly, and Resume uses RESUME.
Diagnostic listening releases wake capture before STT, never prepares overlays
or dispatches commands, and restores prior LISTENING/PAUSED/STOPPED state.
Partial/final transcript is credential-masked RAM-only state, never preferences,
logs or diagnostic history. Final remains until next command or explicit stop.
Capture health reports human-readable route, RMS and client-silenced status;
system STT route remains managed by its provider, not claimed as app-verified.

Main page does not scroll. It has adaptive columns, primary voice controls and
transcript, dynamic DeX/Accessibility status, with configuration/quota/history/
manual-command/full-text dialogs. Minimum supported viewport is 320×480dp;
compact 600×320dp and wide 800×400dp are included in built layout tests.
Actual Fold3 runner now passes all three D-020 tests in5.081s:
`/tmp/t006-dashboard-fold3-renderer-tests.log`. This proves actual Google ready≥2s
without competing app focus/PAUSED, controlled actual partial/final diagnostic
paths/no-dispatch/late-callback protection, and real-renderer layout bounds,
no-scroll and Resume/diagnostic control clicks. Direct Compose semantics avoids
coroutine TestScope initialization in preserved `--no-restart` processes.
Signed test hash `bfcdc87274ff1cd7b00a9b14aa18e1afed2758a77df0293117f96105a738dbb7`.
Owner confirms real Hey Hey recognition, visible text and complete one-page UI.
Owner subsequently confirmed two successive real Hey Hey sessions: YouTube opened
and numbered controls appeared. Earlier STOP overlapped instrumentation setup
and is not attributed to a YouTube/focus product defect.
No automatic permission grant exists in these tests. Owner re-enabled
Accessibility; main readback confirms bound capability161 before numbered tests.

## Numbered DeX control and current wake

Samsung desktop selection fails closed; immutable numbered Accessibility/image
mappings invalidate on changes. Grid6×4 has depth3 and continued listening.
Bounded on-device X/outline analysis and target-patch revalidation use RAM only.
API34+ raw Accessibility Surface overlay has no input channel/focus; API35 fresh
image capture waits for detach presentation. Gestures consume session once and
report DISPATCHED, not verified popup closure. Node refresh/identity signatures
protect same-bounds replacements; cleanup covers cancel/pause/stop/timeouts.
A later physical six-case regression found preparation unavailable despite valid
DeX/service; snapshot collection had aborted on any unrefreshable Samsung node.
Current fix excludes that node while preserving window occlusion and logs only
stage/class/visited/skipped/target counts. The intermediate copied-fix runner skipped five desktop cases while the owner's
shortcut had disabled Accessibility; skips are not acceptance. Owner enabled the
main switch and disabled the shortcut. The prior six-case desktop + automatic
run had6PASS/1early-content expiry failure; recurring stable content events were
then fixed as described above. Latest six numbered cases and provider cue test
pass without skips. Canvas image/patch/tap, grid, local routing, same-bounds node
changes, changed windows, stable content and actual35s expiry are verified.

Current display callbacks validate unique Samsung desktop identity/geometry;
unrelated phone metadata does not expire a stable DeX session. Fixture preparation
waits bounded Accessibility snapshot stability before numbering, without retries.

Actual Fold3/API35 normal suite passes4tests/no skips in8.817s:
`/tmp/t010-fold3-center-selector-normal.log`. It verifies visible overlay, focused
unoccluded fixture tap/display coordinates, live overlay pass-through, grid
subdivision/cancel, fresh no-node canvas-X close and local malformed routing.
Owner verified historical Hey DeX→grid→split→cancel with continued listening.
Node-change/window checks and35s-expiry verification now pass. Explicit `screenResourceSmoke=true` opts into100 synthetic prepare/clear
cycles with preparation percentiles/processCPU/PSS/release checks; this does not
replace100 spoken-session acceptance. No images/transcripts are exported.

Current wake is only “Hey Hey”: `▁HE Y ▁HE Y @HEY_HEY`, packaged and inspected.
Pinned sherpa-onnx1.13.8/GigaSpeech3.3M model and thresholds are unchanged. Owner
requested this phrase; unrun phone-input/PCM prototype was removed. SYSTEM mic
routing and online-permitted Vietnamese SpeechRecognizer remain unchanged.
Previous matching-signed Hey Hey app/test installed successfully; app hash
`805ea2f1361c8cbbef33ebbf019e760c7cc92f9d847a4a1710378ea826270349`, test
`3502e5998dd69117c6298c11bb47dbbb331e3695c6a18f361eb18abda4533ece`.
Those artifacts precede D-020 and do not prove the current correction.

## Validation and next action

JDK17 pinned voice preparation, unit tests, lint and app/test assemblies pass;
latest full run `/tmp/t010-stable-content-gradle.log`:57units
(52app+5DPC),zero failures/errors,153tasks/28s; diff check passes.
Matching-signed app/test installed successfully on Fold3/API35:
app SHA `5219bb8586dbf81fc89d71329cf3e06e7fef47140a9f21f91264a02bbf025646`,
test SHA `c472f0cfc2533b9e5c2958b61b92fd95c468e70d9f93580483b373380d05435c`.
Latest hardware seven cases pass in66.241s/no skips:
`/tmp/t010-stable-content-fold3-final.log`. Source/APKs remain frozen.
The controlled three-cycle test reuses one service and exercises actual
STT/local invalid final/TTS/native wake capture, with no taps/backend command,
restoring history ciphertext without key rotation. It is not owner wake accuracy.

Main owns matching-signed physical installation and integration validation.
Main reviewed this source and approved publication to Android main; remaining
hardware acceptance stays open. Run instrumentation
with UiAutomation preserving services and `am instrument --no-restart`, never
force-stop setup. Check bound Accessibility/actual dynamic display beforehand.
Physical device ports/displays can change; do not hardcode prior IDs/serials.

Remaining: uninterrupted repeated Hey Hey→Vietnamese command/action, actual popup/YouTube,
resize/disconnect/permission-revoke/error cleanup,100spoken sessions/latency/
CPU/RAM/battery/thermal, wake accuracy/false activation and live provider end-to-end
completion. T-006/T-009 acceptance stays open. Historical details live in canonical
T-006/T-010 evidence and plans; builds alone cannot mark these tasks DONE.
