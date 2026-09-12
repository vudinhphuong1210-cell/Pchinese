# Developer Quickstart: F13 — Home Experience & Marketing Dashboard

**Feature**: `F13-home-marketing-dashboard` | **Date**: 2026-09-12 | **Spec**: [spec.md](./spec.md)

Tài liệu này hướng dẫn cách chạy, kiểm thử và xác minh tính năng Trang chủ P Chinese (phong cách S Chinese) trên môi trường phát triển cục bộ.

---

## 1. Khởi động môi trường phát triển

### 1.1. Chạy Backend Spring Boot
Từ thư mục `backend/`:
```powershell
mvn spring-boot:run
```
Backend sẽ lắng nghe tại `http://localhost:8080`.

### 1.2. Chạy Frontend Vite
Từ thư mục `frontend/`:
```powershell
npm run dev
```
Frontend sẽ chạy tại `http://localhost:5173`. Mở trình duyệt và truy cập `http://localhost:5173` để xem Trang chủ.

---

## 2. Kiểm thử tự động (Automated Tests)

Chạy bộ kiểm thử Jest cho frontend:
```powershell
npm --prefix frontend test
```

Chạy kiểm thử riêng cho HomePage:
```powershell
npm --prefix frontend test -- HomePage
```

---

## 3. Checklist xác minh thủ công (Manual Verification Checklist)

1. **Giao diện Hero & Slogan**:
   - [ ] Tiêu đề chính hiển thị "Học tiếng Trung thú vị và hiệu quả hơn mỗi ngày".
   - [ ] Các nút CTA "Bắt đầu học ngay", "Luyện Shadowing" bấm vào hoạt động đúng.
2. **Điểm danh Streak**:
   - [ ] Bấm nút "Điểm danh hôm nay! (+10 XP)" → nút chuyển sang trạng thái đã hoàn thành, hiển thị lời chúc động viên và cộng điểm.
3. **4 Khối phương pháp (Shadowing, Dictation, AI Buddy, SRS)**:
   - [ ] Bấm nút trên mỗi card → điều hướng mượt mà đến đúng tab chức năng.
4. **Lưới bài học (Lesson Rail)**:
   - [ ] Bấm đổi chip HSK (HSK 1, HSK 2...) → danh sách bài học lọc chính xác.
   - [ ] Bấm vào Lesson Card → mở trang chi tiết bài học.
5. **Quyền lợi & Premium**:
   - [ ] Bấm "Xem trạng thái gói" → hiển thị modal entitlement và thông báo Premium/cổng thanh toán đang hoàn thiện.
6. **FAQ Accordion**:
   - [ ] Bấm vào các câu hỏi thường gặp → nội dung câu trả lời mở ra mượt mà.
7. **Đổi Theme (10 theme)**:
   - [ ] Thử đổi theme qua Sidebar (Dark Pink, Dark Purple, Dark Blue, Light Pink, Light) → xác nhận 100% màu sắc và độ tương phản hiển thị hoàn hảo.
