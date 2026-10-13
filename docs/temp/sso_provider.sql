-- =============================================================================
-- 统一身份认证（SSO）配置 - 表结构 v1.3.0
-- 支持 CAS 与 OAuth2 / OIDC 两种单点登录协议，全程后台配置，零代码适配高校。
--
-- 设计目标
--   1. 新增一所学校的单点登录，只需在本模块界面填表，不改代码、不重启以外的配置
--   2. CAS 与 OAuth2 的差异字段分离，公共字段收敛到主表
--   3. 各校 IdP 返回的用户字段名千差万别，靠属性映射表吸收差异
--   4. 外部身份与本系统账号解耦，支持"自动开户 / 预置账号绑定 / 事后绑绑"三种模式
--
-- 表清单
--   sys_sso_provider                统一身份认证服务商主表（公共配置）
--   sys_sso_provider_cas            CAS 协议专属配置（1:1）
--   sys_sso_provider_oauth2         OAuth2 / OIDC 协议专属配置（1:1）
--   sys_sso_provider_attr_mapping   用户属性映射（1:N）
--   sys_user_third_party            用户第三方账号绑定（跨 provider）
--   另有 ALTER：sys_login_log 增加登录方式与来源字段
--
-- 注意：MySQL 唯一索引对 NULL 值不做重复判定，因此涉及 tenant_id 的唯一约束
--       统一使用生成列 tenant_key（NULL 归一为 '0'，代表平台级配置）参与。
--       如未来确定不做多租户，可去掉该生成列直接按 provider_code 单列唯一。
-- =============================================================================

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;


