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

let refreshPromise: Promise<string | null> | null = null;

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

async function refreshAccessTokenOnce(options: OAuthAxiosOptions): Promise<string | null> {
  if (refreshPromise) {
    return refreshPromise;
  }
  refreshPromise = (async () => {
    const refreshToken = options.getRefreshToken();
    if (!refreshToken) {
      return null;
    }
    try {
      const tokenResponse = await refreshAccessToken(options.getOAuthConfig(), refreshToken);
      options.setTokens(tokenResponse.access_token, tokenResponse.refresh_token);
      options.onTokensUpdated?.(tokenResponse.access_token, tokenResponse.refresh_token);
      return tokenResponse.access_token;
    } catch {
      return null;
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
      const newAccessToken = await refreshAccessTokenOnce(options);
      if (newAccessToken) {
        config._oauthRetried = true;
        config.headers.set("Authorization", `Bearer ${newAccessToken}`);
        return instance.request(config);
      }
    }
    // 令牌刷新失败：仅当本地仍有令牌时清理并跳转登录，避免并发 401 重复触发副作用。
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
      if (error.code === "ECONNABORTED" || /timeout/i.test(error.message ?? "")) {
        const msg = resolveMessage(options, "api.timeout", "请求超时，请稍后重试");
        options.showNotification?.("negative", msg);
        return rejectHandled(msg);
      }
      // 无 response：网络断开 / DNS 失败 / 请求未发出等，此前会把英文 "Network Error" 弹给用户。
      if (!error.response) {
        const msg = resolveMessage(options, "api.networkError", "网络异常，请稍后重试");
        options.showNotification?.("negative", msg);
        return rejectHandled(msg);
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
    const newAccessToken = await refreshAccessTokenOnce(options);
    if (newAccessToken && !retriedConfig._oauthRetried) {
      retriedConfig._oauthRetried = true;
      return oauthRequest(instance, options, retriedConfig);
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
