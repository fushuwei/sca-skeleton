import axios, { type AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from "axios";
import {
  API_SUCCESS_CODE,
  API_UNAUTHORIZED_CODE
} from "./constants";
import type { OAuthAppConfig, OAuthLoginOptions } from "./pkce";
import { refreshAccessToken, startOAuthLogin } from "./pkce";

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
  unauthorizedCode?: number;
  /** 全局通知回调，用于在 HTTP 错误时弹出提示（可选）。 */
  showNotification?: (type: NotificationType, message: string) => void;
}

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _oauthRetried?: boolean;
}

let refreshPromise: Promise<string | null> | null = null;

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

  async function redirectToLogin(returnUrl?: string, loginOptions?: OAuthLoginOptions): Promise<void> {
    options.clearTokens();
    // 同步 Pinia store 内存态，确保 isLoggedIn 立即变为 false，
    // 避免路由守卫在页面跳转前因残留的 isLoggedIn=true 状态而错误放行
    options.onTokensUpdated?.("", undefined);
    await startOAuthLogin(
      options.getOAuthConfig(),
      returnUrl ?? window.location.pathname,
      loginOptions ?? { prompt: "login" }
    );
  }

  async function handleUnauthorized(
    config: RetryableRequestConfig | undefined,
    returnUrl: string
  ): Promise<unknown> {
    if (config && !config._oauthRetried) {
      const newAccessToken = await refreshAccessTokenOnce(options);
      if (newAccessToken) {
        config._oauthRetried = true;
        config.headers.set("Authorization", `Bearer ${newAccessToken}`);
        return instance.request(config);
      }
    }
    await redirectToLogin(returnUrl);
    return Promise.reject(new Error("登录已过期，请重新登录"));
  }

  instance.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const config = error.config as RetryableRequestConfig | undefined;
      if (error.response?.status === 401) {
        return handleUnauthorized(config, window.location.pathname);
      }
      if (error.response?.status === 403) {
        options.showNotification?.("negative", "权限不足，无法访问该功能");
        const handled = new Error("权限不足，无法访问该功能");
        (handled as Error & { _notificationHandled?: boolean })._notificationHandled = true;
        return Promise.reject(handled);
      }
      if (error.response?.status === 429) {
        options.showNotification?.("negative", "请求过于频繁，请稍后重试");
        const handled = new Error("请求过于频繁，请稍后重试");
        (handled as Error & { _notificationHandled?: boolean })._notificationHandled = true;
        return Promise.reject(handled);
      }
      if (error.response && error.response.status >= 500) {
        options.showNotification?.("negative", "服务器异常，请稍后重试");
        const handled = new Error("服务器异常，请稍后重试");
        (handled as Error & { _notificationHandled?: boolean })._notificationHandled = true;
        return Promise.reject(handled);
      }
      const msg = error.message || "网络异常，请稍后重试";
      options.showNotification?.("negative", msg);
      const handled = new Error(msg);
      (handled as Error & { _notificationHandled?: boolean })._notificationHandled = true;
      return Promise.reject(handled);
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
    options.clearTokens();
    await startOAuthLogin(options.getOAuthConfig(), window.location.pathname, { prompt: "login" });
    throw new Error(payload.message || "登录已过期，请重新登录");
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
