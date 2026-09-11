# Data Model: F10 Spaced Repetition Review

## Database Schema (Canonical Contract)

### `srs_schedules`

| Column | Type | Constraints | Description |
| --- | --- | --- | --- |
| `srs_schedule_id` | `uuid` | `PRIMARY KEY` | Unique ID for SRS schedule |
| `saved_word_id` | `uuid` | `NOT NULL, UNIQUE, FK saved_words` | One schedule per saved word |
| `user_id` | `uuid` | `NOT NULL, FK users` | Learner ownership boundary |
| `status` | `varchar(16)` | `NOT NULL, CHECK (status IN ('LEARNING', 'REVIEW', 'RELEARNING', 'SUSPENDED'))` | Current SRS state |
| `due_at` | `timestamptz` | `NOT NULL` | Server-calculated next due timestamp |
| `interval_days` | `numeric(8,3)` | `NOT NULL DEFAULT 0` | Current review interval in days |
| `ease_factor` | `numeric(5,3)` | `NOT NULL DEFAULT 2.500, CHECK (ease_factor BETWEEN 1.300 AND 2.500)` | Bounded difficulty multiplier |
| `repetitions` | `integer` | `NOT NULL DEFAULT 0` | Total successful review count |
| `lapses` | `integer` | `NOT NULL DEFAULT 0` | Total `AGAIN` count on `REVIEW` items |
| `last_reviewed_at` | `timestamptz` | `NULL` | Most recent review completion timestamp |
| `created_at` | `timestamptz` | `NOT NULL` | Record creation timestamp |
| `updated_at` | `timestamptz` | `NOT NULL` | Record update timestamp |
| `version` | `bigint` | `NOT NULL DEFAULT 0` | Optimistic locking version |

**Indexes:**
* `ix_srs_schedules_user_status_due`: `(user_id, status, due_at)` — Optimized for due queue retrieval.
* `uq_srs_schedules_saved_word`: `(saved_word_id)` — Enforces one schedule per saved word.
* `uq_srs_schedules_id_user`: `(srs_schedule_id, user_id)` — Composite FK target for review events.

---

### `srs_review_events`

| Column | Type | Constraints | Description |
| --- | --- | --- | --- |
| `srs_review_event_id` | `uuid` | `PRIMARY KEY` | Unique ID for review event |
| `srs_schedule_id` | `uuid` | `NOT NULL, FK srs_schedules` | Reference to schedule |
| `user_id` | `uuid` | `NOT NULL, FK users` | Learner ownership |
| `client_review_id` | `uuid` | `NOT NULL` | Client idempotency key per review |
| `rating` | `varchar(16)` | `NOT NULL, CHECK (rating IN ('AGAIN', 'HARD', 'GOOD', 'EASY'))` | User rating selection |
| `previous_due_at` | `timestamptz` | `NOT NULL` | `due_at` snapshot prior to review |
| `next_due_at` | `timestamptz` | `NOT NULL` | `due_at` snapshot after review |
| `previous_interval_days` | `numeric(8,3)` | `NOT NULL` | Interval snapshot prior to review |
| `next_interval_days` | `numeric(8,3)` | `NOT NULL` | Interval snapshot after review |
| `reviewed_at` | `timestamptz` | `NOT NULL` | Time review was completed |
| `created_at` | `timestamptz` | `NOT NULL` | Audit timestamp |

**Indexes:**
* `uq_srs_review_events_user_client_review`: `(user_id, client_review_id)` — Enforces exact idempotency per user.
* `ix_srs_review_events_schedule_reviewed`: `(srs_schedule_id, reviewed_at DESC)` — History retrieval order.

---

## State Transition Rules

```text
Status: LEARNING
- AGAIN -> due in 10 minutes (0.007 days), status: LEARNING
- HARD  -> due in 1 day (1.000 day), status: LEARNING
- GOOD  -> due in 3 days (3.000 days), status: REVIEW
- EASY  -> due in 7 days (7.000 days), status: REVIEW

Status: REVIEW
- AGAIN -> due in 10 minutes, ease = clamp(ease - 0.20), lapses++, status: RELEARNING
- HARD  -> due in (prev_interval * 1.20) days, ease = clamp(ease - 0.15), status: REVIEW
- GOOD  -> due in (prev_interval * ease) days, repetitions++, status: REVIEW
- EASY  -> due in (prev_interval * (ease + 0.15)) days, repetitions++, status: REVIEW

Status: RELEARNING
- AGAIN -> due in 10 minutes, status: RELEARNING
- HARD  -> due in (prev_interval * 1.20) days, status: RELEARNING
- GOOD  -> due in (prev_interval * ease) days, status: REVIEW
- EASY  -> due in (prev_interval * (ease + 0.15)) days, status: REVIEW
```

---

## F09 Integration Lifecycle

1. **Initial Save**: When F09 saves a new word, F10 creates `srs_schedules` row with `status = 'LEARNING'`, `due_at = now()`, `interval_days = 0`, `ease_factor = 2.500`, `repetitions = 0`, `lapses = 0`.
2. **Word Removal**: When F09 removes a saved word, F10 updates schedule `status = 'SUSPENDED'`, retaining interval, ease, repetitions, and history.
3. **Word Restoration**: When F09 restores a removed word, F10 updates schedule status to `LEARNING` (if `repetitions == 0`) or `REVIEW` (if `repetitions > 0`). If `due_at` is in the past, it remains due immediately without creating a second schedule.
