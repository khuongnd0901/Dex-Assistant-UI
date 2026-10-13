> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-013 — Backend resource tuning

## Outcome and boundaries
Improve backend quality and measure operation with Docker 0.1 CPU and 1 GiB RAM. Preserve wire/auth semantics and existing owner data. Android and native migration excluded.

## Inputs and constraints
Task T-013 acceptance in TASKS.md; Java25, current JVM backend, isolated disposable container/data and synthetic credentials. Existing worktree changes must survive.

## Decision questions and evidence
Measure default versus tuned startup/readiness, cgroup memory/CPU throttling, authenticated WebSocket latency and errors using identical workload. Accept tuning only with functional regressions passing and observed resource benefit; report cold-start versus steady-state separately. No external paid AI calls required; fake planner evidence is not provider latency.

## Milestones
1. Inspect quality/resource hotspots and capture baseline image/run.
2. Implement justified fixes and opt-in low-resource runtime configuration.
3. Repeat constrained test, run full verification and document results.

## Validation
`./mvnw verify`; Docker build and compose syntax; isolated authenticated WebSocket workload and readiness under inspect-confirmed 100000000 NanoCPUs / 1073741824 memory bytes; cgroup evidence and OOM inspection; targeted whitespace checks.

## Risks and escalation
0.1 CPU can substantially slow Spring cold start. Never loosen authentication or deadlines to manufacture success. Separate fake planner capacity from remote-provider/physical-device acceptance. Stop only task-owned containers.

## Progress and findings
- [x] Baseline24/24 commands under verified 0.1CPU/1GiB limits; readiness156s, peak236MiB.
- [x] Same-image JVM comparison24/24: readiness71s, peak153MiB, CPU7.41s versus16.37s.
- [x] Added configurable1024 LRU conversation cap and shared schema mapper, six regressions; fixed container healthcheck. D-029 records context eviction semantics.
- [x] Java63 tests pass; repository package retry succeeds after transient `.jar.original` rename failure. Isolated Linux full verify63 tests/JAR PASS; source copy verified identical, contract fixtures included.
- [x] Final image24/24 measured commands +2 device-result exchanges +rate rejection +health PASS; readiness77.046s, peak153.852MiB, first1297.616ms/p954.748ms, no OOM. Containers cleaned; evidence and task/status updated.
- Evidence: [measurement report](../../evidence/T-013-resource-tuning-2026-10-05.md).

## Decision and requirement updates
D-029 records bounded context/LRU semantics. Settings and measured limitations are in `docs/RESOURCE_TUNING.md` and the evidence report. No product SLO or shared contract changes. T-013 DONE; physical/provider acceptance belongs to existing tasks.
