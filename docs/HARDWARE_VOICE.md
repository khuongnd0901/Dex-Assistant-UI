# Gọi trợ lý bằng nút thoại

Nút thoại mở bước nghe câu lệnh trực tiếp, tương đương gọi “Hey hey”.
Đợi tiếng báo sẵn sàng rồi nói câu lệnh. Nút không tự phát nhạc hoặc thực hiện
lệnh từ bản nhận dạng chưa hoàn chỉnh.

## Điều kiện sử dụng

- Chọn **DeX Assistant** làm trợ lý mặc định của Android.
- Điện thoại đã mở khóa, quyền microphone được cấp và trạng thái trợ lý là READY.
- Tự nghe/wake word đang bật; lựa chọn Tạm dừng hoặc Dừng được giữ nguyên.
  Bật nghe lại trong ứng dụng trước khi dùng nút nếu đã tạm dừng.
- Không đang gọi điện hoặc sử dụng chế độ âm thanh cuộc gọi.
- Nếu đang nhận một câu lệnh hoặc xử lý lệnh, lần bấm tiếp theo không mở thêm
  phiên nghe. Đợi phiên hiện tại kết thúc.

## Chuột hoặc thiết bị Bluetooth

Chuột đang thử nghiệm thực tế mở hộp chọn ứng dụng bằng speech `WEB_SEARCH`
(`android.speech.action.WEB_SEARCH`), không gọi thẳng trợ lý mặc định. Ứng dụng
đăng ký đúng route này; một số thiết bị khác gửi `VOICE_COMMAND` hoặc `ASSIST`.
Nếu lần bấm hiển thị hộp chọn ứng dụng, chọn **DeX Assistant**. Chọn
**Luôn luôn** nếu muốn Android dùng ứng dụng này cho các lần bấm tiếp theo.
Ứng dụng không tự thay đổi lựa chọn của hệ thống.

Nút thoại trên vô lăng dùng cùng đường đi nếu đầu xe/thiết bị Bluetooth gửi
speech `WEB_SEARCH`, `VOICE_COMMAND`, `ASSIST`, gọi phiên trợ lý Android hoặc
gửi phím ASSIST/VOICE_ASSIST.
Mỗi đầu xe cần kiểm chứng thực tế; không bảo đảm mọi giao thức vô lăng đều gửi
một trong các tín hiệu này.

Đây là khả năng kích hoạt trợ lý, không phải cam kết dùng microphone của xe.
Capture vẫn do dịch vụ STT Android hiện tại chọn route; chưa có cấu hình SCO
hoặc thay đổi route Bluetooth trong tính năng này. Microphone/âm thanh qua từng
xe, đầu xe và thiết bị Bluetooth phải được kiểm chứng riêng.

Đường phím ngoài yêu cầu dịch vụ Trợ năng DeX đã được chủ thiết bị bật. Chỉ phím
ASSIST/VOICE_ASSIST được xử lý. Phím tai nghe chung, phát/dừng media, âm lượng,
tắt microphone và phím nhập chữ giữ hành vi của hệ thống. Không ghi nội dung
phím hoặc dữ liệu đi kèm intent; intent lạ không mở phiên nghe.

Nếu chưa sẵn sàng hoặc đang bận, entry từ hộp chọn ứng dụng hiện thông báo ngắn
và đóng ngay, không mở dashboard. STT vẫn dùng dịch vụ nhận dạng hiện tại; độ
trễ mạng/endpoint của dịch vụ đó chưa được thay đổi bởi chức năng nút thoại.

## Kiểm chứng dành cho phát triển

- `VoiceHardwareActivationTest`: admission, debounce, phím ngoài, cặp down/up,
  repeat, phím không hỗ trợ và intent không hỗ trợ.
- `VoiceHardwareDeviceTest#registeredVoiceCommandResolverIncludesDex`: kiểm tra
  DeX có trong resolver speech WEB_SEARCH/VOICE_COMMAND/ASSIST sau khi cài đúng APK.
- `runDirectButtonProbe=true`: chạy probe direct activation khi trợ lý đang
  LISTENING và giữ im lặng. Probe dùng cờ diagnostic hiện có trước khi callback
  STT chạy, kiểm tra recorder cũ đã release, readiness thực, từ chối trigger
  khi bận, hủy nhận dạng rồi phục hồi wake; không ghi history hoặc đổi policy.
- `runNumericVolumeProbe=true`: khi không phát media, kiểm tra lệnh 40% và 100%
  qua parser/executor thật, đọc lại mức âm lượng, luôn phục hồi mức và mute ban
  đầu. Không phát âm thanh trong test.

