-- =============================================================================
-- Pchinese — Database MVP gọn
-- Flyway migration: V1__pchinese_mvp_schema.sql
--
-- Canonical MVP schema contract (24 tables) for:
--   auth/registration, ADMIN role, Free/Premium + AI quota, content catalog,
--   Dictation, Shadowing, dictionary/SRS, progress and AI Buddy.
--
-- Target: PostgreSQL 18 + Spring Data JPA (no raw SQL in application code).
-- Conventions: snake_case, plural table names, uuid primary keys,
--              timestamptz (UTC), varchar + CHECK for Java-enum-backed state,
--              *_ciphertext = bytea (application-level encryption),
--              *_hash = char(64) HMAC/SHA-256, optimistic locking via
--              `version bigint not null default 0`.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. AUTH / ACCOUNT TABLES
-- -----------------------------------------------------------------------------

-- 1.1 users --------------------------------------------------------------------
CREATE TABLE users (
    user_id                    uuid            NOT NULL,
    email_ciphertext           bytea           NOT NULL,
    email_lookup_hash          char(64)        NOT NULL,
    password_hash              varchar(255)    NOT NULL,
    status                     varchar(32)     NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified_at          timestamptz     NULL,
    display_name_ciphertext    bytea           NULL,
    native_language_code       varchar(10)     NOT NULL DEFAULT 'vi',
    interface_locale           varchar(16)     NOT NULL DEFAULT 'vi-VN',
    time_zone                  varchar(64)     NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    target_hsk_level           smallint        NULL,
    daily_goal_minutes         smallint        NOT NULL DEFAULT 15,
    authz_version              bigint          NOT NULL DEFAULT 1,
    kms_key_reference          varchar(255)    NULL,
    last_activity_at           timestamptz     NOT NULL,
    created_at                 timestamptz     NOT NULL,
    updated_at                 timestamptz     NOT NULL,
    version                    bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_users_email_lookup_hash UNIQUE (email_lookup_hash),
    CONSTRAINT ck_users_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT ck_users_target_hsk_level CHECK (target_hsk_level IS NULL OR target_hsk_level BETWEEN 1 AND 6),
    CONSTRAINT ck_users_daily_goal_minutes CHECK (daily_goal_minutes > 0)
);

CREATE INDEX ix_users_status_last_activity ON users (status, last_activity_at);

-- 1.2 user_roles -----------------------------------------------------------------
CREATE TABLE user_roles (
    user_role_grant_id         uuid            NOT NULL,
    user_id                    uuid            NOT NULL,
    role_code                  varchar(32)     NOT NULL,
    granted_by_user_id         uuid            NULL,
    granted_at                 timestamptz     NOT NULL,
    revoked_at                 timestamptz     NULL,
    correlation_id             uuid            NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_role_grant_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_user_roles_granted_by FOREIGN KEY (granted_by_user_id) REFERENCES users (user_id),
    CONSTRAINT ck_user_roles_role_code CHECK (role_code IN ('ADMIN'))
);

CREATE UNIQUE INDEX uq_user_roles_active_grant
    ON user_roles (user_id, role_code)
    WHERE revoked_at IS NULL;

-- 1.3 auth_sessions ----------------------------------------------------------------
CREATE TABLE auth_sessions (
    session_id                 uuid            NOT NULL,
    user_id                    uuid            NOT NULL,
    family_id                  uuid            NOT NULL,
    authz_version              bigint          NOT NULL,
    device_id                  varchar(128)    NOT NULL,
    device_label               varchar(120)    NOT NULL,
    platform                   varchar(24)     NOT NULL,
    ip_hash                    char(64)        NULL,
    created_at                 timestamptz     NOT NULL,
    last_seen_at                timestamptz     NOT NULL,
    idle_expires_at            timestamptz     NOT NULL,
    absolute_expires_at        timestamptz     NOT NULL,
    revoked_at                 timestamptz     NULL,
    revoked_reason             varchar(64)     NULL,
    version                    bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_sessions PRIMARY KEY (session_id),
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT ck_auth_sessions_platform CHECK (platform IN ('WEB', 'IOS', 'ANDROID'))
);

CREATE INDEX ix_auth_sessions_user_revoked ON auth_sessions (user_id, revoked_at);
CREATE INDEX ix_auth_sessions_family ON auth_sessions (family_id);

-- 1.4 refresh_tokens -----------------------------------------------------------------
CREATE TABLE refresh_tokens (
    refresh_token_id           uuid            NOT NULL,
    session_id                 uuid            NOT NULL,
    family_id                  uuid            NOT NULL,
    token_hash                 char(64)        NOT NULL,
    issued_at                  timestamptz     NOT NULL,
    expires_at                 timestamptz     NOT NULL,
    rotated_at                 timestamptz     NULL,
    replaced_by_token_id       uuid            NULL,
    revoked_at                 timestamptz     NULL,
    revoked_reason             varchar(64)     NULL,
    created_at                 timestamptz     NOT NULL,
    version                    bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (refresh_token_id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_session FOREIGN KEY (session_id) REFERENCES auth_sessions (session_id),
    CONSTRAINT fk_refresh_tokens_replaced_by FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens (refresh_token_id)
);

CREATE INDEX ix_refresh_tokens_session ON refresh_tokens (session_id);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);

