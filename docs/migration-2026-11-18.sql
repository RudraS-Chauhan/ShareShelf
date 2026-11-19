-- Run this ONCE on a database that was created BEFORE 18 Nov 2026 (e.g. the live TiDB database).
-- A fresh database made from schema.sql / schema-hosted.sql already has these columns.
-- TiDB's SQL editor runs one statement per click, so run the five lines one at a time.

ALTER TABLE test.resource_items ADD COLUMN suggested_hours INT NULL;
ALTER TABLE test.requests ADD COLUMN due_at DATETIME NULL;
ALTER TABLE test.requests ADD COLUMN meet_time DATETIME NULL;
ALTER TABLE test.requests ADD COLUMN meet_note VARCHAR(255) NULL;
ALTER TABLE test.requests ADD COLUMN was_overdue BOOLEAN DEFAULT FALSE;
