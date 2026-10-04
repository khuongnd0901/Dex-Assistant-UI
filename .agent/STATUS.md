# Current Android status — 2026-10-04

T-006 hardware voice + local command integration: shared direct activation from
default-assistant session, lightweight speech WEB_SEARCH/VOICE_COMMAND/ASSIST activity and external
ASSIST/VOICE_ASSIST keys now enters normal command STT without wake detection.
Worker FIFO preserves old recorder stop AND release before STT. Busy/debounce,
unlocked screen, assistant role/READY, permissions, call-mode and explicit
Pause/Stop policy gates retained. Unknown intents/extras and unrelated media/
headset/typing keys do not activate. No mic route/SCO/MediaSession change.
Local parser now accepts natural absolute 0..100% targets, shortcuts and bounded
app/media/search/brightness synonyms, retaining privacy/negation/compound guards.
Docs: `docs/HARDWARE_VOICE.md`, `docs/LOCAL_COMMANDS.md`.

Frozen JDK17 test/lint/app+testAPK PASS201tasks/36s;75debug+75release units
zero failures/errors/skips;diff check PASS. Log
`/tmp/t006-hardware-local-commands-frozen-gradle.log`. Main installed original-cert
matching app SHA9ad2844ddf0ba123e48631f999ba4cd30b445773c7629e4fe7fd6e677d12c4ce,
preserving owner configuration/credential state. VOICE_COMMAND resolver now
includes DeX alongside Google/Bixby. Resolver device PASS0.018s; actual parser/
executor volume40%+100% readback and initial volume/mute restoration PASS0.071s
(no assumption skip); direct activation/provider readiness/old recorder release/
busy rejection/cancel and actual refreshed wake capture PASS2.143s, policy/history
unchanged. First physical owner mouse trial FAILED; actual action fix is recorded
below. Wheel/SCO microphone compatibility and natural spoken latency remain unverified.
No playback verification implied. T-006 remains IN_PROGRESS. Next: broader
natural speech/latency and representative vehicle testing; physical mouse
acceptance is recorded below. Source publication is reviewed Git
history; no build artifacts/logs/secrets committed.

Physical follow-up FAILED on first deployed build: owner still saw only Bixby/
Google. Main captured actual chooser intent `android.speech.action.WEB_SEARCH`
(ResolverActivity,flags0x10800000), not assumed VOICE_COMMAND. Narrow follow-up
adds RecognizerIntent.ACTION_WEB_SEARCH to button activity/action allowlist and
resolver regression. Generic Intent.ACTION_WEB_SEARCH/ACTION_SEARCH and
unobserved ASR/hands-free actions remain rejected; external extras ignored.
Readiness/worker handoff/route/policy unchanged. Docs now describe observed route.
Frozen JDK17 test/lint/app+testAPK PASS201tasks/35s;75debug+75release units,
diffcheck PASS; log `/tmp/t006-hardware-speech-websearch-frozen-gradle.log`.
Unsigned appSHAd60f55c41c1daffdae97b6f8908922f8027ddbe0bc1fd2723125f75b00a7cebd;
testSHA47c1dfccc3cda0a0fc195653f9a79b6dcdf8550562a2c2399f69ed47b0d84218.
Main installed original-cert matching follow-up app
SHA64b375c3f1887f5ce4738d1f8f788af98d3c7860c821f6eadbdc2fbae7b2c253,
certificate unchanged. Exact speech WEB_SEARCH resolver now includes DeX,
Google and Bixby. Updated resolver device test covering VOICE_COMMAND/ASSIST/
speech WEB_SEARCH PASS0.010s, status0. Owner physical retrial PASS: DeX appeared
in chooser, assistant listened and opened YouTube. First physical trial FAILED
remains prior evidence; observed speech WEB_SEARCH registration fixed that route.
Main latest epoch6 metadata: button_requested→stt_requested8ms, button_requested
→provider-ready538ms. These are one activation timing sample, not end-to-end
spoken-command latency or a benchmark distribution. Wheel/car SCO microphone
compatibility and broader natural speech latency remain unverified.
Owner-requested Always preference verified: main selected DeX with “Luôn chọn”
in the exact speech WEB_SEARCH chooser. Subsequent resolve-activity returns
VoiceButtonActivity; package preferred entry for that action has mAlways=true
selected from three candidates. No permission/assistant-role/generic media
change. Owner subsequently confirmed post-Always physical repeat PASS (“đã test
thành công”). Persisted preference and repeat button use are verified on this
device; wheel/car SCO and wider natural-speech benchmarks remain open.

