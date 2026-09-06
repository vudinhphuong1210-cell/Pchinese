# CLAUDE.md — Backend Architecture and Conventions

> **Vai trò:** Spring Boot modular monolith là transactional authority của Pchinese. Backend xác minh và thực thi mọi domain/security decision trước khi dữ liệu hoặc provider interaction xảy ra.

## 1. Source of truth

`../AGENT.md` và `../CONSTITUTION.md` là quy tắc cấp dự án. `../CLAUDE.md` định nghĩa domain flow, API, session security và privacy policy. `../DATA_short.md` là canonical schema contract cho MVP; không phát triển migration từ `../DATA.md` trừ khi feature/mapping đã được phê duyệt.

## 2. Module và dependency direction

```text
backend/
|-- pom.xml                                      # Java 21 / Spring Boot 3.4.5 build
|-- mvnw, mvnw.cmd, .mvn/                        # Maven wrapper
|-- AGENT.md
|-- CLAUDE.md
|-- CONSTITUTION.md
`-- src/
    |-- main/
    |   |-- java/net/pchinese/
    |   |   |-- PchineseApplication.java
    |   |   |-- common/
    |   |   |   |-- api/                         # envelope, pagination and API DTO helpers
    |   |   |   |-- config/                      # application, OpenAPI and web configuration
    |   |   |   |-- error/                       # exception types and RestControllerAdvice
    |   |   |   `-- idempotency/                 # shared request-idempotency support
    |   |   |-- security/
    |   |   |   |-- SecurityConfig.java
    |   |   |   |-- JwtAuthenticationFilter.java
    |   |   |   |-- JwtPrincipal.java
    |   |   |   `-- authorization/               # role policy and audit helpers
    |   |   |-- auth/
    |   |   |   |-- api/                         # AuthController and request/response DTOs
    |   |   |   |-- application/                 # registration, credential and session services
    |   |   |   |-- domain/                      # account/session/credential domain types
    |   |   |   `-- persistence/                 # JPA entities and repositories
    |   |   |-- users/
    |   |   |   |-- api/                         # profile and role-management endpoints
    |   |   |   |-- application/
    |   |   |   |-- domain/
    |   |   |   `-- persistence/
    |   |   |-- learning/
    |   |   |   |-- api/                         # topic, lesson and segment DTOs/controllers
    |   |   |   |-- application/
    |   |   |   |-- domain/
    |   |   |   `-- persistence/
    |   |   |-- media/
    |   |   |   |-- application/
    |   |   |   |-- domain/
    |   |   |   `-- integration/                # approved media-provider adapter
    |   |   |-- dictation/
    |   |   |   |-- api/
    |   |   |   |-- application/
    |   |   |   |-- domain/
    |   |   |   `-- persistence/
    |   |   |-- shadowing/
    |   |   |   |-- api/
    |   |   |   |-- application/
    |   |   |   |-- domain/
    |   |   |   `-- persistence/
    |   |   |-- vocabulary/
    |   |   |   |-- api/
    |   |   |   |-- application/
    |   |   |   |-- domain/
    |   |   |   `-- persistence/
    |   |   |-- ai/
    |   |   |   |-- api/                         # AI Buddy public backend endpoint/DTOs
    |   |   |   |-- application/                 # quota, ownership and persistence orchestration
    |   |   |   |-- domain/
    |   |   |   |-- persistence/
    |   |   |   `-- integration/                # typed private ai-service client
    |   |   |-- progress/
    |   |   |   |-- api/
    |   |   |   |-- application/                # sole aggregate-progress authority
    |   |   |   |-- domain/
    |   |   |   `-- persistence/
    |   |   `-- entitlement/
    |   |       |-- api/
    |   |       |-- application/                # plans, content access and AI quota
    |   |       |-- domain/
    |   |       `-- persistence/
    |   `-- resources/
    |       |-- application.yml
    |       |-- application-local.yml           # no committed credentials
    |       `-- db/migration/                   # approved Flyway schema migrations only
    `-- test/
        `-- java/net/pchinese/
            |-- unit/                           # focused service/domain tests
            |-- integration/                    # controller, repository and contract tests
            `-- e2e/                            # critical API journey tests
