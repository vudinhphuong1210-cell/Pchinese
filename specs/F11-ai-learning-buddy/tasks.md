---
description: "Actionable implementation tasks for F11 AI Learning Buddy"
---

# Tasks: F11 AI Learning Buddy

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/f11-openapi.yaml and quickstart.md.

**Tests**: Required for ownership, scenario/safety pre-gate, F03 idempotency, exact private HMAC contract, schema validation, provider-policy gate, envelope-key erasure, audit privacy and deletion/context isolation.

**Implementation status (2026-09-11)**: No F11 task is accepted as complete yet. Any current
code or migration is work-in-progress and must be reviewed, completed, and validated against the
relevant tests before its task may be marked `[X]`.

## Phase 1: Setup

- [ ] T001 Create AI Buddy backend packages and the strict-TypeScript ai-service scaffold, including pinned npm lockfile dependencies (`@mastra/core`, Zod and `@ai-sdk/deepseek`), scripts, `.env.example` names/placeholders, Dockerfile and the planned src/{config,http,mastra,features/ai-buddy,adapters,shared,test} structure in backend/src/main/java/net/pchinese/aibuddy/ and ai-service/.
- [ ] T002 [P] Create the AI Buddy conversation route and screen shell in frontend/src/features/ai-buddy/AiBuddyPage.jsx and frontend/src/routes/aiBuddyRoutes.jsx.
- [ ] T003 [P] Configure the one-container ai-service private-only listener, validated bind/port configuration, no public ingress, disabled Mastra Studio/default routes, and fail-closed direct-DeepSeek environment validation for `DEEPSEEK_API_KEY`, V4-only `DEEPSEEK_MODEL`, region/DPA/no-training/<=24-hour retention/deletion-attestation in ai-service/{src/server.ts,src/config/env.ts,src/config/modelRegistry.ts,.env.example}.

## Phase 2: Foundational

- [ ] T004 Add `user_data_keys`, conversation wrapped-DEK/deletion-work metadata, message retry-deadline fields, versioned audit metadata, `ai_retention_notices`, `ai_conversation_legal_holds`, indexes and constraints from DATA_short.md in the configured Flyway location, supabase/migrations/20260911200000_f11_ai_buddy.sql.
- [ ] T005 Implement KMS envelope-key abstraction, user/conversation DEK lifecycle, conversation/message/audit/retention-notice/legal-hold JPA entities, owner query methods, `last_message_at` updates and lifecycle enums in backend/src/main/java/net/pchinese/{security/crypto/,aibuddy/persistence/}.
- [ ] T006 [P] Implement the 25-second private-TLS HMAC-SHA-256 client using the exact lowercase-hex UTF-8 newline canonicalization and `contracts/f11-hmac-test-vector.json`, fixed `AI_BUDDY` capability, correlated success/error DTOs and typed boundary in backend/src/main/java/net/pchinese/aibuddy/infrastructure/AiBuddyClient.java.
- [ ] T007 Implement the per-learner five-attempt rolling-minute send-rate gate (excluding idempotent replays), then the versioned deterministic allow/deny scenario and safety gate: allow Vietnamese/Chinese learning requests matching the selected scenario and reject ambiguous input, general chat, prompt injection, PII, sexual content, violence/self-harm, hate and illegal activity before any provider call, persistence or F03 reservation; write the active input-policy version only as content-free audit metadata in backend/src/main/java/net/pchinese/aibuddy/application/AiBuddySafetyService.java.

## Phase 3: User Story 1 - Hold a private learning conversation (Priority: P1) MVP

**Goal**: Let a learner create a fixed-scenario conversation and send one eligible message for one bounded bilingual reply.

**Independent Test**: An owner creates a no-cost conversation, sends a safe in-scenario message with allowance, gets one valid reply and no cross-conversation context.

