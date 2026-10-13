> Migrated to Android on 2026-10-13 ([D-037](../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Implementation Plan

## Phase 1: Infrastructure & "Brain" Setup (Backend First)
**Goal:** Có một bộ não nhận text, hiểu lệnh và trả về JSON chuẩn xác.
- [ ] Khởi tạo Spring Boot 4 + Spring AI / LangChain4j.
- [ ] Đăng ký Google AI Studio lấy Gemini API Key (Free).
- [ ] Dựng WebSocket server endpoint.
- [ ] Định nghĩa các dummy tools (chỉ in log) để test khả năng routing của LLM.
- [ ] Deploy lên Oracle Cloud Always Free (dùng Docker/Podman).

## Phase 2: Android Core & Connectivity
**Goal:** App Android kết nối được với não, giao tiếp qua lại bằng Text.
- [ ] Khởi tạo Android Project, cấu hình Foreground Service (bỏ qua mọi giới hạn Doze mode).
- [ ] Tích hợp WebSocket Client (OkHttp hoặc Java-WebSocket).
- [ ] Tích hợp Google SpeechRecognizer để nghe lệnh tĩnh (bỏ qua wake word ở phase này để test nhanh).
- [ ] Tích hợp Text-To-Speech (TTS) để app đọc phản hồi.

## Phase 3: System Control (Native APIs)
**Goal:** Thực thi các lệnh hệ thống không cần giao diện.
- [ ] Xử lý Tool `toggleWifi`: Yêu cầu quyền `CHANGE_WIFI_STATE`.
- [ ] Xử lý Tool `connectWifi`: Dùng `WifiNetworkSuggestion.Builder` để kết nối Wifi ngầm định.
- [ ] Xử lý Tool `launchApp` & `splitScreen`: Quản lý các Intent flags.

## Phase 4: UI Automation (Accessibility)
**Goal:** Tương tác sâu vào các app bên thứ 3 (Youtube, Zalo...).
- [ ] Viết lớp kế thừa `AccessibilityService`. Yêu cầu cấp quyền thủ công trong Settings.
- [ ] Xử lý Tool `clickUI`: Duyệt `AccessibilityNodeInfo` tìm node chứa text -> `performAction(ACTION_CLICK)`.
- [ ] Xử lý Tool `inputText`: Tìm EditText đang focus -> `performAction(ACTION_SET_TEXT)`.

## Phase 5: Always-On Wake Word (Hoàn thiện)
**Goal:** Không cần bấm nút, tự động lắng nghe bằng "Hey Robot".
- [ ] Tích hợp Picovoice Porcupine (Free Tier) chạy liên tục trong Foreground Service.
- [ ] Tối ưu luồng: Wake Word detected -> Bật STT -> Lấy Text -> Bắn Server.