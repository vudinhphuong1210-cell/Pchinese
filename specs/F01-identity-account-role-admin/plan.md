# Implementation Plan: Identity, Account, and Role Administration

**Branch**: `F01-identity-account-role-admin` | **Date**: 2026-09-05 | **Spec**: [spec.md](./spec.md)  
**Input**: F01 specification, [research.md](./research.md), [data-model.md](./data-model.md), [contracts/f01-openapi.yaml](./contracts/f01-openapi.yaml), project constitution and API registry.

## Summary

Deliver the first protected Pchinese product slice: account registration and verification, sign-in,
credential recovery, server-managed session lifecycle, and the restricted Admin Account/Roles tab.
Spring Boot remains the transactional authority for credentials, sessions, account status, role
grants and audit records. React supplies typed client flows, accessible form UX and route guards
only; it never treats its state or JWT claims as authority.

The implementation uses the existing Java 21/Spring Boot modular-monolith target and the canonical
`DATA_short.md` auth schema. It does not add a service, provider, entitlement/quota feature,
learner-data administration capability, or direct browser-to-provider call.

## Technical Context

**Language/Version**: Java 21 + Spring Boot 3.4.5 (Maven); TypeScript strict + React 18 + Vite  
**Primary Dependencies**: Spring Security, Spring Web, Jakarta Validation, Spring Data JPA, Flyway,
PostgreSQL driver; React typed API client, Tailwind CSS 3.x, Jest  
**Storage**: PostgreSQL 18; canonical auth tables in `DATA_short.md`; schema migration via Flyway  
**Testing**: JUnit 5 + Mockito; Spring Boot/PostgreSQL-compatible integration tests; Jest typed-client
and component tests; browser E2E for critical identity/admin journeys  
**Target Platform**: Spring Boot API server and responsive browser SPA; mobile client contract is
documented but no native client is implemented in this feature  
**Project Type**: Web application — React SPA plus Spring Boot modular monolith  
**Performance Goals**: Under normal conditions, a learner completes registration, verification and
first sign-in in under five minutes (F01 SC-003). No additional latency SLO is introduced.  
**Constraints**: Public API uses `/api/v1` and `{ success, data, error, meta }`; access JWT lives
10 minutes in browser memory; refresh is opaque, rotated and never exposed in ordinary JSON; no raw
credential/token/private learning data in API, logs or audit events.  
**Scale/Scope**: One F01 slice: 10 authentication/session endpoints, 5 exact-ID Admin account/role
operations, their DTOs, tables/migrations, React flows and contract/security tests.

## Constitution Check

### Pre-design gate

| Gate | Result | Plan response |
| --- | --- | --- |
| Fixed architecture | PASS | Uses only React SPA, Spring Boot modular monolith, PostgreSQL/JPA and Flyway. No AI service or new infrastructure. |
| Server authority | PASS | Spring Boot validates current account/session/authz version and executes every role/status/session mutation transactionally. |
| Security/privacy | PASS | Bcrypt cost ≥12, signed access JWT, opaque hashed refresh token, CSRF/origin checks, exact-ID admin projection, safe audit data and no learner-data bypass. |
| Contract/migration safety | PASS | DTO/OpenAPI/envelope first; schema exactly follows `DATA_short.md`; Flyway migrations are focused and clean-DB tested. |
| Quality/accessibility | PASS | Backend unit/integration/security coverage and frontend Jest/E2E accessibility/error-state coverage are planned. |

### Decisions made during research

1. F01's non-enumeration requirement overrides the older `409 DUPLICATE_RESOURCE` register
   entry in the current API registry. Registration returns one neutral accepted response for a
   new, existing or ineligible email; verification delivery is conditional and never disclosed.
   The implementation change must update `API.md` and OpenAPI in the same review.
2. `OTHER` audit reason has a trimmed, non-empty safe note capped at 280 characters. It excludes
   credentials and learner-private content. This makes “short” testable without storing unbounded
   administrator input.
3. Only an `ACTIVE`, verified target account is manageable for Admin role/lock/unlock commands.
   Pending, disabled, locked or otherwise unmanageable IDs return the same safe not-found result.
4. Every accepted or rejected sensitive role/lock/unlock command writes a minimal safe audit
   outcome. Only successful role lifecycle changes write `user_roles` grant/revoke rows.
5. F01 owns the secure auth/session API and current-session logout. F02 may later compose the
   profile/settings and device-management UX; F01 does not create F03 plan/entitlement records.
   Until F03 owns entitlement resolution, the documented Free two-session ceiling is a server
   authentication configuration, not a client rule or manual entitlement operation.

### Post-design gate

PASS. The design artifacts keep all authority/persistence in the backend, introduce no
Constitution exception, and leave Premium/quota, profile preferences, data export/deletion and
learner-private administration outside F01.

## Project Structure

### Documentation (this feature)

~~~text
specs/F01-identity-account-role-admin/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── f01-openapi.yaml
~~~

### Source Code (repository root)

~~~text
backend/
└── src/
    ├── main/java/net/pchinese/
    │   ├── common/
    │   │   ├── api/                 # envelope, correlation and pagination
    │   │   ├── error/               # stable API exception mapping
    │   │   └── idempotency/         # refresh replay support
    │   ├── security/
    │   │   ├── authentication/      # JWT validation/principal
    │   │   └── authorization/       # ADMIN policy and audit helpers
    │   ├── auth/
    │   │   ├── api/                 # AuthController + validated DTOs
    │   │   ├── application/         # registration, verification, login, reset, sessions
    │   │   ├── domain/
    │   │   └── persistence/         # entities + Spring Data repositories
    │   └── users/
    │       ├── api/                 # exact-ID role/status DTOs and controller
    │       ├── application/          # account role/access commands
    │       ├── domain/
    │       └── persistence/
    ├── main/resources/db/migration/  # approved F01 Flyway migrations
    └── test/java/net/pchinese/
        ├── auth/
        ├── security/
        └── users/

