-- ============================================================
-- 数据源管理模块建表脚本
-- 对应设计方案 v1.3 第八章
-- 表名统一 ds_ 前缀，字段风格沿用 sys_post 既有约定
-- ============================================================

-- -----------------------------------------------------------
-- 1. 驱动表 ds_driver
-- 系统级全局资产（无 tenant_id）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ds_driver` (
    `id`              VARCHAR(64)    NOT NULL                COMMENT '主键ID',
    `db_type`         VARCHAR(50)    NOT NULL                COMMENT '数据库类型',
    `driver_name`     VARCHAR(200)   NOT NULL                COMMENT '驱动名称',
    `driver_class`    VARCHAR(255)   NOT NULL                COMMENT 'JDBC Driver 全限定类名',
    `driver_version`  VARCHAR(50)    NOT NULL                COMMENT '驱动业务版本',
    `jar_sha256`      VARCHAR(64)    NOT NULL                COMMENT 'JAR 文件 SHA256',
    `object_key`      VARCHAR(500)   NOT NULL                COMMENT '存储对象键',
    `file_size`       BIGINT         DEFAULT NULL            COMMENT 'JAR 文件大小（字节）',
    `storage_type`    VARCHAR(10)    NOT NULL DEFAULT 'local' COMMENT '存储类型（local/minio）',
    `url_template`    VARCHAR(500)   DEFAULT NULL            COMMENT 'JDBC URL 前缀模板',
    `allowed_params`  JSON           DEFAULT NULL            COMMENT 'URL 参数白名单',
    `status`          VARCHAR(10)    NOT NULL DEFAULT 'enabled' COMMENT '状态（enabled/disabled）',
    `is_builtin`      TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '是否内置（0否 1是）',
    `remark`          TEXT           DEFAULT NULL            COMMENT '备注',
    `version`         INT            NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(64)    DEFAULT NULL            COMMENT '创建人ID',
    `create_time`     DATETIME       DEFAULT NULL            COMMENT '创建时间',
    `update_by`       VARCHAR(64)    DEFAULT NULL            COMMENT '更新人ID',
    `update_time`     DATETIME       DEFAULT NULL            COMMENT '更新时间',
    `is_deleted`      TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_db_type_status` (`db_type`, `status`),
    KEY `idx_sha256` (`jar_sha256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源驱动表';

-- -----------------------------------------------------------
-- 2. 数据源表 ds_datasource
-- 按租户隔离。v1 不提供 url_override 字段（已删除）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ds_datasource` (
    `id`                VARCHAR(64)    NOT NULL            COMMENT '主键ID',
    `tenant_id`         VARCHAR(64)    DEFAULT NULL        COMMENT '租户ID',
    `name`              VARCHAR(200)   NOT NULL            COMMENT '数据源名称',
    `db_type`           VARCHAR(50)    NOT NULL            COMMENT '数据库类型',
    `driver_id`         VARCHAR(64)    DEFAULT NULL        COMMENT '关联驱动ID',
    `host`              VARCHAR(255)   NOT NULL            COMMENT '主机地址',
    `port`              INT            NOT NULL            COMMENT '端口',
    `database_name`     VARCHAR(255)   DEFAULT NULL        COMMENT '数据库名',
    `username`          VARCHAR(200)   NOT NULL            COMMENT '用户名',
    `password_cipher`   VARCHAR(512)   NOT NULL            COMMENT '密码（AES-GCM加密）',
    `cipher_version`    VARCHAR(10)    DEFAULT NULL        COMMENT '密钥版本',
    `connection_params` TEXT           DEFAULT NULL            COMMENT '连接参数（JSON 或 query string 格式）',
    `pool_config`       JSON           DEFAULT NULL        COMMENT '连接池配置（JSON）',
    `enabled`           TINYINT(1)     NOT NULL DEFAULT 1  COMMENT '管理态（1启用 0禁用）',
    `connection_state`  VARCHAR(10)    DEFAULT 'offline'   COMMENT '运行态（offline/online/error）',
    `error_msg`         TEXT           DEFAULT NULL        COMMENT '连接错误信息',
    `last_connect_time` DATETIME       DEFAULT NULL        COMMENT '最后连接时间',
    `version`           INT           NOT NULL DEFAULT 0   COMMENT '乐观锁版本号',
    `create_by`         VARCHAR(64)    DEFAULT NULL        COMMENT '创建人ID',
    `create_time`       DATETIME       DEFAULT NULL        COMMENT '创建时间',
    `update_by`         VARCHAR(64)    DEFAULT NULL        COMMENT '更新人ID',
    `update_time`       DATETIME       DEFAULT NULL        COMMENT '更新时间',
    `is_deleted`        TINYINT(1)     NOT NULL DEFAULT 0  COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_enabled` (`tenant_id`, `enabled`, `is_deleted`),
    KEY `idx_driver_id` (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源表';

-- -----------------------------------------------------------
-- 3. SQL 查询历史表 ds_query_log（日志表，只追加）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ds_query_log` (
    `id`                VARCHAR(64)    NOT NULL            COMMENT '主键ID',
    `tenant_id`         VARCHAR(64)    DEFAULT NULL        COMMENT '租户ID',
    `datasource_id`     VARCHAR(64)    NOT NULL            COMMENT '数据源ID',
    `datasource_name`   VARCHAR(200)   DEFAULT NULL        COMMENT '数据源名称（冗余）',
    `database_type`     VARCHAR(50)    NOT NULL            COMMENT '数据库类型',
    `user_id`           VARCHAR(64)    NOT NULL            COMMENT '执行人ID',
    `user_name`         VARCHAR(200)   DEFAULT NULL        COMMENT '执行人名称',
    `sql_content`      VARCHAR(4000)  NOT NULL            COMMENT 'SQL语句（截断4000）',
    `sql_type`          VARCHAR(20)   DEFAULT NULL        COMMENT 'SQL类型',
    `status`            VARCHAR(20)   NOT NULL             COMMENT '状态（success/fail）',
    `cost_ms`           BIGINT        DEFAULT NULL        COMMENT '耗时（毫秒）',
    `row_count`         BIGINT        DEFAULT NULL        COMMENT '行数',
    `error_msg`         TEXT          DEFAULT NULL        COMMENT '错误信息',
    `client_ip`         VARCHAR(128)  DEFAULT NULL        COMMENT '客户端IP',
    `create_time`       DATETIME      NOT NULL            COMMENT '执行时间',
    PRIMARY KEY (`id`),
    KEY `idx_datasource_time` (`datasource_id`, `create_time`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL查询历史表';

-- -----------------------------------------------------------
-- 4. SQL 模板表 ds_sql_template
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ds_sql_template` (
    `id`              VARCHAR(64)    NOT NULL                COMMENT '主键ID',
    `tenant_id`       VARCHAR(64)    DEFAULT NULL            COMMENT '租户ID',
    `datasource_id`   VARCHAR(64)    NOT NULL                COMMENT '数据源ID',
    `name`            VARCHAR(200)   NOT NULL                COMMENT '模板名称',
    `description`     TEXT           DEFAULT NULL            COMMENT '描述',
    `sql_content`     MEDIUMTEXT     NOT NULL                COMMENT 'SQL内容',
    `parameters`      JSON           DEFAULT NULL            COMMENT '参数定义',
    `category`        VARCHAR(100)   DEFAULT NULL            COMMENT '分类',
    `tags`            VARCHAR(500)   DEFAULT NULL            COMMENT '标签',
    `is_builtin`      TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '是否内置',
    `status`          VARCHAR(10)    NOT NULL DEFAULT 'enabled' COMMENT '状态',
    `version`         INT            NOT NULL DEFAULT 0      COMMENT '乐观锁',
    `create_by`       VARCHAR(64)    DEFAULT NULL            COMMENT '创建人ID',
    `create_time`     DATETIME       DEFAULT NULL            COMMENT '创建时间',
    `update_by`       VARCHAR(64)    DEFAULT NULL            COMMENT '更新人ID',
    `update_time`     DATETIME       DEFAULT NULL            COMMENT '更新时间',
    `is_deleted`      TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_ds` (`tenant_id`, `datasource_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL查询模板表';
