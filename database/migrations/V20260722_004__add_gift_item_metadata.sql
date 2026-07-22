ALTER TABLE gift_item
    ADD COLUMN image_url VARCHAR(500) NULL AFTER item_name,
    ADD COLUMN item_type VARCHAR(64) NULL AFTER image_url,
    ADD COLUMN item_size VARCHAR(64) NULL AFTER item_type,
    ADD COLUMN description VARCHAR(1000) NULL AFTER item_size,
    ADD COLUMN unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER description,
    ADD COLUMN gender_restriction VARCHAR(16) NOT NULL DEFAULT 'ALL' AFTER unit_price,
    ADD COLUMN required_flag TINYINT NOT NULL DEFAULT 0 AFTER gender_restriction;

ALTER TABLE gift_item
    ADD CONSTRAINT chk_gift_item_unit_price CHECK (unit_price >= 0),
    ADD CONSTRAINT chk_gift_item_gender_restriction CHECK (gender_restriction IN ('ALL','MALE','FEMALE')),
    ADD CONSTRAINT chk_gift_item_required_flag CHECK (required_flag IN (0, 1));
