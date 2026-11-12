-- Task 2 : outpatient appointment desk (MySQL)
CREATE DATABASE IF NOT EXISTS hospital;
USE hospital;

CREATE TABLE department (
    dept_id INT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(40) NOT NULL UNIQUE
);
CREATE TABLE doctor (
    doctor_id VARCHAR(8)  PRIMARY KEY,
    name      VARCHAR(60) NOT NULL,
    dept_id   INT         NOT NULL,
    FOREIGN KEY (dept_id) REFERENCES department(dept_id),
    INDEX idx_doctor_dept (dept_id)             -- makes "doctors of a department" an index lookup
);
CREATE TABLE patient (
    patient_id VARCHAR(8) PRIMARY KEY,
    name       VARCHAR(60) NOT NULL,
    phone      VARCHAR(15)
);
CREATE TABLE appointment (
    appt_id    INT AUTO_INCREMENT PRIMARY KEY,
    patient_id VARCHAR(8) NOT NULL,
    doctor_id  VARCHAR(8) NOT NULL,
    appt_date  DATE       NOT NULL,
    slot_time  CHAR(5)    NOT NULL,
    FOREIGN KEY (patient_id) REFERENCES patient(patient_id),
    FOREIGN KEY (doctor_id)  REFERENCES doctor(doctor_id),
    UNIQUE KEY uq_doctor_slot (doctor_id, appt_date, slot_time)   -- a doctor cannot be booked twice for one slot
);
