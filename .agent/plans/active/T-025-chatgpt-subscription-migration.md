# T-025 — ChatGPT Subscription migration ExecPlan

## Outcome and boundaries
Native DeX Assistant subscription authentication and inference for Chat and typed command planning. Preserve local routes, privacy, voice/TTS, ActionRouter and legacy keys. No backend, proxy, CLI credentials, private endpoints, POC changes or automatic paid fallback. Never activate commands/default before real account acceptance.

## Inputs and constraints
Owner migration request M0–M7; AGENTS, D-030/D-036–038, BRD/SRS, DEVICE_COMMANDS_V1/V2, current source. POC reference `/home/kunrg/ChatGPT-Subscription-Android-POC` commit `2c67aacd06fdc572e6305ea016667daafe9d84a6`, MIT. POC report is reference, not target evidence. Existing dirty icon/collector/memory edits are retained and excluded from migration commits. Device data/signature/roles/grants must survive.

## Decision questions and evidence
- Eligibility: official SIWC quickstart/overview plus public MIT target; independently confirmed public GitHub metadata 2026-10-22. Account consent remains required.
- Protocol: official sign-in, accounts/sessions, models/inference, preview-limitations and function-calling docs (URLs in report). Require namespace tools and successful real account calls before command activation.
- Completion: terminal response.completed, completed status, bounded valid output; no partial actions/history, no reconnect/replay.
- Auth: separate installation identity and encrypted credentials; state/nonce/signature/issuer/audience/lifetime/scope checks; serialized refresh and logout fencing.

## Milestones
1. M0 audit/matrix, branch and eligibility evidence.
2. M1 adapt POC OAuth/store/verifier; synthetic security/session tests.
3. M2 shared bounded cancellable SSE client and account models; synthetic failure tests.
4. M3 Chat adapter using bounded explicit history and existing lifecycle/TTS.
5. M4 namespace schema/strict typed extraction; keep execution blocked without account proof.
6. M5 deliberate browser/account/settings/test inference; no key UI for subscription.
7. M6 explicit routing, preserved legacy fallback choices, no subscription fallback or default activation before acceptance.
8. M7 host/build/simulator checks, owner Fold3 auth/inference/tools/voice/metrics, evidence, milestone commits/push/draft PR.

## Validation
JDK17 `./gradlew test lint assembleDebug`, focused offline tests, scoped instrumentation, `git diff --check`; memory/link checks. Simulator selector first. Fold3 sign-in only owner interaction; never collect token/callback logs. Separate all physical results from synthetic/simulator results. Compilation cannot prove OAuth or model tool support.

## Risks and escalation
Preview protocol/account policy may change. Missing account tools => command BLOCKED, no text/JSON workaround. Owner absent => sign-in/physical NOT TESTED, migration IN_PROGRESS. Browser only explicit settings button, never voice/driving auto-launch. No resets/uninstalls/grant changes. Retain existing backup exclusions.

## Progress and findings
- [x] Read repository instructions/current routing/contracts and POC implementation.
- [x] Feature branch created, dirty tracked baseline saved outside repository.
- [x] Public OSS eligibility and documented native direct protocol established.
- [x] M1–M6 gated implementation and host verification: app226tests/variant, DPC5/variant, lint/build/app+test APK PASS.
- [x] Original signing identity available; signed candidate verified, no install yet.
- [ ] Owner follow-up: push first, then build/test on real Fold3; Windows simulator unavailable, scoped offline physical run now authorized.
- [ ] Target OAuth/inference/function calls and physical acceptance (owner interaction required).
- [ ] Default activation after all acceptance gates.

## Decision and requirement updates
D-039 records migration direction and gates; READMEs/report distinguish API keys from subscription and document rollback. Existing physical tasks remain unfinished. Update TASKS/STATUS/this plan with verified outcomes only.