-- 1.5 auth_action_tokens ---------------------------------------------------------------
CREATE TABLE auth_action_tokens (
    auth_action_token_id       uuid            NOT NULL,
    user_id                    uuid            NOT NULL,
    purpose                    varchar(32)     NOT NULL,
    token_hash                 char(64)        NOT NULL,
    issued_at                  timestamptz     NOT NULL,
    expires_at                 timestamptz     NOT NULL,
    consumed_at                timestamptz     NULL,
    invalidated_at             timestamptz     NULL,
    request_ip_hash            char(64)        NULL,
    created_at                 timestamptz     NOT NULL,
    CONSTRAINT pk_auth_action_tokens PRIMARY KEY (auth_action_token_id),
    CONSTRAINT uq_auth_action_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_auth_action_tokens_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT ck_auth_action_tokens_purpose CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

CREATE INDEX ix_auth_action_tokens_user_purpose_expires
    ON auth_action_tokens (user_id, purpose, expires_at);

-- 1.6 refresh_idempotency ----------------------------------------------------------
CREATE TABLE refresh_idempotency (
    refresh_idempotency_id     uuid            NOT NULL,
    session_id                 uuid            NOT NULL,
    source_refresh_token_id    uuid            NOT NULL,
    refresh_request_id         uuid            NOT NULL,
    response_ciphertext        bytea           NOT NULL,
    expires_at                 timestamptz     NOT NULL,
    created_at                 timestamptz     NOT NULL,
    CONSTRAINT pk_refresh_idempotency PRIMARY KEY (refresh_idempotency_id),
    CONSTRAINT fk_refresh_idempotency_session FOREIGN KEY (session_id) REFERENCES auth_sessions (session_id),
    CONSTRAINT fk_refresh_idempotency_token FOREIGN KEY (source_refresh_token_id) REFERENCES refresh_tokens (refresh_token_id),
    CONSTRAINT uq_refresh_idempotency_session_request UNIQUE (session_id, refresh_request_id)
);

-- 1.7 auth_audit_events -------------------------------------------------------------
CREATE TABLE auth_audit_events (
    auth_audit_event_id        uuid            NOT NULL,
    event_type                 varchar(64)     NOT NULL,
    actor_user_id               uuid            NULL,
    target_user_id             uuid            NULL,
    session_id                 uuid            NULL,
    before_roles                jsonb           NULL,
    after_roles                 jsonb           NULL,
    correlation_id             uuid            NOT NULL,
    details                    jsonb           NULL,
    occurred_at                timestamptz     NOT NULL,
    CONSTRAINT pk_auth_audit_events PRIMARY KEY (auth_audit_event_id),
    CONSTRAINT fk_auth_audit_events_actor FOREIGN KEY (actor_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_auth_audit_events_target FOREIGN KEY (target_user_id) REFERENCES users (user_id)
);

CREATE INDEX ix_auth_audit_events_target_occurred ON auth_audit_events (target_user_id, occurred_at DESC);
CREATE INDEX ix_auth_audit_events_type_occurred ON auth_audit_events (event_type, occurred_at DESC);
CREATE INDEX ix_auth_audit_events_correlation ON auth_audit_events (correlation_id);

-- -----------------------------------------------------------------------------
-- 2. PREMIUM / AI QUOTA TABLES
-- -----------------------------------------------------------------------------

-- 2.1 subscription_plans -----------------------------------------------------------
CREATE TABLE subscription_plans (
    subscription_plan_id       uuid            NOT NULL,
    plan_code                  varchar(32)     NOT NULL,
    display_name               varchar(100)    NOT NULL,
    max_active_sessions        smallint        NOT NULL,
    ai_quota_units             integer         NOT NULL,
    ai_quota_period            varchar(16)     NOT NULL,
    status                     varchar(16)     NOT NULL,
    created_at                 timestamptz     NOT NULL,
    updated_at                 timestamptz     NOT NULL,
    CONSTRAINT pk_subscription_plans PRIMARY KEY (subscription_plan_id),
    CONSTRAINT uq_subscription_plans_plan_code UNIQUE (plan_code),
    CONSTRAINT ck_subscription_plans_plan_code CHECK (plan_code IN ('FREE', 'PREMIUM')),
    CONSTRAINT ck_subscription_plans_ai_quota_units CHECK (ai_quota_units >= 0),
    CONSTRAINT ck_subscription_plans_ai_quota_period CHECK (ai_quota_period IN ('DAY', 'MONTH')),
    CONSTRAINT ck_subscription_plans_status CHECK (status IN ('ACTIVE', 'RETIRED'))
);

-- Seed data: FREE plan is ACTIVE with 30 units / 30-day rolling cycle.
-- PREMIUM is reserved structure only; not activated in MVP.
INSERT INTO subscription_plans (
    subscription_plan_id, plan_code, display_name, max_active_sessions,
    ai_quota_units, ai_quota_period, status, created_at, updated_at
) VALUES
    (gen_random_uuid(), 'FREE', 'Free', 2, 30, 'MONTH', 'ACTIVE', now(), now()),
    (gen_random_uuid(), 'PREMIUM', 'Premium', 5, 0, 'MONTH', 'RETIRED', now(), now());

-- 2.2 user_entitlements ------------------------------------------------------------
CREATE TABLE user_entitlements (
    user_entitlement_id            uuid            NOT NULL,
    user_id                        uuid            NOT NULL,
    subscription_plan_id           uuid            NOT NULL,
    status                         varchar(24)     NOT NULL,
    source_type                    varchar(32)     NOT NULL,
    external_reference_ciphertext  bytea           NULL,
    starts_at                      timestamptz     NOT NULL,
    ends_at                        timestamptz     NULL,
    ai_quota_period_started_at     timestamptz     NOT NULL,
    ai_used_units                  integer         NOT NULL DEFAULT 0,
    created_at                     timestamptz     NOT NULL,
    updated_at                     timestamptz     NOT NULL,
    version                        bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_user_entitlements PRIMARY KEY (user_entitlement_id),
    CONSTRAINT fk_user_entitlements_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_user_entitlements_plan FOREIGN KEY (subscription_plan_id) REFERENCES subscription_plans (subscription_plan_id),
    CONSTRAINT uq_user_entitlements_id_user UNIQUE (user_entitlement_id, user_id),
    CONSTRAINT ck_user_entitlements_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELED', 'REVOKED')),
    CONSTRAINT ck_user_entitlements_source_type CHECK (source_type IN ('DEFAULT', 'BILLING')),
    CONSTRAINT ck_user_entitlements_ai_used_units CHECK (ai_used_units >= 0),
    CONSTRAINT ck_user_entitlements_ends_at CHECK (ends_at IS NULL OR ends_at > starts_at)
);

CREATE UNIQUE INDEX uq_user_entitlements_active_per_user
    ON user_entitlements (user_id)
    WHERE status = 'ACTIVE';

CREATE INDEX ix_user_entitlements_user_status_dates
    ON user_entitlements (user_id, status, starts_at, ends_at);

-- 2.3 ai_usage_events ---------------------------------------------------------------
CREATE TABLE ai_usage_events (
    ai_usage_event_id               uuid            NOT NULL,
    user_entitlement_id             uuid            NOT NULL,
    user_id                         uuid            NOT NULL,
    client_request_id               uuid            NOT NULL,
    request_fingerprint_hash        char(64)        NOT NULL,
    feature_type                    varchar(32)     NOT NULL,
    requested_units                 smallint        NOT NULL DEFAULT 1,
    status                          varchar(24)     NOT NULL,
    ai_service_request_reference    varchar(128)    NULL,
    failure_code                    varchar(64)     NULL,
    created_at                      timestamptz     NOT NULL,
    completed_at                    timestamptz     NULL,
    CONSTRAINT pk_ai_usage_events PRIMARY KEY (ai_usage_event_id),
    CONSTRAINT fk_ai_usage_events_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_ai_usage_events_entitlement FOREIGN KEY (user_entitlement_id, user_id)
        REFERENCES user_entitlements (user_entitlement_id, user_id),
    CONSTRAINT uq_ai_usage_events_user_client_request UNIQUE (user_id, client_request_id),
    CONSTRAINT uq_ai_usage_events_id_user UNIQUE (ai_usage_event_id, user_id),
    CONSTRAINT ck_ai_usage_events_feature_type CHECK (feature_type IN ('AI_BUDDY', 'SHADOWING_ASSESSMENT')),
    CONSTRAINT ck_ai_usage_events_status CHECK (status IN ('RESERVED', 'SUCCEEDED', 'FAILED_REFUNDED', 'FAILED_CONSUMED')),
    CONSTRAINT ck_ai_usage_events_requested_units CHECK (requested_units > 0)
);

CREATE INDEX ix_ai_usage_events_entitlement_status_created
    ON ai_usage_events (user_entitlement_id, status, created_at DESC);
CREATE INDEX ix_ai_usage_events_user_created
    ON ai_usage_events (user_id, created_at DESC);

-- -----------------------------------------------------------------------------
-- 3. CONTENT CATALOG TABLES
-- -----------------------------------------------------------------------------

-- 3.1 topics --------------------------------------------------------------------
CREATE TABLE topics (
    topic_id                   uuid            NOT NULL,
    slug                       varchar(160)    NOT NULL,
    title                      varchar(255)    NOT NULL,
    description                text            NULL,
    hsk_level                  smallint        NULL,
    sort_order                 integer         NOT NULL DEFAULT 0,
    publication_state          varchar(16)     NOT NULL,
    created_by_user_id         uuid            NOT NULL,
    updated_by_user_id         uuid            NOT NULL,
    published_at               timestamptz     NULL,
    created_at                 timestamptz     NOT NULL,
    updated_at                 timestamptz     NOT NULL,
    version                    bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_topics PRIMARY KEY (topic_id),
    CONSTRAINT uq_topics_slug UNIQUE (slug),
    CONSTRAINT fk_topics_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_topics_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES users (user_id),
    CONSTRAINT ck_topics_hsk_level CHECK (hsk_level IS NULL OR hsk_level BETWEEN 1 AND 6),
    CONSTRAINT ck_topics_publication_state CHECK (publication_state IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'ARCHIVED'))
);

CREATE INDEX ix_topics_sort_order_title ON topics (sort_order, title);

-- 3.2 media_assets ---------------------------------------------------------------
CREATE TABLE media_assets (
    media_asset_id              uuid            NOT NULL,
    provider_name                varchar(64)     NOT NULL,
    provider_asset_identifier    varchar(255)    NOT NULL,
    media_kind                   varchar(16)     NOT NULL,
    mime_type                    varchar(127)    NULL,
    duration_milliseconds        integer         NULL,
    approval_status               varchar(24)     NOT NULL,
    malware_scan_status          varchar(24)     NOT NULL,
    approved_by_user_id          uuid            NULL,
    approved_at                  timestamptz     NULL,
    created_by_user_id           uuid            NOT NULL,
    created_at                   timestamptz     NOT NULL,
    updated_at                   timestamptz     NOT NULL,
    version                      bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_media_assets PRIMARY KEY (media_asset_id),
    CONSTRAINT uq_media_assets_provider UNIQUE (provider_name, provider_asset_identifier),
    CONSTRAINT fk_media_assets_approved_by FOREIGN KEY (approved_by_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_media_assets_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (user_id),
    CONSTRAINT ck_media_assets_media_kind CHECK (media_kind IN ('AUDIO', 'VIDEO', 'IMAGE')),
    CONSTRAINT ck_media_assets_approval_status CHECK (approval_status IN ('PENDING_SCAN', 'APPROVED', 'REJECTED', 'QUARANTINED')),
    CONSTRAINT ck_media_assets_duration CHECK (duration_milliseconds IS NULL OR duration_milliseconds >= 0)
);

-- 3.3 lessons -------------------------------------------------------------------
CREATE TABLE lessons (
    lesson_id                       uuid            NOT NULL,
    topic_id                        uuid            NOT NULL,
    slug                            varchar(160)    NOT NULL,
    title                           varchar(255)    NOT NULL,
    summary                         text            NULL,
    hsk_level                       smallint        NULL,
    lesson_type                     varchar(24)     NOT NULL,
    access_level                    varchar(16)     NOT NULL,
    publication_state               varchar(16)     NOT NULL,
    sort_order                      integer         NOT NULL DEFAULT 0,
    estimated_duration_seconds      integer         NOT NULL DEFAULT 0,
    completion_min_percent          smallint        NOT NULL DEFAULT 100,
    created_by_user_id              uuid            NOT NULL,
    updated_by_user_id              uuid            NOT NULL,
    published_at                    timestamptz     NULL,
    created_at                      timestamptz     NOT NULL,
    updated_at                      timestamptz     NOT NULL,
    version                         bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_lessons PRIMARY KEY (lesson_id),
    CONSTRAINT uq_lessons_slug UNIQUE (slug),
    CONSTRAINT uq_lessons_id_topic UNIQUE (lesson_id, topic_id),
    CONSTRAINT fk_lessons_topic FOREIGN KEY (topic_id) REFERENCES topics (topic_id),
    CONSTRAINT fk_lessons_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_lessons_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES users (user_id),
    CONSTRAINT ck_lessons_hsk_level CHECK (hsk_level IS NULL OR hsk_level BETWEEN 1 AND 6),
    CONSTRAINT ck_lessons_lesson_type CHECK (lesson_type IN ('AUDIO', 'VIDEO', 'MIXED')),
    CONSTRAINT ck_lessons_access_level CHECK (access_level IN ('FREE', 'PREMIUM')),
    CONSTRAINT ck_lessons_publication_state CHECK (publication_state IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_lessons_completion_min_percent CHECK (completion_min_percent BETWEEN 1 AND 100)
);

CREATE INDEX ix_lessons_topic_sort ON lessons (topic_id, sort_order);
CREATE INDEX ix_lessons_pub_access_hsk ON lessons (publication_state, access_level, hsk_level);

-- 3.4 segments ------------------------------------------------------------------
CREATE TABLE segments (
    segment_id                  uuid            NOT NULL,
    lesson_id                   uuid            NOT NULL,
    media_asset_id              uuid            NOT NULL,
    sequence_no                 integer         NOT NULL,
    segment_type                varchar(24)     NOT NULL,
    publication_state           varchar(16)     NOT NULL,
    start_milliseconds          integer         NOT NULL,
    end_milliseconds            integer         NOT NULL,
    transcript_hanzi            text            NOT NULL,
    transcript_pinyin           text            NULL,
    translation_vi              text            NULL,
    dictation_hint               text            NULL,
    created_by_user_id          uuid            NOT NULL,
    updated_by_user_id          uuid            NOT NULL,
    published_at                timestamptz     NULL,
    created_at                  timestamptz     NOT NULL,
    updated_at                  timestamptz     NOT NULL,
    version                     bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_segments PRIMARY KEY (segment_id),
    CONSTRAINT uq_segments_lesson_sequence UNIQUE (lesson_id, sequence_no),
    CONSTRAINT uq_segments_id_lesson UNIQUE (segment_id, lesson_id),
    CONSTRAINT fk_segments_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (lesson_id),
    CONSTRAINT fk_segments_media_asset FOREIGN KEY (media_asset_id) REFERENCES media_assets (media_asset_id),
    CONSTRAINT fk_segments_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_segments_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES users (user_id),
    CONSTRAINT ck_segments_sequence_no CHECK (sequence_no > 0),
    CONSTRAINT ck_segments_segment_type CHECK (segment_type IN ('DICTATION', 'SHADOWING', 'BOTH', 'PLAYBACK_ONLY')),
    CONSTRAINT ck_segments_publication_state CHECK (publication_state IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_segments_start_ms CHECK (start_milliseconds >= 0),
    CONSTRAINT ck_segments_end_after_start CHECK (end_milliseconds > start_milliseconds)
);

CREATE INDEX ix_segments_lesson_pub_sequence ON segments (lesson_id, publication_state, sequence_no);

-- -----------------------------------------------------------------------------
-- 4. LEARNING / VOCABULARY TABLES
-- -----------------------------------------------------------------------------

-- 4.1 dictionary_entries -----------------------------------------------------------
CREATE TABLE dictionary_entries (
    dictionary_entry_id         uuid            NOT NULL,
    simplified_hanzi            varchar(128)    NOT NULL,
    traditional_hanzi           varchar(128)    NULL,
    normalized_hanzi            varchar(128)    NOT NULL,
    primary_pinyin              varchar(255)    NOT NULL,
    normalized_pinyin           varchar(255)    NOT NULL,
    hsk_level                   smallint        NULL,
    word_type                   varchar(32)     NULL,
    senses                      jsonb           NOT NULL,
    audio_media_asset_id        uuid            NULL,
    image_media_asset_id        uuid            NULL,
    publication_state           varchar(16)     NOT NULL DEFAULT 'PUBLISHED',
    created_at                  timestamptz     NOT NULL,
    updated_at                  timestamptz     NOT NULL,
    version                     bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_dictionary_entries PRIMARY KEY (dictionary_entry_id),
    CONSTRAINT uq_dictionary_entries_normalized UNIQUE (normalized_hanzi, normalized_pinyin),
    CONSTRAINT fk_dictionary_entries_audio FOREIGN KEY (audio_media_asset_id) REFERENCES media_assets (media_asset_id),
    CONSTRAINT fk_dictionary_entries_image FOREIGN KEY (image_media_asset_id) REFERENCES media_assets (media_asset_id),
    CONSTRAINT ck_dictionary_entries_hsk_level CHECK (hsk_level IS NULL OR hsk_level BETWEEN 1 AND 6),
    CONSTRAINT ck_dictionary_entries_publication_state CHECK (publication_state IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'ARCHIVED'))
);

-- Trigram / full-text search support for dictionary lookup.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX ix_dictionary_entries_simplified_trgm
    ON dictionary_entries USING gin (simplified_hanzi gin_trgm_ops);
CREATE INDEX ix_dictionary_entries_traditional_trgm
    ON dictionary_entries USING gin (traditional_hanzi gin_trgm_ops);
CREATE INDEX ix_dictionary_entries_pinyin_trgm
    ON dictionary_entries USING gin (normalized_pinyin gin_trgm_ops);

-- 4.2 saved_words ---------------------------------------------------------------
CREATE TABLE saved_words (
    saved_word_id                uuid            NOT NULL,
    user_id                      uuid            NOT NULL,
    dictionary_entry_id          uuid            NOT NULL,
    personal_note_ciphertext     bytea           NULL,
    status                       varchar(16)     NOT NULL DEFAULT 'ACTIVE',
    saved_at                     timestamptz     NOT NULL,
    deleted_at                   timestamptz     NULL,
    created_at                   timestamptz     NOT NULL,
    updated_at                   timestamptz     NOT NULL,
    version                      bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_saved_words PRIMARY KEY (saved_word_id),
    CONSTRAINT uq_saved_words_id_user UNIQUE (saved_word_id, user_id),
    CONSTRAINT fk_saved_words_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_saved_words_dictionary_entry FOREIGN KEY (dictionary_entry_id) REFERENCES dictionary_entries (dictionary_entry_id),
    CONSTRAINT ck_saved_words_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);

CREATE UNIQUE INDEX uq_saved_words_active_per_user_entry
    ON saved_words (user_id, dictionary_entry_id)
    WHERE deleted_at IS NULL;

-- 4.3 srs_schedules --------------------------------------------------------------
CREATE TABLE srs_schedules (
    srs_schedule_id              uuid            NOT NULL,
    saved_word_id                uuid            NOT NULL,
    user_id                      uuid            NOT NULL,
    status                       varchar(16)     NOT NULL,
    due_at                       timestamptz     NOT NULL,
    interval_days                numeric(8,3)    NOT NULL DEFAULT 0,
    ease_factor                  numeric(5,3)    NOT NULL DEFAULT 2.500,
    repetitions                  integer         NOT NULL DEFAULT 0,
    lapses                       integer         NOT NULL DEFAULT 0,
    last_reviewed_at             timestamptz     NULL,
    created_at                   timestamptz     NOT NULL,
    updated_at                   timestamptz     NOT NULL,
    version                      bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_srs_schedules PRIMARY KEY (srs_schedule_id),
    CONSTRAINT uq_srs_schedules_saved_word UNIQUE (saved_word_id),
    CONSTRAINT uq_srs_schedules_id_user UNIQUE (srs_schedule_id, user_id),
    CONSTRAINT fk_srs_schedules_saved_word FOREIGN KEY (saved_word_id, user_id)
        REFERENCES saved_words (saved_word_id, user_id),
    CONSTRAINT fk_srs_schedules_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT ck_srs_schedules_status CHECK (status IN ('LEARNING', 'REVIEW', 'RELEARNING', 'SUSPENDED'))
);

CREATE INDEX ix_srs_schedules_user_status_due ON srs_schedules (user_id, status, due_at);

-- 4.4 srs_review_events ------------------------------------------------------------
CREATE TABLE srs_review_events (
    srs_review_event_id          uuid            NOT NULL,
    srs_schedule_id              uuid            NOT NULL,
    user_id                      uuid            NOT NULL,
    rating                       varchar(16)     NOT NULL,
    previous_due_at              timestamptz     NOT NULL,
    next_due_at                  timestamptz     NOT NULL,
    previous_interval_days       numeric(8,3)    NOT NULL,
    next_interval_days           numeric(8,3)    NOT NULL,
    reviewed_at                  timestamptz     NOT NULL,
    created_at                   timestamptz     NOT NULL,
    CONSTRAINT pk_srs_review_events PRIMARY KEY (srs_review_event_id),
    CONSTRAINT fk_srs_review_events_schedule FOREIGN KEY (srs_schedule_id, user_id)
        REFERENCES srs_schedules (srs_schedule_id, user_id),
    CONSTRAINT fk_srs_review_events_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT ck_srs_review_events_rating CHECK (rating IN ('AGAIN', 'HARD', 'GOOD', 'EASY'))
);

CREATE INDEX ix_srs_review_events_schedule_reviewed ON srs_review_events (srs_schedule_id, reviewed_at DESC);
CREATE INDEX ix_srs_review_events_user_reviewed ON srs_review_events (user_id, reviewed_at DESC);

-- 4.5 dictation_attempts ------------------------------------------------------------
CREATE TABLE dictation_attempts (
    dictation_attempt_id         uuid            NOT NULL,
    user_id                      uuid            NOT NULL,
    lesson_id                    uuid            NOT NULL,
    segment_id                   uuid            NOT NULL,
    status                       varchar(24)     NOT NULL,
    answer_ciphertext            bytea           NULL,
    overall_score                numeric(5,2)    NULL,
    accuracy_percent             numeric(5,2)    NULL,
    feedback_ciphertext          bytea           NULL,
    started_at                   timestamptz     NOT NULL,
    submitted_at                 timestamptz     NULL,
    evaluated_at                 timestamptz     NULL,
    deleted_at                   timestamptz     NULL,
    created_at                   timestamptz     NOT NULL,
    updated_at                   timestamptz     NOT NULL,
    version                      bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_dictation_attempts PRIMARY KEY (dictation_attempt_id),
    CONSTRAINT fk_dictation_attempts_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_dictation_attempts_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (lesson_id),
    CONSTRAINT fk_dictation_attempts_segment FOREIGN KEY (segment_id, lesson_id)
        REFERENCES segments (segment_id, lesson_id),
    CONSTRAINT ck_dictation_attempts_status CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'EVALUATED', 'FAILED', 'DELETED')),
    CONSTRAINT ck_dictation_attempts_overall_score CHECK (overall_score IS NULL OR overall_score BETWEEN 0 AND 100),
    CONSTRAINT ck_dictation_attempts_accuracy_percent CHECK (accuracy_percent IS NULL OR accuracy_percent BETWEEN 0 AND 100)
);

CREATE INDEX ix_dictation_attempts_user_segment_created
    ON dictation_attempts (user_id, segment_id, created_at DESC);
CREATE INDEX ix_dictation_attempts_user_lesson_status
    ON dictation_attempts (user_id, lesson_id, status);

-- 4.6 recordings ----------------------------------------------------------------
CREATE TABLE recordings (
    recording_id                 uuid            NOT NULL,
    user_id                      uuid            NOT NULL,
    classification               varchar(32)     NOT NULL DEFAULT 'ASSESSMENT_ONLY',
    status                       varchar(24)     NOT NULL,
    storage_provider             varchar(64)     NOT NULL,
    object_key_ciphertext        bytea           NULL,
    mime_type                    varchar(127)    NOT NULL,
    byte_size                    bigint          NOT NULL,
    duration_milliseconds        integer         NULL,
    checksum_sha256               char(64)        NOT NULL,
    malware_scan_status          varchar(24)     NOT NULL,
    assessment_succeeded_at      timestamptz     NULL,
    expires_at                   timestamptz     NOT NULL,
    deleted_at                   timestamptz     NULL,
    created_at                   timestamptz     NOT NULL,
    updated_at                   timestamptz     NOT NULL,
    version                      bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_recordings PRIMARY KEY (recording_id),
    CONSTRAINT uq_recordings_id_user UNIQUE (recording_id, user_id),
    CONSTRAINT fk_recordings_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT ck_recordings_classification CHECK (classification IN ('ASSESSMENT_ONLY', 'SAVED_RECORDING')),
    CONSTRAINT ck_recordings_status CHECK (status IN ('UPLOADING', 'SCANNING', 'AVAILABLE', 'QUARANTINED', 'DELETED')),
    CONSTRAINT ck_recordings_byte_size CHECK (byte_size > 0),
    CONSTRAINT ck_recordings_duration CHECK (duration_milliseconds IS NULL OR duration_milliseconds >= 0)
);

CREATE INDEX ix_recordings_user_status_created ON recordings (user_id, status, created_at DESC);
CREATE INDEX ix_recordings_classification_expires ON recordings (classification, expires_at);

-- 4.7 shadowing_attempts ------------------------------------------------------------
CREATE TABLE shadowing_attempts (
    shadowing_attempt_id         uuid            NOT NULL,
    user_id                      uuid            NOT NULL,
    lesson_id                    uuid            NOT NULL,
    segment_id                   uuid            NOT NULL,
    recording_id                 uuid            NOT NULL,
    status                       varchar(24)     NOT NULL,
    overall_score                numeric(5,2)    NULL,
    pronunciation_score          numeric(5,2)    NULL,
    tone_score                   numeric(5,2)    NULL,
    rhythm_score                 numeric(5,2)    NULL,
    feedback_ciphertext          bytea           NULL,
    ai_usage_event_id            uuid            NULL,
    submitted_at                 timestamptz     NULL,
    evaluated_at                 timestamptz     NULL,
    deleted_at                   timestamptz     NULL,
    created_at                   timestamptz     NOT NULL,
    updated_at                   timestamptz     NOT NULL,
    version                      bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_shadowing_attempts PRIMARY KEY (shadowing_attempt_id),
    CONSTRAINT uq_shadowing_attempts_recording UNIQUE (recording_id),
    CONSTRAINT fk_shadowing_attempts_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_shadowing_attempts_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (lesson_id),
    CONSTRAINT fk_shadowing_attempts_segment FOREIGN KEY (segment_id, lesson_id)
        REFERENCES segments (segment_id, lesson_id),
    CONSTRAINT fk_shadowing_attempts_recording FOREIGN KEY (recording_id, user_id)
        REFERENCES recordings (recording_id, user_id),
    CONSTRAINT fk_shadowing_attempts_ai_usage_event FOREIGN KEY (ai_usage_event_id, user_id)
        REFERENCES ai_usage_events (ai_usage_event_id, user_id),
    CONSTRAINT ck_shadowing_attempts_status CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'EVALUATED', 'FAILED', 'DELETED')),
    CONSTRAINT ck_shadowing_attempts_overall_score CHECK (overall_score IS NULL OR overall_score BETWEEN 0 AND 100),
    CONSTRAINT ck_shadowing_attempts_pronunciation_score CHECK (pronunciation_score IS NULL OR pronunciation_score BETWEEN 0 AND 100),
    CONSTRAINT ck_shadowing_attempts_tone_score CHECK (tone_score IS NULL OR tone_score BETWEEN 0 AND 100),
    CONSTRAINT ck_shadowing_attempts_rhythm_score CHECK (rhythm_score IS NULL OR rhythm_score BETWEEN 0 AND 100)
);

