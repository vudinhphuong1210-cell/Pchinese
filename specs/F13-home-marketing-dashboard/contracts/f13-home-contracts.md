# Interface & API Contracts: F13 — Home Experience & Marketing Dashboard

**Feature**: `F13-home-marketing-dashboard` | **Date**: 2026-09-12 | **Spec**: [spec.md](./spec.md)

Tài liệu này định nghĩa các hợp đồng giao tiếp giữa Component Frontend Trang chủ và Backend REST APIs cũng như các Props interfaces của các UI Components.

---

## 1. Frontend Component Props Contracts

### 1.1. `HomePage`
```typescript
interface HomePageProps {
  onOpenLesson: (lessonId: string) => void;       // Mở trình phát bài học hoặc trang chi tiết
  onSelectTab: (tabName: string) => void;         // Điều hướng giữa các phân hệ (shadowing, dictation, ai-buddy, etc.)
  onOpenUpgradeModal: () => void;                 // Mở modal entitlement/Premium status
  onOpenAuth: (mode?: 'login' | 'register') => void;// Mở modal đăng nhập / đăng ký
}
```

### 1.2. `DailyStreakWidget`
```typescript
interface DailyStreakWidgetProps {
  isGuest?: boolean;                              // Trạng thái khách vãng lai
  onRequireAuth?: () => void;                     // Callback khi khách bấm vào nút điểm danh
}
```

### 1.3. `LearningPillarsSection`
```typescript
interface LearningPillarsSectionProps {
  onNavigate: (targetTab: string) => void;        // Điều hướng đến tính năng tương ứng
}
```

### 1.4. `HomeLessonRail`
```typescript
interface HomeLessonRailProps {
  onOpenLesson: (lessonId: string) => void;       // Click vào bài học
  onViewAllCatalog: () => void;                   // Chuyển sang toàn bộ catalog
}
```

### 1.5. `PricingComparisonSection`
```typescript
interface PricingComparisonSectionProps {
  onUpgradeClick: () => void;                     // Click nút "Nâng cấp ngay"
  isPremiumUser?: boolean;                        // Người dùng đã là Premium hay chưa
}
```

### 1.6. `FaqAccordionSection`
```typescript
interface FaqAccordionSectionProps {
  onContactSupport?: () => void;                  // Hỗ trợ liên hệ
}
```

---

## 2. Backend REST API Endpoints Used by Home

### 2.1. Get Published Topics
- **Endpoint**: `GET /api/v1/topics`
- **Query Params**: `page=0&size=50`
- **Headers**: Optional `Authorization: Bearer <jwt>`
- **Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "7fa85f64-5717-4562-b3fc-2c963f66afa6",
        "title": "Giao tiếp hàng ngày",
        "description": "Các bài học đàm thoại quen thuộc",
        "hskLevel": 2,
        "slug": "giao-tiep-hang-ngay",
        "publishedLessonCount": 18
      }
    ],
    "totalElements": 6
  }
}
```

### 2.2. Get Published Lessons
- **Endpoint**: `GET /api/v1/lessons`
- **Query Params**: `topicId={uuid}&hskLevel={1-6}&page=0&size=20`
- **Headers**: Optional `Authorization: Bearer <jwt>`
- **Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "title": "Học tiếng Trung qua bài hát Ánh Trăng Nói Hộ Lòng Tôi",
        "hskLevel": 2,
        "slug": "anh-trang-noi-ho-long-toi",
        "description": "Luyện nghe qua bài hát",
        "estimatedDurationSeconds": 210,
        "publishedSegmentCount": 16,
        "accessLevel": "FREE"
      }
    ],
    "totalElements": 24
  }
}
```

### 2.3. Get Daily Streak
- **Endpoint**: `GET /api/v1/daily-streak`
- **Headers**: `Authorization: Bearer <jwt>` bắt buộc
- **Response**: standard envelope với `currentStreak`, `longestStreak`, `totalXp`, `checkedInToday`, `today` và đúng 7 phần tử `weekdays`.

### 2.4. Check In Today
- **Endpoint**: `POST /api/v1/daily-streak/check-ins`
- **Headers**: `Authorization: Bearer <jwt>` bắt buộc
- **Body**: không có; client không được gửi ngày, XP hay timezone làm authority.
- **Idempotency**: nhiều request trong cùng ngày theo `users.time_zone` chỉ tạo một bản ghi và cộng 10 XP một lần. Response đặt `alreadyCheckedIn=true` cho lần gọi lặp.
- **Response example**:
```json
{
  "success": true,
  "data": {
    "currentStreak": 3,
    "longestStreak": 5,
    "totalXp": 80,
    "checkedInToday": true,
    "alreadyCheckedIn": false,
    "dailyXpReward": 10,
    "awardedXp": 10,
    "today": "2026-09-12",
    "weekdays": [
      { "label": "T2", "date": "2026-09-07", "status": "COMPLETED" }
    ]
  },
  "error": null,
  "meta": { "correlationId": "uuid" }
}
```
