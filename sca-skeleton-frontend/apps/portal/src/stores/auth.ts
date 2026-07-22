import { defineStore } from "pinia";
import type { Router } from "vue-router";
import { getPortalOAuthConfig } from "../config/oauth";
import { getUserProfileApi, getUserMenusApi } from "../apis/user";
import { ensureDynamicRoutes } from "../router/dynamic";
import {
  MENUS_STORAGE_KEY,
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY
} from "../constants/auth-storage";
import type { MenuItem, PortalProfile, SysPermission } from "../types/auth";

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

  // 按 type 过滤：只保留 folder、menu（按钮权限不参与菜单树构建）
  const filtered = permissions.filter((p) => ["folder", "menu"].includes(p.type));

  // 按 sort 排序
  const sorted = [...filtered].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));

  // 构建 map
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

  // 构建树
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

  // 清理空 children
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
    /** OAuth2 PKCE 回调成功后写入令牌并加载菜单。 */
    async applyOAuthTokens(accessToken: string, refreshToken?: string) {
      this.token = accessToken;
      this.refreshToken = refreshToken ?? "";
      localStorage.setItem(TOKEN_STORAGE_KEY, accessToken);
      if (refreshToken) {
        localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
      }
      // 从后端获取用户权限
      try {
        const result = await getUserMenusApi();
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
    async logout() {
      const oauthConfig = getPortalOAuthConfig();
      const accessToken = this.token;
      const refreshToken = this.refreshToken;

      // 仅清除持久化状态，不清内存（form.submit 会触发浏览器导航，页面销毁后内存自然释放）
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
      localStorage.removeItem(MENUS_STORAGE_KEY);

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
