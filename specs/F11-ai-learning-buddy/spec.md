# Feature Specification: AI Learning Buddy

**Feature Branch**: `[F11-ai-learning-buddy]`
**Created**: 2026-09-05
**Status**: Draft
**Input**: F11 from the MVP Feature Map.

## Purpose

AI Learning Buddy gives each learner a private, guided space to practise Chinese. It supports only
three learning activities: daily conversation, vocabulary or grammar explanation, and role-play.
It is not a general-purpose chat feature.

The feature protects learner privacy, applies the existing Free AI allowance, refuses unsafe or
out-of-scope content before it is processed, and lets learners control the lifetime of their own
conversation history.

## Clarified Product Decisions

- A learner chooses one of the three supported Chinese-learning activities when creating a
  conversation. The choice remains fixed for that conversation.
- Messages may be written in Vietnamese or Chinese. They must be plain text and no longer than
  1,000 characters.
- A suitable reply contains Chinese, a concise Vietnamese explanation, and at most one suggestion
  for the learner's next practice.
- Content that is unsafe, ambiguous, or outside the selected learning activity is declined with
  safe guidance. It is neither saved nor charged.
- A learner can make at most five send attempts in a rolling one-minute period. Retrying the same
  logical message is not a new attempt and must not create an additional charge or reply.
- A normal send action completes with either a reply or a safe final result within 30 seconds. The
  learner never has to poll for a pending result.
- A reply receives only the selected activity, the current valid message, and at most ten recent
  completed messages from the same conversation.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hold a private learning conversation (Priority: P1)

As a learner, I want to create and continue my own AI learning conversation so that I can practise
Chinese in a guided setting.

**Why this priority**: This is the learner-facing use of the approved Free AI allowance.

**Independent Test**: A learner creates an owned conversation without spending allowance, sends a
permitted message, receives one reply for one allowance unit, and sees only their own history.

**Acceptance Scenarios**:

1. **Given** a signed-in learner, **When** they create a conversation, **Then** the empty
   conversation is visible only to that learner and consumes no allowance.
2. **Given** remaining Free AI allowance and an owned active conversation, **When** the learner
   sends a permitted message, **Then** one assistant reply is visible only to that learner and one
   allowance unit is consumed.
3. **Given** no remaining allowance, **When** the learner starts an AI action, **Then** the action
   is declined before processing begins and the conversation remains intact.
4. **Given** a learner creates a conversation, **When** they select daily conversation,
   vocabulary/grammar explanation, or role-play, **Then** the conversation remains within that
   selected Chinese-learning activity.
5. **Given** a message is unsafe, ambiguous, or outside the selected activity, **When** the learner
   submits it, **Then** the learner receives safe guidance and no message, reply, or allowance
   change is created.
6. **Given** a learner submits a suitable Chinese-learning request in Vietnamese or Chinese,
   **When** it matches the selected activity, **Then** it can proceed. General chat, attempts to
   alter the assistant's instructions, personal data, sexual content, violence or self-harm, hate,
   and illegal activity are declined.
7. **Given** a learner has made five send attempts in the preceding minute, **When** they make a
   sixth attempt, **Then** the learner receives clear retry guidance and no processing, allowance
   change, or message is created. Repeating an existing logical message does not count as a new
   attempt.
8. **Given** a permitted message, **When** it is processed, **Then** only the current message, its
   selected activity, and no more than ten recent completed messages from the same conversation are
   used as context.
9. **Given** a learner submits suitable plain text of no more than 1,000 characters, **When** it
   matches the selected activity, **Then** it is eligible for the normal AI Buddy flow.
10. **Given** a valid assistant result, **When** the learner views it, **Then** it contains a
    Chinese response, a concise Vietnamese explanation, and at most one next-practice suggestion.
11. **Given** a learner submits an eligible message, **When** the action completes, **Then** within
    30 seconds they receive either one completed learner/assistant pair or a safe final result, and
    any reserved allowance is restored on failure.
12. **Given** an internal AI-processing request is not trustworthy or is no longer current,
    **When** it is received, **Then** it is rejected before any AI work begins.
13. **Given** a permitted request reaches the AI-processing component, **When** its input is unsafe
    or invalid, **Then** it is declined with a safe trace reference and produces no AI work.
14. **Given** the AI-processing component receives a result, **When** the result is invalid or
    unsafe, **Then** it is declined with a safe trace reference and is never saved as a reply.
