# Quickstart Validation — F01 Identity, Account, and Role Administration

This guide validates the implemented F01 slice. It does not authorize implementation shortcuts;
use [plan.md](./plan.md), [data-model.md](./data-model.md) and
[contracts/f01-openapi.yaml](./contracts/f01-openapi.yaml) as the design references.

## Prerequisites

1. Java 21, Maven wrapper support, Node/npm and a clean PostgreSQL-compatible test database.
2. Deployment-managed JWT signing keys, bcrypt configuration, email test transport, token pepper,
   KMS/test encryption configuration and allowed web origin. Do not commit any secret.
3. One initial ADMIN provisioned by the controlled deployment/bootstrap process. Do not create an
   Admin through public registration.
4. Backend migration state starts clean and matches `DATA_short.md`.

## Build and automated checks

From the repository root on Windows:

~~~powershell
Set-Location backend
.mvnw.cmd test

Set-Location ..rontend
npm ci
npm test
~~~

When the project adds explicit lint/build scripts, run their documented commands as part of the same
check. Run Flyway-backed integration tests against a clean database before testing a shared
migration.

## End-to-end validation scenarios

### 1. Neutral registration, verification and sign-in

1. Submit a valid new email/password to `POST /api/v1/auth/register`.
2. Submit the same request again and compare HTTP status/body shape.
3. Obtain the test-mail verification credential and confirm it.
4. Sign in with `email,password,deviceId,deviceLabel,platform`.

Expected:

- Both registration requests receive the same neutral accepted shape; neither response reveals
  whether the account already existed.
- Verification is single-use; a consumed/expired credential is safely rejected.
- Successful verification makes the account ACTIVE.
- Login returns an in-memory access token/session result. Browser refresh credential is only a
  protected cookie, never ordinary JSON, storage or console output.

### 2. Refresh rotation and revoked authorization

1. Login as an ACTIVE learner and retain the current refresh request ID.
2. Send refresh twice with the same ID before the 30-second idempotency expiry.
3. Retry the pre-rotation refresh credential with a different ID.
4. Login again, then lock that account through an authorized Admin request.
5. Call a protected endpoint with the old access token and attempt refresh.

Expected:

- Same-ID retry replays the one result; no second active refresh descendant exists.
- Different-ID old-token retry invalidates the refresh family and returns generic
  `401 REFRESH_TOKEN_INVALID`.
- After lock, old access/refresh credentials lose access immediately; unlock requires a new sign-in.
- No response/log displays the raw refresh token or detects reuse verbosely.

### 3. Current-session sign-out

1. Sign in as a learner.
2. Call `POST /api/v1/auth/logout` with the current refresh credential.
3. Attempt to refresh or access a protected route with that credential.

Expected:

- The current authentication state is cleared and the learner returns to sign-in.
- The revoked credential cannot be used for a later refresh or protected action.

### 4. Password recovery

1. Request reset for an existing email and an unknown email.
2. Confirm a valid reset with a compliant replacement password.
3. Attempt to use every pre-reset session/refresh credential.

Expected:

- Both reset-request responses are neutral accepted results.
- Reset credential is one-use and safe on invalid/expired input.
- Successful reset updates credential, increments authorization state, revokes all existing sessions
  and requires a new sign-in.

### 5. Admin User Management restriction and audit

1. Sign in as controlled bootstrap ADMIN and enter /admin/users.
2. Load the first and a later directory page; use boundary size values 1 and 50, then attempt the
   route as a non-ADMIN.
3. Confirm every directory row has the owner-set account name (or “Chưa đặt tên”), target UUID,
   lifecycle state and ADMIN role state; inspect the response to confirm it has no email, other
   profile fields, session, entitlement, quota or learner data.
4. Grant ADMIN, revoke ADMIN, lock and unlock a second eligible target using each standard reason;
   use OTHER once with a 1–280-character safe note.
5. Attempt self role/lock change and concurrently attempt to revoke/lock the final active ADMIN.

Expected:

- User Management has a server-paginated safe directory with a display-only account name, no
  email/name/profile search and no learner-data path; a non-ADMIN receives a safe authorization failure.
- Every accepted/rejected command has an immutable safe audit outcome; successful role changes have
  before/after role history.
- Empty/missing OTHER note is validation failure with no state change.
- Self/final-ADMIN operations return `409 STATE_CONFLICT` and preserve a valid active Admin.
- Grant/revoke/lock invalidates target sessions; Admin cannot edit plan/quota or private learner data.

