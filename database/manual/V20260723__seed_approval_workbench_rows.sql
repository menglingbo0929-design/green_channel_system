-- 用途：按审核层级递增准备数据：辅导员 10 条、学院 20 条、学校 30 条。
-- 前置：先执行 V20260723_001__complete_batch_information.sql 与
--       V20260723_002__add_college_user_scope.sql。
-- 特点：不使用 OceanBase 不支持的 TEMPORARY TABLE；固定 APPR-SEED 前缀，可重复执行。
-- 金额按环节递增且区间不重叠：辅导员 600~1500，学院 2000~3900，学校 4100~7000。

ROLLBACK;

-- 仅清理本脚本生成的数据。
DELETE FROM approval_submission_record
WHERE request_id LIKE 'APPR-SEED-%';

DELETE FROM approval_record
WHERE request_id LIKE 'APPR-SEED-%'
   OR application_id IN (SELECT id FROM application WHERE application_no LIKE 'APPR-SEED-%');

DELETE FROM application_attachment
WHERE application_id IN (SELECT id FROM application WHERE application_no LIKE 'APPR-SEED-%');

DELETE FROM arrears_application
WHERE application_id IN (SELECT id FROM application WHERE application_no LIKE 'APPR-SEED-%');

DELETE FROM application_operation_record
WHERE application_id IN (SELECT id FROM application WHERE application_no LIKE 'APPR-SEED-%');

DELETE FROM application
WHERE application_no LIKE 'APPR-SEED-%';

-- 取真实审核账号。
SET @counselor_user_id = (
    SELECT MIN(u.id) FROM sys_user u
    JOIN sys_user_role ur ON ur.user_id=u.id
    JOIN sys_role r ON r.id=ur.role_id AND r.role_code='COUNSELOR'
    WHERE u.deleted=0
);
SET @college_user_id = (
    SELECT MIN(u.id) FROM sys_user u
    JOIN sys_user_role ur ON ur.user_id=u.id
    JOIN sys_role r ON r.id=ur.role_id AND r.role_code='COLLEGE'
    WHERE u.deleted=0
);
SET @school_user_id = (
    SELECT MIN(u.id) FROM sys_user u
    JOIN sys_user_role ur ON ur.user_id=u.id
    JOIN sys_role r ON r.id=ur.role_id AND r.role_code='SCHOOL'
    WHERE u.deleted=0
);

-- 选择学生数量最多的学院，确保学院账号能看到足够数据。
SET @college_id = (
    SELECT s.college_id FROM student s
    WHERE s.deleted=0 AND s.enabled=1 AND s.college_id IS NOT NULL
    GROUP BY s.college_id ORDER BY COUNT(*) DESC, s.college_id LIMIT 1
);

-- 若该学院真实学生不足 30 人，补齐固定学号的审核联调学生；组织字段复用该学院真实学生，
-- 不伪造学院/专业/年级/班级主数据，也不影响现有学生。
SET @seed_major_id=(SELECT major_id FROM student WHERE college_id=@college_id AND deleted=0 ORDER BY id LIMIT 1);
SET @seed_grade_id=(SELECT grade_id FROM student WHERE college_id=@college_id AND deleted=0 ORDER BY id LIMIT 1);
SET @seed_class_id=(SELECT class_id FROM student WHERE college_id=@college_id AND deleted=0 ORDER BY id LIMIT 1);
INSERT INTO student(
    student_no,student_name,college_id,major_id,grade_id,class_id,phone,
    origin_loan,campus_loan,difficulty_level,info_complete,counselor_id,enabled,remark,
    create_time,update_time,deleted
)
SELECT CONCAT('APPRS',LPAD(n.seq_no,3,'0')),CONCAT('审核联调学生',LPAD(n.seq_no,2,'0')),
       @college_id,@seed_major_id,@seed_grade_id,@seed_class_id,NULL,
       MOD(n.seq_no,2),0,'DIFFICULTY',1,@counselor_user_id,1,'APPR-SEED 审核工作台测试学生',NOW(),NOW(),0
