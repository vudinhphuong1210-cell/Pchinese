# Pchinese — Database MVP gọn

> **Trạng thái:** Schema contract gốc (canonical) cho Sprint 1/MVP. Mọi Flyway migration hiện tại phải bắt nguồn từ tài liệu này.  
> **Quan hệ với `DATA.md`:** `DATA.md` chỉ là kiến trúc đích/dự kiến. Khác biệt giữa hai tài liệu là có chủ đích; chỉ áp dụng phần thiết kế đích sau khi feature được phê duyệt, có migration plan/ADR và cập nhật contract MVP hoặc successor của nó.  
> Phạm vi: schema đủ để triển khai Sprint 1/MVP, không phải toàn bộ production-compliance design.  
> Nguồn: `AGENT.md`, `CLAUDE.md`, `CONSTITUTION.md`.  
> PostgreSQL 18 + Spring Data JPA; không dùng raw SQL trong application code.

## 1. Phạm vi và quy ước

MVP dùng **24 bảng** cho: đăng ký/đăng nhập, role `ADMIN`, Free/Premium + quota AI, catalog bài học, Dictation, Shadowing, từ điển/SRS, tiến độ và AI Buddy.

- Bảng/cột dùng `snake_case`, bảng số nhiều; primary key là `uuid`.
- Thời gian dùng `timestamptz` (UTC), trạng thái dùng `varchar` map Java enum và có `CHECK` trong Flyway migration.
- `*_ciphertext` là `bytea` đã mã hóa cấp ứng dụng; `*_hash` là `char(64)` HMAC/SHA-256. Không lưu token thô, JWT, password, API key hay arbitrary media URL.
- Mọi resource riêng tư có `user_id`; service luôn kiểm tra ownership. `ADMIN` chỉ quản trị content/role, không đọc attempts, recordings hoặc chat của learner.
- Các cột `version bigint not null default 0` dùng optimistic locking. Refresh token và quota phải được cập nhật trong transaction khóa dòng.

### Ranh giới dữ liệu AI Service

- `ai-service` là private, stateless và không có bảng hoặc product-data persistence riêng. Spring
  Boot là nơi duy nhất tạo/đọc/ghi conversation, message, recording, attempt, score, progress,
  entitlement và AI usage event.
- `ai_usage_event_id` là correlation ID do backend tạo và truyền qua private HMAC/mTLS contract;
  `client_request_id` chỉ là idempotency key cho logical request từ client. Không truyền browser JWT,
  provider credential, prompt/raw transcript hoặc raw recording vào ledger.
- Với F08, Spring Boot đọc recording đã kiểm tra ownership, trạng thái và malware scan rồi stream
  trực tiếp bytes cần thiết qua private HMAC/mTLS request. `ai-service` không có object-storage
  credential, object key, signed URL hoặc persistent access reference.
- Chỉ một F08 actual `SHADOWING_ASSESSMENT` và một F11 learner message yêu cầu assistant reply mới
  tiêu thụ quota trong MVP. Mở Shadowing và lifecycle conversation AI Buddy không tạo
  `ai_usage_events`; F07 Dictation là deterministic tại Spring Boot và không tạo event AI.
- F08 MVP chỉ lưu score và feedback đã được Spring Boot xác thực; transcript do speech engine tạo
  không được lưu, trả về hoặc dùng làm dữ liệu product.

### Enum tối thiểu

| Nhóm                       | Giá trị                                                      |
| -------------------------- | ------------------------------------------------------------ |
| `user_status`              | `PENDING_VERIFICATION`, `ACTIVE`, `LOCKED`, `DISABLED`       |
| `publication_state`        | `DRAFT`, `PUBLISHED`, `UNPUBLISHED`, `ARCHIVED`              |
| `access_level`             | `FREE`, `PREMIUM`                                            |
| `entitlement_status`       | `ACTIVE`, `EXPIRED`, `CANCELED`, `REVOKED`                   |
| `attempt_status`           | `IN_PROGRESS`, `SUBMITTED`, `EVALUATED`, `FAILED`, `DELETED` |
| `recording_classification` | `ASSESSMENT_ONLY`, `SAVED_RECORDING`                         |
| `review_rating`            | `AGAIN`, `HARD`, `GOOD`, `EASY`                              |
| `srs_status`               | `LEARNING`, `REVIEW`, `RELEARNING`, `SUSPENDED`              |
| `ai_usage_status`          | `RESERVED`, `SUCCEEDED`, `FAILED_REFUNDED`, `FAILED_CONSUMED` |

## 2. Quan hệ chính

```text
users ──< user_roles
  ├──< auth_sessions ──< refresh_tokens
  ├──< user_entitlements ──> subscription_plans
  │     └──< ai_usage_events
  ├──< dictation_attempts >── segments ──> lessons ──> topics
  ├──< shadowing_attempts ──> recordings
  ├──< saved_words >── dictionary_entries
  ├──< srs_schedules ──< srs_review_events
  ├──< lesson_progresses >── lessons
  └──< ai_conversations ──< ai_messages

segments ──> media_assets
```

## 3. Bảng tài khoản và xác thực

### `users`

Gộp profile/cài đặt cơ bản vào user để giảm một bảng cho MVP.

| Cột                       | Kiểu           | Ghi chú                                                       |
| ------------------------- | -------------- | ------------------------------------------------------------- |
| `user_id`                 | `uuid`         | PK.                                                           |
| `email_ciphertext`        | `bytea`        | NOT NULL; email đã mã hóa.                                    |
| `email_lookup_hash`       | `char(64)`     | NOT NULL, UNIQUE; hash email chuẩn hóa.                       |
| `password_hash`           | `varchar(255)` | NOT NULL; bcrypt cost >= 12.                                  |
| `status`                  | `varchar(32)`  | NOT NULL default `PENDING_VERIFICATION`; `user_status`.       |
| `email_verified_at`       | `timestamptz`  | NULL.                                                         |
| `display_name_ciphertext` | `bytea`        | NULL.                                                         |
| `native_language_code`    | `varchar(10)`  | NOT NULL default `vi`.                                        |
| `interface_locale`        | `varchar(16)`  | NOT NULL default `vi-VN`.                                     |
| `time_zone`               | `varchar(64)`  | NOT NULL default `Asia/Ho_Chi_Minh`.                          |
| `target_hsk_level`        | `smallint`     | NULL; CHECK 1–6.                                              |
| `daily_goal_minutes`      | `smallint`     | NOT NULL default `15`; CHECK `> 0`.                           |
| `authz_version`           | `bigint`       | NOT NULL default `1`; tăng khi đổi password/role/entitlement. |
| `kms_key_reference`       | `varchar(255)` | NULL; reference DEK/KMS, không lưu key material.              |
| `last_activity_at`        | `timestamptz`  | NOT NULL.                                                     |
| `created_at`              | `timestamptz`  | NOT NULL.                                                     |
| `updated_at`              | `timestamptz`  | NOT NULL.                                                     |
| `version`                 | `bigint`       | NOT NULL default `0`.                                         |

