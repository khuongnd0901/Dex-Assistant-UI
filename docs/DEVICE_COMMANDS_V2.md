> Migrated to Android on 2026-10-13 ([D-037](../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Device command action schema 2

Schema 2 adds `CLICK_TARGET` and the executor-gated `APP_CLOSE_WINDOW` to
[schema 1](DEVICE_COMMANDS_V1.md). The WebSocket
envelope, authentication, limits, result states and all schema-1 actions remain
unchanged. A schema-1 descriptor cannot advertise either new action; a client with
no descriptor continues to use legacy tools. A schema-2 descriptor has exactly
`actionSchemaVersion: 2`, `supportedActions` and `configuredAppIds`. The backend
offers only actions in that descriptor and emits `actionSchemaVersion: 2` for
them. Android must advertise schema 2 and `CLICK_TARGET` only after its semantic
executor is available. M1 prepares validation; it does not enable execution.

`CLICK_TARGET` has exactly one `params` key, `target`. `target` is an object
with these fields:

| Field | Rule |
| --- | --- |
| `appId` | Required configured app ID, `[a-z][a-z0-9_]{0,63}` |
| `role` | Optional: `BUTTON`, `TEXT`, `IMAGE`, or `LIST_ITEM` |
| `text`, `contentDescription`, `resourceId`, `contextText` | Optional nonblank text, each at most 200 UTF-16 code units and without control characters |
| `ordinal` | Optional integer 1–50, applied only after role/context filtering; cannot identify a target alone |

At least one of `text`, `contentDescription`, `resourceId`, or `contextText` is
required. Unknown fields, null, type coercion, coordinates, scripts, execution
policy and confirmation claims are rejected by both sides. The target is a
selector request, never evidence that an element was found or clicked. Android
alone checks configured package, selected Samsung DeX display, window, bounds,
visibility, ambiguity and live state before gesture, and obtains the required
spoken confirmation. The backend receives no UI tree, screen image or audio.

The canonical valid/invalid examples are in
[`device-command-schema-2.json`](contract-fixtures/device-command-schema-2.json).

`APP_CLOSE_WINDOW` is an additive close-window request with exactly
`params: {"app": "configured_app_id"}`. `app` must match the configured ID rules
and be advertised in `configuredAppIds`. Unknown fields, null and coercion are
rejected. It requires action schema 2 and explicit `supportedActions` membership;
old/schema-1 clients are not offered this action. Android must not advertise it
until its selected-DeX executor has passed physical readiness checks.

This means closing the application's selected Samsung DeX window, not killing,
suspending or force-stopping its process. Android must locate a unique actual
window close control for the configured package, obtain spoken confirmation,
revalidate the window/target, consume once and report delivery/readback honestly.
Missing/ambiguous controls fail locally; there is no unscoped Back/Home, shell,
coordinate guess or Settings force-stop fallback. This contract addition is being
implemented; use T-011 evidence for actual executor/capability readiness.
