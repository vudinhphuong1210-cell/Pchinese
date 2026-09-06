<!--
Sync Impact Report
- Version change: 1.0.0 -> 1.1.0
- Modified principles: II. Fixed Architecture and Server Authority; III. Security, Privacy and
  Data Integrity; V. Testable Quality and Accessible Learning Experience.
- Added sections: AI Service constraints in Layer-Specific Constraints.
- Removed sections: none.
- Follow-up TODOs: none.
-->

# Pchinese Spec Kit Constitution

> **Purpose:** Executable governance for Spec Kit workflows. This file summarizes the rules that
> specs, plans and tasks MUST follow; it does not replace the project's canonical governance.
>
> **Canonical sources:** `CONSTITUTION.md` at the repository root is project law.
> `frontend/CONSTITUTION.md` and `backend/CONSTITUTION.md` add binding layer rules. If a conflict
> exists, the root constitution prevails; this file MUST never weaken any of those documents.

## Core Principles

### I. Canonical Contracts and Layer Boundaries

Every Spec Kit artifact MUST identify the affected layer and comply with the root constitution,
`AGENT.md`, `CLAUDE.md`, the relevant feature specification and the relevant layer contract.
Frontend owns presentation, accessibility and typed API consumption. Backend owns business rules,
authorization, transactions, persistence and provider integrations. A full-stack feature MUST
apply both layer contracts. No plan or task may move a responsibility across that boundary merely
to simplify implementation.

### II. Fixed Architecture and Server Authority

Pchinese MUST remain a React 18 + TypeScript + Vite SPA and a Spring Boot 3.4.5 + Java 21 modular
monolith backed by PostgreSQL 18. Persistence MUST use Spring Data JPA repositories; application
raw SQL is prohibited. The sole approved supporting-service exception is a private Node.js +
TypeScript Mastra `ai-service`, introduced only by an approved AI feature specification. It MAY
orchestrate AI providers but MUST NOT own product data, business decisions or public API authority.
No other microservice, Kafka, Redis, API gateway, NoSQL store or unapproved framework may be
introduced through a feature plan.

The backend is the authority for authentication, `ADMIN`, ownership, Premium entitlement, AI
quota, score, SRS scheduling, lifecycle transitions and retention. React route visibility, local
state, JWT-derived convenience data and client request fields MUST NOT be treated as authority.

### III. Security, Privacy and Data Integrity

Every protected API MUST validate the JWT signature, approved algorithm, `kid`, issuer, audience,
type, expiry, active session and `authzVersion`. Passwords MUST use bcrypt with cost at least 12.
Secrets, credentials, raw tokens, provider keys and unnecessary learner-private data MUST NOT be
committed, logged or exposed in API errors.

Services MUST enforce learner ownership, server-managed `ADMIN` authorization and entitlement/AI
quota before each applicable operation. `ADMIN` MUST NOT bypass access to private learner data.
AI and media providers MUST be called only through controlled backend adapters with minimized,
validated input. React MUST NOT call Mastra, an LLM provider or a speech provider directly. Before
calling `ai-service`, Spring Boot MUST validate identity, ownership, input safety, rate limits,
idempotency and AI quota, then use its private HMAC/mTLS boundary.

`ai-service` MUST NOT accept browser JWTs, query Pchinese product tables, expose Mastra default
routes or Studio publicly, or persist learner conversation data in Mastra Memory during MVP.
Provider credentials remain deployment secrets and logs or traces MUST NOT retain raw
learner-private payloads. Mastra MAY return structured explanations and feedback, but Spring Boot
remains authoritative for Dictation scores, SRS scheduling, HSK classification, lesson access,
entitlements, quota and progress. Audio-pronunciation assessment MUST use a dedicated speech
engine. Plans affecting recording, chat, profile or learning data MUST include the applicable
consent, retention, deletion and audit requirements.

### IV. Contract-First Change and Migration Safety

Every bounded change MUST begin with a feature specification that states the actor, entry point,
access rule, state changes, failure cases, API impact, acceptance criteria, tests and non-goals.
Public APIs MUST use `/api/v1/*` and the `{ success, data, error, meta }` envelope.

`DATA_short.md` is the canonical MVP schema contract. `DATA.md` is target architecture and MUST
NOT be used as a migration source until its feature and mapping are approved. Flyway migrations
MUST be approved schema changes, focused, tested against a clean database and consolidated while
unmerged. A migration already applied to shared or production environments MUST NOT be edited or
deleted.

