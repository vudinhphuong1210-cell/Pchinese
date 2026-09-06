# DATA.md — Thiết kế cơ sở dữ liệu Pchinese

> **Trạng thái:** Kiến trúc đích/dự kiến; không phải nguồn tạo Flyway migration ở giai đoạn MVP.  
> **Quy tắc ưu tiên:** `DATA_short.md` là schema contract gốc (canonical) cho MVP. Mọi migration hiện tại phải theo file đó; chỉ đưa một phần của tài liệu này vào migration khi feature tương ứng đã được phê duyệt, có migration plan/ADR và `DATA_short.md` hoặc successor của nó được cập nhật mapping rõ ràng.  
> Phiên bản thiết kế đích: 1.0 · 2026-09-03  
> Nguồn yêu cầu: `AGENT.md`, `CLAUDE.md`, `CONSTITUTION.md`  
> Mục tiêu: PostgreSQL 18 cho modular monolith Spring Boot/JPA; mô tả hướng phát triển production-compliance và không cho phép truy cập dữ liệu bằng raw SQL trong application code.

## 1. Nguyên tắc thiết kế

- Tên bảng số nhiều và `snake_case`; tên cột `snake_case`. Khóa chính dùng UUID (`uuid`) và được gọi rõ là `<entity>_id`.
- Thời điểm dùng `timestamptz` và lưu UTC. Ngày học/SRS dùng `date`; giao diện tự hiển thị theo `user_profiles.time_zone`.
- Mọi FK đều phải có index. Các dữ liệu riêng tư luôn truy vấn kèm `user_id` và service phải kiểm tra ownership; `ADMIN` **không** bỏ qua ownership này.
- Dữ liệu cá nhân/nội dung học riêng tư được mã hóa cấp ứng dụng trước khi ghi DB. Cột hậu tố `_ciphertext` là `bytea`; cột `_lookup_hash` là HMAC-SHA-256 dạng `char(64)`, không phải dữ liệu gốc.
- Không lưu mật khẩu, JWT, refresh token, signed URL, provider API key hay URL phát media thô. Chỉ lưu hash, mã định danh đã duyệt, hoặc dữ liệu đã mã hóa khi thật sự cần.
- Các giá trị trạng thái bên dưới nên map bằng Java enum + `varchar` có `CHECK` trong migration. Không để client tự đặt các cột role, trạng thái xuất bản, entitlement, quota, hay điểm.
- Bảng có `version bigint not null default 0` là optimistic-lock version của JPA. Các bảng quota/refresh token có nghiệp vụ cạnh tranh dùng transaction khóa dòng (`PESSIMISTIC_WRITE`) ở repository.
- `jsonb` chỉ dùng cho metadata có cấu trúc thay đổi (payload provider đã được lọc, quy tắc hoàn thành, thuộc tính event); trường cần lọc/join/index luôn có cột riêng.

## 2. Bộ giá trị kiểm soát (logical enums)

| Nhóm | Giá trị hợp lệ |
| --- | --- |
| `user_status` | `PENDING_VERIFICATION`, `ACTIVE`, `LOCKED`, `DISABLED`, `PENDING_DELETION`, `DELETED` |
| `role_code` | Hiện tại chỉ có `ADMIN`; thêm role cần Constitution/spec được phê duyệt. |
| `publication_state` | `DRAFT`, `PUBLISHED`, `UNPUBLISHED`, `ARCHIVED` |
| `lesson_access_level` | `FREE`, `PREMIUM` |
| `media_kind` | `AUDIO`, `VIDEO`, `IMAGE`, `DOCUMENT` |
| `media_approval_status` | `PENDING_SCAN`, `QUARANTINED`, `REJECTED`, `APPROVED`, `RETIRED` |
| `entitlement_status` | `SCHEDULED`, `ACTIVE`, `GRACE`, `EXPIRED`, `CANCELED`, `REVOKED` |
| `attempt_status` | `IN_PROGRESS`, `SUBMITTED`, `EVALUATED`, `FAILED`, `DELETION_QUEUED`, `DELETED` |
| `recording_classification` | `ASSESSMENT_ONLY`, `SAVED_RECORDING` — mặc định luôn là `ASSESSMENT_ONLY`. |
| `recording_status` | `UPLOADING`, `SCANNING`, `AVAILABLE`, `QUARANTINED`, `ASSESSING`, `DELETION_QUEUED`, `DELETED`, `EXPIRED` |
| `review_rating` | `AGAIN`, `HARD`, `GOOD`, `EASY` |
| `srs_status` | `LEARNING`, `REVIEW`, `RELEARNING`, `SUSPENDED`, `ARCHIVED` |
| `ai_usage_status` | `RESERVED`, `SUCCEEDED`, `FAILED_REFUNDED`, `FAILED_CONSUMED`, `BLOCKED` |
| `consent_state` | `GRANTED`, `WITHDRAWN` |
| `deletion_status` | `PENDING`, `CANCELED`, `ELIGIBLE`, `RUNNING`, `COMPLETED`, `FAILED`, `BLOCKED_LEGAL_HOLD` |
| `export_status` | `QUEUED`, `BUILDING`, `READY`, `DOWNLOADED`, `EXPIRED`, `FAILED`, `CANCELED` |

## 3. Sơ đồ quan hệ cấp cao

```text
users ──< user_roles                 users ──< auth_sessions ──< refresh_tokens
  │  └─< user_entitlements ──< ai_usage_counters / ai_usage_events
  ├── 1 user_profiles                users ──< consent_events / privacy requests
  ├──< lesson_progresses >── lessons ──< segments ──> media_assets
  ├──< dictation_attempts >── segments
  ├──< shadowing_attempts ──> recordings
  ├──< saved_words >── dictionary_entries ──< senses / examples / pronunciations
  │                       └──< dictionary_entry_characters >── hanzi_characters
  ├──< srs_schedules ──< srs_review_events
  └──< ai_conversations ──< ai_messages

topics ──< lessons ──< segments
subscription_plans ──< plan_ai_quotas
user_entitlements ──< ai_usage_counters / ai_usage_events
deletion_jobs ──< deletion_job_steps
leaderboard_snapshots ──< leaderboard_entries
```

## 4. Tài khoản, hồ sơ, mã hóa và xác thực

### 4.1 `users`

Tài khoản xác thực tối thiểu. Email và lý do trạng thái là dữ liệu riêng tư; không đặt `is_admin`, plan hoặc quota ở đây.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `user_id` | `uuid` | PK. |
| `email_ciphertext` | `bytea` | NOT NULL; email chuẩn hóa, mã hóa bằng DEK của learner. |
| `email_lookup_hash` | `char(64)` | NOT NULL, UNIQUE; HMAC của email đã chuẩn hóa, dùng cho đăng nhập/khử trùng lặp. |
| `password_hash` | `varchar(255)` | NOT NULL; bcrypt cost >= 12, không bao giờ là mật khẩu thô. |
| `status` | `varchar(32)` | NOT NULL; `user_status`, mặc định `PENDING_VERIFICATION`. |
| `email_verified_at` | `timestamptz` | NULL; chỉ được login khi khác NULL và `status = ACTIVE`. |
| `authz_version` | `bigint` | NOT NULL default `1`; tăng khi đổi password/role/entitlement hoặc revoke all. |
| `last_activity_at` | `timestamptz` | NOT NULL; cơ sở tính 12 tháng inactivity. |
| `last_login_at` | `timestamptz` | NULL. |
| `disabled_at` | `timestamptz` | NULL. |
| `status_reason_ciphertext` | `bytea` | NULL; chỉ dùng cho lý do lock/disable, không đưa ra client. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `deleted_at` | `timestamptz` | NULL; chỉ ghi sau crypto-erasure/physical deletion workflow; không dùng như soft delete thông thường. |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes: UNIQUE `email_lookup_hash`; `(status, last_activity_at)` cho retention sweep.

### 4.2 `encryption_keys`

Theo dõi DEK được KMS envelope-encrypt. KMS giữ key material; bảng chỉ có DEK đã bọc, reference và trạng thái crypto-erasure.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `encryption_key_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`; chủ sở hữu dữ liệu. |
| `parent_encryption_key_id` | `uuid` | NULL FK → `encryption_keys.encryption_key_id`; recording key có thể derive từ user key. |
| `key_scope` | `varchar(32)` | NOT NULL: `USER_DATA` hoặc `RECORDING`. |
| `kms_key_reference` | `varchar(255)` | NOT NULL; ARN/ID KMS không bí mật. |
| `wrapped_dek_ciphertext` | `bytea` | NOT NULL khi `ACTIVE`; NULL sau crypto-erasure nếu KMS workflow yêu cầu. |
| `algorithm` | `varchar(64)` | NOT NULL; ví dụ `AES-256-GCM`. |
| `status` | `varchar(24)` | NOT NULL: `ACTIVE`, `RETIRED`, `DESTROYED`. |
| `created_at` | `timestamptz` | NOT NULL. |
| `destroyed_at` | `timestamptz` | NULL; thời điểm hủy wrapped DEK. |
| `destruction_reason` | `varchar(64)` | NULL: `ACCOUNT_DELETION`, `RECORDING_DELETION`, `ROTATION`. |

Indexes/ràng buộc: `(user_id, status)`, `parent_encryption_key_id`; UNIQUE một key `USER_DATA/ACTIVE` trên mỗi user (partial unique). Không được xóa key row trước khi audit/deletion job đã hoàn tất.

### 4.3 `user_profiles`

Quan hệ 1–1 với user, gồm cả cài đặt phục vụ trải nghiệm học.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `user_id` | `uuid` | PK, FK → `users.user_id`. |
| `encryption_key_id` | `uuid` | NOT NULL FK → `encryption_keys.encryption_key_id`. |
| `display_name_ciphertext` | `bytea` | NULL; tên hiển thị. |
| `native_language_code` | `varchar(10)` | NOT NULL default `vi`. |
| `interface_locale` | `varchar(16)` | NOT NULL default `vi-VN`. |
| `time_zone` | `varchar(64)` | NOT NULL default `Asia/Ho_Chi_Minh`; IANA timezone. |
| `target_hsk_level` | `smallint` | NULL; CHECK 1–6. |
| `learning_goal` | `varchar(32)` | NULL: `LISTENING`, `SPEAKING`, `VOCABULARY`, `EXAM`, `GENERAL`. |
| `daily_goal_minutes` | `smallint` | NOT NULL default `15`; CHECK 1–1440. |
| `show_pinyin_default` | `boolean` | NOT NULL default `true`. |
| `onboarding_completed_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

### 4.4 `user_roles`

Role là server-managed. Không cấp role qua đăng ký/cập nhật profile.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `user_id` | `uuid` | PK (phần 1), FK → `users.user_id`. |
| `role_code` | `varchar(32)` | PK (phần 2); hiện chỉ nhận `ADMIN`. |
| `granted_by_user_id` | `uuid` | NULL FK → `users.user_id`; NULL chỉ cho initial Admin được provision có kiểm soát. |
| `granted_at` | `timestamptz` | NOT NULL. |
| `revoked_at` | `timestamptz` | NULL; không được dùng để tái cấp cùng row. |
| `grant_correlation_id` | `uuid` | NOT NULL. |

Ràng buộc: chỉ role chưa revoked được coi là active; UNIQUE partial `(user_id, role_code) WHERE revoked_at IS NULL`. Service cấm self-grant, self-revoke và revoke Admin cuối cùng.

### 4.5 `email_verification_tokens`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `email_verification_token_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `token_hash` | `char(64)` | NOT NULL, UNIQUE; hash/HMAC credential, không lưu credential thô. |
| `purpose` | `varchar(32)` | NOT NULL: `REGISTRATION` hoặc `EMAIL_CHANGE`. |
| `issued_at` | `timestamptz` | NOT NULL. |
| `expires_at` | `timestamptz` | NOT NULL. |
| `consumed_at` | `timestamptz` | NULL. |
| `invalidated_at` | `timestamptz` | NULL. |
| `request_ip_hash` | `char(64)` | NULL; privacy-safe hash. |
| `created_at` | `timestamptz` | NOT NULL. |