Index: UNIQUE `email_lookup_hash`; `(status, last_activity_at)`.

### `user_roles`

| Cột                  | Kiểu          | Ghi chú                                          |
| -------------------- | ------------- | ------------------------------------------------ |
| `user_role_grant_id` | `uuid`        | PK; một lần cấp role.                            |
| `user_id`            | `uuid`        | NOT NULL FK → `users`.                           |
| `role_code`          | `varchar(32)` | NOT NULL; hiện chỉ `ADMIN`.                      |
| `granted_by_user_id` | `uuid`        | NULL FK → `users`; NULL chỉ cho bootstrap Admin. |
| `granted_at`         | `timestamptz` | NOT NULL.                                        |
| `revoked_at`         | `timestamptz` | NULL; row revoked là immutable.                  |
| `correlation_id`     | `uuid`        | NOT NULL.                                        |

Ràng buộc: UNIQUE partial `(user_id, role_code) WHERE revoked_at IS NULL`. Re-grant phải tạo row mới để giữ lịch sử; service cấm tự đổi role hoặc xóa Admin cuối cùng.

### `auth_sessions`

| Cột                   | Kiểu           | Ghi chú                             |
| --------------------- | -------------- | ----------------------------------- |
| `session_id`          | `uuid`         | PK.                                 |
| `user_id`             | `uuid`         | NOT NULL FK → `users`.              |
| `family_id`           | `uuid`         | NOT NULL; refresh-token family.     |
| `authz_version`       | `bigint`       | NOT NULL; snapshot lúc cấp session. |
| `device_id`           | `varchar(128)` | NOT NULL.                           |
| `device_label`        | `varchar(120)` | NOT NULL; sanitized.                |
| `platform`            | `varchar(24)`  | NOT NULL: `WEB`, `IOS`, `ANDROID`.  |
| `ip_hash`             | `char(64)`     | NULL; không lưu IP thô.             |
| `created_at`          | `timestamptz`  | NOT NULL.                           |
| `last_seen_at`        | `timestamptz`  | NOT NULL.                           |
| `idle_expires_at`     | `timestamptz`  | NOT NULL.                           |
| `absolute_expires_at` | `timestamptz`  | NOT NULL.                           |
| `revoked_at`          | `timestamptz`  | NULL.                               |
| `revoked_reason`      | `varchar(64)`  | NULL.                               |
| `version`             | `bigint`       | NOT NULL default `0`.               |

Indexes: `(user_id, revoked_at)`, `family_id`. Free giới hạn 2 sessions, Premium 5 sessions ở service.

### `refresh_tokens`

| Cột                    | Kiểu          | Ghi chú                                        |
| ---------------------- | ------------- | ---------------------------------------------- |
| `refresh_token_id`     | `uuid`        | PK.                                            |
| `session_id`           | `uuid`        | NOT NULL FK → `auth_sessions`.                 |
| `family_id`            | `uuid`        | NOT NULL.                                      |
| `token_hash`           | `char(64)`    | NOT NULL, UNIQUE; HMAC với server-held pepper. |
| `issued_at`            | `timestamptz` | NOT NULL.                                      |
| `expires_at`           | `timestamptz` | NOT NULL.                                      |
| `rotated_at`           | `timestamptz` | NULL.                                          |
| `replaced_by_token_id` | `uuid`        | NULL FK → `refresh_tokens`.                    |
| `revoked_at`           | `timestamptz` | NULL.                                          |
| `revoked_reason`       | `varchar(64)` | NULL.                                          |
| `created_at`           | `timestamptz` | NOT NULL.                                      |
| `version`              | `bigint`      | NOT NULL default `0`.                          |

Indexes: `token_hash`, `session_id`, `family_id`. Token cũ được giữ hash đến absolute expiry để phát hiện reuse; không lưu refresh token thô.

### `auth_action_tokens`

Gộp email verification và password reset token vào một bảng MVP.

| Cột                    | Kiểu          | Ghi chú                                           |
| ---------------------- | ------------- | ------------------------------------------------- |
| `auth_action_token_id` | `uuid`        | PK.                                               |
| `user_id`              | `uuid`        | NOT NULL FK → `users`.                            |
| `purpose`              | `varchar(32)` | NOT NULL: `EMAIL_VERIFICATION`, `PASSWORD_RESET`. |
| `token_hash`           | `char(64)`    | NOT NULL, UNIQUE.                                 |
| `issued_at`            | `timestamptz` | NOT NULL.                                         |
| `expires_at`           | `timestamptz` | NOT NULL.                                         |
| `consumed_at`          | `timestamptz` | NULL.                                             |
| `invalidated_at`       | `timestamptz` | NULL.                                             |
| `request_ip_hash`      | `char(64)`    | NULL.                                             |
| `created_at`           | `timestamptz` | NOT NULL.                                         |

Index: `(user_id, purpose, expires_at)`. Worker xóa token đã hết hạn.

### `refresh_idempotency`

Giữ retry response tối đa 30 giây để một network retry với cùng `X-Refresh-Request-Id` không xoay refresh token lần hai.

| Cột                       | Kiểu          | Ghi chú                                                                                       |
| ------------------------- | ------------- | --------------------------------------------------------------------------------------------- |
| `refresh_idempotency_id`  | `uuid`        | PK.                                                                                           |
| `session_id`              | `uuid`        | NOT NULL FK → `auth_sessions`.                                                                |
| `source_refresh_token_id` | `uuid`        | NOT NULL FK → `refresh_tokens`.                                                               |
| `refresh_request_id`      | `uuid`        | NOT NULL.                                                                                     |
| `response_ciphertext`     | `bytea`       | NOT NULL; KMS-encrypted response, ngoại lệ duy nhất được giữ refresh token thô trong 30 giây. |
| `expires_at`              | `timestamptz` | NOT NULL; tối đa 30 giây sau khi tạo.                                                         |
| `created_at`              | `timestamptz` | NOT NULL.                                                                                     |

Ràng buộc/index: UNIQUE `(session_id, refresh_request_id)`; xóa ngay sau expiry. Request ID khác dùng refresh token đã rotate phải revoke family và ghi audit.

### `auth_audit_events`

Append-only log tối thiểu cho auth và role changes.

| Cột                   | Kiểu          | Ghi chú                                                                 |
| --------------------- | ------------- | ----------------------------------------------------------------------- |
| `auth_audit_event_id` | `uuid`        | PK.                                                                     |
| `event_type`          | `varchar(64)` | NOT NULL: `LOGIN`, `REFRESH_REUSE`, `ROLE_GRANTED`, `ROLE_REVOKED`, ... |
| `actor_user_id`       | `uuid`        | NULL FK → `users`.                                                      |
| `target_user_id`      | `uuid`        | NULL FK → `users`.                                                      |
| `session_id`          | `uuid`        | NULL.                                                                   |
| `before_roles`        | `jsonb`       | NULL; bắt buộc khi role change.                                         |
| `after_roles`         | `jsonb`       | NULL; bắt buộc khi role change.                                         |
| `correlation_id`      | `uuid`        | NOT NULL.                                                               |
| `details`             | `jsonb`       | NULL; safe metadata, không token/password.                              |
| `occurred_at`         | `timestamptz` | NOT NULL.                                                               |

