-- ================================================================
-- SCA Platform - 清空所有业务数据
-- 注意：此脚本会清空所有业务表数据，请谨慎使用
-- ================================================================

-- 关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 清空用户与权限相关表
TRUNCATE TABLE `sys_user_role`;
TRUNCATE TABLE `sys_role_permission`;
TRUNCATE TABLE `sys_role_data_scope`;
TRUNCATE TABLE `sys_user_dept`;
TRUNCATE TABLE `sys_user_post`;
TRUNCATE TABLE `sys_user_password_history`;
TRUNCATE TABLE `sys_permission`;
TRUNCATE TABLE `sys_role`;
TRUNCATE TABLE `sys_user`;

-- 清空组织架构相关表
TRUNCATE TABLE `sys_dept`;
TRUNCATE TABLE `sys_post`;

-- 清空租户相关表
TRUNCATE TABLE `sys_tenant_package_permission`;
TRUNCATE TABLE `sys_tenant_package`;
TRUNCATE TABLE `sys_tenant`;

-- 清空系统配置相关表
TRUNCATE TABLE `sys_recycle_bin`;
TRUNCATE TABLE `sys_dict_data`;
TRUNCATE TABLE `sys_dict`;
TRUNCATE TABLE `sys_config`;
TRUNCATE TABLE `sys_release`;

-- 清空日志表
TRUNCATE TABLE `sys_login_log`;
TRUNCATE TABLE `sys_operation_log`;

-- 清空 OAuth2 相关表
TRUNCATE TABLE `oauth2_authorization_consent`;
TRUNCATE TABLE `oauth2_authorization`;
TRUNCATE TABLE `oauth2_registered_client`;

-- 打开外键检查
SET FOREIGN_KEY_CHECKS = 1;
