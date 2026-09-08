# Quickstart Validation — F02

## Prerequisites

- F01 authentication is available; run backend tests with a clean compatible DB
  and frontend Jest tests from the documented Maven/npm commands.
- Use two authenticated learners for ownership checks.

## Validation scenarios

1. Read profile on device A, update on B, then submit A’s old version. Expect 409, no overwrite and
   reloadable latest projection.
2. Read or update learner B’s profile using learner A’s credentials. Expect an authorization-safe
   result and no exposure or change to learner B’s preferences.

3. Update learner A's profile with a new valid version, then call
   `GET /api/v1/admin/audit-events?page=0&size=20` as an ADMIN. Expect one new
   `PROFILE_PREFERENCES_UPDATED` item with a timestamp and permitted account-name labels, but no
   profile value, UUID, session, correlation ID or audit details. Verify later pages and empty history.
4. Call the system activity route as learner B. Expect `403`; the learner sidebar has no activity
   history item. As an ADMIN, the same route returns the newest safe events from across accounts.
5. In backend service tests, generate a successful profile update and an applicable account/security
   operation. Verify every appended record has an approved event type/category mapping and bounded
   outcome/reason/changed-field codes only. A profile event may identify changed field codes, never
   prior or new values.
6. Verify audit writer tests reject or omit arbitrary detail keys and free-text notes. Confirm that
   password, credential/token, email, IP address, raw user-agent, profile value, session ID and
   correlation ID are absent from both safe details and the ADMIN response. The ADMIN UI still shows
   only event type, time and permitted account-name labels.

Check keyboard/focus handling, WCAG AA semantic tokens, 44px touch targets, safe 400/401/409 UI and
absence of browser token storage.

## Corrected ADMIN system-history validation - 2026-09-09

- `mvn -q verify` passed. Testcontainers integration cases were skipped because Docker is not
  available in this environment; the ADMIN authorization, safe projection and pagination tests ran.
- `npm test -- --runInBand` passed: 9 suites and 23 tests. `npm run build` also passed.
- `npm run lint` completed with zero errors. It retains three unrelated existing unused-variable
  warnings in `App.test.jsx` and the F01 sidebar files.
- The only history contract is `GET /api/v1/admin/audit-events`. A learner has no `/me/activity`
  route, client, navigation item or settings-panel history. The Admin sidebar provides the system
  activity screen.
- No migration was introduced or modified; the feature uses the existing `auth_audit_events` table
  recorded in `DATA_short.md`.
- Structured audit-event validation uses the approved category/type taxonomy and bounded summary
  codes defined in `data-model.md`; it does not widen the public ADMIN response.
- Validation on 2026-09-08: `mvn -q verify` passed, including the new audit-taxonomy/redaction and
  safe ADMIN-projection tests. `npm test -- --runInBand` passed (9 suites, 23 tests), and
  `npm run build` passed. ESLint has zero errors and retains only the three pre-existing warnings
  in `App.test.jsx` and F01 sidebar files. Testcontainers cases remain skipped when Docker is
  unavailable.