Indexes: `(user_id, purpose, expires_at)`; cleanup expired tokens. Confirm chỉ chấp nhận row chưa consumed/invalidated và chưa hết hạn.

### 4.6 `password_reset_tokens`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `password_reset_token_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `token_hash` | `char(64)` | NOT NULL, UNIQUE; không lưu reset credential thô. |
| `issued_at` | `timestamptz` | NOT NULL. |
| `expires_at` | `timestamptz` | NOT NULL. |
| `consumed_at` | `timestamptz` | NULL. |
| `invalidated_at` | `timestamptz` | NULL. |
| `request_ip_hash` | `char(64)` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |

### 4.7 `auth_sessions`

Bảng bắt buộc theo JWT/session contract. Một session đại diện một device login và một token family.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `session_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `family_id` | `uuid` | NOT NULL; định danh refresh-token family. |
| `authz_version` | `bigint` | NOT NULL; snapshot lúc cấp session. |
| `device_id` | `varchar(128)` | NOT NULL; installation/device ID đã validate. |
| `device_label` | `varchar(120)` | NOT NULL; sanitized, không chứa raw user-agent. |
| `platform` | `varchar(24)` | NOT NULL: `WEB`, `IOS`, `ANDROID`. |
| `ip_hash` | `char(64)` | NULL; hash privacy-safe, không lưu IP gốc. |
| `created_at` | `timestamptz` | NOT NULL. |
| `last_seen_at` | `timestamptz` | NOT NULL. |
| `idle_expires_at` | `timestamptz` | NOT NULL. |
| `absolute_expires_at` | `timestamptz` | NOT NULL. |
| `revoked_at` | `timestamptz` | NULL. |
| `revoked_reason` | `varchar(64)` | NULL: `LOGOUT`, `PASSWORD_RESET`, `ROLE_CHANGED`, `REFRESH_REUSE`, ... |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes: `(user_id, revoked_at)`, `family_id`, `(user_id, last_seen_at DESC)`. UNIQUE `family_id` cho mỗi session family đang hoạt động; service tính giới hạn thiết bị từ session chưa revoked/chưa hết hạn.

### 4.8 `refresh_tokens`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `refresh_token_id` | `uuid` | PK. |
| `session_id` | `uuid` | NOT NULL FK → `auth_sessions.session_id`. |
| `family_id` | `uuid` | NOT NULL; phải bằng `auth_sessions.family_id`. |
| `token_hash` | `char(64)` | NOT NULL, UNIQUE; HMAC-SHA-256 với pepper ở secret store. |
| `issued_at` | `timestamptz` | NOT NULL. |
| `expires_at` | `timestamptz` | NOT NULL; không vượt absolute expiry của session. |
| `rotated_at` | `timestamptz` | NULL. |
| `replaced_by_token_id` | `uuid` | NULL FK → `refresh_tokens.refresh_token_id`. |
| `revoked_at` | `timestamptz` | NULL. |
| `revoked_reason` | `varchar(64)` | NULL. |
| `refresh_request_id` | `uuid` | NULL; request id đã tạo child token. |
| `created_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes: `session_id`, `family_id`, `expires_at`. Rotated hash giữ đến absolute expiry để phát hiện reuse; một token active chỉ có tối đa một descendant.

### 4.9 `refresh_idempotency`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `refresh_idempotency_id` | `uuid` | PK. |
| `session_id` | `uuid` | NOT NULL FK → `auth_sessions.session_id`. |
| `refresh_request_id` | `uuid` | NOT NULL. |
| `source_refresh_token_id` | `uuid` | NOT NULL FK → `refresh_tokens.refresh_token_id`. |
| `response_ciphertext` | `bytea` | NOT NULL; KMS-encrypted replay payload, chứa raw refresh token duy nhất được phép lưu tạm. |
| `encryption_key_reference` | `varchar(255)` | NOT NULL; KMS key reference. |
| `created_at` | `timestamptz` | NOT NULL. |
| `expires_at` | `timestamptz` | NOT NULL; CHECK không quá 30 giây sau `created_at`. |
| `consumed_at` | `timestamptz` | NULL. |

Ràng buộc: UNIQUE `(session_id, refresh_request_id)`; worker xóa khi hết hạn. Không log nội dung `response_ciphertext`.

### 4.10 `auth_audit_events`

Append-only security log, không có FK bắt buộc để vẫn giữ được audit sau account deletion.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `auth_audit_event_id` | `uuid` | PK. |
| `occurred_at` | `timestamptz` | NOT NULL. |
| `event_type` | `varchar(64)` | NOT NULL: `LOGIN_SUCCEEDED`, `LOGIN_FAILED`, `REFRESH_REUSE`, `SESSION_REVOKED`, ... |
| `severity` | `varchar(16)` | NOT NULL: `INFO`, `WARNING`, `HIGH`. |
| `subject_user_id` | `uuid` | NULL; FK nullable → `users.user_id`, `ON DELETE SET NULL`. |
| `subject_pseudonym` | `char(64)` | NULL; giữ liên kết điều tra không định danh khi user bị xóa. |
| `actor_user_id` | `uuid` | NULL; FK nullable → `users.user_id`, `ON DELETE SET NULL`. |
| `session_id` | `uuid` | NULL; không cần FK cứng sau retention. |
| `family_id` | `uuid` | NULL. |
| `correlation_id` | `uuid` | NOT NULL. |
| `device_context` | `jsonb` | NULL; chỉ label/platform/ip hash đã lọc, không raw token/user agent. |
| `details` | `jsonb` | NULL; allowlist mã lỗi, không passwords/JWT/AI content. |

Indexes: `(subject_user_id, occurred_at DESC)`, `(event_type, occurred_at DESC)`, `correlation_id`; expire theo partition sau 12 tháng trừ legal/security hold.

### 4.11 `role_change_audit_events`

Append-only record riêng để đáp ứng yêu cầu actor/target/before-after role.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `role_change_audit_event_id` | `uuid` | PK. |
| `actor_user_id` | `uuid` | NULL FK → `users.user_id`, `ON DELETE SET NULL`; NULL chỉ bootstrap có kiểm soát. |
| `target_user_id` | `uuid` | NULL FK → `users.user_id`, `ON DELETE SET NULL`. |
| `target_pseudonym` | `char(64)` | NOT NULL. |
| `action` | `varchar(16)` | NOT NULL: `GRANTED` hoặc `REVOKED`. |
| `role_code` | `varchar(32)` | NOT NULL; hiện `ADMIN`. |
| `before_roles` | `jsonb` | NOT NULL; mảng role code đã validate. |
| `after_roles` | `jsonb` | NOT NULL; mảng role code đã validate. |
| `correlation_id` | `uuid` | NOT NULL. |
| `occurred_at` | `timestamptz` | NOT NULL. |
| `reason` | `varchar(255)` | NULL; không chứa dữ liệu nhạy cảm. |

Indexes: `(target_user_id, occurred_at DESC)`, `(actor_user_id, occurred_at DESC)`, `correlation_id`. Cấm UPDATE/DELETE ở database permission cho application role thông thường.

## 5. Gói, entitlement và quota AI

### 5.1 `subscription_plans`

Không lưu Payment Card. Đây là cấu hình versioned của Free/Premium; một plan bị retire vẫn giữ để giải thích lịch sử entitlement.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `subscription_plan_id` | `uuid` | PK. |
| `plan_code` | `varchar(32)` | NOT NULL, UNIQUE: `FREE`, `PREMIUM`. |
| `plan_version` | `integer` | NOT NULL; tăng khi cấu hình thay đổi. |
| `display_name` | `varchar(100)` | NOT NULL. |
| `status` | `varchar(16)` | NOT NULL: `ACTIVE`, `RETIRED`. |
| `max_active_sessions` | `smallint` | NOT NULL; Free=2, Premium=5 theo policy hiện hành. |
| `refresh_idle_days` | `smallint` | NOT NULL; Free=14, Premium=30. |
| `refresh_absolute_days` | `smallint` | NOT NULL; Free=30, Premium=90. |
| `created_at` | `timestamptz` | NOT NULL. |
| `retired_at` | `timestamptz` | NULL. |

Ràng buộc: UNIQUE `(plan_code, plan_version)`; `max_active_sessions > 0`; không update cấu hình đã được entitlement tham chiếu, tạo version mới.

### 5.2 `plan_ai_quotas`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `plan_ai_quota_id` | `uuid` | PK. |
| `subscription_plan_id` | `uuid` | NOT NULL FK → `subscription_plans.subscription_plan_id`. |
| `quota_key` | `varchar(48)` | NOT NULL: `AI_BUDDY_MESSAGE`, `DICTATION_EVALUATION`, `SHADOWING_ASSESSMENT`, ... |
| `period_type` | `varchar(16)` | NOT NULL: `DAY`, `MONTH`, `BILLING_CYCLE`. |
| `max_units` | `integer` | NOT NULL CHECK `>= 0`. |
| `effective_from` | `timestamptz` | NOT NULL. |
| `effective_to` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |

Ràng buộc/index: UNIQUE `(subscription_plan_id, quota_key, effective_from)`; CHECK period không chồng lấn được kiểm soát khi publish plan version.

### 5.3 `user_entitlements`

