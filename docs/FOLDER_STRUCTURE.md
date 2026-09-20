# ShareShelf — Folder Structure Guide (GitHub, Multi-Contributor)

Since everyone commits directly to `main`, the #1 rule is: **stay inside your own folder as much as possible.** Two people editing the same file on the same day is where conflicts happen — two people editing *different* files never conflicts, even on `main`. This structure is designed around that.

## Full tree

```
ShareShelf-Java/
│
├── README.md                      ← Leader maintains; entry point for anyone opening the repo
├── schema.sql                     ← Leader owns; run once locally to create your DB
├── pom.xml                        ← Leader owns; touch only when adding a new dependency
├── .gitignore
│
├── docs/                          ← Planning documents — travel with the code, not just this chat
│   ├── FINAL_SPEC.md              ← the master reference — what we're building
│   ├── SETUP_GUIDE.md             ← Leader wrote; everyone reads before touching code
│   ├── ROADMAP.md                 ← the weekend-by-weekend plan
│   ├── TEAM_ROLES.md              ← who owns what
│   ├── UI_GUIDE.md                ← HTML/Servlet field-name contract
│   ├── TEST_CASES.md              ← Member 3 owns — created/updated weekend by weekend
│   ├── USER_FLOWS.md              ← Member 3 owns
│   └── TEAM_CONTRIBUTIONS.md      ← Member 4 owns — keep current, not written from memory at the end
│
└── src/main/
    ├── java/com/shareshelf/
    │   ├── model/                 ← Leader owns (Weekend 1). Simple POJOs — low conflict risk even if others peek in
    │   │   ├── Student.java
    │   │   ├── ResourceItem.java
    │   │   └── Request.java
    │   │
    │   ├── dao/                   ← Leader owns — JDBC/transactions live here, the highest-risk code to have two people editing at once
    │   │   ├── StudentDAO.java
    │   │   ├── ListingDAO.java
    │   │   └── RequestDAO.java
    │   │
    │   ├── servlet/                ← Leader owns primarily; Member 2 may add ONE simple servlet later with review, once comfortable
    │   │   ├── HelloServlet.java
    │   │   ├── RegisterServlet.java
    │   │   ├── LoginServlet.java
    │   │   ├── AddListingServlet.java
    │   │   ├── SearchServlet.java
    │   │   ├── SendRequestServlet.java
    │   │   ├── AcceptRequestServlet.java
    │   │   ├── RejectRequestServlet.java
    │   │   ├── CompleteRequestServlet.java
    │   │   ├── RevealContactServlet.java
    │   │   └── ReportsServlet.java
    │   │
    │   └── util/                   ← Leader owns
    │       ├── DBConnection.java
    │       └── StaleReservationChecker.java   (the background thread)
    │
    └── webapp/                     ← Member 2's territory — almost never touched by Leader except to confirm field names match the servlets
        ├── index.html               (login/register)
        ├── dashboard.html
        ├── add-listing.html
        ├── search.html
        ├── my-requests.html
        ├── reports.html
        ├── css/
        │   └── style.css
        └── WEB-INF/
            └── web.xml
```

## Who touches what (the actual conflict-avoidance rule)

| Folder/file | Primary owner | Others should... |
|---|---|---|
| `model/`, `dao/`, `servlet/`, `util/` | **Leader** | Not edit directly; if you spot a bug, flag it in your group chat rather than editing Leader's Java files yourself |
| `webapp/*.html`, `webapp/css/` | **Member 2** | Leader only edits to wire a servlet's exact URL/field names — coordinate first |
| `docs/TEST_CASES.md`, `docs/USER_FLOWS.md` | **Member 3** | Others can read anytime; only Member 3 edits |
| `docs/README.md`, `docs/TEAM_CONTRIBUTIONS.md` | **Member 4** | Same — read freely, edit only if you're the owner |
| `schema.sql`, `pom.xml` | **Leader** | Never edit without telling the team — these affect everyone's local setup |

## The one shared danger zone: HTML ↔ Servlet field names

This is the one place Leader and Member 2 genuinely work on the same feature from two sides. Fix this with a simple convention, not by editing the same file:

- Before Member 2 builds a form, Leader shares the exact field names the servlet expects (e.g. `title`, `category`, `listingType`) in your group chat or a shared note
- Member 2 uses those exact `name="..."` attributes on `<input>`/`<select>` tags
- Neither person edits the other's file to "fix" a mismatch — flag it and let the owner fix it, so you always know whose commit changed what

## Commit message convention (keeps history readable for your README/demo)

```
feat: add AddListingServlet and DAO insert method
feat: build add-listing.html page
fix: correct field name mismatch in search form
docs: update TEST_CASES with Weekend 3 results
```

Prefix with `feat:`, `fix:`, or `docs:` — small habit, makes your Git history look intentional rather than a pile of "update" commits, which matters for the "Teamwork & Collaboration" rubric row and for how the repo reads on your GitHub profile later.

## Before every work session

```bash
git pull origin main
```

Since there's no branch/PR step catching conflicts early, pulling first is the only thing standing between you and overwriting a teammate's work from an hour ago. Make it a habit, every single time, before you open your editor.
