import axios, { type AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from "axios";
import {
  API_SUCCESS_CODE,
  API_UNAUTHORIZED_CODE
} from "./constants";
import type { OAuthAppConfig } from "./password-grant";
import { refreshAccessToken } from "./password-grant";

/** 通知类型，与各 UI 框架的语义对齐。 */
export type NotificationType = "positive" | "negative" | "warning";

/** OAuth Axios 客户端所需的外部化存储与配置回调。 */
export interface OAuthAxiosOptions {
  getAccessToken: () => string | null;
  getRefreshToken: () => string | null;
  setTokens: (accessToken: string, refreshToken?: string) => void;
  clearTokens: () => void;
  getOAuthConfig: () => OAuthAppConfig;
  /** refresh 成功后同步 Pinia 等内存态（可选）。 */
  onTokensUpdated?: (accessToken: string, refreshToken?: string) => void;
  /** 令牌失效且刷新失败时，由 SPA 导航到登录页（可选）。 */
  redirectToLogin?: () => void;
  unauthorizedCode?: number;
  /** 全局通知回调，用于在 HTTP 错误时弹出提示（可选）。 */
  showNotification?: (type: NotificationType, message: string) => void;
  /**
   * 文案翻译回调，用于将 HTTP 错误提示国际化（可选）。
   * <p>
   * 由各 SPA 在启动阶段注入（如 vue-i18n 的 `i18n.global.t`）。未注入时，
   * 所有错误文案回退到下方各分支的中文兜底，行为与历史版本一致。
   */
  translate?: (key: string, params?: Record<string, string | number>) => string;
}

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _oauthRetried?: boolean;
}

/** refresh 尝试的结果：区分"瞬时网络失败（勿登出）"与"令牌失效（应登出）"。 */
type RefreshOutcome =
  | { ok: true; accessToken: string }
  | { ok: false; transient: boolean };

let refreshPromise: Promise<RefreshOutcome> | null = null;

/**
 * 解析 HTTP 错误提示文案：优先走注入的 {@link OAuthAxiosOptions.translate}（i18n），
 * 未注入时回退到中文兜底，保持向后兼容。
 */
function resolveMessage(
  options: OAuthAxiosOptions,
  key: string,
  fallback: string
): string {
  return options.translate ? options.translate(key) : fallback;
}

/**
 * 构造一个"已弹出通知"的 reject 错误，并打上 `_notificationHandled` 标记。
 * <p>
 * 用于响应拦截器各分支，调用方 catch 块可通过 {@link isNotificationHandled}
 * 判定后跳过重复提示。
 */
function rejectHandled(message: string): Promise<never> {
  const handled = new Error(message);
  (handled as Error & { _notificationHandled?: boolean })._notificationHandled = true;
  return Promise.reject(handled);
}

/** Error 上附加的内部标记类型。 */
interface TaggedError extends Error {
  _notificationHandled?: boolean;
  _transient?: boolean;
}

/**
 * 构造一个"瞬时网络/中止"的错误，打上 `_transient` 标记（可重试的瞬时故障，如断网、浏览器导航中止、超时）。
 * <p>
 * 注意：此函数不设置 `_notificationHandled`。是否已弹通知取决于调用方：
 * - 响应拦截器在调用 {@link rejectTransient} 前已调用 showNotification，由 rejectTransient 补设标记；
 * - handleUnauthorized / oauthRequest 直接使用本函数，未弹通知，因此不设标记。
 */
function buildTransientError(message: string): TaggedError {
  const err = new Error(message) as TaggedError;
  err._transient = true;
  return err;
}

/**
 * 响应拦截器专用：构造瞬时错误并标记已弹通知（调用前已执行 showNotification），
 * 然后以 reject 返回。
 */
function rejectTransient(message: string): Promise<never> {
  const err = buildTransientError(message);
  err._notificationHandled = true;
  return Promise.reject(err);
}

