# CONSTITUTION.md — Backend Law

> Tài liệu này ràng buộc mọi thay đổi trong `backend/`. `../CONSTITUTION.md` là luật cấp dự án và có quyền ưu tiên; thay đổi tài liệu này cần review của team backend và không được làm suy yếu luật gốc.

## ARTICLE 1 — Runtime và architecture bất biến

- Java 21, Spring Boot 3.4.5, Maven, PostgreSQL 18 và Spring Data JPA là stack bắt buộc.
- Backend là modular monolith; không thêm microservice, Kafka, Redis, API Gateway hoặc NoSQL nếu chưa có feature spec và constitutional approval.
- Mã tổ chức theo domain module. Controller → Service → Repository → Entity là dependency direction chuẩn.

## ARTICLE 2 — API và business boundary

- Controller dùng request/response DTO, Jakarta Bean Validation và standard response envelope; không trả JPA entity hoặc chứa business logic.
- Service sở hữu authorization, entitlement, ownership, transaction và domain state transition.
- Repository chỉ dùng Spring Data JPA. Raw SQL, native-query convenience, string-built query và truy cập database ngoài repository bị cấm trong application code.
- Error phải đi qua centralized `@RestControllerAdvice`, với stable code và không lộ stack trace/internal provider details.

## ARTICLE 3 — Security không thể thương lượng

- Password chỉ bcrypt cost ≥ 12. Secret, credential, JWT signing material, `.env`, raw token và PII không được commit hoặc log.
- Protected API kiểm tra JWT đầy đủ, active server session và `authzVersion` trước service authorization.
- `ADMIN` là role server-managed, không suy diễn từ request, JWT permission list, client state, route hay Premium. Admin không có bypass với private learner resources.
- Ownership, Premium access và AI quota luôn được enforce phía service. Client input không bao giờ là source of truth cho score, state hay permission.
- AI/media provider luôn qua controlled adapter. Upload phải validate và scan trước exposure.

## ARTICLE 4 — Data và migration integrity

- `../DATA_short.md` là canonical MVP schema contract. `../DATA.md` là target architecture, không phải nguồn migration hiện tại.
- Flyway chỉ chứa schema change đã phê duyệt; migration phải focused, test clean database, consolidate trước merge và không sửa/xóa sau shared/production apply.
- Dùng transaction và DB constraint cho integrity quan trọng. Quota/refresh/idempotency phải an toàn khi concurrent; không chấp nhận read-then-write race.
- Tôn trọng data minimization, consent, retention, deletion và audit contract. Không hard delete/cross-store cleanup ngoài workflow được phê duyệt.

## ARTICLE 5 — Quality gate

- Không merge nếu validation, authorization, API docs, tests, format/build hoặc error contract chưa hoàn chỉnh.
- Backend unit/integration tests phải bao phủ happy path và failure path; feature sensitive phải test ownership, `ADMIN`, entitlement/quota và state conflict.
- Provider adapter phải được mock trong unit/integration tests; review bắt buộc xem transaction scope, privacy/logging và migration impact.
