-- 绿色通道信息设置：补齐学年、资金来源，并为现有批次提供安全默认值。
ALTER TABLE green_channel_batch
    ADD COLUMN academic_year VARCHAR(16) NOT NULL DEFAULT '2026-2027' COMMENT '学年，如 2026-2027' AFTER batch_name;

ALTER TABLE subsidy_batch
    ADD COLUMN academic_year VARCHAR(16) NOT NULL DEFAULT '2026-2027' COMMENT '学年，如 2026-2027' AFTER batch_name;

CREATE TABLE batch_funding_source (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    source_code VARCHAR(32) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_funding_source (batch_id, source_code),
    KEY idx_bfs_batch (batch_id),
    CONSTRAINT chk_batch_funding_source_code
        CHECK (source_code IN ('SCHOOL','GOVERNMENT','SOCIETY','OTHER'))
) ENGINE = InnoDB;

INSERT INTO batch_funding_source (batch_id, source_code)
SELECT id, 'SCHOOL'
FROM green_channel_batch
WHERE deleted = 0;
