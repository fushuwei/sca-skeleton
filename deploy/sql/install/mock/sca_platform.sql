-- ================================================================
-- SCA Platform - Mock 初始化数据
-- 包含：租户、部门、岗位、用户、角色等模拟数据（单租户模式，默认租户：东湖大学）
-- ================================================================

-- 设置字符集
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- 关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;


-- ================================================================
-- 一、租户数据（默认租户：东湖大学）
-- ================================================================

-- 默认租户：东湖大学
INSERT INTO `sys_tenant` (
    `id`, `name`, `code`, `package_id`, `contact_name`, `contact_phone`, `contact_email`,
    `domain_name`, `effective_time`, `expire_time`, `status`, `config_json`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '100', '东湖大学', 'donghu_university', '1',
    '张明远', '13800001001', 'admin@donghu.edu.cn',
    'donghu.edu.cn', NULL, NULL, 'enabled', NULL,
    '综合性省属重点大学，以工学、理学为主干学科', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_tenant` WHERE `code` = 'donghu_university' AND `is_deleted` = 0
);


-- ================================================================
-- 二、部门数据（10 个一级部门及其下属部门，共 30 个部门）
-- ================================================================

-- ---------------------------------------------------
-- 租户1：东湖大学 - 部门
-- ---------------------------------------------------

-- 一级部门：信息中心
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1001', '100', '0', '信息中心', 'xxzx', 10, '王志刚', '13900001001', 'wzg@donghu.edu.cn',
    'enabled', '0,1001', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1001' AND `is_deleted` = 0
);

-- 子部门：网络运维部
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1002', '100', '1001', '网络运维部', 'wlwyb', 10, '刘建华', '13900001002', 'ljh@donghu.edu.cn',
    'enabled', '0,1001,1002', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1002' AND `is_deleted` = 0
);

-- 子部门：数据管理部
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1003', '100', '1001', '数据管理部', 'sjglb', 20, '陈思远', '13900001003', 'csy@donghu.edu.cn',
    'enabled', '0,1001,1003', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1003' AND `is_deleted` = 0
);

-- 一级部门：人事处
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1004', '100', '0', '人事处', 'rsc', 20, '赵文轩', '13900001004', 'zwx@donghu.edu.cn',
    'enabled', '0,1004', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1004' AND `is_deleted` = 0
);

-- 子部门：人事管理科
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1005', '100', '1004', '人事管理科', 'rsglk', 10, '孙丽华', '13900001005', 'slh@donghu.edu.cn',
    'enabled', '0,1004,1005', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1005' AND `is_deleted` = 0
);

-- 子部门：师资培训科
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1006', '100', '1004', '师资培训科', 'szpxk', 20, '周建国', '13900001006', 'zjg@donghu.edu.cn',
    'enabled', '0,1004,1006', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1006' AND `is_deleted` = 0
);

-- 一级部门：教务处
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1007', '100', '0', '教务处', 'jwc', 30, '吴启明', '13900001007', 'wqm@donghu.edu.cn',
    'enabled', '0,1007', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1007' AND `is_deleted` = 0
);

-- 子部门：教学运行科
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1008', '100', '1007', '教学运行科', 'jxyxk', 10, '郑慧芳', '13900001008', 'zhf@donghu.edu.cn',
    'enabled', '0,1007,1008', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1008' AND `is_deleted` = 0
);

-- 子部门：课程建设科
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1009', '100', '1007', '课程建设科', 'kcjsk', 20, '黄志强', '13900001009', 'hzq@donghu.edu.cn',
    'enabled', '0,1007,1009', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1009' AND `is_deleted` = 0
);

-- 一级部门：科研处
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1010', '100', '0', '科研处', 'kyc', 40, '钱学文', '13900001010', 'qxw@donghu.edu.cn',
    'enabled', '0,1010', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1010' AND `is_deleted` = 0
);

-- 子部门：科研管理科
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1011', '100', '1010', '科研管理科', 'kyglk', 10, '冯德胜', '13900001011', 'fds@donghu.edu.cn',
    'enabled', '0,1010,1011', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1011' AND `is_deleted` = 0
);

-- 子部门：成果转化科
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1012', '100', '1010', '成果转化科', 'cgzhk', 20, '韩秀英', '13900001012', 'hxy@donghu.edu.cn',
    'enabled', '0,1010,1012', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1012' AND `is_deleted` = 0
);

-- 一级部门：财务处
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1013', '100', '0', '财务处', 'cwc', 50, '蒋国强', '13900001013', 'jgq@donghu.edu.cn',
    'enabled', '0,1013', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1013' AND `is_deleted` = 0
);

-- 子部门：财务核算科
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1014', '100', '1013', '财务核算科', 'cwksk', 10, '沈雅琴', '13900001014', 'syq@donghu.edu.cn',
    'enabled', '0,1013,1014', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1014' AND `is_deleted` = 0
);

-- 子部门：预算管理科
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1015', '100', '1013', '预算管理科', 'ysglk', 20, '朱伟明', '13900001015', 'zwm@donghu.edu.cn',
    'enabled', '0,1013,1015', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1015' AND `is_deleted` = 0
);

-- 一级部门：学生处
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1016', '100', '0', '学生处', 'xsc', 60, '秦凤珍', '13900001016', 'qfz@donghu.edu.cn',
    'enabled', '0,1016', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1016' AND `is_deleted` = 0
);

-- 子部门：学生事务中心
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1017', '100', '1016', '学生事务中心', 'xsswzx', 10, '许建平', '13900001017', 'xjp@donghu.edu.cn',
    'enabled', '0,1016,1017', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1017' AND `is_deleted` = 0
);

-- 子部门：心理咨询中心
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1018', '100', '1016', '心理咨询中心', 'xlzxzx', 20, '曹瑞芳', '13900001018', 'crf@donghu.edu.cn',
    'enabled', '0,1016,1018', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1018' AND `is_deleted` = 0
);

-- 一级部门：图书馆
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1019', '100', '0', '图书馆', 'tsg', 70, '邓淑华', '13900001019', 'dsh@donghu.edu.cn',
    'enabled', '0,1019', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1019' AND `is_deleted` = 0
);

-- 子部门：数字资源部
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1020', '100', '1019', '数字资源部', 'szyzb', 10, '高明辉', '13900001020', 'gmh@donghu.edu.cn',
    'enabled', '0,1019,1020', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1020' AND `is_deleted` = 0
);

-- 子部门：参考咨询部
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1021', '100', '1019', '参考咨询部', 'ckzxb', 20, '方秀兰', '13900001021', 'fxl@donghu.edu.cn',
    'enabled', '0,1019,1021', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1021' AND `is_deleted` = 0
);

-- 一级部门：外事处
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1022', '100', '0', '外事处', 'wsc', 80, '程伟业', '13900001022', 'cwy@donghu.edu.cn',
    'enabled', '0,1022', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1022' AND `is_deleted` = 0
);

-- 子部门：国际交流科
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1023', '100', '1022', '国际交流科', 'gjjlk', 10, '彭慧敏', '13900001023', 'phm@donghu.edu.cn',
    'enabled', '0,1022,1023', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1023' AND `is_deleted` = 0
);

-- 子部门：留学生管理科
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1024', '100', '1022', '留学生管理科', 'lxsglk', 20, '余志远', '13900001024', 'yzy@donghu.edu.cn',
    'enabled', '0,1022,1024', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1024' AND `is_deleted` = 0
);

-- 一级部门：审计处
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1025', '100', '0', '审计处', 'sjc', 90, '田永刚', '13900001025', 'tyg@donghu.edu.cn',
    'enabled', '0,1025', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1025' AND `is_deleted` = 0
);

-- 子部门：审计一室
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1026', '100', '1025', '审计一室', 'sjys', 10, '贺建民', '13900001026', 'hjm@donghu.edu.cn',
    'enabled', '0,1025,1026', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1026' AND `is_deleted` = 0
);

-- 子部门：审计二室
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1027', '100', '1025', '审计二室', 'sjes', 20, '罗淑珍', '13900001027', 'lsz@donghu.edu.cn',
    'enabled', '0,1025,1027', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1027' AND `is_deleted` = 0
);

-- 一级部门：后勤处
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1028', '100', '0', '后勤处', 'hqc', 100, '梁宏达', '13900001028', 'lhd@donghu.edu.cn',
    'enabled', '0,1028', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1028' AND `is_deleted` = 0
);

-- 子部门：基建维修中心
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1029', '100', '1028', '基建维修中心', 'jswxzx', 10, '谢永强', '13900001029', 'xyq@donghu.edu.cn',
    'enabled', '0,1028,1029', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1029' AND `is_deleted` = 0
);

-- 子部门：校园管理中心
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`,
    `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1030', '100', '1028', '校园管理中心', 'xyglzx', 20, '钟玉兰', '13900001030', 'zyl@donghu.edu.cn',
    'enabled', '0,1028,1030', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` WHERE `id` = '1030' AND `is_deleted` = 0
);


