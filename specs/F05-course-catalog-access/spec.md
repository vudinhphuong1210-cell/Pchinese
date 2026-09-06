# Feature Specification: Course Catalog and Access

**Feature Branch**: `[F05-course-catalog-access]`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "F05 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: Visitor chưa đăng nhập được đi đến mức nào với một lesson Free trong MVP? → A: Visitor xem catalog và lesson summary; đăng nhập mới vào Lesson Player/playback/practice.
- Q: Nếu lesson, segment hoặc media đang được learner dùng chuyển sang unavailable, learner nên nhận kết quả nào? → A: Chặn khi learner truy cập lesson/segment/media kế tiếp; hiển thị unavailable và quay về catalog, giữ progress cũ.
- Q: Catalog MVP nên cung cấp cách tìm nội dung nào cho visitor và learner? → A: Search title topic/lesson, filter HSK 1–6; mặc định theo `sort_order` do Admin quản lý.
- Q: Một catalog summary của topic hoặc lesson nên hiển thị những thông tin nào trong MVP? → A: Title, summary, HSK, duration và số item published; không transcript, media URL hay practice content.
- Q: Nội dung published chưa có HSK level nên xuất hiện thế nào khi learner dùng HSK filter? → A: Hiện trong catalog All; bị loại khi filter HSK 1–6.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Discover published learning content (Priority: P1)

As a visitor, I want to browse published Chinese-learning topics and lessons so that I can choose
what to study before or after signing in.

**Why this priority**: Catalog discovery is the entry point for the learning experience.

**Independent Test**: A visitor can find only published Free topics and lessons and cannot discover
draft, unpublished, archived, or private content.

**Acceptance Scenarios**:

1. **Given** published Free content exists, **When** a visitor browses or filters the catalog,
   **Then** they see accurate topic and lesson summaries.
2. **Given** content is not published, **When** a visitor attempts to access it, **Then** it is not
   presented as available content.
3. **Given** a visitor selects a published Free lesson, **When** they view its catalog detail,
   **Then** they see only its permitted summary and a sign-in path, not Lesson Player, playback, or
   practice content.
4. **Given** a visitor or learner supplies a title keyword or HSK level, **When** they search or
   filter the catalog, **Then** only matching published Free topic and lesson summaries are shown
   in the administrator-defined order.
5. **Given** a visitor or learner views a catalog item, **When** the item is returned, **Then** its
   title, short summary, HSK level, estimated duration where applicable, and current published-item
   count are shown without transcript, media URL, or practice content.
6. **Given** a published catalog item has no HSK level, **When** a visitor or learner selects a
   specific HSK filter, **Then** that item is absent; it remains visible in the unfiltered catalog.

---

### User Story 2 - Enter a permitted lesson (Priority: P1)

As a learner, I want to open a published Free lesson from the catalog so that I can continue into
the lesson experience.

**Why this priority**: The catalog must hand off a clear, permitted learning context to F06.

**Independent Test**: Selecting a published Free lesson opens its permitted summary; invalid or
retired content does not expose playback or practice material.

**Acceptance Scenarios**:

1. **Given** a published Free lesson, **When** a learner selects it, **Then** they can enter its
   permitted learning context.
2. **Given** a lesson whose state changes while displayed, **When** it is selected, **Then** the
   latest access state is applied and stale content is not used.
3. **Given** a learner is using a lesson whose lesson, segment, or media later becomes unavailable,
   **When** they request the next lesson, segment, or media resource, **Then** access is blocked,
   a recoverable unavailable result provides a return to catalog, and existing progress is kept.

---

### Edge Cases

- Empty filters show a useful empty state without exposing hidden content.
- A copied or stale lesson reference does not bypass publication state.
- MVP does not show Premium locks, paid upgrade prompts, or purchasable content.
- A visitor cannot use a catalog or copied lesson reference to enter Lesson Player, playback, or
  practice without signing in.
