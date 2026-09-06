# CONSTITUTION.md — Project Law

# Ratified: 2026-09-03 | Amended: 2026-09-05 | Team: Pchinese | Version: 1.3

# RULE: Any change to this document requires unanimous team approval.

## ARTICLE 1 — TECH STACK (immutable)

Backend runtime: Java 21  
Backend framework: Spring Boot 3.4.5  
Backend build tool: Maven  
Database: PostgreSQL 18 — no NoSQL without unanimous team approval  
Persistence: Spring Data JPA repositories only — no application-level raw SQL  
Frontend: React 18 + TypeScript + Vite — no class components  
Styling: Tailwind CSS 3.x — add custom CSS only when Tailwind is insufficient  
Frontend package tool: npm  
Authentication: JWT + bcrypt (cost factor >= 12)

Architecture: a modular monolith consisting of a React SPA and a Spring Boot REST API. The only approved supporting-service exception is a private Node.js + TypeScript Mastra `ai-service`, introduced only for an approved AI feature specification. It orchestrates AI providers only; it never owns product data, business decisions or public API authority. Do not introduce other microservices, Kafka, Redis or an API Gateway unless an approved feature specification and a Constitution amendment require it.

## ARTICLE 2 — CODING STANDARDS

Backend language: Java 21, following Spring Boot conventions and domain-module boundaries  
Frontend language: TypeScript strict mode; `any` is forbidden  
Persistence: repositories encapsulate persistence access through Spring Data JPA; services own business rules; API payloads use explicit DTOs  
Formatter: committed formatter configuration must be used  
Linter: ESLint for frontend; no lint, formatting, compilation or TypeScript errors may remain  
Naming: React components use PascalCase; utility files/functions use camelCase; API route segments use kebab-case; PostgreSQL tables use plural snake_case  
Comments: explain WHY, not WHAT. Remove debug code, dead code and unresolved TODO comments before merge.

Public REST endpoints use `/api/v1/[resource]` and the response envelope `{ success, data, error, meta }`. The Spring Boot backend uses centralized error handling through `@RestControllerAdvice`; errors must return the standard envelope and must not expose stack traces, credentials, JWTs, secrets or provider internals.

Use Flyway only for approved, versioned schema changes. Do not create migrations for non-schema, temporary or experimental changes; consolidate migrations for the same unmerged schema change; never modify or delete a migration already applied to a shared or production environment.

## ARTICLE 3 — SECURITY POLICIES (non-negotiable)

- Passwords: bcrypt with cost factor >= 12 — NEVER plain text, MD5 or recoverable password storage.
- Secrets: API keys, JWT signing material and database credentials stay in environment-managed secret storage — never commit them, passwords or `.env` files to source control.
- Database: all application data access goes through Spring Data JPA repositories — no raw SQL or string-built queries.
- Input validation: Jakarta Bean Validation is required on every backend request DTO; frontend validation never replaces backend validation.
- Authentication: protected APIs validate the JWT signature, approved algorithm, `kid`, issuer, audience, type, expiry, subject, active session and `authzVersion`.
- Authorization: `ADMIN` is server-managed, separate from Premium and never inferred from a client-provided flag, route visibility or plan. Privileged operations require `ADMIN` in the service layer; an Admin does not gain access to learner-private data merely through that role.
- Entitlements and ownership: Premium access, AI quota and learner-resource ownership are enforced by the backend on every applicable operation, never only by React navigation or UI.
- AI Service boundary: React never calls Mastra, an LLM provider or a speech provider directly. Spring Boot validates identity, ownership, input safety, rate limits, idempotency and AI quota before a private HMAC/mTLS call to `ai-service`.
- AI Service data: `ai-service` must not accept browser JWTs, query Pchinese product tables, expose Mastra default routes/Studio publicly, or persist learner conversation data in Mastra Memory during MVP. Provider credentials remain deployment secrets and logs/traces must not retain raw learner-private payloads.
- Learning authority: Mastra may produce structured explanations and feedback, but Spring Boot remains authoritative for Dictation scores, SRS scheduling, HSK classification, lesson access, entitlements, quota and progress. A dedicated speech engine is required for actual audio-pronunciation assessment.
- File uploads: validate file type and configured size limit before storage; scan stored uploads for malware; media uploads do not publish content by themselves.
- CORS: allowlisted origins only — no wildcard (`*`) in production.
- Privacy and retention: apply the type-specific retention matrix in `CLAUDE.md`; learner personal and learning data use the 12-month inactivity baseline unless that matrix defines a shorter technical period or a legal exception.
- Consent and access: consent for optional purposes is granular and withdrawable; internal and vendor access follows least privilege, masking by default, audit logging and the approved purpose.

## ARTICLE 4 — GIT WORKFLOW

Main branch: protected — no direct push  
Branch naming: `feat/[feature-name]` | `fix/[bug-name]` | `spec/[feature-name]`  
Commit format: `[type]: [scope] - [description]`  
Example: `feat(auth): add JWT refresh token endpoint`

Each feature has a focused specification at `specs/<feature>/spec.md`. Before implementation, the change owner identifies its actor, entry point, role, entitlement rule, state changes, failure cases, API impact and acceptance criteria. A pull request may merge only when the applicable Definition of Done is satisfied and its review confirms that it does not unintentionally alter another learning flow.

## ARTICLE 5 — TESTING REQUIREMENTS

Required: JUnit 5 + Mockito unit tests for backend service/business rules and relevant Jest tests for frontend behavior.  
Required: integration coverage for API contracts, validation and both happy and error paths.  
Required for critical learner journeys: end-to-end coverage where practical, including authentication, learning practice/progress and Admin content management.  
Required for authorization-sensitive changes: test ownership, Free/Premium entitlement checks, AI quota and `ADMIN` authorization.  
External adapters: mock `AI Service` and `Media Provider` in unit/integration tests; E2E tests must not depend on nondeterministic AI output without a dedicated controlled environment.  
Mastra features: test the typed Spring ↔ `ai-service` contract, schema-validate agent output before persistence, and cover quota reservation/idempotent retry plus safe provider-failure handling.  
No merge if relevant existing tests break.

## ARTICLE 6 — AI AGENT RULES

- Before changing a feature, read `AGENT.md`, `CLAUDE.md`, this Constitution and the relevant `specs/<feature>/spec.md` file.
- Treat `AGENT.md` as the source of truth for the mandatory tech stack and operating rules; this Constitution is the binding team law; `CLAUDE.md` defines the system architecture, domain flows and conventions.
- Implement backend DTOs, service logic, repository mappings and API documentation together; use typed frontend API clients and do not duplicate backend authorization or entitlement rules in React.
- Follow the Definition of Done: tests pass, API changes are documented in Swagger/OpenAPI, validation and authorization are complete, and error responses follow the standard contract.
- For an AI feature, preserve the private Spring Boot → `ai-service` boundary and the ownership rules in ARTICLE 3; detailed agent/workflow structure belongs in `CLAUDE.md`, not in frontend code or unreviewed provider prompts.
- Do not approve or merge changes that violate these Articles, the applicable feature specification or the Definition of Done.

## ARTICLE 7 — REVIEW PROCESS

Code review: verify the applicable Definition of Done, API contract, migration safety, tests and security/authorization impact before merge.  
Spec review: confirm the relevant `specs/<feature>/spec.md` documents actors, access rules, state transitions, failure cases, acceptance criteria, test cases and non-goals before implementation.  
Architecture changes: require an approved feature specification and a unanimous Constitution amendment vote.  
Emergency hotfix: may use an expedited review, but must preserve all security, data-protection and authorization rules and receive a documented follow-up review.
