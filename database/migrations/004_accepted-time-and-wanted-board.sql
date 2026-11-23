-- Run ONCE on the live TiDB database, ONE statement per Run, after 003_borrow-hours-and-return-condition.sql.
-- A fresh database made from database/schema.sql / database/schema-hosted.sql already has all of this.

ALTER TABLE test.requests ADD COLUMN accepted_at DATETIME NULL;
ALTER TABLE test.requests ADD COLUMN cancel_reason VARCHAR(60) NULL;

CREATE TABLE IF NOT EXISTS test.wanted_posts (
  post_id     INT AUTO_INCREMENT PRIMARY KEY,
  student_id  VARCHAR(20) NOT NULL,
  title       VARCHAR(100) NOT NULL,
  category    VARCHAR(40) NOT NULL,
  note        VARCHAR(200),
  status      VARCHAR(10) NOT NULL DEFAULT 'OPEN',
  created_at  DATETIME NOT NULL,
  FOREIGN KEY (student_id) REFERENCES test.students(student_id),
  INDEX idx_wanted_status (status, created_at)
);

CREATE TABLE IF NOT EXISTS test.wanted_offers (
  post_id     INT NOT NULL,
  offerer_id  VARCHAR(20) NOT NULL,
  PRIMARY KEY (post_id, offerer_id),
  FOREIGN KEY (post_id) REFERENCES test.wanted_posts(post_id),
  FOREIGN KEY (offerer_id) REFERENCES test.students(student_id)
);
