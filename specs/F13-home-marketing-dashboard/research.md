# Research & Design Choices: F13 — Home Experience & Marketing Dashboard

**Feature**: `F13-home-marketing-dashboard` | **Date**: 2026-09-12 | **Spec**: [spec.md](./spec.md)

Tài liệu này tổng hợp phân tích giao diện thực tế của [S Chinese (https://schinese.net/)](https://schinese.net/), các quyết định thiết kế (Design Decisions), cơ chế giữ chân người học (Habit Loop & Gamification) và cách phối hợp với hệ thống Design System Dark Pink của P Chinese.

---

## 1. Phân tích trải nghiệm từ S Chinese (https://schinese.net/)

### 1.1. Các thành phần tạo nên sức hút của S Chinese:
1. **Hero Section súc tích & Đánh trúng nhu cầu**:
   - Khẳng định ngay giải pháp: "Luyện Dictation & Shadowing cùng các bài học Audio, YouTube tuyển chọn, học cùng AI, tra từ tức thì và dịch thuật — tất cả gói gọn trong một đăng ký."
   - Thay vì lý thuyết ngữ pháp khô khan, tập trung vào **nghe - nói thực chiến** và công nghệ hỗ trợ hiện đại.
2. **Habit Loop qua Daily Streak**:
   - Widget điểm danh 7 ngày với các trạng thái rõ ràng, tạo cảm giác thôi thúc (FOMO & Loss Aversion) để người học không muốn bị đứt chuỗi streak.
3. **4 Trụ cột phương pháp trực quan**:
   - Phân định rõ 4 kỹ năng / công cụ: Shadowing (Nói đuổi), Dictation (Chính tả), AI Buddy (Trợ giảng đàm thoại), Spaced Repetition (Lặp lại ngắt quãng & Game).
4. **Bảng so sánh gói dịch vụ (Free vs Premium)**:
   - Minh bạch về quyền lợi: gói Free có quota cấu hình; Premium khi phát hành có quota cao hơn và vẫn được backend kiểm soát. Không mô tả AI hoặc quyền truy cập là “không giới hạn”.
5. **Khu vực FAQs Accordion**:
   - Giải đáp triệt để các rào cản tâm lý của người học: "Dùng trên điện thoại thế nào?", "AI giúp được gì?", "Khác biệt giữa gói Free và Premium?".

---

## 2. Quyết định thiết kế (Design Decisions) cho P Chinese

### Quyết định 1: Hòa quyện triết lý S Chinese với Dark-Pink Dashboard của P Chinese
- *Vấn đề*: S Chinese dùng giao diện sáng/tối hiện đại. P Chinese có bản sắc thương hiệu riêng là **study dashboard tối, ấm, burgundy pha hồng fuchsia (`dark-pink`)** được quy định trong `frontend/DESIGN.md`.
- *Giải pháp*: Giữ nguyên cấu trúc nội dung và logic tính năng tuyệt vời của S Chinese, nhưng áp dụng 100% hệ thống màu sắc semantic tokens của P Chinese (nền `#11090f`, card `#1a0e17`, viền `#2d1527`, primary fuchsia `#f43f8e`). Khi người dùng đổi sang 9 theme còn lại (Dark-Blue, Dark-Purple, Light, etc.), toàn bộ giao diện tự động đồng bộ.

### Quyết định 2: Tích hợp Lưới bài học tương tác với API Catalog thực tế
- *Vấn đề*: Trang chủ không nên chỉ là ảnh tĩnh giới thiệu, mà phải cho phép người học xem và chọn bài học thật ngay lập tức.
- *Giải pháp*: Tích hợp trực tiếp component `TopicFilter`, `HskFilter` và `LessonCard` gọi đến API `/api/v1/lessons` và `/api/v1/topics` với skeleton loading mượt mà.

### Quyết định 3: Gamification với Daily Streak & XP
- *Vấn đề*: Khách vãng lai cần thấy giá trị của việc đăng ký tài khoản; học viên cần động lực duy trì việc học mỗi ngày.
- *Giải pháp*: Xây dựng `DailyStreakWidget` với animation mừng thành tích khi bấm check-in, lưu trạng thái thông minh và hiển thị lời chúc cá nhân hóa theo độ dài chuỗi streak.

---

## 3. Khả năng tương thích & Responsive Grids

| Thiết bị | Breakpoint | Bố cục Trang chủ |
| :--- | :--- | :--- |
| **Mobile (Điện thoại)** | `< 640px` | 1 cột duy nhất; Streak bar dạng trượt ngang gọn gàng; Lesson grid 2 cột; Pricing card xếp chồng dạng tab. |
| **Tablet (Máy tính bảng)** | `640px – 1024px` | 2 cột cho các khối phương pháp; Lesson grid 3 cột; Hero hiển thị 2 CTA song song. |
| **Desktop (Máy tính)** | `> 1024px` | Sidebar cố định 320px bên trái; Workspace cuộn mượt mà; 4 Pillars dạng lưới 2x2 hoặc 4 cột ngang; Lesson grid 5 cột chuẩn `DESIGN.md`. |
