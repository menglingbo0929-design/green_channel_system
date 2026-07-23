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
    CONSTRAINT chk_batch_gift_item_counts CHECK (stock_total >= reserved_count + used_count AND reserved_count >= 0 AND used_count >= 0),
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
    CONSTRAINT chk_college_gift_quota_counts CHECK (quota_total >= reserved_count + used_count AND reserved_count >= 0 AND used_count >= 0)
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
    CONSTRAINT chk_grade_gift_quota_counts CHECK (quota_total >= reserved_count + used_count AND reserved_count >= 0 AND used_count >= 0)
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
    CONSTRAINT chk_college_subsidy_quota_amounts CHECK (quota_amount >= reserved_amount + used_amount AND reserved_amount >= 0 AND used_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE grade_subsidy_quota (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL, grade_id BIGINT NOT NULL,
    quota_amount DECIMAL(12,2) NOT NULL, reserved_amount DECIMAL(12,2) NOT NULL DEFAULT 0, used_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_grade_subsidy_quota_batch_grade_deleted (batch_id, grade_id, deleted),
    CONSTRAINT chk_grade_subsidy_quota_amounts CHECK (quota_amount >= reserved_amount + used_amount AND reserved_amount >= 0 AND used_amount >= 0)
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
