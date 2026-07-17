-- 数据库表结构
CREATE TABLE `user`(
                        id BIGINT AUTO_INCREMENT,
                        login_name VARCHAR(255) COMMENT '用户名',
                        `password` VARCHAR(64),
                        last_login_time DATETIME,
                        remark  VARCHAR(255),
                        is_deleted   BOOL         NOT NULL DEFAULT 0 COMMENT '是否删除',
                        gmt_created  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        gmt_modified TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (id)
);
INSERT INTO user(login_name,`password`,remark) VALUES ('admin','admin','测试数据:管理员用户');
