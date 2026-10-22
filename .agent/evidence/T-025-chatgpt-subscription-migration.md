# T-025 — ChatGPT Subscription migration evidence

Date: 2026-10-22. Status: IN_PROGRESS; subscription device commands/default activation BLOCKED pending target runtime acceptance. No OAuth secrets, callback queries, prompts from the owner, or private account information captured.

## Eligibility and references

GitHub connected read independently reports `khuongnd0901/Dex-Assistant-UI` PUBLIC, default main. Root MIT and D-038 apply. The user's private-target premise is outdated; importing the POC does not transfer consent. Official pages read 2026-10-22:

- [Quickstart](https://developers.openai.com/siwc/quickstart): OSS plan-usage integration available; private commercial identity eligibility is distinct.
- [Overview](https://developers.openai.com/siwc/token-sharing-open-source): local host identity/registration boundaries.
- [Registration](https://developers.openai.com/siwc/token-sharing-open-source/sign-in): dynamic_agent_client, issued ID, PKCE/nonce/state/loopback/OIDC/scopes.
- [Sessions](https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions): refresh rotation, local protected tokens, renewable-session revocation.
- [Models/inference](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference): models[].visibility/slug/display_name, public Responses SSE and terminal completion.
- [Preview limits](https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations): namespace function/custom tools; explicit bounded history; no previous_response_id/temperature/max_output_tokens.
- [Function calling](https://developers.openai.com/api/docs/guides/function-calling): namespace envelope and function parameters.

Reference source: local POC commit `2c67aacd06fdc572e6305ea016667daafe9d84a6`, MIT notice retained in `third-party/siwc-poc-LICENSE`. Inspected SiwcAuth, SecureStore, OidcVerifier, HttpClient, MainActivity/report. Checked-in report records Fold3 success, but none of its account/device results is accepted as target-app evidence. Reference repository remains unmodified.

## M0 migration matrix / architecture

| Capability | Existing flow and constraints | Migration implementation / gate |
| --- | --- | --- |
| Local Command | CommandRouter deterministic catalog → TypedAction → ActionRouter → native/DPC/scoped Accessibility | Unchanged; never invokes subscription, including offline/local failures |
| AI Command | AssistantCoordinator → MobileAiProxy → Gemini/Groq function calls → AiToolCallParser → typed guarded router | Dedicated subscription branch/shared transport; namespace schemas reuse AiToolSchema and common typed validator; device execution code-gated until real tools proven |
| AI Chat | DirectChatState → DirectAiChat → Gemini/Groq text → complete history → Android TTS | Subscription adapter sends explicit user/assistant history, no tools; final validated response only; production selection gated |
| Voice Command | Wake/STT → coordinator command route → interaction/confirmation/action → TTS | Same route, request IDs, cancellation, 30s watchdog/120s total and stale guards; no auth/browser launch from voice |
| Voice Chat | Existing explicit Chat mode, follow-up after TTS, stop/idle/epoch guards | Same lifecycle and TTS; no action dispatcher in Chat; natural voice acceptance still pending |
| Selection | AiProviderSettings default + enabled/model/free-tier confirmation | New CHATGPT_SUBSCRIPTION identity, distinct OPENAI API-key label; no default change; subscriber auth bypasses BYOK/free-tier candidate loop |
| Credentials | AiCredentialStore AES-GCM keys per provider; separate legacy bearer | New encrypted atomic noBackup record + dedicated Keystore alias; no old key mutation/token import; own stable host/issued ID |
| Cancellation | Future + active HTTP cancellation, requestId/stale response check | Per-consumer operation cancellation fences auth-to-HTTP races; no stream reconnect/action replay; preference changes cancel active AI sessions/plans |
| History | Command encrypted bounded durable; Chat volatile 20 pairs/24KB approximate context, 200 turns, 15min idle, 2000 input/4000 output code points | Existing state retained; only completed final Chat text committed; partial output never history; no previous_response_id |
| Errors/fallback | Legacy Gemini/Groq eligibility and bounded fallback; unsupported/credential/malformed fail closed | Subscription pre-stream401 refresh at most once;403/429/5xx explicit failures; no subscription-to-BYOK fallback; truncated/failed/incomplete SSE never successful |

Current production route remains legacy until acceptance. Implemented proposed route is Android → own OAuth installation → public models/Responses SSE → text or structured validated typed plan → existing consumers. No backend/CLI/proxy/internal API layer exists.

## Authentication architecture

Adapted POC PKCE S256, state/nonce and RS256 JWKS checks (issuer/audience/azp/expiry/iat/nbf/subject). Browser uses HTTP127.0.0.1 ephemeral port and fixed /auth/callback; listener starts first and bounds request lines/time. Issued client persisted before token exchange. Successful identity/scope validation precedes session replacement. Re-login validates the saved subject. Single saved account/workspace mapping is supported; additional account picker remains a limitation.

AES-GCM record uses DeX-only Android Keystore alias and AtomicFile under noBackupFilesDir. Existing all-domain backup exclusions are unchanged. Host/registration survive logout; tokens do not. Refresh lock serializes rotating-token writes and ignores obsolete401 tokens after another refresh. Login/logout generation fences prevent resurrection. Revocation uses discovery's HTTPS endpoint, retries one transient failure after200ms and never redirects; failure still clears local tokens and displays unconfirmed remote revocation. No token/URL/body/provider exception is displayed or logged.

## Provider / Chat / command implementation

Shared ChatGptResponsesClient uses OAuth bearer, fixed public endpoint, store=false/stream=true and instructions/input. Incremental bounded SSE rejects malformed/truncated/incomplete/failed results; completed final response/status is authoritative. Limits:20s inference call/15s read/5s connect,262144-byte line/event,2MiB stream,10000events,65536delta characters,4000Chat code points. Cancel operations interrupt active inference calls and fence results; an in-progress shared refresh remains bounded by30s auth HTTP timeout.

Chat sends bounded existing history and local time; no tools, action output rejected. Existing final Reply/TTS interface has no provisional UI event, so streaming display is not wired; incomplete output never reaches TTS/history. 1024token cap cannot be hard-enforced by unsupported max_output_tokens; instructions request brevity and local4000code-point cap rejects excess.

SubscriptionTools translates existing capability/app-filtered declarations into a `device` namespace. Only completed function_call items with namespace, unique call_id, supported function name and valid typed arguments are accepted. Existing decode enforces schema1/allowlists/params; maximum8actions. No generated text becomes an action. Tools can be built/tested offline, but real output namespace/model behavior must be checked before enabling commands. Release constants keep both device commands and default activation false; settings cannot override them. No action/router executor implementation was replaced.

Settings include status, deliberate browser sign-in, verified account email, scope status, visible account-model list/selection, harmless inference test, nonexecuting real-schema function-call probe, cancellation, usage link and local/remote logout feedback. Available states include all eight requested states. No API-key entry for subscription. Legacy providers/keys unchanged.

## Validation / device status

Clean-checkout JDK17 validation PASS: `./gradlew test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1 --no-watch-fs`. Final run201tasks (26executed/175up-to-date),1m53s, after initial clean201-task run6m14s. App226tests per debug/release variant,0failures/errors/1conditional skip each; DPC5tests per variant PASS. Lint0errors/fatal, app32/DPC2existing warnings. New tests cover OIDC, state/scopes/rotation/restoration/logout/concurrent refresh and logout fencing, bounded history/SSE/tools/401/403/429/5xx/offline/replay rejection. Offline Android fixture compiled/packaged; execution still pending.

Initial overlapping workspace builds produced Kotlin incremental-cache/internal compiler failures; both were discarded as evidence and replaced by the single-worker isolated checkout `/tmp/dex-t025-validation`, excluding unrelated dirty sources. Host logs are `/tmp/dex-t025-validation.log` and `/tmp/dex-t025-final-validation.log`. Whitespace PASS. Clean migration memory/link validation PASS144Markdown/473links; original workspace retains its pre-existing missing T-017 recovery-evidence link. Twelve unrelated tracked patches compared byte-for-byte unchanged.

Original9ddb signing identity is available locally. Candidate `/tmp/dex-t025-artifacts/dex-assistant-subscription.apk` was re-signed and verified with that original certificate; SHA256 `617de1c0d7bccfd0fc6454787ff9814506914fb5844678272858f8950157f9ec`. No install claimed at this checkpoint. Owner subsequently explicitly requests push first, then build/test on real Fold3; scoped physical verification is now authorized despite unavailable Windows simulator, and will be recorded separately. Windows emulator-first selector attempted: FAIL because `/mnt/d/Android/Sdk/platform-tools/adb.exe` is absent. Linux SDK exists at `/home/kunrg/Android/Sdk` (the old `/home/khuongnd/Android/Sdk` path is absent); JDK17 exists. Linux ADB5037 reports connected SM-F926B; no sign-in, installation, role/grant changes, live provider calls or physical acceptance performed at this pre-push checkpoint. Owner browser interaction remains required.

| Gate | Current result |
| --- | --- |
| Target OAuth, consent/scope, refresh/restart, model list | NOT TESTED |
| Real target inference, Vietnamese/multi-turn Chat | NOT TESTED |
| Real account function calling / Android execution | NOT TESTED; command activation BLOCKED |
| Simulator offline regression / account UI | NOT TESTED; Windows tooling missing |
| Actual Fold3 scoped offline regression | PASS20/20; account/device-action/natural-voice gates remain open |
| Fold3 mic/STT/TTS, interruption, local/confirmation/security regression | NOT TESTED |
| Latency, streaming completion rate, PSS/memory/network/cancellation measurements | NOT TESTED; no fabricated metrics |

## Real Fold3 checkpoint after push

Owner explicitly requested push first, then build/test on real Z Fold3. Commits `9bfedea` and `85cafa6` were pushed; [draft PR2](https://github.com/khuongnd0901/Dex-Assistant-UI/pull/2) targets main, unmerged. Subsequent device-candidate build after push PASS201tasks (42executed/159up-to-date),1m26s, using the same full test/lint/app+instrumentation APK targets.

Source: pushed85cafa6 plus the owner's pre-existing T-023 launcher icon overlay (2manifest lines and7resource files) to preserve the installed icon. No T-017 collector changes copied; migration commits contain no icon/collector edits. Candidate and test APKs re-signed with the original certificate `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`; installed previous APK certificate independently matches. Original installed APK backed up privately before replacement. Initial90s wireless pull timed out; full retry completed113.984s and verified as a valid original-signed APK. No uninstall/reset/reprovision/grant or role changes.

- Device: independently queried SM-F926B/API35, Linux ADB5037; actual Samsung Desktop display2,1920×1080.
- In-place app and instrumentation installations PASS. Installed app SHA256 matches `1e70d0d9d438adadd2bfaf699d6b5da21cb3555296480adce5bf83fa951841c5` exactly. Test APK SHA256 `add1f5124b96840b36db505fa74e3ad4ecf480aa4968081da054573dc94c12d0`.
- Real-device scoped offline instrumentation **20/20PASS**,4.489s,0failures: new subscription fixture3 (Android JSON/SSE completion, reserved synthetic Keystore encryption/non-exportability/restore/logout/host retention, activation gates), existing isolated DirectAiFault14 (Chat/Command HTTP faults, malformed/bounded data, idle/cancel/stale/no-replay/key/fallback/schema regressions), pure local catalog/strict typed protocol/matcher3.
- Actual Fold3 matcher1000patterns/300warm samples p95=361354ns (0.361354ms), below existing5ms gate. This is local matching only, not inference or natural-command latency.
- After installation/tests and before dashboard launch,131/133existing private files byte-identical, including all8history files, original API-key/provider settings and voice policy. Only volatile `voice_health.xml` and ART `files/profileInstalled` changed. UID/appId/data-directory inode, Assistant role/component, voice-interaction component, enabled Accessibility, grant rows and Device Owner component set unchanged. No owner plaintext/tokens read into host output.
- DeX MainActivity launch PASS/WARM,806ms. Dashboard launch may re-encrypt retained history via the pre-existing read/persist policy; prior byte-equality evidence is explicitly before dashboard launch.
- Logs/private preservation metadata/original APK: `/tmp/dex-t025-device/`; candidates: `/tmp/dex-t025-artifacts/`. Nothing private/generated committed. A pre-install resource snapshot exists; no matched memory-impact comparison or natural/real-provider metrics claimed.

Owner completed native target sign-in and authorized model selection (verified in account settings). The first harmless inference probe reports ERROR; real inference is not PASS. Added an explicit-opt-in instrumentation probe that emits only HTTP status/counts/structural flags/local failure code, never token/body/text/IDs. Live diagnostics received HTTP200 in1946ms/3537ms but consumed0body characters: the added exact-MIME guard rejected the response header before parsing. This guard was not present in the proven POC or required by the documented plan-usage contract. Removed it; SSE structure/bounds/terminal completion remain mandatory. Added regression coverage for missing/noncanonical MIME and rejection of ordinary JSON bodies. Fixed-build/device retest and function-calling results are pending; no credentials/callback/browser logs or screenshots are collected during authorization. This physical offline PASS does not establish real subscription inference, device-command execution, natural voice/TTS, network/latency/PSS or soak acceptance.

## Files and rollback

Added auth/{ChatGptAuthManager,ChatGptSessionStore,ChatGptOidcVerifier,ChatGptHttp}, ai/{ChatGptResponsesClient,ChatGptModelRepository,ChatGptSubscriptionProvider}, ChatGptSubscriptionSettings, synthetic auth/OIDC/SSE/transport/tool tests and offline Android fixture. Modified AiProvidersSettings, AiCredentialStore/provider identity/settings, MobileAiProxy/common typed validator, DirectAiChat, AssistantController and API-key probe/tests. Updated canonical task/status/decision/plan, both READMEs and MIT adaptation notice. Unrelated icon/collector changes excluded from migration commits.

Rollback: leave existing Gemini/Groq default/settings intact, or explicitly choose the previous configured provider/model. Never delete legacy credentials or uninstall/clear app data. Disconnect subscription to clear its tokens; manage ChatGPT app access if remote revocation unconfirmed. Revert migration commits only after reviewing later work; retain independent account mapping/host identity for reconnect. No paid fallback introduced.

## Outstanding acceptance and limitations

Owner must sign in to DeX Assistant itself and approve plan usage. Run current model catalog and harmless inference, restart/refresh/logout/reconnect, Vietnamese multi-turn Chat and real namespace function-call extraction without execution first. Verify guarded execution/confirmation/local/voice/cancel/stale/dedup behavior and measurements through T-020. Windows simulator tooling remains unavailable; owner explicitly resumed real Fold3 build/testing after push. Preserve device state and run the scoped offline fixture first; account interaction remains owner-mediated. Keep draft PR unmerged and release gates false until these pass.

Additional implementation limits: one saved registration, no multi-account UI; no provisional Chat streaming view; auth exchange/refresh cancellation uses bounded HTTP completion plus generation fencing. These are not claimed as completed account/physical gates. Legacy spoken exit during TTS and other pre-existing physical gates remain open.

## Subscription stream compatibility finding

After removing the exact-MIME guard, real HTTP200 streams delivered response.created, text deltas, output_item.done and response.completed/status=completed, but terminal output was empty. Treating the terminal snapshot as the sole output incorrectly rejected real text and tool results. The [official streaming-event reference](https://developers.openai.com/api/reference/resources/responses/streaming-events) defines output_item.done as carrying the finished item. The decoder now retains bounded completed items, checks unique IDs/indices, added→done completeness, contiguous ordering and terminal response identity/status, then uses these items only after response.completed. Conflicting nonempty terminal snapshots reject. Deltas never become final replies or actions. Synthetic regressions cover missing terminal/done, duplicates/gaps/incomplete/conflicting items and structured tools.

Real native refresh and restart restoration PASS: replacement access and refresh tokens observed only via in-process boolean comparisons, subscription scope retained, authorized catalog7models. Earlier failing live batch: tool completion4402ms and harmless inference4370ms reached completed, but both empty-snapshot handling and Chat validation failed as recorded. These are failed baselines, not accepted inference/Chat/tool PASS. Fixed item-aggregation retest pending.
