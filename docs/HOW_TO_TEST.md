# How to check and test ShareShelf

Three levels, from easiest to most thorough. Do level 1 before every demo.

## Level 1: Click through it yourself (10 minutes)
Run the app (see the README) and open http://localhost:8080/ShareShelf/ (or your Render URL). You need **two browsers** (for example Chrome and a private window) so you can be two students at once.

Admission numbers must appear inside the email, for example `25SCSE1001001` with `25scse1001001@galgotiasuniversity.ac.in`.

| # | Do this | You should see |
|---|---|---|
| 1 | Register **Student A** in browser 1 and **Student B** in browser 2 | Redirect to login with a success message |
| 2 | Try registering again with the same admission number | A clear error, no crash |
| 3 | Log in with a wrong password | "Invalid" message, still on login |
| 4 | **A:** Add a listing (type Borrow, location Library, 3 days) | Appears on A's dashboard with status AVAILABLE |
| 5 | **A:** Add a listing with an empty title | Form shows again with a message |
| 6 | **B:** Search for A's item by keyword (try upper/lower case) | A's item is listed |
| 7 | **B:** Send a request | Status PENDING in B's "My requests" |
| 8 | **B:** Request the same item again | Refused (duplicate) |
| 9 | **A:** Accept and pick a return date | Item RESERVED, request ACCEPTED |
| 10 | **A:** Mark handed over, then completed | Item BORROWED, then AVAILABLE again |
| 11 | Check the Reports page | Counts changed; "most requested category" shown |
| 12 | Open `/ShareShelf/reports?format=csv` | A CSV file downloads |
| 13 | Log out, then open `/ShareShelf/dashboard` | Sent back to login (login filter works) |
| 14 | Type `<script>alert(1)</script>` as an item title | It shows as plain text, no popup |

The full manual test list is [`TEST_CASES.md`](TEST_CASES.md). Write **Pass/Fail and the date** in its Result column as you go, with your own name.

### Testing the overdue safety net
Overdue needs a past due date, which the form will not allow. Backdate one in the database:
```sql
UPDATE requests SET expected_return_date = CURDATE() - INTERVAL 2 DAY
WHERE status = 'ACCEPTED' AND request_id = <id>;
```
Within `overdueCheckMinutes` (default 5) the status becomes OVERDUE, the owner sees a banner, and only then does the "reveal WhatsApp" button work. Each reveal adds a row to `contact_reveal_logs`.

## Level 2: Automated unit tests
```bash
mvn test
```
Expect **24 tests, 0 failures**. The DAO integration tests need a reachable database; without one they skip themselves.

## Level 3: Automated end-to-end script
With the app running and an **empty** database (re-run `schema.sql` to empty it):
```bash
python3 tests/e2e_smoke.py                    # 92 checks
```
It registers students, lists items, runs every request step, and checks escaping and security. Every line prints `PASS` or `FAIL`.

The overdue part needs a hook that backdates a due date and restarts the app, so it only runs when you set `OVERDUE_HOOK`; that brings it to the full 98 checks. Our recorded run is in [`test-results/e2e-output.txt`](test-results/e2e-output.txt).

## What a good demo test looks like
Run level 1 steps 1-10 live: it tells the whole story of the app in about three minutes.

## If a test fails
1. Read the `FAIL` line, which says what was expected.
2. Check the server log (Tomcat `logs/catalina.out`, or Render → Logs) for the Java stack trace.
3. Most failures are a wrong DB password, the database not empty, or the app on a different port than the script expects (`python3 tests/e2e_smoke.py http://localhost:8080/ShareShelf`).
