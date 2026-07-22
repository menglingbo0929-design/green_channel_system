-- Establish the member-one counselor/student scope table for databases created
-- before the table was added to the shared schema.
CREATE TABLE IF NOT EXISTS counselor_student (
    id BIGINT NOT NULL AUTO_INCREMENT,
    counselor_user_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_counselor_student (counselor_user_id, student_id),
    KEY idx_cs_counselor (counselor_user_id),
    KEY idx_cs_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- The local demo data has one counselor account. Assign all active demo students
-- that do not yet have an owner, without overwriting an existing assignment.
UPDATE student s
JOIN sys_user u ON u.login_name = 'counselor_demo' AND u.deleted = 0
SET s.counselor_id = u.id
WHERE s.deleted = 0 AND s.counselor_id IS NULL;

INSERT IGNORE INTO counselor_student (counselor_user_id, student_id)
SELECT u.id, s.id
FROM sys_user u
JOIN student s ON s.deleted = 0 AND s.counselor_id = u.id
WHERE u.login_name = 'counselor_demo' AND u.deleted = 0;
