-- ============================================================
-- v1.3.0 统一身份认证（SSO）—— 规范化方案（4 张表）
-- 设计文档：docs/统一身份认证(SSO)配置-表结构设计.md
-- 适用库：sca_platform（system 与 auth 服务共用）
-- 说明：与并行产物 1.3.0_sso_config.sql（单表+JSON）为【互斥】方案，二者取其一执行。
-- ============================================================
USE sca_platform;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET FOREIGN_KEY_CHECKS = 0;


-- ------------------------------------------------------------
-- 1) 统一身份认证配置主表：公共配置 + 启用控制 + 用户映射策略
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_sso_config` (
    `id`                   VARCHAR(64)   NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`            VARCHAR(64)   DEFAULT NULL                COMMENT '租户ID（多租户隔离，每所学校独立配置）',
    `name`                 VARCHAR(100)  NOT NULL                    COMMENT '配置名称，如"XX大学统一身份认证"',
    `protocol_type`        VARCHAR(20)   NOT NULL                    COMMENT '协议类型（cas / oauth2）',
    `status`               VARCHAR(10)   NOT NULL DEFAULT 'disabled' COMMENT '启用状态（enabled 启用，disabled 禁用）',
    `is_default`           TINYINT(1)    NOT NULL DEFAULT 0          COMMENT '是否默认登录方式（0否 1是；每个租户至多一条为1）',
    `sort`                 INT           NOT NULL DEFAULT 100        COMMENT '排序，数字越小越靠前',
    `auto_create_user`     TINYINT(1)    NOT NULL DEFAULT 1          COMMENT 'SSO 首次登录时是否自动创建本地用户（0否 1是）',
    `default_role_ids`     VARCHAR(500)  DEFAULT NULL                COMMENT '自动创建用户时默认绑定的角色ID，多个以逗号分隔',
    `default_dept_id`      VARCHAR(64)   DEFAULT NULL                COMMENT '自动创建用户时默认归属部门ID',
    `username_attribute`   VARCHAR(100)  DEFAULT NULL                COMMENT '用户名映射属性（SSO 返回属性中作为系统用户名的字段，如工号/学号/preferred_username）',
    `nickname_attribute`   VARCHAR(100)  DEFAULT NULL                COMMENT '昵称映射属性（SSO 属性 -> sys_user.nickname）',
    `real_name_attribute`  VARCHAR(100)  DEFAULT NULL                COMMENT '姓名映射属性（SSO 属性 -> sys_user.real_name）',
    `email_attribute`      VARCHAR(100)  DEFAULT NULL                COMMENT '邮箱映射属性（SSO 属性 -> sys_user.email）',
    `phone_attribute`      VARCHAR(100)  DEFAULT NULL                COMMENT '手机号映射属性（SSO 属性 -> sys_user.phone）',
    `avatar_attribute`     VARCHAR(100)  DEFAULT NULL                COMMENT '头像映射属性（SSO 属性 -> sys_user.avatar）',
    `remark`               TEXT          DEFAULT NULL                COMMENT '备注',
    `version`              INT           NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`            VARCHAR(64)   DEFAULT NULL                COMMENT '创建人',
    `create_time`          DATETIME      DEFAULT NULL                COMMENT '创建时间',
    `update_by`            VARCHAR(64)   DEFAULT NULL                COMMENT '更新人',
    `update_time`          DATETIME      DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`           TINYINT(1)    NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),


-- ------------------------------------------------------------
-- 2) CAS 协议配置明细表（与主表 1:1，protocol_type='cas' 时使用）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_sso_cas_config` (
    `id`                 VARCHAR(64)   NOT NULL                    COMMENT '主键ID，唯一标识',
    `sso_config_id`      VARCHAR(64)   NOT NULL                    COMMENT '关联统一身份认证配置主表ID（sys_sso_config.id）',
    `tenant_id`          VARCHAR(64)   DEFAULT NULL                COMMENT '租户ID（冗余，便于隔离查询）',
    `server_url_prefix`  VARCHAR(255)  NOT NULL                    COMMENT 'CAS 服务端地址前缀，如 https://cas.xxx.edu.cn/cas',
    `server_login_url`   VARCHAR(255)  NOT NULL                    COMMENT 'CAS 登录地址，如 https://cas.xxx.edu.cn/cas/login',
    `server_logout_url`  VARCHAR(255)  DEFAULT NULL                COMMENT 'CAS 登出地址，如 https://cas.xxx.edu.cn/cas/logout',
    `service_url`        VARCHAR(255)  NOT NULL                    COMMENT '本系统服务（回调）地址，CAS 认证成功后携带 ticket 回调该地址',
    `cas_version`        VARCHAR(10)   NOT NULL DEFAULT '3.0'      COMMENT 'CAS 协议版本（2.0 / 3.0）',
    `charset`            VARCHAR(20)   NOT NULL DEFAULT 'utf-8'    COMMENT 'CAS 服务端字符编码',
    `create_by`          VARCHAR(64)   DEFAULT NULL                COMMENT '创建人',
    `create_time`        DATETIME      DEFAULT NULL                COMMENT '创建时间',
    `update_by`          VARCHAR(64)   DEFAULT NULL                COMMENT '更新人',
    `update_time`        DATETIME      DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`         TINYINT(1)    NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_sso_config_id` (`sso_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一身份认证 CAS 协议配置明细表';


