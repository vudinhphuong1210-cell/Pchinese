---
description: "Actionable implementation tasks for F06 Lesson Learning and Progress"
---

# Tasks: F06 Lesson Learning and Progress

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f06-openapi.yaml and quickstart.md.

**Tests**: Required for ownership, sequential completion, capability/watermark validation and concurrent event safety.

## Phase 1: Setup

- [ ] T001 Create lesson-progress module packages in backend/src/main/java/net/pchinese/learning/ and player feature folders in frontend/src/features/lesson-player/.
- [ ] T002 [P] Add protected lesson-player route composition in frontend/src/routes/lessonPlayerRoutes.tsx.

## Phase 2: Foundational

- [ ] T003 Add learner-owned lesson-progress, segment-progress and idempotent playback-event tables/indexes in backend/src/main/resources/db/migration/V006__f06_learning_progress.sql.
- [ ] T004 [P] Implement JPA entities, locking repository methods and progress projections in backend/src/main/java/net/pchinese/learning/persistence/.
- [ ] T005 Implement short-lived signed playback-capability issuance and validation in backend/src/main/java/net/pchinese/learning/application/PlaybackCapabilityService.java.

## Phase 3: User Story 1 - Learn through ordered lesson segments (Priority: P1) MVP

**Goal**: Serve permitted ordered segments and advance only when a server-validated Player reaches the current segment end.

**Independent Test**: A learner sees current/completed segments, cannot jump ahead and one automatic end event unlocks exactly one next segment.

- [ ] T006 [P] [US1] Add playback capability, locked-segment, watermark and automatic-end integration tests in backend/src/test/java/net/pchinese/learning/LessonPlaybackControllerIT.java.
- [ ] T007 [P] [US1] Add service concurrency tests for one-winner segment completion in backend/src/test/java/net/pchinese/learning/LessonProgressServiceTest.java.
- [ ] T008 [US1] Implement permitted-player entry, lazy progress initialization and ordered segment projection in backend/src/main/java/net/pchinese/learning/application/LessonPlaybackService.java.
- [ ] T009 [US1] Implement server-authoritative playback-event and one-time completion transition service in backend/src/main/java/net/pchinese/learning/application/LessonProgressService.java.
- [ ] T010 [US1] Implement playback and playback-event controllers/DTOs in backend/src/main/java/net/pchinese/learning/api/LessonPlaybackController.java.
- [ ] T011 [US1] Implement typed playback client, watermark-limited player and automatic end event in frontend/src/api/lessonPlayback.ts and frontend/src/features/lesson-player/LessonPlayer.tsx.

## Phase 4: User Story 2 - Resume accurate learning progress (Priority: P1)

**Goal**: Show a learner's latest server-confirmed progress without creating state from catalog browsing.

**Independent Test**: A learner returns after partial/final completion and sees the correct current segment or completed state without duplicate counts.

- [ ] T012 [P] [US2] Add own-progress, no-catalog-row, final-completion and replay-event integration tests in backend/src/test/java/net/pchinese/learning/LessonProgressControllerIT.java.
- [ ] T013 [US2] Implement own aggregate progress query and not-started projection in backend/src/main/java/net/pchinese/learning/application/LessonProgressQueryService.java.
- [ ] T014 [US2] Implement GET lesson-progress endpoint in backend/src/main/java/net/pchinese/learning/api/LessonProgressController.java.
- [ ] T015 [US2] Implement resume, completed, loading and unavailable Player UI states in frontend/src/features/lesson-player/LessonProgressPanel.tsx.
- [ ] T016 [US2] Add Jest coverage for auto-complete, no-forward-seek and recoverable unavailable states in frontend/src/features/lesson-player/LessonPlayer.test.tsx.

## Phase 5: Polish and Cross-Cutting Concerns

- [ ] T017 [P] Add multi-device completion-race and playback-watermark E2E coverage in frontend/e2e/f06-lesson-progress.spec.ts.
- [ ] T018 Run F06 quickstart validation and clean-migration/backend/frontend test suites using specs/F06-lesson-learning-progress/quickstart.md.

## Dependencies and Execution Order

- F06 requires F01 learner identity and F05 current publication/access decisions.
- Phase 2 blocks both stories.
- US2 consumes the progress state and transitions built in US1.
- F07 and F08 require F06 permitted current/completed segment context.

## Parallel Opportunities

- T002 and T004 can proceed after T001.
- T006 and T007 are independent test suites.
- Frontend player T011 can begin after the playback response contract is stable.

## Implementation Strategy

Deliver the protected sequential player first. Add resume/overview only after server transitions and replay-safe completion have passed concurrency tests.

