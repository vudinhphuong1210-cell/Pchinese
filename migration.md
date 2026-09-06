# Migration Register and Implementation Checklist

## C1 — Content audit schema missing from the canonical contract

**Status:** Open — implement when F04 Content Administration code begins.

### Problem

F04 needs append-only `content_audit_events` records for ADMIN content and media actions. The
database already exists on Supabase, so an applied Flyway migration must never be changed or
deleted. `DATA_short.md` is the canonical MVP schema contract and must describe the audit table
before the corresponding migration is implemented.

### Required changes

1. Update `DATA_short.md` with the approved `content_audit_events` table, its indexes, foreign
   keys, retention/privacy rules, and the list of audit-safe fields. Do not include credentials,
   tokens, raw media, learner-private data, or unredacted request bodies.
2. Read the Supabase database's `flyway_schema_history` table and select the next unused migration
   version. Never assume a fixed version number and never reuse or edit an applied version.
3. Add one focused forward migration at
   `backend/src/main/resources/db/migration/V<next>__content_audit_events.sql`.
4. Add the JPA entity/repository and transactional append-only audit write only after the migration
   and schema contract agree. Audit rows must not be updated or deleted through application code.
5. Test the migration on both a clean database and a copy/representative instance of the current
   Supabase schema. Confirm an upgrade preserves existing content, learner progress, attempts,
   recordings, conversations, entitlements, and sessions.

### Migration design constraints

- Use a new forward-only Flyway migration; do not alter `V001`–`V011` or any migration recorded
  in Supabase.
- Keep the migration limited to the audit table, its constraints, and its indexes. Put unrelated
  schema work in a different approved migration.
- Relate each event to the responsible ADMIN actor and the affected content or media resource using
  approved foreign keys. Store only minimal, safe before/after metadata needed for an audit trail.
- Apply the standard UUID, UTC `timestamptz`, snake_case, Spring Data JPA, transaction, and
  error-envelope conventions from `DATA_short.md`, `CONSTITUTION.md`, and `backend/AGENT.md`.
- If a repair is ever needed after deployment, add another forward corrective migration. Do not
  roll back by editing history.

### Verification checklist

- [ ] `DATA_short.md` is updated and approved before writing SQL.
- [ ] The next version is confirmed from `flyway_schema_history`.
- [ ] The new migration succeeds on a clean database.
- [ ] The new migration succeeds against the current Supabase schema without data loss.
- [ ] Migration validation/checksum passes without changing applied entries.
- [ ] F04 audit writes are transactional, append-only, and contain no secrets or learner-private
      payloads.
- [ ] Backend integration tests cover successful audit creation and failed content mutations that
      must not create an audit event.