- [ ] T008 [P] [US1] Add conversation/message ownership, scenario, versioned input/output-policy tests, accepted Vietnamese/Chinese learning input and rejected ambiguous, general-chat, prompt-injection, PII and risk-category input, five-attempt rolling-minute throttle, same-fingerprint in-flight retry join, changed-request conflict, 1,000-character/quota-precheck and `502 PROVIDER_ERROR`/`503 SERVICE_UNAVAILABLE` integration tests in backend/src/test/java/net/pchinese/aibuddy/AiBuddyControllerIT.java.
- [ ] T009 [P] [US1] Add Spring Boot private-contract tests consuming the normative HMAC fixture, fixed `AI_BUDDY` capability, correlation ID, five-minute timestamp/nonce replay, request context-limit, typed safe error, 25-second deadline and structured-output schema tests in backend/src/test/java/net/pchinese/aibuddy/AiBuddyPrivateContractIT.java and specs/F11-ai-learning-buddy/contracts/f11-hmac-test-vector.json.
- [ ] T010 [P] [US1] Add private-route denial, fixture-compatible HMAC/replay rejection and input/output-safety unit tests in ai-service/src/test/unit/{internalAuth.test.ts,aiBuddySafety.test.ts}; correlated contract tests in ai-service/src/test/contract/internalAiBuddyContract.test.ts; and 22-second provider-deadline, provider-policy-gate and stateless mocked-provider integration tests in ai-service/src/test/integration/aiBuddyResponder.test.ts.
- [ ] T011 [US1] Implement owner conversation creation and safe list/read projection in backend/src/main/java/net/pchinese/aibuddy/application/AiConversationService.java.
- [ ] T012 [US1] Implement private ai-service exact-HMAC/replay middleware, typed request/response/correlated-error contracts, input/output safety guards, private route, redacted observability, Mastra registration, Chinese tutor agent, DeepSeek V4 policy gate and 22-second no-retry `@ai-sdk/deepseek` adapter boundary in ai-service/src/{http/{routes/internalAiBuddy.route.ts,middleware/internalAuth.ts,middleware/requestValidation.ts,middleware/errorHandler.ts},shared/{contracts/{internalRequest.ts,internalResponse.ts,errors.ts},safety/{inputGuard.ts,outputGuard.ts}},config/{env.ts,modelRegistry.ts,observability.ts},mastra/index.ts,features/ai-buddy/{aiBuddy.schema.ts,chineseTutor.agent.ts,chineseTutor.prompt.ts,aiBuddy.service.ts},adapters/llm/modelProvider.ts}.
- [ ] T013 [US1] Implement per-conversation DEK encryption, message idempotency with an immutable original processing deadline and same-fingerprint retry join, F03 reserve/succeed/refund, <=10 COMPLETE-context selection, `last_message_at` update, output-policy-v1 validation, public `502 PROVIDER_ERROR`/`503 SERVICE_UNAVAILABLE` mapping, and content-free policy-versioned processing audit writes in backend/src/main/java/net/pchinese/aibuddy/application/AiMessageService.java.
- [ ] T014 [US1] Implement public conversation/message DTOs/controllers and private client endpoint mapping in backend/src/main/java/net/pchinese/aibuddy/api/AiBuddyController.java.
- [ ] T015 [US1] Implement contract-bound synchronous conversation/message client, scenario chooser, composer and bounded reply UI with a 30-second terminal wait and no polling in frontend/src/api/aiBuddy.js and frontend/src/features/ai-buddy/ConversationPanel.jsx.

## Phase 4: User Story 2 - Control conversation history (Priority: P1)

**Goal**: Let a learner rename/delete only their own conversation and ensure deleted content never becomes model context.

**Independent Test**: Owner rename/delete updates only their list; another learner/Admin cannot act, and deleted/pending/failed messages are excluded from context.