CREATE INDEX ix_shadowing_attempts_user_segment_created
    ON shadowing_attempts (user_id, segment_id, created_at DESC);
CREATE INDEX ix_shadowing_attempts_recording ON shadowing_attempts (recording_id);
CREATE INDEX ix_shadowing_attempts_ai_usage_event ON shadowing_attempts (ai_usage_event_id);
CREATE UNIQUE INDEX uq_shadowing_attempts_ai_usage_event
    ON shadowing_attempts (ai_usage_event_id)
    WHERE ai_usage_event_id IS NOT NULL;

-- 4.8 lesson_progresses -------------------------------------------------------------
CREATE TABLE lesson_progresses (
    lesson_progress_id           uuid            NOT NULL,
    user_id                      uuid            NOT NULL,
    lesson_id                    uuid            NOT NULL,
    status                       varchar(16)     NOT NULL,
    current_segment_id           uuid            NULL,
    completed_segment_count      integer         NOT NULL DEFAULT 0,
    total_segment_count          integer         NOT NULL DEFAULT 0,
    completion_percent           numeric(5,2)    NOT NULL DEFAULT 0,
    dictation_best_score         numeric(5,2)    NULL,
    shadowing_best_score         numeric(5,2)    NULL,
    practice_seconds             integer         NOT NULL DEFAULT 0,
    started_at                   timestamptz     NULL,
    completed_at                 timestamptz     NULL,
    last_activity_at             timestamptz     NOT NULL,
    created_at                   timestamptz     NOT NULL,
    updated_at                   timestamptz     NOT NULL,
    version                      bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_lesson_progresses PRIMARY KEY (lesson_progress_id),
    CONSTRAINT uq_lesson_progresses_user_lesson UNIQUE (user_id, lesson_id),
    CONSTRAINT fk_lesson_progresses_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_lesson_progresses_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (lesson_id),
    CONSTRAINT fk_lesson_progresses_current_segment FOREIGN KEY (current_segment_id, lesson_id)
        REFERENCES segments (segment_id, lesson_id),
    CONSTRAINT ck_lesson_progresses_status CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT ck_lesson_progresses_completion_percent CHECK (completion_percent BETWEEN 0 AND 100),
    CONSTRAINT ck_lesson_progresses_dictation_best_score CHECK (dictation_best_score IS NULL OR dictation_best_score BETWEEN 0 AND 100),
    CONSTRAINT ck_lesson_progresses_shadowing_best_score CHECK (shadowing_best_score IS NULL OR shadowing_best_score BETWEEN 0 AND 100)
);

