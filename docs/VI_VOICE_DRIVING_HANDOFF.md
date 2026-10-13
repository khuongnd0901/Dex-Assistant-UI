> Migrated to Android on 2026-10-13 ([D-037](../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Handoff — Bộ lệnh tiếng Việt không chạm khi lái xe

Ngày: 2026-10-02. Liên quan T-009 (local routing/executor) và T-006
(wake word/STT/TTS/headless). Đây là handoff chuẩn bị triển khai, không phải
tuyên bố các tính năng đã hoạt động hoặc kế hoạch kỹ thuật đã khóa.

## Lựa chọn đã được chủ dự án chốt

- Ưu tiên đợt đầu: nền tảng lái xe, không triển khai toàn bộ danh sách cùng lúc.
- D-016 supersedes the earlier voice choices: **“Hey DeX”** offline via
  sherpa-onnx English KWS; Vietnamese commands through Android SpeechRecognizer
  with online processing allowed, without consent prompts for each command.
  Speak after the readiness cue; quality/false activations require measurement.
- Ứng dụng mục tiêu: Google Maps, YouTube/YouTube Music, Zalo.
- Giao tiếp tự nhiên, nhiều cách nói tương đương; phản hồi ngắn bằng tiếng Việt,
  không yêu cầu nhìn màn hình hoặc dùng remote trong luồng lái xe.
- Đợt đầu tập trung âm lượng/media, mở app, kết nối Wi-Fi có mật khẩu,
  hỏi trạng thái, hủy và xác nhận. Dẫn đường/liên lạc được khảo sát trước,
  không mặc định tự động hóa được chỉ vì mở được ứng dụng.

## Ranh giới an toàn và riêng tư cần giữ

- System STT may receive sensitive command audio under D-016. Credential text
  remains guarded from backend/Gemini and Wi-Fi provisioning is unsupported
  until its local executor exists. No app-owned audio storage/wake upload.
- Không ghi mật khẩu vào log/lịch sử, không đọc lại mật khẩu công khai.
  History chỉ giữ câu đã che thông tin nhạy cảm; không lưu audio.
- Xác nhận bằng giọng nói trước khi gửi tin nhắn/chia sẻ vị trí. Không đoán
  người nhận, địa chỉ, số điện thoại hay mật khẩu khi mơ hồ.
- Không tự gửi lại tác vụ có tác dụng phụ khi timeout/reconnect. Cho phép hủy
  trước khi dispatch; hủy không được báo là hoàn tác một việc đã thực hiện.
- Chỉ báo thành công khi có bằng chứng trạng thái/kết quả; mở app hoặc gửi
  intent chỉ là dispatch. Báo rõ bước thành công/thất bại của lệnh kết hợp.
- Khi lái xe chỉ phát âm thanh, không thiết kế luồng xem video. Kiểm thử thực
  tế khi xe đỗ hoặc trong môi trường kiểm soát, không yêu cầu người lái thao tác.
- Quyền Android, firmware và API ứng dụng có thể giới hạn zero-touch. Nếu cần
  màn hình/xác nhận hệ thống, báo chưa làm được không chạm; không lách bảo vệ.

## Danh mục câu lệnh đề xuất (chưa phải capability hiện hành)

Các nhóm ngoài đợt đầu là backlog cần khảo sát, không phải cam kết đã hỗ trợ.

| Nhóm | Ví dụ tiếng Việt |
| --- | --- |
| Hội thoại | “Hey DeX”; “bạn làm được gì?”; “nói lại”; “nói chậm hơn”; “nói ngắn gọn”; “nói to hơn”; “tiếp tục”; “dừng lại”; “hủy”; “thôi, không cần”; “đúng rồi”; “không phải, ý tôi là…”; “chọn cái đầu tiên”; “chọn cái thứ hai”; “quay lại”; “tạm ngừng nghe”; “ngừng nghe mười phút” |
| Dẫn đường | “chỉ đường về nhà/đến công ty”; “đến sân bay Tân Sơn Nhất”; “tìm cây xăng/trạm sạc/bãi đỗ/nhà vệ sinh/trạm dừng nghỉ/bệnh viện/nhà thuốc gần nhất”; “tìm quán ăn có chỗ đỗ xe”; “thêm cây xăng vào hành trình”; “đổi điểm đến”; “tránh đường thu phí/cao tốc”; “còn bao lâu/bao nhiêu cây số?”; “tôi đang ở đâu?”; “đọc lại hướng rẽ”; “bật/tắt tiếng chỉ đường”; “dừng dẫn đường” |
| Gọi điện | “gọi cho mẹ/vợ/anh Nam ở công ty”; “gọi bằng loa ngoài”; “gọi lại số vừa gọi”; “ai đang gọi?”; “nghe máy”; “từ chối”; “kết thúc cuộc gọi”; “bật/tắt loa ngoài”; “tắt/bật micro cuộc gọi” |
| Nhắn tin/vị trí | “nhắn cho Lan tôi đang lái xe”; “nhắn cho mẹ hai mươi phút nữa con về”; “trả lời tôi gọi lại sau”; “đọc tin nhắn mới nhất của vợ”; “có ai nhắn không?”; “chia sẻ vị trí cho Nam”; “gửi giờ dự kiến đến nơi cho vợ” |
| Media | “mở nhạc”; “nghe tiếp”; “tạm dừng/dừng”; “bài tiếp theo/bài trước”; “bài này tên gì?”; “mở nhạc nhẹ/không lời/yêu thích”; “phát bài … của …”; “mở podcast đang nghe dở”; “tua tới ba mươi giây”; “tua lại mười lăm giây”; “đọc tiếp sách nói”; “dừng phát sau hai mươi phút” |
| Âm lượng | “to lên”; “nhỏ lại”; “tăng/giảm một chút”; “âm lượng media ba mươi phần trăm”; “tắt tiếng”; “bật tiếng lại” |
| Nhắc việc | “nhắc gọi khách lúc ba giờ”; “mười lăm phút nữa nhắc gọi Nam”; “nhắc mua sữa lúc sáu giờ”; “mai bảy giờ nhắc mang giấy tờ”; “hẹn giờ hai mươi phút”; “còn bao lâu?”; “hủy hẹn giờ”; “báo thức sáu giờ sáng mai”; “tắt báo thức ngày mai”; “đọc lời nhắc hôm nay” |
| Ghi chú/lịch | “ghi chú: chiều gửi báo giá”; “ghi lại ý tưởng…”; “thêm nước uống vào danh sách mua sắm”; “đọc danh sách”; “đánh dấu gọi khách đã xong”; “đọc lịch hôm nay”; “cuộc hẹn tiếp theo lúc mấy giờ?” |
| Thông tin | “mấy giờ/ngày bao nhiêu?”; “pin bao nhiêu/đang sạc không?”; “có mạng không?”; “đang kết nối Wi-Fi nào?”; “Bluetooth nối thiết bị nào?”; “thời tiết tại điểm đến”; “chiều có mưa không?”; “đọc thông báo quan trọng”; “có cuộc gọi nhỡ không?”; “tính một trăm tám mươi nghìn chia ba”; “đổi hai mươi đô ra tiền Việt”; “dịch câu này…” |
| Kết nối/chế độ | “bật/tắt Wi-Fi”; “truy cập Wi-Fi abc, mật khẩu [mật khẩu]”; “vào mạng Nhà Tôi, mật khẩu là…”; “kết nối lại mạng vừa dùng”; “ngắt Wi-Fi”; “bật/tắt Bluetooth”; “kết nối với xe”; “ngắt tai nghe”; “bật/tắt không làm phiền”; “chỉ cho cuộc gọi người thân”; “bật/tắt chế độ lái xe”; “tiết kiệm pin”; “giảm độ sáng”; “tắt màn hình, tiếp tục nghe nhạc” |
| Lệnh kết hợp | “chỉ đường về nhà rồi mở nhạc nhẹ”; “tìm cây xăng rồi dẫn tới đó”; “giảm nhạc hai mươi phần trăm rồi đọc tin nhắn”; “dừng nhạc rồi gọi vợ”; “nhắn Nam tôi đến muộn rồi tiếp tục dẫn đường”; “mở podcast, âm lượng ba mươi phần trăm”; “bật chế độ lái xe, đến công ty”; “nhắc gọi khách sau ba mươi phút rồi nghe nhạc tiếp”; “đọc lịch, chỉ giờ và tên”; “tắt tiếng thông báo, giữ tiếng chỉ đường” |

Hiểu biến thể: “về nhà thôi” = “chỉ đường về nhà”; “nhỏ quá” = tăng âm lượng;
“ồn quá” = giảm âm lượng; “khoan đã” = tạm dừng theo ngữ cảnh;
“vào mạng/truy cập Wi-Fi/kết nối mạng” cùng ý định. Hỗ trợ số nói bằng tiếng
Việt, dấu/cách viết và giọng vùng miền; không suy đoán dữ liệu nhạy cảm.

## Sự thật hiện tại và điểm chưa biết

- T-009 vẫn IN_PROGRESS. Local hiện có mở app, Wi-Fi toggle, âm lượng, độ sáng,
  media và YouTube search; quyền và mức evidence khác nhau theo tính năng.
- AVD đã có bảy instrumentation tests; intent YouTube chỉ chứng minh dispatch,
  app AVD hiện có màn hình yêu cầu nâng cấp. Chưa chứng minh cloud-plan thực
  thi trọn vòng trên Android hay toàn bộ hành vi trên Fold3.
- T-006 chưa hoàn tất offline wake/STT/TTS/final-text handoff. Microphone probe
  và trạng thái LISTENING không phải chứng minh nhận dạng lời nói thực tế.
- D-016 đã khóa hướng Hey DeX English KWS + vi-VN SpeechRecognizer online
  allowed. Native/runtime integration and real Fold3 voice quality remain under
  validation; Zalo/Maps zero-touch integration remains backlog.
- Log Docker đã có TimeoutException lúc 06:58:50, 07:00:00, 07:04:16 UTC
  ngày 2026-10-02; proxy đang chờ provider 10s. Chưa chứng minh free tier là
  nguyên nhân. Chưa đổi timeout/restart trong lượt chẩn đoán.

## Trình tự phiên tiếp theo

1. Đọc STATUS/NEXT_SESSION, T-009/T-006 và các ExecPlan liên quan; đọc Android
   AGENTS/STATUS trước công việc Android. Không lặp lại Wave 1 T-009 đã khóa.
2. Triển khai D-016 hybrid voice; kiểm chứng native KWS và vi-VN system STT
   trên AVD/Fold3, đo độ đúng, độ trễ, tiếng nhạc/xe, kích hoạt nhầm, tài nguyên
   và license. Không tự nhận cam kết chi phí mới.
3. Kiểm kê Maps/YouTube Music/Zalo: tách mở app, điều khiển media và gọi/gửi
   tin thực sự; ghi rõ permission/API/quy trình cần màn hình.
4. Main agent khóa kế hoạch và capability/privacy contract đợt đầu, cập nhật
   ExecPlan T-006/T-009 tương ứng. Shared wire/action thay đổi phải phối hợp
   cả hai repo; credentials chỉ local, không thêm mật khẩu vào remote action.
5. Triển khai theo milestone: voice cơ bản → lệnh local mở rộng/Wi-Fi bảo mật
   → hủy/xác nhận/online consent → tích hợp được kiểm chứng. Nhóm còn lại tiếp
   tục là backlog; không tự mở rộng sang gửi Zalo hoặc navigation không có bằng chứng.
6. Kiểm thử parser/executor/privacy/offline, Maven verify nếu backend bị ảnh
   hưởng, Gradle test/lint/assembleDebug, AVD và Fold3. Ghi runnable evidence
   từ voice input đến state/result, không chỉ compile hoặc text injection.
7. Cập nhật task/status/ExecPlan; giữ các acceptance T-009 còn thiếu.
   Commit/push riêng từng repo khi được thực thi trong phạm vi yêu cầu gốc,
   không commit secrets/artifacts hoặc thay đổi user-owned.

Acceptance cần có trước khi gọi đợt đầu hoàn tất: câu gọi và STT/TTS thực sự
hoạt động; lệnh local không mạng; hội thoại hủy/xác nhận không chạm; xử lý
SSID/mật khẩu đúng nguyên văn và không rò rỉ; từ chối/timeout đúng; không
dispatch trùng; trạng thái thực chứng minh kết quả. Ngưỡng đo voice cụ thể
phải khóa trong ExecPlan sau khảo sát, không tự tuyên bố đạt.

## Trạng thái handoff

D-016 hybrid voice implementation is underway; current evidence and exact
remaining gates are in `.agent/evidence/T-006-hybrid-voice-2026-10-02.md`.
No additional command executor or wire shape is introduced. Preserve the
existing T-009 gaps in `.agent/evidence/T-009-avd-docker-2026-10-02.md`.
