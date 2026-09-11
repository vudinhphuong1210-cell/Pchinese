# Research: F10 Spaced Repetition Review

## Decisions

| Topic | Decision | Rationale |
| --- | --- | --- |
| Due queue batching | `GET /api/v1/srs/due` returns at most 20 learner-owned due items where `due_at <= now()`, ordered by `due_at ASC, srs_schedule_id ASC`. | Enforces bounded batching without client-side memory bloat. |
| AGAIN card rotation | An item rated `AGAIN` during an active review session is rotated to the end of the active 20-item batch queue. | Allows the learner to re-test recall before completing the active session without waiting for full 10-minute timer. |
| Status transitions | `LEARNING` → `REVIEW` on `GOOD`/`EASY`; `AGAIN`/`HARD` retains `LEARNING`. `REVIEW` → `RELEARNING` on `AGAIN`; `HARD`/`GOOD`/`EASY` retains `REVIEW`. `RELEARNING` → `REVIEW` on `GOOD`/`EASY`; `AGAIN`/`HARD` retains `RELEARNING`. | Implements canonical Anki SRS state machine separating new, mastered, and lapsed vocabulary. |
| Initial review policy | On first review: `AGAIN` (10 minutes), `HARD` (1 day), `GOOD` (3 days), `EASY` (7 days). Initial ease = 2.500. | Establishes standard beginner spacing rules. |
| Later review policy | On later reviews: `AGAIN` (10 minutes, ease -0.20); `HARD` (prior interval × 1.20, ease -0.15); `GOOD` (prior interval × prior ease); `EASY` (prior interval × (prior ease + 0.15)). | Standard SRS interval multiplier and ease factor adjustment. |
| Ease factor bounds | Clamp `ease_factor` strictly between 1.300 and 2.500. | Prevents extreme exponential growth or infinite repetition collapse. |
| Idempotency & retries | Submit review with `clientReviewId` (UUID) and `expectedScheduleVersion`. | Re-submitting identical `clientReviewId` returns original accepted result without duplicate events; stale version mismatch returns `409 STATE_CONFLICT` with safe latest schedule. |
| F09 integration boundary | F09 saved-word creation creates/reuses an immediate `LEARNING` schedule due `now()`. Removal suspends schedule (`SUSPENDED`). Restoration resumes existing schedule without resetting interval, ease, or history. | Keeps vocabulary ownership in F09 while reserving schedule state and history in F10. |
| Flashcard UI disclosure | Front: Hanzi (Simplified/Traditional) + Audio. Back: Pinyin + Vietnamese Meaning + Examples + Personal Note. | Optimal recall prompt architecture for Chinese character recognition. |

## SRS State Machine Rules

```mermaid
stateDiagram-v2
    [*] --> LEARNING : F09 Save Word (due_at = now)
    
    state LEARNING {
        [*] --> InitialReview
        InitialReview --> LEARNING : AGAIN (10m) / HARD (1d)
        InitialReview --> REVIEW : GOOD (3d) / EASY (7d)
    }

    state REVIEW {
        REVIEW --> REVIEW : HARD (×1.2) / GOOD (×ease) / EASY (×(ease+0.15))
        REVIEW --> RELEARNING : AGAIN (10m, ease -0.20)
    }

    state RELEARNING {
        RELEARNING --> RELEARNING : AGAIN (10m) / HARD (×1.2)
        RELEARNING --> REVIEW : GOOD (×ease) / EASY (×(ease+0.15))
    }

    LEARNING --> SUSPENDED : F09 Delete Saved Word
    REVIEW --> SUSPENDED : F09 Delete Saved Word
    RELEARNING --> SUSPENDED : F09 Delete Saved Word

    SUSPENDED --> LEARNING : F09 Restore Word (if repetitions == 0)
    SUSPENDED --> REVIEW : F09 Restore Word (if repetitions > 0)
```

## Atomic Review Submission

Within one database transaction under pessimistic user/schedule lock:

1. Check if `srs_review_events` contains an event matching `(user_id, client_review_id)`. If found, return original result projection without adding new event.
2. Lock `srs_schedules` row by `srs_schedule_id` and `user_id`. Verify `version == expectedScheduleVersion` and `status != 'SUSPENDED'`.
3. Calculate next `due_at`, `interval_days`, `ease_factor`, `status`, `repetitions`, and `lapses`.
4. Increment `version`, update `srs_schedules`.
5. Insert immutable `srs_review_events` row.
6. Commit transaction and return updated schedule and event projections.

## Rejected Alternatives

- Client-calculated SRS intervals: Allows clock tampering and inconsistent interval calculations.
- Deleting SRS schedule on F09 word removal: Destroys learning history and repetition progress.
- Unbounded due queue fetching: Can cause memory exhaustion under large due backlogs.
- AI-based interval calculation: Adds latency, cost, and non-deterministic behavior to a deterministic rule engine.
