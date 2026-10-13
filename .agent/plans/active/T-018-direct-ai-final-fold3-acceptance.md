> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

> Scheduling override 2026-10-13: owner requests Windows simulator tests through WSL during continuation. All remaining physical Z Fold3 execution is consolidated in [T-020](T-020-consolidated-fold3-acceptance.md), one prepared campaign. This overrides older immediate/separate device-run instructions, preserves acceptance gates and does not imply PASS.

# T-018 — One-shot Fold3 acceptance after Android-only AI migration

Status: IN_PROGRESS — owner requested one end-to-end physical device test **after** full source migration; no early physical test evidence is assumed.

## Preconditions

1. Android latest main `testDebugUnitTest lint assembleDebug` PASS and matching signature device-test app APK available (certificate SHA256 `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`). Never install arbitrary GitHub CI debug APK over device-test-signed production Device Owner installation.
2. `adb devices -l` physical `SM-F926B` API35 device; Samsung DeX external display 2 present, charging/mic setup stable. Capture current package/signing/Assistant role/Accessibility/DPC and app data presence without showing secrets.
3. The owner enters provider keys **only through Android Settings**, with verified eligible free-tier model; no API keys typed into ADB, screenshots, logs, shell history or Git. Paid models disabled.
4. Do not uninstall/clear-data, rotate DPC keys, reset Assistant role, toggle Wi-Fi unexpectedly, change Android signing identity or execute real phone calls/SMS. Network-off tests only when wireless ADB is not the sole device connection.

## One controlled acceptance pass

1. Preflight: check role/accessibility/service and active command catalog, record installed APK certificate and current app data retention indicators.
2. Update app **in place** only with matching signing certificate. Ensure permissions and original runtime service states retained; if not, mark FAIL and stop.
3. Settings: test provider selection, model discovery and free-tier note; save/update/delete/re-add a dedicated test key locally, no backend endpoint visible, expected state persists across restart. Do not expose key contents.
4. Local commands with cloud unavailable: deterministic volume setting or harmless app launch -> local only, never API; exercise unsupported/private phrase -> local rejection.
5. Direct Command Gemini verified model: harmless simple and composite command, bounded valid tool calls through ActionRouter; invalid tools/capabilities fail closed. If Groq verified free, test transient Gemini 429 via **mock/stub** and one live Groq normal request. Don't deliberately exhaust quota.
6. Chat: say 'bắt đầu trò chuyện', speak three harmless Vietnamese turns requiring earlier context, then 'kết thúc trò chuyện'. Repeat using UI End Chat during in-flight response; verify no late TTS, no device action and no requirement for Spring Boot.
7. Faults: temporarily invalid credential (then restore), no Internet/timeout (wired ADB only), 429 via stub where possible, session 15min idle via deterministic unit clock tests; no account-billing risk. Verify failure/speech re-arm, no auto-replay.
8. Soak/latency: collect ≥30 natural samples per active route (local, direct Command, direct Chat), p50/p95, terminal errors and app UI latency. Count spoken user-confirmation wait separately; 100-session stability only if time permits and requested benchmark gate, never fake counts.
9. Postflight: assert no unauthorized Device Owner, signing, permission, service, Assistant role or saved data differences. Restore keys/model preferences if test changed. Report PASS/FAIL/SKIP per case with evidence and commit+device serial redacted.

## Exit criteria

- At least local, direct Command, direct Chat, cancel/error paths and rollback-free operation on **physical** Z Fold3 proven; required paths no backend WebSocket or server.
- Test evidence is from real ADB/device, not a GitHub runner/emulator. Clear user-visible failure reason for each skipped or failed case.
- Do NOT stop/delete live AWS EC2 or EBS/DNS until physical PASS and explicit authorization. Backend Git repository remains retained as source history.

## Codex local execution instruction

Read `.agent/STATUS.md`, this plan and Android `.agent/STATUS.md`. Use one controlled ADB session against the physical Fold3 after confirming CI and matching signing cert. No uninstall/data wipe or Assistant/DPC/Accessibility mutations. Capture only redacted evidence. Do not mark T-018 DONE without physical data. Patch only verified regressions, re-run host CI, then perform one final physical acceptance. Commit sanitized evidence and exact results.

## Progress and findings — 2026-10-10