frontend/
└── src/
    ├── api/                         # auth and admin-users typed clients + DTOs
    ├── features/
    │   ├── auth/                    # register, verify, login, reset, in-memory auth session
    │   └── admin/                   # exact-ID Account/Roles tab only
    ├── pages/                       # public auth pages and protected settings/admin composition
    ├── routes/                      # guest/auth/admin UX guards
    └── test/                        # Jest client/component/E2E support
~~~

**Structure Decision**: Create only the `auth`, `security` and minimal `users` modules required
by F01. Keep Controller → validated DTO → Service → Spring Data repository → JPA entity boundaries.
Frontend feature components consume only `src/api/` methods; reusable dialog/form primitives remain
in `src/components/`.

## Implementation Approach

### 1. Backend account and credential lifecycle

- Implement `users` status `PENDING_VERIFICATION → ACTIVE` only after a valid single-use email
  verification credential. `ACTIVE → LOCKED → ACTIVE` is an Admin-only state path; unlock never
  restores an old session. `DISABLED` remains reserved for governed operations outside F01.
- Store normalized email lookup as a unique HMAC hash and credentials with bcrypt cost at least 12;
  never return either value. Create only controlled deployment bootstrap Admins, never public ones.
- Use hashed, single-use, expiry-bound `auth_action_tokens` for verification and reset. Register,
  verification resend and password-reset request responses are neutral to prevent account
  enumeration.

### 2. Authentication, JWT and session lifecycle

- Issue an RS256 (or approved asymmetric allowlisted) access JWT containing only
  `iss,aud,sub,sid,jti,iat,nbf,exp,typ=access,authzVersion`. Validate signature, algorithm,
  `kid`, issuer/audience, token type/time, active session/account and current authorization version
  before a protected principal exists.
- Create server-side `auth_sessions` and opaque 256-bit refresh-token families. For Web, set the
  refresh value only in `__Host-pchinese-refresh` (HttpOnly/Secure/SameSite=Strict) and issue the
  server-bound CSRF cookie. Access token stays only in frontend memory.
- Rotate refresh token under `PESSIMISTIC_WRITE` or equivalent compare-and-set. An identical
  `X-Refresh-Request-Id` replays a KMS-encrypted result for at most 30 seconds; a different request
  against a rotated/revoked token revokes its whole family, appends a safe high-severity audit event
  and returns generic `401 REFRESH_TOKEN_INVALID`.
- Logout/revoke uses owned session IDs only. Role grant/revoke, password reset and account lock
  increment `authz_version` and revoke affected sessions/families atomically.

### 3. Minimal Admin Account/Roles capability

- `GET /users/{userId}/roles` accepts an exact UUID and returns only role(s) plus locked/unlocked
  access state. It has no directory listing, autocomplete, email/name/profile, entitlement, quota,
  attempt, recording, vocabulary, progress or chat field.
- Role grant/revoke permits only another eligible target and serializes target/active-ADMIN role
  state. A command that would self-change or leave no active ADMIN returns `409 STATE_CONFLICT`
  without changing data.
- Lock/unlock require one standard reason `SECURITY|POLICY|USER_REQUEST|OTHER`; `OTHER` requires
  the bounded safe note. Lock ends active access immediately; unlock requires a new sign-in.
- Append an immutable audit event for outcome, actor, target, correlation ID and safe reason/details.
  Successful role changes additionally capture before/after roles. No audit payload contains a
  password, raw token, learner-private content or unnecessary PII.

### 4. Frontend flows and UI states

- Add guest-only Register, Verify email, Login, Forgot password and Reset password pages. Keep them
  outside authenticated app shell; use neutral messages for requests that might otherwise enumerate
  accounts.
- Add typed `auth` and `adminUsers` API modules. Implement in-memory access token state and
  refresh single-flight; clear state and redirect on failed refresh, revoked token or current-session
  revoke. Never introduce browser token storage or a global domain store.
- Add Session Settings UI for current/selected/all session commands required by F01; F02 later owns
  richer session-management/profile presentation. Current-session revoke requires confirmation.
- Add the Admin Account/Roles tab: exact UUID input first, minimal projection only, confirm dialogs
  for destructive role/status commands, reason selector and conditional `OTHER` note. Do not make
  optimistic admin mutations; reload projection after `409`.
- Use semantic design tokens, labels, field errors, error-summary focus, live status feedback,
  keyboard/focus-managed dialogs, WCAG AA contrast, 44×44px touch targets and responsive layouts.

### 5. Contract, migration and test sequence

1. Update OpenAPI/API.md and typed DTO contracts first, including the neutral registration outcome.
2. Add schema/entity/repository migrations in canonical auth dependency order:
   `users → user_roles → auth_sessions → refresh_tokens → auth_action_tokens →
   refresh_idempotency → auth_audit_events`.
3. Implement services and transaction/locking rules, then thin controllers and exception mapping.
4. Implement typed frontend clients before forms/pages; map `400/401/403/404/409/429` to safe UX.
5. Add unit, integration, contract and E2E tests; verify migrations on a clean PostgreSQL database.

## Complexity Tracking

No Constitution violation or complexity exception is required.
