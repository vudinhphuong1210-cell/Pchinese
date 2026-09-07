# Feature Specification: Profile Preferences

**Feature Branch**: `[F02-profile-preferences]`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "F02 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: Nếu learner chỉnh profile trên hai thiết bị gần như cùng lúc, hệ thống sẽ xử lý lần lưu sau như thế nào? → A: Nếu profile đã đổi từ lúc màn hình được mở, báo xung đột và yêu cầu tải lại trước khi lưu lại.

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

### Edge Cases

- A profile update submitted from an older profile version is rejected without overwriting saved
  preferences; the learner is instructed to reload the latest profile.
- Profile data from a locked or deleted account is never exposed through this feature.

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

### Key Entities

- **Learner profile**: private preferences and learning-context settings owned by one learner,
  including a current version used to prevent an older edit from overwriting a newer one.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of profile reads and updates are restricted to the authenticated learner.
- **SC-002**: A learner can update a valid profile preference in under 1 minute.
- **SC-003**: 100% of profile updates based on an older profile version are rejected without
  overwriting a saved preference.

## Assumptions

- F01 provides verified identity and account state.
- Profile settings do not include role, Premium, quota, score, or another learner's data.
- The canonical profile row's existing version is the profile version used to detect concurrent
  updates; no separate profile table is introduced for MVP.
- Account deletion and data export are deferred from MVP.
