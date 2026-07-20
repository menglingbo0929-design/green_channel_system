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
