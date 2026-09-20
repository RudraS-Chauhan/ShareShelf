# ShareShelf — Project Summary

**One line:** A student-to-student platform for Galgotias students to borrow, exchange, donate, and discover useful academic items — HTML frontend, Java Servlets backend, MySQL database.

## The decisions we locked in

| Area | Decision |
|---|---|
| Track | Rubric B — Web-Based (confirm with faculty it's definitely applied) |
| Stack | HTML/CSS → Servlets (Java) → JDBC → MySQL, no Tomcat 10 (use 9) |
| Categories | Academic, Lab Equipment, Stationery, Event Supplies, Electronics — **Hostel Items dropped** (doesn't fit a team spread across different hostels) |
| Safety model | No GPS, no in-app chat. Borrow duration → due date, overdue auto-flagging via background thread, WhatsApp contact reveal **only if overdue** (`wa.me` link, logged, consent captured at registration) |
| Git workflow | Direct commits to `main`, folder-based ownership to avoid conflicts (see `FOLDER_STRUCTURE.md`) |
| Innovation feature (2 marks) | Not locked yet — decide Weekend 6 |

## Team & what each person owns

| Person | Core job |
|---|---|
| **Leader (you)** | Servlets, DAO/JDBC, sessions, threading, integration, deployment, GitHub setup |
| **Member 2** | All HTML/CSS pages, schema assistance, light servlet work later |
| **Member 3** | Item catalog research (`ITEM_CATALOG.md`), test cases, user flows, demo rehearsal |
| **Member 4** | Student survey (`USER_FEEDBACK.md`), README, team contributions log |

## Timeline

- **This weekend (Sept 19–20):** environment setup, DB schema, first working servlet
- **Sept 26–27:** core CRUD screens wired end-to-end
- **Oct 3–4:** workflow logic, threading, validation, testing
- **Sat Oct 10:** Review 1 due (33 marks)
- **Oct 17 – Nov 8:** reports, code quality, docs, one extra feature, full rehearsal
- **Sun Nov 15:** Review 2 due (17 marks)

Full week-by-week detail: `ROADMAP.md`

## Document map

| File | What it's for |
|---|---|
| `FINAL_SPEC.md` | The master reference — read this first if you're catching up |
| `SETUP_GUIDE.md` / `BUILD_IT_YOURSELF.md` | Environment setup, done manually step-by-step |
| `TEAM_ROLES.md` | Full task breakdown per person |
| `ROADMAP.md` | Weekend-by-weekend plan with hard deadlines |
| `UI_GUIDE.md` | Page list + the HTML/Servlet field-name contract |
| `FOLDER_STRUCTURE.md` | Who touches which files on GitHub |

## The one open action item

Message faculty: confirm Rubric B applies to your submission (the guidelines PDF listed both rubrics as "to be filled in" — a one-line question removes the ambiguity).