15. **Given** a permitted request is processed, **When** the system records the event, **Then** it
    records only pseudonymous operational metadata and never records learner content, prompts,
    credentials, or full third-party responses.
16. **Given** a conversation is deleted or expires, **When** deletion begins, **Then** access to
    its content is irreversibly removed before permanent deletion work and no other learner's
    conversation is affected.

---

### User Story 2 - Control conversation history (Priority: P1)

As a learner, I want to rename or delete my own conversation so that I control my learning history.

**Why this priority**: Chat content is private learner data and requires explicit ownership control.

**Independent Test**: A learner renames and deletes one owned conversation; another learner and an
administrator cannot access or perform either action.

**Acceptance Scenarios**:

1. **Given** an owned conversation, **When** the learner renames it, **Then** the new title appears
   only in their own conversation list.
2. **Given** an owned conversation, **When** the learner deletes it, **Then** it is immediately
   hidden, excluded from future context, and queued for permanent deletion under the chat-retention
   lifecycle.

---

### Edge Cases

- Invalid, oversized, unsafe, or rate-limited input gives a recoverable result without exposing
  internal instructions or third-party information.
- A conversation cannot become a general-purpose chat or change activity; the learner creates a
  new conversation for another approved activity.
- Repeating one logical message cannot consume allowance or create two assistant replies.
- Context never includes another conversation's messages or messages that failed, are pending, or
  were deleted.
- Rich text and attachments are rejected before processing, charging, or persistence.
- A reply missing the required Chinese response or Vietnamese explanation, or containing more than
  one suggestion, is not saved.
- Other learners, administrators, and external AI providers do not gain general access to private
  conversation history.

## Requirements *(mandatory)*

- **FR-001**: Learners MUST be able to create, list, view, rename, and delete only their own AI
  learning conversations.
- **FR-002**: Learners MUST be able to send permitted messages only to an owned active
  conversation.
- **FR-003**: The system MUST enforce the available Free AI allowance, a maximum of five AI Buddy
  send attempts per learner in a rolling one-minute window, and input safeguards before a reply is
  created. A repeated logical message MUST not count as a new attempt. Conversation management
  actions MUST NOT consume allowance.
- **FR-004**: Repeating one logical message MUST never create more than one allowance charge or
  more than one assistant reply. A same-fingerprint repeat while the original request is in
  progress MUST join that request and wait only until its original 30-second terminal deadline;
  it receives the same completed pair or safe terminal failure, never a public pending state. A
  changed message under the same logical-message identifier MUST be declined as a conflict.
- **FR-005**: Conversation content MUST follow learner ownership, minimisation, retention, and
  deletion requirements. Administrators have no private-chat access. Conversations and messages are
  retained until learner deletion or 12 months after the last chat activity, with one notice 30 days
  before inactivity deletion. Learner deletion MUST immediately hide the conversation and create
  durable, database-backed permanent-removal work; it MUST NOT introduce a queue service. Only an
  active legal hold, created or released by the documented privacy/legal operational workflow, may
  defer removal. A legal hold grants neither administrators nor the hold workflow access to chat
  content.
- **FR-006**: The learner-facing experience MUST not expose credentials, prompts, or internal
  failure details.
- **FR-007**: AI processing MUST start only after identity, ownership, message suitability, rate
  limits, allowance, and duplicate-message safeguards have been confirmed.
- **FR-008**: AI processing MUST receive only the minimum valid context through a protected,
  authenticated internal boundary. It MUST reject untrusted, altered, stale, or replayed requests
  before AI work begins; independently validate input and output; return a safe trace reference on
  rejection or failure; and retain no learner conversation data or product decisions.
- **FR-009**: The system MUST validate an AI result before saving it. An invalid result or an
  unavailable AI-processing component MUST produce a safe final result, without a duplicate charge
  or third-party detail.
- **FR-010**: A learner MUST select exactly one immutable Chinese-learning activity when creating
  a conversation: daily conversation, vocabulary/grammar explanation, or role-play. AI Buddy MUST
  support only that activity and MUST NOT operate as general-purpose chat.
- **FR-011**: The system MUST use versioned, deterministic safety and activity rules to decline a
  message outside the selected activity, an unsafe message, or an ambiguous message before any AI
  work, allowance change, or message persistence. Permitted content is Chinese learning in
  Vietnamese or Chinese. Declined categories include general chat, attempts to alter internal
  instructions, personal data, sexual content, violence or self-harm, hate, and illegal activity.
  The learner receives only safe guidance. The active policy version MUST be recorded as safe audit
  metadata for each local rejection and permitted provider dispatch.
