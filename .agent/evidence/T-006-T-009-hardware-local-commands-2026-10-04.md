> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Hardware voice activation and local percentage correction — 2026-10-04

## Observed failure and boundaries

Owner mouse mic button is one press; Android displays Bixby/Google chooser.
DeX already holds assistant role but original VOICE_COMMAND resolver omitted
DeX. Metadata-only dedicated-key traces found no supported key events; this
establishes a voice intent entry, not the underlying HID/HFP transport.
Subsequent live chooser trace identifies WEB_SEARCH as this mouse actual action. Direct
Bluetooth steering-wheel requested, unavailable for physical verification.
Local normalizer stripped trailing percent signs before routing, explaining
why “tăng âm lượng 100%” failed local matching.

## Implementation

Android VoiceButtonActivity handles VOICE_COMMAND/ASSIST and observed speech
WEB_SEARCH after the follow-up; assistant session
and dedicated external ASSIST/VOICE_ASSIST keys share VoiceCommandActivation.
Unlocked/role/readiness/permission/auto-start/suppression/call-mode and idle
checks are preserved. Repeat/busy triggers rejected; balanced key events, other
keys passthrough. VoiceAssistantService queues recognition after wake worker
stops and releases recorder. No microphone routing, SCO, provider, protocol or
media-session ownership change. User chooses DeX in chooser themselves.

LocalRouting preserves percent notation, matches strict 0..100 absolute target
variants, keeps step commands and adds synonyms for existing app/media/search/
brightness/mute actions. Malformed, negative, out-of-range, relative-percent,
negated/private/compound routing remains guarded. No force-stop/autoplay or new
Bluetooth capability. See Android docs/HARDWARE_VOICE.md and LOCAL_COMMANDS.md.

## Frozen automated validation

JDK17 `test lint assembleDebug assembleDebugAndroidTest` PASS201tasks/36s.
75debug +75release unit tests, zero failures/errors/skips. Diff check PASS.
Frozen log: /tmp/t006-hardware-local-commands-frozen-gradle.log (untracked).
Original certificate SHA256:
`9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
Signed app SHA256:
`9ad2844ddf0ba123e48631f999ba4cd30b445773c7629e4fe7fd6e677d12c4ce`.
Signed test SHA256:
`791a0630f2880cdb772e626c44fd9e4c6f42247787119e30ee3975c805e7e079`.

## Device validation

Matching-certificate app/test installed on Fold3; installed app SHA equals the
signed artifact above. VOICE_COMMAND query now includes Google, Bixby and DeX
VoiceButtonActivity. No system preference was changed automatically.

- registeredVoiceCommandResolverIncludesDex PASS0.018s (VOICE_COMMAND and ASSIST).
- numericPercentVolumeExecutesLocallyAndRestoresVolumeAndMute PASS0.071s:
  production parser/executor 40% and100%, actual STREAM_MUSIC readback, original
  volume/mute restored. Raw runner status0, no assumption skip; no active media.
- directButtonReleasesWakeRecorderBeforeActualSttAndCancelsCleanly PASS2.143s:
  actual provider readiness, previous AudioRecord released, second busy trigger
  rejected, recognizer cancellation and genuine unsilenced wake capture resumed.
  Existing diagnostic isolation prevented command execution; owner history and
  voice policy snapshots unchanged. This invokes the shared production entry,
  not a physical HID/button press or assistant-session UI test.

Protected configuration/credential prefs absent before and after install/probes;
presence metadata unchanged. No permission/role/preferred-app reset, route/SCO
change, owner history deletion, audio/transcript/screenshots or ordinary typing
export. Owner physical mouse-button trial after probes FAIL: still only Bixby/Google
visible. Package resolver includes DeX, so installed-registration proof is
insufficient for physical chooser acceptance. Actual live chooser trace confirms
`act=android.speech.action.WEB_SEARCH flg=0x10800000` in ResolverActivity;
WEB_SEARCH query has only Google/Bixby. Original VOICE_COMMAND route inference
was incomplete; follow-up adds the observed RecognizerIntent.ACTION_WEB_SEARCH
to the same guarded entry. Android implementation published
as `9ad233c0c41b1f3586bc5734a1d025537c6cb53b`, this failure remains open.

## Remaining acceptance

Follow-up physical mouse trial PASS: owner sees DeX, hears listening and opens
YouTube. Retain the first physical FAIL as evidence of incomplete initial route.
Direct probe is not physical-button validation. Actual Bluetooth wheel and car
microphone/SCO unverified. SpeechRecognizer final endpoint/network latency is
unchanged; partial text remains display-only. Broad T-006/T-009 remain IN_PROGRESS;
A–J voice/UI architecture migration remains deferred, not implemented.

## Observed-action follow-up

Physical chooser inspection showed actual RecognizerIntent.ACTION_WEB_SEARCH,
not VOICE_COMMAND. Precisely android.speech.action.WEB_SEARCH added to manifest
and supported-action gate; same session/permission/suppression/release policy.
Generic ACTION_SEARCH and android.intent.action.WEB_SEARCH remain rejected;
external query/extras ignored. JVM allowlist distinguishes these names. Resolver
test now includes the observed action.

Frozen JDK17 integrated test/lint/app+test PASS201tasks/35s; device install and
updated resolver passed and owner physical trial passed (see below). Signed follow-up app SHA256
`64b375c3f1887f5ce4738d1f8f788af98d3c7860c821f6eadbdc2fbae7b2c253`;
test SHA256 `33a93eb3d5093568417c26351b6b2b88e13875f619611305d69c67cc51300e7a`.
Original certificate unchanged.

Reference: https://developer.android.com/reference/android/speech/RecognizerIntent#ACTION_WEB_SEARCH

Follow-up installed app SHA verified equal signed artifact. Updated resolver
VOICE_COMMAND/ASSIST/speech WEB_SEARCH PASS0.010s, no skip. Exact WEB_SEARCH
query now3 candidates including DeX; final75debug+75release units0fail/skip.
Protected configuration/credential presence unchanged, assistant READY readback.
Owner explicitly confirms “Thấy DeX, nghe được và mở YouTube”.
One button activation sample: STT requested8ms after button request, actual
provider ready538ms after request; this does not establish speech-end latency.
Earlier epoch and unrelated log entries excluded from that sample.

Owner subsequently requested automatic choice on every button press. System
chooser opened for exact speech WEB_SEARCH, DeX selected. Readback:
`cmd package resolve-activity --brief -a android.speech.action.WEB_SEARCH`
returns DeX VoiceButtonActivity; `dumpsys package preferred` exact action has
DeX `mAlways=true`, selected from current3candidates with DEFAULT category.
Thus preferred handler is persisted, not only assistant role. No other
preferred handlers, permissions or mic routes reset. Temporary chooser XML
removed; only whitelisted chooser labels/bounds and preference metadata read.
Owner subsequently confirmed test successful after preferred-selection.
Physical repeated mic invocation acceptance is owner-confirmed PASS; actual
wheel/car microphone remains unverified.