- [ ] T016 [P] [US2] Add rename/delete ownership, Admin denial, context exclusion, `last_message_at` retention-anchor, empty-conversation fallback, idempotency-keyed 30-day notice/retry, privacy-workflow legal-hold blocking without chat access, database-backed hard-delete work, per-conversation DEK-only erasure and content-free deletion-attestation integration tests in backend/src/test/java/net/pchinese/aibuddy/AiConversationLifecycleIT.java.
- [ ] T017 [US2] Implement rename/delete transactions, immediate hide/context exclusion, affected-conversation DEK destruction before durable database-backed hard deletion, content-free deletion attestation, context-eligible message query, privacy-workflow legal-hold check without a learner/admin API, and daily idempotent retention notice/deletion-work sweeps through the configured mail adapter without a new queue/cache in backend/src/main/java/net/pchinese/aibuddy/{application/AiConversationLifecycleService.java,application/AiConversationRetentionService.java,infrastructure/AiConversationRetentionJob.java}.
- [ ] T018 [US2] Implement rename/delete controller operations with safe not-found projection in backend/src/main/java/net/pchinese/aibuddy/api/AiConversationLifecycleController.java.
- [ ] T019 [US2] Implement accessible conversation list rename/delete confirmation and deleted-state UI in frontend/src/features/ai-buddy/ConversationList.jsx.
- [ ] T020 [US2] Add Jest coverage for scenario immutability, safe rejection and deletion UI in frontend/src/features/ai-buddy/AiBuddyPage.test.jsx.

## Phase 5: Polish and Cross-Cutting Concerns

- [ ] T021 [P] Add AI Buddy create/send/retry/quota/delete E2E coverage with mocked private provider in frontend/e2e/f11-ai-buddy.spec.js.
- [ ] T022 Verify raw learner messages are absent from ai-service logs/traces, Mastra memory, DeepSeek-processing audit and persistence; only private routes are enabled; KMS/DEK material stays in Spring Boot; the DeepSeek V4 policy is fail-closed; the one-container replay-store assumption and synthetic-only fixtures hold in ai-service/src/{config/observability.ts,test/,features/ai-buddy/}; then run specs/F11-ai-learning-buddy/quickstart.md.
- [ ] T023 Run F03/F11 allowance-refund, exact HMAC-fixture, provider-policy, DEK-erasure/audit, backend/ai-service/frontend contract and test suites, TypeScript typecheck/lint/format, and quickstart validation from backend/pom.xml, ai-service/package.json and frontend/package.json.
- [ ] T024 Before direct-DeepSeek release, record the DeepSeek V4 model/transfer-region/DPA/no-training/<=24-hour-retention/deletion-attestation and KMS key reference, inject the real `DEEPSEEK_API_KEY` only into the deployment secret store, and verify the pinned DeepSeek/KMS adapters in ai-service/{package.json,src/adapters/llm/modelProvider.ts,.env.example} and backend/src/main/java/net/pchinese/security/crypto/.

## Dependencies and Execution Order

- F11 requires F01 identity and F03 internal allowance reservation.
- A live AI Buddy deployment requires the DeepSeek and KMS approval evidence in T024; provider adapters remain mocked in automated tests, but runtime uses direct DeepSeek only.
- Phase 2 blocks both stories.
- US2 depends on the conversation lifecycle and context query from US1.

## Parallel Opportunities

- T002, T003, T005 and T006 can proceed in parallel after T001.
- T008/T009/T010 are independent backend/private-service tests.
- Frontend T015 can proceed after T014 defines public response schemas.

## Implementation Strategy

Release fixed-scenario conversation/message flow through direct DeepSeek V4 only after the Spring Boot pre-gate, ai-service defense-in-depth safety guards, F03 reservation, exact signed private contract, DeepSeek-policy gate, envelope-key lifecycle and 22/25/30-second deadline behavior are complete. Add rename/delete and the retention sweep after model context is proven to exclude deleted and non-complete content. Direct DeepSeek is prohibited until T024's DPA/region/retention/attestation and KMS approval evidence plus deployment secret are complete. Do not expose the service or enable Mastra Memory at any stage.
