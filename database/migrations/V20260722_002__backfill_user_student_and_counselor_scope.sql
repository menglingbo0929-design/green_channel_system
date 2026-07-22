-- Backfill relationships for existing installations before new writes use the formal mappings.
-- A login name equal to the student number is the import contract used by StudentImportService.
UPDATE student s
JOIN sys_user u ON u.login_name = s.student_no AND u.deleted = 0
LEFT JOIN student mapped ON mapped.user_id = u.id AND mapped.id <> s.id AND mapped.deleted = 0
SET s.user_id = u.id
WHERE s.deleted = 0 AND s.user_id IS NULL AND mapped.id IS NULL;

-- Every mapped student account must have the STUDENT role so its JWT carries student authority.
INSERT IGNORE INTO sys_user_role (user_id, role_id, create_time)
SELECT s.user_id, r.id, NOW()
FROM student s
JOIN sys_role r ON r.role_code = 'STUDENT'
WHERE s.deleted = 0 AND s.user_id IS NOT NULL;

-- Keep the normalized responsibility relation in sync with legacy primary-counselor values.
INSERT IGNORE INTO counselor_student (counselor_user_id, student_id)
SELECT s.counselor_id, s.id
FROM student s
JOIN sys_user u ON u.id = s.counselor_id AND u.deleted = 0
WHERE s.deleted = 0 AND s.counselor_id IS NOT NULL;

-- Existing mapping-table-only data gains a deterministic primary counselor cache.
UPDATE student s
JOIN (
    SELECT student_id, MIN(counselor_user_id) AS counselor_user_id
    FROM counselor_student
    GROUP BY student_id
) cs ON cs.student_id = s.id
SET s.counselor_id = cs.counselor_user_id
WHERE s.deleted = 0 AND s.counselor_id IS NULL;

ALTER TABLE student ADD UNIQUE INDEX uk_student_user (user_id);
