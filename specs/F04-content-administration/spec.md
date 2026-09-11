# Feature Specification: Content Administration

**Feature Branch**: `[F04-content-administration]`

**Created**: 2026-09-05

**Last Updated**: 2026-09-11

**Status**: Draft

**Input**: User description: "F04 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: Một lesson cần đạt điều kiện nào trước khi Admin có thể publish để learner nhìn thấy? → A: Parent topic đã published, lesson có ít nhất một segment, mọi segment đã published và media của chúng đã approved. Topic có thể được publish trước nhưng không hiện trong catalog khi chưa có lesson hợp lệ.
- Q: Khi hai Admin cùng sửa một content item, hệ thống nên xử lý lượt lưu thứ hai như thế nào? → A: Từ chối lượt lưu dùng bản cũ; hiển thị trạng thái recoverable để Admin tải lại và xử lý khác biệt.
- Q: Khi Admin cần thay đổi nội dung learner đang thấy trong một lesson đã published, quy trình MVP nên thế nào? → A: Admin unpublish lesson, chỉnh sửa/kiểm tra điều kiện, rồi publish lại.
- Q: Nếu một media asset đã được dùng trong lesson published sau đó bị `REJECTED` hoặc `QUARANTINED`, hệ thống nên xử lý các lesson phụ thuộc thế nào? → A: Tự unpublish mọi lesson phụ thuộc; ẩn khỏi learner đến khi thay media approved và republish.
- Q: Sau khi Admin archive một topic, lesson hoặc segment, item đó có được khôi phục để publish lại trong MVP không? → A: Archive là cuối; không restore hoặc republish item cũ.

### Session 2026-09-11

- Q: After media is `REJECTED` or `QUARANTINED`, may an administrator approve the same asset again? → A: No. A distinct approved replacement is required before the lesson may be republished.
- Q: May an uploaded media file or provider reference be approved immediately? → A: No. Only permitted sources or valid files may enter safety review, and the required safety checks complete before approval.
- Q: May an administrator make a learner-visible segment, ordering, or media change while its lesson is published? → A: No. The request is rejected until the lesson is unpublished first; only rejected/quarantined media automatically withdraws dependent lessons.
- Q: How does an administrator add the MVP learning video when it is hosted on YouTube? → A: They paste a supported YouTube watch, share, or embed link (or its video ID). The system validates and retains the canonical video ID; it does not require a local file upload or download the video from YouTube. Learner playback uses the provider's permitted embedded player in the later lesson-player feature.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Prepare learning content (Priority: P1)

As an authorized administrator, I want to create and organize topics, lessons, segments, and
approved media so that learners receive structured Chinese-learning content.

**Why this priority**: Catalog and lesson features require controlled content before learners can
study.

**Independent Test**: An authorized administrator creates a complete draft lesson with ordered
segments and safety-checked media; a non-administrator cannot make the same change.

**Acceptance Scenarios**:

1. **Given** an authorized administrator, **When** they create or update a draft content item,
   **Then** the item remains unavailable to visitors and learners until published.
2. **Given** an unauthorized actor, **When** they attempt a content change, **Then** no content
   state changes.
3. **Given** a content item was changed after an administrator loaded it, **When** that
   administrator saves the stale version, **Then** no content state changes and they receive a
   recoverable reload result.
4. **Given** an administrator submits a supported YouTube video link or video ID, **When** it is
   malformed, unsupported, or fails required safety checks, **Then** it cannot be approved or become
   learner-visible.
5. **Given** an administrator pastes a valid supported YouTube link, **When** the media record is
   created, **Then** the record enters review without the administrator uploading or downloading a
   local video file.

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
5. **Given** a media asset is rejected or quarantined, **When** an administrator tries to approve
   that asset again, **Then** the request is rejected without state change and a replacement asset
   is required.
6. **Given** a published lesson, **When** an administrator tries to add, reorder, unpublish,
   archive, or alter a segment or its referenced media, **Then** the request is rejected without
   state change until the lesson is first unpublished.

---

### User Story 3 - Administer content safely and accessibly (Priority: P1)

As an authorized administrator, I want content administration to preserve a trustworthy audit trail
and clear, accessible feedback so that I can manage learning content safely and recover from errors.

**Why this priority**: Content changes are safety-sensitive and must remain traceable, secure, and
operable by every authorized administrator.

**Independent Test**: An authorized administrator performs a content action, uses the management
controls with a keyboard, and verifies that the action is traceable while no reusable credential is
left in browser-readable persistence.

**Acceptance Scenarios**:

1. **Given** an administrator performs a content or media action, **When** it succeeds or fails,
   **Then** the system retains an immutable, safe audit record that identifies the actor, action,
   affected content, outcome, version, correlation, and safe state change.
2. **Given** an administrator uses keyboard or assistive technology, **When** they navigate content
   management, conflict recovery, and destructive confirmations, **Then** each control has an
   accessible name, visible focus, and status feedback that does not depend on colour alone.
