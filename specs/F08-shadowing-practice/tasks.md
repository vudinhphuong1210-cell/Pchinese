---
description: "Actionable implementation tasks for F08 Shadowing Practice"
---

# Tasks: F08 Shadowing Practice

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f08-openapi.yaml and quickstart.md.

**Tests**: Required for recording ownership, retention, private AI contract, allowance idempotency and transcript non-persistence.

## Phase 1: Setup

- [ ] T001 Create Shadowing backend/storage packages in backend/src/main/java/net/pchinese/shadowing/ and backend/src/main/java/net/pchinese/storage/.
- [ ] T002 [P] Create the private Node.js and TypeScript ai-service runtime scaffold in ai-service/package.json, ai-service/tsconfig.json, ai-service/src/server.ts and ai-service/vitest.config.ts, plus the private assessment module in ai-service/src/shadowing/index.ts and ai-service/src/contracts/shadowing.ts.
- [ ] T003 [P] Create the dedicated Shadowing route and recording UI shell in frontend/src/features/shadowing/ShadowingPage.jsx and frontend/src/routes/shadowingRoutes.jsx.

## Phase 2: Foundational

- [ ] T004 Add shadowing_recordings and shadowing_attempts schema, owner/idempotency constraints and retention indexes in backend/src/main/resources/db/migration/V008__f08_shadowing.sql.
- [ ] T005 [P] Implement recording/attempt JPA entities, lifecycle enums and locking repositories in backend/src/main/java/net/pchinese/shadowing/persistence/.
- [ ] T006 [P] Implement private encrypted object-storage adapter and scan/quarantine lifecycle in backend/src/main/java/net/pchinese/storage/PrivateRecordingStorageService.java.
- [ ] T007 Implement a typed F03 reservation/succeed/refund adapter and F06 permitted-segment gate in backend/src/main/java/net/pchinese/shadowing/application/ShadowingDependencies.java.

## Phase 3: User Story 1 - Record and submit speaking practice (Priority: P1) MVP

**Goal**: Let a learner open Shadowing from a permitted segment, upload an eligible recording and start one owned assessment.

**Independent Test**: A permitted learner uploads/assesses one valid recording; locked, unowned, oversized or quarantined recordings create no assessment or quota event.

- [ ] T008 [P] [US1] Add recording upload, F06 access, ownership, scan-state and idempotent attempt integration tests in backend/src/test/java/net/pchinese/shadowing/ShadowingRecordingControllerIT.java.
- [ ] T009 [P] [US1] Add recording MIME/size/duration and retention-classification unit tests in backend/src/test/java/net/pchinese/shadowing/RecordingValidationServiceTest.java.
- [ ] T010 [US1] Implement validated recording upload, private storage and scan-state transitions in backend/src/main/java/net/pchinese/shadowing/application/RecordingService.java.
- [ ] T011 [US1] Implement attempt creation, request fingerprinting and F03 reservation before private work in backend/src/main/java/net/pchinese/shadowing/application/ShadowingAttemptService.java.
- [ ] T012 [US1] Implement multipart recording and attempt API DTOs/controllers in backend/src/main/java/net/pchinese/shadowing/api/ShadowingRecordingController.java.
- [ ] T013 [US1] Implement contract-bound recording client, MediaRecorder upload, permission/error handling and locked-segment state in frontend/src/api/shadowing.js and frontend/src/features/shadowing/RecordingPanel.jsx.

## Phase 4: User Story 2 - Receive and manage feedback (Priority: P1)

**Goal**: Produce a safe owned score/feedback result through the private speech service and enforce retention without retaining transcripts.

**Independent Test**: A completed owned attempt displays score/feedback; invalid/private-provider failure refunds once, no transcript persists and other users/Admin cannot read it.

- [ ] T014 [P] [US2] Add private Spring Boot-to-ai-service schema, HMAC/mTLS and no-storage-credential contract tests in backend/src/test/java/net/pchinese/shadowing/ShadowingAssessmentContractIT.java.
- [ ] T015 [P] [US2] Add ai-service structured speech-provider adapter tests with transcript discard in ai-service/src/shadowing/shadowingAssessment.test.ts.
- [ ] T016 [US2] Implement private HMAC/mTLS assessment handler and dedicated speech-engine adapter with bounded schema output in ai-service/src/shadowing/shadowingAssessment.ts.
- [ ] T017 [US2] Implement private assessment client, result validation, F03 success/refund settlement and approved practice-summary update in backend/src/main/java/net/pchinese/shadowing/application/ShadowingAssessmentService.java.
- [ ] T018 [US2] Implement owner-only attempt list/detail endpoints and retention cleanup job in backend/src/main/java/net/pchinese/shadowing/api/ShadowingAttemptController.java and backend/src/main/java/net/pchinese/shadowing/application/ShadowingRetentionJob.java.
- [ ] T019 [US2] Implement processing, completed, failed and expired feedback UI without transcript/playback storage details in frontend/src/features/shadowing/ShadowingResultPanel.jsx.

## Phase 5: Polish and Cross-Cutting Concerns

- [ ] T020 [P] Add Shadowing upload, quota retry/refund, provider failure and retention E2E coverage in frontend/e2e/f08-shadowing.spec.js.
- [ ] T021 Verify ai-service has no public route, Mastra memory, database write or raw-recording log in ai-service/src/shadowing/ and run specs/F08-shadowing-practice/quickstart.md.

## Dependencies and Execution Order

- F08 requires F01 identity, F03 allowance and F06 permitted segment/progress context.
- T002 establishes the ai-service runtime before any private Shadowing handler, contract or test work.
- Phase 2 blocks both stories.
- US2 depends on the recording and idempotent attempt created in US1.

## Parallel Opportunities

- T002, T003, T005 and T006 can proceed in parallel after T001.
- T008/T009 and T014/T015 are independent backend/ai-service test tasks.
- T013 can proceed once T012 fixes the public upload contract.

## Implementation Strategy

Release safe recording upload and attempt creation first. Enable speech assessment only after private contract, quota settlement, transcript discard and provider-failure tests pass.
