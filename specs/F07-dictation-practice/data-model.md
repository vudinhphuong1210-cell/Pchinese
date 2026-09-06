# Data Model — F07

| Entity/field | Invariant |
| --- | --- |
| dictation_attempts owner/lesson/segment | Composite relation guarantees segment belongs to lesson; owner-only read |
| status | IN_PROGRESS -> SUBMITTED -> EVALUATED; FAILED/DELETED retain governed lifecycle |
| answer/feedback ciphertext | Encrypted at rest; expected answer remains segment server data |
| client_submission_id | Partial unique per user; same logical retry never reevaluates |
| overall_score/accuracy_percent | Server result: normalized exact final Simplified Hanzi gives 100; non-match gives 0 |
| lesson_progresses.dictation_best_score | Update only if higher; no completion/unlock mutation |

Future locked segment produces no attempt. Pinyin and Traditional Hanzi are not equivalent answers.
