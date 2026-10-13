> Migrated to Android on 2026-10-13 ([D-037](../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Device command action schema 1

Schema 2 is an additive extension documented in [DEVICE_COMMANDS_V2](DEVICE_COMMANDS_V2.md); this schema-1 contract remains valid for existing clients.

Additive protocol V1 extension for T-011. The optional command.request payload
`clientCapabilities` is `{ "actionSchemaVersion": 1, "supportedActions": ["APP_LAUNCH"], "configuredAppIds": ["youtube"] }`. No descriptor means the legacy tool set. With a descriptor, only advertised schema-1 actions may be planned as DEVICE_COMMAND; app IDs must be configured on the client.

`DEVICE_COMMAND` is REMOTE and has exactly arguments `actionSchemaVersion` (integer 1), `action` (enum below), `params` (object). Unknown params, wrong JSON primitive types, null, out-of-range values, execution flags/scripts/confirmation assertions are rejected on both server and Android. Android resolves aliases, contacts, places and UI locally; no contacts/screen upload. Calls and semantic clicks always need a later spoken confirmation.

| Action | Parameters (all required except marked optional) |
| --- | --- |
| APP_LAUNCH | app: configured app ID |
| APP_PAIR_LAUNCH | firstApp, secondApp: distinct configured app IDs; selected DeX display required, terminal success only after two non-overlapping app windows are observed |
| WIFI_SET, BLUETOOTH_SET | enabled: boolean |
| VOLUME_SET | operation: SET/INCREASE/DECREASE/MUTE/UNMUTE; SET percent integer 0..100; INCREASE/DECREASE steps integer 1..10 |
| BRIGHTNESS_SET | operation: SET/INCREASE/DECREASE/AUTO_ON/AUTO_OFF; SET percent integer 0..100 |
| MEDIA_TRANSPORT | operation: PLAY/PAUSE/STOP/NEXT/PREVIOUS |
| YOUTUBE_SEARCH, YOUTUBE_PLAY_QUERY, SEARCH_PLACE, WEB_SEARCH | query: text 1..500 |
| YOUTUBE_SELECT | optional index integer 1..50 OR title text 1..500 (exactly one) |
| YOUTUBE_FULLSCREEN | enabled: boolean |
| MEDIA_SEEK | operation: FORWARD/BACKWARD; seconds integer 1..3600 |
| NAVIGATE | destination: text 1..500; travelMode: DRIVING/WALKING |
| CALL_CONTACT | contact: text 1..200 |
| CALL_NUMBER | number: optional leading + and 3..15 digits; no guessing |
| CLICK_TEXT | text: text 1..200 |
| SCROLL | direction: FORWARD/BACKWARD |
| FEED_NEXT, FEED_PREVIOUS | no parameters (TikTok foreground required) |
| SYSTEM_ACTION | operation: BACK/HOME/SETTINGS/NOTIFICATIONS; DeX-only globals unavailable without verified display scope |

A plan contains at most eight actions. Each execution step has 30s,
whole command/plan 120s. Interaction wait is independent, starts after successful
TTS and lasts 10s; pause/stop/error/cancel/timeout consumes and cancels. Android
sends action.result only after terminal action result, including confirmation.
Success means terminal VERIFIED or DISPATCHED; dispatch is not playback/call proof.

On Android 14+, `ActivityManager.killBackgroundProcesses` can target only the
calling app's own processes, and `ActivityManager.getAppTasks` returns only the
calling app's tasks. Closing a Samsung DeX window must therefore target its
actual desktop close control; it must not be presented as a force-stop operation.
See [Android process API](https://developer.android.com/reference/android/app/ActivityManager#killBackgroundProcesses(java.lang.String)), [Android task API](https://developer.android.com/reference/android/app/ActivityManager#getAppTasks()), and [Samsung DeX lifecycle](https://developer.samsung.com/sdp/blog/en/2017/12/07/samsung-dex-android-life-cycle-in-samsung-dex).
