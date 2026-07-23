-- Repair installations initialized before batch and policy tables were added.
-- All statements are idempotent so the script is safe for an existing local MySQL database.

CREATE TABLE IF NOT EXISTS green_channel_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_code VARCHAR(32) NOT NULL,
    batch_name VARCHAR(64) NOT NULL,
    academic_year VARCHAR(16) NOT NULL DEFAULT '2026-2027',
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    college_deadline DATETIME NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    enabled TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_code (batch_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS batch_funding_source (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    source_code VARCHAR(32) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_funding_source (batch_id, source_code),
    KEY idx_bfs_batch (batch_id),
    CONSTRAINT chk_batch_funding_source_code CHECK (source_code IN ('SCHOOL','GOVERNMENT','SOCIETY','OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS batch_eligible_grade (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    grade_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_grade (batch_id, grade_id),
    KEY idx_beg_batch (batch_id),
    KEY idx_beg_grade (grade_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS subsidy_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_code VARCHAR(32) NOT NULL,
    batch_name VARCHAR(64) NOT NULL,
    academic_year VARCHAR(16) NOT NULL DEFAULT '2026-2027',
    batch_type VARCHAR(32) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    enabled TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_subsidy_batch_code (batch_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS subsidy_batch_eligible_grade (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    grade_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_subsidy_batch_grade (batch_id, grade_id),
    KEY idx_sbeg_batch (batch_id),
    KEY idx_sbeg_grade (grade_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS policy_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_code VARCHAR(32) NOT NULL,
    rule_name VARCHAR(64) NOT NULL,
    rule_content VARCHAR(2000) NULL,
    batch_type VARCHAR(32) NULL,
    sort_order INT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
