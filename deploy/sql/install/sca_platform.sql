-- 创建数据库
CREATE DATABASE IF NOT EXISTS sca_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 使用数据库
USE sca_platform;

-- 设置连接字符集
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- 关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;



-- ---------------------------------------------------
-- 租户套餐表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_tenant_package` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `name`            VARCHAR(100)    NOT NULL                    COMMENT '套餐名称',
    `code`            VARCHAR(64)     NOT NULL                    COMMENT '套餐编码',
    `status`          VARCHAR(10)     NOT NULL                    COMMENT '套餐状态（enabled 启用，disabled 禁用）',
    `user_limit`      INT             DEFAULT -1                  COMMENT '用户数限制',
    `api_limit`       INT             DEFAULT -1                  COMMENT 'API调用限制/日',
    `storage_limit`   INT             DEFAULT -1                  COMMENT '存储限制(GB)',
    `expire_days`     INT             DEFAULT -1                  COMMENT '有效期天数',
    `sort`            INT             NOT NULL DEFAULT 100        COMMENT '排序，数字越小越靠前',
    `remark`          TEXT            DEFAULT NULL                COMMENT '备注',
    `version`         INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户套餐表';


-- 内置租户套餐（默认套餐，超级管理员 admin 所属租户的套餐）
INSERT INTO `sys_tenant_package` (`id`, `name`, `code`, `status`, `user_limit`, `api_limit`, `storage_limit`, `expire_days`, `sort`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES
	('1', '默认套餐', 'default', 'enabled', -1, -1, -1, -1, 1, '系统内置默认套餐', 0, 'system', NOW(), 'system', NOW(), 0);


-- ---------------------------------------------------
-- 租户套餐权限关联表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_tenant_package_permission` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `package_id`      VARCHAR(64)     NOT NULL                    COMMENT '套餐ID',
    `permission_id`   VARCHAR(64)     NOT NULL                    COMMENT '权限ID',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户套餐权限关联表';


-- ---------------------------------------------------
-- 租户表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_tenant` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `name`            VARCHAR(255)    NOT NULL                    COMMENT '租户名称',
    `code`            VARCHAR(64)     NOT NULL                    COMMENT '租户编码',
    `package_id`      VARCHAR(64)     NOT NULL                    COMMENT '套餐ID',
    `contact_name`    VARCHAR(255)    DEFAULT NULL                COMMENT '联系人姓名',
    `contact_phone`   VARCHAR(255)    DEFAULT NULL                COMMENT '联系人电话',
    `contact_email`   VARCHAR(255)    DEFAULT NULL                COMMENT '联系人邮箱',
    `domain_name`     VARCHAR(255)    DEFAULT NULL                COMMENT '绑定独立域名',
    `effective_time`  DATETIME        DEFAULT NULL                COMMENT '生效时间（NULL表示立即生效）',
    `expire_time`     DATETIME        DEFAULT NULL                COMMENT '过期时间（NULL表示永不过期）',
    `status`          VARCHAR(20)     NOT NULL                    COMMENT '租户状态（normal 正常，disabled 禁用，expired 过期，cancelled 注销）',
    `is_builtin`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否内置（0否 1是）',
    `config_json`     JSON            DEFAULT NULL                COMMENT '租户个性化配置（Logo、主题、策略等）',
    `remark`          TEXT            DEFAULT NULL                COMMENT '备注',
    `version`         INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';


-- 内置租户（默认租户，超级管理员 admin 所属租户）
INSERT INTO `sys_tenant` (`id`, `name`, `code`, `package_id`, `contact_name`, `contact_phone`, `contact_email`, `domain_name`, `effective_time`, `expire_time`, `status`, `is_builtin`, `config_json`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES
	('1', '默认租户', 'default', '1', NULL, NULL, NULL, NULL, NULL, NULL, 'normal', 1, NULL, '系统内置默认租户', 0, 'system', NOW(), 'system', NOW(), 0);


-- ---------------------------------------------------
-- 部门表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dept` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `parent_id`       VARCHAR(64)     NOT NULL DEFAULT '0'        COMMENT '上级部门ID',
    `name`            VARCHAR(255)    NOT NULL                    COMMENT '部门名称',
    `code`            VARCHAR(255)    NOT NULL                    COMMENT '部门编码',
    `sort`            INT             NOT NULL DEFAULT 100        COMMENT '排序，数字越小越靠前',
    `leader`          VARCHAR(255)    DEFAULT NULL                COMMENT '负责人',
    `phone`           VARCHAR(255)    DEFAULT NULL                COMMENT '联系电话',
    `email`           VARCHAR(255)    DEFAULT NULL                COMMENT '邮箱',
    `status`          VARCHAR(10)     NOT NULL                    COMMENT '状态（enabled 启用，disabled 停用）',
    `tree_path`       VARCHAR(500)    NOT NULL                    COMMENT 'ID层级路径，逗号分隔（如: 0,100,1001）',
    `version`         INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';