CREATE INDEX ix_lesson_progresses_user_status_activity
    ON lesson_progresses (user_id, status, last_activity_at DESC);

-- -----------------------------------------------------------------------------
-- 5. AI BUDDY TABLES
-- -----------------------------------------------------------------------------

-- 5.1 ai_conversations ---------------------------------------------------------------
CREATE TABLE ai_conversations (
    ai_conversation_id           uuid            NOT NULL,
    user_id                      uuid            NOT NULL,
    title_ciphertext              bytea           NULL,
    scenario_code                varchar(64)     NULL,
    status                       varchar(16)     NOT NULL DEFAULT 'ACTIVE',
    last_message_at              timestamptz     NULL,
    deleted_at                   timestamptz     NULL,
    created_at                   timestamptz     NOT NULL,
    updated_at                   timestamptz     NOT NULL,
    version                      bigint          NOT NULL DEFAULT 0,
    CONSTRAINT pk_ai_conversations PRIMARY KEY (ai_conversation_id),
    CONSTRAINT uq_ai_conversations_id_user UNIQUE (ai_conversation_id, user_id),
    CONSTRAINT fk_ai_conversations_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT ck_ai_conversations_status CHECK (status IN ('ACTIVE', 'DELETED'))
);

CREATE INDEX ix_ai_conversations_user_status_last_message
    ON ai_conversations (user_id, status, last_message_at DESC);

