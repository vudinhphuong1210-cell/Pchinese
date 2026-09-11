# Implementation Plan: F11 AI Learning Buddy

**Branch**: F11-ai-learning-buddy | **Date**: 2026-09-06 | **Spec**: spec.md
**Input**: Feature specification from D:\schinese\specs\F11-ai-learning-buddy\spec.md

## Summary

Deliver a private learner-owned AI conversation feature with exactly one immutable scenario per conversation: DAILY_CONVERSATION, VOCABULARY_GRAMMAR, or ROLE_PLAY. Spring Boot validates safety, context, ownership, idempotency and F03 allowance; it encrypts each conversation with a dedicated wrapped DEK, records privacy-safe processing audit events, and calls private ai-service. The single private Node.js + TypeScript ai-service uses Mastra as stateless orchestration, independently validates typed input/output safety, and returns only schema-conformant Chinese response, concise Vietnamese explanation, and at most one suggestion.

## Technical Context

| Area | Decision |
| --- | --- |
| Public API | Spring Boot under /api/v1; browser never calls ai-service or provider directly |
| Send-message response | Synchronous public request: wait at most 30 seconds and return a COMPLETE pair or safe terminal error; same-fingerprint retries join the original request until its original deadline; malformed/invalid/output-policy-rejected model output is `502 PROVIDER_ERROR`, timeout/unavailability is `503 SERVICE_UNAVAILABLE`; no PENDING projection or frontend polling |
| Conversation | Learner owns conversations; scenario is selected on creation and cannot change |
| Message input | Plain text only, maximum 1000 characters; attachments, rich text and arbitrary prompt tools are out of scope |
| Pre-AI gate | Spring Boot allows Chinese-learning requests matching the selected scenario in Vietnamese or Chinese; it versionedly rejects ambiguous input, general chat, prompt injection, PII, sexual content, violence/self-harm, hate and illegal activity without a provider call or allowance reservation |
| Rate limit | Five send attempts per learner per rolling minute; accepted and locally rejected input count, idempotent replays do not, and excess returns `429` with `Retry-After` |
| Context | Spring Boot supplies only the current safe learner message plus at most 10 recent COMPLETE messages from the same conversation |
| Private handoff | POST /internal/v1/ai-buddy/respond over private TLS; lowercase-hex HMAC-SHA-256 signs the exact UTF-8 newline-delimited uppercase method, path, timestamp, nonce and lowercase-hex exact-body SHA-256 in that order, verified by the normative contract fixture; request includes correlation/request IDs and fixed `AI_BUDDY` capability; every success/failure returns the correlation ID; reject invalid/replayed nonce or timestamp outside a five-minute accepted clock window |
| ai-service safety | Independently schema-validate and safety-check typed input before provider dispatch, then schema-validate and safety-check output before response; Spring Boot remains the authoritative pre-AI gate and persistence validator |
| Runtime and dependency | One private Node.js + TypeScript container, one Mastra instance and strict npm lockfile with `@mastra/core`, Zod and `@ai-sdk/deepseek`. Runtime calls direct DeepSeek API using deployment-only `DEEPSEEK_API_KEY` and required `DEEPSEEK_MODEL` restricted to `deepseek-v4-flash` or `deepseek-v4-pro`; no public ingress, database, cache, queue or Mastra Memory |
| Deadline budget | Provider call <=22 seconds; Spring Boot private-client call <=25 seconds; public response <=30 seconds; no automatic provider retry |
| Provider privacy | Required `SERVICE_OPERATION` only; `AI_MODEL_IMPROVEMENT` off; fail closed unless the DeepSeek key/model configuration has a DPA, no-training default, encrypted transport, <=24-hour vendor retention, deletion/attestation capability and approved transfer region; append content-free processing audit events |
| Durable data | Spring Boot stores encrypted titles/content using a random conversation DEK wrapped by the learner's KMS-wrapped user DEK, plus database-backed deletion work, retention-notice delivery state, legal-hold metadata, `last_message_at`, policy versions and content-free processing audit events; ai-service has no database, Mastra memory, public route, cache, queue, KMS credential, DEK or durable conversation transcript |
| Retention | Daily backend sweep uses `last_message_at` or `created_at` for empty conversations, creates one idempotency-keyed 30-day notice record and safely retries delivery, then destroys only the affected wrapped conversation DEK, records deletion attestation and processes database-backed hard-delete work after 12 months unless an active content-free legal hold exists |
| Allowance | One F03 AI_BUDDY usage only per eligible learner message; reservation settles success or exactly-once refund |
| Safe output | Spring Boot persists output only after strict schema, size and safety validation |