## Frontend accessibility and error-state checks

- All auth and Admin forms have visible labels, field-associated errors and an error summary that
  receives focus after failed submit.
- Async request status is announced without exposing sensitive account information.
- Lock/unlock dialogs trap/manage focus, allow Escape/cancel and
  return focus to their trigger.
- Keyboard navigation, focus-visible state, WCAG AA contrast, semantic design tokens, reduced
  motion and 44×44px touch targets work at mobile and desktop breakpoints.
- Browser inspection confirms no access/refresh credential is written to localStorage,
  sessionStorage, IndexedDB or a JavaScript-readable cookie.

## Required test evidence

- Backend unit tests: credential/account lifecycle, ACTIVE/LOCKED guards, role status transitions,
  OTHER-note validation, current-session sign-out, role/history audit and final-Admin concurrency logic.
- Backend integration tests: DTO validation, API envelopes, neutral anti-enumeration responses,
  JWT/session rejection, refresh replay/reuse, CSRF/origin checks and clean Flyway migration.
- Frontend Jest: contract-envelope/error mapping, refresh single-flight, redirect/clear behavior,
  no-token-storage behavior, safe paginated User Management UI, safe conflict UX and confirmation flows.
- E2E: the five scenarios above, using stable `data-testid` values and no CSS-coupled selectors.

## Implementation validation — 2026-09-07

- `API.md`, the F01 OpenAPI contract and the frontend API modules all use the same `/api/v1` routes; no contract correction was required.
- `mvn clean verify` passes unit tests and compiles the PostgreSQL-backed controller/migration integration suite. The suite is configured to run automatically through Failsafe when Docker is available.
- This workstation has no reachable Docker daemon, so Testcontainers correctly skipped its clean-PostgreSQL execution. Run `mvn verify` with Docker available before merging a Flyway migration.
- Frontend lint, Jest and Vite production build pass.

## User Management validation — 2026-09-08

- `GET /api/v1/users?page=&size=` is ADMIN-only, validates zero-based pages and sizes 1–50,
  and returns the permitted account name, UUID, lifecycle state and active ADMIN role in a standard envelope.
- Backend unit coverage verifies server pagination and the safe projection. Controller integration
  coverage verifies unauthenticated/non-ADMIN rejection, validation, the permitted account name,
  and the absence of email, other profile fields, session, entitlement and learner-data fields.
- The UI is labelled “Quản lý người dùng” in both its heading and sidebar, loads the paginated
  directory without manually entering a UUID, and retains commands only for the selected user.
- `mvn verify`, frontend lint, all 17 Jest tests and the Vite production build pass. PostgreSQL
  Testcontainers integration execution remains skipped on this workstation because Docker is not
  reachable; run `mvn verify` with Docker available before merging.
- Account-name regression coverage verifies server-side decryption for an ADMIN response, the
  “Chưa đặt tên” fallback, and the modal's rendered account name. `mvn verify`, frontend lint,
  Jest and Vite build passed again after this change.

## Same-browser multi-account validation

1. Sign in verified account A in one tab. Use **Đăng nhập tài khoản khác** to open a new tab and
   sign in verified account B.
2. Reload both tabs, then let each make an authenticated request after access-token refresh. Each
   tab remains on its own account; inspecting browser storage shows only a tab-local
   `browserSessionId`, never an access or refresh token.
3. Sign out B. Reload A and refresh its access session; A remains authenticated. A malformed or
   mismatched `X-Browser-Session-Id` must return the standard safe refresh failure without changing
   either account's session.

## Same-browser multi-account implementation validation — 2026-09-09

- The API now issues an HttpOnly refresh cookie and a paired CSRF cookie named with the server
  session UUID. The frontend retains only that non-credential UUID in tab-local `sessionStorage`;
  access tokens remain memory-only and refresh tokens remain HttpOnly.
- Backend controller coverage adds two verified accounts with both cookie pairs in the same browser
  request: each can refresh independently, logging out B leaves A refreshable, an old global cookie
  migrates once, and a mismatched selector is rejected safely.
- No database migration is required: `auth_sessions.session_id` already provides the server-side
  session identity specified by `DATA_short.md`.
- `mvn verify` passed. The PostgreSQL Testcontainers integration classes compiled but were skipped
  because this workstation has no reachable Docker daemon; run the same command with Docker before
  release to execute the database-backed scenarios.
- Frontend `npm run lint`, Jest (19 tests), and `npm run build` passed. The F01 OpenAPI YAML also
  parsed successfully.
