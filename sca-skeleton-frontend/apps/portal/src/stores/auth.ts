import { defineStore } from "pinia";
import { getPortalOAuthConfig } from "../config/oauth";
import { getUserProfileApi } from "../apis/user";
import {
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY
} from "../constants/auth-storage";

export interface PortalProfile {
  id: string;
  username: string;
  nickname: string;
}

interface PortalAuthState {
  token: string;
  refreshToken: string;
  profile: PortalProfile | null;
}

export const usePortalAuthStore = defineStore("portal-auth", {
  state: (): PortalAuthState => ({
    token: localStorage.getItem(TOKEN_STORAGE_KEY) ?? "",
    refreshToken: localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY) ?? "",
    profile: null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    async applyOAuthTokens(accessToken: string, refreshToken?: string) {
      this.token = accessToken;
      this.refreshToken = refreshToken ?? "";
      localStorage.setItem(TOKEN_STORAGE_KEY, accessToken);
      if (refreshToken) {
        localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
      }
    },
    syncOAuthTokens(accessToken: string, refreshToken?: string) {
      this.token = accessToken;
      this.refreshToken = refreshToken ?? "";
    },
    async fetchProfile() {
      if (!this.token) {
        this.profile = null;
        return;
      }
      this.profile = await getUserProfileApi();
    },
    async logout() {
      const oauthConfig = getPortalOAuthConfig();
      const accessToken = this.token;
      const refreshToken = this.refreshToken;
      this.token = "";
      this.refreshToken = "";
      this.profile = null;
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
      // 按 state 做键后，不需要手动清除 PKCE 会话
      // sessionStorage 会随 tab 关闭自动清空，且每个 OAuth 请求使用独立的 state
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
      appendInput("channel", "portal");
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
