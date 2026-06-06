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
    `storage_limit`   BIGINT          DEFAULT -1                  COMMENT '存储限制(MB)',
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


-- 默认租户套餐
INSERT INTO `sys_tenant_package` (
    `id`, `name`, `code`, `status`, `user_limit`, `api_limit`, `storage_limit`, `expire_days`,
    `sort`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1', '默认套餐', 'default', 'enabled', -1, -1, -1, -1,
    1, '系统内置套餐', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_tenant_package` WHERE `code` = 'default' AND `is_deleted` = 0
);


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
    `account_limit`   INT             DEFAULT -1                  COMMENT '账号数量限制（-1不限）',
    `expire_time`     DATETIME        DEFAULT NULL                COMMENT '过期时间（NULL表示永不过期）',
    `status`          VARCHAR(20)     NOT NULL                    COMMENT '租户状态（normal 正常，disabled 禁用，expired 过期，cancelled 注销）',
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


-- 默认平台租户
INSERT INTO `sys_tenant` (
    `id`, `name`, `code`, `package_id`, `contact_name`, `contact_phone`, `contact_email`, `domain_name`,
    `account_limit`, `expire_time`, `status`, `config_json`, `remark`, `version`,
    `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1', '平台默认租户', 'default', '1',
    NULL, NULL, NULL, NULL,
    -1, NULL, 'normal', NULL, '系统内置租户', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_tenant` WHERE `code` = 'default' AND `is_deleted` = 0
);


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
    `user_category`           VARCHAR(20)     NOT NULL                    COMMENT '用户类别（backend 后台用户，frontend 前台用户）',
    `user_type`               VARCHAR(100)    DEFAULT NULL                COMMENT '用户类型（superadmin 平台超级管理员，tenant_admin 租户管理员，dept_admin 部门管理员，normal 普通用户）',
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
    `is_builtin`              TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否系统内置（0否 1是）',
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


-- 内置管理员账号（密码与用户名相同：admin）
INSERT INTO `sys_user` (
    `id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `gender`, `avatar`, `phone`, `email`,
    `user_category`, `user_type`, `status`, `status_time`, `status_reason`, `login_fail_count`, `must_change_password`,
    `password_update_time`, `effective_start_time`, `effective_end_time`, `last_login_ip`, `last_login_time`,
    `is_builtin`, `source_type`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1', '1', 'admin',
    '{bcrypt}$2b$10$UGpiJ1fpY7c1BZ5AeRPpQumCd3iWyYmUynb.8mGw5W70e82h3.dWS',
    '管理员', '系统管理员', NULL, NULL, NULL, NULL,
    'backend', 'superadmin', 'active', NULL, NULL, 0, 0,
    NOW(), NOW(), NULL, NULL, NULL,
    1, 'initial', '系统内置管理员账号', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_user`
    WHERE `tenant_id` = '1' AND `username` = 'admin' AND `is_deleted` = 0
);

-- 内置前台门户演示账号（密码与用户名相同：portal）
INSERT INTO `sys_user` (
    `id`, `tenant_id`, `username`, `password`, `nickname`, `real_name`, `gender`, `avatar`, `phone`, `email`,
    `user_category`, `user_type`, `status`, `status_time`, `status_reason`, `login_fail_count`, `must_change_password`,
    `password_update_time`, `effective_start_time`, `effective_end_time`, `last_login_ip`, `last_login_time`,
    `is_builtin`, `source_type`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '2', '1', 'portal',
    '{bcrypt}$2b$10$O6pcDPNvxGrX1fA554.Cf.XWfovrwpSJFWYiyo//CTMqQigSQr6MS',
    '门户用户', '前台演示用户', NULL, NULL, NULL, NULL,
    'frontend', 'member', 'active', NULL, NULL, 0, 0,
    NOW(), NOW(), NULL, NULL, NULL,
    1, 'initial', '系统内置门户演示账号', 0, 'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_user`
    WHERE `tenant_id` = '1' AND `username` = 'portal' AND `is_deleted` = 0
);


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
    `data_scope`      VARCHAR(20)     NOT NULL                    COMMENT '数据权限范围（all 全部，tenant 租户，dept_and_sub 本部门及下级，dept 仅本部门，personal 仅本人，custom 自定义）',
    `is_builtin`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否系统内置（0否 1是）',
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


