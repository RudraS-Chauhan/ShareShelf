# ShareShelf — Weekend-Sprint Roadmap (Final: Web Track)

Weekend-only dedicated time. Stack: HTML → Servlets → JDBC → MySQL. Roles per `TEAM_ROLES.md`.

## The real calendar

| Date | What it is |
|---|---|
| Sat–Sun, **Sept 19–20** | Prep weekend 1 |
| Sat–Sun, **Sept 26–27** | Prep weekend 2 |
| Sat–Sun, **Oct 3–4** | Prep weekend 3 |
| **Sat, Oct 10** | **Review 1 due (33 marks)** |
| Sat–Sun, **Oct 17–18** | Prep weekend 4 |
| Sat–Sun, **Oct 24–25** | Prep weekend 5 |
| Sat–Sun, **Oct 31–Nov 1** | Prep weekend 6 |
| Sat–Sun, **Nov 7–8** | Prep weekend 7 |
| Sun, **Nov 15** | **Review 2 due (17 marks)** |

---

## Weekend 1 (Sept 19–20) — Foundation

1. Leader: install JDK, Tomcat 9, Maven, VS Code + Extension Pack (see `SETUP_GUIDE.md`); create Maven project, push skeleton to GitHub
2. Leader: create MySQL schema (3 tables); get `HelloServlet` responding at `localhost:8080/ShareShelf/hello` — **the checkpoint**
3. Member 2: start designing HTML pages standalone (login, dashboard, add-listing) — no servlet dependency yet, just structure + CSS, previewed directly in browser
4. Member 3 & 4: read the PRD and UI_GUIDE fully; Member 3 starts drafting the test case table structure; Member 4 starts the README skeleton

**Deliverable by Sunday night:** servlet pipeline runs; 2–3 HTML pages exist in draft form.

## Weekend 2 (Sept 26–27) — Wire it together

- Leader: `Student`/`ResourceItem` models + DAOs; `RegisterServlet`, `LoginServlet` (with session), `AddListingServlet`, `SearchServlet`
- Member 2: finalize HTML forms for register/login/add-listing/search to match what the servlets expect (`name` attributes matching `request.getParameter(...)` calls — Leader shares the exact field names needed)
- Member 3: begin populating `TEST_CASES.md` with cases to check once features exist
- Member 4: continue README, draft `TEAM_CONTRIBUTIONS.md` structure

**Deliverable:** a student can register, log in, add a listing, and search — full "happy path" working end-to-end, even if styling is rough.

## Weekend 3 (Oct 3–4) — Workflow + threading + validation

- Leader: `Request` model/DAO, accept/reject/cancel/complete servlets, all validation rules, the background thread, exception-handling pass
- Member 2: requests page HTML (sent/received), reports page HTML, CSS polish across all pages
- Member 3: actually run every test case against the live app, record results
- Member 4: finalize README, screenshots, demo script draft

**By Thu Oct 8:** feature-freeze. **By Fri Oct 9 evening:** full integration test. **Sat Oct 10:** submit in the morning.

---

## Weekend 4 (Oct 17–18) — Reports + review feedback

- Leader: `ReportsServlet` (count queries)
- Member 2: wire the reports page to real data display
- Everyone: review Review 1 feedback if available, note weak spots

## Weekend 5 (Oct 24–25) — Code quality pass

- Leader: exception-handling audit, naming consistency, remove dead code
- Member 3: expand test cases with edge cases (empty DB, closed listings, self-requests, search with spacing/case issues)
- Member 4: keep `TEAM_CONTRIBUTIONS.md` current

## Weekend 6 (Oct 31–Nov 1) — Documentation + one innovation feature

- Leader: one small extra feature (2-mark row) — e.g. "most-requested category" stat or CSV export
- Member 2: final CSS/UX pass across all pages
- Member 3 & 4: finalize all docs, gather screenshots

## Weekend 7 (Nov 7–8) — Full rehearsal

- Full team demo run-through, timed, with Member 3 or 4 driving the walkthrough
- Fresh-machine test: clean `git clone` + `mvn package` + redeploy — confirm it's not just working on one dev machine

## Weekend of Nov 14–15 — Submit

- Aim for **Saturday Nov 14**, leaving Sunday as buffer.

---

## Ground rule

If a weekend's deliverable isn't done by Sunday night, the next weekend's plan gets cut down, not expanded to catch up. Cut scope before you cut testing.
