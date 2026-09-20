# ShareShelf — Setup Guide (VS Code + Tomcat + MySQL)

VS Code has no built-in "Dynamic Web Project" wizard like Eclipse, so we use **Maven**: generate a webapp project, add two dependencies, write servlets with `@WebServlet` (no `web.xml` editing needed), build a `.war`, drop it into Tomcat's `webapps/` folder.

**Owner note:** this setup is mainly the Leader's job (deepest Java background). Member 2 doesn't need to touch Tomcat at all — they work in `src/main/webapp/*.html` files, which can be opened and edited directly without any server running, using Live Server or just opening the file in a browser to preview layout before it's wired to a servlet.

## 1. Install (tonight, before Weekend 1)

- **JDK 17** — verify: `java -version`
- **Apache Tomcat 9.x** — get **9, not 10** (Tomcat 10 renamed `javax.servlet` → `jakarta.servlet`; avoid the mismatch with course material). Unzip anywhere, e.g. `~/tomcat9`
- **MySQL** — already installed
- **Maven** — verify: `mvn -version`
- **VS Code extensions:** "Extension Pack for Java" (Microsoft) is enough — skip Servlet-specific plugins, they're often flaky. Member 2 can additionally install "Live Server" for instant HTML preview while designing pages standalone.

## 2. Create the project

```bash
mvn archetype:generate -DgroupId=com.shareshelf -DartifactId=ShareShelf \
  -DarchetypeArtifactId=maven-archetype-webapp -DinteractiveMode=false
```

Open the `ShareShelf` folder in VS Code.

## 3. Folder structure

```
ShareShelf/
├── pom.xml
└── src/main/
    ├── java/com/shareshelf/
    │   ├── model/     (Student.java, ResourceItem.java, Request.java)
    │   ├── dao/        (StudentDAO.java, ListingDAO.java, RequestDAO.java)
    │   ├── servlet/     (RegisterServlet.java, LoginServlet.java, ...)
    │   └── util/         (DBConnection.java, StaleReservationChecker.java)
    └── webapp/
        ├── index.html
        ├── css/style.css
        └── WEB-INF/web.xml
```

Member 2 primarily works in `webapp/*.html` and `webapp/css/style.css`. They can design and preview these standalone (open directly in browser, or Live Server) before they're wired to real data.

## 4. `pom.xml` — add dependencies

```xml
<dependency>
  <groupId>javax.servlet</groupId>
  <artifactId>javax.servlet-api</artifactId>
  <version>4.0.1</version>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>mysql</groupId>
  <artifactId>mysql-connector-java</artifactId>
  <version>8.0.33</version>
</dependency>
```

## 5. First servlet (the checkpoint)

```java
package com.shareshelf.servlet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;

@WebServlet("/hello")
public class HelloServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/html");
        resp.getWriter().println("<h1>ShareShelf servlet is alive</h1>");
    }
}
```

## 6. Build and deploy

```bash
mvn package
cp target/ShareShelf-1.0-SNAPSHOT.war <TOMCAT_HOME>/webapps/ShareShelf.war
<TOMCAT_HOME>/bin/startup.sh    # Mac/Linux — startup.bat on Windows
```

Visit **http://localhost:8080/ShareShelf/hello**. If you see the message, your pipeline works. Rebuild loop while developing: change code → `mvn package` → recopy WAR → restart Tomcat.

## 7. MySQL schema

```sql
CREATE DATABASE shareshelf;
USE shareshelf;

CREATE TABLE students (
  student_id VARCHAR(20) PRIMARY KEY,
  name VARCHAR(100), branch VARCHAR(50), year INT,
  email VARCHAR(100), hostel_or_day VARCHAR(20), password VARCHAR(50)
);

CREATE TABLE resource_items (
  item_id INT AUTO_INCREMENT PRIMARY KEY,
  owner_id VARCHAR(20), title VARCHAR(100), description TEXT,
  category VARCHAR(50), listing_type VARCHAR(20), item_condition VARCHAR(20),
  location VARCHAR(100), status VARCHAR(20) DEFAULT 'AVAILABLE',
  FOREIGN KEY (owner_id) REFERENCES students(student_id)
);

CREATE TABLE requests (
  request_id INT AUTO_INCREMENT PRIMARY KEY,
  item_id INT, requester_id VARCHAR(20), owner_id VARCHAR(20),
  message VARCHAR(255), status VARCHAR(20) DEFAULT 'PENDING',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (item_id) REFERENCES resource_items(item_id),
  FOREIGN KEY (requester_id) REFERENCES students(student_id),
  FOREIGN KEY (owner_id) REFERENCES students(student_id)
);
```

`DBConnection.java`:

```java
package com.shareshelf.util;
import java.sql.*;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/shareshelf";
    private static final String USER = "root";
    private static final String PASS = "your_mysql_password";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
```

## Common errors and fixes

| Error | Fix |
|---|---|
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | Confirm `mysql-connector-java` is in `pom.xml`, reload Maven project, rerun `mvn package` |
| Port 8080 already in use | Stop the other process, or change Tomcat's port in `conf/server.xml` |
| 404 on your servlet URL | Check `@WebServlet("/path")` matches exactly; redeploy the WAR after changes |
| `Access denied for user 'root'` | Password mismatch in `DBConnection.java` |