Nguồn quyền truy cập duy nhất cho Premium; lifecycle tự động của hệ thống hoặc provider được phê duyệt tạo/cập nhật row này. `ADMIN` không làm thay đổi row này.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `user_entitlement_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `subscription_plan_id` | `uuid` | NOT NULL FK → `subscription_plans.subscription_plan_id`. |
| `status` | `varchar(24)` | NOT NULL; `entitlement_status`. |
| `source_type` | `varchar(32)` | NOT NULL: `DEFAULT`, `BILLING`, `PROMOTION`, `MIGRATION`; mọi nguồn đều do lifecycle tự động, không có admin grant. |
| `external_reference_ciphertext` | `bytea` | NULL; mã subscription/payment provider nếu có, không lưu card data. |
| `starts_at` | `timestamptz` | NOT NULL. |
| `ends_at` | `timestamptz` | NULL; NULL chỉ cho Free/default entitlement. |
| `grace_ends_at` | `timestamptz` | NULL. |
| `revoked_at` | `timestamptz` | NULL. |
| `revoked_reason` | `varchar(64)` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes/ràng buộc: `(user_id, status, starts_at, ends_at)` và `(subscription_plan_id, status)`; tối đa một entitlement `ACTIVE/GRACE` có hiệu lực tại một thời điểm cho một user (enforce bằng service + exclusion/partial constraint hợp lý). Mỗi thay đổi hiệu lực phải tăng `users.authz_version` và re-evaluate session limit.

### 5.4 `ai_usage_counters`

Counter được khóa và tăng **trước** provider call; đây là điểm thực thi atomic quota.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `ai_usage_counter_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `user_entitlement_id` | `uuid` | NOT NULL FK → `user_entitlements.user_entitlement_id`. |
| `plan_ai_quota_id` | `uuid` | NOT NULL FK → `plan_ai_quotas.plan_ai_quota_id`. |
| `quota_key` | `varchar(48)` | NOT NULL, denormalized để lock/query. |
| `period_started_at` | `timestamptz` | NOT NULL. |
| `period_ends_at` | `timestamptz` | NOT NULL. |
| `max_units_snapshot` | `integer` | NOT NULL. |
| `reserved_units` | `integer` | NOT NULL default `0`. |
| `consumed_units` | `integer` | NOT NULL default `0`. |
| `refunded_units` | `integer` | NOT NULL default `0`. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`; row lock trong quota service. |

Ràng buộc: UNIQUE `(user_id, quota_key, period_started_at)`; CHECK các số không âm và `reserved_units + consumed_units - refunded_units <= max_units_snapshot` theo transaction. Index `(user_id, quota_key, period_ends_at)`.

### 5.5 `ai_usage_events`

Không chứa prompt, raw audio hay toàn bộ chat. Dùng để nối request với quota và phản hồi provider đã kiểm chứng.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `ai_usage_event_id` | `uuid` | PK; cũng là random request correlation ID gửi vendor. |
| `ai_usage_counter_id` | `uuid` | NOT NULL FK → `ai_usage_counters.ai_usage_counter_id`. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `feature_type` | `varchar(40)` | NOT NULL: `AI_BUDDY`, `DICTATION`, `SHADOWING`, `TRANSLATION`, `READING_AID`. |
| `provider_name` | `varchar(64)` | NOT NULL. |
| `provider_model` | `varchar(128)` | NULL; model identifier không bí mật. |
| `status` | `varchar(32)` | NOT NULL; `ai_usage_status`. |
| `requested_units` | `smallint` | NOT NULL default `1`. |
| `reserved_at` | `timestamptz` | NOT NULL. |
| `completed_at` | `timestamptz` | NULL. |
| `provider_request_reference` | `varchar(128)` | NULL; pseudonymous vendor reference. |
| `failure_code` | `varchar(64)` | NULL; safe internal code. |
| `correlation_id` | `uuid` | NOT NULL. |
| `created_at` | `timestamptz` | NOT NULL. |

Indexes: `(user_id, created_at DESC)`, `(ai_usage_counter_id, status)`, `correlation_id`. Khi fail, service chỉ refund khi policy xác định request chưa được provider xử lý; mọi transition ghi atomically với counter.

## 6. Nội dung học và media đã duyệt

### 6.1 `topics`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `topic_id` | `uuid` | PK. |
| `slug` | `varchar(160)` | NOT NULL, UNIQUE; immutable sau publish trừ redirect được phê duyệt. |
| `title` | `varchar(255)` | NOT NULL. |
| `description` | `text` | NULL. |
| `hsk_level` | `smallint` | NULL CHECK 1–6. |
| `sort_order` | `integer` | NOT NULL default `0`. |
| `publication_state` | `varchar(16)` | NOT NULL; `publication_state`. |
| `created_by_user_id` | `uuid` | NOT NULL FK → `users.user_id` (Admin). |
| `updated_by_user_id` | `uuid` | NOT NULL FK → `users.user_id` (Admin). |
| `published_at` | `timestamptz` | NULL. |
| `archived_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes: `(publication_state, sort_order)`, `(hsk_level, publication_state)`. State transition chỉ qua service theo state machine trong `CLAUDE.md`.

### 6.2 `lessons`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `lesson_id` | `uuid` | PK. |
| `topic_id` | `uuid` | NOT NULL FK → `topics.topic_id`. |
| `slug` | `varchar(160)` | NOT NULL, UNIQUE. |
| `title` | `varchar(255)` | NOT NULL. |
| `summary` | `text` | NULL. |
| `hsk_level` | `smallint` | NULL CHECK 1–6. |
| `lesson_type` | `varchar(32)` | NOT NULL: `AUDIO`, `VIDEO`, `MIXED`. |
| `access_level` | `varchar(16)` | NOT NULL; `FREE`/`PREMIUM`. |
| `publication_state` | `varchar(16)` | NOT NULL; `publication_state`. |
| `sort_order` | `integer` | NOT NULL default `0`. |
| `estimated_duration_seconds` | `integer` | NOT NULL default `0`. |
| `completion_rule` | `jsonb` | NOT NULL; versioned, validated rule như minimum completed segments/score. |
| `cover_media_asset_id` | `uuid` | NULL FK → `media_assets.media_asset_id`; phải là image `APPROVED`. |
| `created_by_user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `updated_by_user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `published_at` | `timestamptz` | NULL. |
| `archived_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes: `(topic_id, sort_order)`, `(publication_state, access_level, hsk_level)`, `(topic_id, publication_state)`. Learner access cần cả lesson/topic/segment `PUBLISHED` và entitlement server-side.

### 6.3 `media_assets`

Chỉ Admin tạo/cập nhật. `provider_asset_identifier` là định danh được allowlist/duyệt; không có cột URL do client cung cấp.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `media_asset_id` | `uuid` | PK. |
| `provider_name` | `varchar(64)` | NOT NULL: `YOUTUBE`, `OBJECT_STORAGE`, ... đã allowlist. |
| `provider_asset_identifier` | `varchar(255)` | NOT NULL; ID của provider, không phải arbitrary URL. |
| `media_kind` | `varchar(16)` | NOT NULL; `media_kind`. |
| `title` | `varchar(255)` | NOT NULL. |
| `description` | `text` | NULL. |
| `mime_type` | `varchar(127)` | NULL; bắt buộc với object upload. |
| `byte_size` | `bigint` | NULL CHECK `>= 0`. |
| `duration_milliseconds` | `integer` | NULL CHECK `>= 0`. |
| `checksum_sha256` | `char(64)` | NULL; bắt buộc cho upload nội bộ. |
| `approval_status` | `varchar(24)` | NOT NULL; `media_approval_status`. |
| `malware_scan_status` | `varchar(24)` | NOT NULL: `NOT_REQUIRED`, `PENDING`, `CLEAN`, `INFECTED`, `FAILED`. |
| `scan_completed_at` | `timestamptz` | NULL. |
| `approved_by_user_id` | `uuid` | NULL FK → `users.user_id`. |
| `approved_at` | `timestamptz` | NULL. |
| `rejected_reason` | `varchar(255)` | NULL. |
| `metadata` | `jsonb` | NULL; allowlisted provider metadata, không có secret/signed URL. |
| `created_by_user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Ràng buộc/index: UNIQUE `(provider_name, provider_asset_identifier)`; `(approval_status, media_kind)`. Asset chưa `APPROVED` không bao giờ được playback, và approval không tự publish lesson.

### 6.4 `segments`

Một đoạn luyện tập trên một media đã duyệt; transcript chuẩn ở đây là answer key server-controlled cho Dictation.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `segment_id` | `uuid` | PK. |
| `lesson_id` | `uuid` | NOT NULL FK → `lessons.lesson_id`. |
| `media_asset_id` | `uuid` | NOT NULL FK → `media_assets.media_asset_id`; asset phải APPROVED khi publish. |
| `sequence_no` | `integer` | NOT NULL CHECK `> 0`; thứ tự duy nhất trong lesson. |
| `segment_type` | `varchar(24)` | NOT NULL: `DICTATION`, `SHADOWING`, `BOTH`, `PLAYBACK_ONLY`. |
| `publication_state` | `varchar(16)` | NOT NULL; `publication_state`. |
| `start_milliseconds` | `integer` | NOT NULL CHECK `>= 0`. |
| `end_milliseconds` | `integer` | NOT NULL CHECK `> start_milliseconds`. |
| `transcript_hanzi` | `text` | NOT NULL; canonical transcript. |
| `transcript_pinyin` | `text` | NULL. |
| `translation_vi` | `text` | NULL. |
| `dictation_hint` | `text` | NULL; hint được phép lộ. |
| `answer_reveal_policy` | `varchar(24)` | NOT NULL default `AFTER_SUBMIT`: `NEVER`, `AFTER_SUBMIT`, `AFTER_MAX_ATTEMPTS`. |
| `sort_order` | `integer` | NOT NULL; thường trùng `sequence_no`. |
| `created_by_user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `updated_by_user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `published_at` | `timestamptz` | NULL. |
| `archived_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Ràng buộc/index: UNIQUE `(lesson_id, sequence_no)`; `(lesson_id, publication_state, sort_order)`; check `end_milliseconds <= media_assets.duration_milliseconds` ở service khi duration biết. Không xóa cứng segment có attempt; archive/unpublish để bảo toàn history.

### 6.5 `content_lifecycle_events`

Audit lịch sử content, tách khỏi private learner data.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `content_lifecycle_event_id` | `uuid` | PK. |
| `resource_type` | `varchar(16)` | NOT NULL: `TOPIC`, `LESSON`, `SEGMENT`, `MEDIA`. |
| `resource_id` | `uuid` | NOT NULL; ID resource, FK đa hình được service validate. |
| `action` | `varchar(32)` | NOT NULL: `CREATED`, `UPDATED`, `PUBLISHED`, `UNPUBLISHED`, `ARCHIVED`, `APPROVED`, `REJECTED`. |
| `before_state` | `varchar(24)` | NULL. |
| `after_state` | `varchar(24)` | NULL. |
| `actor_user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `correlation_id` | `uuid` | NOT NULL. |
| `change_summary` | `jsonb` | NULL; allowlisted field names/diff, không media URLs secrets. |
| `occurred_at` | `timestamptz` | NOT NULL. |

Indexes: `(resource_type, resource_id, occurred_at DESC)`, `(actor_user_id, occurred_at DESC)`, `correlation_id`. Application DB role chỉ INSERT.

## 7. Dictation, Shadowing và dữ liệu luyện tập

