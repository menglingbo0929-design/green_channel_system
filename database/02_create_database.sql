-- 数据库表结构
CREATE TABLE `user`(
                        id BIGINT AUTO_INCREMENT,
                        login_name VARCHAR(255) COMMENT '用户名',
                        `password` VARCHAR(64),
                        last_login_time DATETIME,
                        remark  VARCHAR(255),
                        is_deleted   BOOL         NOT NULL DEFAULT 0 COMMENT '是否删除',
                        gmt_created  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        gmt_modified TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (id)
);
INSERT INTO user(login_name,`password`,remark) VALUES ('admin','admin','测试数据:管理员用户');

CREATE TABLE `arrears_confirmation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键、自增',
  `application_id` bigint unsigned NOT NULL COMMENT '关联申请主表 ID',
  `voucher_no` varchar(32) NOT NULL COMMENT '唯一欠费单据编号；格式为 GC + 确认年份 + 申请 ID 六位补零，例：GC2026000001',
  `applied_amount` decimal(12,2) NOT NULL COMMENT '学生申报金额快照；确认时从成员二提供的读取 Service 获取',
  `confirmed_amount` decimal(12,2) NOT NULL COMMENT '学校最终确认的实际欠费金额',
  `confirm_user_id` bigint unsigned NOT NULL COMMENT '确认学校管理员的用户 ID',
  `confirmed_at` datetime NOT NULL COMMENT '最终确认时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bigint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除标记，0 表示有效；删除时写入本行id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_voucher_no` (`voucher_no`,`deleted`),
  KEY `idx_application_id` (`application_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='欠费待确认绿色通道申请最终确认记录表(成员四维护)';

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

CREATE TABLE `approval_submission_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` bigint NOT NULL COMMENT '批次 ID',
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
  UNIQUE KEY `uk_submission_scope` (`batch_id`,`submission_level`,`scope_id`,`submission_type`,`application_id`,`review_round`),
  KEY `idx_submission_batch` (`batch_id`,`submission_level`,`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='辅导员/学院批量上报、退回单条补交记录表(成员三维护)';

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
