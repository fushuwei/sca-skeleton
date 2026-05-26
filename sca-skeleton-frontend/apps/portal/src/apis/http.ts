import { createOAuthAxiosInstance, oauthRequest } from "@repo/shared";
import { getPortalOAuthConfig } from "../config/oauth";
import {
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY
} from "../constants/auth-storage";

const REQUEST_TIMEOUT = 10_000;
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

let onTokensUpdated: ((accessToken: string, refreshToken?: string) => void) | undefined;

/** 注册 token refresh 后的 Pinia 同步回调。 */
export function registerPortalTokenSync(
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
  getOAuthConfig: getPortalOAuthConfig,
  onTokensUpdated: (accessToken: string, refreshToken?: string) => {
    onTokensUpdated?.(accessToken, refreshToken);
  }
};

export const http = createOAuthAxiosInstance(oauthAxiosOptions, {
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT
});

export async function portalRequest<T>(
  config: Parameters<typeof http.request>[0]
): Promise<{ code: number; message: string; data: T }> {
  return oauthRequest<T>(http, oauthAxiosOptions, config);
}
