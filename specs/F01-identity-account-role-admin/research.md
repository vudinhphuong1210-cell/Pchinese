# Phase 0 Research — F01 Identity, Account, and Role Administration

## Sources examined

- `spec.md` and the F01/F02/F03 ownership boundary in the MVP Feature Map
- Root `AGENT.md`, `CONSTITUTION.md`, `CLAUDE.md` and `API.md`
- `backend/AGENT.md`, `backend/CLAUDE.md`, `backend/CONSTITUTION.md`
- `frontend/AGENT.md`, `frontend/CLAUDE.md`, `frontend/CONSTITUTION.md`, `frontend/DESIGN.md`
- `DATA_short.md`

No external service, AI provider or new infrastructure is required by F01.

## Decision 1 — Keep identity authority in the Spring Boot modular monolith

**Decision**: Implement F01 in backend `auth`, `security` and minimal `users` modules. React
only renders forms, calls contract-bound API clients and supplies UX route guards.

**Rationale**: The constitution assigns authentication, account state, authorization, transactions,
persistence and audit to Spring Boot. It prohibits client-derived authority and any new service.

**Alternatives considered**:

- Browser/JWT-claim role authorization — rejected: role and account state can become stale or be
  tampered with; the backend must load current server state.
- A separate identity microservice or external provider — rejected: violates the fixed architecture
  and is not authorized by F01.

## Decision 2 — Use server-managed, revocable session credentials

**Decision**: Use 10-minute signed access JWTs plus server-side sessions and opaque, rotated refresh
tokens. Browser access tokens are memory-only; browser refresh credentials use the protected
`__Host-pchinese-refresh` cookie and CSRF/origin validation.

**Rationale**: This is the locked project JWT/session policy. It allows immediate invalidation after
role, reset or lock events through `authzVersion` and session/family revocation.

**Alternatives considered**:

- Long-lived JWT without server session — rejected: cannot reliably revoke access after lock/role
  changes.
- Storing refresh tokens in localStorage or readable cookies — rejected: violates token/privacy
  policy and expands XSS exposure.

## Decision 3 — Make refresh rotation concurrency-safe

**Decision**: Lock the refresh token/session row for rotation. Replay an identical
`X-Refresh-Request-Id` for at most 30 seconds from the KMS-encrypted idempotency record. A distinct
retry using an old/rotated/revoked token revokes the token family and returns a generic 401.

**Rationale**: It prevents two valid descendants of one refresh token while preserving harmless
network retries. It is explicitly required by `CLAUDE.md` and `DATA_short.md`.

**Alternatives considered**:

- Accept every concurrent refresh — rejected: creates parallel valid refresh tokens.
- Treat every retry as reuse — rejected: makes normal network failure unsafe for learners.

## Decision 4 — Use exact-ID minimal account-management projections

**Decision**: Admin lookup is only `GET /api/v1/users/{userId}/roles` with an exact UUID. The
response contains permitted ADMIN-role and locked/unlocked state only. Role grant/revoke and
lock/unlock use dedicated command routes; no list/search/autocomplete is exposed.

**Rationale**: F01 and the MVP map prohibit Admin private-data browsing while still requiring the
Account/Roles tab. A safe 404 prevents account-existence disclosure for invalid/unmanageable IDs.

**Alternatives considered**:

- User directory/search by email/name — rejected: violates F01 FR-007 and leaks account metadata.
- Returning profile or learning summaries with the role projection — rejected: violates F01 FR-006.

## Decision 5 — Serialize sensitive role and account-state changes

**Decision**: Execute each role/status command in one service transaction. Lock the target account
and active role state, evaluate self-action/final-active-ADMIN guards, mutate only if valid, update
`authz_version`, revoke affected credentials where required and append a safe immutable audit outcome.

**Rationale**: Concurrent revokes/locks must not remove or disable the final active administrator.
Server-side locks and constraints are required; client UI cannot protect this invariant.

**Alternatives considered**:

- Check active-ADMIN count before a later mutation without locking — rejected: concurrent requests
  can both pass and leave no active Admin.
- Hard-delete a role row — rejected: loses audit history; `user_roles` requires immutable
  grant/revoke history.

## Decision 6 — Make vague safety terms testable

**Decision**: A role target must be ACTIVE and verified. `OTHER` requires a trimmed safe note of
1–280 characters. Sensitive role/lock/unlock requests append an accepted or rejected safe audit
outcome; only successful role changes create a role history row.

**Rationale**: F01 requires an eligible target, a short safe note and audit outcomes but leaves
their exact mechanics unspecified. These constraints prevent dormant privilege, unbounded audit
input and untraceable rejected security operations.

**Alternatives considered**:

- Allow pending/locked/disabled targets — rejected: creates ambiguous status transitions and
  dormant privilege.
- Leave note unbounded — rejected: violates data minimization and makes validation untestable.
- Audit only successes — rejected: does not fully meet F01 SC-002's actor/target/reason/outcome
  evidence goal.

## Decision 7 — Resolve registration anti-enumeration conflict in favour of F01

**Decision**: Return a neutral accepted registration response for both new and existing/ineligible
emails, and conditionally send the verification flow without revealing the result. Update the older
`409 DUPLICATE_RESOURCE` register entry in `API.md` and root auth table during implementation.

**Rationale**: F01 explicitly says duplicate registration must not reveal whether another account
exists. The privacy-preserving requirement is stricter than the older registry behavior.

**Alternatives considered**:

- Preserve `409 DUPLICATE_RESOURCE` — rejected: distinguishable response can enumerate accounts.
- Return account status/verification result — rejected: directly leaks account information.

## Decision 8 — Keep F03/F02 boundaries intact

**Decision**: F01 creates no subscription plan, entitlement, Premium, quota, profile-preference or
privacy-export data. It provides the secure auth/session lifecycle and current-session logout.
F02 owns profile preferences; F03 owns automatic Free entitlement/quota.

**Rationale**: The Feature Map assigns those responsibilities to F02/F03.

**Alternatives considered**:

- Add entitlement records during registration — rejected: duplicates F03 responsibility.
- Defer every session endpoint to F02 — rejected: F01 must provide secure session lifecycle and
  protected identity foundation for dependent features.
