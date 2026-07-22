-- 数据库表结构

-- 用户表
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `login_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `last_login_time` datetime NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `deleted` bigint(20) NOT NULL DEFAULT 0,
  `gmt_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_login_name`(`login_name`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- 角色表
CREATE TABLE `sys_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码',
  `role_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色中文名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code`) USING BTREE
) ENGINE = oceanbase CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- 用户角色关联表
CREATE TABLE `sys_user_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_role_id`(`role_id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_id`, `role_id`) USING BTREE
) ENGINE = oceanbase CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- 学院表
CREATE TABLE `college` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `college_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学院编码',
  `college_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学院名称',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '启用状态 0=停用 1=启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=有效',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_college_code`(`college_code`) USING BTREE,
  UNIQUE INDEX `uk_college_name`(`college_name`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- 专业表
CREATE TABLE `major` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `major_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专业编码',
  `major_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专业名称',
  `college_id` bigint(20) NOT NULL COMMENT '所属学院ID',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '启用状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_major_code`(`major_code`) USING BTREE,
  INDEX `idx_major_college`(`college_id`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- 年级表
CREATE TABLE `grade` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `grade_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '年级编码，如 2024',
  `grade_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '年级名称，如 2024级',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '启用状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_grade_code`(`grade_code`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- 班级表
CREATE TABLE `class_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `class_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班级编码',
  `class_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班级名称，如 计科2401',
  `major_id` bigint(20) NOT NULL COMMENT '所属专业ID',
  `grade_id` bigint(20) NOT NULL COMMENT '所属年级ID',
  `college_id` bigint(20) NOT NULL COMMENT '所属学院ID（冗余，加速查询）',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '启用状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_class_code`(`class_code`) USING BTREE,
  INDEX `idx_class_major`(`major_id`) USING BTREE,
  INDEX `idx_class_grade`(`grade_id`) USING BTREE,
  INDEX `idx_class_college`(`college_id`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- 学生表
CREATE TABLE `student` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学号',
  `student_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '姓名',
  `college_id` bigint(20) NOT NULL COMMENT '所属学院ID',
  `major_id` bigint(20) NOT NULL COMMENT '所属专业ID',
  `grade_id` bigint(20) NOT NULL COMMENT '所属年级ID',
  `class_id` bigint(20) NOT NULL COMMENT '所属班级ID',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系电话',
  `origin_loan` tinyint(4) NULL DEFAULT 0 COMMENT '生源地贷款 0=无 1=有',
  `campus_loan` tinyint(4) NULL DEFAULT 0 COMMENT '拟申请校园地贷款 0=否 1=是',
  `subsidy_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '资助认定等级',
  `difficulty_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '家庭困难等级',
  `info_complete` tinyint(4) NULL DEFAULT 0 COMMENT '信息是否完善 0=否 1=是',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '关联sys_user.id，导入时自动创建',
  `counselor_id` bigint(20) NULL DEFAULT NULL COMMENT '辅导员用户ID',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '启用状态',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_no`(`student_no`) USING BTREE,
  INDEX `idx_student_college`(`college_id`) USING BTREE,
  INDEX `idx_student_major`(`major_id`) USING BTREE,
  INDEX `idx_student_grade`(`grade_id`) USING BTREE,
  INDEX `idx_student_class`(`class_id`) USING BTREE,
  INDEX `idx_student_user`(`user_id`) USING BTREE,
  INDEX `idx_student_counselor`(`counselor_id`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- 辅导员-学生关联表
CREATE TABLE `counselor_student` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `counselor_user_id` bigint(20) NOT NULL COMMENT '辅导员用户ID，关联sys_user.id',
  `student_id` bigint(20) NOT NULL COMMENT '学生ID，关联student.id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_counselor_student`(`counselor_user_id`, `student_id`) USING BTREE,
  INDEX `idx_cs_counselor`(`counselor_user_id`) USING BTREE,
  INDEX `idx_cs_student`(`student_id`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- 绿色通道批次表
CREATE TABLE `green_channel_batch` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `batch_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '批次编号',
  `batch_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '批次名称，如 2026年绿色通道',
  `start_time` datetime NOT NULL COMMENT '学生申请开始时间',
  `end_time` datetime NOT NULL COMMENT '学生申请截止时间',
  `college_deadline` datetime NOT NULL COMMENT '学院上报学校截止时间',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT '批次状态：DRAFT=草稿 OPEN=开放申请 CLOSED=已关闭',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '启用状态',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_batch_code`(`batch_code`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- 绿色通道批次适用年级关联表
CREATE TABLE `batch_eligible_grade` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `batch_id` bigint(20) NOT NULL COMMENT '批次ID，关联green_channel_batch.id',
  `grade_id` bigint(20) NOT NULL COMMENT '年级ID，关联grade.id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_batch_grade`(`batch_id`, `grade_id`) USING BTREE,
  INDEX `idx_beg_batch`(`batch_id`) USING BTREE,
  INDEX `idx_beg_grade`(`grade_id`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- 补助批次表
CREATE TABLE `subsidy_batch` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `batch_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '批次编号',
  `batch_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '批次名称，如 2026年生活补助',
  `batch_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '补助类型：LIVING_SUBSIDY=生活补助 TRAVEL_SUBSIDY=路费补助',
  `start_time` datetime NOT NULL COMMENT '申请开始时间',
  `end_time` datetime NOT NULL COMMENT '申请截止时间',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT '批次状态：DRAFT=草稿 OPEN=开放申请 CLOSED=已关闭',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '启用状态',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_subsidy_batch_code`(`batch_code`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- 补助批次适用年级关联表
CREATE TABLE `subsidy_batch_eligible_grade` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `batch_id` bigint(20) NOT NULL COMMENT '补助批次ID，关联subsidy_batch.id',
  `grade_id` bigint(20) NOT NULL COMMENT '年级ID，关联grade.id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_subsidy_batch_grade`(`batch_id`, `grade_id`) USING BTREE,
  INDEX `idx_sbeg_batch`(`batch_id`) USING BTREE,
  INDEX `idx_sbeg_grade`(`grade_id`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- 学生标签表
CREATE TABLE `student_tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tag_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签编码',
  `tag_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称，如 建档立卡/低保/孤儿/残疾',
  `tag_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签分类：DIFFICULTY=困难类型 SUBSIDY=资助类型 SPECIAL=特殊群体',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '启用状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tag_code`(`tag_code`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- 政策规则表
CREATE TABLE `policy_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `rule_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则编码',
  `rule_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
  `rule_content` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '规则内容/提示文案',
  `batch_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '适用批次类型：GREEN_CHANNEL/LIVING_SUBSIDY/TRAVEL_SUBSIDY/ALL',
  `sort_order` int(11) NULL DEFAULT 0 COMMENT '排序序号',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '启用状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rule_code`(`rule_code`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- 欠费确认表
CREATE TABLE `arrears_confirmation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键、自增',
  `application_id` bigint unsigned NOT NULL COMMENT '关联申请主表 ID',
  `voucher_no` varchar(32) NOT NULL COMMENT '唯一欠费单据编号；格式为 GC + 确认年份 + 申请 ID 六位补零，例：GC2026000001',
  `applied_amount` decimal(12,2) NOT NULL COMMENT '学生申报金额快照；确认时从成员二提供的读取 Service 获取',
  `confirmed_amount` decimal(12,2) NOT NULL COMMENT '学校最终确认的实际欠费金额',
  `confirm_user_id` bigint unsigned NOT NULL COMMENT '确认学校管理员的用户 ID',
  `request_id` varchar(64) NOT NULL COMMENT '确认写操作的幂等请求号',
  `confirmed_at` datetime NOT NULL COMMENT '最终确认时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bigint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0 表示有效；删除时写入本行id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_arrears_confirmation_application_id_deleted` (`application_id`,`deleted`),
  UNIQUE KEY `uk_arrears_confirmation_voucher_no` (`voucher_no`),
  UNIQUE KEY `uk_arrears_confirmation_request_id` (`request_id`),
  KEY `idx_arrears_confirmation_confirm_user_id` (`confirm_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='欠费待确认绿色通道申请最终确认记录表(成员四维护)';

-- 审核记录表
CREATE TABLE `approval_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `application_id` bigint NOT NULL COMMENT '申请 ID',
  `review_round` int NOT NULL COMMENT '操作发生时的审核轮次',
  `approval_level` varchar(32) NOT NULL COMMENT '层级：STUDENT/COUNSELOR/COLLEGE/SCHOOL/CONFIRMATION/SYSTEM',
  `approver_id` bigint NULL DEFAULT NULL COMMENT '操作人 ID；系统动作可为空',
  `approver_name_snapshot` varchar(100) NULL DEFAULT NULL COMMENT '操作人姓名快照',
  `action` varchar(32) NOT NULL COMMENT '动作：APPROVE/RETURN/REJECT/MODIFY/SUBMIT/CANCEL',
  `comment` varchar(1000) NULL DEFAULT NULL COMMENT '审核意见或原因',
  `old_status` varchar(32) NOT NULL COMMENT '操作前状态',
  `new_status` varchar(32) NOT NULL COMMENT '操作后状态',
  `modified_fields` json NULL DEFAULT NULL COMMENT 'MODIFY 动作的修改摘要，不存身份证、完整手机号、附件',
  `request_id` varchar(64) NOT NULL COMMENT '幂等请求号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_approval_record_request` (`request_id`),
  KEY `idx_approval_record_application` (`application_id`,`create_time`,`id`),
  KEY `idx_approval_record_level` (`approval_level`,`approver_id`,`create_time`),
  KEY `idx_approval_record_round` (`application_id`,`review_round`,`approval_level`,`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='绿色通道申请审核操作历史记录表(成员三维护)';

-- 批量上报记录表
CREATE TABLE `approval_submission_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_type` varchar(32) NOT NULL COMMENT '批次类型：GREEN_CHANNEL/SUBSIDY',
  `green_channel_batch_id` bigint NULL DEFAULT NULL COMMENT '绿色通道批次 ID',
  `subsidy_batch_id` bigint NULL DEFAULT NULL COMMENT '补助批次 ID',
  `submission_level` varchar(32) NOT NULL COMMENT '上报层级：COUNSELOR/COLLEGE',
  `submission_type` varchar(32) NOT NULL COMMENT '上报类型：INITIAL_BATCH首次批量/RETURN_RESUBMIT退回补交',
  `scope_type` varchar(32) NOT NULL COMMENT '范围类型：COUNSELOR/COLLEGE',
  `scope_id` bigint NOT NULL COMMENT '辅导员用户 ID 或学院 ID',
  `application_id` bigint NOT NULL DEFAULT 0 COMMENT '首次上报为 0，退回补交为申请 ID',
  `review_round` int NOT NULL DEFAULT 0 COMMENT '首次上报为 0，补交为当前审核轮次',
  `submitter_id` bigint NOT NULL COMMENT '上报人 ID',
  `submitted_count` int NOT NULL DEFAULT 0 COMMENT '本次上报成功推进的申请数量',
  `status` varchar(32) NOT NULL COMMENT '状态，第一阶段统一使用 SUBMITTED',
  `request_id` varchar(64) NOT NULL COMMENT '幂等请求号',
  `submit_time` datetime NOT NULL COMMENT '上报操作时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_submission_request` (`request_id`),
  UNIQUE KEY `uk_submission_green_scope` (`green_channel_batch_id`,`submission_level`,`scope_id`,`submission_type`,`application_id`,`review_round`),
  UNIQUE KEY `uk_submission_subsidy_scope` (`subsidy_batch_id`,`submission_level`,`scope_id`,`submission_type`,`application_id`,`review_round`),
  KEY `idx_submission_green_batch` (`green_channel_batch_id`,`submission_level`,`submit_time`),
  KEY `idx_submission_subsidy_batch` (`subsidy_batch_id`,`submission_level`,`submit_time`),
  CONSTRAINT `chk_submission_batch_reference` CHECK (
    (`batch_type` = 'GREEN_CHANNEL' AND `green_channel_batch_id` IS NOT NULL AND `subsidy_batch_id` IS NULL)
    OR
    (`batch_type` = 'SUBSIDY' AND `green_channel_batch_id` IS NULL AND `subsidy_batch_id` IS NOT NULL)
  ),
  CONSTRAINT `chk_submission_review_round` CHECK (`review_round` >= 0),
  CONSTRAINT `chk_submission_count` CHECK (`submitted_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='辅导员/学院批量上报、退回单条补交记录表(成员三维护)';

-- 系统消息表
CREATE TABLE `system_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `receiver_user_id` bigint NOT NULL COMMENT '接收用户 ID',
  `message_type` varchar(32) NOT NULL COMMENT '消息类型（退回/拒绝/通过/截止提醒/线下办理）',
  `business_type` varchar(32) NOT NULL COMMENT '业务类型：APPLICATION单条申请 / BATCH批量批次',
  `business_id` bigint NOT NULL COMMENT '申请 ID 或批次 ID',
  `title` varchar(200) NOT NULL COMMENT '消息标题',
  `content` varchar(2000) NOT NULL COMMENT '消息正文内容',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人；系统自动推送消息可为空',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_msg_receiver` (`receiver_user_id`,`create_time` DESC),
  KEY `idx_msg_business` (`business_type`,`business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '系统消息表：退回、拒绝、通过、截止时间、线下办理审核通知';

-- 消息已读记录表
CREATE TABLE `message_read_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `message_id` bigint NOT NULL COMMENT '消息 ID，关联 system_message 主键',
  `user_id` bigint NOT NULL COMMENT '阅读人 ID',
  `read_time` datetime NOT NULL COMMENT '消息阅读时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_read` (`message_id`,`user_id`),
  KEY `idx_message_read_user` (`user_id`,`read_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '系统消息已读记录表(成员三维护)';
