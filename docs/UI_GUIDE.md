# ShareShelf — UI Guide (HTML Frontend, Final)

## The stack

```
Browser (HTML/CSS forms) → Servlet (Java) → DAO (JDBC) → MySQL
```

HTML alone isn't a Java project — Servlets are what turn these pages into a working app. See `SETUP_GUIDE.md` for exactly how that's wired.

## The pages (Member 2 builds these)

1. **Login / Register** — two simple forms
2. **Dashboard** — nav links (My Listings / Search / My Requests / Reports), welcome banner, pending-requests-on-your-items section
3. **Add Listing** — title, description, category dropdown, listing type dropdown, condition, location, and (for Borrow type) a suggested duration dropdown (1 day / 3 days / 1 week / custom)
4. **Browse/Search** — search box + filter dropdowns, results as a table or cards, each with a "Send Request" button
5. **My Requests / Received Requests** — table with Accept/Reject/Cancel buttons per row
6. **Reports** — a few stat cards (total listings, available, pending requests, etc.)

An interactive mockup of exactly this flow is linked separately — use it as the structural reference, not code to copy line-for-line.

## Build tips

- One shared `style.css`, linked from every page — no inline styles scattered around
- Plain `<form method="post" action="...">` — no JS framework needed
- **Field names matter across the HTML/Servlet boundary**: whatever `name="..."` you put on an `<input>`, the Servlet reads it back via `request.getParameter("thatExactName")`. Leader and Member 2 should agree on field names *before* building both sides, or things silently won't connect.
- Session: `session.setAttribute("studentId", id)` once at login, read wherever needed — that's your session-management rubric row
- Redirect after every POST (login, add listing, send request) back to a GET page — avoids "resubmit form on refresh" and looks more polished in a demo

## What NOT to worry about

- Responsive/mobile layout — not graded
- JS animations/interactivity — not required, adds risk with no reward
- Logos/branding — a clean heading is enough
