# Feature Specification: Shadowing Practice

**Feature Branch**: `[F08-shadowing-practice]`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "F08 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: F08 có phải dùng speech engine chuyên dụng qua private `ai-service` để tạo điểm phát âm thực tế trong MVP không? → A: Có. F08 dùng speech engine chuyên dụng qua private `ai-service`; Spring Boot vẫn giữ quyền xác thực, quota, idempotency và persistence.
- Q: F08 nên dùng route nội bộ nào của `ai-service` để thực hiện đánh giá phát âm? → A: `POST /internal/v1/shadowing/assess`, tách riêng khỏi AI Buddy.
- Q: `ai-service` nên nhận recording F08 bằng cách nào để gọi speech engine? → A: Spring Boot stream audio đã kiểm tra trực tiếp qua private HMAC/mTLS; `ai-service` không nhận storage credential, object key hoặc signed URL.
- Q: F08 có nên lưu transcript do speech engine tạo ra trong MVP không? → A: Không. F08 MVP chỉ lưu score và feedback đã được Spring Boot xác thực.
- Q: Learner được luyện Shadowing ở segment nào? → A: Segment hiện đang mở hoặc đã hoàn thành; future locked segment bị từ chối.

### Session 2026-09-06

- Q: Spring Boot nên truyền audio sang private ai-service Shadowing theo định dạng nào? → A: `multipart/form-data` gồm metadata đã xác thực và binary audio stream; không base64 audio trong JSON.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Record and submit speaking practice (Priority: P1)

As a learner, I want to open a dedicated Shadowing screen from a lesson, record a selected segment,
and submit it for speaking feedback so that I can improve pronunciation.

**Why this priority**: Speaking practice needs a focused, consent-aware recording journey separate
from the Lesson Player while retaining the selected lesson context.

**Independent Test**: A learner opens Shadowing from a permitted segment, submits an eligible
recording, and receives only their own attempt state.

**Acceptance Scenarios**:

1. **Given** the learner's current unlocked lesson segment or an earlier completed segment, **When**
   the learner opens Shadowing, **Then** the separate screen carries the selected lesson and segment
   context.
2. **Given** an eligible recording, **When** the learner submits it, **Then** the attempt enters a
   visible processing or completed state owned only by that learner.
3. **Given** a future segment remains locked by F06 sequential progress, **When** the learner tries
   to open Shadowing or submit a recording for it, **Then** access is denied and no recording or
   attempt is created.

---

### User Story 2 - Receive and manage feedback (Priority: P1)

As a learner, I want to view my permitted speaking score and feedback so that I can practice again
while retaining control of my recording.

**Why this priority**: Voice and feedback are sensitive learner data and require strong ownership.

**Independent Test**: A learner can view their completed attempt and delete/expire behaviour follows
the stated recording classification; another learner or Admin cannot access it.

**Acceptance Scenarios**:

1. **Given** a completed owned attempt, **When** the learner opens it, **Then** they see the
   permitted score and feedback.
2. **Given** a failed, unsafe, or expired recording, **When** the learner views the attempt,
   **Then** they receive a recoverable result without a playable retained recording.

---

### Edge Cases

- Unsupported, oversized, unsafe, or unowned recordings are rejected before assessment.
- A future segment that remains locked by F06 sequential progress cannot create a recording or
  Shadowing attempt.
- A retried assessment cannot consume allowance or produce feedback twice for one logical request.
- Admin, other learners, and providers do not receive general access to raw recordings or feedback.
- A speech-engine timeout, unavailable provider, invalid output, or safety rejection returns a
  recoverable result without duplicate allowance consumption, duplicate progress, raw recording, or
  provider details.

## Requirements *(mandatory)*

- **FR-001**: Shadowing MUST open as a dedicated learner-owned practice screen from the Lesson
  Player with the selected context preserved. The selected segment MUST be either the learner's
  current unlocked segment or an earlier completed segment; the system MUST deny a future segment
  that remains locked by F06 sequential progress.
