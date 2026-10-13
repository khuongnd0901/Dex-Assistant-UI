> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Mobile direct AI Command pilot + multi-provider Settings (2026-10-09)

- Status: **SOURCE IMPLEMENTED / CI PENDING / FOLD3 ACCEPTANCE OPEN**
- Owner clarification: **OpenAI (ChatGPT API), Gemini, Grok (xAI), DeepSeek and Groq** each have independent enabled flag, locally stored API key and selected default model. The owner also wants an explicitly selectable preferred provider.
- Policy: no API secrets stored in Git, source or APK. Per-provider encrypted Android Keystore store was introduced previously. Provider model API lists show accessibility, **not free pricing**.

## Code delivered (Android repo)

- \`ai/AiToolSchema.kt\`: function schemas derived from supported Android ActionIds, limited to V1 actions.
- \`ai/MobileAiProxy.kt\`: Gemini REST functionCalling and Groq OpenAI-compatible tool_calls, provider failover only on transient provider errors; bounded timeout; parse output exclusively through existing \`DeviceCommandProtocol.decode\`; reject malformed/unsupported/unknown actions; 8 action limit; no raw text gesture execution.
- \`AssistantController.kt\`: optional Direct AI Command behind \`AiProviderSettings.directMode()\` default **false**; Local remains local; direct actions reuse existing \`ActionRouter\` with requestId/confirmation/epoch gating. Legacy WebSocket Command path remains rollback, Chat stays WebSocket and may connect independently.
- \`security/AiCredentialStore.kt\`: five providers with independent Keystore keys, enable flags, saved default model, explicit owner-confirmed model Free Tier and preferred provider; \`GEMINI\` and \`GROQ\` adapter supported, \`OPENAI\`, \`GROK\`, \`DEEPSEEK\` **configuration-only until provider/model free eligibility and tool contract validated**. No auto-route through unsupported default.
- \`ai/AiKeyProbe.kt\`: authenticated no-inference listing for five official model endpoints; model picker. No claim of free-tier status or tool-calling support.
- \`AiProvidersSettings.kt\`: pick five providers from dropdown, per-provider masked API key save/update/delete, enable, download available model IDs, edit/save chosen default model, explicit Free Tier confirmation, set preferred provider, Direct AI Command opt-in toggle. All local config.
- Added / updated JUnit tests for Gemini/Groq valid and invalid tool calls; test expected model listing and endpoint classification.

## No-go boundaries

- Never label a provider/model free solely based on being present in model-list response.
- OpenAI, xAI Grok and DeepSeek billing/access are not proven free from this repo; they must not silently run on a supposedly zero-cost path.
- Direct AI Command is **off by default**. Voice/Device Owner/Accessibility/Chat backend unchanged, except independent direct route when manually opted in.
- No real Gemini, Groq or other provider API key supplied in this GitHub coding session. No physical Fold3 execution. No API cost or latency benchmark claimed.
- Existing M0/M1/M2/M3 migration acceptance remains OPEN until actual signed Fold3 + account/model Free Tier and real provider/function-call evidence.

## CI evidence

Final Android commit/workflow: capture **actual** run result before recording PASS. GitHub Actions runs \`testDebugUnitTest lint assembleDebug\` on every source push to main; all intermediate runs can be cancelled by concurrency configuration.

## Compiler gate follow-up

- Initial Android CI run 37877209340 FAILED at compileDebugKotlin: MobileAiProxy unresolved getString/getJSONObject because calls inferred too broadly.
- Android commit 493cd37 adds explicit List<JSONObject> and buildList<JSONObject>. New CI: https://github.com/khuongnd0901/Dex-Assistant-UI/actions/runs/37877414417. Check final result before claiming PASS.
- No real provider-key inference or signed physical Fold3 regression completed in this GitHub coding session.