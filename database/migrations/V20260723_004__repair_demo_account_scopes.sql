-- 演示学生账号必须关联真实学生档案，否则 JWT 不会携带 studentId。
UPDATE student s
JOIN sys_user u ON u.login_name='student_demo' AND u.deleted=0
JOIN (
    SELECT selected.id
    FROM (
        SELECT MIN(id) id
        FROM student
        WHERE deleted=0 AND enabled=1 AND user_id IS NULL
    ) selected
) candidate ON candidate.id=s.id
LEFT JOIN student linked ON linked.user_id=u.id AND linked.deleted=0 AND linked.id<>s.id
SET s.user_id=u.id,s.update_time=NOW()
WHERE s.deleted=0 AND linked.id IS NULL;

-- Some existing demo databases have already assigned every imported student to
-- a student-number login.  Keep those real mappings intact and create a
-- dedicated profile for student_demo instead of leaving that demo account
-- unusable.
INSERT INTO student(
    student_no,student_name,college_id,major_id,grade_id,class_id,phone,
    origin_loan,campus_loan,difficulty_level,info_complete,user_id,counselor_id,
    enabled,remark,create_time,update_time,deleted
)
SELECT 'STUDENT_DEMO','演示学生',seed.college_id,seed.major_id,seed.grade_id,seed.class_id,
       '13800000000',1,0,'一般困难',1,demo.id,
       (SELECT id FROM sys_user WHERE login_name='counselor_demo' AND deleted=0 LIMIT 1),
       1,'学生申请中心演示账号',NOW(),NOW(),0
FROM (
    SELECT college_id,major_id,grade_id,class_id
    FROM student
    WHERE deleted=0 AND enabled=1
    ORDER BY id
    LIMIT 1
) seed
JOIN sys_user demo ON demo.login_name='student_demo' AND demo.deleted=0
LEFT JOIN student mapped ON mapped.user_id=demo.id AND mapped.deleted=0
WHERE mapped.id IS NULL
  AND NOT EXISTS (SELECT 1 FROM student WHERE student_no='STUDENT_DEMO' AND deleted=0);

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