-- ---------------------------------------------------
-- 岗位表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_post` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `name`            VARCHAR(100)    NOT NULL                    COMMENT '岗位名称',
    `code`            VARCHAR(64)     NOT NULL                    COMMENT '岗位编码',
    `sort`            INT             NOT NULL DEFAULT 100        COMMENT '排序，数字越小越靠前',
    `remark`          TEXT            DEFAULT NULL                COMMENT '备注',
    `version`         INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';


-- ---------------------------------------------------
-- 用户表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`                      VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`               VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `username`                VARCHAR(50)     NOT NULL                    COMMENT '用户名',
    `password`                VARCHAR(128)    DEFAULT NULL                COMMENT '密码',
    `nickname`                VARCHAR(50)     DEFAULT NULL                COMMENT '昵称',
    `real_name`               VARCHAR(50)     DEFAULT NULL                COMMENT '真实姓名',
    `gender`                  VARCHAR(10)     DEFAULT NULL                COMMENT '性别（male 男，female 女）',
    `avatar`                  VARCHAR(500)    DEFAULT NULL                COMMENT '头像地址',
    `phone`                   VARCHAR(20)     DEFAULT NULL                COMMENT '手机号',
    `email`                   VARCHAR(100)    DEFAULT NULL                COMMENT '邮箱',
    `realm`                   VARCHAR(20)     NOT NULL                    COMMENT '用户域（admin：后台用户；portal：前台用户）',
    `user_type`               VARCHAR(20)     NOT NULL                    COMMENT '用户类型（SUPER_ADMIN 超级管理员，TENANT_ADMIN 租户管理员，DEPT_ADMIN 部门管理员，NORMAL 普通用户）',
    `status`                  VARCHAR(10)     NOT NULL                    COMMENT '状态（active 正常，inactive 未激活，locked 锁定，frozen 冻结，expired 过期，disabled 禁用，cancelled 注销）',
    `status_time`             DATETIME        DEFAULT NULL                COMMENT '状态变更时间（锁定、冻结、过期、禁用、注销时间）',
    `status_reason`           VARCHAR(255)    DEFAULT NULL                COMMENT '状态变更原因（锁定、冻结、过期、禁用、注销原因）',
    `login_fail_count`        INT             NOT NULL DEFAULT 0          COMMENT '连续登录失败次数',
    `must_change_password`    TINYINT(1)      NOT NULL DEFAULT 1          COMMENT '是否必须修改密码（0否 1是，新用户默认1）',
    `password_update_time`    DATETIME        DEFAULT NULL                COMMENT '密码最后修改时间',
    `effective_start_time`    DATETIME        DEFAULT NULL                COMMENT '账号有效期起始时间（NULL表示立即生效）',
    `effective_end_time`      DATETIME        DEFAULT NULL                COMMENT '账号有效期结束时间（NULL表示永不过期）',
    `last_login_ip`           VARCHAR(128)    DEFAULT NULL                COMMENT '最后登录IP',
    `last_login_time`         DATETIME        DEFAULT NULL                COMMENT '最后登录时间',
    `is_builtin`              TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否内置（0否 1是）',
    `source_type`             VARCHAR(50)     DEFAULT NULL                COMMENT '数据来源类型（initial 初始化，manual 手动创建，import 数据导入，sync 数据同步，sso 统一认证）',
    `remark`                  TEXT            DEFAULT NULL                COMMENT '备注',
    `version`                 INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`               VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`             DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`               VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`             DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`              TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';


