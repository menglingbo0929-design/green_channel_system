-- ============================================================
-- 组织结构初始数据 —— 西安交通大学
-- 数据来源：xjtu.edu.cn 官方公开信息，2026年
-- ============================================================

-- ========== 学院（college） ==========

INSERT INTO `college` (`college_code`, `college_name`) VALUES
('MECH',       '机械工程学院'),
('MSE',        '材料科学与工程学院'),
('EPE',        '能源与动力工程学院'),
('EE',         '电气工程学院'),
('AERO',       '航天航空学院'),
('MATH',       '数学与统计学院'),
('SOM',        '管理学院'),
('SFL',        '外国语学院'),
('CS',         '计算机科学与技术学院'),
('SE',         '软件学院'),
('ECE',        '电子科学与工程学院'),
('AI',         '人工智能学院'),
('CHEM',       '化学工程与技术学院'),
('ARCH',       '人居环境与建筑工程学院'),
('LAW',        '法学院'),
('ECON',       '经济与金融学院'),
('PHYS',       '物理学院'),
('LIFE',       '生命科学与技术学院'),
('PUBLIC',     '公共政策与管理学院'),
('JOURNAL',    '新闻与新媒体学院');

-- ========== 年级（grade） ==========

INSERT INTO `grade` (`grade_code`, `grade_name`) VALUES
('2022', '2022级'),
('2023', '2023级'),
('2024', '2024级'),
('2025', '2025级'),
('2026', '2026级');

-- ========== 专业（major） ==========

-- 机械工程学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('ME001', '机械工程',            (SELECT id FROM college WHERE college_code = 'MECH')),
('ME002', '智能制造工程',        (SELECT id FROM college WHERE college_code = 'MECH')),
('ME003', '车辆工程',            (SELECT id FROM college WHERE college_code = 'MECH'));

-- 材料科学与工程学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('MSE01', '材料科学与工程',      (SELECT id FROM college WHERE college_code = 'MSE'));

-- 能源与动力工程学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('EPE01', '能源与动力工程',      (SELECT id FROM college WHERE college_code = 'EPE')),
('EPE02', '新能源科学与工程',   (SELECT id FROM college WHERE college_code = 'EPE'));

-- 电气工程学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('EE001', '电气工程及其自动化',  (SELECT id FROM college WHERE college_code = 'EE'));

-- 航天航空学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('AE001', '工程力学',            (SELECT id FROM college WHERE college_code = 'AERO')),
('AE002', '飞行器设计与工程',    (SELECT id FROM college WHERE college_code = 'AERO'));

-- 数学与统计学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('MA001', '数学与应用数学',      (SELECT id FROM college WHERE college_code = 'MATH')),
('MA002', '统计学',              (SELECT id FROM college WHERE college_code = 'MATH'));

-- 管理学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('MG001', '工商管理',            (SELECT id FROM college WHERE college_code = 'SOM')),
('MG002', '大数据管理与应用',    (SELECT id FROM college WHERE college_code = 'SOM'));

-- 外国语学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('FL001', '英语',                (SELECT id FROM college WHERE college_code = 'SFL')),
('FL002', '日语',                (SELECT id FROM college WHERE college_code = 'SFL'));

-- 计算机科学与技术学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('CS001', '计算机科学与技术',    (SELECT id FROM college WHERE college_code = 'CS')),
('CS002', '物联网工程',          (SELECT id FROM college WHERE college_code = 'CS'));

-- 软件学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('SE001', '软件工程',            (SELECT id FROM college WHERE college_code = 'SE'));

-- 电子科学与工程学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('ECE01', '电子科学与技术',      (SELECT id FROM college WHERE college_code = 'ECE')),
('ECE02', '微电子科学与工程',    (SELECT id FROM college WHERE college_code = 'ECE'));

-- 人工智能学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('AI001', '人工智能',            (SELECT id FROM college WHERE college_code = 'AI'));

-- 化学工程与技术学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('CE001', '化学工程与工艺',      (SELECT id FROM college WHERE college_code = 'CHEM'));

-- 人居环境与建筑工程学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('CEV01', '土木工程',            (SELECT id FROM college WHERE college_code = 'ARCH')),
('CEV02', '建筑学',              (SELECT id FROM college WHERE college_code = 'ARCH'));

-- 法学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('LW001', '法学',                (SELECT id FROM college WHERE college_code = 'LAW'));

-- 经济与金融学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('EC001', '经济学',              (SELECT id FROM college WHERE college_code = 'ECON')),
('EC002', '金融学',              (SELECT id FROM college WHERE college_code = 'ECON'));

-- 物理学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('PH001', '应用物理学',          (SELECT id FROM college WHERE college_code = 'PHYS'));

-- 生命科学与技术学院
INSERT INTO `major` (`major_code`, `major_name`, `college_id`) VALUES
('LS001', '生物医学工程',        (SELECT id FROM college WHERE college_code = 'LIFE'));

-- ========== 班级（class_info）—— 选几个学院作为示例 ==========

-- 计算机科学与技术学院：计算机科学与技术专业 — 2024/2025/2026级
INSERT INTO `class_info` (`class_code`, `class_name`, `major_id`, `grade_id`, `college_id`) VALUES
('CS2401', '计科2401', (SELECT id FROM major WHERE major_code = 'CS001'), (SELECT id FROM grade WHERE grade_code = '2024'), (SELECT id FROM college WHERE college_code = 'CS')),
('CS2402', '计科2402', (SELECT id FROM major WHERE major_code = 'CS001'), (SELECT id FROM grade WHERE grade_code = '2024'), (SELECT id FROM college WHERE college_code = 'CS')),
('CS2501', '计科2501', (SELECT id FROM major WHERE major_code = 'CS001'), (SELECT id FROM grade WHERE grade_code = '2025'), (SELECT id FROM college WHERE college_code = 'CS')),
('CS2601', '计科2601', (SELECT id FROM major WHERE major_code = 'CS001'), (SELECT id FROM grade WHERE grade_code = '2026'), (SELECT id FROM college WHERE college_code = 'CS'));

-- 软件学院：软件工程专业
INSERT INTO `class_info` (`class_code`, `class_name`, `major_id`, `grade_id`, `college_id`) VALUES
('SE2401', '软工2401', (SELECT id FROM major WHERE major_code = 'SE001'), (SELECT id FROM grade WHERE grade_code = '2024'), (SELECT id FROM college WHERE college_code = 'SE')),
('SE2501', '软工2501', (SELECT id FROM major WHERE major_code = 'SE001'), (SELECT id FROM grade WHERE grade_code = '2025'), (SELECT id FROM college WHERE college_code = 'SE'));

-- 电气工程学院：电气工程及其自动化
INSERT INTO `class_info` (`class_code`, `class_name`, `major_id`, `grade_id`, `college_id`) VALUES
('EE2401', '电气2401', (SELECT id FROM major WHERE major_code = 'EE001'), (SELECT id FROM grade WHERE grade_code = '2024'), (SELECT id FROM college WHERE college_code = 'EE')),
('EE2501', '电气2501', (SELECT id FROM major WHERE major_code = 'EE001'), (SELECT id FROM grade WHERE grade_code = '2025'), (SELECT id FROM college WHERE college_code = 'EE'));

-- 经济与金融学院：金融学
INSERT INTO `class_info` (`class_code`, `class_name`, `major_id`, `grade_id`, `college_id`) VALUES
('FN2401', '金融2401', (SELECT id FROM major WHERE major_code = 'EC002'), (SELECT id FROM grade WHERE grade_code = '2024'), (SELECT id FROM college WHERE college_code = 'ECON'));
