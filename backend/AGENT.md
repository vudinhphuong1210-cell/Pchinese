# AGENT.md — Backend Context for AI Agents

> **Phạm vi:** Chỉ áp dụng cho `backend/`. Tài liệu ở thư mục cha vẫn ràng buộc; khi có xung đột, `../CONSTITUTION.md` và `../AGENT.md` được ưu tiên.

## 1. Đọc trước khi thay đổi

1. `../AGENT.md` — stack, operating rules và Definition of Done cấp dự án.
2. `../CONSTITUTION.md` — luật kỹ thuật, security và review không thể thương lượng.
3. `../CLAUDE.md` — domain flow, JWT/session policy, API/error contract, privacy/retention.
4. `../DATA_short.md` — schema contract canonical cho MVP; `../DATA.md` chỉ là kiến trúc đích, không phải migration source hiện tại.
5. `../API.md` và feature spec liên quan tại `../specs/<feature>/spec.md`.

## 2. Tech stack và phạm vi sở hữu

- Java 21, Spring Boot 3.4.5, Maven, PostgreSQL 18 và Spring Data JPA.
- JUnit 5 + Mockito cho unit test; integration test dùng Spring Boot và PostgreSQL-compatible setup.
- Backend sở hữu API, validation, authentication, authorization, ownership, entitlement/quota, domain state, persistence, transactions và controlled provider adapters.
- React không là authority. Không để controller hoặc request body quyết định `ADMIN`, Premium, score, quota, ownership hay lifecycle state.

## 3. Layer và module rules

```text
Controller → request DTO validation → Service → Repository → JPA Entity
```

- Controller: parse/authenticate request, `@Valid` DTO, gọi một application service, trả standard envelope; không chứa business rule hoặc entity response.
- Service: authorization, ownership, entitlement, transaction, domain state transition và orchestration.
- Repository: Spring Data JPA persistence query/projection phân trang; không raw SQL hoặc string-built query.
- Entity: mapping persistence nội bộ; không expose trực tiếp qua API.
- Tổ chức theo package-by-feature: `auth`, `security`, `users`, `learning`, `media`, `dictation`, `shadowing`, `vocabulary`, `ai`, `progress`, `entitlement`, cùng `common`.

## 4. Quy tắc bảo mật và domain bắt buộc

- DTO request nào cũng dùng Jakarta Bean Validation; path/query/upload metadata cũng phải validate.
- Mọi resource learner-scoped đều kiểm tra ownership trong service. `ADMIN` không bypass attempts, saved words, recordings hoặc AI conversations.
- `ADMIN` là role server-managed, độc lập Premium. Role grant/revoke phải audit, invalidate target sessions, cấm self-change và cấm xóa Admin cuối cùng.
- Premium lesson access và AI quota luôn kiểm tra server-side. Quota được reserve/idempotent atomically trước AI provider call.
- Password bcrypt cost ≥ 12. JWT bắt buộc xác thực signature, algorithm allowlist, `kid`, issuer, audience, type, expiry, session active và `authzVersion`.
- Không persist/log JWT, raw refresh token, password, provider secret hay learner-private payload không cần thiết. Refresh token rotation/reuse phải theo `../CLAUDE.md`.
- AI và media chỉ được gọi qua backend adapter; validate/minimize request, không tin URL/identifier do client tự khai.

## 5. Persistence, migration và privacy

- Chỉ Spring Data JPA repository truy cập dữ liệu ứng dụng; không raw SQL trong application code.
- Schema MVP lấy `../DATA_short.md` làm nguồn chuẩn. Migration Flyway chỉ cho schema change đã phê duyệt, tập trung, test clean database; consolidate migration chưa merge và không sửa migration đã áp dụng shared/production.
- Dùng transaction service cho mutation liên quan attempt/progress, saved-word/SRS, entitlement/quota, role/session hoặc deletion state.
- Tôn trọng retention, deletion, consent và data-minimization trong `../CLAUDE.md`. Không tạo private search, analytics, retention hoặc deletion shortcut ngoài contract được phê duyệt.

## 6. Definition of Done cho backend

- [ ] Feature spec xác định actor, endpoint, role/ownership/entitlement, state change, failure case và acceptance criteria.
- [ ] DTO validation, service authorization, error mapping và Swagger/OpenAPI được cập nhật cùng endpoint.
- [ ] Standard envelope `{ success, data, error, meta }` và HTTP/error code đúng contract.
- [ ] Unit và integration tests bao phủ happy/error path, ownership, `ADMIN`, entitlement/quota khi áp dụng.
- [ ] Transaction/migration safety được kiểm tra; không lộ secret, PII, token, stack trace hay provider detail.
- [ ] Maven test/format/lint/build liên quan không còn lỗi, không có debug/dead code/TODO chưa xử lý.