-- ================================================================
-- 三、岗位数据（10 个岗位，高校行业相关）
-- ================================================================

-- 岗位1：系统管理员
INSERT INTO `sys_post` (
    `id`, `tenant_id`, `name`, `code`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '100', NULL, '系统管理员', 'sys_admin', 1,
    '负责系统整体运维管理，包括用户权限配置、系统参数设置、数据备份恢复等核心管理工作', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_post` WHERE `code` = 'sys_admin' AND `is_deleted` = 0
);

-- 岗位2：网络运维工程师
INSERT INTO `sys_post` (
    `id`, `tenant_id`, `name`, `code`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '101', NULL, '网络运维工程师', 'net_ops_engineer', 2,
    '负责校园网络基础设施的建设、维护和优化，保障网络稳定运行', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_post` WHERE `code` = 'net_ops_engineer' AND `is_deleted` = 0
);

-- 岗位3：数据库管理员
INSERT INTO `sys_post` (
    `id`, `tenant_id`, `name`, `code`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '102', NULL, '数据库管理员', 'dba', 3,
    '负责各类业务数据库的设计、部署、性能调优和日常运维管理', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_post` WHERE `code` = 'dba' AND `is_deleted` = 0
);

-- 岗位4：信息安全专员
INSERT INTO `sys_post` (
    `id`, `tenant_id`, `name`, `code`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '103', NULL, '信息安全专员', 'info_security_officer', 4,
    '负责信息安全策略制定、安全漏洞检测、安全事件响应和安全培训工作', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_post` WHERE `code` = 'info_security_officer' AND `is_deleted` = 0
);

