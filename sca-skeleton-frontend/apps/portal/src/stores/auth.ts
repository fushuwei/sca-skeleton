import { defineStore } from "pinia";
import type { Router } from "vue-router";
import { getPortalOAuthConfig } from "../config/oauth";
import { getUserProfileApi, getUserPermissionsApi } from "../apis/user";
import { ensureDynamicRoutes } from "../router/dynamic";
import {
  MENUS_STORAGE_KEY,
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY
} from "../constants/auth-storage";
import type { MenuItem, PortalProfile, SysPermission } from "../types/auth";
import { loginWithPassword, revokeOAuthToken } from "@repo/shared";

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

/** 将后端返回的扁平权限列表转换为前端树形菜单（仅 folder/menu） */
function buildMenuTree(permissions: SysPermission[]): MenuItem[] {
  if (!permissions.length) return [];

  const filtered = permissions.filter((p) => ["folder", "menu"].includes(p.type));
  const sorted = [...filtered].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));

  const map = new Map<string, MenuItem>();
  for (const p of sorted) {
    map.set(p.id, {
      id: p.id,
      name: p.name ?? "",
      nameEn: p.nameEn || undefined,
      path: p.path ?? "",
      component: (p.component as MenuItem["component"]) ?? undefined,
      icon: p.icon ?? undefined,
      children: []
    });
  }

  const roots: MenuItem[] = [];
  for (const p of sorted) {
    const node = map.get(p.id)!;
    if (!p.parentId || p.parentId === "0") {
      roots.push(node);
    } else {
      const parent = map.get(p.parentId);
      if (parent) {
        parent.children = parent.children ?? [];
        parent.children.push(node);
      } else {
        roots.push(node);
      }
    }
  }

  const cleanEmpty = (nodes: MenuItem[]) => {
    for (const n of nodes) {
      if (n.children?.length) {
        cleanEmpty(n.children);
      } else {
        delete n.children;
      }
    }
  };
  cleanEmpty(roots);

  return roots;
}

interface PortalAuthState {
  token: string;
  refreshToken: string;
  /** 当前用户所有权限（含 button），用于权限校验 */
  permissions: SysPermission[];
  /** 菜单树（仅 folder/menu），用于顶部导航展示 */
  menus: MenuItem[];
  profile: PortalProfile | null;
  dynamicReady: boolean;
}

export const usePortalAuthStore = defineStore("portal-auth", {
  state: (): PortalAuthState => ({
    token: localStorage.getItem(TOKEN_STORAGE_KEY) ?? "",
    refreshToken: localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY) ?? "",
    permissions: [],
    menus: readCachedMenus(),
    profile: null,
    dynamicReady: false
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    isSuperadmin: (state): boolean => state.profile?.isSuperadmin === 1
  },
  actions: {
    /**
     * 密码模式登录：调用 /oauth2/token 获取令牌，然后加载用户权限。
     * Portal 渠道需要图形验证码。
     *
     * @param username    用户名
     * @param password    密码
     * @param captchaKey  验证码 key
     * @param captchaCode 验证码文本
     */
    async login(username: string, password: string, captchaKey: string, captchaCode: string): Promise<void> {
      const config = getPortalOAuthConfig();
      const tokenResponse = await loginWithPassword(config, username, password, captchaKey, captchaCode);

      this.token = tokenResponse.access_token;
      this.refreshToken = tokenResponse.refresh_token ?? "";
      localStorage.setItem(TOKEN_STORAGE_KEY, this.token);
      if (tokenResponse.refresh_token) {
        localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, tokenResponse.refresh_token);
      }

      try {
        const result = await getUserPermissionsApi();
        if (result.code === 10_000 && result.data) {
          this.permissions = result.data;
          this.menus = buildMenuTree(result.data);
        } else {
          this.permissions = [];
          this.menus = [];
        }
      } catch {
        this.permissions = [];
        this.menus = [];
      }
      localStorage.setItem(MENUS_STORAGE_KEY, JSON.stringify(this.menus));
      this.dynamicReady = false;
    },

    /** Axios 静默 refresh 成功后同步 Pinia 内存态。 */
    syncOAuthTokens(accessToken: string, refreshToken?: string) {
      this.token = accessToken;
      this.refreshToken = refreshToken ?? "";
    },
    async fetchProfile() {
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
    /**
     * 重置全部认证状态到未登录态（内存 + localStorage）。
     *
     * 单一清理入口：logout() 和路由守卫认证失败时均调用此方法，
     * 避免多处复制清理逻辑导致字段遗漏。
     */
    resetState(): void {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
      localStorage.removeItem(MENUS_STORAGE_KEY);
      this.token = "";
      this.refreshToken = "";
      this.permissions = [];
      this.menus = [];
      this.profile = null;
      this.dynamicReady = false;
    },
    /**
     * 退出登录：吊销令牌并清除本地状态。
     */
    async logout(router: Router): Promise<void> {
      const config = getPortalOAuthConfig();
      const accessToken = this.token;
      const refreshToken = this.refreshToken;

      if (accessToken) {
        void revokeOAuthToken(config, accessToken, "access_token");
      }
      if (refreshToken) {
        void revokeOAuthToken(config, refreshToken, "refresh_token");
      }

      this.resetState();

      await router.push({ name: "Login" });
    }
  }
});
