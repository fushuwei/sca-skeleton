-- ============================================================
-- 部门种子数据：北京科技大学组织架构
-- ============================================================
-- 使用场景：首次部署时初始化默认组织机构，基于北京科技大学真实组织架构简化而来
-- 生成方式：手动编写
-- 注意：ID 使用 UUID 格式，treePath 格式为 "0,parentId,selfId"
--       父部门 ID 为 "0" 表示根节点
-- ============================================================

INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)

-- ==================== 党群/管理机构 ====================

-- 党委办公室、校长办公室（合署办公）
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('201', '1', '0', '党委办公室、校长办公室', 'DANGXIAO_BAN', 10, '张教授', '010-62332236', 'dangxiaoban@ustb.edu.cn', 'enabled', '0,201', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 组织部（含党校）
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('202', '1', '0', '组织部（党校）', 'ZUZHI_BU', 20, '孙部长', '010-62332345', 'zzb@ustb.edu.cn', 'enabled', '0,202', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 宣传部（含新闻中心）
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('203', '1', '0', '宣传部（新闻中心）', 'XUANCHUAN_BU', 30, '李部长', '010-62332245', 'xcb@ustb.edu.cn', 'enabled', '0,203', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 统战部
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('204', '1', '0', '统战部', 'TONGZHAN_BU', 40, '王部长', '010-62333686', 'tzb@ustb.edu.cn', 'enabled', '0,204', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 纪委办公室、监察室
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('205', '1', '0', '纪委办公室（监察室）', 'JIWEI_BAN', 50, '刘书记', '010-62332277', 'jiwei@ustb.edu.cn', 'enabled', '0,205', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 巡察工作办公室
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('206', '1', '0', '巡察工作办公室', 'XUNCHA_BAN', 55, '赵主任', '010-62334663', 'xuncha@ustb.edu.cn', 'enabled', '0,206', 0, 'admin', NOW(), 'admin', NOW(), 0);
-- 工会
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('207', '1', '0', '工会', 'GONG_HUI', 60, '陈主席', '010-62332891', 'gonghui@ustb.edu.cn', 'enabled', '0,207', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 团委
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('208', '1', '0', '团委', 'TUAN_WEI', 70, '王书记', '010-62332293', 'tw@ustb.edu.cn', 'enabled', '0,208', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- ==================== 行政管理处室 ====================

-- 人事处
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('301', '1', '0', '人事处', 'RENSHI_CHU', 110, '赵处长', '010-62332389', 'rsc@ustb.edu.cn', 'enabled', '0,301', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 人事处 - 师资科
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('30101', '1', '301', '师资科', 'SHIZI_KE', 111, '张科长', '010-62332390', 'shizi@ustb.edu.cn', 'enabled', '0,301,30101', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 人事处 - 人事科
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('30102', '1', '301', '人事科', 'RENSHI_KE', 112, '李科长', '010-62332389', 'renshike@ustb.edu.cn', 'enabled', '0,301,30102', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 人事处 - 薪酬福利科
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('30103', '1', '301', '薪酬福利科', 'XINCHOU_KE', 113, '王科长', '010-62332788', 'xinchou@ustb.edu.cn', 'enabled', '0,301,30103', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 人事处 - 人才发展中心
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('30104', '1', '301', '人才发展中心', 'RENCAI_ZX', 114, '陈主任', '010-62334897', 'rencai@ustb.edu.cn', 'enabled', '0,301,30104', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 教务处
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('302', '1', '0', '教务处', 'JIAOWU_CHU', 120, '刘处长', '010-62332438', 'jwc@ustb.edu.cn', 'enabled', '0,302', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 教务处 - 教学运行科
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('30201', '1', '302', '教学运行科', 'JIAOXUE_YUNXING', 121, '周科长', '010-62332397', 'yxk@ustb.edu.cn', 'enabled', '0,302,30201', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 教务处 - 教学研究科
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('30202', '1', '302', '教学研究科', 'JIAOXUE_YANJIU', 122, '吴科长', '010-62332395', 'jxyj@ustb.edu.cn', 'enabled', '0,302,30202', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 教务处 - 学籍管理科
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('30203', '1', '302', '学籍管理科', 'XUEJI_KE', 123, '郑科长', '010-62332404', 'xjgl@ustb.edu.cn', 'enabled', '0,302,30203', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 科研院（科学研究与发展部）
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('303', '1', '0', '科研院（科学研究与发展部）', 'KEYAN_YUAN', 130, '陈处长', '010-62332414', 'kyy@ustb.edu.cn', 'enabled', '0,303', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 科研院 - 基础研究科
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('30301', '1', '303', '基础研究科', 'JICHU_YANJIU', 131, '刘科长', '010-62332414', 'jcyjk@ustb.edu.cn', 'enabled', '0,303,30301', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 科研院 - 高新技术科
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('30302', '1', '303', '高新技术科', 'GAOXIN_JISHU', 132, '黄科长', '010-62332414', 'gxjs@ustb.edu.cn', 'enabled', '0,303,30302', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 研究生院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('304', '1', '0', '研究生院', 'YANJIUSHENG_YUAN', 140, '张院长', '010-62332391', 'yjsy@ustb.edu.cn', 'enabled', '0,304', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 学生工作部（处）
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('305', '1', '0', '学生工作部（处）', 'XUESHENG_CHU', 150, '秦处长', '010-62332648', 'xsc@ustb.edu.cn', 'enabled', '0,305', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 招生就业处
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('306', '1', '0', '招生就业处', 'ZHAOSHENG_CHU', 160, '徐处长', '010-62332387', 'zsjyc@ustb.edu.cn', 'enabled', '0,306', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 财务处
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('307', '1', '0', '财务处', 'CAIWU_CHU', 170, '周处长', '010-62332328', 'cwc@ustb.edu.cn', 'enabled', '0,307', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 审计处
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('308', '1', '0', '审计处', 'SHENJI_CHU', 180, '马处长', '010-62332238', 'sjc@ustb.edu.cn', 'enabled', '0,308', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 国际合作与交流处（港澳台办）
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('309', '1', '0', '国际合作与交流处（港澳台办）', 'GUOJI_CHU', 190, '冯处长', '010-62332497', 'studyabroad@ustb.edu.cn', 'enabled', '0,309', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 后勤管理处
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('310', '1', '0', '后勤管理处', 'HOUQIN_CHU', 200, '高处长', '010-62332336', 'hqc@ustb.edu.cn', 'enabled', '0,310', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 保卫处
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('311', '1', '0', '保卫处（部）', 'BAOWEI_CHU', 210, '张处长', '010-62332358', 'bwc@ustb.edu.cn', 'enabled', '0,311', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 离退休职工工作处
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('312', '1', '0', '离退休职工工作处', 'LITUIXIU_CHU', 220, '乔处长', '010-62332597', 'ltx@ustb.edu.cn', 'enabled', '0,312', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- ==================== 直属单位/公共服务 ====================

-- 信息化建设与管理办公室（信息中心/网络安全和信息化办公室）
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('401', '1', '0', '信息化建设与管理办公室', 'XINXIHUA_BAN', 310, '杨主任', '010-62332243', 'xxh@ustb.edu.cn', 'enabled', '0,401', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 信息中心 - 网络运维科
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('40101', '1', '401', '网络运维科', 'WANGLAO_KE', 311, '孙科长', '010-62332243', 'wangluo@ustb.edu.cn', 'enabled', '0,401,40101', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 信息中心 - 应用开发科
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('40102', '1', '401', '应用开发科', 'YINGYONG_KE', 312, '李科长', '010-62332244', 'appdev@ustb.edu.cn', 'enabled', '0,401,40102', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 信息中心 - 数据管理科
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
-- 档案馆
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('403', '1', '0', '档案馆', 'DANGANGUAN', 330, '赵馆长', '010-62332396', 'dag@ustb.edu.cn', 'enabled', '0,403', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 校医院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('404', '1', '0', '校医院', 'XIAO_YIYUAN', 340, '徐院长', '010-62332373', 'xyy@ustb.edu.cn', 'enabled', '0,404', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 期刊中心
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('405', '1', '0', '期刊中心', 'QIKAN_ZX', 350, '黄主任', '010-62332459', 'qikan@ustb.edu.cn', 'enabled', '0,405', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 创新创业中心
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('406', '1', '0', '创新创业中心', 'CHUANGXIN_ZX', 360, '何主任', '010-62333787', 'cxcy@ustb.edu.cn', 'enabled', '0,406', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- ==================== 学院与教学单位 ====================

-- 材料科学与工程学院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('501', '1', '0', '材料科学与工程学院', 'CL_XY', 510, '廖院长', '010-62332378', 'clxy@ustb.edu.cn', 'enabled', '0,501', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 冶金与生态工程学院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('502', '1', '0', '冶金与生态工程学院', 'YJ_XY', 520, '焦院长', '010-62332388', 'yjxy@ustb.edu.cn', 'enabled', '0,502', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 土木与资源工程学院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('503', '1', '0', '土木与资源工程学院', 'TM_XY', 530, '苗院长', '010-62332368', 'tmxy@ustb.edu.cn', 'enabled', '0,503', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 机械工程学院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('504', '1', '0', '机械工程学院', 'JX_XY', 540, '马院长', '010-62332348', 'jxxy@ustb.edu.cn', 'enabled', '0,504', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 能源与环境工程学院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('505', '1', '0', '能源与环境工程学院', 'NH_XY', 550, '邢院长', '010-62332366', 'nhxy@ustb.edu.cn', 'enabled', '0,505', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 自动化学院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('506', '1', '0', '自动化学院', 'ZDH_XY', 560, '张院长', '010-62332392', 'zdhxy@ustb.edu.cn', 'enabled', '0,506', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 计算机与通信工程学院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('507', '1', '0', '计算机与通信工程学院', 'JT_XY', 570, '殷院长', '010-62332383', 'jtxy@ustb.edu.cn', 'enabled', '0,507', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 数理学院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('508', '1', '0', '数理学院', 'SL_XY', 580, '庞院长', '010-62332376', 'slxy@ustb.edu.cn', 'enabled', '0,508', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 化学与生物工程学院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('509', '1', '0', '化学与生物工程学院', 'HS_XY', 590, '李院长', '010-62332375', 'hsxy@ustb.edu.cn', 'enabled', '0,509', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 经济管理学院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('510', '1', '0', '经济管理学院', 'JG_XY', 600, '谷院长', '010-62332393', 'jgxy@ustb.edu.cn', 'enabled', '0,510', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 文法学院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('511', '1', '0', '文法学院', 'WF_XY', 610, '魏院长', '010-62332385', 'wfxy@ustb.edu.cn', 'enabled', '0,511', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 马克思主义学院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('512', '1', '0', '马克思主义学院', 'MKS_XY', 620, '宋院长', '010-62332386', 'mksxy@ustb.edu.cn', 'enabled', '0,512', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 外国语学院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('513', '1', '0', '外国语学院', 'WGY_XY', 630, '张院长', '010-62332382', 'wgyxy@ustb.edu.cn', 'enabled', '0,513', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 高等工程师学院
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('514', '1', '0', '高等工程师学院', 'GJ_XY', 640, '张院长', '010-62332388', 'gjxy@ustb.edu.cn', 'enabled', '0,514', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 体育部
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('515', '1', '0', '体育部', 'TIYU_BU', 650, '王主任', '010-62332374', 'tyb@ustb.edu.cn', 'enabled', '0,515', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 国际学生中心
INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`)
VALUES ('516', '1', '0', '国际学生中心', 'GUOJI_XUESHENG', 660, '赵主任', '010-62332497', 'lxg@ustb.edu.cn', 'enabled', '0,516', 0, 'admin', NOW(), 'admin', NOW(), 0);

