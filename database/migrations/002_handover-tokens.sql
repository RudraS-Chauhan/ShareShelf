-- Run ONCE on the live TiDB database, ONE statement per Run (SQL Editor), after the 18 Nov migration.
-- A fresh database made from database/schema.sql / database/schema-hosted.sql already has these tables.

CREATE TABLE IF NOT EXISTS test.handover_tokens (
  token_id    INT AUTO_INCREMENT PRIMARY KEY,
  request_id  INT NOT NULL,
  kind        VARCHAR(10) NOT NULL,
  token       VARCHAR(40) NOT NULL UNIQUE,
  code        VARCHAR(6) NOT NULL,
  expires_at  DATETIME NOT NULL,
  used        BOOLEAN NOT NULL DEFAULT FALSE,
  attempts    INT NOT NULL DEFAULT 0,
  FOREIGN KEY (request_id) REFERENCES test.requests(request_id),
  INDEX idx_tokens_request (request_id)
);

CREATE TABLE IF NOT EXISTS test.notifications (
  notification_id INT AUTO_INCREMENT PRIMARY KEY,
  student_id      VARCHAR(20) NOT NULL,
  message         VARCHAR(255) NOT NULL,
  link            VARCHAR(100),
  is_read         BOOLEAN NOT NULL DEFAULT FALSE,
  created_at      DATETIME NOT NULL,
  FOREIGN KEY (student_id) REFERENCES test.students(student_id),
  INDEX idx_notifications_student (student_id, is_read)
);

CREATE TABLE IF NOT EXISTS test.ratings (
  rating_id      INT AUTO_INCREMENT PRIMARY KEY,
  request_id     INT NOT NULL,
  rater_id       VARCHAR(20) NOT NULL,
  ratee_id       VARCHAR(20) NOT NULL,
  stars          INT NOT NULL,
  item_condition VARCHAR(10),
  comment        VARCHAR(200),
  created_at     DATETIME NOT NULL,
  UNIQUE KEY uq_rating (request_id, rater_id),
  FOREIGN KEY (request_id) REFERENCES test.requests(request_id),
  FOREIGN KEY (rater_id) REFERENCES test.students(student_id),
  FOREIGN KEY (ratee_id) REFERENCES test.students(student_id)
);

