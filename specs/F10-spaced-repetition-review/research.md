# Research: F10 Spaced Repetition Review

## Decisions

| Topic | Decision | Rationale |
| --- | --- | --- |
| Due queue | GET due returns at most 20 owner schedules where dueAt is no later than server now, ordered oldest first. | Gives predictable small batches without client-side limits. |
| Review submission | Submit rating, clientReviewId UUID and expectedScheduleVersion. | Supports safe retry and detects stale tabs. |
| Retry order | Resolve an existing clientReviewId first; otherwise verify version and mutate schedule/event atomically. | Exact retried requests return their original response instead of a stale error. |
| State | LEARNING, REVIEW, RELEARNING and SUSPENDED. | Separates new, established, lapsed and inactive vocabulary. |
| Initial intervals | AGAIN 10 minutes, HARD 1 day, GOOD 3 days, EASY 7 days. | Establishes clear beginner behavior. |
| Later intervals | AGAIN 10 minutes and ease minus 0.20; HARD prior interval times 1.20 and ease minus 0.15; GOOD prior interval times ease; EASY prior interval times ease plus 0.15. | Keeps the policy auditable and consistent. |
| Ease bounds | Clamp to 1.30 through 2.50. | Prevents extreme schedule growth or collapse. |
| F09 restore | A never-reviewed schedule returns to LEARNING; otherwise REVIEW; overdue restoration is due immediately. | Preserves history while restoring learner access. |

## Rating Rules

Use server time and persisted schedule state only. A valid rating is AGAIN, HARD, GOOD or EASY. The response contains the new state, dueAt, interval, ease and version. The label transition policy is recorded as implementation policy with the resulting event snapshot; no client inference is accepted.

## Atomic Submission

Within one database transaction:

1. Locate a review event by user and clientReviewId; if found, compare request fingerprint and return the original result.
2. Lock the owner schedule, verify expectedScheduleVersion, due eligibility and non-suspended state.
3. Calculate next state from the configured rules.
4. Increment schedule version and write an immutable event with before and after snapshots.
5. Commit and return the persisted projection.

If clientReviewId is reused for different schedule/rating data, return conflict. If version is stale, return 409 with a safe latest schedule projection.

## Rejected Alternatives

- Calculating intervals in React: creates clock skew and tamperable schedule state.
- Deleting schedules and events when a saved word is removed: destroys learning history.
- Accepting unbounded due limits: allows expensive queues and inconsistent UI behavior.
- Calling AI to decide intervals: adds cost and nondeterminism to a rule-based feature.

