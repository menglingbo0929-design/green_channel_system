-- A reservation and a confirmed use are disjoint resource states.  Their sum,
-- rather than their relative order, must stay within the configured total.
ALTER TABLE batch_gift_item
    DROP CHECK chk_batch_gift_item_counts,
    ADD CONSTRAINT chk_batch_gift_item_counts
        CHECK (stock_total >= reserved_count + used_count AND reserved_count >= 0 AND used_count >= 0);

ALTER TABLE college_gift_quota
    DROP CHECK chk_college_gift_quota_counts,
    ADD CONSTRAINT chk_college_gift_quota_counts
        CHECK (quota_total >= reserved_count + used_count AND reserved_count >= 0 AND used_count >= 0);

ALTER TABLE grade_gift_quota
    DROP CHECK chk_grade_gift_quota_counts,
    ADD CONSTRAINT chk_grade_gift_quota_counts
        CHECK (quota_total >= reserved_count + used_count AND reserved_count >= 0 AND used_count >= 0);

ALTER TABLE college_subsidy_quota
    DROP CHECK chk_college_subsidy_quota_amounts,
    ADD CONSTRAINT chk_college_subsidy_quota_amounts
        CHECK (quota_amount >= reserved_amount + used_amount AND reserved_amount >= 0 AND used_amount >= 0);

ALTER TABLE grade_subsidy_quota
    DROP CHECK chk_grade_subsidy_quota_amounts,
    ADD CONSTRAINT chk_grade_subsidy_quota_amounts
        CHECK (quota_amount >= reserved_amount + used_amount AND reserved_amount >= 0 AND used_amount >= 0);
