# ai-service Constitution

# Ratified: 2026-09-05 | Project: Pchinese | Version: 1.0

# RULE: `../CONSTITUTION.md` is project law and overrides this document.

## ARTICLE 1 -- ROLE AND ARCHITECTURE

`ai-service` is the single approved private Node.js + TypeScript Mastra supporting service. It MAY
orchestrate approved LLM and speech providers, prompts, agents, workflows and typed safe output.
It MUST NOT become a public product API, own product data or make business decisions. No second
service, database, cache, queue, public ingress or new infrastructure component may be introduced
without an approved feature specification and any required root-Constitution amendment.

## ARTICLE 2 -- AUTHORITY AND DATA OWNERSHIP

Spring Boot is the sole authority for authentication, active sessions, learner ownership, `ADMIN`,
entitlement, AI quota, rate limiting, idempotency, persistence, encryption, deletion, Dictation
scores, SRS scheduling, HSK classification, lesson access and progress. `ai-service` MUST accept
only minimized, already-validated context and return typed, schema-validated explanations or
feedback. It MUST NOT access Pchinese product tables, PostgreSQL or product-data stores.

The service MUST NOT persist learner conversations, messages, prompts, attempts, recordings or
product state. Mastra Memory remains disabled for MVP. A dedicated speech engine is mandatory for
actual audio-pronunciation assessment; Mastra MAY orchestrate and explain its output but MUST NOT
authoritatively score pronunciation.

## ARTICLE 3 -- PRIVATE ACCESS AND SECURITY

The service accepts only private Spring Boot requests authenticated by HMAC or mTLS with replay
protection. It MUST reject browser JWTs and unauthenticated or replayed requests. Mastra Studio,
default routes, agent/workflow routes and provider endpoints MUST NOT be publicly reachable.

Provider credentials, HMAC material and mTLS keys/certificates MUST be deployment-managed secrets.
They MUST NOT be committed, returned, logged, traced or placed in evaluation fixtures. Logs, traces
and errors MUST redact raw learner-private content, prompts, recordings, browser JWTs and provider
payloads. Internal errors MUST be safe and free of stack traces or provider internals.

## ARTICLE 4 -- VALIDATION, SAFETY AND PROVIDERS

Every internal request and every agent/provider result MUST pass an explicit typed schema. Input and
output safety guards MUST run before provider dispatch and before returning a result. Provider calls
MUST use bounded timeouts and controlled failure mapping. An invalid, unsafe or malformed result
MUST be rejected rather than persisted or returned as a best-effort answer.

## ARTICLE 5 -- QUALITY AND TESTING

Each change MUST have unit, integration and contract coverage proportional to its risk. The typed
Spring Boot <-> `ai-service` contract MUST be tested for valid input, invalid input, unsafe input,
malformed output, provider timeout and provider failure. Tests and evaluation fixtures MUST use
only approved synthetic, non-private data. TypeScript compilation, linting and formatting MUST pass
before review.

## ARTICLE 6 -- GOVERNANCE

Before a change, read the root `AGENT.md`, `CLAUDE.md`, `CONSTITUTION.md`, the applicable feature
specification and this directory's guidance. A change that conflicts with a root rule is prohibited.
Semantic changes to the service boundary, product authority, data handling or provider exposure
require an approved feature specification and the root Constitution's amendment process. Reviews
MUST verify compliance with this document and the root Definition of Done.
