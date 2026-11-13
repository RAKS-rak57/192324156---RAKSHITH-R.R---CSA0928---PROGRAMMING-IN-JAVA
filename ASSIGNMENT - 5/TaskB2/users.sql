-- Task B2: LOCAL TEST DATABASE created only for this assignment (never use real passwords here)
CREATE TABLE users (
    user_no INT PRIMARY KEY,
    id      VARCHAR(20) NOT NULL UNIQUE,
    pwd     VARCHAR(40) NOT NULL,
    role    VARCHAR(10) NOT NULL
);
INSERT INTO users(user_no, id, pwd, role) VALUES (1, 'admin', 'Adm!n#2026', 'ADMIN');
INSERT INTO users(user_no, id, pwd, role) VALUES (2, 'arun',  'secret123',  'STUDENT');
INSERT INTO users(user_no, id, pwd, role) VALUES (3, 'divya', 'pass456',    'STUDENT');
INSERT INTO users(user_no, id, pwd, role) VALUES (4, 'obrien', 'it''s-me',  'STUDENT');