### 7.1 `dictation_attempts`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `dictation_attempt_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `lesson_id` | `uuid` | NOT NULL FK → `lessons.lesson_id`; snapshot/query ownership. |
| `segment_id` | `uuid` | NOT NULL FK → `segments.segment_id`; phải thuộc `lesson_id`. |
| `encryption_key_id` | `uuid` | NOT NULL FK → `encryption_keys.encryption_key_id`. |
| `status` | `varchar(24)` | NOT NULL; `attempt_status`. |
| `answer_ciphertext` | `bytea` | NULL; bài learner nhập, chỉ có sau submit. |
| `normalized_answer_ciphertext` | `bytea` | NULL; server-normalized form, không tin giá trị client. |
| `evaluation_source` | `varchar(24)` | NULL: `RULE_BASED`, `AI`, `HYBRID`. |
| `evaluation_version` | `varchar(64)` | NULL. |
| `overall_score` | `numeric(5,2)` | NULL CHECK 0–100. |
| `accuracy_percent` | `numeric(5,2)` | NULL CHECK 0–100. |
| `feedback_ciphertext` | `bytea` | NULL; actionable feedback đã validate. |
| `ai_usage_event_id` | `uuid` | NULL FK → `ai_usage_events.ai_usage_event_id`. |
| `started_at` | `timestamptz` | NOT NULL. |
| `submitted_at` | `timestamptz` | NULL. |
| `evaluated_at` | `timestamptz` | NULL. |
| `hidden_at` | `timestamptz` | NULL; ẩn ngay khi learner xóa. |
| `deleted_at` | `timestamptz` | NULL; chỉ sau hard-delete workflow. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes: `(user_id, segment_id, created_at DESC)`, `(user_id, lesson_id, status)`, `(ai_usage_event_id)`. API list mặc định exclude `hidden_at IS NOT NULL`; service không để client set score/feedback/status `EVALUATED`.

### 7.2 `dictation_attempt_errors`

Lỗi cụ thể của một lần chấm. Không nhân bản answer key plaintext; vị trí target tham chiếu transcript của segment.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `dictation_attempt_error_id` | `uuid` | PK. |
| `dictation_attempt_id` | `uuid` | NOT NULL FK → `dictation_attempts.dictation_attempt_id`. |
| `sequence_no` | `integer` | NOT NULL CHECK `> 0`. |
| `error_type` | `varchar(32)` | NOT NULL: `OMISSION`, `INSERTION`, `SUBSTITUTION`, `ORDER`, `PUNCTUATION`. |
| `target_start_offset` | `integer` | NULL CHECK `>= 0`; offset segment transcript. |
| `target_end_offset` | `integer` | NULL CHECK `>= target_start_offset`. |
| `actual_text_ciphertext` | `bytea` | NULL; phần learner nhập sai. |
| `severity` | `varchar(16)` | NOT NULL default `ERROR`: `INFO`, `WARNING`, `ERROR`. |
| `explanation_ciphertext` | `bytea` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |

Ràng buộc: UNIQUE `(dictation_attempt_id, sequence_no)`; index `dictation_attempt_id`.

### 7.3 `recordings`

Metadata cho raw voice. Object/audio versions nằm trong object storage; không có public URL trong bảng.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `recording_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `encryption_key_id` | `uuid` | NOT NULL FK → `encryption_keys.encryption_key_id`; key scope `RECORDING`. |
| `classification` | `varchar(32)` | NOT NULL; `ASSESSMENT_ONLY` hoặc `SAVED_RECORDING`, server default `ASSESSMENT_ONLY`. |
| `status` | `varchar(32)` | NOT NULL; `recording_status`. |
| `storage_provider` | `varchar(64)` | NOT NULL. |
| `object_key_ciphertext` | `bytea` | NULL; encrypted object key, chỉ tồn tại sau upload accepted. |
| `object_version_ciphertext` | `bytea` | NULL; encrypted version ID. |
| `mime_type` | `varchar(127)` | NOT NULL; allowlist audio. |
| `byte_size` | `bigint` | NOT NULL CHECK `> 0`; validated limit. |
| `duration_milliseconds` | `integer` | NULL CHECK `>= 0`. |
| `checksum_sha256` | `char(64)` | NOT NULL. |
| `malware_scan_status` | `varchar(24)` | NOT NULL: `PENDING`, `CLEAN`, `INFECTED`, `FAILED`. |
| `scan_completed_at` | `timestamptz` | NULL. |
| `save_requested_at` | `timestamptz` | NULL; phải có explicit consent và `RECORDING_RETENTION` active để classification `SAVED_RECORDING`. |
| `assessment_succeeded_at` | `timestamptz` | NULL. |
| `expires_at` | `timestamptz` | NOT NULL; 30 ngày success / 24h failed-abandoned với ASSESSMENT_ONLY. |
| `hidden_at` | `timestamptz` | NULL. |
| `deletion_requested_at` | `timestamptz` | NULL. |
| `deleted_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes: `(user_id, status, created_at DESC)`, `(classification, expires_at)`, `(encryption_key_id)`. Service purge object versions, derivatives, CDN/cache rồi hủy recording key; never return raw object key/signed URL to frontend.

### 7.4 `shadowing_attempts`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `shadowing_attempt_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `lesson_id` | `uuid` | NOT NULL FK → `lessons.lesson_id`. |
| `segment_id` | `uuid` | NOT NULL FK → `segments.segment_id`; phải hỗ trợ Shadowing. |
| `recording_id` | `uuid` | NOT NULL, UNIQUE FK → `recordings.recording_id`; một upload có một assessment. |
| `encryption_key_id` | `uuid` | NOT NULL FK → `encryption_keys.encryption_key_id`. |
| `status` | `varchar(24)` | NOT NULL; `attempt_status`. |
| `transcript_ciphertext` | `bytea` | NULL; ASR result nếu feature cần. |
| `overall_score` | `numeric(5,2)` | NULL CHECK 0–100. |
| `pronunciation_score` | `numeric(5,2)` | NULL CHECK 0–100. |
| `tone_score` | `numeric(5,2)` | NULL CHECK 0–100. |
| `rhythm_score` | `numeric(5,2)` | NULL CHECK 0–100. |
| `fluency_score` | `numeric(5,2)` | NULL CHECK 0–100. |
| `summary_feedback_ciphertext` | `bytea` | NULL; learning guidance, không high-stakes evaluation. |
| `assessment_version` | `varchar(64)` | NULL. |
| `ai_usage_event_id` | `uuid` | NULL FK → `ai_usage_events.ai_usage_event_id`. |
| `started_at` | `timestamptz` | NOT NULL. |
| `submitted_at` | `timestamptz` | NULL. |
| `evaluated_at` | `timestamptz` | NULL. |
| `hidden_at` | `timestamptz` | NULL. |
| `deleted_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes: `(user_id, segment_id, created_at DESC)`, `(user_id, lesson_id, status)`, `recording_id`, `ai_usage_event_id`. Khi AI fail, giữ recording theo lifecycle nếu có thể và đặt `FAILED` với error an toàn ở API, không xóa dữ liệu learner ngay lập tức.

### 7.5 `shadowing_feedback_items`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `shadowing_feedback_item_id` | `uuid` | PK. |
| `shadowing_attempt_id` | `uuid` | NOT NULL FK → `shadowing_attempts.shadowing_attempt_id`. |
| `sequence_no` | `integer` | NOT NULL CHECK `> 0`. |
| `feedback_type` | `varchar(24)` | NOT NULL: `IPA`, `TONE`, `RHYTHM`, `FLUENCY`, `WORD_BOUNDARY`. |
| `severity` | `varchar(16)` | NOT NULL: `PRAISE`, `INFO`, `IMPROVE`. |
| `start_milliseconds` | `integer` | NULL CHECK `>= 0`. |
| `end_milliseconds` | `integer` | NULL CHECK `>= start_milliseconds`. |
| `expected_reference` | `varchar(255)` | NULL; IPA/target lấy từ content, không phải PII. |
| `observed_ciphertext` | `bytea` | NULL; observed IPA/text thuộc learner. |
| `suggestion_ciphertext` | `bytea` | NOT NULL. |
| `created_at` | `timestamptz` | NOT NULL. |

Ràng buộc: UNIQUE `(shadowing_attempt_id, sequence_no)`; index `shadowing_attempt_id`.

## 8. Từ điển, Hán tự, vocabulary và SRS

### 8.1 `dictionary_entries`

Shared canonical vocabulary. Xóa/cập nhật entry này là quản trị nội dung, khác hoàn toàn thao tác xóa `saved_words` của learner.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `dictionary_entry_id` | `uuid` | PK. |
| `simplified_hanzi` | `varchar(128)` | NOT NULL. |
| `traditional_hanzi` | `varchar(128)` | NULL. |
| `normalized_hanzi` | `varchar(128)` | NOT NULL; dùng de-duplicate/search. |
| `primary_pinyin` | `varchar(255)` | NOT NULL. |
| `normalized_pinyin` | `varchar(255)` | NOT NULL; accent/spacing normalized cho search. |
| `word_type` | `varchar(32)` | NULL: `WORD`, `PHRASE`, `CHARACTER`, `IDIOM`. |
| `hsk_level` | `smallint` | NULL CHECK 1–6. |
| `frequency_rank` | `integer` | NULL CHECK `> 0`. |
| `image_media_asset_id` | `uuid` | NULL FK → `media_assets.media_asset_id`; image phải approved. |
| `publication_state` | `varchar(16)` | NOT NULL default `PUBLISHED`. |
| `source_reference` | `varchar(255)` | NULL; license/provenance, không chứa secret. |
| `created_by_user_id` | `uuid` | NULL FK → `users.user_id`. |
| `updated_by_user_id` | `uuid` | NULL FK → `users.user_id`. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes: UNIQUE `(normalized_hanzi, normalized_pinyin)`; GIN/trigram search index cho `simplified_hanzi`, `traditional_hanzi`, `normalized_pinyin`; `(publication_state, hsk_level)`.

### 8.2 `dictionary_senses`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `dictionary_sense_id` | `uuid` | PK. |
| `dictionary_entry_id` | `uuid` | NOT NULL FK → `dictionary_entries.dictionary_entry_id`. |
| `sequence_no` | `integer` | NOT NULL CHECK `> 0`. |
| `part_of_speech` | `varchar(64)` | NULL. |
| `definition_vi` | `text` | NOT NULL. |
| `definition_en` | `text` | NULL. |
| `usage_note_vi` | `text` | NULL. |
| `register_label` | `varchar(64)` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |

Ràng buộc: UNIQUE `(dictionary_entry_id, sequence_no)`; index `dictionary_entry_id`.

### 8.3 `dictionary_examples`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `dictionary_example_id` | `uuid` | PK. |
| `dictionary_sense_id` | `uuid` | NOT NULL FK → `dictionary_senses.dictionary_sense_id`. |
| `sequence_no` | `integer` | NOT NULL CHECK `> 0`. |
| `sentence_hanzi` | `text` | NOT NULL. |
| `sentence_pinyin` | `text` | NULL. |
| `translation_vi` | `text` | NOT NULL. |
| `source_reference` | `varchar(255)` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |

Ràng buộc: UNIQUE `(dictionary_sense_id, sequence_no)`.

### 8.4 `dictionary_pronunciations`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `dictionary_pronunciation_id` | `uuid` | PK. |
| `dictionary_entry_id` | `uuid` | NOT NULL FK → `dictionary_entries.dictionary_entry_id`. |
| `pinyin` | `varchar(255)` | NOT NULL. |
| `dialect_code` | `varchar(16)` | NOT NULL default `cmn`. |
| `media_asset_id` | `uuid` | NOT NULL FK → `media_assets.media_asset_id`; approved audio. |
| `is_primary` | `boolean` | NOT NULL default `false`. |
| `created_at` | `timestamptz` | NOT NULL. |

Ràng buộc: UNIQUE `(dictionary_entry_id, pinyin, dialect_code)` và một `is_primary = true` mỗi entry (partial unique).

### 8.5 `hanzi_characters`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `hanzi_character_id` | `uuid` | PK. |
| `character` | `varchar(8)` | NOT NULL, UNIQUE; một Unicode Han character. |
| `unicode_code_point` | `integer` | NOT NULL, UNIQUE. |
| `radical` | `varchar(16)` | NULL. |
| `stroke_count` | `smallint` | NULL CHECK `> 0`. |
| `primary_pinyin` | `varchar(64)` | NULL. |
| `hsk_level` | `smallint` | NULL CHECK 1–6. |
| `publication_state` | `varchar(16)` | NOT NULL default `PUBLISHED`. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |

### 8.6 `hanzi_stroke_paths`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `hanzi_stroke_path_id` | `uuid` | PK. |
| `hanzi_character_id` | `uuid` | NOT NULL FK → `hanzi_characters.hanzi_character_id`. |
| `sequence_no` | `smallint` | NOT NULL CHECK `> 0`. |
| `stroke_path` | `text` | NOT NULL; validated SVG path/compact vector data, không executable markup. |
| `start_point` | `jsonb` | NULL; `{x,y}` normalized 0–1. |
| `end_point` | `jsonb` | NULL; `{x,y}` normalized 0–1. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |

Ràng buộc: UNIQUE `(hanzi_character_id, sequence_no)`.

### 8.7 `dictionary_entry_characters`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `dictionary_entry_id` | `uuid` | PK (phần 1), FK → `dictionary_entries.dictionary_entry_id`. |
| `hanzi_character_id` | `uuid` | PK (phần 2), FK → `hanzi_characters.hanzi_character_id`. |
| `character_position` | `smallint` | NOT NULL CHECK `>= 0`. |
| `created_at` | `timestamptz` | NOT NULL. |

Ràng buộc: UNIQUE `(dictionary_entry_id, character_position)`; index `hanzi_character_id`.

### 8.8 `saved_words`

Kho từ cá nhân; save cùng từ là idempotent.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `saved_word_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `dictionary_entry_id` | `uuid` | NOT NULL FK → `dictionary_entries.dictionary_entry_id`. |
| `encryption_key_id` | `uuid` | NOT NULL FK → `encryption_keys.encryption_key_id`. |
| `source_lesson_id` | `uuid` | NULL FK → `lessons.lesson_id`; context save. |
| `source_segment_id` | `uuid` | NULL FK → `segments.segment_id`; phải thuộc source lesson nếu cùng có. |
| `personal_note_ciphertext` | `bytea` | NULL. |
| `status` | `varchar(16)` | NOT NULL default `ACTIVE`: `ACTIVE`, `ARCHIVED`, `DELETION_QUEUED`, `DELETED`. |
| `saved_at` | `timestamptz` | NOT NULL. |
| `last_seen_at` | `timestamptz` | NULL. |
| `deleted_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Ràng buộc/index: UNIQUE partial `(user_id, dictionary_entry_id) WHERE deleted_at IS NULL`; `(user_id, status, saved_at DESC)`, `dictionary_entry_id`. POST save trả existing active row, không tạo duplicate.

### 8.9 `srs_schedules`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `srs_schedule_id` | `uuid` | PK. |
| `saved_word_id` | `uuid` | NOT NULL, UNIQUE FK → `saved_words.saved_word_id`. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`; consistency với saved word. |
| `status` | `varchar(16)` | NOT NULL; `srs_status`. |
| `due_at` | `timestamptz` | NOT NULL. |
| `interval_days` | `numeric(8,3)` | NOT NULL default `0`; CHECK `>= 0`. |
| `ease_factor` | `numeric(5,3)` | NOT NULL default `2.500`; algorithm-bounded. |
| `repetitions` | `integer` | NOT NULL default `0`. |
| `lapses` | `integer` | NOT NULL default `0`. |
| `last_reviewed_at` | `timestamptz` | NULL. |
| `algorithm_version` | `varchar(32)` | NOT NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes: `(user_id, status, due_at)`, `saved_word_id`. Review service is sole writer of schedule math; client submits only `review_rating`.

