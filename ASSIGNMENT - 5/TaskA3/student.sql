-- Task A3: tables used by the DAO classes (MySQL / SQLite compatible)
CREATE TABLE student (
    reg_no  VARCHAR(12) PRIMARY KEY,       -- Student.regNo
    name    VARCHAR(60) NOT NULL,          -- Student.name
    email   VARCHAR(80) NOT NULL,          -- Student.email
    year_no INT         NOT NULL           -- Student.year
);
CREATE TABLE course (course_code VARCHAR(10) PRIMARY KEY, title VARCHAR(80) NOT NULL, credits INT NOT NULL);
CREATE TABLE marks  (mark_id INT PRIMARY KEY, reg_no VARCHAR(12) NOT NULL, course_code VARCHAR(10) NOT NULL, marks INT NOT NULL);
