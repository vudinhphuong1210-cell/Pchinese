# Quickstart: F09 Dictionary and Personal Vocabulary

## Prerequisites

- Start backend and frontend.
- Seed published dictionary entries with Simplified, Traditional, pinyin and Vietnamese senses; include one withdrawn entry and an unavailable media asset.
- Have an authenticated learner with F10 available.

## Happy Path

1. Search the same word in Simplified, Traditional, pinyin without tones and a Vietnamese keyword.
2. Open a published entry and confirm its available media/senses render.
3. Save it with a private note; observe an immediate F10 LEARNING schedule.
4. Delete the saved word and confirm it leaves the active and due lists.
5. Save it again and confirm the same saved-word identity and schedule/history are restored.

## Required Checks

- A withdrawn entry returns 404 and its content never leaks from a saved list.
- Another learner cannot read, edit or delete a saved word.
- Invalid or oversized notes are rejected without mutation.
- The four supported search forms yield the intended entry and page size is capped.
- Unavailable media is omitted, not represented by a broken private URL.
- Delete and restore do not create duplicated saved words or SRS schedules.

