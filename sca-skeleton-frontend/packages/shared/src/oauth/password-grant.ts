/**
 * OAuth2 密码模式（Resource Owner Password Credentials Grant）客户端工具。
 *
 * 替代原 PKCE 模块，提供：
 * - 密码模式登录（POST /oauth2/token + grant_type=password）
 * - 刷新令牌（POST /oauth2/token + grant_type=refresh_token）
 * - 吊销令牌（POST /oauth2/revoke）
 * - 从 Vite 环境变量读取机密客户端配置
 */

/** OAuth2 机密客户端应用配置（来自 Vite 环境变量）。 */
export interface OAuthAppConfig {
  /** 客户端 ID */
  clientId: string;
  /** 客户端密钥（机密客户端，用于 Basic 认证） */
  clientSecret: string;
  /** Token 端点 URL（POST /oauth2/token） */
  tokenUrl: string;
  /** Revoke 端点 URL（POST /oauth2/revoke） */
  revokeUrl: string;
  /** 申请的权限范围 */
  scope: string;
  /** SPA HTML base 路径，如 {@code "/admin/"} */
  basePath?: string;
}

/** /oauth2/token 成功响应体（Spring Authorization Server 标准字段）。 */
export interface OAuthTokenResponse {
  access_token: string;
  refresh_token?: string;
  expires_in?: number;
  token_type?: string;
  scope?: string;
}

/** 后端统一响应 Result 格式（登录失败时返回）。 */
export interface ApiResult {
  code: number;
  message: string;
  data?: unknown;
  type?: string;
}

/**
 * 构造 Basic 认证头值（Base64(client_id:client_secret)）。
 */
function buildBasicAuthHeader(config: OAuthAppConfig): string {
  const credentials = `${config.clientId}:${config.clientSecret}`;
  return `Basic ${btoa(credentials)}`;
}

/**
 * 解析 token 端点响应。
 * <p>
 * 后端登录失败（验证码错误、用户名密码错误等）返回 HTTP 200 + Result 格式（code/message），
 * 成功返回 OAuth2 标准格式（access_token/refresh_token）。
 * 通过判断响应体是否包含 access_token 字段区分成功/失败。
 */
async function parseTokenResponse(response: Response): Promise<OAuthTokenResponse> {
  let body: unknown;
  try {
    body = await response.json();
  } catch {
    throw new Error(`认证请求失败，请联系管理员`);
  }
  // 成功响应：OAuth2 标准格式包含 access_token 字段
  if (body && typeof (body as OAuthTokenResponse).access_token === "string") {
    return body as OAuthTokenResponse;
  }
  // 失败响应：Result 格式，提取 message
  const message = (body as ApiResult)?.message ?? `认证请求失败，请联系管理员`;
  throw new Error(message);
}

/**
 * 使用密码模式获取 access_token 和 refresh_token。
 *
 * @param config      OAuth 客户端配置
 * @param username    用户名
 * @param password    密码
 * @param captchaKey  验证码 key（portal 渠道必填）
 * @param captchaCode 验证码文本（portal 渠道必填）
 */
export async function loginWithPassword(
  config: OAuthAppConfig,
  username: string,
  password: string,
  captchaKey?: string,
  captchaCode?: string
): Promise<OAuthTokenResponse> {
  const body = new URLSearchParams({
    grant_type: "password",
    username,
    password,
    scope: config.scope
  });
  if (captchaKey) {
    body.set("captcha_key", captchaKey);
  }
  if (captchaCode) {
    body.set("captcha_code", captchaCode);
  }

  const response = await fetch(config.tokenUrl, {
    method: "POST",
    credentials: "omit",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      Authorization: buildBasicAuthHeader(config)
    },
    body: body.toString()
  });

  return parseTokenResponse(response);
}

/**
 * 使用 refresh_token 静默续期 access_token。
 */
export async function refreshAccessToken(
  config: OAuthAppConfig,
  refreshToken: string
): Promise<OAuthTokenResponse> {
  const body = new URLSearchParams({
    grant_type: "refresh_token",
    refresh_token: refreshToken,
    scope: config.scope
  });

  const response = await fetch(config.tokenUrl, {
    method: "POST",
    credentials: "omit",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      Authorization: buildBasicAuthHeader(config)
    },
    body: body.toString()
  });

  return parseTokenResponse(response);
}

/**
 * 吊销 access_token 或 refresh_token（RFC 7009）。
 * <p>
 * 吊销失败时仅记录控制台警告，不阻断本地令牌清理。
 * access_token TTL 较短（默认 15 分钟），refresh_token 失效后无法续期，
 * 实际风险窗口较小。
 */
export async function revokeOAuthToken(
  config: OAuthAppConfig,
  token: string,
  tokenTypeHint: "access_token" | "refresh_token" = "access_token"
): Promise<void> {
  const body = new URLSearchParams({
    token,
    token_type_hint: tokenTypeHint
  });
  try {
    const response = await fetch(config.revokeUrl, {
      method: "POST",
      credentials: "omit",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Authorization: buildBasicAuthHeader(config)
      },
      body: body.toString()
    });
    if (!response.ok) {
      console.warn(`[OAuth] Token 吊销失败: HTTP ${response.status}，令牌将在 TTL 到期后自动失效`);
    }
  } catch (e) {
    console.warn("[OAuth] Token 吊销请求异常，令牌将在 TTL 到期后自动失效", e);
  }
}

/**
 * 将 API 基地址规范为可用于拼接 OAuth 端点的绝对前缀。
 * 相对路径（如 /api）在浏览器中取当前站点 origin。
 */
function resolveApiBasePrefix(apiBase: string): string {
  const trimmed = apiBase.trim().replace(/\/$/, "");
  if (!trimmed) {
    return "";
  }
  if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
    return trimmed;
  }
  const path = trimmed.startsWith("/") ? trimmed : `/${trimmed}`;
  const origin = typeof window !== "undefined" ? window.location.origin : "";
  return `${origin}${path}`;
}

/**
 * 从 Vite 环境变量读取机密客户端配置。
 *
 * token / revoke 端点走同域路径，经 Nginx/Vite 代理到网关。
 *
 * @param env ImportMeta.env
 */
export function readOAuthConfigFromEnv(env: ImportMetaEnv): OAuthAppConfig {
  const apiPrefix = resolveApiBasePrefix(env.VITE_API_BASE_URL ?? "");
  return {
    clientId: env.VITE_OAUTH_CLIENT_ID,
    clientSecret: env.VITE_OAUTH_CLIENT_SECRET,
    tokenUrl: `${apiPrefix}/auth/oauth2/token`,
    revokeUrl: `${apiPrefix}/auth/oauth2/revoke`,
    scope: env.VITE_OAUTH_SCOPE ?? "profile all"
  };
}

/** Vite 环境变量扩展（各 SPA 的 env.d.ts 应引用相同字段）。 */
export interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_OAUTH_CLIENT_ID: string;
  readonly VITE_OAUTH_CLIENT_SECRET: string;
  /** OAuth2 scope（可选，默认 "profile all"） */
  readonly VITE_OAUTH_SCOPE?: string;
}
