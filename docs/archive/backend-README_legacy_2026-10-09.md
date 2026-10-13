> Migrated to Android on 2026-10-13 ([D-037](../../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

> Historical README snapshot (2026-10-09). This documents earlier WebSocket/backend-era behavior and should not be used as the current setup guide. See [current README](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/README.md) or [Vietnamese](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/README.vi.md).

---

# DeX Assistant backend

Java 25/Spring Boot backend for the first text-command slice. It exposes an
authenticated raw JSON WebSocket, converts text into typed Android execution
plans, and runs with either a deterministic fake planner or a durable metered
AI-provider proxy.

## Build and test

```bash
./mvnw verify
```

Create a device credential; the command prints the plaintext token once and its
SHA-256 digest. Give the plaintext token to the client and configure only the
digest on the server.

```bash
java -jar target/dex-assistant-backend-0.1.0-SNAPSHOT.jar auth issue
export DEX_DEVICE_TOKEN_SHA256=<sha256-output>
java -jar target/dex-assistant-backend-0.1.0-SNAPSHOT.jar
```

The fake planner defaults to `DUMMY`, which completes after planning. For the
Android device-action demo, opt into remote execution explicitly:

```bash
export DEX_FAKE_REMOTE_EXECUTION=true
java -jar target/dex-assistant-backend-0.1.0-SNAPSHOT.jar
```

The local profile permits `ws://127.0.0.1:8080/ws/v1/assistant`; non-loopback
and Gemini deployments require TLS termination and `wss://`. When TLS terminates
at a reverse proxy, list its exact IP in `DEX_AUTH_TRUSTED_PROXY_ADDRESSES`; the
backend ignores forwarded-protocol headers from every other address.

Enable the proxy after configuring public provider/model aliases and
account-specific budgets under `dex.ai-proxy.providers`. Limits are runtime
configuration, never product constants. The H2 counter database survives
restart at `DEX_AI_QUOTA_DATABASE`; keep it outside the source tree. API keys
stay in environment/secret configuration.

```bash
export SPRING_PROFILES_ACTIVE=gemini,ai-proxy
export GEMINI_API_KEY=<secret>
export DEX_AI_QUOTA_DATABASE=/var/lib/dex-assistant/ai-quota
java -jar target/dex-assistant-backend-0.1.0-SNAPSHOT.jar
```

The proxy supports Gemini and OpenAI adapters, random eligible selection,
bounded fallback, independent fixed windows, and authenticated quota snapshots.
Spring AI model clients must be registered for configured providers; absent
clients are reported unavailable. Never put keys or account/project identifiers
in `dex.ai-proxy`.

### Local Java JAR test (without Docker)

After filling the ignored root `.env`, build with Java 25 and run:

```bash
bash ./mvnw verify
python3 scripts/ai/start-java-backend.py --java "$JAVA_HOME/bin/java"
curl -f http://127.0.0.1:8080/actuator/health/readiness
```

The launcher parses `.env` as literal assignments, without shell evaluation.
It preserves provider key/model/profile settings, binds only to `127.0.0.1`,
and explicitly enables the existing insecure-loopback exception for local tests.
An empty or example token digest uses the public local-test `avd-test-token`
identity documented below; a configured valid digest is preserved. It never
rewrites `.env` or prints credentials. Defaults use a temporary quota database
outside the repository; use `--quota-db "$HOME/.local/state/dex-test/ai-quota"`
to retain counters across restarts. `--port` and `--jar` are optional overrides.
Press Ctrl+C for graceful shutdown. For Android, run
`adb reverse tcp:8080 tcp:8080` and use `ws://127.0.0.1:8080/ws/v1/assistant`.

Validate the launcher with `python3 -m unittest discover -s scripts/ai -p 'test_start_java_backend.py'`.

### Local Docker and Android emulator test

Put the Gemini key in the ignored root `.env` as `GEMINI_API_KEY=...`; set
`GEMINI_MODEL` to a model available to that account and
`SPRING_PROFILES_ACTIVE=gemini,ai-proxy`. Provide `DEX_DEVICE_TOKEN_SHA256`
from the token-issuance command above. Compose forwards the key to the backend
and mounts a writable named volume for durable counters. The bundled proxy
profile applies a conservative local request budget of two requests per
minute (`DEX_GEMINI_TEST_RPM`), not a claimed provider allowance.

For a locally built and verified JAR, avoid downloading Maven dependencies
again in Docker:

```bash
./mvnw verify
docker build -f Dockerfile.local -t dex-assistant-backend:local .
docker compose up -d --no-build
curl -f http://127.0.0.1:8080/actuator/health/readiness
```

For the retained AVD, use `compose.avd.yml` as an additional local-test override.
It exposes port 8081 only on host loopback and relays to backend loopback without
rewriting transport headers. Run `adb reverse tcp:8081 tcp:8081`; the Android
endpoint is `ws://127.0.0.1:8081/ws/v1/assistant`. The override explicitly enables
the existing insecure-loopback exception for this local test. Production still
requires WSS.

For the complete isolated test setup after adding the Gemini key/model to
`.env`, run `bash scripts/ai/start-avd-backend.sh`. It builds the local runtime
image and starts the backend/relay with the public test identity. Provision the
AVD with the endpoint above and bearer token `avd-test-token`. After restarting
the backend container alone, restart `avd-loopback` too so it rejoins the new
network namespace.

`scripts/ai/docker-proxy-smoke.py` runs inside the backend network namespace to
exercise authentication, quota correlation, and a safe Gemini planning request.
It uses the public local-test token `avd-test-token`; configure its SHA-256
`adef2694be77e68f9153b149beb7fea4cb379033c2bf7a750a84da3826eeb5d7`
only for this isolated test. It reports action tool names or stable errors and
does not claim that planned actions were executed. `SNAPSHOT_ONLY=1` checks
counters without calling Gemini.

Build or run the container after copying `.env.example` to an untracked `.env`:

```bash
docker compose build
docker compose up -d
```

The integration test is the reproducible demo flow: it starts a random-port
server, authenticates a real WebSocket client, sends `bật wifi`, and verifies
accepted, typed-plan, and completed responses.

```bash
./mvnw -Dtest=WebSocketIntegrationTest test
```

See [../PROTOCOL.md](../PROTOCOL.md) for the wire contract.


T-011 adds capability-filtered `DEVICE_COMMAND` planning while preserving old V1
clients. See [schema and policy](../DEVICE_COMMANDS_V1.md) and
[local catalog](../LOCAL_CAPABILITIES_V1.md). Deploy the backend before the new
Android APK. Android enforces confirmation and permissions at execution time;
provider output cannot grant confirmation. Shared contract fixtures are tested
in Java and Kotlin. [Desktop-like AVD setup](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/scripts/android/README.md) supports
isolated regression without claiming Samsung DeX or physical voice acceptance.

For an optional 0.1 CPU / 1 GiB Docker profile and reproducible resource measurements,
see [resource tuning](../RESOURCE_TUNING.md).
