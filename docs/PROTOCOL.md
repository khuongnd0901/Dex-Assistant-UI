> Migrated to Android on 2026-10-13 ([D-037](../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# DeX Assistant WebSocket protocol v1

Connect to `/ws/v1/assistant` with `Authorization: Bearer <opaque-token>`.
Production transport is WSS. Messages are UTF-8 JSON and limited to 16 KiB.

Every message uses this envelope:

```json
{
  "version": 1,
  "type": "command.request",
  "messageId": "b6ea9785-b06c-4ab4-9f49-98bc760ff165",
  "correlationId": null,
  "sentAt": "2026-09-30T00:00:00Z",
  "payload": {}
}
```

## Client request

```json
{
  "version": 1,
  "type": "command.request",
  "messageId": "b6ea9785-b06c-4ab4-9f49-98bc760ff165",
  "correlationId": null,
  "sentAt": "2026-09-30T00:00:00Z",
  "payload": {
    "conversationId": "primary",
    "text": "bật wifi",
    "locale": "vi-VN"
  }
}
```

The server sends `command.accepted`, then `execution.plan`. Dummy plans finish
with `command.completed` immediately. For remote plans the client sends one
`action.result` per action; the server completes after all succeed or after the
first failure/rejection. An action contains an `actionId`, allowlisted `tool`,
`executionMode`, and tool-specific `arguments`:

```json
{
  "actionId": "2e447fac-e575-4d68-8673-367f6d8f2f44",
  "tool": "TOGGLE_WIFI",
  "executionMode": "DUMMY",
  "arguments": { "enabled": true }
}
```

V1 tools are `TOGGLE_WIFI`, `CONNECT_WIFI`, `LAUNCH_APP`, `SPLIT_SCREEN`,
`CLICK_UI`, and `INPUT_TEXT`. `CONNECT_WIFI` carries an SSID only; credentials
must remain on Android for action execution and must not enter V1 request text
or tool arguments. Under D-016, the separate Android system speech service may
process sensitive command audio online; V1 still carries no audio or passwords.

Errors use type `error` and payload fields `code`, `message`, and `retryable`.
Stable codes include `INVALID_MESSAGE`, `MESSAGE_TOO_LARGE`, `RATE_LIMITED`,
`UNSUPPORTED_MESSAGE`, `COMMAND_IN_PROGRESS`, `INVALID_COMMAND`,
`SENSITIVE_INPUT_REJECTED`, `UNSUPPORTED_COMMAND`, `INVALID_LLM_OUTPUT`,
`LLM_TIMEOUT`, `LLM_UNAVAILABLE`, `AI_QUOTA_EXHAUSTED`, `AI_TIMEOUT`,
`AI_UNAVAILABLE`, and `INTERNAL_ERROR`. New servers use the three `AI_*` codes
for provider-proxy terminal failures; old `LLM_*` codes remain client-compatible.
Android renders all three `AI_*` codes as `Khó quá, không làm được`.

An action result payload contains `planId`, `actionId`, `status` (`SUCCESS`,
`FAILED`, or `REJECTED`), and an optional non-sensitive `errorCode`.

Android V1 uses these stable device error codes: `DEVICE_OWNER_REQUIRED`,
`POLICY_DENIED`, `APP_NOT_FOUND`, `ACTION_TIMEOUT`, and
`DEVICE_STATE_MISMATCH`. A client must reject unsupported actions rather than
reporting synthetic success.

Only one command may be active per connection. The command-rate limiter counts
`command.request`; validated `action.result` replies do not consume that quota.
All frames retain the configured UTF-8 byte-size limit. Pending device plans
expire after 120 seconds from publication and finish with status `FAILED` if no
terminal result arrives. Provider planning has its own timeout; this is not a
120-second total backend command budget. Dummy steps need no device result.

Re-sending a completed `messageId` on the same connection returns its cached
`command.completed` response (most recent 100 requests) without planning again.
Admission and deduplication are per connection, not shared across reconnects.

After an ambiguous disconnect the Android client does not automatically submit
the command again. An explicit retry reuses the original `messageId`; the cache
suppresses duplicate planning only if that connection still owns the cached
response. A new connection must not be assumed to provide replay protection.
Disconnect cancels pending timers and requests cancellation of provider work;
SDK network cancellation remains best effort.

## Quota monitoring extension

An authenticated client may send an additive V1 `quota.snapshot.request` while
no command or action-result semantics change:

```json
{"version":1,"type":"quota.snapshot.request","messageId":"b6ea9785-b06c-4ab4-9f49-98bc760ff165","correlationId":null,"sentAt":"2026-10-02T00:00:00Z","payload":{}}
```

The server replies once with `quota.snapshot`, correlated to the request. This
read uses a separate limiter and does not consume the command rate limit or the
one-command-in-flight slot. The client requests a fresh snapshot after each
authenticated reconnect; it never resubmits a command automatically.

```json
{
  "version": 1,
  "type": "quota.snapshot",
  "messageId": "39238a3a-3a29-45b5-b194-c459ed73b015",
  "correlationId": "b6ea9785-b06c-4ab4-9f49-98bc760ff165",
  "sentAt": "2026-10-02T00:00:01Z",
  "payload": {
    "schemaVersion": 1,
    "snapshotAt": "2026-10-02T00:00:01Z",
    "staleAfter": "2026-10-02T00:05:01Z",
    "providers": [{
      "providerAlias": "gemini-primary",
      "modelAlias": "flash",
      "state": "AVAILABLE",
      "budgets": [{
        "unit": "REQUEST",
        "used": 4,
        "reserved": 1,
        "limit": 100,
        "remaining": 95,
        "windowStart": "2026-10-02T07:00:00Z",
        "resetAt": "2026-10-03T07:00:00Z",
        "source": "ESTIMATED",
        "observedAt": "2026-10-02T00:00:01Z"
      }]
    }]
  }
}
```

Provider `state` is `AVAILABLE`, `EXHAUSTED`, `UNAVAILABLE`, or `UNKNOWN`.
Budget `unit` is `REQUEST`, `INPUT_TOKEN`, `OUTPUT_TOKEN`, or `TOTAL_TOKEN`;
`source` is `ESTIMATED` or `PROVIDER_REPORTED`. Numeric/window fields that are
unknown are JSON `null`, never zero or fabricated. `remaining` is computed only
from an enforceable configured budget or supplied by the provider. A snapshot
is stale at or after `staleAfter`; a disconnected client also marks cached data
stale. Only public aliases and aggregate counters are exposed.

Older clients may ignore the new response type. Older servers return
`UNSUPPORTED_MESSAGE`; Android then displays quota as unknown. Envelope version,
authentication, 16 KiB limit, correlation, reconnect, and command duplicate
rules are unchanged.

## Config-driven client extension

See [Device command schema 1](DEVICE_COMMANDS_V1.md) and [additive schema 2](DEVICE_COMMANDS_V2.md) for optional `clientCapabilities` and validated `DEVICE_COMMAND`. Schema 2 adds `CLICK_TARGET` and `APP_CLOSE_WINDOW`; each action is advertised only after its Android executor is available and validated. Envelope V1 and legacy clients remain compatible. Roll out the backend before the new APK. Confirmation and permissions are enforced on Android.

## Chat session extension (schema 1)

Chat uses the authenticated V1 envelope and frame limit above. It is additive
and has no action-plan or device-command semantics. Clients treat unknown Chat
types as unsupported; V1 Command messages and deduplication are unchanged.
`sessionId` is a random UUID generated for one Chat session and is also its
conversation identity. It is never a user identity. Current authentication
represents the configured shared device credential only.

Client types: `chat.session.start`, `chat.turn.request`, `chat.session.end`.
Server types: `chat.session.started`, `chat.turn.response`,
`chat.session.ended`, `chat.error`. Every server reply correlates to the
triggering `messageId`. Start creates fresh volatile context; a second active
session is rejected. Each client `messageId` is unique within its session. A
duplicate in-flight turn ID does not start another generation; a completed
turn ID returns only its cached response within the bounded live-session replay
cache. End, 15-minute idle expiry, connection close, Stop/Pause, and server
restart end the session and release context. Reconnect requires a new explicit
start and never replays a turn. A session accepts at most 200 completed
user/assistant turns and one in-flight turn.

Payloads:

- `chat.session.start`: `{ "sessionId": "<UUID>", "locale": "vi-VN" }`.
- `chat.session.started`: `{ "sessionId": "<same UUID>", "idleTimeoutSeconds": 900 }`.
- `chat.turn.request`: `{ "sessionId": "<UUID>", "text": "...", "localTime": { "timestamp": "<RFC3339 offset timestamp>", "timeZone": "<IANA zone>" } }`.
- `chat.turn.response`: `{ "sessionId": "<UUID>", "text": "...", "turn": 1 }`.
- `chat.session.end`: `{ "sessionId": "<UUID>", "reason": "USER" }`, where reason is `USER` or `STOP`.
- `chat.session.ended`: `{ "sessionId": "<UUID>" }`.
- `chat.error`: `{ "sessionId": "<UUID or null>", "code": "...", "message": "...", "retryable": false }`.

`localTime` is required explicit context, not a claim of live web access. Text
is final recognized text only; raw audio, screen, contacts, credentials, and
Command history are forbidden. Count input by Unicode code points (maximum
2,000), independently of UTF-8 bytes and the envelope's 16 KiB frame limit.
Responses are complete, non-streaming text, capped at 1,024 generated model
tokens or 4,000 Unicode code points, whichever is reached first. Prompt input,
including system instructions, time, retained context, and current text, is
capped at 8,000 model tokens. Retain at most 20 complete user/assistant pairs,
evicting whole oldest pairs to satisfy both limits. Token counts use the
selected model tokenizer or a conservative documented bound. Failed, cancelled,
expired, or stale turns commit no pair. No Chat provider tools may be
registered; tool-call output is an error and is never routed to Android actions.

The current backend conservatively counts UTF-8 bytes plus fixed role/time
overhead as token bounds and sets Gemini's maximum output to 1,024 tokens.
It may reject shorter text than the numeric token/code-point ceilings imply.
To bound deduplication memory, one live session accepts at most 1,000 distinct
turn IDs, including failed attempts; exhausted sessions return `CHAT_TURN_LIMIT`.

Stable Chat error codes: `CHAT_INVALID_SESSION`, `CHAT_SESSION_LIMIT`,
`CHAT_TURN_IN_PROGRESS`, `CHAT_TURN_LIMIT`, `CHAT_INPUT_TOO_LARGE`,
`CHAT_CONTEXT_LIMIT`, `CHAT_OUTPUT_TOO_LARGE`,
`CHAT_SENSITIVE_INPUT_REJECTED`, `CHAT_PROVIDER_UNAVAILABLE`,
`CHAT_PROVIDER_TIMEOUT`, `CHAT_INVALID_OUTPUT`, and
`CHAT_UNSUPPORTED_MESSAGE`. Provider unavailable/timeout may be retryable;
the same request ID never causes a second generation. Other validation and
policy errors are non-retryable. Error text is bounded and contains no
prompt/response content or secrets.

Chat messages are excluded from durable logs and existing Command history.
Context is volatile and session-only. This protocol makes no claim about
provider-side retention or deletion. Shared fixtures:
`contract-fixtures/chat-session-schema-1.json`. Reject malformed fields,
duplicate keys, invalid UUID/time-zone/timestamp, over-limit Unicode input,
and oversized UTF-8 frames. Never encode Chat responses as `execution.plan` or
`DEVICE_COMMAND`.
