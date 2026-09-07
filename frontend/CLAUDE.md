# CLAUDE.md — Frontend Architecture and Conventions

> **Vai trò:** React SPA là lớp trải nghiệm cho Guest, Learner và Admin. Nó trình bày trạng thái do backend trả về, nhưng không là authority cho security hoặc domain rules.

## 1. Nguồn chuẩn và ranh giới

Đọc tài liệu trong thứ tự nêu ở `AGENT.md`. `../CLAUDE.md` giữ domain flow và API policy cấp dự án; tài liệu này chỉ diễn giải cách frontend thực hiện các policy đó.

Frontend chịu trách nhiệm:

- route-level composition, navigation, form interaction và local view state;
- request/response API client theo contract, mapping response envelope và hiển thị lỗi an toàn;
- accessibility, responsive layout, theme và presentation formatting;
- UI loading, empty, error, retry và optimistic feedback khi API contract cho phép.

Frontend không chịu trách nhiệm:

- kiểm tra JWT, `ADMIN`, ownership, entitlement, quota, state machine, scoring, SRS hay persistence;
- gọi AI/media provider, quản lý provider credential, tạo signed URL tùy ý hoặc ghi dữ liệu trực tiếp vào PostgreSQL.

## 2. Cấu trúc mã nguồn

```text
src/
├── api/          # HTTP wrapper, resource clients theo contract và API schemas
├── components/   # UI dùng lại, không chứa nghiệp vụ feature
├── features/     # admin, auth, dictation, shadowing, vocabulary, ai-buddy, progress
├── pages/        # composition theo route
├── routes/       # route definitions và navigation guards mang tính UX
├── hooks/        # hooks dùng lại
├── lib/          # helper thuần: format, parsing, accessibility
├── contracts/    # shared API schemas/JSDoc definitions không phụ thuộc React
└── test/         # unit, integration, e2e
```

Không tạo một global store khổng lồ. Giữ state gần feature sở hữu nó; chỉ đưa state thật sự cross-route vào lớp shared đã được phê duyệt. API cache không được xem là authorization source. File render JSX dùng `.jsx`; mọi file frontend không render JSX dùng `.js`.

## 3. API và authentication client contract

- Chỉ gọi endpoint `/api/v1/*` qua `src/api/` với DTO có type tường minh.
- Unwrap thành công/thất bại một cách nhất quán từ `{ success, data, error, meta }`; giữ `error.code`, safe message và correlation ID (nếu có) cho UI/support.
- List phải sử dụng pagination trong `meta`; không tải toàn bộ history attempts, chat hay reviews.
- Access token chỉ ở memory. Không ghi access/refresh token vào browser storage; refresh cookie và CSRF header phải theo session contract backend.
- `401 ACCESS_TOKEN_EXPIRED` kích hoạt single-flight refresh theo contract. Nếu refresh thất bại, xóa authenticated state trong memory và điều hướng sign-in; không lặp refresh vô hạn.
- `403 ENTITLEMENT_REQUIRED` có thể hiển thị upgrade CTA; `429 AI_QUOTA_EXCEEDED` hiển thị quota state. Cả hai không được giả định là có thể bypass ở UI.
- Với resource riêng tư trả `404`, hiển thị “không tìm thấy/không còn quyền truy cập”; không cố phân biệt absent với unowned.
- Web gửi access token bằng `Authorization: Bearer`; refresh cookie và CSRF header phải theo backend contract. Không tự tạo, đọc hay log refresh credential.

## 4. Feature UX contract

- Catalog/player chỉ render media metadata backend đã duyệt. Không nhận arbitrary media URL từ input hay query string.
- Dictation/Shadowing chỉ hiển thị score, feedback và completion do server trả về. Disable trạng thái gửi trùng chỉ là UX; API idempotency và quota vẫn do backend bảo vệ.
- Saved word, review và progress luôn refresh/reconcile từ server response; không tự tính `dueAt`, score hoặc aggregate dashboard.
- AI Buddy không đưa system prompt, provider diagnostic hay chat history không cần thiết vào UI state/log. Chỉ learner owner được xem conversation của mình.
- Admin screen chỉ hiển thị công cụ quản trị khi thuận tiện, nhưng mọi content/role command phải xử lý `403`/`409` từ server. `ADMIN` không mở quyền đọc private learner data.

## 5. Design, accessibility và responsive behavior

- Tuân thủ `DESIGN.md`: dark-pink mặc định, CSS semantic tokens, Quicksand/Hanzi fallback, sidebar/drawer và lesson rail.
- Không hard-code giá trị màu theo theme trong component. Theme switch phải thay semantic token mà vẫn giữ meaning của success, warning, destructive và focus ring.
- Card lesson là target điều hướng rõ ràng; không tạo nested button/link invalid. Action phụ phải keyboard/touch accessible.
- Icon-only button có accessible name/tooltip; ảnh bài học có alt phù hợp; dynamic status thông báo vừa đủ cho screen reader.
- Mỗi màn hình hỗ trợ loading, empty, recoverable error và mobile layout. Không lệ thuộc hover để truy cập action quan trọng.

## 6. Kiểm thử và review

- Unit-test utility, formatting, state mapping và component behavior bằng Jest.
- Integration-test API client theo contract với API mock: envelope, validation/error mapping, pagination, 401/403/404/409/429.
- E2E dùng `data-testid`, kiểm tra critical learner/admin flows mà không phụ thuộc output AI không xác định.
- Review phải kiểm tra: response-shape validation, không lộ token/secret, backend authority không bị sao chép, state UI đầy đủ, semantic token, keyboard/mobile behavior.
