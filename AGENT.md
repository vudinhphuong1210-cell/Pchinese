# AGENT.md — Project Context for AI Agents

# Version: 1.2 | Updated: 2026-09-05 | Project: Pchinese

## 1. PROJECT OVERVIEW

Name: Pchinese
Type: Full-stack Web App (SPA + REST API)
Domain: EdTech / Language Learning
Stage: Development

Bạn là một kỹ sư phần mềm senior trong dự án Pchinese.
Mục tiêu chính: Xây dựng nền tảng web học tiếng Trung cho người dùng cá nhân, tập trung vào việc biến nội dung audio và video tiếng Trung thành bài luyện tương tác; giúp người học cải thiện nghe, phát âm, từ vựng và phản xạ giao tiếp thông qua Dictation, Shadowing, AI và lặp lại ngắt quãng.
Domain hiện tại: EdTech học ngôn ngữ, chuyên tiếng Trung cho người học nói tiếng Việt. Người học có tài khoản và tiến độ cá nhân; học theo lộ trình HSK 1–HSK 6; luyện nghe–chép chính tả từ audio/video YouTube; ghi âm shadowing và nhận phản hồi phát âm/IPA từ AI; hội thoại theo tình huống với AI; tra từ điển chữ Hán/pinyin–Việt, dịch thuật, luyện viết chữ Hán; lưu từ vào kho từ vựng và ôn theo Spaced Repetition. Hệ thống theo dõi thời gian luyện tập, số từ đã học, điểm yếu, bảng xếp hạng. Nội dung học gồm bài audio, video được tuyển chọn và bộ từ vựng HSK có hình, pinyin, nghĩa Việt; mô hình sản phẩm có lớp miễn phí và gói Premium mở rộng quyền học.

Đọc trước:

1. `CLAUDE.md` — kiến trúc hệ thống, workflow, patterns, conventions
2. `CONSTITUTION.md` — canonical constitution, development principles và team agreements
3. File này — quy tắc vận hành cụ thể cho agent

## 2. TECH STACK (STRICT — do not deviate)

Backend: Spring Boot 3.4.5 + Java 21 maven
Frontend: React 18 +Vite
Database: PostgreSQL 18
ORM: Spring Data JPA
Auth: JWT + bcrypt (cost factor >= 12)
Testing: JUnit 5 + Mockito (backend), Jest (frontend)
Styling: Tailwind CSS 3.x
AI orchestration (approved AI features only): Mastra + Node.js + TypeScript as a private `ai-service`

## 3. ARCHITECTURE PRINCIPLES

- REST API: /api/v1/[resource] pattern
- Response format: { success, data, error, meta }
- Centralized error handling with Spring Boot `@RestControllerAdvice`
- All database queries must use Spring Data JPA — no raw SQL
- The optional Mastra `ai-service` is a controlled supporting service, not an authority for product data or business rules.

### AI Service (Mastra)

- Create `ai-service/` only when an approved AI feature begins implementation. Start with one internal endpoint and one feature agent; add later feature folders only with their corresponding approved feature.
- React never calls Mastra, LLM providers or speech providers directly. Spring Boot authenticates the learner, verifies ownership, reserves AI quota atomically, applies rate limits and persists the outcome before/after calling `ai-service`.
- `ai-service` accepts only authenticated private service-to-service requests using HMAC or mTLS. It must not accept browser JWTs, expose default Mastra routes/Studio publicly, or access Pchinese product tables directly.
- For MVP, Mastra is stateless: no learner conversation Memory or product-data persistence in `ai-service`. PostgreSQL conversation, message, attempt and progress records remain backend-owned through Spring Data JPA.
- Provider secrets exist only in `ai-service` deployment secrets; never commit, log, return or expose them to the browser.
- Mastra owns prompts, agents, workflows, model selection and typed safe output. Spring Boot owns deterministic scores, SRS scheduling, HSK classification, entitlement, quota and progress calculation.
- A real Shadowing pronunciation score requires a dedicated speech engine. Mastra may orchestrate that call and explain the result, but does not itself assess audio pronunciation.

### Admin authorization

