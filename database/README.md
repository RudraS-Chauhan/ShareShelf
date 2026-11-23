# Database

| File | Use it when |
|---|---|
| [`schema.sql`](schema.sql) | **Local install.** Creates the `shareshelf` database and all ten tables: `mysql -u root -p < database/schema.sql` |
| [`schema-hosted.sql`](schema-hosted.sql) | **Hosted database** (TiDB Cloud and similar) where you cannot create a database yourself. Same tables, no `CREATE DATABASE`. |
| [`migrations/`](migrations/README.md) | **Upgrading an older database** that was created before a feature was added. Not needed for a fresh install. |

Both schema files always contain the **latest** structure, so a fresh install never needs the migrations.
