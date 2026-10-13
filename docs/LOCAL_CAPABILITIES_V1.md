> Migrated to Android on 2026-10-13 ([D-037](../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Vietnamese local capability contract — config schema 1

T-011/D-027 replaces the hard-coded `vi-local-v1` grammar with `vi-config-v1`.
The immutable registry loads `commands.json` once at application startup and
compiles patterns, aliases, typed slots and a prefix index. Invalid reload keeps
the last good registry. See [JSON schema](command-catalog.schema.json) and
[shared action contract](DEVICE_COMMANDS_V1.md). Action policy remains Kotlin.

## Recognition and routes

Normalize NFC, case and whitespace; match the whole utterance. Literal/app alias
matching may use accent folding after collision validation. Free text slots keep
original case, accents and meaningful punctuation. App aliases resolve the whole
slot to configured IDs; contact/place/app names are never guessed fuzzily.
Rank exact/folded quality, applicable foreground scope, literal and typed-slot
specificity. Ambiguous different targets/actions do not execute. Unrecognized
or composite requests go to backend AI automatically, once per guarded final.
Partial STT has no lookup, backend request or execution side effects.

- `LOCAL`: unique typed match. Missing capability/permission fails locally.
- `BACKEND_REQUIRED`: unmatched, ambiguous or reasoning/composite request.
- `UNSUPPORTED`: recognized prohibited/unavailable operation. Privacy/provisioning
  guards, malformed numbered commands, ambiguous spoken phone numbers, app-close
  and confirmation/selection outside pending context remain local failures.

Credential/Wi-Fi provisioning guards run before backend fallback. Config cannot
add scripts, arbitrary flags, workflow steps or override permission/confirmation.
On Android 14+ third-party apps can use `killBackgroundProcesses` only for
their own processes, and `getAppTasks` returns only the calling app's tasks.
Therefore a DeX window-close action must identify the selected desktop's actual
X control, require spoken confirmation and revalidate it; it cannot claim
force-stop semantics. See [Android `killBackgroundProcesses`](https://developer.android.com/reference/android/app/ActivityManager#killBackgroundProcesses(java.lang.String)), [Android `getAppTasks`](https://developer.android.com/reference/android/app/ActivityManager#getAppTasks()), and [Samsung DeX activity lifecycle](https://developer.samsung.com/sdp/blog/en/2017/12/07/samsung-dex-android-life-cycle-in-samsung-dex).

## Catalog and actual availability

| Actions | Execution and policy |
| --- | --- |
| APP_LAUNCH | Package/alias config; installed launch intent; DISPATCHED. T-015 YouTube extension uses selected DeX and observes accessible cards before index-ready feedback. |
| APP_PAIR_LAUNCH | Two distinct configured apps; selected Samsung desktop only; request adjacent launch bounds, verify both app windows are visible and disjoint before VERIFIED |
| WIFI_SET | Existing signature DPC/Device Owner; state readback VERIFIED |
| BLUETOOTH_SET | Unavailable API35 path; not advertised to AI |
| VOLUME_SET / BRIGHTNESS_SET | Existing bounded steps/0..100/readback; brightness special access required |
| MEDIA_TRANSPORT | Media keys; DISPATCHED does not prove playback |
| YOUTUBE_SEARCH | Encoded results URL from original query on selected DeX; observe exact query/cards before preparing frozen selection indexes; no autoplay |
| YOUTUBE_PLAY_QUERY / YOUTUBE_SELECT | Fixed search/observe/select flow, recognized video cards or numbered fallback; spoken confirmation then target revalidation |
| YOUTUBE_FULLSCREEN | Exact semantic label, spoken confirmation, scoped target revalidation |
| MEDIA_SEEK | Verified YouTube/Music foreground plus unique seekable session and notification access; DISPATCHED |
| SEARCH_PLACE / NAVIGATE | Encoded Maps intents; driving/walking; local home/work aliases; HTTPS directions fallback reported distinctly |
| CALL_CONTACT / CALL_NUMBER | Local exact lookup/digit parsing; spoken selection and later confirmation; frozen recipient and permission policy; ACTION_CALL or disclosed ACTION_DIAL fallback |
| WEB_SEARCH | Encoded Google HTTPS query. In Chrome foreground, “tìm địa điểm {query}” is scoped to Chrome and opens the URL explicitly in Chrome; the same phrase in Maps remains SEARCH_PLACE and opens Maps. Foreground is revalidated before the Chrome-targeted intent. |
| CLICK_TEXT | Selected Samsung desktop only; choose/confirm/revalidate before gesture |
| SCROLL / FEED_NEXT / FEED_PREVIOUS | Selected desktop; semantic scroll or one revalidated feed swipe; TikTok context required for feed actions |
| SYSTEM_ACTION | Settings intent explicitly launched on selected desktop; Back/Home/Notifications unavailable until scoped global implementation is proven |

Initial configured apps: YouTube, YouTube Music, Maps, Chrome, Spotify, Facebook
and TikTok. Additional configured launch apps need no Kotlin parser changes.
“Chia màn hình Google Maps và YouTube” launches two configured app tasks on the
selected desktop; an AOSP display or app that ignores the requested bounds fails
closed as layout-unverified. “Mở nhạc {query}” selects YouTube play-query by
explicit phrase intent independent of focused pane; “đưa tôi về {destination}”
uses the local Maps route/address alias regardless of which pane has focus.
Android supports adjacent task launch with `FLAG_ACTIVITY_LAUNCH_ADJACENT` and
launch bounds; Samsung DeX uses Android multi-window/freeform for resizable app
windows. Device/OEM acceptance is still required: [Android multi-window](https://developer.android.com/develop/ui/views/layout/support-multi-window-mode), [Samsung DeX multi-window FAQ](https://developer.samsung.com/samsung-dex/faq.html), [Samsung DeX optimization](https://developer.samsung.com/samsung-dex/modify-optimizing.html).
Incoming calls, account actions, itinerary editing, remote config, OCR and a
general workflow engine are excluded. Legacy CLICK_UI/INPUT_TEXT/SPLIT_SCREEN
remain unsupported; DEVICE_COMMAND CLICK_TEXT is a separate guarded capability.

## Interaction and backend bridge

One pending interaction retains parent command/action IDs. Config-defined whole
confirmation/selection/cancel/page phrases only have meaning in that context.
Every outgoing call and semantic click requires a subsequent spoken confirmation.
Read at most five choices per page; revalidate recipient/target immediately before
execution. Consume before dispatch. A new command abandons the parent; duplicate
final/TTS callbacks and late callbacks cannot revive it. Pause/Stop/STT/TTS error,
cancel or timeout removes pending state. Reply deadline: 10s after successful
TTS; step: 30s; total command/plan: 120s.

Local and validated remote actions use the same Android ActionRouter. Optional
V1 client capabilities restrict provider tools to advertised schema/actions/app
IDs. Old clients retain legacy tools. Both sides reject unknown fields, unsafe
flags, null/coerced/wrong primitive types and ranges. Backend cannot assert local
confirmation or bypass permission. Contacts, place aliases and screen contents
remain local. Send action.result only at terminal completion; cancellation also
releases the remote parent. VERIFIED requires readback; DISPATCHED means delivery
only. Roll out backend before the new APK.

The additive [device action schema 2](DEVICE_COMMANDS_V2.md) does not change
this local command registry schema. `CLICK_TARGET` stays outside the advertised
Android capability set until the semantic executor and its safety checks exist.

## Local diagnostic history

Android encrypts the original recognized text and diagnostic payload with an
independent Android Keystore AES-256-GCM key. Persist only an opaque id,
timestamp needed for pruning, schema/routing version, locale, route, intent,
sanitized outcome/error, duration, and optional backend correlation UUID.
Credential-bearing utterances and Wi-Fi provisioning phrases are rejected locally
before ambiguity/backend routing. Their entire text is redacted in history;
this is a conservative grammar guard, not a guarantee to detect arbitrary secrets.

Never persist audio, bearer/API credentials, passwords, raw provider prompts or
responses. Sanitization happens before the storage boundary.

Insertion and pruning are one transaction: remove records older than the age
limit, then retain only the newest count limit. Defaults are seven days and 100
commands; a record is removed as soon as either limit is exceeded. Prune on
insert, startup, and read. User clear deletes all records and may rotate only
the history key; it must not modify the WebSocket credential key.

D-025 queues redacted immutable records on a process-wide FIFO worker so history
encryption/IO does not delay command terminal or voice feedback. User clear is
ordered with queued writes; UI reports pending deletion and storage failures
separately from action outcome. History publication cannot replace newer command
status. Read still persists pruning/legacy redaction. Malformed or undecryptable
storage surfaces an error while preserving ciphertext; clear remains available.
Queued diagnostics may be lost on abrupt process death; completed actions do not
imply history durability.

## Numbered desktop extension (D-017 / T-010)

A screen sub-router runs before the config-driven general engine and reserves
whole-input `CLICK_TARGET`, `CLICK_CELL`, `SUBDIVIDE`, `GRID`, `CANCEL`
(`INVALID` represents malformed screen input):
`bấm vào số một`, `bấm số 1`, `bấm ô mười một`, `chia ô 11`, `hiện lưới`,
`hủy`. Digits and Vietnamese integer words are accepted. Grid bounds are 1..24;
component bounds come from the immutable active session. Split depth is at most
three. These commands require a valid local desktop session and explicit
Accessibility user grant; missing capabilities fail locally. Malformed,
negated, compound or out-of-range numbered-control requests are reserved local
failures, overriding general ambiguity-to-backend routing. No partial execution.

A wake starts STT after releasing the wake microphone, while numbering prepares
in parallel (D-024); late preparation cannot revive a cancelled voice turn.
Split/show keeps the session
and next STT turn; click/cancel ends it. For numbered targets, window/target
changes invalidate the mapping. For grid sessions (D-023), cells refer to fixed
desktop coordinates and content/focus/app/window changes do not invalidate them:
a tap acts on whatever currently occupies that cell center. Desktop ID/size
changes, permission loss, expiry, pause/stop/STT error/timeout invalidate and remove the overlay.
The session owns its ID, display ID and immutable number mapping. Revalidation
before a tap and callback epochs prevent stale/duplicate dispatch. Never switch
to the phone or replay after timeout. Click is DISPATCHED; no verified popup
closure is claimed. General backend CLICK_UI and INPUT_TEXT remain unsupported.
Images/nodes/coordinates stay local and are not added to protocol/history.

Screen grammar additionally folds Vietnamese accents (including đ) and strips
surrounding punctuation for STT variants; general literals/aliases now also fold accents after collision validation;
free-text slots preserve diacritics. The screen verb is recorded in sanitized local diagnostic history.

`bấm ô số N` and `bấm vào ô số N` are equivalent to `bấm ô N`;
`chia ô số N` is equivalent to `chia ô N`, including spoken integer words and
compact `số4`. Subdivision remains limited to 1..24.

T-015 follow-up adds the whole-input YouTube result phrase `mở video số N`.
Only a valid configured `YOUTUBE_SELECT` index under YouTube foreground scope
may pass the generic screen numeric reservation. Existing `chọn video số N`
and `phát kết quả số N` use that same path. Invalid/out-of-context/compound
numbered phrases remain local failures, with spoken confirmation and live
revalidation before semantic dispatch.

The owner-requested launch/search extension is being validated: opening YouTube
or a simple YouTube search waits for selected-DeX accessible video cards and
prepares 1-based indexes. Search readiness requires the expected query to be
observed on the selected app tree. A RAM-only frozen candidate map has the
existing 35-second screen lifetime; voice preparation/feedback do not extend
it. Selection consumes that map once; list/card/window/display changes reject
instead of silently changing what an index means. No-card/ambiguous trees do
not trigger an automatic grid or blind click. See T-015 evidence for the actual
host/device gate; these source changes are not a claim of Fold3 acceptance.

`mở tiếng` and `mở âm thanh` are aliases for existing media-stream UNMUTE,
alongside `bật tiếng` and `bật âm thanh`; they do not close or launch an app.

Exact single-intent `tắt YouTube`, `đóng YouTube` and the same close phrases for
known app aliases still fail immediately as UNSUPPORTED locally until a selected
DeX close-window adapter is implemented and verified. They do not wait for the
backend to attempt an unavailable app-close executor. Negated/compound
general commands keep the existing whole-input routing policy.
