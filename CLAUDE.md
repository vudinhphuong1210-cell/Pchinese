# CLAUDE.md — Pchinese

## TL;DR

> **Pchinese** là nền tảng EdTech giúp người Việt học tiếng Trung chủ động qua bài học audio/video, Dictation, Shadowing, từ điển, AI Buddy và Spaced Repetition.
>
> **Backend:** Spring Boot 3.4.5 + Java 21 + Maven + Spring Data JPA  
> **Frontend:** React 18 + Vite + Tailwind CSS 3.x  
> **Database:** PostgreSQL 18  
> **Auth:** JWT + bcrypt (cost factor >= 12)  
> **API:** REST tại `/api/v1/*`, phản hồi theo `{ success, data, error, meta }`.

Pchinese biến nội dung tiếng Trung thành vòng lặp **luyện tập chủ động → nhận phản hồi → lưu tiến độ → ôn đúng lúc**, không phải ứng dụng chỉ xem video.

## 1. READ FIRST

1. `AGENT.md` — context, tech stack bắt buộc, quy tắc vận hành.
2. `CONSTITUTION.md` — nguyên tắc phát triển và thoả thuận nhóm (nếu có).
3. File này — kiến trúc, domain flows, patterns và conventions.
4. Feature spec tương ứng tại `specs/<feature>/spec.md` trước khi thay đổi một feature.

### Source-of-truth clarification

`AGENT.md` là nguồn chuẩn cho tech stack và quy tắc bắt buộc. Pchinese dùng Spring Boot/Java, vì vậy mọi truy cập database đi qua **Spring Data JPA repositories**; không dùng raw SQL trong application code.

## 2. PRODUCT AND DOMAIN

### Product promise

Người học luyện tiếng Trung bằng nội dung thực tế và nhận phản hồi ngay trong lúc học:

- **Dictation:** nghe từng đoạn và gõ lại nội dung để luyện nghe chính xác.
- **Shadowing:** nói nhại theo giọng gốc, ghi âm và nhận phản hồi về phát âm/IPA, nhịp điệu và độ trôi chảy.
- **Dictionary:** tra chữ Hán hoặc pinyin, xem nghĩa Việt, ví dụ, âm đọc và luyện viết chữ Hán.
- **Vocabulary & SRS:** lưu từ vào kho cá nhân và ôn bằng Spaced Repetition.
- **AI Buddy:** hội thoại theo tình huống, nhận phản hồi về phát âm, ngữ pháp và từ vựng.
- **Progress:** theo dõi thời gian luyện tập, độ chính xác, từ đã học, điểm yếu và bảng xếp hạng.
- **Access:** có trải nghiệm miễn phí; Premium mở rộng quyền học.

### Actors

| Nhóm       | Actor            | Vai trò                                                              |
| ---------- | ---------------- | -------------------------------------------------------------------- |
| Primary    | `Guest`          | Xem nội dung công khai, đăng ký và đăng nhập                         |
| Primary    | `Learner`        | Học Dictation, Shadowing, từ vựng, AI chat và xem tiến độ            |
| Primary    | `Admin`          | Quản lý nội dung, media và role `ADMIN` theo quyền phía server       |
| Supporting | `AI Service`     | Chấm luyện tập, nhận xét phát âm, hội thoại, dịch và hỗ trợ đọc hiểu |
| Supporting | `Media Provider` | Cung cấp audio/video bài học, chẳng hạn YouTube                      |

`Premium` không phải actor riêng mà là plan/entitlement của `Learner`. `Admin` là actor nội bộ đại diện cho tài khoản đã xác thực có role `ADMIN`; quyền này được xác định và kiểm tra ở backend, không lấy từ cờ do client gửi. Role `ADMIN` tách biệt với entitlement `Premium` và quyền sở hữu dữ liệu của learner. `AI Service` và `Media Provider` là external supporting actors: chúng hỗ trợ use case do `Guest` hoặc `Learner` khởi tạo nhưng không tự bắt đầu luồng học.

### Core concepts

| Concept                        | Trách nhiệm                                                                   |
| ------------------------------ | ----------------------------------------------------------------------------- |
| User & Profile                 | Danh tính, ngôn ngữ, mục tiêu học và cài đặt cá nhân                          |
| Role & Authorization           | Role phía server, gồm `ADMIN`, xác định use case quản trị được phép thực hiện |
| Plan & Entitlement             | Quy định lesson `FREE`/`PREMIUM` và quota AI theo gói, được kiểm tra ở server |
| Topic, Lesson & Segment        | Tổ chức audio/video, trạng thái xuất bản và quyền truy cập theo chủ đề/cấp độ |
| Dictation attempt              | Lưu đáp án, lỗi, điểm và tiến độ theo segment/lesson                          |
| Shadowing attempt              | Lưu tham chiếu bài luyện, kết quả đánh giá và metadata an toàn của bản ghi    |
| Dictionary entry & saved word  | Nghĩa, pinyin, ví dụ và từ người học lưu vào kho cá nhân                      |
| Review schedule                | Trạng thái Spaced Repetition: due date, interval và lịch sử ôn                |
| AI conversation                | Phiên hội thoại, message và phản hồi phục vụ học tập                          |
| Learning progress & statistics | Tiến độ lesson, thời gian luyện, độ chính xác và chỉ số tổng hợp              |

### Admin permissions and role lifecycle

`ADMIN` is the only privileged role currently defined. It may:

- create, edit, publish, unpublish and archive topics, lessons and segments;
- upload or update approved media metadata, then set a lesson's access level to `FREE` or `PREMIUM`;
- grant or revoke `ADMIN` for another account through the protected role-management API.

`ADMIN` may not read or mutate a learner's attempts, saved words, recordings or AI conversations merely because of that role. It may not manually grant, revoke or edit a learner's Premium entitlement or AI quota; those values change only through the automated entitlement lifecycle. A future Admin dashboard may group the permitted role and content flows, but does not add authority. Role changes follow this process:

1. The initial `ADMIN` is provisioned through a controlled deployment operation; it is never created from public registration.
2. An authenticated `ADMIN` assigns or revokes the role only for another account. Self-assignment, self-revocation and removal of the final active `ADMIN` return `409 STATE_CONFLICT`.
3. The backend validates the target account, updates the role transactionally, writes an immutable audit record (actor, target, action, timestamp, correlation ID and before/after roles), then invalidates the target's active sessions.
4. The operation is exposed only as `POST /api/v1/users/{userId}/roles` and `DELETE /api/v1/users/{userId}/roles/ADMIN`; both require `ADMIN` in the service layer.

### Content lifecycle and lesson access

Every topic, lesson and segment has one publication state. A media upload never publishes content by itself.

| State         | Meaning                                                                                | Allowed next state        |
| ------------- | -------------------------------------------------------------------------------------- | ------------------------- |
| `DRAFT`       | Only Admin can view or edit it; learners and guests cannot access it.                  | `PUBLISHED`, `ARCHIVED`   |
| `PUBLISHED`   | It is eligible for playback and practice when its access-level rule is satisfied.      | `UNPUBLISHED`, `ARCHIVED` |
| `UNPUBLISHED` | New learner and guest access is blocked; existing attempts and progress are preserved. | `PUBLISHED`, `ARCHIVED`   |
| `ARCHIVED`    | Read-only historical content; it cannot receive new practice attempts.                 | `DRAFT`                   |

An Admin sets one access level before publishing:

- `FREE`: anyone, including `Guest`, may view/play the published lesson without a Premium entitlement.
- `PREMIUM`: only a `Learner` with an active Premium entitlement may view/play the published lesson.

The server enforces both publication state and lesson access level for every read/play request.

### Entitlement matrix

Admin selects `FREE` or `PREMIUM` for every lesson. The automated entitlement lifecycle determines each learner's active plan and AI use is quota-limited for every plan; Premium receives a higher configured quota, not unlimited AI use. Admin has no direct operation to change an individual learner's entitlement or quota.

| Capability                                 | `Guest`             | `Learner` — Free plan                              | `Learner` — Premium plan                              |
| ------------------------------------------ | ------------------- | -------------------------------------------------- | ----------------------------------------------------- |
| Browse published catalog                   | `FREE` lessons only | `FREE` lessons only                                | `FREE` and `PREMIUM` lessons                          |
| Play/practise a published `FREE` lesson    | Allowed             | Allowed                                            | Allowed                                               |
| Play/practise a published `PREMIUM` lesson | Denied              | Denied                                             | Allowed with active entitlement                       |
| AI Buddy or AI feedback request            | Denied              | Allowed until the configured Free quota is reached | Allowed until the configured Premium quota is reached |

Before any AI provider call, the backend atomically checks and records the applicable quota. A lesson access failure returns `403 ENTITLEMENT_REQUIRED`; a depleted AI quota returns `429 AI_QUOTA_EXCEEDED`. React may show upgrade or remaining-quota UI, but never makes the authorization decision.

## 3. SYSTEM ARCHITECTURE

Pchinese là một **modular monolith**: React SPA và Spring Boot REST API triển khai độc lập; backend tổ chức theo domain module rõ ràng nhưng dùng chung PostgreSQL. Không tự thêm microservice, Kafka, Redis hay API Gateway nếu feature spec chưa yêu cầu.

