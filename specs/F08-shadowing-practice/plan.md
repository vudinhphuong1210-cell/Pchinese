# Implementation Plan: F08 Shadowing Practice

**Branch**: F08-shadowing-practice | **Date**: 2026-09-06 | **Spec**: spec.md
**Input**: Feature specification from D:\schinese\specs\F08-shadowing-practice\spec.md

## Summary

Deliver a protected shadowing practice screen for unlocked lesson segments. Spring Boot accepts and scans recordings, creates an idempotent assessment attempt, reserves the F03 AI allowance, and persists only validated assessment results. The private Node.js ai-service uses Mastra solely to orchestrate an approved speech-assessment provider; it receives no browser traffic, JWT, storage credential, or durable memory.

## Technical Context

| Area | Decision |
| --- | --- |
| Public API and authority | Spring Boot under /api/v1; verifies authentication, lesson unlock, ownership, quotas, idempotency, persistence, and retention jobs |
| AI boundary | Spring Boot calls POST /internal/v1/shadowing/assess over mTLS or HMAC with replay protection; ai-service is stateless |
| Client | React screen, MediaRecorder upload, attempt-status polling, typed API clients; never calls ai-service |
| Storage | Private encrypted object storage for original recording; application database holds metadata and encrypted feedback only |
| Validation | Server validates MIME, byte limit, duration, segment context and malware/scan status before assessment |
| Security | Owner-only recording and attempt access; no transcript is returned, persisted, or logged |
| Allowance | One F03 AI usage is charged only for an eligible assessment; reserve before provider work and refund exactly once on terminal failure |

## Constitution Check

| Principle | Status | Evidence |
| --- | --- | --- |
| Spring Boot is system of record | Pass | It owns auth, content gate, recording lifecycle, quota, scoring persistence, and cleanup. |
| ai-service is private/stateless | Pass | Private assessment contract only; no public routes, database, Mastra memory, or browser identity. |
| Protect learning content | Pass | A short-lived server-issued playback capability protects unlocked segment media; recording storage is private. |
| AI is safe and metered | Pass | Input gate precedes usage; structured output schema, timeout, idempotency, reservation and refund are server-controlled. |
| Privacy by default | Pass | No transcript storage or logging; recordings and feedback have explicit retention. |

## Project Structure

### Documentation

    specs/F08-shadowing-practice/
    ├── plan.md
    ├── research.md
    ├── data-model.md
    ├── quickstart.md
    └── contracts/
        └── f08-api.md

### Source Code

    backend/
    ├── src/main/java/.../shadowing/
    ├── src/main/java/.../storage/
    └── src/main/resources/db/migration/
    ai-service/
    └── src/shadowing/
    frontend/
    └── src/features/shadowing/

## Implementation Phases

1. Add Flyway migrations, entities, encrypted columns, repository queries, lifecycle and cleanup jobs.
2. Enforce lesson entitlement/unlock and create private recording upload/scan processing.
3. Implement idempotent attempt creation and F03 reservation/refund integration.
4. Implement the signed private Spring Boot to ai-service assessment contract and strict result schema validation.
5. Build React recording, upload, polling, result and retry UI.
6. Add authorization, quota, race, retention, provider-failure and no-transcript tests.

## Complexity Tracking

No constitution exceptions are required.

