# T-025 — ChatGPT Subscription migration evidence

Date: 2026-10-22. Status: IN_PROGRESS; owner requested checkpoint/push and pause until tomorrow. Native account/model/inference/tools and guarded routing verified on Fold3; full voice/performance acceptance remains open. No OAuth secrets, callback queries, owner prompts or private account identifiers are included in repository evidence.

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
| AI Command | AssistantCoordinator → MobileAiProxy → Gemini/Groq function calls → AiToolCallParser → typed guarded router | Dedicated subscription branch/shared transport; namespace schemas reuse AiToolSchema and common typed validator; manual execution requires real account/client+model-specific inference/tool proofs |
| AI Chat | DirectChatState → DirectAiChat → Gemini/Groq text → complete history → Android TTS | Subscription adapter sends explicit user/assistant history, no tools; final validated response only; production selection gated |
| Voice Command | Wake/STT → coordinator command route → interaction/confirmation/action → TTS | Same route, request IDs, cancellation, 30s watchdog/120s total and stale guards; no auth/browser launch from voice |
| Voice Chat | Existing explicit Chat mode, follow-up after TTS, stop/idle/epoch guards | Same lifecycle and TTS; no action dispatcher in Chat; natural voice acceptance still pending |
| Selection | AiProviderSettings default + enabled/model/free-tier confirmation | New CHATGPT_SUBSCRIPTION identity, distinct OPENAI API-key label; no default change; subscriber auth bypasses BYOK/free-tier candidate loop |
| Credentials | AiCredentialStore AES-GCM keys per provider; separate legacy bearer | New encrypted atomic noBackup record + dedicated Keystore alias; no old key mutation/token import; own stable host/issued ID |
| Cancellation | Future + active HTTP cancellation, requestId/stale response check | Per-consumer operation cancellation fences auth-to-HTTP races; no stream reconnect/action replay; preference changes cancel active AI sessions/plans |
| History | Command encrypted bounded durable; Chat volatile 20 pairs/24KB approximate context, 200 turns, 15min idle, 2000 input/4000 output code points | Existing state retained; only completed final Chat text committed; partial output never history; no previous_response_id |
| Errors/fallback | Legacy Gemini/Groq eligibility and bounded fallback; unsupported/credential/malformed fail closed | Subscription pre-stream401 refresh at most once;403/429/5xx explicit failures; no subscription-to-BYOK fallback; truncated/failed/incomplete SSE never successful |

Owner default remains Gemini. Explicit manually selected subscription route is Android → own OAuth installation → public models/Responses SSE → text or structured validated typed plan → existing consumers. No backend/CLI/proxy/internal API layer exists.

## Authentication architecture

Adapted POC PKCE S256, state/nonce and RS256 JWKS checks (issuer/audience/azp/expiry/iat/nbf/subject). Browser uses HTTP127.0.0.1 ephemeral port and fixed /auth/callback; listener starts first and bounds request lines/time. Issued client persisted before token exchange. Successful identity/scope validation precedes session replacement. Re-login validates the saved subject. Single saved account/workspace mapping is supported; additional account picker remains a limitation.

AES-GCM record uses DeX-only Android Keystore alias and AtomicFile under noBackupFilesDir. Existing all-domain backup exclusions are unchanged. Host/registration survive logout; tokens do not. Refresh lock serializes rotating-token writes and ignores obsolete401 tokens after another refresh. Login/logout generation fences prevent resurrection. Revocation uses discovery's HTTPS endpoint, retries one transient failure after200ms and never redirects; failure still clears local tokens and displays unconfirmed remote revocation. No token/URL/body/provider exception is displayed or logged.

## Provider / Chat / command implementation

Shared ChatGptResponsesClient uses OAuth bearer, fixed public endpoint, store=false/stream=true and instructions/input. Incremental bounded SSE rejects malformed/truncated/incomplete/failed results; completed final response/status is authoritative. Limits:20s inference call/15s read/5s connect,262144-byte line/event,2MiB stream,10000events,65536delta characters,4000Chat code points. Cancel operations interrupt active inference calls and fence results; an in-progress shared refresh remains bounded by30s auth HTTP timeout.

Chat sends bounded existing history and local time; no tools, action output rejected. Existing final Reply/TTS interface has no provisional UI event, so streaming display is not wired; incomplete output never reaches TTS/history. 1024token cap cannot be hard-enforced by unsupported max_output_tokens; instructions request brevity and local4000code-point cap rejects excess.

SubscriptionTools translates existing capability/app-filtered declarations into a `device` namespace. Only completed function_call items with namespace, unique call_id, supported function name and valid typed arguments are accepted. Existing decode enforces schema1/allowlists/params; maximum8actions. No generated text becomes an action. Real namespace/model behavior is verified on the selected account/model. DEVICE_COMMANDS_ACCEPTED and MANUAL_SELECTION_ACCEPTED permit deliberate trials only with both scoped proofs; DEFAULT_ACTIVATION_ACCEPTED remains false. Account/model changes invalidate proofs. Owner default was never switched by tests. No action/router executor implementation was replaced.

