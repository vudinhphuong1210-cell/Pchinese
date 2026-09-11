# Tasks: F10 Spaced Repetition Review

**Input**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/f10-openapi.yaml](contracts/f10-openapi.yaml), and [quickstart.md](quickstart.md)

**Prerequisites**: F09 Dictionary & Personal Vocabulary must be available. F10 integrates with F09 saved vocabulary via application commands (`SrsScheduleCommands`).

**Tests**: Required. Project acceptance criteria require JUnit 5 + Mockito unit tests, Spring Boot integration tests, Jest component tests, and Playwright E2E tests.

**Organization**: Tasks are organized by phase and user story priority (P1: Review due vocabulary, P2: Trust personal review history).

## Format: `[ID] [P?] [Story?] Description with file path`

- **[P]** marks tasks that affect different files and can run concurrently.
- **[US1]** and **[US2]** map directly to User Stories in [spec.md](spec.md).

---

## Phase 1: Setup and foundational schema

**Purpose**: Establish a clean database and fixture baseline before feature execution.

- [X] T001 Inspect canonical schema in `DATA_short.md` and verify Flyway migration history for `srs_schedules` and `srs_review_events`.
- [X] T002 [P] Create deterministic test fixtures in `backend/src/test/java/net/pchinese/support/SrsReviewFixtureFactory.java`.

---

## Phase 2: Foundational contracts and persistence

**Purpose**: Implement domain enums, entities, repositories, policy calculation engine, and F09 cross-feature commands.

- [X] T003 Implement `SrsStatus` (`LEARNING`, `REVIEW`, `RELEARNING`, `SUSPENDED`) and `ReviewRating` (`AGAIN`, `HARD`, `GOOD`, `EASY`) enums in `backend/src/main/java/net/pchinese/review/domain/SrsStatus.java` and `backend/src/main/java/net/pchinese/review/domain/ReviewRating.java`.
- [X] T004 [P] Implement `SrsScheduleEntity` and `SrsScheduleRepository` in `backend/src/main/java/net/pchinese/review/persistence/SrsScheduleEntity.java` and `backend/src/main/java/net/pchinese/review/persistence/SrsScheduleRepository.java` mapping `srs_schedule_id`, `saved_word_id`, `user_id`, `status`, `due_at`, `interval_days`, `ease_factor`, `repetitions`, `lapses`, `last_reviewed_at`, `version`, and due query index `ix_srs_schedules_user_status_due`.
- [X] T005 [P] Implement `SrsReviewEventEntity` and `SrsReviewEventRepository` in `backend/src/main/java/net/pchinese/review/persistence/SrsReviewEventEntity.java` and `backend/src/main/java/net/pchinese/review/persistence/SrsReviewEventRepository.java` mapping `srs_review_event_id`, `srs_schedule_id`, `user_id`, `client_review_id`, `rating`, `previous_due_at`, `next_due_at`, `previous_interval_days`, `next_interval_days`, `reviewed_at`, and unique constraint `uq_srs_review_events_user_client_review`.
- [X] T006 [P] Implement deterministic `SrsPolicyEngine.java` in `backend/src/main/java/net/pchinese/review/domain/SrsPolicyEngine.java` for 4-rating policy calculations (`AGAIN`, `HARD`, `GOOD`, `EASY`), ease factor clamping (1.300–2.500), and status transitions (`LEARNING`, `REVIEW`, `RELEARNING`).
- [X] T007 [P] Add unit test coverage in `backend/src/test/java/net/pchinese/review/SrsPolicyEngineTest.java` verifying initial reviews, later reviews, relearning transitions, and ease bounds.
- [X] T008 Implement F09 domain integration command handlers in `backend/src/main/java/net/pchinese/review/application/SavedWordScheduleService.java` implementing `SrsScheduleCommands` for initial `LEARNING` schedule creation (`due_at = now()`), `SUSPENDED` status on word delete, and schedule resumption on word restore.

---

## Phase 3: User Story 1 — Review due vocabulary (Priority: P1) 🎯 MVP

**Goal**: A learner can open their due review queue (max 20 cards ordered by oldest `due_at` first), view flashcards (Front: Hanzi + Audio; Back: Pinyin, Meaning, Examples, Note), submit ratings, and see updated due states with 10-minute `AGAIN` batch rotation.

**Independent Test**: An authenticated learner with due words receives a bounded due queue, flips cards, submits ratings, and sees immediate state updates.

