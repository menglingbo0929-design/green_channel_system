-- 用途：为统计看板/明细报表准备 30 条可重复执行的真实关联测试数据。
-- 目标库：本机 OceanBase MySQL 模式 test 库。
-- 执行方式：先在 ODC 中选择 test，再整段执行本文件。
-- 统计口径：按 v4，APPROVED、CONFIRM_PENDING、COMPLETED 均进入统计。
-- 数据分布（用于覆盖整张看板和明细报表，不只测试状态）：
--   10 条绿色通道 COMPLETED（欠费、确认金额、审核记录，混合三种申请来源）
--   10 条绿色通道 CONFIRM_PENDING（欠费、五类原因、礼包、审核记录）
--    5 条生活补助 APPROVED（补助金额）
--    5 条路费补助 APPROVED（补助金额）
-- 同时覆盖：学院/专业/年级/班级、三种申请类型、三种统计状态、
-- STUDENT/SCHOOL_PROXY/SUPPLEMENT 来源、现有欠费项目、现有礼包物品、
-- 五类欠费原因、两个批次体系、30 天申请日期范围、确认金额及审核历史。
--
-- OceanBase MySQL 模式不支持 TEMPORARY TABLE，因此使用三个 rpt30_seed_* 普通中间表。
-- 它们只保存本脚本的 ID 映射，执行结束后会按精确表名删除；不会删除或清空业务表。
-- 每次执行先按 RPT30 固定编号清理上一轮种子业务数据，再完整重建，避免部分提交导致编号与学生错位。

SELECT DATABASE() AS current_database;

-- 若上一次 ODC 批量执行在事务中途失败，先撤销尚未提交的半截事务；
-- 已经提交的固定编号数据不会受影响，后续 NOT EXISTS 会自动跳过。
ROLLBACK;

-- 仅清理本脚本生成的 RPT30 测试申请及其子表数据，不处理其他业务申请和基础配置。
DELETE FROM gift_application_item
WHERE gift_application_id IN (
    SELECT ga.id
    FROM gift_application ga
    JOIN application a ON a.id = ga.application_id
    WHERE a.application_no LIKE 'RPT30-%'
);

DELETE FROM gift_application
WHERE application_id IN (
    SELECT a.id FROM application a WHERE a.application_no LIKE 'RPT30-%'
);

DELETE FROM arrears_application
WHERE application_id IN (
    SELECT a.id FROM application a WHERE a.application_no LIKE 'RPT30-%'
);

DELETE FROM subsidy_application
WHERE application_id IN (
    SELECT a.id FROM application a WHERE a.application_no LIKE 'RPT30-%'
);

DELETE FROM arrears_confirmation
WHERE application_id IN (
    SELECT a.id FROM application a WHERE a.application_no LIKE 'RPT30-%'
);

DELETE FROM approval_record
WHERE application_id IN (
    SELECT a.id FROM application a WHERE a.application_no LIKE 'RPT30-%'
)
   OR request_id LIKE 'RPT30-%';

DELETE FROM application_operation_record
WHERE application_id IN (
    SELECT a.id FROM application a WHERE a.application_no LIKE 'RPT30-%'
);

DELETE FROM application
WHERE application_no LIKE 'RPT30-%';

SET @green_batch_id = (
    SELECT MIN(id)
    FROM green_channel_batch
    WHERE deleted = 0
);

-- application.chk_application_batch_reference 要求补助申请必须带非空 subsidy_batch_id。
-- 测试库没有补助批次时，分别补建生活补助、路费补助批次；固定 batch_code 使本段可重复执行。
INSERT INTO subsidy_batch (
    batch_code, batch_name, batch_type, start_time, end_time,
    status, enabled, remark, create_time, update_time, deleted
)
SELECT
    'RPT30-LIVING-2026', '统计报表联调生活补助批次', 'LIVING_SUBSIDY',
    DATE_SUB(NOW(), INTERVAL 1 YEAR), DATE_ADD(NOW(), INTERVAL 1 YEAR),
    'OPEN', 1, '仅供统计报表 30 条关联数据联调', NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM subsidy_batch
    WHERE batch_code = 'RPT30-LIVING-2026' AND deleted = 0
);

