# Phase 0 Research — F02 Profile Preferences

## Decision: use the existing user row for profile concurrency

**Decision**: `users.version` is the profile version. GET returns it and PATCH must carry
`expectedProfileVersion`.

**Rationale**: F02 explicitly requires a stale update to fail without overwriting a concurrent
valid preference; `DATA_short.md` already provides optimistic-lock versions.

**Alternatives considered**: Last-write-wins and a separate profile table were rejected because they
lose user changes or add an unnecessary F02 table.

## Decision: reuse minimized account audit events for ADMIN system activity history

**Decision**: F02 appends a `PROFILE_PREFERENCES_UPDATED` event to the existing
`auth_audit_events` store for a changed profile and exposes an ADMIN-only, paginated system projection
with only a safe event type, timestamp and permitted actor/target account-name labels.

**Rationale**: The canonical schema already has immutable audit records for account/security
events. Reusing it preserves one audit trail, avoids a migration and gives ADMIN the operational
visibility required to supervise accounts without exposing audit metadata, UUIDs or learner-profile values.

**Alternatives considered**: A profile-specific history table duplicates the canonical audit store;
returning raw audit details can expose sensitive operational context; a learner-facing history would
not satisfy the ADMIN supervision need and is out of scope.

## Decision: use a centralized, allowlisted audit-event taxonomy

**Decision**: Keep `event_type` as the approved event identifier and map it in the backend to one
approved category: `AUTHENTICATION`, `ACCOUNT_LIFECYCLE`, `ACCESS_CONTROL`, `PROFILE_CHANGE`, or
`SECURITY_OPERATION`. The existing audit record carries actor/target, occurrence time and private
session/correlation context. Its safe structured summary may contain only a bounded `outcomeCode`,
an applicable bounded `reasonCode`, and allowlisted `changedFieldCodes`; it never contains a free-text
note or old/new value.

**Rationale**: This gives operations a consistent incident trail while retaining the current schema
and preventing audit details from becoming a second store of learner-private data. The category is
centrally derived from the event type, so writers cannot submit an arbitrary category. The public
ADMIN projection remains unchanged and minimized: type, time and permitted account-name labels only.

**Alternatives considered**: Storing narrative messages makes redaction and reporting unreliable;
persisting a category submitted by each caller permits inconsistent taxonomy; adding a second audit
table or new columns is unnecessary because `auth_audit_events` already supports the required private
references and structured safe details.
