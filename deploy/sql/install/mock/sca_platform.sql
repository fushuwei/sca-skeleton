-- ================================================================
-- SCA Platform - 高校教育行业模拟数据脚本
-- 包含：部门、岗位、角色、用户等模拟数据（用于测试和演示）
-- 注意：此脚本仅用于模拟数据，正式环境请使用 sca_platform.sql
-- ================================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS sca_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 使用数据库
USE sca_platform;

-- 设置连接字符集
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- 关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;


-- ================================================================
-- 一、部门数据（高校教育行业，共60个部门）
-- ================================================================

-- 排序值规则：
--   一级部门：从 10 开始递增 1，两位数（10-99）
--   二级部门：前两位是一级部门排序值，后两位从 10 开始递增 1（10-99），格式 XXYY
--   三级部门：前两位是一级部门排序值，中间两位是二级部门排序值，最后一位从 1 开始递增，格式 XXYYZ
INSERT INTO `sys_dept` (
    `id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`,
    `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
) VALUES
-- 一级：行政管理部门 (sort = 10-29, parent_id = '0')
('101', '1', '0', '校办公室', 'admin_office', 10, '李明', '021-65551001', 'admin_office@hust.edu.cn', 'enabled', '0,101', 0, 'system', NOW(), 'system', NOW(), 0),
('102', '1', '0', '人事处', 'hr_dept', 11, '王芳', '021-65551002', 'hr@hust.edu.cn', 'enabled', '0,102', 0, 'system', NOW(), 'system', NOW(), 0),
-- 二级：人事处下属 (sort = 1110, 1111)
('1021', '1', '102', '人事科', 'hr_personnel', 1110, '张伟', '021-65551021', 'hr_personnel@hust.edu.cn', 'enabled', '0,102,1021', 0, 'system', NOW(), 'system', NOW(), 0),
('1022', '1', '102', '师资科', 'hr_faculty', 1111, '李娜', '021-65551022', 'hr_faculty@hust.edu.cn', 'enabled', '0,102,1022', 0, 'system', NOW(), 'system', NOW(), 0),
('103', '1', '0', '财务处', 'finance_dept', 12, '赵军', '021-65551003', 'finance@hust.edu.cn', 'enabled', '0,103', 0, 'system', NOW(), 'system', NOW(), 0),
-- 二级：财务处下属 (sort = 1210, 1211)
('1031', '1', '103', '会计科', 'finance_accounting', 1210, '刘芳', '021-65551031', 'finance_accounting@hust.edu.cn', 'enabled', '0,103,1031', 0, 'system', NOW(), 'system', NOW(), 0),
('1032', '1', '103', '预算科', 'finance_budget', 1211, '陈明', '021-65551032', 'finance_budget@hust.edu.cn', 'enabled', '0,103,1032', 0, 'system', NOW(), 'system', NOW(), 0),
('104', '1', '0', '教务处', 'academic_affairs', 13, '陈晓燕', '021-65551004', 'jwc@hust.edu.cn', 'enabled', '0,104', 0, 'system', NOW(), 'system', NOW(), 0),
-- 二级：教务处下属 (sort = 1310, 1311, 1312)
('1041', '1', '104', '教务科', 'academic_affairs_office', 1310, '王丽', '021-65551041', 'jwc_office@hust.edu.cn', 'enabled', '0,104,1041', 0, 'system', NOW(), 'system', NOW(), 0),
('1042', '1', '104', '学籍管理科', 'academic_registration', 1311, '张强', '021-65551042', 'xjgl@hust.edu.cn', 'enabled', '0,104,1042', 0, 'system', NOW(), 'system', NOW(), 0),
('1043', '1', '104', '教学质量科', 'academic_quality', 1312, '李红', '021-65551043', 'jxzl@hust.edu.cn', 'enabled', '0,104,1043', 0, 'system', NOW(), 'system', NOW(), 0),
('105', '1', '0', '科研处', 'research_dept', 14, '刘强', '021-65551005', 'research@hust.edu.cn', 'enabled', '0,105', 0, 'system', NOW(), 'system', NOW(), 0),
-- 二级：科研处下属 (sort = 1410, 1411)
('1051', '1', '105', '科研项目科', 'research_project', 1410, '王刚', '021-65551051', 'research_project@hust.edu.cn', 'enabled', '0,105,1051', 0, 'system', NOW(), 'system', NOW(), 0),
('1052', '1', '105', '成果转化科', 'research_transfer', 1411, '赵敏', '021-65551052', 'research_transfer@hust.edu.cn', 'enabled', '0,105,1052', 0, 'system', NOW(), 'system', NOW(), 0),
('106', '1', '0', '学生工作处', 'student_affairs', 15, '周敏', '021-65551006', 'xsc@hust.edu.cn', 'enabled', '0,106', 0, 'system', NOW(), 'system', NOW(), 0),
-- 二级：学生工作处下属 (sort = 1510, 1511)
('1061', '1', '106', '学生管理科', 'student_management', 1510, '陈静', '021-65551061', 'xsgl@hust.edu.cn', 'enabled', '0,106,1061', 0, 'system', NOW(), 'system', NOW(), 0),
('1062', '1', '106', '资助管理中心', 'student_financial_aid', 1511, '刘洋', '021-65551062', 'zzzx@hust.edu.cn', 'enabled', '0,106,1062', 0, 'system', NOW(), 'system', NOW(), 0),
('107', '1', '0', '研究生院', 'graduate_school', 16, '吴建国', '021-65551007', 'yjsy@hust.edu.cn', 'enabled', '0,107', 0, 'system', NOW(), 'system', NOW(), 0),
('108', '1', '0', '国际交流处', 'intl_office', 17, '孙丽华', '021-65551008', 'intl@hust.edu.cn', 'enabled', '0,108', 0, 'system', NOW(), 'system', NOW(), 0),
('109', '1', '0', '招生办公室', 'admission_office', 18, '黄志远', '021-65551009', 'zsb@hust.edu.cn', 'enabled', '0,109', 0, 'system', NOW(), 'system', NOW(), 0),
('110', '1', '0', '就业指导中心', 'career_center', 19, '郑秀英', '021-65551010', 'career@hust.edu.cn', 'enabled', '0,110', 0, 'system', NOW(), 'system', NOW(), 0),
('111', '1', '0', '审计处', 'audit_dept', 20, '何志强', '021-65551011', 'audit@hust.edu.cn', 'enabled', '0,111', 0, 'system', NOW(), 'system', NOW(), 0),
('112', '1', '0', '纪检监察处', 'discipline_dept', 21, '林正华', '021-65551012', 'jijian@hust.edu.cn', 'enabled', '0,112', 0, 'system', NOW(), 'system', NOW(), 0),
('113', '1', '0', '组织部', 'org_dept', 22, '马国强', '021-65551013', 'zzb@hust.edu.cn', 'enabled', '0,113', 0, 'system', NOW(), 'system', NOW(), 0),
('114', '1', '0', '宣传部', 'propaganda_dept', 23, '罗敏', '021-65551014', 'xcb@hust.edu.cn', 'enabled', '0,114', 0, 'system', NOW(), 'system', NOW(), 0),
('115', '1', '0', '统战部', 'united_front', 24, '谢文', '021-65551015', 'tzb@hust.edu.cn', 'enabled', '0,115', 0, 'system', NOW(), 'system', NOW(), 0),
('116', '1', '0', '工会', 'labor_union', 25, '杨红', '021-65551016', 'gh@hust.edu.cn', 'enabled', '0,116', 0, 'system', NOW(), 'system', NOW(), 0),
('117', '1', '0', '团委', 'youth_league', 26, '朱峰', '021-65551017', 'tw@hust.edu.cn', 'enabled', '0,117', 0, 'system', NOW(), 'system', NOW(), 0),
('118', '1', '0', '保卫处', 'security_dept', 27, '韩刚', '021-65551018', 'bwc@hust.edu.cn', 'enabled', '0,118', 0, 'system', NOW(), 'system', NOW(), 0),
('119', '1', '0', '后勤管理处', 'logistics_dept', 28, '曹建平', '021-65551019', 'hq@hust.edu.cn', 'enabled', '0,119', 0, 'system', NOW(), 'system', NOW(), 0),
('120', '1', '0', '国有资产管理处', 'asset_dept', 29, '许明', '021-65551020', 'zcc@hust.edu.cn', 'enabled', '0,120', 0, 'system', NOW(), 'system', NOW(), 0),

-- 一级：信息中心 (sort = 30)
('200', '1', '0', '信息中心', 'info_center', 30, '王建华', '021-65552000', 'info_center@hust.edu.cn', 'enabled', '0,200', 0, 'system', NOW(), 'system', NOW(), 0),
-- 二级：信息中心下属 (sort = 3010, 3011, 3012, 3013)
('201', '1', '200', '信息技术部', 'info_tech', 3010, '陈伟', '021-65552001', 'info_tech@hust.edu.cn', 'enabled', '0,200,201', 0, 'system', NOW(), 'system', NOW(), 0),
('202', '1', '200', '数据管理部', 'data_mgmt', 3011, '张蕾', '021-65552002', 'data_mgmt@hust.edu.cn', 'enabled', '0,200,202', 0, 'system', NOW(), 'system', NOW(), 0),
('203', '1', '200', '网络运维部', 'network_ops', 3012, '李刚', '021-65552003', 'network@hust.edu.cn', 'enabled', '0,200,203', 0, 'system', NOW(), 'system', NOW(), 0),
('204', '1', '200', '系统开发部', 'sys_dev', 3013, '刘洋', '021-65552004', 'sys_dev@hust.edu.cn', 'enabled', '0,200,204', 0, 'system', NOW(), 'system', NOW(), 0),

-- 一级：二级学院（12个）(sort = 31-42)
('300', '1', '0', '计算机学院', 'cs_college', 31, '周志明', '021-65553000', 'cs@hust.edu.cn', 'enabled', '0,300', 0, 'system', NOW(), 'system', NOW(), 0),
-- 二级：计算机学院下属 (sort = 3110, 3111, 3112)
('3001', '1', '300', '计算机科学系', 'cs_science', 3110, '王建华', '021-65553001', 'cs_science@hust.edu.cn', 'enabled', '0,300,3001', 0, 'system', NOW(), 'system', NOW(), 0),
('3002', '1', '300', '软件工程系', 'cs_software', 3111, '李明', '021-65553002', 'cs_software@hust.edu.cn', 'enabled', '0,300,3002', 0, 'system', NOW(), 'system', NOW(), 0),
('3003', '1', '300', '计算机实验中心', 'cs_lab', 3112, '张强', '021-65553003', 'cs_lab@hust.edu.cn', 'enabled', '0,300,3003', 0, 'system', NOW(), 'system', NOW(), 0),
('301', '1', '0', '电子工程学院', 'ee_college', 32, '吴建平', '021-65553010', 'ee@hust.edu.cn', 'enabled', '0,301', 0, 'system', NOW(), 'system', NOW(), 0),
('302', '1', '0', '机械工程学院', 'me_college', 33, '王德华', '021-65553020', 'me@hust.edu.cn', 'enabled', '0,302', 0, 'system', NOW(), 'system', NOW(), 0),
('303', '1', '0', '经济管理学院', 'em_college', 34, '孙丽', '021-65553030', 'em@hust.edu.cn', 'enabled', '0,303', 0, 'system', NOW(), 'system', NOW(), 0),
('304', '1', '0', '外国语学院', 'fl_college', 35, '张敏', '021-65553040', 'fl@hust.edu.cn', 'enabled', '0,304', 0, 'system', NOW(), 'system', NOW(), 0),
('305', '1', '0', '理学院', 'science_college', 36, '陈晓', '021-65553050', 'science@hust.edu.cn', 'enabled', '0,305', 0, 'system', NOW(), 'system', NOW(), 0),
('306', '1', '0', '文学院', 'lit_college', 37, '刘芳', '021-65553060', 'lit@hust.edu.cn', 'enabled', '0,306', 0, 'system', NOW(), 'system', NOW(), 0),
('307', '1', '0', '法学院', 'law_college', 38, '赵刚', '021-65553070', 'law@hust.edu.cn', 'enabled', '0,307', 0, 'system', NOW(), 'system', NOW(), 0),
('308', '1', '0', '艺术学院', 'art_college', 39, '黄华', '021-65553080', 'art@hust.edu.cn', 'enabled', '0,308', 0, 'system', NOW(), 'system', NOW(), 0),
('309', '1', '0', '体育部', 'pe_dept', 40, '韩强', '021-65553090', 'pe@hust.edu.cn', 'enabled', '0,309', 0, 'system', NOW(), 'system', NOW(), 0),
('310', '1', '0', '马克思主义学院', 'marx_college', 41, '林红', '021-65553100', 'marx@hust.edu.cn', 'enabled', '0,310', 0, 'system', NOW(), 'system', NOW(), 0),
('311', '1', '0', '医学院', 'medical_college', 42, '杨明', '021-65553110', 'medical@hust.edu.cn', 'enabled', '0,311', 0, 'system', NOW(), 'system', NOW(), 0),

-- 一级：教学辅助单位 (sort = 43-46)
('400', '1', '0', '图书馆', 'library', 43, '张秀兰', '021-65554000', 'library@hust.edu.cn', 'enabled', '0,400', 0, 'system', NOW(), 'system', NOW(), 0),
('401', '1', '0', '实验中心', 'lab_center', 44, '王强', '021-65554001', 'lab@hust.edu.cn', 'enabled', '0,401', 0, 'system', NOW(), 'system', NOW(), 0),
('402', '1', '0', '网络中心', 'net_center', 45, '李伟', '021-65554002', 'net@hust.edu.cn', 'enabled', '0,402', 0, 'system', NOW(), 'system', NOW(), 0),
('403', '1', '0', '教育技术中心', 'edtech_center', 46, '陈军', '021-65554003', 'edtech@hust.edu.cn', 'enabled', '0,403', 0, 'system', NOW(), 'system', NOW(), 0);


-- ================================================================
-- 二、菜单/权限数据（已迁移至sca_platform.sql正式脚本）
-- ================================================================
-- 注意：菜单/权限数据已迁移至 sca_platform.sql 正式脚本中统一管理
-- 包括：系统管理模块（含租户管理、系统与厂商等目录）和数据源管理模块


-- ================================================================
-- 三、岗位数据
-- ================================================================

INSERT INTO `sys_post` (
    `id`, `tenant_id`, `name`, `code`, `sort`, `remark`, `version`,
    `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
) VALUES
-- 后台岗位
('1001', '1', '平台超级管理员', 'POST_SUPERADMIN', 1, '平台最高权限管理员', 0, 'system', NOW(), 'system', NOW(), 0),
('1002', '1', '部门管理员', 'POST_DEPT_ADMIN', 2, '部门级别管理员，负责本部门管理', 0, 'system', NOW(), 'system', NOW(), 0),
('1003', '1', '系统管理员', 'POST_SYS_ADMIN', 3, '系统运维管理员', 0, 'system', NOW(), 'system', NOW(), 0),
('1004', '1', '教务管理员', 'POST_ACADEMIC_ADMIN', 4, '教务处管理人员', 0, 'system', NOW(), 'system', NOW(), 0),
('1005', '1', '科研管理员', 'POST_RESEARCH_ADMIN', 5, '科研处管理人员', 0, 'system', NOW(), 'system', NOW(), 0),
('1006', '1', '学生管理员', 'POST_STUDENT_ADMIN', 6, '学生处管理人员', 0, 'system', NOW(), 'system', NOW(), 0),
('1007', '1', '财务管理员', 'POST_FINANCE_ADMIN', 7, '财务处管理人员', 0, 'system', NOW(), 'system', NOW(), 0),
('1008', '1', '人事管理员', 'POST_HR_ADMIN', 8, '人事处管理人员', 0, 'system', NOW(), 'system', NOW(), 0),
('1009', '1', '信息中心主管', 'POST_IT_MANAGER', 9, '信息中心管理人员', 0, 'system', NOW(), 'system', NOW(), 0),
('1010', '1', '数据库管理员', 'POST_DBA', 10, '负责数据库运维管理', 0, 'system', NOW(), 'system', NOW(), 0),
('1011', '1', '系统运维工程师', 'POST_OPS_ENGINEER', 11, '负责系统运维和监控', 0, 'system', NOW(), 'system', NOW(), 0),
('1012', '1', '数据分析师', 'POST_DATA_ANALYST', 12, '负责数据分析和报表', 0, 'system', NOW(), 'system', NOW(), 0),
('1013', '1', '招生专员', 'POST_ADMISSION_OFFICER', 13, '招生办公室工作人员', 0, 'system', NOW(), 'system', NOW(), 0),
('1014', '1', '就业指导师', 'POST_CAREER_ADVISOR', 14, '就业指导中心工作人员', 0, 'system', NOW(), 'system', NOW(), 0),
('1015', '1', '图书管理员', 'POST_LIBRARIAN', 15, '图书馆工作人员', 0, 'system', NOW(), 'system', NOW(), 0),