Indexes: `(target_user_id, occurred_at DESC)`, `(event_type, occurred_at DESC)`, `correlation_id`.

## 4. Premium và quota AI

### `subscription_plans`

| Cột                    | Kiểu           | Ghi chú                                           |
| ---------------------- | -------------- | ------------------------------------------------- |
| `subscription_plan_id` | `uuid`         | PK.                                               |
| `plan_code`            | `varchar(32)`  | NOT NULL, UNIQUE: `FREE`, `PREMIUM`.              |
| `display_name`         | `varchar(100)` | NOT NULL.                                         |
| `max_active_sessions`  | `smallint`     | NOT NULL; Free=2, Premium=5.                      |
| `ai_quota_units`       | `integer`      | NOT NULL CHECK `>= 0`; quota AI tổng theo chu kỳ. |
| `ai_quota_period`      | `varchar(16)`  | NOT NULL: `DAY`, `MONTH`.                         |
| `status`               | `varchar(16)`  | NOT NULL: `ACTIVE`, `RETIRED`.                    |
| `created_at`           | `timestamptz`  | NOT NULL.                                         |
| `updated_at`           | `timestamptz`  | NOT NULL.                                         |

Chính sách seed MVP: plan `FREE` đang `ACTIVE` có `ai_quota_units = 30` và
`ai_quota_period = 'MONTH'`; một chu kỳ Free là 30 ngày liên tiếp tính từ
`user_entitlements.ai_quota_period_started_at`. `PREMIUM` chỉ là cấu trúc được dành trước, không
được kích hoạt trong MVP.

### `user_entitlements`

Premium là entitlement server-side do lifecycle tự động tạo/cập nhật, không liên quan `ADMIN`. Admin không được cấp, thu hồi hoặc sửa entitlement/quota thủ công.

| Cột                             | Kiểu          | Ghi chú                                                          |
| ------------------------------- | ------------- | ---------------------------------------------------------------- |
| `user_entitlement_id`           | `uuid`        | PK.                                                              |
| `user_id`                       | `uuid`        | NOT NULL FK → `users`.                                           |
| `subscription_plan_id`          | `uuid`        | NOT NULL FK → `subscription_plans`.                              |
| `status`                        | `varchar(24)` | NOT NULL; `entitlement_status`.                                  |
| `source_type`                   | `varchar(32)` | NOT NULL: `DEFAULT`, `BILLING`; nguồn lifecycle tự động, không có admin grant. |
| `external_reference_ciphertext` | `bytea`       | NULL; mã payment/subscription, không card data.                  |
| `starts_at`                     | `timestamptz` | NOT NULL.                                                        |
| `ends_at`                       | `timestamptz` | NULL.                                                            |
| `ai_quota_period_started_at`    | `timestamptz` | NOT NULL.                                                        |
| `ai_used_units`                 | `integer`     | NOT NULL default `0`; CHECK `>= 0`; counter cập nhật atomically trước AI call. |
| `created_at`                    | `timestamptz` | NOT NULL.                                                        |
| `updated_at`                    | `timestamptz` | NOT NULL.                                                        |
| `version`                       | `bigint`      | NOT NULL default `0`.                                            |

Ràng buộc/index: CHECK `ends_at IS NULL OR ends_at > starts_at`; UNIQUE partial `user_id WHERE status = 'ACTIVE'`; UNIQUE `(user_entitlement_id, user_id)`; `(user_id, status, starts_at, ends_at)`. Composite unique phục vụ FK ghép quota. Service phải xác thực `users.status = 'ACTIVE'` trước khi khóa entitlement active, rollover chu kỳ quota và so `ai_used_units + requested_units <= subscription_plans.ai_quota_units` trước mọi AI request. Lock/unlock account không tạo, thu hồi, reset hay sửa entitlement, `ai_quota_period_started_at` hoặc `ai_used_units`; thay đổi entitlement thực sự mới tăng `users.authz_version`.

### `ai_usage_events`

Ledger idempotent tối thiểu cho mọi request tính quota; không chứa prompt, transcript hay audio thô.

| Cột                         | Kiểu          | Ghi chú                                                                  |
| --------------------------- | ------------- | ------------------------------------------------------------------------ |
| `ai_usage_event_id`         | `uuid`        | PK.                                                                      |
| `user_entitlement_id`       | `uuid`        | NOT NULL; FK ghép bảo đảm entitlement thuộc user.                        |
| `user_id`                   | `uuid`        | NOT NULL FK → `users`.                                                   |
| `client_request_id`         | `uuid`        | NOT NULL; idempotency key cho một logical AI request.                    |
| `request_fingerprint_hash`  | `char(64)`    | NOT NULL; HMAC/SHA-256 của feature và owned operation ID, không chứa raw prompt/audio. |
| `feature_type`              | `varchar(32)` | NOT NULL: `AI_BUDDY`, `SHADOWING_ASSESSMENT`.                            |
| `requested_units`           | `smallint`    | NOT NULL default `1`; CHECK `> 0`.                                      |
| `status`                    | `varchar(24)` | NOT NULL; `ai_usage_status`.                                             |
| `ai_service_request_reference` | `varchar(128)` | NULL; opaque reference đã lọc từ `ai-service`, không phải provider/credential. |
| `failure_code`              | `varchar(64)` | NULL; safe internal code.                                                 |
| `created_at`                | `timestamptz` | NOT NULL.                                                                |
| `completed_at`              | `timestamptz` | NULL.                                                                    |

Ràng buộc/index: UNIQUE `(user_id, client_request_id)`; UNIQUE `(ai_usage_event_id, user_id)`; FK ghép `(user_entitlement_id, user_id)` → `user_entitlements(user_entitlement_id, user_id)`; `(user_entitlement_id, status, created_at DESC)`; `(user_id, created_at DESC)`. Trong một transaction khóa entitlement, request mới tạo event `RESERVED` và tăng `ai_used_units`; retry cùng key và `request_fingerprint_hash` chỉ trả event hiện có. Cùng `(user_id, client_request_id)` nhưng fingerprint khác phải trả idempotency conflict trước AI call, không tạo event và không đổi quota. `ai_usage_event_id` được truyền làm correlation ID cho `ai-service`; chỉ `ai_service_request_reference` đã lọc có thể được ghi lại sau phản hồi. Với F08/F11 MVP, result schema-validated thành công chuyển event sang `SUCCEEDED`; timeout, unavailable service, invalid output hoặc safety rejection không có result hợp lệ phải chuyển sang `FAILED_REFUNDED` và giảm counter đúng một lần trong cùng transaction. `FAILED_CONSUMED` được giữ cho chính sách tương lai nhưng không có transition trong MVP. Chỉ backend service được phép đổi trạng thái event.