Settings include status, deliberate browser sign-in, verified account email, scope status, visible account-model list/selection, harmless inference test, nonexecuting real-schema function-call probe, cancellation, usage link and local/remote logout feedback. Available states include all eight requested states. No API-key entry for subscription. Legacy providers/keys unchanged.

## Final verified host checkpoint

Source `6515ec83bdb11bcd73cc96ac3eec7ddf0ea18d2d` includes fail-closed rejection of any nonnull contradictory completed-response error. Full isolated JDK17 command PASS:

```bash
./gradlew -p /tmp/dex-t025-validation test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1 --no-watch-fs
```

201tasks (22executed/179up-to-date),1m15s. App234tests per debug/release variant,0failures/errors,1conditional skip each; DPC5/variant PASS. Lint0errors/fatal; app32/DPC2existing warnings. Log `/tmp/dex-t025-release-candidate-build.log`. Whitespace checks PASS. Latest source built but final6515ec8 hardening NOT installed on Fold3 at pause.

Initial overlapping builds failed incremental caches and were discarded as evidence; isolated single-worker builds passed. Actual SDK `/home/kunrg/Android/Sdk`, JDK `/home/kunrg/.local/opt/jdk17`; old instructed SDK path absent. Final staged migration snapshot memory/link validation PASS144Markdown/473links/0errors; root workspace retains unrelated dirty T-017 missing-evidence link. Only the four T-025 handoff documents are staged in this checkpoint.

## Physical Fold3 evidence

Device independently verified SM-F926B/API35, Linux ADB5037, Samsung Desktop display2/1920×1080. Windows simulator NOT TESTED: Windows ADB absent. Owner explicitly requested push then physical tests; initial implementation was pushed before installation. Owner personally approved/completed system-browser sign-in. No authentication-browser/callback capture or secret logging.

Original signing certificate SHA256 `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03` retained. Original installed APK privately backed up; app/test installed in place with original signing, no uninstall/reset/grant/role changes. Device builds include only migration source and owner's existing launcher-icon overlay, excluding collector edits.

Current installed app is production through c89fa2b; last test fixture includes2183400. Installed trial artifact `/tmp/dex-t025-artifacts/dex-assistant-subscription-trial.apk` SHA256 `ce39dccc0df9dbd3be5b23869a1dbaa97ca3f5e221caa49d2927bd5545c6ce9a`. It excludes latest6515ec8 error hardening. Never report latest host build as already deployed.

| Gate | Actual result |
| --- | --- |
| Native OAuth/consent/subscription scope | PASS; owner's independent target installation |
| Session restoration and real refresh rotation | PASS; both token changes observed only as in-process booleans; scopes retained |
| Account model catalog | PASS;7models; owner selected GPT-6-Astra, no hardcoded model |
| Harmless inference/completed SSE | PASS, Vietnamese output |
| Native two-turn DirectAiChat | PASS, bounded prior-pair counts0/1; command-like second input remains Chat, no actions |
| Structured namespace function calling | PASS; finished typed tools only; initial VOLUME_SET/MUTE probe never executed |
| Native MobileAiProxy → ActionRouter | PASS; one APP_LAUNCH Chrome dispatched, actual Chrome focus confirmed by package metadata |
| Live streaming cancellation | PASS;1ms after first delta, CANCELLED, no partial history/replay |
| Final scoped core+offline regression | PASS23/23,24.349s |
| Separate real routing regression | PASS3/3,14.305s |
| Windows simulator | NOT TESTED; missing tooling |
| Owner selected Subscription as saved default | NOT DONE; saved default remains Gemini |
| Natural voice/STT/TTS/interruption/confirmation | NOT TESTED for subscription acceptance |
| Physical logout/reconnect | NOT TESTED; isolated secure-store/logout tests PASS |
| Natural p95/matched PSS/100sessions/8hsoak | NOT TESTED; no acceptance claim |

The final23-test batch combines live3 and offline20. Offline20 includes new subscription3, isolated DirectAiFault14 and pure local catalog/protocol/matcher3. Initial offline20/20 also passed4.489s. Final matcher1000patterns/300samples p95=0.360052ms; this is deterministic matching only.

Live account core final timings: nonexecuting tool3368ms; harmless inference3406ms; Chat4538/3286ms. Earlier corrected core3/3 passed20.026s, with tool4100ms/inference3869ms/Chat3829 and3613ms. Real refresh rotated access+refresh and loaded7models. Small passing batches establish protocol behavior, not statistical completion-rate or natural latency acceptance.