INSERT INTO subsidy_batch (
    batch_code, batch_name, batch_type, start_time, end_time,
    status, enabled, remark, create_time, update_time, deleted
)
SELECT
    'RPT30-TRAVEL-2026', '统计报表联调路费补助批次', 'TRAVEL_SUBSIDY',
    DATE_SUB(NOW(), INTERVAL 1 YEAR), DATE_ADD(NOW(), INTERVAL 1 YEAR),
    'OPEN', 1, '仅供统计报表 30 条关联数据联调', NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM subsidy_batch
    WHERE batch_code = 'RPT30-TRAVEL-2026' AND deleted = 0
);

SET @living_subsidy_batch_id = (
    SELECT id
    FROM subsidy_batch
    WHERE batch_code = 'RPT30-LIVING-2026' AND deleted = 0
    LIMIT 1
);

SET @travel_subsidy_batch_id = (
    SELECT id
    FROM subsidy_batch
    WHERE batch_code = 'RPT30-TRAVEL-2026' AND deleted = 0
    LIMIT 1
);

-- 空库也能覆盖欠费项目维度：只补齐缺失的三类基础费用项。
INSERT INTO fee_item (item_name, enabled, create_time, update_time, deleted)
SELECT '学费', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM fee_item WHERE item_name = '学费' AND deleted = 0);

INSERT INTO fee_item (item_name, enabled, create_time, update_time, deleted)
SELECT '住宿费', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM fee_item WHERE item_name = '住宿费' AND deleted = 0);

INSERT INTO fee_item (item_name, enabled, create_time, update_time, deleted)
SELECT '教材费', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM fee_item WHERE item_name = '教材费' AND deleted = 0);

UPDATE fee_item
SET enabled = 1, update_time = NOW()
WHERE item_name IN ('学费', '住宿费', '教材费') AND deleted = 0 AND enabled <> 1;

SET @fee_item_id = (
    SELECT MIN(id)
    FROM fee_item
    WHERE deleted = 0 AND enabled = 1
);

SET @fee_item_count = (
    SELECT COUNT(*)
    FROM fee_item
    WHERE deleted = 0 AND enabled = 1
);

-- 空库也能覆盖礼包维度：补齐三类基础礼包，再建立当前绿色通道批次的库存关系。
INSERT INTO gift_item (item_name, enabled, create_time, update_time, deleted)
SELECT '床上用品礼包', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM gift_item WHERE item_name = '床上用品礼包' AND deleted = 0);

INSERT INTO gift_item (item_name, enabled, create_time, update_time, deleted)
SELECT '洗漱用品礼包', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM gift_item WHERE item_name = '洗漱用品礼包' AND deleted = 0);

INSERT INTO gift_item (item_name, enabled, create_time, update_time, deleted)
SELECT '学习用品礼包', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM gift_item WHERE item_name = '学习用品礼包' AND deleted = 0);

UPDATE gift_item
SET enabled = 1, update_time = NOW()
WHERE item_name IN ('床上用品礼包', '洗漱用品礼包', '学习用品礼包')
  AND deleted = 0 AND enabled <> 1;

INSERT INTO batch_gift_item (
    batch_id, gift_item_id, stock_total, reserved_count, used_count,
    per_student_limit, version, create_time, update_time, deleted
)
SELECT
    @green_batch_id, gi.id, 100, 0, 0, 2, 0, NOW(), NOW(), 0
FROM gift_item gi
WHERE gi.deleted = 0 AND gi.enabled = 1
  AND @green_batch_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM batch_gift_item bgi
      WHERE bgi.batch_id = @green_batch_id
        AND bgi.gift_item_id = gi.id
        AND bgi.deleted = 0
  );

SET @gift_item_count = (
    SELECT COUNT(*)
    FROM batch_gift_item
    WHERE batch_id = @green_batch_id AND deleted = 0
);

SET @school_user_id = (
    SELECT MIN(u.id)
    FROM sys_user u
    JOIN sys_user_role ur ON ur.user_id = u.id
    JOIN sys_role r ON r.id = ur.role_id
    WHERE u.deleted = 0 AND r.role_code = 'SCHOOL'
);

