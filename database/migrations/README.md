# Migrations (only for upgrading an older database)

A **fresh** database made from `../schema.sql` or `../schema-hosted.sql` already has everything here. These files exist because the live TiDB database was created early and grew with each phase. Run them **once, in order**, on an older database:

| Step | File | Adds |
|---|---|---|
| 1 | `001_due-dates-and-meet-time.sql` | suggested borrow hours, due date, meet time |
| 2 | `002_handover-tokens.sql` | `handover_tokens` table (QR pickup and return) |
| 3 | `003_borrow-hours-and-return-condition.sql` | borrow hours, return condition |
| 4 | `004_accepted-time-and-wanted-board.sql` | accepted time, cancel reason, `wanted_posts` table |
| 5 | `005_wanted-location.sql` | location on wanted posts |
| 6 | `006_remember-me-tokens.sql` | `remember_tokens` table ("keep me signed in") |

**How to run on TiDB Cloud:** open the SQL Editor and run **one statement per Run**. A "Duplicate column name" error just means that step was already applied, so carry on with the next line.
