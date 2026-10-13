> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

> 2026-10-13 verified continuation: T-014-06 gained automatic idle context/service teardown and real HTTP cancellation. Host191tests/variant + lint/APKs PASS; Windows AVD9offline fault/idle/cancel/isolation tests PASS. Spoken exit during TTS remains unsupported; physical execution stays T-020. [Evidence](../evidence/Android-simulator-continuation-2026-10-13.md).

> 2026-10-13 scheduling override: T-014-09 physical execution belongs to [T-020](../plans/active/T-020-consolidated-fold3-acceptance.md); T-014-06 implementation and host/Windows AVD validation continue. Older stop-after-subtask restrictions are superseded by owner request to continue open Android tasks.

> 2026-10-10 scope override: T-014-08 server-backed provider/resource outcome CANCELLED; remaining direct-provider Android measurements transfer to T-018. T-014-06 and T-014-09 remain IN_PROGRESS for Android voice/TTS/physical acceptance under the direct-provider architecture, without server/protocol-client prerequisites. Completed subtasks retain historical DONE evidence. Older backend dependencies and next actions below are superseded.

### T-014-02 — Chốt lifecycle, giới hạn và provider path

- Trạng thái: `DONE`; cập nhật: 2026-10-06; phụ trách: Main.
- Phụ thuộc: T-014-01; milestone ExecPlan: 1.
- Công việc: Chốt idle, pair/token/byte/input/output limits, disconnect/reconnect, dedupe, reset/Pause/Stop và retryable errors bằng đề xuất có cách đo/enforce. Owner xác nhận một user, tối đa một Chat session, một conversation ID/session, dùng luồng voice command với câu mở/kết thúc và không dispatch hành động khi ở Chat. Giữ Gemini qua API metered proxy; bỏ qua provider-policy review.
- Output: Ma trận accepted/proposed/open cùng giá trị và approval; cập nhật D-030, SRS và cấu hình khi ảnh hưởng. Ghi rõ provider/project thực tế chưa xác minh và không đưa ra kết luận tuân thủ policy.
- Nghiệm thu: Các giới hạn lifecycle/content có giá trị được chốt và cách đo/enforce. Provider-policy review nằm ngoài phạm vi theo chỉ đạo owner; không tự thêm provider, paid fallback, quota, model, timeout hoặc thay luồng tích hợp.
- Bằng chứng / lệnh + kết quả: [T-014-02 decision record](../evidence/T-014-02-policy-review-2026-10-06.md); source/config review ngày 2026-10-06. Owner giữ Gemini qua API proxy hiện có, personal scope và mục tiêu $0/tháng; không gọi provider, không đọc credentials, không áp dụng cấu hình.
- Blocker / bước tiếp theo: Không còn blocker trong phạm vi T-014-02. Owner đã chốt single-user/session identity; idle 15 phút; disconnect kết thúc và reconnect tạo phiên mới; Stop/Pause kết thúc; 1 phiên đồng thời; 20 cặp/8.000 prompt tokens; input 2.000 code points; output 1.024 tokens hoặc 4.000 code points (ngưỡng nào đến trước); tối đa 200 lượt/phiên và 1 lượt đang chạy. T-014-03 đã hoàn tất; chỉ tiếp tục khi owner yêu cầu task kế tiếp.

### T-014-03 — Định nghĩa protocol Chat và fixture tương thích

- Trạng thái: `DONE`; cập nhật: 2026-10-06; phụ trách: Main; phụ thuộc: T-014-02; milestone ExecPlan: 2.
- Công việc: Chốt additive V1 Chat start/turn/end/response/error envelope và payload, correlation, lifecycle/error semantics; đưa các giới hạn đã được owner chấp thuận vào contract; cấm action semantics trong Chat; giữ nguyên Command V1.
- Output / nghiệm thu: `docs/PROTOCOL.md` là nguồn chuẩn; fixture dùng chung bao phủ message hợp lệ, input bất hợp lệ, byte-limit procedure và Command V1 compatibility. Updated SRS link. Chưa triển khai service/client.
- Bằng chứng / validation: [T-014-03 protocol evidence](../evidence/T-014-03-protocol-2026-10-06.md); Python JSON/fixture structure check PASS (7 valid, 7 invalid, legacy Command, 16 KiB); `git -c core.whitespace=cr-at-eol diff --check` PASS.
- Blocker / bước tiếp theo: Không còn blocker trong phạm vi protocol definition. T-014-04 backend đã hoàn tất; Android implementation chưa bắt đầu. Fixture review riêng không thay thế server/client contract tests; backend runtime compatibility đã được kiểm tra trong T-014-04.

