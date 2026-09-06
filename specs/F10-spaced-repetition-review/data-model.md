# Data Model: F10 Spaced Repetition Review

## srs_schedules

| Column | Rules |
| --- | --- |
| id | UUID primary key |
| saved_word_id | Unique FK saved_words |
| user_id | FK users; owner lookup and integrity |
| state | LEARNING, REVIEW, RELEARNING, SUSPENDED |
| due_at | Server-calculated next due time |
| interval_minutes | Positive server-calculated interval |
| ease_factor | Decimal clamped from 1.30 to 2.50 |
| repetitions, lapses | Non-negative counters |
| version | Optimistic lock; increment each successful rating |
| created_at, updated_at, suspended_at | Lifecycle timestamps |

Indexes:

- user_id plus state plus due_at for due queue selection.
- saved_word_id unique to enforce one schedule per saved word.

## srs_review_events

| Column | Rules |
| --- | --- |
| id | UUID primary key |
| schedule_id, user_id | Owner and schedule reference |
| client_review_id | UUID, unique per user |
| request_fingerprint | Detects changed idempotency replay |
| rating | AGAIN, HARD, GOOD or EASY |
| previous_snapshot, next_snapshot | Immutable serialized state projections |
| reviewed_at | Server timestamp |
| created_at | Audit timestamp |

Review events are append-only. Reversals, if ever needed, require a separately audited domain operation; they are not edits to past events.

## F09 Lifecycle Mapping

F09 initial save creates schedule state LEARNING and due_at equal to server now. Delete sets SUSPENDED while retaining all scheduling and event data. Restore clears suspension and chooses LEARNING only when repetitions is zero; otherwise REVIEW, with due_at set to now if already overdue.

