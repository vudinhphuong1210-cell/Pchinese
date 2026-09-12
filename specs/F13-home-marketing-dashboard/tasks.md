# Tasks: F13 — Home Experience & Marketing Dashboard

**Feature**: `F13-home-marketing-dashboard` | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

Danh sách nhiệm vụ thực thi (Tasks) theo thứ tự phụ thuộc (dependency-ordered), được chia thành các giai đoạn rõ ràng.

---

## Phase 1: Foundations & Daily Streak Gamification

- [x] **T-000**: Bổ sung backend-authoritative daily streak
  - Thêm bảng/migration `daily_check_ins`, JPA entity/repository/service/controller và API `GET /api/v1/daily-streak`, `POST /api/v1/daily-streak/check-ins`.
  - Dùng timezone hồ sơ, khóa user và unique constraint để chống cộng XP trùng; cập nhật `DATA_short.md` và unit test backend.

- [x] **T-001**: Xây dựng custom hook `useDailyStreak.js` tại `frontend/src/features/home/hooks/useDailyStreak.js`
  - Reconcile state 7 ngày, streak và XP từ backend; chỉ Guest preview dùng state tạo tại client.
- [x] **T-002**: Xây dựng component `DailyStreakWidget.jsx` tại `frontend/src/features/home/components/DailyStreakWidget.jsx`
  - Hiển thị thanh 7 ngày trong tuần với icon trạng thái (tích xanh, lửa streak, hôm nay, sắp tới).
  - Nút bấm tương tác "Điểm danh hôm nay! (+10 XP)" kèm animation chúc mừng khi hoàn thành.
  - Hỗ trợ trạng thái Guest preview và Learner active.

---

## Phase 2: Hero & 4 Core Learning Pillars

- [x] **T-003**: Xây dựng component `HomeHeroSection.jsx` tại `frontend/src/features/home/components/HomeHeroSection.jsx`
  - Tiêu đề chính, phụ đề song ngữ truyền cảm hứng theo chuẩn S Chinese.
  - Các nút CTA chính ("Bắt đầu học ngay", "Luyện Shadowing", "Khám phá lộ trình").
  - 4 badge nhận diện giá trị (Pinyin tức thì, Video YouTube, AI chấm phát âm, Spaced Repetition).
- [x] **T-004**: Xây dựng component `LearningPillarsSection.jsx` tại `frontend/src/features/home/components/LearningPillarsSection.jsx`
  - Trực quan hóa 4 phương pháp học thực chiến (Shadowing, Dictation, AI Buddy, Vocabulary Games & SRS).
  - Mỗi khối gồm icon sinh động, danh sách ưu điểm, badge công nghệ và nút điều hướng tới module học tập tương ứng.

---

## Phase 3: Curated Lessons, Interactive Preview & Community Stats

- [x] **T-005**: Xây dựng component `HomeLessonRail.jsx` tại `frontend/src/features/home/components/HomeLessonRail.jsx`
  - Tích hợp bộ lọc HSK 1–6 và Chủ đề.
  - Hiển thị danh sách video YouTube bài học thực tế từ Catalog API với grid 5 cột chuẩn `DESIGN.md`.
  - Hỗ trợ trạng thái loading skeleton và empty filter state.
- [x] **T-006**: Xây dựng component `InteractiveDemoSection.jsx` tại `frontend/src/features/home/components/InteractiveDemoSection.jsx`
  - Widget trải nghiệm nhanh thẻ từ vựng lật 2 mặt kèm phiên âm Pinyin và âm thanh mẫu cho khách dùng thử.
- [x] **T-007**: Xây dựng component `CommunityStatsSection.jsx` tại `frontend/src/features/home/components/CommunityStatsSection.jsx`
  - Chỉ hiển thị các khả năng đã xác minh (6 HSK, 4 phương pháp, 30 lượt AI Free, 10 theme), không tạo social proof giả.

---

## Phase 4: Monetization, FAQs & Page Assembly

- [x] **T-008**: Xây dựng component `PricingComparisonSection.jsx` tại `frontend/src/features/home/components/PricingComparisonSection.jsx`
  - Bảng 2 cột so sánh quyền lợi chi tiết Free vs Premium.
  - CTA kích hoạt `PremiumUpgradeModal` hiện có; hiển thị đúng Premium/cổng thanh toán đang hoàn thiện.
- [x] **T-009**: Xây dựng component `FaqAccordionSection.jsx` tại `frontend/src/features/home/components/FaqAccordionSection.jsx`
  - Accordion các câu hỏi thường gặp về phương pháp, thiết bị, gói cước và hỗ trợ.
  - Hiệu ứng mở/đóng mượt mà.
- [x] **T-010**: Xây dựng component `HomeFooterCta.jsx` tại `frontend/src/features/home/components/HomeFooterCta.jsx`
  - Banner kêu gọi hành động cuối trang kích thích học viên bắt đầu ngay.
- [x] **T-011**: Ghép nối hoàn chỉnh `HomePage.jsx` tại `frontend/src/pages/HomePage.jsx`
  - Tích hợp tất cả các section thành trang Home hoàn chỉnh, truyền các props và callbacks điều hướng.
- [x] **T-012**: Cập nhật `frontend/src/App.jsx`
  - Kết nối `HomePage` vào tab mặc định hoặc tab `home` / `dashboard`.
  - Đảm bảo các luồng điều hướng giữa Home và các tính năng khác (Lesson Player, Dictation, AI Buddy, Auth, Settings) hoạt động liền mạch.

---

## Phase 5: Verification, Testing & Polish

- [x] **T-013**: Viết unit test & component tests trong `frontend/src/pages/HomePage.test.jsx` và API client liên quan.
- [x] **T-014**: Chạy toàn bộ test suite frontend với `npm test` đảm bảo 100% passing.
- [x] **T-015**: Kiểm tra tính tương thích trên 10 theme màu và responsive bằng Playwright ở viewport mobile 360px và desktop.