SET @school_user_name = (
    SELECT login_name
    FROM sys_user
    WHERE id = @school_user_id AND deleted = 0
    LIMIT 1
);

-- 执行前检查：批次、费用项、礼包项、学校用户都应有值，eligible_students 应为 20。
SELECT
    @green_batch_id AS green_batch_id,
    @living_subsidy_batch_id AS living_subsidy_batch_id,
    @travel_subsidy_batch_id AS travel_subsidy_batch_id,
    @fee_item_id AS first_fee_item_id,
    @fee_item_count AS enabled_fee_item_count,
    @gift_item_count AS available_batch_gift_item_count,
    @school_user_id AS school_user_id;

CREATE TABLE IF NOT EXISTS rpt30_seed_students (
    seq_no INT NOT NULL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    student_no VARCHAR(64) NOT NULL
);

TRUNCATE TABLE rpt30_seed_students;

INSERT INTO rpt30_seed_students (seq_no, student_id, student_no)
SELECT ROW_NUMBER() OVER (ORDER BY candidate.id), candidate.id, candidate.student_no
FROM (
    SELECT scoped.id, scoped.student_no
    FROM (
        SELECT
            s.id,
            s.student_no,
            s.college_id,
            s.major_id,
            s.grade_id,
            s.class_id,
            ROW_NUMBER() OVER (
                PARTITION BY s.college_id
                ORDER BY s.major_id, s.grade_id, s.class_id, s.id
            ) AS college_row_no
        FROM student s
        WHERE s.deleted = 0
    ) scoped
    -- 优先从不同学院、专业、年级和班级轮流取学生，扩大组织筛选与图表覆盖面。
    ORDER BY scoped.college_row_no, scoped.college_id,
             scoped.major_id, scoped.grade_id, scoped.class_id, scoped.id
    LIMIT 20
) candidate;

SELECT COUNT(*) AS eligible_students FROM rpt30_seed_students;

CREATE TABLE IF NOT EXISTS rpt30_seed_fee_items (
    seq_no INT NOT NULL PRIMARY KEY,
    fee_item_id BIGINT NOT NULL
);

TRUNCATE TABLE rpt30_seed_fee_items;

INSERT INTO rpt30_seed_fee_items (seq_no, fee_item_id)
SELECT ROW_NUMBER() OVER (ORDER BY id), id
FROM fee_item
WHERE deleted = 0 AND enabled = 1;

CREATE TABLE IF NOT EXISTS rpt30_seed_gift_items (
    seq_no INT NOT NULL PRIMARY KEY,
    batch_gift_item_id BIGINT NOT NULL
);

TRUNCATE TABLE rpt30_seed_gift_items;

INSERT INTO rpt30_seed_gift_items (seq_no, batch_gift_item_id)
SELECT ROW_NUMBER() OVER (ORDER BY id), id
FROM batch_gift_item
WHERE batch_id = @green_batch_id AND deleted = 0;

START TRANSACTION;

-- 1~10：已完成的绿色通道申请。
INSERT INTO application (
    application_no, student_id, application_type, source, batch_type,
    green_channel_batch_id, subsidy_batch_id, status, current_level,
    review_round, version, application_reason, submit_time,
    create_by, update_by, create_time, update_time, deleted
)
SELECT
    CONCAT('RPT30-GC-C-', LPAD(t.seq_no, 2, '0')),
    t.student_id,
    'GREEN_CHANNEL',
    CASE MOD(t.seq_no, 3)
        WHEN 0 THEN 'SUPPLEMENT'
        WHEN 1 THEN 'STUDENT'
        ELSE 'SCHOOL_PROXY'
    END,
    'GREEN_CHANNEL',
    @green_batch_id,
    NULL,
    'COMPLETED',
    'FINISHED',
    0,
    2,
    CONCAT('统计报表联调数据：已完成绿色通道申请 ', t.seq_no),
    DATE_SUB(NOW(), INTERVAL (31 - t.seq_no) DAY),
    @school_user_id,
    @school_user_id,
    DATE_SUB(NOW(), INTERVAL (31 - t.seq_no) DAY),
    DATE_SUB(NOW(), INTERVAL (21 - t.seq_no) DAY),
    0
