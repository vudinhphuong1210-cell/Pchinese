# Data Model — F02 Profile and Session Control

## Reused canonical entities

| Entity | F02 fields/rules |
| --- | --- |
| `users` | display_name, native_language_code, interface_locale, time_zone, target_hsk_level, daily_goal_minutes and `version`; values are validated server-side and profile version detects conflict. |
| `auth_sessions` | owner, device label/platform, created/last seen, revoke state and session ID; owner-only projection identifies `currentSession`. |
| `refresh_tokens` | Revoked through its owning session/family; never returned to UI. |
| `auth_audit_events` | Records safe session revoke/security outcome without raw token or private profile data. |

## State and invariants

~~~text
profile read(version N) -> PATCH(expected N) -> saved version N+1
profile read(version N) -> concurrent update -> PATCH(expected N) => 409 + latest projection

active session -> revoke one/all/current-confirmed -> revoked terminal state
device limit -> restricted flow -> revoke one owned session -> flow ends -> learner retries login
~~~

The restricted flow is transient request state, not a database account session: it owns no profile,
role, entitlement or learner-content access and must never be restored after a page reload.

## Validation

- Profile request accepts only approved preference fields and never role/status/authz values.
- Session IDs and flow credentials are server-validated; absent/unowned sessions return safe 404.
- Current-session revoke requires `confirmCurrentSession=true`.
- No profile update on validation/state-conflict failure; no device-limit flow creates a normal login.