-- 岗位5：教务管理员
INSERT INTO `sys_post` (
    `id`, `tenant_id`, `name`, `code`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '104', NULL, '教务管理员', 'academic_affairs_admin', 5,
    '负责教学计划管理、排课选课、考试安排、学籍管理等教务工作', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_post` WHERE `code` = 'academic_affairs_admin' AND `is_deleted` = 0
);

-- 岗位6：科研秘书
INSERT INTO `sys_post` (
    `id`, `tenant_id`, `name`, `code`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '105', NULL, '科研秘书', 'research_secretary', 6,
    '负责科研项目申报管理、科研成果统计、学术活动组织等科研管理工作', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_post` WHERE `code` = 'research_secretary' AND `is_deleted` = 0
);

-- 岗位7：学生事务专员
INSERT INTO `sys_post` (
    `id`, `tenant_id`, `name`, `code`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '106', NULL, '学生事务专员', 'student_affairs_officer', 7,
    '负责学生日常管理、奖助学金评定、心理健康教育、就业指导等工作', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_post` WHERE `code` = 'student_affairs_officer' AND `is_deleted` = 0
);

-- 岗位8：人事管理专员
INSERT INTO `sys_post` (
    `id`, `tenant_id`, `name`, `code`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '107', NULL, '人事管理专员', 'hr_officer', 8,
    '负责教职工招聘、入职离职办理、薪酬核算、绩效考核等人事管理工作', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_post` WHERE `code` = 'hr_officer' AND `is_deleted` = 0
);

-- 岗位9：财务核算会计
INSERT INTO `sys_post` (
    `id`, `tenant_id`, `name`, `code`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '108', NULL, '财务核算会计', 'financial_accountant', 9,
    '负责财务凭证编制、账务处理、财务报表编制、预算执行监控等工作', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_post` WHERE `code` = 'financial_accountant' AND `is_deleted` = 0
);

-- 岗位10：资产管理员
INSERT INTO `sys_post` (
    `id`, `tenant_id`, `name`, `code`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '109', NULL, '资产管理员', 'asset_manager', 10,
    '负责固定资产登记、盘点、调拨、报废等资产全生命周期管理工作', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_post` WHERE `code` = 'asset_manager' AND `is_deleted` = 0
);


-- ================================================================
-- 四、用户数据（5 个后台用户 + 3 个前台用户，共 8 个用户）
-- 密码统一使用 BCrypt 加密的 admin@123
-- ================================================================

