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
