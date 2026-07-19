-- 设置字符集
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- 关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;


-- ================================================================
-- 租户
-- ================================================================

INSERT INTO sys_tenant_package (id,name,code,status,user_limit,api_limit,storage_limit,expire_days,sort,remark,version,create_by,create_time,update_by,update_time,is_deleted) VALUES
	 ('019f78ea8c99777c8447223e72447f01','默认套餐','default','enabled',-1,-1,-1,-1,100,NULL,0,'1',now(),'1',now(),0),
	 ('019f78eb189c787a94583b24bb22f7f3','基础套餐','base','enabled',20,1000,10,180,100,NULL,1,'1',now(),'1',now(),0),
	 ('019f78ec00ff7f12ad6712b028778f14','迷你套餐','mini','enabled',10,500,5,180,100,NULL,0,'1',now(),'1',now(),0),
	 ('019f78ecfc057dce9eab9d3f2ddaf3f3','测试套餐（已停用）','test','disabled',-1,-1,-1,-1,100,NULL,0,'1',now(),'1',now(),0);

INSERT INTO sys_tenant (id,name,code,package_id,contact_name,contact_phone,contact_email,domain_name,effective_time,expire_time,status,config_json,remark,version,create_by,create_time,update_by,update_time,is_deleted) VALUES
	 ('019f78f2f934710e829a7f53e2f6d77d','北京科技大学','ustb','019f78ea8c99777c8447223e72447f01','李白','13656780987','libai@example.com','https://www.ustb.edu.cn',NULL,NULL,'normal',NULL,NULL,0,'1',now(),'1',now(),0);


-- ================================================================
-- 部门
-- ================================================================




-- ================================================================
-- 岗位
-- ================================================================




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
