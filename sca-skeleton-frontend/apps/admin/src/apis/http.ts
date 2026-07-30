import { createOAuthAxiosInstance, oauthRequest, showToast } from "@repo/shared";
import type { ApiEnvelope } from "../types/auth";
import { getAdminOAuthConfig } from "../config/oauth";
import {
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY
} from "../constants/auth-storage";

const REQUEST_TIMEOUT = 15_000;
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

let onTokensUpdated: ((accessToken: string, refreshToken?: string) => void) | undefined;

/** 注册 token refresh 后的 Pinia 同步回调（在 main.ts 中调用，避免 http ↔ store 循环依赖）。 */
export function registerAdminTokenSync(
  handler: (accessToken: string, refreshToken?: string) => void
): void {
  onTokensUpdated = handler;
}

let translateFn:
  | ((key: string, params?: Record<string, string | number>) => string)
  | undefined;

/**
 * 注册 i18n 翻译回调，供 HTTP 错误拦截器国际化提示文案。
 * <p>
 * 因 http.ts 模块加载早于 vue-i18n 实例创建，需在 main.ts 启动阶段调用此方法延迟注入
 * （与 {@link registerAdminTokenSync} 同理）。
 */
export function registerAdminTranslator(
  fn: (key: string, params?: Record<string, string | number>) => string
): void {
  translateFn = fn;
}

const oauthAxiosOptions = {
  getAccessToken: () => localStorage.getItem(TOKEN_STORAGE_KEY),
  getRefreshToken: () => localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY),
  setTokens: (accessToken: string, refreshToken?: string) => {
    localStorage.setItem(TOKEN_STORAGE_KEY, accessToken);
    if (refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
    }
  },
  clearTokens: () => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
  },
  getOAuthConfig: getAdminOAuthConfig,
  onTokensUpdated: (accessToken: string, refreshToken?: string) => {
    onTokensUpdated?.(accessToken, refreshToken);
  },
  redirectToLogin: () => {
    // 令牌失效且刷新失败时，整页跳转到登录页
    const basePath = import.meta.env.BASE_URL;
    const loginPath = basePath.endsWith("/") ? basePath + "login" : basePath + "/login";
    window.location.href = loginPath;
  },
  showNotification: (type: "positive" | "negative" | "warning", message: string) => {
    showToast(message, type);
  },
  translate: (key: string, params?: Record<string, string | number>) =>
    translateFn?.(key, params) ?? key
};

export const http = createOAuthAxiosInstance(oauthAxiosOptions, {
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT
});

export async function request<T>(config: Parameters<typeof http.request>[0]): Promise<ApiEnvelope<T>> {
  return oauthRequest<T>(http, oauthAxiosOptions, config);
}
