> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-006 YouTube search correction

Owner reports “tìm nhạc Sơn Tùng” returns “Đã gửi yêu cầu” but leaves
YouTube on its previous screen. Existing local executor used ACTION_SEARCH;
its old device test checked dispatch history only, not actual search UI.

## Baseline / diagnosis

Original production installed during comparison:

| Intent variant | Visible query check | Duration | Limitation |
| --- | --- | --- | --- |
| Legacy SEARCH, default routing | FAIL | 15.282s | YouTube visibility inconclusive |
| Legacy SEARCH, explicit current desktop | FAIL | 15.833s | YouTube visible, query absent |
| Results VIEW, explicit current desktop | PASS | 0.585s | Exact query visible; not playback/result relevance |

Earlier manual VIEW trial was reported unchanged by owner. Later instrumented
fixture explicitly targeting current desktop passed, so do not generalize
from launch success alone. Manual USB reconnect changed display IDs; old
YouTube tasks remained in activity dumps. No hardcoded display added to app.

## Localized implementation

Android LocalActionExecutor.youtube now uses package-bound ACTION_VIEW to
`https://www.youtube.com/results?search_query=…`. YoutubeSearchIntents encodes
the entire normalized query with UTF8; spaces %20, literal plus %2B, reserved
characters stay inside one parameter. Browser intent only when YouTube package
is absent. Installed-app launch failure remains FAILED. No autoplay, parser,
history, microphone, protocol or production display-selection changes.

Tests: two JVM query retention/escaping cases; Android intent contract; opt-in
warm YouTube device regression invokes production executor using ordinary
application context, with no display override on execution. Warm prerequisite
launch selects current desktop. Exact fixture query must be initially absent
and become visible to pass. Screen text stays in RAM; metadata-only results.
Does not prove content relevance or video playback.

Frozen JDK17 `test lint assembleDebug assembleDebugAndroidTest` PASS201tasks/51s,
66debug+66release units, zero failures/errors/skips; diff check PASS.
Build log `/tmp/t006-youtube-view-fix-final-gradle.log`.

Unsigned app SHA62335a23d69b173cc7d84bfb42971e773b8bd30842fbf68541873f87990a5bd7;
unsigned test SHAf45e2fc8d5c8bd1e929842bc57606e6b7af9cc7f56cdc2d20d59f92fafb1fcc2.
Original-cert signed appSHAaf1b81062504d0e4356f0a8f82e0853dbb07d9850b1d7e50a866396432989af5;
testSHA70b85bc03dae20203dd20db0b65d431cd91787d974cb815d5e977506eab978c4.
Certificate SHA2569ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03.

## Final device validation

Owner resumed after pause; ADB192.168.1.18:41261 reconnected. Current desktop
Desktop/display2; physical HDMI/display6. Matching-original-cert app/test
install-r PASS; installed app hash matches signed app. Protected owner
assistant_config/device_credential preference presence unchanged (both absent).
No permission resets, data clears, microphone routes or thresholds changed.

Device intent contract PASS1test/0.011s. Actual production executor using normal
application context, YouTube already open, no execution display override:

- Distinct fixture “tìm video nhạc Sơn Tùng”: PASS1test/1.382s. General-display
  diagnostic metadata omitted hidden Desktop2; target2 assertion did execute.
- Owner exact fixture “tìm nhạc Sơn Tùng”: PASS1test/0.496s. Corrected metadata
  explicitly shows targetDesktop2 before visible/query0, after visible/query1.

Test-only reporting was corrected to include selected target even if absent
from general display enumeration; predicates unchanged. Final test-only lint/
assembleDebugAndroidTest PASS108tasks/20s; unsigned test finalSHA
833ffd9f357470a03444ca90d73d6e68634c7ddf497733e55fe175854a305c17.
Only test APK replaced for this reporting correction; installed app unchanged.

This validates actual search query UI updates for the warm local executor path;
not natural spoken STT latency, result relevance, autoplay, or all YouTube/device
versions. Browser-only fallback not physically tested by uninstalling YouTube.
Read-only microphone observer built but not run; USB investigation remains
owner-deferred. Broader T-006 acceptance remains IN_PROGRESS.

Owner explicitly requests committing/pushing all related changes in canonical
and Android repositories. Saved full voice-ui-migration.md A–J plan remains
392lines and deferred; no broad architectural migration implemented.