-- 内置超级管理员用户（密码：admin@123）
INSERT INTO `sys_user` (`id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `gender`, `avatar`, `phone`, `email`, `realm`, `user_type`, `status`, `status_time`, `status_reason`, `login_fail_count`, `must_change_password`, `password_update_time`, `effective_start_time`, `effective_end_time`, `last_login_ip`, `last_login_time`, `is_builtin`, `source_type`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES
	('1', '1', 'admin', '{bcrypt}$2b$10$oW8PgdSN8jCUwZoApsRpc.8xhcCNInM8i0iH/6k.G7cmKB/5tb.Pq', '超级管理员', '超级管理员', NULL, NULL, NULL, NULL, 'admin', 'SUPER_ADMIN', 'active', NULL, NULL, 0, 0, NOW(), NOW(), NULL, NULL, NULL, 1, 'initial', '系统内置管理员账号', 0, 'system', NOW(), 'system', NOW(), 0);


-- ---------------------------------------------------
-- 用户部门关联表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_dept` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID',
    `tenant_id`       VARCHAR(64)     NOT NULL                    COMMENT '租户ID',
    `user_id`         VARCHAR(64)     NOT NULL                    COMMENT '用户ID',
    `dept_id`         VARCHAR(64)     NOT NULL                    COMMENT '部门ID',
    `is_primary`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否主部门（0否 1是）',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_dept` (`user_id`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户部门关联表';


-- ---------------------------------------------------
-- 用户岗位关联表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_post` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `user_id`         VARCHAR(64)     NOT NULL                    COMMENT '用户ID',
    `post_id`         VARCHAR(64)     NOT NULL                    COMMENT '岗位ID',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户岗位关联表';


-- ---------------------------------------------------
-- 用户密码历史记录表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_password_history` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `user_id`         VARCHAR(64)     NOT NULL                    COMMENT '用户ID',
    `password`        VARCHAR(128)    NOT NULL                    COMMENT '历史密码（加密存储）',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户密码历史记录表';


-- ---------------------------------------------------
-- 角色表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `name`            VARCHAR(100)    NOT NULL                    COMMENT '角色名称',
    `code`            VARCHAR(100)    NOT NULL                    COMMENT '角色编码',
    `data_scope`      VARCHAR(20)     NOT NULL                    COMMENT '数据权限范围（all 当前租户全部数据，dept_and_sub 本部门及以下，dept 仅本部门，personal 仅本人，custom 自定义）',
    `realm`           VARCHAR(20)     NOT NULL                    COMMENT '角色域（admin：后台角色；portal：前台角色）',
    `is_builtin`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否内置（0否 1是）',
    `sort`            INT             NOT NULL DEFAULT 100        COMMENT '排序，数字越小越靠前',
    `remark`          TEXT            DEFAULT NULL                COMMENT '备注',
    `version`         INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';


-- ---------------------------------------------------
-- 角色部门关联表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role_dept` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `role_id`         VARCHAR(64)     NOT NULL                    COMMENT '角色ID',
    `dept_id`         VARCHAR(64)     NOT NULL                    COMMENT '角色可访问的部门ID',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色部门关联表（指定角色可以访问哪些部门的数据）';


-- ---------------------------------------------------
-- 用户角色关联表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `user_id`         VARCHAR(64)     NOT NULL                    COMMENT '用户ID',
    `role_id`         VARCHAR(64)     NOT NULL                    COMMENT '角色ID',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';



-- ---------------------------------------------------
-- 权限表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `parent_id`       VARCHAR(64)     NOT NULL DEFAULT '0'        COMMENT '父权限ID',
    `name`            VARCHAR(255)    NOT NULL                    COMMENT '权限名称',
    `name_en`         VARCHAR(255)    DEFAULT NULL                COMMENT '英文权限名称，用于国际化',
    `type`            VARCHAR(10)     NOT NULL                    COMMENT '权限类型（module模块 folder目录 menu菜单 button按钮）',
    `code`            VARCHAR(255)    DEFAULT NULL                COMMENT '权限编码（如: system:user:list）',
    `path`            VARCHAR(255)    DEFAULT NULL                COMMENT '路由地址',
    `component`       VARCHAR(255)    DEFAULT NULL                COMMENT '前端组件路径',
    `icon`            VARCHAR(255)    DEFAULT NULL                COMMENT '图标',
    `sort`            INT             NOT NULL DEFAULT 100        COMMENT '排序，数字越小越靠前',
    `is_visible`      TINYINT(1)      NOT NULL DEFAULT 1          COMMENT '是否可见（0否 1是）',
    `is_external`     TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否外链（0否 1是）',
    `status`          VARCHAR(10)     NOT NULL                    COMMENT '状态（enabled正常 disabled停用）',
    `tree_path`       VARCHAR(500)    NOT NULL                    COMMENT 'ID层级路径，逗号分隔（如: 0,100,1001）',
    `realm`           VARCHAR(20)     NOT NULL                    COMMENT '权限域（admin：后台权限；portal：前台权限）',
    `remark`          TEXT            DEFAULT NULL                COMMENT '备注',
    `version`         INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `name_en`, `type`, `code`, `path`, `component`, `icon`, `sort`, `is_visible`, `is_external`, `status`, `tree_path`, `realm`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES
	('1000', '0', '数据源管理', 'Data Source', 'module', NULL, NULL, NULL, 'sym_r_database', 10, 1, 0, 'enabled', '0,1000', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('1100', '1000', '数据源管理', 'Data Sources', 'menu', 'sys:datasource:list', '/datasource/list', 'DatasourceListView', 'sym_r_nest_eco_leaf', 1010, 1, 0, 'enabled', '0,1000,1100', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('1200', '1000', '数据查询', 'SQL Query', 'menu', 'sys:datasource:sql-query', '/datasource/sql-query', 'SqlQueryView', 'sym_r_nest_eco_leaf', 1011, 1, 0, 'enabled', '0,1000,1200', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('1300', '1000', '驱动管理', 'Drivers', 'menu', 'sys:datasource:driver:list', '/datasource/driver-list', 'DriverListView', 'sym_r_nest_eco_leaf', 1012, 1, 0, 'enabled', '0,1000,1300', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0);

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `name_en`, `type`, `code`, `path`, `component`, `icon`, `sort`, `is_visible`, `is_external`, `status`, `tree_path`, `realm`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES
	('9999', '0', '系统管理', 'System', 'module', NULL, NULL, NULL, 'sym_r_settings', 99, 1, 0, 'enabled', '0,9999', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('9910', '9999', '租户管理', 'Tenant Management', 'folder', NULL, NULL, NULL, 'sym_r_folder', 9910, 1, 0, 'enabled', '0,9999,9910', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('9911', '9910', '租户管理', 'Tenants', 'menu', 'sys:tenant:list', '/system/tenant', 'TenantListView', 'sym_r_nest_eco_leaf', 99101, 1, 0, 'enabled', '0,9999,9910,9911', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('9912', '9910', '套餐管理', 'Packages', 'menu', 'sys:tenant-package:list', '/system/tenant-package', 'TenantPackageListView', 'sym_r_nest_eco_leaf', 99102, 1, 0, 'enabled', '0,9999,9910,9912', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('9921', '9999', '用户管理', 'Users', 'menu', 'sys:user:list', '/system/user', 'UserListView', 'sym_r_nest_eco_leaf', 9911, 1, 0, 'enabled', '0,9999,9921', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('9922', '9999', '角色管理', 'Roles', 'menu', 'sys:role:list', '/system/role', 'RoleListView', 'sym_r_nest_eco_leaf', 9912, 1, 0, 'enabled', '0,9999,9922', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('9923', '9999', '权限管理', 'Permissions', 'menu', 'sys:permission:list', '/system/permission', 'PermissionListView', 'sym_r_nest_eco_leaf', 9913, 1, 0, 'enabled', '0,9999,9923', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('9924', '9999', '部门管理', 'Departments', 'menu', 'sys:dept:list', '/system/dept', 'DeptListView', 'sym_r_nest_eco_leaf', 9914, 1, 0, 'enabled', '0,9999,9924', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('9925', '9999', '岗位管理', 'Positions', 'menu', 'sys:post:list', '/system/post', 'PostListView', 'sym_r_nest_eco_leaf', 9915, 1, 0, 'enabled', '0,9999,9925', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('9926', '9999', '字典管理', 'Dictionaries', 'menu', 'sys:dict:list', '/system/dict', 'DictListView', 'sym_r_nest_eco_leaf', 9916, 1, 0, 'enabled', '0,9999,9926', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('9927', '9999', '系统配置', 'System Config', 'menu', 'sys:config:list', '/system/config', 'ConfigListView', 'sym_r_nest_eco_leaf', 9917, 1, 0, 'enabled', '0,9999,9927', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('9928', '9999', '通知公告', 'Announcements', 'menu', 'sys:notice:list', '/system/notice', 'NoticeListView', 'sym_r_nest_eco_leaf', 9918, 1, 0, 'enabled', '0,9999,9928', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('9929', '9999', '操作日志', 'Operation Logs', 'menu', 'sys:operation-log:list', '/system/operation-log', 'OperationLogListView', 'sym_r_nest_eco_leaf', 9919, 1, 0, 'enabled', '0,9999,9929', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('9930', '9999', '登录日志', 'Login Logs', 'menu', 'sys:login-log:list', '/system/login-log', 'LoginLogListView', 'sym_r_nest_eco_leaf', 9920, 1, 0, 'enabled', '0,9999,9930', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0);

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `name_en`, `type`, `code`, `path`, `component`, `icon`, `sort`, `is_visible`, `is_external`, `status`, `tree_path`, `realm`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES
	('1', '0', '首页', 'Home', 'menu', 'portal:home:view', '/portal/home', 'HomeView', 'sym_r_home', 1, 1, 0, 'enabled', '0,1', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('2', '0', '数据目录', 'Data Catalog', 'folder', NULL, NULL, NULL, 'sym_r_folder', 2, 1, 0, 'enabled', '0,2', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('21', '2', '数据地图', 'Data Map', 'menu', 'portal:data-map:view', '/portal/data/map', 'DataMapView', 'sym_r_map', 21, 1, 0, 'enabled', '0,2,21', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('22', '2', '资源目录', 'Resource Catalog', 'menu', 'portal:data-resource:list', '/portal/data/resources', 'ResourceCatalogView', 'sym_r_table', 22, 1, 0, 'enabled', '0,2,22', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('23', '2', '数据标准', 'Data Standards', 'menu', 'portal:data-standard:list', '/portal/data/standards', 'DataStandardView', 'sym_r_checklist', 23, 1, 0, 'enabled', '0,2,23', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('3', '0', '数据服务', 'Data Services', 'folder', NULL, NULL, NULL, 'sym_r_api', 3, 1, 0, 'enabled', '0,3', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('31', '3', '智能问数', 'AI Query', 'menu', 'portal:ai-query:view', '/portal/service/ai-query', 'AiQueryView', 'sym_r_smart_toy', 31, 1, 0, 'enabled', '0,3,31', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('32', '3', '数据集市', 'Data Marketplace', 'menu', 'portal:data-market:list', '/portal/service/data-market', 'DataMarketView', 'sym_r_store', 32, 1, 0, 'enabled', '0,3,32', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('33', '3', '数据填报', 'Data Submit', 'menu', 'portal:data-submit:view', '/portal/service/data-submit', 'DataSubmitView', 'sym_r_edit_note', 33, 1, 0, 'enabled', '0,3,33', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('4', '0', '个人中心', 'Profile', 'folder', NULL, NULL, NULL, 'sym_r_account_circle', 4, 1, 0, 'enabled', '0,4', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('41', '4', '个人信息', 'Personal Info', 'menu', 'portal:profile:view', '/portal/profile', 'ProfileView', 'sym_r_person', 41, 1, 0, 'enabled', '0,4,41', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('42', '4', '我的申请', 'My Requests', 'menu', 'portal:request:list', '/portal/my/requests', 'MyRequestView', 'sym_r_description', 42, 1, 0, 'enabled', '0,4,42', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('43', '4', '我的下载', 'My Downloads', 'menu', 'portal:download:list', '/portal/my/downloads', 'MyDownloadView', 'sym_r_download', 43, 1, 0, 'enabled', '0,4,43', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('44', '4', '我的收藏', 'My Favorites', 'menu', 'portal:favorite:list', '/portal/my/favorites', 'MyFavoriteView', 'sym_r_star', 44, 1, 0, 'enabled', '0,4,44', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('45', '4', '消息通知', 'Notifications', 'menu', 'portal:notification:list', '/portal/profile/notifications', 'NotificationView', 'sym_r_notifications', 45, 1, 0, 'enabled', '0,4,45', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('46', '4', '应用接入', 'App Integration', 'menu', 'portal:app-integration:list', '/portal/profile/app-integrations', 'AppIntegrationView', 'sym_r_link', 46, 1, 0, 'enabled', '0,4,46', 'portal', NULL, 0, 'system', NOW(), 'system', NOW(), 0);


-- ---------------------------------------------------
-- 角色权限关联表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `role_id`         VARCHAR(64)     NOT NULL                    COMMENT '角色ID',
    `permission_id`   VARCHAR(64)     NOT NULL                    COMMENT '权限ID',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';


-- ---------------------------------------------------
-- 系统回收站表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_recycle_bin` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `module`          VARCHAR(255)    NOT NULL                    COMMENT '业务模块',
    `key`             VARCHAR(64)     NOT NULL                    COMMENT '业务记录的原始ID',
    `title`           VARCHAR(255)    NOT NULL                    COMMENT '业务标题',
    `content`         TEXT            DEFAULT NULL                COMMENT '业务数据快照',
    `version`         INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人ID',
    `create_time`     DATETIME        NOT NULL                    COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人ID',
    `update_time`     DATETIME        NOT NULL                    COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统回收站表';


-- ---------------------------------------------------
-- 字典表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dict` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `name`            VARCHAR(100)    NOT NULL                    COMMENT '字典名称',
    `code`            VARCHAR(100)    NOT NULL                    COMMENT '字典编码',
    `status`          VARCHAR(10)     NOT NULL                    COMMENT '状态（enabled 启用，disabled 禁用）',
    `is_builtin`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否内置（0否 1是）',
    `remark`          TEXT            DEFAULT NULL                COMMENT '备注',
    `version`         INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典表';


-- ---------------------------------------------------
-- 字典数据表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dict_data` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `dict_id`         VARCHAR(64)     NOT NULL                    COMMENT '字典ID',
    `label`           VARCHAR(100)    NOT NULL                    COMMENT '字典标签',
    `value`           VARCHAR(100)    NOT NULL                    COMMENT '字典值',
    `status`          VARCHAR(10)     NOT NULL                    COMMENT '状态（enabled 启用，disabled 禁用）',
    `sort`            INT             NOT NULL DEFAULT 100        COMMENT '排序，数字越小越靠前',
    `remark`          TEXT            DEFAULT NULL                COMMENT '备注',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';


-- ---------------------------------------------------
-- 系统配置表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_config` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `name`            VARCHAR(100)    NOT NULL                    COMMENT '配置名称',
    `key`             VARCHAR(100)    NOT NULL                    COMMENT '配置键',
    `value`           TEXT            DEFAULT NULL                COMMENT '配置值',
    `type`            VARCHAR(20)     DEFAULT 'string'            COMMENT '类型（string 字符串，number 数字，boolean 布尔值，datetime 日期时间，json JSON对象或数组）',
    `status`          VARCHAR(10)     NOT NULL                    COMMENT '状态（enabled 启用，disabled 禁用）',
    `is_builtin`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否内置（0否 1是）',
    `remark`          VARCHAR(500)    DEFAULT NULL                COMMENT '备注',
    `version`         INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';


-- ---------------------------------------------------
-- 登录日志表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_login_log` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `user_id`         VARCHAR(64)     DEFAULT NULL                COMMENT '用户ID',
    `username`        VARCHAR(64)     NOT NULL                    COMMENT '登录时输入的用户名（原始输入，无论用户是否存在都记录）',
    `client_ip`       VARCHAR(128)    DEFAULT NULL                COMMENT '客户端IP',
    `location`        VARCHAR(255)    DEFAULT NULL                COMMENT '登录位置',
    `device`          VARCHAR(100)    DEFAULT NULL                COMMENT '设备类型',
    `browser`         VARCHAR(100)    DEFAULT NULL                COMMENT '浏览器',
    `os`              VARCHAR(100)    DEFAULT NULL                COMMENT '操作系统',
    `is_success`      TINYINT(1)      NOT NULL DEFAULT 1          COMMENT '是否成功（0失败 1成功）',
    `error_message`   TEXT            DEFAULT NULL                COMMENT '异常信息',
    `cost_ms`         BIGINT          DEFAULT NULL                COMMENT '操作耗时（毫秒）',
    `login_time`      DATETIME        NOT NULL                    COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_login_time` (`login_time`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';


-- ---------------------------------------------------
-- 操作日志表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `trace_id`        VARCHAR(64)     DEFAULT NULL                COMMENT '链路追踪ID',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `user_id`         VARCHAR(64)     DEFAULT NULL                COMMENT '用户ID',
    `module`          VARCHAR(100)    DEFAULT NULL                COMMENT '操作模块',
    `action`          VARCHAR(100)    DEFAULT NULL                COMMENT '操作动作',
    `http_method`     VARCHAR(10)     DEFAULT NULL                COMMENT '请求方法（GET/POST等）',
    `request_uri`     VARCHAR(500)    DEFAULT NULL                COMMENT '请求路径',
    `class_name`      VARCHAR(255)    DEFAULT NULL                COMMENT '目标类全限定名',
    `method_name`     VARCHAR(100)    DEFAULT NULL                COMMENT '目标方法名',
    `request_args`    TEXT            DEFAULT NULL                COMMENT '请求参数（JSON）',
    `response_result` TEXT            DEFAULT NULL                COMMENT '响应结果（JSON）',
    `is_success`      TINYINT(1)      NOT NULL DEFAULT 1          COMMENT '是否成功（0失败 1成功）',
    `error_message`   TEXT            DEFAULT NULL                COMMENT '异常信息',
    `cost_ms`         BIGINT          DEFAULT NULL                COMMENT '操作耗时（毫秒）',
    `client_ip`       VARCHAR(128)    DEFAULT NULL                COMMENT '客户端IP',
    `location`        VARCHAR(255)    DEFAULT NULL                COMMENT '操作位置',
    `device`          VARCHAR(100)    DEFAULT NULL                COMMENT '设备类型',
    `browser`         VARCHAR(100)    DEFAULT NULL                COMMENT '浏览器',
    `os`              VARCHAR(100)    DEFAULT NULL                COMMENT '操作系统',
    `operation_time`  DATETIME        NOT NULL                    COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_operation_time` (`operation_time`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';


-- ---------------------------------------------------
-- OAuth2 注册客户端表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id                              varchar(100)    NOT NULL                           COMMENT '主键ID，唯一标识',
    client_id                       varchar(100)    NOT NULL                           COMMENT '客户端ID（对外暴露的唯一标识，相当于账号）',
    client_id_issued_at             timestamp       DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '客户端ID签发时间',
    client_secret                   varchar(200)    DEFAULT NULL                       COMMENT '客户端密钥（加密存储，用于客户端身份认证）',
    client_secret_expires_at        timestamp       DEFAULT NULL                       COMMENT '客户端密钥过期时间（NULL 表示永不过期）',
    client_name                     varchar(200)    NOT NULL                           COMMENT '客户端名称（便于管理和界面展示）',
    client_authentication_methods   varchar(1000)   NOT NULL                           COMMENT '客户端认证方式，多个以逗号分隔（如：client_secret_basic, client_secret_post, none 等）',
    authorization_grant_types       varchar(1000)   NOT NULL                           COMMENT '授权类型，多个以逗号分隔（如：authorization_code, refresh_token, client_credentials 等）',
    redirect_uris                   varchar(1000)   DEFAULT NULL                       COMMENT '授权码模式下允许的重定向 URI 列表，多个以逗号分隔',
    post_logout_redirect_uris       varchar(1000)   DEFAULT NULL                       COMMENT '注销（登出）后允许跳转的重定向 URI 列表，多个以逗号分隔',
    scopes                          varchar(1000)   NOT NULL                           COMMENT '客户端申请的权限范围，多个以逗号分隔（如：openid, profile, email 等）',
    client_settings                 varchar(2000)   NOT NULL                           COMMENT '客户端配置信息（JSON 格式，如：requireProofKey、requireAuthorizationConsent 等）',
    token_settings                  varchar(2000)   NOT NULL                           COMMENT '令牌配置信息（JSON 格式，如：access_token 存活时长、refresh_token 存活时长、签名算法等）',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2 注册客户端表：存储接入授权服务器的客户端应用配置信息';


-- 内置 OAuth2 客户端：管理后台（机密客户端 + 密码模式）
-- 注意：client_secret 在应用启动时由 OAuth2RegisteredClientInitializer 从配置中读取并加密写入
INSERT INTO `oauth2_registered_client` (id, client_id, client_id_issued_at, client_secret, client_secret_expires_at, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, post_logout_redirect_uris, scopes, client_settings, token_settings) VALUES
	('100', 'sca-admin-client', CURRENT_TIMESTAMP, NULL, NULL, 'SCA Admin SPA', 'client_secret_basic', 'password,refresh_token', NULL, NULL, 'profile,all', '{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2ClientSettings","settings.client.require-authorization-consent":false}', '{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenSettings","settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.core.OAuth2AccessToken$TokenFormat","value":"reference"},"settings.token.access-token-time-to-live":["java.time.Duration",900.000000000],"settings.token.refresh-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.reuse-refresh-tokens":false}');

-- 内置 OAuth2 客户端：前台门户（机密客户端 + 密码模式）
-- 注意：client_secret 在应用启动时由 OAuth2RegisteredClientInitializer 从配置中读取并加密写入
INSERT INTO `oauth2_registered_client` (id, client_id, client_id_issued_at, client_secret, client_secret_expires_at, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, post_logout_redirect_uris, scopes, client_settings, token_settings) VALUES
	('200', 'sca-portal-client', CURRENT_TIMESTAMP, NULL, NULL, 'SCA Portal SPA', 'client_secret_basic', 'password,refresh_token', NULL, NULL, 'profile,all', '{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2ClientSettings","settings.client.require-authorization-consent":false}', '{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenSettings","settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.core.OAuth2AccessToken$TokenFormat","value":"reference"},"settings.token.access-token-time-to-live":["java.time.Duration",900.000000000],"settings.token.refresh-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.reuse-refresh-tokens":false}');


-- ---------------------------------------------------
-- OAuth2 授权记录表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS oauth2_authorization (
    id                              varchar(100)    NOT NULL        COMMENT '主键ID，唯一标识',
    registered_client_id            varchar(100)    NOT NULL        COMMENT '关联的注册客户端ID（对应 oauth2_registered_client.id）',
    principal_name                  varchar(200)    NOT NULL        COMMENT '授权主体名称（即登录用户的用户名）',
    authorization_grant_type        varchar(100)    NOT NULL        COMMENT '本次授权使用的授权类型（如：authorization_code、client_credentials、refresh_token 等）',
    authorized_scopes               varchar(1000)   DEFAULT NULL    COMMENT '本次授权实际批准的权限范围，多个以逗号分隔',
    attributes                      blob            DEFAULT NULL    COMMENT '授权附加属性（序列化存储，包含 SecurityContext 等上下文信息）',
    state                           varchar(500)    DEFAULT NULL    COMMENT 'OAuth2 授权请求中的 state 参数（用于防止 CSRF 攻击）',

    -- 授权码（Authorization Code）相关字段
    authorization_code_value        blob            DEFAULT NULL    COMMENT '授权码的值（加密序列化存储）',
    authorization_code_issued_at    timestamp       DEFAULT NULL    COMMENT '授权码签发时间',
    authorization_code_expires_at   timestamp       DEFAULT NULL    COMMENT '授权码过期时间',
    authorization_code_metadata     blob            DEFAULT NULL    COMMENT '授权码附加元数据（序列化存储）',

    -- 访问令牌（Access Token）相关字段
    access_token_value              blob            DEFAULT NULL    COMMENT 'Access Token 的值（加密序列化存储）',
    access_token_issued_at          timestamp       DEFAULT NULL    COMMENT 'Access Token 签发时间',
    access_token_expires_at         timestamp       DEFAULT NULL    COMMENT 'Access Token 过期时间',
    access_token_metadata           blob            DEFAULT NULL    COMMENT 'Access Token 附加元数据（序列化存储）',
    access_token_type               varchar(100)    DEFAULT NULL    COMMENT 'Access Token 类型（如 Bearer）',
    access_token_scopes             varchar(1000)   DEFAULT NULL    COMMENT 'Access Token 实际携带的权限范围，多个以逗号分隔',

    -- OIDC ID 令牌（ID Token）相关字段
    oidc_id_token_value             blob            DEFAULT NULL    COMMENT 'OIDC ID Token 的值（加密序列化存储）',
    oidc_id_token_issued_at         timestamp       DEFAULT NULL    COMMENT 'OIDC ID Token 签发时间',
    oidc_id_token_expires_at        timestamp       DEFAULT NULL    COMMENT 'OIDC ID Token 过期时间',
    oidc_id_token_metadata          blob            DEFAULT NULL    COMMENT 'OIDC ID Token 附加元数据（序列化存储）',

    -- 刷新令牌（Refresh Token）相关字段
    refresh_token_value             blob            DEFAULT NULL    COMMENT 'Refresh Token 的值（加密序列化存储）',
    refresh_token_issued_at         timestamp       DEFAULT NULL    COMMENT 'Refresh Token 签发时间',
    refresh_token_expires_at        timestamp       DEFAULT NULL    COMMENT 'Refresh Token 过期时间',
    refresh_token_metadata          blob            DEFAULT NULL    COMMENT 'Refresh Token 附加元数据（序列化存储）',

    -- 用户码（User Code，设备授权模式）相关字段
    user_code_value                 blob            DEFAULT NULL    COMMENT '用户码的值（设备授权模式，用户在浏览器端输入的短码）',
    user_code_issued_at             timestamp       DEFAULT NULL    COMMENT '用户码签发时间',
    user_code_expires_at            timestamp       DEFAULT NULL    COMMENT '用户码过期时间',
    user_code_metadata              blob            DEFAULT NULL    COMMENT '用户码附加元数据（序列化存储）',

    -- 设备码（Device Code，设备授权模式）相关字段
    device_code_value               blob            DEFAULT NULL    COMMENT '设备码的值（设备授权模式，设备端轮询使用的长码）',
    device_code_issued_at           timestamp       DEFAULT NULL    COMMENT '设备码签发时间',
    device_code_expires_at          timestamp       DEFAULT NULL    COMMENT '设备码过期时间',
    device_code_metadata            blob            DEFAULT NULL    COMMENT '设备码附加元数据（序列化存储）',

    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2 授权记录表：存储每次授权流程产生的授权信息';


-- ---------------------------------------------------
-- OAuth2 授权确认表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
    registered_client_id    varchar(100)    NOT NULL    COMMENT '关联的注册客户端ID（对应 oauth2_registered_client.id）',
    principal_name          varchar(200)    NOT NULL    COMMENT '授权主体名称（即登录用户的用户名）',
    authorities             varchar(1000)   NOT NULL    COMMENT '用户已确认授权的权限列表，多个以逗号分隔（如：SCOPE_xxx）',
    PRIMARY KEY (registered_client_id, principal_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2 授权确认表：记录用户针对特定客户端手动确认过的权限范围，避免每次登录重复弹出授权确认页';


-- ---------------------------------------------------
-- 索引
-- ---------------------------------------------------
-- CREATE INDEX idx_oauth2_authorization_client_id ON oauth2_authorization (registered_client_id);
-- CREATE INDEX idx_oauth2_authorization_principal ON oauth2_authorization (principal_name);


-- ---------------------------------------------------
-- 产品发布记录表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_release` (
    `id`            VARCHAR(64)   NOT NULL                COMMENT '主键ID，唯一标识',
    `version`       VARCHAR(32)   NOT NULL                COMMENT '版本号，遵循 SemVer 规范，如 v2.3.1（主版本号.次版本号.修订版本号）',
    `title`         VARCHAR(255)  NOT NULL                COMMENT '发布标题，如"新增用户权限管理模块"',
    `description`   TEXT          DEFAULT NULL            COMMENT '发布详情 / 更新说明（支持 Markdown）',
    `release_by`    VARCHAR(64)   DEFAULT NULL            COMMENT '发布人',
    `release_time`  DATETIME      NOT NULL                COMMENT '发布时间',
    `remark`        TEXT          DEFAULT NULL            COMMENT '备注',
    `create_by`     VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`   DATETIME      DEFAULT NULL            COMMENT '创建时间',
    `update_by`     VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`   DATETIME      DEFAULT NULL            COMMENT '更新时间',
    `is_deleted`    TINYINT       NOT NULL DEFAULT 0      COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '产品发布记录表';

-- ---------------------------------------------------
-- 通知公告表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_notice` (
    `id`                VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`         VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `title`             VARCHAR(255)    NOT NULL                    COMMENT '标题',
    `type`              VARCHAR(20)     NOT NULL                    COMMENT '类型（notice 通知，announcement 公告，system 系统消息，other 其他）',
    `content`           LONGTEXT        DEFAULT NULL                COMMENT '内容',
    `level`             VARCHAR(10)     NOT NULL DEFAULT 'normal'   COMMENT '重要级别（normal 普通，important 重要，urgent 紧急）',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'draft'    COMMENT '状态（draft 草稿，published 已发布，revoked 已撤回，archived 已归档）',
    `publisher`         VARCHAR(64)     DEFAULT NULL                COMMENT '发布人',
    `publish_time`      DATETIME        DEFAULT NULL                COMMENT '发布时间（NULL 表示未发布）',
    `effective_time`    DATETIME        DEFAULT NULL                COMMENT '生效时间（NULL 表示立即生效）',
    `expire_time`       DATETIME        DEFAULT NULL                COMMENT '失效时间（NULL 表示永久有效）',
    `is_top`            TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否置顶（0否 1是）',
    `top_expire_time`   DATETIME        DEFAULT NULL                COMMENT '置顶到期时间（NULL 表示永久置顶）',
    `is_popup`          TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否登录弹窗提示（0否 1是）',
    `target_type`       VARCHAR(20)     NOT NULL DEFAULT 'all'      COMMENT '接收范围（all 全体用户，dept 指定部门，role 指定角色，user 指定用户）',
    `read_count`        INT             NOT NULL DEFAULT 0          COMMENT '已读次数',
    `sort`              INT             NOT NULL DEFAULT 100        COMMENT '排序，数字越小越靠前（只用于针对置顶消息排序，即存在多个置顶消息时，根据该字段进行排序，非置顶消息根据发布时间降序）',
    `remark`            TEXT            DEFAULT NULL                COMMENT '备注',
    `version`           INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`         VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`       DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`         VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`       DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告表';


-- ---------------------------------------------------
-- 通知公告接收目标表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_notice_target` (
    `id`                VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`         VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `notice_id`         VARCHAR(64)     NOT NULL                    COMMENT '通知公告ID',
    `target_type`       VARCHAR(20)     NOT NULL                    COMMENT '目标类型（dept 部门，role 角色，user 用户）',
    `target_id`         VARCHAR(64)     NOT NULL                    COMMENT '目标ID（部门ID / 角色ID / 用户ID）',
    `create_by`         VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`       DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`         VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`       DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_notice_target` (`notice_id`, `target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告接收目标表';


-- ---------------------------------------------------
-- 通知公告已读记录表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_notice_read` (
    `id`                VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`         VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `notice_id`         VARCHAR(64)     NOT NULL                    COMMENT '通知公告ID',
    `user_id`           VARCHAR(64)     NOT NULL                    COMMENT '用户ID',
    `read_time`         DATETIME        DEFAULT NULL                COMMENT '读取时间',
    `create_by`         VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`       DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`         VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`       DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告已读记录表';


-- ---------------------------------------------------
-- xxx
-- ---------------------------------------------------






-- 打开外键检查
SET FOREIGN_KEY_CHECKS = 1;
