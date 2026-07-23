-- Upgrade legacy approval_submission_record tables that still use batch_id.
-- The current application stores green-channel and subsidy batch references
-- separately so that both workflow types can be queried correctly.

DELIMITER $$

CREATE PROCEDURE upgrade_approval_submission_batch_reference()
BEGIN
    DECLARE has_legacy_batch_id INT DEFAULT 0;
    DECLARE has_batch_type INT DEFAULT 0;
    DECLARE has_green_batch_id INT DEFAULT 0;
    DECLARE has_subsidy_batch_id INT DEFAULT 0;
    DECLARE has_old_unique INT DEFAULT 0;
    DECLARE has_old_index INT DEFAULT 0;
    DECLARE has_green_unique INT DEFAULT 0;
    DECLARE has_subsidy_unique INT DEFAULT 0;
    DECLARE has_green_index INT DEFAULT 0;
    DECLARE has_subsidy_index INT DEFAULT 0;
    DECLARE has_batch_check INT DEFAULT 0;

    SELECT COUNT(*) INTO has_legacy_batch_id FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'approval_submission_record' AND column_name = 'batch_id';
    SELECT COUNT(*) INTO has_batch_type FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'approval_submission_record' AND column_name = 'batch_type';
    SELECT COUNT(*) INTO has_green_batch_id FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'approval_submission_record' AND column_name = 'green_channel_batch_id';
    SELECT COUNT(*) INTO has_subsidy_batch_id FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'approval_submission_record' AND column_name = 'subsidy_batch_id';

    IF has_batch_type = 0 THEN
        ALTER TABLE approval_submission_record ADD COLUMN batch_type VARCHAR(32) NULL AFTER id;
    END IF;
    IF has_green_batch_id = 0 THEN
        ALTER TABLE approval_submission_record ADD COLUMN green_channel_batch_id BIGINT NULL AFTER batch_type;
    END IF;
    IF has_subsidy_batch_id = 0 THEN
        ALTER TABLE approval_submission_record ADD COLUMN subsidy_batch_id BIGINT NULL AFTER green_channel_batch_id;
    END IF;

    IF has_legacy_batch_id = 1 THEN
        UPDATE approval_submission_record
        SET batch_type = COALESCE(batch_type, 'GREEN_CHANNEL'),
            green_channel_batch_id = COALESCE(green_channel_batch_id, batch_id)
        WHERE batch_type IS NULL OR green_channel_batch_id IS NULL;
        ALTER TABLE approval_submission_record MODIFY COLUMN batch_id BIGINT NULL;
    END IF;
    ALTER TABLE approval_submission_record MODIFY COLUMN batch_type VARCHAR(32) NOT NULL;

    SELECT COUNT(*) INTO has_old_unique FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'approval_submission_record' AND index_name = 'uk_submission_scope';
    IF has_old_unique > 0 THEN
        ALTER TABLE approval_submission_record DROP INDEX uk_submission_scope;
    END IF;
    SELECT COUNT(*) INTO has_old_index FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'approval_submission_record' AND index_name = 'idx_submission_batch';
    IF has_old_index > 0 THEN
        ALTER TABLE approval_submission_record DROP INDEX idx_submission_batch;
    END IF;

    SELECT COUNT(*) INTO has_green_unique FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'approval_submission_record' AND index_name = 'uk_submission_green_scope';
    IF has_green_unique = 0 THEN
        ALTER TABLE approval_submission_record ADD UNIQUE KEY uk_submission_green_scope (green_channel_batch_id, submission_level, scope_id, submission_type, application_id, review_round);
    END IF;
    SELECT COUNT(*) INTO has_subsidy_unique FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'approval_submission_record' AND index_name = 'uk_submission_subsidy_scope';
    IF has_subsidy_unique = 0 THEN
        ALTER TABLE approval_submission_record ADD UNIQUE KEY uk_submission_subsidy_scope (subsidy_batch_id, submission_level, scope_id, submission_type, application_id, review_round);
    END IF;
    SELECT COUNT(*) INTO has_green_index FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'approval_submission_record' AND index_name = 'idx_submission_green_batch';
    IF has_green_index = 0 THEN
        ALTER TABLE approval_submission_record ADD KEY idx_submission_green_batch (green_channel_batch_id, submission_level, submit_time);
    END IF;
    SELECT COUNT(*) INTO has_subsidy_index FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'approval_submission_record' AND index_name = 'idx_submission_subsidy_batch';
    IF has_subsidy_index = 0 THEN
        ALTER TABLE approval_submission_record ADD KEY idx_submission_subsidy_batch (subsidy_batch_id, submission_level, submit_time);
    END IF;

    SELECT COUNT(*) INTO has_batch_check FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'approval_submission_record' AND constraint_name = 'chk_submission_batch_reference';
    IF has_batch_check = 0 THEN
        ALTER TABLE approval_submission_record ADD CONSTRAINT chk_submission_batch_reference CHECK (
            (batch_type = 'GREEN_CHANNEL' AND green_channel_batch_id IS NOT NULL AND subsidy_batch_id IS NULL)
            OR (batch_type = 'SUBSIDY' AND green_channel_batch_id IS NULL AND subsidy_batch_id IS NOT NULL)
        );
    END IF;
END$$

DELIMITER ;

CALL upgrade_approval_submission_batch_reference();
DROP PROCEDURE upgrade_approval_submission_batch_reference;