### 8.10 `review_sessions`

Nhóm các review event của một lượt luyện SRS, hữu ích cho resume và analytics.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `review_session_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `status` | `varchar(16)` | NOT NULL: `IN_PROGRESS`, `COMPLETED`, `ABANDONED`. |
| `scheduled_item_count` | `integer` | NOT NULL default `0`. |
| `reviewed_item_count` | `integer` | NOT NULL default `0`. |
| `started_at` | `timestamptz` | NOT NULL. |
| `completed_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Index: `(user_id, status, started_at DESC)`.

### 8.11 `srs_review_events`

Lịch sử bất biến trước/sau mỗi lần tính lịch.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `srs_review_event_id` | `uuid` | PK. |
| `srs_schedule_id` | `uuid` | NOT NULL FK → `srs_schedules.srs_schedule_id`. |
| `review_session_id` | `uuid` | NULL FK → `review_sessions.review_session_id`. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `rating` | `varchar(16)` | NOT NULL; `review_rating`. |
| `response_time_milliseconds` | `integer` | NULL CHECK `>= 0`. |
| `previous_due_at` | `timestamptz` | NOT NULL. |
| `next_due_at` | `timestamptz` | NOT NULL. |
| `previous_interval_days` | `numeric(8,3)` | NOT NULL. |
| `next_interval_days` | `numeric(8,3)` | NOT NULL. |
| `previous_ease_factor` | `numeric(5,3)` | NOT NULL. |
| `next_ease_factor` | `numeric(5,3)` | NOT NULL. |
| `algorithm_version` | `varchar(32)` | NOT NULL. |
| `reviewed_at` | `timestamptz` | NOT NULL. |
| `created_at` | `timestamptz` | NOT NULL. |

Indexes: `(srs_schedule_id, reviewed_at DESC)`, `(user_id, reviewed_at DESC)`, `review_session_id`. Không UPDATE/DELETE thông thường; deletion account/attempt theo workflow privacy.

### 8.12 `character_writing_attempts`

Hỗ trợ luyện viết Hán tự mà không phải lưu toàn bộ nét thô không cần thiết.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `character_writing_attempt_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `hanzi_character_id` | `uuid` | NOT NULL FK → `hanzi_characters.hanzi_character_id`. |
| `encryption_key_id` | `uuid` | NOT NULL FK → `encryption_keys.encryption_key_id`. |
| `score` | `numeric(5,2)` | NULL CHECK 0–100. |
| `stroke_count_submitted` | `smallint` | NOT NULL CHECK `>= 0`. |
| `stroke_count_expected` | `smallint` | NOT NULL CHECK `>= 0`. |
| `evaluation_summary_ciphertext` | `bytea` | NULL. |
| `evaluation_version` | `varchar(32)` | NOT NULL. |
| `started_at` | `timestamptz` | NOT NULL. |
| `submitted_at` | `timestamptz` | NULL. |
| `deleted_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |

Indexes: `(user_id, hanzi_character_id, created_at DESC)`. Nếu feature sau này thật sự cần stroke trace replay, lưu object mã hóa ngắn hạn ở bảng/retention riêng được spec duyệt; không thêm mặc định.

## 9. Tiến độ, thống kê, khuyến nghị và bảng xếp hạng

### 9.1 `lesson_progresses`

Nguồn tổng hợp tiến độ lesson của learner; attempts/reviews gọi một Progress service để cập nhật row này.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `lesson_progress_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `lesson_id` | `uuid` | NOT NULL FK → `lessons.lesson_id`. |
| `status` | `varchar(16)` | NOT NULL: `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`. |
| `current_segment_id` | `uuid` | NULL FK → `segments.segment_id`; phải thuộc lesson. |
| `completed_segment_count` | `integer` | NOT NULL default `0`. |
| `total_segment_count_snapshot` | `integer` | NOT NULL default `0`. |
| `completion_percent` | `numeric(5,2)` | NOT NULL default `0` CHECK 0–100. |
| `dictation_best_score` | `numeric(5,2)` | NULL CHECK 0–100. |
| `shadowing_best_score` | `numeric(5,2)` | NULL CHECK 0–100. |
| `practice_seconds` | `integer` | NOT NULL default `0`. |
| `started_at` | `timestamptz` | NULL. |
| `completed_at` | `timestamptz` | NULL; chỉ thỏa `lessons.completion_rule`. |
| `last_activity_at` | `timestamptz` | NOT NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Ràng buộc: UNIQUE `(user_id, lesson_id)`; indexes `(user_id, status, last_activity_at DESC)`, `(lesson_id, status)`.

### 9.2 `segment_progresses`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `segment_progress_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `lesson_id` | `uuid` | NOT NULL FK → `lessons.lesson_id`. |
| `segment_id` | `uuid` | NOT NULL FK → `segments.segment_id`. |
| `dictation_attempt_count` | `integer` | NOT NULL default `0`. |
| `dictation_best_score` | `numeric(5,2)` | NULL CHECK 0–100. |
| `shadowing_attempt_count` | `integer` | NOT NULL default `0`. |
| `shadowing_best_score` | `numeric(5,2)` | NULL CHECK 0–100. |
| `is_completed` | `boolean` | NOT NULL default `false`. |
| `completed_at` | `timestamptz` | NULL. |
| `last_practiced_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Ràng buộc/index: UNIQUE `(user_id, segment_id)`; `(user_id, lesson_id, is_completed)`. Service đảm bảo segment thuộc lesson.

### 9.3 `learning_activity_events`

Event cá nhân cho thời lượng học/chỉ số nguồn; khác `analytics_events` là dữ liệu product analytics pseudonymous.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `learning_activity_event_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `event_type` | `varchar(40)` | NOT NULL: `LESSON_PLAYED`, `DICTATION_EVALUATED`, `SHADOWING_EVALUATED`, `SRS_REVIEWED`, `WORD_SAVED`, `WRITING_SUBMITTED`. |
| `lesson_id` | `uuid` | NULL FK → `lessons.lesson_id`. |
| `segment_id` | `uuid` | NULL FK → `segments.segment_id`. |
| `dictionary_entry_id` | `uuid` | NULL FK → `dictionary_entries.dictionary_entry_id`. |
| `source_resource_type` | `varchar(32)` | NULL. |
| `source_resource_id` | `uuid` | NULL; service-validated link to attempt/review. |
| `duration_seconds` | `integer` | NOT NULL default `0` CHECK `>= 0`. |
| `score` | `numeric(5,2)` | NULL CHECK 0–100. |
| `occurred_at` | `timestamptz` | NOT NULL. |
| `created_at` | `timestamptz` | NOT NULL. |