## 5. Catalog nội dung

Mọi thay đổi hoặc state transition trên `topics`, `lessons`, `segments` và `media_assets` phải dùng
`version` hiện tại. Version không khớp bị từ chối không thay đổi row; API Admin trả trạng thái reload
recoverable, không áp dụng last-write-wins hoặc tự gộp dữ liệu.

`ARCHIVED` là trạng thái terminal cho `topics`, `lessons` và `segments`: service không cho phép
transition tiếp theo hoặc republish item đã archive. Row được giữ để bảo toàn FK và historical learner
work; nội dung thay thế phải là row mới.

### `topics`

| Cột                  | Kiểu           | Ghi chú                        |
| -------------------- | -------------- | ------------------------------ |
| `topic_id`           | `uuid`         | PK.                            |
| `slug`               | `varchar(160)` | NOT NULL, UNIQUE.              |
| `title`              | `varchar(255)` | NOT NULL.                      |
| `description`        | `text`         | NULL.                          |
| `hsk_level`          | `smallint`     | NULL; CHECK 1–6.               |
| `sort_order`         | `integer`      | NOT NULL default `0`.          |
| `publication_state`  | `varchar(16)`  | NOT NULL.                      |
| `created_by_user_id` | `uuid`         | NOT NULL FK → `users` (Admin). |
| `updated_by_user_id` | `uuid`         | NOT NULL FK → `users` (Admin). |
| `published_at`       | `timestamptz`  | NULL.                          |
| `created_at`         | `timestamptz`  | NOT NULL.                      |
| `updated_at`         | `timestamptz`  | NOT NULL.                      |
| `version`            | `bigint`       | NOT NULL default `0`.          |

Topic có thể chuyển sang `PUBLISHED` để chuẩn bị nội dung, nhưng API catalog không được trả topic đó
nếu không có lesson publishable theo ràng buộc bên dưới.
Catalog mặc định sắp topic theo `(sort_order, title)` và lesson theo `(topic_id, sort_order, title)`.
Search catalog chỉ khớp title topic/lesson và filter HSK 1–6 sau khi đã áp dụng visibility/access
check; không search transcript, playback hay practice data.
Catalog projection chỉ trả title, short summary, HSK, estimated duration khi áp dụng, và số lesson
hoặc segment `PUBLISHED` hiện có. Projection không trả transcript, media URL, provider identifier,
playlist, hay practice data.
Topic hoặc lesson `hsk_level IS NULL` chỉ có trong catalog không filter; mọi filter HSK 1–6 phải
loại item đó sau visibility/access check.

### `lessons`

| Cột                          | Kiểu           | Ghi chú                              |
| ---------------------------- | -------------- | ------------------------------------ |
| `lesson_id`                  | `uuid`         | PK.                                  |
| `topic_id`                   | `uuid`         | NOT NULL FK → `topics`.              |
| `slug`                       | `varchar(160)` | NOT NULL, UNIQUE.                    |
| `title`                      | `varchar(255)` | NOT NULL.                            |
| `summary`                    | `text`         | NULL.                                |
| `hsk_level`                  | `smallint`     | NULL; CHECK 1–6.                     |
| `lesson_type`                | `varchar(24)`  | NOT NULL: `AUDIO`, `VIDEO`, `MIXED`. |
| `access_level`               | `varchar(16)`  | NOT NULL: `FREE`, `PREMIUM`.         |
| `publication_state`          | `varchar(16)`  | NOT NULL.                            |
| `sort_order`                 | `integer`      | NOT NULL default `0`.                |
| `estimated_duration_seconds` | `integer`      | NOT NULL default `0`.                |
| `completion_min_percent`     | `smallint`     | NOT NULL fixed `100` trong MVP; Admin không được sửa. |
| `created_by_user_id`         | `uuid`         | NOT NULL FK → `users`.               |
| `updated_by_user_id`         | `uuid`         | NOT NULL FK → `users`.               |
| `published_at`               | `timestamptz`  | NULL.                                |
| `created_at`                 | `timestamptz`  | NOT NULL.                            |
| `updated_at`                 | `timestamptz`  | NOT NULL.                            |
| `version`                    | `bigint`       | NOT NULL default `0`.                |

Indexes: `(topic_id, sort_order)`, `(publication_state, access_level, hsk_level)`. Lesson chỉ được
chuyển sang `PUBLISHED` khi topic cha là `PUBLISHED`, có ít nhất một segment, tất cả segment của
lesson là `PUBLISHED` và mọi media asset được tham chiếu là `APPROVED`. API catalog chỉ trả topic có
ít nhất một lesson thỏa các điều kiện này. Trước khi sửa learner-visible lesson field, segment,
sequence hoặc media relationship của lesson `PUBLISHED`, service phải chuyển lesson sang
`UNPUBLISHED`; chỉ được publish lại sau khi kiểm tra đầy đủ các điều kiện trên.

### `media_assets`

| Cột                         | Kiểu           | Ghi chú                                                          |
| --------------------------- | -------------- | ---------------------------------------------------------------- |
| `media_asset_id`            | `uuid`         | PK.                                                              |
| `provider_name`             | `varchar(64)`  | NOT NULL; allowlisted provider.                                  |
| `provider_asset_identifier` | `varchar(255)` | NOT NULL; provider ID, không phải URL client gửi.                |
| `media_kind`                | `varchar(16)`  | NOT NULL: `AUDIO`, `VIDEO`, `IMAGE`.                             |
| `mime_type`                 | `varchar(127)` | NULL.                                                            |
| `duration_milliseconds`     | `integer`      | NULL CHECK `>= 0`.                                               |
| `approval_status`           | `varchar(24)`  | NOT NULL: `PENDING_SCAN`, `APPROVED`, `REJECTED`, `QUARANTINED`. |
| `malware_scan_status`       | `varchar(24)`  | NOT NULL.                                                        |
| `approved_by_user_id`       | `uuid`         | NULL FK → `users`.                                               |
| `approved_at`               | `timestamptz`  | NULL.                                                            |
| `created_by_user_id`        | `uuid`         | NOT NULL FK → `users`.                                           |
| `created_at`                | `timestamptz`  | NOT NULL.                                                        |
| `updated_at`                | `timestamptz`  | NOT NULL.                                                        |
| `version`                   | `bigint`       | NOT NULL default `0`.                                            |

Ràng buộc: UNIQUE `(provider_name, provider_asset_identifier)`. Asset chưa `APPROVED` không bao giờ
phát được. Khi asset `APPROVED` được tham chiếu bởi một hoặc nhiều lesson `PUBLISHED` chuyển sang
`REJECTED` hoặc `QUARANTINED`, Spring Boot phải transition mọi lesson phụ thuộc sang `UNPUBLISHED`
trước lần learner access tiếp theo; chỉ media replacement `APPROVED` và republish hợp lệ mới khôi
phục learner visibility.

### `segments`