## Constitution Check

| Principle | Status | Evidence |
| --- | --- | --- |
| Backend remains authority | Pass | Spring Boot owns identity, safety, scenarios, quotas, persistence and response publication. |
| AI boundary is private | Pass | One private ai-service container accepts only the typed HMAC-authenticated contract and validates safety before provider dispatch. |
| Metered AI | Pass | F03 reservation/idempotency governs every eligible response. |
| Privacy by default | Pass | Per-conversation envelope encryption, content-free provider/deletion audit, no-training provider gate and no ai-service durable memory, database, key material or trace containing learner text. |
| Safe and bounded learning | Pass | Scenario scope, Spring Boot pre-gate, ai-service defense-in-depth guards, output schema, deadline budget and short context window constrain the assistant. |

## Project Structure

### Documentation

    specs/F11-ai-learning-buddy/
    ├── plan.md
    ├── research.md
    ├── data-model.md
    ├── quickstart.md
    └── contracts/
        ├── f11-openapi.yaml
        └── f11-hmac-test-vector.json

### Source Code

    backend/
    ├── src/main/java/.../aibuddy/
    ├── src/main/java/.../allowance/
    ├── src/main/java/.../security/crypto/
    └── runtime Flyway location: ../supabase/migrations/
    ai-service/
    ├── package.json
    ├── package-lock.json
    ├── tsconfig.json
    ├── .env.example
    ├── Dockerfile
    └── src/
        ├── server.ts
        ├── config/{env.ts,modelRegistry.ts,observability.ts}
        ├── http/routes/internalAiBuddy.route.ts
        ├── http/middleware/{internalAuth.ts,requestValidation.ts,errorHandler.ts}
        ├── mastra/index.ts
        ├── features/ai-buddy/{chineseTutor.agent.ts,chineseTutor.prompt.ts,aiBuddy.schema.ts,aiBuddy.service.ts}
        ├── adapters/llm/modelProvider.ts
        └── shared/{contracts/{internalRequest.ts,internalResponse.ts,errors.ts},safety/{inputGuard.ts,outputGuard.ts}}
    frontend/
    └── src/features/ai-buddy/

## Implementation Phases

1. Add KMS-envelope user/conversation key metadata, encrypted conversations/messages, processing audit, ordering, lifecycle and idempotency constraints.
2. Implement owner-only conversation CRUD and paginated message retrieval.
3. Validate safe input before persisting eligible messages or reserving F03 usage.
4. Scaffold the private strict-TypeScript Mastra service with the direct DeepSeek V4 adapter, its private-only runtime, HMAC/replay middleware, typed contract and defense-in-depth safety guards.
5. Implement the exact signed private ai-service contract with `AI_BUDDY` capability/correlation propagation, 22/25/30-second deadline budget, public provider error mapping and output validation.
6. Validate the DeepSeek deployment privacy gate and KMS envelope configuration; inject the real DeepSeek key only through deployment secrets.
7. Settle allowance/result atomically and implement safe retry/failure behavior plus content-free processing audit writes.
8. Build React scenario selection, conversation list, message composer and response states.
9. Implement the daily retention notice/deletion sweep without introducing a new queue or cache.
10. Test ownership, context isolation, quota, retries, exact HMAC fixture, service-side safety, deadline budget, invalid output, provider policy gate, key erasure, retention sweep and no durable ai-service memory.

## Complexity Tracking

No constitution exceptions are required.