/**
 * 判定错误是否为"瞬时网络 / 浏览器中止"（应重试而非登出）。
 *
 * 命中条件：
 * - 原生 `fetch`（如 refreshAccessToken）在网络错误 / 导航中止时抛出 `TypeError`；
 * - axios 拦截器将"无响应 / 超时"包装为带 `_transient` 标记的 Error。
 *
 * 真实 HTTP 错误（403 / 500 等）与认证失败（401）不匹配，按非瞬态处理。
 */
export function isTransientNetworkError(error: unknown): boolean {
  if (error instanceof TypeError) {
    return true;
  }
  return error instanceof Error && (error as TaggedError)._transient === true;
}

async function refreshAccessTokenOnce(options: OAuthAxiosOptions): Promise<RefreshOutcome> {
  if (refreshPromise) {
    return refreshPromise;
  }
  refreshPromise = (async () => {
    const refreshToken = options.getRefreshToken();
    if (!refreshToken) {
      // 无 refresh_token：不可能续期，视为"令牌失效"（非瞬态），由调用方走登出逻辑
      return { ok: false, transient: false };
    }
    try {
      const tokenResponse = await refreshAccessToken(options.getOAuthConfig(), refreshToken);
      options.setTokens(tokenResponse.access_token, tokenResponse.refresh_token);
      options.onTokensUpdated?.(tokenResponse.access_token, tokenResponse.refresh_token);
      return { ok: true, accessToken: tokenResponse.access_token };
    } catch (error) {
      // 瞬时网络 / 浏览器中止：不是刷新令牌失效，调用方应保留令牌、跳过清令牌副作用
      return { ok: false, transient: isTransientNetworkError(error) };
    } finally {
      refreshPromise = null;
    }
  })();
  return refreshPromise;
}

/**
 * 创建带 Bearer 注入、401 静默 refresh、业务码未认证跳转的 Axios 实例。
 *
 * 密码模式下，令牌失效且刷新失败时调用 {@link OAuthAxiosOptions.redirectToLogin}
 * 导航到 SPA 登录页（由各 SPA 提供具体实现）。
 */
export function createOAuthAxiosInstance(
  options: OAuthAxiosOptions,
  axiosDefaults?: Parameters<typeof axios.create>[0]
): AxiosInstance {
  const instance = axios.create(axiosDefaults);

  instance.interceptors.request.use((config) => {
    const token = options.getAccessToken();
    if (token) {
      config.headers.set("Authorization", `Bearer ${token}`);
    }
    return config;
  });

  async function handleUnauthorized(
    config: RetryableRequestConfig | undefined
  ): Promise<unknown> {
    if (config && !config._oauthRetried) {
      const outcome = await refreshAccessTokenOnce(options);
      if (outcome.ok) {
        config._oauthRetried = true;
        config.headers.set("Authorization", `Bearer ${outcome.accessToken}`);
        return instance.request(config);
      }
      // 瞬时网络/中止（非令牌失效）：保留令牌、不跳转，抛可重试错误
      if (outcome.transient) {
        const msg = resolveMessage(options, "api.networkError", "网络异常，请稍后重试");
        return Promise.reject(buildTransientError(msg));
      }
    }
    // 令牌刷新失败（或请求已被重试过）：仅当本地仍有令牌时清理并跳转登录，避免并发 401 重复触发副作用。
    // 多个并发 401 共享同一个 refreshPromise，resolve 后同步逐个执行本分支：
    // 第一个调用 clearTokens() 后 localStorage 被清空，后续调用 getAccessToken() 返回 null，跳过副作用。
    if (options.getAccessToken()) {
      options.clearTokens();
      options.onTokensUpdated?.("", undefined);
      options.redirectToLogin?.();
    }
    return Promise.reject(new Error(resolveMessage(options, "api.unauthorized", "登录已过期，请重新登录")));
  }

  instance.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const config = error.config as RetryableRequestConfig | undefined;
      if (error.response?.status === 401) {
        return handleUnauthorized(config);
      }
      // 请求超时：axios 置 error.code 为 ECONNABORTED，message 形如 "timeout of 10000ms exceeded"。
      // 必须在 401 之后、无 response 分支之前判断，避免把超时错误泄漏为英文原始 message。
      // 标记为 _transient，供路由守卫识别为"可重试、勿登出"的瞬时故障。
      if (error.code === "ECONNABORTED" || /timeout/i.test(error.message ?? "")) {
        const msg = resolveMessage(options, "api.timeout", "请求超时，请稍后重试");
        options.showNotification?.("negative", msg);
        return rejectTransient(msg);
      }
      // 无 response：网络断开 / DNS 失败 / 请求未发出 / 浏览器导航中止等，此前会把英文 "Network Error" 弹给用户。
      // 标记为 _transient，供路由守卫识别为"可重试、勿登出"的瞬时故障。
      if (!error.response) {
        const msg = resolveMessage(options, "api.networkError", "网络异常，请稍后重试");
        options.showNotification?.("negative", msg);
        return rejectTransient(msg);
      }
      if (error.response.status === 403) {
        const msg = resolveMessage(options, "api.forbidden", "权限不足，无法访问该功能");
        options.showNotification?.("negative", msg);
        return rejectHandled(msg);
      }
      if (error.response.status === 429) {
        const msg = resolveMessage(options, "api.tooManyRequests", "请求过于频繁，请稍后重试");
        options.showNotification?.("negative", msg);
        return rejectHandled(msg);
      }
      if (error.response.status >= 500) {
        const msg = resolveMessage(options, "api.serverError", "服务器异常，请稍后重试");
        options.showNotification?.("negative", msg);
        return rejectHandled(msg);
      }
      // 其余带 response 的非预期错误，回退到原始 message 或网络异常兜底。
      const msg = error.message || resolveMessage(options, "api.networkError", "网络异常，请稍后重试");
      options.showNotification?.("negative", msg);
      return rejectHandled(msg);
    }
  );

  return instance;
}

