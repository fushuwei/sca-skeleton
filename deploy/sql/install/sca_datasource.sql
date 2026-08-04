-- 创建数据库
CREATE DATABASE IF NOT EXISTS sca_datasource DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 使用数据库
USE sca_datasource;

-- 设置连接字符集
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- 关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;



-- -----------------------------------------------------------
-- 驱动表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ds_driver` (
    `id`              VARCHAR(64)     NOT NULL                        COMMENT '主键ID，唯一标识',
    `name`            VARCHAR(200)    NOT NULL                        COMMENT '驱动名称',
    `db_type`         VARCHAR(50)     NOT NULL                        COMMENT '数据库类型',
    `driver_class`    VARCHAR(255)    DEFAULT NULL                    COMMENT 'JDBC Driver 全限定类名（MongoDB 等非 JDBC 类型可为空）',
    `storage_type`    VARCHAR(10)     NOT NULL DEFAULT 'local'        COMMENT '存储类型（local 本地存储，minio 对象存储）',
    `url_template`    VARCHAR(500)    DEFAULT NULL                    COMMENT 'JDBC URL 前缀模板',
    `remark`          TEXT            DEFAULT NULL                    COMMENT '备注',
    `version`         INT             NOT NULL DEFAULT 0              COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(64)     DEFAULT NULL                    COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                    COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                    COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                    COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0              COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    KEY `idx_db_type` (`db_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源驱动表';

-- -----------------------------------------------------------
-- 驱动文件表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ds_driver_file` (
    `id`              VARCHAR(64)     NOT NULL                        COMMENT '主键ID，唯一标识',
    `driver_id`       VARCHAR(64)     NOT NULL                        COMMENT '关联驱动ID',
    `file_name`       VARCHAR(255)    NOT NULL                        COMMENT '文件名（上传时的原始文件名）',
    `file_size`       BIGINT          DEFAULT NULL                    COMMENT '文件大小（字节）',
    `sha256`          VARCHAR(64)     NOT NULL                        COMMENT '文件 SHA256 校验值',
    `sort_order`      INT             NOT NULL DEFAULT 0              COMMENT '排序序号（主文件 0，依赖按上传顺序递增）',
    `create_by`       VARCHAR(64)     DEFAULT NULL                    COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                    COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                    COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                    COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0              COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    KEY `idx_driver_id` (`driver_id`),
    KEY `idx_sha256` (`sha256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='驱动文件表';

