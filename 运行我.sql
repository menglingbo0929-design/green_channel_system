-- ========================================
-- 1. 修student表class_id允许为空
ALTER TABLE student MODIFY class_id bigint(20) NULL DEFAULT NULL;

-- ========================================
-- 2. 成员二 申请模块
-- 成员二：申请配置与资源表
-- MySQL 8.0.16+ / Asia/Shanghai / utf8mb4

CREATE TABLE fee_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_name VARCHAR(100) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fee_item_name_deleted (item_name, deleted),
    CONSTRAINT chk_fee_item_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fee_amount_option (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fee_item_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fee_amount_option_item_amount_deleted (fee_item_id, amount, deleted),
    KEY idx_fee_amount_option_fee_item_id (fee_item_id),
    CONSTRAINT chk_fee_amount_option_amount CHECK (amount > 0),
    CONSTRAINT chk_fee_amount_option_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE gift_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_name VARCHAR(100) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_gift_item_name_deleted (item_name, deleted),
    CONSTRAINT chk_gift_item_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE batch_gift_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    gift_item_id BIGINT NOT NULL,
    stock_total INT NOT NULL,
    reserved_count INT NOT NULL DEFAULT 0,
    used_count INT NOT NULL DEFAULT 0,
    per_student_limit INT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_gift_item_batch_item_deleted (batch_id, gift_item_id, deleted),
    KEY idx_batch_gift_item_batch_id (batch_id),
    CONSTRAINT chk_batch_gift_item_counts CHECK (stock_total >= reserved_count AND reserved_count >= used_count AND used_count >= 0),
    CONSTRAINT chk_batch_gift_item_limit CHECK (per_student_limit > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE college_gift_quota (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    college_id BIGINT NOT NULL,
    quota_total INT NOT NULL,
    reserved_count INT NOT NULL DEFAULT 0,
    used_count INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_college_gift_quota_batch_college_deleted (batch_id, college_id, deleted),
    CONSTRAINT chk_college_gift_quota_counts CHECK (quota_total >= reserved_count AND reserved_count >= used_count AND used_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE grade_gift_quota (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    grade_id BIGINT NOT NULL,
    quota_total INT NOT NULL,
    reserved_count INT NOT NULL DEFAULT 0,
    used_count INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_grade_gift_quota_batch_grade_deleted (batch_id, grade_id, deleted),
    CONSTRAINT chk_grade_gift_quota_counts CHECK (quota_total >= reserved_count AND reserved_count >= used_count AND used_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE application (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_no VARCHAR(32) NOT NULL,
    student_id BIGINT NOT NULL,
    application_type VARCHAR(32) NOT NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'STUDENT',
    batch_type VARCHAR(32) NOT NULL,
    green_channel_batch_id BIGINT NULL,
    subsidy_batch_id BIGINT NULL,
    batch_id BIGINT GENERATED ALWAYS AS (COALESCE(green_channel_batch_id, subsidy_batch_id)) STORED,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    current_level VARCHAR(32) NOT NULL DEFAULT 'STUDENT',
    review_round INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    application_reason VARCHAR(1000) NULL,
    supplement_reason VARCHAR(500) NULL,
    supplemented_at DATETIME NULL,
    submit_time DATETIME NULL,
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_application_no (application_no),
    UNIQUE KEY uk_application_student_batch_type_deleted (student_id, batch_id, application_type, deleted),
    KEY idx_application_student_id (student_id),
    KEY idx_application_status_batch (status, green_channel_batch_id, subsidy_batch_id),
    CONSTRAINT chk_application_batch_reference CHECK ((green_channel_batch_id IS NOT NULL AND subsidy_batch_id IS NULL AND batch_type = 'GREEN_CHANNEL') OR (green_channel_batch_id IS NULL AND subsidy_batch_id IS NOT NULL AND batch_type = 'SUBSIDY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE arrears_application (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    fee_item_id BIGINT NOT NULL,
    declared_amount DECIMAL(12,2) NOT NULL,
    arrears_reason_code VARCHAR(32) NOT NULL DEFAULT 'OTHER',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_arrears_application_app_item_deleted (application_id, fee_item_id, deleted),
    KEY idx_arrears_application_application_id (application_id),
    CONSTRAINT chk_arrears_application_amount CHECK (declared_amount > 0),
    CONSTRAINT chk_arrears_reason_code CHECK (arrears_reason_code IN ('FAMILY_FINANCIAL_DIFFICULTY','FAMILY_EMERGENCY','MAJOR_ILLNESS','DISASTER_ACCIDENT','OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE gift_application (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_gift_application_application_deleted (application_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE gift_application_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    gift_application_id BIGINT NOT NULL,
    batch_gift_item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_gift_application_item_app_item_deleted (gift_application_id, batch_gift_item_id, deleted),
    CONSTRAINT chk_gift_application_item_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE subsidy_application (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    expected_amount DECIMAL(12,2) NOT NULL,
    final_amount DECIMAL(12,2) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_subsidy_application_application_deleted (application_id, deleted),
    CONSTRAINT chk_subsidy_application_expected_amount CHECK (expected_amount > 0),
    CONSTRAINT chk_subsidy_application_final_amount CHECK (final_amount IS NULL OR final_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE college_subsidy_quota (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL, college_id BIGINT NOT NULL,
    quota_amount DECIMAL(12,2) NOT NULL, reserved_amount DECIMAL(12,2) NOT NULL DEFAULT 0, used_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_college_subsidy_quota_batch_college_deleted (batch_id, college_id, deleted),
    CONSTRAINT chk_college_subsidy_quota_amounts CHECK (quota_amount >= reserved_amount AND reserved_amount >= used_amount AND used_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE grade_subsidy_quota (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL, grade_id BIGINT NOT NULL,
    quota_amount DECIMAL(12,2) NOT NULL, reserved_amount DECIMAL(12,2) NOT NULL DEFAULT 0, used_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_grade_subsidy_quota_batch_grade_deleted (batch_id, grade_id, deleted),
    CONSTRAINT chk_grade_subsidy_quota_amounts CHECK (quota_amount >= reserved_amount AND reserved_amount >= used_amount AND used_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE application_attachment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    file_id VARCHAR(64) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_application_attachment_file_id (file_id), KEY idx_application_attachment_application_id (application_id),
    CONSTRAINT chk_application_attachment_file_size CHECK (file_size > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE application_operation_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NULL,
    operation_type VARCHAR(32) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    operator_id BIGINT NULL,
    result_snapshot JSON NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_application_operation_request (request_id),
    UNIQUE KEY uk_application_operation_app_type_request (application_id, operation_type, request_id),
    KEY idx_application_operation_application_id (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE student_recommendation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL, batch_id BIGINT NOT NULL, recommendation_type VARCHAR(32) NOT NULL,
    content_snapshot JSON NOT NULL, read_flag TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_student_recommendation_student_batch_type_deleted (student_id, batch_id, recommendation_type, deleted),
    CONSTRAINT chk_student_recommendation_read_flag CHECK (read_flag IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 成员二：补录审计字段与欠费原因统计维度。
ALTER TABLE application
    ADD COLUMN supplement_reason VARCHAR(500) NULL AFTER application_reason,
    ADD COLUMN supplemented_at DATETIME NULL AFTER supplement_reason;

ALTER TABLE arrears_application
    ADD COLUMN arrears_reason_code VARCHAR(32) NOT NULL DEFAULT 'OTHER' AFTER declared_amount;

ALTER TABLE arrears_application
    ADD CONSTRAINT chk_arrears_reason_code CHECK (arrears_reason_code IN
        ('FAMILY_FINANCIAL_DIFFICULTY','FAMILY_EMERGENCY','MAJOR_ILLNESS','DISASTER_ACCIDENT','OTHER'));

-- ========================================
-- 3. 成员三 审核模块
-- 成员三：审核、上报与消息模块首版持久层。
-- 适用于尚未创建这四张成员三表的环境。

CREATE TABLE `approval_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `application_id` BIGINT NOT NULL COMMENT '申请 ID',
    `review_round` INT NOT NULL COMMENT '操作发生时的审核轮次',
    `approval_level` VARCHAR(32) NOT NULL COMMENT 'STUDENT/COUNSELOR/COLLEGE/SCHOOL/CONFIRMATION/SYSTEM',
    `approver_id` BIGINT NULL COMMENT '操作人 ID；系统动作可为空',
    `approver_name_snapshot` VARCHAR(100) NULL COMMENT '操作人姓名快照',
    `action` VARCHAR(32) NOT NULL COMMENT 'APPROVE/RETURN/REJECT/MODIFY/SUBMIT/CANCEL',
    `comment` VARCHAR(1000) NULL COMMENT '审核意见或原因',
    `old_status` VARCHAR(32) NOT NULL COMMENT '操作前状态',
    `new_status` VARCHAR(32) NOT NULL COMMENT '操作后状态',
    `modified_fields` JSON NULL COMMENT 'MODIFY 动作的脱敏修改摘要',
    `request_id` VARCHAR(64) NOT NULL COMMENT '幂等请求号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_approval_record_request` (`request_id`),
    KEY `idx_approval_record_application` (`application_id`, `create_time`, `id`),
    KEY `idx_approval_record_level` (`approval_level`, `approver_id`, `create_time`),
    KEY `idx_approval_record_round` (`application_id`, `review_round`, `approval_level`, `action`),
    CONSTRAINT `chk_approval_record_review_round` CHECK (`review_round` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申请审核操作历史记录';

CREATE TABLE `approval_submission_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `batch_type` VARCHAR(32) NOT NULL COMMENT 'GREEN_CHANNEL/SUBSIDY',
    `green_channel_batch_id` BIGINT NULL COMMENT '绿色通道批次 ID',
    `subsidy_batch_id` BIGINT NULL COMMENT '补助批次 ID',
    `submission_level` VARCHAR(32) NOT NULL COMMENT 'COUNSELOR/COLLEGE',
    `submission_type` VARCHAR(32) NOT NULL COMMENT 'INITIAL_BATCH/RETURN_RESUBMIT',
    `scope_type` VARCHAR(32) NOT NULL COMMENT 'COUNSELOR/COLLEGE',
    `scope_id` BIGINT NOT NULL COMMENT '辅导员用户 ID 或学院 ID',
    `application_id` BIGINT NOT NULL DEFAULT 0 COMMENT '首次上报为 0，退回补交为申请 ID',
    `review_round` INT NOT NULL DEFAULT 0 COMMENT '首次上报为 0，补交为当前审核轮次',
    `submitter_id` BIGINT NOT NULL COMMENT '上报人 ID',
    `submitted_count` INT NOT NULL DEFAULT 0 COMMENT '成功推进数量',
    `status` VARCHAR(32) NOT NULL COMMENT '第一阶段固定为 SUBMITTED',
    `request_id` VARCHAR(64) NOT NULL COMMENT '幂等请求号',
    `submit_time` DATETIME NOT NULL COMMENT '上报时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_submission_request` (`request_id`),
    UNIQUE KEY `uk_submission_green_scope` (`green_channel_batch_id`, `submission_level`, `scope_id`, `submission_type`, `application_id`, `review_round`),
    UNIQUE KEY `uk_submission_subsidy_scope` (`subsidy_batch_id`, `submission_level`, `scope_id`, `submission_type`, `application_id`, `review_round`),
    KEY `idx_submission_green_batch` (`green_channel_batch_id`, `submission_level`, `submit_time`),
    KEY `idx_submission_subsidy_batch` (`subsidy_batch_id`, `submission_level`, `submit_time`),
    CONSTRAINT `chk_submission_batch_reference` CHECK (
        (`batch_type` = 'GREEN_CHANNEL' AND `green_channel_batch_id` IS NOT NULL AND `subsidy_batch_id` IS NULL)
        OR
        (`batch_type` = 'SUBSIDY' AND `green_channel_batch_id` IS NULL AND `subsidy_batch_id` IS NOT NULL)
    ),
    CONSTRAINT `chk_submission_review_round` CHECK (`review_round` >= 0),
    CONSTRAINT `chk_submission_count` CHECK (`submitted_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核首次批量上报及退回补交记录';

CREATE TABLE `system_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `receiver_user_id` BIGINT NOT NULL COMMENT '接收用户 ID',
    `message_type` VARCHAR(32) NOT NULL COMMENT 'APPROVAL_RETURNED/APPROVAL_REJECTED/APPROVAL_APPROVED/BATCH_DEADLINE/OFFLINE_PROCESSING',
    `business_type` VARCHAR(32) NOT NULL COMMENT 'APPLICATION/BATCH',
    `business_id` BIGINT NOT NULL COMMENT '申请 ID 或批次 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` VARCHAR(2000) NOT NULL COMMENT '正文',
    `create_by` BIGINT NULL COMMENT '创建人；系统消息可为空',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_system_message_receiver` (`receiver_user_id`, `create_time`),
    KEY `idx_system_message_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统审核消息';

CREATE TABLE `message_read_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `message_id` BIGINT NOT NULL COMMENT '消息 ID',
    `user_id` BIGINT NOT NULL COMMENT '阅读人 ID',
    `read_time` DATETIME NOT NULL COMMENT '阅读时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_read` (`message_id`, `user_id`),
    KEY `idx_message_read_user` (`user_id`, `read_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统消息已读记录';

-- ========================================
-- 4. 成员四 确认模块
-- 负责人：成员四（feature/confirmation-statistics）
-- 用途：创建 6.1.1 欠费信息最终确认记录表
-- 依赖：application 表必须已由成员二创建；本脚本不修改 application 表，也不建立跨模块外键
-- 执行顺序：在成员二完成 application 表初始化之后执行
-- 说明：已执行的迁移不得修改；后续字段变化必须新增新的迁移文件

CREATE TABLE IF NOT EXISTS arrears_confirmation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '关联申请主表 ID',
    voucher_no VARCHAR(32) NOT NULL COMMENT '欠费单据编号，格式：GC+确认年份+申请ID六位补零',
    applied_amount DECIMAL(12, 2) NOT NULL COMMENT '学生申报欠费金额快照',
    confirmed_amount DECIMAL(12, 2) NOT NULL COMMENT '学校最终确认的实际欠费金额',
    confirm_user_id BIGINT UNSIGNED NOT NULL COMMENT '确认人（学校管理员）用户 ID',
    request_id VARCHAR(64) NOT NULL COMMENT '幂等请求号，同一请求只允许确认一次',
    confirmed_at DATETIME NOT NULL COMMENT '最终确认时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0 为有效，删除时写入本行 id',
    PRIMARY KEY (id),
    UNIQUE KEY uk_arrears_confirmation_application_id_deleted (application_id, deleted),
    UNIQUE KEY uk_arrears_confirmation_voucher_no (voucher_no),
    UNIQUE KEY uk_arrears_confirmation_request_id (request_id),
    KEY idx_arrears_confirmation_confirm_user_id (confirm_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='欠费最终确认记录';

-- Compatibility migration for databases initialized before request_id was added
-- to arrears_confirmation. Keep legacy rows unique before enforcing NOT NULL.
ALTER TABLE arrears_confirmation
    ADD COLUMN request_id VARCHAR(64) NULL AFTER confirm_user_id;

UPDATE arrears_confirmation
SET request_id = CONCAT('LEGACY-', id)
WHERE request_id IS NULL OR request_id = '';

ALTER TABLE arrears_confirmation
    MODIFY COLUMN request_id VARCHAR(64) NOT NULL,
    ADD UNIQUE KEY uk_arrears_confirmation_request_id (request_id);

-- ========================================
-- 5. 学员范围与礼包字段
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

ALTER TABLE gift_item
    ADD COLUMN image_url VARCHAR(500) NULL AFTER item_name,
    ADD COLUMN item_type VARCHAR(64) NULL AFTER image_url,
    ADD COLUMN item_size VARCHAR(64) NULL AFTER item_type,
    ADD COLUMN description VARCHAR(1000) NULL AFTER item_size,
    ADD COLUMN unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER description,
    ADD COLUMN gender_restriction VARCHAR(16) NOT NULL DEFAULT 'ALL' AFTER unit_price,
    ADD COLUMN required_flag TINYINT NOT NULL DEFAULT 0 AFTER gender_restriction;

ALTER TABLE gift_item
    ADD CONSTRAINT chk_gift_item_unit_price CHECK (unit_price >= 0),
    ADD CONSTRAINT chk_gift_item_gender_restriction CHECK (gender_restriction IN ('ALL','MALE','FEMALE')),
    ADD CONSTRAINT chk_gift_item_required_flag CHECK (required_flag IN (0, 1));
