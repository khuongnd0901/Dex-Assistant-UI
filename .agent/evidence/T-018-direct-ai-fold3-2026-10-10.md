> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-018 — automatic Fold3 direct-AI acceptance, 2026-10-10

Overall task: **IN_PROGRESS**. Owner selected automatic-only testing; the selected automatic paths passed after test-fixture repairs. Natural voice/TTS and full acceptance populations remain open.

## Verified outcome

Physical Samsung SM-F926B/API35, Samsung DeX display2 (1920×1080); serial redacted. Android production source baseline `49cb5ad` uses MobileAiProxy Command and DirectAiChat without active Spring Boot/WebSocket construction. Matching-certificate app installed in place with `install --no-incremental -r`; no uninstall/data clear/grant/role/DPC changes. Only test fixtures and read-only preflight script changed in Android source during this task. Android fixture/status commit `5c6b114a10a8a467b2745eb5224c9c67ccd9493c` is local, not pushed.

| Selected physical case | Result | Evidence/limit |
| --- | --- | --- |
| Seven safe ConfigCommandDeviceTest guards | PASS7/7,0.811s | Catalog/query, malformed parameters, matcher, TTS/cancel/duplicate/desktop guards; no real call/SMS |
| Provider Settings lifecycle | PASS1/1,12.246s | Actual UI save/update/delete/readd, Keystore equality/cipher changes, model API discovery/selection, authentication, enable/free-confirm, Activity recreation/persistence |
| Local volume with cloud providers disabled | PASS | LOCAL/VERIFIED, exact40% stream value; original enable flags restored before direct calls |
| Direct simple volume30% | PASS | Real Gemini, guarded ActionRouter, VERIFIED and actual platform-rounded value |
| Direct compound20%→60% | PASS | Two executionLatency events for the same request, final60% VERIFIED; final scoped volume fixture1/1,12.349s |
| Local Wi-Fi idempotent enable | PASS | Existing Device Owner bound, LOCAL/VERIFIED, radio alreadyON and staysON; scoped volume+WiFi2/2,11.613s; no off/oncycle |
| Direct text Chat/context/cancel | PASS | Three real Vietnamese turns retain random marker; bounded replies/no action history; immediate end yields no late event over25s |
| Invalid credential/error/restart | PASS | Single Error, no repeated event over2s, no action history, original credential restored and session restarts |

Chat/error cases passed in3-case suite (2PASS/1fixtureFAIL),58.715s; repaired volume subsequently passed. Together these represent12 distinct selected automatic cases, not12 natural sessions. Tests use unique isolated histories and preserve owner voice policy. Initial media volume is restored in fixture cleanup.

## Provider configuration and rate

Owner explicitly authorized key/model from local `.env` and ADB transfer, overriding the original Settings-only/no-ADB input rule for this test. Credentials were sent through stdin into a chmod600 private app input, then entered via actual masked Settings UI and normal Keystore save. Private input was deleted. No credential values entered tool outputs, test arguments, source or Git. Gemini configuration intentionally remains enabled for owner testing.

Model: `gemini-3.5-flash-lite`. Authenticated host model GET HTTP200; one harmless host inference HTTP200/STOP/text,1220ms. Host probes are separate from physical acceptance. Google official pricing accessed2026-10-10 lists model Free Tier: https://ai.google.dev/gemini-api/docs/pricing. Provider account billing tier was not independently inspected.

Owner raised local test budget from2 to15RPM: `.env` DEX_GEMINI_TEST_RPM=15 (Git-ignored). Test argument `providerTestRpm=15` is validated1..15 and spaces requests at least4100ms. This changes client test pacing, not account quota. Earlier Settings baseline passed1/1 in65.52s at2RPM.

## Host validation and repaired failures

JDK17/SDK35: `./gradlew test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1` PASS201tasks (latest full baseline19s). App176tests per debug/release,0failures/errors,1expected skip each; DPC5per variant PASS. DirectAiChat9/9 includes deterministic15min boundary. Final affected lint/testAPK command PASS108tasks16s. `git diff --check` and preflight `bash -n` PASS.

First full host run failed instrumentation compilation on removed disconnect/provision APIs and old dashboard signature. Obsolete backend-only fixtures explicitly retired; retained local/dashboard fixtures repaired. Preflight `cmd role holders` was unsupported on Fold3; corrected to `get-role-holders --user0`.

