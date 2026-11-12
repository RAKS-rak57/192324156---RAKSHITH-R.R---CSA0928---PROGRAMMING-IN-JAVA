-- Task 1 : fee payment table (MySQL)
CREATE DATABASE IF NOT EXISTS college;
USE college;

CREATE TABLE IF NOT EXISTS student (
    reg_no  VARCHAR(12) PRIMARY KEY,
    name    VARCHAR(60) NOT NULL
);

CREATE TABLE IF NOT EXISTS fees (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    reg_no     VARCHAR(12)   NOT NULL,
    amount     DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    paid_on    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reg_no) REFERENCES student(reg_no)
);
