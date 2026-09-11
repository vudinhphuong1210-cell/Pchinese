# Quickstart Guide: F10 Spaced Repetition Review

**Feature Branch**: `F10-spaced-repetition-review`

This guide outlines end-to-end integration and verification scenarios for the Spaced Repetition Review feature.

---

## 1. Prerequisites & Environment Setup

* **Backend**: Java 21, Spring Boot 3.4.5 running on `http://localhost:8080`.
* **Database**: PostgreSQL 18 with Flyway schema migration `20260905143053_initial_schema.sql` (containing `srs_schedules` and `srs_review_events`).
* **Frontend**: React 18 SPA running on `http://localhost:5173`.
* **Dependencies**: F09 Dictionary & Personal Vocabulary, F01 Learner Auth.

---

## 2. Integration Scenarios

### Scenario A: Initial Saved Word Schedule Creation
1. Authenticate as a learner.
2. Save a dictionary entry via `POST /api/v1/saved-words`.
3. Verify F09 invokes `onSavedWordCreated()` -> exact 1 `srs_schedules` row is created with `status = 'LEARNING'`, `due_at = now()`.

### Scenario B: Retrieve Due Review Queue
1. Execute `GET /api/v1/srs/due`.
2. Verify response envelope returns `success: true` with at most 20 items ordered by `due_at ASC`.
3. Verify cards display Front (Hanzi, Audio) and Back (Pinyin, Meaning, Examples, Personal Note).

### Scenario C: Submit Rating & SRS State Transition
1. Submit rating via `POST /api/v1/srs/review` with `rating = "GOOD"` and `expectedScheduleVersion = 0`.
2. Verify response returns `nextStatus = "REVIEW"`, `nextIntervalDays = 3.000`, `newScheduleVersion = 1`.
3. Verify `srs_review_events` row is recorded with matching `clientReviewId`.

### Scenario D: Idempotency & Conflict Check
1. Re-submit identical payload with same `clientReviewId`.
2. Verify response returns original result with `idempotentReplay: true` without creating duplicate events.
3. Submit a rating with a stale `expectedScheduleVersion = 0`.
4. Verify response returns `409 STATE_CONFLICT` with latest schedule state.

---

## 3. Verification Commands

```bash
# Backend unit & integration tests
cd backend
mvn test -Dtest=net.pchinese.review.**

# Frontend unit & component tests
cd frontend
npm test -- src/features/review
```
