# Feature Specification: Dictation Practice

**Feature Branch**: `[F07-dictation-practice]`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "F07 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: F07 có bao gồm AI-generated feedback trong MVP ngoài kết quả Dictation được Spring Boot xác nhận không? → A: Không. F07 MVP dùng kết quả deterministic phía Spring Boot; không gọi `ai-service` và không tiêu thụ AI allowance.
- Q: Sau khi đã có kết quả Dictation, learner có được làm lại cùng segment trong MVP không? → A: Cho retake; attempt mới được giữ trong history, best score cập nhật khi cao hơn.
- Q: Dictation MVP nên chấp nhận dạng câu trả lời nào để chấm deterministic? → A: Simplified Hanzi; bỏ khác biệt Unicode, space, punctuation.
- Q: Sau khi Dictation được chấm, learner nên nhận mức feedback nào trong MVP? → A: Overall score, accuracy, guidance chung; không highlight lỗi từng ký tự hay tự hiện full answer.
- Q: Overall score Dictation nên được tính theo cách nào sau khi answer đã normalize? → A: Chỉ chấm đúng hoặc sai: answer khớp hoàn toàn nhận 100, khác nhận 0. Learner có thể retake để sửa và tiến bộ.
- Q: Learner được mở Dictation trên những segment nào? → A: Chỉ segment hiện đang unlocked hoặc đã hoàn thành trước đó; segment tương lai đang khóa không được luyện.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Complete dictation in a lesson (Priority: P1)

As a learner, I want to listen to a practiceable lesson segment and submit my answer inside the
Lesson Player so that I can improve listening and writing without losing lesson context.

**Why this priority**: Dictation is a primary learner practice flow and intentionally has no
separate primary screen.

**Independent Test**: A learner starts a permitted Dictation activity, submits an answer, and sees
their own result while remaining in the selected lesson.

**Acceptance Scenarios**:

1. **Given** the learner's current unlocked segment or an earlier completed practiceable segment,
   **When** the learner starts Dictation, **Then** the activity uses that lesson/segment context
   inside the Lesson Player.
2. **Given** a valid submitted answer, **When** evaluation completes, **Then** the learner sees the
   allowed result and next-step guidance.
3. **Given** an evaluated Dictation attempt, **When** the learner starts Dictation again for the
   same permitted segment, **Then** a new attempt is created and the prior attempt remains in their
   history.
4. **Given** a learner submits simplified Hanzi with only whitespace, punctuation, or Unicode-form
   differences from the expected answer, **When** it is evaluated, **Then** those differences do not
   reduce the deterministic result; character choice and order remain significant.
5. **Given** a Dictation evaluation completes, **When** the learner sees its result, **Then** they
   receive only overall score, accuracy, and general retry guidance without per-character analysis
   or an automatically displayed full expected answer.
6. **Given** a normalized answer differs from the expected answer in any character or order,
   **When** it is evaluated, **Then** it receives score 0; an exactly matching normalized answer
   receives score 100.
7. **Given** a future segment remains locked by F06 sequential progress, **When** the learner tries
   to start Dictation for it, **Then** access is denied and no attempt is created.

---

### User Story 2 - Review owned attempts (Priority: P2)

As a learner, I want to see my own Dictation attempts so that I can understand prior results
without exposing answers or scores of others.

**Why this priority**: Personal history supports improvement while preserving ownership.

**Independent Test**: A learner lists and opens only their own attempts for a lesson or segment.

**Acceptance Scenarios**:

1. **Given** prior attempts, **When** the learner views history, **Then** only their own summaries
   and permitted detail appear.

---

### Edge Cases

- An answer cannot submit a learner identity, score, feedback, lesson, or access decision as truth.
- A future segment that remains locked, or a duplicate submission, does not create an invalid
  additional attempt.
- A retry of the same logical submission returns its existing attempt and result; a deliberate
  retake creates a new attempt without overwriting history.
- Pinyin or traditional Hanzi does not substitute for the expected simplified-Hanzi answer.
- A result does not reveal per-character error analysis or the complete expected answer
  automatically.
- Any normalized mismatch receives score 0; only an exact normalized match receives score 100, and
  the same normalized answer and expected answer always receive the same score.
- Administrators cannot browse or evaluate another learner's private attempts through Admin tools.

## Requirements *(mandatory)*

