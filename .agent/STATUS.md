# Android repository status

Updated 2026-10-03. Canonical tasks/contracts/acceptance remain in
[/home/kunrg/DeX-Assistant/.agent/STATUS.md](/home/kunrg/DeX-Assistant/.agent/STATUS.md).
T-006/T-009/T-010 remain IN_PROGRESS. V1/backend tools and DPC/signing identity
are unchanged. No reset, global microphone routing change or provider replacement.

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
Two uninterrupted wake→command cycles remain pending; an observed stop overlapped
instrumentation STOP setup and is not attributed to a YouTube/focus product defect.
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
main switch and disabled the shortcut; current six-case desktop + automatic run
executes all seven with no skips:6PASS and1expiry assertion failure/63.017s,
`/tmp/t010-display-scoped-fold3-final.log`. Canvas image/patch/tap, grid, local
routing, same-bounds node changes and automatic listening pass. Expiry case
correctly invalidates on a real content event after7s (eventType2048), then fails
its34s-active assertion; required quiet35s expiry proof remains open. Content
invalidation is retained, not relaxed to pass a timing assertion.

Current display callbacks validate unique Samsung desktop identity/geometry;
unrelated phone metadata does not expire a stable DeX session. Fixture preparation
waits bounded Accessibility snapshot stability before numbering, without retries.

Actual Fold3/API35 normal suite passes4tests/no skips in8.817s:
`/tmp/t010-fold3-center-selector-normal.log`. It verifies visible overlay, focused
unoccluded fixture tap/display coordinates, live overlay pass-through, grid
subdivision/cancel, fresh no-node canvas-X close and local malformed routing.
Owner verified historical Hey DeX→grid→split→cancel with continued listening.
Node-change/window checks now pass; uninterrupted quiet35s-expiry verification
remains open because real content invalidates the session earlier. Explicit `screenResourceSmoke=true` opts into100 synthetic prepare/clear
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
current full run `/tmp/t010-display-identity-final.log`:55units
(50app+5DPC),zero failures/errors,153tasks/28s. Unit/lint/app/test assemblies and
source diff check pass. Source/APKs are frozen for main's matching-signed
installation completed; integrated hardware result is6PASS/1early-content-expiry
assertion failure. Source/APKs remain frozen.
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
