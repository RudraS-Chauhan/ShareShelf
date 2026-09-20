# ShareShelf — Team Roles (Realistic, Based on Actual Skill Levels)

Assigning tasks that match what people can genuinely do beats a "fair-looking" split where three people quietly can't contribute. Honesty here protects you at review time too — if a faculty member asks a teammate to explain their part, they need something true to say.

## You — Leader (Java through ISC 12th, deep) — Java + UI (HTML via AI-assisted)

- `model/` — `Student`, `ResourceItem`, `Request` (shared with Shreya so both sides agree on fields)
- All Servlets: `RegisterServlet`, `LoginServlet`, `AddListingServlet`, `SearchServlet`, request-workflow servlets, `RevealContactServlet`, `ReportsServlet`
- `webapp/` — all HTML/CSS pages, AI-assisted, per `docs/UI_GUIDE.md`
- Session management (`HttpSession`)
- The background thread (`StaleReservationChecker`)
- Exception handling across servlets
- Environment setup (Tomcat, Maven, deployment)
- Final integration — merging everyone's work, resolving conflicts
- GitHub repo structure, branch reviews
- Agreeing the exact DAO method contract with Shreya before she starts implementing (see `docs/DAO_CONTRACT.md`) — this is the one place your two pieces meet

## Shreya (Member 2) — Database layer: full DAO classes, in Java

- Owns `dao/StudentDAO.java`, `dao/ListingDAO.java`, `dao/RequestDAO.java` entirely — every method signature agreed in advance (see `docs/DAO_CONTRACT.md`) so Leader's servlets can call them without knowing the SQL inside
- Owns `schema.sql` edits — coordinate with Leader before changing column names, since models/servlets depend on them matching exactly
- Every method: `PreparedStatement` (never string-concatenated SQL), try-with-resources — follow the pattern in the existing `StudentDAO` example
- `RequestDAO.accept()` and `.complete()` must be real transactions (`conn.setAutoCommit(false)` ... `conn.commit()`/`rollback()`) since they update two tables together — this is the JDBC "Transaction Management" rubric row

## Member 3 — No prior coding background

- **Collect real category/item data:** for each of the 5 categories (Academic, Lab Equipment, Stationery, Event Supplies, Electronics), list 3–5 real examples classmates actually own/need (e.g. "scientific calculator," "drafter set," "DBMS textbook 3rd ed") — this becomes your demo/sample data instead of made-up placeholder items, and makes the final demo feel real rather than generic. Save as `docs/ITEM_CATALOG.md`.
- Own `docs/TEST_CASES.md` — actually run every test case on the running app and record pass/fail honestly; this is real, checkable work
- Write `docs/USER_FLOWS.md` — step-by-step user interactions, screenshots once UI exists
- Own the demo script and rehearsal — the person who walks through the app during review should know it inside out, which doesn't require having written the code
- Help Leader review HTML pages for clarity/typos/flow issues

## Member 4 — No prior coding background

- **Run the mini student survey:** ask 10–15 classmates the 5 questions from the original plan (what have you needed temporarily, have you bought something for short-term use, where do you currently ask, would you borrow from a verified student, which category matters most). Compile answers into `docs/USER_FEEDBACK.md` — this gives your Review 0/README a real "we validated this with students" line instead of an assumption.
- Own `README.md` drafting (setup instructions, project description, screenshots) — coordinate with Leader for accuracy
- Own `docs/TEAM_CONTRIBUTIONS.md` — keep it accurate and updated weekend by weekend, not written from memory at the end
- Sit in on integration sessions to understand what's happening, so they can speak to the project structure in a review even without having written code

## The honesty rule

If a reviewer asks Member 3 or 4 to explain a piece of servlet code they didn't write, the honest answer is "I tested this / documented this / I can walk you through what it does at a high level, but [Leader] wrote it." That's a legitimate answer for a team project. Claiming otherwise is the actual risk — not the unequal code split.
