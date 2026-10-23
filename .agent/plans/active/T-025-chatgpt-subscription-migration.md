# T-025 — ChatGPT Subscription migration ExecPlan

## Outcome and boundaries
Native DeX Assistant subscription authentication and inference for Chat and typed command planning. Preserve local routes, privacy, voice/TTS, ActionRouter and stored legacy keys. No backend, proxy, CLI credentials, private endpoints, POC changes or automatic paid fallback. Never activate commands/default before real account acceptance.

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
7. M6 explicit routing, preserved legacy fallback choices (historical; D-040 retires them), no subscription fallback or default activation before acceptance.
8. M7 host/build/simulator checks, owner Fold3 auth/inference/tools/voice/metrics, evidence, milestone commits/push/draft PR.

## Validation
JDK17 `./gradlew test lint assembleDebug`, focused offline tests, scoped instrumentation, `git diff --check`; memory/link checks. Simulator selector first. Fold3 sign-in only owner interaction; never collect token/callback logs. Separate all physical results from synthetic/simulator results. Compilation cannot prove OAuth or model tool support.

## Risks and escalation
Preview protocol/account policy may change. Missing account tools => command BLOCKED, no text/JSON workaround. Owner absent => sign-in/physical NOT TESTED, migration IN_PROGRESS. Browser only explicit settings button, never voice/driving auto-launch. No resets/uninstalls/grant changes. Retain existing backup exclusions.

## Progress and findings
- [x] M0 audit/matrix, repository/contract/POC reads, feature branch, independent public-MIT eligibility.
- [x] M1–M6 native auth/shared transport/Chat/typed planning/settings; legacy keys and local/security routes preserved.
- [x] Historical2026-10-22 source6515ec8 host test/lint/app+test APK PASS201tasks; app234tests/variant (1skip),DPC5/variant,0lint errors.
- [x] Original-signed in-place Fold3 installation; owner personally completed sign-in. Scope/restoration/refresh rotation and7models verified.
- [x] Actual inference,2-turn Vietnamese Chat and completed structured namespace tools PASS; real cancellation1ms after delta, no partial history.
- [x] Final core+offline23/23PASS; routing3/3PASS: actual MobileAiProxy/ActionRouter Chrome dispatch, no local-match shortcut, Chat action isolation. No owner default/key change.
- [x] Account/client+model-bound inference/tool proofs allow deliberate manual trials; automatic activation remains false. Draft PR2 unmerged.
- [ ] Latest6515ec8 hardened completion error check installed on Fold3 (built/host-tested only; current device app c89fa2b).
- [x] Windows simulator2026-10-23: final cleanup candidate13/13scoped subscription/offline/local tests PASS14.309s,API35; original missing-ADB limitation resolved on current host.
- [ ] Owner manual Subscription selection and natural voice/STT/TTS/interrupt/confirmation/security acceptance; physical logout/reconnect; matched PSS/natural p95/soak.
- [ ] Full acceptance/default activation/merge; migration remains IN_PROGRESS.

### Continuation 2026-10-23 — current work
Owner requests latest GitHub source, continuing its existing subscription branch and removing obsolete API code. Fetched ba054d3 and created isolated worktree `/home/khuongnd/worktrees/dex-subscription`; original dirty main preserved. D-040 supersedes in-app BYOK rollback. Current work removes API adapters/key editor and dead settings, routes coordinator directly to subscription, retains stored credentials and explicit activation/proofs, ports offline lifecycle/perf/schema tests and updates bilingual docs. Validation and results: [continuation evidence](../../evidence/T-025-subscription-only-2026-10-23.md). No physical/live-account action performed this session. Current cleanup JDK17 test/lint/APKs PASS201tasks; app219tests/variant (1skip),DPC5/variant. Windows API35 AVD final subscription/offline lifecycle/settings/Keystore/component/local suites PASS13/13,14.309s. LocalFirst fixture updated to current UI tags after initial2failures. Whitespace and memory145Markdown/471links PASS. Owner-authorized implementation commit5f5bd25 pushed to the existing feature branch; remote SHA verified. No physical/live calls or merge. Next coordinate original-signed physical acceptance. Do not reuse previous physical results as cleanup validation.

### Historical next-session handoff (owner pause 2026-10-22)
No more live calls/device changes today. Source pushed through6515ec8; checkpoint documentation follows it. Read STATUS and report first. Rediscover wireless ADB; never reuse historical port blindly. Use JDK17 and `/home/kunrg/Android/Sdk`; `/tmp/dex-t025-validation` is isolated build workspace. Latest full build log `/tmp/dex-t025-release-candidate-build.log`.

Re-sign latest app/test with original9ddb identity, retaining the owner's existing icon overlay, then install in place without uninstall/data/grant/role changes. Current installed trial artifact SHA256 ce39dccc0df9dbd3be5b23869a1dbaa97ca3f5e221caa49d2927bd5545c6ce9a; final6515ec8 hardening is not in it. Verify installed hash and scoped offline/final harmless live smoke. Do not repeat the Chrome execution unnecessarily.

Owner default is still Gemini. Existing real account/model proofs enable settings button “Chọn Subscription để thử Chat/AI Command”; obtain deliberate owner choice before altering their default. Continue natural voice/TTS/interrupt and acceptance measurements, recording missing gates honestly. No automatic default migration, paid fallback or merge. Original APK/private metadata/logs live under `/tmp/dex-t025-device/`; artifacts under `/tmp/dex-t025-artifacts/`. Never commit these private/generated files. Prior unrelated dirty files stay untouched.

## Decision and requirement updates
D-039 records migration direction and gates; READMEs/report distinguish API keys from subscription and document rollback. Existing physical tasks remain unfinished. Update TASKS/STATUS/this plan with verified outcomes only.
