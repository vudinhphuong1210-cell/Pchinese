-- F04 content administration audit trail. This migration intentionally upgrades
-- the earlier V004 development schema when it is already present, as well as a
-- clean baseline database. It does not rewrite prior audit-event rows.

ALTER TABLE media_assets
    ADD COLUMN IF NOT EXISTS title varchar(200),
    ADD COLUMN IF NOT EXISTS alt_text varchar(500);

ALTER TABLE media_assets
    ALTER COLUMN malware_scan_status SET DEFAULT 'PENDING';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'media_assets'::regclass
          AND conname = 'ck_media_assets_scan_status'
    ) THEN
        ALTER TABLE media_assets
            ADD CONSTRAINT ck_media_assets_scan_status
            CHECK (malware_scan_status IN ('PENDING', 'CLEAN', 'INFECTED', 'FAILED')) NOT VALID;
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS content_audit_events (
    content_audit_event_id uuid NOT NULL,
    event_type varchar(100) NOT NULL,
    actor_user_id uuid NOT NULL,
    target_entity_type varchar(50) NOT NULL,
    target_entity_id uuid NOT NULL,
    outcome varchar(20) NOT NULL,
    reason_code varchar(100),
    expected_version bigint,
    observed_version bigint,
    before_state varchar(50),
    after_state varchar(50),
    safe_details jsonb,
    correlation_id uuid NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_content_audit_events PRIMARY KEY (content_audit_event_id),
    CONSTRAINT fk_content_audit_events_actor FOREIGN KEY (actor_user_id) REFERENCES users (user_id),
    CONSTRAINT ck_content_audit_events_target_type CHECK (target_entity_type IN ('TOPIC', 'LESSON', 'SEGMENT', 'MEDIA_ASSET')),
    CONSTRAINT ck_content_audit_events_outcome CHECK (outcome IN ('SUCCESS', 'REJECTED')),
    CONSTRAINT ck_content_audit_events_safe_details_object CHECK (safe_details IS NULL OR jsonb_typeof(safe_details) = 'object')
);

-- V004 used a narrower, preliminary audit table. Add the canonical columns
-- without modifying its retained event rows or dropping its legacy columns.
ALTER TABLE content_audit_events
    ADD COLUMN IF NOT EXISTS reason_code varchar(100),
    ADD COLUMN IF NOT EXISTS expected_version bigint,
    ADD COLUMN IF NOT EXISTS observed_version bigint,
    ADD COLUMN IF NOT EXISTS before_state varchar(50),
    ADD COLUMN IF NOT EXISTS after_state varchar(50),
    ADD COLUMN IF NOT EXISTS safe_details jsonb;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'content_audit_events'::regclass
          AND conname = 'ck_content_audit_events_target_type'
    ) THEN
        ALTER TABLE content_audit_events
            ADD CONSTRAINT ck_content_audit_events_target_type
            CHECK (target_entity_type IN ('TOPIC', 'LESSON', 'SEGMENT', 'MEDIA_ASSET')) NOT VALID;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'content_audit_events'::regclass
          AND conname = 'ck_content_audit_events_outcome'
    ) THEN
        ALTER TABLE content_audit_events
            ADD CONSTRAINT ck_content_audit_events_outcome
            CHECK (outcome IN ('SUCCESS', 'REJECTED')) NOT VALID;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'content_audit_events'::regclass
          AND conname = 'ck_content_audit_events_safe_details_object'
    ) THEN
        ALTER TABLE content_audit_events
            ADD CONSTRAINT ck_content_audit_events_safe_details_object
            CHECK (safe_details IS NULL OR jsonb_typeof(safe_details) = 'object') NOT VALID;
    END IF;
END;
$$;

CREATE INDEX IF NOT EXISTS ix_content_audit_events_target_occurred
    ON content_audit_events (target_entity_type, target_entity_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_content_audit_events_actor_occurred
    ON content_audit_events (actor_user_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_content_audit_events_correlation
    ON content_audit_events (correlation_id);

CREATE OR REPLACE FUNCTION prevent_content_audit_event_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'content_audit_events are append-only';
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgrelid = 'content_audit_events'::regclass
          AND tgname = 'trg_content_audit_events_append_only'
    ) THEN
        EXECUTE 'CREATE TRIGGER trg_content_audit_events_append_only
                 BEFORE UPDATE OR DELETE ON content_audit_events
                 FOR EACH ROW EXECUTE FUNCTION prevent_content_audit_event_mutation()';
    END IF;
END;
$$;