-- 前台岗位
('2001', '1', '教师', 'POST_TEACHER', 20, '高校教师', 0, 'system', NOW(), 'system', NOW(), 0),
('2002', '1', '教授', 'POST_PROFESSOR', 21, '正高级职称教师', 0, 'system', NOW(), 'system', NOW(), 0),
('2003', '1', '副教授', 'POST_ASSOCIATE_PROF', 22, '副高级职称教师', 0, 'system', NOW(), 'system', NOW(), 0),
('2004', '1', '讲师', 'POST_LECTURER', 23, '中级职称教师', 0, 'system', NOW(), 'system', NOW(), 0),
('2005', '1', '助教', 'POST_ASSISTANT', 24, '初级职称教师', 0, 'system', NOW(), 'system', NOW(), 0),
('2006', '1', '本科生', 'POST_UNDERGRADUATE', 25, '本科在读学生', 0, 'system', NOW(), 'system', NOW(), 0),
('2007', '1', '研究生', 'POST_GRADUATE', 26, '硕士研究生在读', 0, 'system', NOW(), 'system', NOW(), 0),
('2008', '1', '博士生', 'POST_PHD_STUDENT', 27, '博士研究生在读', 0, 'system', NOW(), 'system', NOW(), 0),
('2009', '1', '访问学者', 'POST_VISITING_SCHOLAR', 28, '外聘访问学者', 0, 'system', NOW(), 'system', NOW(), 0),
('2010', '1', '校友', 'POST_ALUMNI', 29, '已毕业校友', 0, 'system', NOW(), 'system', NOW(), 0);


