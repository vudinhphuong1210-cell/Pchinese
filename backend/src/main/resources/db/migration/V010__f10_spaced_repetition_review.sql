-- =============================================================================
-- Migration: V010__f10_spaced_repetition_review.sql
-- Description: Add client_review_id to srs_review_events, unique idempotency index, and SRS due queue lookup index.
-- =============================================================================

ALTER TABLE srs_review_events
    ADD COLUMN IF NOT EXISTS client_review_id uuid;

CREATE UNIQUE INDEX IF NOT EXISTS uq_srs_review_events_user_client_review
    ON srs_review_events (user_id, client_review_id)
    WHERE client_review_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_srs_schedules_user_status_due
    ON srs_schedules (user_id, status, due_at, srs_schedule_id);