-- 后台用户1：李白（信息中心 - 东湖大学）
INSERT INTO `sys_user` (
    `id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `gender`, `avatar`, `phone`, `email`,
    `user_type`, `is_superadmin`, `status`, `status_time`, `status_reason`, `login_fail_count`, `must_change_password`,
    `password_update_time`, `effective_start_time`, `effective_end_time`, `last_login_ip`, `last_login_time`,
    `is_builtin`, `source_type`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1001', '100', 'libai',
    '{bcrypt}$2b$10$oW8PgdSN8jCUwZoApsRpc.8xhcCNInM8i0iH/6k.G7cmKB/5tb.Pq',
    '李白', '李白', 'male', NULL, '13800011001', 'libai@donghu.edu.cn',
    'backend', 0, 'active', NULL, NULL, 0, 0,
    NOW(), NOW(), NULL, NULL, NULL,
    0, 'manual', NULL, 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_user` WHERE `username` = 'libai' AND `is_deleted` = 0
);

-- 后台用户2：杜甫（人事处 - 东湖大学）
INSERT INTO `sys_user` (
    `id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `gender`, `avatar`, `phone`, `email`,
    `user_type`, `is_superadmin`, `status`, `status_time`, `status_reason`, `login_fail_count`, `must_change_password`,
    `password_update_time`, `effective_start_time`, `effective_end_time`, `last_login_ip`, `last_login_time`,
    `is_builtin`, `source_type`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1002', '100', 'dufu',
    '{bcrypt}$2b$10$oW8PgdSN8jCUwZoApsRpc.8xhcCNInM8i0iH/6k.G7cmKB/5tb.Pq',
    '杜甫', '杜甫', 'male', NULL, '13800011002', 'dufu@donghu.edu.cn',
    'backend', 0, 'active', NULL, NULL, 0, 0,
    NOW(), NOW(), NULL, NULL, NULL,
    0, 'manual', NULL, 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_user` WHERE `username` = 'dufu' AND `is_deleted` = 0
);

-- 后台用户3：王维（教务处 - 东湖大学）
INSERT INTO `sys_user` (
    `id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `gender`, `avatar`, `phone`, `email`,
    `user_type`, `is_superadmin`, `status`, `status_time`, `status_reason`, `login_fail_count`, `must_change_password`,
    `password_update_time`, `effective_start_time`, `effective_end_time`, `last_login_ip`, `last_login_time`,
    `is_builtin`, `source_type`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1003', '100', 'wangwei',
    '{bcrypt}$2b$10$oW8PgdSN8jCUwZoApsRpc.8xhcCNInM8i0iH/6k.G7cmKB/5tb.Pq',
    '王维', '王维', 'male', NULL, '13800011003', 'wangwei@donghu.edu.cn',
    'backend', 0, 'active', NULL, NULL, 0, 0,
    NOW(), NOW(), NULL, NULL, NULL,
    0, 'manual', NULL, 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_user` WHERE `username` = 'wangwei' AND `is_deleted` = 0
);

-- 后台用户4：苏轼（科研处 - 东湖大学）
INSERT INTO `sys_user` (
    `id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `gender`, `avatar`, `phone`, `email`,
    `user_type`, `is_superadmin`, `status`, `status_time`, `status_reason`, `login_fail_count`, `must_change_password`,
    `password_update_time`, `effective_start_time`, `effective_end_time`, `last_login_ip`, `last_login_time`,
    `is_builtin`, `source_type`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1004', '100', 'sushi',
    '{bcrypt}$2b$10$oW8PgdSN8jCUwZoApsRpc.8xhcCNInM8i0iH/6k.G7cmKB/5tb.Pq',
    '苏轼', '苏轼', 'male', NULL, '13800011004', 'sushi@donghu.edu.cn',
    'backend', 0, 'active', NULL, NULL, 0, 0,
    NOW(), NOW(), NULL, NULL, NULL,
    0, 'manual', NULL, 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_user` WHERE `username` = 'sushi' AND `is_deleted` = 0
);

