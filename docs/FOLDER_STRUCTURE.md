# ShareShelf — Folder Structure

```
ShareShelf-Java/
├── README.md                    project overview + how to run
├── pom.xml                      Maven build (WAR, Servlet 4.0 / Tomcat 9, JSTL, MySQL driver, JUnit 5)
├── schema.sql                   creates the database and 4 tables
├── tests/
│   └── e2e_smoke.py             black-box HTTP test of every workflow (98 checks)
├── docs/                        planning docs, test cases, screenshots
└── src/
    ├── main/
    │   ├── java/com/shareshelf/
    │   │   ├── model/           Student, ResourceItem, Request (+ ListingType, ListingStatus, RequestStatus enums)
    │   │   ├── dao/             StudentDAO, ListingDAO, RequestDAO (all SQL and transactions)
    │   │   ├── servlet/         BaseServlet + one servlet per action (login, register, search, request/*, ...)
    │   │   ├── filter/          AuthFilter (login gate: every page except login/register needs a session)
    │   │   ├── listener/        AppLifecycleListener (starts/stops the background thread)
    │   │   └── util/            DBConnection, PasswordUtil, ValidationUtil, AppConstants,
    │   │                        DataAccessException, StaleReservationChecker (the overdue thread)
    │   ├── resources/           db.properties.example (copy to db.properties; git-ignored)
    │   └── webapp/
    │       ├── index.jsp        login / register
    │       ├── css/style.css    the single stylesheet
    │       └── WEB-INF/
    │           ├── web.xml      welcome file, sessions, JSP config, error pages
    │           └── views/       dashboard, search, add-listing, my-requests, reports, error (.jsp)
    │                            + fragments/ (header, footer, owner-actions, trust, taglibs)
    └── test/java/com/shareshelf/   JUnit 5: util tests + DAO integration tests (24 tests)
```

Request flow: **browser → servlet (validates, checks ownership) → DAO (JDBC) → MySQL**, then the servlet either
redirects (Post/Redirect/Get, with a one-time flash message) or forwards to a JSP in `WEB-INF/views`
(JSPs cannot be opened directly, only through a servlet).
