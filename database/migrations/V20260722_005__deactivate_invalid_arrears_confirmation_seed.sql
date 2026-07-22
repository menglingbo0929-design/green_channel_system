-- 清理旧测试脚本直接写入的欠费确认数据。
-- 有效确认必须同时满足：存在有效欠费申请、确认人为有效学校管理员。
-- 使用逻辑删除保留历史痕迹，并释放 (application_id, deleted=0) 唯一键供正式流程写入。
UPDATE arrears_confirmation ac
LEFT JOIN arrears_application aa
       ON aa.application_id = ac.application_id
      AND aa.deleted = 0
LEFT JOIN sys_user su
       ON su.id = ac.confirm_user_id
      AND su.deleted = 0
LEFT JOIN sys_user_role sur
       ON sur.user_id = su.id
      AND sur.role_id = 4
SET ac.deleted = ac.id
WHERE ac.deleted = 0
  AND (aa.id IS NULL OR sur.id IS NULL);
