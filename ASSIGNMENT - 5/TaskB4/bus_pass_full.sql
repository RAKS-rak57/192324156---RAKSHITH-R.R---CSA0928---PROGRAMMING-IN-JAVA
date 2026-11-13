-- Task B4: the production table for 12 campuses (MySQL; the same text also runs in SQLite)
DROP TABLE IF EXISTS bus_pass_full;
CREATE TABLE bus_pass_full (
    pass_id       VARCHAR(10)   PRIMARY KEY,
    reg_no        VARCHAR(12)   NOT NULL,
    name          VARCHAR(60)   NOT NULL,
    route_no      INT           NOT NULL,
    boarding_stop VARCHAR(30)   NOT NULL,
    validity_date DATE          NOT NULL,
    amount        DECIMAL(8,2)  NOT NULL,
    campus_id     INT           NOT NULL DEFAULT 1
);
