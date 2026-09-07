# Feature Specification: Identity, Account, and Role Administration

**Feature Branch**: `[F01-identity-account-role-admin]`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "F01 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: Trong tab Account/Roles, Admin sẽ chọn đúng tài khoản cần quản lý bằng cách nào mà không duyệt dữ liệu riêng tư của learner? → A: Nhập chính xác mã tài khoản; chỉ thấy role và trạng thái access.
- Q: Khi Admin khóa hoặc mở khóa account, lý do audit sẽ được ghi theo cách nào? → A: Chọn lý do chuẩn; `OTHER` yêu cầu ghi chú ngắn.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create and secure an account (Priority: P1)

As a learner, I want to register, verify, sign in, recover access, and sign out so that only
I can use my learning account.

**Why this priority**: Every protected learner journey depends on a verified, revocable identity.

**Independent Test**: A learner can complete registration through sign-in and recovery without
accessing another account.

**Acceptance Scenarios**:

1. **Given** a new email address, **When** the learner completes registration and verification,
   **Then** they can sign in and receive an active account.
2. **Given** a lost credential or suspicious session, **When** the learner uses recovery or logout,
   **Then** the affected access is ended without revealing another account's data.

---

### User Story 2 - Administer roles and account access (Priority: P1)

As an authorized administrator, I want to grant or revoke `ADMIN` and lock or unlock another
account selected by its exact account identifier so that permitted administrative access is
controlled without exposing learning data.

**Why this priority**: The MVP Admin dashboard requires controlled account operations from day one.

**Independent Test**: An administrator changes an eligible target's role or access state and the
operation is audited; a non-administrator and prohibited self/final-admin action are rejected.

**Acceptance Scenarios**:

1. **Given** an eligible target, **When** an administrator changes the target's role or account
   access state, **Then** the new state takes effect and is auditable.
2. **Given** an administrator attempts a self-change or to disable the final active administrator,
   **When** the action is submitted, **Then** it is rejected without changing any state.

---

### Edge Cases

- Duplicate registration or a reused/expired verification or recovery request does not disclose
  whether another account exists.
- An invalid, unavailable, or unmanageable account identifier returns a safe result without exposing
  profile, learning, or account-existence data.
- An administrator selects `OTHER` without a short safe note: the lock/unlock action is rejected
  without changing the target's account or session state.
- A locked account cannot use an existing session or create a new one; unlock requires a new sign-in.
- Role or account-status changes end affected access and preserve learner-private data.

## Feature Boundaries and Governance

### Actors and entry points

- **Learner**: starts registration, verifies an account, signs in, requests credential recovery, or
  signs out through the account-access experience.
- **Authorized administrator**: enters the Account/Roles tab with an exact account identifier to
  inspect the permitted minimal projection or request a role/access-state change for another
  account.
- **Unauthenticated, locked, or unauthorized actor**: receives a safe failure result and cannot
  obtain target-account information or perform a protected action.

### Access, entitlement, and state rules

- Public account-start and recovery actions use safe, neutral results. Protected actions use the
  current account, role, and session state controlled by the system.
- `ADMIN` is an account-administration role only. This feature neither grants nor changes Premium
  entitlement or AI quota, and those values never authorize an account or role administration
  action.
- A newly registered account moves from pending verification to active only after successful
  verification. An administrator may lock an eligible active account; a later authorized unlock
  restores only its account access state, never an old session. Role grants and revocations change
  only the target's `ADMIN` role and revoke affected access as required by this specification.

### Failure cases and API impact

- Duplicate, invalid, expired, unavailable, unmanageable, self-targeted, and final-active-
  administrator requests follow the safe outcomes in the acceptance scenarios and edge cases;
  they do not disclose account existence or learner-private data, and they do not partially change
  account, role, or session state.
- This feature adds account-lifecycle and sign-out operations for learners, plus protected
  exact-identifier Account/Roles operations for administrators. Their responses expose only the
  information permitted by this specification and do not change the learning, entitlement, quota,
  or learner-data administration interfaces.

### Acceptance and test basis

- The independent tests and acceptance scenarios for User Stories 1 and 2, together with the edge
  cases above, are the acceptance test basis. They cover neutral account recovery, current-state
  authorization, exact-ID-only administration, audit
  evidence, safe validation failures, and self/final-administrator rejection.

### Non-goals

- Account browsing; search by learner name, email, profile, or learning record.
- Viewing or changing a learner's attempts, recordings, chats, vocabulary, progress, Premium
  entitlement, or AI quota.
- Public creation of an `ADMIN` account, profile preferences, self-service device/session management, or any
  account lifecycle not stated in this feature.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Learners MUST be able to register, verify their account, sign in, recover access,
  and sign out of the current account session.
- **FR-002**: The system MUST use the current server-controlled identity and account state for every
  protected operation; client-provided role or account-state claims are not authoritative.
- **FR-003**: Only an authorized administrator MAY grant or revoke `ADMIN` for another account.
- **FR-004**: Only an authorized administrator MAY lock or unlock another account; locking MUST end
  the target's active access and require an auditable reason.
- **FR-005**: The system MUST reject self-role/self-lock changes and any action that removes the
  final active administrator.
- **FR-006**: Account and role administration MUST NOT expose or change a learner's attempts,
  recordings, chats, vocabulary, progress, Premium entitlement, or AI quota.
- **FR-007**: The Account/Roles tab MUST require an exact account identifier and show only the
  target's permitted role and lock/unlock state; it MUST NOT offer account browsing, profile views,
  or learner-data search.
- **FR-008**: Lock and unlock actions MUST record one standard reason (`SECURITY`, `POLICY`,
  `USER_REQUEST`, or `OTHER`); `OTHER` MUST include a short safe note and no reason may contain
  learner-private content.

### Key Entities

- **Account**: a verified learner identity with a lifecycle state and current authorization version.
- **Role grant**: an auditable record that gives or revokes the `ADMIN` role for a target account.
- **Active session**: a revocable authenticated device/session belonging only to its account owner.
- **Security audit event**: immutable evidence of an account or role-sensitive action.
- **Account access reason**: a standard non-sensitive reason, with a short safe note only for
  `OTHER`, attached to a lock or unlock audit event.
- **Account management projection**: the minimal role and access-state result returned after an
  exact authorized account identifier is supplied.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of protected account actions reject an unauthenticated, locked, or unauthorized
  actor without exposing target data.
- **SC-002**: 100% of role, lock, and unlock actions produce an auditable actor, target, and
  outcome record.
- **SC-003**: A learner can complete account registration, verification, and first sign-in in under
  5 minutes under normal conditions.
- **SC-004**: No administrator action in this feature reveals learner-private learning data.
- **SC-005**: 100% of Account/Roles lookups require an exact account identifier and return no
  profile or learner-data fields beyond the permitted role/access-state result.
- **SC-006**: 100% of lock/unlock audit events contain a valid standard reason; every `OTHER` event
  contains a short safe note.

## Assumptions

- F00 is complete and this feature is the first learner/account behaviour in MVP.
- The MVP Admin dashboard Account/Roles tab is limited to the role and account-status operations
  defined here.
- Administrators receive an exact account identifier through a separate approved administrative
  process; the dashboard does not provide account browsing or learner lookup.
- Audit reasons use only the four standard categories above; any `OTHER` note is brief and excludes
  credentials, private learning data, and unnecessary personal information.
- Premium and AI allowance are owned by F03 and are excluded from this feature.
