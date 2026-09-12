# Data Model & State Specifications: F13 — Home Experience & Marketing Dashboard

**Feature**: `F13-home-marketing-dashboard` | **Date**: 2026-09-12 | **Spec**: [spec.md](./spec.md)

Tài liệu này mô tả chi tiết mô hình dữ liệu (Data Model), cấu trúc State ở Frontend và ánh xạ DTO từ Backend sang View Model của Trang chủ.

---

## 1. Frontend State Models & ViewModels

### 1.1. Daily Streak State (`DailyStreakState`)

Quản lý trạng thái điểm danh trong tuần của người học:

```typescript
interface DailyStreakState {
  currentStreak: number;          // Số ngày streak liên tục hiện tại (ví dụ: 5)
  longestStreak: number;          // Kỷ lục chuỗi dài nhất (ví dụ: 14)
  totalXp: number;                // Tổng XP do backend cộng
  checkedInToday: boolean;        // Đã điểm danh hôm nay chưa
  alreadyCheckedIn: boolean;      // POST là retry hay lần ghi đầu tiên
  dailyXpReward: number;          // Mức thưởng mỗi ngày hiện tại
  awardedXp: number;              // XP thực cộng trong request hiện tại (0 khi GET/retry)
  weekdays: {
    label: string;                // "T2", "T3", ... "CN"
    date: string;                 // "2026-09-12"
    status: 'COMPLETED' | 'MISSED' | 'TODAY' | 'UPCOMING';
  }[];
}
```

### 1.2. Learning Pillar Item Model (`LearningPillar`)

Cấu trúc dữ liệu cho 4 khối phương pháp học tập:

```typescript
interface LearningPillar {
  id: 'shadowing' | 'dictation' | 'ai-buddy' | 'vocabulary-srs';
  title: string;                  // "Luyện Shadowing", "Luyện Dictation", etc.
  subtitle: string;               // Tiêu đề phụ súc tích
  description: string;            // Mô tả cách học và lợi ích thực chiến
  badge: string;                  // "Video YouTube", "AI Powered", "SM-2 Algorithm"
  icon: string;                   // Tên icon Lucide
  highlights: string[];           // Danh sách 3-4 điểm mạnh chính
  ctaText: string;                // "Luyện nói ngay", "Thử Dictation", "Chat với AI"
  targetTab: string;              // 'dashboard' | 'dictation' | 'ai-buddy' | 'vocabulary'
}
```

### 1.3. Pricing Tier Item Model (`PricingTier`)

Cấu trúc dữ liệu cho bảng so sánh Free vs Premium:

```typescript
interface PricingTier {
  id: 'free' | 'premium';
  name: string;                   // "Free" | "Premium"
  tagline: string;                // Mô tả quyền lợi đã được phê duyệt
  badge?: string;                 // "Phổ biến nhất" | "Siêu hời"
  priceMonthly: string;           // "0đ" | "Đang hoàn thiện"
  features: {
    label: string;
    included: boolean;
    highlight?: boolean;
    note?: string;
  }[];
  ctaText: string;                // "Đang sử dụng" | "Nâng cấp Premium"
  isPopular?: boolean;
}
```

### 1.4. FAQ Item Model (`FaqItem`)

Cấu trúc câu hỏi thường gặp dạng accordion:

```typescript
interface FaqItem {
  id: string;
  question: string;
  answer: string;
  category: 'methodology' | 'ai' | 'pricing' | 'device';
}
```

---

## 2. API Integration & DTO Mapping

### 2.1. Topics DTO (từ `/api/v1/topics`)
```json
{
  "content": [
    {
      "id": "uuid",
      "slug": "daily-life",
      "title": "Đời sống hàng ngày",
      "description": "Các đoạn hội thoại mua sắm, ăn uống, làm quen",
      "hskLevel": 2,
      "publishedLessonCount": 24
    }
  ],
  "totalElements": 6,
  "totalPages": 1
}
```

### 2.2. Lessons DTO (từ `/api/v1/lessons`)
```json
{
  "content": [
    {
      "id": "uuid",
      "topicId": "uuid",
      "title": "Đặt đồ ăn tại nhà hàng Bắc Kinh",
      "slug": "dat-do-an-bac-kinh",
      "description": "Hội thoại gọi món trong nhà hàng",
      "hskLevel": 2,
      "estimatedDurationSeconds": 180,
      "publishedSegmentCount": 12,
      "accessLevel": "FREE"
    }
  ],
  "totalElements": 48
}
```

---

## 3. Backend Persistence & Browser State

- **Daily check-in**: bảng `daily_check_ins` lưu `daily_check_in_id`, `user_id`, `check_in_date`, `awarded_xp`, `created_at`; unique `(user_id, check_in_date)`. Ngày tính theo `users.time_zone`. Backend là authority cho streak và XP.
- **Frontend streak state**: chỉ giữ trong React memory và luôn reconcile bằng response API. Guest dùng preview không chứa dữ liệu cá nhân.
- **Theme Cache**: `localStorage.getItem('pchinese_theme')` đảm bảo giao diện hiển thị đúng theme người dùng chọn ngay khi khởi tạo trang mà không bị nhấp nháy (FOUC).
