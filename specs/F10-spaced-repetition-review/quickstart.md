# Quickstart: F10 Spaced Repetition Review

## Prerequisites

- Start backend and frontend.
- Create a published F09 dictionary entry and save it as an authenticated learner.
- Provide a controllable backend clock in integration tests.

## Happy Path

1. Save a word for the first time and request the due queue.
2. Confirm the schedule appears in LEARNING and is due immediately.
3. Submit GOOD with a clientReviewId and current version.
4. Confirm the returned due date is server-calculated and an immutable event exists.
5. Retry the identical request and verify the same result returns without a second event.
6. Delete and re-save the word through F09 and confirm schedule/history remain preserved.

## Required Checks

- Due queue caps at 20 and orders by oldest dueAt.
- AGAIN, HARD, GOOD and EASY follow initial and established interval rules with ease clamping.
- A stale expected version yields 409 and does not write an event.
- Reusing clientReviewId with different content yields 409.
- Another learner cannot access or submit a schedule.
- SUSPENDED schedules do not enter the due queue.
- There are no calls to ai-service or F03 usage records for F10.

