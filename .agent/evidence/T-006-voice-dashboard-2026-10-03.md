> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-006/T-010 voice handoff and dashboard correction — 2026-10-03

## Scope and observed failure
Owner approved five-part correction: repair focus handoff, pause/resume,
RAM-only partial/final transcript plus non-executing STT diagnostic, adaptive
single-page no-scroll dashboard, validation and commit/push both repos.
No V1/backend executable change or new STT provider/model is authorized/needed.

Read-only physical Fold3 SM-F926B/API35 at wireless ADB192.168.1.18:33891:
voice_health is PAUSED with AUDIO_FOCUS_LOST. AudioService focus events repeat:

| Time | Event |
| --- | --- |
| 09:54:02.204 | app uid10325 requests assistant/speech focus req3 |
| 09:54:02.235 | selected Google STT uid10284 requests focus req4 |
| 09:54:02.239 | app abandons focus after its focus-loss cancellation |
| 09:54:02.249 | Google abandons after recognizer cancellation |

Equivalent sequence at09:47:35,09:49:04,09:53:27,09:55:31. App recorder stops,
Google16kmono starts and stops in~125ms. Source only requests this app focus
in startRecognition, after native wake detection; listener unconditionally
cancelSession+PAUSE on loss/transient loss. Therefore wake reaches STT handoff;
competing focus causes reproducible self-cancellation. This does not prove
recognition accuracy or general microphone health. Earlier Live Transcribe
concurrent captures do silence app audio; do not conflate that with this focus bug.
No transcripts, screenshots or audio were exported in this investigation.

## Primary API sources
Checked2026-10-03:
- https://developer.android.com/media/optimize/audio-focus
- https://developer.android.com/reference/android/speech/RecognitionListener
- https://developer.android.com/reference/android/speech/RecognizerIntent
- https://developer.android.com/reference/android/media/AudioRecordingConfiguration
Audio focus governs playback; selected provider independently requests focus.
Partials are requested with EXTRA_PARTIAL_RESULTS but availability is provider-
dependent. Android reports capture silencing separately from initialization.

## Validation results
Final frozen JDK17 `testDebugUnitTest lint assembleDebug assembleDebugAndroidTest`
PASS153tasks/35s, `/tmp/t010-d020-compact-final.log`. XML47app+5DPC=52units,
zero failures/errors/skips. Both staged/unstaged diff checks PASS.
Source includes feedback-only focus with per-request generations, diagnostic
release barrier/prior-state restoration, credential-masked RAM StateFlow, partial
callbacks without dispatch, and adaptive dashboard with bounded transcript card.
Three new physical cases built; execution pending. No positive spoken final
acceptance claimed yet. Old4-case T-010 suite remains historical.

App signed with verified original certificate: SHA256
`023f0ca21675ec4c6a921317cbf49087f851eae7da6f9a2f6004687c81aeebf4`.
Old wireless port33891 disappeared; adb autoconnect found42331 and model
SM-F926B reverified before retry. App installation PASS (no-streaming49.8s).
Initial test original-cert SHA256
`905c892609680326a67e166dec56bd45a85b3d9d82e0d57de9eb53dcef13c2c9`, installPASS.

Initial three-case suite `/tmp/t006-dashboard-fold3-new-tests.log`:
realProviderHandoffStaysReadyWithoutCompetingAppFocus PASS: actual Google ready
held≥2s, no app focus, no PAUSED. Next case process crashed: test helper started
STOP via startForegroundService although STOP handler correctly stops without
starting foreground. Android ForegroundServiceDidNotStartInTimeException points
at test action helper; production stopVoice already uses startService.
Corrected STOP harness ordinary startService+bounded STOPPED/active-null barrier;
production unchanged. TestAPK/lint PASS108tasks9s
`/tmp/t010-voice-stop-harness.log`; matching-test replacement/rerun next.

Actual Accessibility secure enabled_accessibility_services=null, Enabled/Bound
services empty. Shortcut/button component alone is not active permission. Owner
asked to enable main toggle again; no shell permission mutation. Numbered desktop
acceptance awaits that setup; voice provider/layout tests need no grant.