-- 后台用户5：欧阳修（财务处 - 东湖大学）
INSERT INTO `sys_user` (
    `id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `gender`, `avatar`, `phone`, `email`,
    `user_type`, `is_superadmin`, `status`, `status_time`, `status_reason`, `login_fail_count`, `must_change_password`,
    `password_update_time`, `effective_start_time`, `effective_end_time`, `last_login_ip`, `last_login_time`,
    `is_builtin`, `source_type`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1005', '100', 'ouyangxiu',
    '{bcrypt}$2b$10$oW8PgdSN8jCUwZoApsRpc.8xhcCNInM8i0iH/6k.G7cmKB/5tb.Pq',
    '欧阳修', '欧阳修', 'male', NULL, '13800011005', 'ouyangxiu@donghu.edu.cn',
    'backend', 0, 'active', NULL, NULL, 0, 0,
    NOW(), NOW(), NULL, NULL, NULL,
    0, 'manual', NULL, 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_user` WHERE `username` = 'ouyangxiu' AND `is_deleted` = 0
);

-- 前台用户1：黄庭坚（学生 - 东湖大学）
INSERT INTO `sys_user` (
    `id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `gender`, `avatar`, `phone`, `email`,
    `user_type`, `is_superadmin`, `status`, `status_time`, `status_reason`, `login_fail_count`, `must_change_password`,
    `password_update_time`, `effective_start_time`, `effective_end_time`, `last_login_ip`, `last_login_time`,
    `is_builtin`, `source_type`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '2001', '100', 'huangtingjian',
    '{bcrypt}$2b$10$oW8PgdSN8jCUwZoApsRpc.8xhcCNInM8i0iH/6k.G7cmKB/5tb.Pq',
    '黄庭坚', '黄庭坚', 'male', NULL, '13800012001', 'huangtingjian@stu.donghu.edu.cn',
    'frontend', 0, 'active', NULL, NULL, 0, 0,
    NOW(), NOW(), NULL, NULL, NULL,
    0, 'manual', NULL, 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_user` WHERE `username` = 'huangtingjian' AND `is_deleted` = 0
);

-- 前台用户2：晏殊（学生 - 东湖大学）
INSERT INTO `sys_user` (
    `id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `gender`, `avatar`, `phone`, `email`,
    `user_type`, `is_superadmin`, `status`, `status_time`, `status_reason`, `login_fail_count`, `must_change_password`,
    `password_update_time`, `effective_start_time`, `effective_end_time`, `last_login_ip`, `last_login_time`,
    `is_builtin`, `source_type`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '2002', '100', 'yanshu',
    '{bcrypt}$2b$10$oW8PgdSN8jCUwZoApsRpc.8xhcCNInM8i0iH/6k.G7cmKB/5tb.Pq',
    '晏殊', '晏殊', 'male', NULL, '13800012002', 'yanshu@stu.donghu.edu.cn',
    'frontend', 0, 'active', NULL, NULL, 0, 0,
    NOW(), NOW(), NULL, NULL, NULL,
    0, 'manual', NULL, 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_user` WHERE `username` = 'yanshu' AND `is_deleted` = 0
);

-- 前台用户3：周邦彦（教师 - 东湖大学）
INSERT INTO `sys_user` (
    `id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `gender`, `avatar`, `phone`, `email`,
    `user_type`, `is_superadmin`, `status`, `status_time`, `status_reason`, `login_fail_count`, `must_change_password`,
    `password_update_time`, `effective_start_time`, `effective_end_time`, `last_login_ip`, `last_login_time`,
    `is_builtin`, `source_type`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '2003', '100', 'zhoubangyan',
    '{bcrypt}$2b$10$oW8PgdSN8jCUwZoApsRpc.8xhcCNInM8i0iH/6k.G7cmKB/5tb.Pq',
    '周邦彦', '周邦彦', 'male', NULL, '13800012003', 'zhoubangyan@stu.donghu.edu.cn',
    'frontend', 0, 'active', NULL, NULL, 0, 0,
    NOW(), NOW(), NULL, NULL, NULL,
    0, 'manual', NULL, 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_user` WHERE `username` = 'zhoubangyan' AND `is_deleted` = 0
);


-- ================================================================
-- 五、用户与部门关联数据
-- ================================================================

-- 东湖大学用户部门关联
INSERT INTO `sys_user_dept` (`id`, `tenant_id`, `user_id`, `dept_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'd001', '100', '1001', '1001', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_dept` WHERE `id` = 'd001');
INSERT INTO `sys_user_dept` (`id`, `tenant_id`, `user_id`, `dept_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'd002', '100', '1002', '1004', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_dept` WHERE `id` = 'd002');
INSERT INTO `sys_user_dept` (`id`, `tenant_id`, `user_id`, `dept_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'd003', '100', '1003', '1007', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_dept` WHERE `id` = 'd003');
INSERT INTO `sys_user_dept` (`id`, `tenant_id`, `user_id`, `dept_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'd004', '100', '1004', '1010', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_dept` WHERE `id` = 'd004');
INSERT INTO `sys_user_dept` (`id`, `tenant_id`, `user_id`, `dept_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'd005', '100', '1005', '1013', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_dept` WHERE `id` = 'd005');

