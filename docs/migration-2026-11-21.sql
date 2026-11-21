-- Run ONCE on the live TiDB database (SQL Editor), after migration-2026-11-20.sql.
-- A fresh database made from schema.sql / schema-hosted.sql already has it.

CREATE TABLE IF NOT EXISTS test.remember_tokens (
  token_id    INT AUTO_INCREMENT PRIMARY KEY,
  student_id  VARCHAR(20) NOT NULL,
  selector    VARCHAR(24) NOT NULL UNIQUE,
  token_hash  VARCHAR(64) NOT NULL,
  expires_at  DATETIME NOT NULL,
  FOREIGN KEY (student_id) REFERENCES test.students(student_id)
);
