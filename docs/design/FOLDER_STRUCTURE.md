# ShareShelf — Folder Structure

```
ShareShelf-Java/
├── README.md                    project overview + how to run
├── pom.xml                      Maven build (WAR, Servlet 4.0 / Tomcat 9, JSTL, MySQL driver, JUnit 5)
├── LICENSE                      MIT license
├── database/
│   ├── schema.sql               creates the database and all ten tables (local install)
│   ├── schema-hosted.sql        the same tables for a hosted database (no CREATE DATABASE)
│   └── migrations/              001 to 006: only for upgrading an older database
├── Dockerfile                   builds the WAR and runs it on Tomcat 9 (used by Render)
├── tests/
│   └── e2e_smoke.py             black-box HTTP test of every workflow (173 checks)
├── docs/
│   ├── guides/                  setup, deployment, build guide
│   ├── design/                  user flows, UI guide, DAO contract, this file
│   ├── testing/                 test cases, how to test, recorded results
│   ├── planning/                project summary, final spec, roadmap, Review 0
│   ├── team/                    roles and contributions
│   └── screenshots/             images used in the README
└── src/
    ├── main/
    │   ├── java/com/shareshelf/
    │   │   ├── model/           Student, ResourceItem, Request (+ ListingType, ListingStatus, RequestStatus enums)
    │   │   ├── dao/             Dao<T,ID> interface; StudentDAO, ListingDAO, RequestDAO (reads),
    │   │                    RequestTransactionService (multi-table transactions), WantedDAO, ...
    │   │   ├── servlet/         BaseServlet + one servlet per action (login, register, search, request/*, ...)
    │   │   ├── filter/          AuthFilter (login gate: every page except login/register needs a session)
    │   │   ├── listener/        AppLifecycleListener (starts/stops the background thread)
    │   │   └── util/            DBConnection, PasswordUtil, ValidationUtil, AppConstants,
    │   │                        JsonUtil, ConnectionPool, TtlCache, TrustLevel, DataAccessException,
    │   │                        StaleReservationChecker (the overdue thread)
    │   ├── resources/           db.properties.example (copy to db.properties; git-ignored)
    │   └── webapp/
    │       ├── index.jsp        login / register
    │       ├── css/style.css    the single stylesheet
    │       └── WEB-INF/
    │           ├── web.xml      welcome file, sessions, JSP config, error pages
    │           └── views/       dashboard, search, add-listing, my-requests, reports, error (.jsp)
    │                            + fragments/ (header, footer, owner-actions, trust, taglibs)
    └── test/java/com/shareshelf/   JUnit 5: util tests + DAO integration tests (61 tests)
```

Request flow: **browser → servlet (validates, checks ownership) → DAO (JDBC) → MySQL**, then the servlet either
redirects (Post/Redirect/Get, with a one-time flash message) or forwards to a JSP in `WEB-INF/views`
(JSPs cannot be opened directly, only through a servlet).
