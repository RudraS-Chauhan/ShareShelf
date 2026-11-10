# Putting ShareShelf online

**Why not Vercel?** Vercel runs static sites and serverless functions (Node, Python, Go). ShareShelf is a Java servlet + JSP app, so it needs a Tomcat server, and Vercel has no way to run one. We use the same idea (push to GitHub, it deploys) on a platform that runs Docker.

```
GitHub repo ──push──▶ Render (builds the Dockerfile, runs Tomcat 9) ──JDBC──▶ hosted MySQL
```

You need two things: **a hosted MySQL database** and **a web service that runs the Dockerfile**. Do them in this order. Free plans change, so check each site's current free tier before you start.

---

## Step 1: Push the code to GitHub
```bash
git add . && git commit -m "docs: add deployment guide" && git push
```
Check on github.com that `Dockerfile`, `schema-hosted.sql` and `pom.xml` are in the repo, and that `db.properties` is **not**.

## Step 2: Create a hosted MySQL database
Pick one provider (we suggest **Aiven** or **Railway**; **TiDB Cloud** also speaks MySQL).

1. Sign up and create a **MySQL** service on the free plan.
2. Copy these five values from its dashboard: **host, port, database name, user, password**.
3. Note whether it says SSL is required (Aiven does). We handle that in the URL below.

## Step 3: Create the tables (no MySQL install needed)
Use a free GUI client such as **DBeaver** (or the "MySQL" extension in VS Code):

1. New connection → MySQL → enter the host, port, user, password from Step 2. Turn SSL on if the provider requires it.
2. Open **`schema-hosted.sql`** (not `schema.sql`; the hosted version does not try to create a database, which most providers block).
3. Make sure the connection's active database is the one from Step 2 (often `defaultdb`), then run the whole script.
4. Refresh. You should see 4 tables: `students`, `resource_items`, `requests`, `contact_reveal_logs`.

## Step 4: Create the web service on Render
1. Sign up at render.com with your GitHub account.
2. **New → Web Service →** pick the `ShareShelf-Java` repo.
3. **Runtime: Docker** (Render finds the `Dockerfile` by itself). Pick the free instance type if it is offered.
4. Under **Environment Variables** add:

| Key | Value |
|---|---|
| `SHARESHELF_DB_URL` | `jdbc:mysql://HOST:PORT/DBNAME?sslMode=REQUIRED` |
| `SHARESHELF_DB_USER` | the user from Step 2 |
| `SHARESHELF_DB_PASSWORD` | the password from Step 2 |
| `PORT` | `8080` |

   Replace HOST, PORT and DBNAME. If your provider does **not** use SSL, drop `?sslMode=REQUIRED`.
5. Click **Create Web Service**. The first build takes several minutes (Maven downloads dependencies).

## Step 5: Open it and check
- `https://<your-service>.onrender.com/` redirects to `/ShareShelf/`.
- `/ShareShelf/hello` should answer, which proves Tomcat and the servlets are alive.
- Register an account and list an item. Then look in DBeaver: you should see rows in `students` and `resource_items`.

From now on, every `git push` to `main` redeploys automatically.

---

## If something goes wrong
| Symptom | Likely cause and fix |
|---|---|
| Build fails at `mvn package` | Read the Render build log; paste the last 30 lines to us. |
| Site loads but login/register shows an error page | Database connection. Check the three `SHARESHELF_DB_*` values for typos, and that `?sslMode=REQUIRED` matches what your provider needs. Render → **Logs** shows the exact SQL error. |
| `Communications link failure` in logs | Wrong host/port, or the provider's firewall (IP allow-list) blocks Render. Allow all IPs on the free tier, or add Render's outbound IPs. |
| `Table ... doesn't exist` | Step 3 ran in a different database than the one in your URL. |
| First visit takes ~30-60 seconds | Free instances sleep after inactivity. Open the site a few minutes before a demo. |
| Port timeout on deploy | `PORT` must be `8080` (Tomcat's port in the Dockerfile). |

## Keep in mind
- The app and DB passwords live only in Render's environment variables, never in Git.
- A free hosted database may be deleted or paused after a period of inactivity. Before the evaluation, open the site and register once.
- The overdue background thread runs inside the container. If the free instance sleeps, it stops scanning until the site wakes up again.
- We could not run Docker where this was written, so the first deploy is the first real test of the `Dockerfile`.
