-- ================================================================
-- SCA Platform - 清空所有业务数据
-- 注意：此脚本会清空所有业务表数据，请谨慎使用
-- ================================================================

-- 关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 清空用户与权限相关表
DELETE FROM `sys_user_role`;
DELETE FROM `sys_role_permission`;
DELETE FROM `sys_role_data_scope`;
DELETE FROM `sys_user_dept`;
DELETE FROM `sys_user_post`;
DELETE FROM `sys_user_password_history`;
DELETE FROM `sys_permission`;
DELETE FROM `sys_role`;
DELETE FROM `sys_user`;

-- 清空组织架构相关表
DELETE FROM `sys_dept`;
DELETE FROM `sys_post`;

-- 清空租户相关表
DELETE FROM `sys_tenant_package_permission`;
DELETE FROM `sys_tenant_package`;
DELETE FROM `sys_tenant`;

-- 清空系统配置相关表
DELETE FROM `sys_recycle_bin`;
DELETE FROM `sys_dict_data`;
DELETE FROM `sys_dict`;
DELETE FROM `sys_config`;
DELETE FROM `sys_release`;

-- 清空日志表
DELETE FROM `sys_login_log`;
DELETE FROM `sys_operation_log`;

-- 清空 OAuth2 相关表
DELETE FROM `oauth2_authorization_consent`;
DELETE FROM `oauth2_authorization`;
DELETE FROM `oauth2_registered_client`;

-- 打开外键检查
SET FOREIGN_KEY_CHECKS = 1;