FROM (
    SELECT 1 seq_no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
    UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
    UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25
    UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
) n
WHERE @college_id IS NOT NULL AND @seed_major_id IS NOT NULL AND @seed_grade_id IS NOT NULL AND @seed_class_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM student s WHERE s.student_no=CONCAT('APPRS',LPAD(n.seq_no,3,'0')));

INSERT INTO user_college_scope(user_id,college_id)
SELECT @college_user_id,@college_id
WHERE @college_user_id IS NOT NULL AND @college_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_college_scope WHERE user_id=@college_user_id);
UPDATE user_college_scope SET college_id=@college_id WHERE user_id=@college_user_id;

CREATE TABLE IF NOT EXISTS appr_seed_students (
    seq_no INT NOT NULL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    grade_id BIGINT NOT NULL
);
TRUNCATE TABLE appr_seed_students;
INSERT INTO appr_seed_students(seq_no,student_id,grade_id)
SELECT ROW_NUMBER() OVER (ORDER BY s.id),s.id,s.grade_id
FROM student s
WHERE s.deleted=0 AND s.enabled=1 AND s.college_id=@college_id AND s.grade_id IS NOT NULL
ORDER BY s.id LIMIT 30;

INSERT INTO counselor_student(counselor_user_id,student_id,create_time)
SELECT @counselor_user_id,t.student_id,NOW()
FROM appr_seed_students t
WHERE @counselor_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM counselor_student cs
      WHERE cs.counselor_user_id=@counselor_user_id AND cs.student_id=t.student_id
  );

