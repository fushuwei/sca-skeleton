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
 * 构造 sessionStorage 键名（按 clientId 隔离 admin / portal）。
 *
 * @param clientId OAuth2 client_id
 */
function pkceStorageKey(clientId: string): string {
  return `oauth_pkce_session:${clientId}`;
}

/**
 * 保存 PKCE 会话到 sessionStorage。
 *
 * @param clientId OAuth2 client_id
 * @param session  verifier / state / returnUrl
 */
export function savePkceSession(clientId: string, session: PkceSession): void {
  sessionStorage.setItem(pkceStorageKey(clientId), JSON.stringify(session));
}

/**
 * 读取并清除 PKCE 会话（一次性消费）。
 *
 * @param clientId OAuth2 client_id
 */
export function consumePkceSession(clientId: string): PkceSession | null {
  const raw = sessionStorage.getItem(pkceStorageKey(clientId));
  sessionStorage.removeItem(pkceStorageKey(clientId));
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
export async function startOAuthLogin(
  config: OAuthAppConfig,
  returnUrl: string,
  options?: OAuthLoginOptions
): Promise<void> {
  const codeVerifier = generateCodeVerifier();
  const codeChallenge = await generateCodeChallenge(codeVerifier);
  const state = generateState();
  savePkceSession(config.clientId, { codeVerifier, state, returnUrl });
  window.location.href = buildAuthorizeUrl(config, codeChallenge, state, options);
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
 * 从 Vite 环境变量读取 OAuth 应用配置。
 *
 * @param env ImportMeta.env
 */
export function readOAuthConfigFromEnv(env: ImportMetaEnv): OAuthAppConfig {
  return {
    clientId: env.VITE_OAUTH_CLIENT_ID,
    authorizeUrl: env.VITE_OAUTH_AUTHORIZE_URL,
    tokenUrl: env.VITE_OAUTH_TOKEN_URL,
    redirectUri: env.VITE_OAUTH_REDIRECT_URI,
    scope: env.VITE_OAUTH_SCOPE ?? "openid profile all"
  };
}

/** Vite 环境变量扩展（各 SPA 的 env.d.ts 应引用相同字段）。 */
export interface ImportMetaEnv {
  readonly VITE_OAUTH_CLIENT_ID: string;
  readonly VITE_OAUTH_AUTHORIZE_URL: string;
  readonly VITE_OAUTH_TOKEN_URL: string;
  readonly VITE_OAUTH_REDIRECT_URI: string;
  readonly VITE_OAUTH_SCOPE?: string;
}
