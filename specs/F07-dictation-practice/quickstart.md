# Quickstart Validation — F07

1. Start from Player on current unlocked/completed segment and on future locked segment. Verify only
   permitted context creates/reuses attempt.
2. Submit answers differing only Unicode/space/punctuation: normalize equivalently. Verify correct
   Simplified final answer gets 100; incorrect/pinyin/Traditional final answer gets 0 and general
   retry guidance only.
3. Retry same clientSubmissionId concurrently: one evaluation/result. Complete a retake: new owned
   history and best score only rises.
4. Verify another learner and ADMIN cannot read/submit the attempt, expected answer is never exposed,
   and no ai-service request/AI usage event occurs.
5. Verify Dictation result changes only practice metric, not Player current segment/count/completion.