```

This is the target package-by-feature structure, not permission to scaffold every package in
advance. Create a feature module only with its approved specification. Within a module, keep
controllers, request/response DTOs, services, entities and repositories close together; never add
global `controllers/`, `services/` or `repositories/` folders that mix unrelated domains.

Một module gọi public application service của module khác khi cần workflow cross-domain; không truy cập repository/entity của module khác để lách rule. `progress` là nơi hợp nhất progress, không để Dictation/Shadowing/Vocabulary tự tính dashboard cạnh tranh.

## 3. HTTP và error contract

- Endpoint công khai dùng `/api/v1/*`; route resource dùng kebab-case.
- Controller nhận request DTO rõ ràng, `@Valid`, gọi service và trả DTO qua `{ success, data, error, meta }`. Không expose JPA entity.
- `@RestControllerAdvice` duy nhất map domain exception sang standard envelope. Không trả stack trace, raw provider response, token, password hoặc internal implementation detail.
- Dùng error code ổn định: `VALIDATION_ERROR`, `UNAUTHENTICATED`, `AUTHORIZATION_DENIED`, `ENTITLEMENT_REQUIRED`, `RESOURCE_NOT_FOUND`, `STATE_CONFLICT`, `AI_QUOTA_EXCEEDED`, `PROVIDER_ERROR` và các code đã chuẩn hóa tại `../CLAUDE.md`.
- Collection phải phân trang, filter/sort allowlist và index theo query shape. Resource absent hoặc unowned trả `404` khi contract yêu cầu không disclosure.

## 4. Authorization và session behavior

- Authentication filter chỉ dựng principal sau khi JWT qua toàn bộ checks: signature, algorithm allowlist, `kid`, `iss`, `aud`, `typ`, `exp`/`nbf`, `sid`, active session/user và `authzVersion`.
- Service luôn load current server state rồi kiểm tra ownership, `ADMIN` hoặc entitlement. Không trust role/plan/score/id từ client hoặc từ JWT ngoài identity/session contract.
- Auth/session mutation theo transaction và refresh policy: opaque refresh token hash + pepper, rotation, lock/cas, 30-second idempotency replay và revoke family khi reuse.
- Password reset/change, role change, account disable, entitlement change và logout có tác động `authzVersion`/session theo `../CLAUDE.md`; đừng tự tạo luồng rút gọn.

## 5. Transactions, persistence và migrations

- `@Transactional` đặt tại service method sở hữu business operation, không đặt provider network call trong transaction dài trừ khi feature spec thiết kế rõ.
- JPA repository là đường truy cập DB duy nhất trong application code. Ưu tiên derived query, specification/projection và pagination hơn raw SQL/native query.
- Đảm bảo constraint và index nằm trong Flyway cho schema change được phê duyệt. Data model hiện tại phải khớp `../DATA_short.md`, gồm ownership FK, role history, entitlement active và AI usage event idempotency.
- Mutation quota: lock entitlement, reserve/reuse AI usage event idempotently, update counter, sau đó xử lý outcome/refund theo transaction/state contract.
- Migration không dùng cho seed tạm/experiment. Không sửa migration đã chạy shared/production; luôn test migration trên clean database.

## 6. Provider boundary và privacy

- `AI Service` và `Media Provider` sau typed adapter interface. Adapter chịu request minimization, timeout, retry policy được phê duyệt, safe error mapping và correlation ID.
- Không gửi profile/identity dư thừa, full chat history, raw audio hay secret sang provider nếu feature không cần. Không persist provider credential hoặc signed URL bền vững.
- Upload phải kiểm tra authorization, allowlisted MIME/type, size limit và malware state trước storage/playback. Media upload không tự publish content.
- Recording, chat và learning history là personal data: tôn trọng consent, retention, deletion queue và crypto-erasure policy khi feature được triển khai.

## 7. Test và review contract

- Unit-test service decision: ownership, role lifecycle, entitlement/quota, SRS, state transition, error mapping.
- Integration-test controller/DTO validation, status/envelope, persistence constraints và authorization-sensitive routes.
- Mock provider adapter trong unit/integration; E2E không phụ thuộc AI output không xác định.
- Review kiểm tra ranh giới module, DTO/entity separation, transaction scope, migration safety, input validation, privacy/logging và tests applicable.
