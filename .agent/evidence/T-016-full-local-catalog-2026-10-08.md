> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-016 full local catalog UI — 2026-10-08

## Coverage and presentation

- The bundled `commands.json` has 49 command definitions and 183 patterns. Runtime action filtering excludes both Bluetooth definitions (3 patterns); the UI shows 47 supported definitions and all 180 supported phrases.
- Nine intent-oriented groups: applications/windows, Wi-Fi, volume/brightness, playback, YouTube, maps/search, calling, screen/system controls, and short-form video/TikTok. Each command presents a main phrase with alternative phrasings folded beneath it; the full view supports accent-insensitive phrase/group search.
- Any displayed phrase fills manual entry without execution. In short windows, selecting a phrase returns to the filled entry and explicit Run button. Context and permission notes remain attached to commands.

## Validation

- Focused catalog test derived supported definitions/pattern counts from the bundled JSON and asserted every displayed phrase routes LOCAL in its required app context; PASS. It also checked all nine groups, exclusion of unsupported actions, alias/group search and accent-insensitive queries.
- Final JDK17 `./gradlew test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1` PASS 201 tasks (18 executed, 183 up-to-date). App unit tests: 156 each debug/release, zero failures/errors and one expected skip each. Lint and both APKs built.
- Installed the current app/test APKs with `adb.exe -P 5038 -s 127.0.0.1:5555 install -r` on `DeX_Command_Test_API35`, API 35; no data clear. `DashboardLayoutTest` PASS 1/1 at 320×480, 800×400 and 600×320, covering full-list search/list visibility, canonical and alias fill, and no dispatch until Run.
- AVD runs found compact full-mode height collapse at 320×480 and 600×320; moved search inside the bounded list and reserved compact browsing space. Visual review at 1920×1080 led to left-aligned phrase buttons and clearer group headings; a new screenshot was inspected after that fix. Final presentation build `:app:assembleDebug` PASS 38 tasks; reinstalled app and reran `DashboardLayoutTest` PASS 1/1 (3.034 s). Scoped diff check PASS.

## Limit

The AOSP AVD cannot verify Samsung DeX display placement or physical Fold3 behavior. T-016 remains `IN_PROGRESS` for that gate. T-015 worktree changes were preserved.
