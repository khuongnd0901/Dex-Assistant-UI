> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-006/T-009 history latency continuation — 2026-10-04

Owner resumed canonical handoff; both dirty trees preserved. Main delegates
Android source/tests to Android owner and owns physical integration and canonical
memory. No new task: existing T-006, coordinated T-009/T-010, stay IN_PROGRESS.

## Device discovery and Settings rerun
SDK37 ADB finds SM-F926B/API35 at192.168.1.18:40581. Enabled and bound
DexAccessibilityService preserved. Prior42789 endpoint refuses connection.
Cached matching-signed app/test artifacts remain on device.

Existing installed test run:
```sh
adb -s DEVICE shell am instrument --no-restart -w \
  -e class com.khuongnd.dexassistant.ui.NumberedScreenDeviceTest#liveForegroundGridPersistsThenCancelsOrDispatchesOnce \
  -e screenLivePackage com.android.settings -e screenLiveTap true \
  com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner
```
FAIL1test/0.740s at first `assertLiveGridOverlay`: “Grid badge must actually
appear in the local screenshot ()”. Foreground Settings package assertion and
GRID SCREEN_CONTINUE passed; no cell tap was reached. DexScreen logged
RAW_SURFACE display2/api35. RAM bitmap was disposed inside test; no screenshot
was exported. Visibility cause remains unknown; do not count Settings passed.

## Timing-only baseline
Android adds monotonic DexHistoryTiming stage/count/loadMs/persistMs/elapsedMs,
without speech/entry IDs/ciphertext. Existing synchronous behavior preserved for
measurement before accepting FIFO scheduling. Measurements and validation follow.

Timing-only full host validation PASS201tasks/2m56s;59debug+59release unit
executions,zero failures/errors;lint/app/test assembly/diff check pass.
Signed app SHA8036ba93a8fa40e2f70a839204c83d0e7444527586795315f8c20e0908011a2a,
original certificate9ddb7f47… verified. First compressed overwrite transfer
produced different remote hash and Android parse rejection; application unchanged.
Uncompressed transfer to unique filename matches hash and pm install-r PASS.

Controlled baseline voice runner PASS1test/23.905s (three successive local-final
cycles, no taps/backend). Raw sanitized logs /tmp/t006-history-baseline-fold3.log
and /tmp/t006-history-baseline-timing.log. Retained history71entries before test,
72/73/74during cycles; original ciphertext restored with same key.

| Cycle | Final→dispatch ms | Dispatch→terminal ms | record ms | read ms |
| --- | ---: | ---: | ---: | ---: |
| 1 | 6 | 1846 | 912 | 873 |
| 2 | 5 | 1459 | 721 | 710 |
| 3 | 5 | 1634 | 801 | 807 |

Second/third history sums explain~98% of wait. D-025 selected for FIFO
background history under existing authorization; validation remains pending.

## Settings diagnostic follow-up
Test-only all24 fixed grid-badge crop counts added; original cell1 assertion
unchanged. TestAPK/lint PASS108tasks/21s. Signed testSHA
c08790400f0db09265f3483e7c214c11dc07c2242dba1fcd3b318d7f49936617 matches
device hash; matching-signed test install PASS. Explicitly opened Settings on
display2 with `am start --display2 -a android.settings.SETTINGS` (actual command
uses `--display 2`). Live GRID runner PASS1test/6.497s; cell1 yellow40/black924
and all24 expected badges positive at both captures. Five-second persistence,
subdivision, one coordinate dispatch, cleanup and duplicate rejection pass.
Raw runner /tmp/t010-settings-badge-diagnostic-fold3.log; RAM-only crop counts
in DexScreenTest. This proves coordinate dispatch, not semantic action/speech.

Immediate rerun without reopening Settings fails foreground-package prerequisite
(1test/0.060s); preceding intentional tap changed focus. No gesture occurred in
that prerequisite failure. First badge failure in pre-existing focused WiFi-debug
pane remains unreproduced/unexplained; no test relaxation or renderer change.

## FIFO implementation and frozen artifacts
One process-wide worker serializes all stores. Coordinator record/read/clear use
async immutable pre-redacted entries and generation-guarded snapshots. Pending
clear and storage errors are visible independently of command status. Checked
commits/decrypt errors preserve existing ciphertext; read persists legacy scrub.
Synchronous read/record/clear plus flush submit to the same FIFO; tests drain before
owner ciphertext backup/restore. Abrupt process death can lose queued diagnostics.

Five new unit cases cover redaction, FIFO/failure/clear, callback exception
survival, graceful shutdown/rejection, and publication supersession. Five isolated
physical tests use their own prefs/key alias and cannot delete owner history key.

