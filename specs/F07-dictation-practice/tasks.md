---
description: "Actionable implementation tasks for F07 Dictation Practice"
---

# Tasks: F07 Dictation Practice

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f07-openapi.yaml and quickstart.md.

**Tests**: Required for ownership, F06 context, normalization, binary scoring and idempotent retakes.

## Phase 1: Setup

- [ ] T001 Create Dictation module packages in backend/src/main/java/net/pchinese/dictation/ and in-player components in frontend/src/features/dictation/.
- [ ] T002 [P] Add the Dictation panel mount point to frontend/src/features/lesson-player/LessonPlayer.tsx.

## Phase 2: Foundational

- [ ] T003 Add dictation_attempts schema, client-submission idempotency constraint and lesson-summary fields approved by DATA_short.md in backend/src/main/resources/db/migration/V007__f07_dictation.sql.
- [ ] T004 [P] Implement Dictation JPA entity, repository and encrypted-answer persistence mapping in backend/src/main/java/net/pchinese/dictation/persistence/.
- [ ] T005 Implement an F06 permitted-segment query port that permits only current unlocked or completed segments in backend/src/main/java/net/pchinese/dictation/application/PermittedDictationSegmentPort.java.

## Phase 3: User Story 1 - Complete dictation in a lesson (Priority: P1) MVP

**Goal**: Let a learner start and submit a permitted in-player Dictation attempt with deterministic binary scoring.

**Independent Test**: A learner submits a normalized exact answer for 100 or any mismatch for 0; no attempt reaches a future locked segment and no AI call occurs.

- [ ] T006 [P] [US1] Add controller integration tests for permitted context, owned submit, idempotent retry, retake and safe result projection in backend/src/test/java/net/pchinese/dictation/DictationControllerIT.java.
- [ ] T007 [P] [US1] Add normalization and binary exact-match scoring unit tests in backend/src/test/java/net/pchinese/dictation/DictationEvaluationServiceTest.java.
- [ ] T008 [US1] Implement Unicode/whitespace/punctuation normalization and 100-or-0 exact simplified-Hanzi evaluator in backend/src/main/java/net/pchinese/dictation/application/DictationEvaluationService.java.
- [ ] T009 [US1] Implement attempt start/submit transaction, F06 gate and approved best-score update without progress unlock in backend/src/main/java/net/pchinese/dictation/application/DictationAttemptService.java.
- [ ] T010 [US1] Implement Dictation start/submit DTOs and controllers in backend/src/main/java/net/pchinese/dictation/api/DictationController.java.
- [ ] T011 [US1] Implement typed Dictation client, in-player answer form and score/general-guidance UI without expected-answer or character analysis in frontend/src/api/dictation.ts and frontend/src/features/dictation/DictationPanel.tsx.

## Phase 4: User Story 2 - Review owned attempts (Priority: P2)

**Goal**: Let a learner list and open only their own prior Dictation attempts.

**Independent Test**: A learner sees only their attempts filtered by lesson/segment; an Admin or another learner receives no attempt data.

- [ ] T012 [P] [US2] Add owned-history, filter and no-ADMIN-bypass integration tests in backend/src/test/java/net/pchinese/dictation/DictationHistoryControllerIT.java.
- [ ] T013 [US2] Implement paginated owner-only attempt history query in backend/src/main/java/net/pchinese/dictation/application/DictationHistoryService.java.
- [ ] T014 [US2] Implement list/detail attempt endpoints in backend/src/main/java/net/pchinese/dictation/api/DictationHistoryController.java.
- [ ] T015 [US2] Implement owned attempt-history panel and retry entry point in frontend/src/features/dictation/DictationHistoryPanel.tsx.

## Phase 5: Polish and Cross-Cutting Concerns

- [ ] T016 [P] Add in-player Dictation retake and locked-segment E2E coverage in frontend/e2e/f07-dictation.spec.ts.
- [ ] T017 Verify no F07 code imports ai-service or F03 allowance services using backend/src/main/java/net/pchinese/dictation/ and run specs/F07-dictation-practice/quickstart.md.

## Dependencies and Execution Order

- F07 requires F01 ownership and F06 Player/segment context.
- Phase 2 blocks both stories.
- US2 depends on the attempts created by US1.

## Parallel Opportunities

- T002 and T004 can run after T001.
- T006 and T007 are independent tests before T008.
- T012 can be prepared in parallel with the US1 frontend after the attempt projection is stable.

## Implementation Strategy

Deliver the in-player deterministic evaluation first. Add history only after retry/retake idempotency and the exact binary score policy are fully covered.