T-006 YouTube search regression: main device comparison found legacy default
SEARCH FAIL15.282s (YouTube window not visible, inconclusive query failure),
explicit-display SEARCH FAIL15.833s (YouTube visible, query absent), then results
VIEW PASS0.585s (YouTube and exact whole query visible). Initial owner's manual
VIEW trial left old screen; retain that prior contradictory evidence.
Localized source fix now sends package-bound HTTPS results VIEW with percent-
encoded full normalized query; browser only when YouTube package is absent.
Installed-app dispatch failure remains FAILED; no auto-play, display override,
parser/history/protocol/microphone/permission change. Two URL/encoding JVM tests
and Android intent contract added. Opt-in UI regression
`YoutubeSearchDeviceTest#warmYoutubeSearchDisplaysRequestedQueryOnDex` requires
`runYoutubeSearchUi=true`; default `youtubeSearchVariant=executor_default` uses
actual production executor and ordinary target context without display options.
Legacy/default/explicit-display VIEW comparisons retained. Optional
`youtubeDisplayId`, public `youtubeFixtureCommand`, `expectedYoutubeQuery`;
`observeYoutubeOnly=true` reads existing UI only. Reports public numeric display
and fixed-query equality booleans; no screen text export. Mutation mode rejects
already-visible fixture query and asserts actual visible query, not DISPATCHED.
No coordinator/history/config/key or voice lifecycle touched by test. Main owns
signing/deploy/device verification; Android agent did not install/run device
tests. Frozen JDK17 test/lint/app+testAPK PASS201tasks/51s;66debug+66release units
zero failures/errors/skips;diff check PASS. Log
`/tmp/t006-youtube-view-fix-final-gradle.log`. Fresh Gradle-debug-signed app
SHA62335a23d69b173cc7d84bfb42971e773b8bd30842fbf68541873f87990a5bd7;
testSHAf45e2fc8d5c8bd1e929842bc57606e6b7af9cc7f56cdc2d20d59f92fafb1fcc2.
Main installed matching-original-cert app/test preserving protected config and
credential state. Intent contract PASS0.011s. Actual normal-context production
executor_default warm fixture PASS1.382s. Corrected-report test exact owner
fixture PASS0.496s: Desktop2 before YouTube visible/query absent, after visible/
exact query present. General getDisplays omitted Desktop2; reporting now unions
general/category/requested IDs without relaxing predicates. Corrected test-only
lint/build PASS108tasks/20s; unsigned test
SHA833ffd9f357470a03444ca90d73d6e68634c7ddf497733e55fe175854a305c17,
installed after main original-cert signing. Production unchanged between cases.
This proves actual visible search query with warm YouTube and default executor
routing, not natural spoken STT latency, result relevance or playback. T-006
broader voice acceptance remains IN_PROGRESS; observer device run not claimed.

T-006 natural voice regression diagnostic: owner reports absent readiness cue,
slow YouTube launch, then no wake response. Added opt-in read-only
`VoiceCaptureObserverDeviceTest#observeExistingCapture`; requires
`--no-restart`, `observeExistingVoice=true`, optional `observeSeconds=1..20`,
and DONT_SUPPRESS_ACCESSIBILITY_SERVICES. Observes existing service/session,
numeric microphone RMS/update age/recorder state/silenced/routes, audio-device
source/sink capabilities, and filtered worker stack methods. No lifecycle calls,
callback injection, prefs/history/keys/permission/route/model changes or audio/
transcript capture/logging. Observation success is not proof of wake accuracy;
absent service/read error/main timeout are explicit. Nonvolatile mic age and
worker stack sampling can be inconclusive. Main agent reviews and runs device
probe; none installed/run by Android agent. Voice production source unchanged.
JDK17 frozen debug units/lint/testAPK build PASS120tasks/30s;64debug units
zero failures/errors/skips;diff check PASS. Build log
`/tmp/t006-voice-observer-frozen-gradle.log`. T-006 remains IN_PROGRESS.

Canonical tasks/decisions/plans remain authoritative:
[/home/kunrg/DeX-Assistant/.agent/STATUS.md](/home/kunrg/DeX-Assistant/.agent/STATUS.md)
and [handoff](/home/kunrg/DeX-Assistant/.agent/SESSION_HANDOFF.md).
T-006/T-009/T-010 remain IN_PROGRESS; measured D-025 slice validated.

