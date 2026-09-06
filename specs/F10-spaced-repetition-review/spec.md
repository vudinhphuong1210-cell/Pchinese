# Feature Specification: Spaced Repetition Review

**Feature Branch**: `[F10-spaced-repetition-review]`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "F10 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: Một saved word mới nên trở thành due để review khi nào? → A: Ngay sau lần save đầu tiên; word xuất hiện trong due queue ngay.
- Q: Sau khi learner chọn Again, Hard, Good hoặc Easy, lịch review tiếp theo nên được tính theo quy tắc nào? → A: Lần đầu: Again 10 phút, Hard 1 ngày, Good 3 ngày, Easy 7 ngày. Các lần sau: Again 10 phút và giảm ease 0.2; Hard ×1.2, giảm ease 0.15; Good ×ease; Easy ×(ease + 0.15). Ease luôn trong 1.3–2.5.
- Q: Khi learner remove một saved word rồi restore lại, SRS schedule cũ nên xử lý thế nào? → A: Resume schedule cũ và giữ interval, ease, history; word quá hạn trở thành due ngay.
- Q: Khi learner có nhiều word đến hạn, review queue nên chọn và sắp xếp chúng thế nào? → A: Tối đa 20 word mỗi queue, sắp theo due time cũ nhất trước; hoàn tất queue thì tải batch mới.
- Q: Nếu cùng một review bị gửi lại do retry hoặc đồng thời từ hai thiết bị, hệ thống nên phản hồi thế nào? → A: Retry cùng logical review trả kết quả đã có, không tạo event mới; review stale khác bị từ chối và trả latest schedule.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Review due vocabulary (Priority: P1)

As a learner, I want to see my due saved words and submit a review response so that I can reinforce
vocabulary at the right time.

**Why this priority**: The review queue turns saved vocabulary into an ongoing learning habit.

**Independent Test**: A learner with due words receives only their own bounded due queue and can
submit a valid response for an owned schedule.

**Acceptance Scenarios**:

1. **Given** due saved words, **When** the learner opens review, **Then** they see only their own
   due vocabulary in a bounded queue.
2. **Given** an owned due item, **When** the learner submits a valid rating, **Then** the next due
   state is returned.
3. **Given** a learner saves a dictionary entry for the first time, **When** the save succeeds,
   **Then** exactly one learner-owned `LEARNING` schedule is created or reused as due immediately.
4. **Given** an initial or later due schedule, **When** the learner submits `AGAIN`, `HARD`,
   `GOOD`, or `EASY`, **Then** the server returns the next due state from the agreed four-rating
   schedule policy.
5. **Given** a learner restores a removed saved word, **When** F10 resumes its original schedule,
   **Then** interval, ease, and immutable history remain unchanged; an elapsed due time makes the
   word due immediately without a second schedule.
6. **Given** more than 20 owned words are due, **When** the learner opens review, **Then** they
   receive at most 20 words ordered by the oldest due time first; after completing that queue, they
   can load the next batch.

---

### User Story 2 - Trust a personal review history (Priority: P2)

As a learner, I want my review result and next due time to remain accurate so that repeated review
does not create conflicting schedules.

**Why this priority**: Confidence in the schedule is necessary for continued review.

**Independent Test**: A learner submits concurrent or repeated review actions and observes one
authoritative next schedule with a consistent personal history.

**Acceptance Scenarios**:

1. **Given** a review has already updated a schedule, **When** the learner repeats the same stale
   action, **Then** the system prevents a conflicting second schedule update.
2. **Given** a retry of an accepted logical review, **When** the learner submits it again, **Then**
   they receive the original review result without a second event; a different stale review action
   is rejected with the latest schedule.

---

### Edge Cases

- A schedule or saved word belonging to another learner is indistinguishable from unavailable.
- No due words produces an encouraging empty state without exposing another learner's queue.
- Repeating the initial-save action cannot create a second review schedule for the same saved word.
- A rating outside `AGAIN`, `HARD`, `GOOD`, or `EASY` is rejected without creating a review event
  or changing the schedule.
- A removed saved word has its existing review schedule suspended and does not appear in a queue.
  When its owner restores it, F10 resumes the same schedule without resetting interval, ease, or
  history; an already elapsed due time is immediately due.
- A queue includes no more than 20 owned schedules that are due at retrieval; schedules that become
  due later are considered only in a later batch.
- A retry of the same accepted logical review returns its recorded result. A different action based
  on an older schedule state creates no event and returns the latest schedule for recovery.

## Requirements *(mandatory)*

