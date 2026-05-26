import { createOAuthAxiosInstance, oauthRequest } from "@repo/shared";
import type { ApiEnvelope } from "../types/auth";
import { getAdminOAuthConfig } from "../config/oauth";
import {
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY
} from "../constants/auth-storage";

const REQUEST_TIMEOUT = 10_000;
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

let onTokensUpdated: ((accessToken: string, refreshToken?: string) => void) | undefined;

/** 注册 token refresh 后的 Pinia 同步回调（在 main.ts 中调用，避免 http ↔ store 循环依赖）。 */
export function registerAdminTokenSync(
  handler: (accessToken: string, refreshToken?: string) => void
): void {
  onTokensUpdated = handler;
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
  }
};

export const http = createOAuthAxiosInstance(oauthAxiosOptions, {
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT
});

export async function request<T>(config: Parameters<typeof http.request>[0]): Promise<ApiEnvelope<T>> {
  return oauthRequest<T>(http, oauthAxiosOptions, config);
}