### Tests for User Story 1

- [X] T009 [P] [US1] Add integration and contract tests in `backend/src/test/java/net/pchinese/review/SrsReviewControllerIT.java` covering `GET /api/v1/srs/due` batch limit of 20 items ordered by oldest `due_at`, `POST /api/v1/srs/review` rating calculations, and owner data isolation.
- [X] T010 [P] [US1] Add API client and accessible UI tests in `frontend/src/api/review.test.js` and `frontend/src/features/review/ReviewPage.test.jsx` for due queue loading, card flip interaction, rating submission, empty state, and 10-minute `AGAIN` batch rotation.

### Implementation for User Story 1

- [X] T011 [US1] Implement due queue retrieval and rating submission in `backend/src/main/java/net/pchinese/review/application/SrsReviewService.java` with atomic user/schedule row locking and dictionary detail projection.
- [X] T012 [US1] Implement `GET /api/v1/srs/due` and `POST /api/v1/srs/review` REST endpoints with validated DTOs in `backend/src/main/java/net/pchinese/review/api/SrsReviewController.java` and `backend/src/main/java/net/pchinese/review/api/SrsReviewDtos.java`.
- [X] T013 [US1] Implement contract-bound client in `frontend/src/api/review.js` and flashcard UI components in `frontend/src/features/review/ReviewPage.jsx`, `frontend/src/features/review/FlashcardDeck.jsx`, and `frontend/src/features/review/FlashcardItem.jsx` (Front: Hanzi + Audio; Back: Pinyin, Meaning, Examples, Personal Note; 10-min `AGAIN` batch rotation).
- [X] T014 [US1] Add E2E learner review flow coverage in `frontend/e2e/spaced-repetition-review.spec.js`.

---

## Phase 4: User Story 2 — Trust a personal review history (Priority: P2)

**Goal**: Repeated or concurrent review submissions return original accepted results (`clientReviewId` idempotency) or `409 STATE_CONFLICT` without creating duplicate events or corrupted schedules.

**Independent Test**: Submitting duplicate `clientReviewId` requests returns original result projections without adding review events.

### Tests for User Story 2

- [X] T015 [P] [US2] Add concurrency, idempotency, and version conflict integration tests in `backend/src/test/java/net/pchinese/review/SrsReviewIdempotencyIT.java` for duplicate `clientReviewId` replay, stale `expectedScheduleVersion` rejection (`409 STATE_CONFLICT`), and immutable event audit history.
- [X] T016 [P] [US2] Add component & stale version reload tests in `frontend/src/features/review/ReviewSummary.test.jsx`.

### Implementation for User Story 2

- [X] T017 [US2] Implement idempotent replay lookup, atomic event logging, and `expectedScheduleVersion` conflict mapping in `backend/src/main/java/net/pchinese/review/application/SrsReviewService.java` and `backend/src/main/java/net/pchinese/common/error/ApiExceptionHandler.java`.
- [X] T018 [US2] Add session summary and conflict recovery UI in `frontend/src/features/review/ReviewSummary.jsx` and `frontend/src/features/review/ReviewPage.jsx`.

---

## Phase 5: Polish and cross-cutting concerns

**Purpose**: Finalize metrics, perform end-to-end verification, and validate project quality criteria.

- [X] T019 [P] Add privacy-safe metrics for due queue latency, rating selection counters, idempotency replays, and stale conflict counts in `backend/src/main/java/net/pchinese/review/application/SrsReviewMetrics.java`.
- [X] T020 Run full backend and frontend test suites (`mvn test`, `npm test`) to verify all 20 tasks pass cleanly.

---

## Dependencies and Execution Order

1. **Phase 1** can start immediately.
2. **Phase 2** depends on T001–T002. T003–T007 run concurrently; T008 connects F09 commands.
3. **Phase 3 (US1)** depends on Phase 2. T009–T010 tests run before T011–T014 implementation.
4. **Phase 4 (US2)** depends on Phase 3. T015–T016 tests run before T017–T018 implementation.
5. **Phase 5** runs after US1 and US2 are complete.

## Implementation Strategy

1. **MVP First**: Complete Phase 1, Phase 2, and Phase 3 (US1) to deliver core flashcard review capability.
2. **Incremental Delivery**: Add Phase 4 (US2) for idempotency, retry safety, and audit history.
3. **Validation**: Complete Phase 5 to verify cross-cutting quality requirements.
