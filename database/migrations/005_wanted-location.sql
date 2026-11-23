-- Run ONCE on the live TiDB database (SQL Editor, one statement per Run), after 004_accepted-time-and-wanted-board.sql.
-- A "Duplicate column name" error means it was already added: that is fine.
-- A fresh database made from database/schema.sql / database/schema-hosted.sql already has it.

ALTER TABLE test.wanted_posts ADD COLUMN location VARCHAR(60) NULL;
