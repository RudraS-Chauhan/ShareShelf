# ShareShelf — Test Cases

**Owner: Member 3.** The "Automated check" column shows which automated test already covers each case (see `TESTING.md`). The "Result (manual run)" column is still yours: run each case in the browser and record what you actually saw. Don't fill it from imagination. Update the Result column honestly; a failing test you found and logged is worth more at review time than a suspiciously perfect table.

| # | Test case | Input/action | Expected result | Automated check | Result (manual run) | Notes |
|---|---|---|---|---|---|---|
| 1 | Duplicate student | Register with an existing student ID | Registration rejected | ✅ e2e + DaoIntegrationTest | | |
| 2 | Unknown owner | Create listing with invalid owner ID | Listing not created | ✅ DaoIntegrationTest (FK refuses unknown owner; the UI always uses the logged-in id) | | |
| 3 | Empty title | Leave title blank on Add Listing | User asked to re-enter | ✅ e2e | | |
| 4 | Search listing | Search "calculator" | Matching listings appear | ✅ e2e | | |
| 5 | Duplicate request | Request the same item twice | Second request blocked | ✅ e2e + DaoIntegrationTest | | |
| 6 | Closed listing | Request a closed item | Request rejected | ✅ e2e | | |
| 7 | Owner accepts request | Accept a pending request | Request becomes ACCEPTED, listing becomes RESERVED | ✅ e2e + DaoIntegrationTest | | |
| 8 | Non-owner accepts | Another student tries to accept a request that isn't theirs | Action denied | ✅ e2e | | |
| 9 | Self-request | Student tries to request their own listing | Blocked | ✅ e2e | | |
| 10 | No results | Search for a nonexistent item | Clear "no listings found" message | ✅ e2e | | |
| 11 | Empty database | Browse listings with none in the DB | No crash, clear empty state | ✅ e2e | | |
| 12 | Case-insensitive search | Search "CALCULATOR" | Same results as lowercase | ✅ e2e + DaoIntegrationTest | | |
| 13 | Search with extra spaces | Search "  calculator  " | Still matches | ✅ e2e + DaoIntegrationTest | | |
| 14 | Overdue flagging | Let expected_return_date pass without completing | Request shows OVERDUE | ✅ e2e (background thread) + StaleReservationCheckerTest | | |
| 15 | Contact reveal — not overdue | Try to reveal contact on a non-overdue request | Blocked | ✅ e2e + DaoIntegrationTest | | |
| 16 | Contact reveal — overdue | Reveal contact on a genuinely overdue request | WhatsApp link shown, reveal logged | ✅ e2e + DaoIntegrationTest | | |
| 17 | Invalid menu/page action | Navigate to an invalid state (e.g. accept an already-completed request) | Handled gracefully, no crash | ✅ e2e | | |

### Extra cases added (all covered by the automated e2e script)

| # | Test case | Expected result | Automated check | Result (manual run) |
|---|---|---|---|---|
| 18 | Register with a non-college email | Rejected | ✅ e2e | |
| 19 | Register with an email that lacks your own admission number | Rejected | ✅ e2e + ValidationUtilTest | |
| 20 | Weak password (short, or no digit) | Rejected | ✅ e2e + ValidationUtilTest | |
| 21 | Register without ticking the WhatsApp consent box | Rejected | ✅ e2e | |
| 22 | Password stored in the database | Salted hash, never plaintext | ✅ DaoIntegrationTest + PasswordUtilTest | |
| 23 | Open any page or action while logged out | Redirected to login | ✅ e2e | |
| 24 | Listing title `<script>alert(1)</script>` | Shown as text, script never runs | ✅ e2e | |
| 25 | Search for `' OR 1=1 --` or `%` | Harmless, no extra results | ✅ e2e + DaoIntegrationTest | |
| 26 | Two students request the same item; owner accepts one | The other is auto-rejected | ✅ e2e + DaoIntegrationTest | |
| 27 | Close someone else's listing / close a borrowed item | Refused | ✅ e2e | |
| 28 | Reveal contact as a non-owner of the request | Refused | ✅ e2e + DaoIntegrationTest | |
| 29 | Reports and CSV export with no data and with data | Page loads; CSV downloads | ✅ e2e | |
| 30 | Open the site on a phone-sized screen | Layout fits, nothing overflows | manual (see docs/screenshots/09-mobile-search.png) | |

Add more rows as you find edge cases during testing — this table is meant to grow, not stay at 17.