-- 前台用户部门关联
INSERT INTO `sys_user_dept` (`id`, `tenant_id`, `user_id`, `dept_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'd011', '100', '2001', '1016', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_dept` WHERE `id` = 'd011');
INSERT INTO `sys_user_dept` (`id`, `tenant_id`, `user_id`, `dept_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'd012', '100', '2002', '1016', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_dept` WHERE `id` = 'd012');
INSERT INTO `sys_user_dept` (`id`, `tenant_id`, `user_id`, `dept_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'd013', '100', '2003', '1007', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_dept` WHERE `id` = 'd013');


-- ================================================================
-- 六、用户与岗位关联数据
-- ================================================================

-- 东湖大学后台用户岗位
INSERT INTO `sys_user_post` (`id`, `tenant_id`, `user_id`, `post_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'p001', '100', '1001', '100', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_post` WHERE `id` = 'p001');
INSERT INTO `sys_user_post` (`id`, `tenant_id`, `user_id`, `post_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'p002', '100', '1002', '107', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_post` WHERE `id` = 'p002');
INSERT INTO `sys_user_post` (`id`, `tenant_id`, `user_id`, `post_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'p003', '100', '1003', '104', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_post` WHERE `id` = 'p003');
INSERT INTO `sys_user_post` (`id`, `tenant_id`, `user_id`, `post_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'p004', '100', '1004', '105', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_post` WHERE `id` = 'p004');
INSERT INTO `sys_user_post` (`id`, `tenant_id`, `user_id`, `post_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'p005', '100', '1005', '108', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_post` WHERE `id` = 'p005');


-- ================================================================
-- 七、角色数据（5 个角色）
-- ================================================================

-- ---------------------------------------------------
-- 租户1：东湖大学 - 角色
-- ---------------------------------------------------

-- 角色1：租户管理员（东湖大学）
INSERT INTO `sys_role` (
    `id`, `tenant_id`, `name`, `code`, `data_scope`, `is_builtin`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1001', '100', '租户管理员', 'tenant_admin', 'tenant', 0, 1,
    '租户级系统管理员，负责本租户的用户、部门、角色、岗位等基础数据的管理维护', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role` WHERE `id` = '1001' AND `is_deleted` = 0
);

-- 角色2：信息中心主任（东湖大学）
INSERT INTO `sys_role` (
    `id`, `tenant_id`, `name`, `code`, `data_scope`, `is_builtin`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1002', '100', '信息中心主任', 'info_center_director', 'dept_and_sub', 0, 2,
    '信息中心部门负责人，管理信息中心及下属部门的人员和业务数据', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role` WHERE `id` = '1002' AND `is_deleted` = 0
);

-- 角色3：教务管理员（东湖大学）
INSERT INTO `sys_role` (
    `id`, `tenant_id`, `name`, `code`, `data_scope`, `is_builtin`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1003', '100', '教务管理员', 'academic_admin', 'dept_and_sub', 0, 3,
    '教务处工作人员，管理教学计划、排课选课、考试安排、学籍异动等教务业务', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role` WHERE `id` = '1003' AND `is_deleted` = 0
);

-- 角色4：人事专员（东湖大学）
INSERT INTO `sys_role` (
    `id`, `tenant_id`, `name`, `code`, `data_scope`, `is_builtin`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1005', '100', '人事专员', 'hr_specialist', 'dept_and_sub', 0, 5,
    '人事处工作人员，负责教职工入离职办理、考勤管理、薪酬核算等人事工作', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role` WHERE `id` = '1005' AND `is_deleted` = 0
);

