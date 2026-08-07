-- 关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- ================================================================
-- 清空 sca_platform 库（平台库）
-- ================================================================

-- 清空用户与权限相关表
DELETE FROM `sca_platform`.`sys_user_role`;
DELETE FROM `sca_platform`.`sys_role_permission`;
DELETE FROM `sca_platform`.`sys_role_data_scope`;
DELETE FROM `sca_platform`.`sys_user_dept`;
DELETE FROM `sca_platform`.`sys_user_post`;
DELETE FROM `sca_platform`.`sys_user_password_history`;
DELETE FROM `sca_platform`.`sys_permission`;
DELETE FROM `sca_platform`.`sys_role`;
DELETE FROM `sca_platform`.`sys_user`;

-- 清空组织架构相关表
DELETE FROM `sca_platform`.`sys_dept`;
DELETE FROM `sca_platform`.`sys_post`;

-- 清空租户相关表
DELETE FROM `sca_platform`.`sys_tenant_package_permission`;
DELETE FROM `sca_platform`.`sys_tenant_package`;
DELETE FROM `sca_platform`.`sys_tenant`;

-- 清空系统配置相关表
DELETE FROM `sca_platform`.`sys_recycle_bin`;
DELETE FROM `sca_platform`.`sys_dict_data`;
DELETE FROM `sca_platform`.`sys_dict`;
DELETE FROM `sca_platform`.`sys_config`;
DELETE FROM `sca_platform`.`sys_release`;

-- 清空日志表
-- DELETE FROM `sca_platform`.`sys_login_log`;
-- DELETE FROM `sca_platform`.`sys_operation_log`;

-- 清空 OAuth2 相关表
DELETE FROM `sca_platform`.`oauth2_authorization_consent`;
DELETE FROM `sca_platform`.`oauth2_authorization`;
DELETE FROM `sca_platform`.`oauth2_registered_client`;

-- ================================================================
-- 清空 sca_datasource 库（数据源管理库）
-- ================================================================

-- 清空数据源相关表
-- DELETE FROM `sca_datasource`.`ds_sql_template`;
-- DELETE FROM `sca_datasource`.`ds_query_log`;
-- DELETE FROM `sca_datasource`.`ds_datasource`;
-- DELETE FROM `sca_datasource`.`ds_driver_file`;
-- DELETE FROM `sca_datasource`.`ds_driver`;

-- 打开外键检查
SET FOREIGN_KEY_CHECKS = 1;