| Cột                  | Kiểu          | Ghi chú                                                      |
| -------------------- | ------------- | ------------------------------------------------------------ |
| `segment_id`         | `uuid`        | PK.                                                          |
| `lesson_id`          | `uuid`        | NOT NULL FK → `lessons`.                                     |
| `media_asset_id`     | `uuid`        | NOT NULL FK → `media_assets`.                                |
| `sequence_no`        | `integer`     | NOT NULL CHECK `> 0`; UNIQUE trong lesson.                   |
| `segment_type`       | `varchar(24)` | NOT NULL: `DICTATION`, `SHADOWING`, `BOTH`, `PLAYBACK_ONLY`. |
| `publication_state`  | `varchar(16)` | NOT NULL.                                                    |
| `start_milliseconds` | `integer`     | NOT NULL CHECK `>= 0`.                                       |
| `end_milliseconds`   | `integer`     | NOT NULL CHECK `> start_milliseconds`.                       |
| `transcript_hanzi`   | `text`        | NOT NULL; answer key server-side.                            |
| `transcript_pinyin`  | `text`        | NULL.                                                        |
| `translation_vi`     | `text`        | NULL.                                                        |
| `dictation_hint`     | `text`        | NULL.                                                        |
| `created_by_user_id` | `uuid`        | NOT NULL FK → `users`.                                       |
| `updated_by_user_id` | `uuid`        | NOT NULL FK → `users`.                                       |
| `published_at`       | `timestamptz` | NULL.                                                        |
| `created_at`         | `timestamptz` | NOT NULL.                                                    |
| `updated_at`         | `timestamptz` | NOT NULL.                                                    |
| `version`            | `bigint`      | NOT NULL default `0`.                                        |

Ràng buộc/index: UNIQUE `(lesson_id, sequence_no)`, UNIQUE `(segment_id, lesson_id)`; `(lesson_id, publication_state, sequence_no)`. Composite unique thứ hai phục vụ FK ghép bảo đảm segment thuộc lesson. `transcript_hanzi` là expected simplified-Hanzi answer của F07; Dictation compare sau Unicode/whitespace/punctuation normalization nhưng giữ character identity và order, không coi pinyin hoặc traditional Hanzi là tương đương. API learner phải kiểm tra topic/lesson/segment đều `PUBLISHED`, media approved và entitlement nếu Premium.

## 6. Luyện tập và từ vựng

### `dictation_attempts`

| Cột                    | Kiểu           | Ghi chú                                    |
| ---------------------- | -------------- | ------------------------------------------ |
| `dictation_attempt_id` | `uuid`         | PK.                                        |
| `user_id`              | `uuid`         | NOT NULL FK → `users`.                     |
| `lesson_id`            | `uuid`         | NOT NULL FK → `lessons`.                   |
| `segment_id`           | `uuid`         | NOT NULL FK → `segments`.                  |
| `status`               | `varchar(24)`  | NOT NULL; `attempt_status`.                |
| `client_submission_id` | `uuid`         | NULL; idempotency key cho một logical submit, chỉ có sau khi submit. |
| `answer_ciphertext`    | `bytea`        | NULL; learner answer.                      |
| `overall_score`        | `numeric(5,2)` | NULL CHECK 0–100; server calculated.       |
| `accuracy_percent`     | `numeric(5,2)` | NULL CHECK 0–100.                          |
| `feedback_ciphertext`  | `bytea`        | NULL; guidance chung để retry, không có per-character analysis hoặc full expected answer. |
| `started_at`           | `timestamptz`  | NOT NULL.                                  |
| `submitted_at`         | `timestamptz`  | NULL.                                      |
| `evaluated_at`         | `timestamptz`  | NULL.                                      |
| `deleted_at`           | `timestamptz`  | NULL.                                      |
| `created_at`           | `timestamptz`  | NOT NULL.                                  |
| `updated_at`           | `timestamptz`  | NOT NULL.                                  |
| `version`              | `bigint`       | NOT NULL default `0`.                      |

Indexes/ràng buộc: `(user_id, segment_id, created_at DESC)`, `(user_id, lesson_id, status)`, UNIQUE partial `(user_id, client_submission_id) WHERE client_submission_id IS NOT NULL`; FK ghép `(segment_id, lesson_id)` → `segments(segment_id, lesson_id)` bảo đảm segment thuộc lesson. Trước khi tạo hay trả Dictation attempt, Spring Boot xác thực F06 context của user: segment phải là current unlocked hoặc đã completed; future locked segment bị từ chối và không có row mới. Mỗi explicit retake tạo row attempt mới sau attempt trước đã evaluated; retry cùng `client_submission_id` chỉ trả row/result cũ và không reevaluate. F07 đặt `overall_score = accuracy_percent` bằng 100 khi answer khớp chính xác expected answer sau normalization, ngược lại bằng 0; không dùng character edit distance. Approved result chỉ cập nhật `dictation_best_score` khi cao hơn score hiện tại, không xóa history. F07 là deterministic tại Spring Boot, không có FK hoặc quota event AI.

### `recordings`

Object raw audio nằm trong object storage, không có public URL trong database.

| Cột                       | Kiểu           | Ghi chú                                                                   |
| ------------------------- | -------------- | ------------------------------------------------------------------------- |
| `recording_id`            | `uuid`         | PK.                                                                       |
| `user_id`                 | `uuid`         | NOT NULL FK → `users`.                                                    |
| `classification`          | `varchar(32)`  | NOT NULL default `ASSESSMENT_ONLY`.                                       |
| `status`                  | `varchar(24)`  | NOT NULL: `UPLOADING`, `SCANNING`, `AVAILABLE`, `QUARANTINED`, `DELETED`. |
| `storage_provider`        | `varchar(64)`  | NOT NULL.                                                                 |
| `object_key_ciphertext`   | `bytea`        | NULL; encrypted object key, chỉ Spring Boot/object-storage boundary đọc.   |
| `mime_type`               | `varchar(127)` | NOT NULL.                                                                 |
| `byte_size`               | `bigint`       | NOT NULL CHECK `> 0`.                                                     |
| `duration_milliseconds`   | `integer`      | NULL CHECK `>= 0`.                                                        |
| `checksum_sha256`         | `char(64)`     | NOT NULL.                                                                 |
| `malware_scan_status`     | `varchar(24)`  | NOT NULL.                                                                 |
| `assessment_succeeded_at` | `timestamptz`  | NULL.                                                                     |
| `expires_at`              | `timestamptz`  | NOT NULL; 30 ngày success/24h failed với ASSESSMENT_ONLY.                 |
| `deleted_at`              | `timestamptz`  | NULL.                                                                     |
| `created_at`              | `timestamptz`  | NOT NULL.                                                                 |
| `updated_at`              | `timestamptz`  | NOT NULL.                                                                 |
| `version`                 | `bigint`       | NOT NULL default `0`.                                                     |