## Reproduction and residual acceptance
Use original signing certificate, app/test replacement only, preserve DPC/data/
owner-enabled Accessibility. Instrumentation --no-restart and UiAutomation
FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES; deterministic callback tests and
real provider tests must be distinguished. Genuine HeyHey→vi-VN final utterance,
window fit/resize/IME and old soak/popup/YouTube criteria remain required.


### Corrected physical suite — two voice cases pass; layout harness blocked
Corrected test original-cert SHA256
`8cb0331bfa9668b16fcd707100bae2a1d0c48b4210da34a1564be745b65ea44b`, installPASS.
`/tmp/t006-dashboard-fold3-corrected-tests.log` runs3cases in7.242s:
- PASS actual Google readiness/no competing app focus held≥2s/no PAUSED.
- PASS actual CommandListener partial/final diagnostic paths: credential masked,
  history unchanged, stop clears transcript, late callbacks cannot update/dispatch.
  Inputs are controlled fake text; not real speech accuracy or packet capture.
- FAIL layout test before UI assertions: Coroutine TestScope exception handler
  not found via ServiceLoader. Preserved app coroutine registry initialized before
  test APK ExceptionCollectorAsService became visible. Packaged provider resources
  are present; Android agent replacing TestRule harness with ActivityScenario/
  actual Compose semantics without TestScope, retaining meaningful bounds/clicks.
No production restart/dependency change to work around this harness failure.
Owner asked actual HeyHey+hiệnlưới/text/no-scroll and Accessibility main toggle.

## Integrated automatic-listening follow-up

Cooldown revision JDK17 full unit/lint/app+test APK PASS54units/153tasks23s
(`/tmp/t006-autorecovery-cooldown-final.log`). Matching-signed Fold3 upgrade
PASS appSHA256 `9d159967d2cebacc5b9b32f7747e30b1cd823c4ad98eda9cbc60c45adbf33f00`,
testSHA256 `46f1e01bb68343e4002a11e7e74c08ba34c4d740bbe9f6b556fb7af08698e2f0`.
Actual three voice-handoff tests and real one-page renderer PASS; automatic
foreground test fails its initial listening wait, pending explicit STOP-delivery
barrier/diagnostics. Five-test run34.499s,4PASS/1FAIL; no blanket PASS claim.

Accessibility disable is proven Samsung shortcut invocation: system callstacks
09:42:26 and10:32:42 `performAccessibilityShortcutTargetService` call
`disableAccessibilityServiceLocked` and clear enabled services. Owner now
confirms main service ON/shortcut OFF; secure component readback confirms ON.
No SecureSettings grant was written. Later numbered run reads targets correctly:
6cases4PASS/2window-focus failures, zero skips; Settings/DeX systemUI focus
confirmed by dumpsys and owner confirms covering window/dialog. Guards refused
gestures. Rerun after owner removes cover, without weakening focus protection.

Automatic foreground failure was a test ordering race: when service was already
absent, active==null passed before queued ACTION_STOP was delivered, then STOP
reintroduced USER_STOPPED after policy cleanup. Test now waits actual STOP receipt
(suppression+STOPPED health+null service) before preparing auto startup. Production
unchanged. Fixed harness/test APK/lint PASS108tasks9s; signed testSHA256
`47476deaa6f05750c0adfd648b2f0e694fd7e5c6ee47dcc39da2351f4cdca644`.
AutomaticListening Fold3 PASS1test3.478s (`/tmp/t006-autolistening-fold3-stop-barrier.log`):
auto foreground open, explicitPause/resume, unintentional service-loss recovery,
explicitStop remains suppressed. Original policy restored.

Owner closed covering dialog; targeted numbered rerun2cases has gridPASS and
canvas prepareSCREEN_SESSION_EXPIRED (deadline/epoch invalidation), pending
fixture/event settling review; no stale or failed gesture was dispatched.

Final scoped-display artifacts fullJDK17 PASS55units(50app+5DPC),zero failures/
errors/skips,153tasks28s. Original-signed install PASS appSHA256
`076c386553689dd6e56743b6c769109beaf5333cffd9caa691f42b1fa0096175`,
testSHA256 `776d40b270b0bb67cc835cf256baf840d15a6fc0c83aadcc08ed979061962ac3`.
Final integrated Fold3 seven-case run63.017s:6PASS/1FAIL zero skips. Automatic
startup/recovery plus numbered routing/grid/split/no-node X/fresh-node cases pass.
Real35s-expiry case expected still-live session at34s but content event2048
invalidated earlier: overlay11:02:28.304, invalidation11:02:35.227. Conservative
cleanup remains intact; no expiry PASS inferred for this final run. Earlier
window/expiry case passed, but uninterrupted quiet final expiry remains open.
No retry/forced gesture or weakened content guard.