-- ================================================================
-- 四、角色数据（平台超级管理员通过 sys_user.is_superadmin 标记，不走角色体系）
-- ================================================================

INSERT INTO `sys_role` (
    `id`, `tenant_id`, `name`, `code`, `data_scope`, `is_builtin`, `sort`, `remark`, `version`,
    `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
) VALUES
-- 业务管理角色
('100', '1', '系统管理员', 'ROLE_SYSTEM_ADMIN', 'all', 0, 2, '系统运维管理员，负责系统配置和维护', 0, 'system', NOW(), 'system', NOW(), 0),
('200', '1', '教务管理员', 'ROLE_ACADEMIC_ADMIN', 'tenant', 0, 10, '教务处管理人员，管理课程、成绩、学籍等', 0, 'system', NOW(), 'system', NOW(), 0),
('201', '1', '科研管理员', 'ROLE_RESEARCH_ADMIN', 'tenant', 0, 11, '科研处管理人员，管理科研项目和成果', 0, 'system', NOW(), 'system', NOW(), 0),
('202', '1', '学生管理员', 'ROLE_STUDENT_ADMIN', 'tenant', 0, 12, '学生处管理人员，管理学生事务', 0, 'system', NOW(), 'system', NOW(), 0),
('203', '1', '财务管理员', 'ROLE_FINANCE_ADMIN', 'tenant', 0, 13, '财务处管理人员，管理财务收支', 0, 'system', NOW(), 'system', NOW(), 0),
('204', '1', '人事管理员', 'ROLE_HR_ADMIN', 'tenant', 0, 14, '人事处管理人员，管理教职工信息', 0, 'system', NOW(), 'system', NOW(), 0),
('205', '1', '招生管理员', 'ROLE_ADMISSION_ADMIN', 'tenant', 0, 15, '招生办公室人员，管理招生工作', 0, 'system', NOW(), 'system', NOW(), 0),
('206', '1', '就业管理员', 'ROLE_CAREER_ADMIN', 'tenant', 0, 16, '就业指导中心人员，管理就业工作', 0, 'system', NOW(), 'system', NOW(), 0),

-- 信息中心角色
('300', '1', '信息中心主任', 'ROLE_IT_DIRECTOR', 'tenant', 0, 20, '信息中心管理人员', 0, 'system', NOW(), 'system', NOW(), 0),
('301', '1', '数据库管理员', 'ROLE_DBA', 'tenant', 0, 21, '负责数据库运维和管理', 0, 'system', NOW(), 'system', NOW(), 0),
('302', '1', '系统运维工程师', 'ROLE_OPS_ENGINEER', 'tenant', 0, 22, '负责系统运维和监控', 0, 'system', NOW(), 'system', NOW(), 0),
('303', '1', '数据分析师', 'ROLE_DATA_ANALYST', 'tenant', 0, 23, '负责数据分析和报表', 0, 'system', NOW(), 'system', NOW(), 0),
('304', '1', '数据源管理员', 'ROLE_DATASOURCE_ADMIN', 'tenant', 0, 24, '负责数据源配置和管理', 0, 'system', NOW(), 'system', NOW(), 0),
('305', '1', '驱动管理员', 'ROLE_DRIVER_ADMIN', 'tenant', 0, 25, '负责数据库驱动管理', 0, 'system', NOW(), 'system', NOW(), 0),

-- 业务系统使用角色
('400', '1', '教师', 'ROLE_TEACHER', 'dept_and_sub', 0, 30, '教师用户，可查看和管理所授课程', 0, 'system', NOW(), 'system', NOW(), 0),
('401', '1', '学生', 'ROLE_STUDENT', 'personal', 0, 31, '学生用户，可查看个人学习信息', 0, 'system', NOW(), 'system', NOW(), 0),
('402', '1', '访问学者', 'ROLE_VISITOR', 'personal', 0, 32, '访问学者用户，有限的系统访问权限', 0, 'system', NOW(), 'system', NOW(), 0),
('403', '1', '校友', 'ROLE_ALUMNI', 'personal', 0, 33, '校友用户，可查看校友相关功能', 0, 'system', NOW(), 'system', NOW(), 0),
('404', '1', '部门管理员', 'ROLE_DEPT_ADMIN', 'dept', 0, 34, '部门级管理员，管理本部门事务', 0, 'system', NOW(), 'system', NOW(), 0),
('405', '1', '普通用户', 'ROLE_USER', 'personal', 0, 35, '普通用户，基本系统访问权限', 0, 'system', NOW(), 'system', NOW(), 0);


-- ================================================================
-- 五、角色权限关联数据
-- ================================================================

INSERT INTO `sys_role_permission` (
    `id`, `tenant_id`, `role_id`, `permission_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
) VALUES
-- 系统管理员拥有所有系统管理权限
('500001', '1', '100', '1000', 'system', NOW(), 'system', NOW(), 0),
('500002', '1', '100', '1100', 'system', NOW(), 'system', NOW(), 0),
('500003', '1', '100', '1101', 'system', NOW(), 'system', NOW(), 0),
('500004', '1', '100', '1102', 'system', NOW(), 'system', NOW(), 0),
('500005', '1', '100', '1200', 'system', NOW(), 'system', NOW(), 0),
('500006', '1', '100', '1201', 'system', NOW(), 'system', NOW(), 0),
('500007', '1', '100', '1202', 'system', NOW(), 'system', NOW(), 0),
('500008', '1', '100', '1203', 'system', NOW(), 'system', NOW(), 0),
('500009', '1', '100', '1300', 'system', NOW(), 'system', NOW(), 0),
('500010', '1', '100', '1301', 'system', NOW(), 'system', NOW(), 0),
('500011', '1', '100', '1302', 'system', NOW(), 'system', NOW(), 0),
('500012', '1', '100', '1400', 'system', NOW(), 'system', NOW(), 0),
('500013', '1', '100', '1401', 'system', NOW(), 'system', NOW(), 0),
('500014', '1', '100', '1402', 'system', NOW(), 'system', NOW(), 0),
('500015', '1', '100', '1500', 'system', NOW(), 'system', NOW(), 0),
('500016', '1', '100', '1501', 'system', NOW(), 'system', NOW(), 0),
('500017', '1', '100', '1502', 'system', NOW(), 'system', NOW(), 0),
('500018', '1', '100', '1503', 'system', NOW(), 'system', NOW(), 0),
('500019', '1', '100', '1504', 'system', NOW(), 'system', NOW(), 0),
('500020', '1', '100', '2000', 'system', NOW(), 'system', NOW(), 0),
('500021', '1', '100', '2100', 'system', NOW(), 'system', NOW(), 0),
('500022', '1', '100', '2200', 'system', NOW(), 'system', NOW(), 0),
('500023', '1', '100', '2300', 'system', NOW(), 'system', NOW(), 0),
('500024', '1', '100', '5000', 'system', NOW(), 'system', NOW(), 0),
('500025', '1', '100', '5100', 'system', NOW(), 'system', NOW(), 0),
('500026', '1', '100', '5101', 'system', NOW(), 'system', NOW(), 0),
('500027', '1', '100', '5102', 'system', NOW(), 'system', NOW(), 0),
('500028', '1', '100', '5200', 'system', NOW(), 'system', NOW(), 0),
('500029', '1', '100', '5201', 'system', NOW(), 'system', NOW(), 0),
('500030', '1', '100', '5202', 'system', NOW(), 'system', NOW(), 0),
('500031', '1', '100', '5300', 'system', NOW(), 'system', NOW(), 0),
('500032', '1', '100', '5301', 'system', NOW(), 'system', NOW(), 0),
('500033', '1', '100', '5302', 'system', NOW(), 'system', NOW(), 0),

