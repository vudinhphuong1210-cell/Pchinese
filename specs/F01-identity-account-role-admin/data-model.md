# Data Model — F01 Identity, Account, and Role Administration

This design uses `DATA_short.md` as the MVP schema authority. Names are PostgreSQL snake_case;
all IDs are UUIDs; timestamps are UTC `timestamptz`. No table stores a raw password, access JWT,
ordinary raw refresh token or provider credential.

## Relationship map

~~~text
users
 ├──< user_roles
 ├──< auth_sessions ──< refresh_tokens
 │                     └──< refresh_idempotency
 ├──< auth_action_tokens
 └──< auth_audit_events (actor, target and optional session references)
~~~

## Entities

### users

The account and basic-profile row. F01 uses identity/status/authz fields; profile-preference editing
is F02 scope.

| Field | Rule used by F01 |
| --- | --- |
| user_id | UUID primary key |
| email_ciphertext | Application-encrypted; never returned in F01 Admin projection |
| email_lookup_hash | Normalized email HMAC/SHA-256, unique lookup key |
| password_hash | bcrypt cost ≥12; never logged or returned |
| status | PENDING_VERIFICATION, ACTIVE, LOCKED or DISABLED |
| email_verified_at | Required before normal login/ACTIVE management target |
| authz_version | Monotonic authorization invalidation value, default 1 |
| version | Optimistic persistence version, default 0 |
| last_activity_at, created_at, updated_at | Server timestamps |

Indexes: unique `email_lookup_hash`; `(status, last_activity_at)`.

**State transitions**

~~~text
PENDING_VERIFICATION --valid verification token--> ACTIVE
ACTIVE --Admin lock--> LOCKED
LOCKED --Admin unlock--> ACTIVE (no old session becomes valid)
DISABLED --not an F01 command--> DISABLED
~~~

Only an ACTIVE verified account is a valid F01 Admin-command target. Locked/disabled/pending target
identifiers resolve as a safe unmanageable/not-found result.

### user_roles

Immutable ADMIN role-grant history.

| Field | Rule used by F01 |
| --- | --- |
| user_role_grant_id | UUID primary key |
| user_id | Required FK to users |
| role_code | ADMIN only in MVP |
| granted_by_user_id | Actor FK; null only for controlled bootstrap Admin |
| granted_at / revoked_at | A revoke timestamps the historical row; no hard delete |
| correlation_id | Required request correlation |

Constraint: partial unique `(user_id, role_code) WHERE revoked_at IS NULL`. Re-grant after a revoke
creates a new row. A transaction must reject self grant/revoke and any successful revoke/lock that
would leave no ACTIVE account with an active ADMIN role.

### auth_sessions

Revocable authenticated device session.

| Field | Rule used by F01 |
| --- | --- |
| session_id / family_id / user_id | UUID identity, refresh family and owner |
| authz_version | Snapshot at session issue |
| device_id | Validated installation/device identifier |
| device_label | Sanitized, maximum 120 characters |
| platform | WEB, IOS or ANDROID |
| ip_hash | Optional privacy-safe hash; no raw IP in normal response |
| created_at, last_seen_at | Server timestamps |
| idle_expires_at, absolute_expires_at | Server-controlled expiry |
| revoked_at, revoked_reason | Null while active; revocation is terminal |
| version | Lock/CAS support |

Indexes: `(user_id, revoked_at)` and `family_id`. These records support the server-managed
authentication lifecycle and security invalidation; they are not exposed for self-service management.

### refresh_tokens

Opaque refresh credential lineage.

| Field | Rule used by F01 |
| --- | --- |
| refresh_token_id / session_id / family_id | UUID identity and ownership |
| token_hash | Unique HMAC-SHA-256 with server-held pepper |
| issued_at / expires_at | Server timestamps |
| rotated_at / replaced_by_token_id | Rotation lineage |
| revoked_at / revoked_reason | Revoke/reuse state |
| version | Lock/CAS support |

Old hashes remain until absolute expiry only to detect reuse. The raw refresh value is never
persisted here.

### auth_action_tokens

Hashed, single-use authentication action credential.

| Field | Rule used by F01 |
| --- | --- |
| auth_action_token_id / user_id | UUID PK/FK |
| purpose | EMAIL_VERIFICATION or PASSWORD_RESET |
| token_hash | Unique server hash |
| issued_at / expires_at | Server-controlled validity |
| consumed_at / invalidated_at | Enforce one use and invalidation |
| request_ip_hash | Optional privacy-safe abuse signal |

Expired action tokens are removed by the existing governed worker policy. Public request endpoints
remain neutral whether a usable account/token exists.

### refresh_idempotency

Short-lived safe replay for a single refresh retry.

| Field | Rule used by F01 |
| --- | --- |
| refresh_idempotency_id | UUID primary key |
| session_id / source_refresh_token_id | FKs to the exact rotated lineage |
| refresh_request_id | Client UUID request identifier |
| response_ciphertext | KMS-encrypted response; sole temporary raw-refresh exception |
| expires_at | At most 30 seconds after creation |
| created_at | Server timestamp |

Constraint: unique `(session_id, refresh_request_id)`. It is deleted at expiry. A different
request ID applied to a rotated/revoked token is reuse, not an idempotent retry.

### auth_audit_events

Append-only security evidence for auth/session/role/account access actions.

| Field | Rule used by F01 |
| --- | --- |
| auth_audit_event_id / event_type | UUID and safe event enum, including LOGIN, REFRESH_REUSE, ROLE_GRANTED, ROLE_REVOKED, ACCOUNT_LOCKED, ACCOUNT_UNLOCKED |
| actor_user_id / target_user_id / session_id | Optional FKs needed for attributable outcome |
| before_roles / after_roles | Required for successful role lifecycle change |
| correlation_id / occurred_at | Required traceability |
| details | Safe bounded metadata: reason, optional OTHER note, device context and accepted/rejected outcome only |

Audit details must exclude passwords, token values, raw email, private learner data, recording/chat
content and provider information.

## Transaction boundaries and invariants

| Operation | Transaction/invariant |
| --- | --- |
| Register | Normalize/hash email; create one PENDING_VERIFICATION user and action token only when new; return neutral result regardless of duplicate/ineligible lookup. |
| Verify email | Lock/validate unused unexpired verification token; consume once; transition owner to ACTIVE; audit safe outcome. |
| Login | Authenticate ACTIVE verified user; create one session/family and hashed refresh token atomically. |
| Refresh | Lock refresh/session record; rotate once or replay same request ID ≤30s; distinct reuse revokes family and audits. |
| Logout | Revoke only the current refresh-session family from the presented credential. |
| Password reset confirm | Consume valid reset token; replace bcrypt hash, increment authz version, revoke every active session/family and audit atomically. |
| Role grant/revoke | Lock target + active-role set; verify ADMIN actor, active target, no self/final-admin violation; write immutable role history, update authz version, revoke target sessions and audit. |
| Lock/unlock | Lock target account; require valid reason and OTHER note; reject self/final-active-ADMIN removal; lock increments authz version/revokes sessions; unlock never restores sessions; audit outcome. |

## API projections

| Projection | Fields allowed |
| --- | --- |
| Auth session response | Access-session metadata; raw refresh only through protected platform mechanism, never normal JSON persistence/logging |
| Admin account-management projection | Exact target ID, active ADMIN role state and locked/unlocked access state only |
| Public error | Stable code, safe message, correlation ID; no account-existence, target-private or credential detail |