Indexes: `(user_id, occurred_at DESC)`, `(user_id, event_type, occurred_at DESC)`, `(lesson_id, occurred_at)`. Retain với learning history tối đa 12 tháng inactivity; no unbounded API.

### 9.4 `learning_daily_statistics`

Daily aggregate materialized bởi Progress service, không để frontend tự cộng dữ liệu.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `learning_daily_statistic_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `local_date` | `date` | NOT NULL; ngày theo timezone snapshot. |
| `time_zone` | `varchar(64)` | NOT NULL. |
| `practice_seconds` | `integer` | NOT NULL default `0`. |
| `lessons_started_count` | `integer` | NOT NULL default `0`. |
| `lessons_completed_count` | `integer` | NOT NULL default `0`. |
| `dictation_attempt_count` | `integer` | NOT NULL default `0`. |
| `dictation_accuracy_percent` | `numeric(5,2)` | NULL CHECK 0–100. |
| `shadowing_attempt_count` | `integer` | NOT NULL default `0`. |
| `shadowing_score_percent` | `numeric(5,2)` | NULL CHECK 0–100. |
| `review_count` | `integer` | NOT NULL default `0`. |
| `new_words_saved_count` | `integer` | NOT NULL default `0`. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Ràng buộc/index: UNIQUE `(user_id, local_date)`; `(user_id, local_date DESC)`. Xóa/anonymize theo learning history, không giữ linked personal row sau account deletion.

### 9.5 `learning_weaknesses`

Điểm yếu là kết quả suy ra, có evidence và expiry để khuyến nghị không lỗi thời.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `learning_weakness_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `weakness_type` | `varchar(32)` | NOT NULL: `DICTATION_ACCURACY`, `TONE`, `IPA`, `RHYTHM`, `VOCABULARY_RECALL`, `HANZI_WRITING`. |
| `scope_type` | `varchar(32)` | NOT NULL: `GLOBAL`, `HSK_LEVEL`, `LESSON`, `SEGMENT`, `DICTIONARY_ENTRY`, `HANZI_CHARACTER`. |
| `scope_resource_id` | `uuid` | NULL; required trừ `GLOBAL`, validated by service. |
| `severity_score` | `numeric(5,2)` | NOT NULL CHECK 0–100; cao hơn = cần ưu tiên hơn. |
| `evidence_count` | `integer` | NOT NULL CHECK `>= 0`. |
| `first_observed_at` | `timestamptz` | NOT NULL. |
| `last_observed_at` | `timestamptz` | NOT NULL. |
| `resolved_at` | `timestamptz` | NULL. |
| `expires_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes: `(user_id, resolved_at, severity_score DESC)`, `(user_id, weakness_type, last_observed_at DESC)`; service upsert theo user/type/scope.

### 9.6 `learning_recommendations`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `learning_recommendation_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `recommendation_type` | `varchar(40)` | NOT NULL: `REVIEW_DUE`, `RESUME_LESSON`, `PRACTICE_WEAKNESS`, `NEW_LESSON`. |
| `target_resource_type` | `varchar(32)` | NOT NULL. |
| `target_resource_id` | `uuid` | NOT NULL; service-validated. |
| `reason_code` | `varchar(64)` | NOT NULL. |
| `rank_score` | `numeric(10,4)` | NOT NULL. |
| `generated_at` | `timestamptz` | NOT NULL. |
| `expires_at` | `timestamptz` | NOT NULL. |
| `dismissed_at` | `timestamptz` | NULL. |
| `acted_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |

Indexes: `(user_id, dismissed_at, expires_at, rank_score DESC)`; recommendations không cấp quyền đọc content Premium, API access check vẫn chạy.

### 9.7 `leaderboard_preferences`

Opt-in privacy-safe cho bảng xếp hạng; không dùng display name mặc định của profile.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `user_id` | `uuid` | PK FK → `users.user_id`. |
| `is_opted_in` | `boolean` | NOT NULL default `false`. |
| `public_alias` | `varchar(48)` | NULL; required khi opt-in, validated/no PII policy. |
| `include_hsk_scope` | `boolean` | NOT NULL default `true`. |
| `opted_in_at` | `timestamptz` | NULL. |
| `opted_out_at` | `timestamptz` | NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Ràng buộc: UNIQUE `public_alias` khi not null. Opt-out lập tức ẩn entry kỳ hiện tại và queue xóa snapshot entries có user đó nếu chính sách yêu cầu.

### 9.8 `leaderboard_snapshots`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `leaderboard_snapshot_id` | `uuid` | PK. |
| `period_type` | `varchar(16)` | NOT NULL: `WEEKLY`, `MONTHLY`. |
| `period_starts_on` | `date` | NOT NULL. |
| `period_ends_on` | `date` | NOT NULL. |
| `scope_type` | `varchar(16)` | NOT NULL: `GLOBAL`, `HSK_LEVEL`. |
| `hsk_level` | `smallint` | NULL CHECK 1–6; required khi scope HSK_LEVEL. |
| `generated_at` | `timestamptz` | NOT NULL. |
| `status` | `varchar(16)` | NOT NULL: `BUILDING`, `PUBLISHED`, `RETIRED`. |
| `created_at` | `timestamptz` | NOT NULL. |

Ràng buộc: UNIQUE `(period_type, period_starts_on, scope_type, hsk_level)`; check `period_ends_on >= period_starts_on`.

### 9.9 `leaderboard_entries`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `leaderboard_entry_id` | `uuid` | PK. |
| `leaderboard_snapshot_id` | `uuid` | NOT NULL FK → `leaderboard_snapshots.leaderboard_snapshot_id`. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `public_alias_snapshot` | `varchar(48)` | NOT NULL. |
| `rank` | `integer` | NOT NULL CHECK `> 0`. |
| `score` | `numeric(12,2)` | NOT NULL CHECK `>= 0`. |
| `practice_seconds` | `integer` | NOT NULL CHECK `>= 0`. |
| `created_at` | `timestamptz` | NOT NULL. |

Ràng buộc/index: UNIQUE `(leaderboard_snapshot_id, user_id)`, UNIQUE `(leaderboard_snapshot_id, rank)`, `(user_id, created_at DESC)`. Không expose `user_id` public.

## 10. AI Buddy

### 10.1 `ai_scenarios`

Nội dung tình huống/tutor prompt template do Admin quản lý; không bao giờ trả provider system prompt nguyên trạng cho client.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `ai_scenario_id` | `uuid` | PK. |
| `slug` | `varchar(160)` | NOT NULL, UNIQUE. |
| `title` | `varchar(255)` | NOT NULL. |
| `description` | `text` | NULL. |
| `hsk_level` | `smallint` | NULL CHECK 1–6. |
| `publication_state` | `varchar(16)` | NOT NULL. |
| `prompt_template_ciphertext` | `bytea` | NOT NULL; server/provider-controlled template, restricted access. |
| `template_version` | `varchar(32)` | NOT NULL. |
| `created_by_user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `updated_by_user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `published_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Index: `(publication_state, hsk_level)`. Nếu prompt template được coi là service configuration thay vì content, chỉ Security/Operations access; Admin edit cần feature spec riêng.

### 10.2 `ai_conversations`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `ai_conversation_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `encryption_key_id` | `uuid` | NOT NULL FK → `encryption_keys.encryption_key_id`. |
| `ai_scenario_id` | `uuid` | NULL FK → `ai_scenarios.ai_scenario_id`. |
| `title_ciphertext` | `bytea` | NULL; learner có thể rename. |
| `status` | `varchar(16)` | NOT NULL default `ACTIVE`: `ACTIVE`, `DELETION_QUEUED`, `DELETED`. |
| `last_message_at` | `timestamptz` | NULL. |
| `hidden_at` | `timestamptz` | NULL; xóa UI ngay lập tức. |
| `deleted_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes: `(user_id, hidden_at, last_message_at DESC)`, `ai_scenario_id`. Mọi GET/PATCH/DELETE require owner; Admin không có bypass.

### 10.3 `ai_messages`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `ai_message_id` | `uuid` | PK. |
| `ai_conversation_id` | `uuid` | NOT NULL FK → `ai_conversations.ai_conversation_id`. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`; consistency với conversation. |
| `encryption_key_id` | `uuid` | NOT NULL FK → `encryption_keys.encryption_key_id`. |
| `sequence_no` | `integer` | NOT NULL CHECK `> 0`. |
| `sender_type` | `varchar(16)` | NOT NULL: `LEARNER`, `ASSISTANT`. Không persist hidden system prompt. |
| `content_ciphertext` | `bytea` | NOT NULL. |
| `language_code` | `varchar(16)` | NULL. |
| `status` | `varchar(16)` | NOT NULL: `PENDING`, `COMPLETE`, `FAILED`, `DELETED`. |
| `ai_usage_event_id` | `uuid` | NULL FK → `ai_usage_events.ai_usage_event_id`; chỉ assistant message. |
| `provider_model` | `varchar(128)` | NULL. |
| `safety_result` | `varchar(32)` | NULL; safe code, không lưu prompt classifier raw. |
| `created_at` | `timestamptz` | NOT NULL. |
| `completed_at` | `timestamptz` | NULL. |
| `deleted_at` | `timestamptz` | NULL. |

Ràng buộc/index: UNIQUE `(ai_conversation_id, sequence_no)`; `(user_id, created_at DESC)`, `ai_usage_event_id`. Provider chỉ nhận context tối thiểu; content bị xóa phải bị loại khỏi search/cache/export ngay.

## 11. Privacy, consent, export, retention và deletion

### 11.1 `consent_events`

Append-only evidence. Current consent được suy ra từ event valid mới nhất cho `(user, purpose, scope)`.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `consent_event_id` | `uuid` | PK. |
| `user_id` | `uuid` | NULL FK → `users.user_id`, `ON DELETE SET NULL`. |
| `user_pseudonym` | `char(64)` | NOT NULL; giữ evidence sau deletion. |
| `purpose` | `varchar(32)` | NOT NULL: `SERVICE_OPERATION`, `RECORDING_RETENTION`, `AI_MODEL_IMPROVEMENT`, `MARKETING`, `TEACHER_SHARING`. |
| `scope_type` | `varchar(32)` | NOT NULL default `GLOBAL`: `GLOBAL`, `TEACHER`, `CLASS`, `RECORDING`. |
| `scope_reference_ciphertext` | `bytea` | NULL; recipient/class/recording ref khi applicable. |
| `state` | `varchar(16)` | NOT NULL; `GRANTED`/`WITHDRAWN`. |
| `policy_version` | `varchar(64)` | NOT NULL. |
| `locale` | `varchar(16)` | NOT NULL. |
| `source` | `varchar(32)` | NOT NULL: `WEB`, `MOBILE`, `SUPPORT_VERIFIED`. |
| `ip_hash` | `char(64)` | NULL. |
| `correlation_id` | `uuid` | NOT NULL. |
| `occurred_at` | `timestamptz` | NOT NULL. |
| `expires_at` | `timestamptz` | NOT NULL; 5 năm sau withdrawal/account deletion subject legal approval. |