Frozen JDK17 `test lint assembleDebug assembleDebugAndroidTest` PASS201tasks/38s,
64debug+64release units,zero failures/errors/skips;diff check passes. Final log
/tmp/t006-history-fifo-frozen-gradle.log. Main source review completed.

Unsigned appSHA764afaa06ceb05e663fb12a1a0847c274a343bad9e6d55d52a63303cb2878b2a;
unsigned testSHA3b377496ba983212075ca730b943ac878ec01bee7035434c5c9ac3f3a28d5d10.
Final signing uses original certificate9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03:
- signed appSHA6c397e1efc2e2c21f2e6fa7989ff2968709376ebce24e322b54f803bf9307114
- signed testSHA9256b2c88649f8f159ecb1e2918145ac3667ae5ba9848d5f0d0c3c497684d12d

Both device hashes match; same-signed `pm install-r` PASS, DPC/data retained.
Earlier606d/43b0pair was prepared before final test/style edits and not installed.
Final pair above is the sole FIFO hardware acceptance artifact.

19-case physical runner selection excludes opt-in/unused cases: all5isolated
history,all4VoiceHandoff,AutomaticListening,and9screen cases. Raw runner
/tmp/t006-history-fifo-regression-fold3.log, sanitized timing
/tmp/t006-history-fifo-timing.log. Final result follows.

## Frozen physical regression and measured improvement
Combined runner19cases/116.825s:18PASS,1FAIL,zero skips. All5isolated history,
4voice/provider/recovery,automatic listening and8screen cases pass. Canvas-X
numbered target expectedDISPATCHED but receivedSCREEN_SESSION_EXPIRED before
gesture. Sanitized log records TYPE_WINDOW_STATE_CHANGED32 clearing the session;
window guards remain strict. Focused unchanged canvas rerun PASS1test/4.700s
(/tmp/t006-history-fifo-canvas-focused-fold3.log). This is not19/19 acceptance;
full nine-screen unchanged rerun is started to test fixture interaction.

Controlled three-local-final comparison on71owner history entries:

| Cycle | Baseline final→terminal ms | FIFO final→terminal ms | FIFO dispatch→terminal ms |
| --- | ---: | ---: | ---: |
| 1 | 1852 | 40 | 34 |
| 2 | 1464 | 41 | 35 |
| 3 | 1639 | 38 | 33 |

Actual STT provider readiness, injected local invalid final, TTS and real native
mic rearm are exercised; no natural speech/action/owner audibility inference.
Record/read continue on background worker and retain original ciphertext/key
after test cleanup. History isolated test proves blocked worker cannot block
terminal, errors remain separate, corrupt blob preserved, cross-instance clear
deletes only dedicated test key, legacy scrub persists, stale publication rejected.

Unchanged nine-screen rerun PASS9tests/81.128s,zero failures/skips
(/tmp/t006-history-fifo-screen-rerun-fold3.log), including canvas-X and actual
expiry. Original mixed19case failure remains recorded, not rewritten as19/19.

Final live YouTube runner PASS1test/6.460s on FIFO app and original final test.
Settings attempts after non-waiting launch failed foreground prerequisite
(0.051/0.061s),no overlay or tap. Test-only assertion message extended with
actual/expected public package/window/display metadata; predicate unchanged,
no retries. Small testAPK+lint PASS108tasks/15s; production APK unchanged.
Latest test unsignedSHAbb9a3db42b54c9ebd8b197c05086df88aafe6e5ecafc42cfd4c943e5b1a346ca;
signedSHAdd78c509b32256d36813b9cd328a06902b69e5bbfecfe0bf4191b477e3272a01,
device hash matches and pm install-r PASS. Explicit `am start -W --display 2
-a android.settings.SETTINGS` waits actual warm launch; final Settings live runner
PASS1test/6.480s (/tmp/t006-history-fifo-settings-focus-diagnostic-fold3.log).
Both live cases retain original visibility/persistence/dispatch/duplicate guards.
Initial earlier first-badge failure remains unexplained; no renderer change.

Final readback: voice health LISTENING / Listening for Hey Hey offline, no
explicitSuppression, enabled DexAccessibilityService preserved. No permission
grant/reset,mic route/model/threshold change, speech/screenshots exported.
D-025 latency slice accepted with measured improvement and remaining intermittent
fixture event explicitly recorded. T-006/T-009/T-010 broad natural-speech/provider/
100-session/resource/soak criteria remain IN_PROGRESS.

## Source publication
Android reviewed17files committed/pushed at
e64ae1d24768ebc268cef4654660559240455bc9;localHEAD/origin/main/remote match and
worktree clean. Canonical docs/evidence publication is represented by Git HEAD;
ignored.env remains
untracked-by-policy and contains no API key at preparation. No generated APK
or signing material committed.