- Owner resumed T-018 and authorized local `.env` Gemini credential/model for tests. Only model and configured/not-configured metadata exposed. Model `gemini-3.5-flash-lite`: authenticated model GET HTTP200; one harmless host generateContent HTTP200/STOP/text, 1220ms. Host probe is not physical Android acceptance. Google pricing accessed2026-10-10 lists Free Tier: https://ai.google.dev/gemini-api/docs/pricing.
- Android main HEAD `49cb5ad`, clean working tree at start; active controller constructs MobileAiProxy and DirectAiChat. JDK17 test/lint/app+test APK running. Physical SM-F926B/API35/DeX display2 present; Assistant role and Accessibility configured. Installed app backup/signature verification pending. Wireless ADB only: network-off skipped.
- Credential entry on device remains Settings-only per precondition3; awaiting owner readiness while independent checks continue.

- Owner explicitly overrides Settings-only/no-ADB credential entry for this test on2026-10-10: use `.env` key through ADB in trusted environment. Entry will use stdin, masked Settings field and normal Keystore save; no secret in logs/Git/test artifacts.
- Initial full host command FAILED instrumentation compilation: retired disconnect/provision API fixtures and dashboard signature drift. Android agent repairing test sources before physical install. Installed original APK backup complete; certificate matches required9ddb7f47…abd83d03.

- Owner selects automatic-only execution this turn. Natural mic/TTS/voice Chat and30natural samples per route are deferred, not accepted or removed from overall T-018 gates.

- Owner raised test request budget to15RPM (local `.env` DEX_GEMINI_TEST_RPM=15). Subsequent fixtures use validated providerTestRpm=15 and>=4100ms spacing; already-running Settings keeps prior2RPM. This sets client test pacing, not provider-account quota.

## Verified automatic milestone — 2026-10-10

- [x] Migration source baseline49cb5ad; full JDK17 host checks PASS201tasks. Test-only migration regressions repaired; Android commit5c6b114 local/not pushed.
- [x] Original signing certificate verified; app installed-r, private original backup, owner state/postflight retained. Private credential input deleted.
- [x] Selected12automatic physical cases PASS; Settings lifecycle/model discovery/persistence, local with cloud disabled, Gemini simple/two-action compound, idempotent Wi-Fi ON, bounded3turn text Chat/cancel, invalid credential/no repeated callback/restart.
- [ ] Full task exit: natural mic/TTS/UI End Chat during confirmed in-flight response,30natural samples/latency/resources, Groq/429/auth transport/network faults, Wi-Fi cycle. Owner explicitly deferred natural testing. Immediate text cancellation has a narrower claim.

Exact commands, outcomes, original fixture failures and postflight limitations: [T-018 evidence](../../evidence/T-018-direct-ai-fold3-2026-10-10.md). Latest full host baseline201tasks19s; final affected lint/testAPK108tasks16s. Client test pacing15RPM/4100ms; credential never in Git/test args/logs. Task remains IN_PROGRESS; next deterministic faults followed by owner-assisted physical acceptance. No cloud operations performed.

## Gemini-only follow-up — 2026-10-10

Owner confirms only Gemini currently available and requests Gemini testing. Provider preflight proves default/enabled/free-confirmed Gemini with saved encrypted key, model `gemini-3.5-flash-lite`; other providers neither enabled nor credential-configured. Repeat selected3live cases on physical Fold3 at15RPM using installed matching-cert fixtures. No app/source/credential-entry changes required. Groq/fallback acceptance is conditional future-provider work, not a blocker for current Gemini-only scope. Natural testing remains deferred per owner instruction. Results: selected3/3PASS61.312s; local/cloud-disabled +direct simple/two-step compound,3turn context/immediate cancel, invalid credential/no replay/restart. Provider settings/voice policy unchanged, key retained, role/Accessibility retained, history present25308bytes. No source/build/install changes. See Gemini-only repeat section in evidence; T-018 overall IN_PROGRESS.


## Windows simulator continuation — 2026-10-13

Continuation 2026-10-13: simulator offline faults/cancellation/expiry/isolation PASS9/9; full JDK17 test/lint/app+test APK PASS201tasks (app191tests/variant). HTTP429/auth/timeout no-replay and confirmed in-flight stale-result/restart gates now have deterministic evidence. No new live provider/physical acceptance. [Evidence](../../evidence/Android-simulator-continuation-2026-10-13.md).
