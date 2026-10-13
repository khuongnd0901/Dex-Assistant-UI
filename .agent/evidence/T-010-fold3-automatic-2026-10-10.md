> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-010 automatic Fold3 continuation — 2026-10-10

Owner cancels T-008 and prioritizes T-010. Previous automatic-only preference
retained; natural acceptance is separate. Canonical Android source checkout:
`/home/kunrg/Dex-Assistant-UI`, baseline `5c6b114`; physical Fold3 SM-F926B/API35
ADB `192.168.1.3:35889`. Android owns test changes; main owns this evidence.

## Change and verification

Added one test-only physical regression in `NumberedScreenDeviceTest.kt`.
Actual service display callbacks preserve a valid grid during unrelated phone
callbacks, invalidate simulated frozen-session geometry/identity mismatches,
release overlays and reject a subsequent cell tap without dispatching a gesture.
Reflection changes only the test session snapshot; desktop configuration is
untouched. This is not actual display resize/disconnection acceptance.

JDK17 `./gradlew test lint assembleDebug assembleDebugAndroidTest --offline
--max-workers=1`: PASS201 tasks,21s. App176 tests per debug/release variant,
zero failures/errors and1 expected skip each. Log `/tmp/t010-oct10-build.log`.
Existing original test certificate verified before signing/install-r:
`9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
Only updated testAPK installed; production app/source unchanged.
TestAPK SHA256 `65cd29e0e9749cb9d03f2ea5eff2d01697657c7d4c870b1b5c2c534de28791bf`.

Selected current-source `am instrument --no-restart -w -r` Fold3 run:
**4/4 PASS,54.089s**, each test completion status0, no skips/failures:

- `displayCallbacksPreserveValidGridAndRejectStaleGeometryOrIdentityWithoutTap`
- `voiceCoordinatorGridSubdivisionSurvivesFreshPreparationAndCompactFinals`
- `freshGridSurvivesContentAndWindowChangesThenDispatchesOnce`
- `changedWindowAndRealSessionExpiryCleanUpWithoutTap`

This verifies actual fixture coordinate dispatch/consume-once, subdivision
continuity through the coordinator, popup/window invalidation and real35.5s
expiry without stale tap. Raw log `/tmp/t010-oct10-current-device.log`.

Initial signing attempt lacked Java on PATH and failed before install. A
premature invocation used the old installed testAPK and silently omitted the
new method:3 existing cases PASS52.526s; it does not validate the added test.
Earlier isolated existing content/window case PASS1/4.141s. Logs retained in
`/tmp/t010-oct10-device.log` and `/tmp/t010-oct10-content.log`; final4case run
above is the accepted current fixture evidence.

## Preservation and remaining acceptance

No reset/uninstall, shell permission grant, key/provider edit or DPC action.
Original Assistant role and Device Owner retained; Accessibility enabled/bound.
Voice policy hash matches previous baseline. History exists25308bytes matching
previous size, but no current preflight hash or plaintext comparison was taken;
full history equality is not claimed. Coordinator fixture uses disposable history.
Both repositories pass CRLF-aware diff checks. Private logs/APKs remain outside Git.

T-010 stays IN_PROGRESS. Actual DeX resize/disconnect, real app/popup natural
flow,100 natural wake/number/tap sessions and accepted latency/resources/soak
population remain open. Existing synthetic100cycle metrics are not natural
reliability evidence. Sole wireless ADB was not disconnected. Next: independent
control path for real display-change testing and owner-assisted natural sessions.