- **FR-012**: For an eligible request, processing MUST receive only a trace reference, a
  duplicate-message reference, the fixed AI Buddy capability, the selected activity, the current
  valid learner message, and at most ten recent completed messages from the same conversation. It
  MUST exclude authentication credentials, allowance decisions, product records, other
  conversations, and messages that are pending, failed, or deleted.
- **FR-013**: A learner AI Buddy message MUST be plain text of at most 1,000 characters and MUST
  NOT include rich text or an attachment. An invalid message MUST be declined before AI work,
  allowance change, or message persistence.
- **FR-014**: A saved assistant reply MUST contain a Chinese response, a concise Vietnamese
  explanation, and at most one next-practice suggestion. Output policy version 1 rejects a reply
  containing prompt or system-instruction disclosure, credentials, personal data, sexual content,
  violence or self-harm, hate, illegal-instruction content, or a missing/oversized required field.
  Invalid, unsafe, or malformed provider output MUST return `502 PROVIDER_ERROR`, must not be
  persisted, and MUST follow the exactly-once allowance-restoration policy.
- **FR-015**: A learner's send-message action MUST complete within 30 seconds with either a
  completed learner/assistant pair or a safe final result and exactly-once allowance restoration on
  failure. A concurrent same-fingerprint retry observes the original request's terminal result by
  the original deadline; timeout/unavailability returns `503 SERVICE_UNAVAILABLE`. The learner
  experience MUST NOT require polling for a pending result.
- **FR-016**: Every internal AI-processing request MUST be integrity-protected with a
  deployment-held secret and include freshness and one-time-use checks that are bound to the exact
  request content. A request that fails these checks MUST be rejected.
- **FR-017**: Every internal AI-processing request and outcome MUST carry a trace reference. Safe
  outcomes MUST not disclose third-party, prompt, credential, or learner-content detail.
- **FR-018**: Internal processing MUST reserve sufficient time for validation, safe persistence,
  and a final learner result within the 30-second public limit. It MUST not automatically repeat a
  third-party AI request.
- **FR-019**: AI Buddy processing MUST be used only to provide the requested service, never to
  improve an AI model. It MUST use an approved provider and an approved model, with an approved data
  processing agreement, encrypted transfer, a no-training commitment, vendor retention no longer
  than 24 hours, deletion evidence, and an approved data-transfer region. Processing MUST fail
  safely when any of these conditions is absent.
- **FR-020**: AI chat fields MUST be encrypted with an independently protected key for each
  conversation. Plaintext protection keys and key-management credentials MUST never be persisted,
  logged, sent to the AI processor, or exposed to the learner-facing experience. Deletion MUST
  irreversibly remove access to the affected conversation's protection key only.
- **FR-021**: The system MUST append a content-free processing audit event for approved processing,
  safe outcomes, failures, local safety decisions, and deletion evidence. It may contain a trace
  reference, active input/output policy version, approved provider/model/region, outcome, safe
  reason, and a hashed third-party reference; it MUST NOT contain learner content, prompts,
  credentials, authentication tokens, request proofs, or full provider bodies.

### Key Entities

- **AI conversation**: a learner-owned learning history with a title, selected activity, and
  lifecycle.
- **AI message**: ordered learner or assistant content belonging to one owned conversation.
- **AI usage event**: an allowance record for one logical AI Buddy request.
- **AI learning activity**: the immutable Chinese-learning purpose selected for a conversation.
- **Local safe rejection**: non-persisted, non-billable guidance for a message that is unsafe or
  outside its selected activity.
- **Safety and activity policy**: versioned rules that identify clearly permitted Chinese-learning
  content and decline ambiguous or prohibited content.
- **Minimised AI context**: the selected activity, current valid message, and at most ten completed
  messages from the same conversation, plus the minimal trace and duplicate-message references.
- **Eligible learner message**: a safe, in-activity plain-text learner message of at most 1,000
  characters and without an attachment.
- **Bilingual learning reply**: a valid assistant result containing a Chinese response, a concise
  Vietnamese explanation, and at most one next-practice suggestion.
- **AI processing audit event**: an append-only, content-free record of approved processing or
  deletion evidence.
