# Implementation Plan: F11 AI Learning Buddy

**Branch**: F11-ai-learning-buddy | **Date**: 2026-09-06 | **Spec**: spec.md
**Input**: Feature specification from D:\schinese\specs\F11-ai-learning-buddy\spec.md

## Summary

Deliver a private learner-owned AI conversation feature with exactly one immutable scenario per conversation: DAILY_CONVERSATION, VOCABULARY_GRAMMAR, or ROLE_PLAY. Spring Boot validates safety, context, ownership, idempotency and F03 allowance; it persists encrypted conversation state and calls private ai-service. The Node.js ai-service uses Mastra as stateless orchestration for the approved model and returns only schema-conformant Chinese response, concise Vietnamese explanation, and at most one suggestion.

## Technical Context

| Area | Decision |
| --- | --- |
| Public API | Spring Boot under /api/v1; browser never calls ai-service or provider directly |
| Conversation | Learner owns conversations; scenario is selected on creation and cannot change |
| Message input | Plain text only, maximum 1000 characters; attachments, rich text and arbitrary prompt tools are out of scope |
| Context | Spring Boot supplies only the current safe learner message plus at most 10 recent COMPLETE messages from the same conversation |
| Private handoff | POST /internal/v1/ai-buddy/respond with mTLS or HMAC, nonce, timestamp and correlation ID |
| Durable data | Spring Boot stores encrypted titles/content; ai-service has no database, Mastra memory, public route, or durable conversation transcript |
| Allowance | One F03 AI_BUDDY usage only per eligible learner message; reservation settles success or exactly-once refund |
| Safe output | Spring Boot persists output only after strict schema, size and safety validation |

## Constitution Check

| Principle | Status | Evidence |
| --- | --- | --- |
| Backend remains authority | Pass | Spring Boot owns identity, safety, scenarios, quotas, persistence and response publication. |
| AI boundary is private | Pass | ai-service gets minimal vetted context over authenticated service-to-service transport. |
| Metered AI | Pass | F03 reservation/idempotency governs every eligible response. |
| Privacy by default | Pass | Encrypted stored content; no ai-service durable memory, database or trace containing learner text. |
| Safe and bounded learning | Pass | Scenario scope, input guard, output schema and short context window constrain the assistant. |

## Project Structure

### Documentation

    specs/F11-ai-learning-buddy/
    ├── plan.md
    ├── research.md
    ├── data-model.md
    ├── quickstart.md
    └── contracts/
        └── f11-api.md

### Source Code

    backend/
    ├── src/main/java/.../aibuddy/
    ├── src/main/java/.../allowance/
    └── src/main/resources/db/migration/
    ai-service/
    └── src/ai-buddy/
    frontend/
    └── src/features/ai-buddy/

## Implementation Phases

1. Add encrypted conversations/messages, ordering, lifecycle and idempotency constraints.
2. Implement owner-only conversation CRUD and paginated message retrieval.
3. Validate safe input before persisting eligible messages or reserving F03 usage.
4. Implement signed private ai-service contract, Mastra orchestration and output schema validation.
5. Settle allowance/result atomically and implement safe retry/failure behavior.
6. Build React scenario selection, conversation list, message composer and response states.
7. Test ownership, context isolation, quota, retries, unsafe input, invalid output and no durable ai-service memory.

## Complexity Tracking

No constitution exceptions are required.

