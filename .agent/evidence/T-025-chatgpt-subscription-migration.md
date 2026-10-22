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
| Fold3 mic/STT/TTS, interruption, local/confirmation/security regression | NOT TESTED |
| Latency, streaming completion rate, PSS/memory/network/cancellation measurements | NOT TESTED; no fabricated metrics |

## Files and rollback

Added auth/{ChatGptAuthManager,ChatGptSessionStore,ChatGptOidcVerifier,ChatGptHttp}, ai/{ChatGptResponsesClient,ChatGptModelRepository,ChatGptSubscriptionProvider}, ChatGptSubscriptionSettings, synthetic auth/OIDC/SSE/transport/tool tests and offline Android fixture. Modified AiProvidersSettings, AiCredentialStore/provider identity/settings, MobileAiProxy/common typed validator, DirectAiChat, AssistantController and API-key probe/tests. Updated canonical task/status/decision/plan, both READMEs and MIT adaptation notice. Unrelated icon/collector changes excluded from migration commits.

Rollback: leave existing Gemini/Groq default/settings intact, or explicitly choose the previous configured provider/model. Never delete legacy credentials or uninstall/clear app data. Disconnect subscription to clear its tokens; manage ChatGPT app access if remote revocation unconfirmed. Revert migration commits only after reviewing later work; retain independent account mapping/host identity for reconnect. No paid fallback introduced.

## Outstanding acceptance and limitations

Owner must sign in to DeX Assistant itself and approve plan usage. Run current model catalog and harmless inference, restart/refresh/logout/reconnect, Vietnamese multi-turn Chat and real namespace function-call extraction without execution first. Verify guarded execution/confirmation/local/voice/cancel/stale/dedup behavior and measurements through T-020. Windows simulator tooling remains unavailable; owner explicitly resumed real Fold3 build/testing after push. Preserve device state and run the scoped offline fixture first; account interaction remains owner-mediated. Keep draft PR unmerged and release gates false until these pass.

Additional implementation limits: one saved registration, no multi-account UI; no provisional Chat streaming view; auth exchange/refresh cancellation uses bounded HTTP completion plus generation fencing. These are not claimed as completed account/physical gates. Legacy spoken exit during TTS and other pre-existing physical gates remain open.