FROM rpt30_seed_students t
WHERE t.seq_no BETWEEN 1 AND 10
  AND NOT EXISTS (
      SELECT 1 FROM application a
      WHERE a.application_no = CONCAT('RPT30-GC-C-', LPAD(t.seq_no, 2, '0'))
  );

-- 11~20：学校已通过、等待欠费确认的绿色通道申请。
INSERT INTO application (
    application_no, student_id, application_type, source, batch_type,
    green_channel_batch_id, subsidy_batch_id, status, current_level,
    review_round, version, application_reason, submit_time,
    create_by, update_by, create_time, update_time, deleted
)
SELECT
    CONCAT('RPT30-GC-P-', LPAD(t.seq_no, 2, '0')),
    t.student_id,
    'GREEN_CHANNEL',
    CASE MOD(t.seq_no, 3)
        WHEN 0 THEN 'SUPPLEMENT'
        WHEN 1 THEN 'STUDENT'
        ELSE 'SCHOOL_PROXY'
    END,
    'GREEN_CHANNEL',
    @green_batch_id,
    NULL,
    'CONFIRM_PENDING',
    'CONFIRMATION',
    0,
    2,
    CONCAT('统计报表联调数据：待欠费确认绿色通道申请 ', t.seq_no),
    DATE_SUB(NOW(), INTERVAL (31 - t.seq_no) DAY),
    @school_user_id,
    @school_user_id,
    DATE_SUB(NOW(), INTERVAL (31 - t.seq_no) DAY),
    DATE_SUB(NOW(), INTERVAL (21 - t.seq_no) DAY),
    0
FROM rpt30_seed_students t
WHERE t.seq_no BETWEEN 11 AND 20
  AND NOT EXISTS (
      SELECT 1 FROM application a
      WHERE a.application_no = CONCAT('RPT30-GC-P-', LPAD(t.seq_no, 2, '0'))
  );

-- 21~25：学校审核通过的生活补助申请。
INSERT INTO application (
    application_no, student_id, application_type, source, batch_type,
    green_channel_batch_id, subsidy_batch_id, status, current_level,
    review_round, version, application_reason, submit_time,
    create_by, update_by, create_time, update_time, deleted
)
SELECT
    CONCAT('RPT30-LS-A-', LPAD(t.seq_no + 20, 2, '0')),
    t.student_id,
    'LIVING_SUBSIDY',
    'STUDENT',
    'SUBSIDY',
    NULL,
    @living_subsidy_batch_id,
    'APPROVED',
    'FINISHED',
    0,
    1,
    CONCAT('统计报表联调数据：已通过生活补助申请 ', t.seq_no + 20),
    DATE_SUB(NOW(), INTERVAL (11 - t.seq_no) DAY),
    @school_user_id,
    @school_user_id,
    DATE_SUB(NOW(), INTERVAL (11 - t.seq_no) DAY),
    DATE_SUB(NOW(), INTERVAL (6 - FLOOR(t.seq_no / 2)) DAY),
    0
FROM rpt30_seed_students t
WHERE t.seq_no BETWEEN 1 AND 5
  AND NOT EXISTS (
      SELECT 1 FROM application a
      WHERE a.application_no = CONCAT('RPT30-LS-A-', LPAD(t.seq_no + 20, 2, '0'))
  );

-- 26~30：学校审核通过的路费补助申请。
INSERT INTO application (
    application_no, student_id, application_type, source, batch_type,
    green_channel_batch_id, subsidy_batch_id, status, current_level,
    review_round, version, application_reason, submit_time,
    create_by, update_by, create_time, update_time, deleted
)
SELECT
    CONCAT('RPT30-TS-A-', LPAD(t.seq_no + 20, 2, '0')),
    t.student_id,
    'TRAVEL_SUBSIDY',
    'STUDENT',
    'SUBSIDY',
    NULL,
    @travel_subsidy_batch_id,
    'APPROVED',
    'FINISHED',
    0,
    1,
    CONCAT('统计报表联调数据：已通过路费补助申请 ', t.seq_no + 20),
    DATE_SUB(NOW(), INTERVAL (11 - t.seq_no) DAY),
    @school_user_id,
    @school_user_id,
    DATE_SUB(NOW(), INTERVAL (11 - t.seq_no) DAY),
    DATE_SUB(NOW(), INTERVAL (6 - FLOOR(t.seq_no / 2)) DAY),
    0
