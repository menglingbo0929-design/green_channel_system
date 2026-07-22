-- 演示学生账号必须关联真实学生档案，否则 JWT 不会携带 studentId。
UPDATE student s
JOIN (
    SELECT candidate.id student_id,u.id user_id
    FROM sys_user u
    JOIN (
        SELECT MIN(id) id FROM student WHERE deleted=0 AND enabled=1 AND user_id IS NULL
    ) candidate
    WHERE u.login_name='student_demo' AND u.deleted=0
) mapping ON mapping.student_id=s.id
SET s.user_id=mapping.user_id,s.update_time=NOW()
WHERE s.deleted=0
  AND NOT EXISTS (
      SELECT 1 FROM student linked
      WHERE linked.user_id=mapping.user_id AND linked.deleted=0 AND linked.id<>s.id
  );

-- 兼容已有 counselor_id 但缺 counselor_student 关系的环境。
INSERT INTO counselor_student(counselor_user_id,student_id,create_time)
SELECT s.counselor_id,s.id,NOW()
FROM student s
JOIN sys_user u ON u.id=s.counselor_id AND u.deleted=0
WHERE s.deleted=0 AND s.counselor_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM counselor_student cs
      WHERE cs.counselor_user_id=s.counselor_id AND cs.student_id=s.id
  );
