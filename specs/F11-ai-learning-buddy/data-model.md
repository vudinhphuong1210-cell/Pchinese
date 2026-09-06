# Data Model: F11 AI Learning Buddy

## ai_conversations

| Column | Rules |
| --- | --- |
| id | UUID primary key |
| user_id | FK users; owner only |
| scenario | Immutable enum DAILY_CONVERSATION, VOCABULARY_GRAMMAR, ROLE_PLAY |
| title_encrypted | Optional encrypted learner-visible title |
| status | ACTIVE or DELETED |
| created_at, updated_at, deleted_at | Lifecycle timestamps |

Indexes: user_id plus status plus updated_at for conversation listing.

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
| status | PENDING, COMPLETE, FAILED, DELETED |
| client_request_id | Learner message UUID; unique per user |
| request_fingerprint | Detects changed idempotency retry |
| failure_code | Safe non-sensitive failure category |
| created_at, completed_at, updated_at | Lifecycle timestamps |

Constraints:

- unique conversation_id plus sequence_number;
- unique user_id plus client_request_id where client_request_id is not null;
- index conversation_id plus status plus sequence_number for bounded context reads.

## F03 Usage Association

Existing F03 ai_usage_events stores feature AI_BUDDY and a reference to the learner message or pending assistant response. A successful eligible learner request has at most one successful usage event. Reservation and final settlement are correlated by an immutable request reference.

## Retention and Deletion

Deleting a conversation removes it from public projections immediately and excludes all associated messages from context. Encrypted message content is retained or purged only under the project retention policy; ai-service retains no copy.

