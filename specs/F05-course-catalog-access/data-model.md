# Data Model — F05

| Entity | Catalog invariant |
| --- | --- |
| topics | PUBLISHED parent only; HSK nullable and default sort order. |
| lessons | PUBLISHED FREE lesson summary; valid parent and media/segment prerequisites. |
| segments | Publication/order used only to decide validity; transcript is not a catalog field. |
| media_assets | APPROVED media prerequisite; provider/object identifiers never returned. |
| lesson_progresses | Preserved if content becomes unavailable; not created by catalog read. |

Query order is server allowlisted as sort_order then stable title/ID. Search is title-only and
bounded; HSK filter excludes null HSK while All includes it.
