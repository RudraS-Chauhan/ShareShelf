-- ShareShelf schema for HOSTED databases (TiDB Cloud, Railway, ...).
-- Same tables as schema.sql, but it does not create or select a database:
-- you run it inside the database the provider gave you (often `defaultdb`).
-- See docs/DEPLOYMENT.md.

-- Mirrors model/Student.java
-- password holds a salted PBKDF2 hash ("pbkdf2$iterations$salt$hash"), never plaintext.
CREATE TABLE IF NOT EXISTS students (
  student_id      VARCHAR(20) PRIMARY KEY,
  name            VARCHAR(100) NOT NULL,
  branch          VARCHAR(50),
  year            INT,
  email           VARCHAR(100) NOT NULL UNIQUE,
  hostel_or_day   VARCHAR(20),
  password        VARCHAR(255) NOT NULL,
  whatsapp_number VARCHAR(15) NOT NULL
);

-- Mirrors model/ResourceItem.java
-- suggested_days: owner's suggested borrow duration (BORROW listings only).
CREATE TABLE IF NOT EXISTS resource_items (
  item_id         INT AUTO_INCREMENT PRIMARY KEY,
  owner_id        VARCHAR(20) NOT NULL,
  title           VARCHAR(100) NOT NULL,
  description     TEXT,
  category        VARCHAR(50),
  listing_type    VARCHAR(20),
  item_condition  VARCHAR(20),
  location        VARCHAR(100),
  suggested_days  INT NULL,
  suggested_hours INT NULL,
  status          VARCHAR(20) DEFAULT 'AVAILABLE',
  FOREIGN KEY (owner_id) REFERENCES students(student_id),
  CONSTRAINT chk_item_status CHECK (status IN ('AVAILABLE','RESERVED','BORROWED','CLOSED')),
  CONSTRAINT chk_listing_type CHECK (listing_type IN ('BORROW','EXCHANGE','DONATE','GIVEAWAY')),
  INDEX idx_items_status (status),
  INDEX idx_items_owner (owner_id)
);

-- Mirrors model/Request.java
CREATE TABLE IF NOT EXISTS requests (
  request_id            INT AUTO_INCREMENT PRIMARY KEY,
  item_id               INT NOT NULL,
  requester_id          VARCHAR(20) NOT NULL,
  owner_id              VARCHAR(20) NOT NULL,
  message               VARCHAR(255),
  status                VARCHAR(20) DEFAULT 'PENDING',
  expected_return_date  DATE NULL,
  due_at                DATETIME NULL,
  meet_time             DATETIME NULL,
  meet_note             VARCHAR(255) NULL,
  was_overdue           BOOLEAN DEFAULT FALSE,
  borrow_hours INT NULL,
  return_condition VARCHAR(10) NULL,
  accepted_at DATETIME NULL,
  cancel_reason VARCHAR(60) NULL,
  contact_revealed      BOOLEAN DEFAULT FALSE,
  revealed_at           TIMESTAMP NULL,
  created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (item_id) REFERENCES resource_items(item_id),
  FOREIGN KEY (requester_id) REFERENCES students(student_id),
  FOREIGN KEY (owner_id) REFERENCES students(student_id),
  CONSTRAINT chk_request_status CHECK (status IN ('PENDING','ACCEPTED','REJECTED','CANCELLED','COMPLETED','OVERDUE')),
  INDEX idx_requests_status (status),
  INDEX idx_requests_item (item_id)
);

-- Audit log: one row per WhatsApp contact reveal (who / when / which request).
CREATE TABLE IF NOT EXISTS contact_reveal_logs (
  log_id       INT AUTO_INCREMENT PRIMARY KEY,
  request_id   INT NOT NULL,
  owner_id     VARCHAR(20) NOT NULL,
  revealed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (request_id) REFERENCES requests(request_id),
  FOREIGN KEY (owner_id) REFERENCES students(student_id)
);

-- ===== Phase 2 (19 Nov 2026): QR handover/return, notifications, ratings =====

-- One-time QR token + 6-digit fallback code for a handover (kind HANDOVER) or a return (kind RETURN).
-- Valid for a few minutes; the code is locked after 5 wrong tries.
CREATE TABLE IF NOT EXISTS handover_tokens (
  token_id    INT AUTO_INCREMENT PRIMARY KEY,
  request_id  INT NOT NULL,
  kind        VARCHAR(10) NOT NULL,
  token       VARCHAR(40) NOT NULL UNIQUE,
  code        VARCHAR(6) NOT NULL,
  expires_at  DATETIME NOT NULL,
  used        BOOLEAN NOT NULL DEFAULT FALSE,
  attempts    INT NOT NULL DEFAULT 0,
  FOREIGN KEY (request_id) REFERENCES requests(request_id),
  INDEX idx_tokens_request (request_id)
);

-- In-app notifications (the bell in the top bar).
CREATE TABLE IF NOT EXISTS notifications (
  notification_id INT AUTO_INCREMENT PRIMARY KEY,
  student_id      VARCHAR(20) NOT NULL,
  message         VARCHAR(255) NOT NULL,
  link            VARCHAR(100),
  is_read         BOOLEAN NOT NULL DEFAULT FALSE,
  created_at      DATETIME NOT NULL,
  FOREIGN KEY (student_id) REFERENCES students(student_id),
  INDEX idx_notifications_student (student_id, is_read)
);

-- One rating per person per completed request. A rating only counts on a profile once the
-- other side has rated the same request too, so nobody can rate in revenge.
CREATE TABLE IF NOT EXISTS ratings (
  rating_id      INT AUTO_INCREMENT PRIMARY KEY,
  request_id     INT NOT NULL,
  rater_id       VARCHAR(20) NOT NULL,
  ratee_id       VARCHAR(20) NOT NULL,
  stars          INT NOT NULL,
  item_condition VARCHAR(10),
  comment        VARCHAR(200),
  created_at     DATETIME NOT NULL,
  UNIQUE KEY uq_rating (request_id, rater_id),
  FOREIGN KEY (request_id) REFERENCES requests(request_id),
  FOREIGN KEY (rater_id) REFERENCES students(student_id),
  FOREIGN KEY (ratee_id) REFERENCES students(student_id)
);

-- ===== Phase 4 (19 Nov 2026): wanted board, cancel after accept, unclaimed reservations =====
CREATE TABLE IF NOT EXISTS wanted_posts (
  post_id     INT AUTO_INCREMENT PRIMARY KEY,
  student_id  VARCHAR(20) NOT NULL,
  title       VARCHAR(100) NOT NULL,
  category    VARCHAR(40) NOT NULL,
  note        VARCHAR(200),
  status      VARCHAR(10) NOT NULL DEFAULT 'OPEN',
  created_at  DATETIME NOT NULL,
  FOREIGN KEY (student_id) REFERENCES students(student_id),
  INDEX idx_wanted_status (status, created_at)
);

-- "I have this" answers: one per person per post, so the poster is not spammed.
CREATE TABLE IF NOT EXISTS wanted_offers (
  post_id     INT NOT NULL,
  offerer_id  VARCHAR(20) NOT NULL,
  PRIMARY KEY (post_id, offerer_id),
  FOREIGN KEY (post_id) REFERENCES wanted_posts(post_id),
  FOREIGN KEY (offerer_id) REFERENCES students(student_id)
);
