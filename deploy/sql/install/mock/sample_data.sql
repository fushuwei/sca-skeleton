-- 设置字符集
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- 关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;


-- ================================================================
-- 租户
-- ================================================================

INSERT INTO sys_tenant_package (id,name,code,status,user_limit,api_limit,storage_limit,expire_days,sort,remark,version,create_by,create_time,update_by,update_time,is_deleted) VALUES
	 ('1','默认套餐','default','enabled',-1,-1,-1,-1,100,NULL,0,'1',now(),'1',now(),0),
	 ('2','基础套餐','base','enabled',20,1000,10,180,100,NULL,1,'1',now(),'1',now(),0),
	 ('3','迷你套餐','mini','enabled',10,500,5,180,100,NULL,0,'1',now(),'1',now(),0),
	 ('4','测试套餐（已停用）','test','disabled',-1,-1,-1,-1,100,NULL,0,'1',now(),'1',now(),0);

INSERT INTO sys_tenant (id,name,code,package_id,contact_name,contact_phone,contact_email,domain_name,effective_time,expire_time,status,is_builtin,config_json,remark,version,create_by,create_time,update_by,update_time,is_deleted) VALUES
	 ('1','默认租户','default','1','李白','13656780987','libai@example.com','https://newease.cloud',NULL,NULL,'normal',1,NULL,NULL,0,'1',now(),'1',now(),0);


-- ================================================================
-- 部门
-- ================================================================

