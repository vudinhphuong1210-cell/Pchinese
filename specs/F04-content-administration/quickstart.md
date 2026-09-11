# Quickstart Validation — F04 Content Administration

## Prerequisites

- A PostgreSQL/Testcontainers-capable local environment.
- Backend and frontend dependencies installed.
- Canonical migrations available from the shared Supabase migration directory.
- An active ADMIN session and a non-ADMIN test session.

## One-time legacy V004 upgrade

The former `V004__f04_content_administration.sql` was a non-canonical development migration.
Do not restore or manually delete individual history rows for it. For a database that has applied
V004 but now uses the canonical migrations, a migration owner must: back up/inspect the schema,
run Flyway `repair` to mark V004 as deleted, record the already-existing canonical initial schema
as applied without executing it, then run the forward canonical migration. The canonical F04
migration preserves legacy audit rows and upgrades their table in place. This controlled upgrade
has been completed for the configured Supabase database.

## Validation sequence

1. Provision an empty PostgreSQL database using the canonical migration source. Verify the required
   content audit table, target/actor/correlation indexes, and database rejection of audit update or
   delete attempts. Provision a representative pre-F04 fixture and verify the forward migration
   preserves existing data.
2. As a non-ADMIN, attempt every draft read and write operation. Each returns a safe authorization
   error and changes no content or audit state outside the applicable authorization boundary.
3. As an ADMIN, submit a valid YouTube watch, share, or embed link and a bare video ID. Each starts
   pending review with the same canonical video identity. A watch URL may include incidental `list` or
   radio parameters when it contains exactly one valid `v` ID; those parameters are discarded. Attempt
   HTTP, non-YouTube, standalone playlist, channel, short, malformed, and arbitrary links; none becomes
   approvable or learner-visible.
   Confirm the flow neither offers a local video file input nor uploads/downloads video bytes.
4. Exercise every media transition. Permit only pending-to-approved/rejected/quarantined and
   approved-to-rejected/quarantined. Verify rejected/quarantined assets cannot be approved again and
   require a new approved replacement.
5. Create a Free topic, lesson, and ordered segments. Prove lesson publication fails for an
   unpublished parent, no segment, any draft/unpublished/archived segment, unapproved media, or
   non-Free access. Publish a valid chain and confirm the safe public-query predicate accepts it.
6. While a lesson is published, attempt lesson, segment, ordering, and media changes. Each must be
   rejected without a live state change. Unpublish, make the change, validate, and republish.
7. Reject or quarantine media used by multiple published lessons. Verify every dependency becomes
   unavailable atomically, all related audit events share one correlation, and learner records are
   untouched.
8. Archive every content type. Verify it cannot be restored or republished and that required safe
   audit events exist for accepted and rejected lifecycle commands.
9. In the admin UI, verify keyboard tabs, focus visibility, labelled fields, dialog focus/escape/
   restore behavior, semantic status feedback, responsive controls, conflict reload, and destructive
   confirmation. Confirm no access credential is written to browser-readable persistent storage.
10. Verify the stored canonical video ID, not the submitted URL, is the sole source available to the
    later learner-player feature for constructing its permitted YouTube embed. Learner playback UI
    itself remains outside F04.

## Commands

From backend/, run the relevant unit, controller-integration, and migration suites:

~~~powershell
mvn test
mvn verify
~~~

From frontend/, run the F04 Jest and E2E coverage plus quality gates:

~~~powershell
npm test -- --runInBand ContentAdminPage.test.jsx
npx playwright test e2e/f04-content-administration.spec.js
npm run lint
npm run build
~~~

## Expected result

All commands pass. Each scenario returns the documented standard response shape, exposes no learner
or provider-private data, preserves the required version and lifecycle invariants, and leaves the
canonical audit history immutable and complete.