Routing fixture uses unique temporary preferences and the same real native auth session. DirectAiChat two turns3325/4522ms; live cancel3027ms total,1ms from delta to abort. Command phrase is asserted not to match deterministic local catalog, model allowlist APP_LAUNCH only; one validated Chrome action planned3185ms, end-to-end3245ms, DISPATCHED through existing ActionRouter. No retry/reconnect or duplicate action. Tests assert owner's API keys/default unchanged. They do not alter owner conversation history. Chrome may remain foreground after the test; no further device mutation after owner pause.

### Preservation and private artifacts

Before dashboard launch,131/133existing private files byte-identical, including all8history files, legacy keys/provider settings and voice policy; changes only volatile voice_health.xml/ART profileInstalled. UID/appId/data-directory inode, Assistant/voice components, Accessibility/grant metadata and Device Owner components unchanged. Subsequent owner sign-in/model/proofs and real refresh legitimately change subscription metadata/tokens; do not claim provider preferences remain byte-identical after that. Dashboard's pre-existing read/persist can re-encrypt history, so earlier byte equality has an explicit pre-dashboard boundary.

Private original APK, preservation metadata and finite diagnostic logs: `/tmp/dex-t025-device/`. Candidate APKs: `/tmp/dex-t025-artifacts/`. Generated/private artifacts are not committed. A baseline resource snapshot exists; no matched candidate PSS/memory-impact comparison. No memory savings claimed.

## Protocol findings and resolved failures

Initial real HTTP200 requests consumed no body because an unnecessary exact-MIME guard rejected a noncanonical header. Removed it, matching the proven POC; bounded SSE structure and terminal validation still mandatory, ordinary JSON/HTML cannot succeed. Failed probes1946/3537ms are not inference PASS.

Subsequent streams carried finished items in response.output_item.done but terminal response.completed.output was empty. Initial tool4402ms/inference4370ms reached completed yet failed validation. The [official streaming-event reference](https://developers.openai.com/api/reference/resources/responses/streaming-events) defines done as carrying the finished item. Decoder now buffers bounded finished items, verifies IDs/indices/added→done completeness/contiguous ordering/terminal identity and status, and releases output only after response.completed. Conflicting nonempty snapshots and nonnull terminal errors reject. Deltas never become completed replies/actions. Synthetic tests cover gaps/duplicates/incomplete/conflicting output. Corrected actual account tests above PASS.

## Files and rollback

Added auth/{ChatGptAuthManager,ChatGptSessionStore,ChatGptOidcVerifier,ChatGptHttp}, ai/{ChatGptResponsesClient,ChatGptModelRepository,ChatGptSubscriptionProvider}, ChatGptSubscriptionSettings, auth/OIDC/SSE/transport/tool unit tests and scoped offline/live/routing Android fixtures. Modified AiProvidersSettings, AiCredentialStore/provider settings, MobileAiProxy/common typed validator, DirectAiChat, AssistantController, API-key probe/tests. Updated canonical task/status/decision/plan, SRS, both READMEs and MIT adaptation notice. Unrelated icon/collector/memory edits excluded; POC/backend unchanged.

Rollback: owner's default is still Gemini. If manually switched later, explicitly choose previous configured Gemini/Groq provider/model; keys remain intact. Never uninstall/clear data. Disconnect subscription to clear its tokens; manage ChatGPT app access if remote revocation unconfirmed. Keep host/registration for reconnect. Review later work before reverting commits. No silent paid fallback.

## Tomorrow's resume checklist and remaining gates

Owner requested pause/checkpoint on2026-10-22. Branch feat/chatgpt-subscription-migration; [draft PR2](https://github.com/khuongnd0901/Dex-Assistant-UI/pull/2), unmerged. Source through6515ec8 pushed; documentation checkpoint follows it. Task remains IN_PROGRESS.

1. Read STATUS/plan, rediscover wireless ADB. Re-sign latest app/test with original9ddb identity, retain icon overlay and install in place; verify installed hash. Final6515ec8 hardening is built/host-tested only.
2. Run scoped final offline plus one harmless inference smoke; do not repeat actual Chrome dispatch unnecessarily.
3. Ask owner to deliberately use “Chọn Subscription để thử Chat/AI Command” if ready. Both account/client+model-specific proofs already stored by real probes. Do not silently change saved default; automatic activation remains false.
4. Verify natural Vietnamese voice/STT/TTS, interruptions, Chat lifecycle, local commands, confirmation/security, physical logout/reconnect and required performance campaign under T-020/T-021. No automatic action retry after ambiguity.
5. Simulator remains NOT TESTED until tooling available. Record all unmet gates before considering default activation/merge; no DONE claim today.

Additional limits: one saved registration/no multi-account UI, no provisional Chat streaming display, instruction-only1024token request with hard local4000code-point bound, auth network cancellation bounded30s plus generation fence. Existing spoken exit during TTS remains a separate T-014 limitation. Current small inference samples3–4.5seconds do not establish the SRS2second p95 target. Natural30samples/route,100sessions,8hsoak and matched PSS remain open.
