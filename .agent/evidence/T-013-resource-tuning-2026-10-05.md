> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-013 constrained backend evidence — 2026-10-05

## Environment and method

Docker Desktop28.4.0 Linux/amd64; Temurin25.0.4.1; SpringBoot4.0.8.
Runtime limits independently checked with Docker inspect and cgroup v2:
`NanoCpus=100000000`, `cpu.max=10000 100000`,
`Memory=MemorySwap=1073741824`, `memory.max=1073741824`.
No published ports, owner data, paid provider calls or physical device actions.
Client runs outside backend cgroup in the same network namespace, authenticating
with the existing public synthetic test identity.

Each run uses 24 sequential fake-planner Wi-Fi commands, in three eight-command
rounds separated by 61 seconds. Connections reopen per round. Default ten-command
per minute limit stays enabled. `accepted → plan → completed` and correlation IDs
are asserted. Samples include first-use costs and reconnection effects. This is
a short single-client soak, not a concurrency, leak, or production AI benchmark.

## Isolated JVM comparison (same baseline image)

Image: `sha256:8151c31a20501128bf390d2db0006d8a5698954be8efc2b02551cccac4b26415`.
Baseline JAR captured before T-013 Java edits. Tuned run changes only JVM flags:
`-XX:ActiveProcessorCount=1 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xms64m -Xmx384m`.

| Metric | Baseline | JVM tuning only |
| --- | ---: | ---: |
| Application startup (Spring log) | 129.292 s | 57.671 s |
| Process uptime at Spring startup | 149.586 s | 65.603 s |
| Client readiness probe | 155.975 s | 71.044 s |
| Complete run, including startup | 281.635 s | 196.511 s |
| First command | 1996.867 ms | 1303.444 ms |
| Overall p50 / p95 (24 samples) | 3.835 / 249.771 ms | 3.268 / 191.659 ms |
| Cgroup memory peak | 236.434 MiB | 153.445 MiB |
| Cgroup memory at end | 235.867 MiB | 151.453 MiB |
| CPU consumed (including startup) | 16.370 s | 7.412 s |
| Throttled periods / total periods | 1633 / 1818 | 725 / 933 |
| Accumulated throttled time | 335.329 s | 87.656 s |
| Successful measured commands | 24 / 24 | 24 / 24 |
| OOM / OOM kill events | 0 / 0 | 0 / 0 |

Measured peak memory fell about35%, CPU consumed about55%, application startup
about55%. These are single-run observations, not statistical capacity claims.
Cgroup memory includes file cache; throttle time may accumulate across threads
and is not directly wall time. Readiness excludes startup of the client container.
The baseline harness stored first/p50/p95/max only; subsequent harness adds raw
samples and p95 excluding the first command. Tuned same-image p95 excluding first
is5.980ms, but a191.659ms reconnection-associated outlier remains. Baseline steady
p95 cannot be recovered from its aggregate data. Assertions were strengthened
after baseline to require DUMMY mode, enabled=true and PLANNED completion; the
requests and pacing are unchanged.

## Final implementation validation

Final image `sha256:3dfb45a3b29351d565a44c83591648e6935d7cdfde68a4d3fdf34a794095edb4` passed:

| Metric | Final code + tuned JVM |
| --- | ---: |
| Spring startup / process uptime | 64.297 / 72.409 s |
| Client readiness / total wall time | 77.046 / 202.387 s |
| First command | 1297.616 ms |
| Overall p50 / p95 | 3.810 / 4.748 ms |
| p95 excluding first command | 4.674 ms |
| Cgroup peak / final memory | 153.852 / 152.941 MiB |
| Cumulative CPU used | 8.188 s |
| Throttled periods / total | 801 / 1029 |
| Accumulated throttled time | 101.959 s |
| OOM / OOM kill events | 0 / 0 |

24/24 measured commands plus two DEVICE_COMMAND → simulated SUCCESS result →
EXECUTED completion exchanges pass. Eleventh command in the final connection is
correctly RATE_LIMITED. Packaged healthcheck exits0, Docker reports healthy. Both
task-owned containers removed after measurement. The low overall p95 in this run
does not imply every command is fast: the first still takes1.298s, and the previous
tuned run had a191.659ms outlier. Final image enables periodic health probes that
were absent in the baseline local image; do not attribute all differences to code.

Final latency samples in milliseconds (rounded to three decimals):
`1297.616, 3.522, 2.322, 2.494, 2.497, 3.743, 2.072, 3.060, 3.893, 4.748, 4.414, 3.810, 4.002, 4.460, 2.531, 4.051, 3.041, 3.603, 2.951, 4.674, 4.559, 4.094, 4.074, 1.836`.

Validation completed:

- `./mvnw -o verify` on an identical source copy under Linux `/tmp`: BUILD SUCCESS,
  63 tests, zero failures/errors/skips, JAR packaged. Source bytes verified against
  workspace; shared `docs/contract-fixtures` copied before successful full run.
- Initial workspace run also passed63 tests but packaging hit a `.jar.original`
  rename error on `/mnt/d`. `./mvnw -o -e -DskipTests package` retry succeeded;
  this workspace JAR is the final Docker input. The first isolated attempt lacked
  contract fixtures (two errors); restoring the fixture directory resolved it.
- `docker build -f Dockerfile.local -t dex-assistant-backend:resource-test .`: PASS.
- Compose base+low-resource configuration: PASS with synthetic digest.
- `docker run --rm -v "$PWD/scripts:/scripts:ro" python:3.12-slim python /scripts/perf/test_healthcheck.py`: PASS (200/UP,503/UP,200/DOWN,invalid body).
- Bash syntax, Python compilation, scoped diff whitespace and new document links: PASS.
- No commits/pushes, owner-data mounts or Android changes performed.

## Residual limits

No real Gemini/OpenAI, physical Android, production concurrency or long-duration
soak acceptance claimed. The conversation cap bounds entry count, not exact bytes.
Persistent quota windows/attempts still lack a retention policy; no records deleted.
