# Feature Specification: AI Learning Buddy

**Feature Branch**: `[F11-ai-learning-buddy]`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "F11 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: Khi tạo một AI Buddy conversation, learner nên bắt đầu theo phạm vi nào? → A: Chọn một scenario cố định: hội thoại hằng ngày, giải thích từ vựng/ngữ pháp, hoặc role-play; AI chỉ hỗ trợ học tiếng Trung.
- Q: Khi learner gửi message ngoài scenario đã chọn hoặc không an toàn, AI Buddy nên xử lý thế nào? → A: Reject trước AI call; không allowance, không persist message/reply, trả hướng dẫn an toàn.
- Q: Mỗi lần tạo AI reply, Spring Boot nên gửi bao nhiêu lịch sử conversation cho ai-service? → A: Scenario, message hiện tại đã validate, và tối đa 10 message hoàn tất gần nhất của cùng conversation.
- Q: Một learner message gửi AI Buddy trong MVP có giới hạn và định dạng nào? → A: Plain text, tối đa 1.000 ký tự; không file đính kèm hoặc rich-text.
- Q: AI Buddy nên trả lời bằng ngôn ngữ và cấu trúc nào trong MVP? → A: Phản hồi tiếng Trung, kèm giải thích tiếng Việt ngắn và tối đa một gợi ý luyện tiếp.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hold a private learning conversation (Priority: P1)

As a learner, I want to create and continue my own AI learning conversation so that I can practice
Chinese in a guided setting.

**Why this priority**: AI Buddy is the learner-facing conversational use of the approved AI
allowance.

**Independent Test**: A learner creates an owned conversation without spending allowance, sends a
safe message, receives one permitted reply for one allowance unit, and sees only their own history.

**Acceptance Scenarios**:

1. **Given** a signed-in learner, **When** they create a conversation, **Then** the empty
   conversation state is visible only to that learner and consumes no AI allowance.
2. **Given** remaining Free AI allowance and an owned active conversation, **When** the learner
   sends a permitted message requesting an assistant reply, **Then** that reply is visible only to
   that learner and consumes one allowance unit.
3. **Given** no remaining allowance, **When** the learner starts an AI action, **Then** the action
   is declined before any AI work begins and the existing conversation remains intact.
4. **Given** a learner creates a conversation, **When** they select daily conversation,
   vocabulary/grammar explanation, or role-play, **Then** the conversation remains within that
   Chinese-learning scenario.
5. **Given** a message is outside the selected scenario or unsafe, **When** the learner submits
   it, **Then** the system returns safe guidance without an AI call, allowance change, or persisted
   learner or assistant message.
6. **Given** an eligible message requests an assistant reply, **When** Spring Boot calls the
   private service, **Then** it sends that message, the selected scenario, and no more than the
   10 most recent completed messages from the same conversation.
7. **Given** a learner submits plain text of no more than 1,000 characters, **When** it is within
   the selected scenario and safe, **Then** it is eligible for the normal AI Buddy request flow.
8. **Given** a schema-valid assistant result for an eligible request, **When** the learner views
   it, **Then** they receive a Chinese response, concise Vietnamese explanation, and at most one
   next-practice suggestion.

---

### User Story 2 - Control conversation history (Priority: P1)

As a learner, I want to rename or delete my own conversation so that I control my learning history.

**Why this priority**: Chat content is private learner data and requires explicit ownership control.

**Independent Test**: A learner renames and deletes one owned conversation; another learner and
Admin cannot access or perform either action.

**Acceptance Scenarios**:

1. **Given** an owned conversation, **When** the learner renames it, **Then** the new title appears
   only in their own conversation list.
2. **Given** an owned conversation, **When** the learner deletes it, **Then** it is immediately
   hidden and follows the configured deletion lifecycle.

---

### Edge Cases

- Unsafe, invalid, oversized, or rate-limited input returns a recoverable result without leaking
  internal instructions or provider information. A message outside its scenario or unsafe is
  rejected before an AI call, allowance change, or message persistence.
- Retrying one logical message cannot consume allowance or create two assistant replies.
- A conversation cannot become a general-purpose chat or switch to an unapproved scenario; the
  learner creates another conversation to use a different approved learning scenario.
- An AI request never includes another conversation's messages, or failed, pending, or deleted
  messages; it includes no more than 10 completed prior messages from its own conversation.
- A learner message that exceeds 1,000 characters or contains rich text or an attachment is
  rejected before an AI call, allowance change, or message persistence.
- An assistant result missing its Chinese response or concise Vietnamese explanation, or containing
  more than one next-practice suggestion, is invalid and is not persisted.
- Admin, another learner, and an AI provider do not gain general access to conversation history.
- A private AI-service timeout, unavailable provider, invalid output, or safety rejection returns a
  recoverable result without a duplicate allowance charge, duplicate assistant reply, provider
  details, or raw conversation content in logs.

## Requirements *(mandatory)*

- **FR-001**: Learners MUST be able to create, list, view, rename, and delete only their own AI
  learning conversations.
- **FR-002**: Learners MUST be able to send permitted messages only to an owned active conversation.
- **FR-003**: The system MUST enforce one current Free AI allowance unit, rate limits, and input
  safeguards before an AI-assisted reply is created. Conversation create/list/view/rename/delete
  actions MUST NOT consume allowance.
- **FR-004**: The system MUST make a repeated logical message idempotent for allowance and reply
  creation.
