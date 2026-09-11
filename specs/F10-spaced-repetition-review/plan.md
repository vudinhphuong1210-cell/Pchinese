# Implementation Plan: F10 Spaced Repetition Review

**Branch**: `F10-spaced-repetition-review` | **Date**: 2026-09-11 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification at `specs/F10-spaced-repetition-review/spec.md`

## Summary

Deliver deterministic, server-authoritative spaced repetition vocabulary review scheduling. The backend owns due queue selection (`due_at <= now()`, max 20 items per batch ordered by oldest due time first), four-rating calculation policy (`AGAIN`, `HARD`, `GOOD`, `EASY`), ease factor clamping (1.300–2.500), SRS status state machine (`LEARNING`, `REVIEW`, `RELEARNING`, `SUSPENDED`), and immutable review audit history. React presents flashcard cards (Front: Hanzi + Audio; Back: Pinyin, Meaning, Examples, Personal Note), handles 10-minute `AGAIN` batch rotation, and submits server-authoritative ratings with client idempotency.

## Technical Context

| Area | Decision |
| --- | --- |
| Language / version | Java 21 with Spring Boot 3.4.5; JavaScript/JSX with React 18 and Vite |
| Primary dependencies | Spring Data JPA, Jakarta Validation, Spring Security, PostgreSQL 18; Tailwind CSS 3.x, Jest and Playwright |
| Storage | PostgreSQL 18. Canonical schema tables `srs_schedules` and `srs_review_events` defined in `DATA_short.md`. |
| Testing | JUnit 5 + Mockito unit tests, Spring Boot integration tests, Jest component tests, Playwright E2E tests |
| Target platform | Spring Boot REST API and modern desktop/mobile browsers |
| Project type | React SPA plus Spring Boot modular monolith |
| Due queue limit | Maximum 20 items per batch, ordered by `due_at ASC, srs_schedule_id ASC` |
| Idempotency & retries | `clientReviewId` (UUID) guarantees exact replay; `expectedScheduleVersion` prevents stale optimistic updates |
| F09 boundary | F09 triggers schedule creation (`LEARNING`, due immediately), suspension (`SUSPENDED`), and restoration. F10 owns SRS calculations & history. |

## Constitution Check

| Gate | Status | Evidence |
| --- | --- | --- |
| Server authority | Pass | Spring calculates all due times, intervals, ease factors, and status transitions; React never calculates or alters schedule values. |
| Data integrity & idempotency | Pass | Transactions lock user schedule; unique `(user_id, client_review_id)` index prevents duplicate review events; versioning rejects stale edits. |
| Immutable review history | Pass | `srs_review_events` is append-only with before/after state snapshots. |
| Module isolation | Pass | Cross-module calls use `SrsScheduleCommands` interface in `net.pchinese.review.application`; no raw SQL or shared repository coupling. |
| Quality & accessibility | Pass | Required unit/integration/E2E tests, accessible 44x44px touch targets, keyboard navigation, and empty/loading states. |

## Design Decisions

### 1. SRS State Machine & Calculation Policy
- **Initial Review (`LEARNING`)**:
  - `AGAIN`: 10 minutes (0.007 days), status `LEARNING`
  - `HARD`: 1 day (1.000 day), status `LEARNING`
  - `GOOD`: 3 days (3.000 days), status `REVIEW`
  - `EASY`: 7 days (7.000 days), status `REVIEW`
- **Later Reviews (`REVIEW`)**:
  - `AGAIN`: 10 minutes, ease = `clamp(ease - 0.20)`, lapses++, status `RELEARNING`
  - `HARD`: interval = `prev_interval * 1.20`, ease = `clamp(ease - 0.15)`, status `REVIEW`
  - `GOOD`: interval = `prev_interval * ease`, repetitions++, status `REVIEW`
  - `EASY`: interval = `prev_interval * (ease + 0.15)`, repetitions++, status `REVIEW`
- **Relearning (`RELEARNING`)**:
  - `AGAIN`: 10 minutes, status `RELEARNING`
  - `HARD`: interval = `prev_interval * 1.20`, status `RELEARNING`
  - `GOOD`: interval = `prev_interval * ease`, status `REVIEW`
  - `EASY`: interval = `prev_interval * (ease + 0.15)`, status `REVIEW`
- **Ease bounds**: `1.300 <= ease_factor <= 2.500`.

### 2. Lock Order & Transaction Isolation
All review mutations execute within a single transaction using lock order:
1. `users` row lock
2. `srs_schedules` row lock by `(srs_schedule_id, user_id)`

This prevents concurrent rating submissions from different tabs or devices from overwriting schedules out of order.

### 3. Flashcard UI & AGAIN Rotation
- **Front of Card**: Simplified/Traditional Hanzi, HSK badge, Audio button.
- **Back of Card**: Pinyin, Vietnamese Senses, Example Sentences, Personal Note.
- **AGAIN Rotation**: Rating `AGAIN` moves the card to the end of the active 20-item queue in React local state, allowing immediate re-testing within the active session.

## Project Structure

### Documentation

```text
specs/F10-spaced-repetition-review/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── f10-openapi.yaml
```

### Source code

```text
backend/src/main/java/net/pchinese/
├── review/
│   ├── api/
│   │   ├── SrsReviewController.java
│   │   └── SrsReviewDtos.java
│   ├── application/
│   │   ├── SrsScheduleService.java
│   │   ├── SrsReviewService.java
│   │   ├── SrsScheduleCommands.java
│   │   └── SavedWordScheduleService.java
│   ├── domain/
│   │   ├── SrsStatus.java
│   │   ├── ReviewRating.java
│   │   └── SrsPolicyEngine.java
│   └── persistence/
│       ├── SrsScheduleEntity.java
│       ├── SrsScheduleRepository.java
│       ├── SrsReviewEventEntity.java
│       └── SrsReviewEventRepository.java

frontend/src/
├── api/review.js
├── features/review/
│   ├── ReviewPage.jsx
│   ├── FlashcardDeck.jsx
│   ├── FlashcardItem.jsx
│   └── ReviewSummary.jsx
```

## Implementation Phases

1. Create SRS domain entities (`SrsScheduleEntity`, `SrsReviewEventEntity`) and repositories (`SrsScheduleRepository`, `SrsReviewEventRepository`) matching canonical schema.
2. Implement `SrsPolicyEngine` with unit test coverage for initial reviews, later reviews, relearning transitions, and ease clamping.
3. Wire F09 lifecycle hooks (`SavedWordScheduleService`) for initial `LEARNING` schedule creation, `SUSPENDED` on delete, and resume on restore.
4. Implement `SrsReviewService` and `SrsReviewController` (`GET /api/v1/srs/due`, `POST /api/v1/srs/review`) with atomic locking, version checking, and idempotency.
5. Implement React Flashcard components (`ReviewPage.jsx`, `FlashcardItem.jsx`) with front/back flip interaction, 10-minute `AGAIN` batch rotation, and accessible controls.
6. Add unit, integration, and E2E validation tests (`SrsReviewControllerIT.java`, `ReviewPage.test.jsx`, `spaced-repetition-review.spec.js`).

## Complexity Tracking

No constitution exception is required.
