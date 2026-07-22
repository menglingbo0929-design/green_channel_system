-- 学院账号必须从后端持久化范围生成 JWT collegeId，禁止由前端传入。
CREATE TABLE user_college_scope (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    college_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_college_scope_user (user_id),
    KEY idx_user_college_scope_college (college_id)
) ENGINE = oceanbase;

-- 演示库兼容：将已有学院角色账号映射到第一个启用学院，后续可在基础数据中调整。
INSERT INTO user_college_scope (user_id, college_id)
SELECT u.id, (SELECT MIN(c.id) FROM college c WHERE c.deleted = 0 AND c.enabled = 1)
FROM sys_user u
JOIN sys_user_role ur ON ur.user_id = u.id
JOIN sys_role r ON r.id = ur.role_id AND r.role_code = 'COLLEGE'
WHERE u.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM user_college_scope ucs WHERE ucs.user_id = u.id)
  AND EXISTS (SELECT 1 FROM college c WHERE c.deleted = 0 AND c.enabled = 1);