Indexes: `(user_id, purpose, occurred_at DESC)`, `(user_pseudonym, purpose, occurred_at DESC)`, `(purpose, state)`. Application role insert-only; required `SERVICE_OPERATION` không được withdraw qua API mà phải trả `409` với account deletion option.

### 11.2 `processing_restrictions`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `processing_restriction_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `purpose` | `varchar(32)` | NOT NULL; optional purpose duy nhất. |
| `status` | `varchar(24)` | NOT NULL: `PENDING`, `ACTIVE`, `REJECTED`, `RELEASED`. |
| `requested_at` | `timestamptz` | NOT NULL. |
| `applied_at` | `timestamptz` | NULL; pending đã chặn new processing ngay. |
| `released_at` | `timestamptz` | NULL. |
| `decision_reason_ciphertext` | `bytea` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Ràng buộc: UNIQUE partial `(user_id, purpose) WHERE status IN ('PENDING','ACTIVE')`; index `(purpose, status, requested_at)`.

### 11.3 `teacher_sharing_grants`

Reserved privacy contract: Teacher không là role/actor mặc định. Bảng chỉ được dùng khi feature sharing được phê duyệt.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `teacher_sharing_grant_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `recipient_reference_ciphertext` | `bytea` | NOT NULL; teacher/account external reference. |
| `recipient_lookup_hash` | `char(64)` | NOT NULL; lookup không lộ identifier. |
| `class_reference_ciphertext` | `bytea` | NULL. |
| `scope` | `jsonb` | NOT NULL; allowlisted `progress`, `srs_summary`, v.v.; raw chat/recording default excluded. |
| `consent_event_id` | `uuid` | NOT NULL FK → `consent_events.consent_event_id`; phải là grant hiện hành `TEACHER_SHARING`. |
| `status` | `varchar(16)` | NOT NULL: `ACTIVE`, `REVOKED`, `EXPIRED`. |
| `granted_at` | `timestamptz` | NOT NULL. |
| `expires_at` | `timestamptz` | NOT NULL. |
| `revoked_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |

Indexes: `(user_id, status, expires_at)`, `(recipient_lookup_hash, status)`. Withdrawal consent revokes all active grants atomically.

### 11.4 `support_access_grants`

Ticket-bound, time-limited break-glass. `ADMIN` không dùng bảng này để truy cập learner private data.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `support_access_grant_id` | `uuid` | PK. |
| `subject_user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `support_principal_reference` | `varchar(128)` | NOT NULL; identity internal, không phải product role. |
| `ticket_reference` | `varchar(128)` | NOT NULL. |
| `access_level` | `varchar(24)` | NOT NULL: `MASKED`, `UNMASKED_BREAK_GLASS`. |
| `allowed_data_scope` | `jsonb` | NOT NULL; allowlisted categories, no blanket access. |
| `reason_ciphertext` | `bytea` | NOT NULL. |
| `mfa_verified_at` | `timestamptz` | NULL; mandatory khi unmasked. |
| `granted_at` | `timestamptz` | NOT NULL. |
| `expires_at` | `timestamptz` | NOT NULL. |
| `revoked_at` | `timestamptz` | NULL. |
| `granted_by_principal_reference` | `varchar(128)` | NOT NULL. |
| `created_at` | `timestamptz` | NOT NULL. |

Indexes: `(subject_user_id, expires_at)`, `(support_principal_reference, expires_at)`. CHECK `expires_at > granted_at`; unmasked requires `mfa_verified_at IS NOT NULL`; all reads require active grant + audit event.

### 11.5 `privacy_access_audit_events`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `privacy_access_audit_event_id` | `uuid` | PK. |
| `occurred_at` | `timestamptz` | NOT NULL. |
| `actor_category` | `varchar(24)` | NOT NULL: `LEARNER`, `TEACHER`, `SUPPORT`, `PRIVACY`, `AI_VENDOR`, `SYSTEM`. |
| `actor_reference` | `varchar(128)` | NOT NULL. |
| `subject_user_id` | `uuid` | NULL FK → `users.user_id`, `ON DELETE SET NULL`. |
| `subject_pseudonym` | `char(64)` | NULL. |
| `action` | `varchar(48)` | NOT NULL: `VIEW_MASKED`, `BREAK_GLASS_VIEW`, `EXPORT_BUILT`, `VENDOR_DISCLOSURE`, ... |
| `data_category` | `varchar(48)` | NOT NULL. |
| `resource_type` | `varchar(32)` | NULL. |
| `resource_id` | `uuid` | NULL. |
| `support_access_grant_id` | `uuid` | NULL FK → `support_access_grants.support_access_grant_id`. |
| `teacher_sharing_grant_id` | `uuid` | NULL FK → `teacher_sharing_grants.teacher_sharing_grant_id`. |
| `correlation_id` | `uuid` | NOT NULL. |
| `details` | `jsonb` | NULL; safe metadata only. |

Indexes: `(subject_user_id, occurred_at DESC)`, `(actor_reference, occurred_at DESC)`, `correlation_id`. Append-only, retention security audit 12 months trừ legal hold.

### 11.6 `account_deletion_requests`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `account_deletion_request_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `status` | `varchar(32)` | NOT NULL; `deletion_status`. |
| `recent_reauthentication_at` | `timestamptz` | NOT NULL. |
| `requested_at` | `timestamptz` | NOT NULL. |
| `cancellation_window_ends_at` | `timestamptz` | NOT NULL; exactly request + 30 days. |
| `canceled_at` | `timestamptz` | NULL. |
| `eligible_at` | `timestamptz` | NULL. |
| `completed_at` | `timestamptz` | NULL. |
| `failure_code` | `varchar(64)` | NULL. |
| `correlation_id` | `uuid` | NOT NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Ràng buộc: UNIQUE partial `user_id WHERE status IN ('PENDING','ELIGIBLE','RUNNING','BLOCKED_LEGAL_HOLD')`; request phải revoke sessions và đặt `users.status = PENDING_DELETION` transactionally. Cancel chỉ hợp lệ trước window end và không có completed crypto-erasure.

### 11.7 `data_export_requests`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `data_export_request_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `status` | `varchar(16)` | NOT NULL; `export_status`. |
| `recent_reauthentication_at` | `timestamptz` | NOT NULL. |
| `requested_at` | `timestamptz` | NOT NULL. |
| `started_at` | `timestamptz` | NULL. |
| `completed_at` | `timestamptz` | NULL. |
| `manifest_ciphertext` | `bytea` | NULL; content list, not a public payload. |
| `storage_provider` | `varchar(64)` | NULL. |
| `object_key_ciphertext` | `bytea` | NULL; encrypted ZIP object reference. |
| `encryption_key_reference` | `varchar(255)` | NULL; KMS envelope key reference. |
| `download_token_hash` | `char(64)` | NULL, UNIQUE; token thô chỉ xuất hiện trong one-time signed link. |
| `download_link_expires_at` | `timestamptz` | NULL; <= completed + 24h. |
| `downloaded_at` | `timestamptz` | NULL; first download makes token unusable. |
| `object_expires_at` | `timestamptz` | NULL; <= completed + 72h. |
| `failed_at` | `timestamptz` | NULL. |
| `failure_code` | `varchar(64)` | NULL. |
| `correlation_id` | `uuid` | NOT NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes: `(user_id, requested_at DESC)`, `(status, object_expires_at)`. Export worker and download both verify owner + live session + recent re-auth; no attachment email.

### 11.8 `retention_notices`

Idempotency/evidence cho notice 30 và 7 ngày; không bị marketing preference chặn.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `retention_notice_id` | `uuid` | PK. |
| `user_id` | `uuid` | NOT NULL FK → `users.user_id`. |
| `notice_type` | `varchar(40)` | NOT NULL: `INACTIVITY_30_DAYS`, `INACTIVITY_7_DAYS`, `CHAT_INACTIVITY_30_DAYS`. |
| `related_resource_type` | `varchar(32)` | NOT NULL: `ACCOUNT`, `AI_CONVERSATION`, `RECORDING`. |
| `related_resource_id` | `uuid` | NULL. |
| `scheduled_for` | `timestamptz` | NOT NULL. |
| `sent_at` | `timestamptz` | NULL. |
| `canceled_at` | `timestamptz` | NULL; activity/keep-account cancels. |
| `delivery_status` | `varchar(16)` | NOT NULL: `PENDING`, `SENT`, `FAILED`, `CANCELED`. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |

Ràng buộc/index: UNIQUE `(user_id, notice_type, related_resource_type, related_resource_id, scheduled_for)`; `(delivery_status, scheduled_for)`.

### 11.9 `legal_holds`

Metadata của legal/security hold đã phê duyệt; không dùng cờ tùy ý trên user để giữ data vô thời hạn.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `legal_hold_id` | `uuid` | PK. |
| `hold_type` | `varchar(32)` | NOT NULL: `LEGAL`, `SECURITY`. |
| `legal_basis_reference_ciphertext` | `bytea` | NOT NULL. |
| `description_ciphertext` | `bytea` | NOT NULL. |
| `status` | `varchar(16)` | NOT NULL: `ACTIVE`, `RELEASED`, `EXPIRED`. |
| `approved_by_principal_reference` | `varchar(128)` | NOT NULL. |
| `starts_at` | `timestamptz` | NOT NULL. |
| `ends_at` | `timestamptz` | NOT NULL; fixed documented expiry. |
| `released_at` | `timestamptz` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |

Indexes: `(status, ends_at)`. Bất kỳ hold active phải nằm ở isolated restricted storage, loại trừ learning/AI improvement/marketing reuse.

### 11.10 `legal_hold_scopes`

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `legal_hold_scope_id` | `uuid` | PK. |
| `legal_hold_id` | `uuid` | NOT NULL FK → `legal_holds.legal_hold_id`. |
| `subject_user_id` | `uuid` | NULL FK → `users.user_id`. |
| `data_category` | `varchar(48)` | NOT NULL: `PROFILE`, `CHAT`, `RECORDING`, `LEARNING_HISTORY`, `AUDIT`, ... |
| `resource_type` | `varchar(32)` | NULL. |
| `resource_id` | `uuid` | NULL; required nếu hold chỉ một resource. |
| `created_at` | `timestamptz` | NOT NULL. |

Ràng buộc: UNIQUE `(legal_hold_id, subject_user_id, data_category, resource_type, resource_id)`; index `(subject_user_id, data_category)`. Deletion orchestrator phải check scope trước mọi physical delete/key destroy.

### 11.11 `deletion_jobs`

