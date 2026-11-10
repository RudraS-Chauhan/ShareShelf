-- ShareShelf schema for HOSTED databases (Aiven, Railway, TiDB, ...).
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
