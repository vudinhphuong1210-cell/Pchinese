# Feature Specification: Lesson Learning and Progress

**Feature Branch**: `[F06-lesson-learning-progress]`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "F06 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: Learner nên hoàn thành và đi tiếp qua các segment theo cách nào trong MVP? → A: Learner hoàn thành current segment; server ghi một lần và mở segment kế tiếp. Có thể xem lại segment đã hoàn thành.
- Q: Lesson nên được đánh dấu `COMPLETED` tại thời điểm nào trong MVP? → A: `COMPLETED` chỉ khi learner hoàn thành toàn bộ segment theo thứ tự.
- Q: Kết quả Dictation hoặc Shadowing đã được duyệt nên tác động thế nào đến việc hoàn thành segment? → A: Approved Dictation/Shadowing cập nhật practice metrics, không complete/unlock segment.
- Q: Khi nào hệ thống nên tạo progress state cho một learner và lesson? → A: Tạo lazy khi learner lần đầu vào Lesson Player.
- Q: Nếu learner complete cùng current segment từ hai thiết bị gần như đồng thời, hệ thống nên xử lý request đến sau thế nào? → A: Một completion thắng; request còn lại không đổi progress và trả latest state.
- Q: Learner nên xác nhận hoàn thành current segment bằng cách nào trong MVP? → A: Player tự complete ngay khi segment phát tới cuối.
- Q: Learner có được kéo tới cuối current segment để Player auto-complete mà không thực sự phát nội dung không? → A: Chỉ cho tua lại; không tua vượt phần đã phát, auto-complete khi playback thực sự tới cuối.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Learn through ordered lesson segments (Priority: P1)

As a learner, I want to play a permitted lesson and move through its ordered segments so that I can
study Chinese content in a coherent sequence.

**Why this priority**: Lesson learning is the core journey that connects catalog content to practice.

**Independent Test**: A learner opens a published Free lesson, views only permitted ordered segments,
and continues through them without seeing hidden content.

**Acceptance Scenarios**:

1. **Given** a permitted lesson, **When** the learner opens it, **Then** they see its current
   ordered learning segments and allowed playback.
2. **Given** a segment becomes unavailable, **When** the learner reaches it, **Then** the lesson
   shows a recoverable unavailable state with a return to catalog rather than stale material, while
   retaining the learner's existing progress.
3. **Given** a learner has not completed the current segment, **When** they attempt to access a
   later segment, **Then** that later segment remains unavailable; completing the current segment
   opens only the next ordered segment, while earlier completed segments remain available to revisit.
4. **Given** a learner with no progress state enters a permitted Lesson Player for the first time,
   **When** the lesson opens, **Then** one `NOT_STARTED` state is created with the first ordered
   segment as current and zero completed segments.
5. **Given** two completion requests target the same current segment, **When** one completion has
   already advanced progress, **Then** the later request leaves progress unchanged and receives the
   latest progress state.
6. **Given** the Player reaches the end of a learner's current segment, **When** the end is reached,
   **Then** the Player automatically requests that segment's completion; no separate learner
   confirmation is required.
7. **Given** a learner has played only part of the current segment, **When** they seek within the
   Player, **Then** they may move back only within the already played portion and cannot jump ahead;
   auto-completion occurs only after server-validated playback reaches the segment end.

---

### User Story 2 - Resume accurate learning progress (Priority: P1)

As a signed-in learner, I want to resume my lesson and see an accurate overview of progress so that
I can continue from the right place.

**Why this priority**: Progress must be consistent across normal learning and later practice flows.

**Independent Test**: A learner completes permitted segment actions, leaves, returns, and sees the
server-confirmed current segment and completion state.

**Acceptance Scenarios**:

1. **Given** partial lesson progress, **When** the learner returns, **Then** the current segment and
   completion information match the latest confirmed state.
2. **Given** a completed lesson, **When** the learner views progress, **Then** completion is shown
   once without double-counting practice results.
3. **Given** a learner completes the final unlocked segment, **When** the completion is accepted,
   **Then** the lesson becomes `COMPLETED` at 100% and has no next segment to unlock.

---

### Edge Cases

- Guests may browse catalog and lesson summaries through F05 but cannot enter Lesson Player,
  playback, Dictation, or Shadowing; signed-in learners receive only their own progress.
- If a lesson, segment, or associated media becomes unavailable, the next requested resource is
  blocked and returns a recoverable catalog path; existing learner progress is retained.
- Repeated completion actions do not inflate completion or practice totals.
- A learner cannot skip a current segment to unlock a later segment, but may revisit any completed
  segment without changing its completion count.
- Browsing the catalog or lesson summary alone does not create progress; initial progress exists only
  after the learner enters a permitted Lesson Player.
- Concurrent requests for the same current segment cannot create a second completion or unlock an
  additional segment; the later request receives the current authoritative progress state.
- A lesson remains `IN_PROGRESS` until every ordered segment is completed; no partial threshold can
  mark it complete in MVP.
