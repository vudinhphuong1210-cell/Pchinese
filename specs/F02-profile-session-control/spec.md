# Feature Specification: Profile and Session Control

**Feature Branch**: `[F02-profile-session-control]`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "F02 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: Khi learner đăng nhập ở thiết bị mới nhưng đã đủ giới hạn thiết bị, họ sẽ gỡ một session cũ bằng cách nào trước khi đăng nhập lại? → A: Cấp luồng quản lý session ngắn hạn sau khi xác thực mật khẩu thành công; learner chọn gỡ một session rồi đăng nhập lại.
- Q: Nếu learner chỉnh profile trên hai thiết bị gần như cùng lúc, hệ thống sẽ xử lý lần lưu sau như thế nào? → A: Nếu profile đã đổi từ lúc màn hình được mở, báo xung đột và yêu cầu tải lại trước khi lưu lại.
- Q: Khi learner chọn gỡ chính session đang dùng, hệ thống sẽ xử lý như thế nào? → A: Hiển thị “thiết bị này”; yêu cầu xác nhận trước khi gỡ; sau đó đăng xuất và đưa về màn hình đăng nhập.
- Q: Luồng quản lý session ngắn hạn khi learner chạm giới hạn thiết bị sẽ hết hiệu lực khi nào? → A: Giữ hiệu lực đến khi learner đóng hoặc tải lại màn hình.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manage personal learning preferences (Priority: P1)

As a signed-in learner, I want to view and update my own profile preferences so that the learning
experience reflects my language, goal, locale, and time zone.

**Why this priority**: Personal settings affect the learner's experience but must remain private.

**Independent Test**: A learner changes permitted preferences, sees the saved values after reload,
and cannot view or alter another learner's profile.

**Acceptance Scenarios**:

1. **Given** an authenticated learner, **When** they submit valid profile preferences, **Then**
   their own profile shows the saved values.
2. **Given** invalid or unsupported preference values, **When** they are submitted, **Then** the
   learner receives a recoverable validation result and the prior values remain unchanged.
3. **Given** the learner's profile was changed after their settings screen loaded, **When** they
   submit the older version, **Then** the change is rejected, no saved preference is overwritten,
   and the learner is asked to reload the current profile before trying again.

---

### User Story 2 - Control active devices (Priority: P1)

As a signed-in learner, I want to see and revoke my own active sessions so that I can remove a
lost or unrecognized device.

**Why this priority**: Self-service session control reduces account-security risk.

**Independent Test**: A learner revokes one of their sessions and that session can no longer access
the account while unrelated learners remain unaffected.

**Acceptance Scenarios**:

1. **Given** multiple active sessions, **When** the learner revokes one selected session, **Then**
   only that session loses access.
2. **Given** the learner chooses to end all sessions, **When** the action is confirmed, **Then** all
   active sessions, including the current one, lose access.
3. **Given** the learner has correctly verified their credentials but has reached the device limit,
   **When** they choose a session to revoke in the restricted session-management flow, **Then** only
   that owned session is revoked and the learner retries sign-in to access the new device.
4. **Given** the learner selects their current session, **When** they confirm its revocation,
   **Then** that session loses access immediately and the learner is returned to sign-in.

---

### Edge Cases

- A stale session displayed in the list is already revoked when selected: the learner sees the
  latest session state without affecting another session.
- A profile update submitted from an older profile version is rejected without overwriting saved
  preferences; the learner is instructed to reload the latest profile.
- A session cannot be revoked by anyone other than its account owner.
- If the learner cancels confirmation to revoke their current session, that session remains active.
- Profile data from a locked or deleted account is never exposed through this feature.
- A restricted session-management flow ends when its screen is closed or reloaded; it then grants
  no account access and reveals no session or account data.
- Reaching the device limit never silently revokes an existing session.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A learner MUST be able to view and update only their own permitted profile settings.
- **FR-002**: The system MUST validate profile values before saving and preserve prior valid values
  when a change fails.
- **FR-003**: A learner MUST be able to list only their own active sessions with privacy-safe device
  information.
- **FR-004**: A learner MUST be able to revoke one owned session or all owned sessions.
- **FR-005**: Session controls MUST not grant access to account, role, entitlement, or private data
  belonging to another learner.
- **FR-006**: This feature MUST not allow an administrator to browse or edit learner profiles or
  self-service sessions merely through the `ADMIN` role.
- **FR-007**: When a learner's credentials have been successfully verified but the device limit
  prevents sign-in, the system MUST issue a restricted, single-purpose session-management flow
  that lists privacy-safe information for only that learner's sessions and permits revoking one
  owned session; it MUST NOT grant normal account access, silently evict a device, or expose another
  account's data.
- **FR-008**: Each profile read MUST provide its current profile version, and a profile update MUST
  identify the version it was based on. If that version is no longer current, the system MUST reject
  the update without overwriting any saved preference and require the learner to reload before a
  new update.
- **FR-009**: The session list MUST identify the learner's current session. Revoking that session
  MUST require explicit confirmation; after confirmation, it MUST lose access immediately and the
  learner MUST be returned to sign-in. Cancelling confirmation MUST leave the session unchanged.
- **FR-010**: The restricted session-management flow MUST remain available only while its screen is
  open and has not been reloaded. Closing or reloading that screen MUST end the flow; a new flow
  requires successful credential verification again.

### Key Entities

- **Learner profile**: private preferences and learning-context settings owned by one learner,
  including a current version used to prevent an older edit from overwriting a newer one.
- **Active session**: an authenticated device/session visible and revocable only by its owner; its
  representation identifies whether it is the learner's current session.
- **Restricted session-management flow**: a credential-verified flow used only when the learner
  reaches the device limit; it can list and revoke the learner's sessions but is not an authenticated
  product session and ends when its screen is closed or reloaded.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of profile reads and updates are restricted to the authenticated learner.
- **SC-002**: A learner can update a valid profile preference in under 1 minute.
- **SC-003**: 100% of revoked sessions lose access on their next protected action.
- **SC-004**: A learner can identify and revoke an active session in under 2 minutes.
- **SC-005**: In a device-limit test, a learner with correctly verified credentials can revoke one
  owned session through the restricted flow and then sign in on the new device without receiving
  ordinary account access from that restricted flow.
- **SC-006**: 100% of profile updates based on an older profile version are rejected without
  overwriting a saved preference.
- **SC-007**: 100% of current-session revocations require confirmation; after confirmation, the
  revoked session cannot complete a protected action and the learner is shown sign-in.
- **SC-008**: In all tests, closing or reloading the restricted session-management screen prevents
  its prior flow from listing or revoking a session; successful credential verification is required
  to start a new flow.

## Assumptions

- F01 provides verified identity, account state, and session lifecycle controls.
- Profile settings do not include role, Premium, quota, score, or another learner's data.
- Successful credential verification at the device limit does not itself create an ordinary session
  or issue ordinary account access; the learner must complete sign-in after revoking a session.
- The restricted session-management flow exists only for the lifetime of its open, unreloaded screen;
  it is not restored after a page reload or reopening the screen.
- The canonical profile row's existing version is the profile version used to detect concurrent
  updates; no separate profile table is introduced for MVP.
- Account deletion and data export are deferred from MVP.
