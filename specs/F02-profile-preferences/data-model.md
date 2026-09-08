# Data Model — F02 Profile Preferences

## Reused canonical entities

| Entity | F02 fields/rules |
| --- | --- |
| `users` | `display_name_ciphertext`, native_language_code, interface_locale, time_zone, target_hsk_level, daily_goal_minutes and `version`; display name is encrypted at rest, values are validated server-side and profile version detects conflict. |
| `auth_audit_events` | Existing append-only audit records. F02 writes `PROFILE_PREFERENCES_UPDATED` with the authenticated owner as actor/target, current session when available, and only an allowlisted array of changed-field codes. Only ADMIN reads all events as a safe, paginated projection of event type, occurrence time and permitted actor/target account-name labels. |

## Logical audit-event organization

This is an organization policy over the existing canonical `auth_audit_events` columns and safe
`details` JSON; it does not introduce a new entity, column or migration.

| Concern | Stored or derived representation | Visibility |
| --- | --- | --- |
| What happened | Existing approved `event_type`, centrally mapped to one approved category: `AUTHENTICATION`, `ACCOUNT_LIFECYCLE`, `ACCESS_CONTROL`, `PROFILE_CHANGE`, or `SECURITY_OPERATION`. | ADMIN receives only `eventType`; category is internal. |
| Who/what | Existing `actor_user_id` and applicable `target_user_id`; a system event has a neutral system actor. | ADMIN receives only permitted account-name labels, never identifiers. |
| Result | Bounded `outcomeCode` in safe details, such as `SUCCESS`, `DENIED` or `FAILED`. | Private audit metadata. |
| Reason/change summary | Optional bounded `reasonCode` and allowlisted `changedFieldCodes` in safe details. For profile updates, changed-field codes identify fields only. | Private audit metadata. |
| Operational trace | Existing `occurred_at`, `session_id` and `correlation_id`. | Time is public to ADMIN; session/correlation are private incident trace. |

`details` MUST NOT contain arbitrary keys or values. It excludes passwords, credentials, access or
refresh tokens, email, IP addresses, raw user-agent data, old/new profile values and unrestricted
free-text notes.

## State and invariants

~~~text
profile read(version N) -> PATCH(expected N) -> saved version N+1
profile read(version N) -> concurrent update -> PATCH(expected N) => 409 + latest projection
profile PATCH changes fields -> user row update + one minimized audit event -> both commit or both roll back
approved audit command -> taxonomy/allowlist validation -> immutable audit event
~~~

## Validation

- Profile request accepts only approved preference fields and never role/status/authz values.
- No profile update occurs on validation or state-conflict failure.
- Activity rows are never edited or deleted by F02. Returned ADMIN activity DTOs exclude `details`,
  `before_roles`, `after_roles`, UUIDs, session/correlation identifiers and profile values; account
  labels use the same permitted display-name rule as the ADMIN user directory.
- Audit writers may use only centrally approved event types, outcome/reason codes and changed-field
  codes. They do not accept a caller-supplied category, raw narrative, prior/new value or protected
  trace value as audit detail.
