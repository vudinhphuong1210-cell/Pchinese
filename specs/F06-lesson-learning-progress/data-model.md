# Data Model — F06

| Field/group | Invariant |
| --- | --- |
| lesson_progresses user_id + lesson_id | Unique authoritative aggregate |
| status | NOT_STARTED -> IN_PROGRESS -> COMPLETED only |
| current_segment_id | First ordered published segment at creation; next contiguous segment after accepted completion; null at final completion |
| completed_segment_count/total_segment_count/completion_percent | Server-computed, final state only 100 percent |
| current_segment_playback_ms/max_played_ms | Current watermark; may rewind but not seek above max |
| dictation_best_score/shadowing_best_score | Practice metrics only; never unlock/completion input |
| version | Optimistic/lock concurrency guard |

~~~text
first permitted Player entry => NOT_STARTED, count 0, first segment current
validated progress > 0 => IN_PROGRESS
validated ENDED current => complete once, unlock next or COMPLETED 100 percent
late concurrent ENDED => latest state, no second transition
~~~