FROM rpt30_seed_students t
WHERE t.seq_no BETWEEN 6 AND 10
  AND NOT EXISTS (
      SELECT 1 FROM application a
      WHERE a.application_no = CONCAT('RPT30-TS-A-', LPAD(t.seq_no + 20, 2, '0'))
  );

-- test 库可能残留没有外键约束的旧测试子表行，其 application_id 会与新自增 ID 碰撞。
-- 主申请生成后再按本次 30 个实际 ID 精确清理一遍，随后由下方语句重建全部关联数据。
DELETE FROM gift_application_item
WHERE gift_application_id IN (
    SELECT ga.id
    FROM gift_application ga
    JOIN application a ON a.id = ga.application_id
    WHERE a.application_no LIKE 'RPT30-%'
);

DELETE FROM gift_application
WHERE application_id IN (
    SELECT a.id FROM application a WHERE a.application_no LIKE 'RPT30-%'
);

DELETE FROM arrears_application
WHERE application_id IN (
    SELECT a.id FROM application a WHERE a.application_no LIKE 'RPT30-%'
);

DELETE FROM subsidy_application
WHERE application_id IN (
    SELECT a.id FROM application a WHERE a.application_no LIKE 'RPT30-%'
);

DELETE FROM arrears_confirmation
WHERE application_id IN (
    SELECT a.id FROM application a WHERE a.application_no LIKE 'RPT30-%'
);

DELETE FROM approval_record
WHERE application_id IN (
    SELECT a.id FROM application a WHERE a.application_no LIKE 'RPT30-%'
)
   OR request_id LIKE 'RPT30-%';

DELETE FROM application_operation_record
WHERE application_id IN (
    SELECT a.id FROM application a WHERE a.application_no LIKE 'RPT30-%'
);

UPDATE application
SET status = 'COMPLETED', current_level = 'FINISHED'
WHERE application_no LIKE 'RPT30-GC-C-%';

UPDATE application
SET status = 'CONFIRM_PENDING', current_level = 'CONFIRMATION'
WHERE application_no LIKE 'RPT30-GC-P-%';

UPDATE application
SET batch_type = 'SUBSIDY', green_channel_batch_id = NULL,
    subsidy_batch_id = @living_subsidy_batch_id,
    status = 'APPROVED', current_level = 'FINISHED'
WHERE application_no LIKE 'RPT30-LS-A-%';

UPDATE application
SET batch_type = 'SUBSIDY', green_channel_batch_id = NULL,
    subsidy_batch_id = @travel_subsidy_batch_id,
    status = 'APPROVED', current_level = 'FINISHED'
WHERE application_no LIKE 'RPT30-TS-A-%';

-- 20 条绿色通道欠费明细，供欠费人数、原因、申报金额和待确认列表使用。
INSERT INTO arrears_application (
    application_id, fee_item_id, declared_amount, arrears_reason_code,
    create_time, update_time, deleted
)
SELECT
    a.id,
    fi.fee_item_id,
    1000.00 + t.seq_no * 100.00,
    CASE MOD(t.seq_no, 5)
        WHEN 0 THEN 'FAMILY_FINANCIAL_DIFFICULTY'
        WHEN 1 THEN 'FAMILY_EMERGENCY'
        WHEN 2 THEN 'MAJOR_ILLNESS'
        WHEN 3 THEN 'DISASTER_ACCIDENT'
        ELSE 'OTHER'
    END,
    a.create_time,
    a.update_time,
    0
FROM rpt30_seed_students t
JOIN application a ON a.application_no = CASE
    WHEN t.seq_no <= 10 THEN CONCAT('RPT30-GC-C-', LPAD(t.seq_no, 2, '0'))
    ELSE CONCAT('RPT30-GC-P-', LPAD(t.seq_no, 2, '0'))
END
JOIN rpt30_seed_fee_items fi
  ON fi.seq_no = MOD(t.seq_no - 1, NULLIF(@fee_item_count, 0)) + 1