3. **Given** an administrator's authenticated session ends or the page is reloaded, **When** a
   browser context is inspected, **Then** no content-management access credential remains in
   browser-readable persistent storage.
4. **Given** an administrator reads or mutates content through a documented management interface,
   **When** the operation completes, **Then** the result contains only safe, current content
   information in the documented standard response shape.

---

### Edge Cases

- Media cannot become learner-visible merely because its YouTube reference was registered or approved.
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
- A rejected or quarantined media asset cannot return to an approved state; a distinct approved
  replacement is required.
- A malformed, unsupported, or unapproved YouTube video reference stays unavailable for approval and
  learner access.
- A YouTube channel, playlist, short, or arbitrary web link cannot be substituted for one supported
  video reference.
- An audit record cannot be edited or deleted after it is retained, and a newly provisioned
  environment retains the same required audit history without manual schema work.

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
- **FR-009**: A media asset MAY move from pending review to approved, rejected, or quarantined. An
  approved asset MAY become rejected or quarantined. A rejected or quarantined asset MUST NOT be
  approved again; a distinct approved replacement is required.
- **FR-010**: For MVP video media, the system MUST accept only a supported YouTube video link or
  video ID. It MUST validate and retain the canonical video identity, without requiring a local
  file upload or downloading the video, and MUST complete the required safety checks before an
  administrator can approve it. An administrator MUST NOT make an arbitrary public media location,
  playlist, channel, or unsupported YouTube location learner-visible.
- **FR-011**: Each content and media action, including a rejected action, MUST retain an immutable,
  safe audit record with actor, action, affected content, outcome, current version, correlation, and
  safe before/after state where state changes.
- **FR-012**: Content-management reads and mutations MUST return only safe, current content
  information in the documented standard response shape; list results MUST be bounded so that the
  management experience remains usable and does not expose persistence internals or learner data.
- **FR-013**: Content-management access credentials MUST NOT remain in browser-readable persistent
  storage after use; an administrator must obtain access through the authorized session.
- **FR-014**: The Content/Media administration experience MUST support keyboard navigation, visible
  focus, accessible names, responsive touch targets, sufficient contrast, and status feedback that
  does not rely on colour alone.
- **FR-015**: The required audit history MUST be part of the approved canonical data definition and
  be available in a newly provisioned environment without manual schema changes.
- **FR-016**: An approved YouTube video reference MUST be usable by the learner-facing lesson player
  through the provider's permitted embedded playback, without exposing a raw storage location or
  requiring the application server to relay the video bytes.

### Key Entities

- **Topic**: a published or draft grouping of lessons.
- **Lesson and segment**: ordered learning content and its study units; every segment must be
  published and use approved media before its lesson is learner-visible.
- **Archived content**: a terminal retired topic, lesson, or segment retained only to preserve
  historical references; it cannot return to the learner catalog or lesson player.
- **Media asset**: a safety-reviewed reference to one supported YouTube learning video, retained as
  its canonical video identity; a later rejection or quarantine makes every dependent published
  lesson unavailable.
- **Content audit event**: an immutable safe record of a content or media action, its actor,
  affected item, outcome, correlation, version, and safe state change.

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
- **SC-010**: 100% of media submissions that fail source or safety checks remain unavailable for
  approval and learner access.
- **SC-011**: 100% of content and media actions have one immutable, safe audit record with the
  required action, actor, outcome, version, correlation, and applicable state-change information.
- **SC-012**: 100% of rejected or quarantined media-approval attempts leave the media state
  unchanged and require a distinct approved replacement before lesson republishing.
- **SC-013**: 100% of Content/Media management controls are usable by keyboard and expose focus,
  accessible names, and non-colour-only status feedback.
- **SC-014**: A newly provisioned environment provides the required content audit history without
  manual schema setup.
- **SC-015**: 100% of valid supported YouTube video references can enter safety review without a
  local video file being uploaded or downloaded.

## API & Integration Impact

- Content-management interfaces continue to be limited to authorized administrators and use the
  documented standard response shape for successful and failed operations.
- Create, update, lifecycle, and media-submission operations return safe current content information;
  content lists return bounded safe management information.
- Media submission accepts a validated supported YouTube video link or video ID. Media safety review
  and approval remain separate from learner visibility; the learner player later uses the approved
  provider embedding rather than a local video copy.

## Assumptions

- F01 provides server-controlled `ADMIN` authorization and the MVP Admin dashboard shell.
- F03 supplies only Free-plan enforcement in MVP.
- Catalog, playback, and learner progress are owned by later features.
- The MVP supported-provider scope is YouTube video only. Support for locally uploaded video or
  additional video providers is deferred to a separately specified feature.
- The approved canonical data definition is the single source for a newly provisioned environment;
  the feature's audit record is added to that definition before implementation resumes.
