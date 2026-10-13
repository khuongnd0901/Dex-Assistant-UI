> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Android direct Free-Tier API — local configuration slice (2026-10-09)

## Scope

The owner directed the next implementation slice after approving Android-side entry of Gemini/Groq API keys (no API keys in Git/source; no custom backend/Firebase in the target path).

This slice adds a **Settings UI + Keystore encrypted key storage + authentication-only key probe** in the Android repository. It does **not** switch Command or Chat to direct provider calls; M0–M2 are not accepted end-to-end and the legacy backend remains functional as a rollback.

## Source commits (Android repo)

- \`d453c127\` — \`security/AiCredentialStore.kt\`: independent Gemini/Groq encrypted device credentials, save/load/delete; provider model/enable settings.
- \`936a0e2\` — \`ai/AiKeyProbe.kt\`: Gemini \`GET /v1beta/models\` and Groq \`GET /openai/v1/models\`; uses provider auth headers; no inference or action execution. HTTP 200 / 401 / 403 / 429 / network status distinctions.
- \`44948db\` — \`AiProvidersSettings.kt\`: Compose provider selector, masked key input, save/update/delete, enable switch, model ID and key probe.
- \`05692e0\` — \`MainActivity.kt\`: embeds \`AiProvidersSettings\` within existing Settings dialog; leaves backend configuration in place.
- \`c03108f\` — \`ai/AiKeyProbeTest.kt\`: endpoint and classification unit tests.

## Grounded provider behavior / caveats

Official Gemini list endpoint: https://ai.google.dev/api/models ; official Groq list endpoint: https://console.groq.com/docs/models . This request confirms API authentication but **not** tool calling or Free-Tier eligibility of the selected model; the UI explicitly reports this distinction. No real provider keys or usage available to the GitHub agent for live testing.

Existing Android manifest sets \`android:allowBackup="false"\` and backup/data extraction rule exclusions; the new store is covered by these app-wide rules.

## Validation / evidence status

- Source commits published to Android main, latest \`c03108f\`.
- **Android CI PASS** on commit `c03108f`: https://github.com/khuongnd0901/Dex-Assistant-UI/actions/runs/37874784801 . GitHub Actions ran `./gradlew --no-daemon --build-cache testDebugUnitTest lint assembleDebug`; `Test, lint, and assemble debug APKs` and `Upload APKs` steps succeeded. This establishes host build/tests/lint and debug artifact production only; not a real device or provider test.
- No real provider credentials used; authentication, free model list, test-connection results and Groq/Gemini inference **NOT TESTED**.
- No physical Fold3 / Samsung DeX run by this GitHub-only session. Key persistence after reboot, update/delete behavior, AI voice integration and APK regressions **NOT TESTED**.
- M0 account/provider Free-Tier verification **OPEN**. M1 contract ports/validator **OPEN**. M2 direct Gemini planner inference **OPEN**.
- Important: do not mark M0/M1/M2 DONE on the strength of this UI-only slice.

## Next gate

Once CI is green: add instrumentation using a **test-device-specific isolated store**, verify Settings on a signed Galaxy Z Fold3 with original signing identity and no data wipe. Enter each owner's key manually in Settings; verify auth model listing without inference, restart/reboot persistence, update/delete, errors and secret redaction. Determine tool-compatible free models and implement validated Kotlin planner only after those gates; do not alter production voice, Device Owner, Accessibility or action confirmation until separately accepted.