- `ADMIN` is a server-managed role. It is independent from the `Premium` entitlement and is never inferred from a client-provided field or route visibility.
- An `ADMIN` may create, edit, publish, unpublish and archive learning content; manage approved media metadata; and set lesson access to `FREE` or `PREMIUM`.
- An `ADMIN` may grant or revoke `ADMIN` for another account, but may not change their own role or remove the final active `ADMIN`. Every role change must be audited and invalidate the target's active sessions.
- Premium entitlement and AI quota are created and changed only by the automated entitlement lifecycle. An `ADMIN` may not manually grant, revoke or alter either one; a future Admin dashboard only exposes the permitted role and content operations.
- `ADMIN` does not grant access to a learner's private attempts, saved words, recordings or AI conversations.

## 4. NAMING CONVENTIONS

- React component files: PascalCase (e.g. `UserCard.jsx`)
- Utility files and functions: camelCase (e.g. `formatDate.js`)
- API route segments: kebab-case (e.g. `/api/v1/user-profile`)
- Database tables: snake_case, plural (e.g. `user_profiles`)

## 5. FORBIDDEN PATTERNS

- NEVER commit secrets, passwords, API keys, or `.env` files to Git.
- In `ai-service`, NEVER use `any`; define proper TypeScript types instead.
- NEVER skip input validation on API endpoints.
- NEVER introduce deprecated libraries without team approval.
- NEVER delete files in `/data` or `/uploads` without user confirmation.
- NEVER use raw SQL; all database access must go through Spring Data JPA repositories.
- NEVER enforce Premium access only in the frontend; validate entitlements on the backend.
- NEVER infer administrative access from a client-provided flag, route visibility, or a Premium entitlement; enforce the server-managed `ADMIN` role in the service layer.
- NEVER create Flyway migrations for non-schema, temporary, or experimental changes.
- Before creating, modifying, or proposing a Flyway migration, inspect `DATA/_short.md` as the record of the Supabase schema.
- If `DATA/_short.md` is missing, or the required schema object is not recorded there, report the gap and wait for explicit user approval before adding a migration.
- NEVER create multiple Flyway migrations for the same unmerged schema change; consolidate them before merge.
- NEVER modify or delete a Flyway migration that has been applied to a shared or production environment.
- NEVER call an AI provider, Mastra or speech provider from React, or expose provider credentials/default Mastra endpoints to the public Internet.
- NEVER let `ai-service` query Pchinese product tables, accept user JWTs, or decide ownership, entitlement, quota, SRS, Dictation score, HSK level or progress.
- NEVER persist raw learner conversations, recordings, prompts or provider secrets in Mastra Memory, service logs or evaluation fixtures unless an approved privacy design explicitly permits it.

## 6. DEFINITION OF DONE (per task)

- [ ] Acceptance criteria in the relevant feature spec are met.
- [ ] Backend unit tests (JUnit 5 + Mockito) and relevant frontend tests (Jest) are written and passing.
- [ ] No frontend JavaScript lint, formatting, build, or test errors remain; no ai-service
  TypeScript errors remain.
- [ ] API changes follow `/api/v1` and `{ success, data, error, meta }`, and are documented in Swagger/OpenAPI.
- [ ] Input validation, authentication, authorization (including `ADMIN` for privileged operations), and Premium entitlement checks are implemented where applicable.
- [ ] Error cases return the correct HTTP status code and standard error response.
- [ ] Database changes use Spring Data JPA and include a Flyway migration only when an approved schema change is required.
- [ ] Any Flyway migration is focused, reversible where practical, and tested against a clean database.
- [ ] For an AI feature: Spring validates ownership/input, performs idempotent quota reservation before provider work, and maps provider failure to a safe standard error response.
- [ ] For an AI feature: the Spring ↔ `ai-service` request/response contract is typed and tested; agent output is schema-validated before backend persistence.
- [ ] For an AI feature: `ai-service` is private, stateless for learner data in MVP, and its secrets/logging/traces are configured not to expose learner-private data.
- [ ] No secrets, debug code, dead code, or unresolved TODO comments are included.

## 7. GIT CONVENTIONS

Branch: feat/[feature-name] | fix/[bug-name] | spec/[feature-name]
Commit: [type]: [scope] - [description]
Example: feat(auth): add JWT refresh token endpoint

## 8. CURRENT SPRINT CONTEXT

Sprint: Sprint 1 — Project Foundation
Focus: Set up the project structure, database connection, and authentication foundation.
Active specs: F11 — AI Learning Buddy (`specs/F11-ai-learning-buddy/spec.md`)
