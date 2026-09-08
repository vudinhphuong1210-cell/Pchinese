# Feature Specification: Profile Preferences

**Feature Branch**: `[F02-profile-preferences]`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "F02 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: Nếu learner chỉnh profile trên hai thiết bị gần như cùng lúc, hệ thống sẽ xử lý lần lưu sau như thế nào? → A: Nếu profile đã đổi từ lúc màn hình được mở, báo xung đột và yêu cầu tải lại trước khi lưu lại.

## User Scenarios & Testing *(mandatory)*

### Session 2026-09-09

- Nhật ký hoạt động chỉ hiển thị cho người dùng đã đăng nhập các sự kiện an toàn của chính họ
  (tài khoản/bảo mật hiện có và cập nhật hồ sơ). Nhật ký chỉ có loại sự kiện và thời điểm; không
  có giá trị hồ sơ, token, mật khẩu, session, correlation ID, chi tiết nội bộ hay danh tính người
  khác. ADMIN không được xem nhật ký của learner khác chỉ vì có role.

### Scope correction 2026-09-09

This correction supersedes the earlier personal-history clarification: learners have no activity
history in F02. System activity history is an ADMIN-only operational capability; it returns only
safe event type, time and permitted account-name labels, never learner-profile values or private
audit metadata.

### Audit-information organization 2026-09-08

Every system activity event is organized into five concerns:

1. **What happened**: an approved category and event type, such as authentication, account
   lifecycle, access control, profile change, or security operation.
2. **Who and what were involved**: the acting account and affected account when applicable; a
   system-originated event uses a neutral system label.
3. **Result**: a bounded outcome code and, where relevant, a bounded reason code rather than an
   unstructured explanation.
4. **Safe change summary**: only approved field-name codes for a profile update, never prior or
   new values.
5. **Operational trace**: occurrence time plus private request/session correlation used only for
   incident investigation; it is not part of the ADMIN screen.

The ADMIN screen is a minimized operational view: event type, occurrence time, and permitted
actor/target account-name labels. It does not become a viewer for private audit metadata.

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

### User Story 2 - Review system activity history (Priority: P2)

As an ADMIN, I want to view a paginated history of safe account and system activity across the
application so that I can supervise security and account operations without viewing learner-private data.

**Why this priority**: ADMIN needs a system-wide operational trail; learners do not need an activity-history
screen and must not be able to browse audit records.

**Independent Test**: A profile update creates one event visible in the ADMIN system history. A learner
receives a forbidden result and has no navigation entry for activity history.

**Acceptance Scenarios**:

1. **Given** an authenticated ADMIN with audit activity, **When** they open System Activity History,
   **Then** they receive all newest events first with a safe event type, occurrence time, and permitted
   actor/target account names.
2. **Given** a successful profile preference change, **When** an ADMIN reloads System Activity History,
   **Then** exactly one new profile-update event appears without any previous or new preference value.
3. **Given** an ADMIN viewing a later history page or an empty system history, **When** they load it,
   **Then** the system returns a bounded, paginated safe result.
4. **Given** a learner credential, **When** it requests system activity or uses the UI, **Then** it
   receives a forbidden result and has no activity-history navigation entry.
5. **Given** an account or security operation creates an activity event, **When** an ADMIN reviews
   it, **Then** the event is consistently categorized and includes only the approved operational
   summary; incident trace data remains private.

---

### Edge Cases

- A profile update submitted from an older profile version is rejected without overwriting saved
  preferences; the learner is instructed to reload the latest profile.
- Profile data from a locked or deleted account is never exposed through this feature.
- A failed, stale, invalid, or no-op profile update creates no profile-update activity event.
- Empty system activity history is a normal state; an invalid page or size is rejected safely.
- An event whose actor or target account is unavailable is represented by a neutral safe label rather
  than a UUID or an account-existence error.
- An operation that cannot produce an approved event type, outcome code, reason code, or safe change
  summary is not recorded with a raw fallback message.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A learner MUST be able to view and update only their own permitted profile settings.
- **FR-002**: The system MUST validate profile values before saving and preserve prior valid values
  when a change fails.
- **FR-003**: Each profile read MUST provide its current profile version, and a profile update MUST
  identify the version it was based on. If that version is no longer current, the system MUST reject
  the update without overwriting any saved preference and require the learner to reload before a
  new update.
- **FR-004**: This feature MUST not allow an administrator to browse or edit learner profiles merely
  through the `ADMIN` role.
- **FR-005**: A successful, state-changing profile preference update MUST append one immutable
  activity event in the same business operation. It may identify only which permitted field names
  changed; it MUST NOT record previous/new profile values, credentials, tokens, or private free text.
- **FR-006**: Only a server-authorized `ADMIN` MUST be able to retrieve the newest-first,
  server-paginated system activity history. Learners MUST have no activity-history endpoint or UI.
- **FR-007**: Every system activity item returned to ADMIN MUST contain only a safe public event type,
  occurrence time, and permitted actor/target account names. It MUST exclude UUIDs, sessions,
  correlation IDs, roles, audit details, credentials, and profile values.
- **FR-008**: The activity history MUST use the existing append-only account audit store; F02 MUST
  not create a duplicate activity table or alter the canonical schema.
- **FR-009**: Each audit event MUST be organized using an approved event category/type, applicable
  actor and target, occurrence time, a bounded outcome code, and only approved reason or changed-field
  codes. Request/session correlation is retained only as private incident-trace context.
- **FR-010**: Audit events MUST NOT contain passwords, access or refresh tokens, email addresses,
  IP addresses, raw user-agent data, prior or new profile values, or unrestricted free-text notes.

### Key Entities

- **Learner profile**: private preferences and learning-context settings owned by one learner,
  including a current version used to prevent an older edit from overwriting a newer one.
- **System activity event**: immutable, minimized account or profile event visible only to ADMIN and
  represented by a safe type, occurrence time, and permitted account-name labels.
- **Private incident trace**: non-display operational context that connects an approved event to its
  request/session investigation without becoming learner or ADMIN profile data.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of profile reads and updates are restricted to the authenticated learner.
- **SC-002**: A learner can update a valid profile preference in under 1 minute.
- **SC-003**: 100% of profile updates based on an older profile version are rejected without
  overwriting a saved preference.
- **SC-004**: 100% of system-history requests from a learner are rejected and the learner navigation
  exposes no activity-history entry.
- **SC-005**: 100% of returned ADMIN activity items exclude protected audit metadata, UUIDs, and
  profile values.
- **SC-006**: Each successful state-changing profile update results in exactly one visible
  profile-update item after ADMIN reloads the first system-history page.
- **SC-007**: 100% of newly recorded audit events use the approved event organization and contain no
  prohibited raw sensitive or free-text fields.

## Assumptions

- F01 provides verified identity and account state.
- Profile settings do not include role, Premium, quota, score, or another learner's data.
- The canonical profile row's existing version is the profile version used to detect concurrent
  updates; no separate profile table is introduced for MVP.
- The existing `auth_audit_events` table is the canonical append-only store for F02 activity; its
  retention policy remains governed by the project privacy policy and is not changed by F02.
- F02 displays existing safe account/security events alongside profile-update events only to ADMIN.
  Future learning features may append their own explicitly approved safe activity events; F02 does not
  infer them.
- The approved event categories, outcome codes, reason codes and changed-field codes are centrally
  governed. A separate, access-controlled incident process handles any narrative investigation note.
- Account deletion and data export are deferred from MVP.