First Settings physical failure: coroutine TestScope ServiceLoader under `--no-restart`; actual renderer semantics fixture avoids that test-runtime dependency. Second failure: dropdown text label lacked clickable ancestor. First15RPM lifecycle failure: stale checkbox semantics after saving the same model reset Free Tier confirmation. Waits now require settled persisted and UI states. First direct-volume failure: test bankers rounding disagreed with production roundToInt. All are retained failures; no production behavior was relaxed to pass. Same20%→20% compound was deduplicated, so the final test uses distinct20%→60% and confirms two execution steps.

## Postflight

- Installed original and new app certificate SHA256 `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`; original APK privately retained.
- Installed app SHA256 matches the built/signed APK. MainActivity postflight shows LISTENING/DeX connected/Accessibility ready.
- Assistant role retained; Accessibility enabled/bound; owner voice_policy byte-identical. App permission grants, Device Owner descriptors, first installation/data inodes retained.
- Owner history retained (25308bytes), ciphertext fingerprint differs after normal history reads. Full plaintext equality was not measured. Fixtures write only isolated histories; owner history is never cleared.
- Temporary credential input deleted; owner Gemini key/model enable/free-confirm remain intentionally configured. Invalid-key and temporary provider-disable tests restore values.
- No real call/SMS, Wi-Fi disable, DPC/signing rotation or cloud stop/delete. Private raw logs/APKs stay under `/tmp/t018-*` and existing local signing storage.

## Remaining acceptance

Owner deferred natural microphone/voice routing, audible TTS and UI End Chat during a confirmed in-flight response. Immediate text cancellation does not prove the request reached the remote endpoint.30natural samples per route/p50/p95/resources,100sessions/soak remain unmeasured. Groq has no configured test credential; mocked429 fallback, authentication no-fallback transport, Internet-off/timeout and Wi-Fi off/oncycle remain open. Wireless ADB is the sole connection. Deterministic state idle test does not establish full service idle teardown. Legacy history route label BACKEND_REQUIRED describes direct AI requests in existing source; it is not evidence of a backend call.

Next: add deterministic transport/cancellation faults, then owner-assisted voice/UI/provider acceptance and measured populations; retain T-018 IN_PROGRESS.

## Gemini-only repeat — owner follow-up, 2026-10-10

Owner confirms Gemini is the only currently available provider. Device preflight verified default/enabled/free-confirmed Gemini, encrypted key present and model `gemini-3.5-flash-lite`; Groq/OpenAI/Grok/DeepSeek were neither enabled nor key-configured. Groq/fallback is conditional future-provider work, not a blocker for this Gemini-only test scope.

Selected physical repeat **PASS3/3 in61.312s**, using Android commit5c6b114 fixtures already installed and providerTestRpm15 (4100ms spacing):

- Local40% with cloud adapters disabled; real Gemini direct30%; distinct20%→60% Command. All VERIFIED, final volume checked; compound emitted two guarded executionLatency metrics for the same request. Initial volume/provider enables restored.
- Three real Gemini Vietnamese text Chat turns retain context marker; immediate end suppresses late events25s, no action history.
- Temporarily invalid Gemini credential produces one error/no2s automatic replay; original key restored, session restart and subsequent live replies succeed.

Runner: `adb -s <physical-serial> shell am instrument --no-restart -w -r -e class com.khuongnd.dexassistant.ui.DirectAiAcceptanceDeviceTest#configuredDirectCommandsSetVolumeAndRestoreOwnerVolume,com.khuongnd.dexassistant.ui.DirectAiAcceptanceDeviceTest#configuredProviderRetainsThreeTurnsAndEndSuppressesLateReply,com.khuongnd.dexassistant.ui.DirectAiAcceptanceDeviceTest#invalidCredentialFailsOnceAndCanRestart -e runDirectAiAcceptance true -e providerTestRpm 15 com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner`.

Postflight: exact provider settings and voice-policy fingerprint unchanged, Gemini key retained and no other provider key configured, owner history present25308bytes, Assistant role retained and Accessibility bound. MainActivity on display2 retained. No install/source/credential-input changes in this repeat; Android working tree clean. Private log `/tmp/t018-gemini-only-device.log`; snapshots under `/tmp/t018-gemini-only-*.json`. These are repeated cases, not additional unique cases/natural samples. Automatic-only preference remains; no microphone/TTS/latency-population claim. T-018 overall IN_PROGRESS for current Gemini faults and natural acceptance.