WHERE t.seq_no BETWEEN 1 AND 20
  AND NOT EXISTS (
      SELECT 1
      FROM arrears_application aa
      WHERE aa.application_id = a.id
        AND aa.fee_item_id = fi.fee_item_id
        AND aa.deleted = 0
  );

-- 偶数序号绿色通道申请再增加第二个不同欠费项目，用于验证多项目名称聚合和项目筛选。
INSERT INTO arrears_application (
    application_id, fee_item_id, declared_amount, arrears_reason_code,
    create_time, update_time, deleted
)
SELECT
    a.id,
    fi.fee_item_id,
    300.00 + t.seq_no * 20.00,
    CASE MOD(t.seq_no + 2, 5)
        WHEN 0 THEN 'FAMILY_FINANCIAL_DIFFICULTY'
        WHEN 1 THEN 'FAMILY_EMERGENCY'
        WHEN 2 THEN 'MAJOR_ILLNESS'
        WHEN 3 THEN 'DISASTER_ACCIDENT'
        ELSE 'OTHER'
    END,
    a.create_time,
    a.update_time,
    0
FROM rpt30_seed_students t
JOIN application a ON a.application_no = CASE
    WHEN t.seq_no <= 10 THEN CONCAT('RPT30-GC-C-', LPAD(t.seq_no, 2, '0'))
    ELSE CONCAT('RPT30-GC-P-', LPAD(t.seq_no, 2, '0'))
END
JOIN rpt30_seed_fee_items fi
  ON fi.seq_no = MOD(t.seq_no, NULLIF(@fee_item_count, 0)) + 1
WHERE t.seq_no BETWEEN 1 AND 20
  AND MOD(t.seq_no, 2) = 0
  AND @fee_item_count > 1
  AND NOT EXISTS (
      SELECT 1
      FROM arrears_application aa
      WHERE aa.application_id = a.id
        AND aa.fee_item_id = fi.fee_item_id
        AND aa.deleted = 0
  );

-- 前 12 条绿色通道申请增加礼包申请，循环覆盖当前批次已有礼包物品。
INSERT INTO gift_application (application_id, create_time, update_time, deleted)
SELECT a.id, a.create_time, a.update_time, 0
FROM rpt30_seed_students t
JOIN application a ON a.application_no = CASE
    WHEN t.seq_no <= 10 THEN CONCAT('RPT30-GC-C-', LPAD(t.seq_no, 2, '0'))
    ELSE CONCAT('RPT30-GC-P-', LPAD(t.seq_no, 2, '0'))
END
WHERE t.seq_no BETWEEN 1 AND 12
  AND @gift_item_count > 0
  AND NOT EXISTS (
      SELECT 1 FROM gift_application ga
      WHERE ga.application_id = a.id AND ga.deleted = 0
  );

INSERT INTO gift_application_item (
    gift_application_id, batch_gift_item_id, quantity, create_time, update_time, deleted
)
SELECT
    ga.id,
    gi.batch_gift_item_id,
    1 + MOD(t.seq_no, 2),
    a.create_time,
    a.update_time,
    0
FROM rpt30_seed_students t
JOIN application a ON a.application_no = CASE
    WHEN t.seq_no <= 10 THEN CONCAT('RPT30-GC-C-', LPAD(t.seq_no, 2, '0'))
    ELSE CONCAT('RPT30-GC-P-', LPAD(t.seq_no, 2, '0'))
END
JOIN gift_application ga ON ga.application_id = a.id AND ga.deleted = 0
JOIN rpt30_seed_gift_items gi
  ON gi.seq_no = MOD(t.seq_no - 1, NULLIF(@gift_item_count, 0)) + 1
WHERE t.seq_no BETWEEN 1 AND 12
  AND NOT EXISTS (
      SELECT 1 FROM gift_application_item gai
      WHERE gai.gift_application_id = ga.id
        AND gai.batch_gift_item_id = gi.batch_gift_item_id
        AND gai.deleted = 0
  );

-- 10 条补助明细（5 条生活补助、5 条路费补助）。
INSERT INTO subsidy_application (
    application_id, expected_amount, final_amount, create_time, update_time, deleted
)
SELECT
    a.id,
    500.00 + t.seq_no * 50.00,
    500.00 + t.seq_no * 50.00,
    a.create_time,
    a.update_time,
    0
