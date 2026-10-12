CREATE DATABASE IF NOT EXISTS shareshelf;
USE shareshelf;

-- Table for Student.java
CREATE TABLE IF NOT EXISTS students (
  student_id      VARCHAR(20) PRIMARY KEY,
  name            VARCHAR(100) NOT NULL,
  branch          VARCHAR(50),
  year            INT,
  email           VARCHAR(100) NOT NULL,
  hostel_or_day   VARCHAR(20),
  password        VARCHAR(50) NOT NULL,
  whatsapp_number VARCHAR(15) NOT NULL
);


-- Resources.java
CREATE TABLE IF NOT EXISTS resource_items (
  item_id         INT AUTO_INCREMENT PRIMARY KEY,
  owner_id        VARCHAR(20) NOT NULL,
  title           VARCHAR(100) NOT NULL,
  description     TEXT,
  category        VARCHAR(50),
  listing_type    VARCHAR(20),
  item_condition  VARCHAR(20),
  location        VARCHAR(100),
  status          VARCHAR(20) DEFAULT 'AVAILABLE',
  FOREIGN KEY (owner_id) REFERENCES students(student_id)
);

-- Requests.java

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
  FOREIGN KEY (owner_id) REFERENCES students(student_id)
);