-- 5.2 ai_messages ---------------------------------------------------------------------
CREATE TABLE ai_messages (
    ai_message_id                uuid            NOT NULL,
    ai_conversation_id           uuid            NOT NULL,
    user_id                      uuid            NOT NULL,
    sequence_no                  integer         NOT NULL,
    sender_type                  varchar(16)     NOT NULL,
    content_ciphertext           bytea           NOT NULL,
    status                       varchar(16)     NOT NULL,
    ai_usage_event_id            uuid            NULL,
    created_at                   timestamptz     NOT NULL,
    completed_at                 timestamptz     NULL,
    deleted_at                   timestamptz     NULL,
    CONSTRAINT pk_ai_messages PRIMARY KEY (ai_message_id),
    CONSTRAINT uq_ai_messages_conversation_sequence UNIQUE (ai_conversation_id, sequence_no),
    CONSTRAINT fk_ai_messages_conversation FOREIGN KEY (ai_conversation_id, user_id)
        REFERENCES ai_conversations (ai_conversation_id, user_id),
    CONSTRAINT fk_ai_messages_ai_usage_event FOREIGN KEY (ai_usage_event_id, user_id)
        REFERENCES ai_usage_events (ai_usage_event_id, user_id),
    CONSTRAINT ck_ai_messages_sequence_no CHECK (sequence_no > 0),
    CONSTRAINT ck_ai_messages_sender_type CHECK (sender_type IN ('LEARNER', 'ASSISTANT')),
    CONSTRAINT ck_ai_messages_status CHECK (status IN ('PENDING', 'COMPLETE', 'FAILED', 'DELETED')),
    CONSTRAINT ck_ai_messages_usage_event_assistant_only
        CHECK (ai_usage_event_id IS NULL OR sender_type = 'ASSISTANT')
);

CREATE INDEX ix_ai_messages_user_created ON ai_messages (user_id, created_at DESC);
CREATE INDEX ix_ai_messages_ai_usage_event ON ai_messages (ai_usage_event_id);
CREATE UNIQUE INDEX uq_ai_messages_ai_usage_event
    ON ai_messages (ai_usage_event_id)
    WHERE ai_usage_event_id IS NOT NULL;

-- =============================================================================
-- End of V1__pchinese_mvp_schema.sql (24 tables)
-- =============================================================================
