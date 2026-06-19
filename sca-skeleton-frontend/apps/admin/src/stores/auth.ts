import { defineStore } from "pinia";
import type { Router } from "vue-router";
import { consumePkceSession } from "@repo/shared";
import { getAdminOAuthConfig } from "../config/oauth";
import { getUserProfileApi } from "../apis/user";
import { ensureDynamicRoutes, resetDynamicRoutes } from "../router/dynamic";
import { DEMO_MENUS } from "../apis/mock/menus";
import {
  MENUS_STORAGE_KEY,
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY
} from "../constants/auth-storage";
import type { MenuItem, UserProfile } from "../types/auth";

function readCachedMenus(): MenuItem[] {
  const raw = localStorage.getItem(MENUS_STORAGE_KEY);
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw) as MenuItem[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

interface AuthState {
  token: string;
  refreshToken: string;
  menus: MenuItem[];
  profile: UserProfile | null;
  dynamicReady: boolean;
}

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    token: localStorage.getItem(TOKEN_STORAGE_KEY) ?? "",
    refreshToken: localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY) ?? "",
    menus: readCachedMenus(),
    profile: null,
    dynamicReady: false
  }),
  getters: {
    isLoggedIn: (state): boolean => Boolean(state.token)
  },
  actions: {
    /** OAuth2 PKCE 回调成功后写入令牌并加载演示菜单（菜单 API 待后续对接）。 */
    async applyOAuthTokens(accessToken: string, refreshToken?: string): Promise<void> {
      this.token = accessToken;
      this.refreshToken = refreshToken ?? "";
      this.menus = DEMO_MENUS;
      localStorage.setItem(TOKEN_STORAGE_KEY, this.token);
      if (refreshToken) {
        localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
      }
      localStorage.setItem(MENUS_STORAGE_KEY, JSON.stringify(this.menus));
      this.dynamicReady = false;
    },
    /** Axios 静默 refresh 成功后同步 Pinia 内存态。 */
    syncOAuthTokens(accessToken: string, refreshToken?: string): void {
      this.token = accessToken;
      if (refreshToken) {
        this.refreshToken = refreshToken;
      }
    },
    async fetchProfile(): Promise<void> {
      if (!this.token) {
        this.profile = null;
        return;
      }
      const response = await getUserProfileApi();
      this.profile = response.data;
    },
    ensureRoutes(router: Router): void {
      if (!this.token || this.dynamicReady) {
        return;
      }
      ensureDynamicRoutes(router, this.menus);
      this.dynamicReady = true;
    },
    /** 退出：前端仅调用统一退出端点，认证服务完成会话与令牌清理。 */
    async logout(router: Router): Promise<void> {
      const oauthConfig = getAdminOAuthConfig();
      const accessToken = this.token;
      const refreshToken = this.refreshToken;
      this.token = "";
      this.refreshToken = "";
      this.menus = [];
      this.profile = null;
      this.dynamicReady = false;
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
      localStorage.removeItem(MENUS_STORAGE_KEY);
      // 清除 sessionStorage 中残留的 PKCE 会话，避免二次登录时复用旧 state
      // 导致 OAuth 回调 state 校验失败
      consumePkceSession(oauthConfig.clientId);
      resetDynamicRoutes(router);
      const logoutUrl = oauthConfig.authorizeUrl.replace("/oauth2/authorize", "/logout");
      const form = document.createElement("form");
      form.method = "POST";
      form.action = logoutUrl;
      form.style.display = "none";
      const appendInput = (name: string, value: string) => {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = name;
        input.value = value;
        form.appendChild(input);
      };
      appendInput("channel", "admin");
      if (accessToken) {
        appendInput("access_token", accessToken);
      }
      if (refreshToken) {
        appendInput("refresh_token", refreshToken);
      }
      document.body.appendChild(form);
      form.submit();
    }
  }
});