### T-014-04 — Backend Chat session và text-only provider path

- Trạng thái: `DONE`; cập nhật: 2026-10-06; phụ trách: Backend agent (triển khai/kiểm chứng), Main (tích hợp/trạng thái); phụ thuộc: T-014-03; milestone ExecPlan: 3.
- Công việc: Triển khai Chat V1 ở backend qua WebSocket hiện có; một phiên volatile gắn connection, 15 phút idle, 200 lượt, một lượt đang chạy, replay ID trong phiên; ngữ cảnh tối đa 20 cặp và 8.000 prompt tokens. Tách hoàn toàn Command context/action path, dùng model không tool callbacks qua quota/admission hiện có.
- Nghiệm thu: start/turn/end/error đúng contract; strict payload, giới hạn Unicode/UTF-8, thời gian/múi giờ và đầu ra; close/expiry/cancel không commit lượt muộn; ngữ cảnh không bền vững; unit/integration tests chứng minh đa lượt, cô lập, duplicate, giới hạn, lỗi và Command V1 vẫn hoạt động. Java25 `./mvnw verify` PASS; không cần Android trong task này.
- Bằng chứng / lệnh + kết quả: [T-014-04 backend evidence](../evidence/T-014-04-backend-chat-2026-10-06.md); backend-agent recheck Java25.0.4.1 `./mvnw -q verify` PASS 71 tests/22 suites, 0 failure/error/skip và JAR 118,663,424 bytes; CRLF-aware `git diff --check` PASS. Audit không cần sửa source/test bổ sung.
- Blocker / bước tiếp theo: Không còn blocker trong backend slice. Live Gemini/account/quota/billing, Android, runnable voice demo và Fold3 acceptance chưa xác minh; dừng trước T-014-05 theo yêu cầu owner.

### T-014-05 — Android Chat session và protocol client

- Trạng thái: `DONE`; cập nhật: 2026-10-06; phụ trách: Android agent (triển khai/kiểm chứng), Main (contract/tích hợp/trạng thái); phụ thuộc: T-014-04; milestone ExecPlan: 4.
- Công việc: Android tạo UUID mới cho mỗi Chat session, dùng lại ID đó cho các turn, gửi/nhận start/turn/end/error qua WebSocket V1 theo `docs/PROTOCOL.md`. Giữ correlation, một turn đang chạy, invalidation khi end/disconnect/reconnect, không replay turn; giữ riêng Command routing, action dispatch và Command history.
- Nghiệm thu: Android parse/validate Chat payload và lỗi theo fixture; response chỉ được nhận khi đúng session/generation/request, không chuyển thành action hoặc ghi Command history; Command V1 vẫn hoạt động. Unit tests và JDK17 Gradle test/lint/APK build pass; nếu host baseline resource merge chặn build, ghi rõ lỗi cùng bằng chứng và giữ trạng thái chưa DONE.
- Ranh giới: TTS, wake/STT orchestration, spoken exit, demo end-to-end, provider và Fold3 acceptance thuộc các task sau; không tự bắt đầu.
- Bằng chứng / lệnh + kết quả: [Android Chat client evidence](../evidence/T-014-05-android-chat-client-2026-10-06.md). JDK17 final-source `./gradlew test --max-workers=1 --offline` PASS: app 130 tests/variant, DPC 5 tests/variant; `./gradlew lint assembleDebug assembleDebugAndroidTest --max-workers=1 --offline` PASS 143 tasks; `git diff --check` PASS. Không commit/push.
- Giới hạn / bước tiếp theo: Client protocol chưa nối vào voice/TTS hoặc UI Chat, chưa có demo provider/AVD/Fold3. Dừng tại T-014-05 theo yêu cầu owner; T-014 tổng thể vẫn `IN_PROGRESS`.

### T-014-06 — Android voice Chat và TTS lifecycle