### V. Testable Quality and Accessible Learning Experience

Implementation MUST include relevant JUnit 5 + Mockito backend tests and Jest frontend tests.
Authorization-sensitive changes MUST test ownership, `ADMIN`, Free/Premium entitlement and AI
quota where applicable. Integration tests MUST cover API validation, response/error envelopes and
both happy and error paths. Provider adapters MUST be mocked outside controlled environments. AI
features MUST test the typed Spring Boot <-> `ai-service` contract, schema-validate agent output
before persistence, and cover quota reservation, idempotent retry and safe provider failures.

Frontend work MUST use TypeScript strict mode without `any`, typed API clients and the semantic
design tokens in `frontend/DESIGN.md`. It MUST support keyboard navigation, focus visibility,
WCAG AA contrast, responsive touch targets and loading, empty and recoverable error states.

## Layer-Specific Constraints

### Frontend

- Read and obey `frontend/AGENT.md`, `frontend/CLAUDE.md`, `frontend/CONSTITUTION.md` and
  `frontend/DESIGN.md` before planning frontend work.
- Use React components, typed clients in `src/api/` and semantic theme tokens. Do not use `any`,
  hard-coded theme colours, direct provider calls or browser storage for access/refresh tokens.
- Client validation and route guards improve usability only; backend errors and decisions remain
  authoritative.

### Backend

- Read and obey `backend/AGENT.md`, `backend/CLAUDE.md` and `backend/CONSTITUTION.md` before
  planning backend work.
- Preserve Controller → Service → Repository → Entity boundaries. Controllers use validated DTOs;
  services own transactions and business decisions; repositories use Spring Data JPA only.
- Use `DATA_short.md` for schema work. Plans that modify auth, quota, role, ownership or learner
  data MUST state transaction, concurrency, audit and privacy consequences explicitly.

### AI Service

- Read and obey the root `CONSTITUTION.md` and `CLAUDE.md` before planning an AI feature.
- Keep `ai-service` private behind Spring Boot. It MUST accept only the approved authenticated
  service contract and MUST NOT expose public Mastra routes, Studio, browser-token authentication
  or direct product-database access.
- It MAY orchestrate providers and return schema-validated structured feedback only. Spring Boot
  retains authority for all product state, access, entitlement, quota, scores, scheduling and
  progress decisions.

## Development Workflow and Review Gates

1. Start with `$speckit-specify` for a new bounded feature or approved material change.
2. Use `$speckit-clarify` before planning when the actor, security rule, state transition, API
   contract or acceptance criterion is ambiguous.
3. Use `$speckit-plan` to produce the technical approach, then `$speckit-tasks` to create
   dependency-ordered, layer-aware work.
4. Use `$speckit-analyze` after task generation and resolve cross-artifact conflicts before
   implementation. Use `$speckit-implement` only when the artifacts describe the intended work.
5. Review MUST verify the applicable Definition of Done: contract compatibility, validation,
   authorization, migration safety, privacy impact, tests, lint/format/build and accessibility.
6. A plan MUST be revised when implementation or review reveals a conflict with a canonical source;
   code MUST NOT silently become the new specification.
7. An AI feature MUST define the Spring Boot -> `ai-service` contract, HMAC/mTLS boundary, quota
   reservation and idempotency behaviour, provider-failure handling, structured-output validation,
   privacy controls and the limits of agent authority before implementation.

## Governance

This document is a synchronized Spec Kit derivative, not an independent source of project law.
The root `CONSTITUTION.md` is canonical and requires unanimous team approval for any semantic
amendment. Relevant frontend and backend constitutions are additionally binding for their layers.
When any canonical constitution changes, this file MUST be synchronized in the same review or
immediately afterward; it may clarify but MUST NOT relax a canonical rule. This amendment aligns
the Spec Kit derivative with the root constitution amended on 2026-09-05.

Amendments MUST record rationale, affected principles and resulting artifact impacts. This document
uses semantic versioning: MAJOR for incompatible governance redefinition or removal, MINOR for a
new principle or material obligation, and PATCH for non-semantic clarification. Every plan, task
set, implementation review and migration review MUST verify compliance with the applicable rules.

**Version**: 1.1.0 | **Ratified**: 2026-09-03 | **Last Amended**: 2026-09-05
