> Migrated to Android on 2026-10-13 ([D-037](../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Business Requirements Document (BRD)
**Project Name:** DeX Assistant (Personal AI Agent)
**Target User:** 1 Single User (Developer).
**Platform:** Samsung Galaxy Z Fold3 (Running Samsung DeX, Battery Bypassed, Always Plugged In).

## 1. Executive Summary
Xây dựng một trợ lý ảo toàn quyền điều khiển thiết bị Android qua giọng nói tự nhiên (*Natural Language Voice Command*). Điểm cốt lõi là không giới hạn ở các lệnh tĩnh (static commands), mà dùng LLM để phân tích ngữ nghĩa, tự động lập kế hoạch (*Reasoning & Planning*) và gọi các hàm can thiệp sâu vào hệ điều hành hoặc UI của app khác.

## 2. Project Objectives
- Mục tiêu zero-touch áp dụng cho từng action đã được kiểm chứng trên Fold3;
  chức năng chưa có API/đặc quyền ổn định phải báo unsupported hoặc best-effort.
- Phản hồi siêu tốc độ (Real-time).
- Chi phí vận hành: $0/tháng (Sử dụng 100% Free Tier services).

## 3. Scope of Work (In-Scope)
- **Voice Trigger:** Luôn lắng nghe lệnh (Wake word) 24/7 không lo tốn pin.
- **System Control:** Quản lý Wifi (bật/tắt, kết nối SSID + Password), Bluetooth, Volume, Screen Brightness, Split-screen.
- **App Automation:** Mở app, tìm kiếm, nhập liệu, click nút thông qua Accessibility Service.
- **Context Awareness:** Nhớ ngữ cảnh qua từng câu lệnh (Slot filling).
- **Local-first operation:** Các lệnh cơ bản mà Android có thể phân loại và thực
  thi chắc chắn phải chạy nội tại app; chỉ yêu cầu ngoài khả năng hoặc mơ hồ mới
  gọi trực tiếp provider từ Android theo D-036. Khi provider không sẵn sàng, app phản hồi
  rõ ràng “Khó quá, không làm được”.
- **Vietnamese routines:** Lệnh local nhận tiếng Việt và ánh xạ routine phổ biến
  sang intent xác định; ví dụ `nhạc lái xe` mở YouTube tìm các tiêu đề liên quan,
  không phụ thuộc playlist cố định.
- **Free-tier control and observability:** Backend quản lý mọi lượt gọi AI qua
  một lớp proxy có budget theo provider. App hiển thị tình trạng backend và
  quota/usage của từng API, đồng thời có thể lưu lịch sử chẩn đoán cục bộ đã
  giới hạn để phục vụ tối ưu hệ thống. Proxy chọn ngẫu nhiên API còn quota và tự
  thử API khả dụng khác khi cần.

## 4. Out of Scope
- Quản lý đa người dùng (Multi-tenant).
- Tối ưu hóa pin (*Battery optimization* - /ɔːptɪmɪˈzeɪʃən/).
