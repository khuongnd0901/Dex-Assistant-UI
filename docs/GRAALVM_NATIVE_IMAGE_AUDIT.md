> Migrated to Android on 2026-10-13 ([D-037](../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# GraalVM Native Image Migration Audit

Audit date: 2026-10-05. Documentation task: T-012.

This document preserves the finalized read-only technical audit. Saving the
audit does not authorize implementation. All migration commands below are
proposals and were not executed during the audit. Versions and file references
describe the inspected snapshot, not a guarantee about future repository state.

## Executive Summary

**Overall feasibility: MODERATE.** The application appears suitable for GraalVM
Native Image without changing its Java major version, Spring Boot version, or
architecture.

The strongest migration risks are:

- Jackson serialization in the custom WebSocket handler, especially payloads
  declared as `Object`.
- Profile-dependent bean creation that becomes fixed during AOT processing.
- Validation of the complete AI-provider and persistent H2 execution paths.
- Adapting JVM-oriented tests, launchers, and containers.

**No fundamental Native Image incompatibility was identified.** Native
compilation and runtime compatibility remain unverified.

The audit inspected source, configuration, cached Maven POMs, and the existing
packaged JAR. Its embedded POM exactly matched the repository POM. Existing
reports recorded **57 passing JVM tests**; these were not rerun. No builds,
dependency resolution, installations, or application launches were performed.
Pre-existing working-tree changes were left untouched.

## Current Stack

Versions come from [pom.xml](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/pom.xml), the
[Maven wrapper](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/.mvn/wrapper/maven-wrapper.properties), cached parent/BOM
files, and the existing `target/dex-assistant-backend-0.1.0-SNAPSHOT.jar`.
The JAR and cached files are local audit evidence, not committed artifacts.

| Area | Current baseline |
| --- | --- |
| Java | **25**, enforced as `[25,26)` |
| Installed JDK / previous test runtime | Eclipse Adoptium **25.0.4.1** |
| Spring Boot parent | **4.0.8** |
| Spring Framework | **7.0.9** |
| Spring AI BOM | **2.0.1** |
| Maven | **3.9.16**; wrapper **3.3.4** |
| Packaging | Executable JAR; Maven's default `jar` packaging |
| Application version | `0.1.0-SNAPSHOT` |
| Server | Embedded Tomcat **11.0.24**, Servlet MVC and raw WebSocket |
| Database | H2 **2.4.240**, JDBC; file-backed quota database |
| Security | Spring Security **7.0.7**, custom bearer-token handshake |
| Observability | Actuator, Micrometer **1.16.7**, Prometheus client **1.4.3** |
| JSON | Jackson **3.1.5** in application code; Jackson **2.21.5** and Gson **2.13.2** also packaged transitively |
| Deployment | Temurin 25 JRE container, non-root user, writable `/data`, read-only root filesystem |

**Build plugins:** explicitly declared Spring Boot Maven plugin **4.0.8** and
Maven Enforcer **3.6.2**. Parent-managed lifecycle versions include Compiler
**3.14.1**, Surefire **3.5.6**, Resources **3.3.1**, and JAR **3.4.2**.

**Entry point:**
[DexAssistantApplication.main](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/src/main/java/com/dexassistant/backend/DexAssistantApplication.java)
(line 12). Besides starting Spring, it supports `auth issue`.

**Configuration:** one multi-document
[application.yml](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/src/main/resources/application.yml), binding nested records
through `DexProperties`.

- Default: fake planner, AI proxy disabled.
- `gemini`: enables Google GenAI chat.
- `ai-proxy`: enables quota persistence and metered planning.
- The documented live-provider launch uses **`gemini,ai-proxy` together**.
- Compose defaults to `default` unless overridden. The running deployment's
  profile was not inspected.

**Direct starters:** web, websocket, security, validation, actuator, JDBC,
Google GenAI, and test. Additional direct dependencies are Spring AI OpenAI,
H2, Prometheus registry, Spring Security test, and ArchUnit **1.4.1**.

**Other relevant packaged libraries:** Google GenAI **1.65.0**, OpenAI Java core
**4.49.0**, Google Auth **1.33.0**, Google HTTP Client **1.46.2**, Apache
HttpClient **4.5.14**, OkHttp **4.12.0**, Kotlin reflection **2.2.21**, Reactor
**3.8.7**, protobuf **3.25.5**, and gRPC API/context **1.70.0**.

No application code-generation plugins or annotation processors are configured.
Google SDK dependencies contain pre-generated AutoValue classes and AutoValue
annotations **1.11.0**.

## Native Image Compatibility

### Existing native support

| Item | Finding |
| --- | --- |
| Application AOT configuration | None explicitly configured |
| Native Build Tools | Plugin not declared; **0.11.5** managed by Boot parent |
| Native Maven profiles | `native` and `nativeTest` inherited from Boot parent; no project-specific profiles |
| Application `RuntimeHints` | None |
| Application reflection/resource/proxy/serialization JSON | None |
| Application `native-image.properties` | None |
| Existing JAR AOT-generated application assets | None found |
| Dependency native support | Present in Spring, Tomcat, Google GenAI, OpenAI SDK, H2, and other libraries |

The inherited profile is useful groundwork, but the project still needs the
native plugin declaration and a native build path. This matches
[Spring Boot's Maven native setup](https://docs.spring.io/spring-boot/4.0/how-to/native-image/developing-your-first-application.html).

### Source scan results

- **Reflection:** no explicit production reflection calls found. One test
  accesses a private field.
- **Dynamic loading:** no custom class loaders, runtime JAR loading, plugin
  system, scripting engine, or application `ServiceLoader` usage.
- **AOP/proxies:** ordinary Spring configuration enhancement; no custom proxies
  or `@Aspect`. `MeteredPlannerProxy` is a normal Java class, despite its name.
  Spring AOT handles ordinary configuration proxies at build time.
- **Serialization:** custom Jackson WebSocket binding needs attention. No
  application Java serialization, XML serialization, Avro, or Kryo usage.
  Protobuf is transitive.
- **Resources:** only `application.yml` under main resources. Tool schemas and
  SQL are Java literals. Contract fixtures are read from `docs/` by tests.
- **Spring wiring:** conditional beans and
  `ListableBeanFactory.getBeansOfType` are present. No custom bean-definition
  post-processors, `FactoryBean`, application SpEL, Spring Data repositories,
  Spring Batch, or Spring Cloud.
- **Scheduling:** explicit scheduled executor and virtual-thread executors;
  no annotation-driven scheduler.
- **JNI:** no application native methods, `System.loadLibrary`, or bundled
  `.so`/`.dll` files found in the inspected runtime dependencies. Some
  dependencies ship JNI metadata; that alone does not establish a required
  external native library.
- **Infrastructure absent:** Oracle JDBC, Redis, IBM MQ, ActiveMQ/Artemis,
  RabbitMQ, Kafka, Netty, and OpenTelemetry agents. Spring Messaging and gRPC
  API/context dependencies do not establish broker or gRPC transport usage.

## P0 Blockers

**No confirmed architectural or dependency P0 blocker.**

Two prerequisites prevent the current deployment workflow from producing and
running native artifacts:

1. [pom.xml](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/pom.xml) (line 100) does not declare Native Build Tools.
   `native-image` was not available on the audit environment's PATH.
2. [Dockerfile](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/Dockerfile), [Dockerfile.local](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/Dockerfile.local), and the
   [launcher](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/scripts/ai/start-java-backend.py) build or execute a JVM JAR.

These are bounded build/deployment changes, not evidence that the application
cannot migrate.

## P1 High-Risk Findings

| File / symbol | Why it may fail | Possible native solution |
| --- | --- | --- |
| [AssistantWebSocketHandler](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/src/main/java/com/dexassistant/backend/adapter/in/websocket/AssistantWebSocketHandler.java), lines 86, 135, 299: `readValue`, `treeToValue`, `writeValueAsString` | Custom handler serialization lacks the controller signatures from which Spring commonly infers binding hints. Records being reachable does not guarantee reflective binding metadata. | Register inbound/outbound DTOs with `@RegisterReflectionForBinding` or a focused `RuntimeHintsRegistrar`; exercise every message type. |
| [ProtocolResponse.payload](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/src/main/java/com/dexassistant/backend/adapter/in/websocket/ProtocolResponse.java), line 7; [ActionPayload.arguments](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/src/main/java/com/dexassistant/backend/adapter/in/websocket/ExecutionPlanPayload.java), line 15 | `Object` hides concrete payload types. Registering the containing record alone cannot describe every runtime value. | Explicitly cover all seven argument records, `ErrorPayload`, `ExecutionPlanPayload`, and `QuotaSnapshot` with its nested records. Preserve the wire format. |
| [ApplicationConfiguration](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/src/main/java/com/dexassistant/backend/config/ApplicationConfiguration.java), line 98 onward: conditional planner, datasource and quota beans | A binary AOT-processed with default configuration will not acquire the AI-proxy bean graph when profiles change at runtime. | Build with the intended profile combination; initially use separate artifacts for different bean graphs. Refactor to a stable graph only if one switchable binary is required. |

Spring documents both the fixed bean-graph restriction and build-time proxy
generation in its
[Native Image overview](https://docs.spring.io/spring-boot/4.0/reference/packaging/native-image/introducing-graalvm-native-images.html).
Explicit binding registration is supported through its
[custom-hints mechanism](https://docs.spring.io/spring-boot/4.0/reference/packaging/native-image/advanced-topics.html).

These are high-risk findings, **not observed native failures**.

## P2 Migration Work

| Location / symbol | Required work and native approach |
| --- | --- |
| [ApplicationConfiguration.meteredPlanner/findModel](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/src/main/java/com/dexassistant/backend/config/ApplicationConfiguration.java), line 122 | Lookup queries existing beans; it does not load new classes. Verify intended `ChatModel` beans exist in the AOT graph. Name-based selection can otherwise silently omit a provider. |
| [SpringAiProviderPlanner.plan](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/src/main/java/com/dexassistant/backend/adapter/out/llm/SpringAiProviderPlanner.java), line 28 | Validate real SDK requests, tool-call responses, authentication failures, cancellation and timeout paths. SDK metadata is present, but completeness for this application is unproven. |
| [ApplicationConfiguration.quotaDataSource](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/src/main/java/com/dexassistant/backend/config/ApplicationConfiguration.java), line 107 | Validate H2 file persistence, JDBC discovery, locking and `AUTO_SERVER=TRUE`. H2 ships reflection/resource metadata, including `TcpServer`; do not assume mixed mode is unsupported or remove it without evidence. |
| [JdbcQuotaService.initialize](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/src/main/java/com/dexassistant/backend/adapter/out/llm/JdbcQuotaService.java), line 37 | SQL is inline, so no SQL resource registration is needed. Verify transactions, concurrent reservations and restart continuity against an isolated database. |
| [DexProperties](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/src/main/java/com/dexassistant/backend/config/DexProperties.java), line 7 | Spring AOT should infer configuration binding. Validate nested provider/budget lists and durations; no speculative blanket hints needed. |
| [InMemoryConversationStoreTest](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/src/test/java/com/dexassistant/backend/application/InMemoryConversationStoreTest.java), line 33 | `getDeclaredField`/`setAccessible` is test-only. Keep on JVM, or supply test-only hints/use a behavior assertion if moving this test to native. |
| Mockito-based tests and [ArchitectureTest](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/src/test/java/com/dexassistant/backend/architecture/ArchitectureTest.java), line 12 | Mockito instrumentation and ArchUnit bytecode inspection complicate compiling the entire test suite natively. Retain JVM tests and add native-compatible integration coverage. |
| [DeviceCommandContractTest](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/src/test/java/com/dexassistant/backend/adapter/out/llm/DeviceCommandContractTest.java), line 23 | Fixture loading assumes repository-relative `docs/`. Keep these JVM tests or package fixtures specifically for native tests. They are not production resources. |
| [Dockerfile](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/Dockerfile), line 16; [launcher](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/scripts/ai/start-java-backend.py), line 76 | Replace JAR execution for the native variant. Retain `/data` permissions, TLS trust, graceful shutdown and health checks. The current health check requires Bash and grep, which minimal images may lack. |

`application.yml` should be handled by Boot. Dependency resources—including
H2's `org/h2/util/data.zip`, SDK service descriptors and TLS-related
resources—need runtime validation before adding targeted registrations.

No broad source refactoring is currently justified. **P3 optimizations**,
deferred until correctness: remove demonstrably unused SDKs, reduce image size,
tune native memory/GC, or disable unnecessary configuration proxying.

## Dependency Compatibility Matrix

Versions and embedded-metadata findings were verified from the existing
packaged JAR. “SUPPORTED” describes framework support, not successful native
validation of this application.

| Dependency | Current version | Native compatibility | Risk | Action |
| --- | --- | --- | --- | --- |
| Boot web/WebSocket starters; Spring Framework | 4.0.8 / 7.0.9 | SUPPORTED | Low | Retain; test authenticated WebSocket lifecycle |
| Tomcat core/WebSocket | 11.0.24 | LIKELY SUPPORTED; embedded metadata | Low–medium | Verify upgrade, close and shutdown paths |
| Spring Security | 7.0.7 | SUPPORTED; AOT registrars present | Low | Retain; test handshake and trusted-proxy rules |
| Validation / Hibernate Validator | 4.0.8 / 9.0.1.Final | LIKELY SUPPORTED through Boot | Low | Validate any reachable constraint processing |
| JDBC / Spring transactions | 4.0.8 / 7.0.9 | SUPPORTED | Low | Keep explicit JDBC/transaction approach |
| H2 | 2.4.240 | LIKELY SUPPORTED; exact mixed-mode path UNKNOWN — NEEDS VALIDATION | Medium | Test file DB, `AUTO_SERVER`, locks and restart |
| HikariCP | 7.0.2 | LIKELY SUPPORTED through Boot | Low | Custom quota datasource uses `DriverManagerDataSource`; check default-profile datasource too |
| Spring AI Google GenAI | 2.0.1 | LIKELY SUPPORTED; provider AOT registrar present | Medium | Retain; test actual provider flow |
| Google GenAI SDK | 1.65.0 | LIKELY SUPPORTED; extensive AutoValue/reflection/proxy/resource metadata | Medium | Validate SDK serialization and HTTPS |
| Spring AI OpenAI / OpenAI Java core | 2.0.1 / 4.49.0 | LIKELY SUPPORTED; SDK native metadata present | Medium | First establish whether an OpenAI model bean is intended |
| Google Auth / HTTP Client / Gson adapter | 1.33.0 / 1.46.2 | LIKELY SUPPORTED; native metadata present | Medium | Exercise the configured authentication and HTTP paths |
| Jackson application binding | 3.1.5 | REQUIRES CONFIGURATION | High | Register custom WebSocket DTOs and concrete `Object` payloads |
| Jackson SDK binding / Kotlin reflection | 2.21.5 / 2.2.21 | REQUIRES CONFIGURATION, partly supplied by SDK metadata | Medium | Preserve SDK versions; validate binding paths |
| Gson | 2.13.2 | REQUIRES CONFIGURATION for reflective model binding | Medium | Verify SDK-supplied coverage; no direct app Gson use |
| Apache HttpClient / OkHttp / Okio | 4.5.14 / 4.12.0 / 3.6.0 | LIKELY SUPPORTED | Medium | Test DNS, HTTPS, trust, timeouts and cancellation |
| Actuator / Micrometer / Prometheus | 4.0.8 / 1.16.7 / 1.4.3 | LIKELY SUPPORTED; Micrometer metadata present | Low | Verify health and intended metric access |
| Logback / SLF4J / Log4j bridge | 1.5.38 / 2.0.18 / 2.25.5 | LIKELY SUPPORTED | Low | Retain standard Boot logging |
| Reactor | 3.8.7 | LIKELY SUPPORTED; metadata present | Low | Validate reachable SDK paths |
| gRPC API/context; protobuf | 1.70.0 / 3.25.5 | UNKNOWN — NEEDS VALIDATION if reachable | Low–medium | No application gRPC transport or protobuf contract found |
| JSON-schema generator / validator | 5.0.0 / 3.0.1 | UNKNOWN for reflective generation; validator metadata present | Low–medium | App builds tool schemas explicitly; inspect reachability before adding hints |
| StringTemplate / ANTLR / jtokkit | 4.3.4 / 3.5.3 + 4.13.1 / 1.1.0 | UNKNOWN — NEEDS VALIDATION if reachable | Low–medium | No direct application usage; avoid speculative remediation |
| Test starters / Mockito / Byte Buddy / ArchUnit | 4.0.8 / 5.20.0 / 1.17.8 / 1.4.1 | HIGH RISK for wholesale native test compilation | Test-only | Retain JVM suite; introduce selected native tests |

**No dependency upgrade is recommended at this stage.**

## Recommended Version Matrix

| Component | Current | Recommended migration target |
| --- | --- | --- |
| Java | 25; installed Temurin 25.0.4.1 | **Java 25**, unchanged language level |
| Spring Boot | 4.0.8 | **4.0.8** |
| Spring Framework | 7.0.9 | **7.0.9**, Boot-managed |
| Spring AI | 2.0.1 | **2.0.1** |
| GraalVM | Not configured; executable absent from audit PATH | **GraalVM for JDK 25**, pin the selected distribution/build |
| Native Build Tools | Not enabled; parent manages 0.11.5 | **0.11.5**, enable the managed Maven plugin |
| Maven | 3.9.16 | **3.9.16** |

Boot 4.0.8 explicitly supports GraalVM 25 and Native Build Tools 0.11.5. This
also respects the repository's Java `[25,26)` enforcement. No Boot or Java
major upgrade is necessary. See
[Boot 4.0 system requirements](https://docs.spring.io/spring-boot/4.0/system-requirements.html).

## Migration Plan

**Proposed only. None of these commands was executed.** Future builds/tests
should use isolated test credentials, temporary quota storage, and an
explicitly selected build-time bean graph. This proposal is not an active
implementation ExecPlan; a separately authorized migration task must follow
the repository's planning requirements.

| Phase | Goal / files likely to change | Main risk | Validation command | Success criteria |
| --- | --- | --- | --- | --- |
| **0 — JVM baseline** | Establish reproducible behavior. Tests and smoke harness only if coverage gaps require changes. | Mistaking fake-provider success for real integration success | `./mvnw verify` | Current suite passes; capture JVM startup/RSS and live-provider baseline |
| **1 — AOT/tooling** | Add managed native plugin and build-profile wiring in `pom.xml`; define artifact/profile policy. | Wrong bean graph | `SPRING_PROFILES_ACTIVE=gemini,ai-proxy ./mvnw -Pnative -DskipTests package` | AOT generation succeeds; expected model, quota and planner definitions present |
| **2 — First compilation** | Obtain production-shaped native binary; `pom.xml` and narrowly required hints. | Reachability, SDK initialization, compiler resources | `SPRING_PROFILES_ACTIVE=gemini,ai-proxy ./mvnw -Pnative -DskipTests native:compile` | Native executable starts with runtime configuration and reaches readiness |
| **3 — Hints/resources** | Add DTO binding hints and focused tests; change wiring only for demonstrated failures. | Hidden `Object` payloads and unexercised errors | Repeat native compilation; `./mvnw -PnativeTest test` after configuring a compatible test subset | All wire payload variants and error paths work; resources/proxies resolve |
| **4 — Integration** | Add/extend external native smoke harness and integration tests. | Provider, persistence and cancellation differences | `python3 scripts/ai/docker-proxy-smoke.py` against an isolated configured test instance; extend harness for missing cases | Authentication, provider calls, quota exhaustion, restart, reconnect, deduplication and cancellation pass |
| **5 — Native container** | Native Dockerfile/launcher variant and Compose configuration. | ABI, missing CA certificates/shell, filesystem permissions | `docker build -f Dockerfile.native -t dex-assistant-backend:native .` **after creating that proposed file** | Non-root/read-only container works; `/data`, health checks and termination verified |
| **6 — Performance** | Add repeatable comparison harness and record results. | Provider latency obscures backend performance; throughput regression | `/usr/bin/time -v …` for both launch modes, plus the same fixed-workload harness | Compare startup, idle/load RSS, backend p50/p95, CPU and sustained throughput against agreed thresholds |
| **7 — Production readiness** | Add release/CI configuration and operational documentation. | Wrong architecture/profile artifact, rollback or persistence failure | Re-run `./mvnw verify`, selected native tests and container smoke suite in release CI | Reproducible target-platform build, soak test, restart recovery and JVM rollback demonstrated |

Native tests complement the JVM suite; they do not need to replace every JVM
unit or architecture test. Spring documents both JVM-based and native testing
approaches in
[Testing GraalVM Native Images](https://docs.spring.io/spring-boot/4.0/how-to/native-image/testing-native-applications.html).

## Estimated Effort

Assuming one experienced engineer, available test credentials, one Linux
architecture and a pinned `gemini,ai-proxy` artifact:

| Work | Engineering days |
| --- | ---: |
| JVM baseline and acceptance setup | 0.5–1 |
| Tooling and initial native build | 1–2 |
| AOT, binding and dependency corrections | 2–4 |
| Integration testing | 3–5 |
| Performance validation | 1–2 |
| Container and production hardening | 2–3 |
| **Total** | **Approximately 10–17 days** |

The application is relatively small—**54 production Java files**—with explicit
tool mapping and no plugin architecture or ORM. That limits refactoring. Most
effort is likely to be validation of WebSocket contracts, SDK behavior and
durable quota state.

Multiple native platforms, a working OpenAI integration, or a single binary
supporting runtime bean-graph changes would expand this estimate.

## Open Questions

- Is the target Linux x86-64 only, or are ARM64/Windows artifacts required?
- Can each native artifact have a fixed profile combination?
- Is OpenAI production scope? The library is present, but no OpenAI starter
  or explicit `OpenAiChatModel` bean was found.
- Must H2 retain `AUTO_SERVER=TRUE` for multiple-process access?
- What startup, memory and sustained-throughput improvements justify migration?
- Is Android/device acceptance required for rollout? Repository status at audit
  time recorded physical-device acceptance still outstanding.

An existing configuration nuance should be clarified during baseline testing:
`gemini` alone enables a model, while the planner wiring depends on
`dex.ai-proxy.enabled`; `dex.planner.provider` does not select a Gemini planner
in the inspected configuration.

## Recommended Next Step

In a separately authorized implementation task, build a **Java 25 / Boot 4.0.8
native prototype using the `gemini,ai-proxy` bean graph**, add focused WebSocket
binding hints, and validate one authenticated provider round trip plus quota
persistence across restart. Retain all current dependency versions initially.

The following status refers to the original read-only audit. The later T-012
documentation task saved this report and updated the task/status index only.

AUDIT STATUS: COMPLETE

FILES MODIFIED: NONE