-- 三个独立批次避免 application 的“学生+批次+申请类型”唯一约束冲突。
INSERT INTO green_channel_batch(
    batch_code,batch_name,academic_year,start_time,end_time,college_deadline,
    status,enabled,remark,create_time,update_time,deleted
)
SELECT 'APPR-SEED-C','审核联调-辅导员待审核','2026-2027',
       DATE_SUB(NOW(),INTERVAL 30 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_ADD(NOW(),INTERVAL 30 DAY),
       'OPEN',1,'辅导员工作台约束安全测试数据',NOW(),NOW(),0
WHERE NOT EXISTS (SELECT 1 FROM green_channel_batch WHERE batch_code='APPR-SEED-C' AND deleted=0);

INSERT INTO green_channel_batch(
    batch_code,batch_name,academic_year,start_time,end_time,college_deadline,
    status,enabled,remark,create_time,update_time,deleted
)
SELECT 'APPR-SEED-O','审核联调-学院待审核','2026-2027',
       DATE_SUB(NOW(),INTERVAL 30 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_ADD(NOW(),INTERVAL 30 DAY),
       'OPEN',1,'学院工作台约束安全测试数据',NOW(),NOW(),0
WHERE NOT EXISTS (SELECT 1 FROM green_channel_batch WHERE batch_code='APPR-SEED-O' AND deleted=0);

INSERT INTO green_channel_batch(
    batch_code,batch_name,academic_year,start_time,end_time,college_deadline,
    status,enabled,remark,create_time,update_time,deleted
)
SELECT 'APPR-SEED-S','审核联调-学校待审核','2026-2027',
       DATE_SUB(NOW(),INTERVAL 30 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_ADD(NOW(),INTERVAL 30 DAY),
       'OPEN',1,'学校工作台约束安全测试数据',NOW(),NOW(),0
WHERE NOT EXISTS (SELECT 1 FROM green_channel_batch WHERE batch_code='APPR-SEED-S' AND deleted=0);

SET @batch_c=(SELECT id FROM green_channel_batch WHERE batch_code='APPR-SEED-C' AND deleted=0 LIMIT 1);
SET @batch_o=(SELECT id FROM green_channel_batch WHERE batch_code='APPR-SEED-O' AND deleted=0 LIMIT 1);
SET @batch_s=(SELECT id FROM green_channel_batch WHERE batch_code='APPR-SEED-S' AND deleted=0 LIMIT 1);

UPDATE green_channel_batch SET status='OPEN',enabled=1,
    start_time=DATE_SUB(NOW(),INTERVAL 30 DAY),end_time=DATE_SUB(NOW(),INTERVAL 1 DAY),
    college_deadline=DATE_ADD(NOW(),INTERVAL 30 DAY)
WHERE id IN (@batch_c,@batch_o,@batch_s);

INSERT INTO batch_funding_source(batch_id,source_code)
SELECT b.id,'SCHOOL' FROM green_channel_batch b
WHERE b.id IN (@batch_c,@batch_o,@batch_s)
  AND NOT EXISTS (SELECT 1 FROM batch_funding_source bfs WHERE bfs.batch_id=b.id AND bfs.source_code='SCHOOL');

INSERT INTO batch_eligible_grade(batch_id,grade_id,create_time)
SELECT b.batch_id,t.grade_id,NOW()
FROM (SELECT @batch_c batch_id UNION ALL SELECT @batch_o UNION ALL SELECT @batch_s) b
CROSS JOIN (SELECT DISTINCT grade_id FROM appr_seed_students) t
WHERE NOT EXISTS (
    SELECT 1 FROM batch_eligible_grade beg WHERE beg.batch_id=b.batch_id AND beg.grade_id=t.grade_id
);

INSERT INTO fee_item(item_name,enabled,create_time,update_time,deleted)
SELECT '审核联调欠费项目',1,NOW(),NOW(),0
WHERE NOT EXISTS (SELECT 1 FROM fee_item WHERE item_name='审核联调欠费项目' AND deleted=0);
SET @fee_item_id=(SELECT id FROM fee_item WHERE item_name='审核联调欠费项目' AND deleted=0 LIMIT 1);

-- 10 条辅导员待审核。
INSERT INTO application(
    application_no,student_id,application_type,source,batch_type,
    green_channel_batch_id,subsidy_batch_id,status,current_level,review_round,version,
    application_reason,submit_time,create_by,update_by,create_time,update_time,deleted
)
SELECT CONCAT('APPR-SEED-C-',LPAD(t.seq_no,2,'0')),t.student_id,'GREEN_CHANNEL','STUDENT','GREEN_CHANNEL',
       @batch_c,NULL,'COUNSELOR_PENDING','COUNSELOR',0,1,
       CONCAT('辅导员审核联调申请，序号 ',t.seq_no),DATE_SUB(NOW(),INTERVAL t.seq_no HOUR),
       @counselor_user_id,@counselor_user_id,DATE_SUB(NOW(),INTERVAL t.seq_no HOUR),NOW(),0
FROM appr_seed_students t
WHERE t.seq_no <= 10;

-- 20 条学院待审核。
INSERT INTO application(
    application_no,student_id,application_type,source,batch_type,
    green_channel_batch_id,subsidy_batch_id,status,current_level,review_round,version,
    application_reason,submit_time,create_by,update_by,create_time,update_time,deleted
)
SELECT CONCAT('APPR-SEED-O-',LPAD(t.seq_no,2,'0')),t.student_id,'GREEN_CHANNEL','STUDENT','GREEN_CHANNEL',
       @batch_o,NULL,'COLLEGE_PENDING','COLLEGE',0,3,
       CONCAT('学院审核联调申请，序号 ',t.seq_no),DATE_SUB(NOW(),INTERVAL (t.seq_no+12) HOUR),
       @counselor_user_id,@counselor_user_id,DATE_SUB(NOW(),INTERVAL (t.seq_no+12) HOUR),NOW(),0
FROM appr_seed_students t
WHERE t.seq_no <= 20;

-- 30 条学校待审核。
INSERT INTO application(
    application_no,student_id,application_type,source,batch_type,
    green_channel_batch_id,subsidy_batch_id,status,current_level,review_round,version,
    application_reason,submit_time,create_by,update_by,create_time,update_time,deleted
)
SELECT CONCAT('APPR-SEED-S-',LPAD(t.seq_no,2,'0')),t.student_id,'GREEN_CHANNEL','STUDENT','GREEN_CHANNEL',
       @batch_s,NULL,'SCHOOL_PENDING','SCHOOL',0,5,
       CONCAT('学校审核联调申请，序号 ',t.seq_no),DATE_SUB(NOW(),INTERVAL (t.seq_no+24) HOUR),
       @counselor_user_id,@counselor_user_id,DATE_SUB(NOW(),INTERVAL (t.seq_no+24) HOUR),NOW(),0
FROM appr_seed_students t;

-- 每条申请一条欠费明细，金额随审核阶段增长且全部满足 >0、<=8000。
INSERT INTO arrears_application(application_id,fee_item_id,declared_amount,arrears_reason_code,create_time,update_time,deleted)
SELECT a.id,@fee_item_id,
       CASE
           WHEN a.application_no LIKE 'APPR-SEED-C-%' THEN 500+t.seq_no*100
           WHEN a.application_no LIKE 'APPR-SEED-O-%' THEN 1900+t.seq_no*100
           ELSE 4000+t.seq_no*100
       END,
       CASE MOD(t.seq_no,5)
           WHEN 0 THEN 'FAMILY_FINANCIAL_DIFFICULTY'
           WHEN 1 THEN 'FAMILY_EMERGENCY'
           WHEN 2 THEN 'MAJOR_ILLNESS'
           WHEN 3 THEN 'DISASTER_ACCIDENT'
           ELSE 'OTHER'
       END,NOW(),NOW(),0
FROM appr_seed_students t
JOIN application a ON a.student_id=t.student_id AND a.application_no LIKE 'APPR-SEED-%';

-- 附件元数据覆盖详情页；文件 ID 唯一、大小为正，满足全部 CHECK/UNIQUE 约束。
INSERT INTO application_attachment(application_id,file_id,original_filename,content_type,file_size,create_time,deleted)
SELECT a.id,CONCAT('APPR-SEED-F-',a.id),'审核联调证明.pdf','application/pdf',1024,NOW(),0
FROM application a WHERE a.application_no LIKE 'APPR-SEED-%';

-- 所有申请均记录学生提交。
INSERT INTO approval_record(
    application_id,review_round,approval_level,approver_id,approver_name_snapshot,
    action,comment,old_status,new_status,modified_fields,request_id,create_time
)
SELECT a.id,0,'STUDENT',a.create_by,'学生','SUBMIT','学生已提交申请','DRAFT','COUNSELOR_PENDING',NULL,
       CONCAT('APPR-SEED-STUDENT-',a.id),a.submit_time
FROM application a WHERE a.application_no LIKE 'APPR-SEED-%';

-- 学院/学校数据已有辅导员审核与上报轨迹。
INSERT INTO approval_record(
    application_id,review_round,approval_level,approver_id,approver_name_snapshot,
    action,comment,old_status,new_status,modified_fields,request_id,create_time
)
SELECT a.id,0,'COUNSELOR',@counselor_user_id,'辅导员','APPROVE','辅导员审核通过',
       'COUNSELOR_PENDING','COUNSELOR_PENDING',NULL,CONCAT('APPR-SEED-C-APPROVE-',a.id),DATE_ADD(a.submit_time,INTERVAL 1 HOUR)
FROM application a WHERE a.application_no LIKE 'APPR-SEED-O-%' OR a.application_no LIKE 'APPR-SEED-S-%';

INSERT INTO approval_record(
    application_id,review_round,approval_level,approver_id,approver_name_snapshot,
    action,comment,old_status,new_status,modified_fields,request_id,create_time
)
SELECT a.id,0,'COUNSELOR',@counselor_user_id,'辅导员','SUBMIT','辅导员批量上报学院',
       'COUNSELOR_PENDING','COLLEGE_PENDING',NULL,CONCAT('APPR-SEED-C-SUBMIT-',a.id),DATE_ADD(a.submit_time,INTERVAL 2 HOUR)
FROM application a WHERE a.application_no LIKE 'APPR-SEED-O-%' OR a.application_no LIKE 'APPR-SEED-S-%';

-- 学校数据已有学院审核与上报轨迹。
INSERT INTO approval_record(
    application_id,review_round,approval_level,approver_id,approver_name_snapshot,
    action,comment,old_status,new_status,modified_fields,request_id,create_time
)
SELECT a.id,0,'COLLEGE',@college_user_id,'学院管理员','APPROVE','学院审核通过',
       'COLLEGE_PENDING','COLLEGE_PENDING',NULL,CONCAT('APPR-SEED-O-APPROVE-',a.id),DATE_ADD(a.submit_time,INTERVAL 3 HOUR)
FROM application a WHERE a.application_no LIKE 'APPR-SEED-S-%';

INSERT INTO approval_record(
    application_id,review_round,approval_level,approver_id,approver_name_snapshot,
    action,comment,old_status,new_status,modified_fields,request_id,create_time
)
SELECT a.id,0,'COLLEGE',@college_user_id,'学院管理员','SUBMIT','学院批量上报学校',
       'COLLEGE_PENDING','SCHOOL_PENDING',NULL,CONCAT('APPR-SEED-O-SUBMIT-',a.id),DATE_ADD(a.submit_time,INTERVAL 4 HOUR)
FROM application a WHERE a.application_no LIKE 'APPR-SEED-S-%';

-- 记录已发生的批量上报，供批次状态条与防重复提交规则使用。
INSERT INTO approval_submission_record(
    batch_type,green_channel_batch_id,subsidy_batch_id,submission_level,submission_type,
    scope_type,scope_id,application_id,review_round,submitter_id,submitted_count,status,request_id,submit_time,create_time
)
VALUES
('GREEN_CHANNEL',@batch_o,NULL,'COUNSELOR','INITIAL_BATCH','COUNSELOR',@counselor_user_id,0,0,@counselor_user_id,20,'SUBMITTED','APPR-SEED-BATCH-C-O',DATE_SUB(NOW(),INTERVAL 8 HOUR),NOW()),
('GREEN_CHANNEL',@batch_s,NULL,'COUNSELOR','INITIAL_BATCH','COUNSELOR',@counselor_user_id,0,0,@counselor_user_id,30,'SUBMITTED','APPR-SEED-BATCH-C-S',DATE_SUB(NOW(),INTERVAL 7 HOUR),NOW()),
('GREEN_CHANNEL',@batch_s,NULL,'COLLEGE','INITIAL_BATCH','COLLEGE',@college_id,0,0,@college_user_id,30,'SUBMITTED','APPR-SEED-BATCH-O-S',DATE_SUB(NOW(),INTERVAL 6 HOUR),NOW());

COMMIT;

-- 验证：COUNSELOR_PENDING=10、COLLEGE_PENDING=20、SCHOOL_PENDING=30；金额区间逐级增长；无效行必须为 0。
SELECT status,COUNT(*) row_count,MIN(aa.declared_amount) min_amount,MAX(aa.declared_amount) max_amount
FROM application a
JOIN arrears_application aa ON aa.application_id=a.id AND aa.deleted=0
WHERE a.application_no LIKE 'APPR-SEED-%' AND a.deleted=0
GROUP BY status ORDER BY min_amount;

SELECT
    SUM(CASE WHEN a.batch_type='GREEN_CHANNEL' AND a.green_channel_batch_id IS NOT NULL AND a.subsidy_batch_id IS NULL THEN 0 ELSE 1 END) AS invalid_batch_reference,
    SUM(CASE WHEN aa.declared_amount>0 AND aa.declared_amount<=8000 THEN 0 ELSE 1 END) AS invalid_amount,
    COUNT(DISTINCT a.id) AS application_rows,
    COUNT(DISTINCT ar.application_id) AS applications_with_flow,
    COUNT(DISTINCT att.application_id) AS applications_with_attachment
FROM application a
JOIN arrears_application aa ON aa.application_id=a.id AND aa.deleted=0
LEFT JOIN approval_record ar ON ar.application_id=a.id
LEFT JOIN application_attachment att ON att.application_id=a.id AND att.deleted=0
WHERE a.application_no LIKE 'APPR-SEED-%' AND a.deleted=0;

SELECT @counselor_user_id counselor_user_id,@college_user_id college_user_id,
       @college_id college_scope_id,@school_user_id school_user_id,
       @batch_c counselor_batch_id,@batch_o college_batch_id,@batch_s school_batch_id,
       (SELECT COUNT(*) FROM appr_seed_students) selected_students;

DROP TABLE IF EXISTS appr_seed_students;