-- 数据源管理员拥有数据源管理权限
('500034', '1', '304', '2000', 'system', NOW(), 'system', NOW(), 0),
('500035', '1', '304', '2100', 'system', NOW(), 'system', NOW(), 0),
('500036', '1', '304', '5000', 'system', NOW(), 'system', NOW(), 0),
('500037', '1', '304', '5100', 'system', NOW(), 'system', NOW(), 0),
('500038', '1', '304', '5101', 'system', NOW(), 'system', NOW(), 0),
('500039', '1', '304', '5102', 'system', NOW(), 'system', NOW(), 0),
('500040', '1', '304', '5200', 'system', NOW(), 'system', NOW(), 0),
('500041', '1', '304', '5201', 'system', NOW(), 'system', NOW(), 0),
('500042', '1', '304', '5202', 'system', NOW(), 'system', NOW(), 0),

-- 驱动管理员拥有驱动管理权限
('500043', '1', '305', '2000', 'system', NOW(), 'system', NOW(), 0),
('500044', '1', '305', '2300', 'system', NOW(), 'system', NOW(), 0),
('500045', '1', '305', '5000', 'system', NOW(), 'system', NOW(), 0),
('500046', '1', '305', '5300', 'system', NOW(), 'system', NOW(), 0),
('500047', '1', '305', '5301', 'system', NOW(), 'system', NOW(), 0),
('500048', '1', '305', '5302', 'system', NOW(), 'system', NOW(), 0),

-- 数据分析师拥有数据查询权限
('500049', '1', '303', '2000', 'system', NOW(), 'system', NOW(), 0),
('500050', '1', '303', '2200', 'system', NOW(), 'system', NOW(), 0),
('500051', '1', '303', '5000', 'system', NOW(), 'system', NOW(), 0),
('500052', '1', '303', '5200', 'system', NOW(), 'system', NOW(), 0),
('500053', '1', '303', '5201', 'system', NOW(), 'system', NOW(), 0),
('500054', '1', '303', '5202', 'system', NOW(), 'system', NOW(), 0);


