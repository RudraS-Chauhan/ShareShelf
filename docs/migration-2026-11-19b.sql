-- Run ONCE on the live TiDB database, ONE statement per Run, after migration-2026-11-19.sql.
-- A fresh database made from schema.sql / schema-hosted.sql already has these columns.

ALTER TABLE test.requests ADD COLUMN borrow_hours INT NULL;
ALTER TABLE test.requests ADD COLUMN return_condition VARCHAR(10) NULL;
