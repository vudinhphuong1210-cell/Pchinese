---
description: "Actionable implementation tasks for F10 Spaced Repetition Review"
---

# Tasks: F10 Spaced Repetition Review

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f10-openapi.yaml and quickstart.md.

**Tests**: Required for deterministic rating policy, owner queue, idempotent retry, stale version and F09 lifecycle integration.

## Phase 1: Setup

- [ ] T001 Create spaced-repetition module packages in backend/src/main/java/net/pchinese/review/ and review screen shells in frontend/src/features/review/.
- [ ] T002 [P] Add the review-route entry point in frontend/src/routes/reviewRoutes.jsx.

## Phase 2: Foundational

- [ ] T003 Add srs_schedules and append-only srs_review_events schema, unique constraints and due-queue indexes in backend/src/main/resources/db/migration/V010__f10_spaced_repetition.sql.
- [ ] T004 [P] Implement JPA schedule/event entities and locked owner repository queries in backend/src/main/java/net/pchinese/review/persistence/.
- [ ] T005 Implement the internal F09 ensure-initial, suspend and restore schedule command interface in backend/src/main/java/net/pchinese/review/application/SrsScheduleCommands.java.

## Phase 3: User Story 1 - Review due vocabulary (Priority: P1) MVP

**Goal**: Let a learner retrieve at most 20 oldest due cards and submit one server-authoritative rating.

**Independent Test**: A learner rates a due owned word and sees a persisted next schedule; empty, unowned and invalid-rating states are safe.

- [ ] T006 [P] [US1] Add due-queue size/order/ownership and rating endpoint integration tests in backend/src/test/java/net/pchinese/review/ReviewControllerIT.java.
- [ ] T007 [P] [US1] Add initial and later AGAIN/HARD/GOOD/EASY interval/ease policy unit tests in backend/src/test/java/net/pchinese/review/SrsSchedulingPolicyTest.java.
- [ ] T008 [US1] Implement due selection by server time and deterministic four-rating scheduling policy in backend/src/main/java/net/pchinese/review/application/SrsSchedulingService.java.
- [ ] T009 [US1] Implement review submit transaction, optimistic version check and immutable event append in backend/src/main/java/net/pchinese/review/application/ReviewSubmissionService.java.
- [ ] T010 [US1] Implement due/list and submit DTOs/controllers in backend/src/main/java/net/pchinese/review/api/ReviewController.java.
- [ ] T011 [US1] Implement contract-bound review client, due-card/rating controls and accessible empty/error states in frontend/src/api/reviews.js and frontend/src/features/review/ReviewQueuePage.jsx.

## Phase 4: User Story 2 - Trust a personal review history (Priority: P2)

**Goal**: Preserve one learner-owned schedule/history through retry, stale actions, deletion and restoration.

**Independent Test**: An identical retry returns its original result, a changed stale request returns conflict/latest state, and F09 delete/restore preserves interval/ease/history.

- [ ] T012 [P] [US2] Add idempotency-key reuse, stale-version and concurrent-submit integration tests in backend/src/test/java/net/pchinese/review/ReviewIdempotencyIT.java.
- [ ] T013 [P] [US2] Add F09 first-save, suspend and restore/overdue schedule integration tests in backend/src/test/java/net/pchinese/review/SavedWordScheduleIntegrationTest.java.
- [ ] T014 [US2] Implement exact-retry replay and changed-fingerprint conflict handling in backend/src/main/java/net/pchinese/review/application/ReviewIdempotencyService.java.
- [ ] T015 [US2] Implement F09 schedule lifecycle commands preserving past schedule/event data in backend/src/main/java/net/pchinese/review/application/SavedWordScheduleService.java.
- [ ] T016 [US2] Implement stale-state recovery and retry UI in frontend/src/features/review/ReviewConflictPanel.jsx.
- [ ] T017 [US2] Add Jest coverage for retry result reuse and conflict reload in frontend/src/features/review/ReviewQueuePage.test.jsx.

## Phase 5: Polish and Cross-Cutting Concerns

- [ ] T018 [P] Add due-batch, rating and stale-retry E2E coverage in frontend/e2e/f10-spaced-repetition.spec.js.
- [ ] T019 Run F10 quickstart, F09/F10 integration and clean-migration/backend/frontend test suites using specs/F10-spaced-repetition-review/quickstart.md.

## Dependencies and Execution Order

- F10 requires F01 identity and the F09 saved_words relation.
- Phase 2 blocks both stories.
- F09 US2 must integrate against T005 and T015 before release; F10 review APIs are otherwise independently testable with a seeded saved word.

## Parallel Opportunities

- T002 and T004 can run in parallel after T001.
- T006/T007 and T012/T013 are independent test suites.
- Frontend T011 can proceed after T010 fixes the response contract.

## Implementation Strategy

Deliver due queue and deterministic scheduling first. Add the idempotency/history and F09 delete/restore boundary before enabling review lifecycle in production.
