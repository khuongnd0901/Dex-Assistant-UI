# Current Android status — 2026-10-04

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