/**
 * 统一业务 envelope 请求：校验 code，未认证时尝试 refresh 或跳转登录。
 */
export async function oauthRequest<T>(
  instance: AxiosInstance,
  options: OAuthAxiosOptions,
  config: Parameters<AxiosInstance["request"]>[0]
): Promise<{ code: number; message: string; data: T }> {
  const unauthorizedCode = options.unauthorizedCode ?? API_UNAUTHORIZED_CODE;
  const response = await instance.request<{ code: number; message: string; data: T }>(config);
  const payload = response.data;
  if (payload.code === API_SUCCESS_CODE) {
    return payload;
  }
  if (payload.code === unauthorizedCode) {
    const retriedConfig = { ...(config as RetryableRequestConfig), _oauthRetried: false };
    const outcome = await refreshAccessTokenOnce(options);
    if (outcome.ok && !retriedConfig._oauthRetried) {
      retriedConfig._oauthRetried = true;
      return oauthRequest(instance, options, retriedConfig);
    }
    // 瞬时网络/中止（非令牌失效）：保留令牌，抛可重试错误，避免误登出
    if (!outcome.ok && outcome.transient) {
      const msg = resolveMessage(options, "api.networkError", "网络异常，请稍后重试");
      throw buildTransientError(msg);
    }
    // 令牌刷新失败：仅当本地仍有令牌时清理并跳转登录，避免并发重复触发副作用
    if (options.getAccessToken()) {
      options.clearTokens();
      options.onTokensUpdated?.("", undefined);
      options.redirectToLogin?.();
    }
    throw new Error(payload.message || resolveMessage(options, "api.unauthorized", "登录已过期，请重新登录"));
  }
  return payload;
}

/**
 * 判断错误是否已被全局拦截器处理（已弹出通知）。
 * <p>
 * 用于 caller 的 catch 块，避免重复弹出 toast：
 * <pre>
 * } catch (error) {
 *   if (!isNotificationHandled(error)) {
 *     showToast(t("common.loadFail"), "negative");
 *   }
 * }
 * </pre>
 */
export function isNotificationHandled(error: unknown): boolean {
  return error instanceof Error && "_notificationHandled" in error && (error as Error & { _notificationHandled?: boolean })._notificationHandled === true;
}