Chạy instrumentation với `am instrument --no-restart -r -w`; probe giữ các dịch
vụ Trợ năng đã bật qua `FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES`. Fold3 đã
quan sát `am instrument` restart tiến trình làm mất binding; cờ `--no-restart`
được xác minh trên thiết bị dù `am instrument --help` không liệt kê. Sau cài APK,
kiểm tra binding thực trước khi chạy. Test không tự bật/tắt Trợ năng. Khi cần
chủ máy bật lại qua Settings trong lúc chạy, dùng
`-e ownerAccessibilityBindTimeoutMs 180000`; mặc định 10000 ms, giới hạn
10000–180000 ms, vẫn fail nếu dịch vụ không bind.

Các fixture opt-in mới:

- `NumberedScreenDeviceTest#voiceCoordinatorGridSubdivisionSurvivesFreshPreparationAndCompactFinals`
  với `-e runVoiceGridFixture true`: chỉ tap Activity fixture của APK; kiểm tra
  dispatchVoice liên tục và chuẩn bị activation mới, cả ô2/ô8 và ô 2/ô 8.
- `ConfigCommandDeviceTest#physicalMapsAddressVoiceRouteDispatchesOnSelectedDesktop`
  với `-e runPhysicalMapsAddress true`: mở query công khai Phà Cát Lái qua
  coordinator, kiểm tra kết quả/query trên Maps của display DeX; metadata chỉ
  status/display/boolean. Có thay đổi màn hình Maps, không xuất nội dung khác.
- `ConfigCommandDeviceTest#physicalMapsYoutubeAppPairReportsVerifiedLayout`
  với `-e runPhysicalAppPair true`: mở hai app, chỉ PASS với `APP_PAIR_VERIFIED`;
  metadata chỉ outcome/code và package/bounds thuộc hai app trên desktop.

Fixture coordinator dùng history store/key riêng có tên ngẫu nhiên rồi xóa
chính artifact đó; không đọc, reset hoặc thay đổi history/key của chủ máy.
Không chạy rộng các legacy suites trên thiết bị chủ máy.
Compilation hoặc direct probe không thay thế thao tác bấm chuột/vô lăng thật.
Speech `WEB_SEARCH` là action `android.speech.action.WEB_SEARCH`; generic
`android.intent.action.WEB_SEARCH`, `ACTION_SEARCH`, ASR intents và hands-free
actions chưa quan sát không nằm trong allowlist. Nội dung query/extras bên ngoài
không được dùng làm câu lệnh.

Tham khảo: [Android RecognizerIntent.ACTION_WEB_SEARCH](https://developer.android.com/reference/android/speech/RecognizerIntent#ACTION_WEB_SEARCH).

### DeX STT subtitles (T-006)

Command listening now presents transient Vietnamese subtitles only on the unique
selected Samsung DeX display. Preparation, listening and recognition have distinct
status labels; revised nonempty partials replace the preceding hypothesis, and
final text remains for three seconds. Selection/confirmation and continuous grid
reply turns use the same presentation. Diagnostic listening remains dashboard-only.

Text is redacted in RAM before rendering, never stored or logged by this feature.
Pause, Stop, cancellation and STT failure/timeout clear the presentation. Grid
cleanup and normal return to wake standby preserve the final deadline. Display
loss/ambiguity, Accessibility interruption/unbind and renderer failure suppress the
current turn until a fresh listening turn. Overlays neither take focus nor accept
input. Preparation and image-target revalidation suspend subtitles through a
compositor detachment barrier; pending captures cannot restore stale text.

The recognizer may produce no partial callbacks. Host or injected lifecycle tests
do not prove natural partial-before-final streaming; record that physical gate
separately from overlay rendering. This change does not reduce STT endpoint delay.

Fold3 API35 DeX acceptance on 2026-10-05: owner confirmed subtitles across three
repeated natural commands, partial-before-final and roughly three-second final
retention. Metadata also showed continuous grid reply turns presenting subtitles.
Focused physical tests verify screenshot exclusion, production image revalidation,
input/focus preservation, independent grid cleanup, stale/cancel/expiry guards and
background no-overlay barrier completion. Initial disappearing subtitles were
repaired; failed evidence is retained in the canonical T-006 subtitle report.
Final APK adds only teardown closure release after the owner-tested APK; four
focused subtitle fixtures passed again on it. API30-33/34 and physical display
resize/disconnect are still unverified. This does not close the remaining T-006
wake accuracy, latency, microphone-route, resource and soak gates.
