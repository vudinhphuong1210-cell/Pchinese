# Phase 0 Research — F02 Profile and Session Control

## Decision: use the existing user row for profile concurrency

**Decision**: `users.version` is the profile version. GET returns it and PATCH must carry
`expectedProfileVersion`.

**Rationale**: F02 explicitly requires a stale update to fail without overwriting a concurrent
valid preference; `DATA_short.md` already provides optimistic-lock versions.

**Alternatives considered**: Last-write-wins and a separate profile table were rejected because they
lose user changes or add an unnecessary F02 table.

## Decision: sessions remain F01 security objects

**Decision**: F02 consumes the F01 `auth_sessions` lifecycle; it adds privacy-safe projection,
current-session confirmation and learner UI, not a second session store.

**Rationale**: Session ownership/revocation is a single security authority. ADMIN cannot browse or
operate another learner’s sessions.

**Alternatives considered**: Frontend-maintained devices or Admin support lookup were rejected for
authority/privacy reasons.

## Decision: device-limit recovery is restricted, ephemeral and non-authenticated

**Decision**: A verified-at-login learner at the session ceiling receives only a single-purpose
in-memory flow credential. It can list/revoke owned sessions through dedicated flow routes, never
returns a normal access/refresh session, and ends on completion, close/reload signal or short expiry.

**Rationale**: F02 forbids silent eviction and ordinary product access before the learner frees a
device.

**Alternatives considered**: Automatically revoke least-recent session, issue a full session, or
persist a flow credential in browser storage — all violate F02 security/UX constraints.

## Decision: current-session revocation is explicit

**Decision**: Current device is marked in the projection and requires a confirmation flag/dialog.
Accepted revoke clears in-memory auth and routes to login.

**Rationale**: Prevents an accidental self-lockout while preserving server authority.

**Alternatives considered**: Revoke immediately on list interaction — rejected as destructive and
non-accessible.
