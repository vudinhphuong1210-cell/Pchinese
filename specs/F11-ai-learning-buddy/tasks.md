---
description: "Actionable implementation tasks for F11 AI Learning Buddy"
---

# Tasks: F11 AI Learning Buddy

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f11-openapi.yaml and quickstart.md.

**Tests**: Required for ownership, scenario/safety pre-gate, F03 idempotency, private contract, schema validation and deletion/context isolation.

## Phase 1: Setup

- [ ] T001 Create AI Buddy backend packages in backend/src/main/java/net/pchinese/aibuddy/ and private ai-service modules in ai-service/src/ai-buddy/.
- [ ] T002 [P] Create the AI Buddy conversation route and screen shell in frontend/src/features/ai-buddy/AiBuddyPage.tsx and frontend/src/routes/aiBuddyRoutes.tsx.
- [ ] T003 [P] Configure ai-service to disable public Mastra Studio/default routes in ai-service/src/server.ts.

## Phase 2: Foundational

- [ ] T004 Add ai_conversations and ai_messages encrypted-content schema, ordered-message and idempotency constraints in backend/src/main/resources/db/migration/V011__f11_ai_buddy.sql.
- [ ] T005 [P] Implement conversation/message JPA entities, owner query methods and lifecycle enums in backend/src/main/java/net/pchinese/aibuddy/persistence/.
- [ ] T006 [P] Implement private HMAC/mTLS client configuration, replay protection and typed DTO boundary in backend/src/main/java/net/pchinese/aibuddy/infrastructure/AiBuddyClient.java.
- [ ] T007 Implement safe scenario/input gate before any persistence or F03 reservation in backend/src/main/java/net/pchinese/aibuddy/application/AiBuddySafetyService.java.

## Phase 3: User Story 1 - Hold a private learning conversation (Priority: P1) MVP

**Goal**: Let a learner create a fixed-scenario conversation and send one eligible message for one bounded bilingual reply.

**Independent Test**: An owner creates a no-cost conversation, sends a safe in-scenario message with allowance, gets one valid reply and no cross-conversation context.

- [ ] T008 [P] [US1] Add conversation/message ownership, scenario, unsafe-input, 1,000-character and quota-precheck integration tests in backend/src/test/java/net/pchinese/aibuddy/AiBuddyControllerIT.java.
- [ ] T009 [P] [US1] Add private request context-limit and structured-output schema contract tests in backend/src/test/java/net/pchinese/aibuddy/AiBuddyPrivateContractIT.java.
- [ ] T010 [P] [US1] Add stateless Mastra/provider orchestration tests with no durable memory in ai-service/src/ai-buddy/aiBuddyResponder.test.ts.
- [ ] T011 [US1] Implement owner conversation creation and safe list/read projection in backend/src/main/java/net/pchinese/aibuddy/application/AiConversationService.java.
- [ ] T012 [US1] Implement private ai-service responder that accepts only service-authenticated bounded context and returns Chinese reply, Vietnamese explanation and at most one suggestion in ai-service/src/ai-buddy/aiBuddyResponder.ts.
- [ ] T013 [US1] Implement message idempotency, F03 reserve/succeed/refund, <=10 COMPLETE-context selection and output validation in backend/src/main/java/net/pchinese/aibuddy/application/AiMessageService.java.
- [ ] T014 [US1] Implement public conversation/message DTOs/controllers and private client endpoint mapping in backend/src/main/java/net/pchinese/aibuddy/api/AiBuddyController.java.
- [ ] T015 [US1] Implement typed conversation/message client, scenario chooser, composer and bounded reply UI in frontend/src/api/aiBuddy.ts and frontend/src/features/ai-buddy/ConversationPanel.tsx.

## Phase 4: User Story 2 - Control conversation history (Priority: P1)

**Goal**: Let a learner rename/delete only their own conversation and ensure deleted content never becomes model context.

**Independent Test**: Owner rename/delete updates only their list; another learner/Admin cannot act, and deleted/pending/failed messages are excluded from context.

- [ ] T016 [P] [US2] Add rename/delete ownership, Admin denial and context-exclusion integration tests in backend/src/test/java/net/pchinese/aibuddy/AiConversationLifecycleIT.java.
- [ ] T017 [US2] Implement rename/delete transactions and context-eligible message query in backend/src/main/java/net/pchinese/aibuddy/application/AiConversationLifecycleService.java.
- [ ] T018 [US2] Implement rename/delete controller operations with safe not-found projection in backend/src/main/java/net/pchinese/aibuddy/api/AiConversationLifecycleController.java.
- [ ] T019 [US2] Implement accessible conversation list rename/delete confirmation and deleted-state UI in frontend/src/features/ai-buddy/ConversationList.tsx.
- [ ] T020 [US2] Add Jest coverage for scenario immutability, safe rejection and deletion UI in frontend/src/features/ai-buddy/AiBuddyPage.test.tsx.

## Phase 5: Polish and Cross-Cutting Concerns

- [ ] T021 [P] Add AI Buddy create/send/retry/quota/delete E2E coverage with mocked private provider in frontend/e2e/f11-ai-buddy.spec.ts.
- [ ] T022 Verify raw learner messages are absent from ai-service logs/traces, Mastra memory and persistence in ai-service/src/ai-buddy/ and run specs/F11-ai-learning-buddy/quickstart.md.
- [ ] T023 Run F03/F11 allowance-refund, backend/ai-service/frontend contract and test suites from backend/pom.xml, ai-service/package.json and frontend/package.json.

## Dependencies and Execution Order

- F11 requires F01 identity and F03 internal allowance reservation.
- Phase 2 blocks both stories.
- US2 depends on the conversation lifecycle and context query from US1.

## Parallel Opportunities

- T002, T003, T005 and T006 can proceed in parallel after T001.
- T008/T009/T010 are independent backend/private-service tests.
- Frontend T015 can proceed after T014 defines public typed responses.

## Implementation Strategy

Release fixed-scenario conversation/message flow only after safety pre-gate, F03 reservation and private schema validation are complete. Add rename/delete after model context is proven to exclude deleted and non-complete content.