Ràng buộc/index: UNIQUE `(recording_id, user_id)`; `(user_id, status, created_at DESC)`, `(classification, expires_at)`. Composite unique phục vụ FK ghép của Shadowing. Chỉ Spring Boot được đọc object key và stream recording đã kiểm tra trực tiếp đến private F08 `ai-service` contract; `ai-service` không có storage credential, object key hoặc signed URL. Saved recording chỉ được chọn explicit và có consent khi feature consent được thêm.

### `shadowing_attempts`

| Cột                     | Kiểu           | Ghi chú                             |
| ----------------------- | -------------- | ----------------------------------- |
| `shadowing_attempt_id`  | `uuid`         | PK.                                 |
| `user_id`               | `uuid`         | NOT NULL FK → `users`.              |
| `lesson_id`             | `uuid`         | NOT NULL FK → `lessons`.            |
| `segment_id`            | `uuid`         | NOT NULL FK → `segments`.           |
| `recording_id`          | `uuid`         | NOT NULL, UNIQUE FK → `recordings`. |
| `status`                | `varchar(24)`  | NOT NULL; `attempt_status`.         |
| `overall_score`         | `numeric(5,2)` | NULL CHECK 0–100.                   |
| `pronunciation_score`   | `numeric(5,2)` | NULL CHECK 0–100.                   |
| `tone_score`            | `numeric(5,2)` | NULL CHECK 0–100.                   |
| `rhythm_score`          | `numeric(5,2)` | NULL CHECK 0–100.                   |
| `feedback_ciphertext`   | `bytea`        | NULL; IPA/rhythm guidance.          |
| `ai_usage_event_id`     | `uuid`         | NULL FK → `ai_usage_events`.         |
| `submitted_at`          | `timestamptz`  | NULL.                               |
| `evaluated_at`          | `timestamptz`  | NULL.                               |
| `deleted_at`            | `timestamptz`  | NULL.                               |
| `created_at`            | `timestamptz`  | NOT NULL.                           |
| `updated_at`            | `timestamptz`  | NOT NULL.                           |
| `version`               | `bigint`       | NOT NULL default `0`.               |

Indexes/ràng buộc: `(user_id, segment_id, created_at DESC)`, `recording_id`, `ai_usage_event_id`; UNIQUE partial `ai_usage_event_id WHERE ai_usage_event_id IS NOT NULL`; FK ghép `(segment_id, lesson_id)` → `segments(segment_id, lesson_id)`, `(recording_id, user_id)` → `recordings(recording_id, user_id)` và `(ai_usage_event_id, user_id)` → `ai_usage_events(ai_usage_event_id, user_id)`. Trước khi tạo recording hay Shadowing attempt, Spring Boot xác thực F06 context của user: segment phải là current unlocked hoặc đã completed; future locked segment bị từ chối và không có row hoặc AI usage event mới. Sau ownership/status/scan validation, Spring Boot stream audio trực tiếp qua private F08 `ai-service` contract. Spring Boot chỉ ghi score/feedback sau khi xác thực kết quả schema-validated và xác thực `feature_type = 'SHADOWING_ASSESSMENT'`; transcript speech-engine bị loại bỏ và không được persist hoặc trả về trong MVP.

### `dictionary_entries`

Một row shared đủ cho MVP; senses/examples gom trong `jsonb`, chỉ tách bảng sau khi cần editor/search phức tạp.

| Cột                    | Kiểu           | Ghi chú                                                                  |
| ---------------------- | -------------- | ------------------------------------------------------------------------ |
| `dictionary_entry_id`  | `uuid`         | PK.                                                                      |
| `simplified_hanzi`     | `varchar(128)` | NOT NULL.                                                                |
| `traditional_hanzi`    | `varchar(128)` | NULL.                                                                    |
| `normalized_hanzi`     | `varchar(128)` | NOT NULL.                                                                |
| `primary_pinyin`       | `varchar(255)` | NOT NULL.                                                                |
| `normalized_pinyin`    | `varchar(255)` | NOT NULL.                                                                |
| `hsk_level`            | `smallint`     | NULL CHECK 1–6.                                                          |
| `word_type`            | `varchar(32)`  | NULL.                                                                    |
| `senses`               | `jsonb`        | NOT NULL; mảng `{partOfSpeech, definitionVi, examples[]}` được validate. |
| `audio_media_asset_id` | `uuid`         | NULL FK → `media_assets`.                                                |
| `image_media_asset_id` | `uuid`         | NULL FK → `media_assets`.                                                |
| `publication_state`    | `varchar(16)`  | NOT NULL default `PUBLISHED`.                                            |
| `created_at`           | `timestamptz`  | NOT NULL.                                                                |
| `updated_at`           | `timestamptz`  | NOT NULL.                                                                |
| `version`              | `bigint`       | NOT NULL default `0`.                                                    |

Ràng buộc/index: UNIQUE `(normalized_hanzi, normalized_pinyin)`; trigram/full-text index cho `simplified_hanzi`, `traditional_hanzi`, `normalized_pinyin` và `definitionVi` trong `senses`. F09 search chỉ trả entry `PUBLISHED` và khớp Hanzi giản thể/phồn thể, normalized pinyin bỏ dấu thanh/case/khoảng trắng, hoặc từ khóa nghĩa tiếng Việt. F09 public detail trả Hanzi, pinyin, `senses`/examples và chỉ trả `audio_media_asset_id` hoặc `image_media_asset_id` khi asset liên quan được F04 publish và available; asset không khả dụng bị omit.

### `saved_words`

| Cột                        | Kiểu          | Ghi chú                                                     |
| -------------------------- | ------------- | ----------------------------------------------------------- |
| `saved_word_id`            | `uuid`        | PK.                                                         |
| `user_id`                  | `uuid`        | NOT NULL FK → `users`.                                      |
| `dictionary_entry_id`      | `uuid`        | NOT NULL FK → `dictionary_entries`.                         |
| `personal_note_ciphertext` | `bytea`       | NULL; optional plain text tối đa 500 ký tự, encrypted.      |
| `status`                   | `varchar(16)` | NOT NULL default `ACTIVE`: `ACTIVE`, `ARCHIVED`, `DELETED`. |
| `saved_at`                 | `timestamptz` | NOT NULL.                                                   |
| `deleted_at`               | `timestamptz` | NULL.                                                       |
| `created_at`               | `timestamptz` | NOT NULL.                                                   |
| `updated_at`               | `timestamptz` | NOT NULL.                                                   |
| `version`                  | `bigint`      | NOT NULL default `0`.                                       |

Ràng buộc: UNIQUE partial `(user_id, dictionary_entry_id) WHERE deleted_at IS NULL`; UNIQUE `(saved_word_id, user_id)` để phục vụ FK ghép SRS. Service validate personal note là plain text tối đa 500 ký tự, không format/attachment, trước khi encrypt; input không hợp lệ không được thay row. POST save trả row `ACTIVE` đã có thay vì thêm duplicate; nếu chỉ có row removed của cùng owner/entry, service khôi phục chính `saved_word_id` đó về `ACTIVE`, xoá `deleted_at` và giữ `personal_note_ciphertext`, không tạo row mới hoặc reset F10 SRS schedule. F10 service resume schedule hiện có. Nếu dictionary entry chuyển khỏi `PUBLISHED`, saved-word row và personal note vẫn giữ nhưng F09 vocabulary projection chỉ trả trạng thái unavailable, không trả dictionary detail đã withdrawn.