-- 角色5：普通用户（通用角色）
INSERT INTO `sys_role` (
    `id`, `tenant_id`, `name`, `code`, `data_scope`, `is_builtin`, `sort`, `remark`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1006', '100', '普通用户', 'normal_user', 'personal', 0, 99,
    '普通用户角色，仅可查看和操作本人相关数据', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role` WHERE `id` = '1006' AND `is_deleted` = 0
);


-- ================================================================
-- 八、用户与角色关联数据
-- ================================================================

-- 东湖大学用户角色关联
-- 李白（信息中心）→ 租户管理员 + 信息中心主任
INSERT INTO `sys_user_role` (`id`, `tenant_id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'r001', '100', '1001', '1001', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_role` WHERE `id` = 'r001');
INSERT INTO `sys_user_role` (`id`, `tenant_id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'r002', '100', '1001', '1002', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_role` WHERE `id` = 'r002');

-- 杜甫（人事处）→ 人事专员
INSERT INTO `sys_user_role` (`id`, `tenant_id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'r003', '100', '1002', '1005', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_role` WHERE `id` = 'r003');

-- 王维（教务处）→ 教务管理员
INSERT INTO `sys_user_role` (`id`, `tenant_id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'r004', '100', '1003', '1003', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_role` WHERE `id` = 'r004');

-- 苏轼（科研处）→ 普通用户
INSERT INTO `sys_user_role` (`id`, `tenant_id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'r005', '100', '1004', '1006', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_role` WHERE `id` = 'r005');

-- 欧阳修（财务处）→ 普通用户
INSERT INTO `sys_user_role` (`id`, `tenant_id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'r006', '100', '1005', '1006', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_role` WHERE `id` = 'r006');

-- 前台用户角色关联
-- 黄庭坚（学生 - 东湖大学）→ 普通用户
INSERT INTO `sys_user_role` (`id`, `tenant_id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'r013', '100', '2001', '1006', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_role` WHERE `id` = 'r013');

-- 晏殊（学生 - 东湖大学）→ 普通用户
INSERT INTO `sys_user_role` (`id`, `tenant_id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'r014', '100', '2002', '1006', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_role` WHERE `id` = 'r014');

-- 周邦彦（教师 - 东湖大学）→ 教务管理员
INSERT INTO `sys_user_role` (`id`, `tenant_id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'r015', '100', '2003', '1003', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_role` WHERE `id` = 'r015');


-- ================================================================
-- 九、角色权限关联数据（为各角色分配菜单权限）
-- ================================================================

-- 租户管理员 - 拥有系统管理全部权限
INSERT INTO `sys_role_permission` (`id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'rp001', '100', '1001', '9999', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_permission` WHERE `id` = 'rp001');
INSERT INTO `sys_role_permission` (`id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'rp002', '100', '1001', '9910', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_permission` WHERE `id` = 'rp002');
INSERT INTO `sys_role_permission` (`id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'rp003', '100', '1001', '9911', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_permission` WHERE `id` = 'rp003');
INSERT INTO `sys_role_permission` (`id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'rp004', '100', '1001', '9912', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_permission` WHERE `id` = 'rp004');
INSERT INTO `sys_role_permission` (`id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'rp005', '100', '1001', '9913', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_permission` WHERE `id` = 'rp005');
INSERT INTO `sys_role_permission` (`id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'rp006', '100', '1001', '9914', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_permission` WHERE `id` = 'rp006');
INSERT INTO `sys_role_permission` (`id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'rp007', '100', '1001', '9915', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_permission` WHERE `id` = 'rp007');
INSERT INTO `sys_role_permission` (`id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'rp008', '100', '1001', '9916', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_permission` WHERE `id` = 'rp008');
INSERT INTO `sys_role_permission` (`id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'rp009', '100', '1001', '9917', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_permission` WHERE `id` = 'rp009');
INSERT INTO `sys_role_permission` (`id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'rp010', '100', '1001', '9918', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_permission` WHERE `id` = 'rp010');
INSERT INTO `sys_role_permission` (`id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'rp011', '100', '1001', '1000', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_permission` WHERE `id` = 'rp011');

-- 东湖大学 租户管理员也分配数据源管理权限
INSERT INTO `sys_role_permission` (`id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'rp012', '100', '1001', '1100', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_permission` WHERE `id` = 'rp012');
INSERT INTO `sys_role_permission` (`id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'rp013', '100', '1001', '1200', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_permission` WHERE `id` = 'rp013');
INSERT INTO `sys_role_permission` (`id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
SELECT 'rp014', '100', '1001', '1300', 'system', NOW(), 'system', NOW(), 0 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_permission` WHERE `id` = 'rp014');


-- 打开外键检查
SET FOREIGN_KEY_CHECKS = 1;
