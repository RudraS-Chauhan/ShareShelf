# ShareShelf — Build It Yourself (Manual Guide)

Follow this in order. Each step builds on the last, and you'll hit the Weekend 1 checkpoint (a working servlet) by the end. Do this after you've installed JDK, Tomcat 9, Maven, MySQL, and the VS Code Java extension (see `SETUP_GUIDE.md`).

---

## Step 1 — Generate the project (don't hand-create this part)

```bash
mvn archetype:generate -DgroupId=com.shareshelf -DartifactId=ShareShelf \
  -DarchetypeArtifactId=maven-archetype-webapp -DinteractiveMode=false
```

Maven creates the base folder structure for you — this one command replaces a lot of manual folder-clicking. Open the new `ShareShelf` folder in VS Code.

You'll see it already made:
```
ShareShelf/
├── pom.xml
└── src/main/
    ├── resources/
    └── webapp/
        ├── index.jsp
        └── WEB-INF/web.xml
```

Delete `index.jsp` — we're using plain HTML, not JSP.

## Step 2 — Create your own package folders

In VS Code's file explorer (or terminal), create these folders under `src/main/java/`:

```bash
mkdir -p src/main/java/com/shareshelf/model
mkdir -p src/main/java/com/shareshelf/dao
mkdir -p src/main/java/com/shareshelf/servlet
mkdir -p src/main/java/com/shareshelf/util
mkdir -p src/main/webapp/css
mkdir -p docs
```

At this point your tree should look like:
```
ShareShelf/
├── pom.xml
├── docs/
└── src/main/
    ├── java/com/shareshelf/
    │   ├── model/
    │   ├── dao/
    │   ├── servlet/
    │   └── util/
    └── webapp/
        ├── css/
        └── WEB-INF/web.xml
```

## Step 3 — Edit `pom.xml`

Open `pom.xml`. Inside the `<dependencies>` tag Maven already created, add these two:

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

Save. VS Code should prompt "Maven project changed, reload?" — click yes.

## Step 4 — Create the database

In your terminal (not VS Code):

```bash
mysql -u root -p
```

Once logged in, type or paste:

```sql
CREATE DATABASE shareshelf;
USE shareshelf;

CREATE TABLE students (
  student_id VARCHAR(20) PRIMARY KEY,
  name VARCHAR(100), branch VARCHAR(50), year INT,
  email VARCHAR(100), hostel_or_day VARCHAR(20),
  password VARCHAR(50), whatsapp_number VARCHAR(15) NOT NULL
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
  expected_return_date DATE NULL,
  contact_revealed BOOLEAN DEFAULT FALSE, revealed_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (item_id) REFERENCES resource_items(item_id),
  FOREIGN KEY (requester_id) REFERENCES students(student_id),
  FOREIGN KEY (owner_id) REFERENCES students(student_id)
);
```

Type `exit` to leave MySQL. Your database exists now.

## Step 5 — `util/DBConnection.java`

Right-click the `util` folder → New File → `DBConnection.java`. Type:

```java
package com.shareshelf.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/shareshelf?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "your_mysql_password"; // change this

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("Connected: " + (conn != null));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

Replace `your_mysql_password` with your real one. **Run this file directly** (right-click → Run Java in the editor). You should see `Connected: true` in the terminal output. Don't move on until this works — it's the single most important checkpoint in the whole setup.

## Step 6 — `model/Student.java`

New file in `model/`:

```java
package com.shareshelf.model;

public class Student {
    private String studentId, name, branch, email, hostelOrDay, password, whatsappNumber;
    private int year;

    public Student() {}

    // Getters and setters — type these out, it's tedious but builds muscle memory
    // for how Java objects expose their fields. One per field:
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getHostelOrDay() { return hostelOrDay; }
    public void setHostelOrDay(String hostelOrDay) { this.hostelOrDay = hostelOrDay; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String whatsappNumber) { this.whatsappNumber = whatsappNumber; }
}
```

Then create `ResourceItem.java` and `Request.java` in `model/` the same way — same fields as the `schema.sql` columns you just typed in Step 4 (that's not a coincidence: **each model class mirrors a database table**, field for field). If you want the exact field lists, they're spelled out in `docs/PRD.md` §8.

## Step 7 — `dao/StudentDAO.java`

This is the one worth typing carefully and understanding line by line — it's the pattern you'll repeat for `ListingDAO` and `RequestDAO`:

```java
package com.shareshelf.dao;

import com.shareshelf.model.Student;
import com.shareshelf.util.DBConnection;
import java.sql.*;

public class StudentDAO {

    public boolean register(Student s) {
        String sql = "INSERT INTO students (student_id, name, branch, year, email, hostel_or_day, password, whatsapp_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getStudentId());
            ps.setString(2, s.getName());
            ps.setString(3, s.getBranch());
            ps.setInt(4, s.getYear());
            ps.setString(5, s.getEmail());
            ps.setString(6, s.getHostelOrDay());
            ps.setString(7, s.getPassword());
            ps.setString(8, s.getWhatsappNumber());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Student login(String studentId, String password) {
        String sql = "SELECT * FROM students WHERE student_id = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Student s = new Student();
                s.setStudentId(rs.getString("student_id"));
                s.setName(rs.getString("name"));
                s.setBranch(rs.getString("branch"));
                s.setYear(rs.getInt("year"));
                s.setEmail(rs.getString("email"));
                s.setHostelOrDay(rs.getString("hostel_or_day"));
                s.setWhatsappNumber(rs.getString("whatsapp_number"));
                return s;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
```

**Why `?` instead of just pasting the values into the SQL string?** Because directly concatenating user input into SQL is how SQL-injection attacks work. `PreparedStatement` with `?` placeholders keeps the query structure fixed and treats every value as pure data, never as code — this is exactly what "PreparedStatement" means in your rubric's JDBC row, so understanding *why*, not just typing it, is worth the extra two minutes.

## Step 8 — Your first servlet

New file in `servlet/`: `HelloServlet.java`

```java
package com.shareshelf.servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;

@WebServlet("/hello")
public class HelloServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        resp.getWriter().println("<h1>ShareShelf servlet is alive</h1>");
    }
}
```

## Step 9 — Build, deploy, verify

```bash
mvn package
cp target/ShareShelf-1.0-SNAPSHOT.war <TOMCAT_HOME>/webapps/ShareShelf.war
<TOMCAT_HOME>/bin/startup.sh
```

Visit `http://localhost:8080/ShareShelf/hello` in your browser. Seeing "ShareShelf servlet is alive" means everything — Maven, the servlet, Tomcat, and your project structure — is correctly wired together, by your own hands.

## Step 10 — Copy the planning docs in, commit, push

```bash
mkdir -p docs
# copy PRD.md, SETUP_GUIDE.md, ROADMAP.md, TEAM_ROLES.md, UI_GUIDE.md into docs/

git init
git add .
git commit -m "Weekend 1: project skeleton, DB schema, first working servlet"
git remote add origin https://github.com/RudraS-Chauhan/ShareShelf-Java.git
git push -u origin main
```

---

From here, `ResourceItem.java`/`Request.java` (Step 6's pattern) and `ListingDAO.java`/`RequestDAO.java` (Step 7's pattern) are yours to build the same way — the structure and the reasoning are now something you actually typed and understand, not something that appeared in a zip.
