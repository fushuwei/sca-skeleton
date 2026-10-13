-- ---------------------------------------------------
-- 统一身份认证配置表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_sso_config` (
    `id`              VARCHAR(64)     NOT NULL                    COMMENT '主键ID，唯一标识',
    `tenant_id`       VARCHAR(64)     DEFAULT NULL                COMMENT '租户ID（NULL表示全局/平台级配置）',
    `name`            VARCHAR(100)    NOT NULL                    COMMENT '配置名称（如：北京大学CAS、清华大学OAuth2）',
    `code`            VARCHAR(64)     NOT NULL                    COMMENT '配置编码（唯一标识，用于前端路由跳转）',
    `type`            VARCHAR(20)     NOT NULL                    COMMENT '认证类型（CAS、OAUTH2）',
    `status`          VARCHAR(10)     NOT NULL                    COMMENT '状态（enabled 启用，disabled 禁用）',
    `is_default`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否默认配置（0否 1是，每租户仅能有一个默认）',
    `sort`            INT             NOT NULL DEFAULT 100        COMMENT '排序，数字越小越靠前',
    `config_json`     JSON            DEFAULT NULL                COMMENT '类型专用配置参数（JSON格式，CAS/OAuth2字段不同）',
    `remark`          TEXT            DEFAULT NULL                COMMENT '备注',
    `version`         INT             NOT NULL DEFAULT 0          COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '创建人',
    `create_time`     DATETIME        DEFAULT NULL                COMMENT '创建时间',
    `update_by`       VARCHAR(64)     DEFAULT NULL                COMMENT '更新人',
    `update_time`     DATETIME        DEFAULT NULL                COMMENT '更新时间',
    `is_deleted`      TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否删除（0否 1是）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`),
    KEY `idx_tenant_type` (`tenant_id`, `type`),
    KEY `idx_tenant_default` (`tenant_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一身份认证配置表';


-- ---------------------------------------------------
-- CAS 配置示例（config_json 结构说明）
-- ---------------------------------------------------
/*
CAS 类型的 config_json 示例：
{
  "serverUrlPrefix": "https://cas.university.edu",          // CAS服务器地址（必填）
  "loginUrl": "/cas/login",                                  // 登录页面路径（默认/cas/login）
  "logoutUrl": "/cas/logout",                                // 登出页面路径（默认/cas/logout）
  "validateUrl": "/cas/p3/serviceValidate",                  // 票据验证路径（默认/cas/p3/serviceValidate）
  "serviceUrl": "https://sca-platform.edu/login/cas/callback", // 本系统回调地址（必填）
  "encoding": "UTF-8",                                       // 编码（默认UTF-8）
  "renew": false,                                            // 是否强制重新认证（默认false）
  "gateway": false,                                          // 是否网关模式（默认false）
  "ignoreSslErrors": false,                                  // 忽略SSL证书错误（默认false）
  "proxyCallbackUrl": "",                                    // 代理回调地址（可选）
  "proxyReceptorUrl": "/cas/proxy/receptor",                 // 代理接收器路径（可选）
  "attributePrincipalId": "user_id",                         // 用户唯一标识属性名（默认user_id）
  "attributeUsername": "username",                           // 用户名属性名（默认username）
  "attributeEmail": "email",                                 // 邮箱属性名（可选）
  "attributePhone": "phone",                                 // 手机属性名（可选）
  "attributeRealName": "real_name",                          // 真实姓名属性名（可选）
  "attributeDept": "dept",                                   // 部门属性名（可选）
  "attributeRoles": "roles"                                  // 角色属性名（可选，多值逗号分隔）
}
*/


-- ---------------------------------------------------
-- OAuth2 配置示例（config_json 结构说明）
-- ---------------------------------------------------
/*
OAuth2 类型的 config_json 示例：
{
  "clientId": "sca-platform-client",                         // 客户端ID（必填）
  "clientSecret": "********",                                // 客户端密钥（必填，存储时加密）
  "authorizationUri": "https://oauth.university.edu/oauth2/authorize", // 授权端点（必填）
  "tokenUri": "https://oauth.university.edu/oauth2/token",   // 令牌端点（必填）
  "userInfoUri": "https://oauth.university.edu/oauth2/userinfo", // 用户信息端点（必填）
  "jwkSetUri": "https://oauth.university.edu/oauth2/jwks",   // JWK Set端点（可选，用于JWT验签）
  "redirectUri": "https://sca-platform.edu/login/oauth2/callback", // 回调地址（必填）
  "scope": "openid profile email phone",                     // 申请作用域（默认openid profile）
  "clientAuthenticationMethod": "client_secret_post",        // 客户端认证方式（默认client_secret_post）
  "authorizationGrantType": "authorization_code",            // 授权类型（默认authorization_code）
  "userNameAttributeName": "sub",                            // 用户唯一标识属性名（默认sub）
  "ignoreSslErrors": false,                                  // 忽略SSL证书错误（默认false）
  "usePkce": true,                                           // 使用PKCE（默认true，安全增强）
  "pkceMethod": "S256",                                      // PKCE方法（默认S256）
  "attributeMapping": {                                      // 属性映射（可选）
    "username": "preferred_username",
    "email": "email",
    "phone": "phone_number",
    "realName": "name",
    "dept": "department",
    "roles": "roles"
  }
}
*/


-- ---------------------------------------------------
-- 示例数据（仅作演示，实际部署时由管理员配置）
-- ---------------------------------------------------
-- INSERT INTO `sys_sso_config` (`id`, `tenant_id`, `name`, `code`, `type`, `status`, `is_default`, `sort`, `config_json`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES
-- ('1', '1', '校内CAS统一认证', 'cas-campus', 'CAS', 'enabled', 1, 10, '{"serverUrlPrefix":"https://cas.university.edu","serviceUrl":"https://sca-platform.edu/login/cas/callback"}', '校内统一身份认证CAS配置', 0, 'system', NOW(), 'system', NOW(), 0);
-- 
-- INSERT INTO `sys_sso_config` (`id`, `tenant_id`, `name`, `code`, `type`, `status`, `is_default`, `sort`, `config_json`, `remark`, `version`, `create_by`, `create_time`, `update_by`, `update_time`, `is_deleted`) VALUES
-- ('2', '1', '校内OAuth2统一认证', 'oauth2-campus', 'OAUTH2', 'disabled', 0, 20, '{"clientId":"sca-platform","authorizationUri":"https://oauth.university.edu/oauth2/authorize","tokenUri":"https://oauth.university.edu/oauth2/token","userInfoUri":"https://oauth.university.edu/oauth2/userinfo","redirectUri":"https://sca-platform.edu/login/oauth2/callback"}', '校内OAuth2配置示例', 0, 'system', NOW(), 'system', NOW(), 0);