-- ---------------------------------------------------
-- 统一身份认证服务商主表
-- 承载 CAS / OAuth2 共有的展示、开户与绑定策略配置
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_sso_provider` (
    `id`                        VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`                 VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID；NULL 表示平台级配置，对所有租户生效',
    `tenant_key`                VARCHAR(64)     GENERATED ALWAYS AS (IFNULL(`tenant_id`, '0')) STORED COMMENT '租户唯一键，NULL 归一为 0，仅供唯一索引使用',
    `provider_code`             VARCHAR(50)     NOT NULL                    COMMENT '服务商编码，英文数字下划线，拼接登录与回调URL（如 cas_school、oauth2_wechat）',
    `provider_name`             VARCHAR(100)    NOT NULL                    COMMENT '服务商名称（如 XX大学统一身份认证）',
    `provider_name_en`          VARCHAR(100)    DEFAULT NULL                COMMENT '服务商英文名称，用于国际化',
    `protocol`                  VARCHAR(20)     NOT NULL                    COMMENT '认证协议（cas CAS协议，oauth2 OAuth2/OIDC协议）',
    `realm`                     VARCHAR(20)     NOT NULL DEFAULT 'admin'    COMMENT '登录后用户归属域（admin 后台用户，portal 前台用户）',
    `button_label`              VARCHAR(50)     DEFAULT NULL                COMMENT '登录页按钮文案，为空时回退到服务商名称',
    `button_label_en`           VARCHAR(50)     DEFAULT NULL                COMMENT '登录页按钮英文文案，用于国际化',
    `icon`                      VARCHAR(255)    DEFAULT NULL                COMMENT '登录页按钮图标标识',
    `logo_url`                  VARCHAR(500)    DEFAULT NULL                COMMENT '服务商Logo地址，为空时只用图标',
    `sort`                      INT             NOT NULL DEFAULT 100        COMMENT '登录页按钮排序，数字越小越靠前',
    `auto_create_user`          TINYINT(1)      NOT NULL DEFAULT 1          COMMENT '本系统无账号时是否自动开户（0否 1是）',
    `bind_strategy`             VARCHAR(20)     NOT NULL DEFAULT 'auto'     COMMENT '账号匹配策略（auto 外部标识优先、external_id 仅按外部标识、username 仅按用户名、account_bind 需管理员预绑）',
    `default_role_id`           VARCHAR(64)     DEFAULT NULL                COMMENT '自动开户时的默认角色ID',
    `default_dept_id`           VARCHAR(64)     DEFAULT NULL                COMMENT '自动开户时的默认部门ID',
    `default_user_type`         VARCHAR(20)     DEFAULT 'NORMAL'            COMMENT '自动开户时的默认用户类型（SUPER_ADMIN 超管，TENANT_ADMIN 租户管理员，DEPT_ADMIN 部门管理员，NORMAL 普通用户）',
    `default_user_status`       VARCHAR(10)     NOT NULL DEFAULT 'active'   COMMENT '自动开户后的账号状态（active 正常，inactive 待管理员激活）',
    `update_user_info`          TINYINT(1)      NOT NULL DEFAULT 1          COMMENT '每次登录是否用IdP返回的属性覆盖本地用户信息（0否 1是，覆盖目标字段由属性映射决定）',
    `status`                    VARCHAR(10)     NOT NULL DEFAULT 'enabled'  COMMENT '状态（enabled 启用，disabled 停用）',
    `is_builtin`                TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否内置（0否 1是，内置记录不可删除）',
    `ext_config`                TEXT            DEFAULT NULL                COMMENT '扩展配置（JSON），字段迭代兜底，避免频繁改表',
    `remark`                    VARCHAR(500)    DEFAULT NULL                COMMENT '备注',
    `version`                   INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`                 VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`               DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`                 VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`               DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`                TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_tenant_provider_code` (`tenant_key`, `provider_code`, `is_deleted`) USING BTREE,
    KEY `idx_protocol_status` (`protocol`, `status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一身份认证服务商主表';


-- ---------------------------------------------------
-- CAS 协议配置表（协议版本、票根校验、代理与容错）
-- 依赖 org.springframework.security:spring-security-cas
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_sso_provider_cas` (
    `id`                        VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识（与主表 provider_id 一致）',
    `provider_id`               VARCHAR(64)     NOT NULL                    COMMENT '服务商ID，与主表 sys_sso_provider.id 一一对应',
    `server_url_prefix`         VARCHAR(500)    NOT NULL                    COMMENT 'CAS服务端地址前缀（如 https://cas.xxx.edu.cn/cas）',
    `server_login_url`          VARCHAR(500)    DEFAULT NULL                COMMENT 'CAS登录页地址，为空时取 {prefix}/login',
    `server_logout_url`         VARCHAR(500)    DEFAULT NULL                COMMENT 'CAS登出地址，为空时取 {prefix}/logout',
    `validation_protocol`       VARCHAR(20)     NOT NULL DEFAULT 'CAS30'    COMMENT '校验协议版本（CAS10 CAS1.0，CAS20 CAS2.0，CAS30 CAS3.0，SAML11 SAML1.1）',
    `validation_path`           VARCHAR(500)    DEFAULT NULL                COMMENT '票根校验地址，为空时按协议版本推导默认路径',
    `service_param_name`        VARCHAR(50)     NOT NULL DEFAULT 'service'  COMMENT '回跳地址参数名，个别厂商自定义，默认 service',
    `ticket_param_name`         VARCHAR(50)     NOT NULL DEFAULT 'ticket'   COMMENT '票根参数名，个别厂商自定义，默认 ticket',
    `proxy_callback_url`        VARCHAR(500)    DEFAULT NULL                COMMENT '代理回调地址，需申请 PGT 换取 PT 时才配置，普通场景留空',
    `accept_any_proxy`          TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否接受任意代理（0否 1是，调试用，生产建议0）',
    `allow_empty_ticket`        TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否允许空票根（0否 1是，仅用于排查，生产建议0）',
    `clock_skew_seconds`        INT             NOT NULL DEFAULT 60         COMMENT '允许的服务端与本地时钟偏差（秒），校园网常见时间不同步',
    `charset_encoding`          VARCHAR(20)     NOT NULL DEFAULT 'UTF-8'    COMMENT '票根校验响应字符集编码',
    `skip_ssl_verify`           TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否跳过SSL证书校验（0否 1是，高校大量自签证书，联调期常用，生产建议0）',
    `single_logout_enabled`     TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否启用CAS单点登出（0否 1是）',
    `connect_timeout_ms`        INT             NOT NULL DEFAULT 5000       COMMENT '连接超时时间（毫秒）',
    `read_timeout_ms`           INT             NOT NULL DEFAULT 10000      COMMENT '读取超时时间（毫秒）',
    `remark`                    VARCHAR(500)    DEFAULT NULL                COMMENT '备注',
    `version`                   INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`                 VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`               DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`                 VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`               DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`                TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_provider_id` (`provider_id`, `is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CAS协议配置表';


-- ---------------------------------------------------
-- OAuth2 / OIDC 协议配置表（端点、凭据、令牌与userinfo）
-- 依赖 spring-security-oauth2-client（Spring Boot 已内置管理）
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_sso_provider_oauth2` (
    `id`                        VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识（与主表 provider_id 一致）',
    `provider_id`               VARCHAR(64)     NOT NULL                    COMMENT '服务商ID，与主表 sys_sso_provider.id 一一对应',
    `issuer_uri`                VARCHAR(500)    DEFAULT NULL                COMMENT 'OIDC服务发现地址，填写后可自动推导下方各端点（如 https://idp.xxx.edu.cn/.well-known/openid-configuration）',
    `client_id`                 VARCHAR(200)    NOT NULL                    COMMENT '客户端ID',
    `client_secret`             VARCHAR(1000)   DEFAULT NULL                COMMENT '客户端密钥，必须加密存储，接口返回时脱敏',
    `secret_encrypted`          TINYINT(1)      NOT NULL DEFAULT 1          COMMENT 'client_secret 是否已加密（0否 1是）',
    `client_auth_method`        VARCHAR(30)     NOT NULL DEFAULT 'client_secret_basic' COMMENT '客户端认证方式（client_secret_basic 请求头，client_secret_post 请求体，none 公共客户端配PKCE）',
    `authorization_grant_type`  VARCHAR(30)     NOT NULL DEFAULT 'authorization_code' COMMENT '授权模式（authorization_code 授权码，client_credentials 客户端凭据，urn:ietf:params:oauth:grant-type:jwt-bearer JWT断言）',
    `response_type`             VARCHAR(20)     NOT NULL DEFAULT 'code'     COMMENT '授权请求响应类型，默认 code',
    `redirect_uri_template`     VARCHAR(500)    NOT NULL DEFAULT '{baseUrl}/api/auth/sso/{registrationId}/callback' COMMENT '回调地址模板，占位符 baseUrl 与 registrationId',
    `authorization_uri`         VARCHAR(500)    DEFAULT NULL                COMMENT '授权端点',
    `token_uri`                 VARCHAR(500)    DEFAULT NULL                COMMENT '令牌端点',
    `user_info_uri`             VARCHAR(500)    DEFAULT NULL                COMMENT '用户信息端点',
    `jwk_set_uri`               VARCHAR(500)    DEFAULT NULL                COMMENT 'JWK密钥集端点，ID Token 验签使用',
    `end_session_uri`           VARCHAR(500)    DEFAULT NULL                COMMENT 'OIDC登出端点，用于单点退出',
    `scope`                     VARCHAR(255)    NOT NULL DEFAULT 'openid,profile,email' COMMENT '申请的授权范围，逗号分隔',
    `use_pkce`                  TINYINT(1)      NOT NULL DEFAULT 1          COMMENT '是否启用 PKCE（0否 1是，公共客户端必须开启）',
    `user_name_attribute`       VARCHAR(100)    NOT NULL DEFAULT 'sub'      COMMENT '从 userinfo 或 ID Token 取作用户名的字段名（如 sub、preferred_username、userId）',
    `user_info_auth_method`     VARCHAR(20)     NOT NULL DEFAULT 'header'   COMMENT '调用 userinfo 端点时令牌的传递方式（header Bearer头，form 表单参数，query 查询参数）',
    `user_info_response_type`   VARCHAR(10)     NOT NULL DEFAULT 'json'     COMMENT 'userinfo 响应格式（json JSON对象，jwt JWT令牌需验签后解出）',
    `jws_algorithm`             VARCHAR(100)    DEFAULT NULL                COMMENT 'ID Token 验签算法，为空时按 JWK 自动选择',
    `skip_ssl_verify`           TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否跳过SSL证书校验（0否 1是，联调期常用，生产建议0）',
    `connect_timeout_ms`        INT             NOT NULL DEFAULT 5000       COMMENT '连接超时时间（毫秒）',
    `read_timeout_ms`           INT             NOT NULL DEFAULT 10000      COMMENT '读取超时时间（毫秒）',
    `remark`                    VARCHAR(500)    DEFAULT NULL                COMMENT '备注',
    `version`                   INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`                 VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`               DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`                 VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`               DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`                TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_provider_id` (`provider_id`, `is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2协议配置表';


-- ---------------------------------------------------
-- 统一身份认证属性映射表
-- 把各校 IdP 返回的字段映射到本系统 sys_user 字段，是"零代码适配"的关键
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_sso_provider_attr_mapping` (
    `id`                        VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`                 VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `provider_id`               VARCHAR(64)     NOT NULL                    COMMENT '服务商ID',
    `target_field`              VARCHAR(50)     NOT NULL                    COMMENT '本系统目标字段（username 用户名，nickname 昵称，real_name 真实姓名，email 邮箱，phone 手机号，gender 性别，avatar 头像，employee_no 工号/学号，id_card_no 证件号，department_name 所属部门，ext_attr 扩展属性）',
    `source_attribute`          VARCHAR(100)    NOT NULL                    COMMENT 'IdP 返回的原属性名（CAS 如 uid/cn/mail，OAuth2 如 sub/name/email）',
    `value_path`                VARCHAR(200)    DEFAULT NULL                COMMENT '嵌套取值路径，支持点号或 JSONPath（如 attributes.user.name）',
    `value_type`                VARCHAR(20)     NOT NULL DEFAULT 'string'   COMMENT '值类型（string 字符串，int 整数，date 日期，boolean 布尔，json JSON对象）',
    `value_mapping`             TEXT            DEFAULT NULL                COMMENT '值转换规则（JSON），用于枚举归一，如 {"teacher":"教职工","student":"学生"}',
    `default_value`             VARCHAR(255)    DEFAULT NULL                COMMENT 'IdP 未返回该属性时的兜底值',
    `is_required`               TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否必填（0否 1是，缺失则直接拒绝本次登录）',
    `is_identity`               TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否作为外部唯一标识（0否 1是，写入 sys_user_third_party.external_id，通常且仅一条）',
    `sort`                      INT             NOT NULL DEFAULT 100        COMMENT '排序，数字越小越靠前',
    `remark`                    VARCHAR(500)    DEFAULT NULL                COMMENT '备注',
    `version`                   INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`                 VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`               DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`                 VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`               DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`                TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_provider_target` (`provider_id`, `target_field`, `is_deleted`) USING BTREE,
    KEY `idx_identity` (`provider_id`, `is_identity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一身份认证属性映射表';


-- ---------------------------------------------------
-- 用户第三方账号绑定表
-- 记录本系统用户与外部身份的对应关系，与 sys_user 解耦
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_third_party` (
    `id`                        VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`                 VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID',
    `tenant_key`                VARCHAR(64)     GENERATED ALWAYS AS (IFNULL(`tenant_id`, '0')) STORED COMMENT '租户唯一键，NULL 归一为 0，仅供唯一索引使用',
    `user_id`                   VARCHAR(64)     NOT NULL                    COMMENT '本系统用户ID，对应 sys_user.id',
    `provider_id`               VARCHAR(64)     NOT NULL                    COMMENT '服务商ID',
    `provider_code`             VARCHAR(50)     DEFAULT NULL                COMMENT '服务商编码，冗余存储便于日志与排障',
    `external_id`               VARCHAR(200)    NOT NULL                    COMMENT 'IdP侧唯一标识（CAS 的 uid、OAuth2 的 sub），一个服务商内唯一',
    `origin_attributes`         TEXT            DEFAULT NULL                COMMENT '最近一次 IdP 返回的原始属性快照（JSON），用于溯源与排障',
    `bind_time`                 DATETIME        DEFAULT NULL                COMMENT '首次绑定时间',
    `last_login_time`           DATETIME        DEFAULT NULL                COMMENT '最近一次通过该身份登录的时间',
    `remark`                    VARCHAR(500)    DEFAULT NULL                COMMENT '备注',
    `version`                   INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`                 VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`               DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`                 VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`               DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`                TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_provider_external` (`tenant_key`, `provider_id`, `external_id`, `is_deleted`) USING BTREE,
    UNIQUE INDEX `uk_user_provider` (`tenant_key`, `user_id`, `provider_id`, `is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户第三方账号绑定表';


-- ---------------------------------------------------
-- 登录日志表增强：补充登录方式与来源，使单点登录可审计
-- 下方用 information_schema 判断以保证脚本可重复执行
-- ---------------------------------------------------
SET @exist_login_type = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_login_log' AND COLUMN_NAME = 'login_type');
SET @ddl_login_type = IF(@exist_login_type = 0,
    "ALTER TABLE `sys_login_log` ADD COLUMN `login_type` VARCHAR(20) NOT NULL DEFAULT 'password' COMMENT '登录方式（password 账密，cas CAS统一认证，oauth2 OAuth2统一认证）' AFTER `username`",
    'SELECT 1');
PREPARE stmt FROM @ddl_login_type; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist_provider_code = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_login_log' AND COLUMN_NAME = 'provider_code');
SET @ddl_provider_code = IF(@exist_provider_code = 0,
    "ALTER TABLE `sys_login_log` ADD COLUMN `provider_code` VARCHAR(50) DEFAULT NULL COMMENT '统一身份认证服务商编码，账密登录为空' AFTER `login_type`",
    'SELECT 1');
PREPARE stmt FROM @ddl_provider_code; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist_login_log_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_login_log' AND INDEX_NAME = 'idx_login_type');
SET @ddl_login_log_idx = IF(@exist_login_log_idx = 0,
    'ALTER TABLE `sys_login_log` ADD KEY `idx_login_type` (`login_type`)',
    'SELECT 1');
PREPARE stmt FROM @ddl_login_log_idx; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ---------------------------------------------------
-- 菜单与权限种子数据
-- 挂在「系统管理」(id=9999) 模块下，sort 延续既有序列（9930 -> 9920，本次 9931 -> 9921）
-- tree_path 规则：0 + 祖先ID链 + 自身ID
-- ---------------------------------------------------
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `name_en`, `type`, `code`, `path`, `component`, `icon`, `sort`, `is_visible`, `is_external`, `status`, `tree_path`, `realm`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES
	('9931', '9999', '统一身份认证', 'SSO Provider', 'menu', 'sys:sso-provider:list', '/system/sso-provider', 'SsoProviderListView', 'sym_r_key', 9921, 1, 0, 'enabled', '0,9999,9931', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('993101', '9931', '查询', 'Query', 'button', 'sys:sso-provider:query', NULL, NULL, NULL, 993101, 1, 0, 'enabled', '0,9999,9931,993101', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('993102', '9931', '新增', 'Create', 'button', 'sys:sso-provider:create', NULL, NULL, NULL, 993102, 1, 0, 'enabled', '0,9999,9931,993102', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('993103', '9931', '编辑', 'Update', 'button', 'sys:sso-provider:update', NULL, NULL, NULL, 993103, 1, 0, 'enabled', '0,9999,9931,993103', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('993104', '9931', '删除', 'Delete', 'button', 'sys:sso-provider:delete', NULL, NULL, NULL, 993104, 1, 0, 'enabled', '0,9999,9931,993104', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('993105', '9931', '启停', 'Toggle', 'button', 'sys:sso-provider:toggle', NULL, NULL, NULL, 993105, 1, 0, 'enabled', '0,9999,9931,993105', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0),
	('993106', '9931', '测试连接', 'Test', 'button', 'sys:sso-provider:test', NULL, NULL, NULL, 993106, 1, 0, 'enabled', '0,9999,9931,993106', 'admin', NULL, 0, 'system', NOW(), 'system', NOW(), 0);


-- ---------------------------------------------------
-- 字典种子数据：协议、登录方式等枚举，供前端下拉与列表翻译使用
-- 先写主字典再写字典项，is_builtin=1 表示不允许界面删除
-- ---------------------------------------------------
INSERT INTO `sys_dict` (`id`, `tenant_id`, `name`, `code`, `status`, `is_builtin`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES
	('sso_protocol', NULL, '统一认证协议', 'sso_protocol', 'enabled', 1, 'CAS 与 OAuth2/OIDC 两种单点登录协议', 0, 'system', NOW(), 'system', NOW(), 0),
	('sso_login_type', NULL, '登录方式', 'sso_login_type', 'enabled', 1, '账密登录与各类单点登录方式', 0, 'system', NOW(), 'system', NOW(), 0),
	('sso_bind_strategy', NULL, '账号匹配策略', 'sso_bind_strategy', 'enabled', 1, '外部身份与本系统账号的匹配方式', 0, 'system', NOW(), 'system', NOW(), 0);

INSERT INTO `sys_dict_data` (`id`, `tenant_id`, `dict_id`, `label`, `value`, `status`, `sort`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES
	('sso_protocol_cas', NULL, 'sso_protocol', 'CAS', 'cas', 'enabled', 10, '中央认证服务，高校最主流', 'system', NOW(), 'system', NOW(), 0),
	('sso_protocol_oauth2', NULL, 'sso_protocol', 'OAuth2 / OIDC', 'oauth2', 'enabled', 20, '开放授权协议，含 OIDC 身份层', 'system', NOW(), 'system', NOW(), 0),
	('sso_login_type_password', NULL, 'sso_login_type', '账密登录', 'password', 'enabled', 10, '本系统自己的用户名密码', 'system', NOW(), 'system', NOW(), 0),
	('sso_login_type_cas', NULL, 'sso_login_type', 'CAS 统一认证', 'cas', 'enabled', 20, 'CAS 票根校验登录', 'system', NOW(), 'system', NOW(), 0),
	('sso_login_type_oauth2', NULL, 'sso_login_type', 'OAuth2 统一认证', 'oauth2', 'enabled', 30, 'OAuth2 授权码登录', 'system', NOW(), 'system', NOW(), 0),
	('sso_bind_auto', NULL, 'sso_bind_strategy', '外部标识优先', 'auto', 'enabled', 10, '先按外部标识查绑定，无则按用户名匹配，都没有才开户', 'system', NOW(), 'system', NOW(), 0),
	('sso_bind_external_id', NULL, 'sso_bind_strategy', '仅按外部标识', 'external_id', 'enabled', 20, '严格以绑定表的外部标识为准', 'system', NOW(), 'system', NOW(), 0),
	('sso_bind_username', NULL, 'sso_bind_strategy', '仅按用户名', 'username', 'enabled', 30, '以 IdP 返回的用户名直接匹配本系统账号', 'system', NOW(), 'system', NOW(), 0),
	('sso_bind_account_bind', NULL, 'sso_bind_strategy', '预绑定', 'account_bind', 'enabled', 40, '需管理员先建好绑定关系，否则拒绝登录', 'system', NOW(), 'system', NOW(), 0);


-- ---------------------------------------------------
-- 说明：上面的业务数据（服务商、属性映射）不预置种子。
-- 每所学校的地址、凭据、返回字段都不同，预置反而误导，由实施人员在界面配置。
-- 建议由「新增服务商」向导在首次创建时，按所选协议自动生成一版默认的属性映射，
-- 用户在此基础上增删改即可。
-- ---------------------------------------------------
