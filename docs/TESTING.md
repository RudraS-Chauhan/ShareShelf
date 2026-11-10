# ShareShelf — Testing

Three layers, from fastest to most realistic.

| Layer | Where | What it proves | How to run |
|---|---|---|---|
| Unit tests | `src/test/java/.../util/*Test.java` | Validation rules, password hashing, the overdue-checker task (incl. "a failing scan never kills the thread"), constants | `mvn test` |
| DAO integration tests | `src/test/java/.../dao/DaoIntegrationTest.java` | Real SQL against MySQL: hashed passwords, search rules, the `accept`/`complete`/`revealContact` transactions, overdue flagging | `mvn test` (skipped automatically if no database is reachable) |
| End-to-end test | `tests/e2e_smoke.py` | The whole running app over HTTP: 98 checks covering every case in `TEST_CASES.md` | see below |

## Running the end-to-end test
1. Create an **empty** database (`mysql -u root -p < schema.sql`) and start the app (see README).
2. `python3 tests/e2e_smoke.py` (Python 3, standard library only; optional argument = base URL).
3. To also check overdue flagging, set `OVERDUE_HOOK` to a script that backdates the request's due date and restarts the app
   (the background thread flags overdue requests on startup and every 5 minutes). Without it those checks are skipped.

## Recorded results (final code)
* `docs/test-results/e2e-output.txt`: **98 checks, 98 passed, 0 failed**
* `docs/test-results/unit-test-output.txt`: **24 tests, 24 passed, 0 failed** (7 DAO integration tests ran against a real database)

The runs above used Tomcat 9.0.118 (embedded) with MariaDB 11. The manual "Result" column in `TEST_CASES.md`
is for Member 3's own browser run.

## Bugs the tests caught while building
* The shared JSP fragments were read as ISO-8859-1, so the emoji and the ✓ in them came out garbled; fixed with a `*.jspf` property group in `web.xml`.
* Dates and "N days" labels rendered inconsistently; the markup was made robust and the dashboard re-tested.