- Trạng thái: `IN_PROGRESS`; cập nhật: 2026-10-06; phụ trách: Mobile agent; phụ thuộc: T-014-05; milestone ExecPlan: 5.
- Công việc: Nối câu mở Chat vào session/protocol client; gửi các final STT tiếp theo dưới dạng Chat turns, phát reply bằng TTS rồi mở lại STT không cần wake; câu kết thúc kết thúc phiên cục bộ/remote, dừng TTS và vô hiệu callback; Stop/Pause và lỗi/reconnect kết thúc Chat. Tuyệt đối không gọi Command router/action/history cho nội dung Chat.
- Nghiệm thu: Unit tests cho routing câu điều khiển whole-utterance, multi-turn voice-state, TTS completion/cancel và stale callbacks; JDK17 Gradle tests/lint/APK build; không phá Command. Nghiên cứu/prototype xác định liệu detector đáp ứng câu kết thúc khi TTS đang phát có thể dùng được mà không tranh microphone hoặc tự kích bởi echo; nếu chưa thể đáp ứng an toàn, ghi nhận rõ hạn chế và để gate đó mở.
- Bằng chứng: [T-014-06 Android voice evidence](../evidence/T-014-06-android-voice-chat-2026-10-06.md). JDK17 full test/lint/APK validation later PASS during T-014-07; spoken exit during TTS and echo-safe detector remain open.
- Giới hạn: UI stop button, physical spoken-exit/echo/mic-route, demo provider-backed và host/AVD tích hợp đầy đủ còn thuộc các task sau.

### T-014-07 — Host/AVD Chat integration demo

- Trạng thái: `DONE`; cập nhật: 2026-10-06; phụ trách: Mobile agent (Android demo/validation), Main (backend fixture, contract/integration evidence); phụ thuộc: T-014-05, dùng phần voice đã có của T-014-06; milestone ExecPlan: 6.
- Công việc: Chạy luồng ba lượt thời gian → truyện phù hợp thời gian → hỏi tiếp truyện qua WebSocket Chat V1 thực, với backend fixture/provider xác định và Android TTS thật hoặc callback được tiêm có ghi rõ. Kiểm tra Command vẫn theo route cũ, nội dung Chat không dispatch action, kết thúc phiên/xóa context, huỷ/reconnect không phát lại lượt, và không ghi transcript Chat vào lịch sử bền vững. Ghi danh tính APK/JAR/fixture, lệnh chạy và kết quả.
- Nghiệm thu: Demo host/AVD tái chạy được, có bằng chứng từng frame/turn và kết quả TTS; backend và Android build/test phù hợp, protocol fixture parity và diff check PASS. Giữ T-014-06 `IN_PROGRESS` nếu spoken exit trong TTS, callback lifecycle hoặc JDK17 validation còn thiếu; không suy diễn Fold3/provider-backed acceptance từ fixture.
- Bằng chứng: [T-014-07 host demo](../evidence/T-014-07-host-chat-demo-2026-10-06.md). Backend Java25 `./mvnw -q verify` PASS 73 tests/24 suites, 0 failures/errors, 1 opt-in skip; focused real WebSocket demo PASS. Android JDK17 focused live host test PASS 1/1; `./gradlew test lint assembleDebug --max-workers=1 --offline` PASS, app 133 tests/variant, DPC 5/variant, lint/APKs. CRLF-aware diff checks PASS.
- Giới hạn: TTS callback được tiêm; không chạy AVD, mic hoặc TTS thật. Reconnect demo chờ 800 ms sau disconnect vì close bất đồng bộ; không chứng minh reconnect timing xác định. Provider-backed latency/resource, physical mic/echo và phần nghiệm thu T-014-06 còn mở nằm ngoài T-014-07. Dừng trước task tiếp theo theo yêu cầu owner.

### T-014-08 — Provider-backed Chat demo và resource evidence