All automatic tests finished before owner two-wake manual test. Instrumentation
restored preexisting USER_STOPPED policy; shell cannot start non-exported voice
service (rejected, no state change). Owner asked to Resume once, then repeat
HeyHey→openYouTube→HeyHey→cancel and reopen app without Start. No instrumentation
will interrupt that manual interval.

Android implementation commit: `73d1fb52ae8e9a9a53f6d277c3fce700030366cf`
(Add numbered DeX control and resilient voice dashboard). Canonical memory/
requirements updated separately; actual owner repeated-wake test pending at
commit time, no task completion or full expiry PASS claimed.

## Owner repeated-wake report and missing cue
Owner confirms consecutiveHeyHey1→openYouTube succeeds andHeyHey2→numbers
appear. They then repeat wake and requesttắtYouTube, receiving unsupported
feedback; direct app-close action is not implemented. NumberedX is supported
workflow; no second wake needed once overlay/command turn ready. Owner cannot
hear readiness cue. Source80ms notification tone; readonlyFold3audio confirms
notification volume11/music15 bothunmuted, both routedHDMI. Route/duration may
affect perception; volume mute is not proven. Active fix bounded clearer media
ready cue, preserving provider focus and once/stale lifecycle checks.

Owner further confirms TTS “Không làm được” is audible while ready cue is not.
Audio output is therefore functioning; cue-specific playback/timing remains
the issue. No basis to alter mic route or force speaker/globalvolume.

Owner reports “Không làm được” TTS is audible, but readiness beep afterHeyHey is
not. Existing80ms notification cue shares HDMI path (notification11/music15
unmuted), so no global mute is indicated. Implemented candidate bounded360ms
rising two-note media PCM AudioTrack (USAGE_MEDIA/SONIFICATION), released by≤610ms
and canceled on session teardown; no focus/volume change. Once-per-listener epoch
ready callback now updates RAM-only visible “Sẵn sàng — nói lệnh”. Sanitized
provider health reports media dispatch/finish and routed device type. FullJDK17
56unit tests, lint and app/test build PASS28s (`/tmp/t006-media-ready-cue-gradle.log`);
Fold3 audible confirmation is pending.

Latest owner says after openingYouTube a later HeyHey sometimes shows neither
number nor grid. Prior manual report said HeyHey2 showed numbers, so reproduce
sequence is inconsistent. “Mở YouTube” exists; direct “tắtYouTube” does not.
After calling HeyHey, say “bấm số N” for a numbered target or “hiện lưới” for
full24cell fallback. Do not assume cue alone guarantees overlay; correlate
provider ready, live transcript, sanitized screen prepare result and session epoch.


## Final ready-cue and stable-content Fold3 integration
Source uses a generated bounded360ms rising two-note cue via media sonification;
provider onReady emits once per listener epoch, cancels on recognition teardown,
and mirrors ready state in RAM UI. Android test asserts actual Google service
ready, cue allocated/finished/released, HDMI route9 and no app focus ownership;
`/tmp/t006-media-ready-fold3.log` PASS1test3.37s. Cue audio was dispatched and
finished by Android; user must still confirm audible perception.

After YouTube label-missing report, located unconditional invalidation on every
Accessibility content/scroll event. Fix compares fresh screen snapshot to frozen
node/window mapping while retaining window-state and target/display changes plus
fresh screenshot guard before click. Final integrated Fold3 `/tmp/t010-stable-content-
fold3-final.log` PASS7tests66.241s; includes real Google provider handoff with cue.
Full JDK17 `/tmp/t010-stable-content-gradle.log` PASS57units/153tasks28s.
Final app/test SHA256 `5219bb8586dbf81fc89d71329cf3e06e7fef47140a9f21f91264a02bbf025646` /
`c472f0cfc2533b9e5c2958b61b92fd95c468e70d9f93580483b373380d05435c`. Latest
owner report says HeyHey on YouTube failed to show numbers; repeat after final install
is still needed to establish the fix in natural use.
