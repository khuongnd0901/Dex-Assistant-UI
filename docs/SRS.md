> Migrated to Android on 2026-10-13 ([D-037](../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Software Requirements Specification (SRS)

## 1. System Architecture
Hệ thống lai (Hybrid Architecture) kết hợp sức mạnh phần cứng Z Fold 3 và Cloud LLM:
- **Client (Android Native - Java/Kotlin):**
    - T-003 gồm app text/WebSocket và một DPC APK riêng chạy ở Device Owner;
      T-006 bổ sung voice/microphone service-first; T-010 bổ sung điều khiển màn hình DeX bằng số (D-017).
    - STT/TTS: Google Speech Recognition / Text-To-Speech native.
    - Device API: `WifiManager`, `ConnectivityManager`, `AccessibilityService`.
    - Bộ định tuyến local-first phân loại các lệnh xác định theo capability
      hiện có và thực thi tại thiết bị; chỉ chuyển lệnh mơ hồ/ngoài khả năng đến
      provider trực tiếp từ Android. Khóa provider được nhập lúc chạy và lưu qua Android Keystore theo quyết định của chủ dự án (D-036).
    - Grammar local-first dùng tiếng Việt cho mở app, Wi-Fi, Bluetooth, âm lượng,
      độ sáng, media và action API/DPC xác định. Cụm `nhạc lái xe` được ánh xạ
      thành YouTube search intent tìm tiêu đề liên quan, không dùng playlist cố định.
- **Backend lịch sử, đã ngừng phát triển (Spring Boot 4.0.x + Java 25):**
    - Maven modular monolith, Spring MVC và raw JSON WebSocket có version.
    - Spring AI 2.0.x đặt sau application port; tool calls được backend validate
      và chuyển thành typed execution plan, không tự động chạy Android action.
    - V1 giữ tối đa 6 lượt hội thoại trong memory với TTL 30 phút; Redis được
      hoãn đến khi có nhu cầu persistence đã đo được.
    - Mọi lời gọi Gemini/OpenAI đi qua provider proxy, nơi áp budget cấu hình
      riêng theo RPM và hạn mức request/token theo ngày tùy provider/model; số
      liệu provider thay đổi không được hard-code thành cam kết sản phẩm.
    - Đóng gói container ARM64/AMD64; OCI deployment thực hiện sau khi local
      vertical slice được xác minh.
- **Host mục tiêu:** OCI Always Free A1 trong home region. Tài liệu hiện hành mô
  tả mức tương đương 2 OCPU/12 GB và cảnh báo thiếu capacity/reclaim instance;
  đây không phải cam kết sẵn có 4 OCPU/24 GB.
- **LLM Provider:** stable Gemini Flash qua free tier, model ID cấu hình và pin
  theo decision log. Free-tier content có thể được provider dùng để cải thiện
  sản phẩm; không gửi secrets, password hoặc dữ liệu nhạy cảm rõ ràng.
  Đây là mô tả provider hiện hữu, không phải xác nhận đủ điều kiện cho T-014.
  Theo chỉ đạo owner, T-014 giữ provider Gemini qua metered API proxy hiện có,
  giữ scope cá nhân và mục tiêu chi phí $0/tháng của BRD; không thêm provider,
  paid fallback hay đổi luồng tích hợp. Project/model/quota/billing thực tế chưa
  được xác minh. T-014 không đưa ra kết luận tuân thủ policy nhà cung cấp; xem
  Chat dùng luồng voice command hiện tại với câu mở/kết thúc rõ ràng, một user,
  tối đa một phiên active và một conversation ID cho mỗi session mới. Idle 15 phút;
  disconnect, Stop hoặc Pause kết thúc phiên; reconnect cần phiên mới. Giới hạn
  gồm 20 cặp hội thoại/8.000 prompt input tokens, 2.000 Unicode code points input,
  output tối đa 1.024 tokens hoặc 4.000 code points (ngưỡng nào đến trước), tối đa
  200 lượt/phiên và một lượt đang chạy. Trong Chat, không dispatch hành động.
  Chi tiết: [T-014-02 decision record](../.agent/evidence/T-014-02-policy-review-2026-10-06.md).
  Wire message types, correlation, payload schema and stable Chat error codes:
  [WebSocket protocol V1](PROTOCOL.md#chat-session-extension-schema-1).

## 2. Functional Requirements
### 2.1. Lắng nghe và Xử lý giọng nói
- App chính phải chạy microphone foreground service với notification liên tục,
  tự phục hồi qua system-bound default assistant sau reboot/update (D-011);
  DPC Device Owner chỉ hòa giải quyền, không sở hữu microphone. Có pause/stop
  khẩn cấp; voice pipeline thực tế trên firmware Fold3 phải được xác minh.
- Mặc định tự nghe khi mở app/system-bound assistant sẵn sàng (D-021), không
  cần bấm Bắt đầu. Recovery lỗi/mất service/tạm gián đoạn dùng backoff và guard,
  không cắt phiên STT/lệnh/TTS. Pause/Stop chủ động giữ hiệu lực qua mở app/reboot
  cho đến Start/Resume. Quyền/user unlock phải hợp lệ; đường nền default assistant
  cần assistant role. Sau bốn lần lỗi, chờ năm phút rồi thử lại có giới hạn; không tự
  bật lại Accessibility khi người dùng thu hồi quyền.
- Nút mic/assistant một lần có thể mở phiên nhận lệnh qua VOICE_COMMAND,
  ASSIST, WEB_SEARCH giọng nói đã trace, default-assistant session hoặc phím
  ASSIST/VOICE_ASSIST ngoài (D-026).
  Chỉ nhận khi đã mở khóa, sẵn sàng và đủ quyền/assistant role; không vượt
  Pause/Stop. Khi STT đang nghe, lần bấm mic thứ hai gửi ngay partial mới nhất
  không rỗng qua cùng đường xử lý final cho lệnh, chọn/xác nhận và Chat; các
  partial thông thường chỉ hiển thị. Lượt gửi tiêu thụ đúng một lần, callback
  STT đến muộn không được chạy trùng; partial rỗng không thực thi. Không nhận
  nút mới khi đang định tuyến/thực thi/nói. Nhả recorder wake trước STT; không chiếm
  phím media/headset/mute chung. Người dùng chọn DeX trong chooser nếu xuất hiện.
  Bluetooth vô lăng chỉ hỗ trợ khi thiết bị gửi tín hiệu tương thích; chưa cam
  kết microphone xe/SCO hoặc mọi headunit. Lệnh phần trăm 0..100 cho âm lượng
  dùng giá trị tuyệt đối, kể cả “tăng âm lượng 100%”; giữ kiểm tra câu phủ định,
  nhiều vế và phạm vi giá trị. Không suy diễn phần trăm tương đối.
- Câu kích hoạt “Hey Hey” nhận dạng offline bằng sherpa-onnx 1.13.8 và
  English GigaSpeech KWS 3.3M int8 2024-01-01 (D-019). Runtime/model phải
  pin checksum, license và được kiểm chứng trên thiết bị.
- Sau wake, nhường micro cho Android SpeechRecognizer vi-VN; cho phép dịch vụ
  nhận dạng online mà không hỏi lại từng câu (`EXTRA_PREFER_OFFLINE=false`);
  lệnh có thể thực thi local nhưng nhận dạng lời nói vẫn có thể cần Internet.
  STT không chờ phân tích ảnh/nhãn màn hình; model wake tái sử dụng trong vòng
  đời service với stream mới mỗi phiên (D-024). Phản hồi mẫu bằng TTS tối đa
  4 giây rồi hủy và trở lại nghe; Stop giải phóng model/mic. Callback muộn
  không được mở lại overlay hoặc tạo hai vòng nghe.
  Sau callback ready thực, phát một cue sonification media 360ms và hiện trạng
  thái sẵn sàng RAM; cue không lấy audio focus. Người dùng nói sau cue. Dịch vụ STT có thể nhận audio chứa mật khẩu/thông tin nhạy cảm theo
  lựa chọn của chủ dự án; mất mạng có thể chặn cả lệnh nói thực thi local.
- Không lưu audio hoặc gửi audio wake-word. Chỉ final text của lệnh không nhạy
  cảm cần backend gửi qua WebSocket V1; lệnh local không gửi backend. Credential
  không gửi backend/LLM; lịch sử/log che toàn bộ phần nhạy cảm. Wi-Fi provisioning
  giữ trạng thái chưa hỗ trợ cho đến khi có executor riêng.
- Chỉ một lệnh đang xử lý; câu nhiều vế được định tuyến toàn bộ trước khi thực
  thi. Không tự replay sau mất kết nối/timeout. TTS phản hồi ngắn tiếng Việt,
  ưu tiên voice offline đã cài và cho phép network voice; không đọc mật khẩu.
- Chuyển wake → STT không được giữ audio focus cạnh tranh với chính dịch vụ
  nhận dạng; gián đoạn thực sự phải có lý do và nút tiếp tục hoạt động.
- Màn hình chính một trang không cuộn, ưu tiên cửa sổ DeX và thích ứng điện
  thoại/bàn phím: trạng thái, mức âm thanh, start/resume/pause/stop và partial/final
  transcript luôn truy cập được. Các nhóm ví dụ lệnh local được hỗ trợ và ô
  nhập lệnh hiện khi mở ứng dụng; vùng danh sách có thể cuộn riêng. Chạm ví dụ
  chỉ điền ô nhập, còn thực thi cần nút Chạy rõ ràng. “Xem tất cả” bao phủ mọi
  mẫu câu local đang hỗ trợ, nhóm theo việc cần làm, có tìm kiếm và gập các
  cách nói tương đương dưới câu chính. Trên cửa sổ thấp, chọn một câu từ danh
  sách đầy đủ đưa người dùng về ô nhập đã điền để kiểm tra trước khi Chạy.
  Lệnh theo ngữ cảnh/quyền phải ghi điều kiện sử dụng và
  không quảng bá action chưa có executor. Cấu hình, quota, lịch sử và chẩn đoán
  mở từ mục phụ; nút kết thúc Chat chỉ hiện khi Chat hoạt động.
- Partial transcript chỉ hiển thị, không thực thi, trừ thao tác bấm mic lần hai
  được người dùng chủ động để gửi partial như mô tả trên; diagnostic “Thử nhận
  giọng nói” không gửi backend hoặc thực thi. Transcript/metrics phiên chỉ giữ RAM,
  che credential, không log hoặc ghi SharedPreferences. Lúc chờ wake không
  tạo transcript liên tục; giữ wake audio hoàn toàn local.

- Theo D-031, STT lệnh có phụ đề tạm thời ở giữa phía dưới duy nhất màn hình
  Samsung DeX được chọn: chuẩn bị, đang nghe, đang nhận dạng và transcript đã
  che credential. Partial chỉ trình bày; final hoặc partial được chốt bằng nút
  giữ ba giây theo đồng hồ monotonic.
  Lượt nghe mới thay thế nội dung; hủy/Pause/Stop/lỗi/timeout STT xóa phụ đề.
  Hoàn thành lệnh/TTS/chờ wake không xóa final sớm. Không hiển thị diagnostic
  trên overlay, không lưu transcript phụ đề. Overlay độc lập với grid, không
  lấy focus hoặc chặn input, tạm tháo trước screenshot và chỉ khôi phục trạng
  thái còn hiệu lực. Mất/không xác định duy nhất DeX hoặc ngắt Accessibility
  phải dọn overlay và yêu cầu lượt nghe mới. Nghiệm thu streaming thực trên
  Fold3 cần partial trước final; callback giả lập không thay thế tiêu chí này.

### 2.2. Tool Calling (Định nghĩa trên Spring Boot)
Backend phải export các Tools (Functions) sau cho LLM:
- `toggleWifi(boolean state)`: T-003 bật/tắt Wi-Fi zero-touch qua Device Owner
  DPC và chỉ báo thành công sau khi quan sát trạng thái đích.
- `connectWifi(String ssid)`: Backend chỉ lập kế hoạch theo SSID; credential
  được chọn/nhập và giữ cục bộ trên Android, không gửi đến LLM/backend.
- `launchApp(String packageName, String deepLink)`: Khởi chạy app.
- `splitScreen(String package1, String package2)`: Yêu cầu best-effort bằng
  `FLAG_ACTIVITY_LAUNCH_ADJACENT`; không cam kết placement deterministic.
- `clickUI(String textOrId)`: Gửi lệnh xuống Client để Accessibility click.
- `inputText(String text)`: Dùng Accessibility nhập chuỗi.

### 2.3. Luồng thực thi (Execution Flow)
1. User nói -> Client chuyển thành text và đối chiếu capability/routing policy.
2. Nếu là lệnh local xác định, Client thực thi Native API / Accessibility, quan
   sát kết quả và không gửi lệnh đó đến backend.
3. Nếu ngoài khả năng hoặc mơ hồ, Client gửi qua WebSocket tới Server. Khi cần
   backend nhưng backend không sẵn sàng, Client trả typed failure và phản hồi
   “Khó quá, không làm được”.
4. Spring Boot áp privacy policy rồi gửi Text + history đã sanitize + tool
   descriptions lên Gemini API (hoặc FakePlanner ở local profile).
5. Provider proxy kiểm tra/reserve quota trước lời gọi, hạch toán kết quả và
   công bố snapshot usage/remaining/reset với trạng thái known/estimated/stale.
6. Gemini/OpenAI trả tool calls; backend chuyển chúng thành typed execution plan và
   validate tool/arguments theo allowlist.
7. Spring Boot gửi execution plan qua WebSocket xuống Client.
8. Client invoke Native API / Accessibility để thực thi -> Báo cáo kết quả.

### 2.4. Monitoring và lịch sử chẩn đoán
- App hiển thị backend reachability và quota theo từng provider/model, gồm mức
  đã dùng, còn lại và thời điểm reset khi có dữ liệu; unknown, estimated và
  stale phải được phân biệt, không suy diễn thành quota thật của provider.
- App có thể lưu cục bộ lịch sử bounded, user-clearable của request, routing,
  action/result, latency và mã lỗi để tối ưu hệ thống. Không lưu wake audio,
  token/API key, password hoặc raw provider prompt/response.
- App lưu nguyên văn text đã nhận dạng cùng metadata trong storage mã hóa và cho
  phép người dùng xóa. Record tự hết hạn theo điều kiện đến trước: thời gian giữ
  cấu hình được (mặc định 7 ngày) hoặc số lượng cấu hình được (mặc định 100 lệnh).
  Lịch sử được che dữ liệu nhạy cảm trước khi đưa vào hàng đợi nền tuần tự
  (D-025); mã hóa/ghi đĩa không chặn hoàn tất lệnh. Xóa theo thứ tự với các lần
  ghi đang chờ; UI báo đang xóa hoặc lỗi lưu trữ riêng với kết quả action.
  Process bị hủy đột ngột có thể mất bản ghi đang chờ. Dữ liệu hỏng/không giải
  mã được phải báo lỗi và giữ ciphertext, không âm thầm thay bằng lịch sử rỗng.

## 3. Non-Functional Requirements
- **Latency:** Mục tiêu từ lúc kết thúc câu lệnh đến lúc action bắt đầu là p95 <
  2 giây. Backend overhead không gồm LLM có mục tiêu p95 <= 100 ms; timeout LLM
  là 1.2 giây. Mục tiêu end-to-end chỉ được xác nhận bằng đo đạc trên thiết bị.
- **Security:** Production chỉ dùng `wss://`. HTTP upgrade phải có opaque bearer
  token 256-bit được provision ngoài source; server chỉ lưu SHA-256 digest qua
  secret configuration. Token có thể rotate/revoke và không được log/hardcode.
- **Protocol limits:** JSON message tối đa 16 KiB, một command đang chạy trên mỗi
  connection, tối đa 10 command/phút, UUID correlation và duplicate detection.
- **Quota enforcement:** Backend reserve atomically theo counter/refill window
  riêng của từng provider. Mỗi request chọn ngẫu nhiên trong tập provider đang
  khả dụng và còn budget; nếu call lỗi hoặc hết quota thì thử provider đủ điều
  kiện khác, tối đa một lần/provider. Khi tất cả không dùng được mới fail. Không
  có reset counter thủ công. Cơ chế persistence và hành vi qua restart phải được
  chốt trước khi tuyên bố hard daily limit.

### 2.5. Điều khiển màn hình DeX bằng số (T-010 / D-017)
- Sau Hey Hey, STT bắt đầu ngay sau khi mic wake được nhả; nhãn số chuẩn bị
  song song và không chặn tín hiệu sẵn sàng nghe (D-024). Accessibility
  Settings cho phép người dùng bật dịch vụ một lần; overlay không chặn chuột/focus.
- Chỉ dùng màn hình desktop Samsung được phát hiện động. Ngắt DeX không chuyển
  sang điện thoại. Node hiển thị/được bật/có thể bấm và phân tích Bitmap nhẹ
  (viền nút, dấu X nhiều kích thước) tạo mục tiêu; loại trùng/bị che/overlay.
  Ảnh chỉ ở RAM trong phiên, không OCR/model/upload.
- Nhãn theo hàng từ trái sang phải, trên xuống dưới; ánh xạ giữ nguyên trong
  phiên. Không có mục tiêu hoặc “hiện lưới” dùng 6×4, số 1–24. “Bấm ô 11”
  bấm tâm; “chia ô 11” chia lại 6×4 tối đa ba lần và nghe tiếp không wake lại.
- “Bấm vào số một”, chữ số và số tiếng Việt được xử lý local. Lệnh sai/ghép
  thuộc nhóm điều khiển số không gửi backend. Bấm/hủy kết thúc phiên; pause,
  stop, lỗi STT/timeout dọn overlay. Với nhãn mục tiêu, cửa sổ/mục tiêu đổi làm
  phiên mất hiệu lực; kiểm tra lại trước bấm. Với lưới (D-023), bấm tâm ô là
  chạm tọa độ desktop đang hiển thị: nội dung/cửa sổ/app đổi không hủy lưới.
  Ngắt/đổi ID hoặc kích thước DeX, mất quyền và hết hạn vẫn hủy; không chuyển
  sang màn hình điện thoại hoặc tự bấm lại.
- Kết quả bấm là DISPATCHED, không suy diễn popup đã đóng. Không tự bấm lại.
  Screenshot lỗi vẫn dùng node/lưới. DeX thật và 100 phiên phải được đo trước DONE.


### 2.6. Catalog cấu hình và interaction giọng nói (T-011 / D-027)
- Catalog JSON immutable compile lúc startup/reload; validate schema/alias/pattern/
  typed params. Match toàn câu cuối, giữ nguyên slot tiếng Việt; partial không có
  side effect. Alias/pattern của action có sẵn sửa qua config; policy Kotlin.
- Catalog gồm app, YouTube tìm/phát/chọn/player, Maps search/navigation driving/
  walking, gọi ra, Google search và social scroll. Incoming call/account/itinerary,
  OCR/remote config/general workflow chưa thuộc phạm vi.
- Theo T-015 / D-034, mở YouTube hoặc tìm kiếm YouTube đơn giản phải chờ cây
  Accessibility trong cửa sổ DeX đã chọn và chuẩn bị số thứ tự trên các thẻ video
  đọc được. Kết quả tìm kiếm phải có query đã yêu cầu. Số trỏ tới mapping thẻ
  cố định qua lượt nói tiếp theo; thay đổi danh sách/cửa sổ/display hủy mapping,
  không tự đổi thẻ hoặc bấm fallback. Chọn video vẫn cần xác nhận và revalidation.
  Không đọc được thẻ hoặc chưa xác minh query phải báo đúng trạng thái; nghiệm
  thu trên Fold3 theo T-015, không suy diễn từ build hay intent DISPATCHED.
- Trong DeX, người dùng có thể yêu cầu hai app ID khác nhau đã khai báo/cài đặt
  mở cạnh nhau. APK gửi cả hai app tới đúng desktop qua launch bounds và chỉ báo
  VERIFIED sau khi Accessibility quan sát hai cửa sổ app riêng, không chồng lấp.
  Lệnh explicit-intent như “mở nhạc {query}” và Maps “đưa tôi về {destination}”
  chọn capability theo pattern, không chỉ dựa cửa sổ có focus. App pair DeX thật
  cần kiểm chứng trên Fold3; AOSP AVD không thay bằng chứng Samsung.
- Call/semantic click phải chọn mục nếu cần và xác nhận bằng lượt nói tiếp theo;
  đọc tối đa năm lựa chọn/trang. Revalidate recipient/target, consume trước dispatch;
  timeout/cancel/Pause/Stop/STT/TTS error/new command không được execute lại.
  Reply 10 giây sau TTS thành công, step 30 giây, tổng command/plan 120 giây.
- Lệnh chưa resolve hoặc composite tự dùng backend AI; V1 optional capabilities và
  DEVICE_COMMAND typed allowlist được kiểm tra cả hai phía, dùng chung ActionRouter.
  Backend không khẳng định đã xác nhận; contact/screen/alias giữ local.
- Settings chỉ launch trên desktop chọn; Back/Home/Notifications chưa có executor
  bảo đảm display nên unavailable. Dispatch không chứng minh playback/cuộc gọi.
- Mục tiêu warm match p95 ≤5 ms/1.000 patterns trên Fold3 phải đo riêng. AVD desktop
  approximation dùng cho regression; không thay acceptance DeX/voice/device thật.
