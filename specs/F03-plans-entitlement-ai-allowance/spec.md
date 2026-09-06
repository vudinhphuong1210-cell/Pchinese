# Feature Specification: Plans, Entitlement, and AI Allowance

**Feature Branch**: `[F03-plans-entitlement-ai-allowance]`

**Created**: 2026-09-05

**Status**: Draft

**Input**: User description: "F03 from the MVP Feature Map."

## Clarifications

### Session 2026-09-05

- Q: Chính sách AI allowance Free cho MVP nên là mức nào? → A: 30 lượt/30 ngày, reset theo chu kỳ entitlement của learner.
- Q: Khi F08 hoặc F11 đã reserve quota nhưng ai-service/speech provider bị timeout, không khả dụng, trả kết quả sai schema hoặc safety rejection, allowance nên xử lý thế nào? → A: Hoàn đúng một lượt (`FAILED_REFUNDED`) cho mọi lỗi sau reserve mà không tạo kết quả hợp lệ.
- Q: Những thao tác nào nên tiêu thụ một AI allowance unit trong MVP? → A: F08: mỗi actual pronunciation assessment; F11: mỗi learner message yêu cầu assistant reply. Mở Shadowing, tạo/đổi tên/xóa conversation không tốn lượt.
- Q: Khi tài khoản learner bị khóa rồi được mở khóa lại, entitlement và AI allowance nên hoạt động thế nào? → A: Giữ quota/cycle hiện có; lock không reset, unlock tiếp tục allowance còn lại.
- Q: Nếu cùng một `client_request_id` bị gửi lại cho một hoạt động AI khác, hệ thống nên làm gì? → A: Trả lỗi idempotency conflict an toàn; không gọi AI và không đổi quota.

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

1. **Given** remaining Free allowance, **When** the learner submits an actual F08 pronunciation
   assessment or an F11 message that requests an assistant reply, **Then** one allowance unit is
   recorded once for that activity.
2. **Given** no remaining allowance, **When** the learner starts an AI activity, **Then** it is
   declined without changing the allowance.

---

### Edge Cases

- Concurrent or retried logical requests cannot spend the same allowance more than once.
- A post-reservation timeout, unavailable service, invalid schema output, or safety rejection that
  produces no valid result refunds exactly one allowance unit once.
- No Admin screen or action can grant, revoke, edit, or inspect an individual learner's allowance.
- A client, provider, or `ai-service` cannot reserve, consume, refund, or otherwise mutate an
  allowance; those state changes remain backend-owned.
- Opening Shadowing or creating, renaming, listing, viewing, or deleting an AI Buddy conversation
  does not consume an allowance unit.
- A locked learner is declined before entitlement or quota processing. Locking or unlocking does
  not create, revoke, reset, or otherwise change the current entitlement or allowance cycle.
- Reusing a `client_request_id` for a different server-computed logical-request fingerprint returns
  a safe idempotency conflict without an AI call, new usage event, or allowance change.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The MVP MUST automatically assign and enforce exactly one current Free entitlement
  for every eligible learner. The Free entitlement provides 30 AI allowance units per rolling
  30-day entitlement cycle. Locking or unlocking the learner account MUST preserve that current
  entitlement, its cycle start, and its used-unit count.
- **FR-002**: A learner MUST be able to view only their own safe plan and allowance summary.
- **FR-003**: The system MUST check and record one allowance unit before every actual F08
  pronunciation assessment and every F11 learner message that requests an assistant reply; it MUST
  make repeated logical requests idempotent. Opening Shadowing and AI Buddy conversation lifecycle
  actions MUST NOT consume allowance.
- **FR-008**: Spring Boot MUST bind each `client_request_id` to an immutable, server-computed
  logical-request fingerprint. A retry with the same fingerprint MUST reuse its existing usage
  event; a different fingerprint for that learner and request ID MUST return a safe idempotency
  conflict before any AI call or allowance mutation.
- **FR-004**: The system MUST prevent an exhausted learner from starting another eligible AI
  activity and preserve the existing allowance state.
- **FR-005**: Allowance state MUST be server-authoritative and must not be changed by client input
  or an `ADMIN` action.
- **FR-006**: Paid Premium activation, upgrade journeys, payment-provider integration, and manual
  entitlement/quota operations are out of scope for MVP.
- **FR-007**: Spring Boot MUST atomically reserve or reuse the idempotent AI usage event before an
  eligible F08 or F11 private `ai-service` call. Only Spring Boot MAY apply the resulting
  consumption or refund outcome to the learner's allowance. A schema-validated successful result
  consumes the reserved unit; any post-reservation timeout, unavailable service, invalid result, or
  safety rejection with no valid result MUST transition the event to `FAILED_REFUNDED` and restore
  exactly one unit. `FAILED_CONSUMED` has no MVP transition.

### Key Entities

- **Plan**: the published Free access and allowance policy for MVP.
- **Entitlement**: the automatically applied current access state for one learner.
- **AI usage event**: an idempotent record of one actual F08 assessment or one F11 assistant-reply
  request and its outcome.
- **Logical-request fingerprint**: an HMAC/SHA-256 value over the server-controlled feature and
  owned operation identifier; it contains no raw prompt, transcript, or audio.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of eligible active learners receive exactly one current Free access state.
- **SC-002**: 100% of eligible AI requests are accepted or declined using the learner's current
  allowance before AI work begins.
- **SC-003**: Retrying the same logical AI request never increases usage more than once.
- **SC-004**: No learner-facing Premium activation or upgrade journey is available in MVP.
- **SC-005**: 100% of post-reservation F08/F11 failures without a valid result refund exactly one
  allowance unit and never produce a successful learner result.
- **SC-006**: 100% of Shadowing navigation and AI Buddy conversation lifecycle actions consume zero
  allowance units.
- **SC-007**: 100% of locked-then-unlocked learner accounts retain the same Free entitlement and
  remaining allowance for their current cycle.
- **SC-008**: 100% of reused idempotency keys with a different logical-request fingerprint return
  no AI result, create no new usage event, and leave allowance unchanged.

## Assumptions

- F01 provides the verified, active learner identity.
- The Free entitlement cycle starts at `ai_quota_period_started_at`; each completed 30-day cycle
  resets its used-unit counter before the next eligible AI request is assessed.
- F01 validates an `ACTIVE` learner before F03 accesses entitlement or quota; an account lock is
  an access-state change, not an entitlement lifecycle event.
- F08 consumes one unit only for an actual pronunciation assessment, and F11 consumes one unit only
  for a learner message that requests an assistant reply. Both use this feature after Spring Boot's
  idempotent reservation; neither feature nor `ai-service` changes entitlement rules or allowance
  state directly.
