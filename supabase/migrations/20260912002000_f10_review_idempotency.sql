-- F10 was present only in the unused backend migration directory. Keep the
-- canonical schema and Flyway runtime under supabase/migrations.
ALTER TABLE public.srs_review_events
    ADD COLUMN IF NOT EXISTS client_review_id uuid;

-- Historical review rows predate client-generated idempotency keys. Their event
-- ids are already unique, so they safely serve as immutable legacy keys.
UPDATE public.srs_review_events
SET client_review_id = srs_review_event_id
WHERE client_review_id IS NULL;

ALTER TABLE public.srs_review_events
    ALTER COLUMN client_review_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_srs_review_events_user_client_review
    ON public.srs_review_events (user_id, client_review_id);

CREATE INDEX IF NOT EXISTS ix_srs_schedules_user_status_due
    ON public.srs_schedules (user_id, status, due_at, srs_schedule_id);
