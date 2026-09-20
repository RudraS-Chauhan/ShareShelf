# ShareShelf — Final Specification

**A student-to-student resource-sharing platform for Galgotias University — borrow, exchange, donate, and discover useful academic items within your campus community.**

**Stack:** HTML/CSS (frontend) → Java Servlets (backend) → JDBC → MySQL
**Track:** Rubric B — Web-Based Project
**Team:** 4 (roles below)

---

## 1. Problem & Vision

Students regularly need low-cost, short-term access to items — calculators, lab coats, textbooks, drawing kits, event supplies — that other students already own but rarely use. There's no structured, campus-local way to discover and borrow/exchange/donate them.

> ShareShelf helps students find, borrow, exchange, and donate useful academic resources within their campus community.

**Not:** e-commerce, a payment app, a chat platform, a live-location tracker.

## 2. Categories (final)

- Academic Materials (books, notes, question papers)
- Lab Equipment (coats, instruments)
- Stationery
- Event Supplies (club/event equipment)
- Electronics & Accessories (calculators, chargers, etc.)

**Hostel Items dropped deliberately** — that category assumes easy handover within one hostel, but your team's users are spread across different hostels and off-campus housing, so it doesn't hold up logistically for V1.

## 3. Users

| Role | Description |
|---|---|
| Resource owner | Lists an item to lend/exchange/donate/give away |
| Resource seeker | Searches, filters, sends a request |
| (Future) Admin | Moderation — explicitly out of scope for V1 |

## 4. Core Workflow

```
Register/Login
   → Create a listing (with suggested borrow duration if type=BORROW)
   → Another student searches/filters
   → Sends a request (with a message describing when/where suits them)
   → Owner accepts (sets expected return date) or rejects
   → Listing status updates automatically
   → Handover happens informally, coordinated via listing location + request message
   → Owner marks COMPLETED when returned
```

**Listing status:** `AVAILABLE → RESERVED → BORROWED/EXCHANGED → CLOSED`
**Request status:** `PENDING → ACCEPTED/REJECTED/CANCELLED → COMPLETED` (or `OVERDUE` if the return date passes uncompleted)

## 5. Trust & Safety (privacy-respecting, no GPS/chat)

- **Borrow duration → expected return date**, set at accept time — same trust mechanism as a library due date
- **Overdue auto-flagging** via the background thread (also satisfies your Threading rubric row) — no external notifications, just visible status
- **Reliability signal**: count of completed exchanges shown per student — no star ratings, no written reviews, avoids disputes
- **WhatsApp contact reveal — overdue only**: contact stays hidden for the entire normal, on-time borrow flow (handover is coordinated informally via the listing's `Location` field and the request `message`). Only if a request becomes `OVERDUE` does the owner get a plain `wa.me/<number>` deep-link button to reach the requester directly. No in-app chat is built. WhatsApp number is a **required field at registration**, with explicit consent language covering this specific use. Every reveal is logged (who/when/which request) — never a public contact directory.

## 6. In-Scope Features (V1)

1. Registration & login (session-based)
2. Add / view / close listings
3. Search by keyword, filter by category & listing type
4. Send / accept / reject / cancel / complete a request
5. Full listing and request status lifecycles (above)
6. Basic reports (counts by status/category)
7. Input validation everywhere; no duplicate pending requests; closed listings reject new requests; owners can't act on requests that aren't theirs
8. Background thread for overdue flagging
9. WhatsApp reveal-on-overdue (§5)
10. Reliability signal (§5)

## 7. Out-of-Scope (say this explicitly in your submission)

Payments/UPI, real authentication (hashing — plain check is fine, mention as future work), real-time in-app chat, image upload, push notifications, GPS/live location, cloud deployment, AI recommendations, admin moderation panel, Hostel Items category.

## 8. Data Model

**Student**: `student_id` (PK), `name`, `branch`, `year`, `email`, `hostel_or_day` (own residence type — separate from the dropped category), `password`, `whatsapp_number` (required)

**ResourceItem**: `item_id` (PK), `owner_id` (FK), `title`, `description`, `category`, `listing_type` [BORROW/EXCHANGE/DONATE/GIVEAWAY], `item_condition`, `location`, `status` [AVAILABLE/RESERVED/BORROWED/CLOSED]

**Request**: `request_id` (PK), `item_id` (FK), `requester_id` (FK), `owner_id` (FK), `message`, `status` [PENDING/ACCEPTED/REJECTED/CANCELLED/COMPLETED/OVERDUE], `expected_return_date`, `contact_revealed` (bool), `revealed_at`, `created_at`

## 9. Rubric Mapping (Rubric B — confirm with faculty this is the one applied)

| Row | Marks | Satisfied by |
|---|---|---|
| Problem Understanding & Solution Design | 8 | This spec + user flow |
| Core Java (OOP, Exceptions, Threads) | 10 | Model/DAO classes, exception handling, overdue-checker thread |
| Database Integration (JDBC) | 8 | Schema, CRUD, PreparedStatement, accept/complete transactions |
| Servlets & Web Integration | 7 | Real `HttpServlet` classes, form handling, `HttpSession` |
| Review 2: Code Quality & Testing | 10 | Clean code, `TEST_CASES.md` |
| Review 2: Teamwork & Collaboration | 5 | Git history, `TEAM_CONTRIBUTIONS.md` |
| Review 2: Innovation / Extra Effort | 2 | TBD — decide during Weekend 6, options: "most-requested category" stat or CSV export of reports |

## 10. Team & Roles

| Person | Owns |
|---|---|
| **You (Leader)** | Servlets, DAO layer, JDBC transactions, session management, background thread, exception handling, GitHub setup, integration, deployment |
| **Member 2** | HTML/CSS pages (all screens), assists on schema/sample data, light servlet work with review once comfortable |
| **Member 3** | `TEST_CASES.md` (runs and records every test honestly), `USER_FLOWS.md`, owns the demo rehearsal |
| **Member 4** | `README.md`, `TEAM_CONTRIBUTIONS.md`, coordination, sits in on integration sessions |

## 11. Collaboration Model

Everyone commits directly to `main` — simplest option for a beginner team, chosen deliberately over branch-per-feature + PR review. This means: **pull before you start working each session, and communicate in your group chat before touching a file someone else might be editing**, since there's no PR step catching conflicts before they land. See `FOLDER_STRUCTURE.md` for exactly who touches which files, which minimizes collision risk even without branches.

## 12. Timeline

See `ROADMAP.md` for the full weekend-by-weekend breakdown. Key dates: **Review 1 due Sat Oct 10** (33 marks), **Review 2 due Sun Nov 15** (17 marks).

## 13. Open Item

Confirm with faculty that **Rubric B (Web-Based)** is definitively what's being applied to your submission, since your original guidelines document listed both Rubric A and B as "to be filled in." A one-line message removes any ambiguity before Review 1.