- **FR-005**: Conversation content MUST follow learner ownership, minimization, retention, and
  deletion requirements; `ADMIN` has no private-chat access.
- **FR-006**: The learner-facing experience MUST not expose provider credentials, prompts, or
  internal failure details.
- **FR-007**: All AI Buddy model work MUST use the private `ai-service`
  `POST /internal/v1/ai-buddy/respond` contract. Before that call, Spring Boot MUST validate
  identity, ownership, input safety, rate limits, allowance reservation, and idempotency.
- **FR-008**: `ai-service` MUST receive only minimized validated context over HMAC/mTLS, return a
  schema-validated result, and remain stateless for learner data. It MUST NOT accept browser JWTs,
  expose public routes or Mastra Studio, query product tables, persist Mastra Memory, or decide
  conversation ownership, allowance, score, progress, or deletion.
- **FR-009**: Spring Boot MUST validate the AI Buddy result before persisting the owned message and
  map private-service or provider failure to a safe learner-facing error without provider details.
- **FR-010**: A learner MUST select exactly one immutable Chinese-learning scenario when creating an
  AI Buddy conversation: daily conversation, vocabulary/grammar explanation, or role-play. AI Buddy
  MUST support only the selected scenario and MUST NOT operate as a general-purpose chat.
- **FR-011**: The system MUST reject a message outside its conversation's selected scenario or an
  unsafe message before any `ai-service` call, allowance reservation, or learner/assistant message
  persistence. It MUST return only safe learner-facing guidance without internal or provider detail.
- **FR-012**: For an eligible AI Buddy request, Spring Boot MUST send `ai-service` only the selected
  scenario, the current validated learner message, and at most 10 most recent `COMPLETE` messages
  from that same owned conversation. It MUST exclude other conversations and pending, failed, or
  deleted messages.
- **FR-013**: A learner AI Buddy message MUST be plain text of at most 1,000 characters and MUST
  NOT include rich text or an attachment. The system MUST reject an invalid message before any
  `ai-service` call, allowance reservation, or message persistence.
- **FR-014**: A persisted assistant reply MUST contain a Chinese response, concise Vietnamese
  explanation, and at most one next-practice suggestion. Spring Boot MUST persist it only after
  schema validation; an invalid result MUST follow the existing safe failure and allowance-refund
  policy.

### Key Entities

- **AI conversation**: a learner-owned conversation lifecycle and title.
- **AI message**: ordered learner or assistant content belonging to one owned conversation.
- **AI usage event**: an allowance record for one logical AI Buddy request.
- **AI learning scenario**: the immutable Chinese-learning purpose selected for one conversation:
  daily conversation, vocabulary/grammar explanation, or role-play.
- **Local safe rejection**: the non-persisted, non-billable guidance returned when a message is
  outside its selected scenario or unsafe before it reaches `ai-service`.
- **Minimized AI context**: the selected scenario, current validated learner message, and at most
  10 most recent completed messages from the same conversation.
- **Eligible learner message**: a safe, in-scenario plain-text learner message of at most 1,000
  characters and without rich text or an attachment.
- **Bilingual learning reply**: a schema-validated assistant result containing a Chinese response,
  concise Vietnamese explanation, and at most one next-practice suggestion.

## Success Criteria *(mandatory)*

- **SC-001**: 100% of conversation and message operations are restricted to the owning learner.
- **SC-002**: A learner with remaining allowance can start a conversation and receive a safe result
  in under 30 seconds under normal conditions.
- **SC-003**: Exhausted, unsafe, or rate-limited requests do not create an unintended allowance
  charge or duplicate assistant reply.
- **SC-004**: Deleting a conversation immediately removes it from the learner's active history.
- **SC-005**: Every AI Buddy request uses the typed private Spring Boot to `ai-service`
  `POST /internal/v1/ai-buddy/respond` contract; invalid, unsafe, or unavailable results never
  create a persisted assistant message or duplicate allowance consumption.
- **SC-006**: 100% of AI Buddy conversations have exactly one approved Chinese-learning scenario,
  and no assistant reply is persisted for a general-purpose conversation.
- **SC-007**: 100% of out-of-scenario or unsafe messages create no AI call, allowance event,
  learner message, or assistant message.
- **SC-008**: 100% of AI Buddy requests send no more than 10 completed prior messages and none from
  another conversation or a pending, failed, or deleted message.
- **SC-009**: 100% of learner messages exceeding 1,000 characters or containing rich text or an
  attachment are rejected before an AI call, allowance event, or message persistence.
- **SC-010**: 100% of persisted assistant replies contain the required Chinese response and concise
  Vietnamese explanation, with no more than one next-practice suggestion.

## Assumptions

- F01 provides learner identity and F03 provides the MVP Free AI allowance.
- AI Buddy does not grant any Admin, teacher, or other learner access to private conversations.
- `ai-service` is private and stateless for learner data; Spring Boot owns all conversation/message
  persistence, allowance state, deletion, and learner-facing error mapping.
- Voice assessment belongs to F08 and is not part of AI Buddy.
- AI Buddy conversations are limited to the three approved Chinese-learning scenarios; switching
  scenario requires a new conversation.
- Scenario and safety rejection happens before AI allowance or provider processing and does not
  become part of the conversation history.
- AI context is limited to the selected scenario, current validated message, and at most 10 recent
  completed messages from the same conversation.
- Learner messages are plain text only and limited to 1,000 characters in MVP.
- Assistant replies use the bilingual learning-reply schema and provide at most one next-practice
  suggestion.