- **Deletion work record**: durable database state for a hard-delete attempt; it is not a queue
  service and is idempotently processed by the daily retention job.
- **Retention notice record**: a per-conversation, idempotency-keyed delivery record for the single
  30-day inactivity notice.
- **Legal hold**: content-free operational metadata that pauses only the affected conversation's
  retention deletion. It is managed outside learner and administrator chat APIs.

## Success Criteria *(mandatory)*

- **SC-001**: 100% of conversation and message operations are restricted to the owning learner.
- **SC-002**: Under normal conditions, a learner with remaining allowance receives a completed
  result or safe final result in under 30 seconds, without polling.
- **SC-003**: Exhausted, unsafe, rate-limited, invalid, or repeated requests create neither an
  unintended allowance charge nor a duplicate assistant reply.
- **SC-004**: Deleting a conversation immediately removes it from the learner's active history.
- **SC-005**: 100% of AI Buddy requests use only the defined minimal context. Invalid, unsafe, or
  unavailable processing results never create a saved assistant message or duplicate allowance
  consumption.
- **SC-006**: 100% of AI Buddy conversations have exactly one approved Chinese-learning activity,
  and no assistant reply is saved for a general-purpose conversation.
- **SC-007**: 100% of unsafe, ambiguous, or out-of-activity messages create no AI work, allowance
  event, learner message, or assistant message.
- **SC-008**: 100% of AI Buddy requests use no more than ten completed prior messages, all from the
  same conversation.
- **SC-009**: 100% of learner messages exceeding 1,000 characters or containing rich text or an
  attachment are declined before AI work, allowance change, or message persistence.
- **SC-010**: 100% of saved assistant replies contain the required Chinese response and concise
  Vietnamese explanation, with no more than one next-practice suggestion, and pass output-policy
  version 1 before persistence.
- **SC-011**: 100% of messages declined by the active safety and activity policy create no AI work,
  allowance event, learner message, or assistant message.
- **SC-012**: Policy verification accepts in-activity Chinese-learning requests in Vietnamese and
  Chinese, and declines the prohibited categories before AI work or allowance change.
- **SC-013**: 100% of a learner's sixth AI Buddy send attempt within a rolling minute gives retry
  guidance and creates no AI work, allowance event, learner message, or assistant message; a repeat
  of the same logical message does not count as a new attempt.
- **SC-014**: 100% of protected internal requests that are missing, invalid, stale, altered, or
  replayed are rejected before AI work begins.
- **SC-015**: 100% of invalid or unsafe internal input and output is rejected before a response is
  saved; each safe internal failure carries a trace reference and no learner-private or third-party
  detail.
- **SC-016**: 100% of timeout cases, including concurrent same-fingerprint retries, return the
  original request's safe final learner result within its 30-second deadline and restore the
  allowance exactly once.
- **SC-017**: The daily retention process creates exactly one idempotency-keyed 30-day inactivity
  notice record for each eligible conversation, retries failed delivery safely, and creates
  database-backed deletion work after the retention deadline unless an active legal hold exists.
- **SC-018**: 100% of protected internal requests reject a changed request identity, freshness
  value, one-time-use value, integrity proof, or exact content.
- **SC-019**: 100% of provider-configuration checks prevent processing when the approved provider,
  model, data-processing safeguards, transfer region, or deletion evidence is absent; processing
  audit records contain no learner content.
- **SC-020**: 100% of conversation-deletion checks remove access only to the affected conversation
  and leave other active conversations readable.

## Assumptions

- F01 provides learner identity and F03 provides the MVP Free AI allowance.
- AI Buddy does not grant administrators, teachers, or other learners access to private
  conversations.
- The AI-processing component is private and stateless for learner data; product records,
  allowance, deletion, and learner-facing outcomes remain under the product's control.
- Voice assessment belongs to F08 and is not part of AI Buddy.
- Changing a learning activity requires a new conversation.
- Safety and activity rejection occurs before allowance use or AI processing and does not become
  part of the conversation history.
- Retention uses the most recent chat activity, or the creation time when a conversation is empty.
  Viewing, listing, and renaming do not extend retention.
- The privacy/legal operational workflow is the only hold authority. It stores content-free hold
  metadata, cannot read chat content, and exposes no learner or administrator hold-management API in
  F11.
- The approved provider and model, its data-processing commitments, the approved transfer region,
  and the encryption-key reference are controlled deployment decisions. No request is processed
  until the conditions in FR-019 are met.
