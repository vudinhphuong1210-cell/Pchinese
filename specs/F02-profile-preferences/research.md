# Phase 0 Research — F02 Profile Preferences

## Decision: use the existing user row for profile concurrency

**Decision**: `users.version` is the profile version. GET returns it and PATCH must carry
`expectedProfileVersion`.

**Rationale**: F02 explicitly requires a stale update to fail without overwriting a concurrent
valid preference; `DATA_short.md` already provides optimistic-lock versions.

**Alternatives considered**: Last-write-wins and a separate profile table were rejected because they
lose user changes or add an unnecessary F02 table.