Work queue durable, idempotent cho hard deletion/crypto-erasure; không cần Kafka/Redis.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `deletion_job_id` | `uuid` | PK. |
| `owner_user_id` | `uuid` | NULL FK → `users.user_id`; NULL sau user purge nếu cần giữ operational audit. |
| `target_type` | `varchar(32)` | NOT NULL: `ACCOUNT`, `AI_CONVERSATION`, `RECORDING`, `DICTATION_ATTEMPT`, `SHADOWING_ATTEMPT`, `SAVED_WORD`. |
| `target_id` | `uuid` | NOT NULL. |
| `reason` | `varchar(48)` | NOT NULL: `LEARNER_REQUEST`, `RETENTION_EXPIRY`, `CONSENT_WITHDRAWN`, `ACCOUNT_DELETION`. |
| `status` | `varchar(32)` | NOT NULL; `deletion_status`. |
| `idempotency_key` | `varchar(128)` | NOT NULL, UNIQUE. |
| `scheduled_at` | `timestamptz` | NOT NULL. |
| `started_at` | `timestamptz` | NULL. |
| `completed_at` | `timestamptz` | NULL. |
| `attempt_count` | `integer` | NOT NULL default `0`. |
| `next_retry_at` | `timestamptz` | NULL. |
| `last_failure_code` | `varchar(64)` | NULL. |
| `correlation_id` | `uuid` | NOT NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |
| `version` | `bigint` | NOT NULL default `0`. |

Indexes: `(status, scheduled_at)`, `(owner_user_id, status)`, `(target_type, target_id)`. Worker có thể claim bằng lock; resource phải hidden trước khi job `RUNNING`.

### 11.12 `deletion_job_steps`

Theo dõi hoàn tất từng primary/secondary store để SLA có thể audit/retry.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `deletion_job_step_id` | `uuid` | PK. |
| `deletion_job_id` | `uuid` | NOT NULL FK → `deletion_jobs.deletion_job_id`. |
| `store_type` | `varchar(32)` | NOT NULL: `POSTGRESQL`, `OBJECT_STORAGE`, `SEARCH_INDEX`, `CACHE`, `CDN`, `AI_VENDOR`, `KMS`. |
| `action` | `varchar(48)` | NOT NULL: `DELETE_ROWS`, `DELETE_OBJECTS`, `PURGE`, `DELETE_VENDOR_COPY`, `DESTROY_KEY`. |
| `status` | `varchar(16)` | NOT NULL: `PENDING`, `RUNNING`, `COMPLETED`, `RETRYING`, `FAILED`, `NOT_APPLICABLE`. |
| `external_reference_ciphertext` | `bytea` | NULL; external operation reference, encrypted. |
| `started_at` | `timestamptz` | NULL. |
| `completed_at` | `timestamptz` | NULL. |
| `attempt_count` | `integer` | NOT NULL default `0`. |
| `last_failure_code` | `varchar(64)` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |

Ràng buộc: UNIQUE `(deletion_job_id, store_type, action)`; index `(status, updated_at)`. Account job chỉ `COMPLETED` khi all required steps completed/not applicable; crypto-erasure step phải audit thời điểm key `DESTROYED`.

## 12. Giao nhận thông báo và analytics tách biệt

### 12.1 `notification_deliveries`

Outbox/delivery ledger cho verification, reset, retention notice; không ghi nội dung email có PII.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `notification_delivery_id` | `uuid` | PK. |
| `user_id` | `uuid` | NULL FK → `users.user_id`, `ON DELETE SET NULL`. |
| `notification_type` | `varchar(48)` | NOT NULL: `EMAIL_VERIFICATION`, `PASSWORD_RESET`, `RETENTION_NOTICE`, `SECURITY_ALERT`, `ENTITLEMENT_CHANGED`. |
| `channel` | `varchar(16)` | NOT NULL: `EMAIL`, `IN_APP`. |
| `template_version` | `varchar(64)` | NOT NULL. |
| `related_resource_type` | `varchar(32)` | NULL. |
| `related_resource_id` | `uuid` | NULL. |
| `scheduled_at` | `timestamptz` | NOT NULL. |
| `sent_at` | `timestamptz` | NULL. |
| `status` | `varchar(16)` | NOT NULL: `PENDING`, `SENDING`, `SENT`, `FAILED`, `CANCELED`. |
| `provider_message_reference` | `varchar(128)` | NULL. |
| `attempt_count` | `integer` | NOT NULL default `0`. |
| `next_retry_at` | `timestamptz` | NULL. |
| `last_failure_code` | `varchar(64)` | NULL. |
| `created_at` | `timestamptz` | NOT NULL. |
| `updated_at` | `timestamptz` | NOT NULL. |

Indexes: `(status, scheduled_at)`, `(user_id, created_at DESC)`, `(related_resource_type, related_resource_id)`. Destination email được resolve/decrypt tại send-time; không duplicate email plaintext ở đây.

### 12.2 `analytics_events` (schema logic `analytics`)

Store tách biệt về quyền/connection role với transactional app schema. Chỉ pseudonymous event-level data, không FK trực tiếp `users`.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `analytics_event_id` | `uuid` | PK. |
| `pseudonymous_user_id` | `char(64)` | NULL; rotating/HMAC pseudonym, không thể dùng UI để reverse. |
| `anonymous_session_id` | `uuid` | NULL; Guest event. |
| `event_name` | `varchar(64)` | NOT NULL. |
| `event_version` | `smallint` | NOT NULL default `1`. |
| `occurred_at` | `timestamptz` | NOT NULL. |
| `app_surface` | `varchar(32)` | NOT NULL: `WEB`, `MOBILE`. |
| `properties` | `jsonb` | NULL; schema allowlist, cấm email/name/chat/transcript/audio/recording key. |
| `retention_expires_at` | `timestamptz` | NOT NULL; event-level <= 12 months. |
| `created_at` | `timestamptz` | NOT NULL. |

Indexes/partition: range partition theo `occurred_at`; `(pseudonymous_user_id, occurred_at DESC)`, `(event_name, occurred_at DESC)`. Analytics staff least privilege, no Teacher/Admin.

### 12.3 `analytics_daily_aggregates` (schema logic `analytics`)

Chỉ tạo sau khi k-anonymity/threshold đã được data/privacy policy phê duyệt; không chứa user ID hay segment có thể đảo ngược.

| Cột | Kiểu | Ràng buộc / ý nghĩa |
| --- | --- | --- |
| `analytics_daily_aggregate_id` | `uuid` | PK. |
| `metric_date` | `date` | NOT NULL. |
| `metric_name` | `varchar(64)` | NOT NULL. |
| `dimension_key` | `varchar(64)` | NOT NULL default `GLOBAL`. |
| `dimension_value` | `varchar(128)` | NOT NULL default `ALL`. |
| `metric_value` | `numeric(18,4)` | NOT NULL. |
| `source_event_count` | `bigint` | NOT NULL. |
| `anonymization_version` | `varchar(32)` | NOT NULL. |
| `computed_at` | `timestamptz` | NOT NULL. |
| `created_at` | `timestamptz` | NOT NULL. |

Ràng buộc: UNIQUE `(metric_date, metric_name, dimension_key, dimension_value, anonymization_version)`. Có thể giữ vô thời hạn chỉ khi irreversibly anonymized; aggregate không được tái dùng marketing targeting.

## 13. Quy tắc FK, xóa dữ liệu, index và migration

### 13.1 Chính sách FK/xóa

| Quan hệ | Chính sách |
| --- | --- |
| Shared content → learner data | Không hard-delete topic/lesson/segment/dictionary entry đã được learner tham chiếu; dùng `ARCHIVED`/`UNPUBLISHED`. |
| User-owned child data | Không dựa vào `ON DELETE CASCADE` của một account delete. Deletion Orchestrator xóa theo thứ tự, xóa object/index/cache/vendor copy và hủy key trước/sát physical delete. |
| Audit/consent evidence → user | FK nullable `ON DELETE SET NULL` + `subject_pseudonym` để giữ evidence tối thiểu đúng thời hạn. |
| User roles/sessions/tokens | Revoke trước; rows giữ theo retention security để detect reuse/điều tra, sau đó expire. |
| Recording → storage/key | DB row không phải bằng chứng object đã bị xóa; `deletion_job_steps` phải xác nhận object version, derivative, cache/CDN và `DESTROY_KEY`. |
| Polymorphic `resource_type/resource_id` | Không tạo FK giả. Service allowlist resource type, xác thực existence/ownership, và test từng route. |

### 13.2 Index bắt buộc xuyên hệ thống

- Tất cả FK có B-tree index; các list endpoint dùng index bắt đầu bằng `user_id` và cột filter/sort (`created_at`, `due_at`, `last_activity_at`).
- Catalog public: `lessons(publication_state, access_level, hsk_level)`, `topics(publication_state, sort_order)`, `segments(lesson_id, publication_state, sort_order)`.
- Privacy jobs: `recordings(classification, expires_at)`, `deletion_jobs(status, scheduled_at)`, `data_export_requests(status, object_expires_at)`, `retention_notices(delivery_status, scheduled_for)`, `legal_holds(status, ends_at)`.
- Auth security: unique email/token hashes, active sessions by `(user_id, revoked_at)`, `refresh_tokens(token_hash)`, `refresh_idempotency(session_id, refresh_request_id)`.
- Search: PostgreSQL full-text/trigram indexes chỉ trên **shared content**. Private chat/transcript chỉ có encrypted search index riêng nếu feature được legal/security phê duyệt; xóa phải purge index.

### 13.3 Partition, retention và access database

- Partition theo thời gian cho `auth_audit_events`, `privacy_access_audit_events` và `analytics.analytics_events`; job verify/xóa partition theo retention matrix (audit/event 12 tháng, backup tối đa 35 ngày ở store khác).
- `consent_events` giữ 5 năm sau withdrawal/account deletion theo legal approval; private learning/chats/recordings/progress theo baseline 12 tháng inactivity hoặc retention kỹ thuật ngắn hơn đã nêu.
- Chỉ service/repository dùng DB application role. Tách ít nhất logical schema và role cho `app`, `analytics`, `audit/compliance`, migration worker; Admin UI không có DB credential và `ADMIN` role không tương đương database role.
- Flyway migration tạo constraint/index/partition cần thiết trong một migration schema đã phê duyệt, có test clean database. Không sửa migration đã chạy trên shared/production và không tạo migration cho thử nghiệm/non-schema.

## 14. Trình tự triển khai khuyến nghị

1. Migration nền: `users`, profile/key, roles, auth/session/token/audit, plan/entitlement/quota.
2. Catalog: topics, lessons, media assets, segments, lifecycle audit; sau đó API authorization/capability tests.
3. Learning core: dictation, recordings/shadowing, dictionary/SRS, progress service và indexes phục vụ dashboard.
4. AI Buddy: scenario/conversation/message cùng quota ledger và provider minimization/audit.
5. Privacy/operations: consent, export/deletion/retention/legal-hold, notification delivery, analytics schema riêng.

Mỗi nhóm trên chỉ được chuyển thành Flyway migration sau khi feature spec tương ứng được phê duyệt; kiểm thử bắt buộc gồm ownership, `ADMIN`, Free/Premium, AI quota cạnh tranh, refresh-token reuse, và toàn bộ đường xóa/crypto-erasure.
