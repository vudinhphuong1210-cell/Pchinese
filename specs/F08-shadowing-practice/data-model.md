# Data Model: F08 Shadowing Practice

## New Tables

### shadowing_recordings

| Column | Rules |
| --- | --- |
| id | UUID primary key |
| user_id | FK users; owner only |
| lesson_id, segment_id | Valid unlocked lesson segment at upload time |
| storage_reference | Encrypted private storage locator; never exposed to client |
| content_type, byte_size, duration_ms | Validated server metadata |
| status | UPLOADING, SCANNING, AVAILABLE, QUARANTINED, DELETED |
| scan_completed_at, expires_at, deleted_at | Lifecycle and retention timestamps |
| created_at, updated_at | Audit timestamps |

Indexes: user_id plus created_at; segment_id; status plus expires_at for cleanup.

### shadowing_attempts

| Column | Rules |
| --- | --- |
| id | UUID primary key |
| user_id, lesson_id, segment_id | Owner and learning context |
| recording_id | Unique FK shadowing_recordings |
| client_request_id | UUID; unique per user |
| request_fingerprint | Detects idempotency-key reuse with a changed request |
| status | PROCESSING, COMPLETED, FAILED, EXPIRED |
| overall_score | Nullable until completion; bounded 0 through 100 |
| pronunciation_score, rhythm_score, fluency_score | Nullable bounded dimensions |
| feedback_encrypted | Encrypted bounded assessment feedback; no transcript |
| provider_request_id | Operational correlation only |
| failure_code | Safe terminal error category |
| completed_at, expires_at, created_at, updated_at | Lifecycle timestamps |

### ai_usage_events Integration

Use the existing F03 ledger with feature value SHADOWING_ASSESSMENT and reference to shadowing_attempts.id. Exactly one successful usage event can exist per completed eligible attempt.

## State Transitions

    recording: UPLOADING -> SCANNING -> AVAILABLE
                                      -> QUARANTINED -> DELETED
                                      -> DELETED
    attempt: PROCESSING -> COMPLETED
                         -> FAILED
                         -> EXPIRED

Deletion or expiry removes the storage object and clears its encrypted locator. Attempts keep only the minimal permitted assessment audit data after recording deletion.

