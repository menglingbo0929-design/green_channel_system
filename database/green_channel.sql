/*
 Navicat Premium Data Transfer

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 50725 (5.7.25)
 Source Host           : 127.0.0.1:2881
 Source Schema         : green_channel

 Target Server Type    : MySQL
 Target Server Version : 50725 (5.7.25)
 File Encoding         : 65001

 Date: 20/07/2026 15:18:16
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for class_info
-- ----------------------------
DROP TABLE IF EXISTS `class_info`;
CREATE TABLE `class_info`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `class_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班级编码',
  `class_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班级名称，如 计科2401',
  `major_id` bigint(20) NOT NULL COMMENT '所属专业ID',
  `grade_id` bigint(20) NOT NULL COMMENT '所属年级ID',
  `college_id` bigint(20) NOT NULL COMMENT '所属学院ID（冗余，加速查询）',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '启用状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_class_code`(`class_code`) USING BTREE,
  INDEX `idx_class_major`(`major_id`) USING BTREE,
  INDEX `idx_class_grade`(`grade_id`) USING BTREE,
  INDEX `idx_class_college`(`college_id`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of class_info
-- ----------------------------
INSERT INTO `class_info` VALUES (1, 'CS2401', '计科2401', 16, 3, 9, 1, '2026-07-20 13:48:11', '2026-07-20 13:48:11', 0);
INSERT INTO `class_info` VALUES (2, 'CS2402', '计科2402', 16, 3, 9, 1, '2026-07-20 13:48:11', '2026-07-20 13:48:11', 0);
INSERT INTO `class_info` VALUES (3, 'CS2501', '计科2501', 16, 4, 9, 1, '2026-07-20 13:48:11', '2026-07-20 13:48:11', 0);
INSERT INTO `class_info` VALUES (4, 'CS2601', '计科2601', 16, 5, 9, 1, '2026-07-20 13:48:11', '2026-07-20 13:48:11', 0);
INSERT INTO `class_info` VALUES (5, 'SE2401', '软工2401', 18, 3, 10, 1, '2026-07-20 13:48:11', '2026-07-20 13:48:11', 0);
INSERT INTO `class_info` VALUES (6, 'SE2501', '软工2501', 18, 4, 10, 1, '2026-07-20 13:48:11', '2026-07-20 13:48:11', 0);
INSERT INTO `class_info` VALUES (7, 'EE2401', '电气2401', 7, 3, 4, 1, '2026-07-20 13:48:11', '2026-07-20 13:48:11', 0);
INSERT INTO `class_info` VALUES (8, 'EE2501', '电气2501', 7, 4, 4, 1, '2026-07-20 13:48:11', '2026-07-20 13:48:11', 0);
INSERT INTO `class_info` VALUES (9, 'FN2401', '金融2401', 27, 3, 16, 1, '2026-07-20 13:48:11', '2026-07-20 13:48:11', 0);

-- ----------------------------
-- Table structure for college
-- ----------------------------
DROP TABLE IF EXISTS `college`;
CREATE TABLE `college`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `college_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学院编码',
  `college_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学院名称',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '启用状态 0=停用 1=启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=有效',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_college_code`(`college_code`) USING BTREE,
  UNIQUE INDEX `uk_college_name`(`college_name`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of college
-- ----------------------------
INSERT INTO `college` VALUES (1, 'MECH', '机械工程学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (2, 'MSE', '材料科学与工程学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (3, 'EPE', '能源与动力工程学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (4, 'EE', '电气工程学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (5, 'AERO', '航天航空学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (6, 'MATH', '数学与统计学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (7, 'SOM', '管理学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (8, 'SFL', '外国语学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (9, 'CS', '计算机科学与技术学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (10, 'SE', '软件学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (11, 'ECE', '电子科学与工程学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (12, 'AI', '人工智能学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (13, 'CHEM', '化学工程与技术学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (14, 'ARCH', '人居环境与建筑工程学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (15, 'LAW', '法学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (16, 'ECON', '经济与金融学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (17, 'PHYS', '物理学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (18, 'LIFE', '生命科学与技术学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (19, 'PUBLIC', '公共政策与管理学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `college` VALUES (20, 'JOURNAL', '新闻与新媒体学院', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);

-- ----------------------------
-- Table structure for grade
-- ----------------------------
DROP TABLE IF EXISTS `grade`;
CREATE TABLE `grade`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `grade_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '年级编码，如 2024',
  `grade_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '年级名称，如 2024级',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '启用状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_grade_code`(`grade_code`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of grade
-- ----------------------------
INSERT INTO `grade` VALUES (1, '2022', '2022级', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `grade` VALUES (2, '2023', '2023级', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `grade` VALUES (3, '2024', '2024级', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `grade` VALUES (4, '2025', '2025级', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `grade` VALUES (5, '2026', '2026级', 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);

-- ----------------------------
-- Table structure for major
-- ----------------------------
DROP TABLE IF EXISTS `major`;
CREATE TABLE `major`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `major_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专业编码',
  `major_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专业名称',
  `college_id` bigint(20) NOT NULL COMMENT '所属学院ID',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '启用状态',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_major_code`(`major_code`) USING BTREE,
  INDEX `idx_major_college`(`college_id`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of major
-- ----------------------------
INSERT INTO `major` VALUES (1, 'ME001', '机械工程', 1, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (2, 'ME002', '智能制造工程', 1, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (3, 'ME003', '车辆工程', 1, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (4, 'MSE01', '材料科学与工程', 2, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (5, 'EPE01', '能源与动力工程', 3, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (6, 'EPE02', '新能源科学与工程', 3, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (7, 'EE001', '电气工程及其自动化', 4, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (8, 'AE001', '工程力学', 5, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (9, 'AE002', '飞行器设计与工程', 5, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (10, 'MA001', '数学与应用数学', 6, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (11, 'MA002', '统计学', 6, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (12, 'MG001', '工商管理', 7, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (13, 'MG002', '大数据管理与应用', 7, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (14, 'FL001', '英语', 8, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (15, 'FL002', '日语', 8, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (16, 'CS001', '计算机科学与技术', 9, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (17, 'CS002', '物联网工程', 9, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (18, 'SE001', '软件工程', 10, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (19, 'ECE01', '电子科学与技术', 11, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (20, 'ECE02', '微电子科学与工程', 11, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (21, 'AI001', '人工智能', 12, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (22, 'CE001', '化学工程与工艺', 13, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (23, 'CEV01', '土木工程', 14, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (24, 'CEV02', '建筑学', 14, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (25, 'LW001', '法学', 15, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (26, 'EC001', '经济学', 16, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (27, 'EC002', '金融学', 16, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (28, 'PH001', '应用物理学', 17, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);
INSERT INTO `major` VALUES (29, 'LS001', '生物医学工程', 18, 1, '2026-07-20 13:35:10', '2026-07-20 13:35:10', 0);

-- ----------------------------
-- Table structure for student
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学号',
  `student_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '姓名',
  `college_id` bigint(20) NOT NULL COMMENT '所属学院ID',
  `major_id` bigint(20) NOT NULL COMMENT '所属专业ID',
  `grade_id` bigint(20) NOT NULL COMMENT '所属年级ID',
  `class_id` bigint(20) NOT NULL COMMENT '所属班级ID',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系电话',
  `origin_loan` tinyint(4) NULL DEFAULT 0 COMMENT '生源地贷款 0=无 1=有',
  `campus_loan` tinyint(4) NULL DEFAULT 0 COMMENT '拟申请校园地贷款 0=否 1=是',
  `subsidy_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '资助认定等级',
  `difficulty_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '家庭困难等级',
  `info_complete` tinyint(4) NULL DEFAULT 0 COMMENT '信息是否完善 0=否 1=是',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '关联sys_user.id，导入时自动创建',
  `counselor_id` bigint(20) NULL DEFAULT NULL COMMENT '辅导员用户ID',
  `enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '启用状态',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_no`(`student_no`) USING BTREE,
  INDEX `idx_student_college`(`college_id`) USING BTREE,
  INDEX `idx_student_major`(`major_id`) USING BTREE,
  INDEX `idx_student_grade`(`grade_id`) USING BTREE,
  INDEX `idx_student_class`(`class_id`) USING BTREE,
  INDEX `idx_student_user`(`user_id`) USING BTREE,
  INDEX `idx_student_counselor`(`counselor_id`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of student
-- ----------------------------
INSERT INTO `student` VALUES (1, '123', '奶龙', 9, 16, 3, 1, '10086', 1, 1, NULL, '困难', 0, NULL, NULL, 1, NULL, '2026-07-20 14:46:07', '2026-07-20 14:46:07', 0);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `role_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码',
  `role_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色中文名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code`) USING BTREE
) ENGINE = oceanbase CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'STUDENT', '学生', '2026-07-20 00:30:36');
INSERT INTO `sys_role` VALUES (2, 'COUNSELOR', '辅导员', '2026-07-20 00:30:36');
INSERT INTO `sys_role` VALUES (3, 'COLLEGE', '学院管理员', '2026-07-20 00:30:36');
INSERT INTO `sys_role` VALUES (4, 'SCHOOL', '学校管理员', '2026-07-20 00:30:36');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `login_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `last_login_time` datetime NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `deleted` bigint(20) NOT NULL DEFAULT 0,
  `gmt_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = oceanbase ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$uMIGZZoZ5w3Ec/UrgLOYbONmINKwJ4D0VuSRyKdwVwPbiNq6MktxS', '2026-07-20 14:33:19', '测试数据:管理员用户', 0, '2026-07-17 10:30:44', '2026-07-17 10:30:44');
INSERT INTO `sys_user` VALUES (2, 'school_demo', '$2a$10$o4itNBWvrDOU58VHsV5nCOaOadVhKeea3oeq4sYaSkHvQRMH1O1Ru', '2026-07-20 11:36:15', '学校管理员演示账号', 0, '2026-07-20 11:26:16', '2026-07-20 11:26:59');
INSERT INTO `sys_user` VALUES (3, 'college_demo', '$2a$10$Cn1aYNHMOWR3MaJNbDEkz.e2HORujw520sU2P6Mfz3cMGsAJiWL6K', '2026-07-20 15:12:43', '学院管理员演示账号', 0, '2026-07-20 11:26:16', '2026-07-20 11:27:01');
INSERT INTO `sys_user` VALUES (4, 'counselor_demo', '$2a$10$jVBy4qQ9PBDnaIY9l0mtr.FFNQFPZUuNgDaXv.xfuvR1wgBmTobMy', '2026-07-20 15:12:08', '辅导员演示账号', 0, '2026-07-20 11:26:16', '2026-07-20 11:27:03');
INSERT INTO `sys_user` VALUES (5, 'student_demo', '$2a$10$MRNdzRX5654.TZGEtFkLB.9lcToZjkr36TiIb/pa/dj5Sk4jQyaS2', '2026-07-20 15:11:29', '学生演示账号', 0, '2026-07-20 11:26:16', '2026-07-20 11:28:55');

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_role_id`(`role_id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_id`, `role_id`) USING BTREE
) ENGINE = oceanbase CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1, 4, '2026-07-20 00:30:36');
INSERT INTO `sys_user_role` VALUES (1000001, 2, 4, '2026-07-20 11:26:16');
INSERT INTO `sys_user_role` VALUES (1000002, 3, 3, '2026-07-20 11:26:16');
INSERT INTO `sys_user_role` VALUES (1000003, 4, 2, '2026-07-20 11:26:16');
INSERT INTO `sys_user_role` VALUES (1000004, 5, 1, '2026-07-20 11:26:16');

SET FOREIGN_KEY_CHECKS = 1;
