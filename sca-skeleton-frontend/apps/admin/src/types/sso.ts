/**
 * 统一身份认证配置相关类型
 *
 * 约定：
 * - CAS 与 OAuth2/OIDC 为两套彼此独立的单例配置，各自对应一个「只读表单 + 编辑保存」页面；
 * - 生产环境两种 SSO 互斥，至多启用其一（可以都不启用，此时仅走系统本地登录）；
 * - clientSecret 等敏感字段后端回显为掩码，前端凭 clientSecretMasked 判断是否需重新提交明文。
 */

/** SSO 提供方标识 */
export type SsoProvider = "CAS" | "OAUTH2";

/** 两个提供方的启用状态（用于前端互斥启用时的确认提示） */
export interface SsoEnabledStatus {
  casEnabled: boolean;
  oauth2Enabled: boolean;
}

/** 用户绑定策略：外部身份与本地用户的绑定方式 */
export type SsoBindingPolicy =
  /** 首登自动创建本地用户并绑定 */
  | "AUTO_CREATE"
  /** 首登按唯一标识匹配已导入用户自动绑定，匹配不到则拒绝 */
  | "AUTO_BIND_BY_MATCH"
  /** 必须预先存在绑定关系，否则拒绝登录 */
  | "REQUIRE_PREBIND";

/** 属性映射：从认证响应中读取的字段名 → 本地用户字段 */
export interface SsoAttributeMapping {
  /** 用户唯一标识属性名（CAS principal / OAuth2 sub），用于匹配与绑定 */
  principal: string;
  /** 用户名字段属性名 */
  username: string;
  /** 真实姓名属性名 */
  realName: string;
  /** 邮箱属性名 */
  email: string;
  /** 手机号属性名 */
  phone: string;
  /** 部门/院系属性名 */
  dept: string;
  /** 角色属性名（多值逗号分隔） */
  roles: string;
}

/** 自定义扩展参数键值对（应对各校非标准实现） */
export interface SsoExtraParam {
  key: string;
  value: string;
}

/** CAS 单点登录配置（对应后端 config_json，type=CAS） */
export interface CasConfig {
  id?: string;
  /** 乐观锁版本号 */
  version?: number;
  /** 配置名称 */
  name: string;
  /** 是否启用（与 OAuth2 互斥） */
  enabled: boolean;

  // ── 服务端连接参数 ──
  /** CAS 服务器地址前缀，如 https://cas.university.edu/cas */
  serverUrlPrefix: string;
  /** 本系统服务回调地址（Service / ticket 回调 URL） */
  serviceUrl: string;
  /** 登录端点路径（默认 /login） */
  loginUrl: string;
  /** 登出端点路径（默认 /logout） */
  logoutUrl: string;
  /** 票据校验协议版本：V2=serviceValidate，V3=p3/serviceValidate（带属性） */
  protocolVersion: "V2" | "V3";
  /** 票据校验端点路径 */
  validateUrl: string;

  // ── 用户绑定 ──
  bindingPolicy: SsoBindingPolicy;
  attributeMapping: SsoAttributeMapping;

  // ── 扩展 ──
  extraParams: SsoExtraParam[];
  remark?: string;
}

/** OAuth2 / OIDC 单点登录配置（对应后端 config_json，type=OAUTH2） */
export interface Oauth2Config {
  id?: string;
  version?: number;
  name: string;
  enabled: boolean;

  // ── 端点发现 ──
  /** 是否使用 OIDC Discovery 自动发现端点 */
  useDiscovery: boolean;
  /** OIDC issuer（Discovery 用），如 https://oauth.university.edu/realms/master */
  issuer?: string;
  /** Discovery 文档地址（默认 {issuer}/.well-known/openid-configuration） */
  discoveryUri?: string;

  // ── 客户端参数 ──
  clientId: string;
  /** 客户端密钥（回显为掩码；未改动则不重新提交） */
  clientSecret: string;
  /** 后端回显时 clientSecret 是否为掩码（true 表示前端未拿到明文，保存时不应覆盖） */
  clientSecretMasked?: boolean;

  // ── 协议端点（useDiscovery=false 时手动填写） ──
  authorizationUri: string;
  tokenUri: string;
  userInfoUri: string;
  /** JWK Set 端点（id_token 验签，OIDC 场景） */
  jwkSetUri?: string;
  /** 回调地址 */
  redirectUri: string;
  /** 登出端点（全局退出 / end_session_endpoint） */
  logoutUri: string;

  // ── 授权流程参数 ──
  /** 申请作用域（OIDC 至少含 openid） */
  scope: string;
  /** 响应类型，固定 code（授权码模式，RFC 6749 §1.3.1；禁用 implicit） */
  responseType: string;
  /** 客户端认证方式（RFC 6749 §2.3.1 / OIDC Core §9） */
  clientAuthenticationMethod:
    | "client_secret_basic"
    | "client_secret_post"
    | "client_secret_jwt"
    | "private_key_jwt";
  /** 是否启用 PKCE（RFC 7636，公钥客户端强制） */
  usePkce: boolean;
  /** PKCE 方法（S256 推荐；plain 仅在对方不支持时降级） */
  pkceMethod: "S256" | "plain";

  // ── 用户绑定 ──
  bindingPolicy: SsoBindingPolicy;
  attributeMapping: SsoAttributeMapping;

  // ── 扩展 ──
  extraParams: SsoExtraParam[];
  remark?: string;
}

/** CAS 表单默认值 */
export function defaultCasConfig(): CasConfig {
  return {
    name: "",
    enabled: false,
    serverUrlPrefix: "",
    serviceUrl: "",
    loginUrl: "/login",
    logoutUrl: "/logout",
    protocolVersion: "V3",
    validateUrl: "/p3/serviceValidate",
    bindingPolicy: "AUTO_BIND_BY_MATCH",
    attributeMapping: {
      principal: "",
      username: "",
      realName: "",
      email: "",
      phone: "",
      dept: "",
      roles: ""
    },
    extraParams: [],
    remark: ""
  };
}

/** OAuth2/OIDC 表单默认值 */
export function defaultOauth2Config(): Oauth2Config {
  return {
    name: "",
    enabled: false,
    useDiscovery: true,
    issuer: "",
    discoveryUri: "",
    clientId: "",
    clientSecret: "",
    clientSecretMasked: false,
    authorizationUri: "",
    tokenUri: "",
    userInfoUri: "",
    jwkSetUri: "",
    redirectUri: "",
    logoutUri: "",
    scope: "",
    responseType: "code",
    clientAuthenticationMethod: "client_secret_basic",
    usePkce: true,
    pkceMethod: "S256",
    bindingPolicy: "AUTO_BIND_BY_MATCH",
    attributeMapping: {
      principal: "",
      username: "",
      realName: "",
      email: "",
      phone: "",
      dept: "",
      roles: ""
    },
    extraParams: [],
    remark: ""
  };
}
