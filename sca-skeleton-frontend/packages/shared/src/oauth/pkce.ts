/** PKCE 字符集（RFC 7636 推荐 unreserved 字符） */
const PKCE_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";

/**
 * 生成密码学安全随机字符串。
 *
 * @param length 目标长度
 */
export function generateRandomString(length: number): string {
  const array = new Uint8Array(length);
  crypto.getRandomValues(array);
  return Array.from(array, (byte) => PKCE_CHARSET[byte % PKCE_CHARSET.length]).join("");
}

/** 生成 PKCE code_verifier（43~128 字符，此处固定 64）。 */
export function generateCodeVerifier(): string {
  return generateRandomString(64);
}

/**
 * BASE64URL 编码（无 padding）。
 *
 * @param buffer SHA-256 摘要 ArrayBuffer
 */
function base64UrlEncode(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/**
 * 由 code_verifier 计算 S256 code_challenge。
 *
 * @param verifier PKCE verifier
 */
export async function generateCodeChallenge(verifier: string): Promise<string> {
  const data = new TextEncoder().encode(verifier);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return base64UrlEncode(digest);
}

/** 生成 OAuth2 state（防 CSRF）。 */
export function generateState(): string {
  return generateRandomString(32);
}

/** OAuth2 公共客户端应用配置（来自 Vite 环境变量）。 */
export interface OAuthAppConfig {
  clientId: string;
  authorizeUrl: string;
  tokenUrl: string;
  redirectUri: string;
  scope: string;
  /** SPA HTML base 路径，如 {@code "/admin/"}。用于从绝对浏览器路径中剥离前缀以生成路由器兼容的 returnUrl。 */
  basePath?: string;
}

/**
 * 将 {@code window.location.pathname} 等含 SPA base 的绝对路径标准化为路由器兼容的路径。
 *
 * @param rawUrl   原始路径，可能含 base 前缀，如 {@code "/admin/system/user"}
 * @param basePath SPA HTML base，如 {@code "/admin/"}
 * @returns 剥离 base 后的路径，如 {@code "/system/user"}
 */
function normalizeReturnUrl(rawUrl: string, basePath?: string): string {
  if (!basePath || basePath === "/") {
    return rawUrl;
  }
  const normalizedBase = basePath.endsWith("/") ? basePath : basePath + "/";
  // 循环剥离 base 前缀，防止 URL 已损坏时残留重复前缀（如 /admin/admin/system/user）
  let result = rawUrl;
  while (result.startsWith(normalizedBase)) {
    result = result.slice(normalizedBase.length - 1);
  }
  // 确保结果以 "/" 开头（Vue Router 绝对路径要求）
  if (!result.startsWith("/")) {
    result = "/" + result;
  }
  return result;
}

/** 启动 OAuth 授权时可附加的查询参数。 */
export interface OAuthLoginOptions {
  /** 强制重新登录（logout 场景使用 {@code login}）。 */
  prompt?: string;
}

/** /oauth2/token 成功响应体（Spring Authorization Server 标准字段）。 */
export interface OAuthTokenResponse {
  access_token: string;
  refresh_token?: string;
  expires_in?: number;
  token_type?: string;
  scope?: string;
}

/** 授权 redirect 前写入 sessionStorage 的 PKCE 临时会话。 */
export interface PkceSession {
  codeVerifier: string;
  state: string;
  returnUrl: string;
}

/**
 * 构造 sessionStorage 键名（按 state 隔离不同 OAuth 请求）。
 * 使用 state 作为键，天然全局唯一，不依赖任何浏览器行为假设。
 *
 * @param state OAuth2 state 参数
 */
function pkceStorageKey(state: string): string {
  return `oauth_pkce:${state}`;
}

/**
 * 保存 PKCE 会话到 sessionStorage。
 *
 * @param session  verifier / state / returnUrl
 */
export function savePkceSession(session: PkceSession): void {
  sessionStorage.setItem(pkceStorageKey(session.state), JSON.stringify(session));
}

/**
 * 读取并清除 PKCE 会话（一次性消费）。
 *
 * @param state OAuth2 state 参数
 */
export function consumePkceSession(state: string): PkceSession | null {
  const raw = sessionStorage.getItem(pkceStorageKey(state));
  sessionStorage.removeItem(pkceStorageKey(state));
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as PkceSession;
  } catch {
    return null;
  }
}

/**
 * 构造授权端点 URL（Authorization Code + PKCE）。
 *
 * @param config         OAuth 应用配置
 * @param codeChallenge  S256 challenge
 * @param state          CSRF state
 */
export function buildAuthorizeUrl(
  config: OAuthAppConfig,
  codeChallenge: string,
  state: string,
  options?: OAuthLoginOptions
): string {
  const params = new URLSearchParams({
    response_type: "code",
    client_id: config.clientId,
    redirect_uri: config.redirectUri,
    scope: config.scope,
    state,
    code_challenge: codeChallenge,
    code_challenge_method: "S256"
  });
  if (options?.prompt) {
    params.set("prompt", options.prompt);
  }
  return `${config.authorizeUrl}?${params.toString()}`;
}