-- ------------------------------------------------------------
-- 3) OAuth2 / OIDC 协议配置明细表（与主表 1:1，protocol_type='oauth2' 时使用）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_sso_oauth2_config` (
    `id`                           VARCHAR(64)   NOT NULL                    COMMENT '主键ID，唯一标识',
    `sso_config_id`                VARCHAR(64)   NOT NULL                    COMMENT '关联统一身份认证配置主表ID（sys_sso_config.id）',
    `tenant_id`                    VARCHAR(64)   DEFAULT NULL                COMMENT '租户ID（冗余，便于隔离查询）',
    `client_id`                    VARCHAR(200)  NOT NULL                    COMMENT 'OAuth2 客户端ID（向学校 IdP 申请）',
    `client_secret`                VARCHAR(512)  DEFAULT NULL                COMMENT 'OAuth2 客户端密钥（AES-GCM 加密存储）',
    `cipher_version`               VARCHAR(10)   DEFAULT NULL                COMMENT '密钥版本号（用于密钥轮换时解密旧密文）',
    `authorization_uri`            VARCHAR(255)  NOT NULL                    COMMENT '授权端点，如 https://idp.xxx.edu.cn/oauth2/authorize',
    `token_uri`                    VARCHAR(255)  NOT NULL                    COMMENT '令牌端点，如 https://idp.xxx.edu.cn/oauth2/token',
    `user_info_uri`                VARCHAR(255)  DEFAULT NULL                COMMENT '用户信息端点，如 https://idp.xxx.edu.cn/oauth2/userinfo',
    `jwk_set_uri`                  VARCHAR(255)  DEFAULT NULL                COMMENT 'JWK 公钥集合端点（用于校验 id_token 签名）',
    `issuer_uri`                   VARCHAR(255)  DEFAULT NULL                COMMENT '签发者地址（OIDC Discovery，如 /.well-known/openid-configuration）',
    `redirect_uri`                 VARCHAR(255)  NOT NULL                    COMMENT '授权回调地址（本系统接收 code 的地址，需在 IdP 注册）',
    `scope`                        VARCHAR(255)  DEFAULT 'openid profile'   COMMENT '授权范围，多个以空格分隔（如 openid profile email）',
    `user_name_attribute`          VARCHAR(100)  DEFAULT NULL                COMMENT '用户名属性（userinfo/id_token 中作为系统用户名的 claim，如 preferred_username/sub）',
    `client_authentication_method` VARCHAR(50)   DEFAULT 'client_secret_basic' COMMENT '客户端认证方式（client_secret_basic / client_secret_post / none）',
    `create_by`                    VARCHAR(64)   DEFAULT NULL                COMMENT '创建人',
    `create_time`                  DATETIME      DEFAULT NULL                COMMENT '创建时间',
    `update_by`                    VARCHAR(64)   DEFAULT NULL                COMMENT '更新人',


-- ------------------------------------------------------------
-- 4) 用户 SSO 绑定表：本地用户与外部身份的映射（SSO 登录识别必需）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_sso` (
    `id`                VARCHAR(64)   NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`         VARCHAR(64)   DEFAULT NULL                COMMENT '租户ID',
    `user_id`           VARCHAR(64)   NOT NULL                    COMMENT '本地用户ID（sys_user.id）',
    `sso_config_id`     VARCHAR(64)   NOT NULL                    COMMENT '关联的统一身份认证配置ID（sys_sso_config.id）',
    `protocol_type`     VARCHAR(20)   NOT NULL                    COMMENT '协议类型（cas / oauth2，冗余便于按协议检索）',
    `external_user_id`  VARCHAR(200)  NOT NULL                    COMMENT '外部身份唯一标识（如工号/学号/openid/sub）',
    `external_username` VARCHAR(200)  DEFAULT NULL                COMMENT '外部身份提供方中的账号名',
    `last_login_time`   DATETIME      DEFAULT NULL                COMMENT '最近一次 SSO 登录时间',
    `create_by`         VARCHAR(64)   DEFAULT NULL                COMMENT '创建人',
    `create_time`       DATETIME      DEFAULT NULL                COMMENT '创建时间',
    `update_by`         VARCHAR(64)   DEFAULT NULL                COMMENT '更新人',
    `update_time`       DATETIME      DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`        TINYINT(1)    NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_protocol_external` (`sso_config_id`, `external_user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户 SSO 绑定表（本地用户与外部身份映射）';


SET FOREIGN_KEY_CHECKS = 1;

    `update_time`                  DATETIME      DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`                   TINYINT(1)    NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_sso_config_id` (`sso_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一身份认证 OAuth2/OIDC 协议配置明细表';

    UNIQUE INDEX `uk_tenant_protocol` (`tenant_id`, `protocol_type`),
    KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一身份认证配置主表（CAS/OAuth2 通用配置）';
