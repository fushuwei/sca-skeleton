import { defineStore } from "pinia";
import type { Router } from "vue-router";
import { getAdminOAuthConfig } from "../config/oauth";
import { getUserProfileApi } from "../apis/user";
import { getUserPermissionsApi } from "../apis/permission";
import { ensureDynamicRoutes } from "../router/dynamic";
import {
  MENUS_STORAGE_KEY,
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY
} from "../constants/auth-storage";
import type { MenuItem, UserProfile, SysPermission } from "../types/auth";
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

/** 将后端返回的扁平权限列表转换为前端树形菜单（仅 module/folder/menu） */
function buildMenuTree(permissions: SysPermission[]): MenuItem[] {
  if (!permissions.length) return [];

  const filtered = permissions.filter(p => ["module", "folder", "menu"].includes(p.type));
  const sorted = [...filtered].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));

  const map = new Map<string, MenuItem>();
  for (const p of sorted) {
    map.set(p.id, {
      id: p.id,
      name: p.name ?? "",
      nameEn: p.nameEn || undefined,
      path: p.path ?? "",
      component: p.component as MenuItem["component"] ?? undefined,
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

interface AuthState {
  token: string;
  refreshToken: string;
  /** 当前用户所有权限（含 button），用于权限校验 */
  permissions: SysPermission[];
  /** 菜单树（仅 module/folder/menu），用于侧栏展示 */
  menus: MenuItem[];
  profile: UserProfile | null;
  dynamicReady: boolean;
}

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    token: localStorage.getItem(TOKEN_STORAGE_KEY) ?? "",
    refreshToken: localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY) ?? "",
    permissions: [],
    menus: readCachedMenus(),
    profile: null,
    dynamicReady: false
  }),
  getters: {
    isLoggedIn: (state): boolean => Boolean(state.token),
    /** 当前登录用户是否为平台超级管理员 */
    isSuperadmin: (state): boolean => state.profile?.isSuperadmin === 1
  },
  actions: {
    /**
     * 密码模式登录：调用 /oauth2/token 获取令牌，然后加载用户权限。
     *
     * @param username 用户名
     * @param password 密码
     * @returns 登录成功返回 true，失败抛出异常
     */
    async login(username: string, password: string): Promise<void> {
      const config = getAdminOAuthConfig();
      const tokenResponse = await loginWithPassword(config, username, password);

      this.token = tokenResponse.access_token;
      this.refreshToken = tokenResponse.refresh_token ?? "";
      localStorage.setItem(TOKEN_STORAGE_KEY, this.token);
      if (tokenResponse.refresh_token) {
        localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, tokenResponse.refresh_token);
      }

      // 获取用户权限并构建菜单树
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
    syncOAuthTokens(accessToken: string, refreshToken?: string): void {
      this.token = accessToken;
      this.refreshToken = refreshToken ?? "";
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
    /**
     * 退出登录：吊销令牌并清除本地状态。
     *
     * 密码模式下为无状态认证，仅需吊销 access_token / refresh_token 并清理本地存储，
     * 不再需要后端表单提交跳转。
     */
    async logout(router: Router): Promise<void> {
      const config = getAdminOAuthConfig();
      const accessToken = this.token;
      const refreshToken = this.refreshToken;

      // 异步吊销令牌（失败不阻断本地清理）
      if (accessToken) {
        void revokeOAuthToken(config, accessToken, "access_token");
      }
      if (refreshToken) {
        void revokeOAuthToken(config, refreshToken, "refresh_token");
      }

      // 清除本地存储与内存状态
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
      localStorage.removeItem(MENUS_STORAGE_KEY);
      this.token = "";
      this.refreshToken = "";
      this.permissions = [];
      this.menus = [];
      this.profile = null;
      this.dynamicReady = false;

      // 导航到登录页
      await router.push({ name: "Login" });
    }
  }
});
