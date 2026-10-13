> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-010 natural acceptance tooling preparation — 2026-10-10

Owner latest direction: prepare only. Fold3 rediscovered at192.168.1.3:45413;
no physical acceptance trial started. Android owns tooling/test changes; main
owns canonical memory. Production source/app unchanged; no APK install,
permission/role/DPC/history/key change, microphone capture or display change.

Prepared scripts/android/collect-t010-natural.py and test_collect_t010_natural.py
in /home/kunrg/Dex-Assistant-UI. Strict complete-payload metadata allowlist rejects
private suffixes/prefixes and raw transcripts. Captures no audio/screenshots/raw
logcat. Separate run folders avoid cross-run epoch collisions; summary preserves
run boundaries and treats terminals/taps only as candidates, never natural
acceptance. Measures available wake→ready, recognizer endpoint→final/dispatch,
final→dispatch/terminal, resource diagnostics. Recognizer endpoint is not an
independent acoustic endpoint; PSS alone does not prove object release.

Opt-in harmless desktop fixture dynamically selects Samsung desktop; bounded
configurable touch target(default10) and duration(default900s). No injected
speech/gesture. Normal production history policy applies during later natural
trials; fixture neither clears nor substitutes it. Owner ledger needed to
establish natural speech, intended cell and actual outcome.

Validation: Python unittest6/6PASS; JDK17 :app:lintDebug assembleDebugAndroidTest
--offline --max-workers=1 PASS99tasks1m58s, log
/tmp/t010-natural-tooling-build.log. Main repeat PASS99tasks39s. Android build
artifact is not installed or certified as a current matching-signed replacement.
No device fixture run in this preparation; not a natural reliability pass.

Runbook: /home/kunrg/Dex-Assistant-UI/scripts/android/T010-NATURAL-ACCEPTANCE.md.
Next: owner-ready60s idle,10natural trials,60s cleanup baseline; verify original
certificate before replacing only testAPK. Actual display resolution/disconnect
test retains Wi-FiADB; window resize is not display geometry change.
T-010 remains IN_PROGRESS;100natural sessions and actual display-change
acceptance remain open. T-006 owns separate8hsoak. No commit/push requested.