### `srs_schedules`

| Cột                | Kiểu           | Ghi chú                              |
| ------------------ | -------------- | ------------------------------------ |
| `srs_schedule_id`  | `uuid`         | PK.                                  |
| `saved_word_id`    | `uuid`         | NOT NULL, UNIQUE FK → `saved_words`. |
| `user_id`          | `uuid`         | NOT NULL FK → `users`.               |
| `status`           | `varchar(16)`  | NOT NULL; `srs_status`.              |
| `due_at`           | `timestamptz`  | NOT NULL.                            |
| `interval_days`    | `numeric(8,3)` | NOT NULL default `0`.                |
| `ease_factor`      | `numeric(5,3)` | NOT NULL default `2.500`; CHECK 1.300–2.500. |
| `repetitions`      | `integer`      | NOT NULL default `0`.                |
| `lapses`           | `integer`      | NOT NULL default `0`.                |
| `last_reviewed_at` | `timestamptz`  | NULL.                                |
| `created_at`       | `timestamptz`  | NOT NULL.                            |
| `updated_at`       | `timestamptz`  | NOT NULL.                            |
| `version`          | `bigint`       | NOT NULL default `0`.                |

Ràng buộc/index: UNIQUE `(srs_schedule_id, user_id)`; `(user_id, status, due_at)`; FK ghép `(saved_word_id, user_id)` → `saved_words(saved_word_id, user_id)`. Due queue chỉ chọn schedule owned có `status` là `LEARNING`, `REVIEW` hoặc `RELEARNING` và `due_at` không sau server time, sắp `due_at` tăng dần, tối đa 20 row; `SUSPENDED` không được vào queue. Khi F09 tạo saved word lần đầu, F10 tạo hoặc reuse đúng một schedule `LEARNING` với `due_at` ngay thời điểm save thành công; repeated save không tạo schedule thứ hai. Khi saved word bị remove, F10 đặt schedule hiện có thành `SUSPENDED` nhưng giữ due, interval, ease và review history; restore chuyển chính schedule đó về `LEARNING` nếu `repetitions = 0`, ngược lại `REVIEW`, và giữ nguyên `due_at` nên schedule quá hạn due ngay. Initial rating đặt due: `AGAIN` 10 phút, `HARD` 1 ngày, `GOOD` 3 ngày, `EASY` 7 ngày. Later review: `AGAIN` 10 phút/ease −0.2, `HARD` interval ×1.2/ease −0.15, `GOOD` interval × prior ease, `EASY` interval × (prior ease +0.15); ease luôn clamp 1.3–2.5. Backend là nơi duy nhất tính `due_at`, interval và ease.

### `srs_review_events`

| Cột                      | Kiểu           | Ghi chú                        |
| ------------------------ | -------------- | ------------------------------ |
| `srs_review_event_id`    | `uuid`         | PK.                            |
| `srs_schedule_id`        | `uuid`         | NOT NULL FK → `srs_schedules`. |
| `user_id`                | `uuid`         | NOT NULL FK → `users`.         |
| `client_review_id`       | `uuid`         | NOT NULL; idempotency key cho một logical review. |
| `rating`                 | `varchar(16)`  | NOT NULL; `review_rating`.     |
| `previous_schedule_version` | `bigint`    | NOT NULL.                      |
| `next_schedule_version`  | `bigint`       | NOT NULL.                      |
| `previous_due_at`        | `timestamptz`  | NOT NULL.                      |
| `next_due_at`            | `timestamptz`  | NOT NULL.                      |
| `previous_interval_days` | `numeric(8,3)` | NOT NULL.                      |
| `next_interval_days`     | `numeric(8,3)` | NOT NULL.                      |
| `reviewed_at`            | `timestamptz`  | NOT NULL.                      |
| `created_at`             | `timestamptz`  | NOT NULL.                      |

Indexes/ràng buộc: `(srs_schedule_id, reviewed_at DESC)`, `(user_id, reviewed_at DESC)`, UNIQUE `(user_id, client_review_id)`; FK ghép `(srs_schedule_id, user_id)` → `srs_schedules(srs_schedule_id, user_id)`. Trong một transaction, F10 chỉ update schedule khi `previous_schedule_version` khớp version hiện tại; retry cùng `client_review_id` trả event/result đã có. Request khác dựa trên version cũ bị reject, không tạo event và trả latest schedule. Đây là lịch sử immutable, không tính SRS ở React.

## 7. Tiến độ và AI Buddy

### `lesson_progresses`

| Cột                       | Kiểu           | Ghi chú                                              |
| ------------------------- | -------------- | ---------------------------------------------------- |
| `lesson_progress_id`      | `uuid`         | PK.                                                  |
| `user_id`                 | `uuid`         | NOT NULL FK → `users`.                               |
| `lesson_id`               | `uuid`         | NOT NULL FK → `lessons`.                             |
| `status`                  | `varchar(16)`  | NOT NULL: `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`. |
| `current_segment_id`      | `uuid`         | NULL FK → `segments`.                                |
| `current_segment_playback_ms` | `integer`  | NOT NULL default `0`; playback position tương đối trong current segment, CHECK `>= 0`. |
| `current_segment_max_played_ms` | `integer` | NOT NULL default `0`; server-validated watermark, CHECK `>= 0`. |
| `playback_updated_at`     | `timestamptz`  | NULL; server time của playback update được chấp nhận gần nhất. |
| `completed_segment_count` | `integer`      | NOT NULL default `0`.                                |
| `total_segment_count`     | `integer`      | NOT NULL default `0`.                                |
| `completion_percent`      | `numeric(5,2)` | NOT NULL default `0`; CHECK 0–100.                   |
| `dictation_best_score`    | `numeric(5,2)` | NULL CHECK 0–100.                                    |
| `shadowing_best_score`    | `numeric(5,2)` | NULL CHECK 0–100.                                    |
| `practice_seconds`        | `integer`      | NOT NULL default `0`.                                |
| `started_at`              | `timestamptz`  | NULL.                                                |
| `completed_at`            | `timestamptz`  | NULL.                                                |
| `last_activity_at`        | `timestamptz`  | NOT NULL.                                            |
| `created_at`              | `timestamptz`  | NOT NULL.                                            |
| `updated_at`              | `timestamptz`  | NOT NULL.                                            |
| `version`                 | `bigint`       | NOT NULL default `0`.                                |

