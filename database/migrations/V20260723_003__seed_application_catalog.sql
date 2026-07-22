-- 申请中心基础目录，解决空库中费用项、金额档位和礼包物品完全为空的问题。
INSERT INTO fee_item(item_name,enabled,create_time,update_time,deleted)
SELECT '学费',1,NOW(),NOW(),0
WHERE NOT EXISTS (SELECT 1 FROM fee_item WHERE item_name='学费' AND deleted=0);
INSERT INTO fee_item(item_name,enabled,create_time,update_time,deleted)
SELECT '住宿费',1,NOW(),NOW(),0
WHERE NOT EXISTS (SELECT 1 FROM fee_item WHERE item_name='住宿费' AND deleted=0);
INSERT INTO fee_item(item_name,enabled,create_time,update_time,deleted)
SELECT '教材费',1,NOW(),NOW(),0
WHERE NOT EXISTS (SELECT 1 FROM fee_item WHERE item_name='教材费' AND deleted=0);

INSERT INTO fee_amount_option(fee_item_id,amount,enabled,create_time,update_time,deleted)
SELECT fi.id,amounts.amount,1,NOW(),NOW(),0
FROM fee_item fi
CROSS JOIN (
    SELECT 500.00 amount UNION ALL SELECT 1000.00 UNION ALL SELECT 1500.00 UNION ALL SELECT 2000.00
) amounts
WHERE fi.item_name IN ('学费','住宿费','教材费') AND fi.deleted=0
  AND NOT EXISTS (
      SELECT 1 FROM fee_amount_option fao
      WHERE fao.fee_item_id=fi.id AND fao.amount=amounts.amount AND fao.deleted=0
  );

INSERT INTO gift_item(
    item_name,image_url,item_type,item_size,description,unit_price,
    gender_restriction,required_flag,enabled,create_time,update_time,deleted
)
SELECT '床上用品礼包',NULL,'生活用品','标准套装','被褥、床单及枕套',280.00,'ALL',1,1,NOW(),NOW(),0
WHERE NOT EXISTS (SELECT 1 FROM gift_item WHERE item_name='床上用品礼包' AND deleted=0);
INSERT INTO gift_item(
    item_name,image_url,item_type,item_size,description,unit_price,
    gender_restriction,required_flag,enabled,create_time,update_time,deleted
)
SELECT '洗漱用品礼包',NULL,'生活用品','标准套装','基础洗漱用品',80.00,'ALL',0,1,NOW(),NOW(),0
WHERE NOT EXISTS (SELECT 1 FROM gift_item WHERE item_name='洗漱用品礼包' AND deleted=0);
INSERT INTO gift_item(
    item_name,image_url,item_type,item_size,description,unit_price,
    gender_restriction,required_flag,enabled,create_time,update_time,deleted
)
SELECT '学习用品礼包',NULL,'学习用品','标准套装','笔记本、签字笔及文件袋',60.00,'ALL',0,1,NOW(),NOW(),0
WHERE NOT EXISTS (SELECT 1 FROM gift_item WHERE item_name='学习用品礼包' AND deleted=0);
