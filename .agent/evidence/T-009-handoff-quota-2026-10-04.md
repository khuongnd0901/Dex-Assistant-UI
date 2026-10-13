> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-009 handoff continuation — 2026-10-04

## Verified scope

Both repositories started clean with HEAD=origin/main: canonical
`767165f99db625ceebcf649c88a812666f110256`, Android
`8f7f0bc25fef8ec3433a715d2def73eacf011ac6`. Java backend readiness HTTP200 UP.
Fold3 rediscovered as `192.168.1.18:42563`; ADB reverse8081→8080 restored.
Existing Accessibility service remains enabled; no installation, provisioning,
permission, owner configuration/history, microphone or model changes.

## Live Java quota/reconnect acceptance

Ran `/tmp/t009-handoff-quota-check.py`, importing the existing raw WebSocket
helpers from `scripts/ai/docker-proxy-smoke.py`, against isolated loopback8080
with the existing public test identity and existing external H2 database.
No quota reset, database replacement or fabricated action result.

In UTC window `2026-10-04T08:27:00Z`:
- Initial AVAILABLE, used0/reserved0/remaining2, configured request limit2.
- Two synthetic Settings planning requests each returned execution.plan.
  Each socket was closed without executing actions; authenticated reconnect
  snapshots asserted used1/remaining1 then used2/remaining0, reserved0.
- The next accepted request returned AI_QUOTA_EXHAUSTED; counters stayed
  used2/reserved0/remaining0, state EXHAUSTED.
- Another authenticated connection returned the same window/counters.
  No unsolicited frame arrived during a 2-second observation.

This proves live configured local budget enforcement and server socket reconnect
snapshot continuity. It does not prove Android automatic reconnect/staleness UI,
no-replay across Android process death, provider account limits, runtime restart
persistence, or physical cloud-plan action execution in this slice.
Only two new provider planning calls were made; no device action was executed.

Natural refill readback in UTC window `2026-10-04T08:28:00Z`: AVAILABLE,
used0/reserved0/remaining2. No reset/restart or additional inference was needed.
Backend remains UP with its existing H2 database.

## Automated verification

`JAVA_HOME=/home/kunrg/.local/opt/jdk-25.0.4.1+1 bash ./mvnw -B -ntp
-Dtest=JdbcQuotaServiceTest,MeteredPlannerProxyTest test`: BUILD SUCCESS,
6 tests, zero failures/errors/skips, 6.483s. Log:
`/tmp/t009-handoff-quota-tests.log`.
Coverage includes atomic reservations/service restart, independent natural refill,
bounded fallback/no repeated provider attempt, invalid output, timeout and quota
failure codes. Fallback is stub-tested, not live Gemini/OpenAI fallback.

## Remaining work

T-009 remains IN_PROGRESS. Existing LocalFirstDeviceTest uses owner history,
clears its records/key in several cases, assumes an older UI, and toggles Wi-Fi
used by wireless ADB. BackendMonitoringDeviceTest must also be checked for
credential provisioning before physical use. Do not run those unchanged on the
owner Fold3. Next isolate local/offline and quota/reconnect Android tests using
the existing CoordinatorBackendDeviceTest context/history/token pattern; retain
owner state assertions and Accessibility-preserving instrumentation.
Live multi-provider fallback, daily/token budgets, retention configuration,
Android reconnect/staleness UI and broader physical acceptance remain open.