- An unpublished, archived, or media-blocked resource is denied at the learner's next resource
  access; its historical progress remains intact.
- A title search or HSK filter with no current published Free match returns the normal useful empty
  state and never widens results to hidden content.
- Catalog summaries never expose transcript, media URL, provider identifier, playback playlist, or
  practice material.
- A catalog item with no HSK level is shown only when no specific HSK filter is active.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Visitors and learners MUST be able to browse published Free topic and lesson
  summaries. Each summary MUST expose only title, short summary, HSK level, estimated duration
  where applicable, and its current published lesson or segment count. A visitor MAY view a
  published Free lesson's permitted summary only.
- **FR-002**: The catalog MUST filter by the current publication and access state before showing
  content. It MUST support title-keyword search and HSK-level filtering for published Free topic
  and lesson summaries, with default ordering by the relevant administrator-managed `sort_order`.
  An item with no HSK level MUST appear only in the unfiltered catalog, never in a specific HSK 1–6
  result.
- **FR-003**: Learners MUST be able to open only the currently permitted content from the catalog.
- **FR-007**: Only a signed-in learner MAY enter Lesson Player, playback, or practice from a catalog
  lesson. Visitor catalog detail MUST provide a sign-in path and MUST NOT expose those resources.
- **FR-004**: Catalog views MUST handle empty, unavailable, and changed-content states clearly.
  On the learner's next lesson, segment, or media access, the system MUST apply current availability
  and provide an unavailable result with a return to catalog without deleting existing progress.
- **FR-005**: The MVP MUST not display Premium lock or upgrade journeys in catalog access.
- **FR-006**: Catalog access MUST not disclose private learner progress, attempts, chats, or media
  provider credentials. It MUST NOT disclose transcripts, media URLs, playback playlists, or
  practice content.

### Key Entities

- **Catalog item**: a visible summary of one permitted topic or lesson containing title, short
  summary, HSK level, applicable estimated duration, and current published-item count only.
- **Visitor lesson detail**: the published Free lesson summary visible before sign-in, excluding
  Lesson Player, playback, and practice content.
- **Publication state**: the current visibility state governing whether a content item can appear.
- **Access state**: the currently permitted Free access decision for a lesson.
- **Catalog criteria**: an optional title keyword and HSK level 1–6 applied only to visible topic
  and lesson summaries; it never searches transcripts, playback, or practice content.
- **Unclassified catalog item**: a published summary whose HSK level is absent; it is eligible only
  for the unfiltered catalog.

## Success Criteria *(mandatory)*

- **SC-001**: 100% of catalog items shown to visitors are currently published Free content.
- **SC-002**: A visitor can locate and open a published lesson in under 2 minutes.
- **SC-003**: Changed or unavailable content never yields a playable or practiceable stale view.
- **SC-004**: Catalog empty and unavailable states provide a recoverable next action.
- **SC-005**: 100% of visitor lesson-detail access exposes only the permitted summary and a sign-in
  path; no visitor request opens Lesson Player, playback, or practice.
- **SC-006**: 100% of next-resource requests for content made unavailable after catalog display are
  denied with a recoverable catalog return, while existing progress remains unchanged.
- **SC-007**: 100% of title-keyword and HSK-filter results contain only matching current published
  Free summaries in administrator-defined order.
- **SC-008**: 100% of catalog summaries expose only the permitted summary fields and never expose
  transcript, media URL, provider identifier, playlist, or practice content.
- **SC-009**: 100% of specific HSK 1–6 results exclude items with no HSK level, while unfiltered
  catalog results retain those currently visible items.

## Assumptions

- F03 supplies the MVP Free access state.
- F04 is the source of published content and valid media relationships.
- F06 owns playback, practice, and progress after a lesson is selected.
- Visitor catalog discovery ends at the published Free lesson summary; sign-in is required before
  the F06 Lesson Player context is created.
- Catalog title search is limited to topic and lesson titles; HSK filtering accepts only levels 1–6.
