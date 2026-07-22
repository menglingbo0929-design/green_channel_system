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
