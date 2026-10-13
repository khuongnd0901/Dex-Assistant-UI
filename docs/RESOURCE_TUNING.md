> Migrated to Android on 2026-10-13 ([D-037](../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Backend at 0.1 CPU / 1 GiB RAM

Use the optional override after configuring the normal token digest/provider:

```bash
docker compose -f compose.yml -f compose.low-resource.yml up -d --build
```

This caps CPU to 0.1 core and memory to 1 GiB (1,073,741,824 bytes), with no
additional swap. It retains the base security, persistence and provider settings.
The JVM uses one active processor, Serial GC, 64 MiB initial / 384 MiB maximum
heap and tier-1 compilation. The heap cap leaves room for metaspace, native
buffers, threads and file cache. Tier-1 compilation trades peak throughput for
less compilation work; this override targets small workloads, not general
production sizing. Readiness gets a 180-second startup grace period.

Conversation history now retains at most `dex.conversation.max-conversations`
(default 1024), with the existing six-turn / 30-minute limits. Inserting beyond
the cap evicts the least recently accessed conversation context. Commands and
wire responses are unchanged. This is an entry cap, not an absolute byte bound.
Tool-schema serialization reuses an immutable mapper; each client's capability
schema is still generated separately.

Both Docker build paths use the same readiness check (HTTP 200 and JSON UP).
The previous main image's grep pattern omitted JSON quotes and could falsely
report an otherwise ready application unhealthy.

## Reproduce the measurement

Build outside the constrained runtime so compiler cost is not mixed with serving:

```bash
./mvnw verify
docker build -f Dockerfile.local -t dex-assistant-backend:resource-test .
python3 scripts/perf/docker-resource-test.py --image dex-assistant-backend:resource-test --output /tmp/default.json
python3 scripts/perf/docker-resource-test.py --image dex-assistant-backend:resource-test --tuned --checks --output /tmp/tuned.json
```

Requires Docker, host Python3, `python:3.12-slim` and the backend image. Each run
creates a unique disposable backend and client, without publishing ports,
mounting owner data or using provider credentials. The client shares the
backend network namespace to satisfy loopback authentication, but has its own
CPU/memory accounting. Only the backend has the requested resource limits.
It polls readiness, executes three rounds of eight authenticated fake-planner
Wi-Fi commands with two 61-second round intervals, and checks accepted/plan/
completed responses and correlation IDs. These are simulated actions; no phone
or Wi-Fi settings are changed. With `--checks`, it also asserts two DEVICE_COMMAND result exchanges and a
rate-limited eleventh request, then runs the packaged healthcheck. Device results
are simulated. The script removes its backend and client on completion.

JSON includes first/p50/p95/max latency, readiness probe time, cgroup CPU/memory
statistics, memory peak, OOM state and image identity. Readiness timing begins
inside the client (after client startup); application startup is recorded
separately from the backend log. Cgroup memory includes cache, not just JVM RSS. CPU counters cover startup and
the whole run. Later runs also emit raw latency samples and p95 excluding the
first command. Use `--rounds 10` for a longer short soak.
Do not interpret this short single-client test as a concurrency capacity test,
a long-term leak test, external AI latency or physical DeX acceptance. Provider
quota tables currently have no retention cleanup and require a separate durable
retention/replay policy before pruning.

Measured results are in [T-013 evidence](../.agent/evidence/T-013-resource-tuning-2026-10-05.md).

Configuration references, accessed 2026-10-05:
[Docker Compose service limits](https://docs.docker.com/reference/compose-file/services/)
and [Java25 launcher options](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html).
