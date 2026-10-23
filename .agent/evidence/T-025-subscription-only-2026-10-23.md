# T-025 subscription-only continuation — 2026-10-23

IN_PROGRESS. Owner requests latest source and continuation from existing subscription branch. Fetched origin; selected feat/chatgpt-subscription-migration at ba054d3 in isolated worktree `/home/khuongnd/worktrees/dex-subscription`. Original main at996d7ac and its pre-existing dirty/EOL changes remain untouched. No new task/parallel implementation was retained.

## Changes
- Coordinator calls independent subscription consumers directly for Command and Chat; local route/action confirmation/session epochs remain intact.
- Removed Gemini/Groq HTTP inference, API-key/provider editor and probes, API-key store code and unused provider/free-tier/direct-mode controls. Existing encrypted legacy keys/preferences remain on disk untouched; no runtime reads/deletes. Saved legacy default requires explicit Subscription setup/selection and account/model proofs.
- Reused native OAuth/model/Responses code from the branch; shared SSE transport gains sanitized request/first-byte/body metrics. Native namespace schemas and one-scope immutable schema cache replace provider-specific formats.
- Retired API-key feature tests; preserved session-boundary tests and ported typed validation, schema cache, transport failure/cancellation, coordinator cancellation/late-reply/idle/history isolation, settings/data preservation and component benchmark to subscription. The new benchmark series is not comparable to prior Gemini T-021 timings.
- Bilingual README/current BRD/SRS and D-040 describe the resulting connection/setup/cost contract. Prior evidence and Fold3 acceptance are retained as history.

## Validation
Final checks PASS. JDK17 `/home/khuongnd/.local/opt/jdk17`, Linux SDK `/home/khuongnd/Android/Sdk`. First build stopped at missing ignored voice assets in the new worktree. Copied existing cached archives and verified their pinned hashes with `python3 scripts/prepare-voice-assets.py --offline`; no dependency or model update.

Windows simulator selector starts the repository AVD; confirmed Windows ADB port5038,127.0.0.1:5555,API35. Initial launcher terminal remains pending; a second selector resolved the booted target. No physical device install/test, owner sign-in, live provider or real-account preference/token change has been attempted.


Final host command (2026-10-23):

```bash
JAVA_HOME=/home/khuongnd/.local/opt/jdk17 ANDROID_HOME=/home/khuongnd/Android/Sdk \
  bash ./gradlew test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=2 --no-watch-fs
```

PASS201tasks,20s (23executed/178up-to-date). App219tests/debug and release,
0failures/errors,1conditional skip each; DPC5/variant,0failures/errors/skips.
Lint0errors/32existing app warnings. Log `/tmp/dex-subscription-build.log`.
One intermediate test-APK compile rejected imports of Compose member assertions;
removed those imports and final full checks pass. No production workaround.

Final API35 Windows AVD: app/test installed in place (`install -r`), both Success;
no uninstall/reset/grant/role changes. Selected target from repository selector:
`ADB=/mnt/d/Android/Sdk/platform-tools/adb.exe PORT=5038 SERIAL=127.0.0.1:5555`.
Final single instrumentation batch PASS13/13,14.309s,0failures/skips:

- SubscriptionLifecycleDeviceTest:4 (HTTP cancel/restart,late completion,pre-turn idle,sensitive input/no request).
- SubscriptionSettingsDeviceTest:2 (real rendered subscription-only UI; legacy prefs/ciphertext preservation and scoped proof invalidation).
- ChatGptSubscriptionOfflineDeviceTest:3 (Android JSON/SSE,isolated Keystore restoration/logout,unproven model/activation guards).
- ResponsePerformanceDeviceTest:2 (360synthetic subscription Command samples/discovery and1500synthetic native wake frames; no natural/performance-improvement claim).
- LocalFirstDeviceTest:2 selected methods, `localVolumeExecutesWithoutBackend` and `privateCommandIsRejectedLocallyAndHistoryIsRedacted`.

LocalFirst initially failed2/2 because its helper searched removed historical UI controls
(`command`, `Vietnamese command`, `Run command`). Updated only the helper to the current
`commandInput`/`runCommand` tags. Focused rerun2/2PASS8.189s; final combined13/13PASS.
The production local route was unchanged. Instrumentation logs:
`/tmp/dex-subscription-avd.log`, `/tmp/dex-subscription-local-avd.log`,
`/tmp/dex-subscription-avd-final.log` (final candidate).

Whitespace PASS. Repository memory/link check PASS145Markdown/471local links.
Runtime scan finds no AiProvider/AiCredentialStore/MobileAiProxy/DirectAiChat or
Gemini/Groq/xAI/DeepSeek API endpoints under main source. Remaining public network
endpoints in ai/auth are official auth/JWKS/models/Responses only. No generated assets,
APKs, credentials or logs are added to Git. Owner subsequently authorized commit/push on the existing feature branch;
publication is being prepared. No merge or physical acceptance claim.

Candidate identity (SHA256):

| Artifact | SHA256 |
| --- | --- |
| app/src snapshot | `5e26dd6eb3bc3aa65a67c2f9fc74492cb2d89d3ec5203dffa2b534c14b09b3c5` |
| app-debug.apk | `ba03bb0164d390b3034f5835b9ba10d15fa4a8c0ba0fe46650c5df151c083a3f` |
| app-debug-androidTest.apk | `ceb6f241971d4846e51dcbb9554bca226605bbb3a49a3815814bd3bccefcc255` |

Source snapshot hashes sorted existing tracked/untracked nonignored `app/src` files
as `relativePath + NUL + fileBytes + NUL`; deleted files excluded. APKs live in ignored
build outputs and copied staging under original main's ignored `.cache/subscription-validation`.

## Remaining gates
Original-signed physical install/regression, owner explicit selection, physical logout/reconnect and natural voice/TTS/interrupt/confirmation/performance/soak remain open through T-020. No merge/release/DONE claim. Official subscription documentation consulted: [registration](https://developers.openai.com/siwc/token-sharing-open-source/sign-in), [inference](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference), [preview limits](https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations). Existing native implementation retained; no custom Codex credential copying or private endpoint.