-- ================================================================
-- 六、后台用户数据（20名真实用户）
-- 密码统一使用：{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC (admin@123)
-- ================================================================

INSERT INTO `sys_user` (
    `id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `gender`, `avatar`, `phone`, `email`,
    `user_type`, `is_superadmin`, `status`, `status_time`, `status_reason`, `login_fail_count`, `must_change_password`,
    `password_update_time`, `effective_start_time`, `effective_end_time`, `last_login_ip`, `last_login_time`,
    `is_builtin`, `source_type`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
) VALUES
-- 1. 超级管理员
('200001', '1', 'zhangweiqiang', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '张校长', '张伟强', 'male', NULL, '13800138001', 'zhangwq@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.100', '2026-07-06 09:15:00',
 1, 'initial', '校长，负责学校全面工作', 0, 'system', NOW(), 'system', NOW(), 0),

-- 2. 信息中心主任
('200002', '1', 'wangjianhua', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '王主任', '王建华', 'male', NULL, '13800138002', 'wangjh@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.101', '2026-07-06 08:30:00',
 0, 'manual', '信息中心主任，负责信息化建设', 0, 'system', NOW(), 'system', NOW(), 0),

-- 3. 教务处处长
('200003', '1', 'chenxiaoyan', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '陈处长', '陈晓燕', 'female', NULL, '13800138003', 'chenxy@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.102', '2026-07-06 08:45:00',
 0, 'manual', '教务处处长，负责教学管理工作', 0, 'system', NOW(), 'system', NOW(), 0),

-- 4. 科研处处长
('200004', '1', 'liuqiang', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '刘处长', '刘强', 'male', NULL, '13800138004', 'liuq@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.103', '2026-07-06 09:00:00',
 0, 'manual', '科研处处长，负责科研项目管理', 0, 'system', NOW(), 'system', NOW(), 0),

-- 5. 人事处处长
('200005', '1', 'wangfang', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '王处长', '王芳', 'female', NULL, '13800138005', 'wangf@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.104', '2026-07-06 09:10:00',
 0, 'manual', '人事处处长，负责教职工管理', 0, 'system', NOW(), 'system', NOW(), 0),

-- 6. 财务处处长
('200006', '1', 'zhaojun', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '赵处长', '赵军', 'male', NULL, '13800138006', 'zhaoj@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.105', '2026-07-06 08:20:00',
 0, 'manual', '财务处处长，负责财务管理工作', 0, 'system', NOW(), 'system', NOW(), 0),

-- 7. 学生处处长
('200007', '1', 'zhoumin', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '周处长', '周敏', 'female', NULL, '13800138007', 'zhoum@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.106', '2026-07-06 09:20:00',
 0, 'manual', '学生处处长，负责学生事务管理', 0, 'system', NOW(), 'system', NOW(), 0),

-- 8. 信息技术部主任
('200008', '1', 'chenwei', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '陈主任', '陈伟', 'male', NULL, '13800138008', 'chenw@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.107', '2026-07-06 08:50:00',
 0, 'manual', '信息技术部主任，负责技术开发', 0, 'system', NOW(), 'system', NOW(), 0),

-- 9. 数据管理部主任
('200009', '1', 'zhanglei', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '张主任', '张蕾', 'female', NULL, '13800138009', 'zhanglei@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.108', '2026-07-06 08:55:00',
 0, 'manual', '数据管理部主任，负责数据治理', 0, 'system', NOW(), 'system', NOW(), 0),

-- 10. 网络运维部主任
('200010', '1', 'ligang', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '李主任', '李刚', 'male', NULL, '13800138010', 'lig@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.109', '2026-07-06 09:05:00',
 0, 'manual', '网络运维部主任，负责网络运维', 0, 'system', NOW(), 'system', NOW(), 0),

-- 11. 数据库管理员
('200011', '1', 'liuyang', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '刘工', '刘洋', 'male', NULL, '13800138011', 'liuy@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.110', '2026-07-06 09:25:00',
 0, 'manual', '数据库管理员，负责数据库运维', 0, 'system', NOW(), 'system', NOW(), 0),

-- 12. 系统运维工程师
('200012', '1', 'huangzhiyuan', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '黄工', '黄志远', 'male', NULL, '13800138012', 'huangzy@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.111', '2026-07-06 08:40:00',
 0, 'manual', '系统运维工程师，负责系统监控', 0, 'system', NOW(), 'system', NOW(), 0),

-- 13. 数据分析师
('200013', '1', 'sunlihua', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '孙工', '孙丽华', 'female', NULL, '13800138013', 'sunlh@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.112', '2026-07-06 09:30:00',
 0, 'manual', '数据分析师，负责数据分析报表', 0, 'system', NOW(), 'system', NOW(), 0),

-- 14. 计算机学院院长
('200014', '1', 'zhouzhiming', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '周院长', '周志明', 'male', NULL, '13800138014', 'zhouzm@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.113', '2026-07-06 08:35:00',
 0, 'manual', '计算机学院院长，负责学院全面工作', 0, 'system', NOW(), 'system', NOW(), 0),

-- 15. 招生办公室主任
('200015', '1', 'zhengxiuying', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '郑主任', '郑秀英', 'female', NULL, '13800138015', 'zhengxy@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.114', '2026-07-06 09:35:00',
 0, 'manual', '招生办公室主任，负责招生工作', 0, 'system', NOW(), 'system', NOW(), 0),

-- 16. 教务处副处长
('200016', '1', 'wujianping', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '吴副处长', '吴建平', 'male', NULL, '13800138016', 'wujp@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.115', '2026-07-06 09:40:00',
 0, 'manual', '教务处副处长，协助处长工作', 0, 'system', NOW(), 'system', NOW(), 0),

-- 17. 科研处副处长
('200017', '1', 'wudehua', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '王副处长', '王德华', 'male', NULL, '13800138017', 'wangdh@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.116', '2026-07-06 08:25:00',
 0, 'manual', '科研处副处长，协助处长工作', 0, 'system', NOW(), 'system', NOW(), 0),

-- 18. 财务处会计
('200018', '1', 'hezhiqiang', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '何会计', '何志强', 'male', NULL, '13800138018', 'hezq@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.117', '2026-07-06 09:45:00',
 0, 'manual', '财务处会计，负责账务处理', 0, 'system', NOW(), 'system', NOW(), 0),

-- 19. 人事处副处长
('200019', '1', 'linzhenghua', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '林副处长', '林正华', 'male', NULL, '13800138019', 'linzh@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.118', '2026-07-06 09:50:00',
 0, 'manual', '人事处副处长，协助处长工作', 0, 'system', NOW(), 'system', NOW(), 0),

-- 20. 系统运维工程师
('200020', '1', 'majiang', '{bcrypt}$2b$10$Z0k6MIe47KMVn6.Hpzt9juIuScAP5MMeYxcZ45Axq/m4A9K/AR5vC',
 '马工', '马强', 'male', NULL, '13800138020', 'maj@hust.edu.cn',
 'backend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '192.168.1.119', '2026-07-06 08:15:00',
 0, 'manual', '系统运维工程师，负责服务器维护', 0, 'system', NOW(), 'system', NOW(), 0);


-- ================================================================
-- 七、前台用户数据（30名真实用户）
-- 密码统一使用：{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG (portal@123)
-- ================================================================

INSERT INTO `sys_user` (
    `id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `gender`, `avatar`, `phone`, `email`,
    `user_type`, `is_superadmin`, `status`, `status_time`, `status_reason`, `login_fail_count`, `must_change_password`,
    `password_update_time`, `effective_start_time`, `effective_end_time`, `last_login_ip`, `last_login_time`,
    `is_builtin`, `source_type`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
) VALUES
-- 教师用户（10名）
('300001', '1', 'liming', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '李老师', '李明', 'male', NULL, '13900139001', 'liming@hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.0.1', '2026-07-06 10:00:00',
 0, 'manual', '计算机学院副教授，研究方向为人工智能', 0, 'system', NOW(), 'system', NOW(), 0),

('300002', '1', 'zhangyan', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '张老师', '张燕', 'female', NULL, '13900139002', 'zhangyan@hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.0.2', '2026-07-06 10:15:00',
 0, 'manual', '外国语学院讲师，研究方向为英语语言学', 0, 'system', NOW(), 'system', NOW(), 0),

('300003', '1', 'wanggang', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '王老师', '王刚', 'male', NULL, '13900139003', 'wangg@hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.0.3', '2026-07-06 10:30:00',
 0, 'manual', '机械工程学院教授，研究方向为机器人技术', 0, 'system', NOW(), 'system', NOW(), 0),

('300004', '1', 'liumin', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '刘老师', '刘敏', 'female', NULL, '13900139004', 'liumin@hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.0.4', '2026-07-06 10:45:00',
 0, 'manual', '经济管理学院副教授，研究方向为市场营销', 0, 'system', NOW(), 'system', NOW(), 0),

('300005', '1', 'zhaoli', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '赵老师', '赵丽', 'female', NULL, '13900139005', 'zhaol@hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.0.5', '2026-07-06 11:00:00',
 0, 'manual', '理学院讲师，研究方向为应用数学', 0, 'system', NOW(), 'system', NOW(), 0),

('300006', '1', 'sunwei', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '孙老师', '孙伟', 'male', NULL, '13900139006', 'sunw@hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.0.6', '2026-07-06 11:15:00',
 0, 'manual', '电子工程学院教授，研究方向为通信工程', 0, 'system', NOW(), 'system', NOW(), 0),

('300007', '1', 'zhoujing', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '周老师', '周静', 'female', NULL, '13900139007', 'zhouj@hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.0.7', '2026-07-06 11:30:00',
 0, 'manual', '文学院副教授，研究方向为中国现当代文学', 0, 'system', NOW(), 'system', NOW(), 0),

('300008', '1', 'wuhua', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '吴老师', '吴华', 'male', NULL, '13900139008', 'wuh@hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.0.8', '2026-07-06 11:45:00',
 0, 'manual', '法学院讲师，研究方向为宪法学', 0, 'system', NOW(), 'system', NOW(), 0),

('300009', '1', 'zhengmei', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '郑老师', '郑梅', 'female', NULL, '13900139009', 'zhengm@hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.0.9', '2026-07-06 12:00:00',
 0, 'manual', '艺术学院副教授，研究方向为视觉传达', 0, 'system', NOW(), 'system', NOW(), 0),

('300010', '1', 'huanghai', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '黄老师', '黄海', 'male', NULL, '13900139010', 'huangh@hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.0.10', '2026-07-06 12:15:00',
 0, 'manual', '体育部讲师，研究方向为体育训练', 0, 'system', NOW(), 'system', NOW(), 0),

-- 学生用户（15名）
('300011', '1', 'chenxiaoming', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '陈同学', '陈小明', 'male', NULL, '13900139011', 'chenxm@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.1', '2026-07-06 14:00:00',
 0, 'manual', '计算机学院2023级本科生', 0, 'system', NOW(), 'system', NOW(), 0),

('300012', '1', 'wanglihua', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '王同学', '王丽华', 'female', NULL, '13900139012', 'wanglh@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.2', '2026-07-06 14:15:00',
 0, 'manual', '经济管理学院2024级硕士研究生', 0, 'system', NOW(), 'system', NOW(), 0),

('300013', '1', 'liqiang', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '李同学', '李强', 'male', NULL, '13900139013', 'liqiang@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.3', '2026-07-06 14:30:00',
 0, 'manual', '机械工程学院2023级博士研究生', 0, 'system', NOW(), 'system', NOW(), 0),

('300014', '1', 'zhangmeiling', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '张同学', '张美玲', 'female', NULL, '13900139014', 'zhangml@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.4', '2026-07-06 14:45:00',
 0, 'manual', '外国语学院2024级本科生', 0, 'system', NOW(), 'system', NOW(), 0),

('300015', '1', 'wangpeng', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '王同学', '王鹏', 'male', NULL, '13900139015', 'wangp@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.5', '2026-07-06 15:00:00',
 0, 'manual', '电子工程学院2023级本科生', 0, 'system', NOW(), 'system', NOW(), 0),

('300016', '1', 'liuna', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '刘同学', '刘娜', 'female', NULL, '13900139016', 'liuna@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.6', '2026-07-06 15:15:00',
 0, 'manual', '理学院2024级硕士研究生', 0, 'system', NOW(), 'system', NOW(), 0),

('300017', '1', 'zhaogang', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '赵同学', '赵刚', 'male', NULL, '13900139017', 'zhaog@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.7', '2026-07-06 15:30:00',
 0, 'manual', '计算机学院2023级本科生', 0, 'system', NOW(), 'system', NOW(), 0),

('300018', '1', 'huangyan', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '黄同学', '黄燕', 'female', NULL, '13900139018', 'huangy@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.8', '2026-07-06 15:45:00',
 0, 'manual', '文学院2024级本科生', 0, 'system', NOW(), 'system', NOW(), 0),

('300019', '1', 'zhoujie', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '周同学', '周杰', 'male', NULL, '13900139019', 'zhouj@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.9', '2026-07-06 16:00:00',
 0, 'manual', '法学院2023级本科生', 0, 'system', NOW(), 'system', NOW(), 0),

('300020', '1', 'wumin', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '吴同学', '吴敏', 'female', NULL, '13900139020', 'wumin@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.10', '2026-07-06 16:15:00',
 0, 'manual', '艺术学院2024级本科生', 0, 'system', NOW(), 'system', NOW(), 0),

('300021', '1', 'sunhao', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '孙同学', '孙浩', 'male', NULL, '13900139021', 'sunh@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.11', '2026-07-06 16:30:00',
 0, 'manual', '计算机学院2024级硕士研究生', 0, 'system', NOW(), 'system', NOW(), 0),

('300022', '1', 'chenxiaoli', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '陈同学', '陈小丽', 'female', NULL, '13900139022', 'chenxl@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.12', '2026-07-06 16:45:00',
 0, 'manual', '经济管理学院2023级本科生', 0, 'system', NOW(), 'system', NOW(), 0),

('300023', '1', 'yangfei', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '杨同学', '杨飞', 'male', NULL, '13900139023', 'yangf@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.13', '2026-07-06 17:00:00',
 0, 'manual', '电子工程学院2024级本科生', 0, 'system', NOW(), 'system', NOW(), 0),

('300024', '1', 'hejing', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '何同学', '何静', 'female', NULL, '13900139024', 'hej@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.14', '2026-07-06 17:15:00',
 0, 'manual', '理学院2023级博士研究生', 0, 'system', NOW(), 'system', NOW(), 0),

('300025', '1', 'linbo', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '林同学', '林波', 'male', NULL, '13900139025', 'linb@student.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.1.15', '2026-07-06 17:30:00',
 0, 'manual', '机械工程学院2024级本科生', 0, 'system', NOW(), 'system', NOW(), 0),

-- 校友和访问学者（5名）
('300026', '1', 'xufeng', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '徐校友', '徐峰', 'male', NULL, '13900139026', 'xuf@alumni.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.2.1', '2026-07-06 18:00:00',
 0, 'manual', '2015届毕业生，现就职于华为技术有限公司', 0, 'system', NOW(), 'system', NOW(), 0),

('300027', '1', 'majing', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '马校友', '马静', 'female', NULL, '13900139027', 'maj@alumni.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.2.2', '2026-07-06 18:15:00',
 0, 'manual', '2018届毕业生，现就职于阿里巴巴集团', 0, 'system', NOW(), 'system', NOW(), 0),

('300028', '1', 'gaoming', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '高校友', '高明', 'male', NULL, '13900139028', 'gaom@alumni.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.2.3', '2026-07-06 18:30:00',
 0, 'manual', '2020届毕业生，现就职于腾讯科技', 0, 'system', NOW(), 'system', NOW(), 0),

('300029', '1', 'robert_wang', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '王教授', 'Robert Wang', 'male', NULL, '13900139029', 'robert@visiting.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.3.1', '2026-07-06 19:00:00',
 0, 'manual', 'MIT访问学者，研究方向为计算机视觉', 0, 'system', NOW(), 'system', NOW(), 0),

('300030', '1', 'sarah_li', '{bcrypt}$2b$10$T/ZPI4.46L/XsPyYZTLcmeUF3orBKCfrOjbDgJZUitKeT2d.1g6WG',
 '李博士', 'Sarah Li', 'female', NULL, '13900139030', 'sarah@visiting.hust.edu.cn',
 'frontend', 0, 'active', NULL, NULL, 0, 0,
 NOW(), NOW(), NULL, '10.0.3.2', '2026-07-06 19:15:00',
 0, 'manual', 'Stanford访问学者，研究方向为自然语言处理', 0, 'system', NOW(), 'system', NOW(), 0);


-- ================================================================
-- 八、用户部门关联数据
-- ================================================================

INSERT INTO `sys_user_dept` (
    `id`, `tenant_id`, `user_id`, `dept_id`, `is_primary`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
) VALUES
-- 后台用户部门关联（校长关联校办公室）
('600001', '1', '200001', '101', 1, 'system', NOW(), 'system', NOW(), 0),
('600002', '1', '200002', '200', 1, 'system', NOW(), 'system', NOW(), 0),
('600003', '1', '200003', '104', 1, 'system', NOW(), 'system', NOW(), 0),
('600004', '1', '200004', '105', 1, 'system', NOW(), 'system', NOW(), 0),
('600005', '1', '200005', '102', 1, 'system', NOW(), 'system', NOW(), 0),
('600006', '1', '200006', '103', 1, 'system', NOW(), 'system', NOW(), 0),
('600007', '1', '200007', '106', 1, 'system', NOW(), 'system', NOW(), 0),
('600008', '1', '200008', '201', 1, 'system', NOW(), 'system', NOW(), 0),
('600009', '1', '200009', '202', 1, 'system', NOW(), 'system', NOW(), 0),
('600010', '1', '200010', '203', 1, 'system', NOW(), 'system', NOW(), 0),
('600011', '1', '200011', '202', 1, 'system', NOW(), 'system', NOW(), 0),
('600012', '1', '200012', '203', 1, 'system', NOW(), 'system', NOW(), 0),
('600013', '1', '200013', '202', 1, 'system', NOW(), 'system', NOW(), 0),
('600014', '1', '200014', '300', 1, 'system', NOW(), 'system', NOW(), 0),
('600015', '1', '200015', '109', 1, 'system', NOW(), 'system', NOW(), 0),
('600016', '1', '200016', '104', 1, 'system', NOW(), 'system', NOW(), 0),
('600017', '1', '200017', '105', 1, 'system', NOW(), 'system', NOW(), 0),
('600018', '1', '200018', '103', 1, 'system', NOW(), 'system', NOW(), 0),
('600019', '1', '200019', '102', 1, 'system', NOW(), 'system', NOW(), 0),
('600020', '1', '200020', '203', 1, 'system', NOW(), 'system', NOW(), 0),

-- 前台用户部门关联
('600021', '1', '300001', '300', 1, 'system', NOW(), 'system', NOW(), 0),
('600022', '1', '300002', '304', 1, 'system', NOW(), 'system', NOW(), 0),
('600023', '1', '300003', '302', 1, 'system', NOW(), 'system', NOW(), 0),
('600024', '1', '300004', '303', 1, 'system', NOW(), 'system', NOW(), 0),
('600025', '1', '300005', '305', 1, 'system', NOW(), 'system', NOW(), 0),
('600026', '1', '300006', '301', 1, 'system', NOW(), 'system', NOW(), 0),
('600027', '1', '300007', '306', 1, 'system', NOW(), 'system', NOW(), 0),
('600028', '1', '300008', '307', 1, 'system', NOW(), 'system', NOW(), 0),
('600029', '1', '300009', '308', 1, 'system', NOW(), 'system', NOW(), 0),
('600030', '1', '300010', '309', 1, 'system', NOW(), 'system', NOW(), 0),
('600031', '1', '300011', '300', 1, 'system', NOW(), 'system', NOW(), 0),
('600032', '1', '300012', '303', 1, 'system', NOW(), 'system', NOW(), 0),
('600033', '1', '300013', '302', 1, 'system', NOW(), 'system', NOW(), 0),
('600034', '1', '300014', '304', 1, 'system', NOW(), 'system', NOW(), 0),
('600035', '1', '300015', '301', 1, 'system', NOW(), 'system', NOW(), 0),
('600036', '1', '300016', '305', 1, 'system', NOW(), 'system', NOW(), 0),
('600037', '1', '300017', '300', 1, 'system', NOW(), 'system', NOW(), 0),
('600038', '1', '300018', '306', 1, 'system', NOW(), 'system', NOW(), 0),
('600039', '1', '300019', '307', 1, 'system', NOW(), 'system', NOW(), 0),
('600040', '1', '300020', '308', 1, 'system', NOW(), 'system', NOW(), 0),
('600041', '1', '300021', '300', 1, 'system', NOW(), 'system', NOW(), 0),
('600042', '1', '300022', '303', 1, 'system', NOW(), 'system', NOW(), 0),
('600043', '1', '300023', '301', 1, 'system', NOW(), 'system', NOW(), 0),
('600044', '1', '300024', '305', 1, 'system', NOW(), 'system', NOW(), 0),
('600045', '1', '300025', '302', 1, 'system', NOW(), 'system', NOW(), 0);


-- ================================================================
-- 九、用户岗位关联数据
-- ================================================================

INSERT INTO `sys_user_post` (
    `id`, `tenant_id`, `user_id`, `post_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
) VALUES
-- 后台用户岗位关联
('700001', '1', '200001', '1001', 'system', NOW(), 'system', NOW(), 0),
('700002', '1', '200002', '1009', 'system', NOW(), 'system', NOW(), 0),
('700003', '1', '200003', '1004', 'system', NOW(), 'system', NOW(), 0),
('700004', '1', '200004', '1005', 'system', NOW(), 'system', NOW(), 0),
('700005', '1', '200005', '1008', 'system', NOW(), 'system', NOW(), 0),
('700006', '1', '200006', '1007', 'system', NOW(), 'system', NOW(), 0),
('700007', '1', '200007', '1006', 'system', NOW(), 'system', NOW(), 0),
('700008', '1', '200008', '1009', 'system', NOW(), 'system', NOW(), 0),
('700009', '1', '200009', '1010', 'system', NOW(), 'system', NOW(), 0),
('700010', '1', '200010', '1011', 'system', NOW(), 'system', NOW(), 0),
('700011', '1', '200011', '1010', 'system', NOW(), 'system', NOW(), 0),
('700012', '1', '200012', '1011', 'system', NOW(), 'system', NOW(), 0),
('700013', '1', '200013', '1012', 'system', NOW(), 'system', NOW(), 0),
('700014', '1', '200014', '1002', 'system', NOW(), 'system', NOW(), 0),
('700015', '1', '200015', '1013', 'system', NOW(), 'system', NOW(), 0),
('700016', '1', '200016', '1004', 'system', NOW(), 'system', NOW(), 0),
('700017', '1', '200017', '1005', 'system', NOW(), 'system', NOW(), 0),
('700018', '1', '200018', '1007', 'system', NOW(), 'system', NOW(), 0),
('700019', '1', '200019', '1008', 'system', NOW(), 'system', NOW(), 0),
('700020', '1', '200020', '1011', 'system', NOW(), 'system', NOW(), 0),

-- 前台用户岗位关联
('700021', '1', '300001', '2004', 'system', NOW(), 'system', NOW(), 0),
('700022', '1', '300002', '2004', 'system', NOW(), 'system', NOW(), 0),
('700023', '1', '300003', '2002', 'system', NOW(), 'system', NOW(), 0),
('700024', '1', '300004', '2003', 'system', NOW(), 'system', NOW(), 0),
('700025', '1', '300005', '2004', 'system', NOW(), 'system', NOW(), 0),
('700026', '1', '300006', '2002', 'system', NOW(), 'system', NOW(), 0),
('700027', '1', '300007', '2003', 'system', NOW(), 'system', NOW(), 0),
('700028', '1', '300008', '2004', 'system', NOW(), 'system', NOW(), 0),
('700029', '1', '300009', '2003', 'system', NOW(), 'system', NOW(), 0),
('700030', '1', '300010', '2004', 'system', NOW(), 'system', NOW(), 0),
('700031', '1', '300011', '2006', 'system', NOW(), 'system', NOW(), 0),
('700032', '1', '300012', '2007', 'system', NOW(), 'system', NOW(), 0),
('700033', '1', '300013', '2008', 'system', NOW(), 'system', NOW(), 0),
('700034', '1', '300014', '2006', 'system', NOW(), 'system', NOW(), 0),
('700035', '1', '300015', '2006', 'system', NOW(), 'system', NOW(), 0),
('700036', '1', '300016', '2007', 'system', NOW(), 'system', NOW(), 0),
('700037', '1', '300017', '2006', 'system', NOW(), 'system', NOW(), 0),
('700038', '1', '300018', '2006', 'system', NOW(), 'system', NOW(), 0),
('700039', '1', '300019', '2006', 'system', NOW(), 'system', NOW(), 0),
('700040', '1', '300020', '2006', 'system', NOW(), 'system', NOW(), 0),
('700041', '1', '300021', '2007', 'system', NOW(), 'system', NOW(), 0),
('700042', '1', '300022', '2006', 'system', NOW(), 'system', NOW(), 0),
('700043', '1', '300023', '2006', 'system', NOW(), 'system', NOW(), 0),
('700044', '1', '300024', '2008', 'system', NOW(), 'system', NOW(), 0),
('700045', '1', '300025', '2006', 'system', NOW(), 'system', NOW(), 0),
('700046', '1', '300026', '2010', 'system', NOW(), 'system', NOW(), 0),
('700047', '1', '300027', '2010', 'system', NOW(), 'system', NOW(), 0),
('700048', '1', '300028', '2010', 'system', NOW(), 'system', NOW(), 0),
('700049', '1', '300029', '2009', 'system', NOW(), 'system', NOW(), 0),
('700050', '1', '300030', '2009', 'system', NOW(), 'system', NOW(), 0);


-- ================================================================
-- 十、用户角色关联数据
-- ================================================================

INSERT INTO `sys_user_role` (
    `id`, `tenant_id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
) VALUES
-- 后台用户角色关联
('800001', '1', '200001', '1', 'system', NOW(), 'system', NOW(), 0),
('800002', '1', '200002', '300', 'system', NOW(), 'system', NOW(), 0),
('800003', '1', '200003', '200', 'system', NOW(), 'system', NOW(), 0),
('800004', '1', '200004', '201', 'system', NOW(), 'system', NOW(), 0),
('800005', '1', '200005', '204', 'system', NOW(), 'system', NOW(), 0),
('800006', '1', '200006', '203', 'system', NOW(), 'system', NOW(), 0),
('800007', '1', '200007', '202', 'system', NOW(), 'system', NOW(), 0),
('800008', '1', '200008', '300', 'system', NOW(), 'system', NOW(), 0),
('800009', '1', '200009', '301', 'system', NOW(), 'system', NOW(), 0),
('800010', '1', '200010', '302', 'system', NOW(), 'system', NOW(), 0),
('800011', '1', '200011', '301', 'system', NOW(), 'system', NOW(), 0),
('800012', '1', '200012', '302', 'system', NOW(), 'system', NOW(), 0),
('800013', '1', '200013', '303', 'system', NOW(), 'system', NOW(), 0),
('800014', '1', '200014', '404', 'system', NOW(), 'system', NOW(), 0),
('800015', '1', '200015', '205', 'system', NOW(), 'system', NOW(), 0),
('800016', '1', '200016', '200', 'system', NOW(), 'system', NOW(), 0),
('800017', '1', '200017', '201', 'system', NOW(), 'system', NOW(), 0),
('800018', '1', '200018', '203', 'system', NOW(), 'system', NOW(), 0),
('800019', '1', '200019', '204', 'system', NOW(), 'system', NOW(), 0),
('800020', '1', '200020', '302', 'system', NOW(), 'system', NOW(), 0),

-- 前台用户角色关联
('800021', '1', '300001', '400', 'system', NOW(), 'system', NOW(), 0),
('800022', '1', '300002', '400', 'system', NOW(), 'system', NOW(), 0),
('800023', '1', '300003', '400', 'system', NOW(), 'system', NOW(), 0),
('800024', '1', '300004', '400', 'system', NOW(), 'system', NOW(), 0),
('800025', '1', '300005', '400', 'system', NOW(), 'system', NOW(), 0),
('800026', '1', '300006', '400', 'system', NOW(), 'system', NOW(), 0),
('800027', '1', '300007', '400', 'system', NOW(), 'system', NOW(), 0),
('800028', '1', '300008', '400', 'system', NOW(), 'system', NOW(), 0),
('800029', '1', '300009', '400', 'system', NOW(), 'system', NOW(), 0),
('800030', '1', '300010', '400', 'system', NOW(), 'system', NOW(), 0),
('800031', '1', '300011', '401', 'system', NOW(), 'system', NOW(), 0),
('800032', '1', '300012', '401', 'system', NOW(), 'system', NOW(), 0),
('800033', '1', '300013', '401', 'system', NOW(), 'system', NOW(), 0),
('800034', '1', '300014', '401', 'system', NOW(), 'system', NOW(), 0),
('800035', '1', '300015', '401', 'system', NOW(), 'system', NOW(), 0),
('800036', '1', '300016', '401', 'system', NOW(), 'system', NOW(), 0),
('800037', '1', '300017', '401', 'system', NOW(), 'system', NOW(), 0),
('800038', '1', '300018', '401', 'system', NOW(), 'system', NOW(), 0),
('800039', '1', '300019', '401', 'system', NOW(), 'system', NOW(), 0),
('800040', '1', '300020', '401', 'system', NOW(), 'system', NOW(), 0),
('800041', '1', '300021', '401', 'system', NOW(), 'system', NOW(), 0),
('800042', '1', '300022', '401', 'system', NOW(), 'system', NOW(), 0),
('800043', '1', '300023', '401', 'system', NOW(), 'system', NOW(), 0),
('800044', '1', '300024', '401', 'system', NOW(), 'system', NOW(), 0),
('800045', '1', '300025', '401', 'system', NOW(), 'system', NOW(), 0),
('800046', '1', '300026', '403', 'system', NOW(), 'system', NOW(), 0),
('800047', '1', '300027', '403', 'system', NOW(), 'system', NOW(), 0),
('800048', '1', '300028', '403', 'system', NOW(), 'system', NOW(), 0),
('800049', '1', '300029', '402', 'system', NOW(), 'system', NOW(), 0),
('800050', '1', '300030', '402', 'system', NOW(), 'system', NOW(), 0);


-- ================================================================
-- 十一、模拟用户的角色绑定
-- ================================================================

INSERT INTO `sys_user_role` (
    `id`, `tenant_id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
) VALUES
-- 绑定模拟用户到角色（超级管理员通过 sys_user.is_superadmin 标记，不走角色体系）
('900001', '1', '2', '405', 'system', NOW(), 'system', NOW(), 0);


-- 打开外键检查
SET FOREIGN_KEY_CHECKS = 1;
