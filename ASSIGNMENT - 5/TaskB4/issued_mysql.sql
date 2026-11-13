-- MySQL: the database hands out unique pass numbers to all counters
CREATE TABLE issued (pass_no INT AUTO_INCREMENT PRIMARY KEY, counter_no INT NOT NULL, issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