/**
 * 启动 OAuth2 登录：生成 PKCE → 保存会话 → 浏览器跳转到 /oauth2/authorize。
 *
 * @param config    OAuth 应用配置
 * @param returnUrl 登录成功后回跳的 SPA 路径
 */
/** 防止快速刷新时重复发起 PKCE，覆盖 sessionStorage 中未消费的 state 导致校验失败。 */
let loginRedirectLock = false;

export async function startOAuthLogin(
  config: OAuthAppConfig,
  returnUrl: string,
  options?: OAuthLoginOptions
): Promise<void> {
  // 已有进行中的登录跳转（当前页面生命周期内），忽略重复调用
  if (loginRedirectLock) {
    return;
  }
  loginRedirectLock = true;

  // 剥离 SPA HTML base（如 /admin/），确保 returnUrl 为路由器兼容的相对路径
  const normalizedReturnUrl = normalizeReturnUrl(returnUrl, config.basePath);

  try {
    // 始终生成全新 PKCE，不复用已有 session
    // 按 state 做键后，每个 OAuth 请求天然隔离，不需要复用逻辑
    const codeVerifier = generateCodeVerifier();
    const state = generateState();
    savePkceSession({ codeVerifier, state, returnUrl: normalizedReturnUrl });

    const codeChallenge = await generateCodeChallenge(codeVerifier);
    window.location.href = buildAuthorizeUrl(config, codeChallenge, state, options);
  } finally {
    loginRedirectLock = false;
  }
}

/**
 * 使用 authorization_code + code_verifier 交换 access_token。
 *
 * @param config       OAuth 应用配置
 * @param code         回调 URL 中的 code
 * @param codeVerifier 授权前保存的 verifier
 */
export async function exchangeAuthorizationCode(
  config: OAuthAppConfig,
  code: string,
  codeVerifier: string
): Promise<OAuthTokenResponse> {
  const body = new URLSearchParams({
    grant_type: "authorization_code",
    code,
    redirect_uri: config.redirectUri,
    client_id: config.clientId,
    code_verifier: codeVerifier
  });
  const response = await fetch(config.tokenUrl, {
    method: "POST",
    credentials: "omit",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: body.toString()
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`令牌交换失败 (${response.status}): ${text}`);
  }
  return (await response.json()) as OAuthTokenResponse;
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
    client_id: config.clientId
  });
  const response = await fetch(config.tokenUrl, {
    method: "POST",
    credentials: "omit",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: body.toString()
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`令牌续期失败 (${response.status}): ${text}`);
  }
  return (await response.json()) as OAuthTokenResponse;
}

/**
 * 吊销 access_token 或 refresh_token（RFC 7009）。
 */
export async function revokeOAuthToken(
  config: OAuthAppConfig,
  token: string,
  tokenTypeHint: "access_token" | "refresh_token" = "access_token"
): Promise<void> {
  const revokeUrl = resolveRevokeUrl(config.tokenUrl);
  const body = new URLSearchParams({
    token,
    token_type_hint: tokenTypeHint,
    client_id: config.clientId
  });
  try {
    await fetch(revokeUrl, {
      method: "POST",
      credentials: "omit",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: body.toString()
    });
  } catch {
    // logout 场景下 revoke 失败不阻断本地清理
  }
}

function resolveRevokeUrl(tokenUrl: string): string {
  if (tokenUrl.endsWith("/oauth2/token")) {
    return `${tokenUrl.slice(0, -"/oauth2/token".length)}/oauth2/revoke`;
  }
  return tokenUrl.replace(/\/token\/?$/, "/revoke");
}

/**
 * 将 API 基地址规范为可用于拼接 OAuth 端点的绝对前缀。
 * 相对路径（如 `/api`）在浏览器中取当前站点 origin。
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
 * 从 Vite 环境变量读取 OAuth 应用配置。
 * authorize 整页跳转与 token / revoke 均走同域路径，经 Nginx/Vite 代理到网关。
 *
 * @param env ImportMeta.env
 */
export function readOAuthConfigFromEnv(env: ImportMetaEnv): OAuthAppConfig {
  const apiPrefix = resolveApiBasePrefix(env.VITE_API_BASE_URL ?? "");
  const browserOAuthBase = resolveOAuthBrowserBase();
  return {
    clientId: env.VITE_OAUTH_CLIENT_ID,
    authorizeUrl: `${browserOAuthBase}/auth/oauth2/authorize`,
    tokenUrl: `${apiPrefix}/auth/oauth2/token`,
    redirectUri: env.VITE_OAUTH_REDIRECT_URI,
    scope: "profile offline_access all"  // 添加 offline_access 以请求 refresh token
  };
}

/**
 * 浏览器整页 OAuth 跳转使用的同域根 URL（经 Nginx/Vite 代理到网关，避免跨域 Cookie 分裂）。
 */
function resolveOAuthBrowserBase(): string {
  const origin = typeof window !== "undefined" ? window.location.origin : "";
  return origin;
}

/** Vite 环境变量扩展（各 SPA 的 env.d.ts 应引用相同字段）。 */
export interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_OAUTH_CLIENT_ID: string;
  readonly VITE_OAUTH_REDIRECT_URI: string;
}