```text
┌─────────────────────────┐
│ Guest / Learner / Admin │  Primary actors
└────────────┬────────────┘
             ▼
┌───────────────────────────────────────────────────────────────────────┐
│ React 18 + Vite + Tailwind                                              │
│ Lesson · Dictation · Shadowing · Dictionary · AI · Progress · Admin UI │
└──────────┬───────────────────────────────┬────────────────────────────┘
           │ HTTPS / JSON                  │ approved playback
           ▼                               ▼
┌──────────────────────────────┐   ┌──────────────────────────┐
│ Spring Boot REST API         │   │ Media Provider           │
│ `/api/v1/*`                  │   │ audio/video, e.g. YouTube│
└──────────┬───────────┬───────┘   └──────────────────────────┘
           │           │
           │ JPA       │ controlled AI request
           ▼           ▼
┌──────────────────┐  ┌──────────────────────────────────────┐
│ PostgreSQL 18    │  │ AI Service                           │
│ product data     │  │ scoring · feedback · chat · translate│
└──────────────────┘  └──────────────────────────────────────┘
```

### Backend layer boundaries

```text
HTTP request
    ↓
Controller       — parse request, validate input, map HTTP response
    ↓
Service          — business rules, authorization, transactions, orchestration
    ↓
Repository       — Spring Data JPA persistence only
    ↓
Entity           — PostgreSQL table mapping and relationships
```

Rules:

- Controllers return DTOs; never expose JPA entities directly.
- Controllers do not contain scoring, SRS scheduling or entitlement logic.
- Services own transactions and business decisions.
- Repositories only encapsulate persistence queries; use Spring Data JPA, not raw SQL.
- `AI Service` is called through a dedicated backend integration/client; React never receives provider secrets.
- `Media Provider` receives only approved media requests or embed identifiers. Lesson metadata, entitlement and progress remain controlled by Pchinese.

### Module boundaries

| Module        | Owns                                                                                                     | Must not own                          |
| ------------- | -------------------------------------------------------------------------------------------------------- | ------------------------------------- |
| `auth`        | registration, login, password hashing, sessions and refresh-token rotation                               | lesson or progress business rules     |
| `security`    | access-token validation, authenticated principal, role-based authorization and role-change audit records | plan/entitlement business rules       |
| `users`       | profile, preferences and server-managed account roles                                                    | subscription payment-provider details |
| `learning`    | topics, lessons, segments, content metadata and publication state                                        | a learner's review schedule           |
| `media`       | approved provider identifiers and playback metadata                                                      | learning attempts or progress         |
| `dictation`   | answer evaluation and attempts                                                                           | shadowing assessment                  |
| `shadowing`   | recording workflow and assessment result                                                                 | dictionary definitions                |
| `vocabulary`  | saved words, lists and review scheduling                                                                 | lesson publishing                     |
| `ai`          | AI Buddy sessions and safe AI integration                                                                | authentication implementation         |
| `progress`    | completion, statistics, recommendations                                                                  | direct mutation of attempts           |
| `entitlement` | plan, lesson `FREE`/`PREMIUM` access and server-side AI quota checks                                     | UI-only visibility checks             |

### Mastra AI Service boundary

Khi feature AI Buddy (`F11`) được triển khai, Pchinese có thể chạy một `ai-service` Node.js + TypeScript sử dụng Mastra. Đây là một **supporting service boundary đã được kiến trúc chấp thuận**, không phải domain microservice sở hữu product data. Nó chỉ điều phối model, agent và workflow AI; Spring Boot vẫn là authority duy nhất cho dữ liệu và business rule.

```text
React SPA
    │ HTTPS: /api/v1/*
    ▼
Spring Boot
    │ authenticate · ownership · input safety · rate limit · quota reservation
    │ private HTTP with HMAC or mTLS
    ▼
Mastra ai-service
    │ prompt · agent/workflow · model selection · safe structured output
    ▼
LLM Provider / Speech Provider
```

Ownership rules:

- React never calls Mastra, an LLM provider or a speech provider directly.
- Spring Boot owns JWT/session validation, learner ownership, entitlement, AI quota reservation, idempotency, rate limiting, PostgreSQL persistence, encryption and deletion lifecycle.
- `ai-service` must not access Pchinese product tables or accept browser JWTs. It receives only validated, minimized context from Spring Boot and returns a typed result.
- For MVP, `ai-service` is stateless: do not enable Mastra Memory or persist learner conversation data there. Conversation/message records remain in PostgreSQL through the backend `ai` module.
- Do not expose Mastra-generated default agent/workflow routes or Studio publicly in production.
  Approved private routes are `POST /internal/v1/ai-buddy/respond` for F11 and
  `POST /internal/v1/shadowing/assess` for F08; each is reachable only by Spring Boot on the
  private network and has an independent typed contract.
- Provider credentials exist only in `ai-service` runtime secrets. They are never committed, returned to React or included in application logs.
- Deterministic product decisions remain in Spring Boot: Dictation scoring, SRS scheduling, HSK classification, lesson access, AI quota and progress calculation. Mastra may explain feedback but must not authoritatively calculate those values.
- Actual audio pronunciation assessment requires a dedicated speech engine. Mastra can orchestrate the speech call and turn the result into learner-friendly feedback, but it does not itself score audio pronunciation.

## 4. USER FLOWS AND SWIMLANES

Các flow dưới đây phản ánh hành trình sản phẩm trên Pchinese. Chúng là domain contracts, không áp đặt provider hoặc UI implementation cụ thể.

### 4.1 Browse and play learning media

```text
GUEST / LEARNER        REACT APP                 BACKEND                  MEDIA PROVIDER
       │                   │                         │                           │
       │ browse lesson     │                         │                           │
       ├──────────────────>│ GET lesson metadata    │                           │
       │                   ├────────────────────────>│ check access + load lesson│
       │                   │<────────────────────────┤ approved media identifier │
       │                   │                         │                           │
       │ play audio/video  │                         │                           │
       ├──────────────────>│ request approved media ├──────────────────────────>│
       │<──────────────────┼─────────────────────────┼───────────────────────────┤ stream/embed
```

Business rules:

- `Guest` may access only published `FREE` content; `Learner` access is determined by the lesson access level and backend entitlement checks.
- `Media Provider` supplies playback data only. Pchinese owns lesson structure, access decisions and learning progress.
- Store approved provider identifiers and metadata; do not trust arbitrary client-supplied media URLs.

### 4.2 Dictation: listen, type, receive feedback

```text
LEARNER             REACT APP              BACKEND                AI SERVICE             DATABASE
   │                    │                      │                        │                       │
   │ type answer        │                      │                        │                       │
   ├───────────────────>│ submit answer        │                        │                       │
   │                    ├─────────────────────>│ validate + authorize   │                       │
   │                    │                      ├───────────────────────>│ compare and explain   │
   │                    │                      │<───────────────────────┤ score + corrections   │
   │                    │                      ├──────────────────────────────────────────────>│ save attempt/progress
   │                    │<─────────────────────┤ feedback + next segment│                       │
   │<───────────────────┤ show result          │                        │                       │
```

Business rules:

- The server validates the learner, lesson, segment and entitlement before accepting an attempt.
- The backend controls the prompt/context sent to `AI Service` and validates the returned result before persistence.
- Store the learner answer, evaluation result and score needed for progress; do not trust a client-calculated score.
- An incomplete lesson remains resumable. Mark completion only under the lesson’s defined completion rule.
- Feedback must identify actionable correction without exposing an answer key beyond the allowed mode.

### 4.3 Shadowing: record, assess, improve

```text
LEARNER             REACT APP              BACKEND                AI SERVICE             DATABASE
   │                    │                      │                        │                       │
   │ record shadowing   │                      │                        │                       │
   ├───────────────────>│ upload recording     │                        │                       │
   │                    ├─────────────────────>│ validate + authorize   │                       │
   │                    │                      ├───────────────────────>│ assess pronunciation  │
   │                    │                      │<───────────────────────┤ IPA + rhythm feedback │
   │                    │                      ├──────────────────────────────────────────────>│ save attempt/progress
   │                    │<─────────────────────┤ assessment result      │                       │
   │<───────────────────┤ show feedback        │                        │                       │
```

Business rules:

- Microphone consent and recording happen in the client; credentials for `AI Service` never do.
- The backend enforces file type, size and authorization before forwarding or storing data.
- Persist only the recording and assessment data required by the product and the applicable retention policy.
- Pronunciation feedback is learning guidance, not a high-stakes evaluation. When assessment fails, preserve the learner’s work where possible and return a recoverable error.

### 4.4 Dictionary → saved word → spaced-repetition review

```text
LEARNER                 REACT APP                  BACKEND                    DATABASE
   │                        │                         │                           │
   │ search Hanzi/pinyin    │                         │                           │
   ├───────────────────────>│ GET dictionary query   ├──────────────────────────>│
   │<───────────────────────┤ definition, pinyin, examples                         │
   │                        │                         │                           │
   │ save a word            │                         │                           │
   ├───────────────────────>│ POST saved word        │ authorize + de-duplicate  │
   │                        ├────────────────────────>│ create/update schedule   │
   │                        │                         ├──────────────────────────>│
   │<───────────────────────┤ saved state + next review                             │
   │                        │                         │                           │
   │ complete a review      │ POST review result     │ calculate next due date   │
   │                        ├────────────────────────>├──────────────────────────>│
   │<───────────────────────┤ updated schedule and progress                          │
```

Business rules:

- A saved word belongs to one learner, even when its dictionary definition is shared.
- Saving the same word must be idempotent; it should not create duplicate personal entries.
- The server calculates the next review date. The client only submits a valid review outcome.
- Deleting a saved word and deleting a shared dictionary entry are distinct actions with different permissions.
- When translation or reading assistance is requested, the backend sends only the necessary context to `AI Service`; normal saved-word and SRS operations remain internal.

### 4.5 AI Buddy: contextual conversation with learner control

```text
LEARNER             REACT APP              BACKEND                AI SERVICE             DATABASE
   │                    │                      │                        │                       │
   │ send message       │                      │                        │                       │
   ├───────────────────>│ POST message         │                        │                       │
   │                    ├─────────────────────>│ authorize + context    │                       │
   │                    │                      ├───────────────────────>│ generate tutor reply  │
   │                    │                      │<───────────────────────┤ reply + feedback      │
   │                    │                      ├──────────────────────────────────────────────>│ save conversation
   │                    │<─────────────────────┤ safe response          │                       │
   │<───────────────────┤ show reply           │                        │                       │
```

Business rules:

- Conversation access is always scoped to the authenticated learner.
- The backend applies rate limits, entitlement checks and input safeguards before calling `AI Service`.
- Do not place API keys or provider prompts in the frontend bundle.
- A learner may rename or delete only their own conversations; deletion behaviour follows the product retention policy.

### 4.6 Progress and recommendations

```text
PRACTICE MODULES                 PROGRESS SERVICE                     LEARNER
       │                                │                                │
       │ complete attempt/review         │                                │
       ├───────────────────────────────>│ update completion and aggregates│
       │                                │                                │
       │                                │ derive weak areas / next action │
       │                                ├───────────────────────────────>│ view stats or recommendation
```

Progress is a domain outcome. Dictation, Shadowing and Vocabulary invoke an explicit progress-update operation; they must not each calculate competing dashboard totals in the frontend.

### 4.7 Admin: manage learning content

```text
ADMIN                    REACT ADMIN UI                  BACKEND                    DATABASE
  │                            │                            │                           │
  │ create/edit/publish content│                            │                           │
  ├───────────────────────────>│ POST/PATCH topic, lesson, │                           │
  │                            │ segment or media metadata  │                           │
  │                            ├───────────────────────────>│ authenticate + require   │
  │                            │                            │ `ADMIN` role + validate  │
  │                            │                            ├──────────────────────────>│ persist content/state
  │                            │<───────────────────────────┤ saved content/status      │
  │<───────────────────────────┤ show success or error      │                           │
```

Business rules:

- `POST` and `PATCH` on `/topics`, `/lessons`, `/segments` and `/media` require `ADMIN`. Publishing, unpublishing and archiving are explicit commands (`POST /api/v1/lessons/{id}/publish`, `/unpublish` and `/archive`) so the backend can validate lifecycle transitions.
- An Admin uploads approved learning media through `POST /api/v1/media`, associates it with content, and sets lesson access to `FREE` or `PREMIUM`. Media remains unavailable until its parent lesson is `PUBLISHED`.
- Content is not visible to `Guest` or `Learner` until its publication and lesson access state satisfy the backend rules. An `Admin` role does not bypass a learner's ownership of attempts, saved words, recordings or AI conversations.
- Validate parent-child relationships and approved media identifiers before persisting. Never accept a client-supplied `isAdmin` flag or arbitrary provider URL as authority.

### 4.8 Admin: assign or revoke the Admin role

```text
ADMIN                    REACT ADMIN UI                  BACKEND                    DATABASE / AUDIT LOG
  │                            │                            │                                  │
  │ assign/revoke another role │                            │                                  │
  ├───────────────────────────>│ POST or DELETE role route │                                  │
  │                            ├───────────────────────────>│ require `ADMIN`; validate target │
  │                            │                            │ and lifecycle constraints         │
  │                            │                            ├─────────────────────────────────>│ update role + append audit event
  │                            │<───────────────────────────┤ result; invalidate target sessions │
  │<───────────────────────────┤ show success or error      │                                  │
```

Business rules:

- Role routes are `POST /api/v1/users/{userId}/roles` with `{ "role": "ADMIN" }` and `DELETE /api/v1/users/{userId}/roles/ADMIN`.
- The service rejects a missing account with `404`, a non-admin caller with `403`, an invalid role payload with `400`, and self/final-admin violations with `409`.
- Audit records are append-only and never include credentials, JWTs, raw recordings or AI conversation content.

## 5. IMPORTANT ARCHITECTURAL DECISIONS (ADR)

### ADR-001: Spring Boot modular monolith instead of microservices

**Decision:** Use one Spring Boot backend, partitioned by domain module.

**Why:** The product needs cohesive auth, lesson, practice, vocabulary and progress workflows that share transactional data. A modular monolith minimizes deployment and data-consistency overhead at this stage.

**Trade-off:** Modules need disciplined boundaries so the codebase does not become a monolith without structure.

### ADR-002: Spring Data JPA for all database access

**Decision:** Use JPA entities and Spring Data repositories for database access; no application-level raw SQL.

**Why:** It matches the strict Java/Spring stack, keeps mapping and query code type-safe, and centralizes persistence concerns.

**Trade-off:** Complex queries require deliberate repository design, projections and pagination. Do not bypass the rule merely for convenience.

### ADR-003: REST API with a stable response envelope

**Decision:** Expose backend features under `/api/v1/[resource]` and return `{ success, data, error, meta }`.

**Why:** A stable, predictable contract keeps React feature code simple and provides a clear migration path for future clients.

### ADR-004: PostgreSQL is the transactional source of truth

**Decision:** Keep users, entitlements, learning attempts, review schedules and progress in PostgreSQL.

**Why:** The core workflows require relational integrity and reliable transactions—for example, saving a review result and its next due date together.

**Trade-off:** Analytics must avoid expensive unbounded queries on practice-attempt tables.

### ADR-005: Server-side entitlement enforcement

**Decision:** Enforce Free/Premium access in backend services, not only through frontend navigation.

**Why:** Hiding a button is not authorization. APIs must refuse restricted content and operations consistently.

**Trade-off:** Every protected use case must declare and test its entitlement rule.

### ADR-006: Supporting services stay behind controlled adapters

**Decision:** Store lesson metadata and product data internally; access `Media Provider` and `AI Service` through dedicated, controlled integration boundaries.

**Why:** This limits vendor coupling, protects secrets and makes provider changes testable.

**Trade-off:** Integration interfaces and failure handling require extra design before implementation.

### ADR-007: Explicit server-side roles for administration

**Decision:** Model `Admin` as the server-managed `ADMIN` role on an authenticated account. Require that role for content-management writes and any future administrative operation.

**Why:** Administration is a privileged actor flow. Keeping the role separate from `Premium`, ownership and client-side state prevents users from gaining administrative access by manipulating the browser.

**Trade-off:** Each privileged endpoint needs an authorization test, and role assignment or revocation must be handled through controlled server-side operations.

## 6. IMPLEMENTATION PATTERNS

### Package-by-feature backend structure

```text
backend/
└── src/
    ├── main/java/net/pchinese/
    │   ├── common/                 # API envelope, errors, config, shared utilities
    │   ├── security/               # JWT filter, authentication context, role authorization and audit
    │   ├── auth/                   # registration, login, credentials, sessions and refresh rotation
    │   ├── users/                  # profile, preferences and server-managed account roles
    │   ├── learning/               # topics, lessons, segments, content and publication state
    │   ├── media/                  # approved media provider integration
    │   ├── dictation/              # attempt evaluation and persistence
    │   ├── shadowing/              # assessment workflow and result storage
    │   ├── vocabulary/             # dictionary integration, saved words, SRS
    │   ├── ai/                     # AI Buddy and provider adapter
    │   ├── progress/               # completion, statistics, recommendations
    │   └── entitlement/            # plans and access rules
    └── test/java/net/pchinese/
        ├── unit/
        ├── integration/
        └── e2e/
```

Within a feature, keep controllers, request/response DTOs, services, entities and repositories close together. Avoid one global `services/` or `repositories/` folder that mixes unrelated domains.

### Frontend structure

```text
frontend/
└── src/
    ├── api/                        # contract-bound API clients
    ├── components/                 # reusable, feature-agnostic UI
    ├── features/
    │   ├── admin/                  # content-management UI; server remains authoritative
    │   ├── auth/
    │   ├── dictation/
    │   ├── shadowing/
    │   ├── vocabulary/
    │   ├── ai-buddy/
    │   └── progress/
    ├── pages/                      # route-level composition
    ├── routes/
    ├── hooks/
    ├── lib/                        # formatting and non-domain helpers
    ├── types/
    └── test/
        ├── unit/
        ├── integration/
        └── e2e/
```

### Mastra AI service structure

Create `ai-service/` only when implementation of an approved AI feature begins. The target structure is feature-oriented so each learning capability owns its agent/workflow, prompt and schema. Do not scaffold future feature folders merely for appearance; begin with the paths marked `MVP` and add later paths with their approved feature.

```text
ai-service/
├── src/
│   ├── server.ts                                # MVP: private HTTP server only
│   ├── config/
│   │   ├── env.ts                               # MVP: validate runtime env vars
│   │   ├── modelRegistry.ts                     # model choice by feature/environment
│   │   └── observability.ts                     # redaction, traces and metrics
│   ├── http/
│   │   ├── routes/
│   │   │   ├── internalAiBuddy.route.ts         # MVP
│   │   │   ├── internalDictation.route.ts       # add with Dictation AI feedback
│   │   │   └── internalShadowing.route.ts       # add with Shadowing AI feedback
│   │   └── middleware/
│   │       ├── internalAuth.ts                  # MVP: HMAC/mTLS + replay protection
│   │       ├── requestValidation.ts             # MVP
│   │       └── errorHandler.ts                  # MVP
│   ├── mastra/
│   │   └── index.ts                             # MVP: register allowed agents/workflows
│   ├── features/
│   │   ├── ai-buddy/
│   │   │   ├── chineseTutor.agent.ts            # MVP
│   │   │   ├── chineseTutor.prompt.ts           # MVP
│   │   │   ├── aiBuddy.schema.ts                # MVP: Zod request/output schemas
│   │   │   ├── aiBuddy.service.ts               # MVP
│   │   │   └── aiBuddy.eval.ts                  # MVP: non-private evaluation cases
│   │   ├── dictation-feedback/
│   │   │   ├── dictationFeedback.workflow.ts
│   │   │   ├── dictationFeedback.prompt.ts
│   │   │   ├── dictationFeedback.schema.ts
│   │   │   └── dictationFeedback.service.ts
│   │   ├── shadowing-feedback/
│   │   │   ├── shadowingFeedback.workflow.ts
│   │   │   ├── shadowingFeedback.prompt.ts
│   │   │   ├── shadowingFeedback.schema.ts
│   │   │   └── shadowingFeedback.service.ts
│   │   ├── vocabulary-coach/
│   │   │   ├── vocabularyCoach.agent.ts
│   │   │   ├── vocabularyCoach.prompt.ts
│   │   │   └── vocabularyCoach.schema.ts
│   │   └── lesson-enrichment/
│   │       ├── lessonEnrichment.workflow.ts
│   │       ├── lessonEnrichment.prompt.ts
│   │       └── lessonEnrichment.schema.ts
│   ├── adapters/
│   │   ├── llm/
│   │   │   └── modelProvider.ts                 # MVP: provider-specific configuration
│   │   ├── speech/
│   │   │   └── speechAssessmentClient.ts        # only when pronunciation assessment exists
│   │   └── pchinese/
│   │       └── pchineseInternalClient.ts        # allowlisted private backend tools only
│   ├── shared/
│   │   ├── contracts/
│   │   │   ├── internalRequest.ts               # MVP
│   │   │   ├── internalResponse.ts              # MVP
│   │   │   └── errors.ts                        # MVP
│   │   ├── safety/
│   │   │   ├── inputGuard.ts                    # MVP
│   │   │   └── outputGuard.ts                   # MVP
│   │   └── utils/
│   └── test/
│       ├── unit/
│       ├── integration/
│       └── contract/
├── evals/
│   ├── ai-buddy/
│   │   ├── hsk-scenarios.json
│   │   ├── unsafe-input.json
│   │   └── vietnamese-feedback.json
│   └── shadowing-feedback/
├── Dockerfile
├── package.json
├── tsconfig.json
├── .env.example                            # names/placeholders only; never real secrets
└── README.md
```

The minimum initial service is intentionally small:

```text
src/server.ts
src/config/env.ts
src/http/routes/internalAiBuddy.route.ts
src/http/routes/internalShadowing.route.ts
src/http/middleware/internalAuth.ts
src/mastra/index.ts
src/features/ai-buddy/{chineseTutor.agent.ts,chineseTutor.prompt.ts,aiBuddy.schema.ts,aiBuddy.service.ts}
src/features/shadowing-feedback/{shadowingFeedback.workflow.ts,shadowingFeedback.prompt.ts,shadowingFeedback.schema.ts,shadowingFeedback.service.ts}
src/adapters/llm/modelProvider.ts
src/adapters/speech/speechAssessmentClient.ts
src/shared/contracts/{internalRequest.ts,internalResponse.ts,errors.ts}
```

Splitting these files does not create multiple servers or require multiple API keys. It remains one container, one Mastra instance and initially one LLM credential. The separation keeps HTTP authentication, prompt/agent behavior, external-provider configuration and typed contracts independently testable as new AI skills are introduced.

Required environment variable names are documented in `ai-service/.env.example`; examples include `LLM_PROVIDER`, `LLM_MODEL`, `LLM_API_KEY`, `PCHINESE_INTERNAL_BASE_URL` and either `INTERNAL_HMAC_SECRET` or configured mTLS certificate paths. The real values are supplied by deployment secrets, never committed.

### AI service contract, safety and operations

The tree above is only the implementation layout. Every `ai-service` change MUST also preserve the
following runtime contract.

#### Private request and response contract

- The approved internal routes are `POST /internal/v1/ai-buddy/respond` for F11 and
  `POST /internal/v1/shadowing/assess` for F08. Each accepts private Spring Boot traffic only; no
  browser, public API gateway, provider or internal peer may call it directly.
- Spring Boot authenticates the learner, checks ownership and entitlement, reserves AI quota,
  applies rate limits and chooses idempotency behaviour before creating the request. `ai-service`
  authenticates the service caller using HMAC or mTLS and rejects invalid, expired or replayed
  requests.
- The typed, versioned request contract MUST carry a correlation/request ID, an approved feature
  capability and only the validated, minimized context required for that capability. It MUST NOT
  contain a browser JWT, provider credential, mutable product authority or unrestricted learner
  history/audio.
- The typed response contract MAY contain schema-validated explanations, feedback, safety status
  and controlled internal failure information. It MUST NOT contain authoritative scores, schedules,
  quota decisions, progress mutations, provider internals or secrets. Spring Boot validates the
  result before persistence and owns the public API response.

#### Safety, privacy and failure handling

- Run input safety guards before provider dispatch and output safety/schema guards before returning
  a result. Reject malformed, unsafe or unvalidated provider output; never return a best-effort
  raw model response.
- The service is stateless for learner data in MVP: Mastra Memory is disabled and no conversation,
  message, prompt, recording, speech transcript, attempt or product record is persisted locally.
  No code in the service queries Pchinese product tables.
- For F08, Spring Boot validates ownership, recording status and malware scan, then streams only
  the required audio bytes directly through the private HMAC/mTLS request. `ai-service` receives
  no object-storage credential, object key, signed URL or persistent recording reference.
- Logs, traces, metrics and evaluation fixtures use redacted, minimum necessary data. They MUST NOT
  retain credentials, HMAC material, raw browser JWTs, raw prompts, raw recordings or raw provider
  request/response bodies.
- Provider timeout, unavailable dependency, invalid output and safety rejection are expected
  states. `ai-service` returns a safe typed internal failure with the correlation ID; Spring Boot
  maps it to the standard public error, such as `PROVIDER_ERROR` or `SERVICE_UNAVAILABLE`, without
  exposing a stack trace or provider detail.

#### Runtime, testing and change gates

- The initial deployment is one private container, one Mastra instance and one configured LLM
  credential. It MUST NOT introduce a product database, queue, cache, public ingress or another
  supporting service without an approved feature specification and any required Constitution
  amendment.
- Unit tests cover internal authentication/replay protection, request validation, safety guards,
  prompt/agent behaviour and provider-error normalization. Integration tests use mocked providers.
  Contract tests cover Spring Boot <-> `ai-service` schemas, malformed input/output, timeout and
  safe-failure cases.
- Evaluations use approved synthetic, non-private fixtures. E2E tests MUST NOT depend on
  nondeterministic provider output outside a dedicated controlled environment.
- A new route, agent, provider, memory/persistence capability or future feature directory may be
  added only after its feature specification defines the actor, access rules, minimized data,
  output schema, failure handling, retention and tests.

### Controller and service pattern

- Controller: authenticate, validate request DTO, call one application service, map result to the standard response envelope.
- Service: check authorization/entitlement, load aggregates, apply business rules, persist through repositories and update progress within a transaction where required.
- Repository: narrowly scoped reads/writes using Spring Data JPA. Use pagination for collections.
- DTO: explicit request and response records/classes; never reuse entities as public API payloads.

### Error handling

Use centralized exception handling in the Spring Boot backend (for example, `@RestControllerAdvice`). This implements the centralized error-handling principle from `AGENT.md` in a way that fits Spring Boot. Every error must produce the standard envelope:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The submitted answer is invalid."
  },
  "meta": {}
}
```

Guidelines:

- This status contract applies to every API, not only Admin routes. Each error uses the standard envelope and a stable `error.code`; `meta` may contain a safe correlation ID.

| HTTP status | `error.code`                                                                        | When to use it                                                                                                                 |
| ----------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| `400`       | `VALIDATION_ERROR`                                                                  | Invalid request body, path/query value or unsupported role value.                                                              |
| `401`       | `UNAUTHENTICATED`, `ACCESS_TOKEN_EXPIRED`, `REFRESH_TOKEN_INVALID`, `TOKEN_REVOKED` | Missing, invalid, expired or revoked authentication credential.                                                                |
| `403`       | `AUTHORIZATION_DENIED`, `ENTITLEMENT_REQUIRED`                                      | Authenticated caller lacks ownership, `ADMIN`, or the required entitlement.                                                    |
| `404`       | `RESOURCE_NOT_FOUND`                                                                | Resource is absent or must not be disclosed to the caller.                                                                     |
| `409`       | `STATE_CONFLICT`, `DUPLICATE_RESOURCE`, `SESSION_LIMIT_REACHED`                     | Invalid lifecycle transition, duplicate state, concurrent edit, self-role change, final-admin removal or device-session limit. |
| `413`       | `PAYLOAD_TOO_LARGE`                                                                 | Recording or media upload exceeds the configured size limit.                                                                   |
| `415`       | `UNSUPPORTED_MEDIA_TYPE`                                                            | Uploaded recording or media type is outside the approved allowlist.                                                            |
| `429`       | `RATE_LIMITED`, `AI_QUOTA_EXCEEDED`                                                 | Caller exceeds an endpoint rate limit or the configured AI quota.                                                              |
| `502`       | `PROVIDER_ERROR`                                                                    | A controlled `AI Service` or `Media Provider` call fails unexpectedly.                                                         |
| `503`       | `SERVICE_UNAVAILABLE`                                                               | A required internal dependency or provider is temporarily unavailable.                                                         |
| `500`       | `INTERNAL_ERROR`                                                                    | Unexpected server failure after safe logging.                                                                                  |

- Never return stack traces, internal provider details, passwords, JWTs or secrets to clients.
- Log errors with safe correlation/context data; do not log raw credentials or sensitive recordings.

### Transaction boundaries

Transactions belong in the service layer. Typical transactional operations include:

- accepting an authenticated Dictation or Shadowing attempt and updating progress;
- saving a word and creating/updating its review schedule;
- submitting a review and calculating the next due date;
- changing an entitlement and its effective access state.

Do not make an external provider call inside a long-held database transaction unless the feature spec explicitly designs for it. Validate and persist a safe state, then handle external response/failure deliberately.

## 7. DATA AND API CONVENTIONS

### Naming conventions

| Surface                               | Convention                | Example                                       |
| ------------------------------------- | ------------------------- | --------------------------------------------- |
| Java classes, React components        | PascalCase                | `LessonProgressService`, `PracticePlayer.jsx` |
| Java methods, variables, JavaScript utilities | camelCase             | `calculateNextReview`, `formatDuration.js`    |
| REST route segments                   | kebab-case                | `/api/v1/lesson-progress`                     |
| JSON properties                       | camelCase                 | `nextReviewAt`, `attemptScore`                |
| PostgreSQL tables and columns         | snake_case, plural tables | `lesson_attempts`, `next_review_at`           |
| Java packages                         | lowercase                 | `net.pchinese.vocabulary`                     |
| Feature spec directories              | feature ID + kebab-case   | `specs/F07-dictation-practice/spec.md`        |

### API rules

- Version all public endpoints with `/api/v1`.
- Use nouns for resources, not verbs: `/lessons`, `/saved-words`, `/review-sessions`.
- Place an action under a resource only when it represents a domain command, e.g. `POST /api/v1/reviews/{id}/submit`.
- Paginated lists return pagination data in `meta`; never return an unbounded attempt/history collection.
- The backend validates ownership for every learner-scoped resource ID, even when a valid JWT is present. Role `ADMIN` does not bypass ownership of attempts, saved words, recordings or AI conversations.
- Content-management writes on `/topics`, `/lessons`, `/segments` and `/media` require the server-side `ADMIN` role; public reads and learner access still follow publication and entitlement rules.
- Document endpoints and request/response schemas in Swagger/OpenAPI.

### Authentication and session contract

All successful authentication responses use the standard envelope. The JWT and session policy below is mandatory for Web and Mobile clients; no client chooses its own token-storage or refresh behaviour.

| Endpoint                                        | Request and successful result                                                                                                          | Required error behaviour                                                                                                                                                                           |
| ----------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `POST /api/v1/auth/register`                    | Email and password; returns `201` with `verificationRequired: true` and sends a verification message.                                  | `400` invalid input; `409 DUPLICATE_RESOURCE` for an existing email.                                                                                                                               |
| `POST /api/v1/auth/email-verifications`         | Requests or resends a verification message; returns `202` without revealing whether the email exists.                                  | `400` malformed email; rate-limit with `429`.                                                                                                                                                      |
| `POST /api/v1/auth/email-verifications/confirm` | Verification credential; returns `200` with the verified account state.                                                                | `400` invalid/expired credential; `409 STATE_CONFLICT` when confirmation is no longer valid.                                                                                                       |
| `POST /api/v1/auth/login`                       | Verified email and password; returns `200` with an access-session result and refresh-session result.                                   | `400` malformed request; `401 UNAUTHENTICATED` for invalid credentials; `403 AUTHORIZATION_DENIED` for an unverified account; `409 SESSION_LIMIT_REACHED` when the plan's device limit is reached. |
| `POST /api/v1/auth/refresh`                     | Current refresh-session credential and `X-Refresh-Request-Id`; returns `200` with a rotated access-session and refresh-session result. | `401 REFRESH_TOKEN_INVALID` when expired, revoked, reused or invalid.                                                                                                                              |
| `POST /api/v1/auth/logout`                      | Current refresh-session credential; returns `200` after invalidating that session.                                                     | `401 REFRESH_TOKEN_INVALID` when the credential is no longer valid.                                                                                                                                |
| `GET /api/v1/auth/sessions`                     | Lists only the caller's active devices/sessions with `sessionId`, device label, platform, created time and last seen time.             | `401` when unauthenticated.                                                                                                                                                                        |
| `DELETE /api/v1/auth/sessions/{sessionId}`      | Revokes one session owned by the caller; returns `200`.                                                                                | `401` when unauthenticated; `404` when that session is absent or not owned by the caller.                                                                                                          |
| `DELETE /api/v1/auth/sessions`                  | Revokes all sessions for the caller, including the current session; returns `200`.                                                     | `401` when unauthenticated.                                                                                                                                                                        |
| `POST /api/v1/auth/password-resets`             | Email address; always returns `202` and sends a reset message only when eligible.                                                      | `400` malformed email; rate-limit with `429`; never reveal account existence.                                                                                                                      |
| `POST /api/v1/auth/password-resets/confirm`     | Reset credential and replacement password; returns `200`, then invalidates all active sessions for that account.                       | `400` invalid/expired reset credential or weak password.                                                                                                                                           |

When an access session expires, protected routes return `401 ACCESS_TOKEN_EXPIRED`. The client may request `/auth/refresh`; if refresh fails, it clears local authenticated state and sends the user to sign in. A verified account remains eligible to sign in until it is disabled or its credentials are reset.

### JWT and session security policy

#### Token storage and browser/mobile protection

| Client        | Access token                                                                                                                                              | Refresh token                                                                                                                                                                             | Required protections                                                                                                                                                                                                                                                                                                                     |
| ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Web           | Keep only in JavaScript memory; send with `Authorization: Bearer`. Never use `localStorage`, `sessionStorage`, IndexedDB or a JavaScript-readable cookie. | Store only in the `__Host-pchinese-refresh` cookie with `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/` and no `Domain` attribute; the server accepts it only on authentication routes. | Use a strict Content Security Policy with nonces, output encoding and dependency review to reduce XSS. For every cookie-authentication request, enforce SameSite, an allowlisted `Origin`/`Referer`, and a synchronizer CSRF token in a custom header. Configure CORS with explicit trusted origins and credentials only where required. |
| Mobile native | Keep only in process memory.                                                                                                                              | Store in the platform secure store: iOS Keychain or Android Keystore-backed encrypted storage. Never use AsyncStorage, shared preferences or application logs.                            | Use TLS for every request, bind the session to a device installation identifier, protect local unlock with OS controls where available, and send tokens only in HTTPS headers. CSRF protections are not applicable because native clients do not rely on ambient browser cookies.                                                        |

For Web, login and refresh return the access token in the response envelope and set the raw refresh token only through `Set-Cookie`; it never appears in JSON. The backend also issues a separate non-`HttpOnly` `__Host-pchinese-csrf` cookie whose value is associated with the server session; JavaScript echoes that value in `X-CSRF-Token`, and the server verifies it with the session and trusted origin. Mobile receives the refresh token only over TLS and immediately writes it to the platform secure store.

Access JWT lifetime is **10 minutes**. Refresh sessions have both an idle and absolute expiry, and all values are server configuration rather than client input:

| Plan    | Maximum active device sessions | Refresh idle expiry | Refresh absolute expiry |
| ------- | ------------------------------ | ------------------- | ----------------------- |
| Free    | 2                              | 14 days             | 30 days                 |
| Premium | 5                              | 30 days             | 90 days                 |

At the limit, login returns `409 SESSION_LIMIT_REACHED` and the client must let the learner revoke an existing session before retrying. The server never silently evicts a device. `GET /auth/sessions`, `DELETE /auth/sessions/{sessionId}` and `DELETE /auth/sessions` implement device review, logout of one device and logout of all devices respectively.

#### JWT claims and signing

Access tokens are signed with an approved asymmetric algorithm such as `RS256`, carry a `kid` for key rotation, and are verified with an explicit algorithm allowlist. Never accept `alg: none` or use the token header to choose an arbitrary algorithm.

| Claim               | Purpose                                                              |
| ------------------- | -------------------------------------------------------------------- |
| `iss`               | Fixed issuer, for example `pchinese-auth`.                           |
| `aud`               | Fixed API audience, for example `pchinese-api`.                      |
| `sub`               | Stable authenticated user ID only.                                   |
| `sid`               | Current server-side session ID.                                      |
| `jti`               | Unique access-token identifier for logging and investigation.        |
| `iat`, `nbf`, `exp` | Issued, not-before and expiry timestamps.                            |
| `typ`               | Fixed value `access`.                                                |
| `authzVersion`      | Monotonically increasing version used to reject stale authorization. |

Do not put profile data, the entitlement matrix, lesson/progress data, AI quota, complete role/permission lists or sensitive personal data into a JWT. The authorization and entitlement services load current server-side state after authentication.

#### Refresh rotation, concurrency and reuse detection

Refresh tokens are opaque, cryptographically random values of at least 256 bits. The backend returns the raw value only at login/refresh time and stores only an HMAC-SHA-256 hash using a server-held pepper.

1. Every successful refresh locks the refresh-token/session row through Spring Data JPA (`PESSIMISTIC_WRITE` or an equivalent compare-and-set), verifies it is active and within expiry, marks it `ROTATED`, and creates a new refresh token linked to the same family and session.
2. Web and Mobile clients implement single-flight refresh: only one refresh request may be in flight per session. Each request also includes a UUID `X-Refresh-Request-Id`.
3. For a retry of the _same_ request ID, the backend may replay the already-created response from a short-lived, KMS-encrypted idempotency record (maximum 30 seconds); it must not rotate a second time.
4. A different request ID using a `ROTATED`, revoked or expired token is refresh-token reuse. The backend revokes the entire token family/session transactionally, writes a high-severity audit event, clears the client session and returns generic `401 REFRESH_TOKEN_INVALID`. Do not disclose whether reuse was detected.

#### Authorization revocation

`users.authz_version` is checked against the `authzVersion` claim on every protected request. The value may be cached only with event-driven invalidation; a stale cache must not keep a revoked authorization valid.

| Event                           | Required server action                                                                                                                                                                                                                                                                                                                                                            |
| ------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Password change/reset           | Increment `authz_version`, revoke every active session/token family for that user, and require sign-in again.                                                                                                                                                                                                                                                                     |
| `ADMIN` role grant/revoke       | Increment `authz_version`, revoke every active session/token family for the target user, append an audit event, and require sign-in again.                                                                                                                                                                                                                                        |
| Account locked/disabled         | Increment `authz_version`, revoke every active session/token family, and reject all future authentication until restored.                                                                                                                                                                                                                                                         |
| Free/Premium entitlement change | Increment `authz_version` and re-evaluate entitlement server-side on every content/AI operation. Existing access JWTs immediately fail the version check; a still-valid refresh session may obtain a token with current authorization, unless the account is locked. On downgrade, revoke least-recently-seen sessions beyond the new plan's device limit and notify the learner. |
| Single-device logout            | Revoke that session and its token family; access JWTs with its `sid` immediately fail.                                                                                                                                                                                                                                                                                            |
| Logout all devices              | Revoke all active sessions/token families for the caller and increment `authz_version`.                                                                                                                                                                                                                                                                                           |

#### Backend session and refresh-token schema

Use server-managed tables. Never persist a raw JWT or raw refresh token in ordinary columns, indexes or logs. The sole exception is the KMS-encrypted, 30-second idempotency response described below, which exists only to replay an identical in-flight refresh request and is deleted at expiry.

| Table                 | Required columns and rules                                                                                                                                                                                                                                                                                                                                                     |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `auth_sessions`       | `session_id` UUID PK, `user_id` FK, `family_id` UUID, `authz_version`, `device_id`, sanitized `device_label`, `platform`, optional privacy-safe `ip_hash`, `created_at`, `last_seen_at`, `idle_expires_at`, `absolute_expires_at`, `revoked_at`, `revoked_reason`. Index active sessions by `(user_id, revoked_at)`, token family by `family_id`, and session by `session_id`. |
| `refresh_tokens`      | `refresh_token_id` UUID PK, `session_id` FK, `family_id`, unique `token_hash`, `issued_at`, `expires_at`, `rotated_at`, `replaced_by_token_id`, `revoked_at`, `revoked_reason`, `refresh_request_id`. Retain a rotated token hash until its absolute expiry so reuse remains detectable.                                                                                       |
| `refresh_idempotency` | `session_id`, unique `refresh_request_id`, KMS-encrypted response payload, `expires_at` (at most 30 seconds). Delete it after expiry; it exists only to make an identical network retry safe.                                                                                                                                                                                  |
| `auth_audit_events`   | `event_id`, `user_id`, optional actor/session/family references, event type, timestamp, correlation ID and safe device context. Never store raw tokens, passwords, recordings or AI content.                                                                                                                                                                                   |

`auth_sessions.authz_version` is the version at session issuance. On every refresh, compare it to `users.authz_version`; a mismatch requires current authorization to be re-evaluated before minting an access token.

#### Middleware and service pseudocode

```text
authenticateAccess(request):
  rawToken = requireBearerToken(request)
  claims = verifySignatureAndAlgorithm(rawToken, issuer, audience, keyByKid)
  require claims.typ == "access" and claims.exp/nbf are valid
  session = sessionRepository.findActiveById(claims.sid)
  userAuthzVersion = authorizationVersionService.current(claims.sub)
  if session is absent/revoked OR user is disabled OR claims.authzVersion != userAuthzVersion:
      return 401 TOKEN_REVOKED
  attach authenticated principal { userId: claims.sub, sessionId: claims.sid }
  authorization and entitlement services load current server-side permissions
```

```text
login(credentials, device):
  user = authenticateVerifiedUser(credentials)
  reject if account disabled or activeSessions(user) reaches plan limit
  transaction:
      session = createSession(user, newFamilyId(), device, user.authzVersion)
      refresh = randomOpaqueToken(256 bits)
      persistRefreshHash(session, refresh, ACTIVE)
  return signAccessJwt(user.id, session.id, user.authzVersion), refresh

refresh(rawRefresh, requestId):
  transaction with PESSIMISTIC_WRITE:
      token = findRefreshTokenByHash(hashWithPepper(rawRefresh))
      if token is absent:
          auditSecurityEvent("UNKNOWN_REFRESH_TOKEN"); return 401 REFRESH_TOKEN_INVALID
      if token is ACTIVE and session/user are valid:
          newRefresh = randomOpaqueToken(256 bits)
          mark token ROTATED; persist new token and short-lived idempotency response
          update session.lastSeenAt; mint access token using current authzVersion
          return new access + refresh result
      if token is ROTATED and requestId matches idempotency record before expiry:
          return replayIdempotentResult()
      revokeTokenFamily(token.familyId, "REFRESH_REUSE_OR_INVALID")
      auditSecurityEvent(); return 401 REFRESH_TOKEN_INVALID
```

```text
revokeUserAuthorization(userId, reason, revokeAllSessions):
  transaction:
      increment users.authzVersion
      if revokeAllSessions: revoke every active session and refresh token family
      append safe auth audit event
  evict authorization-version cache for userId
```

#### Mandatory security test cases

- Access token expiry returns `401 ACCESS_TOKEN_EXPIRED`; a valid refresh produces a fresh access token.
- A revoked session/family, a disabled account and a mismatched `authzVersion` all reject the access token immediately.
- Password change/reset revokes every device session and blocks both old access and refresh tokens.
- Admin role grant/revoke and Free/Premium change invalidate stale authorization; the next authorization check uses the new server state.
- Refresh rotation marks the prior token unusable; an identical retry ID is idempotent, while a different retry using the old token revokes the family and forces sign-in.
- Concurrent refresh attempts cannot create two active descendants of one refresh token.
- Free and Premium session limits reject a new login at the configured limit; single-device logout and logout-all revoke only the intended sessions.
- Web tests verify no token is written to browser storage, refresh routes reject missing CSRF/invalid Origin, and cross-origin credentialed requests are denied.
- Mobile tests verify refresh tokens use the platform secure store and are never written to application logs or ordinary local storage.

### Persistence rules

- Model tables in `snake_case` and plural names. Map them explicitly with JPA annotations where naming differs.
- Enforce unique constraints for data such as an email address and a learner’s saved copy of the same dictionary word.
- Persist account roles through server-managed data (for example a `user_roles` relationship); do not model administrative authority as a client-editable profile field.
- Persist `users.authz_version`, `auth_sessions`, rotated refresh-token hashes and short-lived refresh idempotency records as defined by the JWT/session policy. Do not persist raw access or refresh tokens outside the KMS-encrypted, 30-second idempotency response required for a safe identical refresh retry.
- Persist the publication state and `FREE`/`PREMIUM` access level of each content item. Enforce allowed lifecycle transitions in the service layer and retain attempts/progress when content is unpublished or archived.
- Atomically record AI usage against the active plan quota before calling `AI Service`, so concurrent requests cannot exceed the configured quota.
- Write role changes to an append-only audit store; the record includes actor, target, action, timestamp, correlation ID and before/after roles, but never credentials or tokens.
- Add indexes for known filtering paths, especially foreign keys and progress/review queries, as informed by the feature spec and query shape.
- Use Flyway only for approved, versioned database schema changes. Do not create migrations for non-schema, temporary, or experimental changes.
- Consolidate migrations for the same unmerged schema change before merge; do not create migration churn.
- Never modify or delete a migration that has been applied to a shared or production environment.
- Keep each migration focused, reversible where practical, and tested against a clean database. Do not mutate shared or production schemas manually.
- Store dates/times in a consistent timezone-aware representation; render them in the learner’s locale in the client.

## 8. SECURITY, PRIVACY, AND ACCESS

- Hash passwords with bcrypt at cost factor >= 12; never encrypt or store a recoverable password.
- Validate JWT signature, approved algorithm, `kid`, issuer, audience, type, expiry, subject, active session and `authzVersion` on every protected API route.
- Resolve roles from the authenticated, server-controlled identity and enforce ownership, roles and entitlement in the service layer—not only in React route guards.
- Keep API keys, JWT signing material and database credentials outside source control. Never commit secrets in `.env` files.
- Validate all external input: request body, path/query parameters, uploaded audio metadata and `Media Provider` identifiers.
- Apply configured upload limits, type allowlists and malware scanning before accepting learner recordings or Admin-uploaded learning media; quarantine failed uploads and never expose them for playback.
- Treat recordings, AI conversations and learning history as personal data. Collect only what a feature needs and apply the retention, deletion and consent policy below.
- Protect all admin/content-management operations with the explicit `ADMIN` role; do not infer admin status from client-provided flags, route visibility or the `Premium` plan.

### Privacy, data retention and learner rights

This is Pchinese's engineering policy for the Vietnamese launch market. It must be approved by legal counsel before production release and reviewed again before entering another market. Legal must validate it against the applicable version of Vietnam's Personal Data Protection Law and its implementing rules; no retention period, legal basis or exception below substitutes for that review.

#### Retention matrix

All timestamps are measured from the stated event. A legal hold overrides the ordinary schedule only for the specific required data, in a separated store and for its documented end date.

**Product decision:** learner personal and learning data is retained for **12 months after the learner's last account activity**, unless a shorter technical retention or a separately approved legal/compliance exception in this matrix applies.

| Data type                                                                 | Purpose                                                              | Retention period                                                                                                        | Storage                                                             | Default access                                                               | Deletion method                                                                                                                  | Exception                                                                                           |
| ------------------------------------------------------------------------- | -------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- | ---------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| Profile, account identity and preferences                                 | Operate account, authentication and localization                     | 12 months after last account activity; 30-day cancellation window after account-deletion request                        | PostgreSQL, encrypted per-user fields                               | Learner; masked Support by ticket; no Admin access                           | Delete/crypto-erase user key after window; purge indexes/cache                                                                   | Legal hold only; never reused for learning, marketing or AI improvement                             |
| AI chat/messages                                                          | Provide AI Buddy and learner history                                 | Until learner deletes it, or 12 months after last chat activity; send notice 30 days before inactivity deletion         | PostgreSQL encrypted fields; encrypted search index only if enabled | Learner; Support only break-glass; AI vendor receives minimized request only | Hard-delete DB rows, search documents and cache/CDN copies; crypto-erase user key material                                       | Legal hold only                                                                                     |
| Raw voice — `ASSESSMENT_ONLY`                                             | One-time pronunciation assessment; learner did not choose to save it | 30 days after successful assessment; 24 hours after a failed/abandoned assessment                                       | Encrypted object storage with per-recording data-encryption key     | Learner while retained; processing worker; no teacher/Admin                  | Delete object/version/derived temporary files and destroy recording key                                                          | Legal hold only; do not keep for AI improvement without separate consent                            |
| Raw voice — `SAVED_RECORDING`                                             | Learner deliberately keeps a recording for review                    | Until learner deletes it, or 12 months after last account activity; notices at 30 and 7 days before inactivity deletion | Encrypted object storage with per-user and per-recording keys       | Learner; teacher only with separate sharing consent; Support break-glass     | Same as assessment recording plus CDN purge                                                                                      | Legal hold only                                                                                     |
| Pronunciation score and feedback (no speech-engine transcript in F08 MVP) | Show results and update learning history                             | Until learner deletes the attempt, or 12 months after last account activity                                             | PostgreSQL encrypted fields; limited search index                   | Learner; teacher only with sharing consent; Support break-glass              | Hard-delete rows/index/cache; recompute aggregates without the attempt                                                           | Legal hold only                                                                                     |
| Learning progress, SRS and aggregates                                     | Resume learning and show progress                                    | Until account deletion, or 12 months after last account activity; notices at 30 and 7 days before cleanup               | PostgreSQL                                                          | Learner; masked Support by ticket                                            | Delete personal rows; retain only irreversibly anonymized aggregate metrics                                                      | Legal hold only                                                                                     |
| Product analytics events                                                  | Reliability, usage measurement and product improvement               | Pseudonymized event-level data: 12 months; irreversible aggregate metrics: indefinite                                   | Segregated analytics store                                          | Privacy/analytics staff with least privilege; no Teacher/Admin               | Delete event rows, identifiers and derived user-level segments                                                                   | Aggregates only after irreversibly anonymized; legal hold does not make them reusable for marketing |
| Consent evidence                                                          | Prove consent, withdrawal and policy version                         | 5 years after withdrawal or account deletion, subject to legal approval                                                 | PostgreSQL compliance store                                         | Privacy/legal staff; masked Support on ticket                                | Delete at expiry; keep immutable audit trail only for required period                                                            | Legal hold only; never used for learning or marketing targeting                                     |
| Security and access audit logs                                            | Security investigation and access accountability                     | 12 months                                                                                                               | Append-only audit store                                             | Security/privacy staff; Support cannot alter                                 | Expire by partition; delete from searchable log and cache                                                                        | Documented legal/security hold only                                                                 |
| Cache and CDN copies                                                      | Performance only                                                     | Application cache: at most 24 hours; CDN: at most 7 days, but purge on deletion request                                 | Redis/cache and CDN                                                 | Service only                                                                 | Immediate purge request plus TTL expiry verification                                                                             | No legal-hold copy in cache/CDN                                                                     |
| Encrypted backups                                                         | Disaster recovery                                                    | 35 days maximum                                                                                                         | Encrypted immutable backup store                                    | Restricted operations; no routine Support/Admin access                       | Expire backup; crypto-erase per-user key immediately on deletion, making residual encrypted backup data unreadable before expiry | A separately approved legal-hold backup with fixed expiry only                                      |

`ASSESSMENT_ONLY` and `SAVED_RECORDING` are separate data classifications and must never be inferred from UI state alone. The learner explicitly selects “Save recording”; the default is `ASSESSMENT_ONLY`.

#### Data lifecycle

```text
collect with purpose + data classification
    → validate/minimize → process (AI request receives only necessary fields)
    → encrypt and store with per-user/per-recording data key
    → learner can view/edit/export/delete or withdraw optional consent
    → retention job warns, then queues deletion at expiry/inactivity
    → deletion orchestrator removes DB rows, object versions, search documents and cache/CDN copies
    → destroy per-user encryption key (crypto-erasure) and record completion
    → encrypted backups become unreadable immediately; their 35-day retention expires independently
```

For crypto-erasure, each learner receives a data-encryption key (DEK) wrapped by the tenant KMS key. User-owned database fields and objects use that DEK or a derived per-recording key. Deleting the wrapped DEK is an immediate, auditable erasure step; ordinary physical deletion still runs across every primary store. Crypto-erasure does not delete data covered by a documented legal hold.

Learners receive an inactivity notice at 30 days and again at 7 days before cleanup, to the verified contact channel. Any authenticated activity or explicit “keep my account” action cancels that scheduled cleanup. A deletion request immediately revokes sessions and hides the account; the learner may cancel only during the 30-day cancellation window.

#### Learner rights and consent

Learners can view their profile, preferences, progress, chats, recordings, consent history and active sharing grants; correct profile/preferences; export their data; delete individual chats/recordings/attempts; request account deletion; withdraw consent; and request restriction of optional processing. Service-operation processing needed to maintain an active account cannot be withdrawn independently—if the learner objects, the product offers account deletion instead.

Consent is purpose-specific, versioned, recorded before optional processing starts, and is not bundled with the required terms of service:

| Purpose                | Default                                                                      | Withdrawal effect                                                                                                                                                    |
| ---------------------- | ---------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `SERVICE_OPERATION`    | Required to create/use an account and deliver the selected learning features | Offer account deletion; do not treat withdrawal as permission for continued account processing.                                                                      |
| `RECORDING_RETENTION`  | Off until learner chooses to save recordings                                 | Stop new saved-recording retention and queue existing saved raw recordings for deletion; assessment-only processing remains separately disclosed.                    |
| `AI_MODEL_IMPROVEMENT` | Off                                                                          | Stop future use for model improvement, remove queued/deletable training copies, and record any technically irreversible model-training limitation approved by legal. |
| `MARKETING`            | Off                                                                          | Remove learner from marketing audiences and campaigns; preserve only consent evidence.                                                                               |
| `TEACHER_SHARING`      | Off                                                                          | Immediately revoke teacher grants and hide shared data; preserve learner-owned data.                                                                                 |

Each consent event records `consent_id`, `user_id`, `purpose`, optional scope (for example teacher/class), `policy_version`, locale, granted/withdrawn state, `granted_at`, `withdrawn_at`, source, privacy-safe IP hash and correlation ID. The history is append-only; the current state is derived from the latest valid event.

#### Internal and vendor access categories

`Teacher` is a conditional data recipient, not a product actor or standing role, until a teacher-sharing feature is separately approved. `Support` and `AI vendor` are internal/processor access categories, not learner-facing actors.

| Access category | Permitted access                                                                                                                  | Required controls                                                                                                                                                                          |
| --------------- | --------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Learner         | Their own data and exports                                                                                                        | Ownership checks on every resource.                                                                                                                                                        |
| Teacher         | Only data explicitly shared through `TEACHER_SHARING`, limited to agreed learner/class scope; no raw recording or chat by default | Separate grant, expiry/revocation, masking and audit log.                                                                                                                                  |
| Support         | Masked account metadata by default; no private content                                                                            | Ticket-bound, time-limited access. Any unmasking is break-glass: reason, ticket ID, MFA, automatic expiry and immutable audit event.                                                       |
| Admin           | Content/media and role-management only                                                                                            | No access to learner private data merely due to `ADMIN`.                                                                                                                                   |
| AI vendor       | Only pseudonymous/minimized text or audio necessary for one request                                                               | Data-processing agreement, no training by default, encrypted transport, contracted retention at most 24 hours, deletion/attestation capability, auditability and approved transfer region. |

The AI adapter sends a random request correlation ID rather than profile data, redacts unnecessary identifiers, and does not send full chat history or raw audio unless necessary to perform the requested feature. A vendor may use learner data for model improvement only after `AI_MODEL_IMPROVEMENT` is active and legal has approved the contract, cross-border transfer mechanism and deletion process.

#### Privacy APIs and background jobs

| API/job                                                                                             | Contract                                                                                                                                                                                                                                     |
| --------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `GET /api/v1/me/privacy`                                                                            | Returns current consent states, retention schedule, active teacher-sharing grants and pending deletion/export requests for the authenticated learner.                                                                                        |
| `PUT /api/v1/me/consents/{purpose}`                                                                 | Records a grant for one optional purpose with current policy version; requires explicit affirmative action.                                                                                                                                  |
| `DELETE /api/v1/me/consents/{purpose}`                                                              | Records withdrawal, blocks future optional processing immediately and queues the relevant cleanup. Required service-operation processing returns `409 STATE_CONFLICT` with an account-deletion alternative.                                  |
| `POST /api/v1/me/processing-restrictions`                                                           | Records a request to restrict one optional processing purpose while it is reviewed/applied; returns `202` and prevents new processing for that purpose pending the result.                                                                   |
| `POST /api/v1/me/exports`                                                                           | Requires recent re-authentication; returns `202` with an export request. Export worker creates an encrypted ZIP containing manifest JSON, profile/preferences, progress/SRS, chats and permitted media files.                                |
| `GET /api/v1/me/exports/{exportId}`                                                                 | Returns status/manifest only to the request owner. A completed export is delivered through a one-time, TLS-only, signed download link after re-authentication; link expires in 24 hours and the encrypted export object expires in 72 hours. |
| `DELETE /api/v1/me/conversations/{conversationId}` and `DELETE /api/v1/me/recordings/{recordingId}` | Immediately hide the owned item, then queue hard deletion across database/object storage/search/cache. Return `404` for absent or unowned items.                                                                                             |
| `POST /api/v1/me/deletion-requests`                                                                 | Requires recent re-authentication, revokes sessions, creates a 30-day cancellation window and returns `202`. After the window, the deletion orchestrator performs crypto-erasure and cross-store deletion.                                   |
| `DELETE /api/v1/me/deletion-requests/{requestId}`                                                   | Cancels a pending account-deletion request during its cancellation window; re-authentication is required.                                                                                                                                    |
| `RetentionSweepJob` (daily)                                                                         | Finds expired data/inactive accounts, sends scheduled notices, then queues deletion work idempotently.                                                                                                                                       |
| `DeletionOrchestratorJob`                                                                           | Deletes primary DB/object/search/cache/CDN data, destroys keys, records per-store completion and retries safe transient failures.                                                                                                            |
| `BackupExpiryVerifierJob` (daily)                                                                   | Verifies every backup is within the 35-day window and reports/alerts on an overdue backup or undeleted key.                                                                                                                                  |

Exports are never emailed as attachments. The ZIP is encrypted at rest with a KMS envelope key, delivered only over TLS through the one-time link, and becomes unavailable after first download or after 24 hours. The export job and download both require the requesting learner's current authenticated session plus recent re-authentication.

#### Acceptance criteria and required privacy tests

| Scenario                 | Acceptance criteria                                                                                                                                                                                        |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Chat deletion            | Deleted chat is unavailable immediately; within the deletion job SLA it is absent from PostgreSQL, search index, cache/CDN and export; AI vendor copies are deleted/confirmed according to contract.       |
| Recording deletion       | `ASSESSMENT_ONLY` and `SAVED_RECORDING` follow their separate schedules; delete removes original/object versions and derivatives, destroys the recording key, purges CDN/cache and leaves no playable URL. |
| Account deletion         | Sessions revoke immediately; after cancellation window, all user-owned primary data is deleted/crypto-erased, export links are invalidated, indexes/cache are purged and per-store completion is audited.  |
| Inactive account cleanup | Notices are sent at 30 and 7 days; activity cancels cleanup; an inactive account is deleted/anonymized only after its stated retention period and no active legal hold.                                    |
| Consent withdrawal       | Withdrawal blocks future optional processing immediately; marketing/teacher access is removed, saved recordings or training queues are handled per purpose, and consent evidence remains accurate.         |
| Export                   | Re-authentication, ownership, encryption, one-time download, 24-hour link expiry, 72-hour object expiry and exclusion of another learner's data are all enforced.                                          |
| Backup expiry            | User key destruction makes retained backup data unreadable; every backup expires by day 35 and an alert is emitted for any overdue artifact.                                                               |
| Access control           | Learner, Teacher, Support, Admin and AI vendor cannot exceed the matrix; masked-by-default, ticket-bound break-glass, audit logs and consent-scoped teacher sharing are tested.                            |

#### Legal-review checklist

Before launch and when entering a new market, legal counsel must decide and document:

- controller/processor/third-party roles, the lawful basis for each purpose, consent language/versioning and the treatment of voice, children/minors and any sensitive-data classification;
- required notices, data-subject request timelines, consent withdrawal, account deletion, legal-hold and evidence-retention requirements;
- vendor data-processing agreements, AI training prohibition/opt-in, data residency, cross-border transfers, subprocessors, deletion attestations and security obligations;
- whether crypto-erasure, the selected retention periods, backup expiry, audit retention and pseudonymized analytics meet the applicable deletion/recordkeeping rules;
- breach/incident response, notification obligations, regulator registrations/impact assessments and any sector-specific education, consumer, marketing or payment requirements;
- any exception that must be retained by law. Such data must be isolated, access-restricted, assigned a fixed expiry and excluded from learning, analytics, AI improvement and marketing reuse.

## 9. TESTING STRATEGY

| Test level  | Backend                                             | Frontend                    | Purpose                                              |
| ----------- | --------------------------------------------------- | --------------------------- | ---------------------------------------------------- |
| Unit        | JUnit 5 + Mockito                                   | Jest                        | Isolated business rules, components and utilities    |
| Integration | Spring Boot test + PostgreSQL-compatible test setup | Jest + API mocking          | Repository, controller and client contract behaviour |
| E2E         | API + database test flow                            | Browser-driven learner flow | Validate end-to-end user journeys                    |

Minimum coverage by feature:

- Unit-test business decisions: Dictation evaluation mapping, SRS schedule calculation, ownership checks, entitlement checks and `ADMIN` authorization.
- Integration-test API status codes, validation, standard error envelope and `403` for a non-admin content-management request.
- Add E2E coverage for user-visible critical paths: sign in, start/resume Dictation, submit an answer, record/receive Shadowing feedback where testable, save/review a word, view progress, and manage content as an Admin.
- Use stable selectors such as `data-testid` in E2E tests; do not couple tests to layout or styling classes.
- Mock the `AI Service` and `Media Provider` adapters in unit/integration tests. E2E tests must not depend on nondeterministic AI output unless a dedicated controlled environment exists.

## 10. ANTI-PATTERNS TO AVOID

| Anti-pattern                            | Why it hurts Pchinese                                      | Required alternative                         |
| --------------------------------------- | ---------------------------------------------------------- | -------------------------------------------- |
| Business logic in React                 | Scores and progress become inconsistent and easy to bypass | Keep rules in backend services               |
| Entity returned as API payload          | Leaks persistence details and creates brittle contracts    | Return explicit DTOs                         |
| Raw SQL in application code             | Violates the JPA stack and scatters persistence logic      | Use Spring Data JPA repositories/projections |
| Client-only Premium checks              | Users can call protected APIs directly                     | Enforce entitlement server-side              |
| Client-provided admin flag              | A user could claim privileged access                       | Resolve and enforce `ADMIN` server-side      |
| `AI Service` called from browser        | Exposes secrets and bypasses safeguards                    | Call through backend adapter                 |
| Unbounded history endpoints             | Attempt and chat data grows quickly                        | Paginate, filter and index                   |
| Duplicate progress calculations         | Dashboard totals disagree with practice screens            | Centralize progress update rules             |
| Persisting every recording indefinitely | Unnecessary privacy and storage risk                       | Follow minimal data and retention rules      |
| Unvalidated frontend response shape      | Can make UI state disagree with the API contract           | Validate documented response shapes at the client boundary |
| Skipping validation                     | Breaks learner data and security assumptions               | Validate every API input                     |
| Deprecated library without approval     | Creates unsupported technical debt                         | Obtain team approval before introduction     |
| Deleting `/data` or `/uploads` blindly  | Risks irreversible loss of learner or content data         | Obtain user confirmation before deletion     |

## 11. DEVELOPMENT WORKFLOW

1. Identify the feature module and read its `specs/<feature>/spec.md` file.
2. Confirm the actor, required role, entry point, entitlement rule, state changes and failure cases.
3. Update or add backend DTOs, service logic, repository mappings and API documentation together.
4. Update the React feature using contract-bound API clients; do not duplicate backend rules.
5. Add the appropriate unit, integration and E2E tests.
6. Run relevant tests and linting before handoff.
7. Check that the change does not alter a different learning flow unintentionally.

### Definition of done

- [ ] Acceptance criteria in the relevant feature spec are met.
- [ ] Backend unit tests (JUnit 5 + Mockito) and relevant frontend tests (Jest) are written and passing.
- [ ] No JavaScript linting, formatting, build, or test errors remain; ai-service TypeScript
  errors are resolved.
- [ ] API changes follow `/api/v1` and `{ success, data, error, meta }`, and are documented in Swagger/OpenAPI.
- [ ] Input validation, authentication, authorization (including `ADMIN` for privileged operations), and Premium entitlement checks are implemented where applicable.
- [ ] Error cases return the correct HTTP status code and standard error response.
- [ ] Database changes use Spring Data JPA and include a Flyway migration only when an approved schema change is required.
- [ ] Any Flyway migration is focused, reversible where practical, and tested against a clean database.
- [ ] No secrets, debug code, dead code, or unresolved TODO comments are included.

## 12. GIT AND SPEC CONVENTIONS

```text
Branches: feat/[feature-name] | fix/[bug-name] | spec/[feature-name]
Commit:   [type]: [scope] - [description]
Example:  feat(dictation): save segment attempt and progress
```

Create one feature directory per feature, not one large sprint file. Each directory starts with
`spec.md` and may contain its plan, tasks and supporting design artifacts. Example:

```text
specs/
├── F01-identity-account-role-admin/
│   └── spec.md
├── F05-course-catalog-access/
│   └── spec.md
├── F07-dictation-practice/
│   └── spec.md
├── F08-shadowing-practice/
│   └── spec.md
├── F09-dictionary-personal-vocabulary/
│   └── spec.md
├── F10-spaced-repetition-review/
│   └── spec.md
└── F11-ai-learning-buddy/
    └── spec.md
```

Each feature spec should identify actors, user story, access rule, API changes, domain state changes, acceptance criteria, test cases and non-goals.

## 13. CURRENT SPRINT CONTEXT

Sprint: Sprint 1 — Project Foundation  
Focus: Set up the project structure, database connection, and authentication foundation.  
Active specs:

- F11 — AI Learning Buddy: `specs/F11-ai-learning-buddy/spec.md`

All approved feature specifications are located under `specs/`.
