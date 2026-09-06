# Feature Specification: Content Administration

**Feature Branch**: `[F04-content-administration]`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "F04 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: Một lesson cần đạt điều kiện nào trước khi Admin có thể publish để learner nhìn thấy? → A: Parent topic đã published, lesson có ít nhất một segment, mọi segment đã published và media của chúng đã approved. Topic có thể được publish trước nhưng không hiện trong catalog khi chưa có lesson hợp lệ.
- Q: Khi hai Admin cùng sửa một content item, hệ thống nên xử lý lượt lưu thứ hai như thế nào? → A: Từ chối lượt lưu dùng bản cũ; hiển thị trạng thái recoverable để Admin tải lại và xử lý khác biệt.
- Q: Khi Admin cần thay đổi nội dung learner đang thấy trong một lesson đã published, quy trình MVP nên thế nào? → A: Admin unpublish lesson, chỉnh sửa/kiểm tra điều kiện, rồi publish lại.
- Q: Nếu một media asset đã được dùng trong lesson published sau đó bị `REJECTED` hoặc `QUARANTINED`, hệ thống nên xử lý các lesson phụ thuộc thế nào? → A: Tự unpublish mọi lesson phụ thuộc; ẩn khỏi learner đến khi thay media approved và republish.
- Q: Sau khi Admin archive một topic, lesson hoặc segment, item đó có được khôi phục để publish lại trong MVP không? → A: Archive là cuối; không restore hoặc republish item cũ.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Prepare learning content (Priority: P1)

As an authorized administrator, I want to create and organize topics, lessons, segments, and
approved media so that learners receive structured Chinese-learning content.

**Why this priority**: Catalog and lesson features require controlled content before learners can
study.

**Independent Test**: An authorized administrator creates a complete draft lesson with ordered
segments and approved media; a non-administrator cannot make the same change.

**Acceptance Scenarios**:

1. **Given** an authorized administrator, **When** they create or update a draft content item,
   **Then** the item remains unavailable to visitors and learners until published.
2. **Given** an unauthorized actor, **When** they attempt a content change, **Then** no content
   state changes.
3. **Given** a content item was changed after an administrator loaded it, **When** that
   administrator saves the stale version, **Then** no content state changes and they receive a
   recoverable reload result.

---

### User Story 2 - Publish and retire safe content (Priority: P1)

As an authorized administrator, I want to publish, unpublish, or archive content through explicit
states so that only appropriate Free learning material is visible.

**Why this priority**: Publishing state protects learners from incomplete or unapproved material.

**Independent Test**: A published Free lesson becomes discoverable; an unpublished or archived
lesson is no longer newly accessible while historical learner work remains protected.

**Acceptance Scenarios**:

1. **Given** a draft lesson whose parent topic is published and whose one or more segments are all
   published with approved media, **When** the administrator publishes it, **Then** it becomes
   visible as Free content. A published topic without a publishable lesson remains hidden from the
   learner catalog.
2. **Given** published content, **When** the administrator unpublishes or archives it, **Then**
   new learner access follows the changed state without exposing private learner work.
3. **Given** a published lesson, **When** the administrator needs to change learner-visible lesson,
   segment, ordering, or media content, **Then** they unpublish it first; it is hidden while edited
   and can become visible again only after a valid republish.
4. **Given** an approved media asset used by a published lesson becomes rejected or quarantined,
   **When** its safety state changes, **Then** every dependent published lesson is automatically
   unpublished and hidden from learners until validly republished.

---

### Edge Cases

- Media cannot become learner-visible merely because it was uploaded or approved.
- Invalid parent/child ordering or unapproved media is rejected without changing publication state.
- A lesson lacking a published parent topic, at least one published segment, or approved media for
  every segment cannot be published or exposed to learners.
- A stale Admin edit is rejected without overwriting the latest content state.
- A learner-visible change to a published lesson, its segment ordering, or its referenced media is
  rejected until the lesson is first unpublished.
- When an associated media asset becomes `REJECTED` or `QUARANTINED`, every dependent published
  lesson is automatically unpublished; learners cannot access it until approved replacement media
  is validated and the lesson is republished.
- An archived topic, lesson, or segment remains unavailable and cannot be restored or republished;
  a replacement must be created without altering historical learner work.
- Content administration cannot browse learner chats, recordings, attempts, vocabulary, or progress.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Only an authorized administrator MAY access the Content/Media tab of the MVP Admin
  dashboard and create or update learning content.
- **FR-002**: Administrators MUST be able to organize topics, lessons, segments, and approved media
  in a valid parent/child order.
- **FR-003**: Content MUST follow explicit draft, published, unpublished, and archived states. A
  learner-visible change to a `PUBLISHED` lesson, its segments, their ordering, or referenced media
  MUST first transition the lesson to `UNPUBLISHED`; it MAY return to `PUBLISHED` only after all
  publication conditions are checked again. `ARCHIVED` is terminal for topics, lessons, and
  segments: an archived item MUST NOT transition to another publication state or be republished.
- **FR-004**: A lesson MAY become learner-visible only when its parent topic is `PUBLISHED`, it has
  at least one segment, every segment is `PUBLISHED`, and every referenced media asset is
  `APPROVED`. A `PUBLISHED` topic without such a lesson MUST remain absent from the learner catalog.
- **FR-005**: MVP content MUST be published as Free; Premium labelling is deferred with automated
  billing.
- **FR-006**: Content operations MUST NOT grant learner entitlement, change AI allowance, or expose
  learner-private data.
- **FR-007**: Each content create, update, publication, approval, unpublish, or archive action MUST
  use the current content version. A stale version MUST be rejected without changing content state
  and return a recoverable reload result to the authorized administrator.
- **FR-008**: When an `APPROVED` media asset used by one or more `PUBLISHED` lessons becomes
  `REJECTED` or `QUARANTINED`, the system MUST automatically transition every dependent lesson to
  `UNPUBLISHED` before it can be learner-visible again. An administrator MUST use approved
  replacement media and republish the lesson under the normal publication checks.

### Key Entities

- **Topic**: a published or draft grouping of lessons.
- **Lesson and segment**: ordered learning content and its study units; every segment must be
  published and use approved media before its lesson is learner-visible.
- **Archived content**: a terminal retired topic, lesson, or segment retained only to preserve
  historical references; it cannot return to the learner catalog or lesson player.
- **Media asset**: approved learning media associated with permitted content; a later rejection or
  quarantine makes every dependent published lesson unavailable.

## Success Criteria *(mandatory)*

- **SC-001**: 100% of content changes from non-administrators are rejected without state change.
- **SC-002**: A valid Free lesson can move from draft to learner-visible published state in under
  5 minutes of administrator work.
- **SC-003**: 100% of learner-visible content has a published parent, valid ordering, and approved
  associated media.
- **SC-005**: 100% of learner-visible lessons have one or more published segments, and no empty
  published topic is shown in the learner catalog.
- **SC-006**: 100% of stale content save attempts leave the latest content state unchanged and
  return a recoverable reload result.
- **SC-007**: 100% of learner-visible changes to a published lesson are hidden from learners until
  the lesson is validly republished.
- **SC-008**: 100% of published lessons dependent on newly rejected or quarantined media become
  unavailable to learners before any further playback is served.
- **SC-009**: 100% of archived topics, lessons, and segments remain unavailable to learners and
  cannot be restored or republished.
- **SC-004**: No content-management view exposes learner-private learning data.

## Assumptions

- F01 provides server-controlled `ADMIN` authorization and the MVP Admin dashboard shell.
- F03 supplies only Free-plan enforcement in MVP.
- Catalog, playback, and learner progress are owned by later features.