Ràng buộc/index: UNIQUE `(user_id, lesson_id)`; `(user_id, status, last_activity_at DESC)`; FK ghép `(current_segment_id, lesson_id)` → `segments(segment_id, lesson_id)` khi `current_segment_id` khác NULL. Progress service lazy-create hoặc reuse duy nhất một row khi learner lần đầu vào permitted Lesson Player: `status = 'NOT_STARTED'`, `completed_segment_count = 0`, `completion_percent = 0` và `current_segment_id` là segment `PUBLISHED` có `sequence_no` thấp nhất. Catalog/lesson-summary view không tạo row. Khi Player đến playback end của `current_segment_id`, service chỉ chấp nhận completion của current segment trong transaction/version hiện tại, tăng `completed_segment_count` đúng một lần rồi đặt current sang segment `PUBLISHED` kế tiếp theo `sequence_no`; MVP không có learner confirmation action riêng. Nếu concurrent request cho cùng current segment đến sau khi transition đã commit, service không đổi row và trả latest authoritative state; không được tăng count hoặc mở thêm segment. Khi segment cuối được complete, service đặt `status = 'COMPLETED'`, `completion_percent = 100` và `current_segment_id = NULL`; không có partial completion trong MVP. Count luôn là số segment đầu tiên liên tiếp đã completed; learner có thể revisit segment đã completed nhưng không được mở/complete segment về sau. Approved Dictation/Shadowing chỉ được cập nhật practice summary/best-score fields; không được sửa `current_segment_id`, `completed_segment_count`, `completion_percent` hoặc `status`. Tất cả module gọi Progress service để cập nhật, không tự cộng dashboard ở frontend.

Playback watermark: service chỉ chấp nhận vị trí playback được server-validate và không cho Player
seek forward vượt `current_segment_max_played_ms`; Player chỉ được rewind trong phần đã phát. Khi
watermark đạt duration của current segment, automatic completion mới được phép; khi mở segment kế
tiếp, service reset `current_segment_playback_ms` và `current_segment_max_played_ms` về `0`. Client
position không tự tạo authority cho completion hoặc unlock.

### `ai_conversations`

| Cột                  | Kiểu          | Ghi chú                                         |
| -------------------- | ------------- | ----------------------------------------------- |
| `ai_conversation_id` | `uuid`        | PK.                                             |
| `user_id`            | `uuid`        | NOT NULL FK → `users`.                          |
| `title_ciphertext`   | `bytea`       | NULL; learner có thể rename.                    |
| `scenario_code`      | `varchar(64)` | NOT NULL: `DAILY_CONVERSATION`, `VOCABULARY_GRAMMAR`, `ROLE_PLAY`. |
| `status`             | `varchar(16)` | NOT NULL default `ACTIVE`: `ACTIVE`, `DELETED`. |
| `last_message_at`    | `timestamptz` | NULL.                                           |
| `deleted_at`         | `timestamptz` | NULL.                                           |
| `created_at`         | `timestamptz` | NOT NULL.                                       |
| `updated_at`         | `timestamptz` | NOT NULL.                                       |
| `version`            | `bigint`      | NOT NULL default `0`.                           |

Ràng buộc/index: UNIQUE `(ai_conversation_id, user_id)`; `(user_id, status, last_message_at DESC)`. Composite unique phục vụ FK ghép từ message.

### `ai_messages`

| Cột                  | Kiểu           | Ghi chú                                                       |
| -------------------- | -------------- | ------------------------------------------------------------- |
| `ai_message_id`      | `uuid`         | PK.                                                           |
| `ai_conversation_id` | `uuid`         | NOT NULL FK → `ai_conversations`.                             |
| `user_id`            | `uuid`         | NOT NULL FK → `users`; FK ghép bảo đảm consistency với conversation. |
| `sequence_no`        | `integer`      | NOT NULL CHECK `> 0`.                                         |
| `sender_type`        | `varchar(16)`  | NOT NULL: `LEARNER`, `ASSISTANT`.                             |
| `content_ciphertext` | `bytea`        | NOT NULL; learner plain text hoặc assistant bilingual learning-reply schema encrypted. |
| `status`             | `varchar(16)`  | NOT NULL: `PENDING`, `COMPLETE`, `FAILED`, `DELETED`.         |
| `ai_usage_event_id`  | `uuid`         | NULL FK → `ai_usage_events`; chỉ assistant reply.             |
| `created_at`         | `timestamptz`  | NOT NULL.                                                     |
| `completed_at`       | `timestamptz`  | NULL.                                                         |
| `deleted_at`         | `timestamptz`  | NULL.                                                         |

Ràng buộc/index: UNIQUE `(ai_conversation_id, sequence_no)`; UNIQUE partial `ai_usage_event_id WHERE ai_usage_event_id IS NOT NULL`; `(user_id, created_at DESC)`, `ai_usage_event_id`; FK ghép `(ai_conversation_id, user_id)` → `ai_conversations(ai_conversation_id, user_id)` và `(ai_usage_event_id, user_id)` → `ai_usage_events(ai_usage_event_id, user_id)`. CHECK: `ai_usage_event_id` chỉ non-NULL cho `sender_type = 'ASSISTANT'`. Learner message phải plain text tối đa 1,000 ký tự, không rich-text/attachment; input không hợp lệ không tạo `ai_messages` hay `ai_usage_events`. Backend check quota, ownership, rate limit, scenario scope và input safety trước private `ai-service` call; message ngoài scenario hoặc unsafe không tạo `ai_messages` hay `ai_usage_events`. Với request eligible, backend chỉ gửi scenario, current validated message và tối đa 10 message `COMPLETE` gần nhất của chính conversation; không gửi cross-conversation, `PENDING`, `FAILED` hoặc `DELETED` message. Assistant result chỉ persist sau schema validation: Chinese response, concise Vietnamese explanation, tối đa một next-practice suggestion; invalid result theo `FAILED_REFUNDED` F03 policy. Chỉ assistant reply gắn event có `feature_type = 'AI_BUDDY'`. Một event chỉ tạo tối đa một assistant reply.

## 8. Để Phase 2, không tạo bảng ngay

- Privacy/compliance đầy đủ: consent history, account deletion request, export, retention notice, deletion jobs, legal hold và encryption-key ledger.
- Chi tiết assessment: Dictation error tokens, Shadowing feedback từng IPA/rhythm item, handwriting/stroke history.
- Product expansion: recommendation, weak areas, daily stats, leaderboard, teacher sharing và analytics riêng.
- Production hardening của auth: audit/retention tách partition nếu traffic bắt đầu cần retry cạnh tranh cao.
- Chuẩn hóa dictionary khi editor/search cần phức tạp: `dictionary_senses`, examples, pronunciations, Hanzi stroke path.

## 9. Thứ tự migration MVP

1. Auth: `users` → `user_roles` → auth token/session/audit.
2. Plan: `subscription_plans` → `user_entitlements` → `ai_usage_events`.
3. Content: `topics` → `media_assets` → `lessons` → `segments`.
4. Learning: dictionary/SRS, attempts/recording/shadowing, rồi progress.
5. AI Buddy: conversations/messages sau khi quota service và ownership tests đã có.

Migration phải kèm index/unique/FK nêu trên, validation DTO, entitlement/ownership/`ADMIN` test và không sửa migration đã chạy shared/production.