-- -----------------------------------------------------------
-- 数据源表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ds_datasource` (
    `id`                VARCHAR(64)     NOT NULL                        COMMENT '主键ID，唯一标识',
    `tenant_id`         VARCHAR(64)     DEFAULT NULL                    COMMENT '租户ID',
    `name`              VARCHAR(200)    NOT NULL                        COMMENT '数据源名称',
    `db_type`           VARCHAR(50)     NOT NULL                        COMMENT '数据库类型',
    `driver_id`         VARCHAR(64)     DEFAULT NULL                    COMMENT '关联驱动ID',
    `host`              VARCHAR(255)    NOT NULL                        COMMENT '主机地址（IP 或域名）',
    `port`              INT             NOT NULL                        COMMENT '端口号',
    `database_name`     VARCHAR(255)    DEFAULT NULL                    COMMENT '数据库名称',
    `username`          VARCHAR(200)    NOT NULL                        COMMENT '用户名',
    `password_cipher`   VARCHAR(512)    NOT NULL                        COMMENT '密码（AES-GCM 加密密文）',
    `cipher_version`    VARCHAR(10)     DEFAULT NULL                    COMMENT '密钥版本（用于轮换时解密旧密文）',
    `connection_params` TEXT            DEFAULT NULL                    COMMENT '连接参数（JSON 格式，仅允许白名单内 key）',
    `pool_config`       JSON            DEFAULT NULL                    COMMENT '连接池配置（JSON 格式）',
    `enabled`           TINYINT(1)      NOT NULL DEFAULT 1              COMMENT '管理态（1 启用，0 禁用）',
    `connection_state`  VARCHAR(10)     DEFAULT 'offline'               COMMENT '运行态（offline 离线，在线 online，error 异常）',
    `error_msg`         TEXT            DEFAULT NULL                    COMMENT '最后连接错误信息',
    `last_connect_time` DATETIME        DEFAULT NULL                    COMMENT '最后连接成功时间',
    `version`           INT             NOT NULL DEFAULT 0              COMMENT '乐观锁版本号',
    `create_by`         VARCHAR(64)     DEFAULT NULL                    COMMENT '创建人',
    `create_time`       DATETIME        DEFAULT NULL                    COMMENT '创建时间',
    `update_by`         VARCHAR(64)     DEFAULT NULL                    COMMENT '更新人',
    `update_time`       DATETIME        DEFAULT NULL                    COMMENT '更新时间',
    `is_deleted`        TINYINT(1)      NOT NULL DEFAULT 0              COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_enabled` (`tenant_id`, `enabled`, `is_deleted`),
    KEY `idx_driver_id` (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源表';

-- -----------------------------------------------------------
-- SQL 查询历史表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ds_query_log` (
    `id`                VARCHAR(64)     NOT NULL                        COMMENT '主键ID，唯一标识',
    `tenant_id`         VARCHAR(64)     DEFAULT NULL                    COMMENT '租户ID',
    `datasource_id`     VARCHAR(64)     NOT NULL                        COMMENT '数据源ID',
    `datasource_name`   VARCHAR(200)    DEFAULT NULL                    COMMENT '数据源名称（冗余，防止数据源删除后无法展示）',
    `database_type`     VARCHAR(50)     NOT NULL                        COMMENT '数据库类型',
    `user_id`           VARCHAR(64)     NOT NULL                        COMMENT '执行人ID',
    `user_name`         VARCHAR(200)    DEFAULT NULL                    COMMENT '执行人名称',
    `sql_content`       VARCHAR(4000)   NOT NULL                        COMMENT 'SQL 语句（截断存储，最大 4000 字符）',
    `sql_type`          VARCHAR(20)     DEFAULT NULL                    COMMENT 'SQL 类型（SELECT / SHOW / DESC / EXPLAIN）',
    `status`            VARCHAR(20)     NOT NULL                        COMMENT '执行状态（success 成功，fail 失败）',
    `cost_ms`           BIGINT          DEFAULT NULL                    COMMENT '执行耗时（毫秒）',
    `row_count`         BIGINT          DEFAULT NULL                    COMMENT '返回行数',
    `error_msg`         TEXT            DEFAULT NULL                    COMMENT '查询错误信息',
    `client_ip`         VARCHAR(128)    DEFAULT NULL                    COMMENT '客户端IP',
    `create_time`       DATETIME        NOT NULL                        COMMENT '执行时间',
    PRIMARY KEY (`id`),
    KEY `idx_datasource_time` (`datasource_id`, `create_time`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL 查询历史表';

-- -----------------------------------------------------------
-- SQL 模板表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ds_sql_template` (
    `id`              VARCHAR(64)     NOT NULL                        COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                    COMMENT '租户ID',
    `datasource_id`   VARCHAR(64)     NOT NULL                        COMMENT '数据源ID',
    `name`            VARCHAR(200)    NOT NULL                        COMMENT '模板名称',
    `description`     TEXT            DEFAULT NULL                    COMMENT '描述',
    `sql_content`     MEDIUMTEXT      NOT NULL                        COMMENT 'SQL 语句内容',
    `parameters`      JSON            DEFAULT NULL                    COMMENT '参数定义（JSON 格式，绑定 PreparedStatement 占位符）',
    `category`        VARCHAR(100)    DEFAULT NULL                    COMMENT '模板分类',
    `tags`            VARCHAR(500)    DEFAULT NULL                    COMMENT '模板标签',
    `is_builtin`      TINYINT(1)      NOT NULL DEFAULT 0              COMMENT '是否内置（0否 1是）',
    `status`          VARCHAR(10)     NOT NULL DEFAULT 'enabled'      COMMENT '状态（enabled 启用，disabled 禁用）',
    `version`         INT             NOT NULL DEFAULT 0              COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(64)     DEFAULT NULL                    COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                    COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                    COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                    COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0              COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_ds` (`tenant_id`, `datasource_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL 查询模板表';




-- 打开外键检查
SET FOREIGN_KEY_CHECKS = 1;
