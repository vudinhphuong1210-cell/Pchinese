# Feature Specification: Plans, Entitlement, and AI Allowance

**Feature Branch**: `[F03-plans-entitlement-ai-allowance]`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "F03 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: Chính sách AI allowance Free cho MVP nên là mức nào? → A: Chính sách Free đang được server công bố xác định số lượt và chu kỳ; learner đang có entitlement chỉ nhận thay đổi ở ranh giới chu kỳ kế tiếp.
- Q: Khi một yêu cầu AI đã giữ lượt nhưng không tạo được kết quả hợp lệ, allowance nên xử lý thế nào? → A: Hoàn đúng một lượt cho mọi lỗi sau khi giữ lượt mà không tạo được kết quả hợp lệ.
- Q: Những thao tác nào nên tiêu thụ một AI allowance unit trong MVP? → A: Mỗi lần đánh giá phát âm thực tế và mỗi tin nhắn yêu cầu trợ lý trả lời tiêu thụ một lượt; mở phần luyện phát âm hoặc quản lý cuộc trò chuyện không tiêu thụ lượt.
- Q: Khi tài khoản learner bị khóa rồi được mở khóa lại, entitlement và AI allowance nên hoạt động thế nào? → A: Giữ quota/cycle hiện có; lock không reset, unlock tiếp tục allowance còn lại.
- Q: Nếu mã nhận diện một yêu cầu được dùng lại cho một hoạt động AI khác, hệ thống nên làm gì? → A: Trả về xung đột an toàn; không gọi AI và không thay đổi allowance.

### Session 2026-09-10

- Q: Anh muốn giữ F03 ở cấp yêu cầu sản phẩm, còn chi tiết xây dựng được lưu trong tài liệu kỹ thuật riêng không? → A: Giữ hành vi sản phẩm ở đây; chuyển chi tiết xây dựng sang tài liệu kỹ thuật riêng.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Receive and understand Free access (Priority: P1)

As a verified learner, I want the system to apply my Free access automatically and show my own
current allowance so that I know what learning and AI use is available.

**Why this priority**: MVP deliberately operates Free-only and must do so consistently.

**Independent Test**: A newly eligible learner receives Free access, sees only their own allowance
summary, and can use permitted AI capacity.

**Acceptance Scenarios**:

1. **Given** an eligible verified learner, **When** their account becomes active, **Then** the
   system applies the Free plan without an administrator action.
2. **Given** the learner views their account, **When** an AI allowance exists, **Then** they see a
   safe summary of their own available or exhausted allowance.

---

### User Story 2 - Enforce AI allowance fairly (Priority: P1)

As a learner, I want the product to accept AI-assisted learning only while allowance remains so that
the same published policy applies to every learner.

**Why this priority**: AI use must be protected from accidental or repeated over-consumption.

**Independent Test**: Repeating a logical AI request does not consume allowance twice; a learner
with no remaining allowance receives a clear recoverable result before any AI work begins.

**Acceptance Scenarios**:

1. **Given** remaining Free allowance, **When** the learner submits an actual pronunciation
   assessment or a message that requests an assistant reply, **Then** one allowance unit is
   recorded once for that activity.
2. **Given** no remaining allowance, **When** the learner starts an AI activity, **Then** it is
   declined without changing the allowance.

---

### Edge Cases

- Concurrent or retried logical requests cannot spend the same allowance more than once.
- A service timeout, unavailable service, invalid result, or safety rejection that
  produces no valid result refunds exactly one allowance unit once.
- No Admin screen or action can grant, revoke, edit, or inspect an individual learner's allowance.
- No learner, administrator, or external service can reserve, consume, refund, or otherwise change
  an allowance; only the system can do so.
- Opening Shadowing or creating, renaming, listing, viewing, or deleting an AI Buddy conversation
  does not consume an allowance unit.
- A locked learner is declined before entitlement or quota processing. Locking or unlocking does
  not create, revoke, reset, or otherwise change the current entitlement or allowance cycle.
- A request intended for one AI activity cannot be reused for another; the system returns a safe
  conflict without AI processing, a new usage record, or an allowance change.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The MVP MUST automatically assign and enforce exactly one current Free entitlement
  for every eligible learner. Its allowance limit and period are a server-owned snapshot of the
  current published Free policy at activation or the next allowance-cycle boundary. Locking or
  unlocking the learner account MUST preserve that current entitlement, cycle snapshot, start and
  used-unit count.
- **FR-002**: A learner MUST be able to view only their own safe plan and allowance summary.
- **FR-003**: The system MUST check and record one allowance unit before every actual
  pronunciation assessment and every learner message that requests an assistant reply; it MUST
  prevent a repeated logical request from consuming allowance again. Opening Shadowing and AI Buddy
  conversation lifecycle actions MUST NOT consume allowance.
- **FR-008**: The system MUST recognize a repeat of the same AI activity and reuse its existing
  usage record. A request intended for one activity MUST return a safe conflict before AI
  processing or an allowance change if it is reused for a different activity.
- **FR-004**: The system MUST prevent an exhausted learner from starting another eligible AI
  activity and preserve the existing allowance state.
- **FR-005**: Allowance state MUST be controlled only by the system and must not be changed by
  learner input or an administrator action.
- **FR-006**: Paid Premium activation, upgrade journeys, payment-provider integration, and manual
  entitlement/quota operations are out of scope for MVP. F12 may revise future global catalogue
  policies but never creates a learner-level entitlement or changes an active cycle.
- **FR-007**: Before an eligible pronunciation assessment or assistant reply begins, the system
  MUST reserve or reuse its usage record together as one indivisible action. Only the system MAY consume or
  refund the learner's allowance. A valid successful result consumes the reserved unit; a timeout,
  unavailable service, invalid result, or safety rejection with no valid result MUST refund exactly
  one unit.

### Key Entities

- **Plan**: the published Free access and allowance policy for MVP.
- **Entitlement**: the automatically applied current access state for one learner.
- **AI usage record**: a non-duplicated record of one actual pronunciation assessment or one
  assistant-reply request and its outcome.
- **Request match**: a secure system mechanism that recognises the same learning activity without
  retaining private conversation or recording content.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of eligible active learners receive exactly one current Free access state.
- **SC-002**: 100% of eligible AI requests are accepted or declined using the learner's current
  allowance before AI work begins.
- **SC-003**: Retrying the same logical AI request never increases usage more than once.
- **SC-004**: No learner-facing Premium activation or upgrade journey is available in MVP.
- **SC-005**: 100% of post-reservation pronunciation-assessment or assistant-reply failures without a valid result refund exactly one
  allowance unit and never produce a successful learner result.
- **SC-006**: 100% of Shadowing navigation and AI Buddy conversation lifecycle actions consume zero
  allowance units.
- **SC-007**: 100% of locked-then-unlocked learner accounts retain the same Free entitlement and
  remaining allowance for their current cycle.
- **SC-008**: 100% of requests reused for a different AI activity return no AI result, create no
  new usage record, and leave allowance unchanged.

## Assumptions

- An authenticated, active learner is eligible for Free access.
- The Free entitlement cycle starts when access is granted; each completed policy-defined cycle
  creates a new server-owned allowance snapshot before the next eligible AI request is assessed.
- The system confirms the learner is active before accessing entitlement or quota; an account lock is
  an access-state change, not an entitlement lifecycle event.
- An actual pronunciation assessment and a learner message that requests an assistant reply each
  consume one unit only after the system accepts the request; neither learning flow can change
  entitlement rules or allowance state directly.