INSERT INTO `sys_dept` (`id`, `tenant_id`, `parent_id`, `name`, `code`, `sort`, `leader`, `phone`, `email`, `status`, `tree_path`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES
	('100', '1', '0', '党委办公室、校长办公室', 'DANGXIAO_BAN', 10, '张教授', '010-62332236', 'dangxiaoban@newease.cloud', 'enabled', '0,100', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('101', '1', '0', '组织部（党校）', 'ZUZHI_BU', 20, '孙部长', '010-62332345', 'zzb@newease.cloud', 'enabled', '0,101', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('102', '1', '0', '宣传部（新闻中心）', 'XUANCHUAN_BU', 30, '李部长', '010-62332245', 'xcb@newease.cloud', 'enabled', '0,102', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('103', '1', '0', '统战部', 'TONGZHAN_BU', 40, '王部长', '010-62333686', 'tzb@newease.cloud', 'enabled', '0,103', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('104', '1', '0', '纪委办公室（监察室）', 'JIWEI_BAN', 50, '刘书记', '010-62332277', 'jiwei@newease.cloud', 'enabled', '0,104', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('105', '1', '0', '巡察工作办公室', 'XUNCHA_BAN', 60, '赵主任', '010-62334663', 'xuncha@newease.cloud', 'enabled', '0,105', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('106', '1', '0', '工会', 'GONG_HUI', 70, '陈主席', '010-62332891', 'gonghui@newease.cloud', 'enabled', '0,106', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('107', '1', '0', '团委', 'TUAN_WEI', 80, '王书记', '010-62332293', 'tw@newease.cloud', 'enabled', '0,107', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('108', '1', '0', '人事处', 'RENSHI_CHU', 90, '赵处长', '010-62332389', 'rsc@newease.cloud', 'enabled', '0,108', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('1081', '1', '108', '师资科', 'SHIZI_KE', 100, '张科长', '010-62332390', 'shizi@newease.cloud', 'enabled', '0,108,1081', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('1082', '1', '108', '人事科', 'RENSHI_KE', 110, '李科长', '010-62332389', 'renshike@newease.cloud', 'enabled', '0,108,1082', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('1083', '1', '108', '薪酬福利科', 'XINCHOU_KE', 120, '王科长', '010-62332788', 'xinchou@newease.cloud', 'enabled', '0,108,1083', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('1084', '1', '108', '人才发展中心', 'RENCAI_ZX', 130, '陈主任', '010-62334897', 'rencai@newease.cloud', 'enabled', '0,108,1084', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('109', '1', '0', '教务处', 'JIAOWU_CHU', 140, '刘处长', '010-62332438', 'jwc@newease.cloud', 'enabled', '0,109', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('1091', '1', '109', '教学运行科', 'JIAOXUE_YUNXING', 150, '周科长', '010-62332397', 'yxk@newease.cloud', 'enabled', '0,109,1091', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('1092', '1', '109', '教学研究科', 'JIAOXUE_YANJIU', 160, '吴科长', '010-62332395', 'jxyj@newease.cloud', 'enabled', '0,109,1092', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('1093', '1', '109', '学籍管理科', 'XUEJI_KE', 170, '郑科长', '010-62332404', 'xjgl@newease.cloud', 'enabled', '0,109,1093', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('110', '1', '0', '科研院（科学研究与发展部）', 'KEYAN_YUAN', 180, '陈处长', '010-62332414', 'kyy@newease.cloud', 'enabled', '0,110', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('1101', '1', '110', '基础研究科', 'JICHU_YANJIU', 190, '刘科长', '010-62332414', 'jcyjk@newease.cloud', 'enabled', '0,110,1101', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('1102', '1', '110', '高新技术科', 'GAOXIN_JISHU', 200, '黄科长', '010-62332414', 'gxjs@newease.cloud', 'enabled', '0,110,1102', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('111', '1', '0', '研究生院', 'YANJIUSHENG_YUAN', 210, '张院长', '010-62332391', 'yjsy@newease.cloud', 'enabled', '0,111', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('112', '1', '0', '学生工作部（处）', 'XUESHENG_CHU', 220, '秦处长', '010-62332648', 'xsc@newease.cloud', 'enabled', '0,112', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('113', '1', '0', '招生就业处', 'ZHAOSHENG_CHU', 230, '徐处长', '010-62332387', 'zsjyc@newease.cloud', 'enabled', '0,113', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('114', '1', '0', '财务处', 'CAIWU_CHU', 240, '周处长', '010-62332328', 'cwc@newease.cloud', 'enabled', '0,114', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('115', '1', '0', '审计处', 'SHENJI_CHU', 250, '马处长', '010-62332238', 'sjc@newease.cloud', 'enabled', '0,115', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('116', '1', '0', '国际合作与交流处（港澳台办）', 'GUOJI_CHU', 260, '冯处长', '010-62332497', 'studyabroad@newease.cloud', 'enabled', '0,116', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('117', '1', '0', '后勤管理处', 'HOUQIN_CHU', 270, '高处长', '010-62332336', 'hqc@newease.cloud', 'enabled', '0,117', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('118', '1', '0', '保卫处（部）', 'BAOWEI_CHU', 280, '张处长', '010-62332358', 'bwc@newease.cloud', 'enabled', '0,118', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('119', '1', '0', '离退休职工工作处', 'LITUIXIU_CHU', 290, '乔处长', '010-62332597', 'ltx@newease.cloud', 'enabled', '0,119', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('120', '1', '0', '信息化建设与管理办公室', 'XINXIHUA_BAN', 300, '杨主任', '010-62332243', 'xxh@newease.cloud', 'enabled', '0,120', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('1201', '1', '120', '网络运维科', 'WANGLAO_KE', 310, '孙科长', '010-62332243', 'wangluo@newease.cloud', 'enabled', '0,120,1201', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('1202', '1', '120', '应用开发科', 'YINGYONG_KE', 320, '李科长', '010-62332244', 'appdev@newease.cloud', 'enabled', '0,120,1202', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('121', '1', '0', '档案馆', 'DANGANGUAN', 330, '赵馆长', '010-62332396', 'dag@newease.cloud', 'enabled', '0,121', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('122', '1', '0', '校医院', 'XIAO_YIYUAN', 340, '徐院长', '010-62332373', 'xyy@newease.cloud', 'enabled', '0,122', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('123', '1', '0', '期刊中心', 'QIKAN_ZX', 350, '黄主任', '010-62332459', 'qikan@newease.cloud', 'enabled', '0,123', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('124', '1', '0', '创新创业中心', 'CHUANGXIN_ZX', 360, '何主任', '010-62333787', 'cxcy@newease.cloud', 'enabled', '0,124', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('125', '1', '0', '材料科学与工程学院', 'CL_XY', 370, '廖院长', '010-62332378', 'clxy@newease.cloud', 'enabled', '0,125', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('126', '1', '0', '冶金与生态工程学院', 'YJ_XY', 380, '焦院长', '010-62332388', 'yjxy@newease.cloud', 'enabled', '0,126', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('127', '1', '0', '土木与资源工程学院', 'TM_XY', 390, '苗院长', '010-62332368', 'tmxy@newease.cloud', 'enabled', '0,127', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('128', '1', '0', '机械工程学院', 'JX_XY', 400, '马院长', '010-62332348', 'jxxy@newease.cloud', 'enabled', '0,128', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('129', '1', '0', '能源与环境工程学院', 'NH_XY', 410, '邢院长', '010-62332366', 'nhxy@newease.cloud', 'enabled', '0,129', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('130', '1', '0', '自动化学院', 'ZDH_XY', 420, '张院长', '010-62332392', 'zdhxy@newease.cloud', 'enabled', '0,130', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('131', '1', '0', '计算机与通信工程学院', 'JT_XY', 430, '殷院长', '010-62332383', 'jtxy@newease.cloud', 'enabled', '0,131', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('132', '1', '0', '数理学院', 'SL_XY', 440, '庞院长', '010-62332376', 'slxy@newease.cloud', 'enabled', '0,132', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('133', '1', '0', '化学与生物工程学院', 'HS_XY', 450, '李院长', '010-62332375', 'hsxy@newease.cloud', 'enabled', '0,133', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('134', '1', '0', '经济管理学院', 'JG_XY', 460, '谷院长', '010-62332393', 'jgxy@newease.cloud', 'enabled', '0,134', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('135', '1', '0', '文法学院', 'WF_XY', 470, '魏院长', '010-62332385', 'wfxy@newease.cloud', 'enabled', '0,135', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('136', '1', '0', '马克思主义学院', 'MKS_XY', 480, '宋院长', '010-62332386', 'mksxy@newease.cloud', 'enabled', '0,136', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('137', '1', '0', '外国语学院', 'WGY_XY', 490, '张院长', '010-62332382', 'wgyxy@newease.cloud', 'enabled', '0,137', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('138', '1', '0', '高等工程师学院', 'GJ_XY', 500, '张院长', '010-62332388', 'gjxy@newease.cloud', 'enabled', '0,138', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('139', '1', '0', '体育部', 'TIYU_BU', 510, '王主任', '010-62332374', 'tyb@newease.cloud', 'enabled', '0,139', 0, 'admin', NOW(), 'admin', NOW(), 0),
	('140', '1', '0', '国际学生中心', 'GUOJI_XUESHENG', 520, '赵主任', '010-62332497', 'lxg@newease.cloud', 'enabled', '0,140', 0, 'admin', NOW(), 'admin', NOW(), 0);



-- ================================================================
-- 岗位
-- ================================================================

INSERT INTO sys_post (id, tenant_id, name, code, sort, remark, version, create_by, create_time, update_by, update_time, is_deleted) VALUES
	('1', '1', '处长', 'CHUZHANG', 10, NULL, 0, 'admin', NOW(), 'admin', NOW(), 0),
	('2', '1', '主任', 'ZHUREN', 20, NULL, 0, 'admin', NOW(), 'admin', NOW(), 0),
	('3', '1', '科长', 'KEZHANG', 30, NULL, 0, 'admin', NOW(), 'admin', NOW(), 0),
	('4', '1', '科员', 'KEYUAN', 40, NULL, 0, 'admin', NOW(), 'admin', NOW(), 0);


-- ================================================================
-- 角色
-- ================================================================




-- ================================================================
-- 用户
-- ================================================================




-- ================================================================
-- todo
-- ================================================================





-- 打开外键检查
SET FOREIGN_KEY_CHECKS = 1;