- Trạng thái: `CANCELLED`; cập nhật: 2026-10-06; phụ trách: Backend agent (provider/host/runtime), Main (tích hợp/evidence/trạng thái); phụ thuộc: T-014-07; milestone ExecPlan: 7.
- Công việc: Chạy Chat V1 với Gemini qua metered proxy hiện có trong ngân sách được chấp thuận. Ghi first-reply latency, kích thước/thời lượng output và mức tăng tài nguyên backend/Android nếu môi trường tương ứng chạy được. So sánh tài nguyên backend với T-013, không dùng fake-planner latency làm baseline provider thật.
- Nghiệm thu: Có lệnh demo tái chạy được, danh tính build/cấu hình không bí mật, kết quả ba lượt time/story/follow-up, kiểm tra không action và số đo latency/output/resource có phương pháp rõ ràng. Nếu thiếu credential/quota hoặc Android runtime, ghi blocker/giới hạn thật và giữ task mở; không khẳng định provider-policy compliance theo chỉ đạo owner.
- Bằng chứng / lệnh + kết quả: [T-014-08 provider probe](../evidence/T-014-08-provider-chat-2026-10-06.md). Java25 `./mvnw -q verify` PASS 73 tests/24 suites, 1 expected skip; diff check PASS. Live WebSocket Chat start PASS; hai lượt Gemini trả correlated `CHAT_PROVIDER_TIMEOUT` sau 10.259/10.134 giây; không có action frame. Host JVM VmRSS 417,084 → 443,004 KiB sau hai timeout, không tương đương Docker T-013. Không có output hay Android resource measurement.
- Retest theo yêu cầu owner ngày 2026-10-06: hai phiên WebSocket Chat độc lập, mỗi phiên một lượt Gemini; cả hai có response text đúng correlation sau 2.086 và 1.135 ms, end đúng phiên, không action frame. Giữ RPM 2, deadline 10 giây, model/proxy hiện có; các lượt cách nhau 31 giây. Không tái hiện timeout trong lần này; xem cùng [evidence](../evidence/T-014-08-provider-chat-2026-10-06.md).
- Demo ba lượt theo yêu cầu owner ngày 2026-10-06: `DEX_GEMINI_TEST_RPM` mặc định và `.env.example` đã tăng 2→10 theo [D-030](../DECISIONS.md#d-030--separate-voice-modes-and-session-only-chat-context). Một phiên provider-backed trả lời time/story/follow-up đúng correlation sau 2.876/2.028/1.198 ms, output 44/708/244 code points, không action frame và end đúng phiên. Java25 final-config `./mvnw -q verify` PASS 73 tests/24 suites, 1 expected skip; JAR SHA256 và lệnh validation ở [evidence](../evidence/T-014-08-provider-chat-2026-10-06.md).
- Blocker / bước tiếp theo: Hai timeout đầu vẫn chưa rõ nguyên nhân dao động; demo ba lượt giờ PASS nhưng chưa đo tài nguyên Android/first-reply TTS hoặc xác minh account quota/billing. Digest auth trong `.env` dài 43 ký tự nên launcher thường từ chối; probe dùng digest thử trong bộ nhớ, không sửa `.env`. Giữ `IN_PROGRESS` cho phần resource/Android còn mở; không khẳng định billing/compliance.
- Ranh giới: Không sửa protocol hoặc tự chọn provider/model/quota/fallback; không bắt đầu Fold3 milestone 8 hay tiếp tục T-014-06 trong task này.

### T-014-09 — Fold3 natural voice Chat acceptance

- Trạng thái: `IN_PROGRESS`; cập nhật: 2026-10-06; phụ trách: Mobile agent (Android/device), Main (integration/evidence/status); phụ thuộc: T-014-06/08; milestone ExecPlan: 8.
- Công việc: Kiểm chứng trên Fold3 wake/start, ba lượt Chat không cần wake, cue/listening, spoken exit khi đang nghe/chờ/TTS, chống TTS echo, mic routes, session cleanup, timing/resources và Command regression. Sửa Android trong phạm vi cần thiết để đạt các gate này; giữ protocol và backend contract hiện có.
- Nghiệm thu: Có bằng chứng thiết bị, lệnh/build identity, ba câu time/story/follow-up được phát bằng TTS thật; local exit không bị TTS tự kích, callback trễ không phát lại, context end đúng; Command action vẫn qua guarded route. JDK17 test/lint/APK và diff check pass. Nếu thiết bị hoặc service không sẵn, ghi rõ gate chưa kiểm chứng và giữ `IN_PROGRESS`.
- ADB simulator follow-up 2026-10-06: API35 AOSP `emulator-5554` on ADB port5037 (selector's starting AVD did not attach to5038). Existing installed APK UI smoke: end-Chat control visible and tap left `MainActivity` resumed; recognizer resolver instrumentation PASS1/1 (Google service, no speech); Vietnamese TTS diagnostic PASS1/1 with `TTS_VI_UNAVAILABLE`. Existing APK hashes, exact case outcomes, and limits: [AVD evidence](../evidence/T-014-avd-adb-2026-10-06.md). No APK install or data/permission reset. No JDK17 for fresh build; active Chat, real voice turn, mic, real TTS, echo, timing/resources and Command regression remain untested.
- Bằng chứng: [T-014-09 Fold3 voice Chat evidence](../evidence/T-014-09-fold3-voice-chat-2026-10-06.md). Mobile agent added on-screen Chat end control and local `ACTION_END_CHAT`; source `git diff --check` PASS. JDK17 Gradle build unavailable in that environment; ADB simulator follow-up is recorded above. Real Fold3 TTS and spoken exit during TTS remain open.
- Ranh giới: chỉ T-014-09; không tự chuyển sang task khác, không commit/push.
