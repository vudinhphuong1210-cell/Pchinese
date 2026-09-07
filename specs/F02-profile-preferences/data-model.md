# Data Model — F02 Profile Preferences

## Reused canonical entities

| Entity | F02 fields/rules |
| --- | --- |
| `users` | display_name, native_language_code, interface_locale, time_zone, target_hsk_level, daily_goal_minutes and `version`; values are validated server-side and profile version detects conflict. |

## State and invariants

~~~text
profile read(version N) -> PATCH(expected N) -> saved version N+1
profile read(version N) -> concurrent update -> PATCH(expected N) => 409 + latest projection
~~~

## Validation

- Profile request accepts only approved preference fields and never role/status/authz values.
- No profile update occurs on validation or state-conflict failure.