FROM rpt30_seed_students t
JOIN application a ON a.application_no = CASE
    WHEN t.seq_no <= 5 THEN CONCAT('RPT30-LS-A-', LPAD(t.seq_no + 20, 2, '0'))
    ELSE CONCAT('RPT30-TS-A-', LPAD(t.seq_no + 20, 2, '0'))
END
WHERE t.seq_no BETWEEN 1 AND 10
  AND NOT EXISTS (
      SELECT 1
      FROM subsidy_application sa
      WHERE sa.application_id = a.id AND sa.deleted = 0
  );

-- 30 条学校最终审核记录。
INSERT INTO approval_record (
    application_id, review_round, approval_level, approver_id,
    approver_name_snapshot, action, comment, old_status, new_status,
    request_id, create_time
)
SELECT
    a.id,
    0,
    'SCHOOL',
    @school_user_id,
    @school_user_name,
    'APPROVE',
    '统计报表联调：学校审核通过',
    'SCHOOL_PENDING',
    CASE
        WHEN a.application_type = 'GREEN_CHANNEL' THEN 'CONFIRM_PENDING'
        ELSE 'APPROVED'
    END,
    CONCAT('RPT30-SCHOOL-', a.application_no),
    DATE_SUB(a.update_time, INTERVAL 1 HOUR)
FROM application a
WHERE a.application_no LIKE 'RPT30-%'
  AND NOT EXISTS (
      SELECT 1 FROM approval_record ar
      WHERE ar.request_id = CONCAT('RPT30-SCHOOL-', a.application_no)
  );

-- 前 10 条已完成绿色通道申请的欠费确认记录；确认金额为申报金额的 80%。
INSERT INTO arrears_confirmation (
    application_id, voucher_no, applied_amount, confirmed_amount,
    confirm_user_id, request_id, confirmed_at, created_at, updated_at, deleted
)
SELECT
    a.id,
    CONCAT('GC', YEAR(NOW()), LPAD(a.id, 6, '0')),
    SUM(aa.declared_amount),
    ROUND(SUM(aa.declared_amount) * 0.80, 2),
    @school_user_id,
    CONCAT('RPT30-CONFIRM-', LPAD(t.seq_no, 2, '0')),
    a.update_time,
    a.update_time,
    a.update_time,
    0
FROM rpt30_seed_students t
JOIN application a ON a.application_no = CASE
    WHEN t.seq_no <= 10 THEN CONCAT('RPT30-GC-C-', LPAD(t.seq_no, 2, '0'))
    ELSE CONCAT('RPT30-GC-P-', LPAD(t.seq_no, 2, '0'))
END
JOIN arrears_application aa ON aa.application_id = a.id AND aa.deleted = 0
WHERE t.seq_no BETWEEN 1 AND 10
  AND NOT EXISTS (
      SELECT 1 FROM arrears_confirmation ac
      WHERE ac.application_id = a.id AND ac.deleted = 0
  )
GROUP BY a.id, a.update_time, t.seq_no;

-- COMPLETED 对应的确认完成审核记录。
INSERT INTO approval_record (
    application_id, review_round, approval_level, approver_id,
    approver_name_snapshot, action, comment, old_status, new_status,
    request_id, create_time
)
SELECT
    a.id,
    0,
    'CONFIRMATION',
    @school_user_id,
    @school_user_name,
    'APPROVE',
    '统计报表联调：欠费确认完成',
    'CONFIRM_PENDING',
    'COMPLETED',
    CONCAT('RPT30-COMPLETE-', LPAD(t.seq_no, 2, '0')),
    a.update_time
FROM rpt30_seed_students t
JOIN application a ON a.application_no = CASE
    WHEN t.seq_no <= 10 THEN CONCAT('RPT30-GC-C-', LPAD(t.seq_no, 2, '0'))
    ELSE CONCAT('RPT30-GC-P-', LPAD(t.seq_no, 2, '0'))
END
WHERE t.seq_no BETWEEN 1 AND 10
  AND NOT EXISTS (
      SELECT 1 FROM approval_record ar
      WHERE ar.request_id = CONCAT('RPT30-COMPLETE-', LPAD(t.seq_no, 2, '0'))
  );