- **FR-002**: Learners MUST submit only their own eligible recordings for assessment.
- **FR-003**: The system MUST show an owned attempt's processing, completed, failed, or expired
  state, dedicated-speech-engine pronunciation score, and permitted feedback.
- **FR-004**: Recording access, retention, deletion, and classification MUST follow the canonical
  privacy and retention policy.
- **FR-005**: The system MUST enforce one learner AI allowance unit for each actual pronunciation
  assessment and prevent duplicate logical assessment consumption. Opening the Shadowing screen,
  preparing a recording, or viewing an existing result MUST NOT consume allowance.
- **FR-006**: Only approved Shadowing outcomes MAY update shared practice summary or Shadowing
  best-score values. An approved outcome MUST NOT complete or unlock a segment, or change lesson
  completion status.
- **FR-007**: Actual pronunciation assessment MUST use a dedicated speech engine through the
  private `ai-service` route `POST /internal/v1/shadowing/assess`. Before that call, Spring Boot
  MUST validate learner ownership, recording eligibility and input safety, reserve AI allowance
  idempotently, and apply rate limits.
- **FR-008**: `ai-service` MUST receive only minimized validated context over its private HMAC/mTLS
  contract and return schema-validated feedback. It MUST NOT receive browser JWTs, expose a public
  route, own the score/progress decision, or persist the learner's recording or attempt data.
  Spring Boot MUST stream the validated recording directly for this assessment; `ai-service` MUST
  NOT receive object-storage credentials, an object key, or a signed URL.
- **FR-009**: F08 MVP MUST NOT persist or expose a speech-engine transcript. Spring Boot stores
  only the schema-validated pronunciation score and permitted feedback for the owned attempt.
- **FR-010**: The private Spring Boot to ai-service assessment request MUST use
  `multipart/form-data` with authenticated validated metadata and one binary audio stream part.
  It MUST NOT encode audio as base64 JSON or include object-storage credentials, object keys, or
  signed URLs.

### Key Entities

- **Recording**: a learner-owned, classified voice submission with a controlled lifecycle.
- **Shadowing attempt**: an owned assessment state, score, feedback, and selected lesson segment.
- **AI usage event**: the allowance record associated with one logical actual-pronunciation
  assessment.
- **Speech assessment result**: a schema-validated dedicated-speech-engine result used by Spring
  Boot to produce the owned pronunciation score and feedback; its transcript is discarded.
- **Permitted Shadowing segment**: a segment that F06 identifies for the learner as current and
  unlocked, or as already completed; a future locked segment is not permitted.

## Success Criteria *(mandatory)*

- **SC-001**: 100% of recordings and attempts are accessible only to their owning learner.
- **SC-002**: A learner reaches the Shadowing screen with lesson/segment context intact in one action
  from the Lesson Player.
- **SC-003**: 100% of rejected recordings remain unavailable for feedback or playback.
- **SC-004**: Repeating a logical assessment cannot consume allowance or update progress more than
  once.
- **SC-005**: Every actual pronunciation assessment uses the typed private Spring Boot to
  `ai-service` `POST /internal/v1/shadowing/assess` contract; invalid, unsafe, or unavailable
  speech results are not persisted as a completed score or feedback.
- **SC-006**: 100% of Shadowing open and submission requests for a future F06-locked segment are
  denied without creating a recording, attempt, AI usage event, or assessment result.

## Assumptions

- F03 supplies Free-plan AI allowance and F06 supplies the learner-specific current-unlocked versus
  completed versus future-locked lesson/segment context and progress.
- A dedicated speech engine performs actual pronunciation assessment behind private `ai-service`;
  its dedicated `POST /internal/v1/shadowing/assess` route is separate from AI Buddy. Spring Boot
  remains authoritative for the owned attempt, saved score, quota, and progress.
- Spring Boot streams the validated recording directly to the private assessment contract;
  `ai-service` does not receive object-storage access or a persistent recording reference.
- F08 MVP discards the speech-engine transcript and persists only the validated score and feedback.
- Raw recordings use the MVP assessment-only retention default unless a later consent feature adds
  saved recordings.
- Detailed phonetic analytics and sharing are deferred from MVP.
