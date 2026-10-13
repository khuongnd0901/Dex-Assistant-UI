> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-009 direct Java backend continuation — 2026-10-04

Owner updated ignored.env,explicitly requests backend tests after Android using
java-jar and noDocker. Existing T-009/ExecPlan stays active;T-006/T-010 are not
replaced. Android measured latency slice is installed,published and verified in
T-006-history-latency-2026-10-04.md.

## Configuration/key check
Read.env in memory without shell evaluation/logging credentials. Key present,
profilesgemini,ai-proxy,modelgemini-3.5-flash-lite,testRPM2. Device token digest
remains placeholder. For isolated loopback test only,use existing public
avd-test-token identity via process environment,not owner production config.
Backend binds127.0.0.1;existing explicit insecure-loopback test exception only.
NoDocker installation/use.

Google Models.get with key header returnsHTTP200/346ms,supportedGenerationMethods
includesgenerateContent. No raw response/key exported. This proves authentication
and model metadata access,not inference/project billing tier/quota. Official
pricing lists configured3.5Flash-Lite Standard input/output Free Tier:
https://ai.google.dev/gemini-api/docs/pricing (checked2026-10-04).

## Build/runtime preparation
Backend owner downloads verified first-party Temurin25 to user-localopt,no
system changes. Current wrapper is tracked100644;use bash ./mvnw verify rather
than unnecessary source-mode change. Main owns.env/runtime/Gemini/physical tests.
Results follow.

## Physical isolation
Existing CoordinatorBackendDeviceTest would clear owner history and provision
owner credential alias. Android owner repairs test only:isolated history/context,
RAM socket token,real protocol listener forwarding,auth-failure interception to
protect fixed owner Keystore alias. Test will verify live backend multi-action
completion after observer detaches,not owner credential provisioning.
No production Android changes or mic/model/permission/protocol alteration.

## Verified runtime and initial integration
Verified Temurin25.0.4.1+1 archive SHA256
`dbb698396d478e7fa2b1e50f4103324b2a99b90569ee27c33f2261f9215cf41e`.
`JAVA_HOME=/home/kunrg/.local/opt/jdk-25.0.4.1+1 bash ./mvnw -B -ntp verify`
passes27 tests,zero failures/errors/skips after retrying transient dependency TLS
failure. Four launcher Python tests pass. Direct launcher starts Java JAR bound
127.0.0.1:8080;readinessHTTP200 UP. Missing/invalid bearer WS upgrades both401.
ADB reverse8081→8080 bridges Fold3 to the isolated backend.

Direct synthetic Gemini generateContent HTTP200/904ms,candidates1,usage
prompt4/output2/total6,serviceTierstandard. No raw response exported. This proves
inference works;does not establish account billing/free quota.

Android test-only isolated build passes64debug+64release unit/lint/testAPK.
Signed testAPK SHA256
`00811a7dd883627163bc98e1c79a839e5ee95a46286a9f7ef09918d97eb69572`
installed with original signing identity;production APK unchanged. Two earlier
invocations omitted explicit backendToken and SKIPPED;not acceptance. Correct
explicit publictesttoken invocation ran1test/2.466s and FAILED: expectedDISPATCHED,
actualAI_UNAVAILABLE. Backend sanitized log TRANSIENT/RuntimeException;local
quota used1/reserved0/remaining1. Owner-preservation cleanup assertions did not
fail. Adapter diagnosis/fix and actual rerun remain required.

## Final diagnosis and validation
Repeateddirectmetadata/simplegeneration/six-tool synthetic requests nowreturn
HTTP401. SanitizedJSONerrorcode401/statusUNAUTHENTICATED;no rawprovidererror
exported. Ignored.env mtime unchanged since08:23. GoogleSDK nestedClientException
also401;initialclaimadapterrequestbug was only a hypothesis and not established.
Finalstatus physicalrerun remainsFAIL AI_UNAVAILABLE;no semantic cloudaction
success claimed. Owner requested to check GoogleAIStudio APIkey and update.env
locally;never send credential throughchat. FreeTier availability is not auth.

Confirmedsourcebug: planner classified onlyouterSDKwrapper message,misclassifying
wrapped401/403/429 asTRANSIENT. Fix usesbounded typedApiException causechain:
401/403→AUTHENTICATION,429→QUOTA. Regressiontest usesopaque wrapper/messages.
Permanentlogging is one sanitizedcategory/causeType/numericHTTPstatus line;
no messages/payloads/stacktraces. FinalJava25 fullMavenverify28testsPASS,zero
failures/errors/skips,BUILD SUCCESS32.123s. Pythonlauncher4testsPASS.
README documents safe .env parser/directJava command,loopback/publictesttoken/
externalquotaDB/Androidreverse. NoDocker used. Owner.env ignored/mode0600.
FinalJAR SHA256: `0205bdc4cbdb6947fe3c6330e7feebc5405b65af73d5ef12425d3140e4dc3b9b`.

Runtime restart uses same externalH2path `/tmp/dex-java-jar-runtime/ai-quota`;
launch command:
```bash
python3 scripts/ai/start-java-backend.py --java /home/kunrg/.local/opt/jdk-25.0.4.1+1/bin/java --quota-db /tmp/dex-java-jar-runtime/ai-quota
```
Health/auth/quota finalchecks follow. BroaderT-009 acceptance staysIN_PROGRESS.

Finalreadback: latestJAR readinessHTTP200 UP,WSmissing/invalidtoken401,
authenticatedquota AVAILABLE used0/reserved0/remaining2 innewminute. Existing
Accessibility service remainsenabled. ProductionAndroidAPKunchanged. Backend
sourcepublished8872beeb9d6cf8f16f4d0f80c35968756127d614;Androidtest/status
published e142e9e55b448622c2c1fc4467ec976792b789e2,cleanHEADorigin.
Backendleft runningforowner on127.0.0.1:8080,log
`/tmp/t009-java-launcher-backend.log`;ADB reverse8081→8080 retained.
Noextra realprovider calls afterauthentication failure diagnosis. To reload
updated.env gracefullystop identifiedJavaPID and rerunlauncher,thenrunexplicit
backendEndpoint/backendToken isolatedtest. BroaderT-009 remainsIN_PROGRESS.

## Owner key update — successful rerun2026-10-04
Ownerconfirmednewkey locally. Models.getHTTP200;gracefullystopped identifiedold
JAR process and reloaded.env viaexistinglauncher/sameexternalquotaDB. Readiness
HTTP200. SameinstalledoriginalsignedisolatedtestAPK,explicitendpoint
ws://127.0.0.1:8081/ws/v1/assistant andpublictesttoken:
`CoordinatorBackendDeviceTest` PASS1test4.186s. Assertions requireDISPATCHED
forYouTube→Chrome compoundcommand,detachedobserverunchanged,ownerconfig/
credentialprefs unchanged andcredential/historyKeystorealiaspresence preserved.
This verifies realAndroid→JavaJAR→Gemini→Androidaction flow;doesnot establish
naturalspeech orproductioncredentialprovisioning acceptance. No source/test/
productionAPK changes;existing28Maven+4launcher tests remainapplicable.

Finalmissing/invalidWSauth both401;authenticatedquota AVAILABLE used0/reserved0/
remaining2 inlatestminute (windowrolled;notclaimingzeroearlierconsumption).
Accessibility remainsenabled. Backendleft runningloopback8080 withreverse8081,
log /tmp/t009-java-launcher-backend.log. Prior401blockerresolved. Accountbilling/
FreeTierquota andbroaderT-009 acceptance unverified;T-009 staysIN_PROGRESS.