- **FR-001**: Learners MUST start Dictation only from the Lesson Player for a practiceable segment
  that is either their current unlocked segment or an earlier completed segment. The system MUST
  deny Dictation for a future segment that remains locked by F06 sequential progress.
- **FR-002**: Learners MUST be able to submit an answer and receive a server-confirmed result.
- **FR-003**: The system MUST create and return attempts only for the authenticated learner and
  selected valid lesson/segment.
- **FR-004**: Learners MUST be able to list and view only their own Dictation history.
- **FR-005**: The system MUST update shared practice summary or Dictation best-score values only
  from approved Dictation outcomes. An approved outcome MUST NOT complete or unlock a segment, or
  change lesson completion status.
- **FR-006**: Dictation MUST remain an in-player activity and not create a separate primary screen.
- **FR-007**: Dictation MVP MUST calculate and return its deterministic result through Spring Boot;
  it MUST NOT call `ai-service`, consume AI allowance, or delegate the Dictation score to a provider.
- **FR-008**: A learner MAY start a new Dictation attempt for the same permitted segment after an
  earlier attempt is evaluated. Each retake MUST retain its own owned history record; a retry of the
  same logical submission MUST reuse the original attempt and result. An approved retake updates the
  Dictation best score only when its score is higher.
- **FR-009**: Dictation MUST compare the learner answer to the segment's expected simplified-Hanzi
  answer after Unicode, whitespace, and punctuation normalization. Correct Hanzi characters and
  their order MUST match; pinyin and traditional Hanzi MUST NOT be treated as equivalent answers.
- **FR-010**: A completed Dictation result MUST expose only overall score, accuracy, and general
  retry guidance. It MUST NOT expose per-character error analysis or automatically display the full
  expected answer in MVP.
- **FR-011**: Dictation MUST calculate `overall_score` and `accuracy_percent` as the same binary
  result after normalization: 100 for an exact simplified-Hanzi character-and-order match to the
  expected answer, otherwise 0. The result MUST be deterministic for the same normalized inputs.

### Key Entities

- **Dictation attempt**: a learner-owned answer, status, result, and lesson/segment context.
- **Evaluation outcome**: the permitted overall score, accuracy, and general retry guidance for one
  completed attempt; it excludes per-character analysis and automatic full-answer disclosure.
- **Dictation retake**: a new owned attempt for a segment after an earlier attempt was evaluated;
  it preserves, rather than replaces, prior history.
- **Normalized Dictation answer**: the simplified-Hanzi answer after Unicode, whitespace, and
  punctuation normalization; character identity and order remain meaningful.
- **Binary Dictation accuracy**: the 0–100 result where only an exact normalized answer receives
  100 and every mismatch receives 0.
- **Permitted Dictation segment**: a practiceable segment that F06 identifies for the learner as
  current and unlocked, or as already completed; a future locked segment is not permitted.

## Success Criteria *(mandatory)*

- **SC-001**: 100% of attempt reads and submissions are restricted to the authenticated learner.
- **SC-002**: A learner can start, answer, and receive a completed Dictation result without leaving
  the Lesson Player.
- **SC-003**: Duplicate or invalid submissions never create a second valid result for the same
  logical action.
- **SC-004**: Each approved outcome updates the applicable shared practice summary or Dictation
  best-score value at most once and never changes F06 Player completion or unlocking.
- **SC-005**: 100% of F07 submissions produce a Spring Boot-determined result without an AI usage
  event or `ai-service` request.
- **SC-006**: 100% of valid retakes create a distinct owned history record, while retries of the
  same submission create no second result and best score never decreases.
- **SC-007**: 100% of deterministic evaluations ignore Unicode, whitespace, and punctuation-only
  differences but distinguish simplified Hanzi character/order differences, pinyin, and traditional
  Hanzi.
- **SC-008**: 100% of completed Dictation results expose only score, accuracy, and general retry
  guidance, without per-character analysis or automatic full-answer disclosure.
- **SC-009**: 100% of evaluated normalized answer/expected-answer pairs receive the same binary
  result on retry: 100 for an exact match and 0 for every mismatch.
- **SC-010**: 100% of Dictation start requests for a future F06-locked segment are denied without
  creating an attempt or result.

## Assumptions

- F06 provides the learner-specific current-unlocked versus completed versus future-locked
  lesson/segment context and shared progress state.
- F07 MVP does not use AI-assisted evaluation or consume the F03 AI allowance.
- Detailed token-level error analytics are deferred from MVP.
