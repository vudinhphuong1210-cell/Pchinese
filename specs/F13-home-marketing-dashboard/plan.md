# Implementation Plan: F13 — Home Experience & Marketing Dashboard

**Branch**: `F13-home-marketing-dashboard` | **Date**: 2026-09-12 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/F13-home-marketing-dashboard/spec.md`

## 1. Summary

Xây dựng toàn diện giao diện Trang chủ (Home Page) của **P Chinese** theo cấu trúc và phong cách hiện đại của [S Chinese](https://schinese.net/), phục vụ cả hai đối tượng: khách truy cập (Guest) và học viên đã đăng nhập (Learner). Trang chủ đóng vai trò là Hub tổng quan điều hướng đến 4 tính năng học thực chiến (Shadowing, Dictation, AI Buddy, Vocabulary Games & SRS), tích hợp thanh điểm danh hàng ngày (Daily Streak Widget), thư viện video YouTube tuyển chọn theo HSK 1–6, bảng so sánh gói Free/Premium và khu vực FAQ Accordion.

Giao diện tuân thủ 100% **Design System P Chinese** (`frontend/DESIGN.md`), thiết lập tông màu tối ấm Dark Pink mặc định (`330 40% 5%`), sử dụng biến HSL semantic tokens và tương thích hoàn toàn với 10 theme màu.

---

## 2. Technical Context

| Lĩnh vực | Quyết định kỹ thuật |
| :--- | :--- |
| **Framework & Ngôn ngữ** | React 18 + JavaScript/JSX (không dùng class component) + Vite. |
| **Styling & Theme** | Tailwind CSS 3.x kết hợp CSS Custom Properties semantic tokens (`bg-background`, `text-foreground`, `bg-card`, `border-border`, `text-primary`, `bg-secondary`, `bg-muted`...). Không hard-code mã màu hex/rgb. |
| **Quản lý trạng thái** | `authSessionStore` cho xác thực/phân quyền, React local state cho UI filters/accordion; streak và XP luôn lấy từ backend, không lưu dữ liệu tiến độ cá nhân trong browser storage. |
| **Tích hợp API** | REST API endpoints `/api/v1/topics` và `/api/v1/lessons` từ module Catalog sẵn có. |
| **Bảo mật & Phân quyền** | Guest xem Home và catalog công khai; Learner mới gọi API điểm danh. Backend khóa tài khoản và áp dụng unique constraint để cộng XP đúng một lần/ngày. CTA Premium mở modal entitlement; thanh toán chưa thuộc MVP. |
| **Testing** | Jest + React Testing Library cho unit test & component tests; Playwright cho kiểm thử E2E tương tác. |
| **Thiết bị hỗ trợ** | Fully responsive từ Mobile (360px) đến Desktop 4K (2560px). |

---

## 3. Constitution Check

| Tiêu chí | Kết quả | Giải pháp kỹ thuật |
| :--- | :--- | :--- |
| **Tech Stack (Article 1)** | PASS | React 18 + Vite + Tailwind CSS 3.x. Không cài thêm framework ngoài quy định. |
| **Coding Standards (Article 2)** | PASS | Component dùng PascalCase (`HomePage.jsx`, `DailyStreakWidget.jsx`), utility camelCase, response envelope tuân thủ chuẩn REST. |
| **Security & Auth (Article 3)** | PASS | Giao diện React không tự cấp quyền Premium hay bypass xác thực. Mọi tương tác vào bài học đều qua API backend Spring Boot kiểm tra JWT. |
| **Design System (Article 6 & DESIGN.md)** | PASS | Màu sắc semantic token, dark-pink burgundy mặc định, hỗ trợ 10 themes, bo góc và viền phân lớp tối. |

---

## 4. Project Structure

### Documentation (`specs/F13-home-marketing-dashboard/`)

```text
specs/F13-home-marketing-dashboard/
├── spec.md                     # Đặc tả chi tiết tính năng
├── plan.md                     # Kế hoạch kiến trúc và triển khai
├── data-model.md               # Mô hình dữ liệu UI, ViewModel & State
├── research.md                 # Nghiên cứu S Chinese & Design Patterns
├── tasks.md                    # Danh sách task chi tiết theo giai đoạn
├── quickstart.md               # Hướng dẫn kiểm thử và chạy nhanh
├── checklists/
│   └── requirements.md         # Checklist chất lượng đặc tả
└── contracts/
    └── f13-home-contracts.md   # Hợp đồng giao tiếp UI & API
```

### Source Code (`frontend/src/`)

```text
frontend/src/
├── features/home/
│   ├── components/
│   │   ├── HomeHeroSection.jsx         # Hero banner, slogan, CTAs, value badges
│   │   ├── DailyStreakWidget.jsx       # Widget điểm danh 7 ngày & tích lũy XP
│   │   ├── LearningPillarsSection.jsx  # 4 card phương pháp (Shadowing, Dictation, AI, SRS)
│   │   ├── HomeLessonRail.jsx          # Lưới bài học video YouTube kèm bộ lọc HSK
│   │   ├── InteractiveDemoSection.jsx  # Khu vực tương tác nhanh (Flashcard preview)
│   │   ├── PricingComparisonSection.jsx# Bảng so sánh 2 cột Free vs Premium
│   │   ├── FaqAccordionSection.jsx     # Accordion câu hỏi thường gặp
│   │   ├── CommunityStatsSection.jsx   # Thống kê bài học, từ vựng, học viên
│   │   └── HomeFooterCta.jsx           # Banner kêu gọi hành động cuối trang
│   └── hooks/
│       └── useDailyStreak.js           # Custom hook quản lý trạng thái điểm danh
├── pages/
│   └── HomePage.jsx                    # Trang chủ hoàn chỉnh ghép nối các components
└── App.jsx                             # Tích hợp HomePage vào luồng ứng dụng chính
```

---

## 5. Implementation Phases

- **Phase 1: Foundations & Streak Engine**:
  - Xây dựng `useDailyStreak.js` và `DailyStreakWidget.jsx` với animation chúc mừng và lưu trữ điểm danh.
- **Phase 2: Hero & Core Learning Pillars**:
  - Xây dựng `HomeHeroSection.jsx` với thông điệp chuẩn S Chinese và các nút CTA.
  - Xây dựng `LearningPillarsSection.jsx` trực quan hóa 4 phương pháp học thực chiến.
- **Phase 3: Curated Lessons & Interactive Showcases**:
  - Xây dựng `HomeLessonRail.jsx` tích hợp với API Catalog và bộ lọc HSK / Chủ đề.
  - Xây dựng `InteractiveDemoSection.jsx` cho khách trải nghiệm nhanh.
- **Phase 4: Monetization, FAQs & Assembly**:
  - Xây dựng `PricingComparisonSection.jsx` và `FaqAccordionSection.jsx`.
  - Hoàn thiện `HomePage.jsx` và cập nhật `App.jsx`.
- **Phase 5: Automated Testing & Verification**:
  - Viết unit tests cho các component mới, chạy `npm test` và kiểm tra 10 theme.
