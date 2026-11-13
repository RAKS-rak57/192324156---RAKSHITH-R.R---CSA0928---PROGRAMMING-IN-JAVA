-- Task B3: test table (MySQL).  The same statement also runs in SQLite, which the test driver uses.
DROP TABLE IF EXISTS bus_pass;
CREATE TABLE bus_pass (
    pass_id  VARCHAR(10)   PRIMARY KEY,
    reg_no   VARCHAR(12)   NOT NULL,
    name     VARCHAR(60)   NOT NULL,
    route_no INT           NOT NULL,
    amount   DECIMAL(8,2)  NOT NULL
);
