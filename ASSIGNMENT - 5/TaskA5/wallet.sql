-- Task A5 / B3: two accounts of one student; the sum of both balances must never change by a transfer
DROP TABLE IF EXISTS account;
CREATE TABLE account (
    acc_id  VARCHAR(16)   PRIMARY KEY,
    balance DECIMAL(10,2) NOT NULL CHECK (balance >= 0)       -- an account can never go negative
);
INSERT INTO account(acc_id, balance) VALUES ('HOSTEL-192311001', 1000.00);
INSERT INTO account(acc_id, balance) VALUES ('CANTEEN-192311001', 50.00);