- **FR-001**: Learners MUST be able to view only their own due vocabulary in a queue of at most
  20 items, ordered by oldest due time first. When the learner completes that queue, the system
  MUST allow loading the next due batch.
- **FR-002**: Learners MUST submit only `AGAIN`, `HARD`, `GOOD`, or `EASY` for an owned review
  schedule.
- **FR-003**: The system MUST calculate and persist the next due state authoritatively, not from
  client-provided schedule values.
- **FR-004**: Each accepted review MUST create an immutable learner-owned review history record.
- **FR-005**: The system MUST prevent repeated or concurrent actions from producing conflicting
  schedules or duplicate history for one logical review. A retry with the same logical-review
  identifier MUST return its original accepted result and create no event; a different action based
  on an older schedule state MUST be rejected without change and return the latest schedule.
- **FR-006**: Review views MUST handle no-due, unavailable, and recoverable conflict states.
- **FR-007**: When F09 first creates a learner-owned saved word, F10 MUST create or reuse exactly
  one learner-owned `LEARNING` review schedule due immediately. Repeated saves MUST NOT create a
  second schedule; restoration behaviour for a previously removed word remains governed by its
  existing schedule.
- **FR-008**: The server MUST apply the four-rating schedule policy. On the initial due review,
  `AGAIN` sets the next due time to 10 minutes, `HARD` to 1 day, `GOOD` to 3 days, and `EASY` to
  7 days. On every later review, `AGAIN` sets the next due time to 10 minutes and decreases ease
  by 0.2; `HARD` uses the prior interval × 1.2 and decreases ease by 0.15; `GOOD` uses the prior
  interval × prior ease; `EASY` uses the prior interval × (prior ease + 0.15). The persisted ease
  factor MUST remain between 1.3 and 2.5.
- **FR-009**: When a learner-owned saved word is removed, F10 MUST suspend its existing review
  schedule without resetting interval, ease, or immutable review history. When its owner restores
  that saved word, F10 MUST resume the same schedule without creating another; if its due time has
  elapsed, it MUST be due immediately.
- **FR-010**: The system MUST atomically accept one logical review only from the current schedule
  state. It MUST preserve enough learner-owned history to return the original result on an
  identical retry and to identify a different stale review action safely.

### Key Entities

- **Review schedule**: the learner-owned current due state for one saved word.
- **Review event**: immutable record of a submitted rating and resulting schedule transition.
- **Initial review schedule**: the single `LEARNING` schedule created or reused for a newly saved
  word, due immediately for the learner's first review.
- **Four-rating schedule policy**: the server-owned rule that transforms `AGAIN`, `HARD`, `GOOD`,
  or `EASY` into the next due time, interval, and bounded ease factor.
- **Suspended review schedule**: the existing schedule for a removed saved word, excluded from the
  review queue while retaining its interval, ease, and immutable history until owner restoration.
- **Review queue batch**: at most 20 learner-owned schedules that are due when retrieved, ordered
  by oldest due time first.
- **Logical review**: one learner rating for one current schedule state; an identical retry reuses
  its recorded result, while a different stale action is rejected.

## Success Criteria *(mandatory)*

- **SC-001**: 100% of due queues, schedules, and history records are restricted to their owner.
- **SC-002**: A learner with due words can submit a review and see the next due state in under
  30 seconds.
- **SC-003**: Repeated or concurrent submissions never create conflicting next due states.
- **SC-004**: An empty queue gives the learner a clear completion state.
- **SC-005**: 100% of first-time saved words create or reuse exactly one owned `LEARNING` schedule
  due immediately, while repeated saves create no second schedule.
- **SC-006**: 100% of accepted review ratings apply the four-rating schedule policy server-side and
  persist an ease factor between 1.3 and 2.5.
- **SC-007**: 100% of restored saved words resume their original schedule without resetting
  interval, ease, or history, and without creating a second schedule.
- **SC-008**: 100% of due queues contain no more than 20 learner-owned due schedules, sorted by
  oldest due time first.
- **SC-009**: 100% of identical logical-review retries return the original result without a second
  event, while different stale actions create no event and return the latest schedule.

## Assumptions

- F10 depends on F09 saved vocabulary and F01 learner identity.
- The schedule policy in the canonical MVP data contract is the source of calculation rules.
- A first-time F09 saved-word creation makes its F10 schedule due immediately. F09 preserves the
  original saved-word relationship during restoration; F10, rather than F09, resumes the existing
  schedule without creating a second one.
- Initial and later rating transitions use the four-rating policy in this specification; React does
  not calculate due times, intervals, or ease.
- Shared dictionary editing and learning recommendations are out of scope.
