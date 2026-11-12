-- Task 6 : Campus Event Registration  (MySQL 8)
CREATE DATABASE IF NOT EXISTS campus_events CHARACTER SET utf8mb4;
USE campus_events;

CREATE TABLE student (
    reg_no  VARCHAR(12) PRIMARY KEY,
    name    VARCHAR(60) NOT NULL,
    email   VARCHAR(80) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE event (
    event_id   INT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(80) NOT NULL UNIQUE,
    venue      VARCHAR(60) NOT NULL,
    event_date DATE        NOT NULL
) ENGINE=InnoDB;

CREATE TABLE registration (
    reg_id        INT AUTO_INCREMENT PRIMARY KEY,
    reg_no        VARCHAR(12) NOT NULL,
    event_id      INT         NOT NULL,
    registered_on TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_event UNIQUE (reg_no, event_id),          -- RULE: one registration per student per event
    CONSTRAINT fk_reg_student FOREIGN KEY (reg_no)   REFERENCES student(reg_no),
    CONSTRAINT fk_reg_event   FOREIGN KEY (event_id) REFERENCES event(event_id),
    INDEX idx_reg_event (event_id)                                  -- fast filter by event
) ENGINE=InnoDB;

INSERT INTO event(title, venue, event_date) VALUES
    ('Tech Symposium 2026', 'Main Auditorium',  '2026-12-05'),
    ('Cultural Night',      'Open Air Theatre', '2026-12-12'),
    ('Hackathon 24',        'Computer Lab 3',   '2027-01-10');

INSERT INTO student(reg_no, name, email) VALUES
    ('192311001', 'Arun Kumar',  'arun@example.com'),
    ('192311002', 'Divya S',     'divya@example.com'),
    ('192311003', 'Kavin Raj',   'kavin@example.com');

INSERT INTO registration(reg_no, event_id) VALUES
    ('192311001', 1), ('192311002', 1), ('192311003', 2);

-- the two queries used by the application ---------------------------------------------------------
-- page of registrations (? = event id, ? = page size, ? = offset)
-- SELECT r.reg_id, r.reg_no, s.name, e.title, r.registered_on
--   FROM registration r JOIN student s ON s.reg_no = r.reg_no JOIN event e ON e.event_id = r.event_id
--  WHERE r.event_id = ? ORDER BY r.reg_id LIMIT ? OFFSET ?;
-- duplicate check:  SELECT COUNT(*) FROM registration WHERE reg_no = ? AND event_id = ?;