COMMIT;

-- 结果核验：第一项应为 30；COMPLETED=10，CONFIRM_PENDING=10，APPROVED=10。
SELECT COUNT(*) AS report_rows
FROM application
WHERE application_no LIKE 'RPT30-%' AND deleted = 0;

SELECT status, COUNT(*) AS row_count
FROM application
WHERE application_no LIKE 'RPT30-%' AND deleted = 0
GROUP BY status
ORDER BY status;

SELECT application_type, source, COUNT(*) AS row_count
FROM application
WHERE application_no LIKE 'RPT30-%' AND deleted = 0
GROUP BY application_type, source
ORDER BY application_type, source;

SELECT aa.arrears_reason_code, COUNT(DISTINCT aa.application_id) AS application_count,
       SUM(aa.declared_amount) AS declared_amount
FROM application a
JOIN arrears_application aa ON aa.application_id = a.id AND aa.deleted = 0
WHERE a.application_no LIKE 'RPT30-%' AND a.deleted = 0
GROUP BY aa.arrears_reason_code
ORDER BY aa.arrears_reason_code;

SELECT gi.item_name, COUNT(DISTINCT a.id) AS application_count,
       SUM(gai.quantity) AS requested_quantity
FROM application a
JOIN gift_application ga ON ga.application_id = a.id AND ga.deleted = 0
JOIN gift_application_item gai ON gai.gift_application_id = ga.id AND gai.deleted = 0
JOIN batch_gift_item bgi ON bgi.id = gai.batch_gift_item_id AND bgi.deleted = 0
JOIN gift_item gi ON gi.id = bgi.gift_item_id AND gi.deleted = 0
WHERE a.application_no LIKE 'RPT30-%' AND a.deleted = 0
GROUP BY gi.id, gi.item_name
ORDER BY gi.id;

SELECT
    COUNT(DISTINCT aa.application_id) AS arrears_applications,
    COUNT(DISTINCT sa.application_id) AS subsidy_applications,
    COUNT(DISTINCT ga.application_id) AS gift_applications,
    COUNT(DISTINCT ac.application_id) AS confirmed_applications,
    COUNT(DISTINCT ar.application_id) AS applications_with_approval_history
FROM application a
LEFT JOIN arrears_application aa ON aa.application_id = a.id AND aa.deleted = 0
LEFT JOIN subsidy_application sa ON sa.application_id = a.id AND sa.deleted = 0
LEFT JOIN gift_application ga ON ga.application_id = a.id AND ga.deleted = 0
LEFT JOIN arrears_confirmation ac ON ac.application_id = a.id AND ac.deleted = 0
LEFT JOIN approval_record ar ON ar.application_id = a.id
WHERE a.application_no LIKE 'RPT30-%' AND a.deleted = 0;

-- 数据流一致性核验：应为 completed_confirmed=10、pending_wrong_confirmed=0、subsidy_wrong_confirmed=0。
SELECT
    COUNT(DISTINCT CASE
        WHEN a.status = 'COMPLETED' AND ac.id IS NOT NULL THEN a.id
    END) AS completed_confirmed,
    COUNT(DISTINCT CASE
        WHEN a.status = 'CONFIRM_PENDING' AND ac.id IS NOT NULL THEN a.id
    END) AS pending_wrong_confirmed,
    COUNT(DISTINCT CASE
        WHEN a.application_type IN ('LIVING_SUBSIDY', 'TRAVEL_SUBSIDY')
             AND ac.id IS NOT NULL THEN a.id
    END) AS subsidy_wrong_confirmed
FROM application a
LEFT JOIN arrears_confirmation ac ON ac.application_id = a.id AND ac.deleted = 0
WHERE a.application_no LIKE 'RPT30-%' AND a.deleted = 0;

-- 只清理本脚本创建的三个中间映射表，不处理任何业务表或业务数据。
DROP TABLE IF EXISTS rpt30_seed_gift_items;
DROP TABLE IF EXISTS rpt30_seed_fee_items;
DROP TABLE IF EXISTS rpt30_seed_students;
