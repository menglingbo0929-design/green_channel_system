-- 成员二：补录审计字段与欠费原因统计维度。
ALTER TABLE application
    ADD COLUMN supplement_reason VARCHAR(500) NULL AFTER application_reason,
    ADD COLUMN supplemented_at DATETIME NULL AFTER supplement_reason;

ALTER TABLE arrears_application
    ADD COLUMN arrears_reason_code VARCHAR(32) NOT NULL DEFAULT 'OTHER' AFTER declared_amount;

ALTER TABLE arrears_application
    ADD CONSTRAINT chk_arrears_reason_code CHECK (arrears_reason_code IN
        ('FAMILY_FINANCIAL_DIFFICULTY','FAMILY_EMERGENCY','MAJOR_ILLNESS','DISASTER_ACCIDENT','OTHER'));
