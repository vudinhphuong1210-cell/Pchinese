# Implementation Plan: F10 Spaced Repetition Review

**Branch**: F10-spaced-repetition-review | **Date**: 2026-09-06 | **Spec**: spec.md
**Input**: Feature specification from D:\schinese\specs\F10-spaced-repetition-review\spec.md

## Summary

Deliver deterministic server-authoritative vocabulary review scheduling. Spring Boot returns the oldest twenty due reviews, accepts an idempotent rating submission with optimistic versioning, records immutable review events, and calculates the next schedule. F09 owns saved-word lifecycle and invokes F10 domain commands to create, suspend, and restore schedules. No AI, ai-service, or allowance participates.

## Technical Context

| Area | Decision |
| --- | --- |
| Authority | Spring Boot owns due selection, rating validation, interval calculation, time and persistence |
| Client | React displays server projections and submits a rating; it never computes or persists scheduling values |
| Queue | Server time only; dueAt less than or equal to server now; oldest first; maximum 20 rows per request |
| Concurrency | clientReviewId gives exact idempotent retry; expectedScheduleVersion prevents stale writes |
| History | Immutable review events hold before/after snapshots and are never edited |
| F09 boundary | F09 creates or reuses an immediate LEARNING schedule, suspends on delete, restores schedule/history on re-save |
| AI | No AI service call, Mastra flow, provider use, or F03 allowance is allowed |

## Constitution Check

| Principle | Status | Evidence |
| --- | --- | --- |
| Backend system of record | Pass | Server is the only clock and scheduler. |
| Deterministic learning data | Pass | Versioned state and immutable review events make outcomes auditable. |
| Safe retries | Pass | Same request identifier returns original result; stale mutations cannot overwrite a newer review. |
| Scope separation | Pass | F09 owns vocabulary lifecycle; F10 owns scheduling state and review history. |

## Project Structure

### Documentation

    specs/F10-spaced-repetition-review/
    ├── plan.md
    ├── research.md
    ├── data-model.md
    ├── quickstart.md
    └── contracts/
        └── f10-api.md

### Source Code

    backend/
    ├── src/main/java/.../review/
    └── src/main/resources/db/migration/
    frontend/
    └── src/features/review/

## Implementation Phases

1. Add schedules, immutable events, indexes and optimistic-version constraints.
2. Implement F09 domain hooks for initial create, suspend and restore.
3. Implement due queue and transactional rating submission with idempotency.
4. Build React review queue and rating flow from server projections.
5. Test calculations, batch boundaries, retries, races, suspension and authorization.

## Complexity Tracking

No constitution exceptions are required.

