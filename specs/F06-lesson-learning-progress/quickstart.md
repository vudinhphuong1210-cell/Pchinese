# Quickstart Validation — F06

1. Visit catalog/detail: no progress row. Enter permitted Player twice/concurrently: exactly one
   NOT_STARTED row with first segment current.
2. Verify guest/no-access learner cannot get playback; owner sees only ordered current/completed
   content and approved metadata.
3. Send valid progress, rewind and attempted forward seek. Watermark grows only validly; forward
   skip is denied.
4. Send ENDED before end and then valid end. First is denied; valid end opens exactly one next
   segment. Concurrent duplicate ends do not double count.
5. Complete final segment: expect COMPLETED, 100 percent and no next segment. Revisit completed
   segment without state change.
6. Submit simulated F07/F08 approved metrics: best-score may change but current/count/status remain.
   Withdraw content and verify next access is blocked while progress is retained.
