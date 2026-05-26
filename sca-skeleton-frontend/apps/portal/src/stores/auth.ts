import { defineStore } from "pinia";
import { revokeOAuthToken, startOAuthLogin } from "@repo/shared";
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
      if (refreshToken) {
        this.refreshToken = refreshToken;
      }
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
      if (this.token) {
        await revokeOAuthToken(oauthConfig, this.token, "access_token");
      }
      if (this.refreshToken) {
        await revokeOAuthToken(oauthConfig, this.refreshToken, "refresh_token");
      }
      this.token = "";
      this.refreshToken = "";
      this.profile = null;
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
      void startOAuthLogin(oauthConfig, "/", { prompt: "login" });
    }
  }
});