-- 平台超级管理员角色
INSERT INTO `sys_role` (
    `id`, `tenant_id`, `name`, `code`, `data_scope`, `is_builtin`, `sort`, `remark`, `version`,
    `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1', '1', '平台超级管理员', 'superadmin', 'all', 1, 1,
    '系统内置超级管理员角色', 0,
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role`
    WHERE `tenant_id` = '1' AND `code` = 'superadmin' AND `is_deleted` = 0
);


-- ---------------------------------------------------
-- 角色数据权限范围表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role_data_scope` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色数据权限范围表（指定角色可以访问哪些部门的数据）';


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


-- 管理员角色绑定
INSERT INTO `sys_user_role` (
    `id`, `tenant_id`, `user_id`, `role_id`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`
)
SELECT
    '1', '1', '1', '1',
    'system', NOW(), 'system', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_user_role`
    WHERE `user_id` = '1' AND `role_id` = '1' AND `is_deleted` = 0
);


-- ---------------------------------------------------
-- 权限表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `parent_id`       VARCHAR(64)     NOT NULL DEFAULT '0'        COMMENT '父权限ID',
    `name`            VARCHAR(255)    NOT NULL                    COMMENT '权限名称',
    `type`            VARCHAR(10)     NOT NULL                    COMMENT '权限类型（folder目录 menu菜单 button按钮）',
    `code`            VARCHAR(255)    DEFAULT NULL                COMMENT '权限标识（如: system:user:list）',
    `path`            VARCHAR(255)    DEFAULT NULL                COMMENT '路由地址',
    `component`       VARCHAR(255)    DEFAULT NULL                COMMENT '前端组件路径',
    `icon`            VARCHAR(255)    DEFAULT NULL                COMMENT '图标',
    `sort`            INT             NOT NULL DEFAULT 100        COMMENT '排序，数字越小越靠前',
    `is_visible`      TINYINT(1)      NOT NULL DEFAULT 1          COMMENT '是否可见（0否 1是）',
    `is_external`     TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否外链（0否 1是）',
    `status`          VARCHAR(10)     NOT NULL                    COMMENT '状态（enabled正常 disabled停用）',
    `tree_path`       VARCHAR(500)    NOT NULL                    COMMENT 'ID层级路径，逗号分隔（如: 0,100,1001）',
    `remark`          TEXT            DEFAULT NULL                COMMENT '备注',
    `version`         INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';


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
    `status`          TINYINT(1)      NOT NULL                    COMMENT '状态（enabled 启用，disabled 停用）',
    `is_builtin`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否内置',
    `remark`          VARCHAR(500)    DEFAULT NULL                COMMENT '备注',
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
    `dict_id`         VARCHAR(64)     NOT NULL                    COMMENT '字典类型ID',
    `label`           VARCHAR(100)    NOT NULL                    COMMENT '字典标签',
    `value`           VARCHAR(100)    NOT NULL                    COMMENT '字典值',
    `value_type`      VARCHAR(16)     NOT NULL DEFAULT 'string'   COMMENT '值类型 string/int/boolean',
    `color_type`      VARCHAR(32)     DEFAULT NULL                COMMENT 'Tag颜色类型 default/primary/success/warning/danger/info',
    `css_class`       VARCHAR(128)    DEFAULT NULL                COMMENT '自定义样式类名',
    `status`          TINYINT(1)      NOT NULL                    COMMENT '状态（enabled 启用，disabled 停用）',
    `is_default`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否默认',
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
    `type`            VARCHAR(20)     DEFAULT 'string'            COMMENT '类型',
    `status`          TINYINT(1)      NOT NULL                    COMMENT '状态（enabled 启用，disabled 停用）',
    `is_builtin`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否内置',
    `remark`          VARCHAR(500)    DEFAULT NULL                COMMENT '备注',
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
    `ip`              VARCHAR(128)    DEFAULT NULL                COMMENT '登录IP',
    `location`        VARCHAR(255)    DEFAULT NULL                COMMENT '登录位置',
    `device`          VARCHAR(100)    DEFAULT NULL                COMMENT '设备类型',
    `browser`         VARCHAR(100)    DEFAULT NULL                COMMENT '浏览器',
    `os`              VARCHAR(100)    DEFAULT NULL                COMMENT '操作系统',
    `status`          VARCHAR(20)     NOT NULL                    COMMENT '登录状态',
    `msg`             VARCHAR(255)    DEFAULT NULL                COMMENT '提示消息',
    `login_time`      DATETIME        NOT NULL                    COMMENT '登录时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';


-- ---------------------------------------------------
-- 操作日志表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `user_id`         VARCHAR(64)     DEFAULT NULL                COMMENT '用户ID',
    `module`          VARCHAR(100)    DEFAULT NULL                COMMENT '操作模块',
    `type`            VARCHAR(20)     DEFAULT NULL                COMMENT '操作类型',
    `request_url`     VARCHAR(500)    DEFAULT NULL                COMMENT '请求地址',
    `request_method`  VARCHAR(10)     DEFAULT NULL                COMMENT '请求方法',
    `request_params`  TEXT            DEFAULT NULL                COMMENT '请求参数',
    `response_result` TEXT            DEFAULT NULL                COMMENT '返回结果',
    `cost_time`       VARCHAR(10)     DEFAULT NULL                COMMENT '耗时',
    `ip`              VARCHAR(128)    DEFAULT NULL                COMMENT 'IP地址',
    `location`        VARCHAR(255)    DEFAULT NULL                COMMENT '操作位置',
    `status`          VARCHAR(20)     DEFAULT NULL                COMMENT '操作状态',
    `error_msg`       TEXT            DEFAULT NULL                COMMENT '错误信息',
    `operation_time`  DATETIME        NOT NULL                    COMMENT '操作时间',
    PRIMARY KEY (`id`)
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


-- 内置 OAuth2 客户端：管理后台（公共客户端 + PKCE）
INSERT INTO oauth2_registered_client (
    id,
    client_id,
    client_id_issued_at,
    client_secret,
    client_secret_expires_at,
    client_name,
    client_authentication_methods,
    authorization_grant_types,
    redirect_uris,
    post_logout_redirect_uris,
    scopes,
    client_settings,
    token_settings
)
SELECT
    REPLACE(UUID(), '-', ''),
    'sca-admin-client',
    CURRENT_TIMESTAMP,
    NULL,
    NULL,
    'SCA Admin SPA',
    'none',
    'authorization_code,refresh_token',
    'http://localhost:5173/oauth/callback',
    NULL,
    'openid,profile,offline_access,all',
    '{"settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}',
    '{"settings.token.access-token-format":{"value":"reference"},"settings.token.authorization-code-time-to-live":["java.time.Duration",60.000000000],"settings.token.access-token-time-to-live":["java.time.Duration",900.000000000],"settings.token.refresh-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.reuse-refresh-tokens":false}'
WHERE NOT EXISTS (
    SELECT 1 FROM oauth2_registered_client WHERE client_id = 'sca-admin-client'
);

-- 内置 OAuth2 客户端：前台门户（公共客户端 + PKCE）
INSERT INTO oauth2_registered_client (
    id,
    client_id,
    client_id_issued_at,
    client_secret,
    client_secret_expires_at,
    client_name,
    client_authentication_methods,
    authorization_grant_types,
    redirect_uris,
    post_logout_redirect_uris,
    scopes,
    client_settings,
    token_settings
)
SELECT
    REPLACE(UUID(), '-', ''),
    'sca-portal-client',
    CURRENT_TIMESTAMP,
    NULL,
    NULL,
    'SCA Portal SPA',
    'none',
    'authorization_code,refresh_token',
    'http://localhost:5174/oauth/callback',
    NULL,
    'openid,profile,offline_access,all',
    '{"settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}',
    '{"settings.token.access-token-format":{"value":"reference"},"settings.token.authorization-code-time-to-live":["java.time.Duration",60.000000000],"settings.token.access-token-time-to-live":["java.time.Duration",900.000000000],"settings.token.refresh-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.reuse-refresh-tokens":false}'
WHERE NOT EXISTS (
    SELECT 1 FROM oauth2_registered_client WHERE client_id = 'sca-portal-client'
);


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
-- xxx
-- ---------------------------------------------------






-- 打开外键检查
SET FOREIGN_KEY_CHECKS = 1;
