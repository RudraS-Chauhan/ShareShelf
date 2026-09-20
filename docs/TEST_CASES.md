# ShareShelf — Test Cases

**Owner: Member 3.** Run each of these against the actual running app once the relevant feature exists — don't fill this from imagination. Update the Result column honestly; a failing test you found and logged is worth more at review time than a suspiciously perfect table.

| # | Test case | Input/action | Expected result | Result | Notes |
|---|---|---|---|---|---|
| 1 | Duplicate student | Register with an existing student ID | Registration rejected | | |
| 2 | Unknown owner | Create listing with invalid owner ID | Listing not created | | |
| 3 | Empty title | Leave title blank on Add Listing | User asked to re-enter | | |
| 4 | Search listing | Search "calculator" | Matching listings appear | | |
| 5 | Duplicate request | Request the same item twice | Second request blocked | | |
| 6 | Closed listing | Request a closed item | Request rejected | | |
| 7 | Owner accepts request | Accept a pending request | Request becomes ACCEPTED, listing becomes RESERVED | | |
| 8 | Non-owner accepts | Another student tries to accept a request that isn't theirs | Action denied | | |
| 9 | Self-request | Student tries to request their own listing | Blocked | | |
| 10 | No results | Search for a nonexistent item | Clear "no listings found" message | | |
| 11 | Empty database | Browse listings with none in the DB | No crash, clear empty state | | |
| 12 | Case-insensitive search | Search "CALCULATOR" | Same results as lowercase | | |
| 13 | Search with extra spaces | Search "  calculator  " | Still matches | | |
| 14 | Overdue flagging | Let expected_return_date pass without completing | Request shows OVERDUE | | |
| 15 | Contact reveal — not overdue | Try to reveal contact on a non-overdue request | Blocked | | |
| 16 | Contact reveal — overdue | Reveal contact on a genuinely overdue request | WhatsApp link shown, reveal logged | | |
| 17 | Invalid menu/page action | Navigate to an invalid state (e.g. accept an already-completed request) | Handled gracefully, no crash | | |

Add more rows as you find edge cases during testing — this table is meant to grow, not stay at 17.