- An approved Dictation or Shadowing result can update practice summary metrics only; it cannot
  complete or unlock a segment.
- A segment that does not reach its playback end remains incomplete; learner confirmation alone is
  not a completion action in MVP.
- A learner cannot seek beyond the server-validated furthest point of the current segment; seeking
  backward within the played portion does not change completion or unlock state.
- Dictation starts within the Lesson Player; Shadowing opens with the same selected lesson/segment.

## Requirements *(mandatory)*

- **FR-001**: The Lesson Player MUST show only current permitted, ordered, and unlocked content.
- **FR-002**: Signed-in learners MUST receive and resume only their own lesson progress.
- **FR-003**: The system MUST maintain one authoritative progress state per learner and lesson. It
  MUST mark that state `COMPLETED` at 100% only after every ordered segment is completed; MVP does
  not permit an administrator-set partial completion threshold.
- **FR-004**: The Lesson Player MUST provide the in-player entry point for Dictation and preserve
  selected lesson/segment context when opening Shadowing.
- **FR-005**: This feature MUST not calculate Dictation or Shadowing scores; those features provide
  approved outcomes to the shared progress state. Those outcomes MAY update practice summary or best
  score values only and MUST NOT complete or unlock a segment.
- **FR-006**: Lesson and progress views MUST provide loading, empty, unavailable, and recoverable
  error states.
- **FR-007**: The system MUST accept completion only for the learner's current unlocked segment,
  record it exactly once, and unlock the next ordered segment when the Lesson Player reaches that
  segment's playback end. It MUST allow completed segments to be revisited without changing progress
  and MUST reject an attempt to access or complete a later locked segment; no separate learner
  confirmation is required.
- **FR-008**: Only the Lesson Player's valid automatic end-of-segment completion flow MAY change
  `completed_segment_count`, `current_segment_id`, or lesson completion status. Approved Dictation
  and Shadowing outcomes MUST NOT change those fields.
- **FR-009**: On a learner's first entry to a currently permitted Lesson Player, the system MUST
  create or reuse exactly one `NOT_STARTED` progress state with zero completed segments and the
  lowest-sequence permitted segment as current. Catalog and lesson-summary access MUST NOT create
  progress.
- **FR-010**: When concurrent completion requests target the same current segment, the system MUST
  apply at most one transition. Any later request MUST leave progress unchanged and return the latest
  authoritative progress state.
- **FR-011**: The system MUST keep a server-validated playback watermark for the current segment.
  The Player MAY seek backward within that watermark but MUST NOT seek forward beyond it.
  Automatic completion is permitted only when the watermark reaches the segment end; client-reported
  playback position alone is not authoritative.

### Key Entities

- **Lesson progress**: a learner-owned current unlocked segment, contiguous completed-segment count,
  completion state, and practice summary.
- **Completed lesson**: a progress state with every ordered segment completed, 100% completion, and
  no next segment to unlock.
- **Practice summary**: approved Dictation and Shadowing metrics associated with a lesson; it is not
  segment-completion evidence.
- **Initial lesson progress**: the lazy-created `NOT_STARTED` state with no completed segments and
  the first permitted segment as current.
- **Automatic segment completion**: the Player-initiated completion of the current unlocked segment
  when its playback reaches the end; it has no separate learner-confirmation action.
- **Playback watermark**: the server-validated furthest playback point of the current segment; it
  permits rewind but prevents forward seeking and premature automatic completion.
- **Lesson segment**: an ordered permitted unit of a lesson.

## Success Criteria *(mandatory)*

- **SC-001**: 100% of returned progress belongs to the authenticated learner and selected lesson.
- **SC-002**: A learner can resume a partially completed lesson in under 30 seconds.
- **SC-003**: Repeated completion actions do not change a completed segment or lesson more than once.
- **SC-004**: A learner can enter Dictation or Shadowing with the selected lesson/segment context
  intact.
- **SC-005**: 100% of valid segment completions advance progress by one ordered segment at most;
  no learner request unlocks a later segment before its predecessor is completed.
- **SC-006**: 100% of `COMPLETED` lessons have all ordered segments completed and report 100%
  completion; no partial completion is marked complete.
- **SC-007**: 100% of approved Dictation and Shadowing outcomes leave the current segment,
  completed-segment count, and lesson completion status unchanged.
- **SC-008**: 100% of first permitted Lesson Player entries create or reuse one initial progress
  state; catalog and lesson-summary views create none.
- **SC-009**: 100% of concurrent completion requests for the same segment result in at most one
  completed-segment increment and at most one newly unlocked segment.
- **SC-010**: 100% of current segments whose Player playback reaches the end automatically create at
  most one completion; a learner confirmation control is not required.
- **SC-011**: 100% of forward seeks beyond a current segment's server-validated playback watermark
  are denied, and no such seek can auto-complete or unlock a segment.

## Assumptions

- F01 provides identity and F05 provides current permitted lesson access.
- Dictation and Shadowing are separate feature specifications that may update shared progress.
- Assessment-specific history and scoring remain outside this feature.
