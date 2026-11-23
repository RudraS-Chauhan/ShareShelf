-- Run ONCE on the live TiDB database, ONE statement per Run, after 002_handover-tokens.sql.
-- A fresh database made from database/schema.sql / database/schema-hosted.sql already has these columns.

ALTER TABLE test.requests ADD COLUMN borrow_hours INT NULL;
ALTER TABLE test.requests ADD COLUMN return_condition VARCHAR(10) NULL;
