-- 提供可直接联调学生申请中心的开放批次；截止时间使用相对当前时间，避免演示数据过期。
INSERT INTO green_channel_batch(
    batch_code,batch_name,academic_year,start_time,end_time,college_deadline,
    status,enabled,remark,create_time,update_time,deleted
)
SELECT 'DEMO-GREEN-2026','2026-2027 学年绿色通道','2026-2027',
       DATE_SUB(NOW(),INTERVAL 30 DAY),DATE_ADD(NOW(),INTERVAL 30 DAY),DATE_ADD(NOW(),INTERVAL 60 DAY),
       'OPEN',1,'学生申请中心联调开放批次',NOW(),NOW(),0
WHERE NOT EXISTS (SELECT 1 FROM green_channel_batch WHERE batch_code='DEMO-GREEN-2026' AND deleted=0);

SET @demo_green_batch=(SELECT id FROM green_channel_batch WHERE batch_code='DEMO-GREEN-2026' AND deleted=0 LIMIT 1);
UPDATE green_channel_batch SET batch_name='2026-2027 学年绿色通道',academic_year='2026-2027',
    status='OPEN',enabled=1,remark='学生申请中心联调开放批次',
    start_time=DATE_SUB(NOW(),INTERVAL 30 DAY),end_time=DATE_ADD(NOW(),INTERVAL 30 DAY),
    college_deadline=DATE_ADD(NOW(),INTERVAL 60 DAY),update_time=NOW()
WHERE id=@demo_green_batch;

INSERT INTO batch_eligible_grade(batch_id,grade_id,create_time)
SELECT @demo_green_batch,g.id,NOW() FROM grade g
WHERE g.deleted=0 AND g.enabled=1
  AND NOT EXISTS (SELECT 1 FROM batch_eligible_grade beg WHERE beg.batch_id=@demo_green_batch AND beg.grade_id=g.id);
INSERT INTO batch_funding_source(batch_id,source_code)
SELECT @demo_green_batch,'SCHOOL'
WHERE NOT EXISTS (SELECT 1 FROM batch_funding_source WHERE batch_id=@demo_green_batch AND source_code='SCHOOL');

INSERT INTO subsidy_batch(
    batch_code,batch_name,academic_year,batch_type,start_time,end_time,status,enabled,remark,create_time,update_time,deleted
)
SELECT 'DEMO-LIVING-2026','2026-2027 学年生活补助','2026-2027','LIVING_SUBSIDY',
       DATE_SUB(NOW(),INTERVAL 30 DAY),DATE_ADD(NOW(),INTERVAL 30 DAY),'OPEN',1,'学生申请中心联调开放批次',NOW(),NOW(),0
WHERE NOT EXISTS (SELECT 1 FROM subsidy_batch WHERE batch_code='DEMO-LIVING-2026' AND deleted=0);
INSERT INTO subsidy_batch(
    batch_code,batch_name,academic_year,batch_type,start_time,end_time,status,enabled,remark,create_time,update_time,deleted
)
SELECT 'DEMO-TRAVEL-2026','2026-2027 学年路费补助','2026-2027','TRAVEL_SUBSIDY',
       DATE_SUB(NOW(),INTERVAL 30 DAY),DATE_ADD(NOW(),INTERVAL 30 DAY),'OPEN',1,'学生申请中心联调开放批次',NOW(),NOW(),0
WHERE NOT EXISTS (SELECT 1 FROM subsidy_batch WHERE batch_code='DEMO-TRAVEL-2026' AND deleted=0);

UPDATE subsidy_batch SET
    batch_name=CASE batch_code WHEN 'DEMO-LIVING-2026' THEN '2026-2027 学年生活补助' ELSE '2026-2027 学年路费补助' END,
    academic_year='2026-2027',status='OPEN',enabled=1,remark='学生申请中心联调开放批次',
    start_time=DATE_SUB(NOW(),INTERVAL 30 DAY),end_time=DATE_ADD(NOW(),INTERVAL 30 DAY),update_time=NOW()
WHERE batch_code IN ('DEMO-LIVING-2026','DEMO-TRAVEL-2026') AND deleted=0;

INSERT INTO subsidy_batch_eligible_grade(batch_id,grade_id,create_time)
SELECT sb.id,g.id,NOW()
FROM subsidy_batch sb CROSS JOIN grade g
WHERE sb.batch_code IN ('DEMO-LIVING-2026','DEMO-TRAVEL-2026') AND sb.deleted=0
  AND g.deleted=0 AND g.enabled=1
  AND NOT EXISTS (
      SELECT 1 FROM subsidy_batch_eligible_grade sbeg WHERE sbeg.batch_id=sb.id AND sbeg.grade_id=g.id
  );
