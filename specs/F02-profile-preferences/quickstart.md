# Quickstart Validation — F02

## Prerequisites

- F01 authentication is available; run backend tests with a clean compatible DB
  and frontend Jest tests from the documented Maven/npm commands.
- Use two authenticated learners for ownership checks.

## Validation scenarios

1. Read profile on device A, update on B, then submit A’s old version. Expect 409, no overwrite and
   reloadable latest projection.
2. Read or update learner B’s profile using learner A’s credentials. Expect an authorization-safe
   result and no exposure or change to learner B’s preferences.

Check keyboard/focus handling, WCAG AA semantic tokens, 44px touch targets, safe 400/401/409 UI and
absence of browser token storage.
