> Migrated to Android on 2026-10-13 ([D-037](../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# AI provider limit evidence and accounting contract

Evidence was accessed on 2026-10-02. Provider/model limits are configuration or
provider-reported state, never product constants.

- Google documents RPM, input TPM, and RPD dimensions; limits are per project,
  vary by model/tier/account, RPD resets at midnight Pacific, and active limits
  are shown in AI Studio: <https://ai.google.dev/gemini-api/docs/rate-limits>.
  Model lifecycle and Free Tier eligibility are separate concerns:
  <https://ai.google.dev/gemini-api/docs/models> and
  <https://ai.google.dev/gemini-api/docs/pricing>.
- OpenAI documents RPM/RPD/TPM/TPD, organization/project and model-family
  effects, account limit pages, and remaining/reset response headers:
  <https://developers.openai.com/api/docs/guides/rate-limits>. Its model catalog
  is <https://developers.openai.com/api/docs/models>. These sources do not
  establish a universal OpenAI API free allowance.

Every real Gemini/OpenAI call passes through one metered planner proxy. Public
configuration identifies a provider alias, model alias, enabled/health state,
timeout, and independent budgets. Secrets, endpoint internals, account/project
ids, and raw response headers are never serialized.

Budgets use `REQUEST`, `INPUT_TOKEN`, `OUTPUT_TOKEN`, or `TOTAL_TOKEN` units and
a provider-specific fixed refill window. A durable database transaction
reserves pessimistically before a call; `(requestId, providerAlias)` is unique.
Finalize from trustworthy usage and release only unused token reservation. A
request possibly sent is charged on timeout/network/5xx; unknown token usage
keeps its conservative reservation. Lazy transactional rollover uses each
budget's configured window and survives restart. There is no manual reset.

For each request, shuffle eligible providers using an injectable secure random
source, recheck/reserve atomically, and attempt each provider at most once.
Fallback is eligible for a reservation race/exhaustion, 429/quota response,
connect/response timeout, network I/O, retryable 5xx, or temporary unavailable.
Do not fallback for sensitive/invalid input, unsupported/no safe plan, invalid
LLM output, or authentication/configuration failure. Common validation maps all
provider output to the same typed action allowlist.

Terminal codes are:

- `AI_QUOTA_EXHAUSTED`: configured candidates exist but none can reserve budget.
- `AI_TIMEOUT`: every attempted eligible provider timed out.
- `AI_UNAVAILABLE`: no provider is configured/healthy/reachable, or eligible
  attempts fail transiently without the all-timeout condition.

All three are retryable and Android renders `Khó quá, không làm được`. Existing
`LLM_TIMEOUT` and `LLM_UNAVAILABLE` remain accepted from older servers but new
servers emit the `AI_*` codes.

## Gemini Free Tier recheck — 2026-10-04

Google pricing currently lists `gemini-3.8-flash` Standard input/output as free
on the Free Tier: https://ai.google.dev/gemini-api/docs/pricing. Billing docs
state new accounts start Free and API keys inherit their project's tier:
https://ai.google.dev/gemini-api/docs/billing. This confirms model eligibility,
not the owner's current billing tier,available quota or successful inference.
Active RPM/TPM/RPD must be read from AI Studio for that project; limits are per
project and model,not per key. Repo `DEX_GEMINI_TEST_RPM=10` is only an
application budget and never evidence of Google's actual allowance.

Current host has no root .env/key in environment at discovery,no Docker CLI,
no backend JAR and only JDK17 (backend requiresJava25). Created ignored root .env
from .env.example with mode0600,gemini,ai-proxy profiles,existing model default
and blank key/token-hash placeholder. It is local-only and not committed.
Live key/model/quota/backend smoke has not run. Owner is adding key locally;
Ubuntu hides dotfiles by default (Files Ctrl+H,or explicit editor path).

## T-014 provider path — 2026-10-06

Owner direction is to keep the existing Gemini integration through the
metered API proxy, retain personal scope and the BRD target of $0/month, and
skip provider-policy comparison. Do not add a provider, paid fallback or a new
integration path. The checked-in example does not establish deployed project,
model, quota or billing state. No provider-policy compliance or account-tier
claim is made. See the [T-014-02 decision record](../.agent/evidence/T-014-02-policy-review-2026-10-06.md).
