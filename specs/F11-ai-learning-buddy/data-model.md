# Data Model: F11 AI Learning Buddy

## user_data_keys

| Column | Rules |
| --- | --- |
| user_id | Primary key and FK to users; exactly one active user data-encryption key (DEK) per learner |
| wrapped_user_dek | User DEK wrapped only by the deployment-managed KMS key named by `kms_key_reference`; no plaintext key is stored or logged |
| kms_key_reference | Deployment KMS key identifier/version used for unwrapping; allows rotation without changing learner data |
| status, destroyed_at | ACTIVE or DESTROYED; account-level erasure destroys this envelope and records the timestamp |
| created_at | Key lifecycle timestamp |

The KMS envelope client unwraps this key only inside Spring Boot. `ai-service` never receives a DEK, a
wrapped key, or any KMS credential.

## ai_conversations

| Column | Rules |
| --- | --- |
| id | UUID primary key |
| user_id | FK users; owner only |
| scenario | Immutable enum DAILY_CONVERSATION, VOCABULARY_GRAMMAR, ROLE_PLAY |
| title_encrypted | Optional encrypted learner-visible title |
| wrapped_conversation_dek | Random per-conversation DEK, wrapped by the active user DEK; encrypts this conversation title and all of its message fields |
| status | ACTIVE or DELETED |
| last_message_at | Timestamp of the most recent COMPLETE message pair; null for an empty conversation and not changed by rename/list/view |
| hard_delete_after, hard_delete_status | Durable database-backed deletion work: PENDING, PROCESSING, COMPLETED, or BLOCKED_LEGAL_HOLD; this replaces any queue-service assumption |
| conversation_key_destroyed_at | Timestamp at which the conversation DEK envelope was destroyed; makes a deleted chat unreadable before physical row deletion |
| created_at, updated_at, deleted_at | Lifecycle timestamps |

Indexes: user_id plus status plus updated_at for conversation listing; status plus last_message_at and hard_delete_after plus hard_delete_status for the daily retention sweep.

## ai_messages

| Column | Rules |
| --- | --- |
| id | UUID primary key |
| conversation_id, user_id | Enforces same owner path |
| sequence_number | Unique ordered value per conversation |
| sender | LEARNER or ASSISTANT |
| content_encrypted | Encrypted bounded plain text |
| vietnamese_explanation_encrypted | Assistant-only nullable encrypted concise explanation |
| suggestion_encrypted | Assistant-only nullable encrypted single suggestion |
| status | PENDING, COMPLETE, FAILED, DELETED; PENDING is internal-only and is never exposed by the public API |
| client_request_id | Learner message UUID; unique per user |
| request_fingerprint | Detects changed idempotency retry |
| processing_deadline_at | Immutable initial-request deadline; a same-fingerprint repeat joins the existing operation only until this timestamp |
| failure_code | Safe non-sensitive failure category |
| created_at, completed_at, updated_at | Lifecycle timestamps |

Constraints:

- unique conversation_id plus sequence_number;
- unique user_id plus client_request_id where client_request_id is not null;
- index conversation_id plus status plus sequence_number for bounded context reads.

## ai_processing_audit_events

| Column | Rules |
| --- | --- |
| user_id, ai_conversation_id, correlation_id | Trace a permitted processing event without storing raw learner content, prompt, browser token, provider response or credential |
| provider_code, model_code, transfer_region | Deployment-approved provider/model/region snapshot used for the request; never client-supplied |
| event_type | REQUEST_DISPATCHED, RESPONSE_RECEIVED, REQUEST_REJECTED, or DELETION_ATTESTED |
| outcome, safe_reason_code | SUCCESS, REJECTED, or FAILED plus a safe non-sensitive code only |
| input_policy_version, output_policy_version | Active versioned safety rules used for the decision; no learner content |
| provider_request_reference_hash | Optional one-way reference supplied by the provider; no raw vendor request ID is exposed |
| occurred_at | Append-only event timestamp |

The database audit record is written by Spring Boot. `ai-service` remains stateless and emits only
redacted structured telemetry required to create it.

## ai_retention_notices

| Column | Rules |
| --- | --- |
| ai_retention_notice_id, ai_conversation_id | UUID primary key and FK; unique conversation plus notice type prevents duplicates |
| notice_type, idempotency_key | Fixed `INACTIVITY_30_DAYS` and deterministic `conversationId:INACTIVITY_30_DAYS` key |
| status, attempt_count | PENDING, SENDING, SENT, or FAILED; failed delivery can be retried safely by the mail adapter |
| last_attempt_at, sent_at, created_at, updated_at | Delivery lifecycle only; never stores recipient address or chat content |

The configured mail adapter must receive the idempotency key. The record represents one notice
intent and safely retried delivery, not a claim that SMTP itself provides exactly-once delivery.

## ai_conversation_legal_holds

| Column | Rules |
| --- | --- |
| ai_conversation_legal_hold_id, ai_conversation_id | UUID primary key and FK; one active hold per conversation |
| hold_reason_code, created_by_reference | Content-free legal/privacy workflow metadata; never chat text and never a learner/admin public API |
| held_at, released_at | A non-null `held_at` and null `released_at` blocks only the affected conversation's hard deletion |

Only the documented privacy/legal operational workflow creates or releases a hold. It has no API or
database projection for reading conversation content and does not grant `ADMIN` chat access.

## F03 Usage Association

Existing F03 ai_usage_events stores feature AI_BUDDY and a reference to the learner message or pending assistant response. A successful eligible learner request has at most one successful usage event. Reservation and final settlement are correlated by an immutable request reference.

## Retention and Deletion

AI conversations and messages are retained until the learner deletes them or 12 months after `last_message_at`; an empty conversation uses `created_at` as its retention anchor. The daily backend retention job creates one `ai_retention_notices` record 30 days before expiry, gives its idempotency key to the configured mail adapter, and safely retries failed delivery. At expiry or learner deletion it immediately hides the conversation, excludes all messages from context, destroys the wrapped conversation DEK, writes a deletion-attestation audit event, and creates database-backed hard-delete work. The job processes that work idempotently; it does not introduce a queue or cache service. An active `ai_conversation_legal_holds` record blocks only the affected hard delete. Renaming, listing, and viewing do not extend retention; ai-service retains no copy.

## Provider Privacy Gate

AI Buddy runs only for the required `SERVICE_OPERATION` purpose. `AI_MODEL_IMPROVEMENT` remains off:
the direct DeepSeek provider receives minimized pseudonymous content only, cannot train on it, and
is called through `@ai-sdk/deepseek` with deployment-only `DEEPSEEK_API_KEY` and a V4
`DEEPSEEK_MODEL` (`deepseek-v4-flash` or `deepseek-v4-pro`). It must have a deployment-approved DPA,
approved transfer region, encrypted transport, vendor retention of at most 24 hours,
deletion/attestation capability, and auditable request handling. Spring Boot and `ai-service` refuse
to serve when this provider policy is incomplete.
