# Quickstart Validation — F02

## Prerequisites

- F01 authentication/session lifecycle is available; run backend tests with a clean compatible DB
  and frontend Jest tests from the documented Maven/npm commands.
- Use two authenticated devices for one learner and a second learner for ownership checks.

## Validation scenarios

1. Read profile on device A, update on B, then submit A’s old version. Expect 409, no overwrite and
   reloadable latest projection.
2. List sessions as learner A. Expect only A’s privacy-safe devices and a current-device marker.
3. Revoke A’s non-current session; it immediately fails protected actions. Attempt an unowned ID and
   expect safe 404.
4. Revoke current session without/with confirmation. Expect no mutation first, then sign-out/login
   redirect after confirmed revoke. Verify logout-all revokes only A’s sessions.
5. Reach device limit, enter restricted flow, revoke one owned session and retry login. Verify the
   flow cannot read profile, survives neither page reload nor normal route navigation, and never
   issues an ordinary access session.

Check keyboard/focus handling in confirm dialogs, WCAG AA semantic tokens, 44px touch targets, safe
400/401/404/409 UI and absence of browser token storage.