D-023 coordinate GRID survives content/app/window changes on same unique DeX
geometry; strict target identity/patch checks remain. D-024 starts STT alongside
preparation,retains worker-owned native KWS with fresh streams,4s canned feedback
watchdog. D-025 process-wide FIFO history keeps encryption/IO off terminal path;
redaction before enqueue,ordered cross-instance clear/key deletion,read-time legacy
scrub,guarded async UI publication,separate pending/error state. Corrupt/decrypt
failure preserves ciphertext. Abrupt process death may lose queued diagnostics.

Full frozen JDK17 test/lint/app+test PASS201tasks/38s,64debug+64release units
zero failures/errors/skips. Five isolated device history tests use dedicated
prefs/key,owner history stays intact. Combined19cases18PASS/1canvas window-expiry
failure116.825s; focused unchanged canvas PASS4.700s,full9screen rerun PASS81.128s.
All5history,4voice andautomatic listening cases pass. No weakened target guard.
Controlled final→terminal1852/1464/1639ms→40/41/38ms; natural speech not inferred.

Final live YouTube PASS6.460s,Settings PASS6.480s after waiting app launch;grid
visibility/5s persistence/subdivision/one dispatch/duplicate rejection pass. Earlier
Settings badge failure remains unexplained; non-waiting launch prerequisite failures
recorded separately. Test-only badge counts and public package/window/display
assertion metadata added; predicates unchanged,no retries. Final diagnostic test
APK+lint PASS108tasks/15s;production APK unchanged.

Original-cert signed appSHA6c397e1efc2e2c21f2e6fa7989ff2968709376ebce24e322b54f803bf9307114;
D-025 testSHAdd78c509b32256d36813b9cd328a06902b69e5bbfecfe0bf4191b477e3272a01.
Device hashes verified,matching installs PASS,DPC/data/key preserved. Final Fold3
healthLISTENING,no explicit suppression,Accessibility enabled. SDK37 ADB endpoint
192.168.1.18:40581 may expire;rediscover dynamically. No permission reset/grant,
mic route/model/threshold change or speech/screenshots exported.

T-009 real backend continuation: test-only independent coordinator uses unique
history preferences/key and redirected configuration/credential preferences;
RAM-only WebSocket token and guarded authentication callback never provision or
delete the owner credential key. Observer detaches before compound Vietnamese
command; genuine protocol/action/terminal callbacks remain. Cleanup checks owner
configuration,credential preferences and history/credential key presence.
JDK17 unit/lint/testAPK PASS168tasks/22s,64debug+64release tests,zero failures/
errors;diff check PASS (/tmp/t009-real-backend-isolated-frozen-gradle.log).
Latest isolated signed testSHA00811a7dd883627163bc98e1c79a839e5ee95a46286a9f7ef09918d97eb69572.
Production source/APK unchanged. After owner replaced the provider key,main
restarted the latest local Java JAR;Google Models.get and backend readinessHTTP200.
Same signed isolated Fold3→ADB-reversed loopback→Java JAR test PASS1test/4.186s
(/tmp/t009-java-backend-fold3-newkey.log): compound “mở YouTube rồi mở Chrome”
reached actual DISPATCHED after observer detachment;owner configuration/credential
preferences and key-preservation cleanup assertions passed. This proves the
controlled real WebSocket/backend/action/headless flow,not natural speech or
production credential provisioning. T-009 broader acceptance remains IN_PROGRESS.
Earlier2.466s/2.655s runs failed AI_UNAVAILABLE due confirmed GoogleHTTP401
UNAUTHENTICATED;retain those as prior-key evidence,not current readiness.
No Docker,owner provisioning,policy/microphone/route or permission change.

Source publication is represented by reviewed Git commits;verify HEAD/origin/main
on resume. Next: natural repeated speech/cue audibility,100spoken sessions/
resources/wake/false-activation/soak;investigate late window/initial badge failure
if reproduced. Backend/provider/quota/release acceptance remains open.
Validation evidence:
[/home/kunrg/DeX-Assistant/.agent/evidence/T-006-history-latency-2026-10-04.md](/home/kunrg/DeX-Assistant/.agent/evidence/T-006-history-latency-2026-10-04.md).

Latest main-agent handoff continuation: Fold3 endpoint192.168.1.18:42563,
reverse8081→8080 restored;Accessibility unchanged. Java live RPM exhaustion/
server reconnect snapshot/natural refill PASS;6 focused backend tests PASS.
No Android tests/actions/install in this slice. Before physical offline/local/
monitoring tests, isolate owner history/credential and avoid Wi-Fi disconnect:
legacy LocalFirstDeviceTest clears owner history;monitoring needs review.
Canonical evidence: /home/kunrg/DeX-Assistant/.agent/evidence/T-009-handoff-quota-2026-10-04.md.
